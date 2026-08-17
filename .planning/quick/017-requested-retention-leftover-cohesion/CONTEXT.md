# CONTEXT: Requested-retention leftover cohesion

**Scope:** Cleanup after locking `r = 0.9` and moving fail due onto `I`. No schedule change. Do not accept ADR 0003.

## Locked this session

| Decision | Lock |
|----------|------|
| Production due | Unchanged. After a grade, `last + I(0.9, S)`; non-positive `I` → 24h. Confusion still `last + I`, never later. |
| Keep | `Fsrs.REQUESTED_RETENTION` / `intervalHours` identity. Just-review E2E **8 hours** (user-visible due = Stability). Accidental-match **controller** due pin (`last+round(S)`). Canonical New-fail unit pin (S=0, D unset, 24h). Success due pin `nextRecallAtIsLastRecalledAtPlusStabilityHours`. |
| Drop | E2E due pins that only restate New-fail 24h. The named step that claims “incorrect grade time as Last Recall Time” but only asserts a 24h interval. Tautological fail `S=17` **and** due `last+17` (due already follows `I`). |
| Scheduling types | Inline `MemoryTrackerNextRecallScheduling` and `MemoryTrackerConfusionAdjustment` back onto `MemoryTracker`. Stay under 250 by moving the JPQL fragments (already a separate concept), not by keeping 17-line apply classes. Fallback hours go through `Fsrs.intervalHours(FIRST_SUCCESS_STABILITY_HOURS)`. |

## What inspection found (not a product bug)

Fail due and New-fail 24h match ADR 0003 Proposed. The leftover is test overlap and a file-size split from wrapping `calculateNextRecallAt` / adding the 24h fallback.

## Out of scope

Accept ADR 0003. Varying `r`. Delete `DEFAULT_SPACES`. Backfill in-flight rows. Collapse `CommissionedLearningSessionFeedbackScheduling` (score map, not a second due path).
