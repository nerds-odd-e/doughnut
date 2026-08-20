# 0003 — Spaced-repetition scheduling policy

**Status:** Accepted  
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
- **RecallLog** — The durable history of scheduling events for one memory
  tracker. It records Grades and confusion adjustments in enough context to
  explain and reconstruct the tracker's memory state.
- **Thinking time** — How long the learner took to answer a measured prompt.
  It is retained for presentation and analysis rather than used as a scheduling
  input.
- **Confusion** — A secondary, deliberately weaker memory adjustment when a
  spelling answer accidentally identifies another eligible note. It is not a
  Grade or recall credit.
- **Overlap** — A declared non-distinguishing spelling outcome. It is neither a
  Grade nor a memory-state transition.

### FSRS profile

Doughnut owns its open-FSRS-6 implementation instead of adopting a scheduling
library. It uses one product-wide requested retention—the target probability
of successful recall at the scheduled time—and one product-wide maximum
interval. Neither is a learner setting; per-user fitting is deferred.

The scheduler uses elapsed time at whole-hour precision. It does not use
calendar-day boundaries, interval fuzz, or FSRS card-state step lists.
Short-term and long-term recall follow the corresponding open-FSRS-6 behavior.

### Grade transitions

[ADR 0001](./0001-ubiquitous-language.md) defines Grade and the experiences that
produce it. The scheduler evaluates a Grade from the memory state immediately
before the recall and the elapsed time since the previous Grade:

- The first Grade of a New tracker initializes Stability and Difficulty.
- Again represents failed recall and weakens the memory state without creating
  a permanent short-interval trap. FSRS calls Again on a previously graded
  tracker a lapse and derives its new Stability from the current Difficulty,
  Stability, and Retrievability. Doughnut records lapse history as Again
  outcomes in RecallLog, which also support the frequent-failure warning.
- Hard, Good, and Easy represent successful recall. Their effect increases in
  that order, while more difficult memories gain less Stability.
- A successful overdue recall may gain more Stability than an otherwise equal
  recall at the scheduled interval. That extra growth is based on elapsed time
  and Retrievability, not on punishment or reward for queue compliance, and is
  bounded.

Every Grade appends to RecallLog and updates the memory tracker's current memory
state and due time. The next recall is strictly after the Grade. Scheduling uses
the Grade, current Stability and Difficulty, and elapsed time; Thinking time
remains attached to the Answer for presentation and analysis.

All sources of a Grade have the same scheduling meaning. Commissioned Learning
Session semantics are defined by [ADR
0005](./0005-commissioned-learning-session-protocol.md).

### Accidental matches, confusion, and overlap

Without a declared Overlap, an accidental spelling match has two possible
effects:

1. The tracker under recall receives Again.
2. When the answer identifies exactly one accessible note with an eligible
   active note-level tracker, that matched tracker receives Confusion.

Confusion must remain less harmful than Again. It may bring the due time closer
and weaken Stability, but it does not change Difficulty, count as a Grade or
failed recall, or establish a new recall-time anchor. It remains attributable to
the answer that caused it. If there is no unambiguous eligible tracker, no
secondary adjustment is made.

For a declared Overlap, neither tracker receives recall credit, Again, or
Confusion. Their schedules remain unchanged and the learner retries with a more
specific answer.

### Recall history and current state

RecallLog is the durable sequence of Grades and Confusion events. A prompt
event remains attributable to its Answer; a Grade from just review or Tutor
Feedback need not have an Answer.

A memory tracker carries the current Stability, Difficulty, and due time. Each
scheduling event updates that state transactionally so due work remains a
direct query. The state can be rebuilt deterministically from RecallLog when
needed; normal due-work queries do not replay the log. Removed trackers retain
their history, while deleted trackers are outside reconstruction.

## Consequences

- Scheduling responds to demonstrated recall and elapsed time, and all
  Grade-producing experiences share one transition model.
- RecallLog explains history while the memory tracker's current state keeps
  due-work queries efficient.
- Algorithm mechanics can evolve within this policy. A change to a domain
  concept or transition invariant requires an ADR update.

## Prerequisites / Assumptions

- Grades are trustworthy enough to be the primary scheduling signal.

## Options considered

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
- [`Fsrs`](../../backend/src/main/java/com/odde/doughnut/entities/Fsrs.java) —
  exact algorithm, weights, constants, and numeric rules
- [`MemoryTracker`](../../backend/src/main/java/com/odde/doughnut/entities/MemoryTracker.java)
  — Grade and Confusion transitions and current scheduling-state updates
- [`RecallLog`](../../backend/src/main/java/com/odde/doughnut/entities/RecallLog.java)
  — persisted event shape
- [FSRS algorithm reference](https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm)
- [Anki answer-button semantics](https://docs.ankiweb.net/studying.html#answer-buttons)
- [Reddy et al.](https://arxiv.org/abs/1602.07032)
