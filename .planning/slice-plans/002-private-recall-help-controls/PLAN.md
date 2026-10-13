# Keep private recall help controls in view

**Identity:** SEED-073#story-9
**Source:** [Selected story](../../seeds/SEED-073-learning-refinement-and-retrieval.md#story-9)

## Goal and scope

A learner can identify and close private recall help throughout a long
conversation, then continue the same attempt. Keep the heading and an accessible
Close control in the viewport while all question, selected-answer, exclusion
reason, and transcript content remains scrollable. Cover opening, long replies,
exclusion confirmation, reading in both directions, and reopening.

Preserve Close/Escape, nested refinement return, opener focus, recall progress,
and existing exclusion outcomes. Composer pinning, restoring a previous scroll
position, conversation navigation redesign, and product-wide modal redesign are
deferred. No backend, persistence, scheduling, or API change is required by the
observed solution. Implementation is not authorized by this preparation.

## Existing solution and constraints

PFE found the responsibility in `frontend/src/components/commons/Modal.vue`:
it owns the native dialog, heading/body slots, outer Close, and modal stack.
`recall/ConversationButton.vue` supplies the heading and returns focus to the
opener. `conversations/ConversationComponent.vue` and `ConversationTemplate.vue`
already render the question and transcript with overflow handling;
`commons/ScrollTo.vue` scrolls new content into view. The unbounded content makes
that operation scroll the outer modal and its controls out of sight.

Reuse these owners. The successful probe gave Modal an opt-in bounded panel
with a non-scrolling header and Close, and an internally scrollable body;
only ConversationButton selected it. Keep the ordinary modal behavior as the
default. A plain class on Modal does not work: its Teleport root refuses
automatic attribute inheritance. Let the implementation choose a small coherent
interface rather than copying another dialog or overriding shared scroll
behavior. The existing outer Close needs an accessible name when used as the
persistent dismissal control. Conversation controls can remain in the content;
the promise is an always-available dismissal, not pinning every toolbar action.

Consumer search found ConversationComponent also in MessageCenterPage and
ConversationTemplate in NoteConversation. The Modal default is widely used.
The full frontend suite covers these consumers, recall/history help entry
points, the modal stack, and nested refinement. Keep new geometry assertions at
the mounted recall-page boundary, not CSS-class assertions or a mock dialog.

The ADR index lists 0000–0007 as Accepted, matching their in-file statuses.
[ADR 0002 — Git-native Portable notebook tree synchronization](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
keeps private learning data outside Portable content;
[ADR 0003 — Spaced-repetition scheduling policy](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md)
owns the existing learning effects; this layout work changes neither.
[ADR 0007 — Environments and isolation](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
constrains observations to isolated test state. No exception is needed.
The [North Star](../../NORTH-STAR.md) adds no direction needed for this local
presentation responsibility.

## Preparation observations

Observed on 2026-10-14 at `2534af9c3a13b1712d719b2fb42a6fac5b3dedbc` in
`/Users/terryyin/git/doughnut/.worktrees/keep-private-recall-help-controls-in-view-for-lo`.
All production and test edits were disposable and reverted before recording.

The scratch probe extended `frontend/tests/pages/RecallPage.privateHelp.spec.ts`.
It mounted the actual RecallPage attached to the document, answered an MCQ,
opened help, used `confirmTutorExclusionProposal` with the reason
`"Two choices fit the stem equally well. ".repeat(150)`, and streamed
`"A long explanation. ".repeat(100)` through `simulateAiResponse`. It then
scrolled content, closed, and reopened with the excluded attempt and long reply
returned by mocked HTTP. It used `page.viewport` from `vitest/browser` for
1728×963 and 390×600. HTTP/SSE alone were mocked; real components, CSS,
IntersectionObserver, and scrolling ran in headless Chromium.

| Decisive premise | Consuming operation / literal command | Observed result |
| --- | --- | --- |
| Long content makes the actual dialog lose its controls | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/RecallPage.privateHelp.spec.ts`; first instrumented run measured opening, confirmation, a streamed long reply, and reopening after allowing smooth scrolling to settle. | At 1728×963 the heading moved to y=-2501 after confirmation, y=-157 after the reply, and y=-2191 on reopening. The existing functional assertions still passed (4 tests), so they do not prove visibility. |
| The remedy needs to handle constrained height as well as desktop width | Same command with an initial bounded flex-body experiment. | Desktop geometry passed; at 390×600 confirmation still scrolled the outer panel by 239px and moved the heading to y=-199. This rules out treating outer max-height or a flex body alone as sufficient. |
| A header outside a scrollable body can deliver the journey through existing owners | Same command with an opt-in Modal panel height `calc(100dvh - 40px)`, flex column, outer `overflow: clip`, and body `flex: 1; min-height: 0; overflow: auto`. The ordinary header/outer Close remained outside the body; the Close gained an accessible name. | 5 tests passed in 13.27s. At both sizes the heading remained y=40 and outer Close y≈24.8 through opening, confirmation, the long reply, content scrolling, and reopening. Bounds and center-point hit tests passed. The overflowing body scrolled to its end; its last text rectangle was inside its visible bottom, then it scrolled back to the start. Existing Close/Escape/nested-refinement return checks passed. |
| Geometry proof distinguishes the original behavior | Restored production code while retaining the same scratch spec; ran the same command. | Exactly the two geometry cases failed: heading y=-2501 at desktop and y=-3403 at 390×600 after confirmation. The three functional return cases passed. |
| The opt-in change can preserve existing frontend consumers | Reapplied the successful scratch experiment; `CURSOR_DEV=true nix develop -c pnpm frontend:test`. | All 350 files / 2,189 tests passed in 40.65s, including both geometry cases and the modal/conversation/help consumers. |
| Several reopened exchanges and the long reason can be read fully through the same body | Same focused command with three stored replies (`First exchange. `, `Second exchange. `, `Last exchange. `, each repeated 60 times) and the long excluded reason; click the persistent outer Close, assert opener focus, and reopen. Scroll character ranges at the beginning and end of the reason and first/last exchange into view; assert their clip bounds and the heading/Close bounds and hit targets. | All 5 tests passed in 5.70s at both viewports. This observes multiple stored messages as well as the separate streamed-reply experiment. |
| The proposed interface fits frontend types | On that scratch content, `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit` and `CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit`. | Both exited zero. |

Limitations: synthetic long text and mocked persistence/AI establish frontend
layout feasibility, not real-model quality, device keyboards, or browser-wide
compatibility. The solution depends on overflowing content, not a message
count. No backend behavior is inferred from frontend mocks.
Production implementation and durable regression tests remain to be written.

## Outside-in proof ownership

| Promise | Owning slice | Observable proof |
| --- | --- | --- |
| Heading and usable Close stay visible on open, reply, confirmation, scrolling, and reopen | 1 | Mounted RecallPage browser scenario at 1728×963 and a constrained 390×600 viewport; long reason and several exchanges, stream a long reply, confirm exclusion, close/reopen the same conversation. Assert heading and persistent Close bounding rectangles inside the viewport and the control's hit target after scrolling settles. |
| All question, selected-answer, reason, and transcript content remains readable | 1 | In the same journey, scroll to the beginning and end of the actual reading area(s), observe representative first/last content within their clip bounds, including the long reason and last exchange. Content presence in the DOM alone is insufficient. |
| Closing returns to the same attempt/progress and opener focus; child refinement returns to help | 1 | Preserve existing RecallPage.privateHelp cases for Close, Escape, and nested refinement then Escape. Click the persistent visible Close in the long-content scenario and assert return focus and progress. |
| Existing exclusion result and shared modal/conversation consumers remain usable | 1 | Preserve the existing exclusion assertion and run the complete frontend suite; it covers recall/history help and shared modal, message-center, and note-conversation consumers. |

## Ordered slices

### 1. Read long private help with visible dismissal
Type: Behavior
Status: done
Proof: All rows in the proof table; complete frontend suite and both frontend
typechecks on the final content.

Behavior: A learner opens a long private recall-help conversation, receives a
long reply or confirms an exclusion, and reads its full content → the heading
and Close remain in view. Closing/reopening preserves the existing attempt and
return journey.

Implement the smallest opt-in containment supported by the observation, using
the existing Modal and conversation. Add lasting page-level geometry proof,
using browser actions and settled observations rather than the probe's fixed
one-second delays. Restore the viewport after a parameterized case. Keep
geometry and readability assertions about rendered outcomes. Explain the
reading behavior in `docs/recall-help.md` under Reading and returning.

Sizing hypothesis: about 5 minutes of focused implementation and test authoring
using the observed mechanism, plus roughly one minute for the measured suite
and typechecks and normal delivery gates. This is one cohesive behavior and
proof loop; splitting by viewport, CSS, or tests creates no safe useful partial
outcome. Scrutinize an overrun beyond 5 minutes; above 10 minutes stop and
replan unless only a stated test/external wait remains (AGENTS.md). The story's
30–60 minute estimate includes independent refactoring and delivery overhead.

## Execution complete

Product advice: No additional product work is recommended. The local presentation
change fulfills the story and preserves existing learning/refinement boundaries;
keep the current backlog order and the story's deferred promises.

## Story obligations

### G1. Browser observation scope
Reported: slice 1 — "Observations use headless Chromium and mocked HTTP/SSE with synthetic long content."
Story clause: "A learner reading a long private recall-help conversation can identify and close help without scrolling back to find its controls, then continue the same recall attempt."
Disposition: no user cost "A learner reading a long private recall-help conversation can identify and close help without scrolling back to find its controls, then continue the same recall attempt.": real rendering and scrolling prove the presentation journey; unchanged backend behavior and real-model quality are not inferred from the mocks.

## Verification and delivery

During implementation, iterate with:

```sh
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/RecallPage.privateHelp.spec.ts
```

Before acceptance, the frontend skill requires behavioral proof and both
typechecks on the same content. Use the full suite because this changes shared
modal presentation and its observed runtime is short:

```sh
CURSOR_DEV=true nix develop -c pnpm frontend:test
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit
```

AGENTS.md execution wrap-up remains required: Jidoka, fresh
dough-post-change-refactor agent, API generation if contracts actually change,
coordinator runs `./scripts/run.sh pnpm format:changed` once, update the plan,
commit through the assigned-agent entry point with the check-only hook, and
push through authorized execution delivery. No manual-testing or paid-model
gate is added. Reassess consumers if implementation changes shared defaults.

## Current decisions and learnings

### Retrospective

- Reviewed delivered implementation `f85c5eb53b25464823db8f6f413fc66af84d94b5`
  against the execution-ready plan at `f884b96714131d631e608504d71d418edf3ae906`
  and selected story. The intervening claim is execution provenance.
  No product finding, architectural drift, or correction plan is needed.
- Modal owns containment; ConversationButton selects it and retains focus return;
  conversation data, access, exclusion, and scheduling remain with existing owners.
  ADR 0002, 0003, and 0007 boundaries remain intact. Browser geometry adds unique
  page-level proof; existing ambiguity-exclusion E2E remains integration proof.
- Process review enabled by project configuration: recorded this execution's
  repeated truncated startup retrieval under existing `DD-233` in `DearDough.md`
  (346 physical lines). Agent implementation/refactor stayed within their budgets.
- CI on the accepted story-branch revision is pending during review;
  the retained observer and completion operation own its final verdict.

### Accepted slice proof and delivery preparation

- Implementation took about four minutes. `Modal.scrollBody` owns the bounded
  panel and scrolling body; ConversationButton opts in. Heading and named Close
  stay outside the body. API and component generation triggers are unchanged.
- Inspected `privateHelpGeometryCases.ts` at the mounted RecallPage boundary:
  both viewports cover opening, learner-confirmed long exclusion reason,
  streamed long reply, persistent Close, preserved focus/progress/queue, and
  reopening the original conversation ID with three stored exchanges.
  `privateHelpGeometryTestSupport.ts` observes settled rectangles and center
  hit targets, plus first/last text ranges inside all clipping ancestors for
  question, selected answer, reason, and transcript. Viewports are restored.
- `CURSOR_DEV=true nix develop -c pnpm frontend:test` passed: 350 files,
  2,194 tests, 40.13s. Existing Close/Escape/nested-refinement and exclusion
  observations remain; the suite covers shared modal/conversation consumers.
- Independent refactoring extracted geometry observations and consolidated
  attempt-context reading without changing product code, fixtures, or assertions.
  On that content, `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/RecallPage.privateHelp.spec.ts`
  passed all six tests in 10.79s. Both
  `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit` and
  `CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit` passed.
  Unchanged consumers retain the accepted full-suite evidence.
- Refactor returned `REFACTOR COMPLETE`; whitespace check passed.
  Coordinator `./scripts/run.sh pnpm format:changed` passed once, mechanically
  formatting the two new test modules. No remaining slice or proof gap.

### Execution context

- Mode: story-branch; identity `SEED-073#story-9`; publisher
  `dashboard-territory.local-doughnut`; assigned agent Mihiro-chan.
- Execution checkout:
  `/Users/terryyin/git/doughnut/.worktrees/keep-private-recall-help-controls-in-view-for-lo`;
  branch `codex/keep-private-recall-help-controls-in-view-for-lo`.
  Reused the established linked workspace; no integration-checkout mutation.
- Claim `eeedbbdb1949da9148ead5284a370ac751e59607` accepted on `origin/main`
  by the established start and confirmed on the remote execution branch.
  Starting revision `f884b96714131d631e608504d71d418edf3ae906`.
  Increment target is that execution branch on `origin`.
  Accepted implementation `f85c5eb53b25464823db8f6f413fc66af84d94b5` extends
  claim `eeedbbdb1949da9148ead5284a370ac751e59607` without reconciliation;
  managed delivery reused this coordinator's observer.
- Checkout setup: `CURSOR_DEV=true nix develop -c bash scripts/worktree_setup.sh`
  passed against the current locked installation; applicable command
  `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`
  passed before implementation. The commit hook is check-only `lint:changed`.
- Replanning permission: preserve existing planned-execution authority.
- CI: GitHub `nerds-odd-e/doughnut`, verified `ci.yml` (`donut CI`),
  execution branch target; coordinator `mihiro-story-9`.
  Codex stream cell `9`, session `91477`, directory
  `/tmp/dough-ci-501/watch-3hHaik`, PID `76970`.
  The startup claim on trunk has `pendingCi: unobserved`; this stream covers
  subsequent story-branch deliveries.

- One containment rule owns all content growth; no separate handling for a
  reason, streaming reply, or reopening.
- Keep persistent identity/dismissal separate from scrollable reading content.
  Do not promise a pinned composer or every conversation-toolbar action.
- No Structure slice, new architectural topic, ADR exception, or new product
  dependency is needed.
- Slice-plan refinement was not needed: one bounded outcome has mapped proof,
  a discriminating observed remedy, and no unresolved slice-specific concern.
