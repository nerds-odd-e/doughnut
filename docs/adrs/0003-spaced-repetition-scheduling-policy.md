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
  hours**. After any grade, `nextRecallAt = lastRecalledAt + I(r, S)` with
  requested retention `r` locked at **0.9** (`I(0.9, S) = S` in whole hours).
  When `I` is non-positive, due is 24 hours after the grade (strictly-future
  fallback). A **New** tracker has Stability 0 (due now).
  First mapped grades on New use first-rating (see **First rating on New**);
  due then comes from `I`, not from the 24-hour fallback. Spacing is Stability,
  not a Settings day list. Live scheduling must not walk a spacing-index ladder.
- **Retrievability** is computed from elapsed whole hours and Stability, not stored.
- A recall transition consumes the graded outcome, elapsed time, and that state — never queue lateness.
- **Requested retention** `r` is a **global constant 0.9** — not a Settings
  knob, not persisted, and not otherwise configurable. It may be shown
  read-only in recall statistics (e.g. the heatmap color anchor). There is
  **no lapse count** (see **Lapses**). Memory-state transitions are a
  **RecallLog** (see **RecallLog**).
- **Maximum interval** is a **global constant 36500 days** (open FSRS
  `S_MAX`), compared and persisted as **876000 whole hours** — not a Settings
  knob, not persisted as its own field, and not otherwise configurable. It is
  not shown as its own UI; Memory Tracker still shows Stability (migrated
  rows may drop). After next Stability is computed (FSRS update, thinking-time
  overlay on Good, Tutor **2** shrink, confusion midpoint), on every write of
  next Stability: `S = min(S, 876000)`. Due follows from that S
  (`nextRecallAt = lastRecalledAt + I(0.9, S)`). Do not keep an unbounded S
  beside a capped due. Same strictly-future fallback when `I` is
  non-positive. Existing over-cap rows **will** be clamped (S and due).
  There is **no interval fuzz** (see **Fuzz**).

### Lapses

There is **no lapse count**. Memory state is Difficulty, Stability, and
computed Retrievability. Do not persist, display, or glossary a lifetime
forget counter. Open FSRS-6 After-Again Stability (the published
**post-lapse** formula) consumes Difficulty, Stability, and Retrievability —
not a count. Again outcomes stay on **RecallLog**. The **frequent-failure
warning** is the product signal for repeated incorrect recall; it does not
change the schedule.

### Fuzz

There is **no interval fuzz**. After a grade, due is `lastRecalledAt +
I(0.9, S)` in whole hours (strictly-future fallback when `I` is
non-positive). Do not jitter Stability or due. Open FSRS may randomize the
scheduled interval to spread same-calendar-day clumps; Doughnut already
spreads dues because they are anchored to the actual recall instant in
whole hours. Thinking time may still adjust Stability on ordinary Good;
that is an effort overlay, not random `I`. Session order among already-due
items is not a memory-state concern. Fuzz is not a Settings knob.

### Difficulty on correct recall

Difficulty is persisted memory state in `[1, 10]`. It is shown on the Memory Tracker page (Information card), next to Stability, as the number returned by the API or **N/A** when unset (New / assimilate-only). Harder items gain less Stability on a successful recall. A correct recall also updates Difficulty with Good next-D (see **Difficulty after a mapped grade**).

A **New** tracker ([ADR 0001](./0001-ubiquitous-language.md)) has Stability 0, Difficulty unset / **N/A**, due now. The first mapped grade initializes Stability and Difficulty with FSRS-6 first-rating (see **First rating on New**). Difficulty **5** remains only as the fallback when Stability > 0 and Difficulty is null.

Ordinary correct recall with Stability > 0 updates Stability with open-FSRS-6 Good-equivalent rules (own implementation) and Difficulty with Good next-D. Locked overdue extra growth still holds.

### First rating on New

First mapped **success** on a New tracker (ordinary correct / just review Yes / Tutor **4** Good, Tutor **3** Hard, Tutor **5** Easy) uses published FSRS-6 first-rating (own implementation, frozen `Fsrs.W`):

