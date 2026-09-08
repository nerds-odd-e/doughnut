#!/usr/bin/env bash
# Shared worktree backend-test owner: lock, provision, validate, export URL.
# Sourced by the opt-in launcher and ordinary-command routing. Do not execute.

database_for_worktree() {
  echo "doughnut_${1}_test"
}

unit_database_provisioning_sql() {
  echo "CREATE DATABASE ${1} DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; GRANT ALL PRIVILEGES ON ${1}.* TO 'doughnut'@'localhost'; GRANT ALL PRIVILEGES ON ${1}.* TO 'doughnut'@'127.0.0.1'; FLUSH PRIVILEGES;"
}

# Linked worktrees have a distinct git-dir under the shared common dir.
# Unconfigured primary checkouts keep pass-through (no allocation here).
is_linked_git_worktree() {
  local git_dir common_dir
  git_dir="$(git -C "${1}" rev-parse --git-dir 2>/dev/null)" || return 1
  common_dir="$(git -C "${1}" rev-parse --git-common-dir 2>/dev/null)" || return 1
  git_dir="$(cd "${1}" && cd "${git_dir}" && pwd)" || return 1
  common_dir="$(cd "${1}" && cd "${common_dir}" && pwd)" || return 1
  [[ "${git_dir}" != "${common_dir}" ]]
}

backend_worktree_isolation_applies() {
  [[ -f "${1}/.worktree.local.json" ]] || is_linked_git_worktree "${1}"
}

# Exclusive lock, first-use provision when needed, config validation, URL
# conflict refusal (env and remaining CLI args), and SPRING_DATASOURCE_URL
# export for ${1} (checkout root). Leaves cwd unchanged.
backend_test_worktree_prepare() {
  local checkout_root="$1"
  shift
  local config_path="${checkout_root}/.worktree.local.json"
  local identity_js
  identity_js="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/worktree-identity.mjs"
  local mysql_host="127.0.0.1"
  local mysql_port="3309"
  local lock_dir="${checkout_root}/.worktree.local.lock"
  local owner_pid
  local new_worktree_id
  local new_database
  local provisioning_sql
  local allocated_identity=0
  local worktree_id
  local database
  local expected_url
  local existing_schema
  local var_name
  local arg

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
    new_worktree_id="$(node "${identity_js}" generate)"
    new_database="$(database_for_worktree "${new_worktree_id}")"

    provisioning_sql="$(unit_database_provisioning_sql "${new_database}")"
    echo "Provisioning database administration: creating ${new_database} (worktree id ${new_worktree_id})..." >&2
    mysql -u root -h "${mysql_host}" -P "${mysql_port}" -e "${provisioning_sql}"

    node "${identity_js}" publish "${checkout_root}" "${new_worktree_id}"

    echo "Allocated new worktree environment: ${new_worktree_id}"
    allocated_identity=1
  fi

  worktree_id="$(node "${identity_js}" read "${checkout_root}")"

  database="$(database_for_worktree "${worktree_id}")"
  expected_url="jdbc:mysql://${mysql_host}:${mysql_port}/${database}?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true"

  refuse_conflicting_url() {
    local name="$1"
    local value="$2"
    if [[ -n "${value}" && "${value}" != "${expected_url}" ]]; then
      echo "Conflicting ${name} does not match the configured worktree database ${database}." >&2
      exit 1
    fi
  }

  for var_name in SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL; do
    refuse_conflicting_url "${var_name}" "${!var_name:-}"
  done

  for arg in "$@"; do
    case "${arg}" in
      -Dspring.datasource.url=*|--spring.datasource.url=*|-Dspring.flyway.url=*|--spring.flyway.url=*)
        refuse_conflicting_url "${arg%%=*}" "${arg#*=}"
        ;;
    esac
  done

  if [[ "${allocated_identity}" -eq 0 ]]; then
    existing_schema="$(mysql -u root -h "${mysql_host}" -P "${mysql_port}" -N -e "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='${database}'" | tr -d '[:space:]')"
    if [[ "${existing_schema}" != "${database}" ]]; then
      provisioning_sql="$(unit_database_provisioning_sql "${database}")"
      echo "Provisioning database administration: creating ${database} (worktree id ${worktree_id})..." >&2
      mysql -u root -h "${mysql_host}" -P "${mysql_port}" -e "${provisioning_sql}"
    fi
  fi

  echo "Selected database: ${database}"
  export SPRING_DATASOURCE_URL="${expected_url}"
}
