#!/bin/bash
# Update MIG instance template with new startup script and perform rolling replacement

set -e

SCRIPTPATH="$(
  cd "$(dirname "$0")" >/dev/null 2>&1
  pwd -P
)"

PROJECT_ID="carbon-syntax-298809"
ZONE="us-east1-b"
MIG_NAME="doughnut-app-group"
TEMPLATE_NAME="doughnut-app-debian12-zulu25-openai-mig-template"
STARTUP_SCRIPT="${SCRIPTPATH}/mig-zulu25-openai-app-instance-startup.sh"

echo "Updating MIG instance template with new startup script..."

# Check if startup script exists
if [ ! -f "$STARTUP_SCRIPT" ]; then
  echo "Error: Startup script not found at $STARTUP_SCRIPT"
  exit 1
fi

# Get current template to preserve settings
echo "Fetching current template configuration..."
CURRENT_TEMPLATE=$(gcloud compute instance-groups managed describe $MIG_NAME \
  --zone=$ZONE \
  --format='value(instanceTemplate)' 2>/dev/null || echo "")

if [ -z "$CURRENT_TEMPLATE" ]; then
  echo "Error: Could not find current template for MIG $MIG_NAME"
  exit 1
fi

echo "Current template: $CURRENT_TEMPLATE"

# Create new template with updated startup script
# GCP template names must be lowercase, start with a letter, and contain only [a-z0-9-]
# Max length is 63 characters, so use shorter timestamp format
TIMESTAMP=$(date +%s)
NEW_TEMPLATE_NAME="${TEMPLATE_NAME}-${TIMESTAMP}"
echo "Creating new template: $NEW_TEMPLATE_NAME"

gcloud compute instance-templates create $NEW_TEMPLATE_NAME \
  --image doughnut-debian12-zulu25-mysql84-base-saltstack \
  --service-account 220715781008-compute@developer.gserviceaccount.com \
  --service-account doughnut-gcp-svc-acct@${PROJECT_ID}.iam.gserviceaccount.com \
  --scopes https://www.googleapis.com/auth/cloud-platform \
  --scopes "userinfo-email,cloud-platform" \
  --machine-type e2-medium \
  --metadata-from-file startup-script=${STARTUP_SCRIPT} \
  --metadata BUCKET=dough-01 \
  --tags mig-app-srv

echo "New template created: $NEW_TEMPLATE_NAME"

# Update MIG to use new template
echo "Updating MIG to use new template..."
gcloud compute instance-groups managed set-instance-template $MIG_NAME \
  --template=$NEW_TEMPLATE_NAME \
  --zone=$ZONE

echo "MIG updated. Starting rolling replacement..."

# Perform rolling replacement
gcloud compute instance-groups managed rolling-action replace $MIG_NAME \
  --max-surge 0 \
  --max-unavailable 1 \
  --zone=$ZONE

echo ""
echo "Rolling replacement started!"
echo "Monitor progress with:"
echo "  ./infra/gcp/scripts/check-mig-rollout.sh"
echo ""
echo "Or check health with:"
echo "  ./infra/gcp/scripts/check-mig-doughnut-app-service-health.sh"

