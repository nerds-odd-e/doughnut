# Pitfalls Research

**Domain:** Aligning Doughnut scheduling with ADR 0003  
**Researched:** 2026-08-12  
**Confidence:** HIGH (code); MEDIUM (rollout monitoring)

## Critical pitfalls

### 1. Conflating schedule deviation with memory evidence

**What goes wrong:** Correct recalls use deviation from `nextRecallAt` as the time input to strength math. Early answers shrink the success increment; overdue success does not get retention credit for longer elapsed time.

**Why:** `MemoryTracker.recalledSuccessfully` passes `getDiffInHours(current, calculateNextRecallAt())` into `ForgettingCurve.succeeded`. Early-only tests (`SpacedRepetitionEarlyRecallAdjustmentTest`) encode that model without late/overdue policy coverage.

**Avoid:** Split outcome, observed elapsed since `lastRecalledAt`, and due projection. Never weaken strength solely because the answer was overdue. Replace tests that assert lateness weakens strength.

**Warning signs:** `delayInHours` still from `calculateNextRecallAt()`; “late” tests that only drop index; correct backlog answers still shorten intervals.

---

### 2. Immediate-or-daily trap after correct answers

**What goes wrong:** Tracker stays due at grade instant or loops on short intervals (`nextRecallAt <= now`, or repeat hours = 0).

**Why:** Index floor + zero spacing; early/effort penalties stacking; success path may not enforce strictly-future due; commissioned score 0 needs the same guard on every path.

**Avoid:** After every state-changing grade, `nextRecallAt` strictly after grade time; bump zero intervals to first positive spacing (commissioned path already has this helper).

**Warning signs:** Tests check index not `nextRecallAt > gradedAt`; same tracker due every session despite only correct answers.

---

### 3. Parallel scheduling entry points with divergent semantics

**What goes wrong:** Policy holds on spelling but not MCQ, manual mark-as-recalled, commissioned record, or admin `updateForgettingCurve`.

**Why:** Logic scattered across entity methods, services, and controllers from v1.1–v1.3.

**Avoid:** One seam for “graded evidence → strength + projection”; matrix-test outcomes × entry points on schedule observables.

**Warning signs:** Fix only in `ForgettingCurve` while commissioned still writes fields directly; admin APIs used to “fix” schedule.

---

### 4. Treating accidental match as incorrect recall

**What goes wrong:** Accidental match uses failure-style 12h reschedule and/or counts fully toward frequent-failure as `correct=false`.

**Why:** `markAsAccidentalMatch` predates ADR taxonomy; wrong-count query excludes `OVERLAP` but not necessarily accidental match.

**Avoid:** Weaker than incorrect; normal interval path (not `recallFailed`); product decision on threshold counting (ADR: incorrect-only).

**Warning signs:** Accidental → now+12h; threshold fires from accidental-only streaks.

---

### 5. Overlap mutates schedule or recall credit

**What goes wrong:** Overlap increments `recallCount`, moves index, or changes `nextRecallAt`.

**Why:** Shared grading path refactors that “always mark recalled.”

**Avoid:** Explicit no-op on tracker fields; lock with existing overlap try-again controller tests.

**Warning signs:** `recallCount` up on overlap; service-layer mutation on `OVERLAP` outcome.

---

### 6. Commissioned feedback as a second scheduler

**What goes wrong:** Scores adjust index without shared post-grade safety; score 0 leaves tracker due now; late session applies hidden timing penalty; effort leaks from session duration.

**Why:** v1.3 shipped parallel path beside `ForgettingCurve`.

**Avoid:** ADR table as strength mapping only; always strictly-future `nextRecallAt` (including score 0); effort neutral; late record time must not reduce vs same score on-time.

**Warning signs:** Index-only commissioned tests; invalid scores silently no-op.

---

### 7. Effort inverts or dominates correct outcomes

**What goes wrong:** Slow correct loses more than fast incorrect; early + thinking-time + fail compound; null thinking treated as punitive.

**Why:** Thinking-time adjustment applies on success without a clear polarity floor relative to outcome.

**Avoid:** Cap so correct cannot become failure / immediate due from effort alone; null = 0; commissioned effort hard-neutral.

---

### 8. Frequent-failure warning changes scheduling

**What goes wrong:** Threshold deletes, blocks, or shortens schedule.

**Avoid:** Informational only (`wrongCount`, `threshold`, `periodDays`); count incorrect only; overlap excluded (already); decide accidental.

---

### 9. Policy tests assert internal index instead of schedule

**What goes wrong:** Float index expectations pass while due-work regresses; legacy tests require late correct to weaken.

**Avoid:** Assert `nextRecallAt` deltas, due/not-due at grade, monotonic growth on correct streaks; keep index tests as algorithm units only.

---

### 10. Unsafe due-time rebuild from incomplete history

**What goes wrong:** Bulk recompute `nextRecallAt` from prompts / reinterpret history under new policy — corrupts due queue.

**Avoid:** No mass reschedule migration for this policy fix; update projection transactionally on each new grade; seed from tracker snapshot if needed.

---

## Integration gotchas

| Integration | Mistake | Correct approach |
|-------------|---------|------------------|
| Spelling API | Grade without tracker seam | Outcome → single scheduling apply |
| MCQ recall | Only spelling gets evidence fix | Same evidence rules via shared path |
| Learning Session record | Index updated, due stale | Set `lastRecalledAt`, apply score, strictly-future due |
| Session item without Feedback | Treat as failure | Tracker unchanged (ADR) |
| Manual mark-as-recalled | Bypass evidence rules | Document / route carefully |
| Frequent-failure API | Treat as blocking | Informational only |

## "Looks done but isn't"

- Late correct after multi-day delay lengthens schedule (not index drop for lateness)
- Early correct still `nextRecallAt > gradedAt`
- Accidental not on 12h failure path; weaker than incorrect
- Overlap leaves tracker fields unchanged
- Commissioned score 0 not due at record instant; scores 3–5 move forward
- Frequent-failure does not delete/block; accidental counting intentional
- Slow correct still strictly future
- Understanding MCQ and commissioned covered, not only spelling
- Index and `nextRecallAt` updated together after every grade

## Sources

- `docs/adrs/0003-spaced-repetition-scheduling-policy.md`
- `ForgettingCurve.java`, `MemoryTracker.java`
- `CommissionedLearningSessionFeedbackScheduling.java`
- `SpacedRepetitionEarlyRecallAdjustmentTest.java`
- `RecallPromptOverlapTryAgainTests.java`, `RecallPromptAccidentalMatchEdgeTests.java`
- `.planning/seeds/SEED-004-close-spaced-repetition-scheduling-policy-gap.md`

---
*Researched: 2026-08-12*
