#/bin/sh
gcloud compute instance-templates create doughnut-template \
	--image-family ubuntu-2004-lts \
	--image-project ubuntu-os-cloud \
	--machine-type g1-small \
	--scopes "userinfo-email,cloud-platform" \
	--metadata-from-file startup-script=app-instance-startup.sh \
	--metadata BUCKET=dough-01 \
	--tags http-server
