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

checkout_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
config_path="${checkout_root}/.worktree.local.json"

lock_dir="${checkout_root}/.worktree.local.lock"
if mkdir "${lock_dir}" 2>/dev/null; then
  echo "$$" > "${lock_dir}/owner.pid"
else
  owner_pid="$(cat "${lock_dir}/owner.pid" 2>/dev/null || true)"
  if [[ "${owner_pid}" =~ ^[0-9]+$ ]]; then
    echo "Backend worktree tests are already running in this checkout (owner pid ${owner_pid}). Refusing to start a second run." >&2
  else
    echo "Backend worktree tests are already running in this checkout (owner lock record is invalid). Refusing to start a second run." >&2
  fi
  exit 1
fi

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
exec "${gradle_cmd[@]}"
