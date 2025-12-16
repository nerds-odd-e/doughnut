#!/usr/bin/env bash
#
# Nix development shell hook script
# This script is called by flake.nix shellHook to set up the development environment
#
# Parameters:
#   $1 - fzf path (from pkgs.fzf)
#   $2 - MySQL package path (from pkgs.mysql84)
#   $3 - Redis package path (from pkgs.redis)
#   $4 - Poetry path (from pkgs.poetry)

FZF_PATH="$1"
MYSQL_PKG_PATH="$2"
REDIS_PKG_PATH="$3"
POETRY_PATH="$4"

# Source helper scripts
source ./scripts/shell_setup.sh
source ./scripts/dev_setup.sh

# Initialize basic shell environment
setup_shell
setup_logging
setup_fzf "${FZF_PATH}"

# Add git push script alias
alias g='./scripts/git_push.sh'

# Deactivate nvm if exists
deactivate_nvm

# Setup core environment
setup_env_vars

# Setup MySQL environment
setup_mysql_env "${MYSQL_PKG_PATH}"

# Setup Redis environment
setup_redis_env "${REDIS_PKG_PATH}"

# Add Python to PATH if enabled
if [ "${PYTHON_DEV:-}" = "true" ] && command -v python >/dev/null 2>&1; then
  export PYTHON_PATH="$(dirname $(dirname $(readlink -f $(which python))))"
  export PATH="${POETRY_PATH}:$PYTHON_PATH/bin:$PATH"

  # Setup Python environment if enabled
  setup_python "${POETRY_PATH}"
fi

echo "CURSOR_DEV: ${CURSOR_DEV:-}"
if [ "${CURSOR_DEV:-}" != "true" ]; then
  # Setup development environment
  setup_pnpm_and_biome
  setup_cypress

  # Start MySQL if not running
  if ! lsof -i :3309 -sTCP:LISTEN >/dev/null 2>&1; then
    log "Starting MySQL server..."
    ./scripts/init_mysql.sh
    check_mysql_ready
  else
    log "MySQL is running on port 3309 & ready to go! 🐬"
  fi

  # Start Redis if not running
  if ! lsof -i :6380 -sTCP:LISTEN >/dev/null 2>&1; then
    log "Starting Redis server..."
    ./scripts/init_redis.sh
    check_redis_ready
  else
    log "Redis is running on port 6380 & ready to go! 🗄️"
  fi
fi

# Print environment information
print_env_info

log "Environment setup complete! 🎉"
return 0

