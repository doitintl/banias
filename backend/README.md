## General
Banias aim to provide an easy way to ingest events into Google's BigQuery with the ability to have new schemas as events evolve with minimum code changes.
To achieve this we are using Apache Beam on top of Google's DataFlow as our backend engine.
The code here is a baseline for any transformation graph you would like to create in the future. You can always extend the BaseMap or the MapEvents to get some funky stuff into the graph :-)

Our deployment is driven by a Makefile so you don't need to type too much to get things running...

## Prerequisits
* Java(TM) SE Runtime Environment 1.8
* Apache Maven 3.5

## Configuration
* Project ID: PROJECT_ID=<YOUR_PROJECT_ID>
* BigQuery Dataset name: DATASET_NAME=<YOUR_DATASET_NAME>
* PubSub topic name: TOPIC_NAME=<YOUR_TOPIC_NAME>
* PubSub subscription name: SUBSCRIPTION_NAME=<YOUR_SUBSCRIPTION_NAME>
* Schema's bucket: SCHEMAS_BUCKET=<YOUR_BANIAS_SCHEMAS_BUCKET>
* Temp bucket: TEMP_BUCKET=<YOUR_TEMP_BUCKET>
* Template bucket: TEMPLATE_BUCKET=<YOUR_TEMPLATE_BUCKET>
* Template file name: TEMPLATE_FILE=<YOUR_TEMPLATE_FILE_NAME>

All bucket names should contain only the name. No 'gs://' prefix.

### Schemas Guidelines
* Once the pipeline is started, it will look for the schemas to work on in the SCHEMAS_BUCKET.
* The schema files are in a json format. Sample schemas can be found under the test folder
* The file name (without the '.json') define the schema's key. It will be used to match the event with the schema.
* The events will be mapped according to it's event_name + event_version --> schema key ('event_name'_'event_version' = schema key) 
* If you want to add a new schema, just:
	* Put the new json file in the bucket
	* Restart your pipeline (It is recommended here to use a template)
* Schema cannot be modified.

## Running
### Setup environment
```
make env_setup PROJECT_ID=my-project TOPIC_NAME=topic-name SUBSCRIPTION_NAME=subscription-name SCHEMAS_BUCKET=bucket-with-my-schemas TEMP_BUCKET=mytmpbucket TEMPLATE_BUCKET=my-templates-bucket TEMPLATE_FILE=template-name
```

### Running locally
```
make run_local PROJECT_ID=my-project DATASET_NAME=important-dataset TOPIC_NAME=topic-name SUBSCRIPTION_NAME=subscription-name SCHEMAS_BUCKET=bucket-with-my-schemas TEMP_BUCKET=mytmpbucket
```

### Running on Google's Dataflow
```
make run PROJECT_ID=my-project DATASET_NAME=important-dataset TOPIC_NAME=topic-name SUBSCRIPTION_NAME=subscription-name SCHEMAS_BUCKET=bucket-with-my-schemas TEMP_BUCKET=mytmpbucket
```

### Creating a template for Google DataFlow
```
make run PROJECT_ID=my-project DATASET_NAME=important-dataset TOPIC_NAME=topic-name SUBSCRIPTION_NAME=subscription-name SCHEMAS_BUCKET=bucket-with-my-schemas TEMP_BUCKET=mytmpbucket TEMPLATE_BUCKET=my-templates-bucket TEMPLATE_FILE=template-name
```
