# Doughnut ↔ open FSRS gap (toward ADR 0003)

**Status:** Analysis ready; open issues for discussion  
**Updated:** 2026-08-15  
**Feeds:** Proposed
[ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy.md)  
**Does not:** approve the ADR (humans own announce → discuss → approve)

This is the single tracking doc for (1) the gap between **current Doughnut code** and **open FSRS-6**, and (2) the unresolved choices needed to finalize ADR 0003. ADR 0003 should state the **product shape we want**, expected to be **mostly compatible with open FSRS**. Formulas and rollout stay out of the ADR until a choice is user-visible.

**Locked:** A1 (own FSRS-compatible implementation, no library); B3 (overdue correct: bounded extra growth); remaining gaps close by **vertical slice** (ADR 0003 Decision).

## Vertical slicing (until the FSRS gap is closed)

Preference for all remaining work in this seed: pick **one** high-priority
observable schedule behavior, lock it in ADR 0003, then change Doughnut to
match. Introduce or replace structure **only** when that behavior needs it.
No unused Difficulty, lapse, requested-retention, or RecallLog fields
for later slices.

Ordinary **correct** recall with S > 0 uses FSRS-6 Good SInc and next-D (own implementation, frozen default `w` in `FsrsGoodRecall`). First correct on New inits D=5, S=24h. Remaining in plan 004: E2E day lists. B2 requested-retention knob, B4, and C–E stay open.

Current Doughnut persists **Stability** in whole hours and **Difficulty** (nullable; hidden). Retrievability is computed in the success path (FSRS-6 power curve). There is still **no** lapse count, requested-retention knob, or card state (`New` / `Learning` / `Review` / `Relearning`). Remaining gaps close by **vertical slice** (ADR 0003 Decision).

## 1. What “mostly compatible” should mean

Open FSRS is a **DSR scheduler**: persisted **Difficulty (D)** and **Stability (S)**, computed **Retrievability (R)** from elapsed time, four **grades**, and a **requested retention** that turns S into the next interval. Current Doughnut persists **Stability** (hours) and uses a built-in spacing ladder; it does not persist Difficulty or take a retention target.

For ADR 0003, “mostly compatible” is a product claim, not a library claim.
**A1 is locked:** Doughnut owns an open-FSRS-compatible implementation (D, S,
computed R, grade + elapsed time). No `ts-fsrs` / `fsrs-rs` / other FSRS library.

| Compatible | Not required to call the shape “FSRS-compatible” |
|------------|--------------------------------------------------|
| Transition inputs are grade + elapsed time + current memory state — never queue lateness | Shipping `ts-fsrs` / `fsrs-rs` (rejected: own implementation) |
| Successful overdue recall is at least as strong as on-time; extra reward is bounded via elapsed time vs current interval (low-R stand-in) | Four Anki-style buttons in the Doughnut UI |
| Failure shortens the next interval without a permanent trap | Dropping Doughnut-only outcomes (overlap, accidental match, Tutor 0–5) |
| Due time is derived from memory state (may still be materialized) | Complete replayable history on day one |
| Each memory tracker is one FSRS “card” | Calendar-day time unit (whole hours already locked) |

---

## 2. Doughnut today (code, 2026-08-15)

**Memory state (one tracker = one schedule):**

| Field | Role |
|-------|------|
| `stability` | Current interval in **whole hours**. Assimilate may be `0` (due now). After a grade, `nextRecallAt = lastRecalledAt + stability`. |
| `difficulty` | Persisted memory state in `[1, 10]`. Hidden (`@JsonIgnore`). NULL on New / assimilate-only rows. Graded rows (`stability > 0` OR `recall_count > 0`) backfilled to **5**. |
| `lastRecalledAt` | Anchor for elapsed time. |
| `nextRecallAt` | Materialized due-work projection. |
| `recallCount` | Incremented on state-changing grades. |
| `assimilatedAt` | First intake. Assimilation sets `lastRecalledAt = now` with **no grade**. |

Difficulty is persisted (nullable; not on the learner UI) and consumed on ordinary correct recall. Frozen default FSRS-6 weights live in `FsrsGoodRecall`. There is still **no** lapse count, requested-retention knob, or card state (`New` / `Learning` / `Review` / `Relearning`). Retrievability is not stored.

