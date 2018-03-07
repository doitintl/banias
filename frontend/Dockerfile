# build stage
FROM golang:alpine AS build-env
RUN apk update && apk upgrade && \
    apk add --no-cache  git godep

ADD . /go/src/github.com/doitintl/banias/frontend
RUN go get -u github.com/golang/dep/cmd/dep
RUN cd /go/src/github.com/doitintl/banias/frontend && dep ensure && go build -o banias-frontend .


# final stage
FROM alpine
MAINTAINER Aviv Laufer <aviv.laufer@gmail.com>
WORKDIR /app
COPY --from=build-env /go/src/github.com/doitintl/banias/frontend/banias-frontend /app/
ENTRYPOINT ./banias-frontend




