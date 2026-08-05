# Remove the late-success penalty

**Status:** Planned  
**Scope:** Ad-hoc corrective slice; the broader spaced-repetition ADR remains Proposed and paused  
**Execution:** Test-first with one stop-safe Behavior phase

## Goal

A correct recall submitted after its due time must not receive a shorter next
interval merely because it was late. This fixes the observed loop in which an
overdue correct spelling answer drives the forgetting-curve index to `100` and
makes the item due again immediately.

## Current cause

`ForgettingCurve.succeeded` subtracts a schedule-deviation adjustment based on
the absolute distance from `nextRecallAt`. Once lateness exceeds the previous
interval, a nominally successful answer can reduce the index. The index floor
then turns a sufficiently late success into index `100`; with the default
spacing configuration that produces a zero-hour next interval.

This plan treats `nextRecallAt` only as the existing scheduling input/output.
It does not decide whether that field should eventually be authoritative state
or a rebuildable projection.

## Narrow policy decision

- A late correct answer receives the same timing credit as an on-time correct
  answer when memory state and answer effort are otherwise equal.
- This patch gives no extra bonus for lateness. Whether to add a bounded late
  success bonus belongs to the paused ADR discussion.
- The existing early-recall discount remains unchanged.
- Thinking-time adjustment, incorrect/partial-answer penalties, the index
  floor, user spacing configuration, and `nextRecallAt` persistence remain
  unchanged.
- No database migration, scheduler replacement, FSRS adoption, or recall-state
  reconstruction work is included.

## Phase 1 — Late correct recall keeps at least the on-time interval

**Type:** Behavior  
**Status:** Planned  
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

The regression test compares resulting intervals rather than asserting an
index value, formula, fixed number of days, or internal helper call. This is the
bottom-line contract and leaves room for a later scheduler design.

### Test-first work

1. Extend the existing spelling-answer controller integration tests in
   `backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java`
   with one capability-named scenario such as
   `lateCorrectAnswerDoesNotShortenTheNextInterval`.
2. Drive both answers through `RecallPromptController.answerSpelling`, use equal
   thinking time, and compare `nextRecallAt - answeredAt` for the equivalent
   trackers.
3. Run the controller test and confirm that it fails because the overdue
   answer receives a shorter or zero interval under the current lateness
   calculation—not because of fixture setup or time-zone behavior.
4. Keep only this one intentional red test while changing production code.

### Smallest production change

1. Change `backend/src/main/java/com/odde/doughnut/entities/ForgettingCurve.java`
   so schedule deviation discounts a successful recall only when the recall is
   early (`delayInHours < 0`).
2. For on-time or late success, start from the existing baseline success
   increment, then apply the existing thinking-time adjustment unchanged.
3. Do not introduce late-success bonus logic, new state, feature flags, or a
   new scheduling abstraction in this slice.

### Align existing tests with the corrected contract

1. Rename
   `SpacedRepetitionEarlyRewardsAndLatePenaltyTest.java` to a capability name
   such as `SpacedRepetitionEarlyRecallAdjustmentTest.java`.
2. Retain its on-time and early-recall coverage; remove assertions whose sole
   purpose is to require a late penalty or reset.
3. In `RecallServiceWithSpacedRepetitionAlgorithmTest`, retain relevant
   on-time/early and recall-selection coverage, and remove exact late-penalty
   rows that duplicate the obsolete formula. The controller regression test is
   the durable late-success contract.
4. Search backend tests and production code for other expectations that a
   correct late answer loses index, and align only those directly contradicted
   by this narrow rule. Do not edit the Proposed ADR as part of execution.

### Verification

Run the focused backend set:

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only \
  --tests com.odde.doughnut.controllers.RecallPromptControllerTests \
  --tests com.odde.doughnut.algorithms.SpacedRepetitionEarlyRecallAdjustmentTest \
  --tests com.odde.doughnut.services.RecallServiceWithSpacedRepetitionAlgorithmTest
```

Then inspect the diff for scope, run the repository's normal backend formatting
check if any Java formatting changed, and apply Jidoka: do not close the phase
with a known relevant failure, ignored test, stale late-penalty name, or
unexplained behavior change.

## Acceptance criteria

- The new controller-level regression fails against the current implementation
  for the expected late-penalty reason and passes after the fix.
- With equivalent state and answer effort, lateness alone cannot shorten the
  next interval after a correct answer.
- A sufficiently overdue correct spelling answer is not immediately due again.
- Early-recall adjustment and thinking-time behavior remain covered and green.
- Failure behavior, storage architecture, and the Proposed ADR are untouched.

## Deferred decisions

- Whether a successful overdue recall should earn a bounded bonus.
- Whether to adopt FSRS or another memory model.
- Whether `nextRecallAt` can be reconstructed from preserved events and
  versioned configuration, or must remain de facto stored architecture.
- Broader success/failure/effort penalty policy and migration of existing
  trackers.

