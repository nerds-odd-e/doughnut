# Pitfalls Research

**Domain:** v1.4 SRS scheduling policy (ADR 0003) — adding policy-aligned scheduling to existing Doughnut ForgettingCurve + commissioned feedback
**Researched:** 2026-08-12
**Confidence:** HIGH (ADR + live code paths); MEDIUM on rollout monitoring (no production A/B yet)

## Critical Pitfalls

### Pitfall 1: Conflating schedule deviation with memory evidence

**What goes wrong:**
Correct recalls are graded using deviation from `nextRecallAt` (early = negative `delayInHours` reduces the success increment in `ForgettingCurve.succeeded`) while lateness is implicitly “free.” Worse, the same `delayInHours` knob mixes **queue compliance** with **retention interval** — ADR 0003 requires memory evidence to be graded outcome + **elapsed time since the previous graded recall**, not deviation from the due projection.

**Why it happens:**
`MemoryTracker.recalledSuccessfully` computes `getDiffInHours(current, calculateNextRecallAt())` and passes that single value into `ForgettingCurve.succeeded`. That is the natural fix for “early recall” but it re-encodes schedule metadata into strength math. Developers also mirror `SpacedRepetitionEarlyRecallAdjustmentTest` without adding late/overdue policy tests.

**How to avoid:**
Split inputs explicitly: (1) graded outcome, (2) observed elapsed hours since `lastRecalledAt`, (3) schedule projection only for eligibility/display. Apply early-weaker-evidence bounds against elapsed time or a dedicated early factor — never negative strength solely because the answer was overdue. Replace tests that assert lateness weakens strength.

**Warning signs:**
- `delayInHours` still derived from `calculateNextRecallAt()` inside strength adjustment
- New tests name “late” but assert `forgettingCurveIndex` drops without an incorrect outcome
- Busy users with growing backlog still shrink intervals after correct answers

**Phase to address:**
**Phase 1 — Evidence vs schedule separation** (core `ForgettingCurve` / `MemoryTracker.recalledSuccessfully` refactor)

---

### Pitfall 2: Immediate-or-daily trap after correct answers

**What goes wrong:**
A tracker stays due at the answer instant or enters a short-interval loop: `nextRecallAt <= now` after a correct grade, or `getRepeatInHours()` returns 0 repeatedly so every correct answer leaves the tracker on the daily/immediate queue.

**Why it happens:**
Multiple mechanisms stack: floor at `DEFAULT_FORGETTING_CURVE_INDEX` with zero spacing index; early penalty + thinking-time penalty driving index to floor without a mandated positive post-grade interval; `recallFailed` forcing 12h while success path uses `calculateNextRecallAt()` which can still yield 0h; commissioned score 0 resets index without `ensureNextRecallStrictlyAfterNow` on every path.

**How to avoid:**
Policy gate after every state-changing grade: `nextRecallAt` must be strictly after the grade timestamp. If computed interval is 0, bump to first positive spacing hour (mirror `CommissionedLearningSessionFeedbackScheduling.ensureNextRecallStrictlyAfterNow`). Add simulation tests: N correct answers with large backlog delay must monotonically lengthen intervals.

**Warning signs:**
- E2E/controller tests only check index, not `nextRecallAt > gradedAt`
- `markAsRecalled(true)` at due time leaves `nextRecallAt` equal to `now`
- Recall queue shows same tracker every session despite only correct answers

**Phase to address:**
**Phase 2 — Post-grade schedule safety** (strictly-future `nextRecallAt` invariant across all grade paths)

---

### Pitfall 3: Parallel scheduling entry points with divergent semantics

**What goes wrong:**
Policy holds in one path but not another: spelling vs understanding MCQ, manual `MemoryTrackerController.markAsRecalled`, `updateForgettingCurve` adjustment API, commissioned `recordFeedback`, accidental-match branch, overlap no-op, and legacy `answerSpelling` without recall prompt.

**Why it happens:**
Scheduling is scattered: `MemoryTracker` entity methods, `MemoryTrackerService`, `SpellingRecallGrading`, `CommissionedLearningSessionFeedbackScheduling`, `RecallQuestionService`, and controller shortcuts. Each path evolved independently in v1.1–v1.3.

