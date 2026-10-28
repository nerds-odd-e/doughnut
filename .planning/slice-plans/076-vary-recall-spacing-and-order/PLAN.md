# Vary recall spacing and the order of similarly due questions

**Source:** [Vary recall spacing and the order of similarly due questions](../../seeds/SEED-088-varied-recall-spacing-and-order.md#story-1)
**Identity:** SEED-088#story-1

## Goal and scope

A learner recalls with varied next intervals and varied order among similarly
due questions, without losing older-overdue priority or deterministic schedule
replay. The source story owns the complete scope, exclusions, and key examples.

PFE: change existing owners — `MemoryTrackerRecallDue` / Grade scheduling for
fuzzed due times (Stability stays an FSRS output), and the shared due query
behind `RecallsController.recalling` for band-then-random order. Reuse
`Randomizer` / testability seeding and the product’s existing durable-hash
pattern (CRC32-style) rather than a new scheduler subsystem, load balancer, or
client-owned shuffle as the product rule.

Follow [ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md):
whole-hour fuzzing, due strictly after the Grade, maximum interval, thinking
time not a scheduling input, deterministic reconstruction from RecallLog.
No new North Star topic.

## Exclusions

- Anki load balancer / easy-days selection among fuzz candidates
- Learner-configurable sort or fuzz
- Mass reschedule of existing `next_recall_at` at deploy
- A persisted fuzz column on RecallLog
- Separate fuzz policy for Confusion beyond today’s Stability/due adjustment

## Assumptions

- Fuzz applies on Grade scheduling writes; Confusion stays on its current path.
- Queue order is selected when building the shared due list; later fetches may
  differ.
- Forward-only: stored dues change on the next Grade.

## Settled decisive premises

Checkout: this preparation workspace.

| Premise | Consuming operation | Observation | Result |
| --- | --- | --- | --- |
| Grade scheduling sets due as last recalled + round(Stability) hours with no fuzz | Slice 1 changes `scheduleNextRecallFromStability` / due write | `CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test --tests 'com.odde.donut.entities.MemoryTrackerCorrectRecallSchedulingTest.nextRecallAtIsLastRecalledAtPlusStabilityHours' -Dspring.profiles.active=test` | PASS — current due equals Stability hours |
| Shared due list is served by `RecallsController.recalling` with eligibility and half-day window | Slice 2 changes due order for that response | Same command with `--tests 'com.odde.donut.controllers.RecallsControllerTests'` | PASS — due eligibility and window end behave as today; order among peers is not yet asserted (new discriminating proof required) |
| Exclusion rebuild replays valid events through the same scheduling writes | Slice 1 proof must cover rebuild | Read `MemoryTrackerRebuildFromRecallHistoryTest` + `MemoryTrackerReplay` calling `scheduleNextRecallFromStability` | Confirmed — rebuild uses the scheduling path; extend that suite for fuzzed dues |

## Outside-in proof

- Matching memory states / Grades with different durable event identities can
  get different bounded hour intervals; Stability unchanged by fuzz; short
  intervals (<60h) unfuzzed; clamps hold.
- Same valid history → same Stability, Difficulty, and due after replay /
  exclusion rebuild.
- Eligible due list: earlier 12h `next_recall_at` bands before later ones;
  within a band, SPELLING preference then varied peer order across fetches;
  future-due stay out.

## Ordered slices

Each leaf targets about five minutes of active change, focused checking, and
cleanup.

### 1. Schedule the next recall with deterministic hour fuzz
Type: Behavior
Status: done
Proof: Extend `MemoryTrackerCorrectRecallSchedulingTest` (and sibling scheduling
tests that assert due = Stability hours) plus `MemoryTrackerRebuildFromRecallHistoryTest`
so Grade scheduling and rebuild observe fuzzed hour dues, unfuzzed short
intervals, unchanged Stability, clamps, and identical reconstruction from the
same valid history. Prefer `CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test --tests 'com.odde.donut.entities.MemoryTrackerCorrectRecallSchedulingTest' --tests 'com.odde.donut.entities.MemoryTrackerRebuildFromRecallHistoryTest' -Dspring.profiles.active=test`.

Behavior: After a Grade on a tracker with Stability at or above 60 hours →
`next_recall_at` is last recalled plus a bounded whole-hour interval chosen by
Anki-scaled fuzz factors from durable RecallLog fields (tracker id +
`recordedAt`, and other logged Grade fields as needed), Stability and Difficulty
remain the FSRS outputs, due stays strictly after the Grade and within the
product maximum. Below 60 hours, due stays the unfuzzed Stability interval.
Replay and exclusion rebuild of the same valid events reproduce the same state
and due. Place fuzz beside existing FSRS/due helpers; do not add a RecallLog
fuzz column or mass-reschedule existing dues.

### 2. Order similarly due questions randomly in the shared queue
Type: Behavior
Status: planned
Proof: Extend `RecallsControllerTests` (stable HTTP/controller boundary) so two
eligible trackers in the same 12-hour `next_recall_at` band can appear in
different orders across controlled random seeds, a tracker in an earlier band
precedes that band, SPELLING preference within a band holds, and future-due
stay excluded. Run with `CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test --tests 'com.odde.donut.controllers.RecallsControllerTests' -Dspring.profiles.active=test`.
Stop relying on web/CLI client shuffles as the product rule for this due list
(remove or neutralize double-shuffle so the shared response owns the order).

Behavior: When the shared due list is built for eligible trackers → order by
ascending 12-hour UTC bands of `next_recall_at`; within a band keep SPELLING
first, then randomize remaining peers via existing `Randomizer`/testability
seeding. Materially older overdue work (earlier band) retains priority.
Eligibility and commissioned separation stay as today.

## Current decisions

- Recommended direction from story refinement: Anki-scaled hour fuzz +
  due-then-random within 12h bands; durable hash for scheduling fuzz; forward-only
  application.
- Slice-plan refinement: not needed — two cohesive Behavior slices, one proof
  loop each, no unresolved boundary or sizing concern within the understood
  outcome.
- Slice 1 learning: below-threshold Stability must bypass review-fuzz equal-bounds
  expansion (`upper = lower + 1` when delta is 0); gate short intervals before
  `withReviewFuzz`.

## Accepted proof (slice 1)

```bash
CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test \
  --tests 'com.odde.donut.entities.MemoryTrackerCorrectRecallSchedulingTest' \
  --tests 'com.odde.donut.entities.MemoryTrackerCorrectRecallDueFuzzTest' \
  --tests 'com.odde.donut.entities.MemoryTrackerRebuildFromRecallHistoryTest' \
  -Dspring.profiles.active=test
```

PASS. Due-fuzz assertions live in `MemoryTrackerCorrectRecallDueFuzzTest`
(below/at threshold, identity variance, Easy first, over-cap ≤ max); rebuild
`rebuildingFuzzedDueFromTheSameValidHistoryReproducesStabilityDifficultyAndDue`.
Queue-order promises remain slice 2.

CI repair (run 40626834805): residual fixtures/assertions assumed unfuzzed
dues; builders now reschedule Grade-path fuzz after persist; residual tests
assert relative to actual fuzzed `nextRecallAt` / Stability. Slice 1 remains
`done`.

## Remaining concerns

None identified for readiness.
