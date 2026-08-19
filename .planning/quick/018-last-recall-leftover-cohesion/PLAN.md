# Plan: Last-recall leftover cohesion

**Status:** in progress (slice 1 done)

**Goal:** After New last recall shipped, drop overlapping tests and close the mapped-grade SQL footgun. No product behavior change. Do not accept ADR 0003. Do not touch Flyway 271–278 files.

Inspected range: `6e687f95d6`..`475e52df7a` (New last recall + spelling-schedule capture fix).

## Locked for this plan

- ADR 0003 stays **Proposed**.
- Do not edit committed Flyway `V300000271`–`V300000278` (including gated 271/272 Java wrappers).
- Keep 271/272 placeholder keys at `1=0` so those migrations still replay.
- Keep HTTP assimilate (`assimilateLeavesLastRecallUnsetAndDueAtAssimilatedAt`) and first just-review elapsed **0** (`successfulMarkAsRecalledLeavesOneGoodRecallLog`) as the canonical New last-recall pins.
- Keep E2E `spaced_repetition.feature` assimilate **N/A** / remove-unchanged Last Recall Time.
- Do not extract a shared JDBC backfill test harness (speculative; no next backfill slice).
- Do not inline `MemoryTrackerRecallDue` (would push `MemoryTracker` over 250 lines).

## Out of this plan

- **E4** fitting / accept ADR 0003.
- Easy-only / HARD-only still-New first-rating sibling pins (no evidence those rows exist; Good-only and Shrink-only already cover those branches).
- Frontend Vitest Last Recall Time **N/A** (E2E already shows it).
- Dropping HTTP `remove`/`reEnable` last-recall pins (different surface from E2E).
- `NotePropertyTrackingBackfill` last-recall-on-insert (already-applied historical insert; not this change).
- TS fixture `lastRecalledAt: ''` (falsy still renders **N/A**).

## Discoveries

- No live scheduling bug in the shipped change. Overlap try-again E2E failed because `captureSpellingTrackerSchedule` still required Last Recall Time; that harness is already fixed on main (`475e52df7a`).
- `StillNewFirstRatingBackfillTest` **1=1** cases re-pin Again/Hard first-rating with due from `last_recalled_at` (the old lie). Live repair is ungated `V300000277`. Operators will not enable 271/272. `db-migration.mdc`: drop the enabled-selection harness when the gated migration will not run. Keep **1=0** no-op and invalid-gate fail-loud.
- `MemoryTrackerRecallDuePersistenceTest` elapsed **0** / due = `assimilatedAt` duplicate assimilate HTTP + first-grade elapsed **0**. Unique remainder is DATETIME / nullability of the due columns.
- `UngradedNewLastRecallBackfillTest` parameterizes six mapped outcomes for the same skip. After `ProductOutcome.mappedGradeSqlInList()`, one mapped log proves the `NOT EXISTS`.
- `mappedGradeSqlInList()` is “every `ProductOutcome` except CONFUSION”. A new non-grade outcome would be treated as mapped. Named mapped grades match ADR 0003.
- `removeFromRecall()` already calls `expectSkipped()`; the Gherkin Then does it again. The When should only remove.

## Slices

### 1. Drop gated 271/272 enabled first-rating tests

- **Type:** Structure
- **Status:** done

Kept `1=0` no-op (Again and Hard) and invalid-gate fail-loud. Deleted **1=1** apply/skip cases. After that, inlined unused `stillNew` and dropped leftover `.lastRecalledAt` on the remaining fixture. `StillNewMappedFirstRatingBackfillTest` is still the live first-rating pin.

### 2. Drop entity last-recall due pins covered at HTTP

- **Type:** Structure
- **Status:** planned

No user-facing change. Remove `elapsedHoursUntilIsZeroWhenLastRecallIsUnset` and `calculateNextRecallAtIsAssimilatedAtWhenLastRecallIsUnset` from `MemoryTrackerRecallDuePersistenceTest`. Keep `recallAtColumnIsDatetime` and the max-interval persist pin.

### 3. Mapped grades are an explicit list

- **Type:** Structure
- **Status:** planned

No user-facing change. `ProductOutcome.mappedGradeSqlInList()` lists `GOOD`, `EASY`, `HARD`, `SHRINK`, `AGAIN`, `AGAIN_ZERO` (not “not CONFUSION”). Collapse `UngradedNewLastRecallBackfillTest.leavesStillNewWithMappedLogsUnchanged` to one mapped outcome (AGAIN). Confusion-only skip stays (proves CONFUSION is excluded).

### 4. Removing from recall does not assert skipped in the When

- **Type:** Structure
- **Status:** planned

No user-facing change. `removeFromRecall()` only clicks remove and waits until not busy. Gherkin `Then the memory tracker should be skipped` remains the skip assertion. `spaced_repetition.feature` remove scenario still passes.
