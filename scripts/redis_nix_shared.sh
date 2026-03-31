#!/usr/bin/env bash
# Shared Redis (nix pkgs.redis) datadir/config and foreground server.
# Requires REDIS_BASEDIR, REDIS_HOME, REDIS_DATADIR, REDIS_CONF_FILE,
# REDIS_PID_FILE, REDIS_LOG_FILE, REDIS_TCP_PORT

redis_nix_prepare() {
  mkdir -p "${REDIS_DATADIR}" "${REDIS_HOME}" || true
  chmod 750 "${REDIS_DATADIR}" || true

  cat > "${REDIS_CONF_FILE}" <<EOF
# Redis configuration for development
port ${REDIS_TCP_PORT}
bind 127.0.0.1
dir ${REDIS_DATADIR}
pidfile ${REDIS_PID_FILE}
logfile ${REDIS_LOG_FILE}
loglevel notice
databases 16
save 900 1
save 300 10
save 60 10000
stop-writes-on-bgsave-error yes
rdbcompression yes
rdbchecksum yes
dbfilename dump.rdb
maxmemory-policy allkeys-lru
EOF
}

redis_nix_exec_redis_foreground() {
  exec "${REDIS_BASEDIR}/bin/redis-server" "${REDIS_CONF_FILE}"
}
