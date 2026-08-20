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
Product recall language: [ADR 0001](./0001-ubiquitous-language.md). When
citing FSRS, pair once then use Doughnut terms.

### Spaced repetition glossary

- **Confusion** — Secondary memory-state adjustment on the matched note's
  tracker after an accidental match. Not a grade and not FSRS Again.
- **Overlap** — Declared non-distinguishing spelling outcome. No memory
  change.
- **New** — Ungraded memory tracker (`S = 0`, Difficulty unset / **N/A**).
  Assimilation and confusion are not grades. Not “never succeeded.” After
  any mapped grade the tracker is no longer New. Due at `assimilatedAt`.
  No FSRS Learning / Review / Relearning card-state machine.
- **Stability** — Persisted current interval of a memory tracker, in
  whole hours. Short UI: **Stability**.
- **Difficulty** — Persisted memory state in `[1, 10]`. Shown on the
  Memory Tracker Information card as the API number, or **N/A** when
  unset. Fallback **5** when Stability > 0 and Difficulty is null.
- **Retrievability** — FSRS `R`: predicted probability of recall at
  elapsed time, from Stability. Computed at long-term grade for next-S;
  not stored.
- **Requested retention** — FSRS `request_retention`: desired recall rate
  at due. Not Retrievability. Locked at **0.9**, so `I(0.9, S) = S`. May
  be shown read-only as the color hinge of the observed-recall-rate
  heatmap.
- **`lastRecalledAt`** — Time of the last mapped grade (FSRS
  `last_review`). Unset on New.
- **`assimilatedAt`** — Time the tracker was created by assimilation.
  New due.
- **`nextRecallAt`** — Due time. After a grade,
  `lastRecalledAt + I(0.9, S)`.
- **RecallLog** — One persisted memory-state transition (FSRS-shaped
  review history). Schema: **RecallLog** below.
- **Thinking time** — Duration the prompt measured while the learner
  answered. Recorded on the answer for display; not a memory-state input.

### Doughnut vs FSRS terms

Product outcomes → G: **Outcome-to-grade compatibility map**.

| Doughnut                | Open FSRS           | Notes                                           |
| ----------------------- | ------------------- | ----------------------------------------------- |
| **Recall**              | **Review**          | Because "'recall' is better than 'review'" .    |
| **Just review**         |                     | A method of recall                              |
| **Assimilation**        | **New** card        | Introduce, ungraded                             |
| **New**                 | **New**             | Ungraded; no Learning / Review / Relearning     |
| **Memory tracker**      | Card                |                                                 |
| **Stability**           | Stability `S`       | Whole hours                                     |
| **Difficulty**          | Difficulty `D`      |                                                 |
| **Retrievability**      | Retrievability `R`  | Computed, not stored                            |
| **Requested retention** | `request_retention` | Desired recall rate at due; not `R`; locked 0.9 |
| `lastRecalledAt`        | `last_review`       |                                                 |
| `assimilatedAt`         |                     | New due                                         |
| `nextRecallAt`          | Due                 | `I(r, S)`                                       |
| **RecallLog**           | Review history      |                                                 |
| First mapped grade      | First-rating        | `S0(G)` / `D0(G)`                               |
| Tutor score **1–4**     | Grade `G`           |                                                 |
| **Confusion**           |                     | Doughnut outcome; not a grade                   |
| **Overlap**             |                     | Doughnut outcome; no memory change              |

### Open FSRS-compatible shape, own implementation

Doughnut implements the open FSRS model itself (inputs, state, qualitative
update rules), not `ts-fsrs`, `fsrs-rs`, or another library.

After a grade, when last recall exists, due is `lastRecalledAt + I(0.9, S)`.
When `I` is non-positive, due is 24 hours after the grade (strictly-future
fallback). New due is `assimilatedAt`. First mapped grades on New use
first-rating (see **First rating on New**); due then comes from `I`.

A recall transition consumes the graded outcome, elapsed time, and D/S/R —
never queue lateness.

**Maximum interval** is a **global constant 36500 days** (open FSRS
`S_MAX`), compared and persisted as **876000 whole hours**. It is not a
persisted field. After next Stability is computed (FSRS update, confusion
midpoint), on every write of next Stability: `S = min(S, 876000)`. Due
follows from that S. Stability and due stay consistent under the same cap.
There is **no interval fuzz** (see **Fuzz**).

