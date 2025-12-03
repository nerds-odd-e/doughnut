#!/bin/bash
# Grant logging permissions to service accounts used by GCP instances
# This fixes the "Permission 'logging.logEntries.create' denied" warnings

PROJECT_ID="carbon-syntax-298809"
PROJECT_NUMBER="220715781008"
SERVICE_ACCOUNT="doughnut-gcp-svc-acct@${PROJECT_ID}.iam.gserviceaccount.com"
DEFAULT_COMPUTE_SA="${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"

echo "Granting logging.logWriter role to service accounts..."

# Grant logging permission to custom service account
echo "Granting roles/logging.logWriter to ${SERVICE_ACCOUNT}..."
gcloud projects add-iam-policy-binding ${PROJECT_ID} \
  --member="serviceAccount:${SERVICE_ACCOUNT}" \
  --role="roles/logging.logWriter" \
  --condition=None

# Grant logging permission to default compute service account
echo "Granting roles/logging.logWriter to ${DEFAULT_COMPUTE_SA}..."
gcloud projects add-iam-policy-binding ${PROJECT_ID} \
  --member="serviceAccount:${DEFAULT_COMPUTE_SA}" \
  --role="roles/logging.logWriter" \
  --condition=None

echo "Done! The logging permission warnings should stop appearing after instances restart."
echo "Note: Existing instances will need to be restarted or replaced for the changes to take effect."

