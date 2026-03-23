#!/usr/bin/env bash
set -euo pipefail

# Uploads the bundled CLI to gs://${GCS_BUCKET}/doughnut-cli-latest/doughnut (prod LB + CDN).
# Env: GCS_BUCKET; optional CLI_BUNDLE_SOURCE (default: cli/dist/doughnut-cli.bundle.mjs).

: "${GCS_BUCKET:?GCS_BUCKET is required}"

CLI_SOURCE="${CLI_BUNDLE_SOURCE:-cli/dist/doughnut-cli.bundle.mjs}"
if [[ ! -f "$CLI_SOURCE" ]]; then
	echo "error: CLI bundle not found: $CLI_SOURCE" >&2
	exit 1
fi

DEST="gs://${GCS_BUCKET}/doughnut-cli-latest/doughnut"
echo "Uploading CLI from $CLI_SOURCE to $DEST"
gsutil cp -a public-read "$CLI_SOURCE" "$DEST"
