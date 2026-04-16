#!/bin/bash

# Checks that the total number of @wip-tagged E2E scenarios does not exceed a
# threshold.  When @wip is a feature-level tag (appears before the Feature:
# keyword), every Scenario / Scenario Outline in that file counts.

set -e

FEATURE_DIR="e2e_test/features"
MAX_WIP=5
TOTAL=0

count_scenarios_in_file() {
  grep -cE '^\s*(Scenario|Scenario Outline):' "$1" 2>/dev/null || echo 0
}

is_feature_level_wip() {
  # @wip is feature-level if it appears on a line before the first Feature: line
  awk '
    /^[[:space:]]*Feature:/ { exit 1 }
    /@wip/                  { found=1 }
    END                     { exit !found }
  ' "$1"
}

count_scenario_level_wip() {
  # Count Scenario/Scenario Outline lines that are directly preceded (possibly
  # with blank lines or other tags in between) by a @wip tag after the Feature:
  # line.
  awk '
    BEGIN { wip=0; count=0 }
    /^[[:space:]]*Feature:/  { past_feature=1; next }
    !past_feature            { next }
    /@wip/                   { wip=1; next }
    /^[[:space:]]*@/         { next }
    /^[[:space:]]*$/         { next }
    /^[[:space:]]*(Scenario|Scenario Outline):/ { if (wip) count++; wip=0; next }
    { wip=0 }
    END { print count }
  ' "$1"
}

WIP_FILES=$(find "$FEATURE_DIR" -name "*.feature" -exec grep -l "@wip" {} \; 2>/dev/null || true)

if [ -n "$WIP_FILES" ]; then
  echo "@wip scenarios found:"
  echo ""

  for file in $WIP_FILES; do
    if is_feature_level_wip "$file"; then
      n=$(count_scenarios_in_file "$file")
      echo "  $file (feature-level @wip, $n scenarios)"
    else
      n=$(count_scenario_level_wip "$file")
      echo "  $file ($n scenarios)"
    fi
    TOTAL=$((TOTAL + n))
  done

  echo ""
  echo "Total @wip scenarios: $TOTAL (limit: $MAX_WIP)"
fi

if [ "$TOTAL" -gt "$MAX_WIP" ]; then
  echo ""
  echo "ERROR: Too many @wip scenarios ($TOTAL > $MAX_WIP)."
  echo "Resolve some work-in-progress E2E scenarios before adding more."
  exit 1
else
  echo "OK: @wip scenario count ($TOTAL) is within the limit of $MAX_WIP."
fi