- Stability `S0(G) = w[G−1]` days, persisted as whole hours. With frozen weights: Good **55**, Hard **31**, Easy **199**.
- Difficulty is `D0(G)` clamped to `[1, 10]`, persisted as the Java float from that formula (API number, no extra rounding). Persisted first Easy Difficulty is **1**.
- `D0(Easy)` stays **unclamped** as the later mean-reversion target (see **Difficulty after a mapped grade**).
- Elapsed time and thinking time do **not** change first-rating. Overdue extra does not apply.
- Due is `lastRecalledAt` plus those hours.

First Again / Tutor **0/1** on New uses the same first-rating path with `G=1`: Stability `S0(1)` (**5** hours), Difficulty `D0(1)` (Java float), due `lastRecalledAt + I` (**5h**).

Tutor **2** on New uses the same first-rating path with `G=2`: Stability `S0(2)` (**31** hours), Difficulty `D0(2)` (Java float), due `lastRecalledAt + I` (**31h**) — same first bucket as Tutor **3**. Shrink 80% remains the exception only when `S > 0`.

Going-forward New has no memory-state grade: every mapped grade uses first-rating (all four G, including Tutor **2** Hard) and the tracker is no longer New. Still-New rows with `AGAIN` / `AGAIN_ZERO` RecallLogs **will** be backfilled to `S0(1)` / `D0(1)`, due from `I`. Still-New rows with `SHRINK` RecallLogs **will** be backfilled to Hard first-rating (`S0(2)` / `D0(2)`, due +31h). First-success rows with `S > 0` stay unrestored.

The 24-hour strictly-future fallback is for non-positive `I`, not a New first-rating interval.

### Difficulty after a mapped grade

When Stability > 0, a mapped grade updates Difficulty with published open FSRS-6 next Difficulty (own implementation, frozen `Fsrs.W`):

- `ΔD = -w6 · (G − 3)`
- `D' = D + ΔD · (10 − D) / 9`
- `D'' = w7 · D0(Easy) + (1 − w7) · D'`, then clamp to `[1, 10]`
- `D0(G) = w4 − e^{w5·(G−1)} + 1`; **`D0(Easy)` is unclamped** `D0(4)` (negative with default weights)

`G`: Again=1, Hard=2, Good=3, Easy=4 (`Fsrs.AGAIN` / `HARD` / `GOOD` / `EASY`). Existing persisted Difficulty on already-graded `S > 0` rows is **not** backfilled. New first-rating uses clamped `D0(G)` (see **First rating on New**). Tutor **2** on New uses `D0(2)`; when `S > 0`, Tutor **2** does not change Difficulty. Confusion does not change Difficulty. Display is the API number (no extra rounding).

### Outcome-to-grade compatibility map

Keep Doughnut product outcomes first-class. Do not replace the Tutor 0–5 rubric with Anki Again / Hard / Good / Easy buttons. When memory updates follow an FSRS-shaped engine, map as follows:

| Product | Schedule |
|---------|----------|
| Just review Yes / ordinary correct / Tutor **4** | FSRS-6 Good |
| Tutor **5** | FSRS-6 Easy |
| Tutor **3** | FSRS-6 Hard |
| Tutor **2** | On New: FSRS-6 Hard first-rating (`S0(2)` / `D0(2)`); when `S > 0`: Doughnut exception (80% accumulated S, D unchanged) |
| Just review No / ordinary incorrect | FSRS-6 Again memory; due from `I` (non-positive `I` → 24h) |
| Tutor **1** and **0** | Again memory; due from S (0 same as 1; rubric still differs) |
| Confusion | Not a grade; Again-midpoint S; due not later |
| Overlap | No memory change |

Shared commissioned rules and score-specific memory updates follow. Accidental-match detail is in **Accidental-match and overlap transitions**.

### Just review

Just review stays **two buttons**. **Yes, I remember** is ordinary correct (Tutor **4**, FSRS-6 Good). **No, I need more recall** is ordinary incorrect (Tutor **1**, FSRS-6 Again). Do not add Hard or Easy. Tutor **3** and **5** stay commissioned-only.

### Commissioned Learning Session feedback

A commissioned memory tracker is graded from Tutor Feedback (score 0–5), not from a recall prompt Doughnut asked. [ADR 0001](./0001-ubiquitous-language.md) defines the vocabulary and [ADR 0005](./0005-commissioned-learning-session-protocol.md) defines what the score means to the Tutor. Shared schedule rules:

