#/bin/sh

gcloud compute instances create doughnut-app-instance \
	--service-account 220715781008-compute@developer.gserviceaccount.com \
	--scopes https://www.googleapis.com/auth/cloud-platform \
	--image-family debian-10 \
	--image-project debian-cloud \
	--machine-type g1-small \
	--scopes "userinfo-email,cloud-platform" \
	--metadata-from-file startup-script=app-instance-startup.sh \
	--metadata BUCKET=dough-01 \
	--zone us-east1-b \
	--tags app-server
