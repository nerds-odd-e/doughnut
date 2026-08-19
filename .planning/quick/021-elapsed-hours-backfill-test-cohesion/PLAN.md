# Plan: Elapsed-hours backfill test cohesion

**Status:** in-progress

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

No reconstruction logic bug vs ADR 0003 RecallLog. Live first-grade **0** stays on `successfulMarkAsRecalledLeavesOneGoodRecallLog`. Mixed non-null + `NULL`, negative diff → **0**, and tracker isolation are still untested on `reconstructedNullElapsedById` (slice 2).

## Slices

### 1. Backfill tests do not boot Spring

- **Type:** Structure
- **Status:** done

Dropped the two JDBC/`MakeMe` cases and Spring harness. Four reconstruction cases remain as a plain JUnit class (`onDay` timestamps). Production `run` unchanged.

Unlocks: slice 2 sibling pins on `reconstructedNullElapsedById`.

### 2. Reconstruction siblings match the ADR deltas

- **Type:** Structure
- **Status:** planned

On `reconstructedNullElapsedById` only (siblings assert their delta; canonical first mapped `NULL` → **0** already exists):

- Mixed: earlier mapped elapsed already set is omitted from the update map; later `NULL` still uses that mapped `recorded_at`.
- Negative `recorded_at` vs last mapped → **0**.
- Two trackers: last mapped on tracker A does not fill tracker B.

## Discoveries

- `run()` cannot insert `NULL` after `V300000282`; Flyway remains the production caller. Alias-grade backfill can still plant rewrite rows; elapsed cannot.
- Reconstruction tests no longer boot Spring. Slice 2 adds mixed / negative / tracker-isolation siblings on the same `reconstructedNullElapsedById` surface.
