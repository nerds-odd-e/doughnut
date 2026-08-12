# Architecture Research

**Domain:** Personal Knowledge Management — spaced-repetition scheduling policy (ADR 0003)  
**Project:** Doughnut v1.4 — policy-aligned scheduling without FSRS migration  
**Researched:** 2026-08-12  
**Confidence:** HIGH (grounded in accepted codebase seams + ADR 0003 text)

## Standard Architecture

### System Overview

ADR 0003 does not replace the existing scheduler stack; it **redefines what counts as memory evidence** and **how that evidence updates strength and the due-time projection**. The milestone keeps `forgettingCurveIndex` + `SpacedRepetitionAlgorithm` as the internal model and `nextRecallAt` as the operational due-work projection.

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         HTTP / recall UI layer                            │
│  RecallPromptController  LearningSessionController  MemoryTrackerController│
└───────────────┬──────────────────────────────┬───────────────────────────┘
                │ graded answer / score           │ manual mark-as-recalled
                ▼                                 ▼
┌───────────────────────────────┐    ┌────────────────────────────────────┐
│ SpellingRecallGrading         │    │ RecallQuestionService               │
│ (outcome: correct / incorrect │    │ (MCQ → boolean correct)             │
│  / accidental / overlap)      │    └──────────────────┬─────────────────┘
└───────────────┬───────────────┘                       │
                │                                         │
                ▼                                         ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                    MemoryTrackerService (orchestration)                   │
│  markAsRecalled · updateMemoryTrackerAfterAnsweringQuestion · threshold   │
└───────────────────────────────┬──────────────────────────────────────────┘
                                │ route by graded outcome
                                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│              **NEW** SchedulingPolicyApplier (proposed seam)              │
│  RecallEvidence in → strength delta + nextRecallAt rules out              │
│  Implements ADR 0003: evidence ≠ schedule metadata                        │
└───────┬───────────────────────────────┬──────────────────────────────────┘
        │                               │
        ▼                               ▼
┌───────────────────┐         ┌────────────────────────────────────────────┐
│ ForgettingCurve   │         │ SpacedRepetitionAlgorithm (interval table)  │
│ strength math     │◄────────│ getRepeatInHours(index) — unchanged         │
└───────────────────┘         └────────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ MemoryTracker (entity)                                                    │
│ forgettingCurveIndex · lastRecalledAt · nextRecallAt · recallCount        │
└───────────────────────────────┬──────────────────────────────────────────┘
                                │ queried by
                                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ RecallService / UserService — due-work projection (read-only for policy)  │
└──────────────────────────────────────────────────────────────────────────┘

Parallel path (commissioned):
LearningSessionService → CommissionedLearningSessionFeedbackScheduling
                      → CommissionedLearningSessionFeedbackPolicy
```

### Component Responsibilities

| Component | Responsibility | v1.4 role |
|-----------|----------------|-----------|
| `SpacedRepetitionAlgorithm` | User-configured interval table (`getRepeatInHours`) | **Unchanged** — implementation detail behind policy |
| `ForgettingCurve` | Strength index adjustments (success / fail / partial / effort) | **Modified** — stop using schedule deviation as evidence; use observed elapsed time |
| `MemoryTracker` | Persist memory state + materialize `nextRecallAt` | **Modified** — delegate scheduling mutations to policy seam; keep projection consistent |
| `MemoryTrackerService` | Entry point for recall-side effects + frequent-failure warning | **Modified** — single schedule dispatch; threshold already ADR-aligned |
| `SpellingRecallGrading` | Spelling outcome classification | **Mostly unchanged** — verify overlap is schedule no-op; accidental uses normal path |
| `RecallQuestionService` | MCQ answer → boolean schedule | **Unchanged routing** — inherits policy via `MemoryTrackerService` |
| `CommissionedLearningSessionFeedbackPolicy` | Score → strength delta (accumulated-strength rules) | **Verify only** — already matches ADR quantified table |
| `CommissionedLearningSessionFeedbackScheduling` | Score → tracker fields + positive-interval guard | **Minor modify** — share positive-interval helper with recall path |
| `RecallService` | Due lists from `nextRecallAt` | **Unchanged** — projection stays authoritative for queries |
| **`RecallEvidence`** (new) | Bundle: `gradedAt`, `lastRecalledAt`, outcome, `thinkingTimeMs` | **New** — prevents re-deriving evidence from schedule metadata |
| **`SchedulingPolicy` / `SchedulingPolicyApplier`** (new) | Pure ADR rules: per-outcome strength + `nextRecallAt` | **New** — central policy seam; policy tests target this boundary |

## Recommended Project Structure

```
backend/src/main/java/com/odde/doughnut/
├── algorithms/
│   ├── SpacedRepetitionAlgorithm.java          # unchanged
│   ├── ForgettingCurve.java                    # modified — evidence-based success math
│   ├── SchedulingPolicy.java                   # NEW — ADR outcome rules (pure)
│   ├── SchedulingPolicyApplier.java            # NEW — applies result to MemoryTracker
│   ├── PostGradeIntervalGuard.java             # NEW (or package-private) — shared positive-interval rule
│   ├── CommissionedLearningSessionFeedbackPolicy.java      # verify
│   └── CommissionedLearningSessionFeedbackScheduling.java  # minor — reuse interval guard
├── entities/
│   └── MemoryTracker.java                      # modified — thin; calls applier
└── services/
    ├── MemoryTrackerService.java               # modified — dispatch by outcome
    ├── SpellingRecallGrading.java              # verify overlap / accidental paths
    ├── RecallQuestionService.java              # unchanged
    ├── RecallService.java                      # unchanged
    └── LearningSessionService.java             # unchanged routing

