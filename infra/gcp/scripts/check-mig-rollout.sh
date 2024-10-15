#!/bin/bash

export PROJECT_ID=$(gcloud config get-value project)
export ZONE=us-east1-b
export MIG_NAME=doughnut-app-group

# Check MIG status
max_attempts=30
attempt=1
while [ $attempt -le $max_attempts ]; do
  echo "Attempt $attempt: Checking MIG status..."

  status=$(gcloud compute instance-groups managed describe $MIG_NAME \
    --project=$PROJECT_ID \
    --zone=$ZONE \
    --format='value(status.isStable)' 2>&1)

  if [ $? -eq 0 ]; then
    status_case_insensitive=$(echo "$status" | tr '[:upper:]' '[:lower:]')
    if [ "$status_case_insensitive" = "true" ]; then
      echo "MIG rollout is STABLE."
      exit 0
    else
      echo "MIG is NOT YET Stable. Current status: $status"
    fi
  else
    echo "Error querying MIG status: $status"
    echo "Retrying..."
  fi

  sleep 30
  attempt=$((attempt+1))
done

echo "MIG rollout did not stabilize within the expected time"
echo "Final MIG details:"
gcloud compute instance-groups managed describe $MIG_NAME \
  --project=$PROJECT_ID \
  --zone=$REGION
exit 1
