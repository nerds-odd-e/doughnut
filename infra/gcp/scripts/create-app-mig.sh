#!/bin/bash
gcloud compute instance-groups managed create doughnut-app-group \
	--base-instance-name doughnut-app-group \
	--size 2 \
	--template doughnut-app-debian12-zulu22-openai-mig-template \
	--zone us-east1-b
