#!/bin/bash
PROJECT_ID=aviv-playground
PIPELINE_NAME=text-io-to-bigquery-local
PIPELINE_FOLDER=TextIOToBigQuery

mvn compile exec:java \
    -Dexec.mainClass=com.doitintl.PubSubToBigQuery \
    -Dexec.cleanupDaemonThreads=false \
    -Dexec.args=" \
        --project=${PROJECT_ID} \
        --runner=DirectRunner \
        --stagingLocation=gs://${PROJECT_ID}/dataflow/pipelines/${PIPELINE_FOLDER}/staging \
        --tempLocation=gs://${PROJECT_ID}/dataflow/pipelines/${PIPELINE_FOLDER}/temp \
        --JSONPath=gs://the-coolest-temp-dir/template/template.json \
        --javascriptTextTransformGcsPath=gs://the-coolest-temp-dir/udf/sample_UDF.js \
        --input=gs://the-coolest-temp-dir/sample_data/sample_data.txt \
        --outputTable=data-analytics-pocs:teleport.textiotobq_localschema_helper_test_v3 \
        --javascriptTextTransformFunctionName=transform"