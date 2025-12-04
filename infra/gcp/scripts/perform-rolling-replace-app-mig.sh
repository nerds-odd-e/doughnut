#!/bin/bash
# Perform rolling replacement of MIG instances
# Note: This requires the MIG update policy to allow REPLACE actions

MIG_NAME="doughnut-app-group"
ZONE="us-east1-b"

# Update MIG update policy to allow REPLACE actions if needed
echo "Ensuring MIG update policy allows REPLACE actions..."
gcloud compute instance-groups managed update $MIG_NAME \
  --update-policy-type=PROACTIVE \
  --update-policy-most-disruptive-action=replace \
  --zone=$ZONE

# Perform rolling replacement
echo "Starting rolling replacement..."
gcloud compute instance-groups managed rolling-action replace $MIG_NAME \
  --max-surge 0 \
  --max-unavailable 1 \
  --zone=$ZONE
