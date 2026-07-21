---
phase: 01-shared-cancellation-contract
reviewed: 2026-07-21T05:21:23Z
depth: standard
files_reviewed: 8
files_reviewed_list:
  - frontend/src/managedApi/ApiStatusHandler.ts
  - frontend/src/managedApi/clientSetup.ts
  - frontend/tests/managedApi/clientSetup.loading.spec.ts
  - frontend/tests/managedApi/clientSetup.spec.ts
  - frontend/src/components/commons/LoadingModal.vue
  - frontend/src/DoughnutApp.vue
  - frontend/tests/helpers/GlobalApiLoadingModal.ts
  - frontend/tests/components/commons/LoadingModal.spec.ts
findings:
  critical: 2
  warning: 1
  info: 0
  total: 3
status: issues_found
---

# Phase 1: Code Review Report

**Reviewed:** 2026-07-21T05:21:23Z
**Depth:** standard
**Files Reviewed:** 8
**Status:** issues_found

## Summary

The identity-removal and accepted-cancellation race implementation is otherwise cohesive, and no existing product call site opts into cancellation. However, the public overloads can give a cancelable runtime call the noncancelable static return type, and the modal changes alter existing noncancelable rendering despite this being a Structure phase. One additional prop-contract weakness can leave the rendered control bound to an old action when the identity is omitted or reused. No security vulnerability was found in the reviewed scope.

## Critical Issues

### CR-01: [BLOCKER] Hoisted cancelable options select the noncancelable overload

**File:** `frontend/src/managedApi/clientSetup.ts:50-57`

**Issue:** `CancelableApiLoadingOptions` is structurally assignable to `ApiLoadingOptions`, and a zero-argument callback is assignable to both callback signatures. When cancelable options are stored in a variable, TypeScript selects the first overload and reports `Promise<T>`, while the implementation sees `options.cancelable` and returns `Promise<CancelableApiResult<T>>`. For example, `const options: CancelableApiLoadingOptions = { blockUi: true, cancelable: true }; const value = await apiCallWithLoading(() => request(), options)` is statically SDK-shaped but is runtime-wrapped as `{ status: "completed", result }`. This breaks D-01/D-03 and can make a caller read `data` or `error` from the wrong object. The existing type test declares a hoisted options value but never passes it to the function, so it does not cover this path.

**Fix:** Make the default overload reject the opt-in key and put the cancelable overload first. For example, define a noncancelable options type such as `ApiLoadingOptions & { cancelable?: never }`, use it in the raw-result overload, and retain `CancelableApiLoadingOptions` for the discriminated-result overload. Add a compile-time regression that passes a hoisted `CancelableApiLoadingOptions` value with a zero-argument callback and asserts `Promise<CancelableApiResult<T>>`.

### CR-02: [BLOCKER] New message CSS changes existing noncancelable blockers

**File:** `frontend/src/components/commons/LoadingModal.vue:71-73`

**Issue:** The new `max-width`, `overflow-wrap`, and `text-align` declarations apply to every loading message, including all existing noncancelable product blockers. Before this phase, multiline text used the browser's default start alignment and had no new viewport-width/wrapping constraint. The Phase 1 context and both plans classify this as a Structure phase and require existing blockers to remain externally unchanged; Plan 01-02 specifically says to preserve message CSS/behavior. This is therefore an observable product change outside the allowed dormant cancellation seam.

**Fix:** Remove these three declarations from Phase 1 and remove or rewrite the new computed-style assertion so it verifies compatibility without introducing a new default. If revised wrapping/alignment is desired, deliver and validate it in an explicitly behavioral UI phase rather than bundling it into the cancellation structure.

## Warnings

### WR-01: [WARNING] The component API does not require an identity with a cancel action

**File:** `frontend/src/components/commons/LoadingModal.vue:6-10,27,42-46`

**Issue:** `loadingStateId` and `cancelAction` are independently optional, but the child intentionally captures `cancelAction` only once and relies on the key to replace that capture. If a caller supplies an action without an id, or changes the action while reusing an id, Vue retains the child and the visible Cancel button continues invoking the old operation. Current `DoughnutApp.vue` plumbing passes a unique id and action together, so the shipped path is safe, but the public component contract permits the exact stale-action state the implementation is designed to prevent.

**Fix:** Represent the control as one optional identity/action value, such as `cancelControl?: { id: number; action: () => void }`, or use a discriminated props union that requires `loadingStateId: number` whenever `cancelAction` exists. Key and capture from that paired value, and add a type-level or component regression proving an action cannot be supplied without its identity.

---

_Reviewed: 2026-07-21T05:21:23Z_
_Reviewer: generic-agent workaround (gsd-code-reviewer role)_
_Depth: standard_
