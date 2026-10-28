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

**Recommended:** Adapt Anki's review-interval fuzz to whole hours, and order the
shared due queue as due-time priority then random within a 12-hour similarly-due
band. Derive each Grade's fuzz choice from durable RecallLog fields so replay
and exclusion rebuilds stay deterministic. Apply both only on the next
scheduling write; leave already-stored due times until their next Grade.

**Compared options (same goal):**
- Fuzz only — spreads future dues, but already-due near-batches stay ordered
  (rejected in the original Alternatives).
- Queue random only (exact ties, or shuffle the whole eligible list) — either
  leaves close dues ordered or drops priority for materially older overdue work.
- Anki load-balancer / easy-days selection among fuzz candidates — heavier than
  the selected outcome; not required to remove sequence cues.

**Borrow:** Anki `fuzz.rs` (no fuzz below 2.5 days; percentage bands above that;
uniform pick inside constrained bounds) scaled to hours, and Anki's default
"Due date, then random" review sort. Analogy limit: Anki fuzzes whole days and
may load-balance; Donut schedules whole hours without calendar-day boundaries,
keeps SPELLING preference inside a band, and must derive randomness from
RecallLog rather than a mutable card fuzz factor.

Thinking time remains presentation and analysis data under ADR 0003.

## Story Decomposition

<a id="story-1"></a>

### Vary recall spacing and the order of similarly due questions

**Identity:** SEED-088#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/076-vary-recall-spacing-and-order/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"3e1130a1bf567f610258217840e5ace610a719254111c83cb6e1725305178501","plan":"7ac4bde4251d6cfef964387327f3534327482ae6c7763cceecdce36f870005d6"}}
```

- **Goal:** A learner recalls material with varied spacing and question order,
  reducing predictable sequence cues while retaining useful due-work priority.
- **Scope:**
  - On each Grade that schedules the next recall, apply bounded interval fuzzing
    at whole-hour precision to the scheduled interval derived from Stability.
    Leave the calculated Stability and Difficulty unchanged by fuzz. Keep the
    due time strictly after the Grade and within the product maximum interval.
  - Short-interval policy (Anki scaled to hours): intervals under 60 hours are
    not fuzzed. At and above 60 hours, use Anki's band factors on hour units
    (bands at 60–168 h / 168–480 h / 480+ h with factors 0.15 / 0.10 / 0.05, plus
    a 24-hour base delta), pick uniformly inside the constrained integer-hour
    bounds, and clamp to the product minimum future due and maximum interval.
  - Derive the fuzz choice from durable Grade-event fields already on RecallLog
    (memory tracker id and `recordedAt`, with the Grade's other logged fields as
    needed for a stable hash). Full-history replay and exclusion rebuild that
    start from cached pre-event state and replay later valid events must
    reproduce the same Stability, Difficulty, and due time. Do not add a stored
    fuzz column for this story.
  - Through the shared recall due queue (web and CLI), keep eligibility as
    today. Order eligible work by ascending `next_recall_at` bands of 12 hours
    (UTC hour floors). Within a band, keep SPELLING preference, then randomize
    the remaining peers. Materially older overdue work is any tracker whose
    `next_recall_at` falls in an earlier band. Select that order when building
    the shared due list; a later fetch may differ. Client-only shuffles are not
    the product rule for this outcome.
  - Confusion remains a secondary Stability/due adjustment and does not define
    a separate fuzz policy in this story.
  - Existing stored due times stay until the tracker's next Grade schedules a
    new due; no mass reschedule at deploy.
- **Key examples / evaluation:**
  - Two trackers with matching Stability, Difficulty, and Grade at the same
    recorded time can receive different bounded next intervals at hour
    precision when their durable event identities differ. Random choices may
    coincide; every pair need not receive distinct intervals.
  - An interval of 48 hours is not fuzzed; an interval of 72 hours is fuzzed
    inside its constrained hour bounds and still lands strictly after the Grade
    and at or under the product maximum.
  - Within one 12-hour `next_recall_at` band, similarly due questions (including
    dues that differ by a few hours) can appear in different orders across
    due-list fetches. A tracker whose due is in an earlier band appears before
    that band. Future-due trackers stay outside the eligible queue.
  - SPELLING trackers in a band keep their current preference over other types
    in that band before the remaining peers are randomized.
  - Repeated reconstruction from the same valid history and starting state
    produces identical memory state and due time. Rebuilding after an excluded
    attempt follows the same deterministic rule for the remaining valid events.
- **Architecture:** Scheduling owns fuzz when writing `next_recall_at` from a
  Grade; Stability and Difficulty stay FSRS outputs. Queue order is presentation
  of already-eligible due work in the shared due query/service, not a scheduling
  input and not thinking time ([ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md)).
  Replay must recompute the same fuzzed due from RecallLog without a new source
  of truth. No Accepted ADR conflict; ADR 0003 already requires interval fuzzing
  at whole-hour precision.
- **Value / learning:** Reduce sequence-based recall cues and spread batches
  across future due times without making stored schedules irreproducible.
- **Effort hypothesis:** M, higher confidence after settling window, short-
  interval policy, durable fuzz inputs, and forward-only application.
- **Depends on:** None.
- **Safe stopping point:** The complete behavior stands alone in the existing
  recall flow, with deterministic replay and existing due-work eligibility.

## Open Decisions for Refinement

None remaining for this story.

## When to Surface

First in the product backlog by owner instruction on 2026-10-28.

## Breadcrumbs

- [ADR 0003: Spaced-repetition scheduling policy](../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md).
- Owner instructions in the recall scheduling discussion on 2026-10-28.
- [Anki interval fuzz implementation](https://github.com/ankitects/anki/blob/main/rslib/src/scheduler/states/fuzz.rs).
- [Anki review sort order](https://docs.ankiweb.net/deck-options.html#review-sort-order).
