# 0003 — Spaced-repetition scheduling policy

**Status:** Proposed  
**Date:** 2026-08-05  
**Decision makers:** Terry Yin (approval pending advice)  
**Consulted:** To be filled by the decision maker

## Context

Until 2026-08-05, Doughnut's forgetting-curve success path treated deviation
from the planned next-recall time symmetrically: both early and overdue
correct answers reduced the success increment (`Math.abs(delay)`). An overdue
correct answer could therefore weaken the tracker and, at the floor, become
due immediately. That **late-success penalty is removed** (`735b96623a`):
overdue correct keeps the on-time increment.

What remains: the success API still expresses time as `delayInHours` relative
to a recomputed expected recall time (`lastRecalledAt + current interval`). For
an early correct recall, the current formula is algebraically equivalent to
scaling growth by `elapsed / current interval`; it does not read the persisted
`nextRecallAt`. The contract nevertheless hides elapsed time, and an incorrect
recall currently fails to advance `lastRecalledAt`, so a later correct recall
can span across the failure. Overdue answers are not rewarded the way open FSRS
rewards low retrievability (longer elapsed → larger bounded stability increase).

The planned recall time is a queue target. Missing it can reflect availability,
queue size, question readiness, or system behavior — not that the learner forgot.
A memory-state transition uses the recall outcome and the actual elapsed time
since the previous state-changing recall.

Established schedulers (including FSRS) separate memory-state inputs from
schedule compliance: successful overdue recall is not a failure, and retention
is judged from observed elapsed time. This ADR states Doughnut's durable
scheduling policy and safety properties. It does not select a formula, model,
or constants.

Today's due-work projection (`nextRecallAt`) cannot always be rebuilt from
answer history alone. That operational constraint does not make meeting or
missing the due time an input to the memory-state transition.

## Decision

Finalized choices live here. Move items from **Working draft** into this
section as they are locked.

### Recall, not FSRS "review"

Keep **recall**. FSRS **review** is the same activity; Doughnut names it recall
because **recall is better than review**. Glossary: [ADR 0001](./0001-ubiquitous-language.md).
Do not use **review** as a Doughnut domain noun. When citing FSRS, pair once
(**review (FSRS) = recall**) then use Doughnut terms.

### Whole-hour elapsed-time precision

Use **whole elapsed hours** as the recall-transition time input. Measure the
duration between the current recall time and `lastRecalledAt`, then discard any
sub-hour remainder. This is elapsed duration, independent of the learner's time
zone or whether the timestamps fall on the same calendar day.

Doughnut's morning/afternoon recall windows make day precision too coarse: two
recalls in different halves of one day can have meaningful elapsed time. C1 does
not add a separate same-day transition; a future Stability/Difficulty behavior
may introduce an explicit short-term rule without changing this decision.

### Accidental-match and overlap transitions

An **accidental match** is a spelling answer that fails the note under recall
but names another accessible note by title or plain alias. Unless the notes have
a declared overlap, the answer has the following consequences:

1. The spelling tracker under recall receives the ordinary incorrect-recall
   transition, including its full negative memory-state adjustment and
   relearning projection. It advances `lastRecalledAt` and `recallCount` and
   counts as a failed recall for that tracker.
2. A secondary **confusion adjustment** applies only when the answer matches
   exactly one accessible note and that learner has an eligible active tracker
   for it. Prefer its spelling tracker; otherwise use its note-level
   understanding tracker. Never select a property or commissioned tracker, a
   removed or deleted tracker, or create a tracker implicitly.
3. The confusion adjustment is strictly weaker than ordinary incorrect recall.
   It reduces the selected tracker's memory strength and recomputes its due-work
   projection from its existing recall anchor. It must not advance
   `lastRecalledAt`, increment `recallCount`, count as a failed recall, or move
   an already scheduled recall later.
4. The primary incorrect transition and any secondary confusion adjustment are
   one atomic grading operation. The secondary adjustment must remain durably
   attributable to the accidental-match answer that caused it.
