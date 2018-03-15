
docker build -t banias-frontend .

docker tag  banias-frontend gcr.io/aviv-playground/banias-frontend:test

gcloud docker -- push gcr.io/aviv-playground/banias-frontend



gcloud  container clusters create "banias" --cluster-version "1.9.3-gke.0" --no-enable-legacy-authorization --image-type "UBUNTU"  --machine-type "n1-standard-2" --scopes "https://www.googleapis.com/auth/compute","https://www.googleapis.com/auth/devstorage.read_only","https://www.googleapis.com/auth/logging.write","https://www.googleapis.com/auth/monitoring","https://www.googleapis.com/auth/pubsub","https://www.googleapis.com/auth/servicecontrol","https://www.googleapis.com/auth/service.management.readonly","https://www.googleapis.com/auth/trace.append" --network "default" --enable-cloud-logging --enable-cloud-monitoring --subnetwork "default" 
export primary_account="aviv@doit-intl.com"
alias a-kubectl='kubectl --token="$(gcloud auth print-access-token --account=$primary_account)"'

a-kubectl create secret generic pubsub-key --from-file=key.json=/Users/aviv.laufer/Downloads/aviv-playground-372f883fd220.json
a-kubectl create configmap sysctl.conf --from-file=/Users/aviv.laufer/Downloads/sysctl.txt
a-kubectl exec -it banias-frontend-f8fd5688-7z86p -- /bin/sh

https://cloud.google.com/kubernetes-engine/docs/tutorials/authenticating-to-cloud-platform


go build;./frontend& ;sleep 1; go tool pprof -seconds 60 frontend http://localhost:6060/debug/pprof/heap

https://github.com/camilb/prometheus-kubernetes