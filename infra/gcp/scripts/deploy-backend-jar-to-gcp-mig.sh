#!/usr/bin/env bash
set -euo pipefail

# Compares the built jar and startup script SHA-256 values to
# gs://${GCS_BUCKET}/deploy/last-successful-deploy.json. If both match, skips
# GCS upload and the MIG rollout (record only advances after success).
# Env: GCS_BUCKET, ARTIFACT, VERSION; optional DEPLOY_JAR_PATH; GITHUB_SHA (set by CI).
# Optional FORCE_FULL_DEPLOY=1: run upload + rollout even when hashes match the record.
#
# The portable-trash schema upgrade release closes the MIG before any
# schema-dependent work. Until verified upgrade and reopen exist, publication
# deliberately fails after the MIG is closed and leaves the release record
# publishing.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

: "${GCS_BUCKET:?GCS_BUCKET is required}"
: "${ARTIFACT:?ARTIFACT is required}"
: "${VERSION:?VERSION is required}"
: "${GITHUB_SHA:?GITHUB_SHA is required}"

JAR_NAME="${ARTIFACT}-${VERSION}.jar"
if [[ -n "${DEPLOY_JAR_PATH:-}" ]]; then
  JAR_PATH="$DEPLOY_JAR_PATH"
elif [[ -f "./${JAR_NAME}" ]]; then
  JAR_PATH="./${JAR_NAME}"
elif [[ -f "backend/build/libs/${JAR_NAME}" ]]; then
  JAR_PATH="backend/build/libs/${JAR_NAME}"
else
  echo "error: jar not found (tried ./${JAR_NAME} and backend/build/libs/${JAR_NAME})" >&2
  exit 1
fi

REPO_ROOT="${REPO_ROOT:-$(cd "$SCRIPT_DIR/../../.." && pwd)}"
export REPO_ROOT
bash "$SCRIPT_DIR/apply-doughnut-app-service-url-map.sh"

RECORD_URI="gs://${GCS_BUCKET}/deploy/last-successful-deploy.json"
JAR_DEST="gs://${GCS_BUCKET}/backend_app_jar/${JAR_NAME}"
STARTUP_SCRIPT_PATH="${STARTUP_SCRIPT_PATH:-$SCRIPT_DIR/mig-zulu25-openai-app-instance-startup.sh}"
export STARTUP_SCRIPT_PATH

new_hash=$(sha256sum "$JAR_PATH" | awk '{print $1}')
new_startup_script_hash=$(sha256sum "$STARTUP_SCRIPT_PATH" | awk '{print $1}')

recorded_hash=""
recorded_startup_script_hash=""
if recorded_json=$(gsutil cat "$RECORD_URI" 2>/dev/null); then
  recorded_hash=$(printf '%s' "$recorded_json" | jq -r '.sha256 // empty' 2>/dev/null || true)
  recorded_startup_script_hash=$(
    printf '%s' "$recorded_json" | jq -r '.startup_script_sha256 // empty' 2>/dev/null || true
  )
fi

if [[ -n "$recorded_hash" &&
  "$recorded_hash" == "$new_hash" &&
  -n "$recorded_startup_script_hash" &&
  "$recorded_startup_script_hash" == "$new_startup_script_hash" ]]; then
  if [[ "${FORCE_FULL_DEPLOY:-}" == "1" ]]; then
    echo "Force full deploy: jar and startup script SHA-256 values match record; continuing with upload and rollout."
  else
    echo "Deploy skipped: jar and startup script SHA-256 values match last successful deploy record."
    exit 0
  fi
fi

echo "Deploying: jar SHA-256 $new_hash (record had ${recorded_hash:-<none>}); startup script SHA-256 $new_startup_script_hash (record had ${recorded_startup_script_hash:-<none>})."
gsutil cp "$JAR_PATH" "$JAR_DEST"

bash "$SCRIPT_DIR/enter-maintenance-mode.sh"

echo "Release stopped safely with the MIG closed; verified upgrade and reopen are not implemented yet." >&2
exit 1
