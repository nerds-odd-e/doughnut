# Rebuild Memory Tracker DSR from RecallLog

**Status:** in progress  
**Goal:** One-time fold of each tracker that has a mapped-grade RecallLog under locked FSRS-6, so persisted Stability, Difficulty, `lastRecalledAt`, and `nextRecallAt` match that log. ADR 0003 drops “history is incomplete / do not replay / going forward only.” E4 stays deferred.

## Locked decisions

- Fold **every** tracker with ≥1 mapped grade (`GOOD` / `EASY` / `HARD` / `AGAIN`), from New, in `recorded_at`, `id` order. Use **stored** `elapsed_hours`. First mapped grade is first-rating.
- **Leave** New, confusion-only, and `S > 0` with no mapped-grade log. Include **removed-from-tracking**. Skip `deleted_at IS NOT NULL`.
- Write Stability, Difficulty, `lastRecalledAt` (last mapped `recorded_at`), `nextRecallAt` = last + `I(0.9, S)`. 24h fallback only when `I` is non-positive vs that grade instant. **Past due is allowed** (no clamp, no fuzz).
- Confusion is today’s non-grade (midpoint S; D and last recall unchanged; due never later).
- **One-time ungated Java Flyway** (same pattern as `OverCapStabilityBackfill` / `StillNewMappedFirstRatingBackfill`). Live grading still updates the snapshot. Do **not** fold on every due-work query.
- ADR stays **Proposed**. Do not accept it. Do not start E4.

## Out of scope

- Per-user fitting (E4)
- Query-time rebuild of due-work
- Flyway squash of `V300000260`
- Showing Retrievability
- New E2E: this is a historical-row backfill; pin with SpringBootTest + real DB like other DSR backfills. Existing recall E2E still covers the live path.

## Slices

### 1. Lock RecallLog DSR rebuild in ADR 0003

- **Type:** Structure
- **Status:** done
- **Done:** Proposed ADR 0003 Decision **DSR snapshot** is the one map (cache-of-fold, one-time ungated Flyway, skip rules, past due OK, no query-time fold). Blanket “do not replay / going forward only / history is incomplete” removed. E4 stays deferred. Context no longer says the ADR does not select a formula.
- **Learning:** Keep snapshot policy in that one Decision section; other ADR sections point at it. Slice 6 still owns SEED-004 / gap tracker / STATE leftover copy. ADR is long (one Decision); do not split it in later slices.

### 2. Rebuild a first mapped Good from RecallLog

- **Type:** Behavior
- **Status:** done
- **Done:** Ungated `V300000283` / `RecallLogDsrBackfill` rebuilds leftover first mapped Good to first-rating Good (**55h**, **D0(3)**, last = log `recorded_at`, due = last + 55h). New with no mapped grade is skipped.
- **Learning:** Runner already folds all mapped grades via `Fsrs.after*Recall` (confusion as non-grade). Slice 3 only needs the Again pin on that loop; do not special-case Good.

### 3. Rebuild through a later mapped grade

- **Type:** Behavior
- **Status:** planned
- **Pre:** First Good, then AGAIN at elapsed **≥ 24**, snapshot still leftover.
- **Trigger:** Same backfill.
- **Post:** S/D/due match `Fsrs.afterAgainRecall` on that first-Good state (post-lapse + cap + Again next-D). `lastRecalledAt` is the Again row.

### 4. Replay confusion as a non-grade

- **Type:** Behavior
- **Status:** planned
- **Pre:** First Good, then CONFUSION, leftover snapshot.
- **Trigger:** Same backfill.
- **Post:** S is the confusion midpoint; Difficulty and `lastRecalledAt` stay the Good grade; due is not later than the Good due.

### 5. Leave snapshots with no mapped-grade log

- **Type:** Behavior
- **Status:** planned
- **Pre:** `S > 0` with no mapped-grade log (and a confusion-only leftover if cheap). A **removed** tracker with a mapped Good. A **deleted** tracker with a mapped Good.
- **Trigger:** Same backfill.
- **Post:** No-mapped-grade snapshot unchanged. Removed Good is rebuilt (same numbers as slice 2). Deleted Good unchanged.

### 6. Drop leftover incomplete-history story

- **Type:** Structure
- **Status:** planned
- **Cleanup only:** SEED-004, `FSRS-COMPATIBILITY-GAP.md`, `STATE.md` — remaining deferred is **E4** plus human accept. ADR Consequences / Assumptions / Related match the Decision. No dual “do not replay” map. No product rename of `Fsrs*`.

## Jidoka

- Production due queue may spike (honest overdue). That is accepted.
- Stop after any slice; value is aligned rows for the scenarios already pinned.
- Do not gate the Flyway `1=0` (leftover placeholder risk).