**How to avoid:**
One domain seam for “apply graded evidence → update strength + projection” with outcome-specific strategies. Route every mutating path through it; delete or delegate bypasses. Controller test matrix: each outcome × each entry point asserts the same observable schedule movement.

**Warning signs:**
- New logic only in `ForgettingCurve` but commissioned feedback still applies scores directly
- `updateForgettingCurve` used to “fix” bugs instead of fixing the grade seam
- Spelling overlap skips scheduling but accidental match uses `partialFail` while understanding path differs

**Phase to address:**
**Phase 1** (seam introduction) + **Phase 3–5** (outcome-specific strategies wired through the seam)

---

### Pitfall 4: Treating accidental match as incorrect recall

**What goes wrong:**
Accidental match uses `partialFail()` (half incorrect penalty) and/or 12h failure reschedule patterns; counts toward frequent-failure threshold as plain `correct = false`; scheduling does not follow the “normal interval path” with weaker negative evidence.

**Why it happens:**
`markAsAccidentalMatch` predates ADR outcome taxonomy. `countWrongAnswersSinceForMemoryTracker` counts any `correct = false` except `OVERLAP`. Tests like `shouldStillCountAccidentalMatchTowardWrongAnswerThreshold` encode old semantics.

**How to avoid:**
Accidental match: weaker strength reduction than incorrect, schedule via success-like interval path (not `recallFailed` 12h override). Exclude `ACCIDENTAL_MATCH` from frequent-failure wrong count (ADR: only **incorrect** recalls). Replace threshold tests accordingly.

**Warning signs:**
- Accidental match sets `nextRecallAt` to now+12h
- Frequent-failure warning fires after accidental matches only
- `partialFail()` still the sole accidental-match implementation

**Phase to address:**
**Phase 3 — Accidental match scheduling alignment**

---

### Pitfall 5: Overlap mutates schedule or recall credit

**What goes wrong:**
Overlap try-again flow increments `recallCount`, shifts `forgettingCurveIndex`, or moves `nextRecallAt` — violating ADR “do not change schedule fields; allow same-session retry.”

**Why it happens:**
Overlap handling lives in spelling grading beside normal `markAsRecalled`; a refactor that “always increments recall count on any answer” breaks overlap. Frontend try-again might double-grade.

**How to avoid:**
Keep overlap branch as explicit no-op on tracker fields; only retry path applies normal rules. Lock with controller tests asserting unchanged tracker snapshot (already in `RecallPromptOverlapTryAgainTests` — treat as regression contract).

**Warning signs:**
- `recallCount` increases on overlap grade
- Overlap outcome persists tracker mutation in service layer
- Try-again submits without clearing overlap guard

**Phase to address:**
**Phase 4 — Overlap grading verification** (mostly shipped v1.1–v1.2; regression gate during Phase 1 seam refactor)

---

### Pitfall 6: Commissioned feedback as a second scheduler

**What goes wrong:**
Tutor scores adjust index via `CommissionedLearningSessionFeedbackPolicy` but skip unified effort/timing rules; score 0 leaves tracker due at record instant; late session applies hidden timing penalty; invalid scores silently no-op (`default -> currentIndex`); effort from session duration leaks into strength.

**Why it happens:**
v1.3 shipped commissioned MVP with ADR 0003 table copied into policy class, parallel to `ForgettingCurve`. `recordFeedback` sets fields directly without shared post-grade safety gate on all scores.

**How to avoid:**
Route commissioned scores through the same scheduling seam with ADR table as score→strength mapping only. Always run strictly-future `nextRecallAt` enforcement (including score 0). Neutral effort for commissioned path (ADR § commissioned). Property tests: scores 3–5 always grow; score 0 never leaves `nextRecallAt <= now`; late record time does not reduce index vs on-time same score.

**Warning signs:**
- `CommissionedLearningSessionFeedbackScheduling` still sets tracker fields directly after Phase 1
- Tests assert index only, not schedule horizon
- Score 9 leaves tracker unchanged (silent) instead of validation error

**Phase to address:**
**Phase 5 — Commissioned feedback scheduling alignment**

---

### Pitfall 7: Effort inverts or dominates correct outcomes

**What goes wrong:**
Slow correct answers lose more strength than fast incorrect ones; thinking-time sqrt penalty + early discount + failure decrement compound; missing thinking time treated as punitive; commissioned session duration mistaken for effort.

