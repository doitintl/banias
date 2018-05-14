
## Installation

* Setup a GKE cluster - `scripts/build_cluster.sh`
* If you want to use Prometheus for monitoring, we recommend `https://github.com/camilb/prometheus-kubernetes`  
* Build the images
	* Install dependencies `https://github.com/golang/dep`
	* Run `dep ensure`
	* Build an deploy the image `scripts/dockerize.sh`
* Create a service account with permission for Google Pub/Sub and Stackdiver.
* Download the key file in JSON format
* Create secret - `kubectl create secret generic pubsub-key --from-file=key.json=filename`
* Change the configuration in `deployfrontend-configmap.yaml`
* Deploy - `kubctl apply -f deploy/.`


### Configuration
  
  * **Project ID** - BANIAS_PROJECTID: "your-project-id"
  
  * **Debug flag for log prints** - default level is Info. - BANIAS_DEBUG: "false"
  
  * **Pub/Sub topic name** - BANIAS_TOPIC: "banias"
  
  * **HTTP port** - BANIAS_PORT: "8081"
  
  * **Prometheus port** - BANIAS_METRICSPORT: "8080"
 
  * **Maximum size of a single batch** - BANIAS_PUBSUBMAXBATCH: "1000"
 
  * **Number of pools that listen for events from users** - BANIAS_PUBSUBAGGRIGATORS: "30"
 
  * **Max time to pass in seconds before publishing.** - BANIAS_PUBSUBMAXPUBLISHDELAY: "5"
 
  * **The default maximum amount of goroutines for publishing** - BANIAS_MAXPUBSUBGOROUTINESAMOUNT: "262144"
 
  * **The default maximum idle duration of a goroutine** - BANIAS_MAXPUBSUBGOROUTINEIDLEDURATION: "10"
