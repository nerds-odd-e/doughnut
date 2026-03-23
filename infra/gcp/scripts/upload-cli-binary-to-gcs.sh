#!/usr/bin/env bash
set -euo pipefail

# Uploads the bundled CLI to gs://<bucket>/doughnut-cli-latest/doughnut (prod LB + CDN).
# Destination bucket: GCS_FRONTEND_BUCKET, or GCS_BUCKET if unset.

DEST_BUCKET="${GCS_FRONTEND_BUCKET:-}"
if [[ -z "$DEST_BUCKET" ]]; then
	: "${GCS_BUCKET:?Set GCS_FRONTEND_BUCKET or GCS_BUCKET}"
	DEST_BUCKET="$GCS_BUCKET"
fi

CLI_SOURCE="${CLI_BUNDLE_SOURCE:-cli/dist/doughnut-cli.bundle.mjs}"
if [[ ! -f "$CLI_SOURCE" ]]; then
	echo "error: CLI bundle not found: $CLI_SOURCE" >&2
	exit 1
fi

DEST="gs://${DEST_BUCKET}/doughnut-cli-latest/doughnut"
echo "Uploading CLI from $CLI_SOURCE to $DEST"
gsutil cp -a public-read "$CLI_SOURCE" "$DEST"
