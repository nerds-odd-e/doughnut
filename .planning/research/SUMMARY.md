# Project Research Summary

**Project:** Doughnut v1.4 (SEED-004)
**Domain:** Spaced-repetition scheduling policy alignment — ADR 0003 retrofit on existing ForgettingCurve scheduler
**Researched:** 2026-08-12
**Confidence:** HIGH

## Executive Summary

Doughnut v1.4 is not a new SRS product build — it is a **policy correction milestone** on an existing Personal Knowledge Management platform. ADR 0003 defines safety properties for how graded evidence updates memory strength and the due-time projection (`nextRecallAt`). The codebase already implements most outcome-specific routing (spelling accidental match, overlap no-op, commissioned Tutor 0–5 scores, frequent-failure warning), but the core success path still conflates **queue compliance** with **memory evidence**: `MemoryTracker.recalledSuccessfully` passes `gradedAt − nextRecallAt` into `ForgettingCurve.succeeded`, penalizing early correct answers and creating the correct-but-overdue trap (SEED-004). Industry norm (Anki, FSRS) and ADR 0003 require retention to be judged from **elapsed time since last graded recall**, with `nextRecallAt` as operational queue metadata only.

The recommended approach is **policy-first refactor, not library migration**. Keep Java 25 / Spring Boot 4.1 / MySQL / Vue 3 unchanged; introduce a pure `SchedulingPolicy` + `SchedulingPolicyApplier` seam in `algorithms/`; refactor `ForgettingCurve` to consume observed retention hours; extract shared `PostGradeIntervalGuard` from commissioned scheduling; and verify through **observable schedule tests** (`nextRecallAt` strictly after grade, interval monotonicity, outcome-specific non-movement) — not `forgettingCurveIndex` magic numbers. FSRS, event-sourced history replay, and bulk `nextRecallAt` migration are explicitly deferred anti-features.

Key risks are scattered scheduling entry points (spelling, MCQ, commissioned, manual mark-as-recalled), immediate-due traps when computed interval is zero, and legacy tests that encode pre-policy semantics. Mitigation: one domain seam for all grade→schedule mutations; post-grade strictly-future invariant on every path; replace late-penalty index assertions in the same phase as the evidence fix; no Flyway bulk reschedule.

## Key Findings

### Recommended Stack

No new runtime libraries. v1.4 is a behavioral correction inside existing in-house scheduling modules on the current Spring Boot / MySQL / Vue stack. See [STACK.md](./STACK.md) for full detail.

**Core technologies:**
- **Java 25 + Spring Boot 4.1.0** — scheduling policy lives in pure domain code (`ForgettingCurve`, proposed `SchedulingPolicyApplier`); no external SRS engine
- **Spring Data JPA + MySQL** — existing `forgetting_curve_index`, `next_recall_at`, `last_recalled_at` columns sufficient; no schema migration expected
- **JUnit Jupiter + Hamcrest + `@Transactional` controller tests** — policy verification through real DB fixtures via `makeMe`; assert timestamps and interval hours, not internal index
- **Cypress (targeted specs)** — regression for overdue-correct trap, overlap retry, commissioned record; not full suite unless CI requires

**Explicitly avoid:** FSRS/Anki libraries (ADR deferred), Redis/job queues for per-tracker scheduling, frontend scheduling logic, bulk history replay migrations.

### Expected Features

See [FEATURES.md](./FEATURES.md) for full feature matrix and competitor analysis.

**Must have (table stakes — P1):**
- **Evidence vs due-time separation** — success path uses `lastRecalledAt → gradedAt` elapsed time, not `nextRecallAt` deviation; fixes SEED-004 core bug
- **No late-success penalty + correct-forward scheduling** — overdue correct ≥ on-time; `nextRecallAt` strictly after grade instant on all success paths
- **Policy tests on observable schedule** — controller/service tests asserting due eligibility and interval movement, not index alone
- **Outcome schedule verification** — accidental (weaker than incorrect, normal path), overlap (no mutation), incorrect (outcome-only penalty), commissioned 0–5 (audit existing v1.3)

**Should have (verify/audit — P2):**
- **Effort bounds** — slow correct cannot fail or zero-interval; commissioned path effort-neutral
- **Frequent-failure warning** — informational only; overlap excluded; confirm accidental-match counting intent
- **User space intervals** — unchanged Fibonacci/custom table via `SpacedRepetitionAlgorithm`

**Defer (v2+ — P3):**
- **FSRS migration** — after policy proven in production with migration strategy
- **Rebuildable due-time projection** — event store / versioned scheduler state
- **Bounded lateness bonus** — ADR-allowed optional enhancement post-validation
- **Spelling follow-ons (MCQ, fuzzy)** — orthogonal to scheduling policy

### Architecture Approach

