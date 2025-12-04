#!/bin/bash
# Configure MIG update policy to allow REPLACE actions
# This is required when changing instance templates

MIG_NAME="doughnut-app-group"
ZONE="us-east1-b"

echo "Configuring MIG update policy for $MIG_NAME..."

# Set update policy to allow REPLACE actions
gcloud compute instance-groups managed update $MIG_NAME \
  --update-policy-type=PROACTIVE \
  --update-policy-most-disruptive-action=replace \
  --zone=$ZONE

echo "MIG update policy configured successfully!"
echo ""
echo "The MIG can now perform REPLACE actions when instance templates change."
echo "This allows rolling replacements to work properly."

