---
phase: 02-cancel-refinement-layout-generation
plan: 02
subsystem: ui
tags: [vue, note-refinement, cancelable-api, blockUi, AbortSignal]

requires:
  - phase: 02-cancel-refinement-layout-generation
    provides: Failing cancel suite (02-01) locking REFN-01/02 and CANC-01–04
  - phase: 01-shared-cancellation-contract
    provides: Cancelable apiCallWithLoading overload and identity-bound Cancel
provides:
  - Cancelable loadRefinementLayout with blockUi + layout message
  - Empty/cancelled refinement-layout-empty retry panel
  - Green cancel suite for REFN-01/02 and CANC-01–04
affects:
  - 02-03 (concurrent/retry docs edges; frontend-api.mdc)

tech-stack:
  added: []
  patterns:
    - CancelableApiResult status narrowing before layout apply
    - layoutLoadSettled gates empty panel until first attempt settles
    - Shared empty surface for cancel, zero-item success, cleared failure

key-files:
  created: []
  modified:
    - frontend/src/components/recall/NoteRefinement.vue

key-decisions:
  - "Single feat commit for Tasks 1–2: cancelable load and empty/retry are one vertical GREEN slice"
  - "layoutLoadSettled avoids showing empty panel before the first layout attempt settles"
  - "settleLayout helper collapses cancelled/completed cleanup without a second AbortController"

patterns-established:
  - "Caller opt-in: literal { blockUi: true, cancelable: true, message } + signal into generateRefinementSuggestions"
  - "Post-cancel domain UI: refinement-layout-empty + retry-refinement-layout → same loadRefinementLayout"

requirements-completed: [CANC-01, CANC-02, CANC-03, REFN-01, REFN-02]

coverage:
  - id: D1
    description: Pending layout shows AI is generating layout... and Cancel
    requirement: REFN-01
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/NoteRefinement.layoutGeneration.cancel.spec.ts#shows blocking Cancel while layout generates
        status: pass
    human_judgment: false
  - id: D2
    description: Cancel clears mask silently; no toast or contentUpdated; empty retry visible; late resolve does not apply
    requirement: CANC-02
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/NoteRefinement.layoutGeneration.cancel.spec.ts#cancels silently
        status: pass
    human_judgment: false
  - id: D3
    description: Ask AI to retry starts a fresh cancelable layout request
    requirement: REFN-02
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/NoteRefinement.layoutGeneration.cancel.spec.ts#retries layout generation
        status: pass
    human_judgment: false
  - id: D4
    description: Older concurrent blocker survives layout Cancel
    requirement: CANC-04
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/NoteRefinement.layoutGeneration.cancel.spec.ts#keeps an older concurrent blocker
        status: pass
    human_judgment: false

duration: 12min
completed: 2026-07-21
status: complete
---

# Phase 02 Plan 02: Cancelable Layout Load GREEN Summary

**NoteRefinement opts layout generation into the Phase 1 cancelable blocker and shows an in-dialog Ask AI to retry empty panel so the 02-01 cancel suite is green.**

## Performance

- **Duration:** ~12 min
- **Started:** 2026-07-21T08:39:07Z
- **Completed:** 2026-07-21T08:43:00Z
- **Tasks:** 2/2
- **Files modified:** 1

## Accomplishments

- `loadRefinementLayout` uses `{ blockUi: true, cancelable: true, message: "AI is generating layout..." }`, forwards `AbortSignal`, and returns early on `status === "cancelled"` without emit/toast.
- Empty panel `refinement-layout-empty` + `retry-refinement-layout` appears after settled cancel/empty; populated path unchanged.
- Extract-preview / create-note / remove blockers remain noncancelable.
- Focused cancel suite: 5/5 green; related extract/selection/remove-loading specs: 27/27 green.

## Task Commits

1. **Task 1 + Task 2: Cancelable load + empty/retry panel** - `97cf9a792a` (feat)

**Plan metadata:** (docs commit after SUMMARY)

## Files Created/Modified

- `frontend/src/components/recall/NoteRefinement.vue` — cancelable layout load, `layoutLoadSettled`, empty/retry panel

## Decisions Made

- Combined Tasks 1–2 into one feat commit: empty/retry is required for the same GREEN vertical slice; separate intermediate RED state had no product value.
- `layoutLoadSettled` gates empty UI so the panel does not flash before the first attempt settles.
- Deduped cancelled/completed cleanup via local `settleLayout` helper.

## Deviations from Plan

### Auto-fixed Issues

None - plan executed as written (Pattern 1 + UI-SPEC empty panel).

### Deferred (plan-allowed)

- **File size:** `NoteRefinement.vue` remains ~465 lines (pre-existing overage). Plan/RESEARCH directed minimal Behavior change and deferred extract-only cohesion split; not split in this plan.

## TDD Gate Compliance

- RED: 02-01 cancel suite (`38b5a10b19`)
- GREEN: `97cf9a792a` — focused cancel suite 5/5 pass

## Self-Check: PASSED

- FOUND: `frontend/src/components/recall/NoteRefinement.vue`
- FOUND: `97cf9a792a`
