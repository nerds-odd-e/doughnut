#!/usr/bin/env bash
# Shared worktree backend-test owner: lock, provision, validate, export URL.
# Sourced by the opt-in launcher and ordinary-command routing. Do not execute.

database_for_worktree() {
  echo "doughnut_${1}_test"
}

# Exclusive lock, first-use provision when needed, config validation, and
# SPRING_DATASOURCE_URL export for ${1} (checkout root). Leaves cwd unchanged.
backend_test_worktree_prepare() {
  local checkout_root="$1"
  local config_path="${checkout_root}/.worktree.local.json"
  local mysql_host="127.0.0.1"
  local mysql_port="3309"
  local lock_dir="${checkout_root}/.worktree.local.lock"
  local owner_pid
  local new_worktree_id
  local new_database
  local provisioning_sql
  local worktree_id
  local database
  local expected_url
  local var_name
  local value

  if ! mkdir "${lock_dir}" 2>/dev/null; then
    owner_pid="$(cat "${lock_dir}/owner.pid" 2>/dev/null || true)"
    if [[ ! "${owner_pid}" =~ ^[0-9]+$ ]]; then
      echo "Backend worktree tests are already running in this checkout (owner lock record is invalid). Refusing to start a second run." >&2
      exit 1
    fi
    # Live owner, or another process already reclaimed this dead owner.
    if kill -0 "${owner_pid}" 2>/dev/null \
      || ! mkdir "${lock_dir}/reclaimed.${owner_pid}" 2>/dev/null; then
      echo "Backend worktree tests are already running in this checkout (owner pid ${owner_pid}). Refusing to start a second run." >&2
      exit 1
    fi
  fi
  echo "$$" > "${lock_dir}/owner.pid"

  if [[ ! -f "${config_path}" ]]; then
    new_worktree_id="wt_$(node -e 'process.stdout.write(require("crypto").randomUUID().replace(/-/g, ""))')"
    new_database="$(database_for_worktree "${new_worktree_id}")"

    provisioning_sql="CREATE DATABASE ${new_database} DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; GRANT ALL PRIVILEGES ON ${new_database}.* TO 'doughnut'@'localhost'; GRANT ALL PRIVILEGES ON ${new_database}.* TO 'doughnut'@'127.0.0.1'; FLUSH PRIVILEGES;"
    echo "Provisioning database administration: creating ${new_database} (worktree id ${new_worktree_id})..." >&2
    mysql -u root -h "${mysql_host}" -P "${mysql_port}" -e "${provisioning_sql}"

    WORKTREE_CONFIG="${config_path}" WORKTREE_ID="${new_worktree_id}" node -e '
const fs = require("fs")
fs.writeFileSync(
  process.env.WORKTREE_CONFIG,
  JSON.stringify({ id: process.env.WORKTREE_ID }),
  { flag: "wx" }
)
'

    echo "Allocated new worktree environment: ${new_worktree_id}"
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

  database="$(database_for_worktree "${worktree_id}")"
  expected_url="jdbc:mysql://${mysql_host}:${mysql_port}/${database}?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true"

  for var_name in SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL; do
    value="${!var_name:-}"
    if [[ -n "${value}" && "${value}" != "${expected_url}" ]]; then
      echo "Conflicting ${var_name} does not match the configured worktree database ${database}." >&2
      exit 1
    fi
  done

  echo "Selected database: ${database}"
  export SPRING_DATASOURCE_URL="${expected_url}"
}
