package collector

import (
	"encoding/json"
	"strconv"
	"sync"
	"time"

	"github.com/buger/jsonparser"
	cfg "github.com/doitintl/banias/frontend/config"
	"github.com/doitintl/banias/frontend/publisher"
	"github.com/doitintl/banias/frontend/types"
	"github.com/prometheus/client_golang/prometheus"
	"github.com/valyala/fasthttp"
	"go.uber.org/zap"
	"github.com/henrylee2cn/goutil/pool"
)

var (
	promLabelNames = []string{"code", "method", "path"}
	requestCounter = prometheus.NewCounterVec(
		prometheus.CounterOpts(prometheus.Opts{
			Namespace: "banias",
			Subsystem: "collector",
			Name:      "http_requests_total",
			Help:      "http requests total",
		}), promLabelNames)
	responseTimeSummary = prometheus.NewSummaryVec(prometheus.SummaryOpts{
		Namespace: "banias",
		Subsystem: "collector",
		Name:      "http_request_duration_milliseconds",
		Help:      "http request duration (ms)",
	}, promLabelNames)
)

var (
	strContentType     = []byte("Content-Type")
	strApplicationJSON = []byte("application/json")
	msgPool            *sync.Pool
	paths = [][]string{
		[]string{"type", "event_version"},
		[]string{"type", "event_name"},
		[]string{"payload"},
	}

)

func init() {
	msgPool = &sync.Pool{
		New: func() interface{} {
			return new(types.EventMsg)
		},
	}

	prometheus.MustRegister(requestCounter)
	prometheus.MustRegister(responseTimeSummary)

}

type Collector struct {
	bqEvents chan<- types.EventMsg
	doneChan chan<- bool
	logger   *zap.Logger
	config   *cfg.Config
	gp            *pool.GoPool
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
		gp :gp,
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
		promLabels := prometheus.Labels{"code": strconv.Itoa(ctx.Response.StatusCode()), "method": string(ctx.Method()), "path": string(ctx.Path())}
		responseTime := time.Since(begin).Seconds() * 1000
		responseTimeSummary.With(promLabels).Observe(responseTime)
		requestCounter.With(promLabels).Inc()
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
				err = json.Unmarshal([]byte(t), msg.Event.PayloadField)

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
