## build stage
FROM golang:alpine AS build-env
RUN apk update && apk upgrade && \
    apk add --no-cache  git
ADD . /go/src/github.com/doitintl/banias/frontend
RUN pwd
RUN ls -l
RUN cd /go/src/github.com/doitintl/banias/frontend/ && pwd  && ls -l &&GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -v -ldflags "-w -s" -o banias-frontend -tags netgo -installsuffix netgo .

## final stage
FROM alpine
MAINTAINER Aviv Laufer <aviv.laufer@gmail.com>
RUN apk update && apk upgrade && \
    apk add --no-cache  ca-certificates
WORKDIR /app
COPY --from=build-env go/src/github.com/doitintl/banias/frontend/banias-frontend /app/
ENTRYPOINT ./banias-frontend
