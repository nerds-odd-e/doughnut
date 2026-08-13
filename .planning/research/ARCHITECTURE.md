# Architecture Research

**Domain:** Spaced-repetition scheduling policy (ADR 0003) on existing Doughnut scheduler  
**Researched:** 2026-08-12  
**Confidence:** HIGH

## System overview

ADR 0003 does not replace the scheduler stack; it redefines **what counts as memory evidence** and how that updates strength and the due-time projection. Current model: `forgettingCurveIndex` + `SpacedRepetitionAlgorithm`; `nextRecallAt` is the operational due-work projection.

```
HTTP / recall UI
  → SpellingRecallGrading (correct / incorrect / accidental / overlap)
  → RecallQuestionService (MCQ → boolean)
  → MemoryTrackerService
  → MemoryTracker / ForgettingCurve / SpacedRepetitionAlgorithm
  → RecallService (reads nextRecallAt for due lists)

Parallel: LearningSessionService
  → CommissionedLearningSessionFeedbackScheduling
  → CommissionedLearningSessionFeedbackPolicy
```

## Component status (discovered)

| Component | Finding |
|-----------|---------|
| `SpacedRepetitionAlgorithm` | Interval table — not the late-success bug source |
| `ForgettingCurve` | Late-success penalty removed 2026-08-05; still consumes due-relative `delayInHours` (early shrink only) |
| `MemoryTracker.recalledSuccessfully` | Passes `gradedAt − nextRecallAt` into `succeeded` — **C1 remaining** |
| `MemoryTrackerService` | Orchestration + frequent-failure threshold (informational) |
| `SpellingRecallGrading` | Outcome classification; overlap skips mutation |
| `CommissionedLearningSessionFeedbackPolicy` | Score → strength already matches ADR table |
| `CommissionedLearningSessionFeedbackScheduling` | Has `ensureNextRecallStrictlyAfterNow` |
| `RecallService` | Due queries from `nextRecallAt` — unchanged by policy semantics |

## Remaining C1 (time base)

The late-success **penalty** is shipped-removed. What remains is the wrong clock:

```174:181:backend/src/main/java/com/odde/doughnut/entities/MemoryTracker.java
  public void recalledSuccessfully(Timestamp currentUTCTimestamp, Integer thinkingTimeMs) {
    long delayInHours =
        TimestampOperations.getDiffInHours(currentUTCTimestamp, calculateNextRecallAt());

    setForgettingCurveIndex(forgettingCurve().succeeded(delayInHours, thinkingTimeMs));
    // ...
  }
```

ADR requires observed retention (`current − lastRecalledAt`), not queue deviation (`current − nextRecallAt`).

## Outcome → schedule (policy vs code)

| Outcome | ADR expectation | Current finding |
|---------|-----------------|-----------------|
| Correct | Grow; timing from elapsed retention; overdue ≥ on-time; optional bounded reward | Penalty gone; uses due deviation; no FSRS overdue reward; may not enforce strictly-future on all paths |
| Incorrect | Reduce; timing-neutral; optional short retry separate from long schedule | `recallFailed` 12h + strength cut — largely aligned |
| Accidental match | Weaker than incorrect; normal interval path | `partialFail` + normal path — verify ordering / threshold count |
| Overlap | No strength/schedule change | Skip `markAsRecalled` — aligned |
| Commissioned 0–5 | ADR table; effort & lateness neutral; never due at score instant | Policy class + interval guard — audit score 0 / late session |

## Evidence–schedule separation

- **Memory evidence:** graded outcome + elapsed since last graded recall (+ optional effort).
- **Due metadata:** `nextRecallAt` for eligibility, ordering, display — not negative evidence.
- **Implication:** Every state-changing recall path must not re-derive strength from schedule compliance.

## Integration map (new vs modified — discoveries)

| Component | Likely role if aligning to ADR |
|-----------|--------------------------------|
| Evidence bundle (`gradedAt`, `lastRecalledAt`, outcome, thinking time) | Prevents re-deriving evidence from due time |
| Central schedule apply for recall mutations | Avoids divergent spelling / MCQ / manual paths |
| `ForgettingCurve.succeeded` | Consume observed elapsed hours |
| Shared post-grade strictly-future guard | Pattern already exists on commissioned path |
| DB schema | No change expected for evidence fix; rebuild-from-history deferred |

## Anti-patterns observed / to avoid

1. **Schedule compliance as evidence** — current `recalledSuccessfully` pattern.  
2. **Boolean collapse** for spelling accidental/overlap into `markAsRecalled(false)`.  
3. **Policy tests on `forgettingCurveIndex` alone** — miss traps; couple to representation.  
4. **Big-bang FSRS** while fixing the policy gap — ADR defers FSRS.

## Data already present

`forgetting_curve_index`, `next_recall_at`, `last_recalled_at`, `quiz_answer.outcome`, `quiz_answer.thinking_time_ms`, user space intervals — sufficient for evidence-vs-due separation without new persistence.

## Sources

- `docs/adrs/0003-spaced-repetition-scheduling-policy.md`
- `backend/.../MemoryTracker.java`, `ForgettingCurve.java`
- `backend/.../CommissionedLearningSessionFeedbackPolicy.java`
- `backend/.../SpellingRecallGrading.java`

---
*Researched: 2026-08-12; corrected 2026-08-13 (late-success penalty shipped)*
