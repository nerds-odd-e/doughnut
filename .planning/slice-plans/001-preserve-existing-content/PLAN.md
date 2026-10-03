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
- Slice 1 accepted revision: `64173ad25fbbe7457705aeea972a959d9d3f8dd4` on
  that increment target. Managed delivery reported CI unobserved because the
  delivery invocation omitted the Codex bridge flag and established no stream
  binding. This receipt does not prove host tools were unavailable. No observer
  or shutdown obligation exists; no CI success is claimed. Default-checkout
  maintenance is not applicable.
- Slice 2 accepted revision: `6c2ed996f76960acf2d38b8cb2c810dcac24b915` on
  the same target, with the same explicit observation gap and no rebase.
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
Status: done
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

Accepted proof and delivery preparation:

- The addition-only E2E fixture first failed with only the new text visible,
  losing `This is class 1.`. After append and independent refactoring, the same
  listed E2E command passed its one scenario.
- The listed frontend command passed 38 tests before refactoring. Replacement
  proof after extracting undo-aware text editing and splitting the preservation
  specs passed 63 tests across 11 files with:
  `env -u NODE_ENV CURSOR_DEV=true nix develop -c sh -c 'unset NODE_ENV; pnpm -C frontend test tests/notes/NoteAudioTools tests/store/noteStore tests/store/noteUndo.spec.ts tests/notes/TextContentWrapper.spec.ts tests/notes/NoteTextContent.titleEdit'`.
- `NoteAudioTools.preservation.spec.ts` observes exact long/empty PATCH bodies,
  two additions once, actual undo restoring the five original paragraphs, and
  originating-note PATCH plus untouched destination sentinel. The processing
  spec observes loaded current context and append. `noteStore.spec.ts` observes
  absent-realm append and unchanged conversation replacement. Shared title
  saves, save races, wrapper behavior, and undo also passed.
- The listed backend command passed six cases; refactoring left that boundary
  unchanged. Final `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`
  passed. No API signature changed in this slice, so generation was not needed.
- Independent refactoring centralized realm loading and extracted text editing
  behind the existing store API; all checked files are below 250 lines. Selective
  formatting passed. The fixture builder accepts undefined content consistently
  with its underlying note builder.
- A green E2E attempt failed before setup during concurrent Gradle
  `processResources` and backend hot reload (`sut.log:522–564`: temporary Flyway
  resource validation failure, then LB connection refusal). A serial retry after
  Gradle terminated passed. Do not overlap backend Gradle resource processing
  with the E2E hot-reloading stack. Verification waits and this diagnosis explain
  the slice's over-target duration; the outcome stayed cohesive.
- Paid real-OpenAI and owner evaluation remain external proof. Local E2E does
  not explicitly reload; exact saved PATCH bodies and the integrated save/display
  journey provide local persistence-boundary evidence.

## Execution retrospective

Reviewed the original story and plan at the published claim, implementation
commits `64173ad25f` and `6c2ed996f7`, their aggregate change, the current audio
and conversation consumers, and inspected proof. Both slices are done; no
unrelated commit is included. Product findings: none requiring correction.
The audio-only schema and deterministic append keep existing content outside
model replacement, while the conversation retains its complete-content meaning.
The text-edit seam owns persistence/undo; there are no intermediate bridges or
diff instructions left in the audio path.

The mocked journey drove the red change and retains integrated recording/save
proof. Long/empty/current-body/undo variations remain mounted unit tests;
shared title saves and store replacement retain focused coverage. The real-model
journey remains external proof. No test consolidation is justified by current
coverage/cost evidence; no follow-up plan was created.

Accepted ADRs 0001–0007 and 0000 were classified from the current index. Relevant
ADR 0002 publication remains on the ordinary content PATCH; ADR 0006 failure
handling gains no replacement fallback; ADR 0007 disposable test isolation was
retained. No ADR conflict or North Star topic applies.

Process review was enabled by `skipProcessRetrospective: false`. The available
conversation, agent handoffs, and runtime logs support an ODF-189 occurrence and
new DD-200 (overlapping backend resource writes/E2E) and DD-201 (managed Codex
stream-binding gap) in `DearDough.md`, 450 physical lines. Delegated private tool
history and token cost were not available, so no exact token or net cost is claimed.
CI is unobserved; review asserts no remote verdict or observer shutdown.

## Execution complete

Product advice: retain the current queue order. Completed-speech stability is
still the next selected opportunity; this append change does not settle recent
unfinished-sentence revision, pending editor drafts, or author-controlled titles.
No new product story, correction, or priority change is supported by this execution.