### Lapses

There is **no lapse count**. Do not persist, display, or glossary a lifetime
forget counter. Open FSRS-6 After-Again Stability does not consume a count
(see **Incorrect recall (Again)**). The published **post-lapse** formula
(elapsed **≥ 24**) uses Difficulty, Stability, and Retrievability. Again
outcomes stay on **RecallLog**. The **frequent-failure
warning** is the product signal for repeated incorrect recall; it does not
change the schedule.

### Fuzz

There is **no interval fuzz**. After a grade, due is `lastRecalledAt +
I(0.9, S)` in whole hours (strictly-future fallback when `I` is
non-positive). Do not jitter Stability or due. Open FSRS may randomize the
scheduled interval to spread same-calendar-day clumps; Doughnut already
spreads dues because they are anchored to the actual recall instant in
whole hours. Session order among already-due items is not a memory-state
concern.

### Difficulty on correct recall

Harder items gain less Stability on a successful recall. Ordinary correct
recall with Stability > 0 updates Stability with open-FSRS-6 Good-equivalent
rules (own implementation) and Difficulty with Good next-D (see **Difficulty
after a mapped grade**). First mapped grade on New uses first-rating (see
**First rating on New**).

### First rating on New

First mapped grade on a New tracker (ordinary correct / just review Yes / Tutor **3** Good, Tutor **2** Hard, Tutor **4** Easy, just review No / ordinary incorrect / Tutor **1** Again) uses published FSRS-6 first-rating (own implementation, frozen `Fsrs.W`) with `G` = the mapped grade (Tutor score **is** `G`):

- Stability `S0(G) = w[G−1]` days, persisted as whole hours. With frozen weights: Again **5**, Hard **31**, Good **55**, Easy **199**.
- Difficulty is `D0(G)` clamped to `[1, 10]`, persisted as the Java float from that formula (API number, no extra rounding). Persisted first Easy Difficulty is **1**.
- `D0(Easy)` stays **unclamped** as the later mean-reversion target (see **Difficulty after a mapped grade**).
- Elapsed time does **not** change first-rating. Overdue extra does not apply.
  First-mapped `elapsed_hours` is **0** (see **RecallLog**).
- Due is `lastRecalledAt` plus those hours.

The 24-hour strictly-future fallback is for non-positive `I`, not a New first-rating interval.

### Difficulty after a mapped grade

When Stability > 0, a mapped grade updates Difficulty with published open FSRS-6 next Difficulty (own implementation, frozen `Fsrs.W`):

- `ΔD = -w6 · (G − 3)`
- `D' = D + ΔD · (10 − D) / 9`
- `D'' = w7 · D0(Easy) + (1 − w7) · D'`, then clamp to `[1, 10]`
- `D0(G) = w4 − e^{w5·(G−1)} + 1`; **`D0(Easy)` is unclamped** `D0(4)` (negative with default weights)

`G`: Again=1, Hard=2, Good=3, Easy=4 (`Fsrs.AGAIN` / `HARD` / `GOOD` / `EASY`). Tutor score **is** `G`. New first-rating uses clamped `D0(G)` (see **First rating on New**). Tutor **2** is Hard: on New, `D0(2)`; when `S > 0`, Hard next-D. Confusion does not change Difficulty. Display is the API number (no extra rounding).

### Outcome-to-grade compatibility map

Keep Doughnut product outcomes first-class. Do not replace the Tutor 1–4 rubric with Anki Again / Hard / Good / Easy buttons. The report **score is FSRS G**. When memory updates follow an FSRS-shaped engine, map as follows:

| Product | Schedule |
|---------|----------|
| Just review Yes / ordinary correct / Tutor **3** | FSRS-6 Good |
| Tutor **4** | FSRS-6 Easy |
| Tutor **2** | FSRS-6 Hard |
| Just review No / ordinary incorrect / Tutor **1** | FSRS-6 Again memory; due from `I` (non-positive `I` → 24h) |
| Confusion | Not a grade; Again-midpoint S; due not later |
| Overlap | No memory change |

