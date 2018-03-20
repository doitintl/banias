package collector

import (
	"context"
	"encoding/json"
	"strconv"
	"sync"
	"time"

	"github.com/buger/jsonparser"
	cfg "github.com/doitintl/banias/frontend/config"
	"github.com/doitintl/banias/frontend/publisher"
	"github.com/doitintl/banias/frontend/types"
	"github.com/henrylee2cn/goutil/pool"
	"github.com/valyala/fasthttp"
	"go.opencensus.io/stats"
	"go.opencensus.io/tag"

	"go.opencensus.io/stats/view"
	"go.uber.org/zap"
)

var (
	strContentType     = []byte("Content-Type")
	strApplicationJSON = []byte("application/json")
	msgPool            *sync.Pool
	paths              = [][]string{
		[]string{"type", "event_version"},
		[]string{"type", "event_name"},
		[]string{"payload"},
	}
	DefaultLatencyDistribution = view.DistributionAggregation{0, 1, 2, 3, 4, 5, 6, 8, 10, 13, 16, 20, 25, 30, 40, 50, 65, 80, 100, 130, 160, 200, 250, 300, 400, 500, 650, 800, 1000, 2000, 5000, 10000, 20000, 50000, 100000}
	requestCounter             *stats.Float64Measure
	requestlatency             *stats.Float64Measure
	codeKey                    tag.Key
)

func init() {
	msgPool = &sync.Pool{
		New: func() interface{} {
			return new(types.EventMsg)
		},
	}
	codeKey, _ = tag.NewKey("banias/keys/code")
	requestCounter, _ = stats.Float64("banias/measures/request_count", "Count of HTTP requests processed", stats.UnitNone)
	requestlatency, _ = stats.Float64("banias/measures/request_latency", "Latency distribution of HTTP requests", stats.UnitMilliseconds)
	view.Subscribe(
		&view.View{
			Name:        "request_count",
			Description: "Count of HTTP requests processed",
			TagKeys:     []tag.Key{codeKey},
			Measure:     requestCounter,
			Aggregation: view.CountAggregation{},
		})
	view.Subscribe(
		&view.View{
			Name:        "request_latency",
			Description: "Latency distribution of HTTP requests",
			TagKeys:     []tag.Key{codeKey},
			Measure:     requestlatency,
			Aggregation: DefaultLatencyDistribution,
		})

	view.SetReportingPeriod(1 * time.Second)

}

type Collector struct {
	bqEvents chan<- types.EventMsg
	doneChan chan<- bool
	logger   *zap.Logger
	config   *cfg.Config
	gp       *pool.GoPool
}

func NewCollector(logger *zap.Logger, config *cfg.Config) (*Collector, error) {
	bqEvents := make(chan types.EventMsg, int(config.PubsubMaxBatch*config.PubSubAggrigators))
	doneChan := make(chan bool)
	gp := pool.NewGoPool(int(config.PubSubAggrigators), config.MaxPubSubGoroutineIdleDuration)
	c := Collector{
		bqEvents: bqEvents,
		doneChan: doneChan,
		logger:   logger,
		config:   config,
		gp:       gp,
	}

	for i := 0; i < config.PubSubAggrigators; i++ {
		client, err := publisher.GetClient(config.ProjectID)
		pub, err := publisher.NewPublisher(logger, bqEvents, config, client, i)
		if err == nil {
			c.gp.Go(pub.Run)
		} else {
			logger.Error("Error crating a publisher", zap.Error(err))
			return &c, err
		}

	}

	return &c, nil
}

func (c *Collector) Collect(ctx *fasthttp.RequestCtx) {
	defer func(begin time.Time) {
		responseTime := float64(time.Since(begin).Nanoseconds() / 1000)
		occtx, _ := tag.New(context.Background(), tag.Insert(codeKey, strconv.Itoa(ctx.Response.StatusCode())), )
		stats.Record(occtx, requestCounter.M(1))
		stats.Record(occtx, requestlatency.M(responseTime))

		c.logger.Debug(string(ctx.Path()), zap.String("method", string(ctx.Method())), zap.String("code", strconv.Itoa(ctx.Response.StatusCode())))
	}(time.Now())

	if !ctx.IsPost() {
		ctx.Error("Unsupported method", fasthttp.StatusMethodNotAllowed)
	}
	data := []byte(ctx.PostBody())
	var errors []types.Error
	senderID, err := jsonparser.GetString(data, "sender_id")
	if err != nil {
		ctx.Error(err.Error(), fasthttp.StatusNotAcceptable)
	}
	i := 0
	jsonparser.ArrayEach(data, func(events []byte, dataType jsonparser.ValueType, offset int, err error) {

		msg := msgPool.Get().(*types.EventMsg)
		i++
		counter := 0
		jsonparser.EachKey(events, func(idx int, value []byte, vt jsonparser.ValueType, err error) {
			switch idx {
			case 0:
				t, _ := jsonparser.ParseString(value)
				msg.Event.TypeField.EventVersionField = t
				counter = counter + 1
			case 1:
				t, _ := jsonparser.ParseString(value)
				msg.Event.TypeField.EventNameField = t
				counter = counter + 2
			case 2:
				counter = counter + 4
				t, _, _, _ := jsonparser.Get(value)
				err = json.Unmarshal(t, &msg.Event.PayloadField)
				if err != nil {
					c.logger.Error("Error getting payload ", zap.Error(err))
				}

			}
		}, paths...)
		if counter != 7 {
			var errString string
			switch counter {
			case 0:
				errString = "type/event_version type/event_name payload"
			case 1:
				errString = "type/event_name payload"
			case 2:
				errString = "type/event_version payload"
			case 3:
				errString = "payload"
			case 4:
				errString = "type/event_version type/event_name"
			case 5:
				errString = "type/event_name"
			case 6:
				errString = "type/event_version"
			}
			var eventErr *types.Error
			eventErr = types.NewError(uint64(i), "Missing fields or ill formatted "+errString)
			errors = append(errors, *eventErr)

		} else {
			msg.SenderID = senderID
			c.bqEvents <- *msg

		}
		msgPool.Put(msg)

	}, "events")
	ctx.Response.Header.SetCanonical(strContentType, strApplicationJSON)
	ctx.Response.SetStatusCode(202)
	json.NewEncoder(ctx).Encode(errors)

}

func (c *Collector) Stop() {
	c.gp.Stop()
	go func() {
		c.doneChan <- true
	}()
}
