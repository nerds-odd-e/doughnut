# Feature Research

**Domain:** Spaced-repetition scheduling policy (ADR 0003) — retrofit of existing Doughnut scheduler  
**Project:** Doughnut v1.4 (SEED-004)  
**Researched:** 2026-08-12  
**Confidence:** HIGH

## Feature Landscape

ADR 0003 defines **safety properties** for how graded evidence moves the schedule — not a new SRS engine. For v1.4, features fall into three buckets: **policy corrections** (table stakes for closing the gap), **outcome-specific rules** (mostly built; verify and test), and **Doughnut-specific differentiators** (spelling overlap/accidental match, commissioned Tutor scores) that competitors do not offer in the same form.

### Table Stakes (Users Expect These)

Features learners and practitioners assume from any credible spaced-repetition product. Missing or violating them breaks trust in the schedule.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| **Evidence vs due-time separation** | Industry norm (Anki/FSRS treat overdue success as retention over longer elapsed time, not queue failure). ADR 0003 § Evidence and scheduling. | **MEDIUM** | **Primary gap.** `ForgettingCurve.succeeded()` still keys off deviation from planned recall (`delayInHours` vs `calculateNextRecallAt()`), conflating backlog/availability with memory. Retrofit: use **observed elapsed time since `lastRecalledAt`** for strength; keep `nextRecallAt` as operational due projection only. Depends on existing `last_recalled_at`, `next_recall_at` columns — no schema change. |
| **Correct recall always schedules forward** | After any successful grade, the tracker must not remain due at the answer instant. Universal SRS expectation. | **LOW** | Partially met. Commissioned path enforces via `CommissionedLearningSessionFeedbackScheduling.ensureNextRecallStrictlyAfterNow`. Recall paths should share the same guard after correct and accidental-match grades. |
| **No late-success penalty** | Busy learners must not be punished for clearing backlog while answering correctly — the core SEED-004 trap. | **MEDIUM** | Policy: overdue correct ≥ on-time correct for memory strength; lateness bonus optional and bounded. Replace tests that encode pre-policy semantics (`RecallServiceWithSpacedRepetitionAlgorithmTest` early/late index cases, `SpacedRepetitionEarlyRecallAdjustmentTest`). |
| **Early recall = weaker evidence, not failure** | Answering before the planned interval may grow less but must not reset learning or force immediate redue. | **LOW** | **Mostly built** (`delayInHours < 0` branch reduces increment). Re-verify after evidence refactor so early discount is retention-based, not queue-compliance-based. |
| **Incorrect recall shortens interval without permanent trap** | Failed recall should hurt, but later correct answers must restore expanding intervals. | **LOW** | **Built:** `recallFailed` cuts strength and sets explicit 12h retry; separate from persisted interval path. Verify recovery sequences in policy tests. |
| **Distinct graded outcomes drive schedule** | Boolean correct/incorrect is insufficient when product defines accidental match and overlap. | **LOW** | **Built at routing layer** (`SpellingRecallGrading`, `AnswerOutcome`). Milestone work is ensuring each outcome's **schedule effect** matches ADR — not collapsing paths in `ForgettingCurve`. |
| **User-configurable spacing table** | Doughnut "space setting" (ADR 0001); learners expect control over interval ladder. | **LOW** | **Built:** `SpacedRepetitionAlgorithm` + per-user intervals. Policy sits above this table; no change to Fibonacci/custom list UX. |
| **Post-grade transactional consistency** | Due queue (`nextRecallAt`) must match memory state after every grade in the same transaction. | **LOW** | **Built** via JPA entity update on grade. ADR defers rebuild-from-history; projection stays authoritative for queries. |
| **Policy tests on observable schedule** | Product contract is interval movement and due eligibility, not internal indexes. | **MEDIUM** | **Gap.** Add/replace tests asserting `nextRecallAt`, due-queue presence, and interval ordering — Hamcrest on timestamps and hours, not `forgettingCurveIndex` alone. |
| **Recall effort within bounds** | Optional secondary signal (thinking time) may nudge strength but never flip pass→fail. ADR § Recall effort. | **LOW** | **Built:** `ForgettingCurve.calculateThinkingTimeAdjustment` with clamped ms and sqrt curve. Audit that slow **correct** answers cannot produce zero/negative post-grade interval or strength below pre-answer floor. |
| **Frequent-failure warning (informational)** | Learners need signal when a tracker fails often; schedule must stay independent. ADR § Frequent-failure warning. | **LOW** | **Built:** ≥5 incorrect in 14 days; `ThresholdExceededResult` API (`wrongCount`, `threshold`, `periodDays`); UI alert via `useRecallAnswerHandling`; overlap excluded in `RecallPromptRepository` query. No schedule side effect — verify accidental match counting matches product intent. |

