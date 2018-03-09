package collector

import (
	"encoding/json"
	"strconv"
	"time"

	"github.com/buger/jsonparser"
	cfg "github.com/doitintl/banias/frontend/config"
	"github.com/doitintl/banias/frontend/publisher"
	"github.com/doitintl/banias/frontend/types"
	"github.com/prometheus/client_golang/prometheus"
	"github.com/valyala/fasthttp"
	"go.uber.org/zap"
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

func init() {

	prometheus.MustRegister(requestCounter)
	prometheus.MustRegister(responseTimeSummary)

}

type Collector struct {
	bqEvents chan<- types.EventMsg
	logger   *zap.Logger
	config   *cfg.Config
}

func NewCollector(logger *zap.Logger, config *cfg.Config) (*Collector, error) {
	bqEvents := make(chan types.EventMsg, int ((config.MaxPubSubGoroutinesAmount*config.PubsubMaxBatch*config.PubSubAggrigators)/config.PubSubAggrigators))
	c := Collector{
		bqEvents: bqEvents,
		logger:   logger,
		config:   config,
	}
	for i := 0; i < config.PubSubAggrigators; i++ {
		pub, err := publisher.NewPublisher(logger, bqEvents, config, i)
		if err == nil {
			go pub.Run()
		} else {
			logger.Error("Error crating a publisher",zap.Error(err))
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

		paths := [][]string{
			[]string{"type", "event_version"},
			[]string{"type", "event_name"},
			[]string{"payload"},
		}
		var (
			eventType types.Type
			event     types.Event
			msg       types.EventMsg
		)
		i++
		counter := 0
		jsonparser.EachKey(events, func(idx int, value []byte, vt jsonparser.ValueType, err error) {
			switch idx {
			case 0:
				t, _ := jsonparser.ParseString(value)
				eventType.EventVersionField = t
				counter = counter + 1
			case 1:
				t, _ := jsonparser.ParseString(value)
				eventType.EventNameField = t
				counter = counter + 2
			case 2:
				counter = counter + 4
				t, _, _, _ := jsonparser.Get(value)
				err = json.Unmarshal([]byte(t), &event.PayloadField)
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
			event.TypeField = &eventType
			msg.Event = event
			msg.SenderID = senderID
			c.bqEvents <- msg

		}

	}, "events")
	var (
		strContentType     = []byte("Content-Type")
		strApplicationJSON = []byte("application/json")
	)
	ctx.Response.Header.SetCanonical(strContentType, strApplicationJSON)
	ctx.Response.SetStatusCode(202)
	json.NewEncoder(ctx).Encode(errors)

}
