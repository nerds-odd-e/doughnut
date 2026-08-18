# Plan: Cap Stability at the open-FSRS maximum interval

**Status:** in progress

**Goal:** Persisted Stability (and therefore due, because `I(0.9, S) = S`) never exceeds open FSRS `S_MAX` / `default_maximum_interval` (**36500 days** = **876000 whole hours**). Existing over-cap rows from the old spacing ladder are clamped and their due rebuilt. ADR 0003 stays **Proposed**; this plan only writes the maximum-interval Decision fragment.

Humans already chose the locks below. This plan only sequences them. Do not accept ADR 0003 in this plan.

**Commit grain:** one git commit per slice. Do not bundle slices. Each unit should be ~5 minutes including its targeted tests (`planning.mdc`).

## Locked for this plan

- Cap value: **36500 days**, compared and persisted as **876000 whole hours** (open FSRS `S_MAX`).
- Cap **persisted Stability**. Due follows (`nextRecallAt = lastRecalledAt + I(0.9, S)`). Do not keep an unbounded S beside a capped due.
- Clamp **after** next-S is computed (FSRS update, thinking-time overlay on Good, Tutor **2** shrink, confusion midpoint), on every write of next Stability.
- Existing over-cap rows: clamp S and set `next_recall_at = last_recalled_at + I(S)`. Under-cap rows unchanged. Difficulty and RecallLog unchanged. `last_recalled_at` is `NOT NULL` — there is no null-last case.
- `last_recalled_at` and `next_recall_at` are **DATETIME** (UTC wall-clock; JDBC session TZ stays UTC) so `last + 876000 hours` can persist. Do not convert other `TIMESTAMP` columns here ([SEED-006](../../seeds/SEED-006-remove-mysql-timestamp-2038.md)).
- Ordinary **ungated** one-shot Flyway (not a `1=0` placeholder). Idempotent `LEAST`.
- Global constant, not a Settings knob, not shown as its own UI. Memory Tracker still shows Stability (migrated rows may drop).
- Fuzz stays **deferred**. Fitting / per-user weights (**E4**) stay deferred.
- Do not delete `DEFAULT_SPACES` / `hoursFromLegacyIndex` (still required so `V300000260` can replay).
- Do not enable or change gated still-New first-rating backfills (`V300000271` / `V300000272`).

## Out of this plan

Fuzz, E4 fitting, accepting ADR 0003, dropping the legacy spacing-index converter, thinking-time formula / `LEGACY_INDEX_STEP`, a shorter Doughnut-specific cap, Settings or copy for “max interval.” Remaining MySQL `TIMESTAMP` columns / 2038 ([SEED-006](../../seeds/SEED-006-remove-mysql-timestamp-2038.md)).

## Testing

Organic recall cannot reach `S_MAX` in a session. Do **not** add testability-only seeds or E2E whose only job is to display 876000. Pin the cap at the existing scheduling boundary (`SpacedRepetition*RecallSchedulingTest` → `MemoryTracker` grade APIs) and the backfill at a `StillNewFirstRatingBackfillTest`-style Spring test. Extend `spaced_repetition.feature` only if a later slice makes the cap reachable without a fixture hack.

## Slices

### 1. Lock maximum interval in ADR 0003

- **Type:** Structure
- **Status:** done

Decision fragment in Proposed ADR 0003 (next to requested retention): global **36500 days / 876000 hours**; clamp after next-S; due from that S; existing over-cap rows will be clamped. **E3** remaining is fuzz only. Status stays Proposed.

### 2. A grade from over-cap Stability lands at the cap

- **Type:** Behavior
- **Status:** done

Over-cap ordinary correct (and thinking-time sibling) persist Stability **876000**; due = last + 876000. One write seam: `MemoryTrackerNextStability.write` over `Fsrs.cappedStabilityHours` / `MAXIMUM_INTERVAL_HOURS` (applyRecall, shrink, confusion). Fixture `setStability` is not clamped (precondition can still be over-cap).

### 3. Persist recall due past the TIMESTAMP range

- **Type:** Structure
- **Status:** done

`V300000273` converts `last_recalled_at` / `next_recall_at` to `DATETIME NOT NULL`. Persisted flush of `last + 876000 hours` succeeds (`MemoryTrackerRecallDuePersistenceTest`). Java `Timestamp` mapping unchanged. `assimilated_at` still `TIMESTAMP`. ERD exporter lists only PK/UK/FK columns, so `docs/database-erd.md` did not change.

### 4. Existing over-cap rows are clamped without a grade

- **Type:** Behavior
- **Status:** done

Ungated `V300000274` / `OverCapStabilityBackfill`: over-cap rows get Stability **876000** and `next_recall_at = last_recalled_at + I(S_MAX)`; under-cap untouched; Difficulty unchanged. Cap from `Fsrs.MAXIMUM_INTERVAL_HOURS`; SQL `LEAST`.

### 5. Close maximum-interval in the FSRS tracker

- **Type:** Structure
- **Status:** planned

[FSRS-COMPATIBILITY-GAP.md](../../research/FSRS-COMPATIBILITY-GAP.md), [SEED-004](../../seeds/SEED-004-close-spaced-repetition-scheduling-policy-gap.md), and [STATE.md](../../STATE.md): maximum interval is **closed**; **E3** remaining is fuzz (or E3 dropped if the tracker lists fuzz as its own line); **E4** and accept ADR 0003 unchanged. No product code. Do not claim fuzz or fitting closed.

## Discoveries

- Slice 1: restating the 24h fallback next to the cap duplicated the existing strictly-future bullet; the Decision now points at that fallback instead.
- Slice 2: clamp lives in `Fsrs.cappedStabilityHours`; grade writes go through `MemoryTrackerNextStability`. `setStability` stays uncapped so tests can seed over-cap rows. Those tests are in-memory; MySQL `TIMESTAMP` cannot store `last + 876000 hours`.
- Jidoka: humans accepted DATETIME for `last_recalled_at` / `next_recall_at` only; remaining TIMESTAMP columns are [SEED-006](../../seeds/SEED-006-remove-mysql-timestamp-2038.md). Dropped the null-last clamp case (`NOT NULL`).
- Slice 3: ERD exporter does not list these columns (not PK/UK/FK); no `docs/database-erd.md` delta.
- Slice 4: backfill due hours use `Fsrs.intervalHours(MAXIMUM_INTERVAL_HOURS)` (cap is already S_MAX).
