#/bin/sh
gcloud compute http-health-checks create doughnut-health-check \
	--request-path /api/healthcheck \
	--port 8081