backend/src/test/java/com/odde/doughnut/
├── algorithms/
│   ├── SchedulingPolicyTest.java               # NEW — policy-first observable schedule tests
│   └── SchedulingPolicyApplierTest.java        # NEW — end-to-end tracker mutation
└── controllers/
    └── RecallPromptAnswerSpellingControllerTest.java  # modified — replace index-centric assertions
```

### Structure Rationale

- **Policy in `algorithms/`:** Matches existing `ForgettingCurve`, `CommissionedLearningSessionFeedbackPolicy`, and `SpacedRepetitionAlgorithm` — pure, testable, no Spring.
- **Entity stays thin:** `MemoryTracker` keeps persistence fields; scheduling semantics live in one applier to avoid duplicating ADR rules across spelling, MCQ, and commissioned paths.
- **Services classify, algorithms schedule:** `SpellingRecallGrading` already owns outcome detection; it should not embed interval math.

## Architectural Patterns

### Pattern 1: Evidence–Schedule Separation

**What:** Memory evidence = graded outcome + elapsed time since last graded recall (+ optional effort). `nextRecallAt` is a materialized projection for due-work queries, never negative evidence.

**When to use:** Every state-changing recall path (spelling, MCQ, commissioned score, manual mark-as-recalled).

**Trade-offs:** Requires threading `lastRecalledAt` into strength math instead of `calculateNextRecallAt()` deviation. Existing `ForgettingCurve.succeeded(delayInHours, …)` where `delayInHours = current − plannedDue` must be replaced or re-interpreted.

**Current violation (integration anchor):**

```174:181:backend/src/main/java/com/odde/doughnut/entities/MemoryTracker.java
  public void recalledSuccessfully(Timestamp currentUTCTimestamp, Integer thinkingTimeMs) {
    long delayInHours =
        TimestampOperations.getDiffInHours(currentUTCTimestamp, calculateNextRecallAt());

    setForgettingCurveIndex(forgettingCurve().succeeded(delayInHours, thinkingTimeMs));
    // ...
  }
```

ADR 0003 requires observed retention (`current − lastRecalledAt`), not queue deviation (`current − nextRecallAt`).

### Pattern 2: Outcome-Dispatch Scheduling

**What:** One applier handles each ADR outcome with explicit post-conditions on `nextRecallAt`.

| Outcome | Strength | `nextRecallAt` | Same-session retry |
|---------|----------|----------------|-------------------|
| Correct | Grow (non-negative timing adj.) | Strictly after answer instant | N/A (graded) |
| Incorrect | Reduce (timing-neutral) | 12h relearning override **or** policy interval — keep explicit | UI may retry; persisted schedule separate |
| Accidental match | Weaker negative than incorrect | Normal interval path (not 12h override) | N/A |
| Overlap | No change | No change | Allowed same-session |
| Commissioned 0–5 | `FeedbackPolicy.applyScore` | Positive interval guard (existing) | N/A |

**When to use:** Replace `MemoryTracker.markAsRecalled(boolean)` boolean collapse for spelling; MCQ remains boolean but uses same correct/incorrect arms.

### Pattern 3: Policy Tests on Observable Schedule

**What:** Assert `nextRecallAt − gradedAt > 0`, interval monotonicity for late-correct sequences, and outcome-specific non-movement — not raw `forgettingCurveIndex` unless testing commissioned score table directly.

**When to use:** New `SchedulingPolicyTest` before changing `ForgettingCurve`; migrate tests that assert late answers weaken strength solely for lateness (ADR consequences).

**Trade-offs:** Slightly slower tests; aligns with ADR and survives future FSRS/internal representation changes.

## Data Flow

### Graded Recall Flow (spelling / MCQ)

```
User submits answer
    ↓
