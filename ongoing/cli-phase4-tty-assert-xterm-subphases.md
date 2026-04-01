# Phase 4 sub-phases — xterm.js replay for Current guidance

**Parent plan:** [`cli-terminal-test-library-extraction.md`](./cli-terminal-test-library-extraction.md) (Phase 4: xterm-backed replay for **`getGuidanceContext`** only; Phase 5 finishes migration + API tidy).

**This document:** Splits Phase 4 per `.cursor/rules/planning.mdc` — **one deliverable per sub-phase**, each with an explicit **gate**. **Sub-phases 4.1–4.2 are complete**. **4.3** remains (`outputAssertions` wiring).

**Reference:** [microsoft/tui-test](https://github.com/microsoft/tui-test) — xterm.js for terminal rendering; borrow **feed bytes → read buffer text**, not their test runner.

---

## Scope boundaries (all of Phase 4)

| In scope | Out of scope (Phase 5+) |
|----------|---------------------------|
| `packages/tty-assert`: new xterm replay helper + tests | `facade.ts` replay, past assistant / answered / past user / non-interactive paths |
| `outputAssertions.ts`: **`getGuidanceContext`** switches `replayedPlain` to xterm helper | Removing legacy `ptyTranscriptToVisiblePlaintext` from package exports |
| `extractCurrentGuidanceFromReplayedPlaintext` unchanged (Doughnut heuristic) | Assertion API consolidation |

**Acceptance scenario (scenario-first):** [`e2e_test/features/cli/cli_access_token.feature`](../e2e_test/features/cli/cli_access_token.feature) — `And I should see "E2E CLI Token" in the Current guidance` is the smallest clear proof of Cypress → `tty-assert` → xterm → guidance assertion. Recall features using Current guidance must stay green under the same wiring.

---

## Sub-phase 4.1 — xterm dependency and replay primitive

**User-visible outcome:** None. **Developer outcome:** `tty-assert` can turn a **raw PTY transcript** into a **replay-shaped plain string** via xterm (same **geometry** as today: `CLI_INTERACTIVE_PTY_COLS` × `CLI_INTERACTIVE_PTY_ROWS` from [`geometry.ts`](../packages/tty-assert/src/geometry.ts)).

**Work:**

- Add `@xterm/xterm` and any addon required to read plain text from the terminal buffer in **Node** (confirm headless path against CI — see tui-test / xterm docs).
- New module + export (name TBD, e.g. `ptyTranscriptToVisiblePlaintextViaXterm`) that: creates a fresh `Terminal`, sets dimensions, feeds `raw` in order, returns plaintext suitable for passing into `extractCurrentGuidanceFromReplayedPlaintext`.
- **Smoke tests** in `packages/tty-assert/tests/`: at least one minimal transcript produces stable non-empty plain output (black-box on inputs/outputs).

**Gate:** `pnpm tty-assert:test` and `pnpm tty-assert:lint` green; **no** edits yet to `e2e_test/` for replay wiring. Doughnut Cypress behavior unchanged by definition.

**Done:** `@xterm/headless` + `@xterm/xterm` 6.0.0 on `tty-assert`; export `ptyTranscriptToVisiblePlaintextViaXterm` (`packages/tty-assert/src/ptyTranscriptToVisiblePlaintextViaXterm.ts`); tests in `ptyTranscriptToVisiblePlaintextViaXterm.test.ts`. Headless `Terminal` + `buffer.active` viewport lines; `write` completes via callback before reading the buffer.

**TDD note:** If preferred, add a **failing** test that asserts the new API exists and handles a tiny fixture, then implement until green — keep **one** intentionally failing test at a time while driving 4.1.

---

## Sub-phase 4.2 — Parity vs legacy replay (fixtures)

**User-visible outcome:** None. **Developer outcome:** Confidence that xterm replay matches **`ptyTranscriptToVisiblePlaintext`** for the transcripts that matter to Current guidance, or **documented deltas** with tight, named fixtures.

**Work:**

- Extend Vitest coverage: reuse and extend fixtures from [`ptyTranscriptToVisiblePlaintext.test.ts`](../packages/tty-assert/tests/ptyTranscriptToVisiblePlaintext.test.ts); add CLI-shaped samples if gaps appear.
- For each fixture: compare legacy vs xterm output; **either** assert equality **or** assert a deliberate documented difference (comment + minimal case) if a mismatch is unavoidable and acceptable for guidance extraction.

**Gate:** `tty-assert:test` green; still **no** Doughnut `outputAssertions` switch.

**Done:** `tests/ptyTranscriptReplayParity.test.ts` — CRLF two-line parity with legacy; SGR + CRLF (Ink-shaped) parity; strict parity for CR/LF, ED clear, wrap; **documented deltas:** bare LF (legacy resets column, xterm VT LF-only) and one synthetic multi-line scroll fixture (legacy vs xterm column/scroll detail).

---

## Sub-phase 4.3 — Wire Doughnut Current guidance to xterm replay

**User-visible outcome:** Unchanged Gherkin expectations; **observable to QA/automation:** Current guidance assertions now exercise the xterm replay path end-to-end.

**Work:**

- In [`outputAssertions.ts`](../e2e_test/start/pageObjects/cli/outputAssertions.ts), change **`getGuidanceContext` only**: compute `replayedPlain` with the xterm helper from 4.1 instead of `ptyTranscriptToVisiblePlaintext`.
- Leave **`ptyTranscriptToVisiblePlaintext`** exported and used elsewhere (`facade`, any other imports) until Phase 5.

**Gate:** Full **CLI-relevant** Cypress green (same bar as parent Phase 4). For routine runs, prefer **targeted specs** over the whole suite: at minimum `e2e_test/features/cli/cli_access_token.feature`, `cli_recall.feature`, and `cli_install_and_run.feature` (install does not use Current guidance but catches accidental breakage in shared helpers). Use: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec '<paths>'` per `.cursor/rules/e2e_test.mdc`.

**Phase-complete checklist (parent plan):** Update [`cli-terminal-test-library-extraction.md`](./cli-terminal-test-library-extraction.md) Phase 4 status when 4.3 merges; trim or archive this sub-phase doc when Phase 4 is fully done if nothing left to track.

---

## Dependency chain

- **4.1 → 4.2:** Primitive must exist before parity tests are meaningful.
- **4.2 → 4.3:** Do not switch Doughnut until parity (or documented deltas) is acceptable; otherwise failures are ambiguous (wiring bug vs emulator mismatch).

---

## Deferred (Phase 5 — parent plan)

Facade replay migration; optional past-assistant / answered semantics; removing legacy replay from production paths; assertion API tidy.
