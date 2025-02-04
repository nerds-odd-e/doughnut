#!/usr/bin/env bash

set -uo pipefail

# Function to handle errors
handle_error() {
  local line_no=$1
  local error_code=$2
  log "Error on line ${line_no}: Command exited with status ${error_code}"
}

# Set error trap
trap 'handle_error "${LINENO}" "$?"' ERR

# Check if commit message was provided
if [ -z "${1:-}" ]; then
   log "Error: Please provide a commit message"
   log "Usage: $0 \"<commit message>\""
   exit 1
fi

# Check if we're in a git repository
if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
   log "Error: Not in a git repository"
   exit 1
fi

# Format code
log "Formatting code..."
pnpm format:all

# Check for unstaged changes
if [ -z "$(git status --porcelain)" ]; then
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
if [ "$(git rev-list HEAD..@\{u\} 2>/dev/null | wc -l)" -gt 0 ]; then
   # Pull with rebase
   log "Pulling latest changes..."
   git pull -r
fi

# Push changes
log "Pushing changes..."
git push

log "All done! 🎉"
