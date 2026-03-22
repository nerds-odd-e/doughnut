#!/usr/bin/env bash
set -euo pipefail

# Uploads the built SPA tree (Vite output after pnpm frontend:build / bundle:all)
# to gs://${GCS_BUCKET}/frontend/${GITHUB_SHA}/ for CDN / load-balancer wiring later.
# Env: GCS_BUCKET, GITHUB_SHA; optional FRONTEND_STATIC_DIR (default: frontend/dist).

: "${GCS_BUCKET:?GCS_BUCKET is required}"
: "${GITHUB_SHA:?GITHUB_SHA is required}"

STATIC_DIR="${FRONTEND_STATIC_DIR:-frontend/dist}"
if [[ ! -d "$STATIC_DIR" ]]; then
	echo "error: frontend static directory not found: $STATIC_DIR" >&2
	exit 1
fi

DEST="gs://${GCS_BUCKET}/frontend/${GITHUB_SHA}/"
echo "Uploading frontend static from $STATIC_DIR to $DEST"
gsutil -m rsync -r "$STATIC_DIR" "$DEST"
