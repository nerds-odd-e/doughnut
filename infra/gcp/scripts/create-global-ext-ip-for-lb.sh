#!/bin/bash
gcloud compute addresses create doughnut-app-lb-ext-ip \
    --ip-version=IPV4 \
    --global

gcloud compute addresses describe doughnut-app-lb-ext-ip \
    --format="get(address)" \
    --global
