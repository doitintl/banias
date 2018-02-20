package service

import (
	"context"
	"fmt"
	"time"

	"github.com/doitintl/banias/collector/pkg/types"
	"github.com/go-kit/kit/log"
	"github.com/go-kit/kit/metrics"
)

// Middleware describes a service middleware.
type Middleware func(CollectorService) CollectorService

type loggingMiddleware struct {
	logger log.Logger
	next   CollectorService
}

// LoggingMiddleware takes a logger as a dependency
// and returns a CollectorService Middleware.
func LoggingMiddleware(logger log.Logger) Middleware {
	return func(next CollectorService) CollectorService {
		return &loggingMiddleware{logger, next}
	}

}

func (l loggingMiddleware) Track(ctx context.Context, SenderID string, Events []types.Event) (Errors []types.Error, error error) {
	defer func() {
		l.logger.Log("method", "Track", "SenderID", SenderID, "Events", Events, "Errors", Errors, "error", error)
	}()
	return l.next.Track(ctx, SenderID, Events)
}

type instrumentingMiddleware struct {
	requestCount metrics.Counter
	eventsCount  metrics.Counter
	next         CollectorService
}

// InstrumentingMiddleware returns a CollectorService Middleware.
func InstrumentingMiddleware(requestCount metrics.Counter, eventsCount metrics.Counter) Middleware {
	return func(next CollectorService) CollectorService {
		return &instrumentingMiddleware{requestCount, eventsCount, next}
	}

}
func (i instrumentingMiddleware) Track(ctx context.Context, SenderID string, Events []types.Event) (Errors []types.Error, error error) {
	defer func(begin time.Time) {
		lvs := []string{"method", "track", "error", fmt.Sprint(error != nil)}
		i.requestCount.With(lvs...).Add(1)
		i.eventsCount.With(lvs...).Add(float64(len(Events)))

	}(time.Now())
	return i.next.Track(ctx, SenderID, Events)
}
