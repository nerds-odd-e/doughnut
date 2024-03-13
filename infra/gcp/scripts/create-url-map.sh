#!/bin/bash
gcloud compute url-maps create doughnut-app-service-map \
	--default-service doughnut-app-service
