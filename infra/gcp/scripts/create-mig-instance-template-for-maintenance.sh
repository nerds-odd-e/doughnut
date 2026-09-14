#!/usr/bin/env bash
set -euo pipefail

# Creates a new MIG instance template for the maintenance-protected release
# path (SEED-009 story 37, plan slice 9,
# .planning/quick/123-safe-portable-trash-upgrade/PLAN.md). Duplicates only
# the template-creation gcloud call from update-mig-startup-script.sh -- it
# deliberately does NOT update the MIG's update policy, assign the template
# to the MIG, or trigger a rolling replace. enter-maintenance-mode.sh
# performs the set-instance-template step itself, after stopping instances,
# so the ordinary rolling-replace route is never reached for this one-time
# release. Story 39 removes this script once production has succeeded (see
# the plan's temporary removal inventory).
#
# Prints ONLY the new template name on stdout, so the caller can capture it
# directly, e.g.:
#   NEW_TEMPLATE_NAME="$(bash create-mig-instance-template-for-maintenance.sh)"
# All other output goes to stderr.
#
# Env:
#   STARTUP_SCRIPT_PATH (default mig-zulu25-openai-app-instance-startup.sh
#     next to this script)
#   PROJECT_ID (default carbon-syntax-298809)

SCRIPTPATH="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

PROJECT_ID="${PROJECT_ID:-carbon-syntax-298809}"
TEMPLATE_NAME="doughnut-app-debian12-zulu25-openai-mig-template"
STARTUP_SCRIPT="${STARTUP_SCRIPT_PATH:-${SCRIPTPATH}/mig-zulu25-openai-app-instance-startup.sh}"

if [ ! -f "$STARTUP_SCRIPT" ]; then
	echo "Error: Startup script not found at $STARTUP_SCRIPT" >&2
	exit 1
fi

TIMESTAMP=$(date +%s)
NEW_TEMPLATE_NAME="${TEMPLATE_NAME}-${TIMESTAMP}"
echo "Creating new template: $NEW_TEMPLATE_NAME" >&2

gcloud compute instance-templates create "$NEW_TEMPLATE_NAME" \
	--image doughnut-debian12-zulu25-mysql84-base-saltstack \
	--service-account 220715781008-compute@developer.gserviceaccount.com \
	--service-account "doughnut-gcp-svc-acct@${PROJECT_ID}.iam.gserviceaccount.com" \
	--scopes https://www.googleapis.com/auth/cloud-platform \
	--scopes "userinfo-email,cloud-platform" \
	--machine-type e2-medium \
	--metadata-from-file "startup-script=${STARTUP_SCRIPT}" \
	--metadata BUCKET=dough-01 \
	--tags mig-app-srv >&2

echo "New template created: $NEW_TEMPLATE_NAME" >&2

echo "$NEW_TEMPLATE_NAME"
