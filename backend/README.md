 ## General
BigQuery is Google's serverless, highly scalable, low cost enterprise data warehouse designed to make all your data analysts productive. Because there is no infrastructure to manage, you can focus on analyzing data to find meaningful insights using familiar SQL and you don't need a database administrator.  

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
* Dataset location: DATASET_LOCATION=<YOUR_DATASET_LOCATION> Can be asia-northeast1, EU or US. More info [here](https://cloud.google.com/bigquery/docs/dataset-locations)

All bucket names should contain only the name. No 'gs://' prefix.


## Schemas
BigQuery allows you to specify a table's schema when you load data into a table, and when you create an empty table.  
When you specify a table schema, you must supply each column's name and data type. You may optionally supply a column's description and mode.  
You can find more information about schema and schema creation [here](https://cloud.google.com/bigquery/docs/schemas).  
Banias utilize the standard schema format used by Google's BigQuery. You can find sample schemas under the test folder. 

### Schemas Guidelines
* Once the pipeline is started, it will look for the schemas to work on in the SCHEMAS_BUCKET.
* The schema files are in a json format.
* The file name (without the '.json') defines the schema's key. It will be used to match the event with the schema.
* The events will be mapped according to it's event_name + event_version --> schema key ('event_name'_'event_version' = schema key) 
* If you want to add a new schema, just:
	* Put the new json file in the bucket
	* Restart your pipeline (it is recommended here to use a template)
* Schema cannot be modified.

### Error table
In the error table you will find all the elements that had issues (not having a schema is not an issue...).  
The error table contains the the event type, content and the error that got this event into the error table.  

## Running
### Setup environment
```
make env_setup PROJECT_ID=my-project TOPIC_NAME=topic-name SUBSCRIPTION_NAME=subscription-name SCHEMAS_BUCKET=bucket-with-my-schemas TEMP_BUCKET=mytmpbucket TEMPLATE_BUCKET=my-templates-bucket TEMPLATE_FILE=template-name DATASET_LOCATION=US
```

### Running locally
```
make run_local PROJECT_ID=my-project DATASET_NAME=important-dataset TOPIC_NAME=topic-name SUBSCRIPTION_NAME=subscription-name SCHEMAS_BUCKET=bucket-with-my-schemas TEMP_BUCKET=mytmpbucket
```
> Note: Sending event with no schema to a pipeline running with DirectRunner will cause the runner to exit.

### Running on Google's Dataflow
```
make run PROJECT_ID=my-project DATASET_NAME=important-dataset TOPIC_NAME=topic-name SUBSCRIPTION_NAME=subscription-name SCHEMAS_BUCKET=bucket-with-my-schemas TEMP_BUCKET=mytmpbucket
```

## Dataflow Pipeline Templates
From [Google Cloud Dataflow Templates](https://cloud.google.com/dataflow/docs/templates/overview)
> Cloud Dataflow templates allow you to stage your pipelines on Google Cloud Storage and execute them from a variety of environments. You can use one of the Google-provided templates or create your own.
>
> * Templates provide you with additional benefits compared to traditional Cloud Dataflow deployment, such as:
> * Pipeline execution does not require you to recompile your code every time.
> * You can execute your pipelines without the development environment and associated dependencies that are common with traditional deployment. This is useful for scheduling recurring batch jobs.
> * Runtime parameters allow you to customize the execution.
> * Non-technical users can execute templates with the Google Cloud Platform Console, gcloud command-line tool, or the REST API.

### Creating a template for Google DataFlow
```
make run PROJECT_ID=my-project DATASET_NAME=important-dataset TOPIC_NAME=topic-name SUBSCRIPTION_NAME=subscription-name SCHEMAS_BUCKET=bucket-with-my-schemas TEMP_BUCKET=mytmpbucket TEMPLATE_BUCKET=my-templates-bucket TEMPLATE_FILE=template-name
```