- A recorded score is a grade: it counts the recall, sets `lastRecalledAt`, and reschedules the tracker.
- A Session Item that never receives Feedback supplies no graded recall result: its tracker stays unchanged and the item is abandoned with its session.
- Effort is neutral. A Tutor session carries no trustworthy effort measurement.
- A late session does not weaken the result. The score determines the memory-state adjustment; the recorded time advances `lastRecalledAt`.
- After a score, due is `lastRecalledAt + I(0.9, S)`; non-positive `I` → 24h. A commissioned tracker is due only when the learner commissions another Learning Session.
- **New** (Stability 0, Difficulty unset): scores **3**, **4**, and **5** use FSRS-6 first-rating (see **First rating on New**). Scores **0** and **1** use first-rating Again (`S0(1)` / `D0(1)`, due from `I`, **5h**). Score **2** uses Hard first-rating (`S0(2)` / `D0(2)`, due from `I`, **31h**).

Memory updates with Stability > 0:

- **4:** open-FSRS-6 **Good**-equivalent (same as ordinary correct), including overdue extra and Good next-D (see **Difficulty after a mapped grade**).
- **5:** open-FSRS-6 **Easy**-equivalent, not a percentage above Good. Easy increment at least as large as Good's plus an extra Easy factor, plus Easy next-D (see **Difficulty after a mapped grade**). Next Stability is strictly longer than the same state under score 4. Overdue extra applies.
- **3:** open-FSRS-6 **Hard**-equivalent, not a percentage below Good. Hard increment is the Good increment times an extra Hard factor, plus Hard next-D (see **Difficulty after a mapped grade**). Next Stability is at least the current Stability and strictly shorter than the same state under score 4. Overdue extra applies.
- **2:** Doughnut exception, not Hard. Next Stability is the rounded 80% of current Stability (accumulated hours above assimilate 0); Difficulty unchanged. Ignore elapsed time and Retrievability. No overdue extra.
- **1:** open-FSRS-6 **Again** memory (same as ordinary incorrect: post-lapse Stability and Again next-D; see **Difficulty after a mapped grade**). Due from `I`.
- **0:** same schedule as score **1**. Rubric still differs ([ADR 0005](./0005-commissioned-learning-session-protocol.md)). Does not reset Stability to the assimilate initial level.

### Incorrect recall (Again)

Ordinary incorrect recall (MCQ, just review No, spelling fail) is FSRS **Again**.

When Stability is greater than 0, the memory update for Stability is the open-FSRS-6 post-lapse formula from Difficulty, Stability, and Retrievability (elapsed whole hours vs Stability). Ordinary incorrect also updates Difficulty with Again next-D (see **Difficulty after a mapped grade**). Unset Difficulty on Stability > 0 is treated as **5**. Queue lateness vs `nextRecallAt` is not an input. After ordinary incorrect, due is `lastRecalledAt + I(0.9, S)` of the post-lapse Stability; non-positive `I` → 24h. There is **no relearning step list**.

A **New** tracker (Stability 0) that fails uses first-rating Again: Stability `S0(1)` (**5**), Difficulty `D0(1)` (Java float), due `lastRecalledAt + I` (**5h**); see **First rating on New**. Confusion adjustment is not a grade and is not FSRS Again (see **Accidental-match and overlap transitions**). Failure must not permanently trap the tracker; later correct recalls must be able to restore expanding intervals.

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
windows make day precision too coarse. There is no calendar same-day rule.
When elapsed whole hours are **0** and Stability is greater than 0, success
grades (ordinary correct / Tutor **4** Good, Tutor **3** Hard, Tutor **5** Easy)
update Stability with published open FSRS-6 short-term next Stability (own
implementation, frozen `Fsrs.W`): convert persisted whole hours to days,
`S' = S · e^{w17 · (G − 3 + w18)} · S^{-w19}`, then persist whole hours.
Clamp **SInc ≥ 1** so next Stability does not shrink. With frozen weights
and rounding: Good 24→**25**; Easy 24→**43**; Hard 24 stays **24** (clamp);
Good at 72 hours stays **72** (clamp). Again at elapsed 0 stays post-lapse.
Tutor **2** and confusion are unchanged. New (Stability 0) first-rating is
unchanged by elapsed time (see **First rating on New**). The short-term success
rule is not a Settings knob. Elapsed whole hours **≥ 1** still use long-term
next Stability.

