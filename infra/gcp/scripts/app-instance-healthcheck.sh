#!/usr/bin/env bash
set -euo pipefail

# Probe production /api/healthcheck after a MIG rollout.
# Env: HEALTHCHECK_URL (default https://doughnut.odd-e.com/api/healthcheck);
#      HEALTHCHECK_RETRY_COUNT (default 30); HEALTHCHECK_RETRY_SLEEP_SECONDS (default 10).

HEALTHCHECK_URL="${HEALTHCHECK_URL:-https://doughnut.odd-e.com/api/healthcheck}"
RETRY="${HEALTHCHECK_RETRY_COUNT:-30}"
SLEEP_SECONDS="${HEALTHCHECK_RETRY_SLEEP_SECONDS:-10}"

last_status=""
last_body=""
while [[ ${RETRY} -gt 0 ]]; do
	sleep "$SLEEP_SECONDS"
	echo "RETRY (${RETRY})..."
	body_file=$(mktemp)
	err_file=$(mktemp)
	last_status=$(curl -sk -o "$body_file" -w '%{http_code}' "$HEALTHCHECK_URL" 2>"$err_file") || true
	last_body=$(cat "$body_file")
	if [[ -z "$last_body" && -s "$err_file" ]]; then
		last_body=$(cat "$err_file")
	fi
	rm -f "$body_file" "$err_file"
	echo "status=${last_status} body=${last_body}"
	if [[ "$last_body" == *"OK"* ]]; then
		echo "doughnut-app responded ${last_body}!"
		exit 0
	fi
	RETRY=$((RETRY - 1))
done

echo "doughnut-app NOT RESPONDING! last status=${last_status} body=${last_body}"
exit 1