**Why it happens:**
`calculateThinkingTimeAdjustment` applies negative adjustment for slow thinking on **every** success path. Effort is not bounded relative to outcome polarity in one place.

**How to avoid:**
Cap effort adjustment so a correct outcome cannot drop below on-time correct without effort. Missing/null thinking time = 0 adjustment (already partially true — preserve). Commissioned path: hard neutral effort. Assert policy tests: correct + max slow thinking still schedules strictly in future and does not floor index.

**Warning signs:**
- Correct + 60s thinking yields next interval shorter than incorrect at due time
- Effort tests assert index ordering without checking schedule trap
- Session wall-clock wired into commissioned grading

**Phase to address:**
**Phase 1** (effort bounds with evidence separation) + **Phase 5** (commissioned effort neutral)

---

### Pitfall 8: Frequent-failure warning changes scheduling

**What goes wrong:**
Warning path deletes tracker, blocks recall, extends short interval, or auto-removes from tracking when threshold exceeded — ADR requires informational warning only with live `wrongCount`, `threshold`, `periodDays`.

**Why it happens:**
`markAsRecalled` returns `isThresholdExceeded` to UI; historical product may have tied threshold to deletion. Wrong-count query counts accidental match and MCQ wrongs broadly.

**How to avoid:**
Threshold API read-only for schedule. Count only **incorrect** recalls in rolling window; exclude overlap (already) and accidental match. Warning on each incorrect while ≥ threshold; no confirm/delete. Property trackers: warning names property.

**Warning signs:**
- `MemoryTrackerTrackingControllerTest` expects deletion on threshold (must not)
- Wrong-count SQL lacks outcome filter for accidental match
- Warning shown on overlap or accidental match only

**Phase to address:**
**Phase 6 — Frequent-failure warning (informational only)**

---

### Pitfall 9: Policy tests assert internal index instead of observable schedule

**What goes wrong:**
Tests lock `forgettingCurveIndex` magic numbers (`190.0f`, `120.0f`) and pass while user-visible due work regresses. Internal representation changes break tests without catching policy violations. Legacy tests require late correct to weaken strength.

**Why it happens:**
Existing suite (`SpacedRepetitionEarlyRecallAdjustmentTest`, `RecallServiceWithSpacedRepetitionAlgorithmTest`, accidental-match index assertions) optimized for ForgettingCurve mechanics. ADR explicitly says policy tests assert schedule movement.

**How to avoid:**
Introduce policy-level tests: given tracker snapshot + grade event → assert `nextRecallAt` delta, due/not-due at grade instant, monotonic interval growth under correct streak, ordering constraints (late correct ≥ on-time correct strength **as schedule horizon**). Keep algorithm unit tests for pure math, not as policy gate.

**Warning signs:**
- New ADR work only updates float index expectations
- No test covers overdue correct backlog scenario end-to-end
- CI green while recall queue still traps busy user fixture

**Phase to address:**
**Phase 7 — Policy test suite** (observable schedule behavior); migrate legacy tests in Phase 1–2

---

### Pitfall 10: Unsafe due-time rebuild or migration from incomplete history

**What goes wrong:**
Bulk recompute `nextRecallAt` from recall prompts, reinterpret historical grades under new policy, or drop projection assuming history is complete — due queue corrupts, learners lose or gain spurious due items.

**Why it happens:**
ADR defers rebuild-from-history but encourages “fixing” data. Developers may add Flyway migration re-running scheduler over `recall_prompt` rows without versioned policy or complete evidence.

**How to avoid:**
No bulk reschedule migration in v1.4. Update projection **transactionally on each new grade** only. If seeding needed, use tracker snapshot fields, not replay. Document that legacy strengths were produced under old semantics — monitor post-release, don’t mass-rewrite.

**Warning signs:**
- Flyway script touches `next_recall_at` for all rows
- Migration reads answer history without timestamp/outcome completeness checks
- “Replay” job in background without ADR replay boundary

