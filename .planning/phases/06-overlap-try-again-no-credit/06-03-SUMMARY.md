---
phase: 06-overlap-try-again-no-credit
plan: 03
subsystem: recall
tags: [overlap, spelling, dead-target, readability, vitest]

requires:
  - phase: 06-overlap-try-again-no-credit
    provides: Dual-match OVERLAP grading + try-again UI (06-01)
  - phase: 06-overlap-try-again-no-credit
    provides: Durable outcome + wrong-count exclusion (06-02)
provides:
  - Dead / unreadable / self-token CORRECT proofs (D-01 edges)
  - OVERLAP matched-notes absence under leaked props (D-07)
affects:
  - 06-04 (E2E)

tech-stack:
  added: []
  patterns:
    - Dual-match helper already skips empty resolve, self ids, and unreadable via resolveWikiLinkToken
    - showMatchedNotesSection gated to ACCIDENTAL_MATCH only (D-07)

key-files:
  created: []
  modified:
    - backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java
    - frontend/tests/components/recall/AnsweredSpellingQuestion.spec.ts

key-decisions:
  - "No production code change — 06-01 dual-match + UI gate already satisfy D-01/D-07 edges"
  - "Edge proofs as controller OverlapTryAgain cases + Vitest leaked matchedNotes"

patterns-established:
  - "Non-firing overlap declarations credit as CORRECT (dead/unreadable/self)"
  - "OVERLAP UI refuses matched-notes even if AnsweredQuestion.matchedNotes leaks"

requirements-completed: []  # OVL-01 spans 06-01..06-04; leave open until phase complete

coverage:
  - id: D1
    description: Dead overlap wiki-link target grades CORRECT with credit
    requirement: OVL-01
    verification:
      - kind: integration
        ref: RecallPromptControllerTests$OverlapTryAgain#shouldGradeCorrectWithCreditWhenOverlapTargetDoesNotExist
        status: pass
    human_judgment: false
  - id: D2
    description: Unreadable overlap partner skipped; CORRECT when no other dual-match
    requirement: OVL-01
    verification:
      - kind: integration
        ref: RecallPromptControllerTests$OverlapTryAgain#shouldGradeCorrectWithCreditWhenOverlapPartnerIsUnreadable
        status: pass
    human_judgment: false
  - id: D3
    description: Self-referential overlap token excluded; CORRECT with credit
    requirement: OVL-01
    verification:
      - kind: integration
        ref: RecallPromptControllerTests$OverlapTryAgain#shouldGradeCorrectWithCreditWhenOverlapTokenIsSelfReferential
        status: pass
    human_judgment: false
  - id: D4
    description: OVERLAP with leaked matchedNotes still hides matched-notes and offer-link CTAs
    requirement: OVL-01
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/AnsweredSpellingQuestion.spec.ts#keeps matched-notes section and offer-link CTAs absent when matchedNotes leak on OVERLAP
        status: pass
    human_judgment: false

duration: 3min
completed: 2026-07-24
status: complete
---

# Phase 6 Plan 03: Dead/unreadable/self CORRECT + matched-notes gate Summary

**Edge proofs only — dual-match already credits non-resolving declarations; OVERLAP UI stays matched-notes-free even under leaked props.**

## Performance

- **Duration:** ~3 min
- **Started:** 2026-07-24T12:21:44Z
- **Completed:** 2026-07-24T12:24:14Z
- **Tasks:** 2/2
- **Files modified:** 2

## Accomplishments

- Controller proofs: dead target, unreadable partner, and self-token → CORRECT with credit (D-01 / T-06-01 / Q2)
- Full `backend:test_only` green (includes AccidentalMatch skip-when-correct + AM positive — D-02)
- Vitest: OVERLAP + non-empty leaked `matchedNotes` still shows try-again alert only (D-07)

## Task Commits

1. **Task 1: Grade CORRECT when dual-match cannot fire** - `dff4db8bb6` (test)
2. **Task 2: OVERLAP UI refuses matched-notes even under leaked props** - `8c796baa90` (test)

## Files Created/Modified

- `backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java` — OverlapTryAgain edge cases
- `frontend/tests/components/recall/AnsweredSpellingQuestion.spec.ts` — leaked matchedNotes on OVERLAP

## Decisions Made

- Left `MemoryTrackerService.isNonDistinguishingOverlap` and `showMatchedNotesSection` unchanged — already correct per plan "confirm / fix only if"

## Deviations from Plan

None - plan executed exactly as written (tests-only when production already satisfied).

## TDD Gate Compliance

Tasks used `tdd="true"` but RED passed immediately because production behavior shipped in 06-01. No separate `feat(06-03)` commit — intentional; plan action was "confirm dual-match helper already… Fix only if."

## Self-Check: PASSED

- SUMMARY path exists
- Commits `dff4db8bb6`, `8c796baa90` present in git log
