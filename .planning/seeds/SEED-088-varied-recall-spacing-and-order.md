---
id: SEED-088
status: dormant
planted: 2026-10-28
planted_during: owner-directed recall scheduling discussion
trigger_when: implementing interval fuzzing and varied recall question order
scope: medium
---

# SEED-088: Vary recall spacing and question order

## Why This Matters

A learner reports that recall questions repeatedly appear in the same order,
allowing the sequence to become a cue. The owner selected interval fuzzing at
hour precision and random ordering of similarly due questions, with
deterministic scheduling replay, and requested this as the first queued story.

## Alternatives and Decision

Interval fuzzing disperses future due times; queue randomization varies the
order of questions already due. Deliver both as one learner outcome. Merely
shuffling exact timestamp ties leaves closely spaced batches ordered, while
fuzzing alone leaves some already-due batches ordered. Thinking time remains
presentation and analysis data under ADR 0003.

## Story Decomposition

<a id="story-1"></a>

### Vary recall spacing and the order of similarly due questions

**Identity:** SEED-088#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **Goal:** A learner recalls material with varied spacing and question order,
  reducing predictable sequence cues while retaining useful due-work priority.
- **Scope:** Apply bounded FSRS interval fuzzing at whole-hour precision when
  scheduling the next recall. Fuzz the scheduled interval while retaining the
  calculated Stability and Difficulty. Keep due times strictly after the Grade
  and within the product maximum interval. Randomize similarly due questions
  through the shared recall queue while retaining due eligibility and priority
  for materially older overdue work. Make scheduling randomness reproducible
  from durable event information so history reconstruction remains deterministic.
- **Key examples / evaluation:**
  - Trackers with matching memory states and Grades in the same batch can
    receive different bounded next intervals at hour precision. Random choices
    may coincide; every pair need not receive distinct intervals.
  - Similarly due questions can appear in different orders across recall
    sessions, including a batch whose due timestamps differ slightly.
  - A future-due tracker stays outside the eligible queue, and materially older
    overdue work retains priority over the similarly due batch.
  - Repeated reconstruction from the same valid history and starting state
    produces identical memory state and due time. Rebuilding after an excluded
    attempt follows the same deterministic rule for the remaining valid events.
- **Value / learning:** Reduce sequence-based recall cues and spread batches
  across future due times without making stored schedules irreproducible.
- **Effort hypothesis:** M–L, medium confidence; refine the queue window and
  replay inputs before sizing execution slices.
- **Depends on:** None.
- **Safe stopping point:** The complete behavior stands alone in the existing
  recall flow, with deterministic replay and existing due-work eligibility.

## Open Decisions for Refinement

- Choose the fuzz bounds and short-interval policy at hour precision.
- Define the similarly due window and when the queue's random order is selected.
- Choose durable randomness inputs that work for full-history and exclusion
  rebuilds, including trackers whose complete history is unavailable.
- Decide how the change applies to existing schedules.

## When to Surface

First in the product backlog by owner instruction on 2026-10-28.

## Breadcrumbs

- [ADR 0003: Spaced-repetition scheduling policy](../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md).
- Owner instructions in the recall scheduling discussion on 2026-10-28.
- [Anki interval fuzz implementation](https://github.com/ankitects/anki/blob/main/rslib/src/scheduler/states/fuzz.rs).
- [Anki review sort order](https://docs.ankiweb.net/manual/deck-options#review-sort-order).
