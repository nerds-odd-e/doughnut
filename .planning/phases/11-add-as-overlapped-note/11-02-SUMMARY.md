---
phase: 11-add-as-overlapped-note
plan: 02
subsystem: testing
tags: [cypress, e2e, accidental-match, Add-as-overlapped, page-object]

requires:
  - phase: 11-add-as-overlapped-note
    provides: Add as overlapped note CTA + declare path (11-01)
provides:
  - AnsweredQuestionPage Resolve → Add as overlapped note path
  - Capability-named stay-without-try-again E2E scenario
  - accidental_match step definitions extracted from recall.ts
affects:
  - Phase 12 title navigate / reopen E2E (reuse resolve page-object patterns)
  - CI accidental_match_reveal + overlap_try_again uncoupled

actuals:
  tokens: 2400
  tasks: 2
  commits: 1

tech-stack:
  added: []
  patterns:
    - "E2E openAddAsOverlappedNote via shared openResolveAndClickMatchedNoteCta"
    - "expectNoOverlapTryAgainOnAccidentalMatchResult after declare (D-07)"
    - "accidental_match.ts step module for resolve-dialog answered-question steps"

key-files:
  created:
    - e2e_test/step_definitions/accidental_match.ts
  modified:
    - e2e_test/start/pageObjects/AnsweredQuestionPage.ts
    - e2e_test/features/recall/accidental_match_reveal.feature
    - e2e_test/step_definitions/recall.ts

key-decisions:
  - "Page-object + thin When; extend accidental_match_reveal (D-11)"
  - "Shared Resolve→CTA helper for Build a link and Add as overlapped"
  - "Extract accidental_match steps to keep recall.ts under 250 lines"

patterns-established:
  - "Pattern: declare E2E asserts stay chrome + absent overlap-try-again* (AMR-09)"
  - "Pattern: overlap_try_again.feature left untouched to prove uncoupled"

requirements-completed: [AMR-08, AMR-09]

coverage:
  - id: D1
    description: "Resolve → Add as overlapped note stays on accidental-match result"
    requirement: AMR-08
    verification:
      - kind: e2e
        ref: "e2e_test/features/recall/accidental_match_reveal.feature#Add as overlapped note stays on accidental match without try-again"
        status: pass
    human_judgment: false
  - id: D2
    description: "After declare: no overlap try-again on accidental-match result; overlap suite uncoupled"
    requirement: AMR-09
    verification:
      - kind: e2e
        ref: "e2e_test/features/recall/accidental_match_reveal.feature#Add as overlapped note stays on accidental match without try-again"
        status: pass
      - kind: e2e
        ref: "e2e_test/features/recall/overlap_try_again.feature"
        status: pass
    human_judgment: false

duration: 6min
completed: 2026-08-05
status: complete
---

# Phase 11 Plan 02: Add as overlapped note E2E Summary

**Targeted E2E opens Resolve → Add as overlapped note, stays on accidental-match without try-again; overlap_try_again remains green and uncoupled (AMR-08/AMR-09).**

## Performance

- **Duration:** ~6 min
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments

- `openAddAsOverlappedNote` + `expectNoOverlapTryAgainOnAccidentalMatchResult` on AnsweredQuestionPage
- Shared `openResolveAndClickMatchedNoteCta` for Build a link and Add as overlapped
- Capability-named scenario on `accidental_match_reveal.feature` (no @wip, no phase numbers)
- Thin When/Then in `accidental_match.ts` (extracted from oversized `recall.ts`)
- Targeted Cypress: accidental_match_reveal 4/4 + overlap_try_again 2/2

## Task Commits

1. **Tasks 1–2 + wrap-up** — (this commit) feat(11-02)

## Files Created/Modified

| File | Change |
|------|--------|
| `AnsweredQuestionPage.ts` | openAddAsOverlappedNote; shared Resolve→CTA helper; no-try-again expect |
| `accidental_match_reveal.feature` | Add as overlapped stay-without-try-again scenario |
| `accidental_match.ts` | Accidental-match + overlap try-again steps (new module) |
| `recall.ts` | Removed extracted steps (under 250 lines) |

## Deviations

- Extracted accidental-match steps to `accidental_match.ts` during post-change-refactor (recall.ts was already >250; new steps pushed further over)
- Collapsed Resolve→CTA duplication into `openResolveAndClickMatchedNoteCta`

## Next

Phase 11 complete. Phase 12: title navigate, reopen resolve, E2E polish (AMR-05).
