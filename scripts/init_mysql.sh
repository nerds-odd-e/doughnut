#!/usr/bin/env bash

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=mysql_nix_shared.sh
source "${REPO_ROOT}/scripts/mysql_nix_shared.sh"

init_mysql() {
  local MYSQLD_PID
  MYSQLD_PID=$(pgrep mysqld)
  export MYSQLD_PID

  if [ ! -d "${MYSQL_DATADIR}" ]; then
    mkdir -p "${MYSQL_DATADIR}" || true
    chmod 750 "${MYSQL_DATADIR}" || true
  fi

  if [[ -z "${MYSQLD_PID}" ]]; then
    mysql_nix_prepare_datadir
    mysql_nix_start_mysqld_background
    export MYSQLD_PID=$!
    sleep 5
    MYSQL_READY=""
    export MYSQL_READY
    MYSQL_READY=$("${MYSQL_BASEDIR}/bin/mysqladmin" ping -u root -S "${MYSQL_UNIX_SOCKET}")
    echo "${MYSQL_READY}"
    "${MYSQL_BASEDIR}/bin/mysql" -u root -S "${MYSQL_UNIX_SOCKET}" < "${REPO_ROOT}/scripts/sql/init_doughnut_db.sql"
  fi
}

init_mysql
