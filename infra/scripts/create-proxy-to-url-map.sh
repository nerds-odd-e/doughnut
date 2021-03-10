#!/bin/bash
gcloud compute target-http-proxies create doughnut-service-proxy \
	--url-map doughnut-service-map
