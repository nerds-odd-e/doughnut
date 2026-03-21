# Plan: Explicit interactive CLI send API and steps (no execution yet)

## Context

**Before Phases 1–3:** One Cypress task and `interactive().input` used a single string with heuristics (`trim()`, slash vs line, etc.), which hid trailing-space behavior and mixed intents.

**Now:** Interactive PTY writes go only through `writeInteractiveCliAndWaitForReady(payload)` with bytes built from `interactiveCliTtyPayload` or the matching Cypress tasks (`sendInteractiveCliSlashCommand`, `sendInteractiveCliLine`, `sendInteractiveCliEnter`, `sendInteractiveCliEsc`). Gherkin uses explicit steps (Phase 2).

## Correction: non-`/` input already exists in E2E

Interactive recall and MCQ flows already send lines that do not start with `/`: e.g. `y`, `n`, `1`, `2`, `sedition` in `cli_recall.feature`, plus `answerToPrompt` steps. Those are **supported** line input, not slash commands.

The gap vs `cli_non_interactive_mode.feature` is an explicit **unsupported plain line** case (e.g. `hello` → **Not supported**) under **TTY / `@interactiveCLI`**, mirroring the piped and `-c` scenarios.

## Target shape

### Low level (`cliPtyRunner.ts`)

- One shared helper, e.g. `writeInteractiveCliAndWaitForReady(payload: string)`, containing only: length before write, `pty.write(payload)`, wait for ready OSC, assert no escape leaks, return stdout. **No** interpretation of `payload`.
- Several **named** exports that only assemble bytes (no content-based branching):
  - **Slash command + space + Enter** — exact bytes: `command + ' \n'` where `command` is the slash line **without** relying on trim to invent the space (callers pass the command body they intend; document whether leading `/` is required by the CLI).
  - **Line + Enter** — `line + '\n'` for recall answers, numeric choices, free text, and unsupported probes like `hello`.
  - **Enter only** — `'\n'` (empty line).
  - **ESC** — `'\x1b'`.
  - Optional: **raw** / **exact bytes** if a future test must send `\n` inside a payload without the helper appending; only add if a real scenario needs it.

### Cypress tasks (`cliE2ePluginTasks.ts`, merged in `common.ts` via `createCliE2ePluginTasks`)

- **Done:** Separate task names for each send shape (`sendInteractiveCliSlashCommand`, etc.).

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

- **Done:** `writeInteractiveCliAndWaitForReady` + `interactiveCliTtyPayload` in `cliPtyRunner.ts`. Repo/spawn/bundle in `cliE2eRepo.ts`; CLI Cypress tasks in `cliE2ePluginTasks.ts` (`createCliE2ePluginTasks`).

### Phase 2 — Migrate `@interactiveCLI` scenarios to explicit steps

- **Done:** `cli_interactive_mode.feature`, `cli_recall.feature`, `cli_access_token.feature` use `I enter the slash command …`, `I enter …` (line), `I press Enter …`; page object uses `enterSlashCommand`, `enterLine`, `pressEnter`, `pressEsc`; Cypress tasks `sendInteractiveCliSlashCommand`, `sendInteractiveCliLine`, `sendInteractiveCliEnter`, `sendInteractiveCliEsc` call `writeInteractiveCliAndWaitForReady` with `interactiveCliTtyPayload`.
- Generic `I input {string} in the interactive CLI` removed; down-arrow step name unchanged, implementation uses explicit sends.

### Phase 3 — Remove legacy smart send

- **Done:** Removed `sendToInteractiveCli` from `cliPtyRunner.ts` and the `sendToInteractiveCli` Cypress task from `cliE2ePluginTasks.ts`. `common.ts` only spreads `createCliE2ePluginTasks` — no separate task list to edit.

### Phase 4 — Interactive “Not supported” scenario

- In `e2e_test/features/cli/cli_interactive_mode.feature`, add an `@interactiveCLI` scenario (with `@withCliConfig` if other interactive tests need the same backend/config hooks—match whatever `@interactiveCLI` scenarios that exercise the repo CLI require).
- **Given** the same class of setup as other interactive tests (minimal: start interactive CLI via existing Before hook).
- **When** use the **line** primitive step, e.g. `I enter "hello" in the interactive CLI` (not the slash-command step).
- **Then** assert `Not supported` in **history output** (or Current guidance—match where the CLI actually renders that message in TTY mode; align with existing interactive assertion steps, not `non-interactive output`).

If product behavior differs in TTY (message text or placement), adjust the Then to the correct section; the scenario intent is parity with `cli_non_interactive_mode.feature`’s “Piped stdin responds ‘Not supported’” / `-c "hello"` expectations.

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
