---
phase: 02-cancel-refinement-layout-generation
plan: 03
subsystem: ui
tags: [vue, note-refinement, cancelable-api, frontend-api, vitest]

requires:
  - phase: 02-cancel-refinement-layout-generation
    provides: Cancelable loadRefinementLayout + empty retry (02-02)
  - phase: 01-shared-cancellation-contract
    provides: CancelableApiResult overload and identity-bound Cancel
provides:
  - Cancel-during-retry + concurrent/idempotent edge coverage in cancel suite
  - frontend-api.mdc cancelable opt-in documentation
affects:
  - Phase 3 extract-preview cancel (must stay out of cancelable: true until opted in)
  - Future callers copying blockUi patterns from frontend-api.mdc

tech-stack:
  added: []
  patterns:
    - Product cancelable docs mirror literal clientSetup overload (no parallel helper)
    - Cancel-during-retry asserted at NoteRefinement product seam

key-files:
  created: []
  modified:
    - frontend/tests/components/recall/NoteRefinement.layoutGeneration.cancel.spec.ts
    - .cursor/rules/frontend-api.mdc

key-decisions:
  - "No NoteRefinement.vue fix needed — cancel-during-retry already correct from 02-02"
  - "Docs forbid cancelable on mutations and cancelable runWithBlockingApiLoading"

patterns-established:
  - "frontend-api.mdc Cancelable blocking calls subsection is the canonical caller guide"
  - "rg exclusivity: cancelable: true only NoteRefinement.vue + clientSetup.ts under frontend/src"

requirements-completed: [CANC-02, CANC-04, REFN-02]

coverage:
  - id: D1
    description: Older concurrent blocker survives layout Cancel
    requirement: CANC-04
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/NoteRefinement.layoutGeneration.cancel.spec.ts#keeps an older concurrent blocker
        status: pass
    human_judgment: false
  - id: D2
    description: Retry starts a fresh generateRefinementSuggestions under cancelable blocker
    requirement: REFN-02
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/NoteRefinement.layoutGeneration.cancel.spec.ts#retries layout generation
        status: pass
    human_judgment: false
  - id: D3
    description: Double Cancel is idempotent with no toast
    requirement: CANC-02
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/NoteRefinement.layoutGeneration.cancel.spec.ts#ignores a second Cancel click
        status: pass
    human_judgment: false
  - id: D4
    description: Cancel during retry leaves empty retry and ignores late items
    requirement: REFN-02
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/NoteRefinement.layoutGeneration.cancel.spec.ts#cancels a pending retry without applying late layout items
        status: pass
    human_judgment: false
  - id: D5
    description: Extract/remove paths stay noncancelable; frontend-api documents cancelable opt-in
    requirement: CANC-02
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/NoteRefinement.removeLayout.loading.spec.ts+extractNote.spec.ts+rg exclusivity
        status: pass
      - kind: other
        ref: .cursor/rules/frontend-api.mdc#Cancelable blocking calls
        status: pass
    human_judgment: false

duration: 3min
completed: 2026-07-21
status: complete
---

# Phase 02 Plan 03: Edges + frontend-api Docs Summary

**Cancel suite hardened with cancel-during-retry coverage; frontend-api.mdc documents the literal cancelable blockUi opt-in without inventing a parallel API.**

## Performance

- **Duration:** ~3 min
- **Started:** 2026-07-21T08:44:08Z
- **Completed:** 2026-07-21T08:46:53Z
- **Tasks:** 2/2
- **Files modified:** 2

## Accomplishments

- Added cancel-during-retry case: second pending Cancel returns empty retry and ignores late layout items.
- Confirmed concurrent survivor, idempotent double Cancel, and retry-fresh call count remain green (6/6 cancel suite).
- Extract and remove-layout loading regressions green; `cancelable: true` under `frontend/src` only in `NoteRefinement.vue` and `clientSetup.ts`.
- Documented cancelable overload in `.cursor/rules/frontend-api.mdc`: status narrowing, silent accepted cancel, identity-bound Cancel, client-only abort, mutations forbidden.

## Task Commits

1. **Task 1: Green concurrent survival and retry-interrupt edges** - `6ab3ef0d00` (test)
2. **Task 2: Document cancelable opt-in in frontend-api.mdc** - `3294ce8d4e` (docs)

**Plan metadata:** (docs commit after SUMMARY)

## Files Created/Modified

- `frontend/tests/components/recall/NoteRefinement.layoutGeneration.cancel.spec.ts` — cancel-during-retry edge case
- `.cursor/rules/frontend-api.mdc` — Cancelable blocking calls subsection + table row

## Decisions Made

- No product fix in `NoteRefinement.vue`: existing settleLayout + CancelableApiResult path already satisfies cancel-during-retry.
- Docs state opt-in is for safe read-only blockers only; keep noncancelable `{ blockUi: true }` examples and `runWithBlockingApiLoading` noncancelable.

## Deviations from Plan

None - plan executed exactly as written.

### Deferred (plan-allowed)

- **File size:** `NoteRefinement.vue` remains ~465 lines (untouched this plan; deferred extract-only split from 02-02).

## Issues Encountered

None

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 2 product cancel + docs complete; Phase 3 may adopt cancel for extract-preview only when planned — do not copy cancelable onto mutations.

## Self-Check: PASSED

- FOUND: `frontend/tests/components/recall/NoteRefinement.layoutGeneration.cancel.spec.ts`
- FOUND: `.cursor/rules/frontend-api.mdc`
- FOUND: `6ab3ef0d00`
- FOUND: `3294ce8d4e`
---
*Phase: 02-cancel-refinement-layout-generation*
*Completed: 2026-07-21*
