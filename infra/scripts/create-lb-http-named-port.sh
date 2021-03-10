#!/bin/bash
gcloud compute instance-groups managed set-named-ports doughnut-group \
	--named-ports http:8081 \
	--zone us-east1-b
