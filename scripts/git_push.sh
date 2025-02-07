#!/usr/bin/env bash

set -euo pipefail
trap 'handle_error "${LINENO}" "$?"' ERR

# Format code
log "Formatting code..."
pnpm format:all

# Check for unstaged changes
if [ -z "$(git status --porcelain || true)" ]; then
   log "No changes to commit"
   exit 0
fi

# Stage changes
log "Staging changes..."
git add .

# Commit with provided message
log "Committing changes..."
git commit -am "$1"

# Check if we need to pull
if [ "$(git rev-list HEAD..@\{u\} 2>/dev/null | wc -l || echo 0)" -gt 0 ]; then
   # Pull with rebase
   log "Pulling latest changes..."
   git pull -r
fi

# Push changes
log "Pushing changes..."
git push

log "All done! 🎉"
