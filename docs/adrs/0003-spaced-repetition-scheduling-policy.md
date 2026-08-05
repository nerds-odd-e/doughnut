# 0003 — Spaced-repetition scheduling policy

**Status:** Proposed  
**Date:** 2026-08-05  
**Decision makers:** Terry Yin (approval pending advice)  
**Consulted:** To be filled by the decision maker

## Context

Doughnut schedules memory trackers from a forgetting-curve model that can treat
deviation from the planned next-recall time as a negative adjustment. An overdue
correct answer can therefore weaken the tracker and, at the floor, become due
immediately. Learners who are repeatedly correct but review a busy backlog late
can trap themselves in immediate or daily recalls.

The planned recall time is a queue target. Missing it can reflect availability,
queue size, question readiness, or system behavior — not that the learner forgot.
Memory evidence is the graded outcome and the actual elapsed time since the
previous graded recall.

Established schedulers (including FSRS) separate memory evidence from schedule
compliance: successful overdue recall is not a failure, and retention is judged
from observed elapsed time. This ADR states Doughnut's durable scheduling policy
and safety properties. It does not select a formula, model, or constants.

Today's due-work projection (`nextRecallAt`) cannot always be rebuilt from
answer history alone. That operational constraint does not make meeting or
missing the due time valid memory evidence.

## Decision

### Evidence and scheduling are separate

1. Graded recall outcome and actual elapsed time since the previous graded
   recall are memory evidence.
2. The planned next-recall time is scheduling metadata. Being early or overdue
   is not, by itself, success or failure.
3. Scheduling must not treat backlog age or deviation from the due time as
   negative memory evidence.
4. Every state-changing recall path must record the timestamp and outcome
   evidence the memory model requires. Existing history is a migration input,
   not proof that past evidence was complete.

### Memory state and the due-work projection

1. Conceptual source of truth is memory state, recall evidence, and active
   policy. The due time is a materialized selection projection, not a
   memory-strength goal.
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
4. A correct early recall may grow less than an on-time recall (weaker
   evidence) but must not reset learning or make the tracker immediately due.
5. A sequence of correct recalls separated by meaningful time must show forward
   progress toward longer intervals. Backlog alone must not trap a tracker in
   an immediate or daily-recall loop.

#### Incorrect recall

1. Incorrect recall is negative memory evidence and may reduce strength and
   shorten the next interval.
2. The penalty is based on the failed outcome, not on earliness or lateness.
3. Failure must not permanently trap the tracker; later correct recalls must be
   able to restore expanding intervals.
4. Same-session retry and the persisted next-recall time are separate. The
   post-grade schedule must be explicit.

#### Accidental match (spelling)

An accidental match is a spelling answer that fails the reviewed note but names
another accessible note (title or plain alias).

1. It is negative evidence, but strictly weaker than incorrect recall on the
   same tracker state.
2. After grading, schedule a future recall from the updated memory state using
   the normal interval path — not the incorrect-recall relearning override.
3. Earliness or lateness does not change the grade; timing follows the
   evidence-vs-schedule separation above.

#### Overlap (declared, non-distinguishing spelling)

Overlap is when the reviewed note **explicitly declares** overlap with another
note and the spelling answer would also be accepted by that note — correct in
isolation, but non-distinguishing. Undirected title/alias collision without
declaration is accidental match when the answer fails the reviewed note.

1. Not a successful recall: no memory-strength growth; do not advance as
   correct.
2. Not an incorrect or accidental-match recall: no those penalties; do not
   change the tracker's schedule fields.
3. Do not count toward wrong-answer / re-assimilation failure streaks.
4. Allow same-session retry with a more specific answer. Retry grades under
   the normal outcome rules (correct, incorrect, or accidental match).

### Recall effort

1. Trustworthy effort evidence (e.g. thinking time) may adjust within a correct
   or incorrect outcome, within bounds.
2. Effort must not invert the outcome: a correct answer cannot become failure,
   reset, or immediate reschedule solely because it was slow.
3. Missing or untrustworthy effort data is neutral.

### Configuration and implementation freedom

1. A newly assimilated tracker may be due immediately. After any graded answer,
   a zero persisted interval is not allowed.
2. User-configured spacing remains an input, subject to these safety
   properties.
3. Internal strength representations, interval tables, increments, rounding,
   and any future stability/retrievability model are implementation details.
   Policy tests must assert observable schedule behavior, not internal
   indexes.
4. This ADR does not require FSRS. A smaller compatible algorithm may ship
   first; the internal model may change later without changing these rules.
5. Persisting a due-time projection remains allowed and currently required. Its
   storage does not make due-time compliance memory evidence.

## Consequences

- Busy users are judged on demonstrated recall, not schedule compliance.
- Correct overdue recalls must not create a positive-feedback workload loop.
- Accidental match and declared overlap remain first-class outcomes with
  distinct scheduling rules.
- Implementations must distinguish observed retention time from deviation
  relative to a queue target.
- While history is incomplete, the due-time projection stays operationally
  authoritative for due work even though it is conceptually derived.
- A rebuildable projection needs additional scheduler state or complete
  versioned history.
- Tests that require late correct answers to lose strength solely for lateness
  must be replaced with policy-aligned assertions.
- Workload distributions may change; monitor interval lengths and success rates
  after release.

## Pros

- Aligns scheduling with memory evidence rather than user availability.
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
- Thinking time is optional secondary evidence, not a correctness substitute.
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

- Anki answer semantics: <https://docs.ankiweb.net/studying.html#answer-buttons>
- FSRS algorithm and overdue-review behavior: <https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm>
- Reddy et al., *Unbounded Human Learning: Optimal Scheduling for Spaced Repetition*: <https://arxiv.org/abs/1602.07032>
