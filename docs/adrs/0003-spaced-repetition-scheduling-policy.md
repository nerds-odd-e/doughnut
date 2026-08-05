# 0003 — Spaced-repetition scheduling policy

**Status:** Proposed  
**Date:** 2026-08-05  
**Decision makers:** Terry Yin (approval pending advice)  
**Consulted:** To be filled by the decision maker

## Context

Doughnut currently adjusts a memory tracker's forgetting-curve index according
to how far the answer time is from the planned next-recall time. The adjustment
uses the absolute deviation, so an overdue correct answer can reduce the index.
When the reduction reaches the floor at index `100`, the default first interval
is zero and the tracker becomes due immediately. A learner who is repeatedly
correct but reviews a busy backlog one or more days late can therefore become
trapped in immediate or daily recalls.

The planned recall time is a queue target. Missing it can be caused by user
availability, queue size, question availability, or system behavior; it is not
evidence that the learner forgot the material. The actual answer and the time
elapsed since the previous graded recall are memory evidence.

Established schedulers separate memory evidence from schedule compliance. FSRS
does not penalize a successful recall for being overdue. It uses actual elapsed
time to estimate lower retrievability; successfully recalling at that lower
retrievability increases memory stability more than the equivalent on-time
recall. This additional benefit has diminishing returns as elapsed time grows.
Research and established practice also support expanding intervals and
evaluating retention from actual elapsed time rather than schedule compliance.

This ADR defines Doughnut's durable product policy and observable safety
properties. It deliberately does not select a formula, model, or set of
constants.

### Current persistence and reconstructability

The domain goal is to model the learner's memory well enough to decide what is
eligible for recall. A planned next-recall time is an output of that decision,
not the learning goal or memory evidence itself. Persisting `nextRecallAt` is
intended as a materialized scheduling projection: it lets interactive recall
and batch question generation find and order due trackers without replaying
every review or recalculating every tracker during a queue query.

In the current database, however, `nextRecallAt` is not a disposable cache:

- Due-recall and batch-generation queries read `memory_tracker.next_recall_at`
  directly.
- The current schema has no dedicated `next_recall_at` index, so materializing
  the value avoids replay/recalculation but does not by itself guarantee an
  indexed lookup. Indexing is a separate performance concern.
- Answered MCQ and spelling prompts normally preserve answer time, correctness,
  optional thinking time, and modern special outcomes.
- The public "Just review" path updates the tracker without creating a
  `recall_prompt`/`quiz_answer` history event.
- Only the user's current spacing configuration is stored; configuration
  changes are not versioned with reviews.
- The scheduler algorithm/version used for each historical update is not
  recorded.
- Removing a tracker from recall changes `lastRecalledAt` without recording a
  graded-recall event.
- After an incorrect answer, the current implementation sets `nextRecallAt` to
  answer time plus twelve hours but does not update `lastRecalledAt`, so the
  current tracker fields cannot derive that schedule through the normal
  `lastRecalledAt + interval` calculation.

Consequently, Doughnut cannot reliably reconstruct the exact current schedule
by replaying durable history or by recalculating from the other current tracker
columns. For existing data, `nextRecallAt` is a non-rebuildable materialized
projection and therefore an operational source of truth by necessity. That
legacy constraint does not make deviation from `nextRecallAt` valid memory
evidence.

## Decision

### Evidence and scheduling are separate

1. Recall outcome and actual elapsed time since the previous graded recall are
   memory evidence.
2. The planned next-recall time is scheduling metadata. Being early or overdue
   is not, by itself, a success or a failure.
3. Scheduling logic must not interpret backlog age or deviation from the due
   time as negative memory evidence.
4. Every state-changing recall path must prospectively preserve the timestamp
   and outcome evidence required by the chosen memory model. Existing history
   is a migration input, not proof that all required evidence was recorded.

### Memory state and the materialized schedule

1. The scheduler's conceptual source of truth is memory state plus recall
   evidence and active scheduling policy. `nextRecallAt` is the materialized
   result used to select due work; it is not itself a memory-strength goal.
2. While current history remains insufficient for reconstruction,
   `nextRecallAt` must be retained and updated transactionally with the memory
   state after every scheduling event. Consumers may continue to treat it as
   the authoritative due-work projection.
