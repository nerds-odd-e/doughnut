#!/usr/bin/env bash
set -euo pipefail

# Uploads the built SPA tree (Vite output after pnpm frontend:build / bundle:all)
# to gs://<bucket>/frontend/${GITHUB_SHA}/ for the LB backend bucket.
# Env: GITHUB_SHA; optional FRONTEND_STATIC_DIR (default: frontend/dist).
# Destination bucket: GCS_FRONTEND_BUCKET, or GCS_BUCKET if unset.

DEST_BUCKET="${GCS_FRONTEND_BUCKET:-}"
if [[ -z "$DEST_BUCKET" ]]; then
	: "${GCS_BUCKET:?Set GCS_FRONTEND_BUCKET or GCS_BUCKET}"
	DEST_BUCKET="$GCS_BUCKET"
fi
: "${GITHUB_SHA:?GITHUB_SHA is required}"

STATIC_DIR="${FRONTEND_STATIC_DIR:-frontend/dist}"
if [[ ! -d "$STATIC_DIR" ]]; then
	echo "error: frontend static directory not found: $STATIC_DIR" >&2
	exit 1
fi

DEST="gs://${DEST_BUCKET}/frontend/${GITHUB_SHA}/"
echo "Uploading frontend static from $STATIC_DIR to $DEST"
gsutil -m rsync -r "$STATIC_DIR" "$DEST"
