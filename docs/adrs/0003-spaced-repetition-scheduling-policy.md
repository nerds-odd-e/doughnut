# 0003 — Spaced-repetition scheduling policy

**Status:** Proposed  
**Date:** 2026-08-05  
**Decision makers:** Terry Yin (approval pending advice)  
**Consulted:** To be filled by the decision maker

## Context

The planned recall time is a queue target. Missing it can reflect
availability, queue size, MCQ readiness, or system behavior — not that the
learner forgot. A memory-state transition uses the recall outcome and the
actual elapsed time since the previous state-changing recall.

Established schedulers (including FSRS) separate memory-state inputs from
schedule compliance: successful overdue recall is not a failure. This ADR
states Doughnut's durable scheduling policy and safety properties. It does
not select a formula, model, or constants.

Today's due-work projection (`nextRecallAt`) cannot always be rebuilt from
answer history alone. That operational constraint does not make meeting or
missing the due time an input to the memory-state transition.

## Decision

Finalized choices live here. Move items from **Working draft** into this section as they are locked.

### Recall, not FSRS "review"

Keep **recall**. FSRS **review** is the same activity; Doughnut names it recall
because **recall is better than review**. Glossary: [ADR 0001](./0001-ubiquitous-language.md).
Do not use **review** for the spaced activity. **Just review** is a recall method,
not FSRS review. When citing FSRS, pair once (**review (FSRS) = recall**) then use Doughnut terms.

### Open FSRS-compatible shape, own implementation

The memory-state shape is **open-FSRS-compatible**. Doughnut **implements
that shape itself**. Do not take a dependency on `ts-fsrs`, `fsrs-rs`, or
any other FSRS library. Compatibility is with the open FSRS model (inputs,
state, qualitative update rules), not with a particular crate or version.

- **Stability** is persisted memory state: the current interval in **whole
  hours**. After a grade, `nextRecallAt = lastRecalledAt + stability`. A
  newly assimilated tracker may have Stability 0 (due now). After any graded
  answer, persisted Stability 0 is not allowed. Spacing is Stability, not a
  Settings day list.
- **Retrievability** is computed from elapsed whole hours and Stability, not stored.
- A recall transition consumes the graded outcome, elapsed time, and that state — never queue lateness.
- Requested retention (turning Stability into an interval from a
  retention target), lapses, and a RecallLog remain later gaps. Close one
  remaining gap at a time as **one observable schedule behavior**. Persist a
  field when that behavior uses it.

### Difficulty on correct recall

Difficulty is persisted memory state in `[1, 10]`. It is shown on the Memory Tracker page (Information card), next to Stability, as the number returned by the API or **N/A** when unset (New / assimilate-only). Harder items gain less Stability on a successful recall. A correct recall also updates Difficulty with the open-FSRS Good-equivalent rule.

A newly assimilated tracker is **New**: Stability 0, Difficulty unset, due now. Assimilation is not a grade. The first real correct recall initializes Difficulty to **5** and Stability to **24** hours (short first interval; 12 hours is a later tweak). Existing trackers that already have positive Stability or a recall count are migrated to Difficulty **5**.

Ordinary correct recall with Stability > 0 updates Stability (and Difficulty) with open-FSRS-6 Good-equivalent rules (own implementation). It must not walk a spacing-index ladder. Locked overdue extra growth still holds. Requested retention remains implicit: `nextRecallAt = lastRecalledAt + stability`.

### Incorrect recall (Again)

Ordinary incorrect recall (MCQ, just review, spelling fail) is FSRS **Again**. Doughnut does not offer Hard or Easy buttons; product outcomes stay.

When Stability is greater than 0, the memory update for Stability is the open-FSRS-6 post-lapse formula from Difficulty, Stability, and Retrievability (elapsed whole hours vs Stability). Ordinary incorrect also updates Difficulty with the open-FSRS-6 Again next-D (harder; clamped to `[1, 10]`). Unset Difficulty on Stability > 0 is treated as **5**. Queue lateness vs `nextRecallAt` is not an input. The due-work projection after an ordinary incorrect recall stays **grade time + 12 hours**: that 12-hour retry is schedule metadata (the current default, not a sacred constant), not the new Stability.

