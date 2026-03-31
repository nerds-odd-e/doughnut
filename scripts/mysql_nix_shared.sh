#!/usr/bin/env bash
# Shared MySQL (nix mysql84) datadir init and server invocation.
# Requires MYSQL_BASEDIR, MYSQL_DATADIR, MYSQL_TCP_PORT, MYSQL_HOME,
# MYSQL_PID_FILE, MYSQL_UNIX_SOCKET, MYSQLX_UNIX_SOCKET, MYSQLX_TCP_PORT

mysql_nix_prepare_datadir() {
  if [ ! -d "${MYSQL_DATADIR}" ]; then
    mkdir -p "${MYSQL_DATADIR}" || true
    chmod 750 "${MYSQL_DATADIR}" || true
  fi

  if [ -z "$(ls -A "${MYSQL_DATADIR}" 2>/dev/null || true)" ]; then
    "${MYSQL_BASEDIR}/bin/mysqld" \
      --initialize-insecure \
      --port="${MYSQL_TCP_PORT}" \
      --user="$(whoami)" \
      --datadir="${MYSQL_DATADIR}" \
      --tls-version=TLSv1.2 \
      --basedir="${MYSQL_BASEDIR}" \
      --explicit_defaults_for_timestamp
  fi
}

mysql_nix_start_mysqld_background() {
  "${MYSQL_BASEDIR}/bin/mysqld" \
    --datadir="${MYSQL_DATADIR}" \
    --pid-file="${MYSQL_PID_FILE}" \
    --port="${MYSQL_TCP_PORT}" \
    --socket="${MYSQL_UNIX_SOCKET}" \
    --mysqlx-socket="${MYSQLX_UNIX_SOCKET}" \
    --mysqlx_port="${MYSQLX_TCP_PORT}" \
    --tls-version=TLSv1.2 > "${MYSQL_HOME}/mysql.log" 2>&1 &
}

mysql_nix_exec_mysqld_foreground() {
  exec >> "${MYSQL_HOME}/mysql.log" 2>&1
  exec "${MYSQL_BASEDIR}/bin/mysqld" \
    --datadir="${MYSQL_DATADIR}" \
    --pid-file="${MYSQL_PID_FILE}" \
    --port="${MYSQL_TCP_PORT}" \
    --socket="${MYSQL_UNIX_SOCKET}" \
    --mysqlx-socket="${MYSQLX_UNIX_SOCKET}" \
    --mysqlx_port="${MYSQLX_TCP_PORT}" \
    --tls-version=TLSv1.2
}
