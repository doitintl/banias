#!/bin/bash
if kubectl get configmap config-istio -n knative-serving &> /dev/null; then
    INGRESSGATEWAY=istio-ingressgateway
fi

kubectl get svc $INGRESSGATEWAY --namespace istio-system
export IP_ADDRESS=$(kubectl get svc $INGRESSGATEWAY --namespace istio-system --output 'jsonpath={.status.loadBalancer.ingress[0].ip}')
echo SERVICE IP Address $IP_ADDRESS

echo Getting host URL
kubectl get ksvc banias-frontend  --output=custom-columns=NAME:.metadata.name,DOMAIN:.status.domain
