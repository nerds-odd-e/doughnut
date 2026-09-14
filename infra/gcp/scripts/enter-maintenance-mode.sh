#!/usr/bin/env bash
set -euo pipefail

# One-time maintenance entry for the portable-trash migration release
# (SEED-009 story 37, plan slice 7-8 in
# .planning/quick/123-safe-portable-trash-upgrade/PLAN.md).
#
# Points doughnut-app-group at the already-created, portable-trash-compatible
# instance template, stops every instance, waits, and verifies none remain
# running or transitioning before permitting the legacy-trash migration to
# proceed. The backend's HTTP handlers and its in-process @Scheduled
# background jobs (QuestionGenerationBatchMaintenanceJob,
# EmbeddingMaintenanceJob -- see backend/src/main/java/com/odde/donut/services)
# run inside the same JVM per MIG instance; there is no separate worker
# fleet. Stopping the MIG's instances therefore excludes both surfaces of
# schema-dependent writers, not incoming HTTP alone.
#
# The instance-template assignment (below) is set BEFORE the stop, and
# deliberately does not trigger a rolling replace of the currently-running
# instances (a plain `set-instance-template` does not touch instances
# already running -- see docs/gcp/portable-trash-maintenance-release-runbook.md).
# It exists to durably close the gap a stop-instances-only maintenance entry
# leaves open: `instanceTemplate` is a field GCP stores on the MIG resource
# itself, so it survives this script's own process dying. Both instance-group
# autohealing (infra/gcp/scripts/add-mig-autohealing.sh) and this MIG's
# PROACTIVE/replace update policy (infra/gcp/scripts/configure-mig-update-policy.sh)
# always recreate/replace instances from the MIG's CURRENT instance
# template -- documented GCP behavior, not this script's own invention. So
# once this step commits, ANY subsequent instance start for this MIG --
# autohealing after the stop, the update policy's own proactive convergence,
# or an operator's manual retry/recreate -- necessarily boots the compatible
# template, never the old one, even if this script is killed immediately
# after.
#
# This script is a standalone, opt-in operation. It is deliberately NOT
# called from publish-application.sh or deploy-backend-jar-to-gcp-mig.sh --
# wiring it into ordinary publication is a later, separate plan slice.
#
# Env:
#   ZONE (default us-east1-b)
#   MIG_NAME (default doughnut-app-group)
#   MAINTENANCE_INSTANCE_TEMPLATE (required) -- name of the already-created
#     portable-trash-compatible instance template (created the ordinary way,
#     e.g. via create-mig-app-instance-template.sh/update-mig-startup-script.sh's
#     template-creation step). Not defaulted: a missing value must fail loudly
#     rather than silently keep whatever template the MIG already has.
#   MAINTENANCE_STOP_GRACE_SECONDS (default 10) -- wait after issuing the
#     stop before the first quiescence check.
#   MAINTENANCE_QUIESCENCE_TIMEOUT_SECONDS (default 300)
#   MAINTENANCE_QUIESCENCE_POLL_SECONDS (default 5)
#
# Signal: exit 0 only after quiescence is verified -- migration is
# permitted. Any failure, including quiescence timing out, exits non-zero
# and migration must NOT proceed. No new state is written by this script;
# the instance-template assignment is GCP's own MIG state, and the exit code
# is the only signal this slice produces itself.

ZONE="${ZONE:-us-east1-b}"
MIG_NAME="${MIG_NAME:-doughnut-app-group}"
GRACE_SECONDS="${MAINTENANCE_STOP_GRACE_SECONDS:-10}"
TIMEOUT="${MAINTENANCE_QUIESCENCE_TIMEOUT_SECONDS:-300}"
POLL_INTERVAL="${MAINTENANCE_QUIESCENCE_POLL_SECONDS:-5}"
: "${MAINTENANCE_INSTANCE_TEMPLATE:?MAINTENANCE_INSTANCE_TEMPLATE is required}"

active_instance_statuses() {
	gcloud compute instance-groups managed list-instances "$MIG_NAME" \
		--zone="$ZONE" \
		--format='value(instanceStatus)'
}

echo "Setting ${MIG_NAME} instance template to ${MAINTENANCE_INSTANCE_TEMPLATE}..."
gcloud compute instance-groups managed set-instance-template "$MIG_NAME" \
	--template="$MAINTENANCE_INSTANCE_TEMPLATE" \
	--zone="$ZONE"

echo "Stopping all instances in ${MIG_NAME} (zone ${ZONE})..."
gcloud compute instance-groups managed stop-instances "$MIG_NAME" \
	--zone="$ZONE" \
	--all-instances

echo "Waiting ${GRACE_SECONDS}s before verifying quiescence..."
sleep "$GRACE_SECONDS"

elapsed=0
while true; do
	statuses="$(active_instance_statuses)"
	if ! grep -qE '^(RUNNING|STOPPING|PROVISIONING|STAGING|REPAIRING)$' <<<"$statuses"; then
		echo "Verified quiescent: no active writers remain in ${MIG_NAME}. Migration is permitted."
		exit 0
	fi
	if ((elapsed >= TIMEOUT)); then
		echo "Quiescence verification failed: ${MIG_NAME} still reports active instances after ${TIMEOUT}s:" >&2
		echo "$statuses" >&2
		echo "Migration is NOT permitted." >&2
		exit 1
	fi
	sleep "$POLL_INTERVAL"
	elapsed=$((elapsed + POLL_INTERVAL))
done
