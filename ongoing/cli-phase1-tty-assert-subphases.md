# Phase 1 sub-phases — `tty-assert-staging` and target TTY API shape

**Parent plan:** [`cli-terminal-test-library-extraction.md`](./cli-terminal-test-library-extraction.md) (Phase 1: in-place refactor, **no user-visible or Gherkin behavior change**).

**This document:** Break Phase 1 into **small sub-phases**, each with one **observable gate**, ordered by **stability and seam clarity first** (see `.cursor/rules/planning.mdc`). **Sub-phase 1.1 is done** (see below); **1.2+** describe remaining Phase 1 work.

---

## Target API (north star for naming and seams)

Phase 1 **does not** need to expose every method to Cypress tests on day one. It **does** need **Node-side** types and modules whose names and responsibilities **map cleanly** to this shape so later phases (package extract, xterm, Playwright-style assertions) do not rename everything again.

Intended ergonomics (conceptual):

```ts
const terminal = ttyAssert.startProgram(/* spawn options */)
terminal.write('partial')
terminal.submit() // e.g. line commit: `\r` / `\n` as today
terminal.kill()
terminal.getByText('needle') // logical locator over **visible** terminal text, not DOM
await terminal.expect(locator).toBeVisible(/* opts: timeout, retries */)
terminal.dumpFrames() // debug artifact: see §Frames vs PTY below
```

**Frames vs PTY:** Ink **Vitest** tests have discrete `frames` / `lastFrame()`. The **Cypress PTY** path today is a **growing raw byte buffer** plus replay to a **single** “visible plaintext” view — there is no native frame array. For Phase 1, define `dumpFrames()` as one of:

- **Documented interim:** `dumpFrames()` returns a **structured debug dump** (e.g. buffer length, tail preview, optional **sampled** snapshots on newline or timer — only if sampling does not change timing-sensitive tests), **or**
- **Alias:** `dumpFrames()` = `dumpLastVisiblePlaintext()` + raw tail, until Phase 4 (xterm) provides real screen snapshots.

Pick one behavior in **sub-phase 1.4** and keep it **test-only / diagnostic** so Gherkin outcomes stay unchanged.

---

## Scope boundaries (Phase 1 only)

| Stays in Doughnut-specific layers | In `e2e_test/config/tty-assert-staging/` (after 1.1) / still to move |
|-----------------------------------|-----------------------------------------------------|
| `cliE2eGoogleOAuthSimulation`, install/bundle paths, `cliEnv`, repo spawn helpers; **`cliPtyCurrentGuidanceFromReplay.ts`** (`extractCurrentGuidanceFromReplayedPlaintext` — Ink prompt / guidance heuristics) | **`stripAnsi.ts`** (`stripAnsiCliPty`), **`geometry.ts`** (`CLI_INTERACTIVE_PTY_COLS` / `ROWS`), **`ptyTranscriptToVisiblePlaintext.ts`** (`ptyTranscriptToVisiblePlaintext` + CSI replay helpers) |
| Cypress `cy.task` registration, `repoRoot`, startup substring / timeouts **values** | **Pure** snapshot / safe-text formatting used for assertion errors (generic parts only) — **1.2** |
| Domain strings in `outputAssertions` (recall hints, “wrong step” heuristics for non-interactive vs PTY) | **PTY session object**: buffer, spawn, kill, append from `onData`, optional wait-for-substring — **1.3** |

**Rule:** After each sub-phase, **active CLI E2E** (`e2e_test/features/cli/cli_install_and_run.feature`, non-`@ignore` scenarios) stays green; no change to scenario text or expected user-visible CLI output.

---

## Sub-phase 1.1 — Staging folder and pure module moves

**Status:** Done.

**User-visible outcome:** None. **Developer outcome:** Generic terminal helpers live under one directory and are importable **without** Cypress or product types.

**As implemented:**

- Added [`e2e_test/config/tty-assert-staging/`](../e2e_test/config/tty-assert-staging/): `stripAnsi.ts`, `geometry.ts`, `ptyTranscriptToVisiblePlaintext.ts`.
- Ink/Doughnut **current guidance** extraction lives in [`e2e_test/config/cliPtyCurrentGuidanceFromReplay.ts`](../e2e_test/config/cliPtyCurrentGuidanceFromReplay.ts) (split from the old combined replay module).
- Call sites import staging directly: [`cliE2ePluginTasks.ts`](../e2e_test/config/cliE2ePluginTasks.ts), [`outputAssertions.ts`](../e2e_test/start/pageObjects/cli/outputAssertions.ts).
- Removed `cliPtyAnsi.ts`, `cliInteractivePtyGeometry.ts`, `cliPtyTerminalReplay.ts`.

**Gate:** Same CLI E2E pass as today; `pnpm lint` / typecheck for `e2e_test` clean.

