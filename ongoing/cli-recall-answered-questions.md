# CLI recall — answered questions (rich session UI)

**Status:** Plan only — not started.

**Scope:** During `/recall`, after each question is answered, show a **readonly** summary in the “answered questions” region: breadcrumb ending in note title, note-facing content, and type-specific outcome (just review / spelling / MCQ). Only items answered **in this session**; **no** loading of “answered earlier today” (or any history) from the backend.

**Out of scope for this plan:** Changing backend APIs; new Cucumber scenarios (strengthen existing steps only).

**References**

- Current shell: `RecallSessionChrome` + `answeredRecallLines: string[]` in `cli/src/commands/recall/RecallSessionStage.tsx` (plain `Text` lines; spelling success appends a line; MCQ uses `onMcqSessionNotice` for contest copy only; just-review success often does **not** append to `answeredRecallLines`).
- MCQ submit / wrong path: `cli/src/commands/recall/RecallMcqStage.tsx` (`onSettled('Incorrect.')` / `onMcqSucceeded` → `onSettled('Correct!')` when no next card).
- Spelling: `cli/src/commands/recall/SpellingRecallStage.tsx` (wrong → `onSettled('Incorrect.')`; correct → `onSpellingSessionComplete` → spell line only when chaining to next card).
- Web parity (styling intent): `frontend/src/components/recall/QuestionChoices.vue` (correct = green panel, selected wrong = red), `AnsweredSpellingQuestion.vue` (success vs error alert), `NoteUnderQuestion.vue` + `Breadcrumb` (topology trail).
- E2E: `e2e_test/features/cli/cli_recall.feature` + `answeredQuestions().expectContains` in `e2e_test/start/pageObjects/cli/outputAssertions.ts` (today: substring on full ANSI-stripped PTY transcript).
- Tests: `.cursor/rules/cli.mdc` — `runInteractive` + **mock only** `doughnut-api` controllers; no fixed `setTimeout` waits.

---

## Design constraints (carry through all phases)

1. **Readonly / CPU:** Prefer **immutable** per-answer snapshots (frozen props or static strings) rendered with `React.memo` or plain `Text` trees so answered rows do not depend on live stage state. Avoid re-wrapping markdown for answered blocks on every keystroke of the active card.
2. **Breadcrumb:** Match the **web mental model**: path from notebook / ancestors through **note title** (use `noteTopology` or equivalent fields already present on recall payloads / `MemoryTracker` shapes — same source the CLI already uses for notebook lines). Exact separator characters can follow an existing CLI convention or mirror web `Breadcrumb` order.
3. **Scrollback after leaving recall:** When the recall stage ends (session summary, user confirms leave, or terminal error exit from a card), users must still **scroll up** and see the answered blocks. That implies **not** losing Ink output solely tied to the recall stage subtree: either append finalized blocks to the **same persistent interactive transcript** used for past assistant content, or an equivalent mechanism that survives stage unmount. Decide in implementation; verify in the persistence phase.
4. **Terminal width:** Use existing helpers (`resolvedTerminalWidth`, grapheme-aware wrapping) for any wrapped markdown in answered blocks — do not use raw string `.length` for layout.

---

## Phase 1 — Just review: rich answered block (session + scrollback)

**User-visible behavior:** After the user answers **Yes, I remember?** (and the flow continues or ends), an answered block appears that includes:

- Breadcrumb ending with the note title.
- Note **details** (same markdown→terminal rendering as the live card / current prompt, readonly).
- Explicit text for the user’s choice (remembered vs not recalled), appropriate to the actual outcome.

Only questions answered in this session appear in this list.

**Persistence:** The same block remains visible in terminal scrollback after the recall session step completes (e.g. after “Recalled *n* notes” or “Reviewed: …” paths), not only while the stage is mounted.

**Tests**

