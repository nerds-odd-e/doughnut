#!/usr/bin/env bash
set -euo pipefail

# Probe production /api/healthcheck after a MIG rollout.
# Env: HEALTHCHECK_URL (default https://doughnut.odd-e.com/api/healthcheck);
#      HEALTHCHECK_RETRY_COUNT (default 18); HEALTHCHECK_RETRY_SLEEP_SECONDS (default 10).

HEALTHCHECK_URL="${HEALTHCHECK_URL:-https://doughnut.odd-e.com/api/healthcheck}"
RETRY="${HEALTHCHECK_RETRY_COUNT:-18}"
SLEEP_SECONDS="${HEALTHCHECK_RETRY_SLEEP_SECONDS:-10}"

HEALTHCHECK_STATUS=""
while [[ ${RETRY} -gt 0 ]] && [[ "${HEALTHCHECK_STATUS}" != *"OK"* ]]; do
  sleep "$SLEEP_SECONDS"
  echo "RETRY (${RETRY})..."
  HEALTHCHECK_STATUS=$(curl -sk "$HEALTHCHECK_URL" 2>&1) || true
  if [[ "${HEALTHCHECK_STATUS}" == *"OK"* ]]; then
    echo "doughnut-app responded ${HEALTHCHECK_STATUS}!"
    exit 0
  fi
  RETRY=$((RETRY - 1))
done

echo "doughnut-app NOT RESPONDING!"
exit 1
