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


## Slice 2 acceptance

Fresh source-only `/root/source_promises_2` received only the original complete
Goal/Scope/exclusions/examples/UI with no history and no file access. Its actual
launch and returned individual commitments are retained in the conversation.
Compared original triggers with actual product and assertions:

- Current MCQ generation failure → inline Retry and Self-review; Retry → pending
  inline loader, then same-tracker question or recoverable repeated failure.
  RecallPage.questionRecovery.spec.ts first journey observes three tracker-1
  requests through failure/retry/failure/retry/success, duplicate pending clicks
  yielding one request, progress 0/2, and zero answer/grade calls.
- Pending Retry + Move to end → another current question remains usable.
  Parameterized success/error journey observes Question 2 and 0/2 before and
  after late completion, then revisits tracker 1 via the same page controls
  and observes its own question or Retry. No late result targets the new card.
- Future prefetch failure → current question/choices remain usable; reaching
  failed tracker → its own Retry → its question. Final page journey observes
  these transitions, tracker-2 retry path and unchanged progress. Foreground
  requests retain apiCallWithLoading; background prefetch uses direct SDK.
- Controller failure then success/reload → same due time, RecallLog state,
  unanswered persisted prompt and reuse. The named backend regression flushes
  and refreshes real persisted entities; it mocks only OpenAI generation.
- Explicit Self-review/context/Good/Again and one-grade continuation remain
  covered by slice 1's unchanged assertions and the full suite. Spelling input,
  prefetched input retention, per-attempt invalidation, overlap explanation,
  fresh prompt/focus/specific-answer journey and spelling persistence remain G2.
  Access/error behavior, backend API and grading policy remain unchanged.
  All original deferred promises remain outside this change.

Red: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/RecallPage.questionRecovery.spec.ts`
failed on missing Retry before implementation. Focused questionRecovery+Card
command passed 12 tests. Final `CURSOR_DEV=true nix develop -c pnpm frontend:test`
passed 387 files/2401 tests (79.14s). Both frontend typechecks named in slice 1
passed. `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed (test 73s,
worktree migration 42s), including the controller regression with zero XML
failures/errors. Runtime DB `doughnut_wt_dae038e397854c188d64ae36bb2863c1_test`.
Initial frontend spy-argument typing errors were corrected before final proof.
Active implementation about 3 minutes (agent estimate), verification waits
excluded. Independent refactor consolidated Move-to-end DOM navigation into
recallPageTestSupport.ts for question recovery, MCQ/spelling submission recovery
and queue progress. Coordinator inspected unchanged observing assertions.
`CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/RecallPage.questionRecovery.spec.ts tests/pages/RecallPage.answerRecovery.spec.ts tests/pages/RecallPage.spellingRecovery.spec.ts tests/pages/RecallPage.queueProgress.spec.ts`
passed 20 tests; both frontend typechecks passed. Other accepted proof stayed
compatible. CI repair e46d23deacb110a1cd861519810c311b5faeddbd changed only E2E and
records; slice-2 saved work restored cleanly before this refactor.
G1 is proved; G2 still belongs to slice 3. Preparation basis is not renewed.


## Slice 3 acceptance

Fresh source-only `/root/source_promises_3` received only the original complete
Goal/Scope/exclusions/examples/UI, with no inherited history or file access.
Its actual input and individual returned commitments remain in the conversation.
All source promises were compared with the actual product and proof:

- Spelling failure/repeated failure → Retry; pending → inline busy state and
  unavailable entry; success → actual stem and enabled entry. The new
  RecallPage.spellingQuestionRecovery.spec.ts first journey observes these
  states, duplicate pending clicks, no answer/grade and 0/2 during recovery,
  and returned prompt 71 in the subsequent real DOM submission.
- Failed future prefetch while typing → input retained; reaching tracker →
  its own Retry and loaded stem. Pending Retry then page Move-to-end → other
  stem/input and 0/2 intact after late success or error; revisit → own result.
- OVERLAP → fresh same-tracker load fails → explanation and Retry → pending →
  loaded prompt 72. Final page journey observes retained explanation, empty
  focused entry reachable at 375x667, 0/2, tracker-1 load paths, more-specific
  answer using 72, and 3000ms thinking after a 20-second simulated load delay.
- Shared loader owns tracker/request identity and cache invalidation. Required
  child prompt mounts entry only when presented; standalone display tests keep
  mask, keyboard, timing and submission behavior. Initial full-suite failure
  exposed old duplicate-fetch fixtures in spelling submission-recovery tests;
  those setups/counts were removed while retaining retry payload 101 and fresh
  102 submission observations. Focused affected consumers passed 12/12.
- Real controller/database repeated spelling loads retain the same unanswered
  prompt, RecallLog and due time. MCQ regression moved alongside this in
  MemoryTrackerRecallPromptRecoveryControllerTest and passed again. No API or
  grading-policy change. Slice-1/2 Self-review, loading, Retry, navigation,
  no-learning and wrapper/access promises remain covered in the full suites.
  All original deferred promises remain excluded; no automatic retry added.

Red command `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/RecallPage.spellingQuestionRecovery.spec.ts`
failed on missing-prompt entry before implementation. Final
`CURSOR_DEV=true nix develop -c pnpm frontend:test` passed 388 files/2406 tests.
Both exact frontend typecheck commands retained above passed on frozen final
content. `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed: 2870
tests, 2 skipped, zero failures/errors. `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/recall/recall_quiz_spelling_question.feature,e2e_test/features/recall/overlap_try_again.feature`
passed 4/4. The E2E overlap page method observes card-level feedback and retains
empty/focused entry assertions. Active implementation/diagnosis about 5 minutes
(agent estimate), verification waits excluded. Independent refactor extracted the shared typed deferred prompt response into
recallPageTestSupport.ts. The MCQ/spelling question recovery specs retain all
scenarios/assertions; their focused pair passed 9/9 and both frontend typechecks
passed on refactored content. Other accepted proof stayed compatible. Coordinator
inspected the helper and consumers before status mutation. G2 is proved; no open story gaps remain.

Separate removal reading: `fetchSpellingQuestion` has zero runtime references
across frontend/backend/CLI/MCP/packages/E2E/docs. PLAN's sweep instruction and
DD-318's explicitly dated source observation retain the symbol as provenance;
no observed action was renamed. Every direct SpellingQuestionDisplay call
(Card and standalone mount helper) supplies a loaded prompt. No absence test.
