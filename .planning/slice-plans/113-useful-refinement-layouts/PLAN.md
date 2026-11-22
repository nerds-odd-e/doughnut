# Generate useful refinement layouts for notes

**Identity:** SEED-073#generate-useful-refinement-layouts
**Source:** [Selected story and production replay](../../seeds/SEED-073-learning-refinement-and-retrieval.md#generate-useful-refinement-layouts)
**Source review:** [Source-only review](source-review.md)

## Goal and scope

Give learners a selectable layout that reflects the structure of their note,
with an explanation and retry when generation fails. Cover generation from
a note, assimilation, and recall, including the exported request. Selection
supplies the existing extraction and removal workflows.

## Outside-in proof

| Promise | Slice | Observable proof |
| --- | --- | --- |
| The request has enough room for the reported case, including when exported, and a recall question's context is still carried. | 1 | In the controller tests, a structured provider substitute gives an incomplete response at the old cap and returns a complete, valid layout at the new cap. Observe the returned selectable hierarchy and verify the exported cap matches generation. Check that a question-aware request still carries its context and that the controller returns the items the substitute marked. |
| A compact layout follows the note's meaningful sections, preserves distinct usages, and identifies the content relevant to a recall question. | 2 | Replay the revised exported request once, with a recall question, against a synthetic bilingual dictionary entry. Check the returned top-level senses against the numbered source, and read the children for distinct usages, source-language labels, and support from the note. Check that the layout identifies the question's relevant usage. Record model, cap, completion status and usage. An output supplied by a mock proves the app's handling only. |
| A child usage can be selected on its own, and a recall question preselects the relevant items. | 3 | In the mounted view, select one child while its sibling and parent stay unselected. With question context, the view preselects the tagged items. |
| A learner sees the limit failure and can recover with Retry. | 4 | At the HTTP boundary, an SDK response with `status=incomplete` and `reason=max_output_tokens` produces a safe problem detail. In the mounted view, observe the explanation beside Retry, then a selectable layout after the next request succeeds. |
| Other unusable responses give a relevant fallback, and cancellation stays quiet. | 4 | At the controller boundary cover a non-limit incomplete response and an invalid layout. In the mounted view, prove fallback, cancellation and rejection of late cancelled results. Exercise the existing removal refresh path and recall selection with the same view. |

## Ordered slices

### 1. Send a compact layout request
Type: Behavior
Status: done
Proof: Controller budget, instruction, and question-context checks in the proof table.

Behavior: A learner opens refinement for a structured focus note → generation
and its export allow 3,000 output tokens and ask for concise items in source
order → a substitute that completes only at that cap returns the hierarchy.
Opening refinement from a recall question still carries the question and
returns the items the model marked. The substitute supplies those marks.
The replay in slice 2 is what shows that the layout identified the relevant
usage.

The uncommitted backend edit in this worktree is the parked attempt. Do not
rewrite it unless a focused test fails. It sets
`NOTE_REFINEMENT_LAYOUT_MAX_OUTPUT_TOKENS` to `3000L`, extends
`generateNoteRefinementLayoutAiTool`, and teaches the structured substitute
to answer from the cap on the controller request. Leave the parked frontend
spec for slice 3. Do not call the model.

- Keep the layout grounded in the focus note. Prefer meaningful existing
  sections, treat grammar labels as context, and keep illustrative sentences
  with their sense. Numbered senses are top-level items; distinct usages are
  children; use the note's languages. Decorative markers are not sections.
- The provider substitute returns an incomplete response at 1,000 tokens and
  a complete, valid layout at 3,000, using the cap on the actual controller
  request. The exported cap matches generation. A question-aware request
  still carries its context, and the controller returns the marked items.

Accepted on 2026-11-22 at revision `0882befd7f5d` with uncommitted changes,
exit 0. Command: `pnpm backend:test_only --tests '*AiControllerNoteRefinementTest' --tests '*AiControllerExportRefinementLayoutRequestTest'`.
`returnsCompleteHierarchyWhenLayoutBudgetIsThreeThousandTokens` observes the
3,000-token cap and the returned parent and child ids.
`exportedOutputTokenBudgetMatchesGeneration` observes that export uses that
same cap. `asksForConciseItemsThatFollowSourceSections` observes the new
instruction sentences. `shouldAppendQuestionLedGuidanceWhenQuestionContextProvided`
observes that the question is carried and the stubbed marked item is returned.
Refactor made no edits, so this proof stayed in place. The mounted selection
spec remains uncommitted for slice 3.

### 2. Replay a useful dictionary layout
Type: Behavior
Status: planned
Proof: One live replay in the proof table.

Behavior: The exported request for a synthetic Japanese–Chinese dictionary
note, including a recall question about one distinct usage, is sent once →
the completed layout has five concise sense labels in source order, children
for that note's distinct usages, the note's languages, examples and
grammatical labels kept with their sense, and the question's usage identified.

- Use the request slice 1 delivered. Keep automated suites mocked.
- Send the exported body once, with question context for switching the power
  off. Record model, cap, completion status, and usage. Compare the top-level
  items with the numbered senses, read the children, and check that the
  layout identifies that usage. If that shape is missing, record the response
  and stop. Do not send another model call.

### 3. Select a child usage and recall targets
Type: Behavior
Status: planned
Proof: Mounted selection checks in the proof table.

Behavior: A learner viewing a nested layout selects one child usage on its
own → that child is selected and its sibling and parent are not. Opening
refinement from a recall question preselects the tagged items.

- The parked `selects a child item on its own` case in
  `NoteRefinement.layoutSelection.spec.ts` is the child proof. Re-run the
  existing question-context preselection case in that file. Run both
  frontend typechecks because that test source changed.

### 4. Explain a failed layout and recover inline
Type: Behavior
Status: planned
Proof: Controller HTTP problem response and mounted failure/retry journey.

Behavior: The provider returns an incomplete or unusable layout → **Refine
note** displays an actionable explanation beside **Ask AI to retry**. If the
learner retries and generation completes, the layout replaces the message.
Cancelling leaves the same view available for a fresh request.

- Reuse the structured Responses SDK boundary where the incomplete reason is
  available. Carry a specific, safe message for token exhaustion through the
  existing HTTP problem response. Give other unusable provider results a
  generic refinement-specific message. A genuinely empty note is a valid
  empty layout. Keep the shared handler's existing semantics for callers
  such as recall question generation and extraction.
- Use the existing cancelable loading and retry in `useNoteRefinementLayout`.
  Render the failure in `NoteRefinement`. Use extraction preview's error
  handling as the nearby precedent. Clear the message on a successful fresh
  layout and preserve quiet cancellation.
- Prove the wire response through existing Spring MockMvc setup with an owned
  note. The existing recall prompt recovery test demonstrates how to provide an
  incomplete response from the typed SDK mock. Mount the component with a
  problem response from the generated SDK followed by a successful result.
  Follow the same path for automatic reload after a removal.

## Current decisions

- The 3,000-token allowance applies to layout generation and its export.
  Keep the existing user-initiated retry for terminal failures.
- Use the existing response, loading and recovery boundaries. An explanation
  in the current view is the business outcome for handling generation
  failure. Unexpected failures follow the existing reporting policy.
  [ADR 0006 — Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md#usage)
- Regenerate the OpenAPI client with `pnpm generateTypeScript` if the API
  contract changes. The focused frontend suite must pass both typechecks
  before acceptance. [Frontend proof](../../../.agents/skills/frontend/SKILL.md#frontend-proof)

## Verification commands

Slice 1:

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests '*AiControllerNoteRefinementTest' --tests '*AiControllerExportRefinementLayoutRequestTest'
```

Slice 3, because a frontend test source changed:

```sh
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/NoteRefinement.layoutSelection.spec.ts
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit
```

Slice 4 adds the recall-prompt recovery controller test and the mounted
failure, removal, and entry-point specs:

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests '*MemoryTrackerRecallPromptRecoveryControllerTest'
CURSOR_DEV=true nix develop -c pnpm frontend:test \
  tests/components/recall/NoteRefinement \
  tests/components/recall/RefineNoteModal.extractNote.close.spec.ts \
  tests/components/recall/AnsweredQuestionComponent.spec.ts \
  tests/components/conversation/ConversationComponent.refinement.spec.ts \
  tests/pages/MemoryTrackerPageView.spellingRefinement.spec.ts
```

Run the focused tests for each changed boundary and trace any additional
consumers of the final diff. The existing note-refinement Cypress scenarios
provide a browser journey for removal, reload and extraction if those paths
are affected by the implementation. Slice 2 is one live replay of the exported
request, not an automated suite.

## Decisive premises

- The production replay reached the deployed generation and export endpoints
  on build `e5c95cb37c` and replayed the exported request against its model.
  The cap was the reason OpenAI reported. The larger allowance and shorter
  prompt each completed a replay. The retries were independent model calls.
  [Dated observations](../../seeds/SEED-073-learning-refinement-and-retrieval.md#generate-useful-refinement-layouts)
- The focused Chromium baseline on 2026-11-22 at `049c2cf35c` passed 33
  tests across nine files using the frontend filters above in two runs.
  The mounted view exercised question-context preselection, cancellation,
  retry, content removal and reload, extraction preview, and request export.
  The entry-point tests reached the refinement modal from the answered
  question, reviewed conversation, and spelling recall flows.
- The focused backend command above passed 14 tests on the same revision
  and date using the isolated MySQL test schema. It exercised layout
  generation and validation, export, and recall prompt recovery from the
  SDK's incomplete response for token exhaustion and content filtering.

## Learnings

- Slice 1's controller proof passed on 2026-11-22 (exit 0, base
  `0882befd7f5d`, uncommitted). The refactor pass found nothing to change.
- On 2026-11-22 the first layout slice overran at 17 active minutes
  (`storyOverruns` 1). No proof command started, so completed proof is none.
  The failure point was the clock limit after the edit and before the first
  test or replay. The sizing assumption that failed: writing the layout
  request and also finishing the controller proof, the mounted selection
  proof, and one live replay fits in 15 active minutes.
- The uncommitted product diff is parked in this worktree for the split
  slices. Backend request, substitute, and controller tests belong to slice
  1. The mounted child-selection case belongs to slice 3. Neither is proven.
  Report: the checkout's `open-dough/evidence/slice-1-report.md`.
- `AiControllerNoteRefinementTest.java` is 243 lines and
  `NoteRefinement.layoutSelection.spec.ts` is 245. Both are under 250. An
  edit that finds a file already over 250 stops before that edit.
- A second overrun of this story requires reassessment of the story boundary
  before another slice-only refinement.

## Plan review

The replaced slice combined three proof loops and overran before any of them
ran. The request contract, the one live replay, and mounted selection are now
separate slices. They still exercise one layout rule: concise source-ordered
sections, with dictionary senses as parents and distinct usages as children.
Failure and retry remain the last slice. No Structure slice was added. The
plan holds four slices.
