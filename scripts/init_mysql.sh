#!/usr/bin/env bash

set -uo pipefail

# MySQL initialization function
init_mysql() {
  local MYSQLD_PID
  MYSQLD_PID=$(pgrep mysqld)
  export MYSQLD_PID

  # Only initialize if data directory doesn't exist
  if [ ! -d "${MYSQL_DATADIR}" ]; then
    mkdir -p "${MYSQL_DATADIR}" || true
    chmod 750 "${MYSQL_DATADIR}" || true
  fi

  if [[ -z "${MYSQLD_PID}" ]]; then
      [ ! "$(ls -A "${MYSQL_DATADIR}")" ] && "${MYSQL_BASEDIR}/bin/mysqld" \
      --initialize-insecure \
      --port="${MYSQL_TCP_PORT}" \
      --user="$(whoami)" \
      --datadir="${MYSQL_DATADIR}" \
      --tls-version=TLSv1.2 \
      --basedir="${MYSQL_BASEDIR}" \
      --explicit_defaults_for_timestamp

    "${MYSQL_BASEDIR}/bin/mysqld" \
      --datadir="${MYSQL_DATADIR}" \
      --pid-file="${MYSQL_PID_FILE}" \
      --port="${MYSQL_TCP_PORT}" \
      --socket="${MYSQL_UNIX_SOCKET}" \
      --mysqlx-socket="${MYSQLX_UNIX_SOCKET}" \
      --mysqlx_port="${MYSQLX_TCP_PORT}" \
      --tls-version=TLSv1.2 > "${MYSQL_HOME}/mysql.log" 2>&1 &

    export MYSQLD_PID=$!
    # Wait for MySQL to be ready then init doughnut DB
    sleep 5
    MYSQL_READY=""
    export MYSQL_READY
    MYSQL_READY=$("${MYSQL_BASEDIR}/bin/mysqladmin" ping -u root -S "${MYSQL_UNIX_SOCKET}")
    echo "${MYSQL_READY}"
    "${MYSQL_BASEDIR}/bin/mysql" -u root -S "${MYSQL_UNIX_SOCKET}" < "${PWD}/scripts/mysql/init_doughnut_db.sql"
  fi
}

init_mysql
