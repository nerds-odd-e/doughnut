# RecallLog DSR rebuild leftover cohesion

**Status:** in progress  
**Goal:** Close the one test hole that can hide a wrong confusion due, and drop redundant skip / dead-branch leftovers in `RecallLogDsrBackfill`. Do not invent a second live fold.

## Inspection (028)

No production fold bug found. Live `MemoryTracker.applyRecall` / `adjustForConfusion` and `DsrFold` match on mapped grades (stored elapsed, from New, cap S, 24h fallback vs the grade instant) and on confusion (midpoint S, D and last unchanged, due never later). Past due is actually pinned: first-Good timestamps are historical, so a now-clamp would fail that test.

**Not bugs (out of this plan):**

- `DsrFold` vs live `MemoryTracker` is a second representation **on purpose** (JDBC Flyway, stored `elapsed_hours`, no clamp to now). Do **not** extract a shared fold. [029](../029-remove-spent-java-backfills/PLAN.md) slice 11 **deletes** `V300000283` / `RecallLogDsrBackfill` / its test after production has applied it.
- ADR 0003 still names `OverCapStabilityBackfill` / `StillNewMappedFirstRatingBackfill` as the Flyway pattern. 029 slices 4 and 11 retarget that copy.
- Hard/Easy first-rating through the loop is `Fsrs.after*Recall` → first-rating; Good is the canonical pin.
- `StillNewMappedFirstRatingBackfillTest` Good-only overlaps first-Good numbers on a **different** runner; 029 slice 6 deletes that class.

## Meaningful leftovers

1. **Confusion due is weakly pinned.** `rebuildsLeftoverThroughConfusionAsNonGrade` asserts `nextRecallAt ≤` Good due. If the fold updated S/D/last but **left due at the Good due**, the test still passes. ADR post-condition is last mapped Good + `I(0.9, midpoint S)` when that is not later.
2. **`leavesNewWithNoMappedGradeUnchanged` restates the skip filter.** Canonical skip is leftover `S > 0` with no mapped grade. New skip uses the same `EXISTS` mapped-grade predicate and an unasserted first-Good sibling fixture.
3. **Dead `CONFUSION` arm** in `afterMappedGrade` (caller already branches). `ProductOutcome.isMappedGrade()` is the mapped-grade test.

## Locked decisions

- Do **not** share `DsrFold` with `MemoryTracker`.
- Do **not** delete `V300000283` here (029-11).
- Do **not** edit ADR class citations here (029-4 / 029-11).
- Do **not** run in parallel with 029 on `RecallLogDsrBackfill*` or ADR 0003.
- If 029-11 has already landed, abandon this plan.

## Slices

### 1. Pin confusion due as last + I(midpoint)

- **Type:** Behavior
- **Status:** done
- **Done:** Confusion due pin is exact: Good `recorded_at` + `I(capped confusion S)`. Production unchanged (`applyConfusion` already projected that due).
- **Learning:** Weak `≤ Good due` was the only test hole; no production fix needed.

### 2. Drop redundant New skip and dead confusion arm

- **Type:** Structure
- **Status:** planned
- **Cleanup only:** Delete `leavesNewWithNoMappedGradeUnchanged` (and its unused first-Good sibling). Leftover unmapped skip, confusion-only skip, removed include, and deleted skip stay. Drop the `CONFUSION` throw in `afterMappedGrade` (exhaustive mapped-grade switch, or `isMappedGrade()`). Remaining `RecallLogDsrBackfillTest` still passes.

## Jidoka

- Value fork: skip this whole plan and execute 029 if you do not want to polish a runner 029-11 will delete. The redundant tests die there anyway; slice 1 is the only pin that can still catch a production Flyway miss **before** that delete.
- Stop after either slice; each is stop-safe.
