#!/bin/bash
gcloud compute forwarding-rules create doughnut-app-https-content-rule \
    --address doughnut-app-lb-ext-ip \
    --target-https-proxy doughnut-app-ssl-service-proxy \
    --ports 443 \
    --global

gcloud compute forwarding-rules create doughnut-app-http-content-rule \
   --address doughnut-app-lb-ext-ip \
   --target-http-proxy doughnut-app-service-proxy \
   --ports=80 \
   --global
