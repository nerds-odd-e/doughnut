#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

bash "$SCRIPT_DIR/upload-frontend-static-to-gcs.sh"
bash "$SCRIPT_DIR/upload-cli-binary-to-gcs.sh"
if git log -1 --format=%B "$GITHUB_SHA" | grep -qE 'force-deployment[[:space:]]*:[[:space:]]*true'; then
	export FORCE_FULL_DEPLOY=1
fi
bash "$SCRIPT_DIR/deploy-backend-jar-to-gcp-mig.sh"
