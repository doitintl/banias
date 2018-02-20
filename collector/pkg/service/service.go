package service

import (
	"context"


	"github.com/doitintl/banias/collector/pkg/types"
)

// CollectorService describes the service.
type CollectorService interface {
	Track(ctx context.Context, SenderID string, Events []types.Event) (Errors []types.Error, error error)
}

type basicCollectorService struct {
	bqEvents chan<- types.Event

}

func (b *basicCollectorService) Track(ctx context.Context, SenderID string, Events []types.Event) (Errors []types.Error, error error) {
	// TODO implement the business logic of Track
	for _ ,event :=range Events {
		b.bqEvents <-event
	}
	return Errors, error
}

// NewBasicCollectorService returns a naive, stateless implementation of CollectorService.
func NewBasicCollectorService() CollectorService {
	bqEvents := make(chan types.Event, 100)
	return &basicCollectorService{bqEvents:bqEvents}
}

// New returns a CollectorService with all of the expected middleware wired in.
func New( middleware []Middleware) CollectorService {
	var svc CollectorService = NewBasicCollectorService()
	for _, m := range middleware {
		svc = m(svc)
	}
	return svc
}

