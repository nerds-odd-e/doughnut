# Accidental-match confusion adjustment — cleanup

**Status:** in progress (Phase 2 next)
**Type mix:** Structure
**Context:** Follow-up to the shipped accidental-match confusion adjustment plan (6 phases, all on `main` as of `f602dc1a43`).

## Goal

Tighten the test suite and E2E naming left by the accidental-match confusion adjustment plan. No behavior changes — tests-only and E2E naming only.

## Findings

- No bugs found in the production code.
- No dead production code. `markAsAccidentalMatch` and `partialFail` were correctly removed; all new code is used.
- Two redundant tests and one misleading test name.
- One test file close to the 250-line limit.
- One E2E naming inconsistency.

## Phase index

| # | Type | Status | One outcome |
|---|---|---|---|
| 1 | Structure | done | Consolidate the two "understanding fallback when spelling inactive" tests |
| 2 | Structure | in-progress | Rename the prompted-tracker floor test to reflect `recallFailed` |
| 3 | Structure | planned | Split the confusion-adjustment test class along cohesive seams |
| 4 | Structure | planned | Align E2E understanding-tracker naming |

## Phase 1 — Consolidate understanding-fallback inactive-spelling tests

- **Type:** Structure
- **Status:** done
- **Structure change:** Merged `shouldFallBackToUnderstandingWhenSpellingIsRemovedFromRecall` and
  `shouldFallBackToUnderstandingWhenSpellingIsDeleted` into one parameterized test
  covering both ways a spelling tracker becomes inactive (`removedFromTracking = true`
  vs `deletedAt` set). Both exercise the same `isActive()` code path and assert the
  same delta (understanding tracker linked). Keep one canonical assertion shape;
  parameterize only the inactivation method.

### Verification

- `CURSOR_DEV=true nix develop -c pnpm backend:test_only`

**Stop-safe value:** Fewer redundant tests; same coverage.

## Phase 2 — Rename the prompted-tracker floor test

- **Type:** Structure
- **Status:** in-progress
- **Structure change:** Rename
  `RecallPromptAccidentalMatchEdgeTests.shouldNotDropForgettingCurveIndexBelowFloorOnAccidentalMatch`
  to clarify it tests `MemoryTracker.recallFailed`'s floor through the accidental-match
  boundary, not an accidental-match-specific behavior. Suggested name:
  `shouldNotDropPromptedTrackerBelowStrengthFloorOnAccidentalMatch` or
  `shouldFloorPromptedTrackerStrengthOnAccidentalMatchFailure`.

### Verification

- `CURSOR_DEV=true nix develop -c pnpm backend:test_only`

**Stop-safe value:** Test name matches what it actually tests.

## Phase 3 — Split the confusion-adjustment test class

- **Type:** Structure
- **Status:** planned
- **Structure change:** `RecallPromptAccidentalMatchConfusionAdjustmentTests` is 225 lines.
  Split along cohesive seams into nested `@Nested` classes (or separate files if
  natural) — e.g. `UniqueMatch`, `Eligibility`, `Ambiguous`. Keep the same
  stable boundary (controller) and fixtures. Do not change assertions.

### Verification

- `CURSOR_DEV=true nix develop -c pnpm backend:test_only`

**Stop-safe value:** Test class stays under 250 lines per file; easier to navigate.

## Phase 4 — Align E2E understanding-tracker naming

- **Type:** Structure
- **Status:** planned
- **Structure change:** In `e2e_test/step_definitions/recall_memory_tracker.ts`,
  `openNoteLevelTracker` maps `'understanding'` → `'normal'` to match the
  assimilation page's row label. Either:
  - rename the page object's `openNoteLevelMemoryTracker` parameter from
    `'normal' | 'spelling'` to `'understanding' | 'spelling'` and update the
    row label / selector accordingly, or
  - keep the page object as-is but document the mapping in the step definition.
  Prefer the first option if the row label can be aligned; otherwise document.

### Verification

- `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/accidental_match_scheduling.feature`

**Stop-safe value:** E2E naming matches the domain language; less confusion for future readers.

## Cross-phase constraints

- Tests-only and E2E naming. No production behavior changes.
- Keep the controller as the stable boundary.
- Run `pnpm backend:test_only` after each phase; run the targeted Cypress spec
  only for Phase 4.
- Do not end a phase with CI-breaking failures.
