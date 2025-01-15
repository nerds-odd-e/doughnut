#!/usr/bin/env bash

# Configure pnpm and start Biome
log "Setting up PNPM and Biome..."
corepack prepare pnpm@10.0.0 --activate
corepack use pnpm@10.0.0
pnpm --frozen-lockfile recursive install

# Stop and start Biome server
pnpm biome stop >/dev/null 2>&1 || true
pnpm biome start >/dev/null 2>&1 &

# Setup Cypress if needed
if [[ "$USER" = @(codespace|gitpod) ]]; then
  log "Setting up Cypress for cloud environment..."
  [[ -d $HOME/.cache/Cypress ]] || pnpx cypress install --force
fi
