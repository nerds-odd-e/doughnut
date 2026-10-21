# Recall question recovery evidence

## Preparation observations and decisive premises

Observed on 2026-10-21 in
`/Users/terryyin/git/doughnut/.worktrees/retry-a-failed-recall-question-load-or-choose-se`
at `c5e0b9ff1b315317d6ea53cf620a2a0f93ea599e`, with the temporary changes
described below. All scratch edits were reverted before the preparation writes.
These are local reproductions and bounded observations, not production incidents
or evidence that the planned remedy is implemented.

1. **Failure surfaces and prefetch ownership.** Temporary
   `frontend/tests/recall/QuestionLoadPreparation.spec.ts` mounted the real
   card/children using existing support and wrapped SDK errors. MCQ failure
   rendered `Good Again`; a successful outer fetch followed by a failed spelling
   child fetch rendered `Answer`. Each failed the expected visible Retry check.
   A failed future prefetch left the current `.recall-prompt` present, but
   navigating to that tracker also rendered `Good Again` and failed Retry.
   Command:
   `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/recall/QuestionLoadPreparation.spec.ts tests/recall/RecallPromptCard.spec.ts tests/pages/RecallPage.justReview.spec.ts tests/pages/RecallPage.queueProgress.spec.ts tests/pages/RecallPage.spelling.spec.ts`.
   Result: three expected scratch failures, 20 existing tests passed, 4.17s.
   This proves the reported UI seams through mounted components; mocked HTTP
   does not prove database effects or a real transport outage.
2. **Fetch relocation consumers.** A scratch edit passed the cached prompt
   into `SpellingQuestionDisplay` and removed its own fetch, without changing
   test fixtures or adding overlap invalidation. Command:
   `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/recall/RecallPromptCard.spec.ts tests/pages/RecallPage.spelling.spec.ts tests/components/recall/SpellingQuestionDisplay.spec.ts`.
   Result: 15 passed, three failed, 3.09s. The page's overlap scenario observed
   no fresh request (one call before and after); standalone display fixtures
   lacked the required prompt and emitted no prompt ID. This falsifies simple
   pass-through reuse and establishes the invalidation and fixture work in
   slice 3. After reverting, the five existing files named in these two commands
   (excluding the scratch file, including `SpellingQuestionDisplay.spec.ts`)
   passed all 28 tests with `pnpm frontend:test` under Nix, 4.17s. These baseline
   checks cover normal self-review, progress, spelling feedback, focus and timing;
   final recovery needs the discriminating observations in the [plan proof table](PLAN.md#outside-in-proof-ownership).
3. **Backend loading preserves learning state.** Temporary controller tests
   `preparationRepeatedQuestionLoadsPreserveLearningState` and
   `preparationFailedThenSuccessfulMcqLoadPreservesLearningState` in
   `MemoryTrackerRecallPromptControllerTest` used real isolated MySQL and the
   existing OpenAI response mock. Repeated spelling loads returned the same
   unanswered prompt. MCQ generation returned failure, then a generated prompt
   on the next controller call. Both retained `nextRecallAt`, the queried
   RecallLog list and an unanswered prompt. Command:
   `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
   The first scratch compile used an unavailable getter; replacing it with
   existing `recallLogsOf` corrected the observation. The rerun passed the full
   backend suite (Gradle 58s), including both scratch cases and the existing
   prompt reuse, ownership, 503 and self-review grade tests. Database:
   `doughnut_wt_2baea056495d40eaa70473d09077ef73_test`.
   This establishes endpoint reuse; it does not simulate a real lost response
   or simultaneous requests. Backend implementation changes are not assumed.



## Slice 1 acceptance

Source-only reviewer `/root/source_promises_1` was launched with no history,
only the original story Goal, Scope, exclusions, key examples and UI text,
and instructions to read no files. Its actual input and complete returned
commitment inventory remain in this execution conversation. Comparison:

- Current MCQ failure → explanatory failed state and explicit Self-review;
  loading → ContentLoader; available question → ContestableQuestion. Observed
  by RecallPromptCard.spec.ts deferred loading and tracker-choice journeys.
- Choice → existing full note/reference/property context, Good/Again and
  reachable controls in place; grade alone → normal one-grade continuation.
  RecallPage.justReview.spec.ts observes no grade before/after choice, full
  context, property focus/route, Again geometry and one AGAIN request.
  RecallPage.queueProgress.spec.ts observes 0/3 after choice, one GOOD and 1/3.
- Navigation → choice remains tracker-owned; Card's two-tracker journey
  observes the other question and retained choice when revisiting.
- Retry on the same tracker/position, repeated failure, pending duplicate
  prevention, usable ordinary navigation and late success/failure isolation
  remain slice 2; spelling presentation/input, failed prefetch input retention,
  attempt ownership and OVERLAP recovery/focus remain slice 3. Persistent
  unanswered prompt/RecallLog/schedule observations remain their controller
  regressions. See G1/G2 rather than claiming these from frontend HTTP mocks.
- Existing backend API/grading policy and access/error wrappers are unchanged;
  full frontend suite preserves enclosing consumers and submission recovery.
  Answer-submission changes, spelling self-grading, automatic retry/backoff,
  offline/cross-reload support, quality, generic transport and CLI/MCP remain
  source exclusions.

Red: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/RecallPage.justReview.spec.ts`
failed both new choice journeys against old UI. Final proof:
`CURSOR_DEV=true nix develop -c pnpm frontend:test` passed 386 files/2397 tests
(77.54s); `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`
and `CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit` passed.
A first full run captured a misplaced test count assertion; it was moved after
the grade click and the final full run passed. Active implementation about
4 minutes (agent estimate); suite waits 73.00s and 77.54s plus focused checks.
Independent refactor is accepted in the coordinator conversation before this
status mutation. Scope understanding remains aligned; readiness basis is not renewed.


## CI repair

CI run 39583877557 attempt 1 at slice-1 SHA
`07606012773257fbb71ba09491cc96907e127b6a` failed seven E2E cases in three
features. Job 119167858550's failed assertions expected question-section or
note titles after disabled-AI MCQ loads. Local daily_probe reproduction at
unchanged HEAD failed all 3 scenarios; browser evidence showed GET recall-prompt
503 and the new deliberate Self-review choice. This was an overlooked E2E
consumer, not an infrastructure failure. Source-only reviewer 2's original
explicit-choice commitments apply to this repair as well.

The repaired journeys choose Self-review explicitly before expecting its note
and grade surface. Existing probe statistics, schedule/progress and retained
question/navigation assertions stay intact. Resume is a navigation action;
subsequent explicit choices or remembered-question assertions observe its
result. Self-review methods are cohesive in recallSelfReviewMethods.ts, composed
into the existing recall page object. A first repair batch showed that route
return preserves the choice; redundant repeated choices were removed, while
remount journeys retain an explicit choice.

Final `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/recall/browse_answer_and_notes_while_recalling.feature,e2e_test/features/recall/daily_probe.feature,e2e_test/features/recall/spaced_repetition.feature`
passed all 9 scenarios (2 browse, 3 probe, 4 scheduling), exit 0. Local log:
`/tmp/dough-story2-repair-green-final.log`. No product code or API changed; accepted
slice-1 unit/type proof is compatible. Ad hoc E2E tsc diagnostics reflect existing
unchanged imports/count/time-travel typing (HEAD and candidate each 229 diagnostics, zero new); the repository configures Biome and
Cypress proof, and no broad type remediation was attempted. Slice 2 was paused
and saved through ci-repair-stash before repair and is restored only after
accepted repair publication. G3 is proved by the final repaired journeys.
