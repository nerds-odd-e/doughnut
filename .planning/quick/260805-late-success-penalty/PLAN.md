# Remove the late-success penalty

**Status:** Done  
**Scope:** Ad-hoc corrective slice; the broader spaced-repetition ADR remains Proposed and paused  
**Execution:** Test-first with one stop-safe Behavior phase

## Goal

A correct recall submitted after its due time must not receive a shorter next
interval merely because it was late. This fixes the observed loop in which an
overdue correct spelling answer drives the forgetting-curve index to `100` and
makes the item due again immediately.

## Narrow policy decision

- A late correct answer receives the same timing credit as an on-time correct
  answer when memory state and answer effort are otherwise equal.
- This patch gives no extra bonus for lateness. Whether to add a bounded late
  success bonus belongs to the paused ADR discussion.
- The existing early-recall discount remains unchanged.
- Thinking-time adjustment, incorrect/partial-answer penalties, the index
  floor, user spacing configuration, and `nextRecallAt` persistence remain
  unchanged.

## Phase 1 — Late correct recall keeps at least the on-time interval

**Type:** Behavior  
**Status:** Done  
**Stop-safe outcome:** All affected backend tests are green, and late success
can no longer collapse the next interval through a lateness penalty.

### Observable behavior

**Precondition:** Two spelling memory trackers have equivalent learned state, a
non-zero scheduled interval, and equivalent answer effort.  
**Trigger:** One is answered correctly on time and the other is answered
correctly after a delay large enough to expose the current penalty.  
**Postcondition:** The overdue tracker's interval measured from its actual
answer time is at least the on-time tracker's interval, and therefore remains
strictly in the future.

### Done

- Controller regression:
  `RecallPromptControllerTests.lateCorrectAnswerDoesNotShortenTheNextInterval`
- Production: `ForgettingCurve.succeeded` discounts schedule deviation only when
  early (`delayInHours < 0`)
- Aligned tests renamed/slimmed:
  `SpacedRepetitionEarlyRecallAdjustmentTest`,
  `RecallServiceWithSpacedRepetitionAlgorithmTest`,
  `ForgettingCurveThinkingTimeTest`
- Proposed ADR left untouched

### Learnings

- A 100-day overdue correct answer previously shortened the next interval (not
  always to zero at index 200); interval comparison is a durable contract.
- `ForgettingCurveThinkingTimeTest` also asserted the obsolete late < on-time
  rule and needed aligning alongside the renamed algorithm test.
- `RecallPromptControllerTests` remains oversized (~1400 lines); splitting was
  deferred as a large structural move.

## Acceptance criteria

- [x] Controller-level regression failed for the late-penalty reason, then passed
- [x] Lateness alone cannot shorten the next interval after a correct answer
- [x] Sufficiently overdue correct spelling is not immediately due again
- [x] Early-recall and thinking-time coverage remain green
- [x] Failure behavior, storage architecture, and the Proposed ADR are untouched

## Deferred decisions

- Whether a successful overdue recall should earn a bounded bonus.
- Whether to adopt FSRS or another memory model.
- Whether `nextRecallAt` can be reconstructed from preserved events and
  versioned configuration, or must remain de facto stored architecture.
- Broader success/failure/effort penalty policy and migration of existing
  trackers.
