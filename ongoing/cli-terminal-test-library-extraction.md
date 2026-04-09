# `tty-assert` — PTY terminal test library extraction

**Status:** Phases 1–3 are **complete** in-repo; **Phase 4** is **complete** (4.3: `outputAssertions.getGuidanceContext` uses xterm viewport replay). **Phase 5** is **complete** (sub-phases **5.1–5.9 met**, including legacy replay removal — execution table in [`ongoing/cli-phase5-tty-assert-api-xterm-finish-subphases.md`](./cli-phase5-tty-assert-api-xterm-finish-subphases.md)). **Phase 6** is **complete** (sub-phases **6.1–6.5 met** — managed session, `cliAssert`, docs, removal of unused `cliInteractivePtyGetBuffer`; detail in [`ongoing/cli-phase6-tty-assert-managed-session-subphases.md`](./cli-phase6-tty-assert-managed-session-subphases.md)). **Phase 11** is **complete** (single **`exports`** entry + README that matches; prior-art wording removed from `packages/tty-assert`). **Phase 12** is **complete** (neutral `tty-assert:` validation errors, palette-background failure copy, `dumpDiagnostics` rename). **Phase 13** is **complete** (shared `pollSurfaceAssertLoop`, unified `TtyAssertDumpDiagnostics` + `buildTtyAssertDumpDiagnostics`, assert JSON payload + `managedTtyAssertOptionsFromJson` on package root). **Phase 14** is **complete** (removed unused `facade` module; managed session + `waitForTextInSurface` remain the supported shapes). Phases **7–10**, **15** follow the roadmap (7–10 diagnostics and capture; **15** move out). Sub-phases: Phase 1 — [`ongoing/cli-phase1-tty-assert-subphases.md`](./cli-phase1-tty-assert-subphases.md); Phase 4 — [`ongoing/cli-phase4-tty-assert-xterm-subphases.md`](./cli-phase4-tty-assert-xterm-subphases.md); Phase 5 — [`ongoing/cli-phase5-tty-assert-api-xterm-finish-subphases.md`](./cli-phase5-tty-assert-api-xterm-finish-subphases.md); Phase 6 — [`ongoing/cli-phase6-tty-assert-managed-session-subphases.md`](./cli-phase6-tty-assert-managed-session-subphases.md).

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
| PTY spawn, buffer, write tasks | `e2e_test/config/cliE2ePluginTasks.ts` (glue) + `packages/tty-assert` (`ptySession`, managed session) | `tty-assert` **runtime** API + optional **Cypress task adapter** (thin, ideally in `e2e_test` only) |
| ANSI strip | `packages/tty-assert/src/stripAnsi.ts` | `tty-assert` core |
| Fixed cols/rows | `packages/tty-assert/src/geometry.ts` | `tty-assert` default geometry (configurable) |
| Transcript → visible plaintext / replay | [`ptyTranscriptToVisiblePlaintextViaXterm.ts`](./packages/tty-assert/src/ptyTranscriptToVisiblePlaintextViaXterm.ts) + [`waitForTextInSurface`](./packages/tty-assert/src/waitForTextInSurface.ts) | **Phase 4:** xterm viewport replay for **`getGuidanceContext` / Current guidance**; **Phase 5:** facade + locator surfaces + E2E migration; **5.9:** removed hand-rolled replay |
| Google OAuth PTY simulation | `e2e_test/config/cliE2eGoogleOAuthSimulation.ts` | Stays **Doughnut** (or behind `tty-assert` **hook/extension** interface) |
| Retry, `expectContains`, domain heuristics | `e2e_test/start/pageObjects/cli/outputAssertions.ts` | **Generic** snapshots + **locator/poll** helpers → `tty-assert`; **domain** surfaces (guidance extraction, past-user SGR rules) + Cypress orchestration stay Doughnut |
| Cypress fluents | `e2e_test/start/pageObjects/cli/interactiveCli.ts` | Doughnut; calls `tty-assert`-backed tasks or helpers |

