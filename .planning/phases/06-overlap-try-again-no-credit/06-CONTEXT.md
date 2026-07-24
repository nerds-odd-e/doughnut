# Phase 6: Overlap "try again, no credit" - Context

**Gathered:** 2026-07-24
**Status:** Ready for planning
**Mode:** `--auto` (all gray areas auto-selected; recommended option chosen for each; no interactive prompts)

<domain>
## Phase Boundary

When a spelling answer is **correct for the reviewed note** but that answer is **non-distinguishing** because the reviewed note **declares overlap** (alias-as-wiki-link from Phase 5), grade the attempt as **OVERLAP**: tell the user **"correct, but we're looking for another answer — try again,"** withhold SRS credit, mutate neither note content nor the forgetting curve, and let the user **retry the same review**.

**In this phase (Behavior — OVL-01):**
- Detect overlap at the spelling grading moment in `MemoryTrackerService.answerSpelling` (correct path), using Phase 5 `FrontmatterAliases.overlapWikiLinkTokensFrom*`.
- Set `Answer.outcome = OVERLAP`, `AnsweredQuestion.overlap = true`, and withhold credit (no SRS success, no wrong/partial-fail mutation).
- Surface a distinct try-again message in the spelling result UI and keep the user on the same memory tracker for another attempt.
- Leave accidental-match (Phases 2–4) and plain-alias consumers (Phase 5 OVL-03) unchanged.

**Not in this phase:**
- Changing how overlap is *declared* (Phase 5 already shipped OVL-02/OVL-03).
- Accidental-match penalty, reveal, or offer-link behavior.
- Auto-detecting overlap from shared titles without a wiki-link declaration (PROJECT lock).
- MCQ / fuzzy match / save-time existence checks for overlap targets.

</domain>

<decisions>
## Implementation Decisions

### Overlap trigger gate
- **D-01:** Fire **OVERLAP** only when all of the following hold: (1) `Note.matchAnswer` is **true** for the reviewed note; (2) the reviewed note has one or more overlap wiki-link tokens from `FrontmatterAliases.overlapWikiLinkTokensFromNoteContent`; (3) at least one of those tokens **resolves** to another note; (4) the **same spelling answer also matches** that resolved target note (title or **plain** alias — same exact case-insensitive semantics as `matchAnswer` / accidental-match). If declarations exist but none resolve, or none of the resolved targets also match the answer, grade as normal **CORRECT** with credit. — **Reversibility:** reversible — grading predicate; can widen to “any correct while any declaration exists” later, but that would permanently block credit unless a second success path is invented.
  - Rationale: OVL-01 asks for “another answer” — distinguishing plain aliases (Phase 5 example: `color` beside `[[Other Note]]`) must still be able to earn credit. Dual-match (reviewed + overlap target) is the non-distinguishing case. Dead targets stay authorable (Phase 5 D-04) but do not participate in grading until resolvable.
- **D-02:** Keep Phase 2 **D-06**: when `matchAnswer` is true, **skip** the accidental-match search. Overlap runs only on the correct path; accidental-match remains wrong-path only. Evaluation order: `matchAnswer` → if true, overlap check (D-01) → OVERLAP or CORRECT; if false → existing accidental-match / wrong path. — **Reversibility:** reversible.

### SRS / Answer.correct / no mutation
- **D-03:** On OVERLAP: set `Answer.correct = false` (sole SRS-credit signal stays false — Phase 1/2 lock), `Answer.outcome = OVERLAP`, and `AnsweredQuestion.overlap = true`. Persist the Answer on the RecallPrompt (audit trail). **Do not** call `markAsRecalled` or `markAsAccidentalMatch` — **zero** SRS mutation (no `recallCount` bump, no forgetting-curve change, no `nextRecallAt` change, no note-content mutation). — **Reversibility:** reversible — scheduling branch; accidental-match already proved a third path can bypass `markAsRecalled(false)`.
  - Rationale: ROADMAP success criteria require no credit and no note-data mutation; applying wrong or partial-fail would punish a technically correct but non-distinguishing answer.
- **D-04:** OVERLAP does **not** count toward the wrong-answer re-assimilation threshold (`hasExceededWrongAnswerThreshold`). It is withheld credit, not a fail. — **Reversibility:** reversible.

### Retry / queue UX
- **D-05:** After an OVERLAP response, the frontend **must not advance** the recall queue (`RecallPage.onAnswered` must not call `moveToNextMemoryTracker` for `outcome === 'OVERLAP'`). Show the try-again result, then let the user **retry the same memory tracker** (new spelling prompt for that tracker). Do not auto-advance to the next card. — **Reversibility:** reversible — UI/queue branch.
  - Rationale: ROADMAP: “the user simply retries the same review.” Today `onAnswered` always advances then shows incorrect results; OVERLAP needs an explicit stay-and-retry path. Exact “Try again” control vs auto-clear-back-to-input is Claude’s discretion if behavior matches D-05.

