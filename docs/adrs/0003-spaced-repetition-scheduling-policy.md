# 0003 — Spaced-repetition scheduling policy

**Status:** Proposed  
**Date:** 2026-08-05  
**Decision makers:** Terry Yin
**Consulted:** None

## Context

Doughnut bases its recall schedule on open FSRS-6. The product needs stable
language for memory state and durable policy for how recall outcomes affect a
schedule.

This ADR is the canonical reference for Doughnut's spaced-repetition domain
concepts. It deliberately does not restate executable details already expressed
by the implementation, such as formulas, weights, persistence columns, wire
values, rounding steps, or worked examples.

## Decision

### Authority boundary

The **Spaced-repetition glossary** below owns the meaning of Doughnut's
spaced-repetition terms. Amend it in place when a domain concept changes, and
use the same meanings in product language, code, APIs, and tests. General
product recall language remains in [ADR 0001](./0001-ubiquitous-language.md).

This ADR also records durable scheduling policy: which events affect memory,
which inputs are relevant, and the invariants those transitions preserve.
Source code and tests own the exact FSRS mechanics and their numeric outcomes.
A mechanical change within these policies does not need prose copied here; a
change to a concept or policy does.

### Doughnut and FSRS language

In Doughnut, **recall** is the activity that FSRS calls a **review**. **Just
review** is one method of recall, alongside a recall prompt. A Doughnut **memory
tracker** corresponds to an FSRS card.

**Assimilation** creates a New memory tracker. Doughnut does not model FSRS
Learning, Review, and Relearning as product states and has no learning or
relearning step list.

### Spaced-repetition glossary

- **New** — A memory tracker that has not been graded. It is due immediately.
  Its first Grade establishes its initial memory state.
- **Stability** — The interval over which a memory is expected to remain
  recallable at the requested retention. Higher Stability means slower
  forgetting and normally permits a longer scheduled interval.
- **Difficulty** — The estimated inherent difficulty of retaining a memory.
  Higher Difficulty reduces the Stability gained from a successful recall.
- **Retrievability** — The predicted probability of recalling a memory at a
  particular elapsed time, given its Stability. It is an input to a scheduling
  transition, not part of the persisted current memory state.
- **Requested retention** — The product's target probability of successful
  recall at the scheduled time. It determines the scheduled interval implied
  by Stability.
- **Scheduled interval** — The duration from a graded recall to the next due
  time at the requested retention.
- **Maximum interval** — The product-wide upper bound on a scheduled interval.
  It prevents unbounded schedules and is not a learner setting.
- **Last recalled at** — The time of the latest Grade for a memory tracker. New
  trackers do not have one.
- **Assimilated at** — The time a memory tracker was created through
  assimilation. It anchors the immediate due time while the tracker is New.
- **Next recall at** — The time at which a memory tracker is next due.
- **Grade** — The single scheduling evaluation of a recall: **Again**, **Hard**,
  **Good**, or **Easy**, in increasing order of demonstrated recall. Recall
  prompts, just review, and Tutor Feedback all use this concept.
- **RecallLog** — The durable history of scheduling events for one memory
  tracker. It records Grades and confusion adjustments in enough context to
  explain and reconstruct the tracker's memory state.
- **Thinking time** — How long the learner took to answer a measured prompt.
  It may be reported to the learner but is not evidence of memory state.
- **Confusion** — A secondary, deliberately weaker memory adjustment when a
  spelling answer accidentally identifies another eligible note. It is not a
  Grade or recall credit.
- **Overlap** — A declared non-distinguishing spelling outcome. It is neither a
  Grade nor a memory-state transition.

### FSRS profile

Doughnut owns its open-FSRS-6 implementation instead of adopting a scheduling
library. It uses one product-wide requested retention and maximum interval;
per-user fitting is deferred.

The scheduler uses elapsed time at whole-hour precision. It does not use
calendar-day boundaries, interval fuzz, or FSRS card-state step lists.
Short-term and long-term recall follow the corresponding open-FSRS-6 behavior.

### Lapse

A **Lapse** is an Again Grade on a previously graded tracker. FSRS-6 derives
post-lapse Stability from the tracker's Difficulty, Stability, and
Retrievability; cumulative lapse count is not a scheduling input. Doughnut
represents lapse history as Again outcomes in RecallLog and uses that history
for the frequent-failure warning.

### Grade transitions

A Grade is evaluated from the memory state immediately before the recall and
the elapsed time since the previous Grade:

- The first Grade of a New tracker initializes Stability and Difficulty.
- Again represents failed recall and weakens the memory state without creating
  a permanent short-interval trap.
- Hard, Good, and Easy represent successful recall. Their effect increases in
  that order, while more difficult memories gain less Stability.
