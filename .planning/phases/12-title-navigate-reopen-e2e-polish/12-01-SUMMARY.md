---
phase: 12-title-navigate-reopen-e2e-polish
plan: 01
subsystem: testing
tags: [cypress, cucumber, e2e, accidental-match, keepalive, history-back]

requires:
  - phase: 08-match-path-and-clickable-titles
    provides: NoteTitleWithLink navigable titles inside resolve dialog
  - phase: 07-compact-result-resolve-dialog-shell
    provides: resolve-accidental-match CTA + accidental-match-resolve-dialog
provides:
  - AMR-05 E2E reopen-after-title-navigate via history back
  - AnsweredQuestionPage reopen helpers (openResolveDialog, clickMatchedNoteTitle, returnToRecallViaHistoryBack, expectResolveAvailableAgainWithMatch)
  - KeepAlive path proven sufficient — Plan 12-02 contingency can be skipped
affects:
  - 12-02-keepalive-name-if-remount (skip if KeepAlive green)
  - AMR-05 requirement closure

actuals:
  tokens: 1044
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns:
    - "E2E history back (cy.go back) preserves KeepAlive RecallPage matchedNotes"
    - "Manual Resolve CTA reopen after title navigate — no auto-open"

key-files:
  created: []
  modified:
    - e2e_test/start/pageObjects/AnsweredQuestionPage.ts
    - e2e_test/features/recall/accidental_match_reveal.feature
    - e2e_test/step_definitions/accidental_match.ts

key-decisions:
  - "KeepAlive live previousAnsweredQuestions.matchedNotes sufficient for AMR-05 — no product code, no OpenAPI"
  - "Return via cy.go('back') only — not Resume, not cy.visit('/recall')"
  - "Plan 12-02 KeepAlive name contingency can be skipped"

patterns-established:
  - "Page-object fluent chain: openResolveDialog → clickMatchedNoteTitle → returnToRecallViaHistoryBack → expectResolveAvailableAgainWithMatch"
  - "Capability-named reopen scenario alongside existing open/dismiss path"

requirements-completed: [AMR-05]

coverage:
  - id: D1
    description: "After Resolve → matched title navigate → history back, accidental-match alert and Resolve CTA remain; opening Resolve again lists the same match title"
    requirement: AMR-05
    verification:
      - kind: e2e
        ref: "e2e_test/features/recall/accidental_match_reveal.feature#Reopen resolve after navigating matched title and returning"
        status: pass
    human_judgment: false
  - id: D2
    description: "Existing open/dismiss accidental-match reveal and overlap_try_again stay green and uncoupled"
    requirement: AMR-05
    verification:
      - kind: e2e
        ref: "e2e_test/features/recall/accidental_match_reveal.feature + overlap_try_again.feature (targeted cypress run)"
        status: pass
    human_judgment: false

duration: 2min
completed: 2026-08-05
status: complete
---

# Phase 12 Plan 01: Title navigate reopen E2E Summary

**AMR-05 proven end-to-end via KeepAlive: Resolve → matched title → `cy.go('back')` → manual Resolve shows the same match — no product or OpenAPI changes.**

## Performance

- **Duration:** ~2 min
- **Started:** 2026-08-05T14:09:31Z
- **Completed:** 2026-08-05T14:11:23Z
- **Tasks:** 2/2
- **Files modified:** 3

## Accomplishments

- Added fluent AnsweredQuestionPage helpers for resolve open, title navigate, history-back return, and reopen-with-same-match asserts
- Capability-named E2E scenario `Reopen resolve after navigating matched title and returning` green without `@wip`
- Confirmed KeepAlive preserves live `matchedNotes` after SPA history back — **Plan 12-02 contingency can be skipped**
- Existing open/dismiss path + `overlap_try_again` both green; overlap feature file unchanged

## Task Commits

1. **Task 1: End-to-end reopen after matched title + history back** - `669082f4f9` (test)
2. **Task 2: Green reopen scenario and keep overlap uncoupled** - `fb2568f658` (test)

## Files Created/Modified

- `e2e_test/start/pageObjects/AnsweredQuestionPage.ts` — `openResolveDialog`, `clickMatchedNoteTitle`, `returnToRecallViaHistoryBack`, `expectResolveAvailableAgainWithMatch`
- `e2e_test/features/recall/accidental_match_reveal.feature` — reopen-after-title-navigate scenario (no phase numbers)
- `e2e_test/step_definitions/accidental_match.ts` — thin When/Then wrappers only

## Decisions Made

- Relied on existing DoughnutApp KeepAlive `include=['RecallPage']` — zero product code in this plan
- Manual Resolve reopen only (dialog closed after title navigate until CTA click)
- No OpenAPI / `previouslyAnswered` enrichment (D-04)

## KeepAlive / Plan 02 Gate

**KeepAlive path worked.** After title navigate and `cy.go('back')`, accidental-match alert + Resolve CTA remained and reopen listed matched title `sedation`. Remount / empty-`matchedNotes` symptoms were **not** observed. **Plan 12-02 (KeepAlive name harden) can be skipped.**

## Deviations from Plan

None - plan executed exactly as written (E2E-only; product untouched).

### TDD Gate Compliance

Tracer scenario passed on first run because product KeepAlive behavior already existed — RED gate did not fail. E2E was the missing coverage, not missing product code. Documented per research expectation (“zero or tiny product code”).

## Test Results

```
accidental_match_reveal.feature — 5 passing (incl. reopen)
overlap_try_again.feature — 2 passing
overlap_try_again.feature MD5 unchanged (c74740d9dd43b2b9e579419667a37ee4)
```

## Self-Check: PASSED

- FOUND: e2e_test/start/pageObjects/AnsweredQuestionPage.ts helpers
- FOUND: reopen scenario without @wip
- FOUND: commit 669082f4f9
- FOUND: commit fb2568f658
