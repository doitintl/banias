package publisher

import (
	"context"
	"sync"
	"time"

	gpubsub "cloud.google.com/go/pubsub"
	cfg "github.com/doitintl/banias/frontend/config"
	"github.com/doitintl/banias/frontend/types"
	"github.com/henrylee2cn/goutil/pool"
	"github.com/pquerna/ffjson/ffjson"
	"github.com/prometheus/client_golang/prometheus"
	"go.uber.org/zap"
)

var (
	promLabelNames = []string{"function"}
	publishCounter = prometheus.NewCounterVec(
		prometheus.CounterOpts(prometheus.Opts{
			Namespace: "banias",
			Subsystem: "publisher",
			Name:      "pubsub_publish_total",
			Help:      "pubsub publish total",
		}), promLabelNames)
	publishTimeSummary = prometheus.NewSummaryVec(prometheus.SummaryOpts{
		Namespace: "banias",
		Subsystem: "publisher",
		Name:      "pubsub_publish_duration_milliseconds",
		Help:      "pubsub publish duration (ms)",
	}, promLabelNames)
)

func init() {

	prometheus.MustRegister(publishCounter)
	prometheus.MustRegister(publishTimeSummary)

}

type Publisher struct {
	bqEvents      <-chan types.EventMsg
	logger        *zap.Logger
	gp            *pool.GoPool
	gpubsubClient gpubsub.Client
	config        *cfg.Config
	topic         *gpubsub.Topic
	client        *gpubsub.Client
	wg            *sync.WaitGroup
	id            int
}

func createTopicIfNotExists(projectid string, topic string) (*gpubsub.Topic, error) {
	ctx := context.Background()
	client, err := gpubsub.NewClient(ctx, projectid)
	if err != nil {
		return nil, err
	}

	// Create a topic to subscribe to.
	t := client.Topic(topic)
	ok, err := t.Exists(ctx)
	if err != nil {
		return t, err
	}
	if ok {
		return t, err
	}

	t, err = client.CreateTopic(ctx, topic)
	if err != nil {
		return t, err
	}
	return t, err
}

func NewPublisher(logger *zap.Logger, bqEvents <-chan types.EventMsg, config *cfg.Config, id int) (*Publisher, error) {
	gp := pool.NewGoPool(config.MaxPubSubGoroutinesAmount, config.MaxPubSubGoroutineIdleDuration)
	topic, err := createTopicIfNotExists(config.ProjectID, config.Topic)
	p := Publisher{
		bqEvents: bqEvents,
		logger:   logger,
		gp:       gp,
		config:   config,
		topic:    topic,
		wg:       new(sync.WaitGroup),
		id:       id,
	}
	if err != nil {
		logger.Error("Error creating topic", zap.Error(err))
	}
	return &p, err
}

func (c *Publisher) Publish(messages []gpubsub.Message, t *time.Timer, maxDelay time.Duration, ) {
	c.wg.Add(1)
	c.gp.Go(func() {
		defer func(begin time.Time) {
			promLabels := prometheus.Labels{"function": "Publish"}
			responseTime := time.Since(begin).Seconds() * 1000
			publishTimeSummary.With(promLabels).Observe(responseTime)

		}(time.Now())

		var total int64 = 0
		var errnum int64 = 0
		ctx := context.Background()
		var results []*gpubsub.PublishResult
		for i := range messages {
			r := c.topic.Publish(ctx, &messages[i])
			total++
			results = append(results, r)
		}
		for _, r := range results {
			id, err := r.Get(ctx)
			if err != nil {
				c.logger.Error("Error Publishing", zap.Error(err), zap.String("ID", id))
				errnum++
			}
		}

		messages = nil
		promLabels := prometheus.Labels{"function": "Publish"}
		publishCounter.With(promLabels).Add(float64(total))
		c.logger.Info("Published ", zap.Int64("Success", total-errnum), zap.Int64("Failures", errnum))
		t.Reset(maxDelay)
		c.wg.Done()
	})

}

func (c *Publisher) Run() {

	messages := make([]gpubsub.Message, 0, c.config.PubsubMaxBatch)
	t := time.NewTimer(c.config.PubsubMaxPublishDelay)
	for {
		select {
		case <-t.C:
			if len(messages) == 0 {
				c.logger.Debug("skipping publish due to no messages")
				t.Reset(c.config.PubsubMaxPublishDelay)
				continue
			}
			c.logger.Debug("Calling publish due to time", zap.Int("Number of message", len(messages)), zap.Int("Aggrigator ID", c.id))
			c.Publish(messages, t, c.config.PubsubMaxPublishDelay)
			messages = nil



		case event := <-c.bqEvents:

			buf, err := ffjson.Marshal(event)
			if err != nil {
				c.logger.Error("Error Marshaling event", zap.Error(err))
				continue
			}
			messages = append(messages, gpubsub.Message{Data: buf})
			if len(messages) == c.config.PubsubMaxBatch {

				c.logger.Debug("Calling publish due to capacity ", zap.Int("Number of message", len(messages)), zap.Int("Aggrigator ID", c.id))
				c.Publish(messages, t, c.config.PubsubMaxPublishDelay)
				messages = nil

			}
		}
	}

}
