---
phase: 03-cancel-extraction-preview-generation
plan: 01
subsystem: testing
tags: [vitest, tdd-red, extraction-preview, cancel, note-refinement]

requires:
  - phase: 02-cancel-refinement-layout-generation
    provides: createDeferredGate, clickLoadingModalCancel, loadingModalMask, layout cancel suite patterns
provides:
  - mountNoteRefinementPendingExtractionPreview pending-preview helper
  - Failing NoteRefinement.extractionPreview.cancel.spec.ts locking REFN-03/04
affects:
  - 03-02 (GREEN product cancelable opt-in)
  - 03-03 (edges / docs)

tech-stack:
  added: []
  patterns:
    - "Pending Extract gate after layout ready (not pending-layout mount)"
    - "Reuse Phase 2 Cancel DOM helpers; domain asserts differ (keep selection / prior preview)"

key-files:
  created:
    - frontend/tests/components/recall/NoteRefinement.extractionPreview.cancel.spec.ts
  modified:
    - frontend/tests/components/recall/noteRefinementExtractionTestSupport.ts

key-decisions:
  - "RED only — no frontend/src product edits; Cancel absent is the correct failure"
  - "Pending helper mounts layout ready then holds extractNotePreview (mirror extractNote loading, not pending-layout)"

patterns-established:
  - "mountNoteRefinementPendingExtractionPreview: deferred extractNotePreview + nextTick, no flushPromises"
  - "Extract-cancel asserts layout retained + selection; regenerate-cancel asserts prior expectPreviewFields"

requirements-completed: []  # RED locks REFN-03/04; product completion is 03-02/03

coverage:
  - id: D1
    description: "Pending-preview mount helper holds extractNotePreview open for Cancel asserts"
    requirement: REFN-03
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/noteRefinementExtractionTestSupport.ts#mountNoteRefinementPendingExtractionPreview
        status: pass
    human_judgment: false
  - id: D2
    description: "Failing cancel suite locks REFN-03 Cancel affordance and REFN-04 Extract/regenerate cancel outcomes"
    requirement: REFN-04
    verification:
      - kind: unit
        ref: "frontend/tests/components/recall/NoteRefinement.extractionPreview.cancel.spec.ts#shows blocking Cancel while preview generates (REFN-03)"
        status: fail
    human_judgment: false
    rationale: "Intentional TDD RED — product cancelable opt-in lands in 03-02"

duration: 8min
completed: 2026-07-21
status: complete
---

# Phase 03 Plan 01: Extraction Preview Cancel RED Summary

**Vitest pending-preview helpers and a failing cancel suite that lock REFN-03/04 before product cancelability (Wave 1 TDD RED).**

## Performance

- **Duration:** ~8 min
- **Started:** 2026-07-21T09:17:30Z
- **Completed:** 2026-07-21T09:20:00Z
- **Tasks:** 2/2
- **Files modified:** 2

## Accomplishments

- Exported `mountNoteRefinementPendingExtractionPreview` — layout flushed, item selected, Extract clicked, `extractNotePreview` held via `createDeferredGate`, returns without `flushPromises`.
- Authored `NoteRefinement.extractionPreview.cancel.spec.ts` covering pending Cancel (REFN-03), Extract-cancel preserve selection + late non-apply (REFN-04), and Ask-AI-to-retry cancel keeping prior fields (D-06).
- Confirmed RED for missing Cancel on the preview blocker; existing `extractNote.spec.ts` remains green (15/15).

## Task Commits

| Task | Commit | Description |
|------|--------|-------------|
| 1–2 | c84d794937 | `test(03-01): add failing extraction-preview cancel suite` |

## Deviations from Plan

None - plan executed exactly as written.

## TDD Gate Compliance

- RED gate: cancel suite fails because `Cancel` is absent while `AI is generating preview...` is shown (product still uses noncancelable `runWithBlockingApiLoading`).
- GREEN gate: deferred to Plan 03-02 (product opt-in).

## Verification Evidence

**extractNote (regression — green):**
```
✓ NoteRefinement.extractNote.spec.ts (15 tests)
```

**cancel suite (RED — expected):**
```
× shows blocking Cancel while preview generates (REFN-03)
  AssertionError: expected 'AI is generating preview...' to contain 'Cancel'
× cancels from Extract... (REFN-04)
  TestingLibraryElementError: Unable to find an element with the text: Cancel
× cancels Ask AI to retry... (REFN-04 / D-06)
  TestingLibraryElementError: Unable to find an element with the text: Cancel
```

## Known Stubs

None.

## Threat Flags

None — test-only surface; no new product endpoints or auth paths.

## Self-Check: PASSED
