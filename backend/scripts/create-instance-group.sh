#!/bin/bash
gcloud compute instance-groups managed create doughnut-group \
	--base-instance-name doughnut-group \
	--size 2 \
	--template doughnut-template \
	--zone us-east1-b