**Success** (`ForgettingCurve.succeeded`):

- S > 0: FSRS-6 Good SInc (math in days, persist whole hours). SInc ≥ 1. Null D treated as 5.
- S = 0: first success still **24h**.
- Early (high R) grows less; overdue (low R) grows more and extra converges (B3).
- Optional thinking-time tweak on the FSRS result: `±sqrt(|t − 25s|)` clamped 0–60s, **within** correct only.

**Failure:** step down the ladder, then **forced `nextRecallAt = now + 12h`**, not the interval implied by the reduced Stability. `lastRecalledAt` **does** advance.

**Other grades:**

| Outcome | Memory effect | Anchor / due |
|---------|---------------|--------------|
| Overlap | None | Unchanged (retry in session) |
| Accidental match (primary) | Ordinary failure | Advances; 12h retry |
| Confusion adjustment (secondary) | `−10` index | Due may move earlier, never later; no `lastRecalledAt` / `recallCount` |
| Tutor 5 / 4 / 3 | Success-like `+12` / `+10` / `+8` | Normal interval path; no 12h retry |
| Tutor 2 / 1 / 0 | Reduce accumulated strength 20% / 50% / reset to 100 | Next recall strictly after now |
| Just review | Boolean `mark-as-recalled`; thinking time `null` | Same as MCQ correct/incorrect |

**History:** `answer` (correct, thinking time, spelling outcome, created_at) plus Tutor scores. Not an FSRS review log: no pre/post D/S, no rating 1–4, no scheduled vs elapsed days, no algorithm version.

**Queue (not memory input):** due work uses half-day windows (`alignByHalfADay`). That is selection/display, not a transition input.

---

## 3. Open FSRS today (FSRS-6 / ts-fsrs)

