#!/bin/bash
gcloud compute backend-services add-backend doughnut-app-service \
	--instance-group doughnut-app-group \
	--global \
	--instance-group-zone us-east1-b
