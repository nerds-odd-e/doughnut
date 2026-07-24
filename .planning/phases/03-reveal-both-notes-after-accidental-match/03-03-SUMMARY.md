---
phase: 03-reveal-both-notes-after-accidental-match
plan: 03
subsystem: testing
tags: [accidental-match, e2e, cypress, spelling, matchedNotes]

requires:
  - phase: 03-reveal-both-notes-after-accidental-match
    provides: matchedNotes API + AnsweredSpellingQuestion reveal UI (03-01, 03-02)
provides:
  - Capability-named accidental_match_reveal.feature proving reviewed + matched reveal
  - expectAccidentalMatchReveal separate from plain incorrect expectation
affects:
  - Phase 4 (add-link UI under matched-notes-section — not asserted here)

tech-stack:
  added: []
  patterns:
    - Accidental-match E2E uses dedicated Then + page-object method; plain incorrect string untouched
    - scrollIntoView before asserting matched-notes-section (overflow clip in recall layout)

key-files:
  created:
    - e2e_test/features/recall/accidental_match_reveal.feature
  modified:
    - e2e_test/start/pageObjects/AnsweredQuestionPage.ts
    - e2e_test/step_definitions/recall.ts

key-decisions:
  - "Single-match happy path E2E; multi-match remains Plan 01 controller coverage"
  - "Assert accidental-match-alert + matched-notes-section note titles; do not assert Phase 4 link UI"

patterns-established:
  - "Pattern: expectAccidentalMatchReveal vs expectSpellingAnswerToBeIncorrect as separate methods"

requirements-completed: []  # AM-03 awaits Task 2 human-verify approval before mark-complete

coverage:
  - id: D1
    description: "E2E: answering with another readable note title shows accidental-match alert and matched-notes-section with both notes"
    requirement: AM-03
    verification:
      - kind: e2e
        ref: "e2e_test/features/recall/accidental_match_reveal.feature#Accidental match reveals reviewed and matched notes"
        status: pass
      - kind: e2e
        ref: "CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/accidental_match_reveal.feature"
        status: pass
    human_judgment: false
  - id: D2
    description: "Human spot-check of accidental-match alert copy, vertical Matched note(s) stack, no Phase 4 add-link"
    requirement: AM-03
    verification: []
    human_judgment: true
    rationale: "Plan Task 2 checkpoint:human-verify — visual/copy confirmation in running app; not auto-approved"

duration: 8min
completed: 2026-07-24
status: awaiting-human-verify
---

# Phase 03 Plan 03: Accidental-match reveal E2E Summary

**Capability-named Cypress scenario proves spelling accidental-match reveals reviewed + matched notes; plain-wrong E2E expectation left unchanged. Human spot-check still required.**

## Performance

- **Duration:** ~8 min
- **Started:** 2026-07-24T04:23:53Z
- **Completed (Task 1):** 2026-07-24T04:26:30Z
- **Tasks:** 1/2 auto complete; Task 2 awaiting human verify
- **Files modified:** 3

## Accomplishments

- Added `accidental_match_reveal.feature` (no phase numbers; `@wip` removed after green)
- Added `expectAccidentalMatchReveal` without changing `expectSpellingAnswerToBeIncorrect` copy
- Targeted Cypress run exits 0

## Task Commits

1. **Task 1: E2E feature + page object for accidental-match reveal** - `c969249cb5` (test)
2. **Task 2: Human spot-check** - pending (checkpoint)

## Files Created/Modified

- `e2e_test/features/recall/accidental_match_reveal.feature` - Happy-path accidental match → reveal
- `e2e_test/start/pageObjects/AnsweredQuestionPage.ts` - `expectAccidentalMatchReveal`
- `e2e_test/step_definitions/recall.ts` - Then step wiring

## Decisions Made

- Seed with sedition (spelling review) + sedation (matched title answer), mirroring existing recall notebook fixtures
- Scope matched note title assert inside `matched-notes-section`; scrollIntoView for overflow-clipped section

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Multiple title matches / overflow visibility**
- **Found during:** Task 1
- **Issue:** `findByText(reviewedTitle)` matched multiple DOM nodes; `matched-notes-section` existed but failed `be.visible` due to parent overflow clip
- **Fix:** Assert reviewed/matched titles via `[data-test="note-title"]` filters; `scrollIntoView()` before visibility assert on matched section
- **Files modified:** `AnsweredQuestionPage.ts`
- **Committed in:** `c969249cb5`

## Known Stubs

None.

## Self-Check: PASSED

- FOUND: `e2e_test/features/recall/accidental_match_reveal.feature`
- FOUND: `expectAccidentalMatchReveal` in `AnsweredQuestionPage.ts`
- FOUND: commit `c969249cb5`
- E2E verify: exit 0 (no `@wip`)

## Threat Flags

None new — E2E uses only notes the test user can read; no Phase 4 link assertions.
