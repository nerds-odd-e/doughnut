# Retry a failed recall question or choose self-review

**Identity:** SEED-081#story-2
**Source:** [Retry a failed recall-question load or choose self-review explicitly](../../seeds/SEED-081-recover-recall-request-failures.md#story-2)

## Goal and scope

Recover a web recall question-load failure on the current tracker without
advancing progress or recording a learning event. MCQ offers deliberate Retry
or existing Self-review; spelling offers Retry and presents its answer form
only with a loaded prompt. Keep failures and late completions local to their
tracker/attempt, including prefetch and a new spelling attempt after OVERLAP.
Preserve normal navigation, authorization, grading, timing and focus behavior.

Answer-submission recovery, spelling self-grading, automatic retry/backoff,
offline/cross-reload recovery, question-quality changes, generic transport
infrastructure and CLI/MCP interfaces are deferred. Preparation authorizes no
implementation. Story 1 is independently Taken; reconcile shared recall files
with its actual delivered behavior when execution starts, without acquiring its
submission-recovery work or making it a prerequisite.

## Current decisions and reuse

- Extend `useRecallPromptFetching` and `RecallPromptCard`, the existing web
  question-loading owners. Represent each tracker's pending, failed or loaded
  request distinctly; a failed result is not a successful self-review exercise.
  Keep explicit self-review choice with that tracker. Render the visible
  tracker's state rather than treating a missing cached prompt as a UI mode.
- Retry requests the existing `MemoryTrackerController.getRecallPrompt` for
  the failed tracker. Keep failed prefetch state until the learner deliberately
  retries. Prevent duplicate in-flight recovery and associate completion with
  the initiating tracker/attempt. Navigation can select another tracker while
  the original request finishes. No automatic retry loop or global blocker.
- Reuse `JustReview`, `MemoryTrackerAsync` and `JustReviewButtons`, including
  full note/property context, the existing Good readiness delay and Again,
  `markAsRecalled`, and `useRecallAnswerHandling.onJustReviewed`. Self-review
  selection alone does not call the grading operation.
- Spelling consumes the loaded prompt from the same card loading owner.
  Remove the spelling child's duplicate fetch and its dependent loading state,
  API mock setup and imports. Derive question content from the prompt; preserve
  typing, thinking time, inactive mask, soft-keyboard primer and emitted answer
  payload. A prompt is active for timing only while actually presented.
- The existing `spellingRetryNonce` denotes a new attempt after OVERLAP, not
  a failed-load retry. On that continuation, invalidate the answered cached
  prompt and load the next prompt for the same tracker. Preserve overlap
  feedback and focus on the fresh empty input; do not reuse the answered prompt.
- PFE: `DailyProbeGate` demonstrates inline Retry but owns a different task
  lifecycle, so reuse its presentation convention, not its state model.
  `apiCallWithLoading` already owns foreground loading/error reporting and
  authentication handling; preserve that boundary. Background prefetch does
  not need a foreground modal. CLI `nextRecallCardLoad.ts` and
  `recallMcqCardLoad.ts` have separate terminal-session owners; they share the
  unchanged backend operation, not these Vue states. MCP adds no caller of the
  changed Vue interface. No shared retry framework is justified.
- Accepted [ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md)
  owns Grade/Overlap/timing semantics;
  [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md) permits this
  explicit recovery outcome without suppressing the existing error path.
  Their file statuses and the ADR index agree. The North Star's server-side
  learning-data ownership stays intact; no new direction or ADR exception.

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
   final recovery needs the new discriminating observations below.
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

## Outside-in proof ownership

Use mounted RecallPage/Card tests with only the backend HTTP boundary mocked
for UI/queue ownership. Use controller/database tests for persistent learning
effects. Run a new recovery assertion red before its implementation; an existing
green baseline alone does not prove recovery. No new E2E harness or manual test
is required for this bounded change.

| Promise / example | Slice | Observable proof |
| --- | --- | --- |
| MCQ failure is explicit; Self-review reveals full note/property context and preserves progress until grading | 1 | Real page: fail loading, choose Self-review, observe context and Good/Again, then grade once and observe one normal progress advance. Assert zero grading requests before the grade as proof of the surviving learning boundary. Adapt `RecallPage.justReview.spec.ts` and `RecallPage.queueProgress.spec.ts` through the explicit choice; preserve their content, geometry and route assertions. |
| Current MCQ can fail again and then retry successfully on the same tracker | 2 | Page DOM: failure → Retry → pending → failure → Retry → returned question. Observe unchanged progress/tracker and enabled recovery after the second failure; duplicate pending clicks produce one request. |
| Prefetch failure and late completion preserve another visible attempt | 2, spelling extension 3 | Deferred SDK responses: fail future prefetch while the current question/input stays usable; navigate to it and retry. Move another tracker current during a retry, resolve success/error, and observe that card and progress unchanged; revisit the original tracker to observe its own result. Drive navigation through page controls. |
| Loading/retrying alone changes no answer, Grade or schedule | 2 MCQ; 3 spelling | Page tests observe unchanged progress and no answer/grade API call during recovery. Controller tests load/reload using real persistence and compare unanswered prompt, RecallLog state and nextRecallAt; retain focused versions of the successful preparation observations. Existing self-review grading tests own the subsequent Grade. |
| Spelling offers recovery before entry and then submits the loaded prompt | 3 | Failed initial/repeated load shows Retry; loading occupies the question area; success shows actual stem and enabled entry. Type and submit via DOM, assert the returned prompt ID reaches answerSpelling. Form availability is the promised active refusal for a missing prompt, not an absence test for removed implementation. |
| Spelling overlap continuation, thinking time, focus and keyboard behavior survive | 3 | Real page: accept OVERLAP, fail the next load, retry, receive a different prompt ID, enter a more specific answer and observe that ID in submission with feedback/focus intact. Preserve standalone display timing/mask/keyboard assertions with prompt props supplied by makeMe. |
| Existing authorization/error handling remains in force | 1–3 | Keep the SDK/wrapper and endpoint authorization boundaries; retain existing managedApi error/auth tests and controller unauthorized/503 tests in the relevant full suites. No self-review action bypasses note/grade authorization. |

Consumer trace: the fetcher is consumed by RecallPromptCard, which is rendered
by RecallPage even while answered history hides it. Enclosing observations
include whole-page progress and note-context text, property links, control
geometry, previous-answer display, resume focus and OVERLAP. Direct spelling
mounts go through `spellingQuestionDisplayTestSupport.ts`; supply real prompt
fixtures there after the prop change. `RecallPromptCard.spec.ts` has an existing
failure-to-self-review expectation that must become the explicit-choice journey.
Existing E2E recall scenarios consume ordinary MCQ/spelling/overlap behavior,
not a different API contract; retain their product behavior. The full frontend
suite covers the shared helper and enclosing consumers rather than just the new
spec. CLI/MCP share the unchanged backend contract and do not consume the changed
Vue props; their interface behavior is outside this UI change.

One-time removal reading in slice 3: search the whole product for the removed
`fetchSpellingQuestion` symbol and retire child-fetch-only imports/setup/helpers
transitively; inspect remaining `SpellingQuestionDisplay` call forms for the
new prompt contract. Keep provenance in the observation above. Record the sweep
as an acceptance reading, never a permanent absence test.

## Ordered slices

### 1. Choose self-review after an MCQ load failure
Type: Behavior
Status: planned
Proof: First row of the proof table; preserve existing note/property context,
grade controls, progress and authorization/error behavior.

Behavior: MCQ loading fails → the card explains the failure → the learner
chooses Self-review → existing review controls appear → a subsequent grade
performs the ordinary single review and continuation.

Distinguish failed/loading/loaded MCQ state in the existing loading owner and
make self-review a tracker-owned explicit choice. Preserve the spelling path
until slice 3. Update current fallback-dependent page/card tests together with
the behavior. Sizing hypothesis: about 5 minutes including focused proof and
cleanup. Safe interim: MCQ failure has an intentional self-review route; direct
Retry arrives in slice 2. This is an accepted delivery trade-off.

### 2. Retry the current or prefetched MCQ question
Type: Behavior
Status: planned
Proof: MCQ retry, repeated failure, prefetch/navigation and learning-state rows.

Behavior: the current MCQ load has failed → Retry loads for that tracker →
success presents its question, or failure offers Retry again, with progress and
learning state unchanged. Background failures and late replies remain local.

Extend the same state owner with targeted retry and pending-request ownership;
keep loaded and failed future trackers distinct from unfetched ones. Retain
ordinary navigation and explicit self-review. Add page/card deferred-response
journeys and a controller persistence regression for failed-then-successful MCQ
loading. Sizing hypothesis: 5–8 minutes including proof and cleanup. Scrutiny
above the five-minute target: request identity, retry and prefetch exercise the
same loading rule and one recovery outcome; splitting them would expose a retry
that can corrupt another visible attempt. Safe stopping point: complete MCQ
recovery, with spelling still handled by its original path until slice 3.

### 3. Recover spelling loads through the shared question owner
Type: Behavior
Status: planned
Proof: Spelling recovery, overlap/focus/timing, navigation and learning-state
rows, plus the separate removal reading.

Behavior: a spelling prompt load fails, including the new attempt after
OVERLAP → the learner retries → a real loaded prompt enables answer entry for
that attempt, on the same tracker, without grading from the load itself.

Pass the shared loaded prompt into SpellingQuestionDisplay and remove its
duplicate fetch completely. Invalidate on the existing new-attempt signal,
keeping request results tied to the initiating attempt. Update all direct
mount fixtures and preserve timing/focus/keyboard behavior. Add failed-load and
overlap-retry page journeys and the controller repeated-spelling-load regression.
Sizing hypothesis: 5–8 minutes including proof and cleanup. The extra time
covers the observed overlap/fixture coupling; separating relocation from
invalidation would knowingly break that continuation. Safe stopping point:
all promised question-load recovery is usable independently of story 1.

## Verification and delivery

- Use `CURSOR_DEV=true nix develop -c pnpm frontend:test` at frontend acceptance,
  plus `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`
  and `CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit`
  against the same content, per the frontend skill. Focused file commands above
  are available while iterating; shared helpers and page consumers require the
  full frontend suite before acceptance.
- When retaining the controller tests, run
  `CURSOR_DEV=true nix develop -c pnpm backend:test_only`, the backend skill's
  required full suite. No migration or generated API change is planned.
- Follow AGENTS.md execution wrap-up: Jidoka, fresh post-change-refactor agent,
  API generation if a signature actually changes, coordinator
  `./scripts/run.sh pnpm format:changed` once, plan update, commit through the
  agent helper with check-only hook, and push under the authorized execution
  mode. Planning does not start these execution steps.
- Apply the repository five-minute target and ten-minute hard scrutiny boundary:
  if a leaf exceeds ten minutes, park only owned work and refine the remaining
  leaf before continuing unless a stated focused-test/external-wait exception
  applies. Do not silently expand into answer recovery or a new transport layer.

## Plan review

Slice-plan refinement was not needed: three independently useful recovery
choices use one tracker/attempt loading rule, with the observed spelling
invalidation coupled to its consumer change. Proof covers each final promise;
slice 1's interim lack of direct Retry is explicitly replaced by slice 2.
No remaining slice-specific concern was identified in this review. Production
frequency and recovery savings remain unknown; neither is a premise of this
bounded repair. Readiness records preparation judgment, not execution authority.
