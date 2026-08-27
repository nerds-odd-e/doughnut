# 0003 — Spaced-repetition scheduling policy

**Status:** Accepted  
**Date:** 2026-08-05  
**Decision makers:** Terry Yin
**Consulted:** None

## Context

Doughnut bases its recall schedule on open FSRS-6. This ADR owns
spaced-repetition domain terms and scheduling policy: which events affect
memory, which inputs matter, and which invariants those transitions preserve.
It also owns how a morning's recall is measured for the **Cognitive index**.
General product recall language remains in
[ADR 0001](./0001-ubiquitous-language.md). Source code and tests own FSRS
mechanics and numeric outcomes.

## Decision

### Doughnut and FSRS language

**Recall** is FSRS **review**. A **memory tracker** is an FSRS card.
**Assimilation** creates a New memory tracker. Doughnut does not model FSRS
Learning, Review, and Relearning as product states.

### Spaced-repetition glossary

- **New** — A memory tracker that has not been graded. It is due immediately.
- **Stability** — The interval over which a memory is expected to remain
  recallable at the requested retention. Higher Stability means slower
  forgetting and normally permits a longer scheduled interval.
- **Difficulty** — The estimated inherent difficulty of retaining a memory.
  Higher Difficulty reduces the Stability gained from a successful recall.
- **Retrievability** — The predicted probability of recalling a memory at a
  particular elapsed time, given its Stability. It is a scheduling input, not
  part of the persisted current memory state (though a RecallLog row may cache
  it as a historical snapshot — see "Recall history and current state").
- **RecallLog** — The durable history of scheduling events for one memory
  tracker (Grades and Confusion).
- **Thinking time** — How long the learner took to answer a measured prompt.
  Presentation and analysis only; not a scheduling input. Excludes **away**
  and **detour**; **idle** stays inside it.
- **Confusion** — A secondary, weaker memory adjustment when a spelling answer
  accidentally identifies another eligible note. Not a Grade or recall credit.
- **Overlap** — A declared non-distinguishing spelling outcome. Neither a Grade
  nor a memory-state transition.

### Cognitive index

[ADR 0001](./0001-ubiquitous-language.md) names **Cognitive index**. It is a
residual analysis of a morning's recall, not a scheduling input and not a
restatement of what the scheduler scheduled.

- **Away** — Interval where the learner switches to another tab or app while
  a recall prompt is active, excluded from thinking time. Distinct from a
  detour.
- **Detour** — Interval where the learner navigates away from an active recall
  prompt to view a note or notebook and returns via Resume, excluded from
  thinking time. Attributed to the note that was opened. Distinct from away.
- **Idle** — Stretch of an active recall prompt with no learner input past a
  generous threshold. Stays inside thinking time (unlike away and detour, the
  clock is not paused). It flags the attempt as one to discount; it never
  silently subtracts.
- **Pace** — A learner's per-item time intensity on a given morning, expressed
  against their own recent history rather than as a raw duration. One of the
  three residual channels beneath the Cognitive index.
- **Retrieval lapse** — A correct answer whose thinking time is unusually slow
  relative to that item's own expectation. Distinct from an incorrect answer
  (a knowledge gap) and from Confusion or Overlap (not Grades).
- **Cognitive index** — Daily composite readout comparing a morning's recall
  outcomes, pace, and consistency against expectation and against the
  learner's own baseline. A residual signal, not diagnostic of cause.
- **Daily probe** — Optional, opt-in standalone task (~60 seconds, identical
  stimuli every day) offered once per local day before the first recall
  session, used to validate the Cognitive index independently of recall item
  content.

### FSRS profile

Doughnut owns its open-FSRS-6 implementation. It uses one product-wide
requested retention and one product-wide maximum interval; neither is a learner
setting.

The scheduler uses elapsed time at whole-hour precision, without calendar-day
boundaries, interval fuzz, or FSRS card-state step lists. Short-term and
long-term recall follow open-FSRS-6.

