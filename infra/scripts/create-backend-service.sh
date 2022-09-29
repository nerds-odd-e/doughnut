#!/bin/bash
gcloud compute backend-services create doughnut-app-service \
	--http-health-checks doughnut-app-health-check \
	--global
