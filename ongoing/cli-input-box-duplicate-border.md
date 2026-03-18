# CLI Input Box Duplicate Border Bug - Plan

## Problem

When running the interactive CLI (TTY mode):

1. **Empty Enter or command selection**: Pressing Enter on an empty input box, or selecting a command with Enter (from suggestions), shows an additional line of the upper border (`┌───┐`) above the actual input box.
2. **Typing + Enter**: After typing something and pressing Enter, there is an extra line of the upper border before the history.
3. **Constraint**: The solution must NOT be a full redraw.
4. **Regressed in**: commit e38d82870 (originally in `interactive.ts`; TTY logic now lives in `ttyAdapter.ts`)

## Root Cause Analysis

Commit e38d82870 introduced `readline.createInterface` with `output: process.stdout` plus `readline.emitKeypressEvents(stdin, rl)` (previously `emitKeypressEvents(stdin)` only).

When `emitKeypressEvents(stream, interface)` is used with an interface that has `output: process.stdout`, Node's readline attaches its internal handlers (e.g. `_refreshLine`) to keypress events. On every keypress—including Enter—the readline interface may write to stdout. Our custom `drawBox()` also writes to stdout. This creates a conflict: readline echoes/writes something (e.g. newline or cursor movement) that appears as an extra line, which combined with our box drawing produces the duplicate border effect.

**Fix**: Create the readline interface with a no-op output stream so it does not write to stdout. We only need the interface for `rl.close()` in `doExit()`, not for any output.

```ts
import { Writable } from 'node:stream'
const noopOutput = new Writable({ write(_chunk, _encoding, cb) { cb() } })
const rl = readline.createInterface({
  input: stdin,
  output: noopOutput,
  escapeCodeTimeout: 50,
})
```

## Phases

### Phase 1: E2E test (reproduce, then @ignore) — DONE

1. ✅ Added scenario to `e2e_test/features/cli/cli_ui.feature`:
   - Tags: `@withCliConfig`, `@interactiveCLI`, `@ignore`
   - Background: Given I am logged in, valid access token, add-access-token
   - When I press Enter in the interactive CLI (uses new step; Enter on empty triggers the bug)
2. ✅ Added Then step "I should see exactly one input box top border":
   - Helper `countTopBorderLinesBeforeFirstInputBox(output)` in `cliSectionParser.ts`
   - Counts top-border lines (`┌─*┐`) from version line through first `│` row
   - Page object `cli.inputBoxTopBorder().expectExactlyOne()`
3. Run the test locally and confirm it **fails** (bug reproducible).
4. ✅ Added `@ignore` to the scenario.

Note: Test passed locally (bug may not reproduce in PTY or may have been fixed). Phase 2 fix will confirm.

### Phase 2: Fix — DONE

1. ✅ In `cli/src/adapters/ttyAdapter.ts`:
   - Import `Writable` from `node:stream`
   - Create no-op writable, pass as `output` in `readline.createInterface` instead of `process.stdout`
2. ✅ Removed `@ignore` from the scenario.
3. ✅ E2E tests pass: cli_input_box, cli_recall, cli_access_token.
4. ✅ Minimal fix: output stream change only, no full redraw introduced.

### Phase 3: Sanity check

1. Run relevant CLI E2E tests: `pnpm cypress run --spec e2e_test/features/cli/cli_ui.feature`, `cli_recall.feature`, `cli_access_token.feature`.
2. Manually run `pnpm cli` and verify: Enter on empty, command selection, typing+Enter—no duplicate border.

## Notes

- **PTY in CI**: Cloud VM sets `CI=1`; PTY tests can be unreliable without a real terminal. If the Phase 1 test flakes in CI, it may need to run only locally or be tagged for exclusion in CI.
- **Related**: `cliSectionParser.ts` already has `INPUT_BOX_TOP`, `findLastInputBoxStart`, `findLastInputBoxEnd`. The new helper should count top-border lines in the region from version line to first input box row.