A **New** tracker (Stability 0) that fails stays Stability 0, Difficulty unset, and due in 12 hours. Confusion adjustment and commissioned scores stay on their current rules.

### Overdue correct recall: bounded extra growth

A correct recall after more elapsed whole hours than the tracker's
**Stability** must result in a next Stability **strictly longer** than the
same correct recall at elapsed hours equal to that Stability (same
thinking-time input). Extra growth is driven by elapsed time vs Stability
(low Retrievability), not by lateness vs `nextRecallAt`. It is **bounded**:
further delay must not increase the next interval without limit. A linear
lateness bonus is not allowed. Exact increment math is an implementation
detail; policy tests assert the observable next interval in hours.
Commissioned Tutor scores stay score-driven (Working draft) and do not
inherit this extra until a later Decision says they do.

### Whole-hour elapsed-time precision

Use **whole elapsed hours** as the recall-transition time input: duration
between the current recall time and `lastRecalledAt`, discarding any sub-hour
remainder, independent of time zone or calendar day. Morning/afternoon recall
windows make day precision too coarse. This Decision does not add a separate
same-day transition; a future short-term or Difficulty behavior may add one
without changing this decision.

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
   It reduces the selected tracker's Stability and recomputes its due-work
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
adjustment; schedule fields stay unchanged. Retry in session with a more specific answer.

## Working draft

### Spelling memory tracker

A spelling memory tracker is learner-created ([ADR 0001](./0001-ubiquitous-language.md)).
It does not consume assimilation due or the daily assimilation target (same as
a commissioned tracker). Grading follows the locked accidental-match and overlap
transitions.

### Graded outcomes

Correct, incorrect, accidental match, and overlap are distinct graded outcomes
where the product defines them. Scheduling must follow the outcome — not collapse
special spelling results into boolean correct/incorrect.

#### Correct recall

1. Schedule the tracker strictly in the future; it must not remain due at the
   answer instant.
2. Do not reduce Stability solely because the answer was early or overdue.
   Overdue extra growth is locked in Decision; the floor remains: no worse than
   on-time.
3. A correct early recall may grow less than an on-time recall because it
   demonstrates retention over a shorter interval, but must not reset learning
   or make the tracker immediately due.
4. A sequence of correct recalls separated by meaningful time must show forward
   progress toward longer intervals. Backlog alone must not trap a tracker in
   an immediate or daily-recall loop.

#### Incorrect recall

1. Incorrect recall may reduce Stability and shorten the next interval.
2. The penalty is based on the failed outcome, not on earliness or lateness.
3. Failure must not permanently trap the tracker; later correct recalls must be
   able to restore expanding intervals.
4. Same-session retry and the persisted next-recall time are separate. The
   post-grade schedule must be explicit.

### Frequent-failure warning

When a tracker has ≥ 5 incorrect recalls in the last 14 days, Doughnut warns
the learner (`wrongCount`, `threshold`, `periodDays`) and does not change the
schedule or remove the tracker. **Overlap** does not count. Property trackers
name the property. No confirm action.

### Commissioned learning session feedback

A commissioned memory tracker is graded from Tutor Feedback (score 0–5), not
from a recall prompt Doughnut asked. ADR 0001 defines the vocabulary and ADR
0005 defines what the score means to the Tutor; this section defines the
schedule.

1. A recorded score drives a memory-state transition of the same standing as a
   graded recall answer: recording it counts the recall, sets the last-recalled
   time, and reschedules the tracker.
2. Scores move Stability as follows, where **accumulated Stability** means
   hours above the initial level of a newly assimilated tracker:

| Score | Learner demonstrated | Stability result |
|-------|----------------------|------------------|
| 5 | Mastery with full fluency | Successful recall, growth 20% above the standard increment |
| 4 | Mastery with fluency | Successful recall, standard growth |
| 3 | Mastery, but not fluent | Successful recall, growth 20% below the standard increment |
| 2 | Needed a reminder at first, then showed signs of mastery | No growth; accumulated Stability reduced by 20% |
| 1 | Needed several reminders | No growth; accumulated Stability reduced by 50% |
| 0 | Could not reach the learning point even with help | Accumulated Stability reset to the initial level |

