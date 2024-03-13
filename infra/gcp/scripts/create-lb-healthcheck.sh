#!/bin/bash
gcloud compute http-health-checks create doughnut-app-health-check \
	--request-path /api/healthcheck \
	--port 8081