**Tests:** No new suite; regression is E2E (`cli_install_and_run.feature`). Defer **black-box** unit tests on pure replay/ANSI to parent Phase 3 unless needed sooner (planning.mdc: prefer observable gate).

---

## Sub-phase 1.2 — Generic error snapshot helpers

**User-visible outcome:** None. **Failure output** should remain **substantively the same** (ANSI-stripped preview, truncation limits); wording may shift only if unavoidable — prefer identical messages.

**Work:**

- Extract **generic** pieces from `outputAssertions.ts` such as:
  - safe visible text / control-char rendering for errors,
  - truncation policy constants (or shared `PREVIEW_LEN` / max chars) **where they are not Doughnut-specific**.
- Keep in `outputAssertions.ts` (or adjacent Doughnut file): section labels, `stdoutLooksLikeInteractiveCliPtyCapture` heuristics, Cypress screenshot side effects, `cy.task` orchestration.

**Gate:** CLI E2E green; a **deliberate local failure** (temporary) should show the same fields as before (raw length, stripped preview, truncation behavior) — optional manual check, not committed.

---

## Sub-phase 1.3 — PTY session core in staging (Node only)

**Status:** Done.

**As implemented:** [`e2e_test/config/tty-assert-staging/ptySession.ts`](../e2e_test/config/tty-assert-staging/ptySession.ts) exports `BufferedPtySession`, `startBufferedPtySession`, `disposeBufferedPtySession`, `waitForVisiblePlaintextSubstring`. [`cliE2ePluginTasks.ts`](../e2e_test/config/cliE2ePluginTasks.ts) keeps the singleton, `cliEnv` merge, startup substring/timeouts, and task names; [`cliE2eGoogleOAuthSimulation.ts`](../e2e_test/config/cliE2eGoogleOAuthSimulation.ts) takes `BufferedPtySession`.

**User-visible outcome:** None.

**Work:**

- From `cliE2ePluginTasks.ts`, extract a **session type** (or small module) that owns:
  - spawn via `node-pty` (or a **thin injectable** spawn function for tests later),
  - `onData` → growing buffer,
  - `kill` / idempotent dispose,
  - **optional:** `waitForVisibleSubstring` using existing `stripAnsi` + buffer (same semantics as `installedCliInteractiveWaitForSubstring` today).
- `createCliE2ePluginTasks` becomes: **singleton session**, `cy.task` names, Doughnut **startup marker string**, **timeouts**, `PREVIEW_LEN` for plugin errors, OAuth attach, install bundle — **no** duplicate buffer logic.

**Suggested naming (maps to target API):**

| Target method | Staging / internal (Phase 1) |
|---------------|------------------------------|
| `startProgram` | `startProgram` / `attachTerminalHandle` in [`facade.ts`](../e2e_test/config/tty-assert-staging/facade.ts) (wraps `startBufferedPtySession`); Cypress plugin keeps `cliEnv` merge and uses `attachTerminalHandle` after spawn |
| `write` / `submit` | Handle `write` / `submit`; same task names: `cliInteractiveWriteRaw` / `cliInteractiveWriteLine` |
| `kill` | `handle.kill()` → `disposeBufferedPtySession(session)` |
| `expect` / `dumpFrames` | `handle.expect(loc).toBeVisible`, `handle.dumpFrames()` (diagnostic object; not real frames) |

**Gate:** CLI E2E green (interactive install scenario still finds startup banner and accepts lines).

---

## Sub-phase 1.4 — Facade module and locator / expect sketch

**Status:** Done.

**User-visible outcome:** None for Gherkin; **optional** richer **Node** API for plugin code.

**As implemented:** [`e2e_test/config/tty-assert-staging/facade.ts`](../e2e_test/config/tty-assert-staging/facade.ts) exports `startProgram`, `attachTerminalHandle`, `TtyAssertTerminalHandle` (`write`, `submit`, `kill`, `getRawBuffer`, `getVisiblePlaintext` = stripped cumulative transcript, `getReplayedScreenPlaintext`, `getByText`, `expect(loc).toBeVisible`, `dumpFrames`). [`cliE2ePluginTasks.ts`](../e2e_test/config/cliE2ePluginTasks.ts) stores that handle, waits for startup via `expect(getByText(…)).toBeVisible`, delegates writes/buffer/OAuth `session`. `waitForVisiblePlaintextSubstring` in [`ptySession.ts`](../e2e_test/config/tty-assert-staging/ptySession.ts) takes optional `retryMs` (default 50).

**Work:**