### Differentiators (Competitive Advantage)

Features that distinguish Doughnut's PKM + recall model. Not universal SRS table stakes, but **required for ADR 0003 compliance** in this product.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| **Accidental match (spelling)** | Teaches wiki-link hygiene: wrong note matched, weaker than full incorrect, no 12h relearning trap. | **LOW** | **Built:** `markAsAccidentalMatch` → `partialFail()` + normal `calculateNextRecallAt()` path (not `recallFailed`). Verify penalty ordering: accidental < incorrect on same tracker state. Resolve dialog UX is v1.1–v1.2; scheduling is backend policy. |
| **Declared overlap (spelling)** | Authored `overlaps` frontmatter + non-distinguishing answer = try again without credit or schedule mutation — unique to zettelkasten spelling recall. | **LOW** | **Built:** overlap sets `AnswerOutcome.OVERLAP`, skips `markAsRecalled`; frontend same-session retry (`useRecallAnswerHandling`). Excluded from frequent-failure count. Policy tests should assert **unchanged** `nextRecallAt` / `forgettingCurveIndex` after overlap grade. |
| **Commissioned Tutor feedback scores (0–5)** | Learning Session items graded by human Tutor, not Doughnut recall prompts; mastery-without-fluency still progresses. | **LOW** | **Built v1.3:** `CommissionedLearningSessionFeedbackPolicy` + `CommissionedLearningSessionFeedbackScheduling`. ADR-quantified strength table; no incorrect-recall relearning override; effort neutral; late session must not weaken. Milestone = audit + policy tests, not new UX. |
| **Property-keyed trackers** | Recall individual note properties with independent schedules and threshold copy. | **LOW** | **Built.** Frequent-failure message names property when `propertyKey` set. Scheduling policy applies per tracker regardless of note vs property scope. |
| **Thinking-time effort signal** | Automatic effort proxy without Anki-style four-button self-report. | **LOW** | **Built** for recall grades. Differentiator vs button-based ease; must stay bounded per ADR. Commissioned scores explicitly ignore effort. |
| **Materialized due projection with incomplete history** | Honest operational model: `nextRecallAt` is queue metadata while evidence lives in grades — without requiring event-sourced replay yet. | **MEDIUM** | Architectural stance in ADR 0003. v1.4 ships policy fix on existing snapshot model; rebuildable projection is explicitly **deferred** differentiator for a later milestone. |

### Anti-Features (Commonly Requested, Often Problematic)