### Result messaging & partner reveal
- **D-06:** Show a **distinct** alert (not success green, not the plain “incorrect” / accidental-match copy) whose substance matches ROADMAP: correct, but looking for another answer — try again. Prefer warning-style chrome so it reads as “almost, distinguish further,” not “you failed.” — **Reversibility:** reversible — copy/CSS only.
- **D-07:** Do **not** reuse the accidental-match **matched-notes section** or offer-link CTAs for OVERLAP. Do **not** require revealing overlap partner notes in this phase. Leave `AnsweredQuestion.matchedNotes` unset/empty for OVERLAP (avoid conflating with `ACCIDENTAL_MATCH` UI). `AnsweredQuestion.overlap = true` is the wire flag. — **Reversibility:** reversible — can add partner reveal later without changing the grading gate.

### Claude's Discretion
- Exact English alert string (must communicate “correct but try a more specific answer”) and DaisyUI alert class (`daisy-alert-warning` vs similar).
- How to resolve overlap wiki-link tokens to notes (reuse `WikiLinkResolver` / `WikiLinkTargetReference` with reviewed note’s notebook as focus; readability filter consistent with accidental-match).
- Exact multi-target rule when several overlap targets also match — any one match is enough to fire OVERLAP (D-01); no need to return them on the wire this phase (D-07).
- Frontend retry mechanic details: clear `currentAnsweredSpelling` and re-show `Quiz` for the same index vs an explicit “Try again” button — as long as the queue does not advance and a new prompt can be answered.
- Whether controller/DTO assembly sets `overlap` on `AnsweredQuestion` beside `from(recallPrompt)`, mirroring Phase 3 `matchedNotes` attachment.
- Test split: backend grading unit/integration + Vitest on `AnsweredSpellingQuestion` / `RecallPage` queue behavior + capability-named E2E (UI hint: yes). Prefer extending existing spelling/overlap fixtures, not phase-numbered names.
- No OpenAPI contract shape change expected (`OVERLAP` / `overlap` already exist from Phase 1); regen only if research finds a gap.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project / requirements / roadmap
- `.planning/PROJECT.md` — Overlap handling (Active OVL-01); Constraints “Overlap try again withholds credit but does not mutate note data”; Key Decisions “Overlap is declared, not auto-detected”; Integration point `correct=true` + declared overlap → overlap path.
- `.planning/REQUIREMENTS.md` — **OVL-01** (this phase). OVL-02/OVL-03 complete in Phase 5.
- `.planning/ROADMAP.md` §"Phase 6: Overlap \"try again, no credit\"" — goal, success criteria 1–3, depends on Phase 5, UI hint: yes.

### Prior phase locks (carry forward — do not re-litigate)
- `.planning/phases/05-alias-as-wiki-link-overlap-declaration/05-CONTEXT.md` — D-01–D-04 declaration model; `overlapWikiLinkTokensFrom*`; plain-only consumers; dead targets valid at authoring; grading deferred to this phase.
- `.planning/phases/02-accidental-match-grading-penalty/02-CONTEXT.md` — D-02 `correct` sole SRS-credit signal; D-06 skip accidental-match when `matchAnswer` true; overlap declared not auto-detected.
- `.planning/phases/01-extend-answer-outcome-api/` (SUMMARY) — `AnswerOutcome.OVERLAP`, `AnsweredQuestion.overlap` already on the contract.
- `.planning/phases/03-reveal-both-notes-after-accidental-match/03-CONTEXT.md` / `.planning/phases/04-offer-link-between-notes/04-CONTEXT.md` — accidental-match UI only; do not reuse matched-notes/link CTAs for OVERLAP (D-07).

### Codebase maps
- `.planning/codebase/ARCHITECTURE.md` — service grading boundary (`MemoryTrackerService`) vs Vue recall UI.
- `.planning/codebase/STACK.md` — Spring Boot + Vue + generated OpenAPI client.
- `.planning/codebase/CONVENTIONS.md` — capability-named tests; `data-testid` / recall `data-test`.

