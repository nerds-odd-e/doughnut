## Requirement

Fix regression where `e2e_test/features/cli/cli_recall.feature` intermittently fails at spelling recall with:

- `CLI did not show input box after send within 15s`

## Expected behavior

After sending a keystroke to the interactive CLI PTY, readiness should detect the visible command/input line as soon as the rendered screen is stable, even if extra ANSI/control bytes keep appending.

## Implementation note

Use visual stability (simulated/plain tail signature) instead of raw stdout length growth for readiness debounce.
