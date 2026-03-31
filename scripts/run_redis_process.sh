#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=redis_nix_shared.sh
source "${REPO_ROOT}/scripts/redis_nix_shared.sh"

redis_nix_prepare
redis_nix_exec_redis_foreground
