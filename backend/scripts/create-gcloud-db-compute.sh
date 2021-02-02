#/bin/sh

gcloud compute instances create doughnut-db-instance \
	--service-account 220715781008-compute@developer.gserviceaccount.com \
	--scopes https://www.googleapis.com/auth/cloud-platform \
	--image-family debian-10 \
	--image-project debian-cloud \
	--machine-type g1-small \
	--scopes "userinfo-email,cloud-platform" \
	--metadata-from-file startup-script=db-instance-startup.sh \
	--zone us-east1-b \
	--tags db-server
