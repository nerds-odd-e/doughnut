# Plan: Migrate CLI E2E Tests to Truly Interactive Mode

## Context

Currently, "interactive mode" CLI E2E tests use `runCliDirectWithInput` or `runCliDirectWithInputAndPty`: each When step spawns a **new** CLI process, pipes the full input (including `exit`), and captures stdout. The CLI is never truly interactive—it receives all input at once and exits.

The goal: scenarios that need interactive mode should run a **single live CLI process** for the whole scenario. A `@interactiveCLI` tag triggers a hook that starts the CLI in the Before and exits it in the After. Steps then send input to and read output from this live process.

## Principles

- **Incremental**: Each phase converts one scenario type to interactive mode; the next phase adds another type.
- **Minimum work per phase**: Only add what's needed for that phase's scenarios to pass.
- **Step text migration**: Gradually migrate `I run the doughnut command in interactive mode with input ...` to `I input ... in the interactive CLI` in each phase.
- **Redundancy allowed**: Interim phases may have both old and new step definitions; feature files can use either. Remove dead step defs in Phase 5.
- **Remove dead code immediately**: After each phase, remove any code that becomes unused (except old step defs—see Phase 5).
- **Cohesive**: No duplication; one representation per concept (after Phase 5 cleanup).
- **No if conditions in test code**: Step definitions call `sendToInteractiveCli` directly; scenarios using them must have `@interactiveCLI`. No branching on "is interactive or not."

## Current vs. Desired Behavior

| Aspect | Current | Desired |
|--------|---------|---------|
| CLI lifecycle | One process per When step | One process per scenario |
| Input | Piped all at once with `exit` | Sent incrementally; `exit` only in After |
| Output | Full stdout when process ends | Read after each input until prompt appears |
| PTY | Some steps use PTY (ESC, arrows) | Use PTY for all interactive (key handling) |

## Hook Ordering

- `@interactiveCLI` hook **must run after** `@withCliConfig`.
- Use `order: 1` for `@withCliConfig` and `order: 2` for `@interactiveCLI` (badeball/cypress-cucumber-preprocessor).

---

## Phased Plan

### Phase 1: Single-step, single input

**Scenario type**: One When step with a single input (e.g. `/recall-status`).

**Scenarios**: "Recall status shows count when notes are due"

**Work**:

1. Add Cypress tasks: `startInteractiveCli`, `sendToInteractiveCli`, `stopInteractiveCli` (PTY-based, prompt detection).
2. Add `@interactiveCLI` Before/After hooks (order after `@withCliConfig`).
3. Add `@interactiveCLI` to the 1 scenarios.
4. Add step `I input {string} in the interactive CLI` that calls `sendToInteractiveCli` with input (no `exit`).
5. Update the 1 scenario to use `I input "/recall-status" in the interactive CLI`.
6. Keep old step `I run the doughnut command in interactive mode with input {string}` for now (*redundancy allowed*).

**Dead code**: None—old step kept (remove in Phase 5).

---

### Phase 2: Single-step, command + answer(s)

**Scenario type**: One When step with command plus one or more answers (e.g. `/recall` and `y`).

**Scenarios**: "Recall Just Review" outline, "Recall MCQ - choose correct answer", "Recall spelling - type correct spelling", "Recall substate - /stop exits recall mode", "Recall MCQ - contest and regenerate", "Recall MCQ - down arrow and Enter to select".

**Work**:

1. Add new steps that call `sendToInteractiveCli` (no `exit`):
   - `I input {string} and {string} in the interactive CLI`
   - `I input {string} and {string} and {string} in the interactive CLI`
   - `I input {string} and {string} and {string} and {string} in the interactive CLI`
   - `I input down-arrow selection for {string} in the interactive CLI` (sends `/recall\n2\n`)
2. Update these scenarios to use the new step text (e.g. `I input "/recall" and "y" in the interactive CLI`).
3. Add `@interactiveCLI` to these scenarios.
4. Keep old step defs for now (*redundancy allowed*).

**Dead code**: None—remove old step defs in Phase 5.

---

### Phase 3: Multi-step scenarios

**Scenario type**: Multiple When steps in one scenario sharing one CLI process.