### Thinking time

A trustworthy effort measurement (thinking time) may adjust within a
**correct** outcome only, within bounds. It must not invert the outcome.
Missing or untrustworthy effort is neutral. Thinking time does not change
first-rating on New (see **First rating on New**).

### Accidental-match and overlap transitions

An **accidental match** is a spelling answer that fails the note under recall
but names another accessible note by title or plain alias. Unless the notes have
a declared overlap, the answer has the following consequences:

1. The spelling tracker under recall receives the ordinary incorrect-recall
   transition, including its full negative memory-state adjustment and due.
   It advances `lastRecalledAt` and `recallCount` and counts as a failed
   recall for that tracker.
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
   later (`min(existing due, lastRecalledAt + I(0.9, S))`). Stability 0
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
instant), schedule **24 hours** after the recorded time. This fallback is not
first-rating Stability. Do not use the spacing-index ladder as this fallback.

### Manual and admin paths

`mark-as-recalled` is just review's grade path: successful is just review Yes
(Good); unsuccessful is just review No (Again). `remove` and `revive` are not
grades.

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

### RecallLog

Each memory-state transition is a **RecallLog**
([ADR 0001](./0001-ubiquitous-language.md)). Tutor Feedback is a log row, not a
session bag.

A `recall_log` has `memory_tracker_id`, `recorded_at`, `elapsed_hours`
(nullable on backfill), `product_outcome`, and optional `answer_id`.

A row has `answer_id` xor none: prompt grade and confusion set `answer_id`;
just review and Tutor Feedback set none. Do not store `recall_prompt_id`
(redundant with `answer` → `recall_prompt`).

Do not store FSRS G, Retrievability, `I`, or pre/post Stability/Difficulty.
Current Stability and Difficulty stay on `memory_tracker`. `next_recall_at`
stays the due-work index.

`product_outcome`: `GOOD` | `EASY` | `HARD` | `SHRINK` | `AGAIN` | `AGAIN_ZERO`
| `CONFUSION`.

### Deferred

- **E4:** Fitting / per-user weights

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
  retention is locked at 0.9. Maximum interval is locked at 36500 days
  (876000 whole hours). A lapse count is not memory state. There is no
  interval fuzz. Per-user fitting remains deferred.

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
  global constant 0.9, not a Settings knob. Maximum interval is a global
  constant (36500 days / 876000 whole hours), not a Settings knob.
- **Recompute due time from answer history on demand** — deferred: history is
  incomplete; due-work needs a queryable projection.
- **Just review Hard / Easy buttons** — rejected: just review is rare; keep two
  buttons mapped to Tutor **4** and **1**.
- **Persist a lapse count** — rejected: FSRS-6 After-Again Stability does not
  consume it; RecallLog already holds Again history; the frequent-failure
  warning is the product signal. Do not add an unused counter.
- **FSRS interval fuzz** — rejected: due follows Stability; hour-precision
  recall instants already spread same-calendar-day clumps; thinking time is
  an effort overlay on Good, not random `I`. Open FSRS treats fuzz as
  optional (`enable_fuzz` defaults off in ts-fsrs).

## Related

- Tracker (pointer + deferred IDs, not a second policy map): [`.planning/research/FSRS-COMPATIBILITY-GAP.md`](../../.planning/research/FSRS-COMPATIBILITY-GAP.md)
- ADR 0001 [ubiquitous language](./0001-ubiquitous-language.md) — **recall** (not FSRS **review**); **recall prompt** / **MCQ** / **just review**; **New**; **RecallLog**; commissioned Learning Session terms; spelling memory tracker
- ADR 0005 [commissioned learning session protocol](./0005-commissioned-learning-session-protocol.md) — what a score means to the Tutor
- Anki answer semantics: <https://docs.ankiweb.net/studying.html#answer-buttons>
- FSRS overdue-recall: <https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm>
- Reddy et al.: <https://arxiv.org/abs/1602.07032>
