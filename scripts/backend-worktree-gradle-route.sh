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
has_test=false
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
    test|:test|:backend:test) has_test=true ;;
  esac
done

if [[ "${PWD}" -ef "${backend_dir}" ]]; then
  is_backend_project=true
fi

if [[ "${has_migrate}" == true && "${has_test}" != true && "${is_backend_project}" == true && -f "${checkout_root}/.worktree.local.json" ]]; then
  # shellcheck source=backend-test-worktree-owner.sh
  source "${script_dir}/backend-test-worktree-owner.sh"
  backend_test_worktree_prepare "${checkout_root}"
  set -- "$@" --no-daemon
fi

export DONUT_WORKTREE_HANDOFF=1
exec "${backend_dir}/gradlew" "$@"
