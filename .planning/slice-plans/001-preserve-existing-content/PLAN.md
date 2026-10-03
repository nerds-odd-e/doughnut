# Add a dictated passage without changing existing note content

**Identity:** SEED-066#preserve-existing-content

## Source

[SEED-066 — Add a dictated passage without changing existing note content](../../seeds/SEED-066-voice-input.md#preserve-existing-content),
refined 2026-10-03. Evidence:
[voice-input documentation](../../../docs/voice-input.md#existing-content-can-be-truncated-during-a-navigation-journey).

## Goal and scope

A note author dictating into an existing note gets the passage added once after
the existing body. Every passage that existed before recording stays unchanged
in the saved note, for any body length. The result goes to the note where
recording started.

- **Excluded:** revision between Flush updates and of the unfinished sentence
  (preserve-completed-speech), typing while a result is pending
  (preserve-typed-corrections), automatic title suggestion
  (author-controlled-titles), speed, failure recovery, and UI changes.
- **Rejection constraint (from the story):** a voice result never revises text
  that existed before the recording.
- **Unchanged:** the AI conversation's "complete note content" tool
  (`NoteContentCompletion`, `ToolCallHandler.vue`, `noteStore.completeContent`)
  keeps its whole-content replacement meaning.

## Current decisions

- **The voice result is an addition.** The audio-to-text model output becomes
  its own structured type holding only the new text. It stops reusing
  `NoteContentCompletion`, whose schema tells the model to replace the whole
  content. The prompt asks for only the new text, including any leading space or
  line break it needs. Its "unified diff" wording is removed.
- **The append is deterministic and runs on the client.** The note store appends
  the addition to the originating note's current store content (loading the
  realm if it is absent). It saves through `updateTextField`, so the normal
  content undo still covers it. The client was chosen because it keeps undo and
  needs no new endpoint. The typed-corrections story will revisit editor drafts.
- **The context excerpt is read from the same current store content** instead
  of the `note` prop captured when Audio tools opened. It stays context only;
  its 500-character `...` truncation no longer affects safety.
- No Accepted ADR conflicts. Under ADR 0006, an empty or failed result stays on
  the existing error path and never falls back to replacement. The ordinary
  content PATCH keeps ADR 0002 sync unchanged. No North Star topic applies.

## Decisive premises (observed 2026-10-03)

| Premise | Consumed by | Observation | Result |
| --- | --- | --- | --- |
| The audio result replaces the whole body | Slice 2 red step | Read `useNoteAudioProcessing.ts` → `noteStore.completeContent` → `updateTextField(..., value.content)`; the `NoteContentCompletion` schema says "complete new content" | True. This matches discovery's source PATCH that held only new content, and the `...uesday.` echo of the excerpt |
| Mid-speech chunks send only audio not yet transcribed, so appending cannot duplicate audio | Slice 2 append rule | Read `SRTProcessor.process` (drops the last segment and returns its start as `endTimestamp`) and `rawSampleAudioBuffer.processUnprocessedData` (moves the processed position to that timestamp) | True. The dropped segment's audio is re-sent and was never transcribed into text |
| `NoteContentCompletion` is shared with the AI conversation | Slice 1 boundary | `git grep NoteContentCompletion completeContent` | Shared by `ToolCallHandler.vue`, `aiReplyState.ts`, and `suggestions.ts`; the audio path needs its own type |
| The mocked e2e fixture stubs model output as `{content: …}` | Slices 1–2 e2e proof | `e2e_test/step_definitions/ai.ts` "…for the transcription to text request" | True. The step must emit the new field |
| `findNoteContent` checks containment, not equality | Slice 2 e2e assertion | `e2e_test/start/pageObjects/notePage.ts:103` | Containment per line. The existing scenario passes today only because the mock repeats the old body |
| Focused proofs run locally | All slices | Commands under Proof commands | Frontend audio/store specs: 33 passed. `AiAudioControllerTests`: BUILD SUCCESSFUL. `record_live_audio.feature`: 1 passing |

**Learning:** this shell exports `NODE_ENV=production`, which hides `<script
setup>` bindings from `wrapper.vm`, so every `NoteAudioTools` spec fails with
`processAudio is not a function`. Run frontend tests with `NODE_ENV` unset. CI
is green on main.

## Execution context

- Mode: story-branch; execution checkout:
  `/Users/terryyin/git/doughnut/.worktrees/add-a-dictated-passage-without-changing-existing`.
- Branch: `codex/add-a-dictated-passage-without-changing-existing`; publisher:
  `dashboard-territory.local-doughnut`; agent: Honoka-chan.
- Claim published on `origin/main` and the execution branch:
  `60199c1e3d4b7b9f1e01fae2b8ebdf81ded42313`; starting revision:
  `6a56058009bfbedcde32605b382d6ae974ac2279`.
- Increment target: `origin/refs/heads/codex/add-a-dictated-passage-without-changing-existing`.
- Setup: `./scripts/run.sh bash scripts/worktree_setup.sh` succeeded in this
  checkout against the locked dependencies; initial `vue-tsc --noEmit` passed.
- Replanning remains authorized for the existing planned scope. Focused proof
  and generation waits are the sizing exception for these cohesive slices.

## Proof commands

- Frontend: `env -u NODE_ENV CURSOR_DEV=true nix develop -c sh -c 'unset NODE_ENV; pnpm -C frontend test tests/notes/NoteAudioTools tests/store/noteStore.spec.ts'`
- Backend: `CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test -Dspring.profiles.active=test --build-cache --tests "com.odde.donut.controllers.AiAudioControllerTests"`
- E2E: `env -u NODE_ENV CURSOR_DEV=true nix develop -c sh -c 'unset NODE_ENV; pnpm cy:run --spec e2e_test/features/note_creation_and_update/record_live_audio.feature'`

## Outside-in proof

| Story example | Signal |
| --- | --- |
| 1. Long existing body | Frontend: a body of more than 500 characters plus a result produces one content PATCH whose body is the exact original followed by the addition |
| 2. Empty body | Frontend: the PATCH body is the addition alone |
| 3. Moving to another note | Frontend: the PATCH targets the originating note id and appends to that note's current store content, even when the captured prop content is stale |
| 4. Second recording | Frontend: a second result appends after the first; both appear once |
| Journey | E2E `record_live_audio.feature`: with existing "This is class 1.", a mocked model returning only the addition leaves both visible |

The real-model check is `record_live_audio_with_real_open_ai_service.feature`,
tagged `@usingRealOpenAiService`. It is paid and not run locally; hosted CI owns
it, along with the owner's evaluation dictation of example 1.

## Slices

### 1. Voice results carry only dictated text
Type: Structure
Status: done
Proof: backend `AiAudioControllerTests`, the frontend audio and store specs,
and the e2e `record_live_audio.feature` stay green after API regeneration.

Accepted proof: the listed frontend command passed 33 tests; the backend command
passed six audio-controller cases; the E2E command passed its one scenario.
`pnpm generateTypeScript`, `pnpm -C frontend exec vue-tsc --noEmit`, and
`pnpm openapi:lint`, each through the repository Nix wrapper, passed. Inspected
`AiAudioControllerTests.convertingFormat` observes `dictatedText`, the instruction
and context cases observe model inputs, `noteStore.spec.ts` observes unchanged
replacement/loading, and the mounted audio specs and mocked journey exercise the
new response field. The journey still supplies the whole old body; preservation
is slice 2 proof. Real OpenAI remains hosted proof. Independent refactoring found
no changes; accepted proof remained valid. Selective formatting passed.

Internal change:

- Introduce an audio-specific structured output class holding only the dictated
  text, and use it in `OtherAiServices.getTextFromAudio`, `AiToolFactory`, and
  `TextFromAudioWithCallInfo`.
- Regenerate the TypeScript client (`generate-api-client`).
- Map the new field into the existing replacement call in
  `useNoteAudioProcessing`.
- Make the e2e step stub the new JSON field.

External behavior is unchanged: the result still replaces the body. This
enables slice 2 without touching the conversation tool's `NoteContentCompletion`.

### 2. Dictated passage is appended to the originating note's current body
Type: Behavior
Status: planned
Proof: write red first. Change the e2e mock to return only "Let's talk about
data structure today."; under the current replacement code, "This is class 1."
disappears. Then add frontend `NoteAudioTools.processing` cases for examples 1–4
and update the real-OpenAI feature to also expect "Let's start". All proof
commands must be green.

Behavior: a note with existing body B, where the author dictates and the model
returns addition A → the originating note's saved body is exactly B followed by
A. It appears once and undo restores B. The excerpt sent as context comes from
that current body.

Changes:

- Revise the transcription prompt to return only the new text.
- Add a store append operation that reuses `updateTextField`.
- Replace the stale-prop excerpt and its "reuses previous note content between
  calls" expectation.
