# CLI interactive input-ready signal — simplify for tests

Informal plan; update or remove when done. **Testing:** observable behavior — Vitest `runInteractive` / Cypress PTY steps; see `.cursor/rules/planning.mdc`, `.cursor/rules/cli.mdc`, `.cursor/rules/e2e_test.mdc`.

---

## Problem

Production emits a **private OSC** (`INTERACTIVE_INPUT_READY_OSC` in `cli/src/renderer.ts`) when the TTY input box is ready for keystrokes (empty draft, not in interactive fetch-wait). **Tests** depend on it in two places:

| Surface | Role |
|---------|------|
| **Vitest** | A few cases assert the sequence appears in captured stdout (e.g. MCQ panel shown, post–fetch-wait ready). |
| **E2E (`cliPtyRunner`)** | Waits for prompt stability after each PTY action: `waitForInteractiveInputReadyOsc` plus **`waitForInteractiveInputReadyOscOrSettled`** when fetch-wait omits the OSC — with **timings**, **length-stability** polling, and **`tailLooksLikeInteractiveFetchWait`** heuristics duplicated from fetch-wait copy. |

The E2E layer is **fragile and complex** because the OSC is **absent** during fetch-wait and **chunked** PTY delivery requires drain/settled logic. The byte sequence is also **duplicated** (`renderer.ts` vs `e2e_test/step_definitions/cliSectionParser.ts`).

---

## North star

**End state:** test synchronization is based on **manual-observable terminal behavior only** (prompt/guidance/loading transitions), with **no OSC dependency** in E2E or Vitest.

**Interim state:** keep OSC as a temporary sync primitive only while visible-state waiters are introduced and proven stable.

**Non-goals (unless a phase explicitly expands):** Changing user-visible TTY layout; rewriting the whole Ink shell.

---

## Constraints

- **Production:** OSC must stay **private** (not FinalTerm 133) so shell integration does not treat it as a prompt boundary — see comment on `INTERACTIVE_INPUT_READY_OSC` in `renderer.ts`.
- **OSC scope:** Emitted for **TTY interactive** only; subcommand / script runs (`version`, `update`) have no input-ready OSC — E2E distinguishes PTY vs one-shot stdout (`outputAssertions.ts`).
- **Fetch-wait:** Today the OSC is **omitted** while loading; any simplification must **preserve** correct sync for slow paths (no flaky “type before UI ready”).
- **Removal bar:** do not remove OSC until visible-state waiters pass repeated interactive E2E runs (including fetch-wait and MCQ paths) with no regression in flake rate.

---

## Phases (order by value)

### Phase 1 — Single source of truth for the marker

**Outcome:** E2E (and any other package) does not hardcode a second copy of the OSC string.

**Direction:** Export a tiny shared constant or re-export from one package the E2E runner can import (e.g. `doughnut-test-fixtures` boundary, or generated path — pick the smallest dependency edge the team accepts). Until then, document the duplication as debt in code comments linking both files.

**Verify:** Grep shows one definition; E2E still green on a representative `cli/*.feature`.

### Phase 2 — Contract doc + test comments (regression anchor)

**Outcome:** One short description of **when** the OSC is emitted vs omitted, and what **`interactiveInputReadyOscSuffix`** encodes — linked from `cliPtyRunner.ts` / `ttyAdapter` `handleShellRendered` call chain.

**Verify:** No behavior change; optional Vitest that fails if suffix logic is removed without updating the doc (only if it pays for itself).

### Phase 3 — Reduce PTY waiter complexity (main simplification)

**Outcome:** Fewer tuned constants and heuristics, **or** a more reliable readiness signal so the fallback path is rare.

**Options to evaluate (pick smallest change that removes flakiness):**

