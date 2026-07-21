---
phase: 01-shared-cancellation-contract
reviewed: 2026-07-21T07:45:00Z
depth: standard
files_reviewed: 2
files_reviewed_list:
  - frontend/src/components/commons/Overlay.vue
  - frontend/tests/components/commons/LoadingModal.spec.ts
findings:
  critical: 0
  warning: 1
  info: 1
  total: 2
status: issues_found
---

# Phase 1: Code Review Report

**Reviewed:** 2026-07-21T07:45:00Z
**Depth:** standard
**Files Reviewed:** 2
**Status:** issues_found

## Summary

Gap-closure plan 01-03 correctly pairs `overflow-y: auto` with overflow-aware vertical alignment and adds a real Chromium 1280×720 / 320×568 reachability regression for G-01-3. The Chromium path matches the diagnosed fix. One maintainability/correctness gap remains: `align-items: safe center` is declared alone, so browsers that do not understand `safe` drop the entire `align-items` declaration and lose ordinary vertical centering for fitting blockers.

## Narrative Findings (AI reviewer)

### Warnings

### WR-01: `safe center` has no `center` fallback

**File:** `frontend/src/components/commons/Overlay.vue:47-52`
**Issue:** `.overlay--centered` sets only `align-items: safe center`. Per CSS forward-compatible parsing, if any component of a multi-value property is unsupported, the **entire declaration is ignored**. `safe` / `unsafe` for flex `align-items` are unsupported before Chrome/Edge 115, Safari/iOS 17.6, and Samsung Internet 23 (caniuse / MDN). On those engines the rule falls back to the initial flex `align-items` (`normal` → stretch), so a short “Processing…” stack pins to the top instead of staying centered — regressing the common fitting case that 01-03 and the UI-SPEC require to remain unchanged. `overflow-y: auto` still applies, so narrow overflow may remain scrollable, but ordinary centering does not. The new Vitest browser regression runs only in Chromium (which supports `safe`), so it cannot catch this.
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

Unsupported browsers keep `center` (pre-fix behavior for fitting stacks; oversized stacks still get a vertical scroll path from `overflow-y: auto`, matching the diagnosed “scroll alone” half of the counterfactual). Supporting browsers keep the G-01-3-safe alignment.

### Info

### IN-01: Narrow regression does not assert computed `align-items`

**File:** `frontend/tests/components/commons/LoadingModal.spec.ts:120-167`
**Issue:** The viewport test asserts live geometry (centering delta, spinner/Cancel reachability, scroll metrics) but never checks that computed `align-items` resolves to a safe-capable value when the engine supports it, nor that a fallback `center` remains present in the stylesheet. That is acceptable for G-01-3 endpoint proofs, but it leaves WR-01 invisible to CI.
**Fix:** Optional — after applying WR-01, either keep geometry-only coverage (sufficient if the cascade fallback is in CSS) or add a one-line computed-style assertion in Chromium that `align-items` includes `center` (with or without `safe`).

## Critical Issues

None.

---

_Reviewed: 2026-07-21T07:45:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
