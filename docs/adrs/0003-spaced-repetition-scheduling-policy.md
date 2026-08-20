# 0003 — Spaced-repetition scheduling policy

**Status:** Proposed  
**Date:** 2026-08-05  
**Decision makers:** Terry Yin
**Consulted:** None

## Context

Doughnut's recall schedule follows the **open FSRS-6** model. This ADR
locks that compliance and names every deliberate divergence from
published open FSRS.

## Decision

This ADR's **Spaced repetition glossary** locks DSR and schedule terms.
Doughnut owns its open-FSRS implementation rather than adopting a library.
Product recall language: [ADR 0001](./0001-ubiquitous-language.md). When citing
FSRS, pair once then use Doughnut terms.

### Doughnut vs FSRS terms and card states

In Doughnut, **recall** names the activity that FSRS calls a **review**. The
product prefers “recall” because it describes retrieving a memory more
precisely than “review.” **Just review** is one method of recall.

**Assimilation** is the initial intake action that creates a **New** memory
tracker. Its closest FSRS analogue is creating a **New** card. A Doughnut
**memory tracker** corresponds to an FSRS **card**. Doughnut does not persist
or transition through the FSRS scheduler's **Learning**, **Review**, and
**Relearning** card states, and has no learning or relearning step list.

### Spaced repetition glossary

- **New** — Ungraded memory tracker (`S = 0`, Difficulty unset / **N/A**).
  Created by assimilation and due immediately: `nextRecallAt = assimilatedAt`.
  The first grade initializes Stability and Difficulty, after which the
  tracker is no longer New.
- **Stability** — Persisted current interval of a memory tracker, in
  whole hours.
- **Difficulty** — Persisted memory state in `[1, 10]`.
  Fallback **5** when Stability > 0 and Difficulty is null.
- **Retrievability** — FSRS `R`: predicted probability of recall at
  elapsed time since `lastRecalledAt`, given Stability. Computed during
  long-term grading to calculate next Stability; not stored.
- **Requested retention** — FSRS `request_retention`: desired recall rate
  at due. Locked at **0.9**, so `I(0.9, S) = S`.
- **Scheduled interval** — FSRS `I(r, S)`: interval for requested retention
  `r` and Stability `S`. Scheduling uses whole hours.
- **Maximum interval** — Global, non-persisted FSRS `S_MAX`: **36500 days** =
  **876000 whole hours**. Clamp each computed next Stability to this maximum,
  then derive due from the capped value.
- **`lastRecalledAt`** — Time of the last grade (FSRS
  `last_review`). Unset on New.
- **`assimilatedAt`** — Time the tracker was created by assimilation.
  New due.
- **`nextRecallAt`** — Due time. After a grade,
  `lastRecalledAt + I(0.9, S)`.
- **RecallLog** — One persisted memory-state transition (FSRS-shaped
  review history). Schema: **RecallLog** below.
- **Thinking time** — Duration the prompt measured while the learner
  answered. Recorded on the answer for display; not a memory-state input.
- **Confusion** — Secondary memory-state adjustment on the matched note's
  tracker after an accidental match. Not a grade and not FSRS Again.
- **Overlap** — Declared non-distinguishing spelling outcome. No memory
  change.

### Lapses

There is **no lapse count**; do not persist or display one. Open FSRS-6
After-Again Stability does not consume it. Again outcomes remain on
**RecallLog**; repeated failures surface through the **frequent-failure
warning**, not the memory state. See **Incorrect recall (Again)** for the
post-lapse rule.

### Fuzz

There is **no interval fuzz**: do not jitter Stability or due. Open FSRS may
randomize intervals to spread same-calendar-day clumps; Doughnut's whole-hour
dues are already anchored to each recall instant. Session order among
already-due items is not a memory-state concern.

### Difficulty on correct recall

Harder items gain less Stability on a successful recall. Ordinary correct
recall with Stability > 0 updates Stability with open-FSRS-6 Good
rules (own implementation) and Difficulty with Good next-D (see **Difficulty
after a grade**).

### First rating on New

Every grade on a New tracker uses published FSRS-6 first-rating (own
implementation, frozen `Fsrs.W`), with Grade `G`:

- Stability `S0(G) = w[G−1]` days, persisted as whole hours. With frozen weights: Again **5**, Hard **31**, Good **55**, Easy **199**.
- Difficulty is `D0(G)` clamped to `[1, 10]`, persisted as the Java float from that formula (API number, no extra rounding). Persisted first Easy Difficulty is **1**.
- `D0(Easy)` stays **unclamped** as the later mean-reversion target (see **Difficulty after a grade**).
- Elapsed time does **not** change first-rating; see **RecallLog**.

