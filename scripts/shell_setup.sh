#!/usr/bin/env bash

# Shell configuration
setup_shell() {
  if [ -n "${ZSH_VERSION:-}" ]; then
    emulate -L bash
    export PS1="(nix) ${PS1:-%# }"
  else
    export PS1="(nix) ${PS1:-$ }"
  fi
}

# Logging and error handling
setup_logging() {
  export LOG_FUNCTION='log() { echo "[$(date +"%Y-%m-%d %H:%M:%S")] $*"; }'
  eval "${LOG_FUNCTION}"

  export ERROR_HANDLER='handle_error() { local error_code="$2"; log "Warning: Command exited with status $error_code"; return 0; }'
  eval "${ERROR_HANDLER}"
  trap 'handle_error "0" "$?"' ERR
}

# Configure fzf
setup_fzf() {
  local fzf_path="$1"
  export FZF_DEFAULT_OPTS="--height 40% --layout=reverse --border"

  if [ -n "${ZSH_VERSION:-}" ]; then
    [ -e "${fzf_path}/share/fzf/key-bindings.zsh" ] && source "${fzf_path}/share/fzf/key-bindings.zsh"
    [ -e "${fzf_path}/share/fzf/completion.zsh" ] && source "${fzf_path}/share/fzf/completion.zsh"
  else
    [ -e "${fzf_path}/share/fzf/key-bindings.bash" ] && source "${fzf_path}/share/fzf/key-bindings.bash"
    [ -e "${fzf_path}/share/fzf/completion.bash" ] && source "${fzf_path}/share/fzf/completion.bash"
  fi
}

# Setup core environment variables
setup_env_vars() {
  export LANG="en_US.UTF-8"
  export SOURCE_REPO_NAME="${PWD##*/}"

  # Core paths
  export JAVA_HOME="$(dirname $(dirname $(readlink -f $(which javac))))"
  export NODE_PATH="$(dirname $(dirname $(readlink -f $(which node))))"
  export PNPM_HOME="$(dirname $(dirname $(readlink -f $(which pnpm))))"
  export GRADLE_OPTS="--enable-native-access=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED"

  # Base PATH
  export PATH="${JAVA_HOME}/bin:${NODE_PATH}/bin:${PNPM_HOME}/bin:${PATH}"
}

# MySQL environment setup
setup_mysql_env() {
  export MYSQL_BASEDIR="$1"
  export MYSQL_HOME="${PWD}/mysql"
  export MYSQL_DATADIR="${MYSQL_HOME}/data"
  export MYSQL_UNIX_SOCKET="${MYSQL_HOME}/mysql.sock"
  export MYSQLX_UNIX_SOCKET="${MYSQL_HOME}/mysqlx.sock"
  export MYSQL_PID_FILE="${MYSQL_HOME}/mysql.pid"
  export MYSQL_TCP_PORT="3309"
  export MYSQLX_TCP_PORT="33090"
  export MYSQL_LOG_FILE="${MYSQL_HOME}/mysql.log"
}

# Check if MySQL is ready
check_mysql_ready() {
  local MAX_RETRIES=30
  local RETRY_COUNT=0

  while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if mysqladmin ping -h127.0.0.1 -P3309 --silent >/dev/null 2>&1; then
      log "MySQL is ready! 🚀"
      return 0
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
    log "Waiting for MySQL to be ready... (attempt $RETRY_COUNT/$MAX_RETRIES)"
    sleep 1
  done

  log "Error: MySQL failed to start after $MAX_RETRIES attempts! 💀"
  return 1
}

# Redis environment setup
setup_redis_env() {
  export REDIS_BASEDIR="$1"
  export REDIS_HOME="${PWD}/redis"
  export REDIS_DATADIR="${REDIS_HOME}/data"
  export REDIS_CONF_FILE="${REDIS_HOME}/redis.conf"
  export REDIS_PID_FILE="${REDIS_HOME}/redis.pid"
  export REDIS_LOG_FILE="${REDIS_HOME}/redis.log"
  export REDIS_TCP_PORT="6380"
}

# Check if Redis is ready
check_redis_ready() {
  local MAX_RETRIES=30
  local RETRY_COUNT=0

  while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if redis-cli -p ${REDIS_TCP_PORT} ping >/dev/null 2>&1; then
      local response=$(redis-cli -p ${REDIS_TCP_PORT} ping 2>/dev/null)
      if [ "$response" = "PONG" ]; then
        log "Redis is ready! \ud83d\ude80"
        return 0
      fi
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
    log "Waiting for Redis to be ready... (attempt $RETRY_COUNT/$MAX_RETRIES)"
    sleep 1
  done

  log "Error: Redis failed to start after $MAX_RETRIES attempts! \ud83d\udc80"
  return 1
}