3. Demonstrated mastery always moves forward. Scores 3 through 5 grow Stability,
   so a learner who masters a learning point without ever becoming fluent still
   earns lengthening intervals rather than decaying toward permanent due work.
4. Reductions apply to accumulated Stability, so a tracker already at the initial
   level cannot fall below it.
5. Resetting Stability must never leave a tracker due at the instant its score
   was recorded.
6. Otherwise schedule the next recall from the updated Stability. Do not apply
   the incorrect-recall relearning override: a commissioned tracker is due only
   when the learner commissions another Learning Session, so a short forced
   retry window would express nothing.
7. A Tutor session carries no trustworthy effort measurement, so effort is
   neutral.
8. A late session does not weaken the result. The score determines the
   memory-state adjustment; the recorded time advances `lastRecalledAt`.
9. A Session Item that never receives Feedback supplies no graded recall result:
   its tracker stays unchanged and the item is abandoned with its session.

### Recall effort

A trustworthy effort measurement (e.g. thinking time) may adjust within a
correct or incorrect outcome, within bounds. It must not invert the outcome.
Missing or untrustworthy effort data is neutral.

## Consequences

- Busy users are judged on demonstrated recall, not schedule compliance.
- Aligns memory-state transitions with recall results rather than availability,
  and prevents immediate/daily traps after correct answers.
- Some overdue correct answers will get longer intervals than today; existing
  trackers were scheduled under different semantics — monitor interval lengths
  and success rates after release.
- Tutor Feedback is a grading source alongside Doughnut's own recall prompts
  (and just review), and is the first place this policy quantifies an
  adjustment.
- While history is incomplete, the due-time projection stays operationally
  authoritative. A rebuildable projection needs additional scheduler state or
  complete versioned history.
- Allows a data-fitted scheduler later without a library lock-in. The policy
  does not choose tuning constants or a target retention rate.

## Prerequisites / Assumptions

- Graded outcomes are trustworthy enough to be the primary scheduling signal.
- Answer time and the current tracker snapshot are available when grading;
  complete historical events are not assumed reconstructable.
- Thinking time is an optional secondary input, not a correctness substitute.
- Work preserves the existing due-time projection rather than an unsafe
  historical rebuild.

## Options considered

- **Keep a symmetric early/late penalty** — rejected: conflates queue
  compliance with memory.
- **Adopt an open-FSRS library** — rejected: Doughnut owns the implementation.
- **Name the FSRS-compatible shape without requiring a library** — accepted
  (Decision above).
- **Overdue correct equals on-time increment only** — rejected as the
  destination; the extra is required.
- **Linear lateness bonus (SM-2-style)** — rejected: extra must converge and
  follow elapsed time vs Stability, not `nextRecallAt`.
- **Settings day list as the interval source** — rejected as the destination;
  spacing is persisted Stability in whole hours. Requested retention remains a
  later gap.
- **Recompute due time from answer history on demand** — deferred: history is
  incomplete; due-work needs a queryable projection.

## Related

- Working discussion (code-vs-FSRS analysis + open issues): [`.planning/research/FSRS-COMPATIBILITY-GAP.md`](../../.planning/research/FSRS-COMPATIBILITY-GAP.md) — do not duplicate open issues here; move resolved items into **Decision**
- ADR 0001 [ubiquitous language](./0001-ubiquitous-language.md) — **recall** (not FSRS **review**); **recall prompt** / **MCQ** / **just review**; commissioned learning terms; spelling memory tracker
- ADR 0005 [commissioned learning session protocol](./0005-commissioned-learning-session-protocol.md) — what a score means to the Tutor
- Anki answer semantics: <https://docs.ankiweb.net/studying.html#answer-buttons>
- FSRS overdue-recall: <https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm>
- Reddy et al.: <https://arxiv.org/abs/1602.07032>
