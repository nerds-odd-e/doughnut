---
phase: 02-cancel-refinement-layout-generation
plan: 01
subsystem: testing
tags: [vitest, tdd-red, note-refinement, cancel, loading-modal]

requires:
  - phase: 01-shared-cancellation-contract
    provides: Cancelable apiCallWithLoading overload, identity-bound Cancel, GlobalApiLoadingModal harness
provides:
  - Failing cancel+retry Vitest suite locking REFN-01/02 and CANC-01–04
  - Pending-layout mount and Cancel/retry test helpers
  - Split noteRefinement*TestSupport modules under 250 lines
affects:
  - 02-02 (GREEN product opt-in)
  - 02-03 (concurrent/retry edges + docs)

tech-stack:
  added: []
  patterns:
    - Deferred generateRefinementSuggestions gate without flushPromises before Cancel assert
    - Idempotent Cancel via held button element (LoadingModal pattern)
    - CANC-04 single GlobalApiLoadingModal + NoteRefinement-only mount

key-files:
  created:
    - frontend/tests/components/recall/NoteRefinement.layoutGeneration.cancel.spec.ts
    - frontend/tests/components/recall/noteRefinementLayoutLoadingTestSupport.ts
    - frontend/tests/components/recall/noteRefinementExtractionTestSupport.ts
    - frontend/tests/components/recall/noteRefinementExportTestSupport.ts
    - frontend/tests/components/recall/noteRefinementRemoveTestSupport.ts
  modified:
    - frontend/tests/components/recall/noteRefinementTestSupport.ts
    - frontend/tests/components/recall/NoteRefinement.removeLayout.loading.spec.ts
    - frontend/tests/components/recall/NoteRefinement.extractNote.spec.ts
    - frontend/tests/components/recall/NoteRefinement.exportExtractRequest.spec.ts
    - frontend/tests/components/recall/NoteRefinement.exportBreakdownRequest.spec.ts
    - frontend/tests/components/recall/NoteRefinement.removeLayout.spec.ts
    - frontend/tests/components/recall/NoteRefinement.layoutSelection.spec.ts

key-decisions:
  - "Drop NoteRefinement data-test-id assert; wrapper.exists() is enough for dialog survival"
  - "Idempotent Cancel clicks the same held Cancel element twice (not expect getByText throw)"
  - "Split kitchen-sink noteRefinementTestSupport along layout-loading / extraction / export / remove seams"

patterns-established:
  - "Pending layout mount: mockSdkServiceWithImplementation + createDeferredGate before mount; await nextTick only"
  - "Cancel suite stays RED until Plan 02 opts layout into cancelable blockUi"

requirements-completed: [CANC-01, CANC-02, CANC-03, CANC-04, REFN-01, REFN-02]

coverage:
  - id: D1
    description: Pending-layout mount and Cancel/retry helpers without flushing layout
    requirement: REFN-01
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/NoteRefinement.removeLayout.loading.spec.ts
        status: pass
    human_judgment: false
  - id: D2
    description: Failing cancel suite locks pending Cancel, silent cancel, concurrent survivor, empty retry
    requirement: CANC-01
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/NoteRefinement.layoutGeneration.cancel.spec.ts
        status: fail
    human_judgment: false
    rationale: "TDD RED — failures cite missing Cancel / loading mask / AI is generating layout... until 02-02"

duration: 45min
completed: 2026-07-21
status: complete
---

# Phase 02 Plan 01: Layout Cancel Suite RED Summary

**Failing Vitest cancel+retry suite and pending-mount helpers lock REFN-01/02 and CANC-01–04 before any NoteRefinement product opt-in.**

## Performance

- **Duration:** ~45 min (includes interrupt + resume)
- **Started:** 2026-07-21T08:00:00Z (approx Task 1)
- **Completed:** 2026-07-21T08:40:00Z
- **Tasks:** 2/2
- **Files modified:** 12

## Accomplishments

- Pending-layout mount helper holds `generateRefinementSuggestions` without `flushPromises`; Cancel/retry helpers target teleported overlay and `retry-refinement-layout`.
- Cancel suite authored for REFN-01/02 and CANC-01–04; focused run exits non-zero for missing cancelable blocker / Cancel / layout message (not harness typos).
- Split `noteRefinementTestSupport.ts` (was 536 lines) into cohesive modules all under 250 lines; existing extract/remove/export/selection specs stay green.

## Task Commits

1. **Task 1: Add pending-layout mount and Cancel/retry helpers** - `487e5b806d` (test)
2. **Task 2: Author failing layout-generation cancel suite** - `38b5a10b19` (test)

**Plan metadata:** (docs commit after SUMMARY)

## Files Created/Modified

- `NoteRefinement.layoutGeneration.cancel.spec.ts` — RED cancel+retry observable suite
- `noteRefinementLayoutLoadingTestSupport.ts` — deferred gate, mask, pending mount, Cancel/retry
- `noteRefinementExtractionTestSupport.ts` — extraction preview helpers
- `noteRefinementExportTestSupport.ts` — export request helpers
- `noteRefinementRemoveTestSupport.ts` — nested layout + remove confirm helpers
- `noteRefinementTestSupport.ts` — core mount/setup/selection (217 lines)

## Decisions Made

- Idempotent Cancel: hold the Cancel DOM node and click twice (mirror LoadingModal.spec.ts).
- Drop nonexistent `data-test-id="NoteRefinement"`; assert `wrapper.exists()`.
- CANC-04: one `GlobalApiLoadingModal` via `render()`, then mount `NoteRefinement` alone (no second modal resetting `setupGlobalClient`).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Harness fixes so RED cites product gaps**
- **Found during:** Task 2 (resume Phase A)
- **Issue:** Idempotency expected `getByText` throw; NoteRefinement test-id missing; CANC-04 risked dual GlobalApiLoadingModal
- **Fix:** Held Cancel click; `wrapper.exists()`; NoteRefinement-only mount after single modal render
- **Files modified:** `NoteRefinement.layoutGeneration.cancel.spec.ts`
- **Verification:** Focused cancel run fails on missing mask/Cancel/layout message
- **Committed in:** Task 2 commit

**2. [Rule 2 - Missing critical functionality] Split oversized test support**
- **Found during:** Task 2 post-change-refactor (Phase B)
- **Issue:** `noteRefinementTestSupport.ts` at 536 lines (>250)
- **Fix:** Split along layout-loading / extraction / export / remove seams; update imports
- **Files modified:** support modules + dependent specs
- **Verification:** removeLayout.loading, extractNote, export*, layoutSelection, removeLayout green; cancel still RED
- **Committed in:** Task 2 commit

## TDD Gate Compliance

- RED gate: Task 1 helpers `487e5b806d`; Task 2 cancel suite fails focused run (correct product gap)
- GREEN gate: deferred to Plan 02-02 (product `NoteRefinement.vue` opt-in) — expected for type:tdd Wave 0

## Self-Check: PASSED

- FOUND: `frontend/tests/components/recall/NoteRefinement.layoutGeneration.cancel.spec.ts`
- FOUND: `frontend/tests/components/recall/noteRefinementLayoutLoadingTestSupport.ts`
- FOUND: `487e5b806d` (Task 1)
- FOUND: `38b5a10b19` (Task 2)
