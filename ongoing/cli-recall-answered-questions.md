# CLI recall — answered questions (rich session UI)

**Status:** Plan only — not started for rich UI. Baseline plumbing is already in place.

**Scope:** During `/recall`, after each question is answered, show a **readonly** summary in the “answered questions” region: breadcrumb ending in note title, note-facing content, and type-specific outcome (just review / spelling / MCQ). Only items answered **in this session**; **no** loading of “answered earlier today” (or any history) from the backend.

**Out of scope for this plan:** Changing backend APIs; new Cucumber scenarios (strengthen existing steps only).

**Current baseline (already implemented)**

- Recall stages now emit `RecallQuestionAnswerOutcome` to `onRecallQuestionAnswered` in `cli/src/commands/recall/RecallSessionStage.tsx`.
- `RecallSessionStage` appends each outcome line via `useSessionScrollbackAppend()` + `recallAnsweredLine(...)`.
- `onSettled` usage is reduced to stage/session lifecycle outcomes (summary, stop, fatal errors), not per-answer success lines.
- `RecallAnsweredRow` currently renders plain text (`cli/src/commands/recall/recallAnsweredScrollback.tsx`), which is the extension point for the rich readonly UI.

**References**

- Recall orchestration: `cli/src/commands/recall/RecallSessionStage.tsx`.
- Per-question outcome contract: `cli/src/commands/recall/recallQuestionAnswerOutcome.ts`.
- Just-review answer flow: `cli/src/commands/recall/JustReviewRecallStage.tsx`.
- Spelling answer flow: `cli/src/commands/recall/SpellingRecallStage.tsx`.
- MCQ answer flow + contest notices: `cli/src/commands/recall/RecallMcqStage.tsx`.
- Answered scrollback row: `cli/src/commands/recall/recallAnsweredScrollback.tsx`.
- Web parity (styling intent): `frontend/src/components/recall/QuestionChoices.vue`, `frontend/src/components/recall/AnsweredSpellingQuestion.vue`, `frontend/src/components/recall/NoteUnderQuestion.vue`.
- E2E assertions: `e2e_test/features/cli/cli_recall.feature` and `e2e_test/start/pageObjects/cli/outputAssertions.ts`.
- Session scrollback architecture: `ongoing/cli-session-scrollback.md`.
- Test constraints: `.cursor/rules/cli.mdc` — `runInteractive` + **mock only** `doughnut-api` controllers; no fixed `setTimeout` waits.

---

## Design constraints (carry through all phases)

1. **Readonly / CPU:** Prefer **immutable** per-answer snapshots rendered as static scrollback rows. Avoid coupling answered-row rendering to active-card keystroke state.
2. **Breadcrumb:** Match the **web mental model**: path from notebook / ancestors through **note title** (use `noteTopology` or equivalent fields already present on recall payloads / `MemoryTracker` shapes — same source the CLI already uses for notebook lines). Exact separator characters can follow an existing CLI convention or mirror web `Breadcrumb` order.
3. **Scrollback after leaving recall:** Keep answered items in the shared session scrollback path (`appendScrollbackItem`), so they survive recall stage unmount and remain visible via terminal scrollback.
4. **Terminal width:** Use existing helpers (`resolvedTerminalWidth`, grapheme-aware wrapping) for any wrapped markdown in answered blocks — do not use raw string `.length` for layout.
5. **No answered-via-onSettled regression:** New answered-question UI data should flow through `onRecallQuestionAnswered`/scrollback append, not through `onSettled` text lines.

---

## Phase 1 — Answered row data model for rich readonly blocks

**User-visible behavior:** No visible style change yet; behavior remains current while the row payload model is upgraded.

**Implementation slice:** Introduce a typed readonly answered-row payload (instead of plain single-line text) that can represent just-review/spelling/MCQ fields (breadcrumb parts, details markdown/plain lines, user answer, correctness, optional choices).

**Tests**

- **Unit (Vitest):** Extend high-level recall tests to assert that existing answered strings still appear and session summary flow remains unchanged (guard against breakage while migrating payload shape).
- **E2E:** No new assertions required in this phase.

---

## Phase 2 — Just review rich answered block

**User-visible behavior:** After just-review answer, answered block shows:

- Breadcrumb ending with note title.
- Note details (readonly markdown).
- Explicit user choice/result text (remembered or reduced memory index).

**Tests**

- **Unit:** High-level `runInteractive` just-review paths assert breadcrumb tail, details snippet, and choice/result wording in answered region.
- **E2E:** Strengthen existing just-review scenario assertions in `cli_recall.feature` (no new scenarios).

---

## Phase 3 — Spelling rich answered block + correct/wrong styling

**User-visible behavior:** After spelling submission, answered block shows:

- Breadcrumb ending with note title.
- Note details (readonly markdown).
- User submitted spelling answer text.
- Visual distinction for correct vs incorrect (terminal style aligned with web success/error intent).

**Tests**

- **Unit:** High-level spelling correct/wrong paths via `runInteractive`; assert answer text and style cue in output.
- **E2E:** Strengthen existing spelling scenario assertions only.

---

## Phase 4 — MCQ rich answered block with frontend-like choice styling

**User-visible behavior:** After MCQ submission, answered block shows:

- Breadcrumb ending with note title.
- Stem and all choices in readonly form.
- Styling analogous to frontend answered state (`is-correct` / selected wrong emphasis).

**Tests**

- **Unit:** High-level MCQ flows (correct/wrong/contest-regenerate then answer) assert rendered answered block and style cues.
- **E2E:** Strengthen existing MCQ scenario assertions (no new scenarios).

---

## Phase 5 — Consistency, copy, and terminology

**User-visible behavior:** Unify spacing/separators between answered blocks, align concise copy, and ensure mixed-type answered rows stack clearly.

**Tests:** Run affected Vitest files and the `cli_recall.feature` spec; fix drift only.

---

## Phase discipline (when implementing)

- Each phase: implement the smallest slice, **tests green**, then merge/deploy per team gate (see `.cursor/rules/planning.mdc`).
- **No dead code:** Remove obsolete plain-string helpers once rich typed rows fully replace them.
- **Architecture roadmap:** `ongoing/cli-architecture-roadmap.md` — prefer stage-local composition and transcript boundaries; only introduce new shared abstractions if a second consumer appears.

---

## Open decisions (resolve during Phase 1 implementation)

- **Outcome shape placement:** Keep `RecallQuestionAnswerOutcome` minimal and move rich answered payload creation to stage components, or expand the outcome type to carry structured display data.
- **MCQ contest notices:** Keep contest rejection messages in answered region style, or separate them visually from answered question blocks.
