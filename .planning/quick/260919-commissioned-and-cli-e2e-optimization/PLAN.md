# Commissioned learning session and CLI existing-note-edit E2E optimization

## Source

`dough-test-optimization` pass over the two E2E entries recorded in
`.planning/test-optimization-candidates.md` (both dated 2026-09-18):

- `e2e_test/features/learning_session/commissioned_learning_session.feature`
- `e2e_test/features/cli/cli_notebook_existing_note_edits.feature`

Developer instruction: judge each scenario's value against its cost, keep as few
tests as coverage allows, follow the automated-test guidelines, minimize
exceptional cases, and keep the resulting test design clean and simple —
performance must come from simpler tests, not from added complexity. Where cost
survives a serious attempt and the test is worth keeping, apply
`@skipOptimizationDueToKnownNecessarySlowness` and drop the candidate entry.

## Goal and scope

Shorten the local E2E feedback path for these two features by removing proof
that is already owned at a cheaper boundary, and by merging compatible
observations that share one expensive browser or CLI lifecycle. Keep every
behavioral promise: move FSRS scheduling arithmetic to the in-memory entity
tests that already own that responsibility rather than deleting it.

Out of scope: the frontend candidates in the same record, changes to product
code, and new test infrastructure.

## Baseline

Revision: `ec8581e541` (worktree `worktree-e2e-test-optimization`).
Runner mode: ordinary local wrapper, one spec per invocation, no tag override.

```bash
SUT_TIMEOUT_MS=360000 CURSOR_DEV=true nix develop -c pnpm cy:run --spec <feature>
```

| Feature | Cases | Wall time |
| --- | --- | --- |
| `commissioned_learning_session.feature` | 15 | 00:47 |
| `cli_notebook_existing_note_edits.feature` | 6 | 00:42 |

Per-scenario baseline, commissioned feature: assimilate-commissioned 4085ms;
due-trackers-await-tutor 2539ms; open-request 2856ms; different-notebooks
3347ms; record-report 4472ms; feedback-text-on-tracker 2881ms;
feedback-without-inner-tags 2999ms; dated-feedbacks 3606ms; first-grade outline
2263/2594/2291/2329ms; second-grade outline 3623/3653/3892ms.

Per-scenario baseline, CLI feature: related-edits-second-checkout 12487ms
(absorbs the one-time `@bundleCliE2eInstall` cost for the spec);
successive-commits 5668ms; batch-after-web-save 5475ms; successive-web-saves
4604ms; local-Pasta-and-web-Overview 6835ms; conflict-resolution 6888ms.

Removing the first CLI scenario only saves its marginal cost, because the
install cost moves to whichever scenario runs first.

## Family analysis

### Commissioned learning session

The feature mixes three responsibilities: the commissioned-tracker user
journey, the learning-session request/record wiring, and FSRS scheduling
arithmetic. Only the first is genuinely browser-shaped.

Surviving proof already owned elsewhere:

- `LearningSessionRequestTests.requestMarkdownReflectsPriorRecordedFeedbackPerTracker`
  builds the same two dated Feedbacks for "Hola" (grade 3 "Pronunciation was
  clear", grade 4 "Fluent greeting") and asserts the same rendered session-item
  text, making the "last two dated Feedbacks" scenario duplicated proof.
- `LearningSessionReportFeedbackBlockParsingTest.unclosedSessionItemRunsToEndOfBlock`
  parses a `<session_item_feedback>` block whose inner `<session_item>` tags
  were dropped, which is exactly the "copied without inner tags" scenario.
- `LearningSessionRecordTutorFeedbackRecallLogTests.matchedGradeLeavesMappedRecallLog`
  already covers all four grade values through the record path, so the outlines
  add no wiring protection per grade.
- `RecallsCommissionedLearningSessionTests.shouldListDueCommissionedTrackersSeparatelyFromOrdinaryRecall`
  owns the due-vs-ordinary split at the controller.
- `MemoryTrackerCorrectRecallSchedulingTest` and
  `MemoryTrackerIncorrectRecallSchedulingTest` own FSRS arithmetic in memory and
  already assert the outline's grade-3 row (`FIRST_GOOD_STABILITY_HOURS = 55.0f`,
  `FIRST_GOOD_DIFFICULTY = 2.118104f`) and grade-1 row
  (`FIRST_AGAIN_STABILITY_HOURS = 5f`, `FIRST_AGAIN_DIFFICULTY = 6.4133f`).
  Grades 2 and 4 are not yet owned there; that is a real gap the outline was
  compensating for at browser cost.

