package config

import (
	"math"
	"time"

	"cloud.google.com/go/pubsub"
	"github.com/spf13/viper"
)

type Config struct {
	// Project ID daa
	ProjectID string
	// Pub/Sub topic name
	Topic string
	// Debug flag for log prints - default level is Info.
	Debug bool
	// HTTP port.
	Port int
	//  Prometheus port.
	MetricsPort int
	// Maximum size of a single batch.
	PubsubMaxBatch int
	// Number of pools that listen for events from users
	PubSubAggrigators int
	// Max time to pass in seconds before publishing.
	PubsubMaxPublishDelay time.Duration
	//The default maximum amount of goroutines for publishing.
	MaxPubSubGoroutinesAmount int
	//The default maximum idle duration of a goroutine.
	MaxPubSubGoroutineIdleDuration time.Duration
}

func setConfigDefaults() {
	//TODO change debug to false
	viper.SetDefault("Topic", "banias")
	viper.SetDefault("Debug", false)
	viper.SetDefault("Port", 8081)
	viper.SetDefault("MetricsPort", 8080)
	viper.SetDefault("PubsubMaxBatch", 1000)
	viper.SetDefault("PubSubAggrigators", 30)
	viper.SetDefault("PubsubMaxPublishDelay", 5)
	viper.SetDefault("MaxPubSubGoroutinesAmount", 256*1024)
	viper.SetDefault("MaxPubSubGoroutineIdleDuration", 10)

}

func NewConfig() (*Config, error) {
	viper.SetEnvPrefix("banias")
	viper.AutomaticEnv()
	setConfigDefaults()
	c := Config{
		ProjectID:                      viper.GetString("projectid"),
		Topic:                          viper.GetString("topic"),
		Debug:                          viper.GetBool("debug"),
		Port:                           viper.GetInt("port"),
		MetricsPort:                    viper.GetInt("metricsport"),
		PubsubMaxBatch:                 int(math.Min(float64(viper.GetInt("pubsubmaxbatch")), float64(pubsub.MaxPublishRequestCount))),
		PubSubAggrigators:              viper.GetInt("pubsubaggrigators"),
		PubsubMaxPublishDelay:          time.Duration(viper.GetInt("pubsubmaxpublishdelay")) * time.Second,
		MaxPubSubGoroutinesAmount:      viper.GetInt("maxpubsubgoroutinesamount"),
		MaxPubSubGoroutineIdleDuration: time.Duration(viper.GetInt("maxpubsubgoroutineidleduration")),
	}
	return &c, nil
}
