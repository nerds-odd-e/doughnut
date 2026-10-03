# Keep completed speech as dictation continues

**Identity:** SEED-066#preserve-completed-speech

## Source

[Keep completed speech as dictation continues](../../seeds/SEED-066-voice-input.md#preserve-completed-speech),
refined on 2026-10-03. Owner decision: hold back the unfinished sentence
instead of showing it early and revising it.

## Goal and scope

A note author dictating a longer thought with pauses and Flush ends up with
every completed sentence once and each sentence whole after reload.

- **Included:** On a pause flush or Flush click, write only the speech before
  the last transcription segment and keep that segment's audio. Only Stop writes
  everything. Dictated text, once written, is never revised.
- **Excluded:** Faster appearance of held-back text, typing during processing,
  correcting misheard words, and recognizing whether a sentence is finished.
- **Assumption:** The held-back unit is the existing last transcription segment,
  so a finished last sentence may also wait until the next chunk or Stop.

## Common rule

**Mid-speech processing never writes the last transcription segment.** Timed
chunks, pause flushes, and Flush clicks are all mid-speech; only Stop is not.
When the transcription has only one segment, mid-speech writes nothing and
keeps all of that audio. No separate recognizer or revision path is added.

## Decisive premises

| Premise | Consumed by | Observation | Result |
| --- | --- | --- | --- |
| A pause flush and a Flush click send `isMidSpeech: false`, so the whole tail is written | Slice 3 | Read `audioProcessingScheduler.ts`: `tryFlush()` calls `processAndCallback(false)`; `wireAudioProcessingScheduler` wires the silence callback to `tryFlush()`; the `NoteAudioTools.vue` Flush button calls `audioRecorder.tryFlush()` | Confirmed |
| Silence re-triggers a flush every 3 s of continued silence | Slice 3 | Read `rawSampleAudioBuffer.ts` `push()`: the counter resets to 0 after firing, so the 8 s orchard pause fires twice | Confirmed. Before, the second flush found only silence and made no request. With hold-back, it would re-send the held segment. |
| Timed chunks reach the backend as mid-speech | Slices 2–4 | Slice 2 return: `AudioUploadDTO` binds the form property `midSpeech` (Lombok `setMidSpeech`; `@JsonProperty` does not affect form binding), but the frontend sends `isMidSpeech`; a `WebDataBinder` probe bound `false`; generated `types.gen.ts` lists both fields; every slice 1 response had an empty end timestamp | False. Every chunk is processed as not mid-speech, so the hold-back never runs. Slice 2 fixes the binding. |
| A mid-speech chunk with a single segment is written in full | Slice 2 | Read `SRTProcessor.process`: `segments.length <= 1` returns the whole SRT; `SRTProcessorTests.shouldHandleSingleSegmentWhenIncomplete` asserts it | Confirmed. Without slice 2, the second pause flush would still write the held half sentence. |
| An empty `dictatedText` leaves the note unchanged | Slice 2 | Read `noteTextEditing.ts` `appendDictatedText`: returns early when `dictatedText` is empty | Confirmed |
| A truthy end timestamp of `00:00:00,000` advances no audio | Slice 2 | Read `rawSampleAudioBuffer.ts` `processUnprocessedData`: an empty timestamp marks all audio processed; a parsed 0 s advances no samples | Confirmed. An empty timestamp must not be returned for held audio. |
| The mocked live-audio journey returns one SRT segment for every request and asserts text after the 2-minute timer | Slice 2 | Read `record_live_audio.feature` | Confirmed. After slice 2, the timer step writes nothing and Stop appends once, so the scenario's expectation changes. |
| Focused frontend specs run locally | Slices 2–3 | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/models/audioProcessingScheduler.flush.spec.ts tests/models/audioBuffer.spec.ts` | 14 passed |
| Since the append change, completed orchard passages survive, and the real transcription ends a segment at the eight-second pause | Slices 1 and 4 | Slice 1 real-service orchard journey, 2026-10-03 | Confirmed; see Learnings |

## Outside-in proof

| Promise | Owning slice | Proof |
| --- | --- | --- |
| Completed passages written during a recording stay unchanged and appear once after reload | 1 (baseline), 4 | Real-service orchard journey and reload |
| A pause or Flush writes nothing from the half sentence | 3 | Scheduler flush spec: pause and Flush chunks are mid-speech; a second silence period in the same pause makes no request |
| A lone held segment is not written mid-speech and its audio is kept | 2 | `SRTProcessorTests` and `AiAudioControllerTests`: one segment mid-speech → no completion request, empty text, `00:00:00,000` |
| Stop writes the held-back segment once | 2 | `record_live_audio.feature`: content unchanged after the 2-minute timer; appended once after Stop |
| The whole orchard passage appears once with each sentence whole | 4 | Real-service orchard journey and reload |

Local gates: the `frontend` skill's typecheck for frontend proof, plus the
focused specs and feature named below. No broader suite is required.

## Slices

### 1. Observe the orchard journey on the current append behavior
Type: Structure (probe)
Status: done
Proof: Recorded observation in this plan's Learnings.

Use [dough-manual-testing](../../../.agents/skills/dough-manual-testing/SKILL.md)
with real services and the reproduction in the
[voice-input documentation](../../../docs/voice-input.md#completed-dictated-content-can-disappear).
This runs on the Development stack from the primary checkout, because linked
worktrees refuse it, and uses paid OpenAI calls. Record each written passage,
the saved body after reload, and whether "The book that I bought yesterday" was
written as a separate finished sentence. Changes no product code.

**Stop condition:** If completed orchard facts are still lost after reload, the
append premise is false. Stop dependent slices and replan before changing the
flush rule.

### 2. A lone segment waits until Stop
Type: Behavior
Status: done
Proof: `SRTProcessorTests`, `AiAudioControllerTests`, and
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/record_live_audio.feature`

Behavior: A note is recording, and one transcription segment has been spoken
→ the 60-second mid-speech timer fires → the note body is unchanged and no
completion request is made. Stop then appends that passage once.

The audio upload binds the mid-speech flag the frontend sends: rename the DTO
field to `midSpeech` without the `@JsonProperty`, regenerate the API client,
and send `midSpeech` from `useNoteAudioProcessing`. Without this, the timer
chunk is processed as not mid-speech and writes everything.

`SRTProcessor` returns no SRT and end timestamp `00:00:00,000` for a mid-speech
transcription with one segment, including an empty one. `AiAudioController`
skips the completion call when no SRT remains and returns empty dictated text
with that timestamp. Update the single-segment and empty-SRT processor tests.
Update the mocked feature so it expects unchanged content after the timer and a
single append after Stop.

### 3. A pause or Flush holds back the unfinished tail
Type: Behavior
Status: planned
Proof: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/models/audioProcessingScheduler.flush.spec.ts tests/models/audioBuffer.spec.ts`, plus the frontend typecheck

Behavior: Speech is followed by a silence longer than 3 s, or the author clicks
Flush → the chunk is sent as mid-speech, so the last segment is held back.
Continued silence in the same pause sends no further request. Stop still sends
the remainder as not mid-speech.

`tryFlush()` processes as mid-speech. The silence callback fires once per
silent run and fires again only after non-silent audio. Replace the
"not isMidSpeech when silence triggers" expectation, and add a silence-run
case to `audioBuffer.spec.ts`. Update the
[voice-input documentation](../../../docs/voice-input.md#adding-dictated-text-to-a-note)
paragraph on mid-speech processing to state the common rule.

### 4. The orchard passage survives whole
Type: Behavior
Status: planned
Proof: Repeat slice 1's real-service orchard journey and reload, recording the
result here.

Behavior: An existing paragraph, then the orchard passage with its eight-second
pause, a Flush about 22 s in, and Stop → after reload, the original paragraph,
the orchard facts, the complete book sentence, and the meeting sentences each
appear once. "The book that I bought yesterday" does not appear as its own
finished sentence.

If the transcription did not end a segment at the pause in slice 1, record what
was held back and compare it with the story's boundary assumption. Do not add
sentence detection.

## Current decisions

- Hold back rather than revise (owner, 2026-10-03).
- One rule for all mid-speech chunks. The single-segment change also affects the
  60-second timer, which the story's boundary assumption covers.
- The mid-speech binding fix belongs to slice 2: the story's common rule
  requires timed chunks to be mid-speech, and the mocked timer journey is the
  first proof that observes it (coordinator, 2026-10-03).
- Firing the silence callback once per silent run is part of slice 3. It keeps
  an existing pause from repeatedly re-sending the held segment.

## Learnings

- **Slice 1 baseline (2026-10-03).** Development at `e6134c419f`, Chrome/macOS,
  `manual`, note 13728 with a six-sentence paragraph. Input: `say -r 150` in two
  parts joined by 8.000 s of digital silence (29.211 s, mono 48 kHz), fed
  through a synthetic `getUserMedia` stream; Flush clicked at 22.2 s, Stop at
  31.2 s. Each write appended once and nothing was revised; after reload the
  original paragraph and both orchard facts appear once. The append premise
  holds.
  - Pause flush (11.4 s; one request, none later in the pause) wrote all three
    segments, ending with the finished sentence "The book that I bought
    yesterday." The transcription ended a segment at the pause.
  - Flush wrote " After reading several reviews and comparing different
    editions is a gift from my sister because she" (segments end at "sister"
    and "because she").
  - Stop wrote " enjoys learning about the history of gardens. The meeting is
    on Friday afternoon. We should bring a notebook and a pencil."
  - The complete book sentence never appears. Slice 4 expects the pause flush
    to hold back the "yesterday" segment and Flush to hold back "because she".
  - Every chunk in this run was processed as not mid-speech (see the binding
    premise), so the pause flush and Flush wrote the whole tail as planned,
    and no timer chunk occurred.
  - Outside this story: the first passage had no leading whitespace, so the
    saved body reads "every hour.The orchard".
- **Slice 2 (2026-10-03).** Accepted proof: `AiAudioControllerTests`
  `shouldHoldBackSingleSegmentOfMidSpeechUploadWithoutCompletion` (MockMvc
  multipart with `midSpeech=true`; fails with the old field name),
  `SRTProcessorTests`, and `record_live_audio.feature`, whose exact saved-body
  step after Stop rules out both a mid-speech write and a double append.
  - Stop hung under `@mockBrowserTime` when clicked during an in-flight chunk:
    `stop()` polled with `setTimeout`. The scheduler now awaits the in-flight
    promise (`audioProcessingScheduler.stop.spec.ts`).
  - Controller tests that call the method directly skip form binding; a
    multipart DTO field name needs a MockMvc test.
  - `backend:test:worktree` takes one `--tests` pattern, and it shares
    `backend/build` with `cy:run`, so run them one after another.
