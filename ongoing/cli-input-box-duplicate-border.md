# CLI Input Box Duplicate Border Bug - Plan

## Problem

When running the interactive CLI (TTY mode):

1. **Empty Enter or command selection**: Pressing Enter on an empty input box, or selecting a command with Enter (from suggestions), shows an additional line of the upper border (`┌───┐`) above the actual input box.
2. **Typing + Enter**: After typing something and pressing Enter, there is an additional line of the upper border before the history.
3. **Constraint**: The solution must NOT be a full redraw.
4. **Regressed in**: commit e38d82870

## Root Cause Analysis

Commit e38d82870 introduced:

```javascript
const rl = readline.createInterface({
  input: stdin,
  output: process.stdout,
  escapeCodeTimeout: 50,
})
readline.emitKeypressEvents(stdin, rl)
```

Previously: `readline.emitKeypressEvents(stdin)` (no second argument).

When `emitKeypressEvents(stream, interface)` is used with an interface that has `output: process.stdout`, Node's readline attaches its `_refreshLine` (or similar) to keypress events. On every keypress—including Enter—the readline interface writes to stdout. Our custom `drawBox()` also writes to stdout. This creates a conflict: readline echoes/writes something (e.g. newline or cursor movement) that appears as an extra line, which combined with our box drawing produces the duplicate border effect.

**Likely fix**: Create the readline interface with a no-op output stream so it does not write to stdout. We still need the interface for `rl.close()` in `doExit()`. Use `output: new (require('stream').Writable)({ write(_c, _e, cb) { cb() } })` or equivalent to discard readline's output while preserving the interface for cleanup.

## Phases

### Phase 1: E2E test (reproduce, then @ignore)

1. Add a new feature file or scenario that:
   - Runs the CLI in interactive mode via PTY (`runInstalledCli` with input, or `runCliDirectWithInputAndPty`)
   - Sends minimal input to trigger the bug: e.g. `"\nexit\n"` (Enter on empty) or `"/exit\n"` (command then exit)
2. **Assertion**: The output matches `doughnut <version>` followed by the input box with nothing in between. Concretely:
   - Extract the substring from "doughnut X.X.X" to the first content row (`│`)
   - Between these, there must be exactly one top-border line (`┌` + `─`*n + `┐`)
   - No duplicate top-border line; no extra stray lines that look like the border
3. Run the test and confirm it **fails** (bug reproducible).
4. Add `@ignore` to the scenario.
5. Commit and push; wait for developer to do so before Phase 2.

### Phase 2: Fix

1. In `cli/src/adapters/ttyAdapter.ts`, change the readline interface to use a no-op output stream instead of `process.stdout`.
2. Remove `@ignore` from the scenario.
3. Run the E2E test and confirm it passes.
4. Verify no full redraw was introduced; the fix should be minimal (output stream change only).

### Phase 3: Sanity check

1. Run relevant CLI E2E tests (`cli_install_and_run`, `cli_recall`, etc.) to ensure no regressions.
2. Manually run `pnpm cli` and verify: Enter on empty, command selection, typing+Enter—no duplicate border.