Features that seem reasonable but conflict with ADR 0003, shipped anti-features, or would derail v1.4.

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| **Late-success / schedule-compliance penalty** | "They were late, so treat as weaker recall." | Conflates availability with memory; causes correct-but-overdue trap and immediate/daily loops (SEED-004). | Judge retention from elapsed time since last graded recall; due time is queue-only. |
| **FSRS / full algorithm migration in v1.4** | State-of-the-art retention optimization. | New state model, parameters, fitting, migration — unnecessary to establish policy (ADR Options: deferred). | Apply ADR policy to existing `ForgettingCurve` first; revisit FSRS after production validation. |
| **Rebuild `nextRecallAt` from answer history** | Single source of truth, no projection drift. | Legacy history incomplete/unversioned; unsafe bulk reinterpretation (ADR § Memory state). | Keep transactional projection; defer replay boundary to future phase. |
| **Frequent-failure-driven reschedule or tracker removal** | "Five failures → force re-assimilate or delete." | Violates ADR: warning is informational only; removes learner agency. | Show `wrongCount` / threshold alert; schedule unchanged. **Shipped** replacement for old re-assimilate flow. |
| **Overlap declare → forced resolve / SRS reclaim** | Close the loop on accidental match. | Locked anti-feature (PROJECT.md Out of Scope); overlap action *is* the resolution. | Optional dialog actions only; no secondary retry/credit reclaim after declare. |
| **Commissioned score → 12h forced retry** | Parity with incorrect recall. | Commissioned trackers only review on next commissioned session; short retry window is meaningless (ADR § Commissioned). | Normal interval path after score; strength adjustments per 0–5 table. |
| **Zero interval after any graded answer** | New assimilation may start at 0 spacing. | ADR forbids persisted zero interval after a grade (except pre-assimilation). | `firstPositiveSpacingHours` pattern from commissioned scheduling. |
| **Collapse outcomes to boolean correct** | Simpler scheduler code path. | Breaks accidental match, overlap, and commissioned evidence contracts. | Outcome-specific routing in `SpellingRecallGrading` / learning session record. |
| **Effort inverts outcome** | "Too slow = fail." | Violates ADR § Recall effort; punishes careful correct answers. | Bounded adjustment within correct/incorrect branches only. |
| **Policy tests on internal strength index** | Easy to assert `forgettingCurveIndex`. | ADR requires observable schedule behavior; internal representation may change. | Assert `nextRecallAt` deltas, due-queue eligibility, monotonicity properties. |
| **Frontend-side scheduling** | Faster iteration. | Schedule must be server-authoritative; client already display-only. | Backend `MemoryTrackerService` / `ForgettingCurve` only. |

## Feature Dependencies

```
[Evidence vs due-time separation]
    └──requires──> [Correct recall schedules forward]
    └──requires──> [No late-success penalty]
    └──requires──> [Early recall weaker-evidence discount]
    └──unblocks──> [Policy tests on observable schedule]

[Distinct graded outcomes]
    ├──requires──> [Spelling recall grading] (existing)
    ├──requires──> [Accidental match schedule path] (existing)
    ├──requires──> [Overlap no-op schedule] (existing)
    └──requires──> [Commissioned 0–5 schedule] (existing v1.3)

[Post-grade positive interval guard]
    └──requires──> [Evidence vs due-time separation]
    └──enhances──> [Correct recall schedules forward]
    └──enhances──> [Commissioned feedback scheduling] (pattern exists)

[Frequent-failure warning]
    └──requires──> [Incorrect recall persistence] (existing)
    └──conflicts──> [Frequent-failure-driven reschedule] (anti-feature)

[Recall effort bounds]
    └──requires──> [Correct / incorrect outcome paths]
    └──must not block──> [No late-success penalty]

[User space intervals]
    └──feeds──> [All interval paths via SpacedRepetitionAlgorithm]
    └──policy constrains──> [Safety properties only, not table replacement]
```

### Dependency Notes

- **Evidence separation requires correct-forward scheduling:** Without fixing time evidence, "schedule strictly in the future after correct" can still trap learners if strength drops while answering correctly.
- **Policy tests depend on evidence refactor:** Writing assertions against today's overdue semantics would encode the bug; replace `RecallServiceWithSpacedRepetitionAlgorithmTest` timing cases as part of the same phase.
- **Overlap / accidental match depend on spelling grading but not on each other:** Independent outcome branches; can be verified in parallel once core `ForgettingCurve` fix lands.
- **Commissioned feedback is logically independent** of recall-time evidence fix (already uses score table + `ensureNextRecallStrictlyAfterNow`) but shares **post-grade interval safety** helper extraction.
- **Frequent-failure warning conflicts with any auto-reschedule:** Must remain read-only on schedule; only incorrect grades trigger threshold check (overlap skipped; accidental match currently counts as wrong — confirm intentional).

## MVP Definition

### Launch With (v1.4 — close ADR 0003 gap)

Minimum to stop the correct-but-overdue trap and lock the policy contract. Aligns with PROJECT.md target features.

