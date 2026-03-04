# doughnut_mobile

A new Flutter project.

## Integration tests (Patrol)

Integration tests use [Patrol](https://patrol.leancode.co/). Run them with the Patrol CLI (not `flutter test`).

One-time setup:

```bash
fvm flutter pub global activate patrol_cli
```

Run tests (Android device/emulator must be connected):

```bash
# From doughnut_mobile/
./scripts/patrol_test.sh
# Or with FVM Dart + global snapshot (if script fails due to snapshot version):
fvm dart $HOME/.pub-cache/global_packages/patrol_cli/bin/main.dart-3.11.1.snapshot test --flutter-command "fvm flutter"
```

## Getting Started

This project is a starting point for a Flutter application.

A few resources to get you started if this is your first Flutter project:

- [Lab: Write your first Flutter app](https://flutter.dev/docs/get-started/codelab)
- [Cookbook: Useful Flutter samples](https://flutter.dev/docs/cookbook)

For help getting started with Flutter, view our
[online documentation](https://flutter.dev/docs), which offers tutorials,
samples, guidance on mobile development, and a full API reference.
