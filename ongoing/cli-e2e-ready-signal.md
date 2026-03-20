# CLI E2E ready signal

## Problem

CLI E2E used to infer “ready for input” from ANSI layout (grey placeholders, stability windows). That duplicated rendering logic and broke when the TTY UI or timing changed.

## Goal

The interactive CLI emits a single invisible byte sequence when the bordered input box accepts the next keystroke. PTY-based tests watch for that sequence instead of inferring from screen painting.

## Design

**Emission:** `INTERACTIVE_INPUT_READY_OSC` in `cli/src/renderer.ts` — OSC 133 ; A ST (FinalTerm “prompt start”). Appended in `ttyAdapter` via `finalizeInteractiveLiveRegionPaint` → `interactiveInputReadyOscSuffix` when `InteractiveInputReadyPaint` says the draft is empty and there is no interactive fetch wait.

**Detection:** `e2e_test/config/cliPtyRunner.ts` waits for that sequence (full capture at startup, or only bytes after a `pty.write` after each step). Short flush delay after the OSC so trailing chunks can arrive.

The PTY harness **inlines** the same bytes as `INTERACTIVE_INPUT_READY_OSC` (Cypress plugin load does not resolve imports into `cli/` reliably).

## Phases

### Phase 1: Emit the marker from the CLI — done

See `INTERACTIVE_INPUT_READY_OSC`, `InteractiveInputReadyPaint`, `interactiveInputReadyOscSuffix`, and `finalizeInteractiveLiveRegionPaint` in the codebase.

**Test:** `cli/tests/renderer.test.ts` (`interactiveInputReadyOscSuffix`).

### Phase 2: Detect the marker in the test infrastructure — done

`waitForInteractiveInputReadyOsc` in `cliPtyRunner.ts`; used by `runCliInPty`, `startInteractiveCli`, and `sendToInteractiveCli`.

**Test:** CLI feature specs under `e2e_test/features/cli/` (use a fresh `pnpm cli:bundle` when `CI=1`, or unset `CI` to run via `tsx`).
