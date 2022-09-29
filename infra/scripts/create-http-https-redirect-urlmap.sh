#!/bin/bash
gcloud compute url-maps import doughnut-app-web-app-http \
   --source doughnut-app-http-redirect-https-web-map.yaml \
   --global
