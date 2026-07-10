#!/usr/bin/env bash
# Run pnpm bundle:all unless E2E dist cache already restored the outputs.
set -euo pipefail

if [[ "${E2E_DIST_CACHE_HIT:-}" == "true" ]]; then
  echo "E2E dist cache hit — skipping pnpm bundle:all"
  exit 0
fi

echo "E2E dist cache miss — running pnpm bundle:all"
exec pnpm bundle:all
