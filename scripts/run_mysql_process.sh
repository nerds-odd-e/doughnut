#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=mysql_nix_shared.sh
source "${REPO_ROOT}/scripts/mysql_nix_shared.sh"

mysql_nix_prepare_datadir
mysql_nix_exec_mysqld_foreground
