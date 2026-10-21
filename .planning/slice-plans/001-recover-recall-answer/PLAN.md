# Recover a recall answer after a failed or lost submission response

**Identity:** SEED-081#story-1
**Source:** [Recover a recall answer](../../seeds/SEED-081-recover-recall-request-failures.md#story-1)

## Goal and scope

Recover the current MCQ or spelling submission deliberately, retaining its input
and timing, with one accepted answer, one set of learning effects and one session
continuation. Return the accepted result when the initial response was lost;
accept the retained submission when the initial request recorded nothing.
Preserve ownership, grading, overlap and accidental-match rules.

Question loading, automatic retries, offline work, drafts across page reloads,
historical note snapshots and unrelated mutation recovery are deferred. No
implementation or deployment is authorized by this preparation.

## Current decisions and reuse

- [Execution context](EXECUTION.md#execution-context) retains identity and checkout setup.
- CI observer: GitHub `nerds-odd-e/doughnut`, verified selector `ci.yml`
  (`donut CI`), target `codex/recover-a-recall-answer-after-a-failed-or-lost-s`.
  Coordinator `airi-recall-cbbdfa3f`; directory `/tmp/dough-ci-501/watch-NInIyd`,
  PID 25921, session 30714, yielded cell 9. The earlier trunk claim is
  unobserved by this branch observer; execution increments use this stream.

- The existing prompt ID is the attempt identity. Extend the existing
  `RecallPromptController.answer` and `answerSpelling` operations: authorize,
  serialize acceptance for that prompt, return its accepted answer if present,
  otherwise perform ordinary grading once in the same transaction. A different
  retry body does not revise an accepted answer. Do not introduce a generic
  idempotency-key store, a retry service, or a separate recovery read endpoint.
- Reuse `RecallPromptRepository.findByIdForUpdate`, already used by
  `ConversationService.startConversationAboutRecallPrompt`. The first probe must
  establish fresh answer visibility after lock acquisition: the path-variable
  entity can already be managed before the transactional controller runs. A lock
  alone is not assumed to refresh that entity or its answer association.
  See [probe reassessment](OBSERVATIONS.md#probe-reassessment) for the
  failed inside-transaction snapshot and the required faithful-boundary follow-up.
- Keep `Answer` / `RecallLog` authoritative for the accepted choice, spelling,
  timestamp, timing and mark/outcome. Reuse `AnsweredQuestion` for both fresh and
  recovered replies. Restore accidental-match presentation with the existing
  `WikiLinkResolver.findAllAccidentalMatches` using the persisted spelling answer
  and current authorized note context, only for a persisted ACCIDENTAL_MATCH.
  This is read-only reveal construction, never another grading or Confusion
  operation. Do not re-evaluate the accepted outcome from edited note content.
  Current note context and available actions follow existing access rules; this
  story does not freeze notebook content or match candidates historically.
- Capture one submission payload at the first user submission, including timing.
  Retry sends that payload. Pending/failure/result belongs to that prompt;
  retained spelling input and choice do not disappear on transport failure.
  Process one completion for that attempt and ignore stale completions after
  navigation. Use the existing answer continuation in `useRecallAnswerHandling`:
  OVERLAP starts a new prompt on the same tracker; normal results advance once.
- PFE: `RecallPromptService` and `SpellingRecallGrading` own answer acceptance;
  `MemoryTrackerService` owns learning effects. `Answer.getCorrect` already
  reconstructs historical correctness from outcome/logs. Generic
  `AnsweredQuestion.from` does not reconstruct matched-note context; extend the
  answer response responsibility rather than teaching DTO projection to grade.
  `ConversationSubject`, memory-tracker history, and previous-answer listing
  also consume answer projections; preserve their current observable purposes.
  Searches across CLI and MCP found no callers of the two prompt answer routes;
  their distinct recall actions do not require new recovery UI.
- Relevant Accepted ADRs: [0003, scheduling policy](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md)
  fixes Grade/Confusion/Overlap and timing semantics;
  [0006, failure handling](../../../docs/adrs/0006-failure-handling-accepted.md)
  permits this explicitly required recovery outcome;
  [0007, environments](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
  confines probes to disposable worktree databases. The ADR index and relevant
  record statuses agree. The North Star's server-side learning-data ownership
  is preserved; no new architectural direction or ADR exception is needed.

## Preparation observations and decisive premises

See [the dated preparation observations](OBSERVATIONS.md) for the baseline
setups, literal results and limits consumed by this plan.

## Outside-in proof ownership

Use real mounted RecallPage tests for input, result and queue behavior and real
controller/database tests for accepted results and effects. Mock only the backend
HTTP boundary in frontend tests. A mocked accepted response proves presentation,
not database idempotence; the paired backend tests own that promise. No new
full-stack harness is required for this bounded change.

| Final promise | Owner | Required observation |
| --- | --- | --- |
| First MCQ answer and effects survive lost-response recovery, changed retry input and repeated retries | 2 | Submit, commit, reload in another persistence context, repeat the controller/HTTP request with same and different body; compare answer identity/content, original createdAt/timing and exact log/scheduling state. Include correct and wrong marks and a property tracker. |
| A slow original request overlapping deliberate recovery has one acceptance | 1 probe, 2 retained proof; 4 spelling reuse | Independent transactions with deterministic barriers around the same prompt: one accepted answer, both callers obtain that answer, one Grade/log transition. Also test rollback of the first transaction followed by successful acceptance. Do not rely on sequential direct calls or one outer rollback transaction for this claim. |
| MCQ failure retains choice/prompt/position and deliberate Retry completes once | 3 | Drive the mounted page's choice and Retry controls; API error then success; repeated error remains recoverable; original request timing/body retained, ordinary correct/wrong reveal and queue/history observed. Resolve a delayed response after navigation and observe the current attempt remains correct. |
| Spelling recovery preserves ordinary and special outcomes and effects | 4 | Controller submission followed by fresh-context recovery for correct, wrong, OVERLAP, unique and multiple ACCIDENTAL_MATCH; original spelling/timing/outcome, readable match reveal data, original Grade/Confusion logs and both relevant trackers' state remain authoritative. Change retry text and repeat. |
| Spelling input, session position and special continuations recover | 5 | Actual typing and form submission on mounted RecallPage; fail then Retry, repeated failure, accepted-response-loss shaped reply; original body/timing retained. Observe accidental-match reveal/actions and overlap feedback followed by a new specific-answer attempt on the same tracker. Ordinary result advances/history once. |
| No automatic uncertain resubmission; ownership and normal grading remain enforced | 2–5 | Frontend request count after failure stays at one until Retry; pending Retry cannot duplicate a call. Backend unauthenticated/non-owner cases refuse recovery and preserve state; ordinary grading regressions remain green. |

Changed-contract consumers to inspect and run: backend `RecallPromptAnswerControllerTest`,
`RecallPromptAnswerSpellingControllerTest`, accidental-match/overlap tests,
`MemoryTrackerRecallPromptHistoryCorrectnessControllerTest`, conversation subject
projections and previous-answer retrieval; frontend `RecallPromptComponent`,
`QuestionDisplay`, `RecallPromptCard`, `SpellingQuestionDisplay`, RecallPage
answering/spelling/queue-progress tests and AnsweredSpellingQuestion reveal/action
specs. Existing tests that emit an `answered` event directly do not prove the
submission/recovery journey: new page cases must use actual controls.

## Ordered slices

### 1. Establish fresh-state serialization for one recall attempt
Type: Structure
Status: done
Proof: An isolated real-MySQL probe with two independent transactions observes
fresh answer state after lock acquisition, including the entity already being
loaded before the lock and the first transaction rolling back.

Internal change: Establish the smallest locking/refresh technique usable by the
existing controller/service transaction. This is a bounded state-changing probe
for slice 2, not a new product abstraction. Use disposable worktree test data and
explicit barriers, not sleeps. Record the literal command and observation in
this plan, then remove scratch instrumentation. If the existing lock path sees
stale state or cannot be demonstrated within the bound, revise the acceptance
boundary and reassess dependent slices before implementation. Do not treat an
unproved locking assumption as a passing result.

Sizing: about 5 minutes of probe work plus the required full-backend test wait.
The probe is permitted early because the consuming operation requires isolated
state-changing concurrent transactions. Its failure stops dependent slices.

### 2. Recover the first accepted multiple-choice answer
Type: Behavior
Status: done
Proof: Backend controller/HTTP cases in the proof table, including retained
concurrent and rollback discrimination from slice 1; all backend tests.

Behavior: An owned prompt with an accepted MCQ answer → repeated submission →
the same original AnsweredQuestion and learning evidence. An unanswered prompt
→ deliberate submission → one ordinary answer. Reuse the proven transaction
boundary, authorize before exposing the result, and read the actual persisted
answer rather than the incoming body. Preserve ordinary validation for an
unanswered prompt. Changed-content recovery does not grant answer revision.

Sizing: roughly 5–8 minutes of focused implementation/test authoring and cleanup;
full-backend test runtime is an explicit exception to the ten-minute wall-clock
limit. Interim: API recovery works; the existing web failure interaction is
replaced in slice 3. This is a safe stopping point for the API contract.

### 3. Recover a multiple-choice submission from the current recall page
Type: Behavior
Status: done
Proof: Mounted RecallPage MCQ control-to-result cases from the proof table;
frontend suite and both typechecks.

Behavior: Choice submitted → response fails → same prompt, retained choice and
position with accurate inline feedback and explicit Retry. Retry uses the first
payload and obtains the accepted result or accepts it now. Handle it once through
the normal result/queue path. A further error preserves recovery; pending actions
are disabled and a stale completion cannot move a different current attempt.
Replace the misleading generic deleted/skipped-tracker alert in this path.

Sizing: roughly 5–8 minutes plus frontend verification. One MCQ recovery owner,
one mounted-page proof loop; no generic retry framework. Read the old alert's
callers once at acceptance and remove obsolete message-only fixtures/assertions;
record that sweep as a reading, not a permanent absence test.

### 4. Recover the accepted spelling outcome and its continuation data
Type: Behavior
Status: planned
Proof: Fresh-context spelling controller cases in the proof table, including
repeated unique-match Confusion and multiple-match reveal data; all backend tests.

Behavior: Accepted spelling answer → deliberate repeated submission → original
answer/outcome/timing with the existing reveal or overlap continuation data, and
unchanged original learning effects. Reuse slice 2's acceptance rule; extend the
existing spelling owner and read-only result construction. Obtain matched-note
context through the existing resolver for the persisted spelling, without invoking
grading again. Preserve current access filtering and ordinary first-submission
matching rules. Reuse the independent-transaction proof technique for spelling.

Sizing: roughly 5–8 minutes plus full-backend verification. No schema change is
planned: historical match-candidate snapshots are outside the story. If code
inspection or proof shows recovery needs one to satisfy an included example,
stop and replan, applying the migration skill's schema-before-code release rule.
Interim: spelling API is recoverable; its disabled web form is handled next.

### 5. Recover spelling submissions through the existing recall continuations
Type: Behavior
Status: planned
Proof: Mounted RecallPage spelling input-to-result cases in the proof table;
frontend suite and both typechecks. Read the complete mapping at acceptance.

Behavior: Typed answer submitted → failed response → same input, stem, timing
snapshot and queue position with explicit Retry. Deliberate Retry completes the
same attempt once. Ordinary results advance once; an accepted overlap follows
its existing same-tracker, new-prompt retry; accidental matches display the
existing reveal and permitted resolution actions. Subsequent transport failures
remain recoverable without reloading. Exercise delayed response/navigation and
pending double-submit at the page boundary.

Sizing: roughly 5–8 minutes plus frontend verification. Reuse the MCQ recovery
concept where responsibilities coincide; spelling owns input and the existing
parent/child completion contract must make failure actionable. Do not duplicate
queue advancement or introduce separate spelling scheduling state.

## Verification and delivery

Run tooling under Nix in the executing checkout. These are the applicable local
gates from AGENTS.md and the backend/frontend skills, not a mandate to run every
hosted CI job locally:

- Backend-changing slices: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
  (all backend unit tests as required by the backend skill).
- Frontend-changing slices: `CURSOR_DEV=true nix develop -c pnpm frontend:test`,
  `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`, and
  `CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit`.
  Focused files are allowed during iteration; final proof covers affected
  consumers in the full frontend suite.
- Keep the existing request/reply types where sufficient. If signatures or DTO
  wire types change, use the generate-api-client skill and regenerate with
  `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`; never hand-edit output.
- Every execution slice uses AGENTS.md's required Jidoka → fresh
  dough-post-change-refactor agent → API generation if needed → coordinator
  `./scripts/run.sh pnpm format:changed` once → plan update → commit with its
  check-only lint hook → push. Execution owns CI observation and later wrap-up.
- Target roughly five minutes per leaf including focused tests; scrutinize
  overruns. More than ten minutes requires finer decomposition unless time is
  the stated full-suite wait that decomposition cannot remove. Record actual
  elapsed time and the disproved assumption before continuing.

## Plan assessment

Slice-plan refinement was not needed: the sequence has one early bounded probe
and four cohesive recovery outcomes with explicit proof owners. Both question
kinds share one first-accepted-answer rule, while their existing reveal and
input responsibilities remain distinct. No remaining slice-specific concern is
identified outside the bounded fresh-state serialization probe in slice 1.
The probe's result must precede dependent implementation; it is not claimed as
observed during preparation. No person-owned product decision is pending.

## Accepted execution evidence

[Execution evidence](EXECUTION.md) retains slice proof, source acceptance and learnings.

## Story obligations

### G1. MCQ recovery needs its deliberate web action
Reported: slice 2 — "Coordinator inspected the existing RecallPromptComponent error path; the backend's new recovery result still needs the planned retained-input Retry journey on the current page."
Story clause: "Explain that submission could not be confirmed and offer a deliberate recovery action."
Disposition: proved by slice 3: frontend/tests/pages/RecallPage.answerRecovery.spec.ts drives choice and Retry controls through repeated failures, retained submission, correct/wrong reveal/history and once-only progress; pending/navigation cases preserve the current attempt.
