#!/usr/bin/env bash

# Configure pnpm and start Biome
log "Setting up PNPM and Biome..."
corepack prepare pnpm@10.0.0 --activate >/dev/null 2>&1
corepack use pnpm@10.0.0 >/dev/null 2>&1

set -uo pipefail
pnpm --frozen-lockfile recursive install

# Setup Cypress with specific version
log "Setting up Cypress..."
CYPRESS_VERSION=$(node -p "require('./package.json').devDependencies.cypress")
if [[ ! -d "$HOME/.cache/Cypress/${CYPRESS_VERSION//\"}" ]] && [[ ! -d "$HOME/Library/Caches/Cypress/${CYPRESS_VERSION//\"}" ]]; then
  pnpx cypress install --version ${CYPRESS_VERSION//\"} --force
fi

# Stop and start Biome server
(
  pnpm biome stop >/dev/null 2>&1 || true
  nohup pnpm biome start >/dev/null 2>&1 &
  disown
)
