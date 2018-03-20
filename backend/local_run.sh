#!/bin/bash
PROJECT_ID=aviv-playground
INPUT_TOPIC="projects/aviv-playground/topics/banias"
PIPELINE_FOLDER=TextIOToBigQuery
OUTPUT_TABLE_SPEC="aviv-playground:mydataset.mytable"
mvn compile exec:java \
    -Dexec.mainClass=com.doitintl.banias.BaniasPipeline \
    -Dexec.args="\
        --runner=DirectRunner \
        --inputTopic=${INPUT_TOPIC}\
        --outputTableSpec=${OUTPUT_TABLE_SPEC}"
