---
phase: 07-compact-result-resolve-dialog-shell
plan: 01
subsystem: ui
tags: [vue, vitest, popbutton, modal, accidental-match, recall]

requires: []
provides:
  - AccidentalMatchResolveDialog title-only presentational list
  - Gated Resolve accidental match PopButton under ACCIDENTAL_MATCH alert
  - Compact result without stacked matched NoteShows / link CTAs
  - AccidentalMatch + Overlap Vitest coverage for resolve shell
affects:
  - 07-02 (E2E rewrite for CTA + dialog)
  - Phase 8 (path/breadcrumb in dialog)
  - Phase 9 (Build a link inside dialog)

actuals:
  tokens: 4303
  tasks: 2
  commits: 3

tech-stack:
  added: []
  patterns:
    - "PopButton→Modal host for optional resolve; presentational AccidentalMatchResolveDialog body"
    - "showResolveAccidentalMatchCta = ACCIDENTAL_MATCH && matchedNotes.length > 0 (never length alone)"

key-files:
  created:
    - frontend/src/components/recall/AccidentalMatchResolveDialog.vue
  modified:
    - frontend/src/components/recall/AnsweredSpellingQuestion.vue
    - frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts
    - frontend/tests/components/recall/AnsweredSpellingQuestionOverlap.spec.ts
    - frontend/components.d.ts

key-decisions:
  - "Slot AccidentalMatchResolveDialog without closer prop — Modal dismiss covers AMR-03"
  - "Keep MatchedNoteLinkOffer.vue on disk unused until Phase 9"
  - "afterEach unmount + body clear for Teleport Modal isolation across Vitest cases"

patterns-established:
  - "Capability testids: resolve-accidental-match / accidental-match-resolve-dialog / resolve-match-row-{id}"
  - "Title rows via text interpolation only (no v-html) for XSS mitigation T-07-01"

requirements-completed: [AMR-01, AMR-02, AMR-03]

coverage:
  - id: D1
    description: Compact ACCIDENTAL_MATCH result — no stacked matches; Resolve CTA when matches exist
    requirement: AMR-01
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts#shows compact accidental-match result with Resolve CTA and no stacked matches
        status: pass
    human_judgment: false
  - id: D2
    description: Resolve CTA opens Modal listing matched titles only
    requirement: AMR-02
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts#opens resolve dialog listing matched note titles only
        status: pass
    human_judgment: false
  - id: D3
    description: Dismiss resolve Modal via close button; remain on accidental-match result
    requirement: AMR-03
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts#dismisses resolve dialog via close button and stays on accidental-match result
        status: pass
    human_judgment: false
  - id: D4
    description: Empty matchedNotes and OVERLAP leak omit Resolve CTA
    requirement: AMR-01
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts#omits Resolve CTA when matchedNotes is empty
        status: pass
      - kind: unit
        ref: frontend/tests/components/recall/AnsweredSpellingQuestionOverlap.spec.ts#omits Resolve CTA even when matchedNotes leak on OVERLAP
        status: pass
    human_judgment: false

duration: 6min
completed: 2026-08-05
status: complete
---

# Phase 7 Plan 01: Compact resolve dialog shell Summary

**Dropped stacked matched NoteShows/link CTAs; gated Resolve accidental match PopButton opens a title-only Modal on ACCIDENTAL_MATCH.**

## Performance

- **Duration:** 6 min
- **Started:** 2026-08-05T08:56:44Z
- **Completed:** 2026-08-05T09:03:00Z
- **Tasks:** 2/2
- **Files modified:** 5

## Accomplishments

- Compact accidental-match result keeps reviewed NoteShow primary; no `matched-notes-section`
- Optional **Resolve accidental match** CTA opens title-only `AccidentalMatchResolveDialog`
- Unit edges: empty omit, OVERLAP leak omit, Modal dismiss stays on result

## Task Commits

1. **Task 1 RED:** `8c4e701b67` — test(07-01): add failing tests for compact resolve shell
2. **Task 1 GREEN:** `fd488de191` — feat(07-01): compact accidental-match result with resolve dialog
3. **Task 2:** `b53ceaf020` — test(07-01): cover resolve omit, OVERLAP leak, and dismiss

## Files Created/Modified

- `frontend/src/components/recall/AccidentalMatchResolveDialog.vue` — presentational title list
- `frontend/src/components/recall/AnsweredSpellingQuestion.vue` — Resolve PopButton; stacks/link CTAs removed
- `frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` — compact + dialog + edges
- `frontend/tests/components/recall/AnsweredSpellingQuestionOverlap.spec.ts` — no-resolve leak assert
- `frontend/components.d.ts` — auto-register AccidentalMatchResolveDialog

## Decisions Made

- Followed CONTEXT D-01..D-08 and UI-SPEC testids/copy; no closer prop on dialog body
- Retained `MatchedNoteLinkOffer.vue` unused for Phase 9 reuse

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Teleport Modal pollution broke dismiss assert**
- **Found during:** Task 2 (dismiss edge)
- **Issue:** Prior AccidentalMatch mounts left open Modals on `document.body`, so close-button dismiss left a sibling dialog list in the DOM
- **Fix:** Added `afterEach` unmount + `document.body.innerHTML = ""` in AccidentalMatch and Overlap specs
- **Files modified:** AccidentalMatch.spec.ts, Overlap.spec.ts
- **Commit:** `b53ceaf020`

## Known Stubs

None.

## Threat Flags

None beyond plan threat model (titles use text interpolation; no new endpoints).

## Self-Check: PASSED

- FOUND: frontend/src/components/recall/AccidentalMatchResolveDialog.vue
- FOUND: frontend/src/components/recall/AnsweredSpellingQuestion.vue
- FOUND: 8c4e701b67, fd488de191, b53ceaf020
