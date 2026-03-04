#!/usr/bin/env bash
# Run Patrol integration tests using FVM's Dart (avoids "doesn't support Dart 3.4.4" when system dart is different).
# Requires: fvm, and "fvm flutter pub global activate patrol_cli" once.
set -e
cd "$(dirname "$0")/.."
PATROL_SNAPSHOT="$HOME/.pub-cache/global_packages/patrol_cli/bin/main.dart-3.11.1.snapshot"
if [[ ! -f "$PATROL_SNAPSHOT" ]]; then
  echo "Patrol CLI snapshot not found. Run: fvm flutter pub global activate patrol_cli"
  exit 1
fi
exec fvm dart "$PATROL_SNAPSHOT" test --flutter-command "fvm flutter" "$@"
