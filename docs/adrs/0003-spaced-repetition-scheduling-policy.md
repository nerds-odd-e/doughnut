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

Locked policy for Proposed ADR 0003. Humans still own accept.

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
  hours**. Dues that already come from Stability are `nextRecallAt =
  lastRecalledAt + I(r, S)` with requested retention `r` locked at **0.9**
  (`I(0.9, S) = S` in whole hours). After **ordinary incorrect**, due is grade
  time + 12 hours (schedule metadata). A newly assimilated tracker may have
  Stability 0 (due now). After any graded answer, persisted Stability 0 is
  not allowed, except New fail (S=0 + 12h, already in Again Decision).
  Spacing is Stability, not a Settings day list. Live scheduling must not
  walk a spacing-index ladder.
- **Retrievability** is computed from elapsed whole hours and Stability, not stored.
- A recall transition consumes the graded outcome, elapsed time, and that state — never queue lateness.
- **Requested retention** `r` is a **global constant 0.9** — not a Settings
  knob, not in the UI, not persisted. Lapses and a RecallLog are **deferred**
  (see **Deferred**).

### Difficulty on correct recall

Difficulty is persisted memory state in `[1, 10]`. It is shown on the Memory Tracker page (Information card), next to Stability, as the number returned by the API or **N/A** when unset (New / assimilate-only). Harder items gain less Stability on a successful recall. A correct recall also updates Difficulty with the open-FSRS Good-equivalent rule.

A newly assimilated tracker is **New**: Stability 0, Difficulty unset, due now. Assimilation is not a grade. The first real correct recall initializes Difficulty to **5** and Stability to **24** hours (short first interval; 12 hours is a later tweak). Existing trackers that already have positive Stability or a recall count are migrated to Difficulty **5**.

Ordinary correct recall with Stability > 0 updates Stability (and Difficulty) with open-FSRS-6 Good-equivalent rules (own implementation). Locked overdue extra growth still holds.

### Outcome-to-grade compatibility map

Keep Doughnut product outcomes first-class. Do not replace the Tutor 0–5 rubric with Anki Again / Hard / Good / Easy buttons. When memory updates follow an FSRS-shaped engine, map as follows:

| Product | Schedule |
|---------|----------|
| Ordinary correct / Tutor **4** | FSRS-6 Good |
| Tutor **5** | FSRS-6 Easy |
| Tutor **3** | FSRS-6 Hard |
| Tutor **2** | Doughnut exception (80% accumulated S, D unchanged) |
| Ordinary incorrect | FSRS-6 Again memory; due **+12h** |
| Tutor **1** and **0** | Again memory; due from S (0 same as 1; rubric still differs) |
| Confusion | Not a grade; Again-midpoint S; due not later |
| Overlap | No memory change |

Shared commissioned rules and score-specific memory updates follow. Accidental-match detail is in **Accidental-match and overlap transitions**.

### Commissioned learning session feedback

A commissioned memory tracker is graded from Tutor Feedback (score 0–5), not from a recall prompt Doughnut asked. [ADR 0001](./0001-ubiquitous-language.md) defines the vocabulary and [ADR 0005](./0005-commissioned-learning-session-protocol.md) defines what the score means to the Tutor. Shared schedule rules:

- A recorded score is a grade: it counts the recall, sets `lastRecalledAt`, and reschedules the tracker.
- A Session Item that never receives Feedback supplies no graded recall result: its tracker stays unchanged and the item is abandoned with its session.
- Effort is neutral. A Tutor session carries no trustworthy effort measurement.
- A late session does not weaken the result. The score determines the memory-state adjustment; the recorded time advances `lastRecalledAt`.
- Never apply the ordinary incorrect-recall 12-hour retry. After a score, due is from the updated Stability. A commissioned tracker is due only when the learner commissions another Learning Session.
- **New** (Stability 0, Difficulty unset): scores **3**, **4**, and **5** initialize Difficulty to **5** and Stability to **24** hours, matching the first real correct recall — not FSRS first-rating initials. Scores **0**, **1**, and **2** stay Stability 0 and Difficulty unset; due is strictly after the recorded time (24-hour fallback).

Memory updates with Stability > 0:

- **4:** open-FSRS-6 **Good**-equivalent (same as ordinary correct), including overdue extra.
- **5:** open-FSRS-6 **Easy**-equivalent, not a percentage above Good. Easy increment at least as large as Good's plus an extra Easy factor, plus Easy next-D. Next Stability is strictly longer than the same state under score 4. Overdue extra applies.
- **3:** open-FSRS-6 **Hard**-equivalent, not a percentage below Good. Hard increment is the Good increment times an extra Hard factor, plus Hard next-D. Next Stability is at least the current Stability and strictly shorter than the same state under score 4. Overdue extra applies.
- **2:** Doughnut exception, not Hard. Next Stability is the rounded 80% of current Stability (accumulated hours above assimilate 0); Difficulty unchanged. Ignore elapsed time and Retrievability. No overdue extra.
- **1:** open-FSRS-6 **Again** memory (same as ordinary incorrect: post-lapse Stability and Again next-D). Due from Stability, not the 12-hour retry.
- **0:** same schedule as score **1**. Rubric still differs ([ADR 0005](./0005-commissioned-learning-session-protocol.md)). Does not reset Stability to the assimilate initial level.

### Incorrect recall (Again)

