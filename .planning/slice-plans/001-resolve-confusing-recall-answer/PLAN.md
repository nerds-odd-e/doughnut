# Resolve a confusing recall answer privately and check understanding

**Identity:** SEED-073#story-1

**Source:** [refined story](../../seeds/SEED-073-learning-refinement-and-retrieval.md#story-1)

## Goal and scope

A learner can open help from an answered MCQ, discuss the actual reasoning
privately, try an optional related question, and continue recall or revisit
that exchange. Preserve the attempt, memory tracker, and original Grade.

The owner accepted on 2026-10-12:

- learner-private help, with deliberate sharing reserved for later author
  correction work;
- explanation and conversational practice together; and
- separate reason-aware contest and grade repair, recorded as unqueued
  SEED-073#story-6 and #story-7. An invalid attempt's neutral exclusion is the
  accepted direction for that later policy, not a scheduling change here.

This plan includes usable entry, same-attempt continuation, private access,
reasoning-aware explanation, ungraded conversational practice, and contextual
exit to existing authorized refinement. It adds no mastery tracker, diagnostic
taxonomy, new chat system, sharing mechanism, or grade-repair policy. Story 2
owns repairing recall-chat correction acceptance and durable proposal decisions.
Existing shared discussions keep their audience. No story is taken and no
implementation is authorized by this preparation.

## Architectural approach and existing solutions

One conversation owns the exchange; its subject identifies the attempt and
its access ownership identifies its audience. Existing `Ownership` already
supports a person's personal ownership independently of a notebook. Clarify
the Java/API concept currently called `subjectOwnership`, retain its physical
FK mapping and stored values, and assign new help to the initiating learner's
personal ownership. There is no schema migration or separate help-mode flag.

Authorize help creation through the prompt's MemoryTracker owner, not merely
note readability. Continue a conversation for the same prompt and initiator
only when its access ownership is that initiator's personal ownership. An old
own-note conversation with that audience is compatible; an old shared thread
remains shared. Existing authorization, listing, unread and message owners
then enforce one audience rule. Repeated entry uses that same thread; this is
not a universal one-conversation-per-note rule.

Use the existing Modal and conversation content for contextual help at the
answered attempt. Closing returns to the underlying recall/history view,
which avoids introducing a second navigation/resume mechanism. Keep the
Message Center as an additional later-reading surface. Reuse the question
display and ordinary composer; learner reasoning is optional, with an explicit
explanation shortcut. A practice request, response, and feedback are ordinary
messages. No regenerated RecallPrompt or answer submission represents practice.

The existing attempt serialization already carries choices, selected answer,
marked result, tested focus, and rationale. The common focus-context service
provides current accessible notes. Preserve that distinction between the
original attempt and present source context. Guidance addresses learner
reasoning, allows disagreement and uncertainty, and offers one related check
without exposing its answer before the learner responds.

Refinement context can extend the existing question-context contract with the
selected answer and an explicit learner-chosen clarification. Do not implicitly
publish a private transcript. Existing author permissions and previews own any
content change. Retain question objections as discussion. The formal post-answer
contest action, its verdict and actual consequences belong to story 6; grade
repair belongs to story 7. Neither is smuggled into conversational agreement.

Applicable Accepted decisions:

- [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md): distinct MCQ,
  prompt, Answer, Grade, and Conversation concepts.
- [ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md):
  Grades are scheduling events; explanation/practice keeps the original Grade,
  schedule and tracker. No compensating Grade.
- [ADR 0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
  and [NORTH-STAR: one accepted-change boundary](../../NORTH-STAR.md#one-accepted-change-boundary):
  private learning evidence stays server-side; reviewed note changes use the
  existing publication owner.
- [ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md): use named routes
  for the retained Message Center/navigation integration.
- [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md): failures
  remain visible. An interrupted reply is not a successful check. Reuse current
  request/error handling; add recovery only for a promised business outcome.

No new North Star topic or ADR is warranted for this bounded reuse. Future
grading correction must settle its own ADR 0003 policy and historical reach.

## Outside-in proof ownership

| Promise from the source | Owner | Observable proof |
| --- | --- | --- |
| Explanation addresses the chosen distractor and supplied reasoning; learner can correct a tentative interpretation | 1, 6 | Real model transcript rubric in the early probe; controller captures the consumed AI request; mounted conversation sends reasoning and disagreement and shows returned replies |
| Private help is for the attempt owner; notebook owner/circle membership does not grant it | 3 | Controller creation, direct reads/messages/send/AI/export and inbox/unread observations for learner, notebook owner and another member; retained legacy shared access proves compatibility |
| Same-attempt continuation and retained exchange | 4, 7 | Repeated start returns the compatible thread; shared older thread retains its audience; DB reload returns question, practice answer and feedback |
| Clear entry from answer/history, original context, optional reasoning, easy return to unchanged recall progress | 5 | Mounted real recall/history parent with backend HTTP mocked opens help for the selected attempt; two history items have distinct controls; closing restores the same answer/session and focus |
| Related practice waits for response, then gives feedback; original Grade and schedule remain authoritative | 1, 7 | Real transcript rubric plus controller message loop with actual pre/post tracker and RecallLog; mounted explicit check action, response, feedback and close/reopen journey |
| A note problem can reach existing refinement with relevant context under author permission | 8 | Mounted handoff supplies selected answer and reviewed clarification; AiController request capture observes those values at the refinement model boundary; existing ordinary refinement remains available |
| Suspected question ambiguity is examined and retained without presenting conversation as formal adjudication | 1, 6 | Ambiguity rubric and persisted reasoning/disagreement exchange; formal post-answer action is owned by story 6 |
| Evidence stays private/server-side, and source uncertainty is not portrayed as learner failure | 3, 6, 8 | Access refusal and explicit refinement payload boundaries; request contract and real model ambiguity case; no new Portable-content store or auto-write path |

These are surviving behavior and active authorization-refusal proofs. No removal
or permanent absence-check project is part of this plan. Existing tests are
extended at their real boundaries, not copied into tests of internal helpers.

## Decisive premises and preparation observations

Observed on 2026-10-12 in
`/Users/terryyin/git/doughnut/.worktrees/resolve-a-confusing-recall-answer-with-an-explan`
at `e1bbd1d2727fced534f473bcba709407a46d91c9`. Disposable probes and mutations
were removed before preparation recording. These are preparation observations,
not proof that the planned feature already exists. Execution follows its actual
checkout revision and reruns proof affected by changed consumers.

### Backend context, persistence, and privacy gap

Literal command for both the scratch-probe and restored baseline:

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only
```

The disposable controller probe graded an MCQ, started its conversation, added
learner reasoning, invoked the real AI request-building path, and captured the
mocked external OpenAI `createStreaming` request. Selected choice, incorrect
result, tested focus and reasoning reached that consuming boundary. Streaming a
mocked assistant reply then flushing/clearing persistence and reloading through
the controller retained the reasoning and reply. This settles existing context
and ordinary-message storage, not model quality.

A second probe created the learner's conversation on another owner's note,
switched to that owner, and expected the conversation read to be refused. It
failed because the read was allowed, reproducing the privacy gap that slice 3
must close. Probe run: 2739 tests, one expected privacy failure, two skipped.
Clean suite after probe removal: 2737 tests, zero failures/errors, two skipped;
1m 57s. Isolated database:
`doughnut_wt_7faea69a304644ac80a454f4a594eabb_test`.

Inspected owners and observation locations: `ConversationMessageController`,
`ConversationAiReplyControllerTest`, `ConversationStartControllerTest`,
`ConversationAiRequestBuilder`, `Conversation`, and the JUnit XML under
`backend/build/test-results/test/`. Scratch observations were controller tests;
temporary copies are not required inputs to execution.

### Existing refinement consumer

A further full backend run passed with 2738 tests, zero failures/errors,
two skipped in 51s. Its additional contest probe and limitations are retained
with their owning [story 6](../../seeds/SEED-073-learning-refinement-and-retrieval.md#story-6).
That run also passed
`AiControllerNoteRefinementTest.GenerateRefinementSuggestions.shouldAppendQuestionLedGuidanceWhenQuestionContextProvided`.
Its capture of the consumed mocked Responses request observes stem, choices,
correct answer and tested focus. This establishes the existing question-context
route. Selected answer and reviewed clarification are new behavior in slice 8.
Neither observation establishes quality of the external model's judgment.

### Frontend routes and proof discrimination

```sh
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/toolbars/MainMenu.resume.spec.ts tests/pages/RecallPage.dueQueue.spec.ts tests/pages/RecallPage.answering.spec.ts tests/components/conversation/ConversationInner.spec.ts tests/pages/MessageCenterPage.subject.spec.ts
```

Headless Chromium baseline: five files / 36 tests passed (23.86s), and five /
36 passed after restoration (3.82s). First use installed pinned browser assets.
Existing preset submission, streamed reply rendering, and answered-review resume
state were exercised with the HTTP backend mocked.

Discrimination command:

```sh
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/toolbars/MainMenu.resume.spec.ts tests/pages/RecallPage.dueQueue.spec.ts tests/pages/MessageCenterPage.subject.spec.ts
```

Temporary removal of recall-subject rendering, reset of the catch-up queue
index, and removal of Resume's route push produced three expected failures
(16 other tests passed, 5.28s): the question stem disappeared, the queue lost
its preserved repeated tracker, and Resume remained on `root`. All mutations
were restored. This settles those reused component contracts and the local
runner; it does not prove a full Message Center round trip. The queue test owns
its KeepAlive wrapper. Slice 5 instead proves the new contextual open/close
journey from its real parent, and slice 7 proves the joined learning loop.

### Premises bounded by the early probe

Real generated explanation and follow-up quality is unobserved. The controller
observation above reaches the request path with an external-model mock; it does
not establish the held-app route. Its safe checkout-selection prefix was run:

```sh
CURSOR_DEV=true nix develop -c node --input-type=module - <<'JS'
import { resolveInvocationCheckout } from './scripts/e2e-invocation-selection.mjs';
import { worktreeIsolationApplies } from './scripts/browser-worktree-isolation.mjs';
const result = resolveInvocationCheckout({ checkoutRoot: process.cwd(), isIsolatedCheckoutFn: worktreeIsolationApplies });
console.log(JSON.stringify(result));
if (!result.isolated) process.exitCode = 1;
JS
```

Result: exit 0, `{"isolated":true}`. This is the actual selection helper consumed
by `runE2eHold`, stopped before stateful SUT startup and testability reset. It
proves checkout isolation selection only. App startup, sign-in, fixture creation
and the paid response are unobserved steps owned by slice 1's executor; the
developer owns per-run paid-call authorization. They remain part of the early
probe rather than a claim of working route reachability.
Only an authorized real-service observation can settle the quality premise.
Slice 1 owns that permitted early probe and stops dependent implementation if
its rubric fails or the real-service route is unavailable. Mocked responses
never substitute for its conclusion. The illustrative cases are sufficient for
a bounded quality check, not an efficacy or representative-user claim.

## Ordered slices

All slices are planned. Target about five minutes of implementation, focused
proof and cleanup. Estimates above five minutes are scrutinized below; ten
minutes is the hard decomposition threshold except a stated verification wait.
The measured full backend suite is roughly two minutes; a required suite or
real-service wait is not reduced by splitting a cohesive audience or transcript
contract. Time code work and waits separately, and reassess an actual overrun
instead of silently extending it.

### 1. Validate a focused explanation and practice exchange

Type: Behavior

Status: planned

Sizing: target five minutes of fixture/setup and rubric work, ten-minute hard
limit for active work; only external-service/owner-held waits are excepted.

Behavior: Given the proposed recall-help instructions and an answered MCQ with
learner reasoning, an authorized real-model exchange explains the distinction,
accepts correction of its interpretation, and offers a related ungraded check.

Proof: Use two fixed illustrative fixtures through the existing conversation
AI path: the story's correlation-versus-causation distractor, and an ambiguous
"Which number is even?" MCQ with both 2 and 4 offered. For the first, the reply
must address why co-movement alone does not establish causation; the check
changes the example, withholds its answer until a learner response, and gives
feedback on that response. For the second, the reply must acknowledge the
defensible objection rather than insist that the learner forgot. Keep at most
eight AI replies across these cases; record model, input, prompt, transcript,
revision, rubric outcomes and limitations in this plan.

The documented isolated real-service route is
`CURSOR_DEV=true nix develop -c pnpm e2e:hold --paid-openai`, followed by the
existing conversation flow. This retains that command's explicit per-run owner
authorization requirement; preparation has not granted paid-call authority.
Execution may use an already authorized equivalent real-service route only
after recording the same inputs and consuming outcome. Use the manual-testing
skill for this bounded observation. Stage the proposed help guidance only in
disposable local changes for the probe and revert it before delivery; slice 6
owns the production change. Failed or unavailable observation stops slices
2–8 and returns the concrete learning to refinement. No mock-only approval.

### 2. Name conversation access ownership coherently

Type: Structure

Status: planned

Sizing: about five minutes plus measured required verification/API generation.

Change: Express ownership of conversation access independently of its subject
through the existing Ownership association. Preserve the physical column and
all existing audience values. Update the callers, builders and generated public
contract together. This immediately enables slice 3; introduce no new visibility
resource or schema.

Proof: Existing start/message/listing/menu/AI/export controller behavior remains
green; regenerate the API if its public property changes and verify affected
frontend types. Inspect consumers of the old field and partner labels, including
generated types; use one existing access model rather than a parallel predicate.

### 3. Open help with the learner's private audience

Type: Behavior

Status: planned

Sizing: 5–8 minutes including the backend suite; one cohesive access rule.

Behavior: Given an answered prompt owned by the learner, starting help creates
a conversation under their personal access ownership. The learner can read and
reply; note authorship/circle membership alone gives no access. Existing shared
conversations retain their participants and ordinary behavior.

Proof: Controller tests create a shared-note learner fixture and exercise
creation, direct subject/messages, sending, export, AI request, inbox and unread
consumers of the common access rule. Another user's prompt cannot be used to
create a private copy of their learning evidence. Keep one canonical refusal
shape and assert only each channel's observable delta. Retained shared-thread
access is compatibility proof. Adjust participant labels to identify personal
help truthfully. These consumers belong together: no private entry ships with
a discoverable/readable alternate path.

### 4. Continue the same private attempt discussion

Type: Behavior

Status: planned

Sizing: 5–8 minutes including repeat/overlapping-request handling and the
required backend suite; reuse and creation are one identity-consistency rule.

Behavior: Starting help again for the same attempt finds the learner's compatible
private conversation and retained messages. An existing shared thread is not
misrepresented or repurposed. A different attempt has its own discussion.

Proof: Start-controller fixtures contain private and legacy shared threads;
repeated start/reload observes the same private identity and messages and retained
shared access. Observe the actual create/reuse operation, including overlapping
requests if the chosen implementation admits duplicate creation. Resolve that
at the existing transactional owner, without a second conversation registry.

### 5. Open contextual help and return to the same recall

Type: Behavior

Status: planned

Sizing: 5–8 minutes including mounted proof/typechecks; one shared entry journey.

Behavior: From an answered MCQ or a particular history item, the learner opens
clearly labelled private help with the original attempt visible. Closing it
returns focus and preserves the underlying recall/history position. Multiple
history items each target their own attempt.

Proof: Mounted real parent pages/components use real conversation content and
mock only HTTP. Observe chosen prompt ID, original question/selected answer,
audience label, distinct history controls, and preserved current answer/queue
after close. Reuse Modal and existing answer display; keep the same interaction
keyboard-operable and give it a useful accessible name. Message Center subject
rendering continues to work. A fixture that pre-supplies restored state is not
proof of restoration; drive open and close in the same mounted parent.

### 6. Explain the learner's reasoning and accept clarification

Type: Behavior

Status: planned

Sizing: about five minutes of change plus backend/frontend verification.

Behavior: The learner can request an explanation immediately or provide why a
choice seemed right. The reply addresses that reasoning and the original
attempt; the learner can correct a tentative interpretation in a further turn.

Proof: Install the guidance validated in slice 1 at the existing recall
conversation request owner. Controller request capture proves original question,
selected answer, tested focus, rationale, current note context and actual learner
message reach the external boundary. A mounted conversation exercises both the
explanation shortcut and free reasoning/disagreement through ordinary send/reply.
Persisted replies survive reload. Guidance distinguishes uncertain evidence from
learner failure. Any material prompt change from slice 1 repeats its relevant
real-service observation; keyword assertions alone prove no teaching quality.

### 7. Check the distinction without another Grade

Type: Behavior

Status: planned

Sizing: about five minutes of change plus joined-flow verification.

Behavior: After an explanation, an explicit optional check asks one changed
example about the same distinction. The learner responds before feedback,
can discuss it further or leave, and can later reopen the question/response/
feedback exchange. Their original Grade, tracker and schedule stay authoritative.

Proof: Mounted conversation/recall parent drives explanation → check request →
question → learner response → feedback → close/reopen, with scripted HTTP/model
responses only for interaction proof. Controller message-loop proof reloads
the transcript from storage and observes the same original Answer/RecallLog and
tracker state across practice. The real model rubric in slice 1 owns question
quality and answer withholding. Practice uses ordinary messages, not the recall
answer endpoint, a regenerated prompt, or a structured exercise state machine.

### 8. Carry a useful clarification into authorized refinement

Type: Behavior

Status: planned

Sizing: 5–8 minutes; one contract change across its UI and AI consumer.

Behavior: When the learner wants to refine a note they may edit, the existing
refinement receives the original selected answer and a clarification the learner
explicitly chooses to carry, along with the existing question/focus context.
Existing previews and author permissions govern the resulting content changes.

Proof: Mounted handoff observes the reviewed clarification and selected answer
in the actual refinement request; AiController-level capture reaches the
refinement model boundary with those values. Retain direct refinement with its
ordinary question context and existing permission proof. Regenerate the API
for a changed DTO. Do not automatically include the whole private transcript,
implement sharing, or repair proposal acceptance/decision storage owned by story 2.

## Verification and delivery

Follow the repository's unit-testing, backend/frontend, generated-API and E2E
skills for touched paths. No E2E feature is prescribed merely to duplicate
controller and mounted-parent proof. The early real-model observation is a
distinct quality claim and cannot be replaced by those tests.

- Backend slices: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`;
  the backend skill requires the complete backend suite, not a selected class.
- Frontend iteration: focused `pnpm frontend:test <specs>` under the same Nix
  prefix; before accepting changes, run the frontend suite and both
  `pnpm -C frontend exec vue-tsc --noEmit` and
  `pnpm -C frontend exec tsc --noEmit`, per the frontend skill. Reuse identical
  accepted typecheck evidence rather than rerunning it without a change.
- Public contract changes: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`;
  never hand-edit generated files. Added/removed Vue components require the
  frontend skill's Vite build to refresh `components.d.ts`.
- Execution delivery follows AGENTS.md: Jidoka, fresh post-change-refactor agent,
  API generation when needed, coordinator's single `./scripts/run.sh pnpm
  format:changed`, plan update, independent check-only commit hook, commit and
  authorized push. This plan supplies no execution mode or publication claim.

## Current decisions and learnings

Slice-plan refinement retained slices 1–8 and moved the formal post-answer
contest handoff and proof to story 6, following the accepted boundary. The
remaining >5-minute leaves each own one cohesive proof loop; slice 4 includes
overlapping-request work, and slice 1's active setup is bounded separately from
external waits. No additional story split or remaining slice-specific concern
was identified. Readiness is recorded on the story by the preparation recorder,
not inferred from this review or from the eight-slice count.

- Keep the core as one L-sized story because privacy, reasoning, transcript
  continuity and an ordinary practice turn form one usable loop. The eight
  leaves separate policy validation, necessary structure and cohesive externally
  observable increments; they are not eight new product stories.
- Slice 1 is a real early stop: failed quality or unavailable paid authorization
  stops dependent work. Do not spend the rest of the story building a polished
  surface around an unvalidated explanation/check protocol.
- Adding dedicated practice cards, mastery state, or author sharing changes this
  sizing premise. Return to story refinement and consider the vertical split
  "understand privately and resume" / "verify understanding with a follow-up"
  if needed; do not enlarge the plan silently.
- The later contest/repair candidates are unqueued and unplanned. Their unresolved
  verdict authority and historical reach do not hold this story's scope open.
- No application changes were made during preparation. The observed local
  backend/frontend contracts do not establish long-term learning efficacy.
