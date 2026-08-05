---
phase: 08-match-path-and-clickable-titles
plan: 02
subsystem: testing
tags: [cypress, cucumber, e2e, accidental-match, AMR-04]

requires:
  - phase: 08-01
    provides: AccidentalMatchResolveRow with NoteTitleWithLink + BreadcrumbWithCircle (path + clickable title in dialog)
provides:
  - E2E page-object asserts for notebook path identity and clickable title in resolve dialog
  - Feature blurb aligned to titles + notebook path (no AMR-05 reopen)
affects:
  - Phase 9 (Build a link — @wip scenarios remain)
  - Phase 12 (AMR-05 navigate-and-reopen E2E)

actuals:
  tokens: 409
  tasks: 2
  commits: 1

tech-stack:
  added: []
  patterns:
    - "expectAccidentalMatchReveal asserts path + visible a for title inside dialog; dismiss without noteShow round-trip"
    - "CI=true Cypress skips @wip; local verify uses CI=true for targeted accidental_match_reveal"

key-files:
  created: []
  modified:
    - e2e_test/start/pageObjects/AnsweredQuestionPage.ts
    - e2e_test/features/recall/accidental_match_reveal.feature

key-decisions:
  - "Same-notebook fixture English practice is enough for path identity (D-11); no second notebook"
  - "Assert visible a for matched title without clicking through (AMR-05 / Phase 12)"
  - "Prefer page-object asserts; Feature blurb only for titles + notebook path"

patterns-established:
  - "Resolve-dialog E2E: waitUntilAppIsNotBusy after Resolve click, then path text + contains(a, title), then close-button dismiss"

requirements-completed: [AMR-04]

coverage:
  - id: D1
    description: Resolve dialog E2E asserts English practice path identity alongside matched title
    requirement: AMR-04
    verification:
      - kind: e2e
        ref: e2e_test/features/recall/accidental_match_reveal.feature#Accidental match reveals reviewed and matched notes
        status: pass
    human_judgment: false
  - id: D2
    description: Matched title inside dialog is a visible clickable anchor without navigate-and-reopen
    requirement: AMR-04
    verification:
      - kind: e2e
        ref: e2e_test/start/pageObjects/AnsweredQuestionPage.ts#expectAccidentalMatchReveal
        status: pass
    human_judgment: false
  - id: D3
    description: No AMR-05 reopen scenario; @wip link scenarios and overlap stay out of scope
    requirement: AMR-04
    verification:
      - kind: e2e
        ref: CI=true pnpm cypress run --spec e2e_test/features/recall/accidental_match_reveal.feature
        status: pass
    human_judgment: false

duration: 2min
completed: 2026-08-05
status: complete
---

# Phase 08 Plan 02: Match path and clickable titles Summary

**Accidental-match E2E prove resolve dialog shows English practice path + clickable matched title without AMR-05 reopen.**

## Performance

- **Duration:** 2 min
- **Started:** 2026-08-05T09:40:35Z
- **Completed:** 2026-08-05T09:42:30Z
- **Tasks:** 2/2
- **Files modified:** 2

## Accomplishments

- Extended `expectAccidentalMatchReveal` to assert notebook path `English practice` and visible `a` for matched title inside `accidental-match-resolve-dialog`
- Wait for app not busy after Resolve click so realm-hydrated breadcrumb is present before path assert
- Feature description mentions titles + notebook path; no navigate-and-reopen scenario; `@wip` link scenarios unchanged

## Task Commits

1. **Task 1 + Task 2: path/clickable title asserts + feature wording** — `3661589449` — test(08-02): assert path identity and clickable title in resolve dialog

_Task 2 Feature blurb landed in the same commit (parallel edit during Task 1 commit)._

## Files Created/Modified

- `e2e_test/start/pageObjects/AnsweredQuestionPage.ts` — dialog asserts path + clickable title; dismiss unchanged
- `e2e_test/features/recall/accidental_match_reveal.feature` — Feature blurb titles + notebook path

## Decisions Made

- Followed D-11: single-notebook path identity is enough for Phase 8 E2E
- Did not click title link / reopen (Phase 12 / AMR-05)
- Left `openLinkToMatchedNote` and `@wip` scenarios untouched for Phase 9
- Verified with `CI=true` so `@wip` scenarios are skipped (matches CI tag filter)

## Deviations from Plan

### Auto-fixed Issues

None - plan executed as written.

### Process notes

**1. [Rule 3 - Blocking] Local Cypress without CI=true runs @wip scenarios**
- **Found during:** Task 1 verify
- **Issue:** First run failed on two `@wip` link scenarios (expected Phase 9 gaps)
- **Fix:** Re-run with `CI=true` so tags are `not @ignore and not @wip`
- **Files modified:** none
- **Commit:** n/a (verify env only)

**Total deviations:** 0 product auto-fixes; 1 verify-env note.

## Authentication Gates

None.

## Known Stubs

None.

## Threat Flags

None — E2E asserts only Plan 01 rendered text/anchors.

## Issues Encountered

None.

## Next Phase Readiness

Phase 8 complete (2/2 plans). AMR-04 evidenced by Vitest (08-01) + E2E (08-02). Ready for Phase 9 Build a link / verify-work as orchestrator directs.

## Self-Check: PASSED
