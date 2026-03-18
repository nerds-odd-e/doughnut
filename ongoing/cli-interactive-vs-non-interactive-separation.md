# Plan: Separate Interactive vs Non-Interactive CLI Assertions and Behaviors

## Context

The CLI supports two modes:
- **Interactive mode**: TTY with PTY features (ESC, arrows, suggestions). Uses `runInteractive()` via `runTTY` or `runPiped`. Output has ANSI structure: history-input, history-output, current-prompt sections.
- **Non-interactive mode** (`-c "<command>"` or piped input): Single command, no PTY. Uses `processInput()` directly from `index.ts`. Output is plain stdout (version + command result).

Currently, E2E tests use `I should see X in the history output` for both modes. The `cliSectionParser` parses ANSI sections; for non-interactive output (no separators, no input box), the parser treats all lines as history-output. This works but conflates two distinct concepts.

Commands like `/recall` require multi-turn interaction (y/n, MCQ choices, spelling, load-more). In non-interactive mode, `-c "/recall"` would block waiting for input that never comes (stdin is closed or empty). Such commands should be rejected with a clear message.

## Principles

- **Separate assertion steps**: Interactive uses section parser (history-output, Current guidance). Non-interactive uses entire stdout as "command output".
- **Non-interactive = simple stdio**: No PTY. User confirmation (y/n) is not supported—commands that need it are unavailable.
- **Interactive-only commands**: Only `/recall` (multi-turn: y/n, MCQ, spelling, load-more). `/recall-status`, `/list-access-token`, etc. work in `-c`. In `-c` mode, interactive-only commands show a proper message.
- **Domain vocabulary**: Use consistent terms: "command output" (both modes), "history output" (interactive only, parsed section), "Current guidance" (interactive only).

## Phased Plan

### Phase 1: Product—reject interactive-only commands in `-c` mode ✅

**Scope**: `cli/src/index.ts`, `cli/src/help.ts`, `cli/src/recall.ts`

**Implemented**:
- `CommandDoc` has optional `interactiveOnly?: boolean`. Commands that need multi-turn interaction set `interactiveOnly: true` (e.g. `/recall` in recall.ts).
- `help.ts`: `isInteractiveOnlyCommand(cmd)`, `INTERACTIVE_ONLY_REJECTION_MESSAGE`, `interactiveOnlyUsages` derived from `interactiveDocs`.
- `index.ts`: Before `processInput`, if `isInteractiveOnlyCommand(trimmed)`, log message to stderr and exit 1.
- Unit test in `cli/tests/index.test.ts`: `-c "/recall" rejects interactive-only command with message and exits 1`.

**Deliverable**: Running `doughnut -c "/recall"` prints a clear message and exits non-zero. ✅

---

### Phase 2: Product—ensure non-interactive uses simple stdio ✅

**Scope**: `index.ts` already uses `processInput` with default output (console.log). The `-c` path does not use PTY. Verify:
- `runCliDirectWithArgs` and `runCliDirectWithInput` use `stdio: ['pipe','pipe','pipe']` — no TTY. ✓
- No changes needed if already correct.

**Verified** (e2e_test/config/common.ts):
- `runCliDirectWithArgs`: stdio `['pipe','pipe','pipe']` (line 458)
- `runCliDirectWithInput`: stdio `['pipe','pipe','pipe']` (line 399)
- `runInstalledCli` (no input path): stdio `['pipe','pipe','pipe']` (line 510)
- `runCliDirectWithGmailAdd`: stdio `['pipe','pipe','pipe']` (line 556)

---

### Phase 3: E2E—separate assertion steps

**Scope**: `e2e_test/step_definitions/cli.ts`, `e2e_test/step_definitions/cliSectionParser.ts`, feature files

1. **Add step for non-interactive**: `I should see {string} in the non-interactive output`  
   - Asserts the expected string is in the **entire** stdout (no section parsing). Use for: `-c`, piped input, installed CLI.
2. **Clarify step for interactive**: `I should see {string} in the history output`  
   - Asserts using `getSectionContent(output, 'history-output')`. Use only in `@interactiveCLI` scenarios.
3. **Rename or keep "CLI output"**: Currently `I should see {string} in the CLI output` asserts on raw output. Consider:
   - Use "command output" as the domain term for non-interactive.
   - Use "history output" for interactive (parsed section).
   - Deprecate "CLI output" in favor of "command output" for consistency, or keep "CLI output" as the raw/full output alias.