---

## Phase 1 — In-place refactor: boundaries, no behavior change

**Status:** **Complete** (in-repo). Record of sub-phases 1.1–1.5: [`ongoing/cli-phase1-tty-assert-subphases.md`](./ongoing/cli-phase1-tty-assert-subphases.md).

**Outcome:** Same Gherkin + Cypress behavior; **generic terminal** logic lived under `e2e_test/config/tty-assert-staging/` (removed in Phase 2) and is importable without Cypress or Doughnut product types.

**Delivered:**

- ~~`tty-assert-staging/`~~ → **`packages/tty-assert`** (Phase 2): `stripAnsi`, `geometry`, xterm viewport replay, `ptySession`; Ink guidance extraction stays in `cliPtyCurrentGuidanceFromReplay.ts`.
- **Doughnut-only:** OAuth simulation, install/bundle paths, env wiring, Doughnut CLI vocabulary in `outputAssertions` (see that file’s header for generic vs domain).
- **Plugin:** `createCliE2ePluginTasks` is thin glue (tasks, `cliEnv`, startup waits, OAuth, install paths) over `tty-assert` (`ptySession`, managed session).

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

**Status:** **Complete** (4.1–4.3). **Sub-phases (detail, gates, TDD notes):** [`ongoing/cli-phase4-tty-assert-xterm-subphases.md`](./cli-phase4-tty-assert-xterm-subphases.md).

**Outcome:** Terminal **replay** used for **Current guidance** assertions is driven by **xterm.js** (Node/headless wiring), so simulated screen text aligns with a real terminal emulator. Legacy hand-rolled replay existed for parity until Phase **5.9**, then was removed.

**Sub-phase summary (planning.mdc: one slice per sub-phase, ordered by dependency):**

| Sub-phase | Deliverable | Gate |
|-----------|-------------|------|
| **4.1** | `tty-assert`: xterm deps + replay primitive + smoke unit tests (transcript → plain string contract) | **Met:** `tty-assert` test + lint green; no Doughnut replay wiring |
| **4.2** | Parity (or documented deltas) vs legacy replay on extended fixtures | **Met** (historical); parity tests removed with legacy replay in **5.9** |
| **4.3** | `outputAssertions.getGuidanceContext` uses xterm for `replayedPlain` only | **Met:** targeted `cli_access_token`, `cli_recall`, `cli_install_and_run` Cypress specs green |

**Reference (historical):** Phase 4 used headless xterm.js to map **feed bytes → viewport plaintext**. Implementation notes (Node `Terminal`, addons, headless CI) live in the sub-phase doc.

**Named acceptance example:** `cli_access_token.feature` — `"E2E CLI Token"` in Current guidance; recall guidance steps stay green on the same choke point.

**Phase gate (after 4.3, historical):** Subsequent Phase **5** work moved all replay to xterm and **5.9** deleted the hand-rolled implementation.

**Explicitly deferred to Phase 5:** Facade replay migration; past assistant / answered / scrollback **locators** (viewport vs full buffer vs stripped transcript); removing legacy replay from production paths; assertion API consolidation; Doughnut E2E steps that should stop searching **entire** PTY history when a narrower surface matches user-visible intent.

---

## Phase 5 — Locators, tidy assertion APIs, finish xterm migration

**Sub-phases (detail, gates, locator design notes):** [`ongoing/cli-phase5-tty-assert-api-xterm-finish-subphases.md`](./ongoing/cli-phase5-tty-assert-api-xterm-finish-subphases.md).

**Outcome:** One coherent, documented **assertion surface** in `tty-assert` — explicit **search surfaces** (viewport vs full xterm buffer vs stripped cumulative transcript where still appropriate), **poll + timeout** helpers in the library (row-major matching, strict duplicate handling), and **all** replay-based product paths on xterm. **Doughnut E2E** updates **some** checks that searched the **entire** PTY history so they use **better locators** aligned with where users actually see text.

