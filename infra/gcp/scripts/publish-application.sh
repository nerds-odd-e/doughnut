#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONTROL_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

: "${RELEASE_SOURCE_ROOT:?RELEASE_SOURCE_ROOT is required}"
: "${RELEASE_REF:?RELEASE_REF is required}"
: "${RELEASE_REF_OID:?RELEASE_REF_OID is required}"
: "${GITHUB_SHA:?GITHUB_SHA is required}"
export REPO_ROOT="$RELEASE_SOURCE_ROOT"
export STARTUP_SCRIPT_PATH="$RELEASE_SOURCE_ROOT/infra/gcp/scripts/mig-zulu25-openai-app-instance-startup.sh"
if [[ "$(git -C "$RELEASE_SOURCE_ROOT" rev-parse HEAD)" != "$GITHUB_SHA" ]]; then
  echo "Release source checkout does not match selected SHA: $GITHUB_SHA" >&2
  exit 1
fi
node "$CONTROL_ROOT/scripts/ci/application-release-payload.mjs"
test -s "$STARTUP_SCRIPT_PATH"
PREPARED_URL_MAP="$(mktemp)"
trap 'rm -f "$PREPARED_URL_MAP"' EXIT
bash "$SCRIPT_DIR/apply-doughnut-app-service-url-map.sh" --prepare "$PREPARED_URL_MAP"
export PREPARED_URL_MAP
node "$CONTROL_ROOT/scripts/ci/application-release.mjs" --verify-ref

bash "$SCRIPT_DIR/upload-frontend-static-to-gcs.sh"
bash "$SCRIPT_DIR/upload-cli-binary-to-gcs.sh"
if git -C "$RELEASE_SOURCE_ROOT" log -1 --format=%B "$GITHUB_SHA" | grep -qE 'force-deployment[[:space:]]*:[[:space:]]*true'; then
	export FORCE_FULL_DEPLOY=1
fi
bash "$SCRIPT_DIR/deploy-backend-jar-to-gcp-mig.sh"
