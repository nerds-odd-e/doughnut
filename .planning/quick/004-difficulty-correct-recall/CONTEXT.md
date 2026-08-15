# Difficulty on correct recall — context

Closes FSRS gap **B1** (persist Difficulty when a behavior consumes it) with **D3** (assimilation is New; first grade initializes D). Partial **B2**: success-path interval source is SInc, not the Fibonacci ladder. Proposed [ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy.md) stays Proposed.

Tracker: [FSRS-COMPATIBILITY-GAP.md](../../research/FSRS-COMPATIBILITY-GAP.md). Seed: [SEED-004](../../seeds/SEED-004-close-spaced-repetition-scheduling-policy-gap.md).

## Locked for this plan

- Own FSRS-6 implementation, frozen default `w`, no library. Math in **days**, persist whole **hours**.
- Ordinary **correct** ≡ Good (G=3) for SInc and D-update.
- `nextRecallAt = lastRecalledAt + stability` (`r = 0.9`). No retention Settings.
- **D = 5** backfill on graded trackers; hidden (`@JsonIgnore`). Unset D on a graded tracker is treated as 5.
- **First correct: D = 5, S = 24h** this plan. **12h is parked** (E2E day-at-08:00 schedule in `spaced_repetition.feature` is a 24h first rung; you will revisit 12h).
- Drop success-path ladder + linear elapsed/S increment. Thinking time stays a bounded tweak inside correct on the FSRS result.
- Fail / confusion / commissioned stay on the ladder.

## Why first S stays 24h

Not many E2E files pin hours, but these **will fail** if first success is 12h (and they will fail from SInc even at 24h, which slice 6 repairs):

- `e2e_test/features/recall/spaced_repetition.feature` — “Strictly follow the schedule” / “recall more”: day N at hour 8, Fibonacci 1-day first two rungs.
- `RecallServiceWithSpacedRepetitionAlgorithmTest` day-grid is 24h steps (unit, rewritten with SInc).

Incorrect-recall E2E still pins **12h retry**; do not conflate that with first success.

## ADR 0003 Decision text (slice 1)

Paste under **Decision**. Do not Accept the ADR.

### Difficulty on correct recall

Difficulty is persisted memory state in `[1, 10]`. It is not part of the learner UI in this Decision. Harder items gain less Stability on a successful recall. A correct recall also updates Difficulty with the open-FSRS Good-equivalent rule.

A newly assimilated tracker is **New**: Stability 0, Difficulty unset, due now. Assimilation is not a grade. The first real correct recall initializes Difficulty to **5** and Stability to **24** hours (short first interval; 12 hours is a later tweak). Existing trackers that already have positive Stability or a recall count are migrated to Difficulty **5**.

Ordinary correct recall with Stability > 0 updates Stability (and Difficulty) with open-FSRS-6 Good-equivalent rules (own implementation). It must not walk a spacing-index ladder. Locked overdue extra growth still holds. Requested retention remains implicit: `nextRecallAt = lastRecalledAt + stability`.

Incorrect recall, confusion adjustment, and commissioned scores stay on their current rules.

## Tests and verify

Drive `MemoryTracker.recalledSuccessfully` / `markAsRecalled`. Rewrite Fibonacci-pinned unit hours. Targeted E2E: `e2e_test/features/recall/spaced_repetition.feature` (tag `@wip` in slice 2 if red; un-wip in slice 6).

`CURSOR_DEV=true nix develop -c pnpm backend:test_only` each slice. After the migration slice, regenerate `docs/database-erd.md`.
