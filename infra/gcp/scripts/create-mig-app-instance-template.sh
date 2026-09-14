#!/bin/bash

set -euo pipefail

SCRIPTPATH="$(
  cd "$(dirname "$0")" >/dev/null 2>&1
  pwd -P
)"
DATABASE_PRIVATE_IP=$(bash "$SCRIPTPATH/read-production-database-private-ip.sh")

gcloud compute instance-templates create doughnut-app-debian12-zulu25-openai-mig-template \
  --image doughnut-debian12-zulu25-mysql84-base-saltstack \
  --service-account 220715781008-compute@developer.gserviceaccount.com \
  --service-account doughnut-gcp-svc-acct@carbon-syntax-298809.iam.gserviceaccount.com \
  --scopes https://www.googleapis.com/auth/cloud-platform \
  --scopes "userinfo-email,cloud-platform" \
  --machine-type e2-medium \
  --metadata-from-file startup-script=${SCRIPTPATH}/mig-zulu25-openai-app-instance-startup.sh \
  --metadata BUCKET=dough-01,DATABASE_PRIVATE_IP="$DATABASE_PRIVATE_IP" \
  --tags mig-app-srv
