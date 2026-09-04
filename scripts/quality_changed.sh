#!/usr/bin/env bash

set -euo pipefail

MODE="${1:?Usage: quality_changed.sh format|lint}"
case "$MODE" in
  format|lint) ;;
  *)
    echo "Unknown quality mode: $MODE" >&2
    exit 1
    ;;
esac

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT"

selected_components=""

select_component() {
  if [[ " $selected_components " != *" $1 "* ]]; then
    selected_components+=" $1"
  fi
}

select_biome_components() {
  select_component frontend
  select_component mcp-server
  select_component cli
  select_component test-fixtures
  select_component root
}

select_components_for_file() {
  case "$1" in
    backend/*)
      select_component backend
      ;;
    frontend/*)
      select_component frontend
      ;;
    mcp-server/*)
      select_component mcp-server
      ;;
    cli/*)
      select_component cli
      ;;
    packages/donut-test-fixtures/*)
      select_component test-fixtures
      ;;
    e2e_test/*|cypress/*|cypress.config.*|scripts/*|packages/donut-api/*)
      select_component root
      ;;
    open_api_docs.yaml|redocly.yaml)
      select_component openapi
      ;;
    biome.json)
      select_biome_components
      ;;
    *.js|*.mjs|*.cjs|*.ts|*.tsx|*.json)
      select_component root
      ;;
  esac
}

if [[ "$MODE" == format ]]; then
  changed_files="$({
    git diff --name-only
    git diff --cached --name-only
    git ls-files --others --exclude-standard
  } | sort -u)"
else
  changed_files="$(git diff --cached --name-only)"
fi

while IFS= read -r file; do
  [[ -n "$file" ]] && select_components_for_file "$file"
done <<< "$changed_files"

run_quality_for_component() {
  case "$1" in
    root)
      pnpm "cy:$MODE"
      ;;
    openapi)
      pnpm openapi:lint
      ;;
    *)
      pnpm "$1:$MODE"
      ;;
  esac
}

for component in backend frontend mcp-server cli test-fixtures root openapi; do
  if [[ " $selected_components " == *" $component "* ]]; then
    run_quality_for_component "$component"
  fi
done
