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

### Phase 1: Single-step, single input ✅ Done

**Scenario type**: One When step with a single input (e.g. `/recall-status`).

**Scenarios**: "Recall status shows count when notes are due"

**Work**:

1. Add Cypress tasks: `startInteractiveCli`, `sendToInteractiveCli`, `stopInteractiveCli` (PTY-based, prompt detection).
2. Add `@interactiveCLI` Before/After hooks (order after `@withCliConfig`).
3. Add `@interactiveCLI` to the 1 scenario.
4. Add step `I input {string} in the interactive CLI` that calls `sendToInteractiveCli` with input (no `exit`).
5. Update the 1 scenario to use `I input "/recall-status" in the interactive CLI`.
6. Keep old step `I run the doughnut command in interactive mode with input {string}` for now (*redundancy allowed*).

**Dead code**: None—old step kept (remove in Phase 5).

---

### Phase 1.5: Recall Just Review—split command and answer ✅ Done

**Scenario type**: One scenario with command + answer; split into two When steps so input "y" is sent *after* verifying "Yes, I remember?" appears (truly interactive).

**Scenarios**: "Recall Just Review" outline only.

**Work**:

1. Add `@interactiveCLI` to the scenario.
2. Split the single When into two:
   - `When I input "/recall" in the interactive CLI`
   - `Then I should see "<title>" in the current prompt` ... `And I should see "Yes, I remember?" in the current prompt`
   - `When I input "y" in the interactive CLI`
   - `Then I should see "Recalled successfully" in the history output`
3. Reuse existing step `I input {string} in the interactive CLI` (no new step def needed).
4. Keep old step def for now (*redundancy allowed*).

**Dead code**: None—remove old step def in Phase 5.

---

### Phase 1.6: Recall MCQ—choose correct answer ✅ Done

**Scenario type**: One When step with command plus MCQ answer; split so `/recall` is sent first, Then verify question appears, then When `1` (choice number).

**Scenarios**: "Recall MCQ - choose correct answer and see success" only.

**Principle**: Same as Phase 1.5—split composite inputs into separate When steps; interleave with Then steps that verify we're at the right prompt before sending the next input.

**Work**:

1. Add `@interactiveCLI` to the scenario.
2. Split the single When into two:
   - `When I input "/recall" in the interactive CLI`
   - `Then I should see "What is the meaning of sedition?" in the current prompt` ... `And I should see "to incite violence" in the current prompt`
   - `When I input "1" in the interactive CLI`
   - `Then I should see "Correct!" in the history output` ... `And I should see "Recalled successfully" in the history output`
3. Reuse existing step `I input {string} in the interactive CLI` (no new step def needed).
4. Keep old step def for now (*redundancy allowed*).

**Dead code**: None—remove old step def in Phase 5.

---

### Phase 2: Command + answer(s)—split into separate steps ✅ Done

**Scenario type**: One When step with command plus one or more answers; split so each input is sent only *after* a Then step verifies the expected prompt.

**Scenarios**: "Recall spelling - type correct spelling", "Recall substate - /stop exits recall mode", "Recall MCQ - contest and regenerate", "Recall MCQ - down arrow and Enter to select".

**Principle**: Same as Phase 1.5—split composite inputs into separate When steps; interleave with Then steps that verify we're at the right prompt before sending the next input.

**Work**:

1. Add step `I input down-arrow selection for {string} in the interactive CLI` (sends `/recall\n2\n`) for the arrow-key scenario.
2. Update each scenario: split inputs into separate When steps, with Then steps in between (e.g. `/recall` → Then verify question → When `1` → Then verify success).
3. Add `@interactiveCLI` to these scenarios.
4. Keep old step defs for now (*redundancy allowed*).

**Dead code**: None—remove old step defs in Phase 5.

---

### Phase 3: Multi-step scenarios ✅ Done

**Scenario type**: Multiple When steps in one scenario sharing one CLI process.

**Scenarios**: "Recall MCQ - ESC cancels with y/n confirmation", "Recall session - complete all due notes, see summary, then load more from future days".

**Principle**: Same as Phase 1.5/2—split composite inputs into separate When steps; interleave with Then steps that verify the expected prompt before sending the next input.

**Work**:

