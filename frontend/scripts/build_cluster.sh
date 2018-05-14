#!/bin/bash
gcloud  container clusters create $CLUSTERNAME --cluster-version \
 "1.9.7-gke.0" --no-enable-legacy-authorization --image-type "UBUNTU" \
   --machine-type "n1-standard-4" \
   --scopes "https://www.googleapis.com/auth/compute","https://www.googleapis.com/auth/devstorage.read_only","https://www.googleapis.com/auth/logging.write","https://www.googleapis.com/auth/monitoring","https://www.googleapis.com/auth/pubsub","https://www.googleapis.com/auth/servicecontrol","https://www.googleapis.com/auth/service.management.readonly","https://www.googleapis.com/auth/trace.append" \
   --network "default" --enable-autoscaling --enable-cloud-logging --enable-cloud-monitoring --subnetwork "default"