### Difficulty after a grade

When Stability > 0, a grade updates Difficulty with published open
FSRS-6 next Difficulty (own implementation, frozen `Fsrs.W`), using Grade `G`:

- `ΔD = -w6 · (G − 3)`
- `D' = D + ΔD · (10 − D) / 9`
- `D'' = w7 · D0(Easy) + (1 − w7) · D'`, then clamp to `[1, 10]`
- `D0(G) = w4 − e^{w5·(G−1)} + 1`; **`D0(Easy)` is unclamped** `D0(4)` (negative with default weights)

New first-rating uses clamped `D0(G)` (see **First rating on New**). Confusion
does not change Difficulty. Display the persisted API number without extra
rounding.

### Grade

**Grade** is the single scheduling evaluation concept. Its numeric value **is**
FSRS `G`:

| Grade | G |
|-------|---|
| Again | 1 |
| Hard | 2 |
| Good | 3 |
| Easy | 4 |

Recall prompts, **just review**, and Tutor **Feedback** all submit a Grade.
**Confusion** and **Overlap** are not grades (see **Accidental-match and
overlap transitions**). There is no separate Tutor “score” or Yes/No domain
concept.

### Just review

Just review stays **two buttons**: **Good** and **Again**. Labels are grade
names, not Yes/No. Hard and Easy remain available via commissioned Learning
Sessions (and other graded paths that offer all four), not just review.

### Commissioned Learning Session feedback

A commissioned memory tracker is graded from Tutor Feedback (a **Grade**), not
a Doughnut recall prompt. Vocabulary:
[ADR 0001](./0001-ubiquitous-language.md); Tutor semantics: [ADR
0005](./0005-commissioned-learning-session-protocol.md). Shared rules:

- A recorded Grade at its recorded time counts the recall, sets
  `lastRecalledAt`, and updates and reschedules the tracker by the common rules.
- A Session Item that never receives Feedback supplies no graded recall result: its tracker stays unchanged and the item is abandoned with its session.
- Effort is neutral. A Tutor session carries no trustworthy effort measurement.
- A late session does not weaken the result.
- A commissioned tracker is presented only when the learner commissions
  another Learning Session.
- On New, use **First rating on New**.

Memory updates with Stability > 0:

- **Easy (4):** Good increment times the Easy factor; next Stability is
  strictly longer than the same state under Good. Overdue extra applies.
- **Good (3):** same update as ordinary correct recall; overdue extra applies.
- **Hard (2):** Good increment times the Hard factor; next Stability is at
  least current Stability and strictly shorter than the same state under Good.
  Overdue extra applies.
- **Again (1):** same update as ordinary incorrect recall; see **Incorrect
  recall (Again)**.

All four update Difficulty by **Difficulty after a grade**.

### Incorrect recall (Again)

Ordinary incorrect recall (MCQ, just review Again, spelling fail) is FSRS **Again**.

When Stability is greater than 0, elapsed whole hours **< 24** use the
short-term rule in **Whole-hour elapsed-time precision** with `G = 1`; elapsed
**≥ 24** uses open-FSRS-6 post-lapse Stability. Compute `Sf`, then persist
`S' = min(current S, max(1, round(Sf)))` whole hours. Ordinary incorrect also
uses Again next Difficulty.

On New, Again follows **First rating on New**. Failure must not permanently
trap the tracker; later correct recalls must be able to restore expanding
intervals.

### Overdue correct recall: bounded extra growth

A correct recall after more elapsed whole hours than the tracker's
**Stability** must result in a next Stability **strictly longer** than the
same correct recall at elapsed hours equal to that Stability. Extra growth
is driven by elapsed time vs Stability
(low Retrievability), not by lateness vs `nextRecallAt`. It is **bounded**:
further delay must not increase the next interval without limit. A linear
lateness bonus is not allowed. Exact increment math is an implementation
detail; policy tests assert the observable next interval in hours.
All mapped correct grades inherit this extra; Again does not.

### Whole-hour elapsed-time precision

