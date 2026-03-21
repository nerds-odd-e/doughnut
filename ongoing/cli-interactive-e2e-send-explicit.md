# Plan: Explicit interactive CLI send API and steps (no execution yet)

## Context

**Before Phases 1–3:** One Cypress task and `interactive().input` used a single string with heuristics (`trim()`, slash vs line, etc.), which hid trailing-space behavior and mixed intents.

**Now:** Interactive PTY writes use a single Cypress task `applyInteractiveCliPtyKeystroke` with a typed `InteractiveCliPtyKeystroke` union (`interactiveCliPtyTypes.ts`). Node-side `applyInteractiveCliPtyKeystroke` in `cliPtyRunner.ts` encodes bytes and waits for the ready OSC. Gherkin uses explicit steps (slash vs line vs Enter vs ESC).

## Correction: non-`/` input already exists in E2E

Interactive recall and MCQ flows already send lines that do not start with `/`: e.g. `y`, `n`, `1`, `2`, `sedition` in `cli_recall.feature`, plus `answerToPrompt` steps. Those are **supported** line input, not slash commands.

The gap vs `cli_non_interactive_mode.feature` is an explicit **unsupported plain line** case (e.g. `hello` → **Not supported**) under **TTY / `@interactiveCLI`**, mirroring the piped and `-c` scenarios.

## Target shape

### Low level (`cliPtyRunner.ts` + `interactiveCliPtyTypes.ts`)

- **`InteractiveCliPtyKeystroke`** — discriminated union (slash command line, one line + Enter, bare Enter, ESC). Encoding lives in one `switch` next to `pty.write`.
- **`applyInteractiveCliPtyKeystroke`** — encode keystroke → bytes, write PTY, wait for ready OSC, assert no escape leaks, return stdout.
- Optional future **raw** payload task only if a scenario needs bytes that do not fit the union.

### Cypress tasks (`cliE2ePluginTasks.ts`, merged in `common.ts` via `createCliE2ePluginTasks`)

- **Done:** One task key `INTERACTIVE_CLI_PTY_KEYSTROKE_TASK` (`applyInteractiveCliPtyKeystroke`) for all interactive PTY input.

### Page objects (`execution.ts` → `interactive()`)

- Replace generic `input(text)` with explicit methods, e.g. `enterSlashCommand`, `enterLine`, `pressEnter`, keep `pressEsc`, and adjust `answerToPrompt` / `inputDownArrowSelection` to call the right primitive twice instead of relying on magic.

### Step definitions (`step_definitions/cli.ts`)

- Replace overloaded `I input {string}` with steps whose wording matches behavior, for example:
  - `I enter the slash command {string} in the interactive CLI` — documents space-then-Enter behavior (wording can be tuned to match team taste; important part is **one** meaning).
  - `I enter {string} in the interactive CLI` — line + Enter only.
  - `I press Enter in the interactive CLI` — already exists; wire to `pressEnter` without going through slash logic.
- `I answer ... to prompt ...` stays but should call `enterLine` (or the same bytes as today for `y`/`n`).
- `I input down-arrow selection for {string}` should decompose into documented substeps internally (slash command send + second line `2` + Enter, or whatever the real sequence is)—still two explicit sends, no hidden slash heuristic inside one generic function.

Update **all** `e2e_test/features/cli/*.feature` references that today use `I input ...` so each line matches the intended primitive.

### Docs

- Update `.cursor/rules/cli.mdc` (or the smallest relevant section) so future scenarios use the explicit steps and do not reintroduce a “smart” send.
- Update `.cursor/rules/e2e_test.mdc` with the **explicit conditions / no smart branches** practice (see Phase 5).

---

## Phased delivery (behavior-first, small slices)

Order by **risk reduction** and **mechanical migration**; each phase should end with relevant CLI E2E spec(s) green (e.g. `--spec` for touched features per `e2e_test.mdc`).

### Phase 1 — Dumb write helper + parallel API

- **Done:** `applyInteractiveCliPtyKeystroke` + `InteractiveCliPtyKeystroke` (`interactiveCliPtyTypes.ts` + `cliPtyRunner.ts`). Repo/spawn/bundle in `cliE2eRepo.ts`; CLI Cypress tasks in `cliE2ePluginTasks.ts`.

### Phase 2 — Migrate `@interactiveCLI` scenarios to explicit steps

- **Done:** Same features + steps; page object maps each step to `InteractiveCliPtyKeystroke` and the single PTY keystroke task.
- Generic `I input {string} in the interactive CLI` removed; down-arrow step name unchanged, implementation uses explicit sends.

### Phase 3 — Remove legacy smart send

- **Done:** Removed `sendToInteractiveCli` from `cliPtyRunner.ts` and the `sendToInteractiveCli` Cypress task from `cliE2ePluginTasks.ts`. `common.ts` only spreads `createCliE2ePluginTasks` — no separate task list to edit.

### Phase 4 — Interactive “Not supported” scenario

- **Done:** `cli_interactive_mode.feature` — `@withCliConfig` + `@interactiveCLI`, `I enter "hello" in the interactive CLI`, `Not supported` in **history output** (matches `processInput` → `output.log` in TTY; parity with piped / `-c "hello"` in `cli_non_interactive_mode.feature`).

### Phase 5 — Document “explicit conditions, dumb automation” in E2E rule

- Merge into `.cursor/rules/e2e_test.mdc` (Key Practices): in step definitions and page objects, **avoid conditional logic** unless that branch exists **only** to assert or fail the test with a clear error. **Distinguish variants at the caller:** different steps and/or page-object methods (and scenarios), not one “smart” API that infers mode from parameters. **Steps should state the condition** in their wording (e.g. slash command vs plain line) so each scenario encodes a fixed path and branching in code stays rare.
- This phase can land **after** Phases 1–4 (rule references the CLI send refactor as the motivating example) or **in parallel** once the wording is stable—prefer doing it when the CLI steps are migrated so examples stay accurate.

---

## Out of scope / non-goals

- Changing production CLI behavior unless Phase 4 reveals a real mismatch with non-interactive mode.
- Renaming steps across non-CLI features.
- Broad refactors of `cliPtyRunner` beyond send semantics and task wiring.

---

## Checklist before closing the overall effort

- [x] No nested ternary “what to send” logic in `cliPtyRunner` for interactive input.
- [ ] Gherkin steps read as the user action (slash command with space+Enter vs plain line+Enter vs Enter vs ESC).
- [ ] `.cursor/rules/e2e_test.mdc` includes the explicit-conditions / minimal-branching guidance (Phase 5).
- [ ] `ongoing/cli-interactive-e2e-send-explicit.md` updated or removed when done.