- A successful overdue recall may gain more Stability than an otherwise equal
  recall at the scheduled interval. That extra growth is based on elapsed time
  and Retrievability, not on punishment or reward for queue compliance, and is
  bounded.

Every Grade appends to RecallLog and updates the memory tracker's Stability,
Difficulty, Last recalled at, and Next recall at. The next recall is strictly
after the Grade. Queue lateness, thinking time, time zone, and time of day are
not memory-state inputs.

All sources of a Grade have the same scheduling meaning. In particular, Tutor
Feedback from a commissioned Learning Session is a Grade at its recorded time.
A Session Item without Feedback supplies no Grade and therefore does not change
its memory tracker. Tutor semantics are defined by [ADR
0005](./0005-commissioned-learning-session-protocol.md).

### Accidental matches, confusion, and overlap

Without a declared Overlap, an accidental spelling match has two possible
effects:

1. The tracker under recall receives Again.
2. When the answer identifies exactly one accessible note with an eligible
   active note-level tracker, that matched tracker receives Confusion.

Confusion must remain less harmful than Again. It may bring the due time closer
and weaken Stability, but it does not change Difficulty, count as a Grade or
failed recall, or become the Last recalled at anchor. It remains attributable
to the answer that caused it. If there is no unambiguous eligible tracker, no
secondary adjustment is made.

For a declared Overlap, neither tracker receives recall credit, Again, or
Confusion. Their schedules remain unchanged and the learner retries with a more
specific answer.

### Recall history and current state

RecallLog is the durable sequence of Grades and Confusion events. A prompt
event remains attributable to its Answer; a Grade from just review or Tutor
Feedback need not have an Answer.

MemoryTracker stores the current scheduling state. Each scheduling event
updates that state transactionally so due work remains a direct query. The
scheduling state can be rebuilt deterministically from RecallLog when needed;
normal due-work queries do not replay the log. Removed trackers retain their
history, while deleted trackers are outside reconstruction.

## Consequences

- Spaced-repetition terminology has one official domain reference without
  turning the ADR into a second implementation.
- Scheduling responds to demonstrated recall and elapsed time, not compliance
  with the due queue.
- All Grade-producing experiences share one transition model.
- RecallLog explains history while the current MemoryTracker state keeps
  due-work queries efficient.
- Algorithm mechanics can evolve within this policy without synchronizing
  formulas or numeric examples in prose.
- A change to the meaning of a concept or to a transition invariant requires an
  ADR update as well as an implementation change.

## Prerequisites / Assumptions

- Grades are trustworthy enough to be the primary scheduling signal.
- A scheduling event has the current tracker state, its recorded time, and the
  elapsed time needed to apply the policy.
- RecallLog carries enough information to reconstruct the current scheduling
  state on MemoryTracker.

## Options considered

- **Adopt an open-FSRS library** — Rejected in favor of owning the
  open-FSRS-compatible implementation and its Doughnut-specific policy.
- **Judge early or overdue recall by queue compliance** — Rejected. Memory
  transitions use elapsed time and Retrievability.
- **Use a linear lateness bonus** — Rejected because overdue growth must remain
  bounded.
- **Replay RecallLog for every due-work query** — Rejected in favor of current
  scheduling fields on MemoryTracker.
- **Separate lapse counter** — Rejected because RecallLog already represents
  lapse history and drives the frequent-failure warning.
- **FSRS card states** — Rejected because Doughnut does not use learning or
  relearning step lists.
- **Randomize intervals** — Rejected; Doughnut does not use interval fuzz.
- **Use thinking time as a memory-state input** — Rejected. It is display and
  analysis data only.

## Related

- [ADR 0001: Ubiquitous language](./0001-ubiquitous-language.md) — general
  recall, memory tracker, and assimilation language
- [ADR 0005: Commissioned Learning Session
  protocol](./0005-commissioned-learning-session-protocol.md) — Tutor Feedback
  semantics
- [FSRS compatibility
  tracker](../../.planning/research/FSRS-COMPATIBILITY-GAP.md) — implementation
  gaps and deferred work, not a second policy map
- [`Fsrs`](../../backend/src/main/java/com/odde/doughnut/entities/Fsrs.java) —
  exact algorithm, weights, constants, and numeric rules
- [`MemoryTracker`](../../backend/src/main/java/com/odde/doughnut/entities/MemoryTracker.java)
  — Grade and Confusion transitions and current scheduling-state updates
- [`RecallLog`](../../backend/src/main/java/com/odde/doughnut/entities/RecallLog.java)
  — persisted event shape
- [FSRS algorithm reference](https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm)
- [Anki answer-button semantics](https://docs.ankiweb.net/studying.html#answer-buttons)
- [Reddy et al.](https://arxiv.org/abs/1602.07032)