Recall transitions use the mapped outcome and memory state. Their time input is
**whole elapsed hours** between the current recall time and `lastRecalledAt`,
discarding any sub-hour remainder. Queue lateness vs `nextRecallAt` is not an
input. This is independent of time zone and calendar day; morning/afternoon
recall windows make day precision too coarse.
When elapsed whole hours are **< 24** and Stability is greater than 0, all four
grades update Stability with published open FSRS-6 short-term next
Stability (own implementation, frozen `Fsrs.W`): convert persisted whole hours to days,
`S' = S · e^{w17 · (G − 3 + w18)} · S^{-w19}`, then persist whole hours.
Clamp **SInc ≥ 1** only for **G ≥ 2** so Hard/Good/Easy next Stability does
not shrink. Again (G=1) may shrink. Floor **1 hour**. With frozen weights
and rounding, short-term next Stability: Good **24h** → **25h**; Easy
**24h** → **43h**; Hard **24h** stays **24h** (clamp); Good **72h** stays
**72h** (clamp); same-hour Again after first Good **55h** → **18h**;
same-hour Again on **72h** / D=5 → **24h** (elapsed 0 and 23 are the same
next S). Elapsed whole hours **≥ 24** use long-term next Stability; Again
is then post-lapse (see **Incorrect recall (Again)**): on-time **72h** /
D=5 → **17h**, elapsed **24** → **15h**; on-time after first Good **55h**
→ **15h**; **5h** / D=**1** / elapsed **8760** stays **5h**, not **6h**.
See **Accidental-match and overlap transitions** for confusion and **First
rating on New** for Stability 0. Observable pin:
New → Again
(`S0(1)` = **5h**) → Good at elapsed 5 → short-term next Stability **6h**
(not long-term **21h**).

### Thinking time

Record thinking time on the answer when the prompt measured it. Show it in
recall statistics and Memory Tracker prompt history.

### Accidental-match and overlap transitions

Unless the notes have a declared overlap, an accidental match has the
following consequences:

1. The spelling tracker under recall receives the ordinary incorrect-recall
   transition, including its full negative memory-state adjustment and due.
   It advances `lastRecalledAt` and `recallCount` and counts as a failed
   recall for that tracker.
2. A secondary **confusion adjustment** applies only when the answer matches
   exactly one accessible note and that learner has an eligible active tracker
   for it. Prefer its spelling tracker; otherwise use its note-level
   understanding tracker. Never select a property or commissioned tracker, a
   removed or deleted tracker, or create a tracker implicitly.
3. The confusion adjustment must stay strictly weaker than ordinary incorrect
   recall. When Stability is greater
   than 0, next Stability is the whole-hour midpoint of current Stability and
   FSRS-6 Again Stability for the same Difficulty (using the glossary fallback),
   elapsed whole hours vs `lastRecalledAt`, and current Stability.
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
first-rating Stability. The DSR snapshot may leave a due already past relative
to now (see **DSR snapshot**); do not clamp that due to now.

### Manual and admin paths

`mark-as-recalled` uses the **Just review** Grades (Good / Again). `remove` and `revive` do
not write `lastRecalledAt`.

### Spelling memory tracker

Creation and assimilation-quota semantics are defined by [ADR
0001](./0001-ubiquitous-language.md). Grading follows
**Accidental-match and overlap transitions**.

### Frequent-failure warning

When a tracker has ≥ 5 incorrect recalls in the last 14 days, Doughnut warns
the learner (`wrongCount`, `threshold`, `periodDays`) and does not change the
schedule or remove the tracker. **Overlap** does not count. Property trackers
name the property. No confirm action.

### RecallLog

A `recall_log` has `memory_tracker_id`, `recorded_at`, `elapsed_hours`,
`product_outcome`, and optional `answer_id`. Tutor Feedback is a log row, not a
session bag.

`elapsed_hours` is always present (whole hours; see **Whole-hour elapsed-time
precision**) and required. The first grade on a tracker is **0**. Later
grades are whole hours since the previous grade's `recorded_at`
on that tracker (`GOOD` / `EASY` / `HARD` / `AGAIN`). `CONFUSION` uses the same
elapsed vs the last grade, else **0**, and is not an anchor. Order is
`recorded_at`, then `id`. A negative diff is **0**.

A row has `answer_id` xor none: prompt grade and confusion set `answer_id`;
just review and Tutor Feedback set none. Do not store `recall_prompt_id`
(redundant with `answer` → `recall_prompt`).

Do not store FSRS G, Retrievability, `I`, or pre/post Stability/Difficulty.
Current Stability, Difficulty, `lastRecalledAt`, and `nextRecallAt` stay on
`memory_tracker` (see **DSR snapshot**). `next_recall_at` stays the due-work
index.

