#!/usr/bin/env bash
set -euo pipefail

# Grants the GitHub Actions deploy SA what gcloud compute url-maps import needs:
# - compute.urlMaps.update on the URL map (project roles/compute.loadBalancerAdmin)
# - compute.backendServices.use / backendBuckets.use on referenced backends
#   (resource roles/compute.loadBalancerServiceUser)
#
# Symptoms without this: 403 compute.backendServices.use or compute.urlMaps.update.

PROJECT_ID="${GCP_PROJECT_ID:-carbon-syntax-298809}"
SA="${CI_DEPLOY_GCP_SA:-doughnut-ci-gcp-deploy-svc-acc@${PROJECT_ID}.iam.gserviceaccount.com}"
MEMBER="serviceAccount:${SA}"

gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member="${MEMBER}" \
  --role="roles/compute.loadBalancerAdmin" \
  --condition=None

gcloud compute backend-services add-iam-policy-binding doughnut-app-service \
  --project="${PROJECT_ID}" \
  --global \
  --member="${MEMBER}" \
  --role="roles/compute.loadBalancerServiceUser"

gcloud compute backend-buckets add-iam-policy-binding doughnut-frontend-backend-bucket \
  --project="${PROJECT_ID}" \
  --member="${MEMBER}" \
  --role="roles/compute.loadBalancerServiceUser"

echo "Granted loadBalancerAdmin (project) and loadBalancerServiceUser (backend service + backend bucket) to ${SA}"
