#!/usr/bin/env bash
set -euo pipefail

# Renders prod URL map YAML from infra/gcp/path-routing/doughnut-routing.json for GITHUB_SHA,
# runs path-routing validation, then imports the global URL map.
# Env: GITHUB_SHA (40-char hex). Optional REPO_ROOT (default: repo root from this script).

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="${REPO_ROOT:-$(cd "$SCRIPT_DIR/../../.." && pwd)}"

: "${GITHUB_SHA:?GITHUB_SHA is required}"

RENDERED="$(mktemp)"
trap 'rm -f "$RENDERED"' EXIT

node "$REPO_ROOT/infra/gcp/url-maps/renderDoughnutAppServiceUrlMap.mjs" \
  --sha "$GITHUB_SHA" \
  --write "$RENDERED"

node "$REPO_ROOT/scripts/validate-url-map-static-vs-backend-hints.mjs" \
  --url-map "$RENDERED"

gcloud compute url-maps import doughnut-app-service-map \
  --source="$RENDERED" \
  --global \
  --quiet

# Drop CDN entries that still point at the previous SHA's shell / error-policy
# substitutes so deep links do not keep serving a stale or empty body.
echo "Invalidating Cloud CDN cache for doughnut-app-service-map (/*)"
gcloud compute url-maps invalidate-cdn-cache doughnut-app-service-map \
  --path "/*" \
  --global \
  --async

echo "URL map doughnut-app-service-map updated for frontend prefix frontend/${GITHUB_SHA}/"
