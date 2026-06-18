#!/usr/bin/env bash
set -euo pipefail

# Compares the built jar and startup script SHA-256 values to
# gs://${GCS_BUCKET}/deploy/last-successful-deploy.json. If both match, skips
# GCS upload and MIG rolling replace (record only advances after success).
# Env: GCS_BUCKET, ARTIFACT, VERSION; optional DEPLOY_JAR_PATH; GITHUB_SHA (set by CI).
# Optional FORCE_FULL_DEPLOY=1: run upload + rolling replace even when hashes match the record.

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
STARTUP_SCRIPT_PATH="$SCRIPT_DIR/mig-zulu25-openai-app-instance-startup.sh"

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
    echo "Force full deploy: jar and startup script SHA-256 values match record; continuing with upload and rolling replace."
  else
    echo "Deploy skipped: jar and startup script SHA-256 values match last successful deploy record."
    exit 0
  fi
fi

echo "Deploying: jar SHA-256 $new_hash (record had ${recorded_hash:-<none>}); startup script SHA-256 $new_startup_script_hash (record had ${recorded_startup_script_hash:-<none>})."
gsutil cp "$JAR_PATH" "$JAR_DEST"

bash "$SCRIPT_DIR/update-mig-startup-script.sh"

jq -n \
  --arg sha "$new_hash" \
  --arg startup_script_sha "$new_startup_script_hash" \
  --arg git "$GITHUB_SHA" \
  --arg at "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  '{sha256: $sha, startup_script_sha256: $startup_script_sha, git_sha: $git, recorded_at: $at}' \
  | gsutil cp - "$RECORD_URI"

echo "Recorded last successful deploy at $RECORD_URI"
