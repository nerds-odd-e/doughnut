# Plan: FSRS short-term leftover cohesion

**Status:** done

**Goal:** After [PR #1580](https://github.com/nerds-odd-e/doughnut/pull/1580) (executed `.planning/quick/022-fsrs-short-term-under-one-day/`), the live domain has no SM-2 ladder in `services`, leftover `SpacedRepetitionAlgorithm` names are gone, **New** lives on the memory tracker, and the failure→success elapsed pin no longer straddles the short-term window.

Inspected commits: `d6cf6e9943` … `04dcf7112e` (merged as #1580).

## Findings (inspect only — do not treat as extra product work)

**No production schedule bug.** Elapsed **&lt; 24** / **≥ 24** matches Proposed ADR 0003. E2E Again at day 1 hour **8** then Yes at hour **13** is 5h; unit pin **6h** matches. Again stays post-lapse. Stored S is not rewritten.

**Keep (not redundant across the same surface):**

- E2E `On-time Good after first Again uses short-term Stability 6` and unit `correctRecallAfterNewAgainUsesShortTermGoodStability` — Cypress Memory Tracker vs in-memory `MemoryTracker` grade API.
- Same-hour Good on S=**72** stays 72 (SInc clamp) vs elapsed **23** stays 72 (window) vs elapsed **24** grows (long-term). Different unique claims.

**Do not reopen:** delete `V300000260`, rewrite historical S, E4 fitting, accept ADR 0003.

## Locked

- No schedule / formula change. Existing non-`@wip` tests stay green (adjust fixtures/names only).
- `V300000260` stays in the Flyway chain. Conversion logic may move **next to that class**, not disappear.
- ADR 0003 stays **Proposed**.

## Slices

### 1. Failure-then-Good elapsed pin does not straddle the 24h switch

- **Type:** Structure
- **Status:** done

`correctRecallAfterFailureUsesElapsedHoursSinceFailure` grades Good at **+24h vs +48h** after the same Again (both long-term). Assertion unchanged: later elapsed → strictly larger next S.

**Learning:** 12 vs 24 was short-term vs long-term after #1580; 23 vs 24 pins remain a different claim (window switch) and stay with slice 3’s rename, not this fixture.

### 2. Legacy index conversion is not a live service

- **Type:** Structure
- **Status:** done

`StabilityIndexToHoursBackfill` and `hoursFromLegacyIndex` live in `db.migration` next to `V300000260` (package-private). Breadcrumbs in SEED-004, FSRS-COMPATIBILITY-GAP, and STATE point there. No schema change.

**Learning:** Other `*Backfill` types in `entities`/`services` are different concepts; leave them. Slice 3 still owns `SpacedRepetition*` rename / `MemoryTracker.isNew()`.

### 3. Live names are recall scheduling and New, not SpacedRepetitionAlgorithm

- **Type:** Structure
- **Status:** done

Leftover `SpacedRepetition*` recall-scheduling tests are `MemoryTracker*RecallScheduling*` under `entities/`. `RecallServiceDueWorkTest` is the due-work name. Public **New** is `MemoryTracker.isNew()` (ADR 0001); `Fsrs.isNew(float)` is package-private. Builder uses `entity.isNew()`.

**Learning:** Remaining `algorithms/` tests are wiki/frontmatter/cloze/silent-window, not scheduling. `Fsrs.isNew` stays package-private (clamped S) rather than inlined.

## Out of this plan

- Inlining `FsrsGoodRecall` / eager long-term increment on the short-term path (micro; not user-visible).
- Clamping negative elapsed in `elapsedHoursUntil` (no live path; RecallLog already stores **0**).
- Dropping the E2E or the in-memory **6h** pin.
- Showing Retrievability; fitting; accepting ADR 0003.
