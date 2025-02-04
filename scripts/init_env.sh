#!/usr/bin/env bash

set -uo pipefail
trap 'handle_error "0" "$?"' ERR

# Configure pnpm and start Biome
log "Setting up PNPM and Biome..."
(
  corepack prepare pnpm@10.0.0 --activate >/dev/null 2>&1 || true
  corepack use pnpm@10.0.0 >/dev/null 2>&1 || true
  pnpm --frozen-lockfile recursive install || true
)

# Setup Cypress with specific version
log "Setting up Cypress..."
CYPRESS_VERSION=$(node -p "require('./package.json').devDependencies.cypress" || echo "")
if [ -n "$CYPRESS_VERSION" ]; then
  if [[ ! -d "$HOME/.cache/Cypress/${CYPRESS_VERSION//\"}" ]] && [[ ! -d "$HOME/Library/Caches/Cypress/${CYPRESS_VERSION//\"}" ]]; then
    pnpx cypress install --version ${CYPRESS_VERSION//\"} --force || true
  fi
fi

# Stop and start Biome server
(
  pnpm biome stop >/dev/null 2>&1 || true
  nohup pnpm biome start >/dev/null 2>&1 &
  disown
) || true
