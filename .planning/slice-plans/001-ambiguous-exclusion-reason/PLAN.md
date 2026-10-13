# Require a reason for an ambiguity exclusion

**Identity:** SEED-073#story-8
**Source:** [Require a reason when a learner excludes an ambiguous attempt](../../seeds/SEED-073-learning-refinement-and-retrieval.md#story-8)

## Goal and scope

Every new learner-confirmed ambiguity exclusion retains a nonblank reason.
Refuse missing, null, empty, and whitespace-only reasons before changing the
attempt or its schedule. Help must leave a reasonless proposal declineable but
not confirmable. A repeated confirmation returns the first decision regardless
of the newly supplied reason.

Preserve existing valid exclusion, original evidence, schedule reconstruction,
and displayed reason through the existing flow. Commissioned learning sessions
are outside this change. Correct-answer eligibility, shared-conversation
policy, calendar/streak policy, formal contests, undo, retroactive reason
repair, and a learner reason editor are not delivery commitments. The seed's
deferred owner decisions do not block this bounded rule.

## Existing responsibility and decisions

PFE: change the existing exclusion responsibility, rather than introduce a
second decision or storage model.

- `Answer.excludeAsAmbiguous` owns the first-decision check and records its
  reason/time. Validate a new reason there after that check and before either
  field changes. Unconditional request/DTO validation would incorrectly reject
  a retry whose reason is blank.
- `RecallPromptController.excludeAsAmbiguous` retains authorization;
  `MemoryTrackerService.excludeAsAmbiguous` saves and rebuilds only when the
  Answer records a new decision. Reuse `QuestionAnswerException` for this
  explicit business refusal. No schema or wire-shape change is needed.
- `ToolCallHandler.vue`, reached through `AiResponse`, owns proposal controls.
  Derive confirmability from the exclusion reason and retain the existing
  Decline path and valid-reason API payload. Reuse `AcceptRejectButtons`'s
  existing hidden-accept capability, keeping the display tolerant of a missing
  reason.
- Product-wide consumer search found the controller's exclusion/schedule tests,
  recall statistics and history tests, conversation proposal tests, the shared
  AnsweredQuestion fixture, mounted recall/help/history displays, and
  `e2e_test/features/recall/ambiguous_question_exclusion.feature`. The web
  handler is the production API caller; CLI and MCP have no separate exclusion
  flow. Existing fixtures and E2E supply nonblank reasons.

[ADR 0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
keeps this private learning record in MySQL, outside Portable content.
[ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md)
governs the existing exclusion replay; this change adds no Grade or scheduling
policy.
[ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md) supports the
explicit business refusal. The ADR index and these records agree on Accepted
status; no exception or new ADR is needed. The current
[North Star](../../NORTH-STAR.md) requires no new direction for this local rule.

## Preparation observations

Observed on 2026-10-14 in
`/Users/terryyin/git/doughnut/.worktrees/require-a-reason-when-a-learner-excludes-an-ambi`,
at `2587fb27cb95699d70ed053af1aacdfa32095194`, using disposable edits.
All scratch production and test changes were reverted before recording this plan.
These observations settle the approach; they are not retained implementation.

| Decisive premise | Consuming operation and observation | Result |
| --- | --- | --- |
| A new blank exclusion currently succeeds; the existing controller can prove refusal without a new test boundary | Added a controller probe to `RecallPromptExcludeAsAmbiguousControllerTest`: answer incorrectly, snapshot persisted schedule/history, call exclusion with spaces, require business refusal and unchanged persisted state. Ran `CURSOR_DEV=true nix develop -c pnpm backend:test_only`. | 2,809 tests, exactly the new refusal probe failed because no exception was thrown; two skipped. The same class's added missing-reason retry probe passed. |
| Validation after the first-decision check preserves retry and scheduling semantics | Scratch guard in `Answer.excludeAsAmbiguous`; expanded controller probes across null, empty, spaces, and tab/newline reasons, plus null/empty/spaces retries. Reran the same full backend command. | 2,814 tests, zero failures/errors, two skipped; test build 55s. Refused new decisions retained persisted attempt/schedule/history; retries retained original reason and schedule/history. Existing valid exclusion/replay/authorization/statistics tests passed. |
| A malformed proposal reaches the existing help controls, which can refuse confirmation while retaining decline | Scratch parameterized cases in `AiResponse.exclusionProposal.spec.ts` stream a proposal with undefined, empty, or spaces reason. Run `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/conversation/AiResponse.exclusionProposal.spec.ts`. | All three new cases failed because Confirm was rendered; the three existing cases passed. |
| One derived control condition fixes malformed proposals without breaking consumers | Scratch hidden-accept condition for an exclusion with no trimmed reason; run `CURSOR_DEV=true nix develop -c pnpm frontend:test`. | 350 files / 2,191 tests passed in 49.91s, including missing/blank proposal refusal and successful Decline, valid confirmation, and recall/help/history consumers. |
| The proposed frontend expression fits the generated API types | `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit` and `CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit` on that scratch content. | Both exited zero. |

The frontend probes mock the HTTP/streaming boundary; backend probes use the
real controller, service, entities, and isolated test database. No real-model
quality or manual browser observation is claimed, and execution does not depend
on either. Raw command output stayed outside the repository.

## Outside-in proof ownership

| Promise / example | Owning slice | Observable proof |
| --- | --- | --- |
| New missing/blank reason is refused before exclusion or scheduling changes | 1 | Parameterized controller test with null, empty, spaces, tab/newline; inspect persisted exclusion fields and schedule/history before/after the business refusal. |
| A retry returns the first reason/time and leaves schedule/history unchanged, even with a missing or different reason | 1 | Extend existing repeat controller examples with null/empty/whitespace inputs; retain the different-reason example. |
| Valid confirmation retains the reason and existing evidence/scheduling behavior | 1 and 2 at their respective boundaries | Existing controller exclusion/replay tests; existing mounted valid-proposal confirmation asserts the reason-bearing API request, and recall/help/history tests observe displayed results. |
| A missing/blank proposal cannot be confirmed but can be declined | 2 | Mounted `AiResponse` streams each malformed proposal, observes unavailable Confirm and usable Decline, dismisses it, and checks the exclusion API was not invoked. This is proof of an actively enforced refusal. |
| Existing ownership, readouts, commissioned grading and other proposal kinds retain their behavior | 1 / 2 | Full backend and frontend suites cover consumers identified above; do not add absence tests for out-of-scope behavior. |

## Execution context

Established 2026-10-14: SEED-073#story-8, publisher
`dashboard-territory.local-doughnut`, agent Nao-chan. Story Branch Mode uses
`/Users/terryyin/git/doughnut/.worktrees/require-a-reason-when-a-learner-excludes-an-ambi`
and branch `codex/require-a-reason-when-a-learner-excludes-an-ambi`; this is the
owned reused execution checkout, with no integration checkout supplied.
Remote `origin`, eventual trunk `main`; increments publish to the remote
execution branch. Published claim/base: `171cdbab862392ee90028f25f85de91424c17426`;
starting revision: `e62fd69628118b96ad8077ad055d437006a2d63f`.
The project worktree setup succeeded, followed by
`CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit` (exit zero).
Existing planning authority allows refinement if an attempt overruns; retain
the slices' ten-minute limit and required-verification-wait exception.

CI: GitHub Actions `nerds-odd-e/doughnut`, verified selector `ci.yml`, display
name `donut CI`, push-triggered on the execution branch. Codex observer:
coordinator `nao-story8-2fd471d5`, stream directory
`/tmp/dough-ci-501/watch-mDzYOr`, PID 19779, session 5328, yielded cell 13,
bound to this checkout and the remote execution branch. The trunk claim's CI
is unobserved; this observer owns subsequent branch deliveries.

Accepted branch delivery: slice 1 `772eb5c68e8175056fb2e96110a02363ee3c98ce`,
suffix base `171cdbab862392ee90028f25f85de91424c17426`; no reconciliation,
observer reused and revision registered. This is the next increment's base.

## Ordered slices

### 1. Refuse a new exclusion without its reason
Type: Behavior
Status: done
Proof: Controller refusal and retry examples above; full
`CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

Behavior: Given an owned answered attempt, when exclusion is requested without
a nonblank reason, return the existing business error and preserve persisted
answer, tracker, and recall history. Given a prior exclusion, return that first
decision even when the new reason is missing or blank.

Extend the existing Answer decision rule and controller tests. Keep the valid
reason and retry rules together so validation cannot defeat idempotence. Update
`docs/recall-help.md` to state the new-decision reason requirement and retry
exception. Use existing fixtures; do not introduce migrations, schema
constraints, or a DTO validation rule that runs before the existing decision.

Sizing: approximately 5–7 minutes including the full backend suite and cleanup.
Scrutinized against the five-minute target: this is one atomic refusal rule and
its repeat boundary, not separable outcomes. The observed backend test build is
55s; cold provisioning can add time. At ten minutes, stop and replan unless the
remaining time is solely an already-running required verification wait.

Safe stopping point: the API prevents unexplained new decisions. Until slice 2,
a malformed proposal can still display Confirm and receive the existing API
error; it changes no learning state. Slice 2 replaces that interim interaction.

Accepted proof (2026-10-14):
`CURSOR_DEV=true nix develop -c pnpm backend:test_only` exited zero: 2,814
tests, zero failures/errors, two skipped; all 15 exclusion-controller cases
passed. Inspected `RecallPromptExcludeAsAmbiguousControllerTest.setup` answers
an owned MCQ through the real controller. `refusesANewExclusionWithoutAReason`
observes the business exception, refreshed empty exclusion fields, and identical
persisted tracker/log snapshots for null, empty, spaces, and tab/newline.
`repeatingReturnsTheFirstDecisionAndPreservesScheduleAndHistory` confirms once,
advances time, retries with each malformed/different reason, and observes the
first reason/time plus unchanged persisted schedule/history. Existing valid
decision, replay, original-mark, and authorization examples remained green.
The guard in `Answer.excludeAsAmbiguous` follows the first-decision check and
precedes both writes; the service saves/rebuilds only for a new decision.
Full-suite consumers matched the preparation survey. Implementation took about
five minutes. Fresh refactor returned `none — already clean`, no proof changes
or reruns; whitespace check passed. Selective formatting succeeded once. No API
generation trigger changed.

### 2. Keep reasonless help proposals declineable
Type: Behavior
Status: done
Proof: Mounted malformed/valid proposal cases and full
`CURSOR_DEV=true nix develop -c pnpm frontend:test`, plus
`CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit` and
`CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit`.

Behavior: Given a tutor exclusion proposal with a missing, empty, or
whitespace-only reason, help offers Decline but no confirmation. Declining
dismisses it without an exclusion request. A nonblank reason retains the
ordinary Confirm flow and displayed result.

Use derived state in the existing handler and parameterized mounted
`AiResponse.exclusionProposal.spec.ts` examples, without a new generic proposal
framework. Preserve the other suggestion kinds. Finish the proposal-control
description in `docs/recall-help.md`.

Sizing: approximately 5 minutes including the observed ~50s full frontend
suite, typechecks, and cleanup. One control rule plus its decline continuation;
no Structure slice is justified. Apply the same ten-minute stop rule.

Safe stopping point: the entire story is available through the existing help
and exclusion flow, with no dependency on the deferred product decisions.

Accepted proof (2026-10-14):
`CURSOR_DEV=true nix develop -c pnpm frontend:test` exited zero: 350 files /
2,192 tests passed in 45.34s. Both
`CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit` and
`CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit` exited zero
against the same final frontend content. Inspected mounted
`AiResponse.exclusionProposal.spec.ts`: `beforeEach` supplies the answered
conversation and external SDK/event-source mocks; `showProposal` streams split
JSON through the real response handling. Four malformed cases assert no
Confirm, click real Decline, observe dismissal and no exclusion request.
Valid cases observe rendered reason and the unchanged reason-bearing request.
Inspected confirmation consumers in `ConversationComponent.exclusion.spec.ts`,
`RecallPage.privateHelp.spec.ts`, `MemoryTrackerPage.privateHelp.spec.ts`, and
`MemoryTrackerPageView.history.spec.ts`; full-suite results cover them and the
other proposal kinds. Fresh refactor found no edits, so accepted proof stayed
valid; whitespace passed. Formatting succeeded once with no fixes. G1 is
resolved by these mounted observations. No API generation trigger changed.

## Verification and delivery requirements

Backend's all-tests command is required by the backend skill. Frontend's full
suite covers shared proposal controls and displayed answer consumers;
both typechecks are required by the frontend skill. These high-level boundaries
own this bounded change; no new E2E or manual-testing commitment is added.
Regenerate API artifacts only if execution actually changes a controller
signature or API data type; the observed approach does neither.

For each executed slice, follow the repository's required delivery sequence:
Jidoka, fresh dough-post-change-refactor agent, API generation if needed,
coordinator runs `./scripts/run.sh pnpm format:changed` once, update this plan,
commit with the check-only hook, and push under dough-execute-plan. No execution
is authorized by preparation. Retain this plan for retrospective and story
wrap-up after execution.

## Plan assessment

Slice-plan refinement was not needed: both leaves have one evaluable outcome,
mapped high-level proof, and observed implementation/test routes. The cumulative
design is one first-decision rule with matching help controls; there is no new
representation, speculative structure, or unresolved slice-specific concern.
The backend-first interim interaction is explicit and has slice 2 as its owner.

## Story obligations

### G1. Reasonless proposal controls
Reported: slice 1 — "Malformed Help proposals can display Confirm until slice 2, while the backend safely rejects them."
Story clause: "A tutor proposal that carries no reason cannot be confirmed in help; the learner can still decline it."
Disposition: proved by slice 2: `AiResponse.exclusionProposal.spec.ts` — `offers only decline for a proposal with reason %j` streams each malformed proposal, observes no Confirm, clicks Decline, observes dismissal and no exclusion API request; full frontend suite passed.

### G2. Observation boundaries
Reported: slice 1 — "Direct controller tests exercise null reasons; no HTTP-binding or manual-browser proof is claimed."
Story clause: "A learner who excludes an attempt as an ambiguous question always keeps the reason for that decision."
Disposition: no user cost "A learner who excludes an attempt as an ambiguous question always keeps the reason for that decision.": the real controller/service/entity/database boundary proves the rule; the request shape and binding remain unchanged, and mounted frontend proof belongs to slice 2. No manual or separate HTTP-binding observation is a promised proof item.

### G3. Tutor-quality observation
Reported: slice 2 — "No manual-browser or real-model observation claimed."
Story clause: "A learner who excludes an attempt as an ambiguous question always keeps the reason for that decision."
Disposition: no user cost "A learner who excludes an attempt as an ambiguous question always keeps the reason for that decision.": unchanged tutor quality is outside this rule; malformed and valid proposals exercise the real mounted handling with the external stream mocked, alongside real backend decision proof.
