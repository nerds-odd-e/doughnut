#!/usr/bin/env bash

set -euo pipefail

# Configure pnpm and start Biome
log "Setting up PNPM and Biome..."
corepack prepare pnpm@10.0.0 --activate >/dev/null 2>&1
corepack use pnpm@10.0.0 >/dev/null 2>&1
pnpm --frozen-lockfile recursive install

# Stop and start Biome server
pnpm biome stop >/dev/null 2>&1 || true
nohup pnpm biome start >/dev/null 2>&1 &
disown

# Setup Cypress if needed
if [[ "$USER" = @(codespace|gitpod) ]]; then
  log "Setting up Cypress for cloud environment..."
  [[ -d $HOME/.cache/Cypress ]] || pnpx cypress install --force
fi
