#!/bin/bash
gcloud compute url-maps create doughnut-service-map \
	--default-service doughnut-service
