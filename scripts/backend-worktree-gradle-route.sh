#!/usr/bin/env bash
# Policy for repository-wrapper routing into the shared worktree owner.
# Replaces the wrapper via exec; always re-execs backend/gradlew with an
# invocation-local handoff so the wrapper does not route again.

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
checkout_root="$(cd "${script_dir}/.." && pwd)"
backend_dir="${checkout_root}/backend"

is_backend_project=false
expect_p_value=false
has_migrate=false
test_task=""
for arg in "$@"; do
  if [[ "${expect_p_value}" == true ]]; then
    expect_p_value=false
    if [[ "${arg}" == backend ]]; then
      is_backend_project=true
    fi
    continue
  fi
  case "${arg}" in
    -p) expect_p_value=true ;;
    migrateTestDB|:migrateTestDB) has_migrate=true ;;
    :backend:migrateTestDB)
      has_migrate=true
      is_backend_project=true
      ;;
    test|:test)
      test_task="${arg}"
      ;;
    :backend:test)
      is_backend_project=true
      test_task="${arg}"
      ;;
  esac
done

if [[ "${PWD}" -ef "${backend_dir}" ]]; then
  is_backend_project=true
fi

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

isolate_backend=false
if [[ "${is_backend_project}" == true ]]; then
  if [[ -f "${checkout_root}/.worktree.local.json" ]] \
    || is_linked_git_worktree "${checkout_root}"; then
    isolate_backend=true
  fi
fi

if [[ "${isolate_backend}" == true && ( -n "${test_task}" || "${has_migrate}" == true ) ]]; then
  # shellcheck source=backend-test-worktree-owner.sh
  source "${script_dir}/backend-test-worktree-owner.sh"
  backend_test_worktree_prepare "${checkout_root}" "$@"
  if [[ -n "${test_task}" ]]; then
    export DONUT_WORKTREE_HANDOFF=1

    migrate_args=()
    if [[ "${PWD}" -ef "${backend_dir}" ]]; then
      migrate_args+=(migrateTestDB)
    elif [[ "${test_task}" == ":backend:test" ]]; then
      migrate_args+=(:backend:migrateTestDB)
    else
      migrate_args+=(-p backend migrateTestDB)
    fi
    migrate_args+=(--no-daemon)
    "${backend_dir}/gradlew" "${migrate_args[@]}"

    set -- "$@" -Dspring.profiles.active=test --rerun-tasks --no-build-cache --no-daemon
  else
    set -- "$@" --no-daemon
  fi
fi

export DONUT_WORKTREE_HANDOFF=1
exec "${backend_dir}/gradlew" "$@"