Ordinary incorrect recall (MCQ, just review, spelling fail) is FSRS **Again**. Doughnut does not offer Hard or Easy buttons; product outcomes stay.

When Stability is greater than 0, the memory update for Stability is the open-FSRS-6 post-lapse formula from Difficulty, Stability, and Retrievability (elapsed whole hours vs Stability). Ordinary incorrect also updates Difficulty with the open-FSRS-6 Again next-D (harder; clamped to `[1, 10]`). Unset Difficulty on Stability > 0 is treated as **5**. Queue lateness vs `nextRecallAt` is not an input. The due-work projection after an ordinary incorrect recall stays **grade time + 12 hours**: that 12-hour retry is schedule metadata (the current default, not a sacred constant), not the new Stability. There is **no relearning step list**.

A **New** tracker (Stability 0) that fails stays Stability 0, Difficulty unset, and due in 12 hours. Confusion adjustment is not a grade and is not FSRS Again (see **Accidental-match and overlap transitions**). Failure must not permanently trap the tracker; later correct recalls must be able to restore expanding intervals.

### Overdue correct recall: bounded extra growth

A correct recall after more elapsed whole hours than the tracker's
**Stability** must result in a next Stability **strictly longer** than the
same correct recall at elapsed hours equal to that Stability (same
thinking-time input). Extra growth is driven by elapsed time vs Stability
(low Retrievability), not by lateness vs `nextRecallAt`. It is **bounded**:
further delay must not increase the next interval without limit. A linear
lateness bonus is not allowed. Exact increment math is an implementation
detail; policy tests assert the observable next interval in hours.
Tutor Feedback scores **3**, **4**, and **5** inherit this extra. Score **2** does
not. Scores **0** and **1** use Again memory, not this extra.

### Whole-hour elapsed-time precision

Use **whole elapsed hours** as the recall-transition time input: duration
between the current recall time and `lastRecalledAt`, discarding any sub-hour
remainder, independent of time zone or calendar day. Morning/afternoon recall
windows make day precision too coarse. When elapsed whole hours are **0** on a
tracker with positive Stability, there is no extra success increment. There is
no calendar same-day rule. This Decision does not add a separate same-day
transition; a future short-term behavior may add one without changing this
decision.

### Thinking time

A trustworthy effort measurement (thinking time) may adjust within a
**correct** outcome only, within bounds. It must not invert the outcome.
Missing or untrustworthy effort is neutral.

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
3. The confusion adjustment is not a grade and not FSRS Again. It must stay
   strictly weaker than ordinary incorrect recall. When Stability is greater
   than 0, next Stability is the whole-hour midpoint of current Stability and
   FSRS-6 Again Stability for the same Difficulty (unset Difficulty treated as
   **5**), elapsed whole hours vs `lastRecalledAt`, and current Stability.
   Floor 1 hour. Next Stability must be less than current Stability and
   greater than Again Stability when rounding still distinguishes them.
   Difficulty, `lastRecalledAt`, and `recallCount` are unchanged. Due never
   later (`min(existing due, lastRecalledAt + new Stability)`). Stability 0
   stays 0. It must not count as a failed recall.
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

### Strictly-future fallback (non-positive interval)

After a grade, the tracker must be due strictly after the recorded time. When
the computed interval is non-positive (due would be at or before the grade
instant), schedule **24 hours** after the recorded time — the same first-success
Stability. Do not use the spacing-index ladder as this fallback.

### Manual and admin paths

`mark-as-recalled` is a grade and follows recall-transition rules. `remove` and
`revive` are not grades.

### Spelling memory tracker

A spelling memory tracker is learner-created ([ADR 0001](./0001-ubiquitous-language.md)).
It does not consume assimilation due or the daily assimilation target (same as
a commissioned tracker). Grading follows the locked accidental-match and overlap
transitions.

### Frequent-failure warning

When a tracker has ≥ 5 incorrect recalls in the last 14 days, Doughnut warns
the learner (`wrongCount`, `threshold`, `periodDays`) and does not change the
schedule or remove the tracker. **Overlap** does not count. Property trackers
name the property. No confirm action.

### Deferred

- **B4:** Lapses (no unused counter)
- **C4:** Just-review Hard / Easy buttons
- **E3:** Fuzz / maximum interval
- **E4:** Fitting / per-user weights
- **E6:** RecallLog

## Working draft

Empty pending accept.

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
- Allows a data-fitted scheduler later without a library lock-in. Requested
  retention is locked at 0.9; other tuning constants remain deferred.

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
  spacing is persisted Stability in whole hours. Requested retention is a
  global constant 0.9, not a Settings knob.
- **Recompute due time from answer history on demand** — deferred: history is
  incomplete; due-work needs a queryable projection.

## Related

- Tracker (pointer + deferred IDs, not a second policy map): [`.planning/research/FSRS-COMPATIBILITY-GAP.md`](../../.planning/research/FSRS-COMPATIBILITY-GAP.md)
- ADR 0001 [ubiquitous language](./0001-ubiquitous-language.md) — **recall** (not FSRS **review**); **recall prompt** / **MCQ** / **just review**; commissioned learning terms; spelling memory tracker
- ADR 0005 [commissioned learning session protocol](./0005-commissioned-learning-session-protocol.md) — what a score means to the Tutor
- Anki answer semantics: <https://docs.ankiweb.net/studying.html#answer-buttons>
- FSRS overdue-recall: <https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm>
- Reddy et al.: <https://arxiv.org/abs/1602.07032>
