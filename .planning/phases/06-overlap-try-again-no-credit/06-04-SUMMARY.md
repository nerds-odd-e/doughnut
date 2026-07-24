---
phase: 06-overlap-try-again-no-credit
plan: 04
subsystem: recall
tags: [overlap, spelling, e2e, cypress, cucumber]

requires:
  - phase: 06-overlap-try-again-no-credit
    provides: Dual-match OVERLAP grading + try-again UI remount (06-01)
  - phase: 06-overlap-try-again-no-credit
    provides: Durable outcome + wrong-count exclusion (06-02)
  - phase: 06-overlap-try-again-no-credit
    provides: Dead/unreadable/self edges + matched-notes gate (06-03)
provides:
  - Capability-named overlap_try_again E2E green without @wip (OVL-01 live UI)
affects:
  - Phase 6 complete / milestone ship

tech-stack:
  added: []
  patterns:
    - Accidental-match E2E stack mirrored for OVERLAP (feature + AnsweredQuestionPage + thin recall steps)
    - Correct spelling credit asserted via last-answered navigation after queue advance

key-files:
  created:
    - e2e_test/features/recall/overlap_try_again.feature
  modified:
    - e2e_test/start/pageObjects/AnsweredQuestionPage.ts
    - e2e_test/step_definitions/recall.ts

key-decisions:
  - "Fixture: colour declares color + [[Partner]]; Partner aliases colour for dual-match on title answer"
  - "Assert distinguishing credit via last-answered spelling step (queue advances on CORRECT)"
  - "scrollIntoView on overlap-try-again before visibility (result stack overflow clip)"

patterns-established:
  - "Capability-named overlap_try_again.feature with UI-SPEC testids overlap-try-again-alert / overlap-try-again"
  - "E2E asserts matched-notes-section and accidental-match-alert absent on OVERLAP (D-07)"

requirements-completed: [OVL-01]

coverage:
  - id: D1
    description: Shared non-distinguishing spelling shows overlap try-again alert, no matched-notes/AM chrome
    requirement: OVL-01
    verification:
      - kind: e2e
        ref: e2e_test/features/recall/overlap_try_again.feature#Shared non-distinguishing answer shows overlap try-again without credit
        status: pass
    human_judgment: false
  - id: D2
    description: Try again remounts fresh spelling prompt; distinguishing plain alias credits as correct
    requirement: OVL-01
    verification:
      - kind: e2e
        ref: e2e_test/features/recall/overlap_try_again.feature#Try again then distinguishing plain alias credits as correct
        status: pass
    human_judgment: false

duration: 6min
completed: 2026-07-24
status: complete
---

# Phase 6 Plan 04: Overlap try-again E2E Summary

**Live recall E2E proves OVL-01: dual-match try-again withholds credit, remount retry works, distinguishing alias credits, matched-notes stay off OVERLAP.**

## Performance

- **Duration:** 6 min
- **Started:** 2026-07-24T12:25:30Z
- **Completed:** 2026-07-24T12:31:30Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments

- Capability-named `overlap_try_again.feature` mirrors accidental-match E2E with UI-SPEC copy/testids
- Both scenarios green without `@wip` via targeted `pnpm cypress run --spec`
- ROADMAP success criteria 1–3 observable in the browser (OVL-01)

## Task Commits

1. **Task 1: Add capability-named overlap try-again E2E scaffold** - `015e8156ea` (test)
2. **Task 2: Make overlap try-again E2E green and drop @wip** - `b5a8549a1d` (test)

**Plan metadata:** (docs commit follows)

## Files Created/Modified

- `e2e_test/features/recall/overlap_try_again.feature` — dual-match try-again + distinguishing credit scenarios
- `e2e_test/start/pageObjects/AnsweredQuestionPage.ts` — overlap alert / absence / Try again helpers
- `e2e_test/step_definitions/recall.ts` — thin step wrappers

## Decisions Made

- Dual-match fixture: reviewed `colour` with plain alias `color` + `[[Partner]]`; Partner carries plain alias `colour` so answering the title dual-matches while `color` distinguishes
- After CORRECT, assert via `I should see that my last answer to spelling question is correct` because the queue advances off the immediate result surface

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Try again button clipped by overflow parent**
- **Found during:** Task 2
- **Issue:** `overlap-try-again` existed but failed `be.visible` without scroll
- **Fix:** `scrollIntoView()` before visibility assert (same pattern as alert)
- **Files modified:** `AnsweredQuestionPage.ts`
- **Committed in:** `b5a8549a1d`

**2. [Rule 1 - Bug] Immediate Correct! assertion after distinguishing answer**
- **Found during:** Task 2
- **Issue:** CORRECT advances the recall queue; current view is next tracker / remember UI, not the spelling result
- **Fix:** Reuse existing last-answered spelling step
- **Files modified:** `overlap_try_again.feature`
- **Committed in:** `b5a8549a1d`

## Self-Check: PASSED

- FOUND: `e2e_test/features/recall/overlap_try_again.feature`
- FOUND: `015e8156ea` (task 1)
- FOUND: `b5a8549a1d` (task 2)
- Targeted Cypress: 2 passing, no `@wip`
