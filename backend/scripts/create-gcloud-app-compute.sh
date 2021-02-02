#/bin/sh

gcloud compute instances create doughnut-app-instance \
	--image-family debian-10 \
	--image-project debian-cloud \
	--service-account 220715781008-compute@developer.gserviceaccount.com \
	--service-account doughnut-gcp-svc-acct@carbon-syntax-298809.iam.gserviceaccount.com \
	--scopes https://www.googleapis.com/auth/cloud-platform \
	--scopes "userinfo-email,cloud-platform" \
	--address 35.237.244.180 \
	--private-network-ip 10.142.0.25 \
	--machine-type g1-small \
	--metadata-from-file startup-script=app-instance-startup.sh \
	--metadata BUCKET=dough-01 \
	--zone us-east1-b \
	--tags app-server