5. When no eligible secondary tracker exists, or more than one accessible note
   matches, only the tracker under recall changes. Do not choose a matched note
   arbitrarily.

A declared **overlap** is a separate, non-distinguishing outcome: the answer is
accepted by the note under recall, that note explicitly declares overlap with
another accessible note, and the same answer is accepted by the declared note.
Neither tracker receives recall credit, an incorrect transition, or a confusion
adjustment; their schedule fields remain unchanged, and the learner may retry
with a more specific answer in the same session.

## Working draft

### Recall inputs and scheduling are separate

1. A memory tracker transitions from its persisted pre-recall state using the
   graded recall outcome and actual elapsed time since the previous
   state-changing recall.
2. The planned next-recall time is scheduling metadata. Being early or overdue
   is not, by itself, success or failure.
3. Scheduling must not treat backlog age or deviation from the due time as
   a negative memory-state input.
4. Every state-changing recall path must update `lastRecalledAt` to the recall
   time. Existing history is a migration input, not proof that past recall
   inputs were recorded completely.

### Memory state and the due-work projection

1. Conceptual source of truth is the memory tracker's persisted state, the
   recall outcome and time, and the active policy. The due time is a
   materialized selection projection, not a memory-strength goal.
2. While history remains insufficient for rebuild, the projection must stay
   transactionally consistent with memory state after every scheduling event
   and may remain the authoritative due-work lookup.
3. Memory strength must not be inferred from whether the due time was met. The
   projection may be used for eligibility, ordering, display, and operations.
4. A future rebuildable design must state its boundary explicitly: either enough
   current scheduler state and configuration to recompute the due time, or
   complete versioned history for deterministic replay. Until then, migrations
   must seed from the existing tracker snapshot and must not drop or
   bulk-reinterpret historical due times as if history were complete.
5. Rebuildability is desirable but not a prerequisite for removing the
   late-success penalty.

### Recall history is deferred

1. A future persisted **RecallLog** will be Doughnut's counterpart to the open
   FSRS review log. It will gather the recall result and enough pre-transition
   scheduler state, configuration/version, recall time, and resulting schedule
   data for deterministic replay and parameter fitting.
2. `RecallLog` is not required for C1. C1 transitions the existing persisted
   `MemoryTracker` snapshot and does not add a new history table.
3. Existing answers and Tutor Feedback are partial history. They must not be
   treated as a complete `RecallLog` or used to bulk-reinterpret legacy due
   times.

### Spelling memory tracker

A spelling memory tracker is learner-created (glossary:
[ADR 0001](./0001-ubiquitous-language.md)). Assimilation due and the **daily
assimilation target** include understanding note-level trackers only — a
spelling tracker does not consume either (same as a commissioned tracker for
that queue).

Spelling recall grading follows the locked accidental-match and overlap
transitions in the Decision section.

### Graded outcomes

Correct, incorrect, accidental match, and overlap are distinct graded outcomes
where the product defines them. Scheduling must follow the outcome — not collapse
special spelling results into boolean correct/incorrect.

#### Correct recall

1. Schedule the tracker strictly in the future; it must not remain due at the
   answer instant.
2. Do not reduce learned memory strength solely because the answer was early or
   overdue.
3. A correct overdue recall is successful retention over a longer observed
   interval. Its memory-strength result must be no worse than the same correct
   recall at the planned time. Any lateness bonus may be bounded.
4. A correct early recall may grow less than an on-time recall because it
   demonstrates retention over a shorter interval, but must not reset learning
   or make the tracker immediately due.
5. A sequence of correct recalls separated by meaningful time must show forward
   progress toward longer intervals. Backlog alone must not trap a tracker in
   an immediate or daily-recall loop.

#### Incorrect recall

1. Incorrect recall may reduce strength and shorten the next interval.
2. The penalty is based on the failed outcome, not on earliness or lateness.
3. Failure must not permanently trap the tracker; later correct recalls must be
   able to restore expanding intervals.
