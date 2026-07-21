---
phase: 03-cancel-extraction-preview-generation
plan: 02
subsystem: ui
tags: [vue, cancelable, apiCallWithLoading, extraction-preview, AbortSignal]

requires:
  - phase: 03-cancel-extraction-preview-generation/01
    provides: Failing cancel suite + pending-preview mount helper (RED)
  - phase: 01-shared-cancellation-contract
    provides: Cancelable apiCallWithLoading overload + LoadingModal Cancel
  - phase: 02-cancel-refinement-layout-generation
    provides: Caller-adoption pattern for cancelable layout load
provides:
  - Cancelable extraction-preview generation with domain no-op on cancel
  - Extract cancel preserves layout selection; regenerate cancel keeps prior preview
affects:
  - 03-03 (edges/docs)
  - Phase 4 create-note (must stay noncancelable)

tech-stack:
  added: []
  patterns:
    - "Cancelable preview: apiCallWithLoading(signal => extractNotePreview({..., signal}), { blockUi: true, cancelable: true, message: 'AI is generating preview...' })"
    - "Cancelled branch returns before preview apply / createError / showExtractionPreview mutation"
    - "Do not invent cancelable runWithBlockingApiLoading; leave create-note/remove/layout untouched"

key-files:
  created: []
  modified:
    - frontend/src/components/recall/NoteRefinement.vue
    - frontend/tests/components/recall/noteRefinementTestSupport.ts
    - frontend/tests/components/recall/NoteRefinement.extractNote.spec.ts
    - frontend/tests/components/recall/NoteRefinement.layoutSelection.spec.ts

key-decisions:
  - "Single feat commit for Tasks 1–2 vertical GREEN (mirror Phase 2)"
  - "Collapse fetchExtractionPreview into runExtractionPreview; cancelled is domain no-op (no settleLayout/clearSelection)"
  - "Keep message exactly AI is generating preview... (D-08)"
  - "Defer NoteRefinement.vue cohesion split (pre-existing ~465-line overage; plan minimal Behavior)"

patterns-established:
  - "Preview cancel no-op vs layout cancel settle — Extract stays enabled via preserved selection (D-04)"
  - "refinementLayoutSelectionApiCall(..., { signal: true }) for cancelable extractNotePreview spy asserts"

requirements-completed: [REFN-03, REFN-04]

coverage:
  - id: D1
    description: Pending preview shows global blocker with AI is generating preview... and Cancel
    requirement: REFN-03
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/NoteRefinement.extractionPreview.cancel.spec.ts#shows blocking Cancel while preview generates (REFN-03)
        status: pass
    human_judgment: false
  - id: D2
    description: Extract Cancel stays on layout, keeps selection, silent, ignores late data
    requirement: REFN-04
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/NoteRefinement.extractionPreview.cancel.spec.ts#cancels from Extract: stays on layout, keeps selection, silent, ignores late data (REFN-04)
        status: pass
    human_judgment: false
  - id: D3
    description: Ask AI to retry Cancel keeps prior preview fields unchanged
    requirement: REFN-04
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/NoteRefinement.extractionPreview.cancel.spec.ts#cancels Ask AI to retry without wiping prior preview (REFN-04 / D-06)
        status: pass
    human_judgment: false

duration: 3min
completed: 2026-07-21
status: complete
---

# Phase 03 Plan 02: Cancelable Extraction Preview Summary

**Opted extraction-preview into cancelable `apiCallWithLoading` so Cancel aborts only the browser request, keeps layout selections (or prior preview on regenerate), and never applies cancelled payloads.**

## Performance

- **Duration:** ~3 min
- **Started:** 2026-07-21T09:19:56Z
- **Completed:** 2026-07-21T09:23:09Z
- **Tasks:** 2/2
- **Files modified:** 4

## Accomplishments

- Collapsed `fetchExtractionPreview` + outer `runWithBlockingApiLoading` into one cancelable `runExtractionPreview` with status narrowing and AbortSignal forwarding.
- Cancel suite core REFN-03/04 cases green (pending Cancel, Extract-cancel preserve selection, regenerate-cancel keep fields, late non-apply).
- Create-note / remove / layout cancelable load left unchanged; no new empty-layout panel.

## Task Commits

1. **Task 1–2: Opt runExtractionPreview into cancelable blocking + green landings** - `e15de7d149` (feat)

**Plan metadata:** `0ab6ed4361` (docs: complete plan)

## Files Created/Modified

- `frontend/src/components/recall/NoteRefinement.vue` — cancelable preview path; cancelled no-op
- `frontend/tests/components/recall/noteRefinementTestSupport.ts` — optional `{ signal: true }` for API call asserts
- `frontend/tests/components/recall/NoteRefinement.extractNote.spec.ts` — expect AbortSignal on extractNotePreview
- `frontend/tests/components/recall/NoteRefinement.layoutSelection.spec.ts` — same for extract path only

## Decisions Made

- Single feat commit for Tasks 1–2 (plan + Phase 2 precedent).
- Preview cancel is domain no-op (contrast Phase 2 layout `settleLayout([])`).
- Message stays `AI is generating preview...` (D-08).
- File-size split of `NoteRefinement.vue` deferred (pre-existing overage; plan minimal Behavior).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Extract-note spy asserts broke on AbortSignal**
- **Found during:** Task 2 (related regression)
- **Issue:** `extractNotePreview` now receives wrapper-owned `signal`; exact-arg spies in extractNote/layoutSelection specs failed.
- **Fix:** Extended `refinementLayoutSelectionApiCall` with optional `{ signal: true }` → `expect.any(AbortSignal)`; updated extract call sites only (remove/export unchanged).
- **Files modified:** `noteRefinementTestSupport.ts`, `NoteRefinement.extractNote.spec.ts`, `NoteRefinement.layoutSelection.spec.ts`
- **Verification:** extractNote + layoutSelection + cancel suites green
- **Committed in:** same vertical GREEN feat commit

**Total deviations:** 1 auto-fixed (Rule 1)
**Impact on plan:** Necessary for related-test correctness; no product scope creep.

## Issues Encountered

None beyond the spy-assert deviation above.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 03-03 can cover edge/idempotent/create-note boundary docs and remaining cancel-suite edges if any.
- Create-note remains noncancelable for Phase 4 (`REFN-05`).

## Post-change-refactor

- **Duplication / naming / shotgun / dead code:** none — already clean; removed dead `fetchExtractionPreview` composite as planned.
- **File size:** `NoteRefinement.vue` remains ~461 lines (pre-existing overage). Plan directed minimal Behavior change and deferred cohesion split (same stance as Phase 2 02-02).
- **Related tests:** cancel suite (3), extractNote (15), layoutSelection (6), layoutGeneration.cancel (6) — all pass.

## REFACTOR COMPLETE

## Self-Check: PASSED

- `frontend/src/components/recall/NoteRefinement.vue` — FOUND
- Cancel suite green — FOUND
- Commits verified after commit step

---
*Phase: 03-cancel-extraction-preview-generation*
*Completed: 2026-07-21*