- **Unit (Vitest):** `runInteractive` through the real recall entrypoint; `vi.spyOn` on `RecallsController` / `MemoryTrackerController` / recall-marking API as needed; `makeMe` for payloads. Assert stdout contains breadcrumb tail (note title), a fragment of note detail, and the remembered / not-recalled wording as appropriate.
- **E2E:** Strengthen `cli_recall.feature` **Recall using just review** and **Complete all due notes…** (and similar) with extra `Then` lines: breadcrumb/title substring, short detail substring, choice wording — without adding new scenarios.

---

## Phase 2 — Spelling: rich answered block + correct / wrong styling

**User-visible behavior:** After the user submits a spelling answer, an answered block shows:

- Breadcrumb ending with note title.
- Note details (readonly markdown).
- The user’s submitted answer text.
- **Visual distinction** for correct vs incorrect (terminal SGR / Ink `color` / chalk — align with web success vs error intent from `AnsweredSpellingQuestion.vue`, without requiring pixel parity).

Preserve the existing **flow** after spelling (e.g. correct → just-review chain, wrong → session exit with incorrect outcome) unless product explicitly changes it; this phase is primarily **presentation** of the answer in the answered region and scrollback.

**Tests**

- **Unit:** Spelling correct and wrong paths through `runInteractive`; assert styled markers (e.g. green vs red ANSI) plus plaintext fragments for answer and breadcrumb.
- **E2E:** Strengthen **Recall spelling — correct answer then just review**; if wrong spelling is covered elsewhere, extend assertions there, else rely on unit for wrong path unless an existing scenario already hits it.

---

## Phase 3 — MCQ: answered block with frontend-like choice styling

**User-visible behavior:** After the user submits an MCQ answer, an answered block shows:

- Breadcrumb ending with note title.
- Stem (markdown) and **all choices**, with styling analogous to the web answered state: **correct** choice highlighted like `QuestionChoices.vue` `is-correct`; **selected wrong** choice like `is-selected:not(.is-correct)`; unselected incorrect options visually subdued or neutral as on web.

When the user answers multiple MCQs in one session, **stack** multiple blocks in order; earlier blocks stay readonly.

**Tests**

- **Unit:** Mock OpenAI / recall prompts path as today; assert stem snippet, choice labels, and ANSI or structural cues for correct vs selected-wrong.
- **E2E:** Strengthen **MCQ — choose the correct answer**, **MCQ — wrong choice…**, and **MCQ — contest and regenerate…** with substrings for breadcrumb and at least one styling-sensitive check (extend `outputAssertions.ts` only if needed — e.g. helper mirroring `expectContainsBold` for success/error colors, reusing existing SGR patterns).

---

## Phase 4 — Consistency, copy, and terminology

**User-visible behavior:** Unify spacing/separators between answered blocks (optional blank line or rule), align light copy with web where it helps (“Note under question” vs minimal CLI label — **keep CLI terse** unless E2E depends on a phrase). Update **`.cursor/rules/cli.mdc`** domain table row for **answered questions** if the PTY layout or transcript rules change (e.g. relationship to past assistant messages).

**Tests:** Run affected Vitest files and the `cli_recall.feature` spec once; fix drift only.

---

## Phase discipline (when implementing)

- Each phase: implement the smallest slice, **tests green**, then merge/deploy per team gate (see `.cursor/rules/planning.mdc`).
- **No dead code:** Remove obsolete string-only answered lines once superseded.
- **Architecture roadmap:** `ongoing/cli-architecture-roadmap.md` — prefer stage-local composition and transcript boundaries; only introduce new shared abstractions if a second consumer appears.

---

## Open decisions (resolve during Phase 1 implementation)

- **Where scrollback lives:** Single append to interactive assistant transcript vs dedicated “answered” transcript slice — must be one coherent scrollback experience for the user.
- **Incorrect MCQ** currently ends the stage with a short line; confirm whether the **full** MCQ answered block is shown **before** that settlement line in all cases.