Introduce an **evidence–schedule separation seam** without replacing the internal strength model. Services classify outcomes; algorithms apply policy. See [ARCHITECTURE.md](./ARCHITECTURE.md) for diagrams and file layout.

**Major components:**
1. **`RecallEvidence` (new)** — bundles `gradedAt`, `lastRecalledAt`, outcome, `thinkingTimeMs`; prevents re-deriving evidence from schedule metadata
2. **`SchedulingPolicy` / `SchedulingPolicyApplier` (new)** — pure ADR rules: per-outcome strength delta + `nextRecallAt`; single writer for recall-path tracker mutations
3. **`ForgettingCurve` (modified)** — `succeeded(observedElapsedHours, thinkingTimeMs)` decoupled from `nextRecallAt`
4. **`PostGradeIntervalGuard` (new)** — extracted from `CommissionedLearningSessionFeedbackScheduling.ensureNextRecallStrictlyAfterNow`; shared across recall + commissioned paths
5. **`MemoryTrackerService` + outcome routers (modified/verify)** — dispatch all mutating paths through applier; `RecallService` due queries unchanged

### Critical Pitfalls

See [PITFALLS.md](./PITFALLS.md) for full pitfall-to-phase mapping and recovery strategies.

1. **Conflating schedule deviation with memory evidence** — `delayInHours` from `calculateNextRecallAt()` must not drive strength; split inputs and replace legacy late-penalty tests
2. **Immediate/daily trap after correct answers** — enforce `nextRecallAt > gradedAt` after every grade; bump zero intervals to first positive spacing hour
3. **Parallel scheduling entry points with divergent semantics** — route spelling, MCQ, commissioned, and manual paths through one seam; matrix-test each outcome × entry point
4. **Treating accidental match as incorrect recall** — weaker penalty, normal interval path (not 12h `recallFailed`); reconcile frequent-failure counting
5. **Policy tests on internal index** — assert schedule horizon and monotonicity; keep index tests as algorithm units only, not policy gate

## Implications for Roadmap

Based on combined research, suggested **7-phase** structure aligned with local Behavior/Structure grammar and stop-safe ordering. Critical path: policy tests → applier seam → correct-recall fix (primary gap). Phases 5–6 can parallelize after seam exists.

### Phase 1: Policy Contract + Failing Policy Tests
**Rationale:** TDD anchor before production routing changes; prevents encoding the bug in new assertions
**Delivers:** `SchedulingPolicyTest` with ADR observables — late-correct ≥ on-time strength movement; post-grade `nextRecallAt > gradedAt`; incorrect timing-neutral
**Addresses:** Policy tests on observable schedule (P1)
**Avoids:** Index-only policy tests (Pitfall 9); shipping without overdue backlog coverage

### Phase 2: Evidence Model + Applier Skeleton
**Rationale:** Structure phase enabling all outcome paths without changing external behavior yet
**Delivers:** `RecallEvidence`, `SchedulingPolicy`, `SchedulingPolicyApplier`, `PostGradeIntervalGuard` — wired in tests, not yet routed from production
**Uses:** Existing `ForgettingCurve`, `SpacedRepetitionAlgorithm`, commissioned interval guard pattern
**Implements:** Evidence–schedule separation seam (Architecture Pattern 1–2)
**Avoids:** Divergent entry points (Pitfall 3 foundation)

### Phase 3: Correct-Recall Path (Evidence Separation)
**Rationale:** Primary ADR gap and SEED-004 trap; highest user value; unblocks most other verifications
**Delivers:** `ForgettingCurve` / `MemoryTracker.recalledSuccessfully` uses `lastRecalledAt` elapsed time; removes schedule-deviation penalty; shared positive-interval guard on success path
**Addresses:** Evidence vs due-time separation, no late-success penalty, correct-forward scheduling (P1)
**Avoids:** Conflated evidence/schedule (Pitfall 1), immediate-due trap (Pitfall 2 partial)

### Phase 4: Incorrect-Recall Path
**Rationale:** Confirm failure penalty is outcome-based and timing-neutral; retain explicit 12h same-session retry semantics
**Delivers:** Incorrect path through applier; recovery sequence policy tests (fail then correct re-expands)
**Addresses:** Incorrect recall shortens interval without permanent trap (table stakes)
**Avoids:** Timing leakage into failure penalty

### Phase 5: Accidental-Match + Overlap Verification
**Rationale:** Mostly built v1.1–v1.2; regression gate during seam refactor; independent outcome branches
**Delivers:** Accidental arm in applier (weaker than incorrect, normal interval); overlap no-op locked; threshold counting decision documented
**Addresses:** Accidental match, overlap no schedule change (P1 verify)
**Avoids:** Accidental-as-incorrect (Pitfall 4), overlap mutation (Pitfall 5)