### Grade transitions

[ADR 0001](./0001-ubiquitous-language.md) defines Grade. The scheduler evaluates
a Grade from the memory state immediately before the recall and the elapsed
time since the previous Grade:

- The first Grade of a New tracker initializes Stability and Difficulty.
- Again is failed recall and weakens the memory state without a permanent
  short-interval trap. On a previously graded tracker, FSRS calls this a lapse;
  new Stability comes from current Difficulty, Stability, and Retrievability.
  Lapse history is the Again outcomes in RecallLog (also used for the
  frequent-failure warning).
- Hard, Good, and Easy are successful recall. Their effect increases in that
  order; more difficult memories gain less Stability.
- A successful overdue recall may gain more Stability, based on elapsed time
  and Retrievability, and bounded.

Every Grade appends to RecallLog and updates Stability, Difficulty, and due
time. The next recall is strictly after the Grade. Scheduling inputs are the
Grade, current Stability and Difficulty, and elapsed time.

All Grade sources share this model, including Tutor Feedback.

### Commissioned Learning Session

Recording Tutor Feedback applies that Grade to matched commissioned memory
trackers. Unmatched entries and Grades outside 1–4 are rejected and reported.
A partly usable Report records the Session Items it matched; unmatched Session
Items are unchanged. A further Report is a further Grade.

Request/Report documents and title matching:
[commissioned learning session protocol](../commissioned-learning-session-protocol.md).

### Accidental matches, confusion, and overlap

Without Overlap, an accidental spelling match:

1. The tracker under recall receives Again.
2. When the answer identifies exactly one accessible note with an eligible
   active note-level tracker, that matched tracker receives Confusion.

Confusion may bring due time closer and weaken Stability. It does not change
Difficulty or establish a new recall-time anchor. It remains attributable to
the causing Answer. No unambiguous eligible tracker means no secondary
adjustment.

Declared Overlap leaves both schedules unchanged. The learner retries with a
more specific answer.

### Recall history and current state

A memory tracker carries current Stability, Difficulty, and due time, updated
transactionally with each scheduling event so due work is a direct query. The
state can be rebuilt from RecallLog; due-work queries do not replay the log.
Removed trackers retain history; deleted trackers are outside reconstruction.

A RecallLog row may also cache the Stability, Difficulty, and Retrievability
that produced it (`stability_before`, `difficulty_before`, `retrievability` on
`recall_log`), for readouts that would otherwise replay history per query
(e.g. the Cognitive index's accuracy channel). This is a materialized cache of
a value the frozen FSRS profile always reproduces by replay, not a new source
of truth: RecallLog's Grades and Confusion remain the record, and the cached
columns must never diverge from what replay would produce. If the FSRS profile
ever changes, existing cached columns describe scheduling as it was, not as it
would be recomputed.

A prompt event remains attributable to its Answer. A Grade from just review or
Tutor Feedback need not have an Answer.

## Prerequisites / Assumptions

- Grades are trustworthy enough to be the primary scheduling signal.

## Related

- [ADR 0001: Ubiquitous language](./0001-ubiquitous-language.md) — general
  recall, memory tracker, assimilation, and Cognitive index product language
- [Commissioned learning session protocol](../commissioned-learning-session-protocol.md)
  — Request/Report documents and matching by note title
- [`Fsrs`](../../backend/src/main/java/com/odde/donut/entities/Fsrs.java) —
  exact algorithm, weights, constants, and numeric rules
- [`MemoryTracker`](../../backend/src/main/java/com/odde/donut/entities/MemoryTracker.java)
  — Grade and Confusion transitions and current scheduling-state updates
- [`RecallLog`](../../backend/src/main/java/com/odde/donut/entities/RecallLog.java)
  — persisted event shape
- [FSRS algorithm reference](https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm)
- [Anki answer-button semantics](https://docs.ankiweb.net/studying.html#answer-buttons)
- [Reddy et al.](https://arxiv.org/abs/1602.07032)
