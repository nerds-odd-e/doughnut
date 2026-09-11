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
  local script_dir
  script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  local identity_js="${script_dir}/worktree-identity.mjs"
  local admission_js="${script_dir}/worktree-retirement-admission.mjs"
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
  local admission_held=0
  local reclaimed_dir=""

  release_retirement_admission() {
    if [[ "${admission_held}" -eq 1 ]]; then
      node "${admission_js}" release "${checkout_root}" || true
      admission_held=0
    fi
  }

  # Admission before prepare/provision; release after backend ownership exists.
  node "${admission_js}" acquire "${checkout_root}" || exit 1
  admission_held=1

  if ! mkdir "${lock_dir}" 2>/dev/null; then
    owner_pid="$(cat "${lock_dir}/owner.pid" 2>/dev/null || true)"
    if [[ ! "${owner_pid}" =~ ^[0-9]+$ ]]; then
      release_retirement_admission
      echo "Backend worktree tests are already running in this checkout (owner lock record is invalid). Refusing to start a second run." >&2
      exit 1
    fi
    # Live owner, or another process already reclaimed this dead owner.
    if kill -0 "${owner_pid}" 2>/dev/null \
      || ! mkdir "${lock_dir}/reclaimed.${owner_pid}" 2>/dev/null; then
      release_retirement_admission
      echo "Backend worktree tests are already running in this checkout (owner pid ${owner_pid}). Refusing to start a second run." >&2
      exit 1
    fi
    reclaimed_dir="${lock_dir}/reclaimed.${owner_pid}"
  fi
  echo "$$" > "${lock_dir}/owner.pid"
  if [[ -n "${reclaimed_dir}" ]]; then
    rmdir "${reclaimed_dir}"
  fi
  backend_worktree_lock_dir="${lock_dir}"
  backend_worktree_lock_owned=1
  release_retirement_admission

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

# Move the ownership evidence behind an invocation-specific gate before
# deleting it. Supported contenders cannot replace a live owner, and an
# unexpected file appearing during release makes the final rmdir fail rather
# than deleting that evidence.
backend_test_worktree_release() {
  local release_dir="${backend_worktree_lock_dir}/releasing.$$"
  local moved_owner="${release_dir}/owner.pid"
  local recorded_owner

  mkdir "${release_dir}" || return $?
  if ! mv "${backend_worktree_lock_dir}/owner.pid" "${moved_owner}"; then
    rmdir "${release_dir}" 2>/dev/null || true
    return 1
  fi
  recorded_owner="$(cat "${moved_owner}" 2>/dev/null || true)"
  if [[ "${recorded_owner}" != "$$" ]]; then
    if [[ ! -e "${backend_worktree_lock_dir}/owner.pid" ]]; then
      mv "${moved_owner}" "${backend_worktree_lock_dir}/owner.pid" || true
    fi
    rmdir "${release_dir}" 2>/dev/null || true
    echo "Backend worktree ownership changed before release; preserving the recorded evidence." >&2
    return 1
  fi

  rm "${moved_owner}" || return $?
  rmdir "${release_dir}" || return $?
  rmdir "${backend_worktree_lock_dir}" || return $?
  backend_worktree_lock_owned=0
}

backend_test_worktree_finalize() {
  local status=$?
  local release_status=0
  trap - EXIT

  if [[ "${backend_worktree_lock_owned:-0}" -eq 1 ]]; then
    backend_test_worktree_release || release_status=$?
    if [[ "${release_status}" -ne 0 ]]; then
      echo "Failed to release backend worktree test ownership." >&2
      if [[ "${status}" -eq 0 ]]; then
        status="${release_status}"
      fi
    fi
  fi
  if [[ -n "${backend_worktree_received_signal:-}" ]]; then
    trap - INT TERM
    kill -s "${backend_worktree_received_signal}" "$$"
  fi
  exit "${status}"
}

# Keep the recorded invocation owner alive until its route-specific workload
# has ended and its outcome has been observed.
backend_test_worktree_run() {
  local checkout_root="$1"
  local workload="$2"
  shift 2
  backend_worktree_lock_owned=0
  backend_worktree_lock_dir=""
  backend_worktree_received_signal=""
  local status

  trap 'backend_worktree_received_signal=INT' INT
  trap 'backend_worktree_received_signal=TERM' TERM
  trap backend_test_worktree_finalize EXIT

  backend_test_worktree_prepare "${checkout_root}" "$@"
  if "${workload}"; then
    status=0
  else
    status=$?
  fi

  return "${status}"
}
