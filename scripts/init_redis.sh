#!/usr/bin/env bash

set -uo pipefail

# Redis initialization function
init_redis() {
  local REDIS_PID
  REDIS_PID=$(pgrep redis-server)
  export REDIS_PID

  # Only initialize if data directory doesn't exist
  if [ ! -d "${REDIS_DATADIR}" ]; then
    mkdir -p "${REDIS_DATADIR}" || true
    chmod 750 "${REDIS_DATADIR}" || true
  fi

  # Create Redis home directory
  if [ ! -d "${REDIS_HOME}" ]; then
    mkdir -p "${REDIS_HOME}" || true
  fi

  if [[ -z "${REDIS_PID}" ]]; then
    # Create Redis configuration file
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

    # Start Redis server
    redis-server "${REDIS_CONF_FILE}" --daemonize yes

    export REDIS_PID=$!
    # Wait for Redis to be ready
    sleep 2
    log "Redis server started on port ${REDIS_TCP_PORT}"
  else
    log "Redis server is already running (PID: ${REDIS_PID})"
  fi
}

init_redis
