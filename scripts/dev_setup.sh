#!/usr/bin/env bash

# Deactivate nvm
deactivate_nvm() {
  command -v nvm >/dev/null 2>&1 && { nvm deactivate; }
}

# Skip corepack + pnpm when direnv/nix runs the shell hook more than once per cd (corepack use triggers a workspace install by itself).
DOUGHNUT_PNPM_FINGERPRINT_FILE="${DOUGHNUT_PNPM_FINGERPRINT_FILE:-.doughnut-pnpm-lock.sha256}"

doughnut_workspace_deps_fingerprint() {
  if [ ! -f "${PWD}/pnpm-lock.yaml" ] || [ ! -f "${PWD}/package.json" ]; then
    return 1
  fi
  if command -v shasum >/dev/null 2>&1; then
    (cat "${PWD}/pnpm-lock.yaml" "${PWD}/package.json" "${PWD}/pnpm-workspace.yaml" 2>/dev/null) | shasum -a 256 | awk '{print $1}'
  elif command -v sha256sum >/dev/null 2>&1; then
    (cat "${PWD}/pnpm-lock.yaml" "${PWD}/package.json" "${PWD}/pnpm-workspace.yaml" 2>/dev/null) | sha256sum | awk '{print $1}'
  else
    cksum "${PWD}/pnpm-lock.yaml" "${PWD}/package.json" 2>/dev/null | cksum | awk '{print $1}'
  fi
}

doughnut_needs_pnpm_install() {
  local current
  [ "${DOUGHNUT_SHELL_HOOK_FORCE_PNPM:-}" = "1" ] && return 0
  [ ! -d "${PWD}/node_modules" ] && return 0
  current="$(doughnut_workspace_deps_fingerprint)" || return 0
  [ -z "${current}" ] && return 0
  [ ! -f "${PWD}/${DOUGHNUT_PNPM_FINGERPRINT_FILE}" ] && return 0
  [ "$(cat "${PWD}/${DOUGHNUT_PNPM_FINGERPRINT_FILE}" 2>/dev/null)" != "${current}" ] && return 0
  return 1
}

# Setup PNPM and Biome
setup_pnpm_and_biome() {
  log "Setting up PNPM..."
  if doughnut_needs_pnpm_install; then
    corepack prepare pnpm@10.33.0 --activate
    corepack use pnpm@10.33.0
    pnpm --frozen-lockfile recursive install && doughnut_workspace_deps_fingerprint >"${PWD}/${DOUGHNUT_PNPM_FINGERPRINT_FILE}"
  else
    log "Skipping corepack/pnpm (workspace fingerprint unchanged). Set DOUGHNUT_SHELL_HOOK_FORCE_PNPM=1 to force."
  fi

  if [ -e /etc/NIXOS ] || [ -e /etc/nixos ]; then
    log "Patching Biome binaries on NixOS..."
    BIOME_VERSION=$(node -p "require('./package.json').devDependencies['@biomejs/biome']" 2>/dev/null || echo "")
    # Static link dynamic libs/bin for NixOS
    if [ -n "$BIOME_VERSION" ]; then
      autoPatchelf "./node_modules/.pnpm/@biomejs+cli-linux-x64@${BIOME_VERSION}/node_modules/@biomejs/cli-linux-x64" || true
    fi
  fi

  # Restart biome daemon with error handling
  log "Stopping existing Biome daemon..."
  pnpm biome stop || true
  log "Starting Biome daemon..."
  pnpm biome start || true
}

# Setup Cypress
setup_cypress() {
  log "Setting up Cypress..."
  CYPRESS_VERSION=$(node -p "require('./package.json').devDependencies.cypress" 2>/dev/null || echo "")
  if [ -n "${CYPRESS_VERSION:-}" ]; then
    if [[ ! -d "$HOME/.cache/Cypress/${CYPRESS_VERSION}" ]] && [[ ! -d "$HOME/Library/Caches/Cypress/${CYPRESS_VERSION}" ]]; then
      log "Installing Cypress version ${CYPRESS_VERSION}"
      pnpx cypress install --version "$CYPRESS_VERSION" --force
    fi
  fi

  # Static link dynamic libs/bin for NixOS
  if [ -e /etc/NIXOS ] || [ -e /etc/nixos ]; then
    log "Patching Cypress binaries on NixOS..."
    autoPatchelf "${HOME}/.cache/Cypress/${CYPRESS_VERSION}/Cypress/"
  fi
  export CYPRESS_CACHE_FOLDER=$HOME/.cache/Cypress
}

# Setup Python and Poetry
setup_python() {
  if [ "${PYTHON_DEV:-}" = "true" ]; then
    log "Setting up Python development environment..."
    log "Checking poetry installation..."
    log "Poetry binary should be at: $1/poetry"
    which poetry || log "Poetry not found in PATH: $PATH"

    if command -v poetry >/dev/null 2>&1; then
      poetry --version
      poetry config virtualenvs.in-project true
      if [ ! -f pyproject.toml ]; then
        log "No pyproject.toml found. You can initialize a new Python project with 'poetry init'"
      else
        log "Installing Python dependencies..."
        poetry install
      fi
    fi
  fi
}

# Print environment info
print_env_info() {
  cat <<'EOF'
╔════════════════════════════════════════════════════════════════════════════════════╗
║                         NIX DEVELOPMENT ENVIRONMENT                                ║
╚════════════════════════════════════════════════════════════════════════════════════╝
EOF

  printf "\n%s\n" "🚀 Project: $SOURCE_REPO_NAME"
  printf "📦 Versions:\n"
  printf "  • Nix:    %s\n" "$(nix --version)"
  printf "  • Java:   %s\n" "$(javac --version)"
  printf "  • Node:   %s\n" "$(node --version)"
  printf "  • PNPM:   %s\n" "$(pnpm --version)"
  printf "  • Biome:  %s\n" "$(pnpm biome --version)"

  if command -v python >/dev/null 2>&1; then
    printf "  • Python: %s\n" "$(python --version)"
    if command -v poetry >/dev/null 2>&1; then
      printf "  • Poetry: %s\n" "$(poetry --version)"
    fi
  fi

  printf "\n📂 Paths:\n"
  printf "  • JAVA_HOME:     %s\n" "$JAVA_HOME"
  printf "  • NODE_PATH:     %s\n" "$NODE_PATH"
  printf "  • PNPM_HOME:     %s\n" "$PNPM_HOME"
  if [ -n "$PYTHON_PATH" ]; then
    printf "  • PYTHON_PATH:   %s\n" "$PYTHON_PATH"
  fi
  printf "  • MYSQL_HOME:    %s\n" "$MYSQL_HOME"
  printf "  • MYSQL_DATADIR: %s\n" "$MYSQL_DATADIR"
  printf "  • REDIS_HOME:    %s\n" "$REDIS_HOME"
  printf "  • REDIS_DATADIR: %s\n" "$REDIS_DATADIR"
  printf "\n"
}
