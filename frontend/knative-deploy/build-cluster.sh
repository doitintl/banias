#!/bin/bash

#https://github.com/knative/docs/blob/master/install/Knative-with-GKE.md

#Create a Kubernetes cluster on GKE
gcloud container clusters create $CLUSTER_NAME \
  --zone=$CLUSTER_ZONE \
  --image-type "UBUNTU" \
  --cluster-version=latest \
  --machine-type=n1-standard-4 \
  --enable-autoscaling --min-nodes=1 --max-nodes=10 \
  --enable-autorepair \
  --enable-cloud-logging --enable-cloud-monitoring \
  --scopes=service-control,service-management,compute-rw,storage-ro,cloud-platform,logging-write,monitoring-write,pubsub,datastore \
  --num-nodes=3

#Grant cluster-admin permissions to the current user:

kubectl create clusterrolebinding cluster-admin-binding \
--clusterrole=cluster-admin \
--user=$(gcloud config get-value core/account)

#Install Istio
kubectl apply --filename https://github.com/knative/serving/releases/download/v0.3.0/istio-crds.yaml && \
kubectl apply --filename https://github.com/knative/serving/releases/download/v0.3.0/istio.yaml

#Label the default namespace with istio-injection=enabled

kubectl label namespace default istio-injection=enabled

#knative
kubectl apply --filename https://github.com/knative/serving/releases/download/v0.3.0/serving.yaml \
--filename https://github.com/knative/build/releases/download/v0.3.0/release.yaml \
--filename https://github.com/knative/eventing/releases/download/v0.3.0/release.yaml \
--filename https://github.com/knative/eventing-sources/releases/download/v0.3.0/release.yaml \
--filename https://github.com/knative/serving/releases/download/v0.3.0/monitoring.yaml

#Create a Service Account
gcloud iam service-accounts create knative-build --display-name "Knative Build"

#Allow it to push to GCR
gcloud projects add-iam-policy-binding $PROJECT_ID --member serviceAccount:knative-build@$PROJECT_ID.iam.gserviceaccount.com --role roles/storage.admin

#Create a JSON key
gcloud iam service-accounts keys create knative-key.json --iam-account knative-build@$PROJECT_ID.iam.gserviceaccount.com

#Create a secret for the JSON Key
kubectl create secret generic knative-build-auth --type="kubernetes.io/basic-auth" --from-literal=username="_json_key" --from-file=password=knative-key.json

#add an annotation to the secret.
kubectl annotate secret knative-build-auth build.knative.dev/docker-0=https://gcr.io

#Create a Service Account
kubectl apply -f build/service-account.yaml