1. Ensure `sendToInteractiveCli` returns output per call; each When overwrites `@doughnutOutput`. (Already works—sequential sends accumulate stdout.)
2. Update "Recall MCQ - ESC cancels": When `/recall` → Then question → When `/stop` → Then stopped → When `/recall-status` → Then "1 note". Split into separate steps.
3. Update "Recall session - complete all due...": Split `/recall`, `y`, `y`, `n` and `/recall`, `y`, `y`, `y`, `y` into separate When steps with Then verification in between.
4. Add `@interactiveCLI` to these 2 scenarios.
5. Keep old step defs for now (*redundancy allowed*).

**Dead code**: None—remove old step defs in Phase 5.

---

### Phase 4: PTY scenario (ESC key) ✅ Done

**Scenario type**: Requires PTY for ESC key handling.

**Scenarios**: "ESC cancels remove-access-token selection" (in `cli_access_token.feature`).

**Principle**: Same as earlier phases—split into separate steps: When `/remove-access-token` → Then verify selection prompt → When ESC → Then verify cancelled → When `/list-access-token` → Then verify result.

**Work**:

1. Add step `I press ESC in the interactive CLI` that sends `\x1b` (or equivalent) via `sendToInteractiveCli`.
2. Update the scenario: split into When `/remove-access-token` → Then verify prompt → When `I press ESC` → Then verify cancelled → When `/list-access-token` → Then verify.
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
| 1 ✅ | `common.ts`, `hook.ts`, `cli.ts`, `cli_recall.feature`, `cliPtyRunner.ts`, `constants.ts`, `backendUrl.ts`, `ci.ts`, `mcpAgentActions.ts` | Tasks, hooks, step, migrate 1 scenario; single source for backend URL; `cliEnvWithConfigDir` helper |
| 1.5 ✅ | `cli_recall.feature`, `cliPtyRunner.ts` | Migrate "Recall Just Review"; split `/recall` and `y` into 2 When steps; use input box (│ → ) as ready signal with 10ms poll and 100ms stabilization |
| 1.6 ✅ | `cli_recall.feature` | Migrate "Recall MCQ - choose correct answer and see success"; split `/recall` and `1` into 2 When steps; reuse existing step |
| 2 ✅ | `cli.ts`, `cli_recall.feature` | Add down-arrow step; migrate 4 remaining scenarios (Recall spelling, /stop, contest, down-arrow); split inputs into separate When steps |
| 3 ✅ | `cli_recall.feature` | Migrate 2 multi-step scenarios (ESC cancels, complete all due); split composite inputs into separate When steps with Then verification in between |
| 4 ✅ | `cli.ts`, `cli_access_token.feature`, `common.ts`, `cliPtyRunner.ts` | Split remove-access-token + ESC + list into separate steps; migrate 1 scenario; remove `runCliDirectWithInputAndPty` |
| 5 | `cli.ts`, docs | **Remove dead code**: all old `I run the doughnut command...` step defs; verification and doc updates |

## Phase 1 Implementation Notes

- **Command submission**: When the CLI buffer is a command prefix with suggestions (e.g. `/recall-status` without trailing space), Enter selects the suggestion (adds space) instead of submitting. `sendToInteractiveCli` appends a trailing space before newline for command-like input so submission runs.
- **Prompt detection**: Before hook waits for `/ commands` in stdout; `sendToInteractiveCli` waits for the pattern in *new* content (after lenBeforeSend) to avoid matching the initial display.
- **Phase 1.5 prompt fix**: Use input box (│ → ) as the ready signal—the CLI only draws it when ready for input (drawBox runs after processInput returns). During command execution, no input box. Stabilization (100ms) avoids matching during typing. Poll at 10ms for faster detection. Simpler than pattern-per-substate.
- **Backend URL**: Single source `E2E_BACKEND_BASE_URL` in `e2e_test/config/constants.ts`; `backendBaseUrl()` in `e2e_test/support/backendUrl.ts` for runtime (config or fallback); hook and step defs use it, no hardcoding.
- **CLI env**: `cliEnvWithConfigDir(configDir)` in `cli.ts` centralizes `{ DOUGHNUT_CONFIG_DIR, DOUGHNUT_API_BASE_URL }` for all CLI steps.

## Open Questions

1. **CI vs local**: PTY may behave differently in CI. Validate on GitHub Actions.
