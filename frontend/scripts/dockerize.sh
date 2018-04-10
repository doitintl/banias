#!/bin/bash
docker build -t banias-frontend .
docker tag  banias-frontend gcr.io/$PROJECT_ID/banias-frontend
gcloud docker -- push gcr.io/$PROJECT_ID/banias-frontend