**Phase to address:**
**Phase 1–2** (explicit non-goal); **post-ship monitoring** milestone wrap-up

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Patch only `ForgettingCurve.succeeded` | Fast fix for late-success trap | Other entry paths still wrong | Never as sole fix |
| Assert `forgettingCurveIndex` in policy tests | Matches existing tests | Brittle; misses schedule traps | Algorithm unit tests only |
| Keep commissioned policy class separate | Ships v1.3 behavior unchanged | Two schedulers diverge | Only until Phase 5 seam merge |
| Skip overdue backlog simulation | Smaller test diff | Trap regressions undetected | Never for v1.4 close-out |
| `updateForgettingCurve` for admin tweaks | Quick manual repair | Bypasses policy evidence rules | Admin-only, never user recall path |
| Defer ADR approval while coding | Parallel progress | Product ships against Proposed ADR | Only with explicit drift checks vs ADR text |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| Spelling recall API | Grade overlap/accidental in controller DTO layer without tracker seam | Grading outcome → single scheduling seam |
| Understanding MCQ recall | Only spelling paths get ADR fix | `RecallQuestionService.updateMemoryTrackerAfterAnsweringQuestion` through same seam |
| Learning Session record | Score applied only to index; `nextRecallAt` from stale `lastRecalledAt` | Set `lastRecalledAt`, apply score mapping, enforce strictly-future projection |
| Session Item without Feedback | Treat abandoned item as failure | Tracker unchanged; item abandoned (ADR) — no phantom failure penalty |
| Manual `markAsRecalled` API | Used in tests/production to “fix” schedule without policy | Restrict or route through seam; document as non-grade ops only |
| Frequent-failure API | UI interprets `thresholdExceeded` as blocking | Informational warning; scheduling independent |
| Recall queue / half-day alignment | Eligibility uses `nextRecallAt` while strength uses different clock | Keep projection authoritative for due lookup; don’t feed alignment into strength |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Policy simulation tests over full user history | Slow CI backend suite | Fixture “busy backlog” scenarios with crafted timestamps, not 1000-prompt replay | >~50 graded events per test |
| Per-grade wrong-count query for warning | Extra DB hit on every incorrect | Acceptable at current scale; batch only if profiling shows hot path | Very high incorrect rate bots (edge) |
| Recompute due for all trackers on deploy | Migration timeout, wrong due flood | No bulk recompute in v1.4 | Any production deploy |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Manual mark-as-recalled without auth audit | User or script manipulates strength | Keep existing `AuthorizationService` on controller; seam does not widen access |
| Threshold API leaks other users’ wrong counts | Information disclosure | Per-tracker auth on `threshold-exceeded` endpoint (existing pattern) |
| Commissioned score injection outside 0–5 | Scheduler undefined behavior / bypass | Validate in `LearningSessionService` before `recordFeedback` |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Warning feels like punishment | Learners avoid recall when threshold exceeded | Informational text only; no schedule change |
| Correct overdue still feels “punished” if queue stays huge | Mistrust of SRS | Intervals must lengthen on correct; explain backlog is availability not failure |
| Accidental match warning conflated with “wrong” | Confusing frequent-failure counts | Separate messaging for accidental match vs incorrect |
| Commissioned score 0 immediate re-due | Tutor session feels futile | Strictly-future schedule even on reset |
| Property tracker warning without property name | User can’t tell which facet failed | Include `propertyKey` in warning payload (ADR) |

## "Looks Done But Isn't" Checklist

