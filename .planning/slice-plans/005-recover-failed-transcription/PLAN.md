# Recover a failed transcription without repeating the speech

## Source

- Story: [Recover a failed transcription without repeating the speech](../../seeds/SEED-066-voice-input.md#recover-failed-transcription)
- Identity: SEED-066#recover-failed-transcription
- The recovery interaction was chosen during refinement on 2026-10-05; the
  owner has not yet reviewed it.

## Goal and scope

An author whose speech could not be turned into text keeps that speech and
gets its text without saying it again.

- A conversion request that returns no transcription (an error answer or no
  answer) keeps its audio as not yet converted.
- While recording, the recording continues and the kept audio goes out again
  with the next conversion: timed, pause, Flush, or Stop.
- After Stop, while audio is still not converted, Audio tools offers Retry,
  which converts and writes everything that remains, as Stop does.
- Recovered text is joined once, in spoken order; text already written is not
  written again; a failure leaves the note's saved content unchanged.
- A failure shows one plain sentence, styled as an error, in place of the raw
  `Error: Failed to process audio`. It goes away when a later conversion
  succeeds.

Excluded: recovery after reload, crash, closing Audio tools, or leaving the
page; offline work; automatic retries on a timer; a failure while saving the
note after conversion succeeded; cause-specific messages; any wider change to
the Audio tools controls; any real-service scenario; how the start-recording
and device-switch errors are shown.

Assumptions:

- Not observed and not relied on by any slice: the kept audio stays within the
  transcription service's request limit.
- [001-join-dictated-passages](../001-join-dictated-passages/PLAN.md) is
  planned and also edits `useNoteAudioProcessing.ts` (the response's text
  field). The two plans are independent; whichever runs second adapts to the
  other's shape of that file.

## Architecture

- Existing solutions considered: Audio tools already owns the conversion
  message (`errors` in `NoteAudioTools.vue`) and the "convert now" action
  (Flush); the audio buffer already owns "what is not yet converted"
  (`hasUnprocessedData`, the processed position). This plan extends those
  owners. No retry helper exists elsewhere in the frontend, and none is
  added: recovery is the existing conversion run again on audio that the
  buffer still counts as not converted.
- [One real-service audio test](../../NORTH-STAR.md#one-real-service-audio-test):
  the new journey uses `@usingMockedOpenAiService` and mounted or model
  frontend tests. No real-service scenario is added.
- ADR 0006 (failure handling): the failure is caught for a business outcome,
  keeping the audio and telling the author, not swallowed.

## Outside-in proof

| Promise | Slice | Proof |
| --- | --- | --- |
| A failed conversion keeps its audio; the next conversion sends the earlier and the later speech together | 1 | `frontend/tests/models/audioProcessingScheduler.flush.spec.ts`: real buffer and scheduler, a failing then succeeding callback; the second chunk's file holds both stretches of audio |
| A failure during recording does not stop the recording or later conversions | 1 | Same spec: after the failure the timed conversion and Flush still call the callback |
| Text already written is not written again after a later failure is recovered | 1 | Same spec: after a success that advanced the position, a failure, then a success, the last chunk starts at the position the first success left |
| A failure at Stop keeps its audio; a new recording's first conversion includes it | 1 | `audioProcessingScheduler.stop.spec.ts`: `stop()` still resolves with the whole recording after a failing callback, and unconverted audio remains |
| A failure leaves the note's saved content unchanged | 2 | `frontend/tests/notes/NoteAudioTools.processing.spec.ts`: a failed response makes no content update |
| A failure shows the plain sentence, styled as an error; it goes away when a later conversion succeeds | 2 | Same spec: exact text and error style after a failed response; absent after a following successful one |
| After Stop with audio not converted, Retry is offered; Retry writes the passage once and removes the message and Retry; the saved body matches | 3 | New scenario in `e2e_test/features/note_creation_and_update/record_live_audio.feature` (first key example) |
| Retry that fails again leaves the body, the message, and Retry in place; a later Retry adds the passage once | 3 | `audioProcessingScheduler.stop.spec.ts` for the audio (two failing final conversions, then a success, send the same audio and convert it once) and `NoteAudioTools.recording.spec.ts` for Retry staying visible |
| Retry is not shown while recording or when nothing remains to convert | 3 | `NoteAudioTools.recording.spec.ts` |
| Documentation describes failure and recovery | 1, 3 | `docs/voice-input.md`, "Adding dictated text to a note" and the "Observation boundary" sentence that calls failure recovery unassessed |

Focused commands (from `.agents/agent-map.md`; `env -u NODE_ENV` because the
owner's shell sets `NODE_ENV=production`, which breaks these specs locally):

- `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/models/audioProcessingScheduler.flush.spec.ts`
  (likewise `tests/models/audioProcessingScheduler.stop.spec.ts`,
  `tests/models/audioBuffer.spec.ts`, and the `tests/notes/NoteAudioTools.*.spec.ts`
  files a slice touches)
- `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/record_live_audio.feature`
  in slice 3 only, because that slice adds the scenario.

## Decisive premises

| Premise | Consumed by | Observation | Result |
| --- | --- | --- | --- |
| Today a failed conversion marks its audio as converted | Slice 1 changes this | Throwaway spec on 2026-10-05 at `9e74df4a28`: real `createAudioBuffer`, callback resolving `undefined` on a 1 s chunk, then a second 1 s chunk → `hasUnprocessedData()` false after the first call; the two files sent were 32044 bytes each, so the second held only the new second | Holds: the symptom is reproduced |
| Today a failure shows `Error: Failed to process audio` in an info-styled box, with no toast, and resolves `undefined` | Slice 2 replaces the message | Same throwaway spec: mounted `NoteAudioTools`, `audioToText` resolving an SDK error → alert text `Error: Failed to process audio`, classes `daisy-alert daisy-alert-info`, no toast element, result `undefined` | Holds |
| A callback result of `undefined` cannot mean failure, because success also yields no usable timestamp | Slice 1 needs a separate failure signal | Read `SRTProcessor.process`: Stop returns `""` when the last segment has no timestamp line; read `rawSampleAudioBuffer.ts:119-127`: a falsy or unparsable timestamp advances to the end of the sent audio; existing specs (`audioProcessingScheduler.stop.spec.ts:13`, `audioBuffer.spec.ts:82`) rely on that | Holds: failure is signalled by the callback rejecting |
| The buffer does not move its position when the callback rejects | Slice 1 | Read `rawSampleAudioBuffer.ts:108-148`: both position writes come after `await processorCallback(...)` | Holds |
| A rejecting conversion would escape unhandled from the timed and pause conversions | Slice 1 puts the catch in the scheduler | Read `audioProcessingScheduler.ts`: the timer and the silence callback call `processAndCallback` / `tryFlush` without awaiting or catching; `processAndCallback` has `try/finally` only | Holds |
| The audio buffer outlives Stop and is reused by the next recording | Slices 1 and 3 (Retry, and a new recording picking up kept audio) | Read `rawSampleReceiver.ts`: one `createAudioBuffer` per receiver, never reset; `audioRecorder.ts`: one receiver per recorder; `NoteAudioTools.vue`: one recorder per mounted component | Holds |
| The mounted `NoteAudioTools` specs replace the whole recorder, so they cannot prove that audio is kept | Proof placement: audio in model specs, message and Retry visibility in mounted specs, the joined journey in the mocked E2E | Read `frontend/tests/notes/noteAudioToolsMocks.ts` `audioRecorderMockExports` and the `vi.mock("@/models/audio/audioRecorder")` in each spec | Holds |
| The conversion request shows no global toast | Slice 2 owns the only message | Read `useNoteAudioProcessing.ts`: it calls `AiAudioController.audioToText` directly, not through `apiCallWithLoading`; confirmed by the throwaway spec (no toast) | Holds |
| The mocked E2E can make the transcription fail and then succeed | Slice 3 scenario | Read `e2e_test/support/ServiceMocker.ts`: `stubPosterWithError500Response` adds a POST 500 stub and `replaceStubAt` replaces one stub in place; `openAiService.stubTranscription` adds the success stub; read `useNoteAudioProcessing.ts`: any error answer from the backend counts as failure. Read only, not run: the existing feature's Background adds the success stub first, and the first matching stub answers, so the new scenario must install the failure before the success | Holds by reading; the scenario's first run in slice 3 is the first execution |
| `stopRecording()` in the E2E page object still completes after a failed final conversion | Slice 3 scenario | Read `audioToolsPage.ts`: it waits for Save Audio Locally to be enabled; read `NoteAudioTools.vue`: that needs `audioFile`, set from `scheduler.stop()`; slice 1 keeps `stop()` resolving with the file after a failure | Holds once slice 1 is done |

## Slices

### 1. A failed conversion keeps its audio for the next conversion
Type: Behavior
Status: done
Accepted proof: `audioProcessingScheduler.flush.spec.ts` "when a conversion
fails" (three tests: sent file sizes 1 s then 2 s; timed and Flush still call
after a failure; last file 3 s after success, failure, success) and
`audioProcessingScheduler.stop.spec.ts` "keeps the audio of a failed
conversion at Stop for the next recording" and "stops with the whole
recording when a conversion in progress fails"; `NoteAudioTools.*` specs and
`vue-tsc --noEmit` green.
Proof: `audioProcessingScheduler.flush.spec.ts` and
`audioProcessingScheduler.stop.spec.ts` with the real buffer and a callback
that rejects, then resolves; existing `audioBuffer.spec.ts`,
`audioProcessingScheduler.*.spec.ts`, and `NoteAudioTools.*.spec.ts` stay
green.

Behavior: a recording has speech not yet converted → a conversion fails → that
audio is still not converted and recording goes on; the next conversion
(timed, pause, Flush, or Stop) sends the earlier and the later audio together,
and a position already advanced by an earlier success is not moved back. A
failure at Stop still ends the recording and still yields the whole recording
for Save Audio Locally; the kept audio goes out with the first conversion of
the next recording in the same Audio tools.

The conversion callback rejects on failure instead of resolving `undefined`;
the scheduler catches that rejection so that timed and pause conversions do
not leave it unhandled. `useNoteAudioProcessing` keeps setting its message
before it rejects (slice 2 rewrites the message). Update
`docs/voice-input.md`, "Adding dictated text to a note": a failed conversion
keeps its audio, which is sent again with the next conversion.

Interim: after a failure at Stop the author has no control to convert the kept
audio short of recording again; slice 3 adds Retry.

### 2. A failed conversion tells the author in plain words
Type: Behavior
Status: done
Accepted proof: `NoteAudioTools.processing.spec.ts` "tells the author in
plain words when a conversion fails, until one succeeds" (exact text,
`daisy-alert-error`, no `updateNoteContent` call, store content unchanged;
alert absent after the next success); `NoteAudioTools.*` specs and
`vue-tsc --noEmit` green. The full-screen view is not covered by a test.
Proof: `NoteAudioTools.processing.spec.ts` — after a failed response the alert
reads exactly "Could not turn your speech into text. Your recording is kept.",
is styled as an error, and no content update was sent; after a following
successful response the alert is gone.

Behavior: Audio tools is open → a conversion fails → the sentence above is
shown in the existing message place with error styling, and the note's saved
content is unchanged → a later conversion succeeds → the message is gone.

The full-screen view under Advanced Options shows the same sentence, since it
shows the same message. Other messages in that place (start recording, device
switch) keep their current text and are outside this slice.

### 3. Retry converts what is left after Stop
Type: Behavior
Status: done
Accepted proof: `record_live_audio.feature` "Retry converts the kept
recording after the transcription failed" (mocked, passing);
`audioProcessingScheduler.stop.spec.ts` "converts what is left once after
repeated failing final conversions"; `NoteAudioTools.retry.spec.ts` (Retry
offered after Stop, not while recording, not when nothing remains, kept after
a failed Retry); `vue-tsc --noEmit` green.
Proof: new scenario in `record_live_audio.feature`; `NoteAudioTools.recording.spec.ts`
for when Retry is shown; `audioProcessingScheduler.stop.spec.ts` for repeated
failing final conversions.

