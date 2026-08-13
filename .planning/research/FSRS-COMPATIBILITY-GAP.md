# Doughnut ↔ open FSRS — compatibility gap & ADR 0003 discussion

**Status:** Open for discussion (feeds finalizing Proposed [ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy.md))  
**Created:** 2026-08-12  
**Sources:** ADR 0003 draft; SEED-004; `.planning/research/{SUMMARY,ARCHITECTURE,FEATURES,PITFALLS,STACK}.md`; live scheduler (`MemoryTracker`, `ForgettingCurve`, `SpacedRepetitionAlgorithm`, commissioned feedback policy); [FSRS algorithm wiki](https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm); [ts-fsrs](https://open-spaced-repetition.github.io/ts-fsrs/)

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

Open FSRS is a **DSR memory model**: per-card **Difficulty (D)** and **Stability (S)**, with **Retrievability (R)** derived from **elapsed time since last review** and S. Grades are **Again / Hard / Good / Easy**. Due time is a projection from desired retention and S — not evidence of forgetting.

Doughnut today is a **single-index + user interval table** scheduler. The important semantic bug is that **success strength still consumes schedule deviation** (`gradedAt − nextRecallAt`) instead of **observed retention** (`gradedAt − lastRecalledAt`). That is incompatible with both FSRS and the current ADR 0003 draft.

**Compatibility strategy assumed for discussion (not decided):**

1. **Now:** Lock an ADR product contract that is **FSRS-compatible in evidence, grades, and safety properties**.
2. **Near term:** Fix the existing Doughnut model to obey that contract (SEED-004) — no FSRS library required.
3. **Later (optional):** Swap internals to open FSRS (or D/S state) without rewriting product rules.

ADR 0003 should describe **(1)** fully. Whether **(3)** is a committed destination vs a deferred option is an open issue below.

---

## 2. Concept map

| Open FSRS | Doughnut today | Compatibility note |
|-----------|----------------|--------------------|
| **Stability (S)** — days until R ≈ target (default 90%) | `forgettingCurveIndex` → `SpacedRepetitionAlgorithm` spacing table → hours | Rough analog of “how far out”; not probabilistic; no R(t,S) |
| **Difficulty (D)** ∈ [1, 10] | *None* | Major model gap; mean-reversion / “ease hell” avoidance absent |
| **Retrievability (R)** = f(elapsed, S) | *Not computed*; early path uses due deviation | **Primary semantic gap** |
| **Elapsed days/hours since last review** | `lastRecalledAt` exists | Field present; success path does not use it as evidence |
| **Due / next interval** | `nextRecallAt` | Same role: queue projection, not memory evidence |
| **Grade G** ∈ {1 Again, 2 Hard, 3 Good, 4 Easy} | Correct / incorrect; accidental match; overlap; Tutor score 0–5; thinking-time continuous | Richer product grades; need explicit mapping onto (or extension of) FSRS grades |
| **Request retention** | Implicit via fixed space list | FSRS schedules to a retention target; Doughnut walks a table |
| **Fitted parameters w[]** | User-editable space intervals | Different knobs; both are configuration |
| **Learning / relearning steps** | Incorrect → fixed **12h** override | Analog of short relearning window; not post-lapse S |
| **Card state** New / Learning / Review / Relearning | Assimilated + active tracker; no explicit learning ladder | Optional FSRS surface |
| **Review log / optimizer** | Partial answer history; not versioned for replay | Rebuild/optimize deferred (ADR already) |

---

## 3. Semantic alignment (what “mostly compatible” should mean)

These are the FSRS principles that ADR 0003 should lock as Doughnut product law. Implementation formula can differ.

| # | FSRS principle | ADR 0003 draft | Live Doughnut | Status |
|---|----------------|----------------|---------------|--------|
| P1 | Memory update uses **grade + elapsed since last review** | States this | Success uses **due deviation** | **Gap (code)** |
| P2 | Due time is **scheduling metadata**, not negative evidence | States this | Contradicted by P1 bug | **Gap (code)** |
| P3 | Successful overdue recall is **not failure**; lower R can **increase** next S (bounded) | Overdue ≥ on-time; lateness bonus optional/bounded | No overdue credit; early-only shrink | **Gap (code)**; bonus semantics open |
| P4 | Successful review → SInc ≥ 1 (Hard/Good/Easy) relative to failure | Correct grows; post-grade not immediately due | Usually grows; trap possible at floor / zero interval | Mostly; enforce strictly-future |
| P5 | Failure updates via **post-lapse** path, timing-neutral as “late/early” | Incorrect timing-neutral; short retry separate | `failed()` + 12h — directionally OK | Aligned enough; exact shape open |
| P6 | Grades are ordered evidence (Again < Hard < Good < Easy) | Correct / incorrect / accidental / Tutor 0–5 | Distinct paths exist | **Gap (model)** — mapping undecided |
| P7 | Difficulty is separate from stability | Not in ADR | Absent | **Open** whether target shape includes D |
| P8 | Interval from retention target + S | Not required; freedom clause | Space table | **Open** — keep table forever vs FSRS intervals later |
| P9 | Same-day / short-term reviews handled specially | Early = weaker evidence, not reset | Early discount vs due; thinking-time | Partial; align with short-term rules |
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
| **Overlap** | No mutation | No FSRS analog — **no review event** (not a grade) | Keep |
| **Tutor score 5** | +1.2× increment | Near **Easy** | Mapping table open |
| **Tutor score 4** | +1.0× | **Good** | |
| **Tutor score 3** | +0.8× | Between Hard and Good (or Hard+) | |
| **Tutor score 2** | −20% accumulated | Soft fail / hard success boundary — **not** pure Again | |
| **Tutor score 1** | −50% accumulated | Closer to Again without full reset | |
| **Tutor score 0** | Reset to initial | Strong Again / new-like | |
| **Thinking time** | Continuous ±sqrt adjustment on success | FSRS folds into Hard/Easy buttons | **Open** |

**Compatibility claim to validate in discussion:** Doughnut may keep product-specific outcomes (accidental, overlap, Tutor 0–5) **if** each outcome has a defined effect on a FSRS-shaped memory update `(grade-equivalent, elapsed)` and never reintroduces due-deviation as evidence.

---

## 5. Code gaps (current Doughnut vs FSRS-compatible policy)

Already detailed in research SUMMARY / ARCHITECTURE; summarized for tracking:

| ID | Gap | Evidence | FSRS / ADR impact |
|----|-----|----------|-------------------|
| C1 | Success time base is due deviation | `MemoryTracker.recalledSuccessfully` → `getDiffInHours(..., calculateNextRecallAt())` | Breaks P1–P3 |
| C2 | Early-only shrink; no overdue retention credit | `ForgettingCurve.succeeded` only adjusts when `delayInHours < 0` | Breaks P3 |
| C3 | Strictly-future due not universal on recall success | Commissioned has helper; success path relies on table | Trap risk |
| C4 | Parallel entry points | Spelling / MCQ / commissioned / manual mark | Divergent semantics risk |
| C5 | Policy tests on index floats | Early-recall tests encode old semantics | Miss schedule traps |
| C6 | Frequent-failure count vs accidental | Query treats `correct=false`; ADR says incorrect-only | Product decision |
| C7 | No Difficulty state | Single index only | Blocks full FSRS parity |
| C8 | Incomplete review history for optimizer/replay | Known | FSRS fitting / rebuild deferred |

---

## 6. What ADR 0003 already gets right (keep)

Do not reopen unless a stronger reason appears:

1. Evidence vs schedule separation (P1–P2).
2. Correct overdue must not be worse than on-time; no late-success penalty.
3. Early correct = weaker evidence, not reset / immediate due.
4. Incorrect timing-neutral; short retry separate from long schedule.
5. Accidental match weaker than incorrect; normal interval path.
6. Overlap = no schedule mutation; not frequent-failure.
7. Commissioned 0–5 quantified table; effort neutral; never due at score instant.
8. Frequent-failure warning informational only.
9. Policy tests assert **observable schedule**, not internal indexes.
10. Rebuild-from-incomplete-history deferred; keep transactional `nextRecallAt`.
11. No requirement to ship FSRS formulas in the first implementation.

---

## 7. Open issues (discussion queue)

Check off as decided. Record the decision in one line; then fold into ADR 0003.

### O1. What does “mostly compatible with open FSRS” commit us to?

- [ ] **A.** Semantic compatibility only (evidence, grade ordering, safety) — formulas stay Doughnut until a later ADR  
- [ ] **B.** ADR names **D / S / R** as the target memory vocabulary now; implementation may still use index+table as a stand-in  
- [ ] **C.** ADR commits to adopting open FSRS (library or reimplementation) as the eventual engine  

**Why it matters:** Determines how much model language enters ADR 0003 vs staying “implementation freedom.”  
**Recommendation to discuss:** **B** — vocabulary compatibility without forcing migration.

### O2. Memory state shape in the ADR

- [ ] Keep single opaque “memory strength” + spacing table (current draft)  
- [ ] Adopt **Stability** (and optional **Difficulty**) as named product concepts; strength index becomes transitional  
- [ ] Require Difficulty in v1 of the policy  

**Why it matters:** FSRS’s main structural novelty vs SM-2 is D⊥S. Without D, “compatible” is mostly about elapsed-time evidence.  
**Recommendation to discuss:** Stability-compatible wording now; Difficulty **allowed/reserved**, not required for first retrofit.

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

**Why it matters:** ADR currently allows a bounded bonus but does not require one. SEED-004 can ship without bonus.  
**Recommendation to discuss:** Lock **minimum bar** in ADR now; state FSRS-like bounded bonus as **allowed future**, not v1 requirement.

### O6. Interval source: space table vs request retention

- [ ] Space setting remains the interval source indefinitely  
- [ ] Space setting remains until FSRS migration; ADR allows retention-target intervals later  
- [ ] ADR should already prefer retention-target intervals as the target shape  

**Why it matters:** Biggest practical fork for “eventual FSRS.”  
**Recommendation to discuss:** Keep space setting as current input; ADR explicitly allows replacing table with retention-driven intervals **without** changing evidence rules.

### O7. Incorrect / relearning window

- [ ] Keep fixed 12h post-incorrect projection  
- [ ] Align wording with FSRS relearning steps (configurable short steps) while keeping 12h as default  
- [ ] Move to post-lapse stability only (no forced short due) for all tracker types  

**Note:** Commissioned path correctly avoids 12h (session cadence). Keep that exception.  
**Recommendation to discuss:** Keep 12h for ordinary recall; name it as **relearning projection**, not memory evidence.

### O8. Accidental match ↔ frequent-failure counting

- [ ] Count only **incorrect** (ADR draft) — change query  
- [ ] Count incorrect + accidental match  
- [ ] Separate thresholds  

**Why it matters:** Already flagged in research SUMMARY; blocks “Aligned” claim on warning.  
**Recommendation to discuss:** Incorrect-only (ADR draft).

### O9. Manual / admin schedule paths

- [ ] `markAsRecalled` / `updateForgettingCurve` must obey the same evidence rules  
- [ ] Document as escape hatches outside policy  
- [ ] Remove or gate admin bypasses  

**Recommendation to discuss:** Same evidence rules whenever a grade is recorded; pure admin edits are explicit exceptions and must not invent due-deviation penalties.

### O10. ADR scope sentence on FSRS

Current draft: “This ADR does not require FSRS.”

Proposed replacement options:

- [ ] Keep as-is (FSRS only a reference)  
- [ ] “Target shape is **open-FSRS-compatible**; shipping open FSRS code is not required by this ADR.”  
- [ ] “Doughnut will migrate to open FSRS; this ADR is the product contract that migration must preserve.”  

**Recommendation to discuss:** Middle option — compatibility without migration mandate.

### O11. Same-day / early reviews

FSRS short-term stability vs Doughnut early discount relative to **due**.

- [ ] After C1 fix, early = elapsed ≪ expected interval → weaker growth (retention-based)  
- [ ] Explicit same-day rule (cap growth / short-term S) separate from multi-day  

**Recommendation to discuss:** Retention-based early weakening first; optional same-day rule later if same-day reviews are common.

### O12. Monitoring / rollout

- [ ] What observables after policy change? (interval length distribution, success rate, % due-immediate after correct)  
- [ ] Any one-time data fix? (ADR says no bulk reinterpret — confirm)  

**Recommendation to discuss:** No bulk `nextRecallAt` rewrite; monitor post-release as ADR Consequences already say.

---

## 8. Suggested ADR 0003 final-shape outline (for discussion)

If O1=B and O10=middle option, ADR Decision could be organized as:

1. **Compatibility stance** — open-FSRS-compatible product contract; formula/library optional later.  
2. **Evidence** — grade + elapsed since last graded recall (+ optional bounded effort); never due deviation.  
3. **Memory state** — conceptual Stability (implementation may use index+table); Difficulty reserved.  
4. **Due projection** — materialized `nextRecallAt`; authoritative while history incomplete.  
5. **Grade catalog** — incorrect / correct / accidental / overlap / Tutor 0–5 with FSRS map appendix.  
6. **Safety properties** — strictly-future after grade; overdue ≥ on-time; early not reset; incorrect recoverable; commissioned table; frequent-failure informational.  
7. **Configuration** — space setting today; retention-target intervals allowed later without changing §Evidence.  
8. **Out of scope** — parameter fitting, history replay rebuild, adopting a specific FSRS version’s weights.

*(Do not treat this outline as accepted — fold in only after open issues are decided.)*

---

## 9. Decision log

| Date | Issue | Decision | Where recorded |
|------|-------|----------|----------------|
| *(none yet)* | | | |

---

## 10. Related

- [ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy.md) — policy draft to finalize  
- [SEED-004](../seeds/SEED-004-close-spaced-repetition-scheduling-policy-gap.md) — implementation trigger after policy  
- [SUMMARY.md](./SUMMARY.md) — prior Doughnut-vs-ADR research  
- ADR 0001 — vocabulary (space setting / schedule)  
- ADR 0005 — Tutor score meaning to the Tutor (schedule effect stays in 0003)
