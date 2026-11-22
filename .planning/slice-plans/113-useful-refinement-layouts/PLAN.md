# Generate useful refinement layouts for notes

**Identity:** SEED-073#generate-useful-refinement-layouts
**Source:** [Selected story and production replay](../../seeds/SEED-073-learning-refinement-and-retrieval.md#generate-useful-refinement-layouts)

## Goal and scope

Give learners a selectable layout that reflects the structure of their note,
with an explanation and retry when generation fails. Cover generation from
a note, assimilation, and recall, including the exported request. Selection
supplies the existing extraction and removal workflows.

## Outside-in proof

| Promise | Slice | Observable proof |
| --- | --- | --- |
| A compact layout follows the note's meaningful sections and preserves distinct usages. | 1 | Replay the revised exported request against a synthetic bilingual dictionary entry. Check the returned top-level senses against the numbered source, and read the children for distinct usages, source-language labels, and support from the note. Record model, cap, completion status and usage. An output supplied by a mock proves the app's handling only. |
| The request has enough room for the reported case, including when exported. | 1 | In the controller tests, a structured provider substitute gives an incomplete response at the old cap and returns a complete, valid layout at the new cap. Observe the returned selectable hierarchy and verify the exported cap matches generation. Check that a question-aware request still carries its context and the view preselects tagged items. |
| A learner sees the limit failure and can recover with Retry. | 2 | At the HTTP boundary, an SDK response with `status=incomplete` and `reason=max_output_tokens` produces a safe problem detail. In the mounted view, observe the explanation beside Retry, then a selectable layout after the next request succeeds. |
| Other unusable responses give a relevant fallback, and cancellation stays quiet. | 2 | At the controller boundary cover a non-limit incomplete response and an invalid layout. In the mounted view, prove fallback, cancellation and rejection of late cancelled results. Exercise the existing removal refresh path and recall selection with the same view. |

## Ordered slices

### 1. Generate a usable layout for a structured note
Type: Behavior
Status: planned
Proof: Replay quality check and controller/selection checks in the proof table.

Behavior: A learner opens refinement for a structured focus note → the
request allows 3,000 output tokens and asks for concise items in source
order → the learner sees a complete hierarchy with distinct usages
selectable. Opening refinement from a recall question preselects the
relevant items.

- Change the existing refinement prompt and layout budget in the shared
  request builder path used by generation and export. Prefer meaningful
  existing sections, treat grammar labels as context, and keep illustrative
  sentences with their sense. Keep the layout grounded in the focus note.
- Use the existing controller and structured SDK substitute for a note with
  numbered senses and optional recall question context. Set up the provider
  response using the cap on the actual controller request so the regression
  fails at 1,000. Reuse the export controller test for the request contract.
- Observe the revised prompt through the exported request with a synthetic
  note. The dated replay in the story establishes the defect and feasibility
  of the remedy. A replay of the assembled candidate assesses the actual
  wording in this slice. Keep the normal automated suites mocked.

### 2. Explain a failed layout and recover inline
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

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests '*AiControllerNoteRefinementTest' --tests '*AiControllerExportRefinementLayoutRequestTest' --tests '*MemoryTrackerRecallPromptRecoveryControllerTest'
CURSOR_DEV=true nix develop -c pnpm frontend:test \
  tests/components/recall/NoteRefinement \
  tests/components/recall/RefineNoteModal.extractNote.close.spec.ts \
  tests/components/recall/AnsweredQuestionComponent.spec.ts \
  tests/components/conversation/ConversationComponent.refinement.spec.ts \
  tests/pages/MemoryTrackerPageView.spellingRefinement.spec.ts
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit
```

Run the focused tests for each changed boundary and trace any additional
consumers of the final diff. The existing note-refinement Cypress scenarios
provide a browser journey for removal, reload and extraction if those paths
are affected by the implementation.

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

## Plan review

The two slices each deliver learner-visible value through the existing view.
The higher budget and compact prompt belong to the same layout outcome. The
failure-feedback slice owns the remaining recovery behavior. Existing
controller, provider mock, and mounted component boundaries supply proof
without a Structure slice.
