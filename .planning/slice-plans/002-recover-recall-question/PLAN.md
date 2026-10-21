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
implementation. Story 1's delivered submission recovery is retained in shared recall files;
this work does not acquire its answer-submission responsibility.

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

[Preparation evidence](EVIDENCE.md) retains the dated revision, commands,
observations and limitations. Mounted-page observations reproduced MCQ fallback,
failed future prefetch and empty spelling entry; pass-through spelling reuse
broke OVERLAP continuation and direct fixtures. Real-database controller probes
established safe endpoint reuse without learning-state changes. All scratch
changes were reverted; retained regressions belong to slices 2 and 3.

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

## Execution context

Established start resumed on 2026-10-21: story-branch mode, publisher
`dashboard-territory.local-doughnut`, agent Eimi-chan, workspace
`/Users/terryyin/git/doughnut/.worktrees/retry-a-failed-recall-question-load-or-choose-se`,
branch `codex/retry-a-failed-recall-question-load-or-choose-se`, remote `origin`,
trunk `main`. Starting revision `80e837bc8f0189e0b5564d89e5a378bd5fd87f17`;
published claim and initial candidate `c10f6c2c288448026a3376861be3c8f93c14a0bc`.
The established start reported Changed since readiness review; scope still
aligns with the seed and the reviewed basis remains unchanged. Replanning is
allowed within existing planning authority. The selected checkout is the owned
execution worktree; no default-checkout mutation is needed for increments.

Checkout setup passed `./scripts/run.sh bash scripts/worktree_setup.sh` against
the current lockfile. The applicable project command
`CURSOR_DEV=true nix develop -c pnpm frontend:test tests/recall/RecallPromptCard.spec.ts`
passed 7 tests before implementation. The installed pre-commit hook runs
check-only `./scripts/run.sh pnpm lint:changed`; workspace author is Eimi-chan.

CI source is GitHub Actions, verified selector `ci.yml` (display name `donut CI`),
which runs on branch pushes. Observer coordinator `eimi-story-2`, directory
`/tmp/dough-ci-501/watch-xb0ZSP`, PID `55116`, session `46925`, yielded cell `9`,
is bound to this checkout and the execution branch. The earlier trunk claim is
unobserved by this branch observer. Increments use managed delivery on this stream. Slice 1 was accepted on the
execution branch at `07606012773257fbb71ba09491cc96907e127b6a`.

## Ordered slices

### 1. Choose self-review after an MCQ load failure
Type: Behavior
Status: done
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
Status: done
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


## Story obligations

### G1. MCQ Retry and request-isolation promises remain after slice 1
Reported: slice 1 — "Retry, repeated failure, pending/late-result isolation and MCQ persistence regressions remain for slice 2."
Story clause: "Offer deliberate Retry on the same tracker and session position, including after a repeated failure."
Disposition: proved by slice 2: RecallPage.questionRecovery.spec.ts repeated failure, pending and navigation journeys; MemoryTrackerRecallPromptControllerTest.failedThenSuccessfulMcqLoadingPreservesLearningState

### G2. Spelling question recovery and persistence remain after slice 1
Reported: slice 1 — "Spelling fetch consolidation, failed-load and OVERLAP recovery, input preservation, attempt ownership and persistence regressions remain for slice 3."
Story clause: "For spelling, make the stem and answer form usable only with a loaded prompt."
Disposition: receiving slice 3


### G3. Existing E2E consumers expected automatic Self-review
Reported: slice 1 — "CI run 39583877557 attempt 1 failed seven existing recall/probe/scheduling journeys because their AI-disabled paths expected immediate question/note rendering after a load failure."
Story clause: "For MCQ failures, offer Self-review explicitly. Selecting it reveals the existing note context and Good/Again controls"
Disposition: proved by slice 1: repaired browse_answer_and_notes_while_recalling.feature, daily_probe.feature and spaced_repetition.feature retain their assertions after deliberate Self-review; see EVIDENCE.md CI repair
