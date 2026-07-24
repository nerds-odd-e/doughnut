---
phase: 02-accidental-match-grading-penalty
plan: 01
subsystem: api
tags: [accidental-match, spelling, srs, forgetting-curve, wiki-link-resolver, idor]

requires:
  - phase: 01-extend-answer-outcome-api
    provides: Answer.matchedNoteId + Answer.outcome (@Transient) and AnswerOutcome.ACCIDENTAL_MATCH
provides:
  - Title-leg accidental-match grading (outcome=ACCIDENTAL_MATCH + matchedNoteId + correct=false)
  - Lighter SRS penalty via ForgettingCurve.partialFail (-10 clamped) and MemoryTracker.markAsAccidentalMatch (no +12h)
  - WikiLinkResolver.findAccidentalMatch with userMayReadNotebook IDOR filter
  - Controller proofs for IDOR-unreadable and skip-when-correct-shared-title
affects:
  - 02-02 alias-leg accidental match
  - 03 reveal matched notes

tech-stack:
  added: []
  patterns:
    - Java-side userMayReadNotebook filter on wider title lookup (no DB readability predicate)
    - Dedicated markAsAccidentalMatch seam (never recallFailed) for lighter penalty
    - Set @Transient outcome/matchedNoteId on recallPrompt.getAnswer() after merge/save

key-files:
  created: []
  modified:
    - backend/src/main/java/com/odde/doughnut/entities/repositories/NoteRepository.java
    - backend/src/main/java/com/odde/doughnut/services/WikiLinkResolver.java
    - backend/src/main/java/com/odde/doughnut/entities/ForgettingCurve.java
    - backend/src/main/java/com/odde/doughnut/entities/MemoryTracker.java
    - backend/src/main/java/com/odde/doughnut/services/MemoryTrackerService.java
    - backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java

key-decisions:
  - "Set ACCIDENTAL_MATCH fields on recallPrompt.getAnswer() after EntityPersister.save/merge so @Transient values stick on the managed Answer"
  - "Title leg only in Plan 01; alias fallback deferred to Plan 02"

patterns-established:
  - "Accidental match: !correct → findAccidentalMatch → set outcome/matchedNoteId → markAsAccidentalMatch; else plain markAsRecalled"
  - "partialFail() = add(-INCREMENT) mirrors failed() clamp at floor 100"

requirements-completed: [AM-01, AM-02]

coverage:
  - id: D1
    description: Wrong spelling answer matching another readable note title grades as ACCIDENTAL_MATCH with matchedNoteId and correct=false
    requirement: AM-01
    verification:
      - kind: integration
        ref: backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java#shouldGradeAsAccidentalMatchWhenWrongAnswerMatchesAnotherReadableNoteTitle
        status: pass
    human_judgment: false
  - id: D2
    description: Accidental match applies -10 clamped forgetting-curve penalty and recomputes nextRecallAt without +12h override
    requirement: AM-02
    verification:
      - kind: integration
        ref: backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java#shouldApplyLighterPenaltyThanWrongAnswer
        status: pass
    human_judgment: false
  - id: D3
    description: Unreadable-notebook title match does not leak matchedNoteId (IDOR guard)
    requirement: AM-01
    verification:
      - kind: integration
        ref: backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java#shouldNotLeakMatchedNoteIdFromUnreadableNotebook
        status: pass
    human_judgment: false
  - id: D4
    description: Correct answer skips accidental-match search even when another readable note shares the title
    requirement: AM-01
    verification:
      - kind: integration
        ref: backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java#shouldSkipAccidentalMatchSearchWhenAnswerMatchesReviewedNoteEvenIfAnotherNoteSharesTitle
        status: pass
    human_judgment: false

duration: 6min
completed: 2026-07-24
status: complete
---

# Phase 02 Plan 01: Accidental-match title-leg grading Summary

