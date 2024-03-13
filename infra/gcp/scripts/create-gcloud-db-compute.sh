#!/bin/bash
SCRIPTPATH="$( cd "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

gcloud compute instances create doughnut-db-instance \
	--image-family debian-10 \
	--image-project debian-cloud \
	--service-account 220715781008-compute@developer.gserviceaccount.com \
	--service-account doughnut-gcp-svc-acct@carbon-syntax-298809.iam.gserviceaccount.com \
	--scopes https://www.googleapis.com/auth/cloud-platform \
	--scopes "userinfo-email,cloud-platform" \
	--no-address \
	--private-network-ip 10.142.0.18 \
	--machine-type g1-small \
	--metadata-from-file startup-script=${SCRIPTPATH}/db-instance-startup.sh \
	--zone us-east1-b \
	--tags db-server
