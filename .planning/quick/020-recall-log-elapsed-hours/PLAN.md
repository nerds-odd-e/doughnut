# Plan: RecallLog elapsed hours always present

**Status:** in-progress

**Goal:** Every RecallLog has whole-hour elapsed (`delta_t`). Historical `NULL`s are reconstructed. The column is required. Schedule fields stay unchanged.

## Locked

- Fill **`NULL` elapsed only**. Do not rewrite already-persisted numbers.
- Reconstruct a `NULL` as whole hours since the previous **mapped** log’s `recorded_at` on that tracker (`GOOD` / `EASY` / `HARD` / `AGAIN`). **`CONFUSION` is not an anchor.** No prior mapped log → **0**. Order by `recorded_at`, then `id`. Negative diff → **0**.
- Same whole-hour truncation as live `TimestampOperations.getDiffInHours`.
- Do **not** change Stability, Difficulty, `lastRecalledAt`, or `nextRecallAt`.
- Do **not** store FSRS G, R, `I`, or pre/post S/D. Do **not** replay memory state. **E4** stays deferred.
- ADR 0003 stays **Proposed**. Do not accept it.
- Live writers already set elapsed. First mapped grade on New stays **0**.
- Next Flyway versions: ungated `V300000281` (backfill, shipped), then `V300000282` (`NOT NULL`). Do not edit `V300000265` / `V300000266`. `V300000280` is OS-invalid display names.
- Affirmative current state. No “used to be nullable on backfill” prose in product code, tests, or permanent docs after wrap-up.

## Out of this plan

- Accepting ADR 0003.
- Fitting / per-user weights (E4).
- Replaying S/D/due from history.
- Interval fuzz, lapses, card states, short-term window, thinking time.

## Slices

### 1. Lock required elapsed in ADR 0003

- **Type:** Structure
- **Status:** done

RecallLog Decision in Proposed ADR 0003: `elapsed_hours` always present. First mapped grade **0**; later vs previous mapped grade; `CONFUSION` vs last mapped grade else **0** (not an anchor). Historical `NULL`s will be filled; non-null stays; column becomes `NOT NULL`. No S/D/due rewrite. Working draft empty. FSRS gap / SEED-004 point remaining elapsed work at this plan. Remaining deferred **E4** + accept ADR 0003.

### 2. NULL elapsed is reconstructed

- **Type:** Behavior
- **Status:** done

Ungated `V300000281` (`RecallLogElapsedHoursBackfill` + Flyway wrapper). NULL elapsed filled: first mapped **0**; later vs previous mapped `recorded_at`; `CONFUSION` vs last mapped else **0** (not an anchor). Non-null elapsed, S, D, and due unchanged. `ProductOutcome.isMappedGrade()` is the mapped-grade definition.

### 3. elapsed_hours is NOT NULL

- **Type:** Structure
- **Status:** done

Ungated `V300000282`: `elapsed_hours` `NOT NULL`. Entity / OpenAPI required. `RecallLogBuilder` defaults **0**. `RecallHistory` always shows elapsed. Backfill helper kept; reconstruction pinned in-memory (no NULL inserts). ADR RecallLog Decision present tense; still Proposed.

### 4. Trackers match required elapsed

- **Type:** Structure
- **Status:** planned

`STATE.md` (include `V300000281` / `V300000282`) plus FSRS gap / SEED-004: elapsed is closed; remaining **E4** + accept ADR 0003. Drop “nullable on backfill”. Plan-dir pointers removed from FSRS/SEED (point at ADR 0003). Delete this PLAN directory in wrap-up.

## Discoveries

- `V300000265` / `V300000266` inserted historical logs with `elapsed_hours` NULL. Live `persistRecallLog` already sets elapsed from `elapsedHoursUntil`.
- Prompt history hides elapsed when null; mounted spec pins 24h; E2E only checks the label exists on live grades.
- Reconstruction contract lives only in ADR 0003 RecallLog (mapped **grade**, truncation via **Whole-hour elapsed-time precision**). FSRS/SEED only point here.
- `V300000280` was taken by OS-invalid display names; elapsed backfill is `V300000281`, NOT NULL is `V300000282`. Mapped grade is `ProductOutcome.isMappedGrade()`. After NOT NULL, reconstruction is tested in-memory; the builder defaults elapsed **0**.
