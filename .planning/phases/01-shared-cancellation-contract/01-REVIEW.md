---
phase: 01-shared-cancellation-contract
reviewed: 2026-07-21T07:45:00Z
depth: standard
files_reviewed: 9
files_reviewed_list:
  - frontend/src/DoughnutApp.vue
  - frontend/src/components/commons/LoadingModal.vue
  - frontend/src/components/commons/Overlay.vue
  - frontend/src/managedApi/ApiStatusHandler.ts
  - frontend/src/managedApi/clientSetup.ts
  - frontend/tests/components/commons/LoadingModal.spec.ts
  - frontend/tests/helpers/GlobalApiLoadingModal.ts
  - frontend/tests/managedApi/clientSetup.loading.spec.ts
  - frontend/tests/managedApi/clientSetup.spec.ts
findings:
  critical: 0
  warning: 2
  info: 1
  total: 3
status: issues_found
---

# Phase 1: Code Review Report

**Reviewed:** 2026-07-21T07:45:00Z
**Depth:** standard
**Files Reviewed:** 9
**Status:** issues_found

## Summary

Full standard review of the shared cancellation contract (plans 01-01 through 01-03). The typed overload, identity-bound `finishLoading` acceptance, accepted-cancellation latch + `Promise.race`, silent late settlement, and selected-blocker Cancel projection are coherent with D-01–D-13 and covered by focused tests. No critical correctness or security defects found in the cancel lifecycle. Remaining issues: progressive-enhancement gap for `align-items: safe center`, and module-singleton pollution from the new global-modal test helper.

## Narrative Findings (AI reviewer)

## Warnings

### WR-01: `safe center` has no `center` fallback

**File:** `frontend/src/components/commons/Overlay.vue:47-52`
**Issue:** `.overlay--centered` sets only `align-items: safe center`. Per CSS forward-compatible parsing, if any component of a multi-value property is unsupported, the **entire declaration is ignored**. `safe` / `unsafe` for flex `align-items` are unsupported before Chrome/Edge 115, Safari/iOS 17.6, and Samsung Internet 23. On those engines the rule falls back to the initial flex `align-items` (`normal` → stretch), so a short “Processing…” stack pins to the top instead of staying centered — regressing the common fitting case that 01-03 and the UI-SPEC require to remain unchanged. `overflow-y: auto` still applies, so narrow overflow may remain scrollable, but ordinary centering does not. The Vitest browser regression runs only in Chromium (which supports `safe`), so it cannot catch this.
**Fix:** Keep progressive enhancement with a plain `center` first, then override with `safe center`:

```css
.overlay--centered {
  display: flex;
  align-items: center;
  align-items: safe center;
  justify-content: center;
  overflow-y: auto;
}
```

Unsupported browsers keep `center` (pre-fix fitting behavior; oversized stacks still get a vertical scroll path from `overflow-y: auto`). Supporting browsers keep the G-01-3-safe alignment.

### WR-02: `GlobalApiLoadingModal` never tears down the module `apiStatusHandler`

**File:** `frontend/tests/helpers/GlobalApiLoadingModal.ts:12-17`
**Issue:** `setup()` calls `setupGlobalClient(apiStatus.value)` and never calls `teardownGlobalClientForTesting` (or replaces the handler on unmount). `apiStatusHandler` in `clientSetup.ts` is a process-wide singleton. After the `LoadingModal` integration tests that mount this helper, the singleton keeps pointing at the helper’s (possibly unmounted) `states` array. Later tests in the same Vitest worker that call `apiCallWithLoading` without their own `setupGlobalClient`/`teardownGlobalClientForTesting` can push/pop loading state onto that orphaned object — order-dependent leakage across suites. Other loading-modal helpers in this repo already pair setup with teardown; this new helper does not.
**Fix:** Tear down on unmount (and/or document that callers must tear down in `afterEach`):

```ts
import { setupGlobalClient, teardownGlobalClientForTesting } from "@/managedApi/clientSetup"
import { computed, defineComponent, onUnmounted, ref } from "vue"

// inside setup():
setupGlobalClient(apiStatus.value)
onUnmounted(() => {
  teardownGlobalClientForTesting()
})
```

Also add `afterEach(() => teardownGlobalClientForTesting())` in `LoadingModal.spec.ts` for the suites that mount `GlobalApiLoadingModal`, matching `AssimilationPanel.loadingModal.spec.ts`.

## Info

### IN-01: Narrow regression does not assert computed `align-items`

**File:** `frontend/tests/components/commons/LoadingModal.spec.ts:120-167`
**Issue:** The viewport test asserts live geometry (centering delta, spinner/Cancel reachability, scroll metrics) but never checks that computed `align-items` resolves to a safe-capable value when the engine supports it, nor that a fallback `center` remains present in the stylesheet. Acceptable for G-01-3 endpoint proofs, but it leaves WR-01 invisible to CI.
**Fix:** Optional — after applying WR-01, either keep geometry-only coverage (sufficient if the cascade fallback is in CSS) or add a one-line computed-style assertion in Chromium that `align-items` includes `center` (with or without `safe`).

## Critical Issues

None.

### Review notes (no additional findings)

- **`clientSetup.ts` cancel path:** Latch is set only after exact-id `finishLoading` succeeds; abort and cancellation promise resolve follow; fulfillment/rejection handlers gate on the latch before toasts/`completed`; `.finally` idempotently clears state. Same-turn cancel-vs-resolve and late fulfill/reject consumption are covered in `clientSetup.loading.spec.ts` / `clientSetup.spec.ts`.
- **`LoadingModal` / `DoughnutApp`:** `ApiLoadingCancelControl` + `:key="cancelControl.id"` + one-shot action capture correctly enforce D-07–D-10; noncancelable newest blocker hides Cancel (tested).
- **`ApiStatusHandler`:** Optional `cancel`, monotonic ids, and boolean exact-id removal match the contract; no security-sensitive inputs on the cancel path.

---

_Reviewed: 2026-07-21T07:45:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