4. Same-session retry and the persisted next-recall time are separate. The
   post-grade schedule must be explicit.

### Frequent-failure warning

When a memory tracker accumulates too many wrong answers in a rolling window,
Doughnut warns the learner but does not change the schedule or remove the
tracker.

1. **Threshold:** ≥ 5 incorrect recalls within the last 14 days, per tracker.
   **Overlap** does not count toward this total.
2. **Response:** On each incorrect recall while still at or over the threshold,
   show an informational warning with live counts from the API (`wrongCount`,
   `threshold`, `periodDays`). No confirm action and no tracker deletion.
3. **Property trackers:** The warning names the property when the tracker is
   property-keyed.

### Commissioned learning session feedback

A commissioned memory tracker is graded from the Feedback a Tutor returns for its
Session Item, not from a recall question Doughnut asked. Feedback carries a score
from 0 to 5. ADR 0001 defines the vocabulary and ADR 0005 defines what the score
means to the Tutor; this section defines what it does to the schedule.

1. A recorded score drives a memory-state transition of the same standing as a
   graded recall answer: recording it counts the recall, sets the last-recalled
   time, and reschedules the tracker.
2. Scores move memory strength as follows, where **accumulated strength** means
   strength above the initial level of a newly assimilated tracker:

| Score | Learner demonstrated | Memory-strength result |
|-------|----------------------|------------------------|
| 5 | Mastery with full fluency | Successful recall, growth 20% above the standard increment |
| 4 | Mastery with fluency | Successful recall, standard growth |
| 3 | Mastery, but not fluent | Successful recall, growth 20% below the standard increment |
| 2 | Needed a reminder at first, then showed signs of mastery | No growth; accumulated strength reduced by 20% |
| 1 | Needed several reminders | No growth; accumulated strength reduced by 50% |
| 0 | Could not reach the learning point even with help | Accumulated strength reset to the initial level |

3. Demonstrated mastery always moves forward. Scores 3 through 5 grow strength,
   so a learner who masters a learning point without ever becoming fluent still
   earns lengthening intervals rather than decaying toward permanent due work.
4. Reductions apply to accumulated strength, so a tracker already at the initial
   level cannot fall below it.
5. A learner's spacing list may open with a zero interval. That is legitimate for
   a newly assimilated tracker but not after a graded score, so schedule the next
   recall at or after the first positive interval in the list. Resetting strength
   must never leave a tracker due at the instant its score was recorded.
6. Otherwise schedule the next recall from the updated memory state through the
   normal interval path. Do not apply the incorrect-recall relearning override: a
   commissioned tracker is due only when the learner commissions another
   Learning Session, so a short forced retry window would express nothing.
7. A Tutor session carries no trustworthy effort measurement, so effort is
   neutral.
8. A late session does not weaken the result. The score determines the
   memory-state adjustment; the recorded time advances `lastRecalledAt`.
9. A Session Item that never receives Feedback supplies no graded recall result:
   its tracker stays unchanged and the item is abandoned with its session.

These are the policy's only quantified adjustments. They are stated against
accumulated strength rather than any stored field, index, or interval table, so
an implementation may change representation while preserving them. Policy tests
assert the resulting schedule movement, not the internal measure.

### Recall effort

1. A trustworthy effort measurement (e.g. thinking time) may adjust within a
   correct or incorrect outcome, within bounds.
2. Effort must not invert the outcome: a correct answer cannot become failure,
   reset, or immediate reschedule solely because it was slow.
3. Missing or untrustworthy effort data is neutral.

### Configuration and implementation freedom

1. A newly assimilated tracker may be due immediately. After any graded answer,
   a zero persisted interval is not allowed.
2. User-configured spacing remains an input, subject to these safety
   properties.
3. Internal strength representations, interval tables, increments, interval
   rounding, and any future stability/retrievability model are implementation
   details. Elapsed-time input precision is fixed by the Decision above. Policy
   tests must assert observable schedule behavior, not internal indexes.