4. **Implementation**:
   - `I should see {string} in the non-interactive output` → `assertOutputIncludes(output, expected, 'non-interactive output')` (no section parser).
   - `I should see {string} in the history output` → keep current logic (section parser). Used only when `@doughnutOutput` comes from interactive CLI (sendToInteractiveCli).

5. **Context detection**: The step definitions receive `@doughnutOutput`. For non-interactive, output has no ANSI structure; `getSectionContent` would still return content (all lines go to history-output when no separator). So both steps could work on the same output. The distinction is **semantic**:
   - Scenarios using `I run the doughnut command with -c` or `I run the doughnut command with input` → use `I should see X in the non-interactive output`.
   - Scenarios using `I input X in the interactive CLI` → use `I should see X in the history output` or `I should see X in the Current guidance`.

6. **Migration**: Update feature files:
   - `cli_access_token.feature`: -c steps → "command output"; @interactiveCLI steps → "history output" or "CLI output" (ESC scenario uses "CLI output").
   - `cli_install_and_run.feature`: All use "command output" (no interactive).
   - `cli_gmail.feature`: Uses runCliDirectWithInput–like flows; use "command output".
   - `cli_recall.feature`: All @interactiveCLI; keep "history output" and "Current guidance".

---

### Phase 4: CLI unit tests for new behaviors ✅

**Scope**: `cli/tests/index.test.ts`, `cli/tests/help.test.ts`

**Done**:
- Unit test in `index.test.ts` — `-c "/recall"` rejects with message and exits 1.
- `isInteractiveOnlyCommand` unit tests in `help.test.ts`: `/recall` → true, `/recall-status` → false.

---

### Phase 5: Domain name consistency ✅

**Scope**: All CLI and E2E code

**Implemented**:
1. **Output vocabulary** (ubiquitous language):
   - **Command output**: Text produced by a command. Non-interactive: entire stdout. Interactive: same concept; we also have History output and Current guidance.
   - **History output**: Interactive only. Past command results (parsed section).
   - **Current guidance**: Interactive only. Prompts, hints, options for the current input (above input box: recall, MCQ, y/n; below: / commands, token list).

2. **Step text alignment**:
   - `I should see X in the non-interactive output` → non-interactive output
   - `I should see X in the history output` → history output (interactive)
   - `I should see X in the Current guidance` → Current guidance (interactive)

3. **Product/brand**: "doughnut" (lowercase) for CLI binary and commands. "Doughnut" for product display (e.g. Doughnut Access Token). Feature files follow this.

4. **Audit**: cli.mdc, cliSectionParser.ts, feature files, and step definitions use consistent terminology. No "CLI output" step; "command output" and "Current guidance" are the domain terms.

---

### Phase 6: Update cli.mdc rule

**Scope**: `.cursor/rules/cli.mdc`

1. Add section **"Interactive vs non-interactive mode"**:
   - Interactive: TTY or piped with `runInteractive`. Supports PTY features. Output has sections (history-output, Current guidance).
   - Non-interactive: `-c "<cmd>"` or piped single command. No PTY. Entire stdout is command output.
   - Commands requiring multi-turn interaction (e.g. `/recall`) are rejected in `-c` mode with a clear message.
2. Add section **"E2E assertion steps"**:
   - Interactive: `I should see X in the history output`, `I should see X in the Current guidance`
   - Non-interactive: `I should see X in the non-interactive output`
3. Update "CLI E2E tests" section to mention the separation and when to use each step.

---

## Summary

| Phase | Focus | Files | Status |
|-------|-------|-------|--------|
| 1 | Reject interactive-only commands in `-c` mode | `cli/src/index.ts`, `help.ts`, `recall.ts` | ✅ |
| 2 | Verify non-interactive uses stdio (no PTY) | e2e_test/config/common.ts | ✅ |
| 3 | Separate E2E steps: command output vs history output | `cli.ts`, `cliSectionParser.ts`, feature files | ✅ |
| 4 | CLI unit tests for validation and behaviors | `cli/tests/index.test.ts`, `help.test.ts` | ✅ |
| 5 | Domain vocabulary consistency | All CLI and E2E | ✅ |
| 6 | Update cli.mdc | `.cursor/rules/cli.mdc` | ✅ |

---

## Open Questions

1. **Other interactive-only commands?** `/remove-access-token` without label shows token selection. With `-c "/remove-access-token"` we get "Usage: /remove-access-token <label>". So no change. `/list-access-token` in -c just lists—no selection. Only `/recall` blocks.
2. **`/recall-status` in -c**: Single-shot, no confirmation. Keep available in -c.
