# Plan: Cap Stability at the open-FSRS maximum interval

**Status:** in progress

**Goal:** Persisted Stability (and therefore due, because `I(0.9, S) = S`) never exceeds open FSRS `S_MAX` / `default_maximum_interval` (**36500 days** = **876000 whole hours**). Existing over-cap rows from the old spacing ladder are clamped and their due rebuilt. ADR 0003 stays **Proposed**; this plan only writes the maximum-interval Decision fragment.

Humans already chose the locks below. This plan only sequences them. Do not accept ADR 0003 in this plan.

**Commit grain:** one git commit per slice. Do not bundle slices. Each unit should be ~5 minutes including its targeted tests (`planning.mdc`).

## Locked for this plan

- Cap value: **36500 days**, compared and persisted as **876000 whole hours** (open FSRS `S_MAX`).
- Cap **persisted Stability**. Due follows (`nextRecallAt = lastRecalledAt + I(0.9, S)`). Do not keep an unbounded S beside a capped due.
- Clamp **after** next-S is computed (FSRS update, thinking-time overlay on Good, Tutor **2** shrink, confusion midpoint), on every write of next Stability.
- Existing over-cap rows: clamp S and set `next_recall_at = last_recalled_at + I(S)`. Under-cap rows unchanged. Difficulty and RecallLog unchanged. If `last_recalled_at` is null, cap S only.
- Ordinary **ungated** one-shot Flyway (not a `1=0` placeholder). Idempotent `LEAST`.
- Global constant, not a Settings knob, not shown as its own UI. Memory Tracker still shows Stability (migrated rows may drop).
- Fuzz stays **deferred**. Fitting / per-user weights (**E4**) stay deferred.
- Do not delete `DEFAULT_SPACES` / `hoursFromLegacyIndex` (still required so `V300000260` can replay).
- Do not enable or change gated still-New first-rating backfills (`V300000271` / `V300000272`).

## Out of this plan

Fuzz, E4 fitting, accepting ADR 0003, dropping the legacy spacing-index converter, thinking-time formula / `LEGACY_INDEX_STEP`, a shorter Doughnut-specific cap, Settings or copy for “max interval.”

## Testing

Organic recall cannot reach `S_MAX` in a session. Do **not** add testability-only seeds or E2E whose only job is to display 876000. Pin the cap at the existing scheduling boundary (`SpacedRepetition*RecallSchedulingTest` → `MemoryTracker` grade APIs) and the backfill at a `StillNewFirstRatingBackfillTest`-style Spring test. Extend `spaced_repetition.feature` only if a later slice makes the cap reachable without a fixture hack.

## Slices

### 1. Lock maximum interval in ADR 0003

- **Type:** Structure
- **Status:** done

Decision fragment in Proposed ADR 0003 (next to requested retention): global **36500 days / 876000 hours**; clamp after next-S; due from that S; existing over-cap rows will be clamped. **E3** remaining is fuzz only. Status stays Proposed.

### 2. A grade from over-cap Stability lands at the cap

- **Type:** Behavior
- **Status:** planned

**Pre:** a graded tracker with Stability **above** 876000 hours (legacy ladder scale), `lastRecalledAt` set, due = last + current S.

**Trigger:** ordinary correct (`recalledSuccessfully`, neutral thinking time).

**Post:** Stability **876000**; `nextRecallAt = lastRecalledAt + 876000 hours`. Difficulty follows existing Good next-D (unchanged by this slice’s unique claim — do not re-assert D).

Canonical test in `SpacedRepetitionCorrectRecallSchedulingTest`. Implementation: one clamp used by every next-S write (`applyRecall`, shrink, confusion) so other grades cannot leak an over-cap S. Thinking-time on Good must not pierce the cap (one extra assertion or sibling test; same post-condition). First-rating hours stay far below the cap (no new first-rating tests).

### 3. Existing over-cap rows are clamped without a grade

- **Type:** Behavior
- **Status:** planned

**Pre:** persisted `memory_tracker.stability > 876000` with `last_recalled_at` set (and a sibling under-cap row).

**Trigger:** ungated Flyway `V300000273` (next free after `V300000272`) running a Java backfill (same shape as hours conversion / still-New backfills: logic in an entity/service type, thin `db.migration` wrapper).

**Post:** over-cap row has Stability **876000** and `next_recall_at = last_recalled_at + 876000 hours`; under-cap row untouched; Difficulty unchanged. Null `last_recalled_at`: cap S only.

Test drives the backfill class (not HTTP). Do not gate with `1=0`.

### 4. Close maximum-interval in the FSRS tracker

- **Type:** Structure
- **Status:** planned

[FSRS-COMPATIBILITY-GAP.md](../../research/FSRS-COMPATIBILITY-GAP.md), [SEED-004](../../seeds/SEED-004-close-spaced-repetition-scheduling-policy-gap.md), and [STATE.md](../../STATE.md): maximum interval is **closed**; **E3** remaining is fuzz (or E3 dropped if the tracker lists fuzz as its own line); **E4** and accept ADR 0003 unchanged. No product code. Do not claim fuzz or fitting closed.

## Discoveries

- Slice 1: restating the 24h fallback next to the cap duplicated the existing strictly-future bullet; the Decision now points at that fallback instead.