- [ ] **Evidence vs due-time separation** — Refactor success path to use retention elapsed time; stop using due deviation as negative memory evidence. *Essential: fixes SEED-004 core bug.*
- [ ] **No late-success penalty + correct-forward scheduling** — Overdue correct ≥ on-time; post-answer `nextRecallAt` strictly after grade instant. *Essential: user-visible trap removal.*
- [ ] **Policy tests (observable schedule)** — Controller or service tests through recall/learning-session boundaries asserting interval movement, not index alone. *Essential: prevents regression when representation changes.*
- [ ] **Outcome schedule verification** — Accidental (weaker than incorrect, normal path), overlap (no mutation), incorrect (outcome-only penalty), commissioned 0–5 (audit existing). *Essential: ADR is outcome-complete.*
- [ ] **Effort bounds audit** — Slow correct cannot fail or zero-interval. *Essential: ADR § Recall effort.*
- [ ] **Preserve `nextRecallAt` projection** — No history replay migration. *Essential: operational constraint.*

### Add After Validation (v1.4.x / post-approval)

- [ ] **Bounded lateness bonus** — Optional strength boost for long observed intervals (ADR allows, not required for minimal fix). *Trigger: monitoring shows intervals too conservative after trap fix.*
- [ ] **Shared `ensureNextRecallStrictlyAfterNow` helper** — DRY across recall + commissioned paths. *Trigger: refactor phase after policy green.*
- [ ] **Accidental match threshold semantics** — Explicit decision whether accidental matches count toward frequent-failure (currently count as `correct=false`). *Trigger: user feedback or ADR clarification.*

### Future Consideration (v2+)

- [ ] **FSRS or fitted scheduler** — After policy stable in production and migration strategy defined (ADR deferred option).
- [ ] **Rebuildable due-time projection** — Event store or versioned scheduler state for deterministic replay (ADR deferred).
- [ ] **Descriptive Feedback / in-app Tutor** — PROJECT.md v2+; commissioned protocol already handles offline Tutor exchange.
- [ ] **Spelling follow-ons (MCQ, fuzzy)** — SEED-001; orthogonal to scheduling policy.

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority | Existing? |
|---------|------------|---------------------|----------|-----------|
| Evidence vs due-time separation | HIGH | MEDIUM | **P1** | Gap |
| No late-success / correct-forward | HIGH | MEDIUM | **P1** | Partial |
| Policy tests (observable schedule) | HIGH | MEDIUM | **P1** | Gap |
| Overlap no schedule change | MEDIUM | LOW | **P1** | Built — verify |
| Accidental match weaker penalty | MEDIUM | LOW | **P1** | Built — verify |
| Commissioned 0–5 schedule | MEDIUM | LOW | **P1** | Built — audit |
| Incorrect recall + recovery | HIGH | LOW | **P1** | Built — verify |
| Post-grade positive interval (all paths) | HIGH | LOW | **P1** | Partial (commissioned only) |
| Effort bounds | MEDIUM | LOW | **P2** | Built — audit |
| Frequent-failure warning | MEDIUM | LOW | **P2** | Built |
| User space intervals | MEDIUM | LOW | **P2** | Built |
| Bounded lateness bonus | LOW | LOW | **P3** | Not built |
| FSRS migration | MEDIUM | HIGH | **P3** | Deferred |
| Rebuildable projection | LOW | HIGH | **P3** | Deferred |

**Priority key:** P1 = must ship in v1.4 milestone; P2 = verify/audit in v1.4, no behavioral change expected; P3 = explicitly out of v1.4 scope.

## Competitor Feature Analysis

