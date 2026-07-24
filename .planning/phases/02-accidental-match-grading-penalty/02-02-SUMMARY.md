---
phase: 02-accidental-match-grading-penalty
plan: 02
subsystem: api
tags: [accidental-match, alias, spelling, srs, forgetting-curve, wiki-link-resolver, threshold]

requires:
  - phase: 02-accidental-match-grading-penalty
    provides: Title-leg findAccidentalMatch + markAsAccidentalMatch + partialFail (Plan 01)
provides:
  - Alias-leg accidental-match grading (title-then-alias fallback via findByAliasLookupKeyOrderByNoteIdAsc)
  - Floor-clamp proof at DEFAULT_FORGETTING_CURVE_INDEX (100.0f)
  - Threshold-counts proof (correct=false counted by existing wrong-answer counter)
affects:
  - 03 reveal matched notes

tech-stack:
  added: []
  patterns:
    - Title-then-alias accidental-match fallback mirroring noteCandidates ordering
    - Shared firstReadableAccidentalCandidate filter for title and alias legs
    - Explicit noteAliasIndexService.refreshForNote in alias fixtures (makeMe does not auto-index)

key-files:
  created: []
  modified:
    - backend/src/main/java/com/odde/doughnut/entities/repositories/NoteAliasIndexRepository.java
    - backend/src/main/java/com/odde/doughnut/services/WikiLinkResolver.java
    - backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java

key-decisions:
  - "Alias index fixture must call refreshForNote — research A1 (makeMe auto-refresh) was incorrect"
  - "At floor index, nextRecallAt equals now (0 repeat hours); assert greaterThanOrEqualTo, not greaterThan"

patterns-established:
  - "findAccidentalMatch: title candidates first; alias candidates via normalizedLookupKey + HashSet dedupe; same userMayReadNotebook filter"

requirements-completed: [AM-01, AM-02]

coverage:
  - id: D5
    description: Wrong spelling answer matching another readable note alias (no title match) grades as ACCIDENTAL_MATCH with matchedNoteId
    requirement: AM-01
    verification:
      - kind: integration
        ref: backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java#shouldGradeAsAccidentalMatchWhenWrongAnswerMatchesAnotherReadableNoteAlias
        status: pass
    human_judgment: false
  - id: D6
    description: Accidental match at forgetting-curve floor keeps index at 100.0f (lighter-than-wrong clamp)
    requirement: AM-02
    verification:
      - kind: integration
        ref: backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java#shouldNotDropForgettingCurveIndexBelowFloorOnAccidentalMatch
        status: pass
    human_judgment: false
  - id: D7
    description: Accidental matches count toward wrong-answer re-assimilation threshold via correct=false
    requirement: AM-01
    verification:
      - kind: integration
        ref: backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java#shouldStillCountAccidentalMatchTowardWrongAnswerThreshold
        status: pass
    human_judgment: false

duration: 8min
completed: 2026-07-24
status: complete
---

# Phase 02 Plan 02: Accidental-match alias leg & edge cases Summary

**Alias-leg accidental match completes AM-01 title-then-alias detection; floor-clamp and threshold-counts tests prove AM-02 / D-04 at the edges**

## Performance

- **Duration:** 8 min
- **Started:** 2026-07-24T00:47:17Z
- **Completed:** 2026-07-24T00:55:19Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments

- Added non-notebook-scoped `findByAliasLookupKeyOrderByNoteIdAsc` and title-then-alias fallback in `findAccidentalMatch`
- Proved forgetting-curve floor clamp holds on accidental match (index stays at 100)
- Proved accidental matches (`correct=false`) count toward the existing wrong-answer threshold (no new counter)
- Full backend suite green (`pnpm backend:test_only`)

## Task Commits

Each task was committed atomically:

1. **Task 1 RED: failing alias-match test** - `d0e0223f33` (test)
2. **Task 1 GREEN: alias leg wiring** - `bb41040f89` (feat)
3. **Task 2a: floor-clamp edge test** - `08a42f6957` (test)
4. **Task 2b: threshold-counts edge test** - `5ff23f8cd1` (test)

**Plan metadata:** (docs commit follows)

_Note: TDD used test → feat for Task 1; Task 2 is pure test additions_

## Files Created/Modified

- `NoteAliasIndexRepository.java` - `findByAliasLookupKeyOrderByNoteIdAsc` (reuses SELECT_ALIAS_WITH_NOTEBOOK + ACTIVE_NOTE_AND_NOTEBOOK)
- `WikiLinkResolver.java` - alias leg + `aliasAccidentalCandidates` / `firstReadableAccidentalCandidate` helpers
- `RecallPromptControllerTests.java` - three AccidentalMatch tests (alias, floor-clamp, threshold)

## Decisions Made

- Call `noteAliasIndexService.refreshForNote` after creating alias-bearing notes in tests (makeMe does not populate `note_alias_index`)
- At floor index, `getRepeatInHours()` is 0 so `nextRecallAt == now`; assert `greaterThanOrEqualTo` (not past) rather than strict `greaterThan`

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Alias index not populated by makeMe (research A1 wrong)**
- **Found during:** Task 1 GREEN
- **Issue:** Alias-match test still returned outcome=null after alias leg was wired because `note_alias_index` rows were missing
- **Fix:** Autowire `NoteAliasIndexService` and call `refreshForNote` on the alias-bearing note (same pattern as `SearchControllerAliasTests`)
- **Files modified:** `RecallPromptControllerTests.java`
- **Verification:** AccidentalMatch alias test passes; full `pnpm backend:test_only` green
- **Committed in:** `bb41040f89` (Task 1 feat)

**2. [Rule 1 - Bug] Floor nextRecallAt cannot be strictly greater than now at index 100**
- **Found during:** Task 2 floor-clamp test
- **Issue:** At `DEFAULT_FORGETTING_CURVE_INDEX`, repeat hours are 0; after bumping `lastRecalledAt`, `nextRecallAt == currentUTCTimestamp`
- **Fix:** Assert `greaterThanOrEqualTo` (not in the past / Pitfall 2) instead of strict `greaterThan`
- **Files modified:** `RecallPromptControllerTests.java`
- **Verification:** Floor-clamp test passes
- **Committed in:** `08a42f6957` (Task 2a)

---

**Total deviations:** 2 auto-fixed (Rule 1)
**Impact on plan:** Fixture and assertion accuracy only; no production scope creep

## Issues Encountered

None beyond the deviations above.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 2 accidental-match grading + lighter penalty is fully covered (title + alias + floor + threshold)
- `AnsweredQuestion.matchedNotes` / `overlap` remain null (Phase 3 / Phase 6)
- No Flyway migration; no OpenAPI/frontend change

## Self-Check: PASSED

- FOUND: NoteAliasIndexRepository.findByAliasLookupKeyOrderByNoteIdAsc
- FOUND: WikiLinkResolver.findAccidentalMatch alias leg (findByAliasLookupKeyOrderByNoteIdAsc + normalizedLookupKey)
- FOUND: shouldGradeAsAccidentalMatchWhenWrongAnswerMatchesAnotherReadableNoteAlias
- FOUND: shouldNotDropForgettingCurveIndexBelowFloorOnAccidentalMatch
- FOUND: shouldStillCountAccidentalMatchTowardWrongAnswerThreshold
- FOUND: commits d0e0223f33, bb41040f89, 08a42f6957, 5ff23f8cd1

---
*Phase: 02-accidental-match-grading-penalty*
*Completed: 2026-07-24*
