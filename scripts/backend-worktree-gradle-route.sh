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

configured_backend=false
if [[ "${is_backend_project}" == true && -f "${checkout_root}/.worktree.local.json" ]]; then
  configured_backend=true
fi

if [[ "${configured_backend}" == true && ( -n "${test_task}" || "${has_migrate}" == true ) ]]; then
  # shellcheck source=backend-test-worktree-owner.sh
  source "${script_dir}/backend-test-worktree-owner.sh"
  backend_test_worktree_prepare "${checkout_root}"
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
