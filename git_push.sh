#!/bin/bash

# Exit on any error
set -e

# Check if commit message was provided
if [ -z "$1" ]; then
    echo "Error: Please provide a commit message"
    exit 1
fi

# Format code
echo "Formatting code..."
pnpm format:all

# Stage changes
echo "Staging changes..."
git add .

# Commit with provided message
echo "Committing changes..."
git commit -am "$1"

# Pull with rebase
echo "Pulling latest changes..."
git pull -r

# Push changes
echo "Pushing changes..."
git push

echo "All done! 🎉"
