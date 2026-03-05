#!/usr/bin/env bash
#
# Format only changed subprojects.
# Uses git to detect which directories have changes (staged + unstaged).

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

CHANGED_FILES="$(git diff HEAD --name-only 2>/dev/null || true)"
if [ -z "$CHANGED_FILES" ]; then
  exit 0
fi

# Subprojects: prefix -> format command
run_backend=false
run_frontend=false
run_mcp_server=false
run_cli=false
run_cy=false
run_openapi=false

while IFS= read -r file; do
  case "$file" in
    backend/*) run_backend=true ;;
    frontend/*) run_frontend=true ;;
    mcp-server/*) run_mcp_server=true ;;
    cli/*) run_cli=true ;;
    e2e_test/*|cypress.config.*|cypress/*) run_cy=true ;;
    open_api_docs.yaml) run_openapi=true ;;
  esac
done <<< "$CHANGED_FILES"

if $run_backend || $run_frontend || $run_mcp_server || $run_cli || $run_cy || $run_openapi; then
  pnpm --frozen-lockfile --silent recursive install 2>/dev/null || true

  if $run_backend; then pnpm backend:format; fi
  if $run_frontend; then pnpm frontend:format; fi
  if $run_mcp_server; then pnpm mcp-server:format; fi
  if $run_cli; then pnpm cli:format; fi
  if $run_cy; then pnpm cy:format; fi
  if $run_openapi; then pnpm openapi:lint; fi
fi
