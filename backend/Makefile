# To run this make file you will need to give the next arguments. PROJECT_ID, DATASET_NAME, TOPIC_NAME and SUBSCRIPTION_NAME.
# If you are lazy (like me), you can just set them up in the following lines :-)

# PROJECT_ID=<YOUR_PROJECT_ID>
# DATASET_NAME=<YOUR_DATASET_NAME>
# TOPIC_NAME=<YOUR_TOPIC_NAME>
# SUBSCRIPTION_NAME=<YOUR_SUBSCRIPTION_NAME>

TABLE_NAME=banias
SCHEMAS_BUCKET=banias_schemas
TEMP_BUCKET=banias_temp
TEMP_FOLDER=tmp
EVENTS_SUBSCRIPTION_PATH="projects/$(PROJECT_ID)/subscriptions/$(SUBSCRIPTION_NAME)"
ERRORS_TABLE_NAME=error
DATASET=banias

env_setup: _create_tmp_bucket _create_schemas_bucket _create_topic _create_subscription

install:
	mvn install

build: install
	mvn package

run_local: build
	mvn exec:java -Dexec.mainClass=com.doitintl.banias.BaniasPipeline \
	-Dexec.cleanupDaemonThreads=false \
	-Dexec.args=" \
	--project=$(PROJECT_ID) \
	--tempLocation=gs://$(TEMP_BUCKET)/ \
	--gcpTempLocation=gs://$(TEMP_BUCKET)/$(TEMP_FOLDER) \
	--runner=DirectRunner \
	--defaultWorkerLogLevel=DEBUG \
	--eventsSubscriptionPath=$(EVENTS_SUBSCRIPTION_PATH) \
    --errorsTableName=$(ERRORS_TABLE_NAME) \
    --GCSSchemasBucketName=$(SCHEMAS_BUCKET) \
    --dataset=$(DATASET) \
	"

run: build
	mvn exec:java -Dexec.mainClass=com.doitintl.banias.BaniasPipeline \
	-Dexec.cleanupDaemonThreads=false \
	-Dexec.args=" \
	--project=$(PROJECT_ID) \
	--tempLocation=gs://$(TEMP_BUCKET)/ \
	--gcpTempLocation=gs://$(TEMP_BUCKET)/$(TEMP_FOLDER) \
	--runner=DataflowRunner \
	--defaultWorkerLogLevel=DEBUG \
	--eventsSubscriptionPath=$(EVENTS_SUBSCRIPTION_PATH) \
    --errorsTableName=$(ERRORS_TABLE_NAME) \
    --GCSSchemasBucketName=$(SCHEMAS_BUCKET) \
    --dataset=$(DATASET) \
    --numWorkers=3 \
	"


### Helpers:
_create_tmp_bucket:
	gsutil mb gs://$(TEMP_BUCKET)/

_create_schemas_bucket:
	gsutil mb gs://$(SCHEMAS_BUCKET)/

_create_topic:
	gcloud pubsub --project $(PROJECT_ID) topics create $(TOPIC_NAME)

_create_subscription:
	gcloud pubsub --project $(PROJECT_ID) subscriptions create $(SUBSCRIPTION_NAME) --topic=$(TOPIC_NAME)