Controller → SpellingRecallGrading / RecallQuestionService
    ↓
Classify outcome (correct | incorrect | accidental | overlap)
    ↓
MemoryTrackerService.markAsRecalled / updateMemoryTrackerAfterAnsweringQuestion
    ↓
SchedulingPolicyApplier.apply(tracker, RecallEvidence)
    ├─ ForgettingCurve / FeedbackPolicy (strength)
    ├─ SpacedRepetitionAlgorithm (interval lookup)
    └─ PostGradeIntervalGuard (nextRecallAt > gradedAt)
    ↓
entityPersister.save(tracker)
    ↓
RecallService reads nextRecallAt for due lists (unchanged)
```

### Commissioned Feedback Flow (already largely aligned)

```
LearningSessionService.record
    ↓
CommissionedLearningSessionFeedbackScheduling.recordFeedback
    ├─ CommissionedLearningSessionFeedbackPolicy.applyScore (strength)
    └─ ensureNextRecallStrictlyAfterNow (positive interval)
    ↓
save tracker + session items
```

**v1.4 change:** Extract `ensureNextRecallStrictlyAfterNow` into shared `PostGradeIntervalGuard` and call from correct/incorrect/accidental paths too (ADR: zero persisted interval not allowed after graded answer).

### Key Data Flows

1. **Correct overdue recall:** `RecallEvidence` carries `elapsedHours = gradedAt − lastRecalledAt` (large). Policy applies success growth with **non-negative** lateness adjustment (optional bounded bonus). `nextRecallAt = gradedAt + repeatInHours(updatedIndex)`; must be strictly future.
2. **Incorrect recall:** Strength reduced independent of `gradedAt − nextRecallAt`. `recallFailed` 12h override remains the same-session retry signal; ADR treats it as separate from long-term schedule.
3. **Overlap retry:** First overlap grades prompt only; tracker fields untouched. Retry routes through normal outcome dispatch.

## Integration Points

### External Boundaries

| Boundary | Direction | Change |
|----------|-----------|--------|
| Recall / spelling API | In | None — DTOs unchanged |
| Learning session record API | In | None |
| Due-memory-trackers API | Out | None — still reads `nextRecallAt` |
| Frequent-failure warning API | Out | None — `ThresholdExceededResult` already informational |

### Internal Boundaries (explicit new vs modified)

| Component | Status | Integration note |
|-----------|--------|------------------|
| `RecallEvidence` | **NEW** | Built at service boundary from `RecallPrompt` / answer / tracker snapshot |
| `SchedulingPolicy` | **NEW** | Pure functions; no DB, no Spring |
| `SchedulingPolicyApplier` | **NEW** | Only writer of `forgettingCurveIndex` + `nextRecallAt` on recall paths |
| `PostGradeIntervalGuard` | **NEW** | Extracted from `CommissionedLearningSessionFeedbackScheduling` |
| `ForgettingCurve` | **MODIFIED** | `succeeded(observedElapsedHours, thinkingTimeMs)` — decouple from `nextRecallAt` |
| `MemoryTracker` | **MODIFIED** | Replace `recalledSuccessfully` / `recallFailed` / `markAsAccidentalMatch` internals with applier calls |
| `MemoryTrackerService` | **MODIFIED** | Optional `markAsRecalled(evidence)` overload; threshold logic unchanged |
| `SpellingRecallGrading` | **VERIFY** | Overlap: no tracker mutation (already). Accidental: calls applier accidental arm |
| `RecallQuestionService` | **UNCHANGED** | Boolean path inherits policy |
| `CommissionedLearningSessionFeedbackPolicy` | **VERIFY** | ADR table already implemented |
| `CommissionedLearningSessionFeedbackScheduling` | **MINOR** | Delegate interval guard to shared helper |
| `LearningSessionService` | **UNCHANGED** | Still calls feedback scheduling |
| `SpacedRepetitionAlgorithm` | **UNCHANGED** | Interval table |
| `RecallService` | **UNCHANGED** | Due projection consumer |
| `RecallPromptRepository.countWrongAnswers…` | **UNCHANGED** | Already excludes `OVERLAP` |
| DB schema / migrations | **NONE** | ADR defers rebuildable history; snapshot seeding only |

## Suggested Build Order

Dependency-safe phases for roadmap (each stop-safe per local planning rules):

| Order | Phase | Type | Depends on | Delivers |
|-------|-------|------|------------|----------|
| 1 | **Policy contract + failing policy tests** | Behavior | — | `SchedulingPolicyTest` asserting ADR observables: late-correct ≥ on-time strength movement; post-grade `nextRecallAt > gradedAt`; incorrect timing-neutral |
| 2 | **Evidence model + applier skeleton** | Structure | 1 | `RecallEvidence`, `SchedulingPolicyApplier` wired but not yet routed from production |
| 3 | **Correct-recall path** | Behavior | 2 | Fix `ForgettingCurve` / `recalledSuccessfully` to use observed elapsed; remove schedule-deviation penalty; shared positive-interval guard |
| 4 | **Incorrect-recall path** | Behavior | 2 | Confirm failure penalty ignores earliness/lateness; retain 12h override semantics explicitly |
| 5 | **Accidental-match path** | Behavior | 2 | Weaker than incorrect; normal interval path (not `recallFailed` override) — largely present; align with applier |
| 6 | **Overlap path verification** | Behavior | — | No schedule mutation; excluded from wrong-count (already); add policy test if missing |
| 7 | **Commissioned feedback hardening** | Behavior | 2 | Reuse `PostGradeIntervalGuard`; verify late session does not weaken (score-only evidence) |
| 8 | **Effort bounds audit** | Behavior | 3–4 | `ForgettingCurveThinkingTimeTest` aligned — effort cannot invert outcome |
| 9 | **Test migration + controller assertions** | Structure | 3–7 | Replace index-only late-penalty tests; keep `lateCorrectAnswerDoesNotShortenTheNextInterval` style assertions |

**Critical path:** 1 → 2 → 3 (correct-recall / late-success trap is the primary ADR gap). Phases 5–6 can parallelize after 2. Phase 7 is mostly verification. No FSRS, no history replay, no `nextRecallAt` rebuild.

## Scaling Considerations

| Scale | Architecture Adjustments |
|-------|--------------------------|
| Current (single JVM, MySQL) | No change — scheduling is per-tracker, in-process |
| Higher recall volume | Policy applier stays O(1) per answer; due queries already indexed on `next_recall_at` |
| Future FSRS migration | Replace `ForgettingCurve` internals behind `SchedulingPolicy`; keep outcome dispatch + `nextRecallAt` projection contract |

### Scaling Priorities

1. **First bottleneck:** Due-list queries — unrelated to v1.4; do not denormalize policy into queries.
2. **Future:** Versioned recall events for rebuildable projection (ADR deferred) — would add event store, not alter v1.4 seam.

## Anti-Patterns

### Anti-Pattern 1: Schedule Compliance as Evidence

**What people do:** Pass `gradedAt − nextRecallAt` into strength adjustment (current `recalledSuccessfully`).

**Why it's wrong:** ADR 0003 — backlog age is not forgetting; causes correct-but-overdue trap and conflates queue with memory.

**Do this instead:** Pass `gradedAt − lastRecalledAt` as observed retention; use `nextRecallAt` only for eligibility/display and optional non-negative lateness bonus caps.

### Anti-Pattern 2: Boolean Collapse for Spelling Outcomes

**What people do:** Route accidental match and overlap through `markAsRecalled(correct=false)`.

**Why it's wrong:** Accidental and overlap have distinct ADR schedules; overlap must not move fields.

**Do this instead:** Outcome enum dispatch at `MemoryTrackerService` / applier (overlap → no-op; accidental → dedicated arm).

### Anti-Pattern 3: Policy Tests on Internal Index

**What people do:** Assert `forgettingCurveIndex == 115.0` for late/early scenarios.

**Why it's wrong:** ADR allows representation changes; couples tests to implementation not policy.

**Do this instead:** Assert interval length, strict future due, monotonic progress across correct sequences, and commissioned score band movement.

### Anti-Pattern 4: Big-Bang FSRS Migration

**What people do:** Replace `SpacedRepetitionAlgorithm` while fixing policy gap.

**Why it's wrong:** ADR explicitly defers FSRS; expands scope across state model, fitting, migration.

**Do this instead:** Policy-first on existing model; keep FSRS as future swap behind `SchedulingPolicy`.

## Sources

- `docs/adrs/0003-spaced-repetition-scheduling-policy.md` — authoritative policy (HIGH)
- `backend/src/main/java/com/odde/doughnut/entities/MemoryTracker.java` — current scheduling mutation site (HIGH)
- `backend/src/main/java/com/odde/doughnut/entities/ForgettingCurve.java` — schedule-deviation success math (HIGH)
- `backend/src/main/java/com/odde/doughnut/algorithms/CommissionedLearningSessionFeedbackPolicy.java` — commissioned quantified rules (HIGH)
- `backend/src/main/java/com/odde/doughnut/services/SpellingRecallGrading.java` — outcome routing (HIGH)
- `.planning/PROJECT.md` — v1.4 milestone scope (HIGH)

---
*Architecture research for: Doughnut v1.4 ADR 0003 scheduling policy integration*  
*Researched: 2026-08-12*
