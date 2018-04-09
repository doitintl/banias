PROJECT_ID=yoram-playground
DATASET_NAME=yoram_playground
TABLE_NAME=banias
TEMP_BUCKET=banias_temp
TEMP_FOLDER=tmp

EVENTS_SUBSCRIPTION_PATH="projects/$(PROJECT_ID)/subscriptions/BaniasPipeline"
EVENTS_BQ_TABLE_PREFIX=banias
ERRORS_TABLE_NAME=error
DATASET=banias


create_tmp_bucket:
	gsutil mb gs://$(TEMP_BUCKET)/

delete_tmp_bucket:
	gsutil rm -r gs://$(TEMP_BUCKET)/

recreate_tmp_bucket: delete_tmp_bucket create_tmp_bucket

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
    --dataset=$(DATASET) \
	"