Shared commissioned rules and score-specific memory updates follow. Accidental-match detail is in **Accidental-match and overlap transitions**.

### Just review

Just review stays **two buttons**. **Yes, I remember** is ordinary correct (Tutor **3**, FSRS-6 Good). **No, I need more recall** is ordinary incorrect (Tutor **1**, FSRS-6 Again). Do not add Hard or Easy. Tutor **2** and **4** stay commissioned-only.

### Commissioned Learning Session feedback

A commissioned memory tracker is graded from Tutor Feedback (score 1–4), not from a recall prompt Doughnut asked. Commissioned vocabulary: [ADR 0001](./0001-ubiquitous-language.md). What the score means to the Tutor: [ADR 0005](./0005-commissioned-learning-session-protocol.md). Valid scores are **1, 2, 3, and 4**. The score **is** FSRS G: **1** Again, **2** Hard, **3** Good, **4** Easy. Shared schedule rules:

- A recorded score is a grade: it counts the recall, sets `lastRecalledAt`, and reschedules the tracker.
- A Session Item that never receives Feedback supplies no graded recall result: its tracker stays unchanged and the item is abandoned with its session.
- Effort is neutral. A Tutor session carries no trustworthy effort measurement.
- A late session does not weaken the result. The score determines the memory-state adjustment; the recorded time advances `lastRecalledAt`.
- After a score, due is `lastRecalledAt + I(0.9, S)`; non-positive `I` → 24h. A commissioned tracker is due only when the learner commissions another Learning Session.
- **New:** scores **1–4** use FSRS-6 first-rating with `G = score` (see **First rating on New**).

Memory updates with Stability > 0:

- **4:** open-FSRS-6 **Easy**-equivalent, not a percentage above Good. Easy increment at least as large as Good's plus an extra Easy factor, plus Easy next-D (see **Difficulty after a mapped grade**). Next Stability is strictly longer than the same state under score 3. Overdue extra applies.
- **3:** open-FSRS-6 **Good**-equivalent (same as ordinary correct), including overdue extra and Good next-D (see **Difficulty after a mapped grade**).
- **2:** open-FSRS-6 **Hard**-equivalent, not a percentage below Good. Hard increment is the Good increment times an extra Hard factor, plus Hard next-D (see **Difficulty after a mapped grade**). Next Stability is at least the current Stability and strictly shorter than the same state under score 3. Overdue extra applies.
- **1:** open-FSRS-6 **Again** memory (same as ordinary incorrect: short-term After-Again when elapsed **< 24**, capped post-lapse when **≥ 24**, and Again next-D; see **Incorrect recall (Again)** and **Difficulty after a mapped grade**). Due from `I`.

### Incorrect recall (Again)

Ordinary incorrect recall (MCQ, just review No, spelling fail) is FSRS **Again**.

When Stability is greater than 0, elapsed whole hours **< 24** use short-term After-Again: the same published `S'(S,G)` as **Whole-hour elapsed-time precision** (G=1; SInc may be < 1; floor **1 hour**). Elapsed **≥ 24** use the open-FSRS-6 post-lapse formula from Difficulty, Stability, and Retrievability (elapsed whole hours vs Stability): compute `Sf`, then `S' = min(current S, max(1, round(Sf)))` in whole hours. Floor **1 hour** applies before the cap. Ordinary incorrect also updates Difficulty with Again next-D (see **Difficulty after a mapped grade**). Unset Difficulty on Stability > 0 is treated as **5**. Queue lateness vs `nextRecallAt` is not an input. After ordinary incorrect, due is `lastRecalledAt + I(0.9, S)` of that next Stability; non-positive `I` → 24h. There is **no relearning step list**.

A **New** tracker that fails uses first-rating Again: Stability `S0(1)` (**5**), Difficulty `D0(1)` (Java float), due `lastRecalledAt + I` (**5h**); see **First rating on New**. Failure must not permanently trap the tracker; later correct recalls must be able to restore expanding intervals.

### Overdue correct recall: bounded extra growth

