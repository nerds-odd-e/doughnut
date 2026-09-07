#!/usr/bin/env bash
#
# Usage: pnpm backend:test:worktree
#        pnpm backend:test:worktree --tests '<pattern>'
# Reads this checkout's .worktree.local.json and selects doughnut_<id>_test.
# Migrates that database and runs backend unit tests against it.
#
# Resolve paths from this launcher's checkout, not the shared Git directory.

set -euo pipefail

usage() {
  echo 'Usage: pnpm backend:test:worktree' >&2
  echo "       pnpm backend:test:worktree --tests '<pattern>'" >&2
  echo "Reads this checkout's .worktree.local.json and selects doughnut_<id>_test." >&2
  echo 'Migrates that database and runs backend unit tests against it.' >&2
}

test_pattern=""
if [[ $# -eq 2 && "$1" == --tests && -n "$2" ]]; then
  test_pattern="$2"
elif [[ $# -ne 0 ]]; then
  usage
  exit 1
fi

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
checkout_root="$(cd "${script_dir}/.." && pwd)"
# shellcheck source=backend-test-worktree-owner.sh
source "${script_dir}/backend-test-worktree-owner.sh"
backend_test_worktree_prepare "${checkout_root}"

cd "${checkout_root}"
gradle_cmd=(
  "${checkout_root}/backend/gradlew"
  -p backend
  -PworktreeTestRun
  -Dspring.profiles.active=test
  --rerun-tasks
  --no-build-cache
  --no-daemon
  migrateTestDB
  test
)
if [[ -n "${test_pattern}" ]]; then
  gradle_cmd+=(--tests "${test_pattern}")
fi
export DONUT_WORKTREE_HANDOFF=1
exec "${gradle_cmd[@]}"