1. **Emit a distinct test-only marker** on stderr or a debug fd when `DOUGHNUT_CLI_E2E_*` (or similar) is set — PTY runner waits on that instead of OSC+settled. *Tradeoff:* env flag discipline; must not ship to users by accident.
2. **Always emit OSC on every stable paint** including fetch-wait with a payload variant (e.g. `…-busy` vs `…-ready`) so E2E never needs “settled without OSC” — *Tradeoff:* product/terminal noise; must stay invisible and private.
3. **Tighten Ink/adapter timing** so post-OSC drain needs shorter windows — *Tradeoff:* may be platform-dependent; measure in CI.
4. **Consolidate** `waitForInteractiveInputReadyOsc` and `OrSettled` into one implementation with explicit states (OSC seen | fetch-wait tail | settled) and **logged timeout messages** for debugging.

**Verify:** Run interactive E2E specs that touch fetch-wait and MCQ repeatedly; no new flakes. Prefer extending an existing scenario over many new ones.

### Phase 4 — Vitest alignment

**Outcome:** Interactive Vitest either asserts through **stable user-visible text** where sufficient, or imports the **same canonical** marker as E2E — no third copy.

**Verify:** `pnpm cli:test` interactive suite.

### Phase 5 — Add visible readiness predicates in E2E (manual-observable contract)

**Outcome:** `cliPtyRunner` can wait for readiness using **only visible transcript/simulated-screen conditions**, not OSC bytes.

**Direction:** Add explicit readiness predicates built from existing parser helpers (`ptyTranscriptSimulatedPlainScreen`, section parsers), e.g.:

- command line row visible and stable
- not in fetch-wait visible state (`loading ...` and known fetch-wait labels absent when expecting input)
- expected panel state present (MCQ choices, yes/no prompt, token list) before sending next key

Keep this path behind an internal toggle (`osc` vs `visible`) so both strategies can be run in CI while migrating.

**Verify:** targeted interactive `cli/*.feature` specs pass under visible strategy.

### Phase 6 — Make visible strategy default; OSC fallback only

**Outcome:** all interactive E2E uses visible waits by default; OSC path exists only as fallback/debug gate.

**Direction:** switch default waiter mode to visible strategy, keep temporary fallback for triage, and add failure diagnostics that explain which visible predicate timed out.

**Verify:** repeat-run fetch-wait + MCQ + yes/no scenarios; confirm no increased flakes vs baseline.

### Phase 7 — Remove OSC from tests completely

**Outcome:** no test code depends on OSC sequence.

**Direction:**

1. Remove OSC-based wait path and test constants from E2E.
2. Remove Vitest assertions that require OSC bytes.
3. Remove any test-only docs/comments that prescribe OSC sync.

Keep production OSC temporarily if needed for non-test consumers; removal from production path can be a follow-up plan only if still justified.

**Verify:** interactive E2E and CLI Vitest suites stay green with visible-only synchronization.

---

## Done checklist

- [ ] Canonical OSC string in one place (interim).
- [ ] Contract documented (emit / omit / suffix) with migration note.
- [ ] `cliPtyRunner` supports visible readiness predicates.
- [ ] Visible strategy is default in E2E.
- [ ] OSC removed from all tests (E2E + Vitest assertions).
- [ ] Interactive E2E + Vitest green under visible-only sync.

---

## References

- `cli/src/renderer.ts` — `INTERACTIVE_INPUT_READY_OSC`, `interactiveInputReadyOscSuffix`
- `cli/src/adapters/interactiveTtyStdout.ts` — writes OSC
- `cli/src/adapters/interactiveTtySession.ts` — `handleShellRendered`, fetch-wait buffering notes
- `e2e_test/config/cliPtyRunner.ts` — wait loops and timings
- `e2e_test/step_definitions/cliSectionParser.ts` — visible transcript/screen parsing helpers
- `e2e_test/start/pageObjects/cli/outputAssertions.ts` — PTY vs non-interactive output assertions
- `ongoing/cli-strict-ink-north-star.md` — strict caret / TextInput follow-up
