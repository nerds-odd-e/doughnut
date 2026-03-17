# Plan: Migrate CLI E2E Tests to Truly Interactive Mode

## Context

Currently, "interactive mode" CLI E2E tests use `runCliDirectWithInput` or `runCliDirectWithInputAndPty`: each When step spawns a **new** CLI process, pipes the full input (including `exit`), and captures stdout. The CLI is never truly interactive—it receives all input at once and exits.

The goal: scenarios that need interactive mode should run a **single live CLI process** for the whole scenario. A `@interactiveCLI` tag triggers a hook that starts the CLI in the Before and exits it in the After. Steps then send input to and read output from this live process.

## Current vs. Desired Behavior

| Aspect | Current | Desired |
|--------|---------|---------|
| CLI lifecycle | One process per When step | One process per scenario |
| Input | Piped all at once with `exit` | Sent incrementally; `exit` only in After |
| Output | Full stdout when process ends | Read after each input until prompt appears |
| PTY | Some steps use PTY (ESC, arrows) | Use PTY for all interactive (key handling) |

## Hook Ordering

- `@interactiveCLI` hook **must run after** `@withCliConfig`.
- Scenarios with `@interactiveCLI` typically also need `@withCliConfig` for the temp config dir.
- Use the `order` parameter (badeball/cypress-cucumber-preprocessor): add `order: 1` to the existing `@withCliConfig` Before hook, and `Before({ tags: '@interactiveCLI', order: 2 })` so config runs first.

## Phased Plan

### Phase 1: Add interactive CLI infrastructure

**Scope**: Cypress tasks and hook, no step changes.

1. **Cypress tasks** (in `e2e_test/config/common.ts`):
   - `startInteractiveCli({ env })`: Spawn CLI in PTY, wait for prompt (`/ commands/`), store process ref in module-level state. Return when ready.
   - `sendToInteractiveCli({ input })`: Write input to the live PTY. Read stdout until prompt appears again (or timeout). Return the captured output for that round.
   - `stopInteractiveCli()`: Send `exit\n` (or `/exit\n`), wait for process end, clear state.

2. **Hook** (in `e2e_test/step_definitions/hook.ts`):
   - Add `order: 1` to the existing `Before({ tags: '@withCliConfig' })` so it runs first.
   - `Before({ tags: '@interactiveCLI', order: 2 })`: Call `cy.task('startInteractiveCli', { env })` with config from `@cliConfigDir`.
   - `After({ tags: '@interactiveCLI' })`: Call `cy.task('stopInteractiveCli')`.

3. **Prompt detection**: Use existing `CLI_READY_PATTERN` from `cliPtyRunner.ts`. For "output complete" after sending input, wait for the same prompt to reappear (or a variant like `> ` if the CLI uses that).

**Deliverable**: Infrastructure exists; no scenarios use it yet.

---

### Phase 2: Tag scenarios and update step semantics

**Scope**: Add `@interactiveCLI` to scenarios; change steps to use live CLI when the tag is present.

1. **Identify interactive scenarios**:
   - `cli_recall.feature`: All scenarios (they use "I run the doughnut command in interactive mode ..." or "I run a recall session ..."). Add `@interactiveCLI` to each.
   - `cli_access_token.feature`: Scenarios that use `runCliDirectWithInput` or `runCliDirectWithInputAndPty`:
     - "ESC cancels remove-access-token selection" (uses PTY for ESC)
     - Others use `-c` (non-interactive) and do not need `@interactiveCLI`.

2. **Step definitions** (in `e2e_test/step_definitions/cli.ts`):
   - For steps that currently call `runCliDirectWithInput` or `runCliDirectWithInputAndPty`, add a **conditional path**: if the scenario has `@interactiveCLI`, call `cy.task('sendToInteractiveCli', { input })` and alias result to `@doughnutOutput`. Otherwise keep current behavior (for scenarios without the tag).

   - Cucumber does not expose "does this scenario have tag X?" easily in step defs. **Alternative**: always check for running interactive CLI. If `startInteractiveCli` was called (i.e. scenario has the tag), `sendToInteractiveCli` will use it. Steps can always try `sendToInteractiveCli` when the step is an "interactive mode" step—it will fail if no CLI is running. Simpler: make the interactive steps **only** work with `@interactiveCLI`; scenarios without it use different steps (e.g. `-c` for single commands). So:
     - Scenarios with `@interactiveCLI`: use steps that call `sendToInteractiveCli`.
     - Scenarios without: keep using `runCliDirectWithInput` / `-c` for non-interactive flows.

   - **Recommended**: All steps that say "interactive mode" require `@interactiveCLI`. The step def calls `sendToInteractiveCli`. If the tag is missing, the Before hook never runs, so `sendToInteractiveCli` will throw "no interactive CLI running". That's acceptable—the tag is required. No conditional logic in steps.

