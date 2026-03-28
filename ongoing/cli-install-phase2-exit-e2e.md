# Sub-plan: Phase 2 — `/exit` + past user messages (`cli_install_and_run`)

Parent: `ongoing/cli-install-interactive-mode-e2e.md` (Phase 2). Aligns with `ongoing/cli-architecture-roadmap.md` (interactive path = Ink + React; message transcript; centralized CLI assertions).

## Goal (user-visible)

Full four-step scenario in `e2e_test/features/cli/cli_install_and_run.feature`:

1. Run installed CLI in interactive mode (TTY).
2. See `doughnut 0.1.0` in past CLI assistant messages.
3. Enter slash command `/exit`.
4. See `/exit` in past user messages; process exits cleanly.

## Principles (`.cursor/rules/planning.mdc`)

- One **small** slice per sub-phase; each slice is **justified** by tests that exercise it.
- **No dead code:** new Cypress tasks / page-object APIs are **used** from step definitions.
- **Single scenario:** Do **not** add a duplicate WIP scenario; extend the existing “Install and run the CLI in interactive mode” scenario by **uncommenting** the `/exit` steps there (see **2a**).
- **Deploy gate:** after a sub-phase that changes production or CI-visible Gherkin, usual commit / push / CD before the next sub-phase.

## Ink input (Context7: `/vadimdemedes/ink`)

- Handle TTY keys with **`useInput((input, key) => { ... })`** from `ink` — not raw `stdin` listeners in random components.
- Treat **`key.return`** as submit for the command line; build the buffer from `input` (per-character) or paste path if you add `usePaste` later.
- Prefer **`useFocus`** if multiple focusable regions exist so only the command line consumes keys.

## Sub-phases

### 2a — Wrap interactive spawn + assertions + step glue (no new hooks)

**Outcome:** The step **When I run the installed doughnut command in interactive mode** starts the PTY as today, but the **same** long-lived process is **wrapped / retained** (module-scoped handle + accumulated output, or equivalent) so later steps in that scenario can **write** and **read** the transcript. **No new Cucumber `Before`/`After` hook** and **no session tag** for this — reuse existing install/interactive wiring only.

**Teardown:** It is **not** required that the scenario ends with a clean process exit before **2c**. If the CLI keeps running, **Cypress/step timeout** (or runner teardown) kills the child; document any minimum timeout if needed.

**Deliverables:**

- Refactor or extend the existing interactive task / page-object path so the process opened by **`runInteractiveMode()`** (or the equivalent) stays usable for subsequent steps — **not** a separate one-shot spawn per step unless unavoidable.
- **`installedCliInteractiveWaitForSubstring`** (or reuse existing wait helpers): on timeout, throw with expected substring, timeout, and **ANSI-stripped preview** (same idea as past assistant assertions).
- **`pastUserMessages().expectContains`** in `e2e_test/start/pageObjects/cli/outputAssertions.ts` (reuse stripAnsi; distinct failure copy for “past user messages”).
- Step defs: **`I enter the slash command {string} in the interactive CLI`**, **`I should see {string} in past user messages`**.
- **`execution.ts`:** `runInteractiveMode()` opens, waits for `doughnut 0.1.0`, keeps alias; **`enterSlashCommandInInteractiveCli(command)`** writes a line and refreshes captured output. **Optional before 2c:** skip strict **`waitForExit`** in the step glue; add **`installedCliInteractiveWaitForExit`** (or equivalent) when **2c** requires a clean exit after `/exit`.
- **Feature file:** **Uncomment** the two commented steps in the main **Install and run the CLI in interactive mode** scenario (do **not** add a second scenario).

**CI:** With all four steps active, the spec is **expected to fail** until **`/exit`** behavior lands in **2c** (assertions or product). Batch **2a→2c** on one branch if the team wants green CI throughout; there is **no** separate `@ignore` Phase 2 scenario.

**Checkpoint:** Wrapper + steps + assertions exist; first two steps still behave as in Phase 1; full scenario **green** only after **2c** (or interim red is acceptable per team policy).

### 2b — Vitest for `/exit` (optional but recommended before product)

**Outcome:** `runInteractive` (mock TTY) proves `/exit` appears in user transcript and process exits — fails until **2c** if omitted.

**Checkpoint:** `pnpm cli:test` green after **2c**.

### 2c — Product: `/exit` via Ink + `useInput`

**Outcome:** Installed interactive CLI records **`/exit`** in the user message transcript and exits. No raw stdin handling in the command strip outside Ink’s model.

**Checkpoint:** Main “Install and run the CLI in interactive mode” scenario (already four uncommented steps since **2a**) passes end-to-end; **`waitForExit`** (or equivalent) wired if needed for stable teardown.

**Checkpoint:** Active install feature (all scenarios) passes; no dead Gherkin or unused tasks.

### 2d — Parent plan / docs touch-up

- One-line cross-link in parent Phase 2 if still useful.
- When Phase 3 in parent plan runs, update `.cursor/rules/cli.mdc` **Active CLI E2E** if required.

## Expected failure before 2c

Running **`cli_install_and_run.feature`** should fail on **steps 3–4** (slash input or **`past user messages`**) until **`/exit`** exists, with a **short, explicit** message — not “task not registered” or empty transcript without context. Process exit is only required once **2c** and optional **`waitForExit`** wiring are in place.

## Commands

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_install_and_run.feature
```

While the spec is red between **2a** and **2c**, run the same feature file locally to iterate; no separate ignored scenario.
