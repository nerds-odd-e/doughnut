---
phase: 09-build-a-link-from-resolve-dialog
plan: 02
subsystem: testing
tags: [cypress, e2e, accidental-match, Build-a-link, page-object]

requires:
  - phase: 09-build-a-link-from-resolve-dialog
    provides: Single-Modal Build a link step + link-to-matched-note-* testids (09-01)
provides:
  - AnsweredQuestionPage Resolve → Build a link → Link to: path
  - Untagged wiki-property and relationship stay-on-result E2E scenarios
affects:
  - Phase 11 Add as overlapped note E2E (reuse resolve dialog page-object patterns)
  - CI accidental_match_reveal (no longer skipped via @wip)

actuals:
  tokens: 870
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns:
    - "E2E openLinkToMatchedNote: Resolve CTA → waitUntilAppIsNotBusy → dialog Build a link"
    - "expectStillOnAccidentalMatchResult: alert + Resolve CTA + dialog list still open (D-04)"

key-files:
  created: []
  modified:
    - e2e_test/start/pageObjects/AnsweredQuestionPage.ts
    - e2e_test/features/recall/accidental_match_reveal.feature

key-decisions:
  - "Page-object-only path rewrite; Gherkin step text unchanged (D-09)"
  - "Stay-on-result prefers dialog list still visible after link (D-04); no matched-notes-section"

patterns-established:
  - "Pattern: accidental-match mutate offers driven via resolve dialog, not stacked result section"

requirements-completed: [AMR-06]

coverage:
  - id: D1
    description: "Wiki-property Build a link from resolve dialog stays on accidental-match result"
    requirement: AMR-06
    verification:
      - kind: e2e
        ref: "e2e_test/features/recall/accidental_match_reveal.feature#Offer links the matched note as a wiki property without leaving the result"
        status: pass
    human_judgment: false
  - id: D2
    description: "Relationship Build a link from resolve dialog stays on accidental-match result"
    requirement: AMR-06
    verification:
      - kind: e2e
        ref: "e2e_test/features/recall/accidental_match_reveal.feature#Offer links the matched note as a relationship without leaving the result"
        status: pass
    human_judgment: false

duration: 3min
completed: 2026-08-05
status: complete
---

# Phase 9 Plan 02: Accidental-match Build a link E2E Summary

**E2E page object opens Resolve → Build a link, both property/relationship stay-on-result scenarios green without @wip (AMR-06).**

## Performance

- **Duration:** 3 min
- **Started:** 2026-08-05T12:10:44Z
- **Completed:** 2026-08-05T12:13:13Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- Rewrote `openLinkToMatchedNote` for Resolve → `link-to-matched-note-*` (**Build a link**) → existing Link to: helpers
- Rewrote `expectStillOnAccidentalMatchResult` for alert + Resolve CTA + dialog list (removed stacked `matched-notes-section`)
- Untagged both link scenarios; targeted Cypress 3/3 green; `overlap_try_again` untouched

## Task Commits

1. **Task 1: E2E page object Resolve then Build a link** - `2ff8b21375` (feat)
2. **Task 2: Untag @wip on link scenarios** - `7f40c22879` (test)

**Plan metadata:** `06682491d6` (docs: complete plan)

## Deviations from Plan

None - plan executed exactly as written.

## Self-Check: PASSED

- AnsweredQuestionPage.ts FOUND (Resolve → Build a link path)
- accidental_match_reveal.feature FOUND (no @wip on link scenarios)
- Commits 2ff8b21375, 7f40c22879 FOUND
- Cypress accidental_match_reveal 3/3 pass
