# Plan: Separate Interactive vs Non-Interactive CLI Assertions and Behaviors

## Context

The CLI supports two modes:
- **Interactive mode**: TTY with PTY features (ESC, arrows, suggestions). Uses `runInteractive()` via `runTTY` or `runPiped`. Output has ANSI structure: history-input, history-output, current-prompt sections.
- **Non-interactive mode** (`-c "<command>"` or piped input): Single command, no PTY. Uses `processInput()` directly from `index.ts`. Output is plain stdout (version + command result).

Currently, E2E tests use `I should see X in the history output` for both modes. The `cliSectionParser` parses ANSI sections; for non-interactive output (no separators, no input box), the parser treats all lines as history-output. This works but conflates two distinct concepts.

Commands like `/recall` require multi-turn interaction (y/n, MCQ choices, spelling, load-more). In non-interactive mode, `-c "/recall"` would block waiting for input that never comes (stdin is closed or empty). Such commands should be rejected with a clear message.

## Principles

- **Separate assertion steps**: Interactive uses section parser (history-output, current-prompt). Non-interactive uses entire stdout as "command output".
- **Non-interactive = simple stdio**: No PTY. User confirmation (y/n) is not supported—commands that need it are unavailable.
- **Interactive-only commands**: `/recall`, `/recall-status` (and possibly `/list-access-token` when it shows selection UI) require interactive mode. In `-c` mode, show a proper message.
- **Domain vocabulary**: Use consistent terms: "command output" (both modes), "history output" (interactive only, parsed section), "current prompt" (interactive only).

## Phased Plan

### Phase 1: Product—reject interactive-only commands in `-c` mode

**Scope**: `cli/src/index.ts`, `cli/src/interactive.ts`

1. Add validation: the only interactive-only command is `/recall` (multi-turn: y/n, MCQ, spelling, load-more). `/recall-status`, `/list-access-token`, `/remove-access-token <label>` work in -c (single output or usage).
2. In `index.ts` when handling `-c`, before calling `processInput`:
   - If `trimmed === '/recall'`, log a message like "This command requires interactive mode. Run `doughnut` without -c." to stderr, and exit 1.
5. Implement: in `index.ts`, if the trimmed command is exactly `/recall` (not `/recall-status`), reject with message. Use: `const trimmed = value.trim(); if (trimmed === '/recall') { ... }`.

**Deliverable**: Running `doughnut -c "/recall"` prints a clear message and exits non-zero.

---

### Phase 2: Product—ensure non-interactive uses simple stdio

**Scope**: `index.ts` already uses `processInput` with default output (console.log). The `-c` path does not use PTY. Verify:
- `runCliDirectWithArgs` and `runCliDirectWithInput` use `stdio: ['pipe','pipe','pipe']` — no TTY. ✓
- No changes needed if already correct.

---

### Phase 3: E2E—separate assertion steps

**Scope**: `e2e_test/step_definitions/cli.ts`, `e2e_test/step_definitions/cliSectionParser.ts`, feature files

1. **Add step for non-interactive**: `I should see {string} in the command output`  
   - Asserts the expected string is in the **entire** stdout (no section parsing). Use for: `-c`, piped input, installed CLI.
2. **Clarify step for interactive**: `I should see {string} in the history output`  
   - Asserts using `getSectionContent(output, 'history-output')`. Use only in `@interactiveCLI` scenarios.
3. **Rename or keep "CLI output"**: Currently `I should see {string} in the CLI output` asserts on raw output. Consider:
   - Use "command output" as the domain term for non-interactive.
   - Use "history output" for interactive (parsed section).
   - Deprecate "CLI output" in favor of "command output" for consistency, or keep "CLI output" as the raw/full output alias.
4. **Implementation**:
   - `I should see {string} in the command output` → `assertOutputIncludes(output, expected, 'command output')` (no section parser).
   - `I should see {string} in the history output` → keep current logic (section parser). Used only when `@doughnutOutput` comes from interactive CLI (sendToInteractiveCli).

5. **Context detection**: The step definitions receive `@doughnutOutput`. For non-interactive, output has no ANSI structure; `getSectionContent` would still return content (all lines go to history-output when no separator). So both steps could work on the same output. The distinction is **semantic**:
   - Scenarios using `I run the doughnut command with -c` or `I run the doughnut command with input` → use `I should see X in the command output`.
   - Scenarios using `I input X in the interactive CLI` → use `I should see X in the history output` or `I should see X in the current prompt`.

6. **Migration**: Update feature files:
   - `cli_access_token.feature`: -c steps → "command output"; @interactiveCLI steps → "history output" or "CLI output" (ESC scenario uses "CLI output").
   - `cli_install_and_run.feature`: All use "command output" (no interactive).
   - `cli_gmail.feature`: Uses runCliDirectWithInput–like flows; use "command output".
   - `cli_recall.feature`: All @interactiveCLI; keep "history output" and "current prompt".

