#!/usr/bin/env bash
set -euo pipefail

# One-time maintenance entry for the portable-trash migration release.
# Switches the MIG from PROACTIVE to OPPORTUNISTIC, retains its original
# target size in the release log, resizes it to zero, and verifies that no
# managed instance remains before permitting any template or migration change.
# HTTP handlers and in-process scheduled jobs run in the same instance JVMs,
# so a zero-sized MIG excludes both writer surfaces.
#
# Env: ZONE, MIG_NAME, MAINTENANCE_STOP_GRACE_SECONDS,
# MAINTENANCE_QUIESCENCE_TIMEOUT_SECONDS, MAINTENANCE_QUIESCENCE_POLL_SECONDS.

ZONE="${ZONE:-us-east1-b}"
MIG_NAME="${MIG_NAME:-doughnut-app-group}"
GRACE_SECONDS="${MAINTENANCE_STOP_GRACE_SECONDS:-10}"
TIMEOUT="${MAINTENANCE_QUIESCENCE_TIMEOUT_SECONDS:-300}"
POLL_INTERVAL="${MAINTENANCE_QUIESCENCE_POLL_SECONDS:-5}"

remaining_instances() {
	gcloud compute instance-groups managed list-instances "$MIG_NAME" \
		--zone="$ZONE" \
		--format='value(instance)'
}

original_target_size="$(gcloud compute instance-groups managed describe "$MIG_NAME" \
	--zone="$ZONE" \
	--format='value(targetSize)')"
echo "Retained original MIG target size for recovery: ${original_target_size}"

gcloud compute instance-groups managed update "$MIG_NAME" \
	--update-policy-type=OPPORTUNISTIC \
	--zone="$ZONE"

gcloud compute instance-groups managed resize "$MIG_NAME" \
	--size=0 \
	--zone="$ZONE"

echo "Waiting ${GRACE_SECONDS}s before verifying quiescence..."
sleep "$GRACE_SECONDS"

elapsed=0
while true; do
	instances="$(remaining_instances)"
	if [[ -z "$instances" ]]; then
		echo "Verified closed: no managed instance remains in ${MIG_NAME}."
		exit 0
	fi
	if ((elapsed >= TIMEOUT)); then
		echo "Closure verification failed: ${MIG_NAME} still reports instances after ${TIMEOUT}s:" >&2
		echo "$instances" >&2
		echo "Template assignment and migration are NOT permitted." >&2
		exit 1
	fi
	sleep "$POLL_INTERVAL"
	elapsed=$((elapsed + POLL_INTERVAL))
done
