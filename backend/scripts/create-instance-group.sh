#/bin/sh
gcloud compute instance-groups managed create doughnut-group \
	--base-instance-name doughnut-group \
	--size 1 \
	--template doughnut-template \
	--zone us-east1-b