---

### Phase 4: E2E—add scenario for `/recall` rejected in `-c` mode

**Scope**: `e2e_test/features/cli/cli_recall.feature`, `e2e_test/config/common.ts`

1. **Handle non-zero exit**: The product will exit 1 and may print the message to stderr. Current `runCliDirectWithArgs` rejects on non-zero. Add:
   - Step `I run the doughnut command with -c "{string}" expecting failure` that uses a task with `allowNonZeroExit: true`, capturing both stdout and stderr.
   - Store combined output (stdout + stderr) in `@doughnutOutput` so "I should see X in the command output" can assert on the error message.
2. Add scenario (no @interactiveCLI):
   ```gherkin
   @disableOpenAiService
   Scenario: /recall in -c mode shows proper message
     Given I have a notebook with the head note "English"
     When I run the doughnut command with -c "/recall" expecting failure
     Then I should see "interactive" in the command output
     And I should not see "Load more" in the command output
   ```
3. Adjust expected message to match product (e.g. "requires interactive mode" or similar).

---

### Phase 5: CLI unit tests for new behaviors

**Scope**: `cli/tests/interactive.test.ts` or new `cli/tests/nonInteractive.test.ts`

1. **`-c` mode rejects `/recall`**:
   - Cannot easily test `index.ts` without spawning. Alternative: export a function `validateCommandForNonInteractive(cmd: string): string | null` that returns error message or null. Test that `validateCommandForNonInteractive('/recall')` returns a non-null message, and `validateCommandForNonInteractive('/list-access-token')` returns null.
2. **Non-interactive command output**: Unit test `processInput` for `/recall-status` returns message (already covered). For rejection, test the validation helper.
3. Add `cli/tests/nonInteractive.test.ts` or extend `interactive.test.ts` with a `describe('non-interactive mode -c')` that tests the validation logic.

---

### Phase 6: Domain name consistency

**Scope**: All CLI and E2E code

1. **Output vocabulary** (ubiquitous language):
   - **Command output**: The text produced by a command. In non-interactive mode, this is the entire stdout. In interactive mode, it is the same concept but we also have "history output" (scrollable past command outputs) and "current prompt" (active prompt/hints).
   - **History output**: Interactive only. The section of display containing past command outputs (and current command result before next input).
   - **Current prompt**: Interactive only. The section containing prompts like "Yes, I remember?", "Load more from next 3 days?", etc.

2. **Step text alignment**:
   - `I should see X in the command output` → command output
   - `I should see X in the history output` → history output (interactive)
   - `I should see X in the current prompt` → current prompt (interactive)

3. **Product/brand**: Use "doughnut" (lowercase) for CLI binary and commands. "Doughnut" for UI display where appropriate. Ensure tests use consistent casing.

4. **Audit**: Grep for "history output", "CLI output", "command output", "current prompt" and align naming in comments, step definitions, and feature files.

---

### Phase 7: Update cli.mdc rule

**Scope**: `.cursor/rules/cli.mdc`

1. Add section **"Interactive vs non-interactive mode"**:
   - Interactive: TTY or piped with `runInteractive`. Supports PTY features. Output has sections (history-output, current-prompt).
   - Non-interactive: `-c "<cmd>"` or piped single command. No PTY. Entire stdout is command output.
   - Commands requiring multi-turn interaction (e.g. `/recall`) are rejected in `-c` mode with a clear message.
2. Add section **"E2E assertion steps"**:
   - Interactive: `I should see X in the history output`, `I should see X in the current prompt`
   - Non-interactive: `I should see X in the command output`
3. Update "CLI E2E tests" section to mention the separation and when to use each step.

---

## Summary

| Phase | Focus | Files |
|-------|-------|-------|
| 1 | Reject `/recall` in `-c` mode | `cli/src/index.ts` |
| 2 | Verify non-interactive uses stdio (no PTY) | (verification only) |
| 3 | Separate E2E steps: command output vs history output | `cli.ts`, `cliSectionParser.ts`, feature files |
| 4 | E2E scenario for `/recall` rejected in `-c` | `cli_recall.feature` |
| 5 | CLI unit tests for validation and behaviors | `cli/tests/*.test.ts` |
| 6 | Domain vocabulary consistency | All CLI and E2E |
| 7 | Update cli.mdc | `.cursor/rules/cli.mdc` |

---

## Open Questions

1. **Other interactive-only commands?** `/remove-access-token` without label shows token selection. With `-c "/remove-access-token"` we get "Usage: /remove-access-token <label>". So no change. `/list-access-token` in -c just lists—no selection. Only `/recall` blocks.
2. **`/recall-status` in -c**: Single-shot, no confirmation. Keep available in -c.