`product_outcome`: `GOOD` | `EASY` | `HARD` | `AGAIN` | `CONFUSION`. Persist
named grades (`GOOD` / `EASY` / `HARD` / `AGAIN` are **Grade**; `CONFUSION` is
not). Latest tutor feedback is that Grade's `G` (**1–4**)
([ADR 0005](./0005-commissioned-learning-session-protocol.md)).

### DSR snapshot

The persisted DSR on `memory_tracker` (`Stability`, `Difficulty`,
`lastRecalledAt`, `nextRecallAt`) is a **cache of folding that tracker's
RecallLog** under this locked policy. Live grading still updates the snapshot
on each grade and on confusion. Do **not** fold the log on every
due-work query.

Fold semantics:

- Fold **every** tracker that has at least one grade (`GOOD` / `EASY` /
  `HARD` / `AGAIN`), from New, in `recorded_at`, then `id` order. Use **stored**
  `elapsed_hours`. The first grade is first-rating.
- **Leave** New, confusion-only, and `S > 0` with no mapped-grade log.
  Include **removed-from-tracking**. Skip `deleted_at IS NOT NULL`.
- Write Stability, Difficulty, `lastRecalledAt` (last mapped `recorded_at`),
  and `nextRecallAt` by the common scheduled-interval and fallback rules.
  **Past due is allowed**; do not clamp to now.
- Fold confusion by **Accidental-match and overlap transitions**.

### Deferred

- **E4:** Fitting / per-user weights

## Consequences

- Busy users are judged on demonstrated recall, not schedule compliance.
- Aligns memory-state transitions with recall results rather than availability,
  and prevents immediate/daily traps after correct answers.
- Overdue correct recall lengthens Stability more than on-time correct recall
  at the same Stability; monitor interval lengths and success rates.
- Tutor Feedback is a grading source alongside Doughnut's own recall prompts
  and just review.
- Due-work stays a snapshot read. Some trackers may be honestly past due
  (see **DSR snapshot**).
- Allows a data-fitted scheduler later without a library lock-in; per-user
  fitting remains deferred.

## Prerequisites / Assumptions

- Graded outcomes are trustworthy enough to be the primary scheduling signal.
- Answer time and the current tracker snapshot are available when grading;
  mapped-grade RecallLog rows are sufficient to rebuild the **DSR snapshot**.

## Options considered

- **Overdue handling** — symmetric early/late penalties, on-time-only growth,
  and linear lateness bonuses were rejected. They conflate queue compliance
  with memory or miss the bounded effect of elapsed time vs Stability.
- **Implementation** — Doughnut owns the FSRS-compatible implementation;
  adopting an open-FSRS library was rejected.
- **Rebuild DSR from RecallLog on every due-work query** — rejected: due-work
  needs a queryable snapshot; do not fold at query time.
- **Tutor rubric** — Grades with numeric values **1–4 = G** were selected; a
  shifted 0–5 rubric was rejected.
- **Just review Hard / Easy buttons** — rejected: just review is rare; keep
  **Good** and **Again** only.
- **Persist a lapse count** — rejected: scheduling does not consume it;
  RecallLog and the frequent-failure warning already represent the history.
- **FSRS interval fuzz** — rejected: due follows Stability; hour-precision
  recall instants already spread same-calendar-day clumps.
- **RT as Stability input** — rejected: thinking time is not a DSR input
  (G, elapsed time, D/S/R only); record it for display instead.
- **FSRS card states or last recall at assimilation** — rejected: New remains
  ungraded until first-rating; there is no step list or card-state column.
- **Calendar same-day short-term window** — rejected in favor of whole elapsed
  hours.
- **After-Again cap** — post-lapse `S'` cannot exceed current Stability;
  extending that cap to the short-term rule was rejected.

## Related

- Tracker (pointer + deferred IDs, not a second policy map): [`.planning/research/FSRS-COMPATIBILITY-GAP.md`](../../.planning/research/FSRS-COMPATIBILITY-GAP.md)
- ADR 0001 [ubiquitous language](./0001-ubiquitous-language.md) — notes, assimilation, recall, tracker types, commissioned Learning Session terms. This ADR is the **Spaced repetition glossary**.
- ADR 0005 [commissioned learning session protocol](./0005-commissioned-learning-session-protocol.md) — what a Grade means to the Tutor
- Anki answer semantics: <https://docs.ankiweb.net/studying.html#answer-buttons>
- FSRS overdue-recall: <https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm>
- Reddy et al.: <https://arxiv.org/abs/1602.07032>
