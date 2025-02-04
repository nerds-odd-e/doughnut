#!/usr/bin/env bash

set -uo pipefail

# MySQL initialization function
init_mysql() {
  local attempts=10
  local i=0

  # Only initialize if data directory doesn't exist
  if [ ! -d "${MYSQL_DATADIR}" ]; then
    mkdir -p "${MYSQL_DATADIR}"
    chmod 750 "${MYSQL_DATADIR}"

    log "Initializing MySQL server base & data directory..."
    "${MYSQL_BASEDIR}/bin/mysqld" \
      --initialize-insecure \
      --port="${MYSQL_TCP_PORT}" \
      --datadir="${MYSQL_DATADIR}" \
      --tls-version=TLSv1.2 \
      --basedir="${MYSQL_BASEDIR}" \
      --explicit_defaults_for_timestamp || return 1
  fi

  log "Starting MySQL server..."
  "${MYSQL_BASEDIR}/bin/mysqld" \
    --datadir="${MYSQL_DATADIR}" \
    --pid-file="${MYSQL_PID_FILE}" \
    --port="${MYSQL_TCP_PORT}" \
    --socket="${MYSQL_UNIX_SOCKET}" \
    --mysqlx-socket="${MYSQLX_UNIX_SOCKET}" \
    --mysqlx_port="${MYSQLX_TCP_PORT}" \
    --tls-version=TLSv1.2 > "${MYSQL_HOME}/mysql.log" 2>&1

  while [ $i -lt $attempts ]; do
    if "${MYSQL_BASEDIR}"/bin/mysql -u root -S "${MYSQL_UNIX_SOCKET}" -e "SELECT 1" >/dev/null 2>&1; then
      "${MYSQL_BASEDIR}"/bin/mysql -u root -S "${MYSQL_UNIX_SOCKET}" < "${PWD}/scripts/mysql/init_doughnut_db.sql"
      break
    fi
    i=$((i + 1))
    log "Waiting for MySQL server to be READY... attempt ${i}/${attempts}"
    sleep 0.5
  done

  if [ $i -eq $attempts ]; then
    log "Error: MySQL failed to start after ${attempts} attempts"
    cat "${MYSQL_HOME}/mysql.log"
    return 1
  fi
}

init_mysql