**Legacy hand-rolled replay:** Removed in sub-phase **5.9** (module, parity/unit tests that only targeted it, export path, `check-legacy-replay-imports.sh`).

**Work:**

- **Finish xterm migration:** Route **`facade`** (and any remaining wiring) through xterm-backed replay (**5.1–5.2 met**); hand-rolled replay **deleted** in **5.9**. Scrollback-aware search uses **`waitForTextInSurface`** with **`fullBuffer`** / **`viewableBuffer`** (**5.3 met**); viewport **`\n`‑joined** replay remains **`ptyTranscriptToViewportPlaintext`** for guidance heuristics.
- **Locator primitives in `tty-assert`:** **`waitForTextInSurface`** (**5.3**); Cypress adapter wiring in **5.5+**.
- **Doughnut:** Refactor `outputAssertions` / Cypress adapter to use those helpers; **inventory** CLI steps and migrate scenarios so assertions target the **right** surface; document per-fluent contract (past assistant vs guidance vs …).
- **Consolidate** duplicate strip/replay/poll paths; keep Ink-specific heuristics in Doughnut adapters.
- **Sub-phase 5.9:** **Met** — legacy replay module, dedicated tests, `tty-assert/ptyTranscriptToVisiblePlaintext` export, and grep script removed; `tt/` not present.

**Gate:** E2E green for touched CLI features; `tty-assert` README describes strip vs replay vs locators; `outputAssertions` has no undocumented dual replay/locator paths; **5.9** deletion pass complete.

---

## Phase 6 — Tidy start and terminating APIs of `tty-assert`

**Status:** **Complete** (6.1–6.5). **Sub-phases (detail, design decisions, gates):** [`ongoing/cli-phase6-tty-assert-managed-session-subphases.md`](./cli-phase6-tty-assert-managed-session-subphases.md).

**Outcome:** `tty-assert` owns one managed interactive session: **start session** (spawn, env, geometry), keep the **live PTY + xterm mirror + retry/assertion state** together, **write**, **assert/read state**, then **dispose** with clear teardown semantics. Doughnut Cypress code stays a thin adapter and uses **`cliAssert`** with serialized payloads (no browser-side raw-buffer polling).

**Delivered:**

- `packages/tty-assert/src/managedTtySession.ts`: `startManagedTtySession`, `attachManagedTtySession`, incremental xterm sync, polling `assert`, idempotent `dispose`, `dumpDiagnostics` (integrators import `startManagedTtySession` and types from the package root).
- Plugin: `cliAssert` → `managed.assert`; startup wait uses managed `assert`; **`cliInteractivePtyGetBuffer` removed** (unused).
- `e2e_test/start/pageObjects/cli/outputAssertions.ts`: request builders for current guidance, transcript, full-buffer + cell expectations.

**Gate:** **Met** — `pnpm tty-assert:test` green; CLI E2E paths for interactive assertions use `cliAssert` only.

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

## Phase 11 — Package surface, README truth, and prior-art wording

**Status:** **Complete.**

**Outcome:** What the README promises matches what **`package.json` `exports`** (and TypeScript resolution) actually allow; bibliographic noise and stale notes are gone so external readers are not misled.

**Work:**

- Add or adjust **`exports`** so documented entry points (e.g. `waitForTextInSurface`, helpers used from adapters) are real, **or** narrow README examples to **`tty-assert` root** only until subpaths ship.
- Remove **third-party prior-art** name-drops and links from **`packages/tty-assert`** sources and README; describe row-major search and strict matching in neutral terms (same behavior, no citation trail). Optionally trim duplicate bibliographic lines in **`ongoing/*`** phase notes that only referenced that prior art.
- Delete stale **local research tree** wording from README if it no longer applies.

**Gate:** **Met** — `pnpm tty-assert:lint` + `pnpm tty-assert:test` green; README documents **root-only** `exports`; `packages/tty-assert` sources and README contain no third-party prior-art links.

---

## Phase 12 — Neutral failure copy and clearer symbols

**Status:** **Complete.**

