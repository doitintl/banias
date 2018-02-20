package endpoint

import (
	context "context"

	service "github.com/doitintl/banias/collector/pkg/service"
	"github.com/doitintl/banias/collector/pkg/types"
	endpoint "github.com/go-kit/kit/endpoint"

)

// TrackRequest collects the request parameters for the Track method.
type TrackRequest struct {
	SenderID string        `json:"sender_id"`
	Events   []types.Event `json:"events"`
}

// TrackResponse collects the response parameters for the Track method.
type TrackResponse struct {
	Errors []types.Error `json:"errors"`
	Error  error         `json:"error"`
}

// MakeTrackEndpoint returns an endpoint that invokes Track on the service.
func MakeTrackEndpoint(s service.CollectorService) endpoint.Endpoint {
	return func(ctx context.Context, request interface{}) (interface{}, error) {
		req := request.(TrackRequest)
		Errors, error := s.Track(ctx, req.SenderID, req.Events)
		return TrackResponse{
			Error:  error,
			Errors: Errors,
		}, nil
	}
}

// Failed implements Failer.
func (r TrackResponse) Failed() error {
	return r.Error
}

// Failer is an interface that should be implemented by response types.
// Response encoders can check if responses are Failer, and if so they've
// failed, and if so encode them using a separate write path based on the error.
type Failure interface {
	Failed() error
}

// Track implements Service. Primarily useful in a client.
func (e Endpoints) Track(ctx context.Context, SenderID string, Events []types.Event) (Errors []types.Error, error error) {
	request := TrackRequest{
		Events:   Events,
		SenderID: SenderID,
	}
	response, err := e.TrackEndpoint(ctx, request)
	if err != nil {
		return
	}
	return response.(TrackResponse).Errors, response.(TrackResponse).Error
}

