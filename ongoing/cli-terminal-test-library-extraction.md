# `tty-assert` — PTY terminal test library extraction

**Status:** Phases 1–3 are **complete** in-repo; Phases 4–11 are roadmap only until started. Sub-phases: [`ongoing/cli-phase1-tty-assert-subphases.md`](./ongoing/cli-phase1-tty-assert-subphases.md).

**Intent:** Extract PTY-based terminal testing into a **Cypress-neutral, Doughnut-neutral** library named **`tty-assert`**, publishable on npm and eventually movable out of this repo. Goal: reliable assertions on terminal-visible state, with failures that show **expected vs actual** without manually decoding escape sequences, and CI-friendly artifacts where useful.

**Package name:** `tty-assert` (check npm for availability and scope, e.g. `@your-scope/tty-assert`, before publish).

---

## Principles (from `.cursor/rules/planning.mdc`)

- **One slice per phase** with a clear **observable gate**: full existing CLI-relevant E2E still green after each phase that touches production test paths, unless a phase is library-only (then its gate is `tty-assert` tests + no Doughnut E2E regression when integrated).
- **Order by value:** stability and clear boundaries first; nicer diagnostics next; pixels/animation last; extraction to standalone repo last.
- **Tests:** Prefer driving **observable surfaces** — for `tty-assert`, **unit tests on transcript → visible text, ANSI handling, replay geometry**; Doughnut keeps **Cypress** coverage for real install + interactive flows.
- **No duplicate “framework for framework”:** `tty-assert` owns **generic** PTY buffer, escape handling, screen replay, and **pluggable** assertion helpers; Doughnut keeps **Ink- and product-specific** matchers (e.g. “current guidance” heuristics, OAuth simulation glue) as thin adapters.

---

## Current anchors (what moves vs stays)

| Area | Location today | Likely home |
|------|----------------|-------------|
| PTY spawn, buffer, write tasks | `e2e_test/config/cliE2ePluginTasks.ts` (glue) + `packages/tty-assert` (`ptySession`, `facade`) | `tty-assert` **runtime** API + optional **Cypress task adapter** (thin, ideally in `e2e_test` only) |
| ANSI strip | `packages/tty-assert/src/stripAnsi.ts` | `tty-assert` core |
| Fixed cols/rows | `packages/tty-assert/src/geometry.ts` | `tty-assert` default geometry (configurable) |
| Transcript → visible plaintext / replay | `packages/tty-assert/src/ptyTranscriptToVisiblePlaintext.ts` | `tty-assert` core; **Phase 4** moves authoritative interpretation to **xterm.js** where parity is proven |
| Error snapshot formatting (truncation, safe text) | `packages/tty-assert/src/errorSnapshotFormatting.ts` | `tty-assert` core |
| Google OAuth PTY simulation | `e2e_test/config/cliE2eGoogleOAuthSimulation.ts` | Stays **Doughnut** (or behind `tty-assert` **hook/extension** interface) |
| Retry, `expectContains`, domain heuristics | `e2e_test/start/pageObjects/cli/outputAssertions.ts` | **Generic** snapshots → `tty-assert` (today: `errorSnapshotFormatting`); **domain** sections + Cypress orchestration stay Doughnut |
| Cypress fluents | `e2e_test/start/pageObjects/cli/interactiveCli.ts` | Doughnut; calls `tty-assert`-backed tasks or helpers |

---

## Phase 1 — In-place refactor: boundaries, no behavior change

**Status:** **Complete** (in-repo). Record of sub-phases 1.1–1.5: [`ongoing/cli-phase1-tty-assert-subphases.md`](./ongoing/cli-phase1-tty-assert-subphases.md).

**Outcome:** Same Gherkin + Cypress behavior; **generic terminal** logic lived under `e2e_test/config/tty-assert-staging/` (removed in Phase 2) and is importable without Cypress or Doughnut product types.

**Delivered:**

