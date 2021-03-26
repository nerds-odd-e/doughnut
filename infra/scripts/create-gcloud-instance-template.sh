#!/bin/bash
SCRIPTPATH="$(
	cd "$(dirname "$0")" > /dev/null 2>&1
	pwd -P
)"

gcloud compute instance-templates create doughnut-template \
	--image doughnut-debian10-mysql80-base-saltstack \
	--service-account 220715781008-compute@developer.gserviceaccount.com \
	--service-account doughnut-gcp-svc-acct@carbon-syntax-298809.iam.gserviceaccount.com \
	--scopes https://www.googleapis.com/auth/cloud-platform \
	--scopes "userinfo-email,cloud-platform" \
	--machine-type e2-medium \
	--metadata-from-file startup-script=${SCRIPTPATH}/mig-app-instance-startup.sh \
	--metadata BUCKET=dough-01 \
	--zone us-east1-b \
	--tags app-server