**Outcome:** Assertion failures and API names read **generic** at the library layer; misleading labels are fixed without changing what Doughnut E2E asserts.

**Delivered:**

- Shared validation and strict-mode preambles use **`tty-assert:`** (not **`waitForTextInSurface:`**) so **`ManagedTtySession.assert`** and **`waitForTextInSurface`** share accurate copy.
- **Palette background** cell-check failures are neutral (no past-user / chalk product wording); palette-index-8 hint references **`\\x1b[100m`** only as an escape example.
- **`dumpFrames` → `dumpDiagnostics`** (method + **`TtyAssertDumpDiagnostics`**); README and plan references updated.

**Gate:** **Met** — `pnpm tty-assert:test` + `pnpm tty-assert:lint` green; no E2E plugin path used **`dumpFrames`** (none).

---

## Phase 13 — Shared assertion loop and adapter types

**Status:** **Complete.**

**Outcome:** One place owns **poll + timeout + strict** for surface asserts; duplicate **diagnostic shapes** and **Cypress JSON** bridging stop drifting.

**Delivered:**

- **`pollSurfaceAssertLoop`** — internal driver used by **`waitForTextInSurface`** and **`ManagedTtySession.assert`**.
- **`TtyAssertDumpDiagnostics`** + **`buildTtyAssertDumpDiagnostics`** — single type and builder for **`ManagedTtySession.dumpDiagnostics`** (removed **`ManagedTtySessionDumpDiagnostics`** alias).
- **`TtyAssertStrictModeViolationError`** in **`ttyAssertStrictModeError.ts`** (re-exported from **`waitForTextInSurface`**) to avoid import cycles with the poll module.
- **`ManagedTtyAssertJsonPayload`**, **`SerializableRegExp`**, **`regExpFromSerializable`**, **`managedTtyAssertOptionsFromJson`** on package root; **`cliE2ePluginTasks`** uses them (**`ManagedTtyAssertTaskPayload`** is a type alias).

**Gate:** **Met** — `pnpm tty-assert:test` + `pnpm tty-assert:lint` green; no behavior change intended beyond safer refactors (CLI E2E not re-run in this pass).

---

## Phase 14 — Remove unused `facade` API

**Status:** **Complete.**

**Outcome:** No second interactive terminal handle API; **`managedTtySession` + `waitForTextInSurface`** are the supported shapes.

**Delivered:** Removed **`facade.ts`** and **`facade.test.ts`** (behavior already covered by **`ptyTranscriptToVisiblePlaintextViaXterm`** and **`managedTtySession`** tests). README and plan anchors no longer mention **`facade`** as an integrator-facing module.

**Gate:** **Met** — `pnpm tty-assert:test` green; no production callers used **`facade`**.

---

## Phase 15 — Move out of Doughnut (OSS npm package)

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
- **4 → 5 → 6:** Phase 4 (sub-phases 4.1–4.3) lands xterm replay for **`getGuidanceContext` only**; Phase 5 completes xterm migration (facade **5.1–5.2**), **locators** (**5.3**), E2E adapter + inventory (**5.5–5.8**), then **5.9** removes obsoleted code; Phase 6 is lifecycle API — **4.3** and Phase 5+ end with E2E green; **4.1–4.2** gate on `tty-assert` only.
- **7–10** are mostly sequential in **diagnostic value**; **9–10** may share rendering infrastructure (**8 → 9** especially).
- **11–14** improve **package honesty, cohesion, and internals** before publish; they can overlap **7–10** where there is no conflict, but **complete 11–14 before 15** so the published tarball matches docs and has no dead public sketch.
- **15** last.

---

## Out of scope for this plan

- Replacing Cypress PTY E2E with Vitest `runInteractive` for scenarios already covered in `cli/tests/`; this plan does **not** require migrating those tests.
- Changing **what** Doughnut CLI renders — only **how tests observe and report** failures.

---

## When this plan changes

Update this file as phases complete or scope shifts; remove stale notes.