3. Code must not infer memory strength from whether the materialized due time
   was met. It may use the field for eligibility, ordering, display, and
   operational scheduling.
4. A future scheduler or schema must make its rebuild boundary explicit. It
   must preserve either:
   - sufficient current scheduler state and active configuration to recompute
     the materialized next-recall time; or
   - a complete review/state-transition history together with the policy and
     configuration versions required for deterministic replay.
5. Until one of those rebuild paths exists and is verified, migrations must
   seed new scheduler state explicitly from the existing tracker snapshot.
   Implementations must not drop, bulk-recompute, or reinterpret historical
   `nextRecallAt` values on the assumption that the answer history is complete.
6. Making the projection rebuildable is desirable but is not a prerequisite
   for fixing the late-success penalty defined by this ADR.

### Correct recall

1. A correct recall must schedule the tracker strictly in the future. An
   answered tracker must not remain due at the answer instant.
2. A correct recall must not reduce the tracker's learned memory strength solely
   because it was early or overdue.
3. A correct overdue recall represents successful retention over a longer
   observed interval. Its memory-strength result must be no worse than the same
   correct recall made at the planned time. Any lateness bonus may be bounded.
4. A correct early recall may produce less growth than an on-time recall because
   it provides weaker evidence, but early timing alone must not reset learning
   or make the tracker immediately due.
5. A sequence of correct recalls separated by meaningful time must make
   observable forward progress toward longer intervals. Backlog alone must not
   trap a tracker in an immediate or daily-recall loop.

### Incorrect recall

1. An incorrect recall is negative memory evidence and may reduce memory
   strength and schedule a shorter relearning interval.
2. The failure penalty is based on the failed outcome, not on how early or late
   the answer was.
3. A failure must not create a permanent trap: subsequent correct recalls must
   be able to restore expanding intervals.
4. Same-session retry behavior and the persisted next-recall time are separate
   concerns. The persisted schedule after grading must be explicit and must not
   accidentally arise from an index floor.

### Recall effort

1. Trustworthy effort evidence, such as thinking time, may make a bounded
   adjustment within a correct or incorrect outcome.
2. Effort must not invert the outcome: a correct answer cannot become an
   effective failure, reset, or immediate reschedule solely because it was slow.
3. Missing or untrustworthy effort data is neutral.

### Configuration and implementation freedom

1. A newly assimilated tracker may initially be due immediately. Once an answer
   has been graded, a zero persisted interval is not allowed.
2. User-configured spacing remains an input to scheduling, subject to these
   safety properties.
3. `forgettingCurveIndex`, the current interval table, exact increments,
   rounding, and any future stability/retrievability model are implementation
   details. Tests enforcing this ADR must assert externally observable schedule
   behavior rather than exact internal index values.
4. This ADR does not require adopting FSRS. A smaller compatible algorithm may
   be implemented first, and the internal model may later be replaced without
   changing these product rules.
5. Persisting `nextRecallAt` remains an allowed and currently required
   optimization. Its storage is not evidence that the due time is the domain
   goal or an input to memory-strength evaluation.

## Black-box acceptance scenarios

These scenarios exercise the public recall-answering behavior and then read the
observable memory-tracker schedule. They control time through the existing test
clock but do not call `ForgettingCurve`, assert exact index values, or encode a
specific formula.

| Scenario | Loose observable assertion | Current behavior |
|---|---|---|
| Very overdue correct recall | After a correct answer several prior intervals late, `nextRecallAt` is after the answer time. | Fails when the index reaches `100` and the interval becomes zero. |
| Correct late versus correct on time | Starting from equivalent histories, the late-correct tracker's next interval is not shorter than the on-time-correct tracker's interval. | Fails because lateness is subtracted from the success adjustment. |
| Busy but correct streak | After several correct recalls made at meaningful multi-day separations, the final interval is longer than the first post-answer interval and every answer schedules a future recall. | Fails because repeated one-day lateness cancels progress and longer lateness resets it. |
| Correct early recall | A correct early answer neither makes the tracker immediately due nor shortens its learned interval solely because it was early. | Protects an existing bottom line. |
| Incorrect versus correct | From equivalent histories, an incorrect answer produces a shorter next interval than a correct answer. | Protects the intended failure penalty. |
| Recovery after failure | After a failure followed by a spaced streak of correct answers, the interval eventually grows beyond the relearning interval. | Protects recoverability. |
| Slow but correct overdue recall | A trustworthy but slow correct answer made overdue still schedules a future recall and is not treated as a reset. | Fails in cases where lateness plus effort reaches the index floor. |

