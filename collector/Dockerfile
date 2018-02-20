    FROM golang

RUN mkdir -p /go/src/github.com/doitintl/banias

ADD . /go/src/github.com/doitintl/banias

RUN go get  -t -v ./...
RUN go get  github.com/canthefason/go-watcher
RUN go install github.com/canthefason/go-watcher/cmd/watcher

ENTRYPOINT  watcher -run github.com/doitintl/banias/collector/cmd  -watch github.com/doitintl/banias/collector