4. This ADR does not require FSRS. A smaller compatible algorithm may ship
   first; the internal model may change later without changing these rules.
5. Persisting a due-time projection remains allowed and currently required. Its
   storage does not make due-time compliance a memory-state input.

## Consequences

- Doughnut says **recall**, not FSRS **review**, for the spaced activity
  (see **Decision**).
- Busy users are judged on demonstrated recall, not schedule compliance.
- Correct overdue recalls must not create a positive-feedback workload loop.
- A spelling memory tracker is extra title practice the learner opts into; it
  does not consume assimilation due or the daily assimilation target.
- A non-overlap accidental match fully fails the spelling tracker under recall
  and may also weaken one unambiguously matched spelling or understanding
  tracker without fabricating recall credit for it.
- Declared overlap remains a first-class no-credit, no-penalty outcome with a
  same-session retry.
- Tutor Feedback becomes a grading source alongside Doughnut's own recall
  questions, and is the first place this policy quantifies an adjustment.
- Implementations must distinguish observed retention time from deviation
  relative to a queue target.
- Recall transitions use whole elapsed hours; sub-hour recall timing does not
  create a fractional transition input.
- While history is incomplete, the due-time projection stays operationally
  authoritative for due work even though it is conceptually derived.
- A rebuildable projection needs additional scheduler state or complete
  versioned history.
- Tests that require late correct answers to lose strength solely for lateness
  must be replaced with policy-aligned assertions.
- Workload distributions may change; monitor interval lengths and success rates
  after release.

## Pros

- Aligns memory-state transitions with recall results rather than user
  availability.
- Prevents immediate/daily traps after correct answers.
- Stabilizes the product contract without freezing the next algorithm.
- Allows a minimal fix now and a data-fitted scheduler later.

## Cons

- Some overdue correct answers will get longer intervals than today.
- Existing strength histories were produced under different semantics; rollout
  needs monitoring even without data migration.
- The policy does not choose tuning constants or a target retention rate.

## Prerequisites / Assumptions

- Graded outcomes are trustworthy enough to be the primary scheduling signal.
- Answer time and the current tracker snapshot are available when grading.
  Complete historical events are not assumed reconstructable for legacy data.
- Thinking time is an optional secondary input, not a correctness substitute.
- Initial work preserves and updates the existing due-time projection rather
  than attempting an unsafe historical rebuild.
- Tuning belongs in tests, simulation, and production observation — not in
  this ADR.

## Options considered

### Keep the current symmetric early/late penalty

Rejected: conflates queue compliance with memory and causes the correct-answer
trap.

### Adopt FSRS immediately

Deferred: credible future implementation, but brings a new state model,
parameters, retention targets, fitting, and migration that are unnecessary to
establish the product policy.

### Apply the policy to the existing Doughnut model first

Recommended initial path: use observed retention time, keep successful timing
adjustments non-negative, bound effort, ensure a positive post-answer interval.
Exact mechanics stay outside this ADR.

### Recompute due time from answer history on demand

Deferred: current history is incomplete and unversioned; due-work queries need
a directly queryable projection. May become viable after a complete replay
boundary or sufficient current memory state is persisted.

## Related

- Working discussion (gap + open issues toward finalizing this ADR): [`.planning/research/FSRS-COMPATIBILITY-GAP.md`](../../.planning/research/FSRS-COMPATIBILITY-GAP.md)
- ADR 0001 [ubiquitous language](./0001-ubiquitous-language.md) — **recall** (not FSRS **review**); commissioned learning terms; spelling memory tracker
- ADR 0005 [commissioned learning session protocol](./0005-commissioned-learning-session-protocol.md) — what a score means to the Tutor
- Anki answer semantics: <https://docs.ankiweb.net/studying.html#answer-buttons>
- FSRS algorithm and overdue-recall behavior: <https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm>
- Reddy et al., *Unbounded Human Learning: Optimal Scheduling for Spaced Repetition*: <https://arxiv.org/abs/1602.07032>