Scenario-level redundancy inside the feature: "Due commissioned trackers await a
Tutor rather than ordinary recall" asserts a strict subset of "Notes from
different notebooks are commissioned as separate learning sessions" — the same
zero-notes-to-recall and one-session-for-Spanish observations off the same
Background.

Compatible lifecycle: recording a tutor report and then reading the tracker page
is one user story. Three scenarios (record-report, feedback-text-on-tracker, and
the first-grade outline) each pay for a full browser journey to observe one part
of that single outcome.

### CLI notebook existing-note edits

Every remaining second is real CLI subprocess and Git work, which is the
protocol behavior under test — there is no fixed sleep or avoidable wait. The
available saving is scenario redundancy, not per-step cost.

- "Publishing related edits to existing notes updates Donut and a second
  checkout" is the single-commit case of the very next scenario, and the
  single-commit publish received by a clean second clone is separately owned by
  `cli_notebook_publish_to_clean_clone.feature` ("Publishing a folder README and
  notes together is received by a clean clone"), which asserts the accepted head
  and the received contents.
- "Pulling then publishing keeps a local Pasta edit and a web Overview save" is
  the same rebase class as "Pulling then publishing keeps a related Overview and
  Pasta batch after a Shopping list web save": a local commit plus a web save of
  a different, already-tracked note, rebased on pull and then published. The
  retained scenario is strictly richer (two-file local batch). The single-file
  variant is additionally owned by
  `cli_notebook_web_local_reconciliation.feature`.

## Outside-in proof

- The commissioned feature still proves, in the browser: assimilating with a
  tutor creates both trackers; due commissioned trackers are separated per
  notebook from ordinary recall; the learning-session request is shown without
  being persisted; and recording a tutor report clears the potential session and
  updates the tracker page with the tutor's text and the new schedule.
- FSRS first-grade values for all four grades, and on-time second-grade values,
  are proved by in-memory entity tests.
- A second recorded tutor session schedules from its own time, proved at the
  learning-session controller.
- The CLI feature still proves: a multi-commit publish preserves A-to-C history
  for a second checkout at the accepted head; a pull rebases a local batch over
  a web save and publishes it; successive web saves arrive as an append-only
  chain; and a rebase conflict pauses, resolves, and publishes.

## Ordered slices

### 1. FSRS first-grade values owned in memory for every grade
Type: Behavior
Status: done
Proof: `backend/gradlew -p backend test --tests '*MemoryTracker*RecallSchedulingTest'`

Behavior: a newly assimilated tracker graded EASY or HARD for the first time
takes the FSRS S0/D0 stability and difficulty for that grade, alongside the
GOOD and AGAIN rows already owned.

### 2. On-time second-grade stability owned in memory
Type: Behavior
Status: done
Proof: `backend/gradlew -p backend test --tests '*MemoryTrackerCorrectRecallSchedulingTest'`

Behavior: a tracker first graded GOOD and then graded again exactly at its due
time grows to the FSRS stability for the second grade.

### 3. A later tutor session schedules from its own time
Type: Behavior
Status: done
Proof: `backend/gradlew -p backend test --tests '*LearningSessionRecordTutorFeedbackTests'`

Behavior: recording a second learning session after the first one's due time
sets `lastRecalledAt` to the later session time and projects the next recall
from it.

### 4. Commissioned feature keeps one browser journey per outcome
Type: Structure
Status: done
Proof: `pnpm cy:run --spec e2e_test/features/learning_session/commissioned_learning_session.feature`, three consecutive passes

Structure: drop the duplicated dated-Feedbacks, inner-tag-less feedback, and
due-trackers scenarios; drop both `Scenario Outline`s; merge the record-report
and tracker-observation scenarios into one recorded-session lifecycle that
observes the cleared potential session, the tutor's feedback text, and the
resulting Stability, Difficulty, and interval. Enables the candidate entry's
removal.

### 5. CLI feature keeps one scenario per Git-sync outcome
Type: Structure
Status: done
Proof: `pnpm cy:run --spec e2e_test/features/cli/cli_notebook_existing_note_edits.feature`, three consecutive passes

Structure: drop the single-commit publish scenario after moving its
accepted-head observation onto the successive-commits scenario, and drop the
single-file rebase scenario. Enables the necessary-slowness decision.

### 6. Candidate record reflects the outcome
Type: Structure
Status: done
Proof: candidate record and feature tags read back

Structure: remove both E2E entries from
`.planning/test-optimization-candidates.md`; tag the CLI feature
`@skipOptimizationDueToKnownNecessarySlowness` because its remaining per-scenario
cost is real CLI subprocess and Git protocol work.

## Current decisions

- FSRS arithmetic is an entity responsibility. The browser keeps exactly one
  observation that the tracker page renders a schedule at all; every numeric row
  moves to the in-memory tests.
- The CLI feature is tagged rather than further optimized: its cost is the real
  `clone` / `commit` / `pull` / `rebase --continue` / `publish` subprocesses that
  the feature exists to protect.
- The commissioned feature is not tagged; after this pass its per-scenario cost
  is expected to sit at or below the suite average.

## Results

Measured under the same wrapper, worktree, and machine as the baseline.

| Feature | Cases before → after | Wall time before → after |
| --- | --- | --- |
| `commissioned_learning_session.feature` | 15 → 4 | 00:47 → 00:14 / 00:13 / 00:13 |
| `cli_notebook_existing_note_edits.feature` | 6 → 4 | 00:42 → 00:21 / 00:21 / 00:21 |

Three consecutive passes each, no retries, no added waits. Combined local E2E
feedback for these two specs went from 01:29 to 00:34.

Replacement cost is negligible: the moved proof is six in-memory entity cases
and one learning-session controller case, all inside suites that already run.

Test and support code shrank by 237 lines net, including the removal of the
`expectLearningSessionRequestIncludesDatedFeedbacks` page-object method with its
`escapeRegExp` and `sessionItemSection` helpers, and the two API-shortcut
`Given` steps that only the deleted scenarios used.

Per-scenario after, commissioned feature: assimilate-commissioned 4124ms;
due-trackers-per-notebook 3675ms; open-request 2783ms; recorded-session
lifecycle 3336ms.

Per-scenario after, CLI feature: successive-commits 8031ms (now absorbs the
one-time install); batch-after-web-save 4914ms; successive-web-saves 3616ms;
conflict-resolution 4515ms.

## Learnings

- The two `Scenario Outline`s were compensating for a real gap: FSRS first-grade
  values for HARD and EASY were owned nowhere else. The fix was to close the gap
  in memory, not to keep paying for it in the browser. Grade rows GOOD and AGAIN
  turned out to be exact duplicates of constants that already existed in
  `MemoryTrackerCorrectRecallSchedulingTest` and
  `MemoryTrackerIncorrectRecallSchedulingTest`.
- Merging the record-report, tracker-feedback, and first-grade scenarios did not
  produce a sprawling scenario: recording a tutor report and then reading the
  tracker is one user story, so the merged scenario reads as one lifecycle with
  two postconditions.
- Deleting the first scenario of a `@bundleCliE2eInstall` spec does not save its
  reported duration. The install cost simply moves to the next scenario, so the
  saving is the deleted scenario's marginal cost. Baseline arithmetic on
  per-scenario reporter numbers overstates the gain unless this is accounted for.
