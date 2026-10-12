# Decisive premises and preparation observations

**Identity:** SEED-073#story-1

Supporting evidence for the canonical [PLAN.md](PLAN.md); it grants no execution authority or separate status.

Observed on 2026-10-12 in `/Users/terryyin/git/doughnut/.worktrees/resolve-a-confusing-recall-answer-with-an-explan` at `e1bbd1d2727fced534f473bcba709407a46d91c9`. Disposable probes and mutations were removed before preparation recording. These are preparation observations, not proof that the planned feature already exists. Execution follows its actual checkout revision and reruns proof affected by changed consumers.

### Backend context, persistence, and privacy gap

Literal command for both the scratch-probe and restored baseline:

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only
```

The disposable controller probe graded an MCQ, started its conversation, added learner reasoning, invoked the real AI request-building path, and captured the mocked external OpenAI `createStreaming` request. Selected choice, incorrect result, tested focus and reasoning reached that consuming boundary. Streaming a mocked assistant reply then flushing/clearing persistence and reloading through the controller retained the reasoning and reply. This settles existing context and ordinary-message storage, not model quality.

A second probe created the learner's conversation on another owner's note, switched to that owner, and expected the conversation read to be refused. It failed because the read was allowed, reproducing the privacy gap that original slice 3 (refined slice 5) must close. Probe run: 2739 tests, one expected privacy failure, two skipped. Clean suite after probe removal: 2737 tests, zero failures/errors, two skipped; 1m 57s. Isolated database: `doughnut_wt_7faea69a304644ac80a454f4a594eabb_test`.

Inspected owners and observation locations: `ConversationMessageController`, `ConversationAiReplyControllerTest`, `ConversationStartControllerTest`, `ConversationAiRequestBuilder`, `Conversation`, and the JUnit XML under `backend/build/test-results/test/`. Scratch observations were controller tests; temporary copies are not required inputs to execution.

### Existing refinement consumer

A further full backend run passed with 2738 tests, zero failures/errors, two skipped in 51s. Its additional contest probe and limitations are retained with their owning [story 6](../../seeds/SEED-073-learning-refinement-and-retrieval.md#story-6). That run also passed `AiControllerNoteRefinementTest.GenerateRefinementSuggestions.shouldAppendQuestionLedGuidanceWhenQuestionContextProvided`. Its capture of the consumed mocked Responses request observes stem, choices, correct answer and tested focus. This establishes the existing question-context route. Selected answer and reviewed clarification are new behavior in original slice 8 (current slice 11). Neither observation establishes quality of the external model's judgment.

### Frontend routes and proof discrimination

```sh
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/toolbars/MainMenu.resume.spec.ts tests/pages/RecallPage.dueQueue.spec.ts tests/pages/RecallPage.answering.spec.ts tests/components/conversation/ConversationInner.spec.ts tests/pages/MessageCenterPage.subject.spec.ts
```

Headless Chromium baseline: five files / 36 tests passed (23.86s), and five / 36 passed after restoration (3.82s). First use installed pinned browser assets. Existing preset submission, streamed reply rendering, and answered-review resume state were exercised with the HTTP backend mocked.

Discrimination command:

```sh
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/toolbars/MainMenu.resume.spec.ts tests/pages/RecallPage.dueQueue.spec.ts tests/pages/MessageCenterPage.subject.spec.ts
```

Temporary removal of recall-subject rendering, reset of the catch-up queue index, and removal of Resume's route push produced three expected failures (16 other tests passed, 5.28s): the question stem disappeared, the queue lost its preserved repeated tracker, and Resume remained on `root`. All mutations were restored. This settles those reused component contracts and the local runner; it does not prove a full Message Center round trip. The queue test owns its KeepAlive wrapper. Original slice 5 (current slice 8) instead proves the new contextual open/close journey from its real parent, and original slice 7 (current slice 10) proves the joined learning loop.

### Premises bounded by the early probe

Real generated explanation and follow-up quality is unobserved. The controller observation above reaches the request path with an external-model mock; it does not establish the held-app route. Its safe checkout-selection prefix was run:

```sh
CURSOR_DEV=true nix develop -c node --input-type=module - <<'JS'
import { resolveInvocationCheckout } from './scripts/e2e-invocation-selection.mjs';
import { worktreeIsolationApplies } from './scripts/browser-worktree-isolation.mjs';
const result = resolveInvocationCheckout({ checkoutRoot: process.cwd(), isIsolatedCheckoutFn: worktreeIsolationApplies });
console.log(JSON.stringify(result));
if (!result.isolated) process.exitCode = 1;
JS
```

Result: exit 0, `{"isolated":true}`. This is the actual selection helper consumed by `runE2eHold`, stopped before stateful SUT startup and testability reset. It proves checkout isolation selection only. App startup, sign-in, fixture creation and the paid response are unobserved steps owned by original slice 1's executor (refined slices 1–3); the developer owns per-run paid-call authorization. They remain part of the early probe rather than a claim of working route reachability. Only an authorized real-service observation can settle the quality premise. Original slice 1 owned that permitted early probe; refined slices 1–3 now carry its setup and quality proof, stopping dependent implementation on failed or unavailable quality. Mocked responses never substitute for its conclusion. The illustrative cases are sufficient for a bounded quality check, not an efficacy or representative-user claim.

The later dated execution observations retain the [original six-reply result](REAL-MODEL-OBSERVATION.md) and [two-reply practice follow-up](PRACTICE-FOLLOWUP-OBSERVATION.md). They settle only their stated HTTP/model boundaries; this preparation baseline keeps its original revision and limitations.