- [ ] **Late correct recall:** Often missing overdue backlog scenario — verify correct answer after multi-day delay lengthens `nextRecallAt` and does not floor index solely for lateness
- [ ] **Early correct recall:** Often missing strict future check — verify `nextRecallAt > gradedAt` even when answering immediately before due
- [ ] **Accidental match:** Often missing schedule path — verify not `recallFailed` 12h pattern; uses normal interval path with weaker penalty
- [ ] **Overlap:** Often missing recall-count guard — verify tracker fields unchanged; retry grades under normal rules
- [ ] **Commissioned score 0:** Often missing post-grade due check — verify not due at record instant
- [ ] **Commissioned scores 3–5:** Often missing monotonic growth — verify interval expands vs prior due horizon
- [ ] **Frequent-failure:** Often still deletes or blocks — verify schedule identical with/without threshold; accidental match excluded from count
- [ ] **Effort:** Often missing slow-correct trap test — verify slow correct still strictly future schedule
- [ ] **All entry points:** Often only spelling tested — verify understanding MCQ and commissioned record
- [ ] **Projection consistency:** Often updates index without `nextRecallAt` — verify transactional pair after every grade

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Conflated evidence/schedule shipped | MEDIUM | Hotfix seam; add policy tests; no bulk migration — per-tracker heals on next grade |
| Immediate-due trap in production | MEDIUM | Fix post-grade gate; optional one-off admin adjust for affected users if support tickets cluster |
| Dual scheduler divergence | MEDIUM | Merge commissioned into seam; regression suite for all scores |
| Wrong bulk migration | HIGH | Restore `next_recall_at` from backup; never replay without replay boundary |
| Accidental match threshold miscount | LOW | Fix SQL outcome filter; warning counts self-correct on rolling window |
| Legacy index-only tests | LOW | Rewrite assertions to schedule observables; keep index tests as algorithm units |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| Schedule deviation as evidence | Phase 1 — Evidence vs schedule separation | Late correct does not weaken vs on-time; elapsed vs due inputs split in code |
| Immediate/daily trap | Phase 2 — Post-grade schedule safety | `nextRecallAt > gradedAt` after every outcome; backlog correct streak test |
| Divergent entry points | Phase 1 seam + Phases 3–5 wiring | Matrix test per entry point × outcome |
| Accidental match as incorrect | Phase 3 — Accidental match alignment | No 12h failure path; weaker than incorrect; threshold excludes accidental |
| Overlap schedule mutation | Phase 4 — Overlap verification | `RecallPromptOverlapTryAgainTests` + seam refactor regression |
| Commissioned second scheduler | Phase 5 — Commissioned feedback alignment | Score 0–5 schedule tests; late session neutral |
| Effort inversion | Phase 1 + Phase 5 | Slow correct still future due; commissioned effort neutral |
| Warning changes schedule | Phase 6 — Frequent-failure warning | Incorrect-only count; no deletion; API fields match ADR |
| Index-only policy tests | Phase 7 — Policy test suite | CI policy module asserts horizons, not magic floats |
| Unsafe history rebuild | Phase 1–2 non-goal + ship monitoring | No migration touching mass `next_recall_at`; dashboard interval drift |

### Suggested v1.4 phase order (pitfall-driven)

1. **Phase 1 — Evidence vs schedule separation** — Pitfalls 1, 3 (foundation), 7 (effort bounds)
2. **Phase 2 — Post-grade schedule safety** — Pitfalls 2, 10 (no bulk replay)
3. **Phase 3 — Accidental match scheduling** — Pitfall 4
4. **Phase 4 — Overlap regression gate** — Pitfall 5 (verify during seam work)
5. **Phase 5 — Commissioned feedback alignment** — Pitfalls 6, 7 (commissioned)
6. **Phase 6 — Frequent-failure warning** — Pitfall 8
7. **Phase 7 — Policy test suite** — Pitfall 9 (continuous, but close-out gate)

## Sources

- `docs/adrs/0003-spaced-repetition-scheduling-policy.md` — authoritative policy (Proposed)
- `backend/src/main/java/com/odde/doughnut/entities/ForgettingCurve.java` — early `delayInHours` adjustment, thinking time, `partialFail` / `failed`
- `backend/src/main/java/com/odde/doughnut/entities/MemoryTracker.java` — `recalledSuccessfully` delay from `calculateNextRecallAt()`
- `backend/src/main/java/com/odde/doughnut/algorithms/CommissionedLearningSessionFeedbackScheduling.java` — parallel commissioned path + strictly-future helper
- `backend/src/test/java/com/odde/doughnut/algorithms/SpacedRepetitionEarlyRecallAdjustmentTest.java` — early-only adjustment tests (no late policy coverage)
- `backend/src/test/java/com/odde/doughnut/controllers/RecallPromptOverlapTryAgainTests.java` — overlap no-op contract
- `backend/src/test/java/com/odde/doughnut/controllers/RecallPromptAccidentalMatchEdgeTests.java` — accidental match threshold semantics to reconcile
- `.planning/seeds/SEED-004-close-spaced-repetition-scheduling-policy-gap.md` — milestone scope breadcrumbs
- `.planning/PROJECT.md` — v1.4 milestone goals

---
*Pitfalls research for: v1.4 SRS scheduling policy (ADR 0003)*
*Researched: 2026-08-12*