### Source files (integration points — read before editing)
- `backend/src/main/java/com/odde/doughnut/services/MemoryTrackerService.java` — `answerSpelling(RecallPrompt, …)` correct vs accidental-match branches; primary grading seam.
- `backend/src/main/java/com/odde/doughnut/algorithms/FrontmatterAliases.java` — `overlapWikiLinkTokensFromNoteContent` / `FromFrontmatter` (Phase 5 seam).
- `backend/src/main/java/com/odde/doughnut/services/WikiLinkResolver.java` — resolve wiki-link tokens; accidental-match helpers for readability patterns.
- `backend/src/main/java/com/odde/doughnut/algorithms/WikiLinkTargetReference.java` / `WikiLinkMarkdown.java` — parse overlap token targets.
- `backend/src/main/java/com/odde/doughnut/entities/Note.java` — `matchAnswer` (plain aliases only).
- `backend/src/main/java/com/odde/doughnut/entities/AnswerOutcome.java` — `OVERLAP` enum value.
- `backend/src/main/java/com/odde/doughnut/entities/Answer.java` — `correct`, `@Transient outcome`.
- `backend/src/main/java/com/odde/doughnut/controllers/dto/AnsweredQuestion.java` — `overlap` Boolean; `from(RecallPrompt)`.
- `backend/src/main/java/com/odde/doughnut/controllers/RecallPromptController.java` — `answerSpelling` HTTP entry.
- `backend/src/main/java/com/odde/doughnut/entities/MemoryTracker.java` — `markAsRecalled` / `markAsAccidentalMatch` (must not run on OVERLAP).
- `frontend/src/components/recall/AnsweredSpellingQuestion.vue` — result alert branching (add OVERLAP).
- `frontend/src/pages/RecallPage.vue` — `onAnswered` always advances today; must special-case OVERLAP (D-05).
- `frontend/src/components/recall/Quiz.vue` — spelling answer submit → emit answered.
- `packages/generated/doughnut-backend-api/types.gen.ts` — `outcome` includes `OVERLAP`; `overlap?: boolean`.
- Existing tests: `MemoryTrackerService` / `RecallPromptController` spelling suites; `AnsweredSpellingQuestion.spec.ts`; accidental-match E2E patterns to mirror with overlap fixtures.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `FrontmatterAliases.overlapWikiLinkTokensFromNoteContent` — Phase 5 extraction API; do not re-parse YAML ad hoc.
- `AnswerOutcome.OVERLAP` + `AnsweredQuestion.overlap` — Phase 1 contract; writers still missing until this phase.
- `Note.matchAnswer` — correct-path predicate; already plain-alias-only (wiki-link aliases ignored).
- `WikiLinkResolver` + readability helpers — resolve declared overlap targets with the same “user may read” discipline as accidental match.
- Accidental-match third path in `answerSpelling` — pattern for a branch that sets `outcome` and bypasses default `markAsRecalled(correct)`.

### Established Patterns
- `correct` is the sole SRS-credit signal; `outcome` is metadata.
- Accidental match: `correct=false` + `markAsAccidentalMatch` (partial fail). Overlap needs `correct=false` + **no** mark path (D-03).
- Frontend accidental-match UI keys off `answer.outcome === 'ACCIDENTAL_MATCH'`; OVERLAP needs a parallel branch without matched-notes/link.
- `RecallPage.onAnswered` advances first, then shows incorrect results — OVERLAP must interrupt advance.

### Integration Points
- Backend: `MemoryTrackerService.answerSpelling` after `matchAnswer == true`, before `markAsRecalled(true, …)`.
- DTO: set `AnsweredQuestion.overlap` when assembling the response (controller or service helper).
- Frontend: `AnsweredSpellingQuestion` alert + `RecallPage` queue control for retry.
- E2E: author note with plain distinguishing alias + wiki-link overlap; assert try-again on shared answer and credit on distinguishing alias.

</code_context>

<specifics>
## Specific Ideas

- Phase 5 authored shape that unlocks the distinguishing path:
  ```yaml
  aliases:
    - color
    - "[[Other Note]]"
  ```
  Typing a string that matches both this note and `Other Note` → OVERLAP; typing `color` (reviewed only) → CORRECT with credit.
- ROADMAP user-facing substance: “correct, but we're looking for another answer — try again.”
- UI hint: yes — include observable frontend/E2E coverage for the try-again path.

</specifics>

<deferred>
## Deferred Ideas

- Revealing overlap partner notes on the OVERLAP result surface (Phase 3-style) — out of OVL-01; D-07 keeps this phase message + retry only.
- Save-time existence validation for overlap wiki-link targets — Phase 5 D-04 deferred.
- Separate `overlaps:` frontmatter key — rejected for v1.
- MCQ / fuzzy / auto-detected overlap — out of scope / v2.

None of the above were folded into Phase 6; discussion stayed within OVL-01.

</deferred>

---

*Phase: 6-overlap-try-again-no-credit*
*Context gathered: 2026-07-24*