A correct recall after more elapsed whole hours than the tracker's
**Stability** must result in a next Stability **strictly longer** than the
same correct recall at elapsed hours equal to that Stability. Extra growth
is driven by elapsed time vs Stability
(low Retrievability), not by lateness vs `nextRecallAt`. It is **bounded**:
further delay must not increase the next interval without limit. A linear
lateness bonus is not allowed. Exact increment math is an implementation
detail; policy tests assert the observable next interval in hours.
Tutor Feedback scores **2**, **3**, and **4** inherit this extra. Score **1** uses
Again memory, not this extra.

### Whole-hour elapsed-time precision

Use **whole elapsed hours** as the recall-transition time input: duration
between the current recall time and `lastRecalledAt`, discarding any sub-hour
remainder, independent of time zone or calendar day. Morning/afternoon recall
windows make day precision too coarse. There is no calendar same-day rule.
When elapsed whole hours are **< 24** and Stability is greater than 0, all
four mapped grades (ordinary correct / Tutor **3** Good, Tutor **2** Hard,
Tutor **4** Easy, ordinary incorrect / just review No / Tutor **1** Again)
update Stability with published open FSRS-6 short-term next Stability (own
implementation, frozen `Fsrs.W`): convert persisted whole hours to days,
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
Confusion is unchanged as a non-grade (inherits Again S). New (Stability 0)
first-rating is unchanged by elapsed time (see **First rating on New**).
Observable pin:
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
first-rating Stability. The DSR snapshot may leave a due already past relative
to now (see **DSR snapshot**); do not clamp that due to now.

### Manual and admin paths

`mark-as-recalled` is just review's grade path: successful is just review Yes
(Good); unsuccessful is just review No (Again). `remove` and `revive` do not
write `lastRecalledAt`.

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

A `recall_log` has `memory_tracker_id`, `recorded_at`, `elapsed_hours`,
`product_outcome`, and optional `answer_id`. Tutor Feedback is a log row, not a
session bag.

`elapsed_hours` is always present (whole hours; see **Whole-hour elapsed-time
precision**) and required. The first mapped grade on a tracker is **0**. Later
mapped grades are whole hours since the previous mapped grade's `recorded_at`
on that tracker (`GOOD` / `EASY` / `HARD` / `AGAIN`). `CONFUSION` uses the same
elapsed vs the last mapped grade, else **0**, and is not an anchor. Order is
`recorded_at`, then `id`. A negative diff is **0**.

A row has `answer_id` xor none: prompt grade and confusion set `answer_id`;
just review and Tutor Feedback set none. Do not store `recall_prompt_id`
(redundant with `answer` → `recall_prompt`).

Do not store FSRS G, Retrievability, `I`, or pre/post Stability/Difficulty.
Current Stability, Difficulty, `lastRecalledAt`, and `nextRecallAt` stay on
`memory_tracker` (see **DSR snapshot**). `next_recall_at` stays the due-work
index.

`product_outcome`: `GOOD` | `EASY` | `HARD` | `AGAIN` | `CONFUSION`. Persist
named grades. Latest tutor feedback is **1–4** via `AGAIN→1`, `HARD→2`,
`GOOD→3`, `EASY→4` ([ADR 0005](./0005-commissioned-learning-session-protocol.md)).

### DSR snapshot

The persisted DSR on `memory_tracker` (`Stability`, `Difficulty`,
`lastRecalledAt`, `nextRecallAt`) is a **cache of folding that tracker's
RecallLog** under this locked policy. Live grading still updates the snapshot
on each mapped grade and on confusion. Do **not** fold the log on every
due-work query.

Fold semantics:

- Fold **every** tracker that has at least one mapped grade (`GOOD` / `EASY` /
  `HARD` / `AGAIN`), from New, in `recorded_at`, then `id` order. Use **stored**
  `elapsed_hours`. The first mapped grade is first-rating.
- **Leave** New, confusion-only, and `S > 0` with no mapped-grade log.
  Include **removed-from-tracking**. Skip `deleted_at IS NOT NULL`.
- Write Stability, Difficulty, `lastRecalledAt` (last mapped `recorded_at`),
  and `nextRecallAt` = last + `I(0.9, S)`. Apply the 24-hour fallback only
  when `I` is non-positive versus that grade instant. **Past due is allowed**
  (do not clamp to now).