- ~~`tty-assert-staging/`~~ → **`packages/tty-assert`** (Phase 2): `stripAnsi`, `geometry`, `ptyTranscriptToVisiblePlaintext`, `errorSnapshotFormatting`, `ptySession`, `facade`; Ink guidance extraction stays in `cliPtyCurrentGuidanceFromReplay.ts`.
- **Doughnut-only:** OAuth simulation, install/bundle paths, env wiring, Doughnut CLI vocabulary in `outputAssertions` (see that file’s header for generic vs domain).
- **Plugin:** `createCliE2ePluginTasks` is thin glue (tasks, `cliEnv`, startup waits, OAuth, install paths) over `tty-assert` (`ptySession` / `facade`).

**Gate:** CLI E2E that ran before still passes; no new user-visible behavior.

---

## Phase 2 — Sub-project inside Doughnut monorepo

**Status:** **Complete.** Workspace package `packages/tty-assert`; root `devDependencies` includes `tty-assert` (`workspace:*`) for Cypress / plugin resolution.

**Outcome:** `pnpm-workspace.yaml` includes **`packages/tty-assert`**; `e2e_test` depends on it; **no** Cypress code inside the package.

**Work:**

- Package exports: **Node-only** surface (PTY session manager, buffer, replay, ANSI utilities, types). Prefer keeping any `cy.task` mapping in **`e2e_test`** so `tty-assert` stays test-runner agnostic (no Cypress peer dependency).
- Wire TypeScript project references / build if needed; ensure CI Cypress job resolves the package.

**Gate:** Same as phase 1 for E2E; plus `pnpm install` / CI clean build succeeds.

---

## Phase 3 — Library quality: unit tests, lint, format, CI

**Status:** **Complete.**

**Outcome:** `tty-assert` is trustworthy on its own.

**Delivered:**

- **Vitest** under `packages/tty-assert/tests/`: ANSI strip, replay → visible plaintext, error snapshot truncation / sanitization.
- **Biome** `lint` / `format` unchanged (monorepo-aligned).
- **CI:** `Other-unit-tests` runs `pnpm tty-assert:test`; **`.github/workflows/tty-assert-pr.yml`** runs `tty-assert:lint` + `tty-assert:test` on PRs that touch `packages/tty-assert/**`.

**Gate:** `tty-assert` tests green; Doughnut E2E unchanged.

---

## Phase 4 — Introduce xterm.js for better assertion

**Outcome:** Terminal-visible state used for assertions is driven by **xterm.js** (headless / Node-friendly wiring), so behavior matches a real terminal emulator more closely than a hand-rolled replay.

**Work:**

- Feed the PTY byte stream into an xterm instance sized to the same **cols × rows** as the PTY session.
- Migrate or replace bespoke replay (`ptyTranscriptToVisiblePlaintext` and related) **behind** this model where parity is verified; keep regression tests on fixed transcript fixtures until confidence is high.
- Expose a small internal or package-level API to read **current screen / buffer** for assertions and later diagnostics (PNG, structured debug output).

**Gate:** Doughnut CLI E2E unchanged; `tty-assert` unit tests cover xterm-backed visible state for representative transcripts.

---

## Phase 5 — Tidy assertion APIs of `tty-assert`

**Outcome:** One coherent, documented **assertion surface** (names, parameters, options objects, retries), instead of accreted helpers.

**Work:**

- Consolidate entry points for substring/region checks, timeouts, and error message shape; avoid duplicate “strip then search” paths.
- Separate **generic** matchers from hooks where Doughnut passes domain-specific region logic (e.g. “current guidance”) without widening the core API unnecessarily.
- Update Doughnut page objects to the tidied API with **no** behavior change.

**Gate:** E2E green; `tty-assert` README or `docs/` snippet shows the intended public assertion API.

---

## Phase 6 — Tidy start and terminating APIs of `tty-assert`

**Outcome:** Obvious lifecycle: **start session** (spawn, env, geometry), **write**, **read state**, **stop/dispose** — with clear semantics for errors and teardown.

**Work:**

