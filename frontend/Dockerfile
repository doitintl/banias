FROM golang:1.12-alpine AS build_base
LABEL maintainer="Aviv Laufer <aviv.laufer@gmail.com>"
RUN apk update && apk upgrade && \
    apk add --no-cache git build-base ca-certificates

WORKDIR /go/src/github.com/doitintl/banias/frontend
ENV GO111MODULE=on
COPY go.mod .
COPY go.sum .
RUN go mod download


FROM build_base AS builder
COPY . .

RUN cd /go/src/github.com/doitintl/banias/frontend &&  GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build  -ldflags "-w -s" -o /banias-frontend -tags netgo -installsuffix netgo . 

FROM alpine
RUN apk add --no-cache ca-certificates

COPY --from=builder //banias-frontend /bin//banias-frontend

ENTRYPOINT ["/bin//banias-frontend"]





