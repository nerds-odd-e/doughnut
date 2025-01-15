#!/usr/bin/env bash

# MySQL initialization function
init_mysql() {
  local attempts=10
  local i=0

  # Ensure no existing MySQL processes
  pkill -f "mysqld.*${MYSQL_TCP_PORT}" || true
  pkill -f "mysqld.*${MYSQL_HOME}" || true
  sleep 1

  # Clean up any stale files
  rm -f "${MYSQL_UNIX_SOCKET}" "${MYSQLX_UNIX_SOCKET}" "${MYSQL_PID_FILE}" "${MYSQL_HOME}/mysql.log"

  # Only initialize if data directory doesn't exist
  if [ ! -d "${MYSQL_DATADIR}" ]; then
    mkdir -p "${MYSQL_HOME}"
    mkdir -p "${MYSQL_DATADIR}"
    chmod 750 "${MYSQL_DATADIR}"

    mysqld --initialize-insecure \
      --port="${MYSQL_TCP_PORT}" \
      --user="$(whoami)" \
      --datadir="${MYSQL_DATADIR}" \
      --tls-version=TLSv1.2 \
      --basedir="${MYSQL_BASEDIR}" \
      --explicit_defaults_for_timestamp || return 1
  fi

  mysqld --datadir="${MYSQL_DATADIR}" \
    --pid-file="${MYSQL_PID_FILE}" \
    --port="${MYSQL_TCP_PORT}" \
    --socket="${MYSQL_UNIX_SOCKET}" \
    --mysqlx-socket="${MYSQLX_UNIX_SOCKET}" \
    --mysqlx_port="${MYSQLX_TCP_PORT}" \
    --tls-version=TLSv1.2 > "${MYSQL_HOME}/mysql.log" 2>&1 &

  # Store MySQL PID
  echo $! > "${MYSQL_PID_FILE}"

  while [ $i -lt $attempts ]; do
    if mysql -u root -S "${MYSQL_UNIX_SOCKET}" -e "SELECT 1" >/dev/null 2>&1; then
      mysql -u root -S "${MYSQL_UNIX_SOCKET}" < "${PWD}/scripts/mysql/init_doughnut_db.sql"
      break
    fi
    i=$((i + 1))
    log "Waiting for MySQL... attempt ${i}/${attempts}"
    sleep 1
  done

  if [ $i -eq $attempts ]; then
    log "Error: MySQL failed to start after ${attempts} attempts"
    cat "${MYSQL_HOME}/mysql.log"
    return 1
  fi

  # Keep MySQL running in foreground
  wait "$(cat "${MYSQL_PID_FILE}")"
}

# Execute the function
init_mysql
