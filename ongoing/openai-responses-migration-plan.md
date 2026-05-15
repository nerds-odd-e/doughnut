# OpenAI Responses API Migration Plan

## Goal

Move Doughnut's OpenAI model-generation calls from Chat Completions to Responses API in stop-safe phases, while aggressively deleting dead Chat Completions-specific code as soon as the last caller in each slice is gone.

Keep non-model-generation APIs on their native endpoints:

- Audio transcription stays on `officialClient.audio().transcriptions().create(...)`.
- Embeddings/model listing stay on their current SDK services.

## Current Baseline

Status: in current worktree, not yet committed.

Question generation has already been migrated to Responses structured output:

- MCQ generation, regeneration, contest/evaluation, refinement, and export use `StructuredResponseCreateParams`.
- `OpenAiApiHandler` has `requestAndGetStructuredResponseResult`.
- `OpenAIResponseRequestBuilder` exists for typed Responses structured-output calls.
- Full backend tests pass with `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

Dead-code rule for all later phases:

- Do not leave an unused compatibility method "for later".
- After each phase, run `rg` for removed concepts (`ChatCompletionCreateParams`, `OpenAIChatRequestBuilder`, `requestAndGetJsonSchemaResult`, `completionService`, `chat.completions`, `chat.completion.chunk`) and delete any code made unreachable by that phase.
- Tests must assert Responses request shape where the observable contract is OpenAI request construction.

## Phase 1: Migrate Note Automation Structured Outputs

Type: Behavior

User-visible behavior:

- Understanding checklist generation still shows checklist points.
- Removing checklist points still rewrites note content.
- Promoting a checklist point still creates the sibling note and updates the original note.
- Suggested note title/content automation still behaves the same.

Scope:

- `backend/src/main/java/com/odde/doughnut/services/ai/ChatCompletionNoteAutomationService.java`
- `backend/src/test/java/com/odde/doughnut/services/ai/ChatCompletionNoteAutomationServiceTest.java`
- `backend/src/test/java/com/odde/doughnut/services/NoteAutomationServiceTests.java`
- `backend/src/test/java/com/odde/doughnut/controllers/AiControllerTest.java`
- E2E capability: `e2e_test/features/assimilation/understanding_check.feature`

Implementation notes:

- Replace `OpenAIChatRequestBuilder` usage with `OpenAIResponseRequestBuilder`.
- Route through `OpenAiApiHandler.requestAndGetStructuredResponseResult`.
- Preserve focus-context and notebook assistant instruction ordering.
- Keep behavior stateless; do not use `previous_response_id`.

Cleanup gate:

- Rename or remove `ChatCompletionNoteAutomationService` if it no longer uses Chat Completions. Prefer a domain name such as `NoteAutomationService` or `AiNoteAutomationService`.
- Remove any tests that only prove old `response_format`/Chat Completion internals and replace them with Responses `text.format` assertions.
- Delete any now-unused helper methods in `OpenAIChatRequestBuilder`.

Verification:

- Backend focused tests for note automation and `AiControllerTest`.
- E2E focused run for `e2e_test/features/assimilation/understanding_check.feature`.
- Full backend test gate before closing phase.

## Phase 2: Migrate Audio Post-Processing Structured Output

Type: Behavior

User-visible behavior:

- Recording/uploading audio still transcribes with the audio transcription endpoint.
- Transcript-to-note-content post-processing still appends coherent Markdown/diff output as before.

Scope:

- `backend/src/main/java/com/odde/doughnut/services/ai/OtherAiServices.java`
- `backend/src/test/java/com/odde/doughnut/controllers/AiAudioControllerTests.java`
- E2E capability: `e2e_test/features/note_creation_and_update/record_live_audio.feature`

Implementation notes:

- Keep `getTranscriptionFromAudio` unchanged.
- Move only `getTextFromAudio` from Chat Completions structured output to Responses structured output.
- Preserve additional instructions and previous-note-content input.

Cleanup gate:

- If `OtherAiServices.getOpenAIChatRequestBuilder` becomes unused, delete it.
- Update test helper names/comments away from "completion service" for transcript post-processing.
- Delete Chat Completions assertions from audio post-processing tests.

Verification:

- `AiAudioControllerTests`.
- Focused E2E for `record_live_audio.feature`.
- Full backend test gate before closing phase.

## Phase 3: Migrate Book Layout Reorganization

Type: Behavior

User-visible behavior:

- Book layout reorganization suggestion still returns corrected block depths for PDF-derived outlines.

Scope:

- `backend/src/main/java/com/odde/doughnut/services/book/BookService.java`
- `backend/src/test/java/com/odde/doughnut/controllers/NotebookBooksBlockContentControllerTest.java`
- E2E capability: `e2e_test/features/book_reading/ai_reorganize_layout.feature`

Implementation notes:

- Convert `suggestLayoutReorganization` to typed Responses structured output.
- Keep malformed/empty AI-response error behavior equivalent.

Cleanup gate:

- Delete local Chat Completions JSON parsing in this path.
- Remove stale comments or mocks mentioning chat completion for book layout.

Verification:

- `NotebookBooksBlockContentControllerTest`.
- Focused E2E for `ai_reorganize_layout.feature`.
- Full backend test gate before closing phase.

## Phase 4: Remove Generic Chat Structured-Output Path

Type: Structure

Purpose:

- Prepare for the streaming chat migration by deleting structured-output Chat Completions helpers once phases 1-3 have no callers.

Scope:

- `OpenAiApiHandler.requestAndGetJsonSchemaResult(...)`
- `OpenAIChatRequestBuilder.responseJsonSchema(...)`
- Chat structured-output portions of `OpenAIChatCompletionMock`

Allowed changes:

- Delete methods/classes that no production caller uses.
- Keep Chat Completions streaming support only if chat-about-note still uses it.
- Keep `OpenAIChatRequestBuilder` only if needed by chat-about-note export/request construction; otherwise delete it too.

Verification:

- Compile.
- Full backend test gate.
- `rg` confirms no structured-output Chat Completions callers remain.

## Phase 5: Migrate Chat About A Note Streaming

Type: Behavior

User-visible behavior:

- User can start a conversation with AI about a note and see streamed assistant reply.
- AI receives Doughnut focus context.
- Conversation export still gives a usable external-AI prompt/request representation.
- Tool-call behavior for note updates remains supported or is explicitly removed from the UI if currently dead.

Scope:

- `backend/src/main/java/com/odde/doughnut/services/ai/ChatCompletionConversationService.java`
- `backend/src/main/java/com/odde/doughnut/services/ai/ConversationHistoryBuilder.java`
- `backend/src/main/java/com/odde/doughnut/services/ai/ChatCompletionStream.java`
- `backend/src/main/java/com/odde/doughnut/controllers/ConversationMessageController.java`
- `e2e_test/start/mock_services/openAiService.ts`
- `e2e_test/step_definitions/conversation.ts`
- E2E capability: `e2e_test/features/messages/chat_about_a_note.feature`

Implementation notes:

- Build Responses input from Doughnut's existing persisted conversation history.
- Prefer stateless Responses first: keep Doughnut DB as source of truth, avoid adopting `previous_response_id` until there is a separate product reason.
- Use Responses streaming events internally, but translate to the frontend contract deliberately. Either:
  - preserve the current SSE event names and payload shape through an adapter, or
  - change frontend/e2e to consume Responses event shape in the same phase.
- Preserve model setting behavior: chat-about-note currently uses `evaluation_model`.

Cleanup gate:

- Rename `ChatCompletionConversationService` and `ChatCompletionStream` once they no longer speak Chat Completions.
- Delete `ConversationHistoryBuilder` Chat Completion message-param dependencies; replace with a domain conversation-input builder.
- Delete `streamChatCompletion` from `OpenAiApiHandler` if this is the last streaming Chat Completions caller.
- Delete E2E helpers named `expectLastChatCompletionsBodyContains` or update them to Responses names.

Verification:

- Backend conversation controller/service tests.
- Focused E2E for `e2e_test/features/messages/chat_about_a_note.feature`.
- Full backend test gate.

## Phase 6: Final Chat Completions Removal

Type: Structure

Purpose:

- Remove all production Chat Completions dependencies after the last behavior path is migrated.

Scope:

- `OpenAIChatRequestBuilder`
- Chat Completions imports and request builders.
- `OpenAiApiHandler.chatCompletion` and `streamChatCompletion`.
- Chat Completion branches in `OpenAIChatCompletionMock`.
- E2E Mountebank helpers that only stub `/v1/chat/completions`.

Cleanup gate:

- `rg "ChatCompletion|chat.completions|chat completion|/chat/completions|completionService|ChatCompletionCreateParams" backend e2e_test` should return only historical docs or intentionally renamed compatibility comments. Prefer deleting comments rather than keeping historical references.
- Remove obsolete generated/open API export assumptions if any endpoint still names Chat Completions.

Verification:

- Full backend test gate.
- Relevant E2E specs touched by the migration:
  - `e2e_test/features/messages/chat_about_a_note.feature`
  - `e2e_test/features/assimilation/understanding_check.feature`
  - `e2e_test/features/note_creation_and_update/record_live_audio.feature`
  - `e2e_test/features/book_reading/ai_reorganize_layout.feature`
  - existing question-generation/recall specs affected by mock cleanup.

## Risks And Decisions

- Responses streaming has a different event shape than Chat Completions streaming. Do not migrate chat-about-note in the same phase as one-shot structured outputs.
- Server-side Responses state is useful, but adopting it would duplicate or compete with Doughnut's persisted conversation history. Keep stateless first.
- Tool calls in chat-about-note need an explicit decision during Phase 5: either keep and migrate them, or prove they are dead and delete both backend and frontend handling.
- The test mock should be renamed once Chat Completions support is removed. Until then, it may support both APIs, but each phase should shrink the Chat branch.