**Title-leg accidental match writes Answer.outcome=ACCIDENTAL_MATCH + matchedNoteId with clamped -10 SRS penalty (no +12h), plus IDOR and skip-when-correct controller proofs**

## Performance

- **Duration:** 6 min
- **Started:** 2026-07-24T00:39:42Z
- **Completed:** 2026-07-24T00:45:23Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments

- Wired end-to-end title-leg accidental match through NoteRepository → WikiLinkResolver → MemoryTrackerService → controller response
- Applied lighter SRS path via `ForgettingCurve.partialFail()` / `MemoryTracker.markAsAccidentalMatch` (never `recallFailed`)
- Proved IDOR filter (unreadable notebook) and D-06 skip-when-correct-shared-title with controller tests
- Full backend suite green (`pnpm backend:test_only`)

## Task Commits

Each task was committed atomically:

1. **Task 1 RED: failing AccidentalMatch tracer tests** - `ae107c966b` (test)
2. **Task 1 GREEN: title-leg wiring + lighter penalty** - `3db73ea591` (feat)
3. **Task 2: IDOR + skip-when-correct tests** - `e0a4abf02b` (test)

**Plan metadata:** (docs commit follows)

_Note: TDD tracer used test → feat commits_

## Files Created/Modified

- `NoteRepository.java` - `findByNoteTitleOrderByIdAsc` (non-notebook-scoped title lookup)
- `WikiLinkResolver.java` - `findAccidentalMatch` (title leg + readability + exclude reviewed)
- `ForgettingCurve.java` - `partialFail()` clamped -10
- `MemoryTracker.java` - `markAsAccidentalMatch` (bump lastRecalledAt, recompute nextRecallAt)
- `MemoryTrackerService.java` - inject WikiLinkResolver; accidental branch in `answerSpelling`
- `RecallPromptControllerTests.java` - `@Nested AccidentalMatch` (4 tests)

## Decisions Made

- After `EntityPersister.save`/`merge`, set `@Transient` outcome/matchedNoteId on `recallPrompt.getAnswer()` (managed instance) so fields survive merge copying
- Title-only lookup in Plan 01; alias leg left for Plan 02

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] @Transient fields lost when mutating pre-merge Answer reference**
- **Found during:** Task 1 GREEN
- **Issue:** Setting outcome on the local `answer` after `entityPersister.save(recallPrompt)` left `AnsweredQuestion.from` with null outcome (merge/cascade can leave a different managed Answer on the prompt) while the lighter-penalty path still ran
- **Fix:** Assign `recallPrompt = entityPersister.save(recallPrompt)` and set outcome/matchedNoteId on `recallPrompt.getAnswer()`
- **Files modified:** `MemoryTrackerService.java`
- **Verification:** AccidentalMatch tests pass; full `pnpm backend:test_only` green
- **Committed in:** `3db73ea591` (Task 1 feat)

---

**Total deviations:** 1 auto-fixed (Rule 1)
**Impact on plan:** Required for correct contract surfacing; no scope creep

## Issues Encountered

None beyond the merge/@Transient fix above.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Title-leg writer is live; Plan 02 can add alias fallback in `findAccidentalMatch` plus floor-clamp/threshold tests
- `AnsweredQuestion.matchedNotes` / `overlap` remain null (Phase 3 / Phase 6)
- No Flyway migration; no OpenAPI/frontend change

## Self-Check: PASSED

- FOUND: NoteRepository.findByNoteTitleOrderByIdAsc
- FOUND: WikiLinkResolver.findAccidentalMatch
- FOUND: ForgettingCurve.partialFail
- FOUND: MemoryTracker.markAsAccidentalMatch
- FOUND: MemoryTrackerService accidental branch + WikiLinkResolver injection
- FOUND: AccidentalMatch nested tests (4 methods)
- FOUND: commits ae107c966b, 3db73ea591, e0a4abf02b

---
*Phase: 02-accidental-match-grading-penalty*
*Completed: 2026-07-24*