- Add a small **`tty-assert-staging/facade.ts`** (name TBD) that **wraps** the Phase 1.3 session and replay:

  - `startProgram(opts)` → handle implementing `write`, `submit`, `kill`, `getRawBuffer()`, `getVisiblePlaintext()`.
  - `getByText(text)` → returns an opaque **locator** (e.g. `{ kind: 'substring', value }` or row/column later) — **no DOM**.
  - `expect(locator).toBeVisible({ timeoutMs, retryMs })` → **async** predicate loop over **visible plaintext** (reuse same retry philosophy as `outputAssertions`: bounded turns or ms — **match current E2E stability**, do not tighten or loosen without cause).
  - `dumpFrames()` → implement per **Frames vs PTY** decision above; document in module JSDoc.

- **Cypress plugin** may use this facade **internally** only; step definitions and Gherkin **unchanged** in this sub-phase.

**Gate:** CLI E2E green.

**Note:** Full **Cypress-chainable** `terminal.expect(...).toBeVisible()` can mirror Playwright **later** (parent plan Phase 5–6). Phase 1.4 only requires the **Node** side to exist so tasks stay thin.

---

## Sub-phase 1.5 — Thin Cypress-facing wrapper (optional, still Phase 1)

**Status:** Done.

**User-visible outcome:** None; steps may call **new** helpers only if they are **aliases** of existing page-object methods (prefer **no** Gherkin edits).

**Work:**

- In `e2e_test/start/pageObjects/cli/`, add something like `ttyAssertTerminal.ts` that:
  - exposes `start` / `write` / `submit` / `kill` as **`cy.task`** wrappers aligned with the **same** task names as today (no new tasks unless strictly necessary),
  - exposes `expectVisibleText` / `getByText` as thin wrappers around existing `interactiveCli().pastCliAssistantMessages().expectContains` **or** new tasks that call the **1.4 facade** — **only** if behavior is identical.

**Default:** Skip 1.5 if 1.3–1.4 already achieve the parent Phase 1 “thin plugin layer” goal; do 1.5 when you want **one** import style for new tests before the standalone package exists.

**As implemented:**

- [`e2e_test/start/pageObjects/cli/ttyAssertTerminal.ts`](../e2e_test/start/pageObjects/cli/ttyAssertTerminal.ts): `startRepoInteractive`, `startInstalledInteractive`, `write`, `submit`, `kill`, `getRawBuffer`, `enableGoogleOAuthSimulation`; re-exports `outputAssertions` helpers; `getByText(…).expectVisibleInPastAssistantMessages()` delegates to `pastCliAssistantMessages().expectContains`.
- [`cli` page object](../e2e_test/start/pageObjects/cli/index.ts) exposes `ttyAssertTerminal`; [`interactiveCli.ts`](../e2e_test/start/pageObjects/cli/interactiveCli.ts) uses `pty.submit` / `pty.write` for line and raw writes.
- [`hook.ts`](../e2e_test/step_definitions/hook.ts) `@interactiveCLI` / `@interactiveCLIGmail` and [`execution.ts`](../e2e_test/start/pageObjects/cli/execution.ts) `runInteractiveMode` call the wrapper for start/kill / installed start; [`cli_gmail.ts`](../e2e_test/step_definitions/cli_gmail.ts) OAuth simulation step uses `enableGoogleOAuthSimulation`.

**Gate:** CLI E2E green (`pnpm cypress run --spec e2e_test/features/cli/cli_install_and_run.feature` with SUT on `http://localhost:5173`).

---

## Checklist before closing parent Phase 1

- [x] `tty-assert-staging/` contains **no** imports from `cypress`, `e2e_test/start`, or Doughnut product packages. *(1.1)*
- [x] `createCliE2ePluginTasks` is **obviously** glue: tasks + Doughnut env + OAuth + install paths. *(1.3)*
- [ ] `outputAssertions` domain heuristics remain **local** and documented if still mixed with generic formatting.
- [x] Target API table (above) updated if names changed during implementation. *(1.4)*
- [ ] Update [`cli-terminal-test-library-extraction.md`](./cli-terminal-test-library-extraction.md) Phase 1 bullet to point at this file and mark Phase 1 done when appropriate.

---

## Dependency note (parent roadmap)

- **Phase 2** (workspace package) lifts `tty-assert-staging/` → `packages/tty-assert` with minimal import path changes.
- **Phase 5–6** in the parent plan align **public** method names, retries, and lifecycle docs with the facade introduced in **1.3–1.4**.
- **Vitest** for staging: parent **Phase 3**; Phase 1 may add **only** tests that prevent regressions when moving pure functions (planning.mdc: avoid structure-mapped tests; prefer replay/fixture I/O).

---

## What Phase 1 explicitly does **not** do

- No **npm** package, no `pnpm-workspace` entry (Phase 2).
- No **xterm.js** (Phase 4).
- No change to **which** Cypress scenarios run in CI or their assertions’ **meaning**.
- No replacement of **Vitest** `runInteractive` / `ink-testing-library` tests by PTY tests (out of scope in parent plan).

When this sub-phase plan is stale, edit or remove this file per parent doc instructions.
