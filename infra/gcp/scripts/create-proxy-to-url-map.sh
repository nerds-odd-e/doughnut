#!/bin/bash
gcloud compute target-https-proxies create doughnut-app-ssl-service-proxy \
	--url-map doughnut-app-service-map

gcloud compute target-http-proxies create doughnut-app-service-proxy \
   --url-map doughnut-app-web-map-http \
   --global