Behavior: recording has stopped and audio is still not converted → Audio tools
shows Retry beside the message → the author presses Retry → everything not yet
converted is converted as a final conversion (nothing held back) and joined to
the note once; on success the message and Retry go away; on failure the body
is unchanged and the message and Retry stay. Retry is not shown while
recording or when nothing remains to convert.

E2E scenario (mocked OpenAI, `@mockBrowserTime` as the existing scenario):
note body `This is class 1.`; the transcription service fails; record
`lecture.wav`, stop → the page content is still `This is class 1.`, the
message is shown, Retry is offered; the transcription service returns the
`its talk about dada struct day.` transcript; press Retry → page content and
saved content are `This is class 1. its talk about dada struct day.`, and the
message and Retry are gone. Install the failure stub before any success stub
for this scenario (the feature's Background stubs success first today), and
replace it in place for recovery.

Update `docs/voice-input.md`: Retry in the list of Audio tools actions and in
"Adding dictated text to a note"; replace the "Observation boundary" wording
that calls service-failure recovery unassessed with what is now covered by
tests, keeping real-service failure as unobserved.

## Current decisions

- Failure is signalled by the conversion callback rejecting; a resolved
  `undefined` or unparsable timestamp keeps meaning "all sent audio is
  converted".
- The buffer's processed position is the only record of what is kept; no
  separate store of failed chunks.
- Retry is a final conversion of whatever the buffer still counts as not
  converted; it is not a re-send of one remembered chunk.
- Message wording: "Could not turn your speech into text. Your recording is
  kept."

## Learnings

- Only a failed conversion rejects. A save failure in `appendDictatedText`
  after a good conversion keeps its earlier behavior (message set, resolves
  `undefined`, audio counted as converted), so text is never joined twice.
- The scheduler's `processing` promise never rejects: the catch sits on the
  promise itself, so `stop()` waiting on a conversion in progress also
  survives a failure. Slice 3's Retry goes through the scheduler's existing
  final conversion, not around it.
- The conversion failure is held as `errors.conversion`; a success clears
  only that key, so start-recording and device-switch messages stay. Retry's
  visibility can key on the same field plus the buffer's unconverted audio.
- Retry reuses the Stop handler: Stop's final conversion is exactly "convert
  everything that remains", and running it again when not recording is
  harmless.
- E2E stack startup runs a Biome format check, so an unformatted frontend
  file stops `cy:run` before any scenario runs.

## Execution complete

Product advice: Failed transcriptions now keep their audio, show a plain
error message, and offer Retry after Stop. Model tests, mounted tests, and
one mocked E2E scenario prove this. The owner has not yet reviewed the Retry
interaction or the message wording; review those first, and let
SEED-066#understandable-first-dictation decide where Retry sits among the
Audio tools controls. Still unobserved and not queued: a failure of the real
transcription service, and whether a long outage makes the kept audio larger
than the service accepts. Before
[001-join-dictated-passages](../001-join-dictated-passages/PLAN.md) runs,
refresh its premise about `useNoteAudioProcessing.ts`. That file now has a
nested try for saving and rethrows when a conversion fails. Optional small
cleanups, not queued:
- make Retry's visibility reactive;
- narrow the scheduler's catch to conversion failures;
- drop the call-count check in the retry spec.
