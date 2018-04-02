package main

import (
	"fmt"
	"net"
	"net/http"
	"os"
	"os/signal"
	"strconv"
	"syscall"

	cltr "github.com/doitintl/banias/frontend/collector"
	cfg "github.com/doitintl/banias/frontend/config"
	"github.com/oklog/oklog/pkg/group"

	"github.com/valyala/fasthttp"
	"go.opencensus.io/exporter/prometheus"
	"go.opencensus.io/exporter/stackdriver"
	"go.opencensus.io/stats/view"
	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
)

var config *cfg.Config
var metricsAddr string
var httpAddr string
var collector *cltr.Collector
// We have to endpoints /track for reporting and /metrics for prometheus.

func main() {
	config, _ = cfg.NewConfig()
	httpAddr = ":" + strconv.Itoa(config.Port)
	metricsAddr = ":" + strconv.Itoa(config.MetricsPort)
	atom := zap.NewAtomicLevel()
	if config.Debug {
		atom.SetLevel(zap.DebugLevel)
	}
	encoderCfg := zap.NewProductionEncoderConfig()
	logger := zap.New(zapcore.NewCore(
		zapcore.NewJSONEncoder(encoderCfg),
		zapcore.Lock(os.Stdout),
		atom,
	))
	defer logger.Sync()
	logger.Info("Starting Banias....")
	var err error


	pExporter, err := prometheus.NewExporter(prometheus.Options{})
	if err != nil {
		logger.Error("Error creating prometheus exporter  ", zap.Error(err))
	}
	// Export to Prometheus Monitoring.
	view.RegisterExporter(pExporter)
	sExporter, err := stackdriver.NewExporter(stackdriver.Options{ProjectID: config.ProjectID})
	if err != nil {
		logger.Error("Error creating stackdriver exporter  ", zap.Error(err))
	}
	// Export to Stackdriver Monitoring.
	view.RegisterExporter(sExporter)

	collector, err = cltr.NewCollector(logger, config)
	if err != nil {
		logger.Fatal("Can't init Collector", zap.Error(err))
		os.Exit(-1)
	}
	// create a run group.
	g := &group.Group{}

	initHttpHandler(g, logger)
	initMetricsEndpoint(g, logger, pExporter)
	initCancelInterrupt(g)
	// run the group and wait for exit.
	logger.Info("exit", zap.Error(g.Run()))

}

func initHttpHandler(g *group.Group, logger *zap.Logger) {
	requestHandler := func(ctx *fasthttp.RequestCtx) {
		switch string(ctx.Path()) {
		case "/track":
			collector.Collect(ctx)
		default:
			ctx.Error("Unsupported path", fasthttp.StatusNotFound)
		}
	}
	g.Add(func() error {
		logger.Info("HTTP Server", zap.String("transport", "HTTP"), zap.String("addr", httpAddr))
		return fasthttp.ListenAndServe(httpAddr, requestHandler)
	}, func(error) {
		logger.Error("Error start serving")
	})

}
func initMetricsEndpoint(g *group.Group, logger *zap.Logger, exporter *prometheus.Exporter) {
	http.Handle("/metrics", exporter)
	debugListener, err := net.Listen("tcp", metricsAddr)
	if err != nil {
		logger.Info("Error ", zap.String("transport", "debug/HTTP"), zap.String("during", "Listen"), zap.Error(err))
	}
	g.Add(func() error {
		logger.Info("Promhttp", zap.String("transport", "debug/HTTP"), zap.String("addr", metricsAddr))
		return http.Serve(debugListener, http.DefaultServeMux)
	}, func(error) {
		debugListener.Close()
	})
}

func initCancelInterrupt(g *group.Group) {
	cancelInterrupt := make(chan struct{})
	g.Add(func() error {
		c := make(chan os.Signal, 1)
		signal.Notify(c, syscall.SIGINT, syscall.SIGTERM)
		select {
		case sig := <-c:
			collector.Stop()
			return fmt.Errorf("received signal %s", sig)
		case <-cancelInterrupt:
			return nil
		}
	}, func(error) {
		close(cancelInterrupt)
	})
}
