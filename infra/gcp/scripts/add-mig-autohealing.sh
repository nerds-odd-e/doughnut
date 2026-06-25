#!/bin/bash
# Add autohealing to the doughnut MIG so dead Java processes trigger automatic instance recreate.
# Without this, a dead JVM leaves the VM "running" and isStable:true, causing silent 502s.
#
# The health check polls /api/healthcheck every 30s; after 3 consecutive failures
# (~90s detection) the MIG recreates the instance. initial-delay=300s gives Spring Boot
# time to fully start before the first check counts against it.
#
# Run this once after MIG creation. Re-run if autoHealingPolicies is missing.

MIG_NAME="doughnut-app-group"
ZONE="us-east1-b"
HEALTH_CHECK_NAME="doughnut-app-autohealing-hc"

echo "Creating autohealing health check (if not already present)..."
gcloud compute health-checks describe "$HEALTH_CHECK_NAME" &>/dev/null || \
  gcloud compute health-checks create http "$HEALTH_CHECK_NAME" \
    --port=8081 \
    --request-path=/api/healthcheck \
    --check-interval=30 \
    --timeout=10 \
    --healthy-threshold=1 \
    --unhealthy-threshold=3 \
    --description="MIG autohealing health check for doughnut app"

echo "Attaching autohealing policy to MIG $MIG_NAME..."
gcloud compute instance-groups managed update "$MIG_NAME" \
  --zone="$ZONE" \
  --health-check="$HEALTH_CHECK_NAME" \
  --initial-delay=300

echo "Autohealing configured. Verify with:"
echo "  gcloud compute instance-groups managed describe $MIG_NAME --zone=$ZONE --format='yaml(autoHealingPolicies)'"