The executable tests should choose representative times sufficient to expose
the property, while avoiding exact expected durations. Comparative assertions
and ordering are preferred over numeric constants.

## Consequences

- Busy users are evaluated on demonstrated recall rather than schedule
  compliance.
- Correct overdue recalls no longer create a positive-feedback workload loop.
- The implementation must distinguish actual elapsed retention time from
  deviation relative to a queue target.
- `nextRecallAt` remains the authoritative due-work projection for current
  data, even though it is conceptually derived state.
- A future rebuildable projection requires additional persisted scheduler state
  or complete, versioned state-transition history; the current answer history
  is insufficient.
- Existing tests that explicitly require late correct answers to lose strength
  or reset to index `100` must be replaced with black-box policy tests.
- Scheduling changes may alter recall workload. Focused simulations and
  production monitoring of interval distributions and recall success rates are
  required after release.
- A future migration away from `forgettingCurveIndex` remains possible because
  the policy contract does not expose it.

## Pros

- Aligns the schedule with evidence about memory rather than user availability.
- Prevents immediate/daily recall traps after correct answers.
- Gives tests a stable product contract without freezing the next algorithm.
- Leaves room for a minimal fix now and a data-fitted scheduler later.

## Cons

- Some overdue correct answers will receive longer intervals than today,
  potentially reducing review frequency.
- Existing index histories were produced under different semantics, so rollout
  behavior needs monitoring even if no data migration is required.
- The policy alone does not choose tuning constants or a target retention rate.

## Prerequisites / Assumptions

- The correctness grade is trustworthy enough to be the primary outcome signal.
- The current answer time and tracker snapshot are available when grading a new
  recall. Complete historical events and an exact previous graded-recall
  timestamp are not assumed to be reconstructable for legacy data.
- Thinking time is optional secondary evidence, not a correctness substitute.
- The initial implementation will preserve and update the existing
  `nextRecallAt` projection rather than attempt an unsafe historical rebuild.
- Detailed tuning will be covered by executable tests, simulation, and observed
  production outcomes rather than added to this ADR.

## Options considered

### Keep the current symmetric early/late penalty

Rejected in this proposal because it conflates queue compliance with memory and
causes the reported correct-answer trap.

### Adopt FSRS immediately

Deferred. FSRS is a credible future implementation, but immediate adoption
would also introduce a new state model, parameters, retention targets,
historical-data fitting, and migration questions. Those are not necessary to
establish or protect the product policy.

### Apply the policy to the existing Doughnut model first

Recommended initial implementation. Use actual elapsed retention time, make
successful timing adjustments non-negative, bound effort effects, and ensure a
positive post-answer interval. Exact mechanics remain outside this ADR.

### Recompute `nextRecallAt` from answer history on demand

Deferred because current history is incomplete and unversioned, and due-work
queries need a directly queryable projection. This may become viable after a
future scheduler persists a complete replay boundary or sufficient current
memory state.

## Related

- Current implementation: `backend/src/main/java/com/odde/doughnut/entities/ForgettingCurve.java`
- Current schedule update: `backend/src/main/java/com/odde/doughnut/entities/MemoryTracker.java`
- Due-work projection queries: `backend/src/main/java/com/odde/doughnut/entities/repositories/MemoryTrackerRepository.java`
- Unrecorded Just Review update path: `backend/src/main/java/com/odde/doughnut/controllers/MemoryTrackerController.java`
- Current late-penalty tests: `backend/src/test/java/com/odde/doughnut/algorithms/SpacedRepetitionEarlyRewardsAndLatePenaltyTest.java`
- Anki answer semantics: <https://docs.ankiweb.net/studying.html#answer-buttons>
- FSRS algorithm and overdue-review behavior: <https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm>
- Reddy et al., *Unbounded Human Learning: Optimal Scheduling for Spaced Repetition*: <https://arxiv.org/abs/1602.07032>