**Scenarios**: "Recall status shows zero after recalling the only note in session", "Recall MCQ - ESC cancels with y/n confirmation", "Recall session - complete all due notes, see summary, then load more from future days".

**Work**:

1. Ensure `sendToInteractiveCli` returns output per call; each When overwrites `@doughnutOutput`. Verify prompt-based output boundaries work for sequential sends.
2. Add new steps that call `sendToInteractiveCli`:
   - `I input recall MCQ cancel with ESC in the interactive CLI` → `/recall\n/stop\n`
   - `I input a recall session declining load more in the interactive CLI` → `/recall\ny\ny\nn\n`
   - `I input a recall session with load more from future days in the interactive CLI` → `/recall\ny\ny\ny\ny\n`
3. Update these 3 scenarios to use the new step text. For the multi-step scenario "Recall status shows zero after recalling...", first When becomes `I input "/recall" and "y" in the interactive CLI`, second When becomes `I input "/recall-status" in the interactive CLI`.
4. Add `@interactiveCLI` to these 3 scenarios.
5. Keep old step defs for now (*redundancy allowed*).

**Dead code**: None—remove old step defs in Phase 5.

---

### Phase 4: PTY scenario (ESC key)

**Scenario type**: Requires PTY for ESC key handling.

**Scenarios**: "ESC cancels remove-access-token selection" (in `cli_access_token.feature`).

**Work**:

1. Add step `I input remove-access-token, ESC, then list-access-token in the interactive CLI` that calls `sendToInteractiveCli` with input `/remove-access-token\n\x1b\n/list-access-token\n` (no `exit`).
2. Update the scenario to use the new step text.
3. Add `@interactiveCLI` to this scenario.
4. Remove `runCliDirectWithInputAndPty` task (no longer used).
5. Keep old step def for now (*redundancy allowed*).

**Dead code**: `runCliDirectWithInputAndPty`—remove in this phase. Old step def—remove in Phase 5.

---

### Phase 5: Final cleanup and docs

**Work**:

1. **Remove dead step definitions**: Remove all old `I run the doughnut command in interactive mode...` / `I run a recall session...` / `I run the remove-access-token...` step defs that were kept for redundancy. All scenarios now use `I input ... in the interactive CLI`.
2. Run full CLI E2E: `pnpm cypress run --spec "e2e_test/features/cli/*.feature"`.
3. Update `.cursor/rules/cli.mdc` or `CLAUDE.md` if they document the E2E approach.
4. Remove any other dead code (e.g. helpers that only served old flow).

**Note**: `runCliDirectWithInput` stays—it is used by `I run the doughnut command with input {string}` in `cli_install_and_run.feature` (non-interactive flow).

---

### Out of scope: Gmail scenarios

`cli_gmail.feature` uses `runCliDirectWithGmailAdd` and `runCliDirectWithLastEmail` with their own config setup. Leave as-is; document as future work if needed.

---

## Summary of Changes by Phase

| Phase | Files | Changes |
|-------|-------|---------|
| 1 | `common.ts`, `hook.ts`, `cli.ts`, `cli_recall.feature` | Tasks, hooks, add `I input {string} in the interactive CLI`, migrate 2 scenarios |
| 2 | `cli.ts`, `cli_recall.feature` | Add 4 new `I input ...` steps, migrate 6 scenarios |
| 3 | `cli.ts`, `cli_recall.feature` | Add 3 new `I input ...` steps, migrate 3 multi-step scenarios |
| 4 | `cli.ts`, `cli_access_token.feature`, `common.ts` | Add 1 new step, migrate 1 scenario, remove `runCliDirectWithInputAndPty` |
| 5 | `cli.ts`, docs | **Remove dead code**: all old `I run the doughnut command...` step defs; verification and doc updates |

## Phase 1 Implementation Notes

- **Command submission**: When the CLI buffer is a command prefix with suggestions (e.g. `/recall-status` without trailing space), Enter selects the suggestion (adds space) instead of submitting. `sendToInteractiveCli` appends a trailing space before newline for command-like input so submission runs.
- **Prompt detection**: Before hook waits for `/ commands` in stdout; `sendToInteractiveCli` waits for the pattern in *new* content (after lenBeforeSend) to avoid matching the initial display.

## Open Questions

1. **CI vs local**: PTY may behave differently in CI. Validate on GitHub Actions.