- Confusion is a non-grade: midpoint Stability; Difficulty and last
  recall unchanged; due never later.

### Deferred

- **E4:** Fitting / per-user weights

## Working draft

Empty pending accept.

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
- Allows a data-fitted scheduler later without a library lock-in. Requested
  retention is locked at 0.9. Maximum interval is locked at 36500 days
  (876000 whole hours). A lapse count is not memory state. There is no
  interval fuzz. Per-user fitting remains deferred.

## Prerequisites / Assumptions

- Graded outcomes are trustworthy enough to be the primary scheduling signal.
- Answer time and the current tracker snapshot are available when grading.
  Mapped-grade RecallLog rows (with stored `elapsed_hours`) are sufficient to
  rebuild the DSR snapshot for trackers that have at least one mapped grade
  (see **DSR snapshot**). Due-work assumes that persisted snapshot, not a
  query-time fold.
- Thinking time may be recorded on answers for display.

## Options considered

- **Keep a symmetric early/late penalty** — rejected: conflates queue
  compliance with memory.
- **Adopt an open-FSRS library** — rejected: Doughnut owns the implementation.
- **Name the FSRS-compatible shape without requiring a library** — accepted
  (Decision above).
- **Overdue correct equals on-time increment only** — rejected; the extra
  is required.
- **Linear lateness bonus (SM-2-style)** — rejected: extra must converge and
  follow elapsed time vs Stability, not `nextRecallAt`.
- **Rebuild DSR from RecallLog on every due-work query** — rejected: due-work
  needs a queryable snapshot; do not fold at query time.
- **Tutor scores 1–4 identical to FSRS G** (`1` Again, `2` Hard, `3` Good,
  `4` Easy; `score = G`) — accepted (Decision above).
- **A 0–5 commissioned rubric with a shifted Good/Hard/Easy map** — rejected:
  valid report scores are 1, 2, 3, and 4; Tutor **2** is Hard.
- **Just review Hard / Easy buttons** — rejected: just review is rare; keep two
  buttons mapped to Tutor **3** and **1**.
- **Persist a lapse count** — rejected: FSRS-6 After-Again Stability does not
  consume it; RecallLog already holds Again history; the frequent-failure
  warning is the product signal. Do not add an unused counter.
- **FSRS interval fuzz** — rejected: due follows Stability; hour-precision
  recall instants already spread same-calendar-day clumps. Open FSRS treats
  fuzz as optional (`enable_fuzz` defaults off in ts-fsrs).
- **RT as Stability input** — rejected: thinking time is not a DSR input
  (G, elapsed t, D/S/R only). Record it on the answer and show it in stats /
  prompt history.
- **FSRS card states (Learning / Review / Relearning) or last recall at
  assimilate** — rejected: `lastRecalledAt` is the last mapped grade only;
  New has no last recall and is due at `assimilatedAt`. After first-rating
  the tracker is a graded DSR tracker. No step list or card-state column.
- **Calendar same-day short-term window** — rejected: whole-hour elapsed is
  the time input; there is no calendar-day rule.
- **Allow post-lapse `Sf` to exceed current S** — rejected: open FSRS-4.5+ /
  FSRS-6 uses `S' = min(Sf, S)` so a fail cannot lengthen Stability.
- **Cap short-term After-Again too** — rejected: open FSRS applies `min` on
  post-lapse only; short-term After-Again is unchanged.

## Related

- Tracker (pointer + deferred IDs, not a second policy map): [`.planning/research/FSRS-COMPATIBILITY-GAP.md`](../../.planning/research/FSRS-COMPATIBILITY-GAP.md)
- ADR 0001 [ubiquitous language](./0001-ubiquitous-language.md) — notes, assimilation, recall, tracker types, commissioned Learning Session terms. This ADR is the **Spaced repetition glossary**.
- ADR 0005 [commissioned learning session protocol](./0005-commissioned-learning-session-protocol.md) — what a score means to the Tutor
- Anki answer semantics: <https://docs.ankiweb.net/studying.html#answer-buttons>
- FSRS overdue-recall: <https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm>
- Reddy et al.: <https://arxiv.org/abs/1602.07032>
