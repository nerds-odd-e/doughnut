# Architecture Research

**Domain:** Spaced-repetition scheduling policy (ADR 0003) on existing Doughnut scheduler  
**Researched:** 2026-08-12  
**Confidence:** HIGH

## System overview

ADR 0003 does not replace the scheduler stack; it defines how a recall transitions persisted memory state and the due-time projection. Current model: `forgettingCurveIndex` + `SpacedRepetitionAlgorithm`; `nextRecallAt` is the operational due-work projection.

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
| `ForgettingCurve` | Late-success penalty removed 2026-08-05; still consumes a due-relative `delayInHours`, although early-success math reduces to elapsed/current interval |
| `MemoryTracker` | Gathers current persisted scheduler state; incorrect recall fails to advance `lastRecalledAt` — **C1 remaining** |
| `MemoryTrackerService` | Orchestration + frequent-failure threshold (informational) |
| `SpellingRecallGrading` | Outcome classification; overlap skips mutation |
| `CommissionedLearningSessionFeedbackPolicy` | Score → strength already matches ADR table |
| `CommissionedLearningSessionFeedbackScheduling` | Has `ensureNextRecallStrictlyAfterNow` |
| `RecallService` | Due queries from `nextRecallAt` — unchanged by policy semantics |

## Remaining C1 (recall-time state cohesion)

The late-success **penalty** is shipped-removed. The success contract still
hides elapsed time behind a due-relative value:

```174:181:backend/src/main/java/com/odde/doughnut/entities/MemoryTracker.java
  public void recalledSuccessfully(Timestamp currentUTCTimestamp, Integer thinkingTimeMs) {
    long delayInHours =
        TimestampOperations.getDiffInHours(currentUTCTimestamp, calculateNextRecallAt());

    setForgettingCurveIndex(forgettingCurve().succeeded(delayInHours, thinkingTimeMs));
    // ...
  }
```

`calculateNextRecallAt()` recomputes `lastRecalledAt + current interval`; it
does not read the persisted `nextRecallAt`. For an early correct recall, the
existing adjustment is algebraically equivalent to `elapsed / current
interval`. The behavior is therefore closer to the desired elapsed-time model
than the API suggests.

The functional gap is that `recallFailed` does not advance `lastRecalledAt`.
After correct → incorrect → correct, elapsed time for the final transition can
span from the first correct recall instead of the intervening incorrect recall.
C1 should make elapsed time explicit and make every state-changing recall
advance the anchor.

## Outcome → schedule (policy vs code)

| Outcome | ADR expectation | Current finding |
|---------|-----------------|-----------------|
| Correct | Grow; timing from elapsed retention; overdue ≥ on-time; optional bounded reward | Penalty gone; elapsed is implicit behind due-relative API; no FSRS overdue reward; may not enforce strictly-future on all paths |
| Incorrect | Reduce; timing-neutral; optional short retry separate from long schedule | `recallFailed` 12h + strength cut, but stale `lastRecalledAt` breaks the next elapsed interval |
| Accidental match | Weaker than incorrect; normal interval path | `partialFail` + normal path — verify ordering / threshold count |
| Overlap | No strength/schedule change | Skip `markAsRecalled` — aligned |
| Commissioned 0–5 | ADR table; effort & lateness neutral; never due at score instant | Policy class + interval guard — audit score 0 / late session |

## Recall transition and schedule projection

- **Transition inputs:** persisted pre-recall tracker state + graded outcome +
  elapsed since the previous state-changing recall (+ optional effort).
- **Due metadata:** `nextRecallAt` for eligibility, ordering, and display — not
  an input to the memory-state transition.
- **Implication:** Every state-changing recall path advances `lastRecalledAt`
  and projects `nextRecallAt` atomically.

## Integration map (new vs modified — discoveries)

| Component | Likely role if aligning to ADR |
|-----------|--------------------------------|
| `MemoryTracker` transition from persisted pre-recall state | Keeps elapsed-time calculation and recall-anchor update cohesive |
| Central schedule apply for recall mutations | Avoids divergent spelling / MCQ / manual paths |
| `ForgettingCurve.succeeded` | Consume explicit elapsed hours |
| Shared post-grade strictly-future guard | Pattern already exists on commissioned path |
| DB schema | No schema change for C1; repair legacy `lastRecalledAt` from trustworthy Answer/Tutor-feedback timestamps; `RecallLog` / rebuild-from-history deferred |

## Anti-patterns observed / to avoid

1. **Due-relative transition contract** — current `recalledSuccessfully` pattern obscures elapsed time.
2. **Boolean collapse** for spelling accidental/overlap into `markAsRecalled(false)`.  
3. **Policy tests on `forgettingCurveIndex` alone** — miss traps; couple to representation.  
4. **Big-bang FSRS** while fixing the policy gap — ADR defers FSRS.

## Data already present

`forgetting_curve_index`, `next_recall_at`, `last_recalled_at`, `quiz_answer.outcome`, `quiz_answer.thinking_time_ms`, user space intervals — sufficient for C1 without new schema. C1 still needs a one-time repair of stale `last_recalled_at` values from the latest trustworthy non-overlap Answer or Tutor-feedback timestamp. A unified `RecallLog` remains deferred.

## Sources

- `docs/adrs/0003-spaced-repetition-scheduling-policy.md`
- `backend/.../MemoryTracker.java`, `ForgettingCurve.java`
- `backend/.../CommissionedLearningSessionFeedbackPolicy.java`
- `backend/.../SpellingRecallGrading.java`

---
*Researched: 2026-08-12; corrected 2026-08-13 (late-success penalty shipped)*
