#!/usr/bin/env bash
set -euo pipefail

# Grants the GitHub Actions deploy SA roles/compute.loadBalancerAdmin on the project.
# That role includes compute.urlMaps.update, compute.backendServices.use, and
# compute.backendBuckets.use — everything gcloud compute url-maps import needs.
#
# Symptom without this: 403 compute.backendServices.use or compute.urlMaps.update.

PROJECT_ID="${GCP_PROJECT_ID:-carbon-syntax-298809}"
SA="${CI_DEPLOY_GCP_SA:-doughnut-ci-gcp-deploy-svc-acc@${PROJECT_ID}.iam.gserviceaccount.com}"
MEMBER="serviceAccount:${SA}"

gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member="${MEMBER}" \
  --role="roles/compute.loadBalancerAdmin" \
  --condition=None

echo "Granted roles/compute.loadBalancerAdmin on ${PROJECT_ID} to ${SA}"