Canonical write-up: [The Algorithm](https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm). Runnable shape: [ts-fsrs](https://open-spaced-repetition.github.io/ts-fsrs/).

| Symbol | Meaning |
|--------|---------|
| **D** | Difficulty ∈ [1, 10]. Harder items gain less stability. |
| **S** | Stability: elapsed time (days) at which **R = 90%**. |
| **R(t, S)** | Retrievability. FSRS-6: power curve with trainable decay `w20`. |
| **G** | Grade: 1 Again, 2 Hard, 3 Good, 4 Easy. |
| **I(r, S)** | Next interval from **requested retention** `r` (default 0.9), not a day table. |

Qualitative rules Doughnut already wants (and FSRS implements):

1. Successful review: `SInc ≥ 1` (Hard/Good/Easy never shrink S).
2. Lower R (including overdue) → **larger but converging** S increase — spacing effect, not linear lateness.
3. Failure: post-lapse **S′_f(D, S, R)** then optional **relearning steps**; not “due immediately forever.”
4. Same-day / short-term: separate `S′(S, G)` when `enable_short_term` (FSRS-6).
5. Card states: New → Learning → Review; Again can enter Relearning.
6. Review log is enough to **replay** and **fit** the 21 weights.

FSRS **Hard (G=2) is still success**. That is the sharpest mapping clash with Doughnut Tutor score 2 (no growth, strength reduced).

---

## 4. Gap map

| Open FSRS | Doughnut today | Kind of gap |
|-----------|----------------|-------------|
| Persist D and S; compute R(t, S) | Persist S (hours) and D (nullable, hidden); R computed on success | **Model** (SInc + Good next-D + first-grade init D=5/S=24h) |
| Interval from requested retention | Ordinary correct uses SInc; `nextRecallAt = last + S` (`r = 0.9` implicit). Fail/confusion/commissioned still ladder | **Product knob** (B2 r-knob open) |
| G ∈ {1,2,3,4} | Incorrect / correct + overlap / accidental match + Tutor 0–5 + thinking time | **Grades** |
| Overdue success: bounded extra S via low R | Overdue correct lengthens S more than on-time; extra converges | **Aligned** (B3) |
| Early success: smaller SInc via high R | FSRS-6 R in SInc (same direction) | **Aligned** |
| Post-lapse S then relearning steps | −20 index + fixed 12h | **Relearning** |
| Same-day short-term scheduler | Whole hours; elapsed 0 on a positive interval → **zero growth** | **Short-term** |
| New card has no S/D until first rating | Assimilate is New: S=0, D unset, due now (not a grade); first-grade init later | **First state** (D3 locked in ADR) |
| Review log + optimizer | Partial answers / Tutor scores | **History** (already deferred) |
| `request_retention`, `maximum_interval`, fuzz, learning/relearning steps | Interval table only | **Config** |
| One card | One memory tracker (understanding / spelling / property / commissioned) | **Aligned** if 1 tracker = 1 card |
| Activity named **review** | **Recall** (ADR 0001 / 0003) | **Vocabulary** — locked; not a scheduler gap |

**Already aligned (do not reopen):**

- Memory transition uses outcome + elapsed time, not `nextRecallAt` deviation.
- Overdue success is never worse than on-time.
- State-changing recalls advance `lastRecalledAt`.
- Due time is a projection (materialized because history cannot rebuild it).
- Whole elapsed hours (not calendar days, not sub-hour fractions).
- Accidental-match / overlap transitions (ADR 0003 Decision).
- After a graded answer, `nextRecallAt > recalledAt`.
- Effort cannot invert a correct outcome.
- Commissioned trackers do not use the 12h ordinary-recall retry.
- Frequent-failure warning does not change the schedule; secondary confusion is not a failed recall of the matched tracker.

---

## 5. Discussion areas

Discuss in this order. Each area lists options, a recommendation, and whether ADR 0003 must lock it. Resolve or **explicitly defer** before moving Working draft → Decision.

### A. Target commitment — what the ADR promises

**A1. Meaning of “FSRS-compatible” — resolved 2026-08-15**

Locked in ADR 0003 Decision **Open FSRS-compatible target shape, own
implementation**: D and S as persisted memory state, R computed from elapsed
time and S, transition = grade + elapsed time + that state. Doughnut implements
it; no FSRS library. Today’s index and table may remain until a later Decision
consumes D/S.

**A1 locked.** Remaining open items in B–E close by vertical slice; **B3** is locked in Decision (code still old).

---

### B. Memory-state and interval source — the shape

A1 named **what** the destination is (D, S, computed R, own implementation).
**B** names **how that state turns into the next due time**: what is stored,
which knob sets the interval, whether overdue success gains extra Stability,
and whether a lapse counter exists.

Closing B in the ADR is still a Decision edit. It does not by itself change
due times. Implementing the locked targets later will.

**Vertical slicing** is locked in ADR 0003 for all remaining FSRS gaps (not
only B). First behavior: **B3**.

**B1. Persist D / S when a behavior consumes them** (was O2) — **in code 2026-08-15**

`memory_tracker.difficulty` exists (nullable float, hidden). Graded rows backfilled to **5**; assimilate-only / New rows leave D unset. Ordinary correct with S > 0 consumes D for FSRS-6 Good SInc and persists next-D. First correct on New initializes D=5, S=24h.

**B1 persist D exists and is consumed for SInc.**

**B2. Interval source** (was O6) — user-visible

- Keep the day table indefinitely.
- Keep the table until an FSRS-shaped engine exists.
- **Prefer requested retention as the target knob**; table becomes a migration/compat input.

FSRS interval is `I(r, S)`. Doughnut’s table is a discrete ease ladder. You cannot be fully FSRS-compatible while the table remains the source of truth.

**Recommendation to discuss:** ADR states retention-target intervals as the **target**; the table remains allowed until Doughnut’s FSRS-shaped implementation consumes D/S. Do not silently delete the Settings control in this ADR.

Implementing the target: **behavior** — next interval comes from retention `r`
and S, not from walking the Fibonacci/user table. The Settings control would
change or become a compat/migration input.

**B3. Overdue success reward — resolved 2026-08-15** (was O5); **in code 2026-08-15**

Locked in ADR 0003 Decision **Overdue correct recall: bounded extra growth**, and implemented on `ForgettingCurve.succeeded`: next interval after overdue correct is strictly longer than on-time (same thinking time); extra from elapsed vs Stability, not `nextRecallAt`; bounded, not linear. Commissioned scores do not inherit this extra yet.

**B4. Lapses** (was O15)

Do not add an unused counter. Before adding: which outcomes increment it; first consumer (schedule vs warning vs fitting). FSRS-6 scheduling does not need L; FSRS-1 did.

**Recommendation:** defer; frequent-failure already uses a 14-day wrong-answer window.

Closing B4 by deferring: no change. An unused lapse column later would be
internal only; using lapses to change due times would be behavior.

**B3 in running code:** overdue correct lengthens Stability more than on-time (now via FSRS SInc). **B1** D is consumed for SInc. B2 requested-retention knob and B4 remain open.

---

### C. Grades and Doughnut-only outcomes

**C1. Outcome-to-grade map** (was O3) — user-visible

- Collapse everything onto Again / Hard / Good / Easy (and change the UI).
- **Keep Doughnut outcomes first-class; publish a compatibility map** into FSRS G when an FSRS-shaped engine exists.
- Replace Tutor 0–5 at the product surface.

**Recommendation:** keep product outcomes. Overlap is **not** a recall event. Accidental match is a Doughnut extension (primary = Again; secondary ≠ a recall). MCQ / just review stay binary at the prompt (Again vs Good) unless we add explicit Hard/Easy later.

**C2. Tutor 0–5 vs FSRS Hard-is-success**

| Tutor score | ADR 0003 today | FSRS-like reading |
|-------------|----------------|-------------------|
| 5 | Success, +20% | Easy (G=4) |
| 4 | Success, standard | Good (G=3) |
| 3 | Success, −20% | Hard (G=2) **or** weak Good |
| 2 | **No growth; −20% accumulated** | Clash: FSRS Hard still increases S |
| 1 | −50% accumulated | Again |
| 0 | Reset to initial | Again (severe) |

**Open:** is Tutor 2 **failure-like** (keep current policy) or **Hard-like success** (FSRS)? This is the main commissioned-learning incompatibility.

**C3. Thinking time** (was O4)

- Keep bounded continuous adjustment within correct recall (today).
- Map time bands onto Hard / Good / Easy.
- Remove once explicit grades exist.

**Recommendation:** keep bounded-within-correct so effort cannot invert the outcome. Do not treat slow correct as Again.

**C4. Just review**

Today: boolean, no thinking time. Compatible as Again vs Good. Open: whether just review should offer Hard/Easy (Anki-like) later. **Recommendation:** out of ADR 0003 unless we want three+ self-eval buttons now.

---

### D. Failure, relearning, short-term

**D1. Incorrect-recall retry** (was O7)

- Keep fixed 12h ordinary-recall retry.
- Configurable relearning steps, default 12h.
- FSRS post-lapse S without a forced short retry (interval may already be days).

Commissioned learning stays cadence-driven (already stated).

**Recommendation:** ADR may keep “ordinary incorrect recall is due again on a short, explicit retry” as the product rule, with 12h as the current default — not as a sacred constant. Post-lapse S is the **target** memory update; the retry is scheduling metadata.

**D2. Short-term / same-hour recalls** (was O11)

Whole-hour precision is locked. Open:

- One duration-based transition until D/S exist (today: elapsed 0 + positive interval → no growth).
- Later adopt FSRS-6 short-term `S′(S,G)` and learning/relearning states.
- If special handling is added, bound it by elapsed duration/state, **not** the learner’s calendar date.

**Recommendation:** ADR notes same-hour as “no additional success increment until a short-term rule exists”; do not invent a calendar same-day exception.

**D3. Assimilation vs FSRS New** (new) — **locked in ADR 0003 Decision 2026-08-15**

Locked: assimilation is **New** — Stability 0, Difficulty unset, due now. Assimilation is not a grade. The first real correct recall initializes Difficulty to **5** and Stability to **24** hours.

**D3 New-card semantics locked and in code.** First correct initializes D=5, S=24h (12h parked).

---

### E. Operational contract (lock lightly or defer)

**E1. Manual / admin paths** (was O9)

`mark-as-recalled` is a real grade. `remove` / `revive` are not grades. `updateForgettingCurve(..., 0)` is assimilate init, not a learner grade.

- Apply recall-transition rules whenever a grade is recorded.
- Document non-grade paths as escape hatches.
- Remove or gate administrative bypasses.

**Recommendation:** first option + name remove/revive as non-grades.

**E2. Strictly-future fallback** (was O13)

Invariant locked. Fallback when computed interval is non-positive:

- First positive configured spacing (today, commissioned path).
- 24h only when no positive spacing exists (today’s last resort).

**Recommendation:** adopt the current commissioned fallback as the general rule.

**E3. Fuzz, maximum interval** (new, was implicit in O6)

FSRS defaults: fuzz on, `maximum_interval` 36500 days. User-visible if we expose them.

**Recommendation:** defer to implementation; ADR may allow a maximum interval and small fuzz without requiring them.

**E4. Parameter ownership** (was O14)

A1 rejected an FSRS library. Remaining: global vs per-user weights; whether to fit from history; minimum history for fitting. Not crate/version policy.

**Recommendation:** **explicitly defer** fitting and per-user weights in ADR 0003.

**E5. Monitoring** (was O12)

Interval distribution, success rate, immediately-due-after-grade incidence. Not an ADR Decision unless we want a contractual SLO.

**Recommendation:** keep in Consequences / rollout notes.

**E6. RecallLog** (already in Working draft)

Counterpart to the FSRS review log. Not required to finalize the policy. Keep deferred.

---

## 6. ADR 0003 finalization checklist

When discussion closes:

1. Move resolved items from Working draft into **Decision**; delete or shrink Working draft.
2. Rewrite **Context** against current code (remove `delayInHours`; incorrect recall **does** advance `lastRecalledAt`).
3. ~~State the FSRS-compatible **target shape** (A1) without requiring an engine.~~ Done: own implementation, no FSRS library.
4. Keep Doughnut vocabulary: **recall**, not FSRS **review**.
5. Keep Doughnut-only outcomes first-class with a published map (C1), including the Tutor-2 decision (C2).
6. Implementation freedom where no user-visible contract is needed (formulas, columns, optimizer).
7. Humans accept / reject / supersede the ADR (`docs/adrs/README.md`).

Hygiene while this doc is the tracker: do not duplicate open issues in the ADR; link here from Related (already).

---

## 7. Issue index

| ID | Topic | ADR must lock? | Suggested |
|----|-------|----------------|-----------|
| A1 | FSRS-compatible = own D/S/R implementation, no library | **Resolved** | Locked 2026-08-15 |
| B1 | When to persist D/S | **In code** | Persist D exists; ordinary correct consumes D for SInc |
| B2 | Interval table vs requested retention | Yes | Open |
| B3 | Overdue bounded extra growth | **Resolved** | Locked and implemented 2026-08-15 |
| B4 | Lapses | Defer | Defer |
| C1 | Keep Doughnut outcomes | Yes | Keep + compatibility map |
| C2 | Tutor 2 vs FSRS Hard | Yes | Discuss — current policy ≠ FSRS |
| C3 | Thinking time | Yes | Bounded within correct |
| C4 | Just-review Hard/Easy | No / defer | Binary Again vs Good |
| D1 | 12h retry vs post-lapse S | Yes | Short retry as schedule; S as target update |
| D2 | Short-term / same-hour | Light lock | No growth at elapsed 0 until short-term rule |
| D3 | Assimilation = New card | **In code** | New / due now / D unset; first grade initializes D=5, S=24h |
| E1 | Manual paths | Light lock | Grades vs remove/revive |
| E2 | Non-positive interval fallback | Light lock | First positive spacing, else 24h |
| E3 | Fuzz / max interval | Defer | Allowed, not required |
| E4 | Fitting / per-user weights | Defer | Defer (library already rejected in A1) |
| E5 | Monitoring | Consequences | Observables, not Decision |
| E6 | RecallLog | Already deferred | Keep deferred |

Resolved earlier: accidental-match counts as failure for the prompted tracker only (old O8).

---

## References

- [ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy.md) (Proposed)
- [ADR 0001](../../docs/adrs/0001-ubiquitous-language.md) — **recall** vs FSRS **review**
- [ADR 0005](../../docs/adrs/0005-commissioned-learning-session-protocol.md) — Tutor score meaning
- Seed: [SEED-004](../seeds/SEED-004-close-spaced-repetition-scheduling-policy-gap.md)
- Code: `ForgettingCurve`, `MemoryTracker`, `SpacedRepetitionAlgorithm`, `CommissionedLearningSessionFeedbackPolicy`
- [FSRS-6 algorithm](https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm)
- [ts-fsrs](https://open-spaced-repetition.github.io/ts-fsrs/)
