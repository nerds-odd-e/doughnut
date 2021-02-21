#!/bin/bash
gcloud compute backend-services create doughnut-service \
	--http-health-checks doughnut-health-check \
	--global