3. **Input formatting**: Current steps append `exit` in the step. With interactive CLI, steps must **not** send `exit`—the After hook does that. So each step should send only the user input (e.g. `/recall-status\n`, `/recall\ny\n`, etc.) without `exit`.

**Deliverable**: Tagged scenarios use live CLI; step definitions send input without `exit`.

---

### Phase 3: Multi-step scenarios and output boundaries

**Scope**: Scenarios with multiple When steps sharing one CLI; robust prompt-based output capture.

1. **Output boundary**: After sending input, read until:
   - The prompt pattern (`/ commands/` or equivalent) reappears, **or**
   - A timeout (e.g. 25s like current PTY timeout).

2. **Multi-step examples**:
   - Scenario "Recall status shows zero after recalling..." has:
     - When `/recall` + `y` → Then assertions
     - When `/recall-status` → Then assertions
   - First When: send `/recall\n`, read until prompt (or "y/n" prompt). Then send `y\n`, read until main prompt.
   - Actually the step is "with input `/recall` and `y`"—so one step, two logical inputs. The step sends `/recall\ny\n` in one go. The `sendToInteractiveCli` just sends that and reads until the main prompt. Same as now, but without spawning a new process.

   - For "When ... then When ...": First When sends its input, we capture output. Second When sends its input, we capture new output. Each capture overwrites `@doughnutOutput`. Then steps read from current `@doughnutOutput`. Works.

3. **Compound steps** (e.g. "I run a recall session and recall all due notes, declining load more"): Input is `/recall\ny\ny\nn\nexit` → change to `/recall\ny\ny\nn\n` (no exit). Single `sendToInteractiveCli` call. Output captured. Good.

**Deliverable**: Multi-step scenarios work; output capture is deterministic.

---

### Phase 4: PTY-only for interactive (ESC, arrows)

**Scope**: Ensure all `@interactiveCLI` flows use PTY so ESC and arrow keys work.

1. `startInteractiveCli` already uses PTY (via `runCliInPty` or equivalent).
2. Step "I run the remove-access-token command and cancel with ESC, then list tokens": input is `/remove-access-token\n\x1b\n/list-access-token\n`. No `exit`. The ESC (`\x1b`) requires PTY. With Phase 1–3, this already uses the interactive CLI which is PTY-based.
3. Step "I run the doughnut command in interactive mode with down-arrow selection for /recall": currently sends `/recall\n2\n` (uses "2" as shortcut for second option). If we need real arrow keys, we'd send `\x1b[B` (down) etc. Verify whether current tests use numeric shortcuts or real arrows—if numeric, no change. If arrows, ensure PTY sends them correctly.

**Deliverable**: ESC and arrow-key scenarios pass with live interactive CLI.

---

### Phase 5: Gmail scenarios (optional / separate)

**Scope**: `cli_gmail.feature` uses `runCliDirectWithGmailAdd` and `runCliDirectWithLastEmail`, which create their own temp config dir (not `@withCliConfig`).

- These are one-shot: `/add gmail\nexit`, `/last email\nexit`.
- They could use `@interactiveCLI` only if we extend the hook to support a different config source (e.g. Gmail-specific config). Lower priority.
- **Recommendation**: Leave Gmail scenarios as-is for this migration unless we explicitly want them on the interactive CLI. Document as future work.

**Deliverable**: Gmail scenarios unchanged, or a separate phase if we migrate them.

---

### Phase 6: Cleanup and verification

1. Remove or simplify `runCliDirectWithInput` usage for interactive scenarios—ensure no scenario both has `@interactiveCLI` and uses the old pipe-based flow.
2. Run full CLI E2E: `pnpm cypress run --spec "e2e_test/features/cli/*.feature"`.
3. Update `.cursor/rules/cli.mdc` if it documents the E2E approach.
4. Remove dead code: any wrapper that was only for the old flow.

**Deliverable**: All CLI E2E pass; docs updated; no duplicate logic.

---

## Summary of Changes

| File | Changes |
|------|---------|
| `e2e_test/config/common.ts` | Add `startInteractiveCli`, `sendToInteractiveCli`, `stopInteractiveCli` tasks |
| `e2e_test/step_definitions/hook.ts` | Add Before/After for `@interactiveCLI` (after `@withCliConfig`) |
| `e2e_test/step_definitions/cli.ts` | Update "interactive mode" steps to use `sendToInteractiveCli`, remove `exit` from input |
| `e2e_test/features/cli/cli_recall.feature` | Add `@interactiveCLI` to each scenario |
| `e2e_test/features/cli/cli_access_token.feature` | Add `@interactiveCLI` to "ESC cancels remove-access-token selection" |

## Open Questions

1. **Prompt stability**: Is `/ commands/` the right pattern for "ready for next input"? Or does the CLI vary prompts (e.g. `> ` for main, different for sub-menus)? Verify in `cli/src/` output.
2. **CI vs local**: `runCliDirectWithInput` uses stdio pipe; PTY may behave differently in CI. Validate on GitHub Actions.
