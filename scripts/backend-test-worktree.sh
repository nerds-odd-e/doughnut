#!/usr/bin/env bash
#
# Usage: pnpm backend:test:worktree
# Reads this checkout's .worktree.local.json and selects doughnut_<id>_test.
# Migrates that database and runs the complete backend unit test suite.
#
# Resolve paths from this launcher's checkout, not the shared Git directory.

set -euo pipefail

usage() {
  echo 'Usage: pnpm backend:test:worktree' >&2
  echo "Reads this checkout's .worktree.local.json and selects doughnut_<id>_test." >&2
  echo 'Migrates that database and runs the complete backend unit test suite.' >&2
}

if [[ $# -ne 0 ]]; then
  usage
  exit 1
fi

checkout_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
config_path="${checkout_root}/.worktree.local.json"

worktree_id="$(
  WORKTREE_CONFIG="${config_path}" node -e '
const fs = require("fs")
const config = JSON.parse(fs.readFileSync(process.env.WORKTREE_CONFIG, "utf8"))
if (typeof config.id !== "string") {
  process.stderr.write("Worktree configuration must contain a string \"id\".\n")
  process.exit(1)
}
process.stdout.write(config.id)
'
)"

if ! printf '%s\n' "${worktree_id}" | grep -Eq '^wt_[a-z0-9_]{1,32}$'; then
  echo "Worktree id must match wt_[a-z0-9_]{1,32}: ${worktree_id}" >&2
  exit 1
fi

database="doughnut_${worktree_id}_test"
expected_url="jdbc:mysql://127.0.0.1:3309/${database}?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true"

for var_name in SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL; do
  value="${!var_name:-}"
  if [[ -n "${value}" && "${value}" != "${expected_url}" ]]; then
    echo "Conflicting ${var_name} does not match the configured worktree database ${database}." >&2
    exit 1
  fi
done

echo "Selected database: ${database}"
export SPRING_DATASOURCE_URL="${expected_url}"
cd "${checkout_root}"
exec "${checkout_root}/backend/gradlew" \
  -p backend \
  -PworktreeTestRun \
  -Dspring.profiles.active=test \
  --rerun-tasks \
  --no-build-cache \
  --no-daemon \
  migrateTestDB \
  test