- Unify spawn options, default env, kill vs graceful exit, and **idempotent** dispose (safe double-call, scenario end hooks).
- Define behavior for edge cases: child already dead, timeout waiting for startup marker, leak prevention (listeners, timers).
- Align Cypress plugin tasks with this surface so `e2e_test` stays a thin adapter.

**Gate:** E2E green; unit tests for lifecycle edge cases where practical without a real long-running PTY.

---

## Phase 7 — Clearer failure messages for TTY debugging

**Outcome:** When an assertion fails, developers see **structure** (rows, regions, or frame boundaries) not only a flat wall of text.

**Work:**

- Define a **stable, documented** “debug view” of the last replayed screen (e.g. row numbers, optional ruler, highlighted region when the **caller** passes region bounds — generic API: “annotate this substring range”). Prefer reading layout from **xterm** state where possible.
- Unify duplicate preview logic (plugin `PREVIEW_LEN` vs `outputAssertions` truncation) through `tty-assert` helpers.

**Gate:** Intentionally failing test in `tty-assert` or a temporary Doughnut step shows improved message; real suite green.

---

## Phase 8 — “Final visible screenshot” in plain text (single state)

**Outcome:** Failure artifacts include **one** plain-text rendering of the **final** visible terminal state, not the entire scrollback or every intermediate frame.

**Work:**

- API: `lastVisiblePlaintext` (or equivalent) sourced from **xterm** / shared screen model; error formatter opts into **final screen only** vs **full stripped transcript** for support bundle.
- Cypress: on failure path, attach text artifact via existing screenshot/video / CI artifact patterns where possible.

**Gate:** Failure output is shorter and readable; E2E still pass.

---

## Phase 9 — PNG screenshot of terminal state

**Outcome:** Raster image of the **final** (or selected) visible screen for failed runs.

**Work:**

- Choose renderer (canvas in Node, headless terminal image lib, or HTML → png); **xterm** + canvas is a natural fit if already on the path; keep **optional** heavy dependency (peer or optional install) so lightweight consumers can skip PNG.
- Integrate with Cypress failure hooks: write PNG to `downloadsFolder` / task return path for CI artifacts.

**Gate:** On forced failure, PNG appears in CI artifacts; default CI still passes.

---

## Phase 10 — Animated capture (screen changes over time)

**Outcome:** Optional artifact showing evolution from session start to failure (or full scenario).

**Work:**

- Define **frame sampling** policy (on PTY data events vs debounced time — avoid flakiness; prefer **event-sampled** with max frame cap for CI size).
- Output: GIF or MP4 or frame sequence; document cost and when to enable (e.g. `DEBUG_TTY_ANIM=1`).

**Gate:** Behind flag or opt-in; not required for default CI pass.

---

## Phase 11 — Move out of Doughnut (OSS npm package)

**Outcome:** Standalone repository, semver, changelog, README for **Playwright / Vitest / raw Node** consumers; Doughnut pins a version.

**Work:**

- Extract git history if desired; publish **`tty-assert`** (scoped if needed).
- Doughnut: delete in-repo package, add npm dependency, shrink adapter code.

**Gate:** Doughnut CI green on released version.

---

## Dependencies between phases

- **1 → 2:** Clean seams make the package extraction mechanical.
- **3** can start once **2**’s skeleton exists (tests can live next to the package).
- **3 → 4:** Unit tests and CI exist before swapping the emulation core to xterm.js.
- **4 → 5 → 6:** Visible state model (xterm) stabilizes first; then assertion API; then lifecycle API — each phase ends with E2E green.
- **7–10** are mostly sequential in **diagnostic value**; **9–10** may share rendering infrastructure (**8 → 9** especially).
- **11** last.

---

## Out of scope for this plan

- Replacing Cypress PTY E2E with Vitest `runInteractive` for scenarios already covered in `cli/tests/`; this plan does **not** require migrating those tests.
- Changing **what** Doughnut CLI renders — only **how tests observe and report** failures.

---

## When this plan changes

Update this file as phases complete or scope shifts; remove stale notes.