| Feature | Anki (manual ease buttons) | FSRS (modern Anki scheduler) | Doughnut v1.4 approach |
|---------|---------------------------|-------------------------------|------------------------|
| **Overdue review handling** | Falling behind: delay factored into next interval when returning — not treated as failure solely for lateness ([Anki Manual — Falling Behind](https://docs.ankiweb.net/studying.html#falling-behind)). | Retention judged from elapsed time; schedule compliance separate ([FSRS wiki](https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm)). | ADR 0003 aligns with this norm; retrofit `ForgettingCurve`, no library import. |
| **Answer grading inputs** | Again / Hard / Good / Easy self-report | FSRS uses rating + elapsed time | Doughnut: objective spelling grade + optional `thinkingTimeMs`; commissioned Tutor 0–5. |
| **Failure recovery** | Lapse steps then re-graduate | Stability/retrievability model | Incorrect → strength cut + 12h session retry; correct chain must re-expand. |
| **Ambiguous / wrong-note match** | N/A (card-level) | N/A | **Differentiator:** accidental match + overlap declare with distinct schedule rules. |
| **Human tutor session scores** | N/A | N/A | **Differentiator:** commissioned learning session feedback as first-class evidence (ADR 0003 § Commissioned). |
| **Failure streak UX** | Leech flags / suspend suggestions (deck-dependent) | Varies | Frequent-failure **warning only** — no auto-suspend (intentional product choice). |
| **Configurable intervals** | Per-deck options / FSRS parameters | Desired retention target | User "space setting" list (`SpacedRepetitionAlgorithm`); policy constrains safety only. |
| **Due queue representation** | Card due dates | Due dates from model | Materialized `nextRecallAt` on `memory_tracker`; conceptual separation from evidence. |

## How ADR 0003 Scheduling Features Typically Work

Reference model for roadmap phasing — each row is one **observable behavior** the implementation must preserve.

| Policy area | Typical mechanism | Doughnut touchpoint | v1.4 status |
|-------------|-------------------|---------------------|-------------|
| **Memory evidence** | Last grade outcome + time since last grade | `lastRecalledAt`, `Answer` / Tutor score | Exists; success path must use retention interval |
| **Due metadata** | `nextRecallAt` for queue ordering only | `memory_tracker.next_recall_at`, due queries | Exists; stop feeding into strength penalty |
| **Correct** | Increase strength → longer interval; never due now | `recalledSuccessfully` → `ForgettingCurve.succeeded` | **Fix primary** |
| **Incorrect** | Decrease strength; optional short same-session retry | `recallFailed` (12h) | Aligned |
| **Accidental match** | Smaller decrease; normal next interval | `partialFail` + `calculateNextRecallAt` | Aligned — test ordering |
| **Overlap** | No strength/schedule change; retry in session | Skip `markAsRecalled` | Aligned — test no mutation |
| **Commissioned 0–5** | Quantized strength delta; always forward for mastery | `CommissionedLearningSessionFeedbackPolicy` | Aligned — audit |
| **Effort** | Small bounded nudge inside outcome branch | `thinkingTimeMs` → `calculateThinkingTimeAdjustment` | Aligned — audit bounds |
| **Frequent failure** | Count wrong grades in window; warn only | `getThresholdExceeded` API + UI | Aligned |

## Sources

- `docs/adrs/0003-spaced-repetition-scheduling-policy.md` — authoritative policy (**HIGH**)
- `.planning/seeds/SEED-004-close-spaced-repetition-scheduling-policy-gap.md` — milestone trigger and scope (**HIGH**)
- `.planning/PROJECT.md` — v1.4 target features and locked anti-features (**HIGH**)
- `backend/src/main/java/com/odde/doughnut/entities/ForgettingCurve.java`, `MemoryTracker.java` — current scheduling mechanics (**HIGH**, codebase)
- `backend/src/main/java/com/odde/doughnut/services/SpellingRecallGrading.java` — outcome routing (**HIGH**, codebase)
- `backend/src/main/java/com/odde/doughnut/algorithms/CommissionedLearningSessionFeedbackPolicy.java` — Tutor score table (**HIGH**, codebase)
- `backend/src/test/java/com/odde/doughnut/services/RecallServiceWithSpacedRepetitionAlgorithmTest.java` — legacy timing assertions to replace (**HIGH**, codebase)
- [Anki Manual — Studying / Falling Behind](https://docs.ankiweb.net/studying.html) — industry table-stakes for backlog behavior (**MEDIUM**)
- [FSRS Algorithm wiki](https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm) — evidence-vs-schedule norm (**MEDIUM**)
- `.planning/research/STACK.md` — companion stack research for v1.4 (**HIGH**)

---
*Feature research for: Doughnut v1.4 SRS scheduling policy (ADR 0003)*  
*Researched: 2026-08-12*
