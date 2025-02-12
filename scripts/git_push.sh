#!/usr/bin/env bash

set -uo pipefail
trap 'handle_error "${LINENO}" "$?"' ERR

# Format code
echo "Formatting code..."
pnpm format:all

# Check for unstaged changes
if [ -z "$(git status --porcelain || true)" ]; then
   echo "No changes to commit"
   exit 0
fi

# Stage changes
echo "Staging changes..."
git add .

# Commit with provided message
echo "Committing changes..."
git commit -am "$1"

# Check if we need to pull
if [ "$(git rev-list HEAD..@\{u\} 2>/dev/null | wc -l || echo 0)" -gt 0 ]; then
   # Pull with rebase
   echo "Pulling latest changes..."
   git pull -r
fi

# Push changes
echo "Pushing changes..."
git push

echo "All done! 🎉"
