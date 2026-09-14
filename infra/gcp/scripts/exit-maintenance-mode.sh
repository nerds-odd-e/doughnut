#!/usr/bin/env bash
set -euo pipefail

# Resumes doughnut-app-group instances after enter-maintenance-mode.sh has
# verified quiescence, so the newly-assigned instance template actually
# boots and serves (SEED-009 story 37, plan slice 9,
# .planning/quick/123-safe-portable-trash-upgrade/PLAN.md). Mirrors
# enter-maintenance-mode.sh's stop-instances call with the natural
# start-instances counterpart, same MIG/zone argument shape. Story 39
# removes this script once production has succeeded (see the plan's
# temporary removal inventory).
#
# Instances restart from whichever instance template is currently assigned
# to the MIG -- enter-maintenance-mode.sh already durably set that to the
# compatible template (GCP-owned MIG state) before any instance was
# stopped, so this script does not need to, and does not, touch the
# template itself.
#
# Env:
#   ZONE (default us-east1-b)
#   MIG_NAME (default doughnut-app-group)

ZONE="${ZONE:-us-east1-b}"
MIG_NAME="${MIG_NAME:-doughnut-app-group}"

echo "Starting all instances in ${MIG_NAME} (zone ${ZONE})..."
gcloud compute instance-groups managed start-instances "$MIG_NAME" \
	--zone="$ZONE" \
	--all-instances

echo "Start requested for all instances in ${MIG_NAME}."
