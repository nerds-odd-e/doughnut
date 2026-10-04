# Keep every transcribed sentence when dictated text is written

## Source

- Story: [Keep every transcribed sentence when dictated text is written](../../seeds/SEED-066-voice-input.md#keep-every-transcribed-sentence)
- Identity: SEED-066#keep-every-transcribed-sentence
- Owner decision, 2026-10-04: write the transcription itself and remove the
  step that has a model rewrite it.

## Goal and scope

The passage written for an audio chunk is the text of that chunk's written
transcription segments, in order, with nothing left out, added, or reworded.
The rewrite step and everything only it used are deleted, and the product is
swept so it reads as if they never existed (CLAUDE.md principle 7).

Excluded: correcting misheard words, transcription quality, Markdown shaping
and paragraph breaks, translation or other instructed processing, sentence
recognition for hold-back, and joining that fits Japanese, Chinese, or mixed
languages (that stays with
[join dictated passages](../../seeds/SEED-066-voice-input.md#join-dictated-passages)).

Assumption from the story: text is joined with one space, between segments of
a passage and between a passage and existing text that does not end in
whitespace; a passage added to an empty body starts without one.

## Architecture

PFE: no new responsibility is added. The existing owners stay: `SRTProcessor`
decides which segments are written and the processed audio position;
`NoteTextEditing.appendDictatedText` and the open editor's `appendToDraft`
write a passage. The segment text comes from `SRTProcessor` (it already owns
the segments), and one join rule is shared by both write paths. No Accepted
ADR is affected. [One real-service audio test](../../NORTH-STAR.md#one-real-service-audio-test)
is followed: new behavior is proved with mocked services and mounted
frontend tests; the single real-service feature is only kept true.

## Outside-in proof

| Key example | Owning slice | Proof |
| --- | --- | --- |
| Three-segment mid-speech chunk writes the first two segments joined by one space; the third is held back | 2 | `AiAudioControllerTests` through `POST /api/audio/audio-to-text` |
| Stop on "This is class 1." saves "This is class 1. its talk about dada struct day." | 2 (join from 1) | `record_live_audio.feature` |
| Stop on an empty body writes the passage with no leading space | 1 | `NoteAudioTools.preservation.spec.ts` (empty-body case) |
| Timed chunk, Flush, and Stop each write once, in order, one space between passages | 1 | `NoteAudioTools.preservation.spec.ts` (repeated additions), rich and Markdown typing-while-pending specs for the open-editor path |
| Advanced Options offers full-screen editing and no Processing Instructions field | 3 | `NoteAudioTools.advancedOptions.spec.ts` |
| Real services, orchard passage: every transcribed sentence appears once after reload | 4 | Real-service feature run plus one manual orchard run |

Commands (from `.agents/agent-map.md`):

- Frontend: `CURSOR_DEV=true nix develop -c env -u NODE_ENV pnpm frontend:test <spec files>`, plus the typecheck named in the `frontend` skill's "Frontend proof" rule.
- Backend: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- E2E: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/record_live_audio.feature`

## Decisive premises

| Premise | Consumed by | Observation | Result |
| --- | --- | --- | --- |
| Both write paths append by plain string concatenation onto text they hold | Slice 1 join rule | Read `frontend/src/store/noteTextEditing.ts:71-80` and `frontend/src/components/notes/core/TextContentWrapper.vue:160-163` | True: `content + dictatedText` and `localValue + text` |
| The existing audio specs pass before the change | Slices 1, 3 | `env -u NODE_ENV pnpm frontend:test tests/notes/NoteAudioTools.preservation.spec.ts tests/notes/NoteAudioTools.advancedOptions.spec.ts tests/store/noteStore.spec.ts` on 2026-10-04 | 3 files, 20 tests passed |
| `getTextFromAudio` and `transcriptionToTextAiTool` have one caller, the audio controller | Slice 2 removal | `grep -rn "getTextFromAudio\|transcriptionToTextAiTool" backend/src` | True; only `AiAudioController` and `OtherAiServices` |
| `globalSettingOthers` has other users and stays | Slice 2 removal | `grep -rn globalSettingOthers backend/src/main` | True: `GlobalSettingsService` reads and sets it for the admin model settings |
| The mocked "transcription to text" completion step is used by one feature | Slice 2 removal | `grep -rn "transcription to text" e2e_test` | True: only `record_live_audio.feature` and its definition in `step_definitions/ai.ts:111` |
| A segment's text is the lines after its timestamp line, with or without a leading index line | Slice 2 text extraction | Read `SRTProcessorTests` sample (index line present) and the mocked transcript in `record_live_audio.feature` (no index line) | Both forms exist in tests; extraction must take the lines after the `-->` line |
| Processing Instructions and the context excerpt are used only by the audio request | Slice 3 removal | `grep -rnil "processingInstructions\|Processing Instructions\|previousNoteContentToAppendTo"` over frontend, backend, e2e, docs, cli, mcp-server | True: `NoteAudioTools.vue`, `NoteAudioToolsAdvancedOptions.vue`, `useNoteAudioProcessing.ts`, `AudioUploadDTO`, `AiAudioController`, `OtherAiServices`, three frontend specs, `AiAudioControllerTests` |
| The e2e content step matches text contained in the body | Slice 4 | Read `e2e_test/step_definitions/note.ts:48-59` (`findNoteContent`) | True, so the real-service assertions do not depend on the join |
| Whisper's own text for `lecture.wav` contains "Please be quiet." | Slice 4 | Direct whisper-1 SRT call, 2026-10-04 | True: one segment, "Please be quiet." |

## Slices

### 1. Dictated passages join existing text with one space
Type: Behavior
Status: done
Accepted proof: `env -u NODE_ENV pnpm frontend:test` over the preservation, typing-while-pending, processing, recording, and advanced-options NoteAudioTools specs and `tests/store/noteStore.spec.ts` (48 passed), plus `vue-tsc --noEmit`. The join lives in `NoteTextEditing.appendDictatedText`; the open editor exposes `changeDraft`. The e2e expected body was updated but runs only in CI.
Proof: `NoteAudioTools.preservation.spec.ts`, the typing-while-pending specs, and `tests/store/noteStore.spec.ts` assert exact saved bodies; fixtures supply passages without leading whitespace.

Behavior: a note body or open editor draft that does not end in whitespace →
a passage arrives → the saved text is the existing text, one space, then the
passage. An empty body becomes the passage alone; text already ending in
whitespace gets no extra space. One join rule serves both the saved-body path
and the open-editor draft path. Update the join paragraph of
`docs/voice-input.md`.

Interim: while the rewrite model still runs it may add its own leading
whitespace, giving two spaces; slice 2 removes the model. The mocked journey's
expected body changes to "This is class 1. Let's talk about data structure
today." here and to its final text in slice 2.

### 2. The written passage is the transcription's own text
Type: Behavior
Status: done
Accepted proof: full `pnpm backend:test_only` (`AiAudioControllerTests` mid-speech/Stop/lone-segment cases, `SRTProcessorTests` text extraction with and without index lines), `env -u NODE_ENV pnpm frontend:test tests/notes/ tests/store/noteStore.spec.ts` (282 passed), `vue-tsc --noEmit`, and `record_live_audio.feature` run locally (1 passing). The "no Responses API call" assertion was dropped under CLAUDE.md principle 7 (no absence checks).
Proof: `AiAudioControllerTests` (three-segment mid-speech response is the first two segments' text joined by one space with the end position of the second; Stop writes all segments; no Responses API call is made), `SRTProcessorTests`, and `record_live_audio.feature` saving "This is class 1. its talk about dada struct day.".

Behavior: an audio chunk is uploaded → the response's passage is the text of
the written segments joined by single spaces, and hold-back and the end
position are unchanged. Delete what only the rewrite used: the
`transcriptionToTextAiTool` instruction, `OtherAiServices.getTextFromAudio`,
the `DictatedText` schema class (the response carries the passage as plain
text), the mocked "transcription to text" e2e step and its use, and the
frontend fixtures for the old shape. Regenerate the API client
(`generate-api-client` skill). Rewrite "Adding dictated text to a note" in
`docs/voice-input.md` to describe the present behavior.

### 3. Audio tools send only the audio
Type: Behavior
Status: done
Accepted proof: `env -u NODE_ENV pnpm frontend:test tests/notes/ tests/store/noteStore.spec.ts` (advanced-options "offers full-screen editing as the only advanced option"; processing "sends only the audio and the mid-speech flag"), `vue-tsc --noEmit`, and full `pnpm backend:test_only`. The advanced-options component was folded into `NoteAudioTools.vue`.
Proof: `NoteAudioTools.advancedOptions.spec.ts` shows full-screen editing and no Processing Instructions field; `NoteAudioTools.processing.spec.ts` and `AiAudioControllerTests` show the request carries only the audio and the mid-speech flag.

Behavior: an author opens Advanced Options → it offers full-screen editing
only. Delete the Processing Instructions field, its model binding and styles,
`additionalProcessingInstructions` and `previousNoteContentToAppendTo` from
`AudioUploadDTO` and the client call, the 500-character excerpt helper, and
their tests. Regenerate the API client. Sweep `docs/voice-input.md` (opening
paragraph and context excerpt paragraph) and code comments.

### 4. Real-service dictation keeps every sentence
Type: Behavior
Status: done, owner manual orchard run pending
Accepted proof: a direct whisper-1 SRT call on `lecture.wav` (2026-10-04, owner-authorized real-service use) returned the single segment "Please be quiet.", which the feature already expects, so the feature is unchanged. Linked worktrees refuse live-OpenAI specs, so the feature's run is the CI `note_creation_and_update` shard. The manual orchard run on local Development remains with the owner. The docs section is renamed "Dictating a passage with a pause and Flush".
Proof: `record_live_audio_with_real_open_ai_service.feature` passes, and one manual orchard run on local Development saves every transcribed sentence, including "These facts are finished.", once after reload.

Behavior: with real services, the orchard passage is dictated with a Flush →
every transcribed sentence is in the saved note once after reload. These are
paid, credentialed calls: run them only with the owner's authority for
real-service use. If Whisper's text for `lecture.wav` differs from "Please be
quiet.", set the feature's expected text to what Whisper returns. Replace the
"never reached the note" observation in `docs/voice-input.md` with the
current behavior.

## Current decisions

- The rewrite is removed, not checked or re-instructed (owner, 2026-10-04).
- One space is the join; language-fitting joins belong to the join story.
- `rawSRT` in the response is left as it is; it is not part of the rewrite.

## Learnings

- The e2e transcription mock wrapped the SRT in a JSON body; the rewrite hid it. `stubTranscription` now returns plain-text SRT, as Whisper does for `response_format=srt`.
- `SRTProcessor` reads a segment's end timestamp from its second line, so a segment without an index line (the mocked e2e transcript) yields an empty end timestamp. Real Whisper SRT carries index lines; left unchanged.
- Real-service e2e features cannot run from a linked worktree; their branch proof is CI or the owner's Development run.
