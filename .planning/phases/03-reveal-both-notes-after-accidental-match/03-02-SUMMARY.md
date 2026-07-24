---
phase: 03-reveal-both-notes-after-accidental-match
plan: 02
subsystem: ui
tags: [accidental-match, AnsweredSpellingQuestion, matchedNotes, NoteShow, DaisyUI]

requires:
  - phase: 03-reveal-both-notes-after-accidental-match
    provides: matchedNotes populated on ACCIDENTAL_MATCH from answer-spelling API
provides:
  - Distinct ACCIDENTAL_MATCH alert copy in AnsweredSpellingQuestion
  - Vertical Matched note(s) NoteShow stack (data-testid=matched-notes-section)
  - Plain incorrect alert path unchanged
  - AnsweredQuestionBuilder.withMatchedNotes for frontend fixtures
affects:
  - 03-03 (E2E / messaging)
  - Phase 4 add-link UI under matched-notes-section

tech-stack:
  added: []
  patterns:
    - Branch alert copy on answer.outcome === ACCIDENTAL_MATCH; miss styling stays daisy-alert-error
    - Matched section: mt-6 + text-lg font-semibold heading + flex flex-col gap-4 NoteShows

key-files:
  created:
    - frontend/tests/components/recall/AnsweredSpellingQuestion.spec.ts
  modified:
    - frontend/src/components/recall/AnsweredSpellingQuestion.vue
    - packages/doughnut-test-fixtures/src/AnsweredQuestionBuilder.ts

key-decisions:
  - "Followed UI-SPEC accidental-match copy and Matched note(s) heading verbatim"
  - "Stubbed NoteShow in Vitest to assert noteId/expandChildren without realm load"
  - "AM-03 left In Progress until 03-03 E2E closes reveal story"

patterns-established:
  - "Pattern: AnsweredSpellingQuestion mounts with NoteShow stub + data-testid matched-notes-section"
  - "Pattern: withMatchedNotes on AnsweredQuestionBuilder for ACCIDENTAL_MATCH fixtures"

requirements-completed: []  # AM-03 spans 03-03 E2E; UI alone does not close requirement checkbox

coverage:
  - id: D1
    description: "ACCIDENTAL_MATCH shows distinct alert copy and Matched note(s) NoteShow stack"
    requirement: AM-03
    verification:
      - kind: unit
        ref: "frontend/tests/components/recall/AnsweredSpellingQuestion.spec.ts#shows distinct alert copy and a matched notes section with NoteShows"
        status: pass
      - kind: unit
        ref: "CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/components/recall/AnsweredSpellingQuestion.spec.ts"
        status: pass
    human_judgment: false
  - id: D2
    description: "Plain wrong answers keep is incorrect copy and omit matched-notes-section"
    requirement: AM-03
    verification:
      - kind: unit
        ref: "frontend/tests/components/recall/AnsweredSpellingQuestion.spec.ts#keeps incorrect alert copy and omits matched notes section"
        status: pass
    human_judgment: false
  - id: D3
    description: "Defensive empty matchedNotes omits section but keeps accidental-match alert"
    requirement: AM-03
    verification:
      - kind: unit
        ref: "frontend/tests/components/recall/AnsweredSpellingQuestion.spec.ts#omits matched notes section when matchedNotes is empty"
        status: pass
    human_judgment: false

duration: 4min
completed: 2026-07-24
status: complete
---

# Phase 03 Plan 02: Accidental-match reveal UI Summary

**Spelling answer UI now shows distinct accidental-match miss copy and a vertical Matched note(s) NoteShow stack from `matchedNotes`.**

## Performance

- **Duration:** ~4 min
- **Started:** 2026-07-24T04:19:55Z
- **Completed:** 2026-07-24T04:23:30Z
- **Tasks:** 2/2
- **Files modified:** 3

## Accomplishments

- Vitest RED/GREEN for accidental-match vs plain-wrong alert copy and matched-notes-section presence/absence.
- `AnsweredSpellingQuestion.vue` branches on `ACCIDENTAL_MATCH`, renders full `NoteShow` per matched note (`expand-children=false`), keeps plain incorrect unchanged.
- Extended `AnsweredQuestionBuilder` with `withMatchedNotes` for tidy fixtures.
- Scope guard D-06: no add-link / primary accent chrome on this surface.

## Task Commits

1. **Task 1 RED:** `8dfc6a9154` — test(03-02): add failing test for accidental-match reveal UI
2. **Task 2 GREEN:** `93acbeeab4` — feat(03-02): reveal matched notes after accidental spelling match

**Plan metadata:** (docs commit follows)

## Files Created/Modified

- `AnsweredSpellingQuestion.spec.ts` — accidental-match / plain-wrong / empty matchedNotes coverage
- `AnsweredSpellingQuestion.vue` — alert branch + matched-notes-section NoteShow stack
- `AnsweredQuestionBuilder.ts` — `withMatchedNotes(NoteTopology[])`

## Decisions Made

- Used UI-SPEC copy exactly: `names another note — not correct for this review.`
- Stubbed `NoteShow` in unit tests to assert props without NoteRealmLoader weight.
- Did not mark AM-03 complete (same as 03-01) — 03-03 E2E still open.

## Deviations from Plan

None - plan executed exactly as written.

## TDD Gate Compliance

- RED commit present (`test(03-02): …`) with non-zero exit before GREEN
- GREEN commit present after RED (`feat(03-02): …`) with Vitest exit 0

## Threat Flags

None — only renders `matchedNotes` ids from API props; no new endpoints or link-write controls (T-03-04).

## Known Stubs

None.

## Self-Check: PASSED

- `AnsweredSpellingQuestion.vue` FOUND with `matched-notes-section` and `ACCIDENTAL_MATCH`
- Spec file FOUND; Vitest 3/3 pass
- Commits `8dfc6a9154`, `93acbeeab4` FOUND
- No Phase 4 link UI in component
