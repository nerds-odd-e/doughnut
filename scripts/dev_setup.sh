#!/usr/bin/env bash

# Deactivate nvm
deactivate_nvm() {
  command -v nvm >/dev/null 2>&1 && { nvm deactivate; }
}

# Setup PNPM and Biome
setup_pnpm_and_biome() {
  log "Setting up PNPM..."
  corepack prepare pnpm@10.12.4 --activate
  corepack use pnpm@10.12.4
  pnpm --frozen-lockfile recursive install

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
  printf "\n"
}
