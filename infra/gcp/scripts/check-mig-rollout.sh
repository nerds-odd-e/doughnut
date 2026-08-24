#!/usr/bin/env bash
set -euo pipefail

# Wait until doughnut-app-group instances are running the target instance template.
# Env: MIG_ROLLOUT_TIMEOUT_SECONDS (default 900).

ZONE="${ZONE:-us-east1-b}"
MIG_NAME="${MIG_NAME:-doughnut-app-group}"
TIMEOUT="${MIG_ROLLOUT_TIMEOUT_SECONDS:-900}"

echo "Waiting up to ${TIMEOUT}s for MIG ${MIG_NAME} to reach the new instance template version..."

gcloud compute instance-groups managed wait-until "$MIG_NAME" \
	--version-target-reached \
	--zone="$ZONE" \
	--timeout="$TIMEOUT"

echo "MIG ${MIG_NAME} reached the new instance template version."
