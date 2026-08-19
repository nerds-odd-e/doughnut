# Plan: FSRS-6 short-term under one day

**Status:** in-progress

**Goal:** Success grades with elapsed whole hours **0–23** use published FSRS-6 short-term next Stability. Elapsed **≥ 24** stays long-term. Live domain is DSR (`ForgettingCurve` gone). Ladder conversion stays only for Flyway replay of `V300000260`.

## Locked (accepted 2026-08-19)

- Switch: elapsed hours **< 24** → short-term Hard/Good/Easy; **≥ 24** → long-term. No calendar-day rule. Whole-hour elapsed stays the time input.
- Again is post-lapse at every elapsed, including 0 and 5. Confusion unchanged. New first-rating ignores elapsed.
- Existing S/D/due: **going forward only**. Do not rewrite stored Stability. No new Flyway for this rule.
- ADR 0003 stays **Proposed**. Do not accept it. Do not start **E4** fitting.
- Short-term formula and SInc ≥ 1 clamp unchanged (Good 24→**25**, Easy 24→**43**, Hard 24 stays **24**, Good at 72 stays **72**).
- Observable pin: New → Again (`S0(1)` = **5h**) → Good at 5h → short-term next-S **6h** (not long-term **21h**).

## Out of this plan

- **Delete `V300000260`.** Cannot. `V100000000__baseline.sql` still has `user.space_intervals`; `V300000259` still names the index-scale column. Fresh DBs (CI, new installs) must replay 260 or they never convert to hours. Production having applied it does not make a mid-chain delete safe. A full squash (baseline replace + tip placeholder, `.cursor/rules/db-migration.mdc`) is separate rare maintenance — not this plan.
- Replay memory state from RecallLog; backfill historical S.
- Per-user / fitted weights (E4).
- Showing Retrievability on the Memory Tracker.
- `.planning/quick/021-elapsed-hours-backfill-test-cohesion/` (orthogonal).

## Discoveries

- `correctRecallAfterNewAgainUsesLongTermGoodStability` already pins the fork (**21h**). That test becomes the canonical unit pin (**6h**) and must be renamed.
- `earlyCorrectGrowsLessThanOnTime` uses elapsed **1** on S=72. After the switch that is short-term (clamp, stays **72**), not “early long-term extra.” Move that long-term early fixture to elapsed **≥ 24**.
- `hoursFromLegacyIndex` / `SpacedRepetitionAlgorithm` / `StabilityIndexToHoursBackfill` exist only so `V300000260` can replay. Live scheduling does not call them.

## Slices

### 1. Lock the short-term window in ADR 0003

- **Type:** Structure
- **Status:** done

Proposed ADR 0003 Decision now uses elapsed **< 24** / **≥ 24**. Pin: Again → Good at 5h → **6h**. Rejected: elapsed==0 only; calendar same-day; `enable_short_term` off; rebuild past S. GAP/SEED-004 point at the Decision.

**Learning:** GAP “Current code vs FSRS-6” must not claim live code already uses **< 24**; that lands in slice 2. Clamp examples in the Decision are Stability hours (Good **24h**→**25h**), not elapsed.

### 2. On-time Good after first Again uses short-term Stability 6

- **Type:** Behavior
- **Status:** done

**Pre:** New tracker, just-review No (S=**5**), then 5 whole hours later. **Trigger:** just-review Yes. **Post:** Stability **6**, due 6h after that grade.

Live switch: `elapsedInHours < HOURS_PER_DAY` in `Fsrs.hoursAfterShortTermOrStabilityIncrease`. Unit pin renamed to `correctRecallAfterNewAgainUsesShortTermGoodStability` (6h). Early long-term fixture is `earlyLongTermCorrectGrowsLessThanOnTime` at elapsed **24**. E2E sibling in `spaced_repetition.feature` (recall starts hour 8; `It's day 1, 13 hour` is 5 elapsed).

**Learning:** GAP “Current code vs FSRS-6” now matches live `< 24` / `≥ 24`. Slice 3 still owns the 23 vs 24 clamp/growth pins.

### 3. Good at elapsed 23 is still short-term; 24 is long-term

- **Type:** Behavior
- **Status:** done

**Pre:** graded tracker with Stability **72**. **Trigger:** ordinary Good. **Post:** elapsed **23** → Stability stays **72** (short-term clamp); elapsed **24** → Stability **> 72** (long-term).

Pins sit beside the elapsed-0 S=72 clamp in `SpacedRepetitionSameHourRecallSchedulingTest`. Early-vs-on-time keeps only the magnitude delta. Shared `nextStabilityHours` lives on the scheduling test base.

### 4. Live memory updates are not a ForgettingCurve

- **Type:** Structure
- **Status:** planned

Delete `ForgettingCurve`. `MemoryTracker` + `Fsrs` (and existing `Fsrs*Recall`) own New vs graded, first-rating, next D/S, confusion midpoint, and constants (`S = 0`, 24h fallback, D fallback **5**). Flyway Java that currently constructs `ForgettingCurve` for first-rating numbers calls `Fsrs` instead. No glossary term added — ADR 0001 already has **New**, **Stability**, **Retrievability**. Existing tests still pass; no schedule change.

### 5. Legacy index conversion lives only next to V300000260

- **Type:** Structure
- **Status:** planned

Inline `hoursFromLegacyIndex` / day-list parse into the Flyway helper (`StabilityIndexToHoursBackfill` or package-private next to `V300000260`). Delete `SpacedRepetitionAlgorithm` and `SpacedRepetitionAlgorithmTest` from live `algorithms`. Keep the `V300000260` class. Rename leftover test names that still say `SpacedRepetitionAlgorithm` only if they import the deleted type. No schema change.

## Test notes

- Main user path: E2E on Memory Tracker Stability after Again then Yes at 5h.
- Threshold and early-long-term: existing scheduling unit tests (small test, `MemoryTracker` grade API).
- Do not add a test class per `Fsrs*` helper.
- Product artifacts stay capability-named (`spaced_repetition.feature`, scheduling tests). No GSD numbers in those names.
