#!/bin/bash
TIMEOUT=${1:-10}

echo "Updating doughnut-app-service with timeout value: ${TIMEOUT}"
gcloud compute backend-services update doughnut-app-service \
	--timeout=$TIMEOUT \
	--global
