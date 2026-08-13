# Doughnut ↔ open FSRS — compatibility gap & ADR 0003 discussion

**Status:** Open for discussion (feeds finalizing Proposed [ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy.md))  
**Created:** 2026-08-12  
**Corrected:** 2026-08-13 (late-success penalty is shipped; C1 is recall-time state cohesion, not a persisted-due subtraction bug; FSRS overdue reward remains open)
**Sources:** ADR 0003 draft; SEED-004; `.planning/research/{SUMMARY,ARCHITECTURE,FEATURES,PITFALLS,STACK}.md`; live scheduler (`MemoryTracker`, `ForgettingCurve`, `SpacedRepetitionAlgorithm`, commissioned feedback policy); commit `735b96623a`; [FSRS algorithm wiki](https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm); [ts-fsrs](https://open-spaced-repetition.github.io/ts-fsrs/)

**Goal of this doc:** One place to track (1) the conceptual/code gap between **current Doughnut** and **open FSRS**, and (2) **open issues** that must be settled before ADR 0003 becomes the durable product shape — expected to be **mostly FSRS-compatible** without requiring an immediate library migration.

---

## How to use this doc

| Track | Meaning |
|-------|---------|
| **Aligned** | Doughnut already matches FSRS intent (or ADR already states it) |
| **Gap (code)** | Live scheduler violates FSRS-compatible / ADR intent |
| **Gap (model)** | Conceptual shape differs; decide whether ADR target adopts FSRS shape |
| **Open issue** | Needs human decision before ADR 0003 is finalized |

Resolve open issues → update ADR 0003 Decision text → mark issues **Decided** here → then implement against the Accepted policy (SEED-004).

---

## 1. Executive verdict

Open FSRS is a **DSR memory model**: per-card **Difficulty (D)** and **Stability (S)**, with **Retrievability (R)** derived from **elapsed time since last review (FSRS) = recall (Doughnut)** and S. Grades are **Again / Hard / Good / Easy**. Due time is a projection from desired retention and S; merely becoming due says nothing about whether recall will succeed.

Doughnut today is a **single-index + user interval table** scheduler.

**Shipped 2026-08-05 (`735b96623a`):** the **late-success penalty is gone**. `ForgettingCurve.succeeded` used to shrink the increment with `Math.abs(delayInHours)`, so overdue correct could weaken the tracker and trap busy learners in short intervals. Now overdue (`delayInHours ≥ 0` vs due) gets the **same** standard increment as on-time. Controller test: `lateCorrectAnswerDoesNotShortenTheNextInterval`.

**Still open — do not confuse with that bug:**

1. **C1 (recall-time state cohesion):** the success API consumes `delayInHours` relative to a **recomputed expected recall time** (`lastRecalledAt + current interval`), not the persisted `nextRecallAt`. For early success, its current formula is algebraically equivalent to `elapsed / current interval`, so the ordinary success behavior already depends on elapsed time. The remaining gaps are that elapsed time is hidden by the due-relative contract and an incorrect recall does not advance `lastRecalledAt`, allowing a later recall interval to span across the failure.
2. **FSRS overdue reward (C2 remaining / O5):** open FSRS **increases** stability when a successful recall happens at low retrievability (long elapsed). Doughnut only matches the **minimum bar** (overdue ≥ on-time). It does **not** grant extra credit for longer elapsed time.

**Compatibility strategy assumed for discussion (not decided):**

1. **Now:** Lock an ADR product contract that is **FSRS-compatible in memory-state inputs, grades, and safety properties**.
2. **Near term:** Fix the existing Doughnut model to obey that contract (SEED-004) — no FSRS library required.
3. **Later (optional):** Swap internals to open FSRS (or D/S state) without rewriting product rules.

ADR 0003 should describe **(1)** fully. Whether **(3)** is a committed destination vs a deferred option is an open issue below.

---

## 2. Concept map

| Open FSRS | Doughnut today | Compatibility note |
|-----------|----------------|--------------------|
| **Stability (S)** — days until R ≈ target (default 90%) | `forgettingCurveIndex` → `SpacedRepetitionAlgorithm` spacing table → hours | Rough analog of “how far out”; not probabilistic; no R(t,S) |
| **Difficulty (D)** ∈ [1, 10] | *None* | Major model gap; mean-reversion / “ease hell” avoidance absent |
| **Retrievability (R)** = f(elapsed, S) | *Not computed*; early-success math implicitly uses elapsed/current interval | **C1** makes elapsed since the previous recall explicit and keeps the anchor current after every state-changing recall |
| **Elapsed days/hours since last recall** | `lastRecalledAt` exists | Correct/accidental/Tutor paths advance it; ordinary incorrect currently does not |
| **Due / next interval** | `nextRecallAt` | Same role: queue projection, not a memory-state input |
| **Grade G** ∈ {1 Again, 2 Hard, 3 Good, 4 Easy} | Correct / incorrect; accidental match; overlap; Tutor score 0–5; thinking-time continuous | Richer product grades; need explicit mapping onto (or extension of) FSRS grades |
| **Request retention** | Implicit via fixed space list | FSRS schedules to a retention target; Doughnut walks a table |
| **Fitted parameters w[]** | User-editable space intervals | Different knobs; both are configuration |
| **Learning / relearning steps** | Incorrect → fixed **12h** override | Analog of short relearning window; not post-lapse S |
| **Card state** New / Learning / Review / Relearning | Assimilated + active tracker; no explicit learning ladder | Optional FSRS surface |
| **Recall log / optimizer** | Partial answer and Tutor-feedback history; no unified `RecallLog` | `RecallLog` / rebuild / optimize deferred; not needed for C1 |

---

## 3. Semantic alignment (what “mostly compatible” should mean)

These are the FSRS principles that ADR 0003 should lock as Doughnut product law. Implementation formula can differ.

| # | FSRS principle | ADR 0003 draft | Live Doughnut | Status |
|---|----------------|----------------|---------------|--------|
| P1 | Memory update uses **grade + elapsed since last recall** | States this | Ordinary early-success math is algebraically elapsed-based, but the API is due-relative and incorrect does not advance the recall anchor | **Gap (code) — C1** |
| P2 | Due time is **scheduling metadata**, not a memory-state input | States this | Success recomputes an expected time from memory state rather than reading persisted `nextRecallAt`; misleading contract remains | **Partial** — remaining is C1 |
| P3 | Successful overdue recall is **not failure**; lower R can **increase** next S (bounded) | Overdue ≥ on-time required; lateness bonus optional/bounded | Minimum bar shipped; **no FSRS-style overdue reward** | Minimum **aligned**; reward **open (O5)** |
| P4 | Successful recall → SInc ≥ 1 (Hard/Good/Easy) relative to failure | Correct grows; post-grade not immediately due | Usually grows; trap possible at floor / zero interval | Mostly; enforce strictly-future |
| P5 | Failure updates via **post-lapse** path, timing-neutral as “late/early” | Incorrect timing-neutral; short retry separate | `failed()` + 12h — directionally OK | Aligned enough; exact shape open |
| P6 | Grades are ordered recall results (Again < Hard < Good < Easy) | Correct / incorrect / accidental / Tutor 0–5 | Distinct paths exist | **Gap (model)** — mapping undecided |
| P7 | Difficulty is separate from stability | Not in ADR | Absent | **Open** whether target shape includes D |
| P8 | Interval from retention target + S | Not required; freedom clause | Space table | **Open** — keep table forever vs FSRS intervals later |
| P9 | Same-day / short-term recalls handled specially | Shorter elapsed retention may grow less, not reset | Early-success math scales by elapsed/current interval; thinking-time also adjusts | Partial; align with short-term rules |
| P10 | History enables replay / fitting | Rebuild deferred; projection authoritative | Incomplete history | Aligned as deferred |

---

## 4. Outcome / grade gap

### 4.1 Open FSRS grades

| G | Name | Role |
|---|------|------|
| 1 | Again | Forgot — post-lapse stability |
| 2 | Hard | Recalled with difficulty — success, smaller SInc |
| 3 | Good | Standard success |
| 4 | Easy | Strong success — larger SInc / lower D pressure |

### 4.2 Doughnut outcomes → proposed FSRS-compatible reading

| Doughnut outcome | Current schedule effect | FSRS-compatible reading (proposal for discussion) | Status |
|------------------|-------------------------|-----------------------------------------------------|--------|
| **Incorrect** | Strength −2× increment; `nextRecallAt = now+12h` | **Again** (+ Doughnut-specific short retry projection) | Close; confirm 12h vs relearning steps |
| **Correct** (recall question) | Strength +increment ± early/thinking | **Good** by default; optional Hard/Easy from effort | **Open** — see effort mapping |
| **Accidental match** | `partialFail` (−1×); normal interval path | No FSRS analog — **Doughnut extension**: weaker than Again, still negative; not 12h override | Keep; document as extension |
| **Overlap** | No mutation | No FSRS analog — **no recall event** (not a grade) | Keep |
| **Tutor score 5** | +1.2× increment | Near **Easy** | Mapping table open |
| **Tutor score 4** | +1.0× | **Good** | |
| **Tutor score 3** | +0.8× | Between Hard and Good (or Hard+) | |
| **Tutor score 2** | −20% accumulated | Soft fail / hard success boundary — **not** pure Again | |
| **Tutor score 1** | −50% accumulated | Closer to Again without full reset | |
| **Tutor score 0** | Reset to initial | Strong Again / new-like | |
| **Thinking time** | Continuous ±sqrt adjustment on success | FSRS folds into Hard/Easy buttons | **Open** |

**Compatibility claim to validate in discussion:** Doughnut may keep product-specific outcomes (accidental, overlap, Tutor 0–5) **if** each outcome has a defined effect on a FSRS-shaped memory update `(grade-equivalent, elapsed)` and never uses queue deviation as a memory-state input.

---

## 5. Code gaps (current Doughnut vs FSRS-compatible policy)

Already detailed in research SUMMARY / ARCHITECTURE; summarized for tracking:

| ID | Gap | Code finding | FSRS / ADR impact |
|----|-----|----------|-------------------|
| ~~Late-success penalty~~ | **Shipped** 2026-08-05 `735b96623a` | Overdue vs due no longer shrinks the increment; overdue interval ≥ on-time | P3 minimum bar |
| C1 | Recall-time state is not cohesive | `succeeded(delayInHours)` hides elapsed/current-interval math; `recallFailed` does not advance `lastRecalledAt` | Makes the recall anchor unreliable; blocks an explicit FSRS-shaped transition contract |
| C2 | No FSRS overdue reward | `succeeded` gives overdue the **same** increment as on-time; early-only shrink vs due | P3 reward half open (O5); do after C1 |
| C3 | Strictly-future due not universal on recall success | Commissioned has helper; success path relies on table | Trap risk |
| C4 | Parallel entry points | Spelling / MCQ / commissioned / manual mark | Divergent semantics risk |
| C5 | Policy tests on index floats | Early-recall tests encode old semantics | Miss schedule traps |
| C6 | Frequent-failure count vs accidental | Query treats `correct=false`; ADR says incorrect-only | Product decision |
| C7 | No Stability / Difficulty state | Single index only | Blocks full FSRS parity; migrate in its own later behavior slice, not C1 |
| C8 | Incomplete recall history for optimizer/replay | Known | FSRS fitting / rebuild deferred |
| C9 | No lapse count | No persisted `lapses`; no current external behavior consumes it | Add only with the later behavior that needs it, not as unused C1 structure |

---

## 6. What ADR 0003 already gets right (keep)

Do not reopen unless a stronger reason appears:

1. Recall inputs vs schedule separation (P1–P2).
2. Correct overdue must not be worse than on-time; no late-success penalty.
3. Correct recall after a shorter elapsed interval may grow less, but does not reset or become immediately due.
4. Incorrect timing-neutral; short retry separate from long schedule.
5. Accidental match weaker than incorrect; normal interval path.
6. Overlap = no schedule mutation; not frequent-failure.
7. Commissioned 0–5 quantified table; effort neutral; never due at score instant.
8. Frequent-failure warning informational only.
9. Policy tests assert **observable schedule**, not internal indexes.
10. Rebuild-from-incomplete-history deferred; keep transactional `nextRecallAt`.
11. No requirement to ship FSRS formulas in the first implementation.

---

## 7. Decided delivery boundaries

These decisions constrain implementation ordering without prematurely settling every
long-term FSRS policy choice.

### C1 — recall-time state cohesion

1. Keep the current persisted tracker as the C1 aggregate:
   `lastRecalledAt`, `nextRecallAt`, `recallCount`, and
   `forgettingCurveIndex`.
2. Express elapsed time directly in **hours**, matching Doughnut's current
   hour-based scheduler and preserving the distinction between the morning and
   afternoon halves of a day.
3. Every state-changing recall advances `lastRecalledAt`: correct, incorrect,
   accidental match, and Tutor feedback. Overlap and no-feedback are no-ops. A
   pure administrative schedule edit is not a recall.
4. Repair legacy `lastRecalledAt` values from the latest trustworthy persisted
   timestamp. For each tracker, take the maximum of its current
   `lastRecalledAt`, the latest non-overlap `Answer.createdAt`, and the latest
   `SessionItem.feedbackRecordedAt`. This preserves valid existing/manual
   anchors while repairing trackers whose last action was incorrect or Tutor
   feedback. Do **not** bulk-rewrite `nextRecallAt`.
5. C1 needs a data-repair migration but no schema change. It does not add
   `RecallLog`, Stability, Difficulty, or lapses, and it does not rename
   `nextRecallAt` to `due`/`dueAt` or `recallCount` to `reps`.

### Later behavior slices

1. **Stability / Difficulty migration:** migrate
   `forgettingCurveIndex` to persisted Stability and Difficulty only in its own
   behavior slice, when those states actually drive scheduling. Any schema
   preparation belongs immediately with that behavior.
2. **Lapses:** introduce persisted `lapses` only with the first external behavior
   that consumes it (for example, lapse-sensitive scheduling, reporting, or
   fitting). Do not add a write-only counter in C1.
3. **Strictly-future scheduling (C3):** deliver separately from C1. After every
   state-changing recall, require `nextRecallAt > recalledAt`; overlap remains a
   no-op. If the calculated interval is non-positive, the current recommendation
   is to use the first positive configured spacing, falling back to 24 hours only
   when none exists. The invariant is decided; the fallback remains subject to
   confirmation after the explanation in O13.
4. **FSRS overdue reward (C2):** do not bundle it into C1. Whether longer
   elapsed time should earn a bounded Stability increase remains an independent
   behavior decision in O5.

---

## 8. Open issues (discussion queue)

Check off as decided. Record the decision in one line; then fold into ADR 0003.

### O1. What does “mostly compatible with open FSRS” commit us to?

- [ ] **A.** Semantic compatibility only (memory-state inputs, grade ordering, safety) — formulas stay Doughnut until a later ADR
- [ ] **B.** ADR names **D / S / R** as the target memory vocabulary now; implementation may still use index+table as a stand-in  
- [ ] **C.** ADR commits to adopting open FSRS (library or reimplementation) as the eventual engine  

**Why it matters:** Determines how much model language enters ADR 0003 vs staying “implementation freedom.”  
**Recommendation to discuss:** **B** — vocabulary compatibility without forcing migration.

### O2. Memory state shape in the ADR

- [ ] Keep single opaque “memory strength” + spacing table (current draft)  
- [ ] Adopt **Stability** (and optional **Difficulty**) as named product concepts; strength index becomes transitional  
- [ ] Require Difficulty in v1 of the policy  

**Why it matters:** FSRS’s main structural novelty vs SM-2 is D⊥S. Without D, “compatible” is mostly about using the recall result and elapsed time explicitly.
**Delivery decision:** Do **not** introduce or migrate Stability/Difficulty in
C1. Migrate both in their own later behavior slice.
**Still open:** Whether ADR 0003 should name Stability/Difficulty as the target
vocabulary before that migration exists.

### O3. Grade ontology: map or extend?

- [ ] Collapse all Doughnut outcomes onto Again/Hard/Good/Easy before scheduling  
- [ ] Keep Doughnut outcomes as first-class; publish an explicit **compatibility map** to FSRS grades (extensions allowed)  
- [ ] Replace Tutor 0–5 with Anki-style 1–4 at the product surface  

**Why it matters:** Tutor scores and accidental/overlap are Doughnut differentiators already shipped.  
**Recommendation to discuss:** **Keep extensions** + publish map (accidental/overlap stay Doughnut-specific; Tutor 0–5 ↔ ordered grades).

### O4. Effort / thinking time vs Hard/Easy

- [ ] Keep continuous thinking-time as secondary adjustment within Correct (current)  
- [ ] Map thinking-time bands → Hard / Good / Easy  
- [ ] Drop thinking-time once a 4-button (or Tutor) grade exists  
- [ ] Thinking-time only for auto-graded recall; Tutor scores remain the grade  

**Why it matters:** FSRS expects discrete grades; continuous effort can invert or muddy Hard/Easy semantics if unbounded.  
**Recommendation to discuss:** Keep thinking-time **bounded within Correct ≈ Good**, cannot invert outcome (already ADR); do not pretend it is a full Hard/Easy UI yet.

### O5. Lateness / overdue success bonus

- [ ] **Minimum bar only:** overdue correct ≥ on-time correct (remove penalty; no extra bonus)  
- [ ] **FSRS-like:** longer elapsed → lower R → larger stability increase, **converging / bounded**  
- [ ] Linear lateness bonus (SM-2-ish) — generally poorer fit  

**Why it matters:** The **minimum bar is already shipped** (overdue ≥ on-time). Open FSRS goes further: longer elapsed → lower R → **larger** (bounded) stability increase. Doughnut does not implement that reward. It only makes FSRS-sense **after C1**, because the reward is “elapsed was long,” not “you missed the queue.”  
**Delivery decision:** C1 does not add the reward. Keep the shipped minimum bar
in ADR; decide separately whether C2 should add a bounded FSRS-like reward.

### O6. Interval source: space table vs request retention

- [ ] Space setting remains the interval source indefinitely  
- [ ] Space setting remains until FSRS migration; ADR allows retention-target intervals later  
- [ ] ADR should already prefer retention-target intervals as the target shape  

**Why it matters:** Biggest practical fork for “eventual FSRS.”  
**Recommendation to discuss:** Keep space setting as current input; ADR explicitly allows replacing the table with retention-driven intervals **without** changing the recall-transition contract.

### O7. Incorrect / relearning window

- [ ] Keep fixed 12h post-incorrect projection  
- [ ] Align wording with FSRS relearning steps (configurable short steps) while keeping 12h as default  
- [ ] Move to post-lapse stability only (no forced short due) for all tracker types  

**Note:** Commissioned path correctly avoids 12h (session cadence). Keep that exception.  
**Recommendation to discuss:** Keep 12h for ordinary recall; name it as a **relearning projection**, not a memory-state input.

### O8. Accidental match ↔ frequent-failure counting

- [ ] Count only **incorrect** (ADR draft) — change query  
- [ ] Count incorrect + accidental match  
- [ ] Separate thresholds  

**Why it matters:** Already flagged in research SUMMARY; blocks “Aligned” claim on warning.  
**Recommendation to discuss:** Incorrect-only (ADR draft).

### O9. Manual / admin schedule paths

- [ ] `markAsRecalled` / `updateForgettingCurve` must obey the same recall-transition rules
- [ ] Document as escape hatches outside policy  
- [ ] Remove or gate admin bypasses  

**Recommendation to discuss:** Use the same transition rules whenever a grade is recorded; pure admin edits are explicit exceptions and must not invent due-deviation penalties.

### O10. ADR scope sentence on FSRS

Current draft: “This ADR does not require FSRS.”

Proposed replacement options:

- [ ] Keep as-is (FSRS only a reference)  
- [ ] “Target shape is **open-FSRS-compatible**; shipping open FSRS code is not required by this ADR.”  
- [ ] “Doughnut will migrate to open FSRS; this ADR is the product contract that migration must preserve.”  

**Recommendation to discuss:** Middle option — compatibility without migration mandate.

### O11. Same-day / early recalls

An FSRS “same-day” rule is not merely a calendar grouping. FSRS-6 uses a
separate short-term stability update when elapsed days are zero, and FSRS
implementations can also apply minute/hour learning and relearning steps. It is
a distinct memory-transition path for short-term recalls.

- [x] C1 passes elapsed time in hours; it does not collapse elapsed time to days
- [ ] Keep one duration-based transition until the Stability/Difficulty migration
- [ ] Later adopt an explicit FSRS short-term transition and learning/relearning steps
- [ ] If a special rule is added, define its boundary by elapsed duration/state rather than user-local calendar date

**Why hours:** Doughnut already groups daily activity into morning and afternoon
halves. Day precision would make those observably different recalls look
simultaneous; hour precision preserves their elapsed time.
**Recommendation:** C1 should use the current whole-hour precision with no
special calendar-day branch. Revisit the FSRS short-term formula together with
the Stability/Difficulty behavior slice, where it can update real short-term
memory state rather than act as an isolated exception.
**Decision status:** Hour precision is decided. The eventual same-day/short-term
policy remains open.

### O12. Monitoring / rollout

- [ ] What observables after policy change? (interval length distribution, success rate, % due-immediate after correct)  
- [x] Backfill `lastRecalledAt` from the latest trustworthy Answer/Tutor-feedback timestamp, taking the maximum with its current value
- [x] Do not bulk-rewrite `nextRecallAt`
- [ ] Define rollback criteria for the C1 data repair

**Recommendation to discuss:** Monitor post-release as ADR Consequences already
say. The anchor backfill repairs known stale state; it is not a history replay or
a reinterpretation of legacy due times.

### O13. Strictly-future scheduling

“Strictly future” means that after a state-changing recall at time `t`, the
persisted projection must satisfy `nextRecallAt > t`, not merely
`nextRecallAt >= t`. Doughnut selects due trackers with `nextRecallAt <= now`, so
equality makes a tracker immediately selectable again even though the learner
just graded it.

Ordinary incorrect already projects 12 hours ahead, and commissioned feedback
already guards against a non-positive interval. Ordinary correct and accidental
paths still rely on the spacing table and can project zero hours at the lower
bound.

- [x] Deliver the invariant as C3, separately from C1
- [x] Apply it to correct, incorrect, accidental match, and Tutor feedback
- [x] Keep overlap as a no-op
- [ ] Confirm the fallback: first positive configured spacing; 24 hours only if none exists

**Recommendation:** Adopt that fallback because it respects user configuration
and matches the existing commissioned-feedback precedent. Test the public recall
entry points by asserting the observable schedule rather than internal index
values. This is independent of the same-day policy: a future due time may still
be later on the same day.

### O14. FSRS engine, version, and parameter ownership

- [ ] Use an open-FSRS library or maintain a Doughnut implementation
- [ ] Pin which FSRS major algorithm defines compatibility and how upgrades are decided
- [ ] Store global defaults, per-user desired retention, and fitted parameters at which scopes
- [ ] Decide when incomplete history is sufficient for fitting and how fallback parameters are selected

**Why it matters:** “Adopt Stability/Difficulty later” does not settle which
algorithm version updates those states or who owns the parameters. These choices
belong to the eventual FSRS behavior slice, not C1.

### O15. Lapse semantics and consumer

- [x] Do not add `lapses` in C1
- [x] Add it only with an external behavior that consumes it
- [ ] Define which Doughnut outcomes increment it (incorrect only vs accidental/Tutor thresholds)
- [ ] Choose the first consumer: scheduling, learner-visible reporting, or parameter fitting

**Why it matters:** FSRS cards persist lapse count, but an unused counter would
be speculative structure. Its outcome semantics should be decided by the
behavior that first needs it.

---

## 9. Suggested ADR 0003 final-shape outline (for discussion)

If O1=B and O10=middle option, ADR Decision could be organized as:

1. **Compatibility stance** — open-FSRS-compatible product contract; formula/library optional later.  
2. **Recall transition** — persisted pre-recall tracker state + grade + elapsed since the previous state-changing recall (+ optional bounded effort); never queue deviation.
3. **Memory state** — conceptual Stability (implementation may use index+table); Difficulty reserved.  
4. **Due projection** — materialized `nextRecallAt`; authoritative while history incomplete.  
5. **Grade catalog** — incorrect / correct / accidental / overlap / Tutor 0–5 with FSRS map appendix.  
6. **Safety properties** — strictly-future after grade; overdue ≥ on-time; early not reset; incorrect recoverable; commissioned table; frequent-failure informational.  
7. **Configuration** — space setting today; retention-target intervals allowed later without changing §Recall transition.
8. **Out of scope** — parameter fitting, history replay rebuild, adopting a specific FSRS version’s weights.

*(Do not treat this outline as accepted — fold in only after open issues are decided.)*

---

## 10. Decision log

| Date | Issue | Decision | Where recorded |
|------|-------|----------|----------------|
| 2026-08-05 | Late-success penalty (symmetric `abs(delay)` shrink) | **Shipped** — overdue correct keeps on-time increment; not the same as FSRS overdue reward | `735b96623a`; `lateCorrectAnswerDoesNotShortenTheNextInterval` |
| 2026-08-13 | Doc correction | Research/gap docs no longer describe the shipped penalty as a live bug | This file; SUMMARY / FEATURES / ARCHITECTURE / PITFALLS / SEED-004 |
| 2026-08-13 | Naming: FSRS review vs Doughnut recall | **Decided** — keep **recall**; FSRS review is a compatibility alias only | ADR 0001 § Recall not review; ADR 0003 Decision |
| 2026-08-13 | C1 correction and scheduling vocabulary | **Decided** — describe persisted state and recall-transition inputs directly; C1 is explicit elapsed-time/state cohesion, not a persisted-`nextRecallAt` subtraction bug; `RecallLog` is deferred | This file; ADR 0003; supporting research |
| 2026-08-13 | C1 persistence and migration boundary | **Decided** — keep current fields and hour precision; backfill stale `lastRecalledAt` from trustworthy Answer/Tutor-feedback timestamps; no schema or bulk-due rewrite | This file §7, O11–O12 |
| 2026-08-13 | FSRS state delivery order | **Decided** — migrate Stability/Difficulty in a later behavior slice; introduce lapses only with behavior that consumes it | This file §7, O2, O15 |
| 2026-08-13 | Strictly-future scheduling | **Decided** — a separate C3 behavior applies the invariant to every state-changing recall; exact fallback awaits confirmation | This file §7, O13 |

---

## 11. Related

- [ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy.md) — policy draft to finalize  
- [SEED-004](../seeds/SEED-004-close-spaced-repetition-scheduling-policy-gap.md) — implementation trigger after policy  
- [SUMMARY.md](./SUMMARY.md) — prior Doughnut-vs-ADR research  
- ADR 0001 — vocabulary (space setting / schedule)  
- ADR 0005 — Tutor score meaning to the Tutor (schedule effect stays in 0003)
