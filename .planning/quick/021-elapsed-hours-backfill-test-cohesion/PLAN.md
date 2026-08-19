# Plan: Elapsed-hours backfill test cohesion

**Status:** planned

**Goal:** Reconstruction tests pin the ADR contract on the reconstruction function without Spring or vacuous JDBC. Live persist pins stay on the tracking controller.

## Locked

- Do **not** change `V300000281` / `V300000282` (committed Flyway).
- Keep `RecallLogElapsedHoursBackfill.run` for production 281→282. Do not delete the helper.
- Live first-grade **0** / later **24** stay in `MemoryTrackerTrackingControllerTest`. Do not duplicate them.
- Reconstruction stays a domain-stable function (`reconstructedNullElapsedById`); do not widen it to public.
- ADR 0003 stays **Proposed**. Do not accept it.
- No new E2E.

## Out of this plan

- Changing `RecallLog.elapsedHours` from `Integer` to `int` (missing JSON would silently become 0).
- Streaming / paging the one-time full-table load.
- Frontend history spec for elapsed **0** (`v-if` was null, not zero; `Elapsed hours: 24` already pins the label).
- Accepting ADR 0003.

## Findings (inspect only)

No reconstruction logic bug vs ADR 0003 RecallLog. After `NOT NULL`, two JDBC tests in `RecallLogElapsedHoursBackfillTest` no longer fill `NULL`s: `persistedLogDefaultsElapsedHoursToZero` only pins the Java `RecallLogBuilder` default (live **0** is already on `successfulMarkAsRecalledLeavesOneGoodRecallLog`); `leavesPersistedElapsedAndScheduleUnchanged` is a no-op `UPDATE … WHERE elapsed_hours IS NULL`. The four reconstruction cases still boot `@SpringBootTest`. Mixed non-null + `NULL`, negative diff → **0**, and tracker isolation are untested on the function (the original “non-null left alone” sibling was the JDBC no-op).

## Slices

### 1. Backfill tests do not boot Spring

- **Type:** Structure
- **Status:** planned

Drop `persistedLogDefaultsElapsedHoursToZero` and `leavesPersistedElapsedAndScheduleUnchanged` (and the JDBC/`MakeMe` harness they need). Keep the four reconstruction cases as a plain unit test (no `@SpringBootTest`). Existing tests still pass; no observable product change.

Unlocks: slice 2 can add ADR sibling pins on that cheap surface.

### 2. Reconstruction siblings match the ADR deltas

- **Type:** Structure
- **Status:** planned

On `reconstructedNullElapsedById` only (siblings assert their delta; canonical first mapped `NULL` → **0** already exists):

- Mixed: earlier mapped elapsed already set is omitted from the update map; later `NULL` still uses that mapped `recorded_at`.
- Negative `recorded_at` vs last mapped → **0**.
- Two trackers: last mapped on tracker A does not fill tracker B.

## Discoveries

- `run()` cannot insert `NULL` after `V300000282`; Flyway remains the production caller. Alias-grade backfill can still plant rewrite rows; elapsed cannot.
- `RecallLogElapsedHoursBackfillTest` is `@SpringBootTest` because slice 2 originally drove `run()` with `makeMe` `NULL`s.
