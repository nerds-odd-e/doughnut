#/bin/sh
gcloud compute backend-services add-backend doughnut-service \
	--instance-group doughnut-group \
	--global \
	--instance-group-zone us-east1-b
