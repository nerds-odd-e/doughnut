#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-https://storage.googleapis.com/dough-01}"
INSTALL_PREFIX="${INSTALL_PREFIX:-$HOME/.local}"
INSTALL_DIR="$INSTALL_PREFIX/bin"
DOWNLOAD_URL="$BASE_URL/doughnut-cli-latest/doughnut"

mkdir -p "$INSTALL_DIR"
curl -fsSL "$DOWNLOAD_URL" -o "$INSTALL_DIR/doughnut"
chmod +x "$INSTALL_DIR/doughnut"
echo "Installed doughnut to $INSTALL_DIR/doughnut"
echo "Ensure $INSTALL_DIR is in your PATH:"
echo "  export PATH=\"\$PATH:$INSTALL_DIR\""
