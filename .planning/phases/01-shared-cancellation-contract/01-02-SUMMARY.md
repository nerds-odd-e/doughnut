---
phase: 01-shared-cancellation-contract
plan: 02
subsystem: frontend-ui
tags: [vue, loading-modal, cancellation, vitest]

requires:
  - phase: 01-01
    provides: Identity-bound loading-state cancellation and the explicit cancelled outcome
provides:
  - Dormant selected-blocker Cancel control with unchanged noncancelable defaults
  - Exact message, identity, and action projection from one newest blocking state
  - Stale-control protection across immediate older-blocker revelation
affects: [phase-02-refinement-layout-generation, global-api-loading-modal]

tech-stack:
  added: []
  patterns:
    - A keyed child control captures one state action so detached DOM references cannot retarget a replacement blocker
    - Production and high-level test helpers project all blocker props from one computed selected state

key-files:
  created: []
  modified:
    - frontend/src/components/commons/LoadingModal.vue
    - frontend/src/DoughnutApp.vue
    - frontend/tests/helpers/GlobalApiLoadingModal.ts
    - frontend/tests/components/commons/LoadingModal.spec.ts

key-decisions:
  - "The selected blocker's message, id, and cancel action are passed directly from the same computed state object."
  - "The keyed Cancel child captures its original action at setup so a stale DOM reference cannot adopt a replacement state's callback."

patterns-established:
  - "Selected-state projection: never derive visible cancelability separately from the visible blocker."
  - "Identity-bound control: replacement keeps the overlay mounted but replaces the action-owning child instance."

requirements-completed: [COHE-01]

coverage:
  - id: D1
    description: "LoadingModal preserves the existing spinner/message default and renders one fixed neutral native Cancel control only when an action is supplied."
    requirement: COHE-01
    verification:
      - kind: automated_ui
        ref: "frontend/tests/components/commons/LoadingModal.spec.ts#renders one neutral native Cancel button only when an action is supplied"
        status: pass
      - kind: automated_ui
        ref: "frontend/tests/components/commons/LoadingModal.spec.ts#keeps long messages centered with the optional action"
        status: pass
    human_judgment: false
  - id: D2
    description: "The global modal exposes only the newest selected blocker's action, reveals an older blocker in place, and rejects stale retargeting."
    requirement: COHE-01
    verification:
      - kind: integration
        ref: "frontend/tests/components/commons/LoadingModal.spec.ts#cancels the newest blocker and reveals the older action in the same overlay"
        status: pass
      - kind: integration
        ref: "frontend/tests/components/commons/LoadingModal.spec.ts#hides an older cancelable action behind the newest noncancelable blocker"
        status: pass
    human_judgment: false
  - id: D3
    description: "All current product blockers remain noncancelable while the dormant seam passes the complete frontend regression and build gates."
    requirement: COHE-01
    verification:
      - kind: other
        ref: "CURSOR_DEV=true nix develop -c pnpm frontend:verify"
        status: pass
      - kind: other
        ref: "rg 'cancelable:\\s*true' frontend/src"
        status: pass
    human_judgment: false

duration: 8min
completed: 2026-07-21
status: complete
---

# Phase 1 Plan 2: Conditional Modal Control Summary

**The global blocker now has a dormant, neutral, identity-bound Cancel path that follows only the newest selected loading state while every existing product blocker remains unchanged.**

## Performance

- **Duration:** 8 min
- **Started:** 2026-07-21T05:04:42Z
- **Completed:** 2026-07-21T05:12:24Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments

- Added one fixed native `Cancel` button with the approved neutral DaisyUI treatment, visible focus styling, and no placeholder when absent.
- Projected message, id, and action directly from the same newest blocking state in production and the high-level test helper.
- Proved immediate older-state revelation in the same overlay, hidden cancelability, and stale/repeated action safety.
- Preserved long-message reflow and left all current product call sites button-free.

## Task Commits

1. **Task 1 RED: Define optional modal control behavior** — `b48f20a051` (test)
2. **Task 1 GREEN: Add identity-bound modal control** — `e7051c31a2` (feat)
3. **Task 2 RED: Define selected-blocker cancellation plumbing** — `1f52b0eb90` (test)
4. **Task 2 GREEN: Bind modal action to selected blocker** — `dc2f381e67` (feat)

## Files Created/Modified

- `frontend/src/components/commons/LoadingModal.vue` — Renders the optional fixed-copy action and freezes it to one keyed child identity.
- `frontend/src/DoughnutApp.vue` — Passes the selected state's message, id, and action together.
- `frontend/tests/helpers/GlobalApiLoadingModal.ts` — Mirrors the production selected-state projection.
- `frontend/tests/components/commons/LoadingModal.spec.ts` — Covers defaults, styling, reflow, replacement, overlap, hidden capability, and stale input.

## Decisions Made

- Kept `Cancel` fixed inside the component so callers cannot imply cooperative server termination with alternate labels.
- Captured the action in a keyed child component because Vue can update an element's event invoker before removing a keyed parent; a detached stale element must retain only its original idempotent closure.
- Kept the global overlay mounted while selected blocker content changes, preserving the existing spinner and transition behavior.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Prevented detached controls from adopting a replacement action**
- **Found during:** Task 2 (selected-blocker high-level RED/GREEN test)
- **Issue:** Keying the native button alone replaced it for normal rendering, but Vue updated the old DOM event invoker during patching; a test-held stale element could invoke the newly revealed blocker's action.
- **Fix:** Moved the button into a keyed child component that captures its action during setup, so the old instance and any detached DOM reference remain bound to the original loading state.
- **Files modified:** `frontend/src/components/commons/LoadingModal.vue`
- **Verification:** The stale button is invoked again after older-state revelation, and the older blocker remains visible.
- **Committed in:** `dc2f381e67`

---

**Total deviations:** 1 auto-fixed bug.
**Impact on plan:** The fix is required by D-10 and the high-severity stale-action threat; it adds no product caller, API, or behavior beyond the approved structure seam.

## Issues Encountered

- The first keyed-button implementation did not preserve the old event callback on a detached DOM reference because Vue patches event invokers in place. The identity-owning child component resolved this without changing the visual tree or public props.

## User Setup Required

None - no external service configuration required.

## Known Stubs

None.

## Jidoka and Refactor Gate

- End-of-plan Jidoka found no value fork, credential gate, unsafe caller adoption, or unresolved blocker.
- Fresh post-change refactor review found no duplication, speculative abstraction, dead code, or file over 250 lines; related focused tests remained green.

## Next Phase Readiness

- Phase 2 can opt one safe read-only generated request into the shared contract and provide only its dialog-local retry state.
- Existing blockers remain noncancelable and unchanged.
- No blockers.

## Self-Check: PASSED

- All four claimed files and all four RED/GREEN task commits exist.
- Coverage metadata classifies all three deliverables as auto-covered by passing evidence.
- Focused modal/managed-API tests, all 1,561 frontend tests, production build, typecheck, and formatting pass.
- The only `cancelable: true` occurrence under `frontend/src` is the shared option type; no product caller opts in.

---
*Phase: 01-shared-cancellation-contract*
*Completed: 2026-07-21*
