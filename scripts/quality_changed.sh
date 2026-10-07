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

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
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

# The borrowed copy reuses the checkout's installation, which setup_pnpm_deps has
# just validated. pnpm's automatic verification would compare the copy's project
# paths with the checkout's installation and try to reinstall it, so it is off for
# this copy only.
lint_frontend_index() {
  index_copy="$(mktemp -d "${TMPDIR:-/tmp}/donut-frontend-index.XXXXXX")"
  trap 'rm -rf "$index_copy"' EXIT
  git checkout-index --all --prefix="$index_copy/"
  ln -s "$REPO_ROOT/node_modules" "$index_copy/node_modules"
  ln -s "$REPO_ROOT/frontend/node_modules" "$index_copy/frontend/node_modules"
  pnpm_config_verify_deps_before_run=false pnpm -C "$index_copy/frontend" lint
}

# Lint checks reuse the one installation setup_pnpm_deps validates below, so
# they call each package's check directly rather than its root script, which
# installs first.
run_quality_for_component() {
  case "$MODE:$1" in
    *:openapi)
      pnpm openapi:lint
      ;;
    lint:frontend)
      lint_frontend_index
      ;;
    lint:mcp-server|lint:cli)
      pnpm -C "$1" lint
      ;;
    lint:test-fixtures)
      pnpm -C packages/donut-test-fixtures lint
      ;;
    lint:root)
      pnpm biome check .
      ;;
    format:root)
      pnpm cy:format
      ;;
    *)
      pnpm "$1:$MODE"
      ;;
  esac
}

if [[ "$MODE" == lint ]] && [[ "$selected_components" =~ frontend|mcp-server|cli|test-fixtures|root|openapi ]]; then
  source "$SCRIPT_DIR/dev_setup.sh"
  log() { :; }
  setup_pnpm_deps
fi

for component in backend frontend mcp-server cli test-fixtures root openapi; do
  if [[ " $selected_components " == *" $component "* ]]; then
    run_quality_for_component "$component"
  fi
done
