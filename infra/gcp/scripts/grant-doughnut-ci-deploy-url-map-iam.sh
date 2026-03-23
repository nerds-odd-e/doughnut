#!/usr/bin/env bash
set -euo pipefail

# Grants the GitHub Actions deploy SA permission to reference the MIG backend
# service and the static backend bucket inside gcloud compute url-maps import.
# Symptom without this: HTTP 403 compute.backendServices.use on import.

PROJECT_ID="${GCP_PROJECT_ID:-carbon-syntax-298809}"
SA="${CI_DEPLOY_GCP_SA:-doughnut-ci-gcp-deploy-svc-acc@${PROJECT_ID}.iam.gserviceaccount.com}"
MEMBER="serviceAccount:${SA}"

gcloud compute backend-services add-iam-policy-binding doughnut-app-service \
  --project="${PROJECT_ID}" \
  --global \
  --member="${MEMBER}" \
  --role="roles/compute.loadBalancerServiceUser"

gcloud compute backend-buckets add-iam-policy-binding doughnut-frontend-backend-bucket \
  --project="${PROJECT_ID}" \
  --member="${MEMBER}" \
  --role="roles/compute.loadBalancerServiceUser"

echo "Granted roles/compute.loadBalancerServiceUser on doughnut-app-service and doughnut-frontend-backend-bucket to ${SA}"
