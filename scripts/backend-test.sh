#!/usr/bin/env bash
# Ordinary pnpm backend:test: format, then one verification owner.
# Isolated checkouts skip the separate migrate step because `test` already
# migrates once. Unconfigured primary keeps format + migrate + test_only.

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
checkout_root="$(cd "${script_dir}/.." && pwd)"
cd "${checkout_root}"

# shellcheck source=backend-test-worktree-owner.sh
source "${script_dir}/backend-test-worktree-owner.sh"

pnpm backend:format

if ! backend_worktree_isolation_applies "${checkout_root}"; then
  backend/gradlew -p backend migrateTestDB -Dspring.profiles.active=test || {
    echo 'Migration failed! 💀'
    exit 1
  }
fi
exec pnpm backend:test_only