### Phase 6: Commissioned Feedback Hardening
**Rationale:** v1.3 shipped parallel path; share interval guard and verify ADR table + late-session neutrality
**Delivers:** Commissioned scores route through shared guard; policy tests for scores 0–5 schedule horizons
**Addresses:** Commissioned 0–5 schedule audit (P1)
**Avoids:** Second scheduler divergence (Pitfall 6), commissioned score-0 immediate due

### Phase 7: Effort Bounds + Test Migration + Close-Out
**Rationale:** Audit and migrate legacy tests; final ADR compliance gate before milestone wrap
**Delivers:** Effort cannot invert correct outcomes; replace `SpacedRepetitionEarlyRecallAdjustmentTest` / `RecallServiceWithSpacedRepetitionAlgorithmTest` late-penalty cases; controller assertion migration; frequent-failure informational-only verification
**Addresses:** Effort bounds (P2), frequent-failure warning (P2), policy test suite close-out
**Avoids:** Effort inversion (Pitfall 7), warning changes schedule (Pitfall 8), unsafe bulk replay (Pitfall 10)

### Phase Ordering Rationale

- **Policy tests before refactor (Phase 1)** — ADR contract as failing tests prevents re-encoding the overdue trap
- **Seam before routing (Phase 2 → 3)** — one applier prevents the scattered-entry-point pitfall that caused the original bug
- **Correct-recall before peripheral outcomes (Phase 3 before 5–6)** — SEED-004 core value; accidental/overlap/commissioned largely verify existing behavior through new seam
- **Test migration last (Phase 7)** — legacy index assertions must change alongside evidence fix, not before or in isolation
- **No FSRS, no migration, no frontend scheduling** — scope containment per STACK and FEATURES anti-features

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 5 (Accidental match):** Confirm whether accidental matches should count toward frequent-failure threshold — current code counts `correct=false`; ADR says incorrect-only; needs product decision during `/gsd-plan-phase`
- **Phase 7 (Test migration):** Inventory all index-centric timing tests across backend suite; may need `--research-phase` to map replacement assertions

Phases with standard patterns (skip research-phase):
- **Phase 2 (Applier skeleton):** Follows existing `CommissionedLearningSessionFeedbackPolicy` / `ForgettingCurve` patterns in `algorithms/`
- **Phase 4 (Incorrect path):** `recallFailed` semantics already ADR-aligned; verify-only
- **Phase 6 (Commissioned):** v1.3 implementation already matches ADR quantified table; audit + shared guard extraction

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Grounded in `build.gradle`, ADR 0003, live codebase modules; no new dependencies needed |
| Features | HIGH | ADR 0003 + SEED-004 + codebase audit; competitor norms (Anki/FSRS) corroborate table stakes |
| Architecture | HIGH | Integration seams identified with file-level anchors; proposed structure matches existing `algorithms/` convention |
| Pitfalls | HIGH (code); MEDIUM (rollout) | ADR + live paths verified; production interval drift monitoring not yet established |

**Overall confidence:** HIGH

### Gaps to Address

- **Accidental match threshold semantics:** Current `countWrongAnswersSinceForMemoryTracker` counts accidental matches as wrong — confirm intentional vs ADR "incorrect only" during Phase 5 planning
- **ADR approval status:** ADR 0003 marked Proposed — ensure human approval or explicit drift tracking before ship
- **Bounded lateness bonus:** ADR allows but does not require — defer decision until post-trap-fix monitoring
- **Manual `markAsRecalled` / `updateForgettingCurve` admin paths:** Clarify whether these bypass policy seam or route through it during Phase 2 planning

## Sources

### Primary (HIGH confidence)
- `docs/adrs/0003-spaced-repetition-scheduling-policy.md` — authoritative scheduling policy
- `.planning/seeds/SEED-004-close-spaced-repetition-scheduling-policy-gap.md` — milestone trigger
- `.planning/PROJECT.md` — v1.4 scope and locked anti-features
- `backend/src/main/java/com/odde/doughnut/entities/ForgettingCurve.java`, `MemoryTracker.java` — current scheduling mechanics
- `backend/src/main/java/com/odde/doughnut/algorithms/CommissionedLearningSessionFeedbackScheduling.java` — post-grade interval guard
- `backend/src/main/java/com/odde/doughnut/services/SpellingRecallGrading.java` — outcome routing
- `.planning/research/STACK.md`, `FEATURES.md`, `ARCHITECTURE.md`, `PITFALLS.md` — parallel research outputs

### Secondary (MEDIUM confidence)
- [Anki Manual — Falling Behind](https://docs.ankiweb.net/studying.html#falling-behind) — industry table stakes for backlog behavior
- [FSRS Algorithm wiki](https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm) — evidence-vs-schedule separation norm

---
*Research completed: 2026-08-12*
*Ready for roadmap: yes*
