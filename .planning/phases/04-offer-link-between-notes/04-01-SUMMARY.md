---
phase: 04-offer-link-between-notes
plan: 01
subsystem: ui
tags: [vue, recall, wiki-link, property, accidental-match]

requires:
  - phase: 03-reveal-both-notes-after-accidental-match
    provides: ACCIDENTAL_MATCH matched-notes-section with NoteShow stack
provides:
  - MatchedNoteLinkOffer preselected property-link path via updateTextField
  - Per-matched Link to this note CTAs with D-06 write gate
  - bareWikiLinkAvailable / relationshipOptionAvailable on LinkInsertionChoice
  - appendWikiLinkPropertyRow shared helper
affects:
  - 04-02 relationship finalize path
  - 04-03 E2E / human verify

tech-stack:
  added: []
  patterns:
    - "Recall-scoped LinkInsertionChoice wrapper bypassing useContentCursorInserter"
    - "Direct updateTextField property write via appendWikiLinkPropertyRow"

key-files:
  created:
    - frontend/src/components/recall/MatchedNoteLinkOffer.vue
    - frontend/tests/components/recall/MatchedNoteLinkOffer.spec.ts
  modified:
    - frontend/src/components/links/LinkInsertionChoice.vue
    - frontend/src/utils/noteContentPropertyRows.ts
    - frontend/src/components/notes/core/NoteEditableContent.vue
    - frontend/src/components/recall/AnsweredSpellingQuestion.vue
    - frontend/src/components/commons/Popups/PopButton.vue
    - frontend/tests/components/recall/AnsweredSpellingQuestion.spec.ts

key-decisions:
  - "Wave 1 hides relationship via relationshipOptionAvailable=false (stop-safe)"
  - "wikiPropertyOptionAvailable from parseNoteContentMarkdown, not cursor inserter"
  - "PopButton forwards attrs so link-to-matched-note-{id} lands on the button"

patterns-established:
  - "MatchedNoteLinkOffer: seed NoteSearchResult from NoteStorage realm, skip search"
  - "Property write: buildWikiLinkText → appendWikiLinkPropertyRow → updateTextField"

requirements-completed: [AM-04]

coverage:
  - id: D1
    description: Per-matched Link to this note CTA when reviewed notebook is writable
    requirement: AM-04
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/AnsweredSpellingQuestion.spec.ts#shows one Link to this note control per matched note when writable
        status: pass
    human_judgment: false
  - id: D2
    description: Preselected LinkInsertionChoice with bare wiki and relationship hidden; property write via updateNoteContent
    requirement: AM-04
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/MatchedNoteLinkOffer.spec.ts
        status: pass
    human_judgment: false
  - id: D3
    description: Readonly reviewed notebook omits CTAs; loading gate until realms resolve
    requirement: AM-04
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/AnsweredSpellingQuestion.spec.ts#omits link CTAs when reviewed notebook is readonly
        status: pass
    human_judgment: false

duration: 12min
completed: 2026-07-24
status: complete
---

# Phase 4 Plan 01: Property-link offer tracer Summary

**Accidental-match recall can open a preselected property-link offer and write a wiki-link property via `updateTextField` without touching the cursor inserter.**

## Performance

- **Duration:** ~12 min
- **Started:** 2026-07-24T04:56:41Z
- **Completed:** 2026-07-24T05:04:00Z
- **Tasks:** 2/2
- **Files modified:** 10

## Accomplishments

- Shipped `MatchedNoteLinkOffer` landing on `LinkInsertionChoice` with matched title preselected (no search).
- Property path writes through `appendWikiLinkPropertyRow` + `storedApi().updateTextField`; never auto-writes on open.
- Per-row `Link to this note` CTAs gated on currentUser + writable reviewed realm + loaded matched realm (D-06).
- Wave 1 hides bare wiki insert and relationship option (`bareWikiLinkAvailable` / `relationshipOptionAvailable` false).

## Task Commits

| Task | Name | Commit | Notes |
|------|------|--------|-------|
| 1 | End-to-end property link offer (tracer) | `9c3007450e` | Includes multi-match / readonly / loading-gate Vitest (Task 2 criteria) |
| 2 | Harden CTA gate + multi-match CTA count Vitest | _(same commit)_ | Already covered in Task 1 tests; no extra code |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] PopButton multi-root attrs**
- **Found during:** Task 1 (data-testid not appearing on CTA)
- **Issue:** Multi-root `PopButton` did not forward `data-testid` to the trigger button.
- **Fix:** `inheritAttrs: false` + `v-bind="$attrs"` on the button.
- **Files modified:** `frontend/src/components/commons/Popups/PopButton.vue`
- **Commit:** `9c3007450e`

**2. [Rule 1 - Bug] Vite cannot RED on missing SFC**
- **Found during:** Task 1 TDD RED
- **Issue:** Importing a not-yet-created `.vue` fails Vite dep scan before assertions run.
- **Fix:** Implemented component with tests in one GREEN commit; documented here.
- **Commit:** `9c3007450e`

## TDD Gate Compliance

- Combined RED+GREEN into one `feat(04-01)` commit because missing SFC imports break Vitest before failing assertions can run.
- Behavior covered by Vitest files named in the plan; both exit 0 after commit.

## Auth Gates

None.

## Known Stubs

None — property path is fully wired; relationship finalize intentionally deferred to 04-02 via `relationshipOptionAvailable=false`.

## Threat Flags

None beyond plan register (reuses existing `updateNoteContent` auth).

## Self-Check: PASSED

- FOUND: `frontend/src/components/recall/MatchedNoteLinkOffer.vue`
- FOUND: `frontend/tests/components/recall/MatchedNoteLinkOffer.spec.ts`
- FOUND: commit `9c3007450e`
- Vitest verify exit 0; no `useContentCursorInserter` in MatchedNoteLinkOffer
