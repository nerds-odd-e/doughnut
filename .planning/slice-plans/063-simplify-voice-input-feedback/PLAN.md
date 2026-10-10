# Simplify voice input controls and feedback

Work item: **SEED-066#simplify-voice-input-feedback**

**Source:** [story](../../seeds/SEED-066-voice-input.md#simplify-voice-input-feedback)

## Goal and scope

A note author dictating into a note's body sees their words arrive in the
note instead of reading status messages, hears about problems through the
common error toast that the rest of Donut uses, and gets spoken text into the
note at a cadence chosen after measuring feedback latency against API cost.
Save audio and Full screen are gone without trace.

Included: the five slices below. Each removal follows the story's removal
scope and principle 7 in `AGENTS.md`: delete the thing and everything only it
used, sweep the product, keep no absence check.

Excluded (see the story): the single-button interaction and removing the
panel (next story), Speak the Title changes (third story), a recovery action
inside the toast, a message when no speech was recognized, suppressing
repeated mid-speech failure toasts, automatic retry, language translation,
any layout or component choice for the surviving panel, backend or generated
API changes unless the cadence decision needs one.

Assumptions: the surviving panel keeps the waveform, microphone selector,
Record, Stop, Write text now and Retry; the common toast is the
`vue-toastification` error toast reached through `useToast`, as the shared
API client and `NotebookSettings` use it.

## Known architecture

- **Panel.** `NoteAudioTools.vue` owns the phases `ready | recording |
  stopping | added | nothingAdded | notConverted | micUnavailable`, one
  `role="status"` line keyed by phase, an inline alert for `errors`, Retry when
  `notConverted`, Save audio (`saveAudioLocally.ts`) and `FullScreen.vue`.
  `useNoteAudioProcessing.ts` converts chunks, appends through
  `noteStore.appendDictatedText`, and tracks `passageSaves` solely so
  `writtenResult()` can choose between the added, nothing-added and
  not-saved messages. `SpeakTitleControl.vue` has its own phases and status
  and does not change here.
- **Failures today.** `audioChunkToText` calls `AiAudioController.audioToText`
  on the SDK directly, outside `apiCallWithLoading`, so a failed transcription
  reaches no toast; the composable turns it into `errors.conversion`. Body
  saves go through `updateTextContentRequest`, which uses
  `apiCallWithLoading`, so a failed save already shows the common toast and
  the panel's raw `{{ errors }}` alert duplicates it. Microphone and device
  failures are inline only.
- **Cadence today.** `audioProcessingScheduler.ts`: 60-second timer,
  pause flush when `rawSampleAudioBuffer.ts` sees about 3 s of silence, Write
  text now, and a final conversion at Stop. Mid-speech chunks go to
  `whisper-1` as SRT; `SRTProcessor` writes every segment but the last and
  returns that last written end timestamp, so the held-back last segment's
  audio is sent again with the next chunk. Stop uses
  `gpt-4o-mini-transcribe` plain text.
- **Proof entry points.** Mounted specs `frontend/tests/notes/NoteAudioTools.*.spec.ts`
  with `noteAudioToolsTestSupport.ts`; toasts observed on the page through
  `frontend/tests/helpers/toastTestSupport.ts` (`showToastsOnPage`,
  `toastShown("error")`). E2E `record_live_audio.feature` with
  `audioToolsPage.ts`; the step `I should see an error toast containing
  {string}` already exists in `note_rich_content.ts`. North Star "One
  real-service audio test": no new real-service scenario.
- **Documentation.** `docs/voice-input.md` describes the panel, its status
  wording, Save audio, Full screen, Retry and the cadence; each slice updates
  the paragraphs it changes.

## Premises observed (2026-10-10, this worktree at `fabe47cb03` plus the refined seed)

- `FullScreen.vue` is used only by `NoteAudioTools.vue` and
  `frontend/tests/common/FullScreen.spec.ts`; `saveAudioLocally.ts` only by
  the panel; `mockCreateObjectURL`/`mockRevokeObjectURL` in
  `noteAudioToolsMocks.ts` only by the Save audio tests in
  `NoteAudioTools.recording.spec.ts` (`grep -rln` over `frontend/src`,
  `frontend/tests`, `e2e_test`, `docs`).
- Failed body save already toasts: `noteRequests.ts` wraps
  `updateNoteContent` in `apiCallWithLoading`, whose `handleSdkError` shows
  the error toast (read; the existing spec "keeps appended content after an
  API error" exercises the path without asserting the toast). The open-editor
  save path (`changeDraft` in `TextContentWrapper.vue`) also saves through the
  store. Slice 3 adds no toast for saves; it only deletes the duplicate.
- Pricing (OpenAI pricing page fetched 2026-10-10): Whisper $0.006/min,
  gpt-4o-mini-transcribe $0.003/min, gpt-4o-transcribe $0.006/min, live
  transcription (gpt-realtime-whisper, gpt-live-transcribe) $0.017/min.
  Whether only `whisper-1` returns SRT timestamps is a third-party claim the
  probe confirms before any model change.
- Baseline: the nine `NoteAudioTools` specs pass (73 tests, 9 s) with
  `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteAudioTools`.
  `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/record_live_audio.feature`
  passes in this worktree: 5 scenarios, 19 s, exit 0 (2026-10-10).
- Not observed: request latency of `whisper-1` SRT per chunk length and the
  held-back segment's typical length. Both need the real service (paid,
  owner-authorized); slice 4 is the probe.

## Outside-in proof

| Promise | Proof | Slice |
| --- | --- | --- |
| Panel has no Save audio or Full screen | recording spec lists the idle and recording controls; sweep reading | 1 |
| No status line in routine operation; state perceivable from controls; Record unavailable until Stop's text is added | recording spec; E2E scenario 1 asserts content, no status | 2 |
| Silent recording ends quietly | mounted spec: Stop after a no-segment response leaves idle controls and no toast | 2 |
| Microphone refused, mid-speech failure, Stop failure, device switch failure → common error toast | retry/processing specs with `toastShown("error")`; E2E scenario 2 asserts the toast | 3 |
| Retry is a control shown only while a kept recording awaits conversion; Retry converts it | retry spec; E2E scenario 2 | 3 |
| Failed save shows no raw error in the panel | preservation spec "keeps appended content after an API error" asserts no alert | 3 |
| Closing Audio tools while recording converts the rest | recording spec "stops recording when unmounted while recording" (unchanged) | 2 |
| Cadence and cost recorded before implementation | measurement table and decision in this plan | 4 |
| Text appears at the chosen cadence; completed text and existing content preserved | scheduler specs for the new interval; existing preservation specs and E2E green | 5 |

## Ordered slices

### 1. The panel offers only recording controls
Type: Behavior
Status: done
Proof: `NoteAudioTools.recording.spec.ts`: idle shows Record only; recording
shows Stop, Write text now and the microphone selector; no button named Save
audio or Full screen anywhere in the spec. Delete
`NoteAudioTools.fullScreen.spec.ts`, `frontend/tests/common/FullScreen.spec.ts`,
the two Save audio tests and the object-URL mocks. Sweep reading: a search for
`FullScreen`, `saveAudioLocally`, `Save audio`, `Full screen` over the product
returns nothing.

Behavior: the author opens Audio tools → the panel has the waveform and
Record; while recording it has Stop, Write text now and the selector. Delete
`FullScreen.vue`, `saveAudioLocally.ts`, the `audioFile` ref and the
`secondary-actions` styles; update `docs/voice-input.md`. About 5 min.
Stop-safe: the panel still records and converts as today.

### 2. Routine dictation shows no status; state lives in the controls
Type: Behavior
Status: done
Proof: `NoteAudioTools.recording.spec.ts`: no `role="status"` element; Record
is disabled from Stop until the final text has been added, then enabled (the
existing "tells the result of its recording when Record is pressed while Stop
finishes" becomes this assertion); a Stop whose conversion returned no
segments leaves the idle controls and no toast. Delete
`NoteAudioTools.status.spec.ts`, `NoteAudioTools.savedStatus.spec.ts` and
`dictationStatus`. E2E
`record_live_audio.feature` scenario 1: drop "I should be told my speech was
added to my note" (both scenarios); `audioToolsPage.stopRecording` waits for
Record to be enabled instead of reading the status text; the content
assertions remain the proof.

Behavior: Record → Stop appear as today with no words; Stop → Record is
unavailable until the dictated text is in the note, then returns; nothing
announces the result. Delete the status line, the `added`/`nothingAdded`
phases and messages, `writtenResult` and `passageSaves` (used only for those
messages), and the `saved` return of `appendDictatedText` if nothing else
reads it; keep `stopping`. Update `docs/voice-input.md`. About 8 min.
Stop-safe: failures still appear inline until slice 3.

### 3. Problems reach the author through the common error toast
Type: Behavior
Status: done
Proof: `NoteAudioTools.retry.spec.ts` with `showToastsOnPage()`: a failed Stop
conversion shows an error toast saying the speech could not be turned into
text and the recording is kept until Audio tools closes, Retry appears, Record
is available; Retry converts the kept recording once, the passage joins, and
Retry disappears; a Retry that fails again toasts again and keeps Retry; no
Retry when nothing remains to convert. `NoteAudioTools.processing.spec.ts`: a
failed mid-speech conversion toasts and recording continues without Retry.
`NoteAudioTools.controlsDuringConversion.spec.ts` "returns to Record alone,
adding nothing, when Stop found no speech" runs with `showToastsOnPage()` and observes that
the page holds no toast (obligation G1).
Microphone refused → error toast with the allow-access explanation and Record
available (moved from the deleted status spec). Device switch failure → error
toast. `NoteAudioTools.preservation.spec.ts` "keeps appended content after an
API error" also asserts the panel shows no alert. E2E scenario 2: replace "I
should be told my speech could not be turned into text, with Retry" with the
existing step `I should see an error toast containing "Could not turn your
speech into text"` plus a Retry-visible expectation in `audioToolsPage`; keep
"I retry converting my speech" and the content assertions. Sweep reading: no
`daisy-alert`, `isProblem`, `fullscreen-error` or inline error text remains in
the panel.

Behavior: every failure the panel used to print inline is shown once through
`useToast().showErrorToast` (or the shared composable's equivalent) and the
inline alert and `errors` record go; Retry becomes a plain control bound to
"a kept recording failed to convert at Stop". Update `docs/voice-input.md`
failure paragraphs. About 8 min. Stop-safe: complete feedback redesign
without the cadence change.

### 4. Measure feedback latency against API cost and choose the cadence
Type: Structure
Status: done
Proof: a table in Learnings with, per chunk length (about 10 s, 20 s and 60 s
of speech cut from `e2e_test/fixtures/harvard.wav` or `lecture.wav`), the
median and slowest of three `whisper-1` SRT request times, the number of SRT
segments, and the held-back last segment's length; then, per candidate
cadence, time to first visible text, Stop-to-final text (from the 2026-10-03
measurement, unchanged), conversions per dictated minute, billable seconds
per recorded minute (recorded audio plus the resubmitted tails), and cost per
dictated hour at the prices above. The chosen cadence, its cost estimate and
the reason are recorded under Current decisions before slice 5 starts.

Internal change: none in product code. Method: with the owner's authorization
for paid calls, run `pnpm e2e:hold --paid-openai` from this worktree, sign in
as a seeded account, and post the cut WAV files to `/api/audio/audio-to-text`
with `midSpeech=true` from `curl`, timing each request; the whole run sends
under five minutes of audio (about $0.03). Candidates compared against the
same budget: keep 60 s timer + 3 s pause; shorter timer (15–20 s) + 3 s
pause; pause-only with a shorter silence threshold. Live transcription is
excluded unless bounded chunks cannot put text in the note within about 10 s
of the pause that ends a sentence: it costs 2.8× Whisper and needs a
streaming relay the story does not authorize. Proposed affordable budget for
the owner to confirm at review: billable audio at most 1.5× recorded audio
for mid-speech conversion (at most $0.009 per dictated minute). About 10 min,
of which most is the external wait; an unauthorized or failed run stops
slice 5 and leaves slices 1–3 delivered. Enables slice 5. If the measurement
supports keeping today's cadence, slice 5 is removed and this plan says so.

### 5. Dictated text appears at the chosen cadence
Type: Behavior
Status: planned
Proof: `audioProcessingScheduler.flush.spec.ts` and
`audioProcessingScheduler.convertOnlyAtStop.spec.ts` tick the new interval
(their "60 s tick" wording follows); `audioBuffer.spec.ts` if the silence
threshold changes; the preservation and language-joining specs stay green;
E2E `record_live_audio.feature` scenario 1 keeps the lone segment held until
Stop under the new timer (adjust "it is 2 minutes later" only if the new
interval needs it). `docs/voice-input.md` states the new timing.

Behavior: during sustained speech the body gains completed passages at the
chosen interval or pause; the held-back last segment and written text are
never revised; existing content survives. About 5 min. Depends on slice 4's
recorded decision.

## Story obligations

### G1. Silent Stop shows no toast
Reported: slice 2 — "'No toast' for the silent Stop is not asserted."
Story clause: "panel returns to idle with nothing added and no message"
Disposition: proved by slice 3: frontend/tests/notes/NoteAudioTools.controlsDuringConversion.spec.ts "returns to Record alone, adding nothing, when Stop found no speech" observes `noToastShown()` with toasts on the page

### G2. Passage typed over while a save is in flight is saved by autosave
Reported: slice 2 — "the newer draft holding the passage is now saved by the normal 1-second autosave debounce (or on blur or unmount) instead of immediately"
Story clause: "the text is added and saved through the existing flow"
Disposition: proved by slice 2: frontend/tests/notes/NoteAudioTools.typingWhilePending.spec.ts "keeps typing whose save is still in flight and saves the passage after it once" and "saves the passage as soon as it joins the open editor's draft"

### G3. Repeated mid-speech failures each toast
Reported: slice 3 — "Repeated toasts are only observed for Retry (below); the code has no suppression"
Story clause: "Each failed mid-speech conversion raises its own toast"
Disposition: proved by slice 3: frontend/tests/notes/NoteAudioTools.retry.spec.ts "toasts again and keeps Retry when Retry fails again" observes one toast per failed conversion through the single `convert` path in `useNoteAudioProcessing.ts` that mid-speech conversions also take, and NoteAudioTools.processing.spec.ts "toasts a failed mid-speech conversion and keeps recording without Retry, the body as it was" observes the mid-speech toast

### G4. Failed save toast is observed at the shared client
Reported: slice 3 — "The save toast itself is untested at panel level"
Story clause: "the common error toast reports the failed save; no raw error object is shown in the panel"
Disposition: proved by slice 3: frontend/tests/managedApi/clientSetup.spec.ts "shows error toast for apiCallWithLoading wrapped calls" for the toast the body save raises, and frontend/tests/notes/NoteAudioTools.controlsDuringConversion.spec.ts "offers Record alone again when saving the dictated text fails" for the panel holding only Record

## Current decisions

- Toast wording reuses today's sentences: conversion failure "Could not turn
  your speech into text." with "Your recording is kept until you close Audio
  tools." at Stop and "Your recording is kept." mid-speech; microphone
  "Could not use the microphone. Allow microphone access in your browser,
  then try again."; device "Failed to switch audio device". Default toast
  timeout; no action inside the toast.
- Retry stays a panel button tied to a kept, unconverted recording; closing
  the panel discards it as today.
- No status live region replaces the removed one; the Record/Stop swap and the
  disabled Record during the final conversion carry the state.
- Cadence (2026-10-10, from slice 4's measurement; the owner confirmed the
  budget of billable audio at most 1.5× recorded audio): a 20-second timer
  with the 3-second pause flush, Write text now and the final conversion at
  Stop unchanged; `whisper-1` SRT stays the mid-speech model and the
  hold-back rule stays. Estimated cost during sustained speech: 1.14–1.25×
  recorded audio, $0.41–0.45 per dictated hour, three conversions per
  minute. Reason: it is the shortest timer backed by a direct measurement
  (first text about 23 s into unbroken speech instead of about 66 s) and
  keeps margin under the budget; 15 s was only interpolated and leaves a
  long single-segment sentence less room; pause-only gives no text during
  unbroken speech, and a threshold short enough to fire at every sentence
  bills about 2×. Live transcription is not needed: chunks up to 20 s return
  in 2.6–3.5 s, so text reaches the note about 6 s after a pause.

## Learnings

- Slice 1: the whole-recording file returned at Stop existed only for Save
  audio, so `stopRecording`, the scheduler's `stop` and the buffer no longer
  produce it, and conversion chunks are named `recorded_audio_<timestamp>.wav`.
  `fullscreen-error` went with Full screen, so slice 3's sweep finds it
  already absent. Accepted proof: `NoteAudioTools.recording.spec.ts` "offers
  Record when ready, then Stop, Write text now and the microphone chooser
  while recording, with the status announced" lists every panel button;
  `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteAudioTools tests/models tests/notes/NoteNewForm.spokenTitle tests/notes/NoteEditableTitle.spokenTitle`
  passes. Sweep reading (2026-10-10): `FullScreen`, `saveAudioLocally`,
  `Save audio`, `Full screen` return nothing over the product.
- Slice 2: the confirmed-save machinery (`flushAndConfirmDraftSaved`,
  `persistedVersion`, the `saved` return) existed only to word the result
  message and went with it; an open editor saves the joined draft through
  its ordinary flush. Until slice 3 the Stop-failure and microphone sentences
  show in a `text-error` paragraph with Retry beside it.
  `noteAudioToolsSavedContentTestSupport.ts` stays: the preservation and
  language-joining specs use it. Accepted proof:
  `NoteAudioTools.controlsDuringConversion.spec.ts` "keeps Record unavailable
  from Stop until the last text has been added" and "returns to Record alone,
  adding nothing, when Stop found no speech"; recording spec "offers only
  Record when ready, …"; whole `pnpm frontend:test` (2081 tests) and
  `record_live_audio.feature` (5 scenarios) pass.
- Slice 3: the panel prints nothing; its phases are `ready | recording |
  stopping | notConverted`, and Retry sits in the button row while
  `notConverted`. A refused microphone leaves a pending Retry in place.
  Mounted panel specs cannot observe the failed-save toast (the shared
  client's status handler is not installed there); `clientSetup.spec.ts`
  observes it. The preservation spec's "API error" is a conversion failure;
  the save failure is in `controlsDuringConversion`. Accepted proof: retry,
  processing, recording and controlsDuringConversion specs observe the toast
  text with `toastShown("error")`;
  `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteAudioTools tests/models`
  (124 tests) and `record_live_audio.feature` (5 scenarios, toast and Retry
  in scenario 2) pass. Sweep reading (2026-10-10): `daisy-alert`,
  `isProblem`, `problemByPhase`, `fullscreen-error`, `text-error` return
  nothing in `NoteAudioTools.vue`.
- Slice 4 measurement (2026-10-10, `whisper-1` SRT through
  `/api/audio/audio-to-text` with `midSpeech=true`, three requests per
  length, 270 s submitted, about $0.027; `harvard.wav` looped because no
  fixture holds 60 s of speech; the three responses per length were
  identical):

  | Chunk | Request times (s) | Median | Slowest | SRT segments | End of last written segment | Resubmitted tail |
  | --- | --- | --- | --- | --- | --- | --- |
  | 10 s | 3.29, 2.93, 2.59 | 2.93 | 3.29 | 3 | 7.08 s | 2.92 s |
  | 20 s | 3.51, 3.26, 3.39 | 3.39 | 3.51 | 6 | 15.00 s | 5.00 s |
  | 60 s | 3.74, 7.31, 5.55 | 5.55 | 7.31 | 20 | 59.48 s | 0.52 s |

  The response holds only the written texts and the last written end time,
  so the segment count is written texts plus one and the tail is chunk
  length minus that end time (what the buffer sends again). Candidates, with
  the tail taken as 2.8 s (mean) and 5.0 s (worst) whatever the timer:

  | Candidate | First text in unbroken speech | Conversions per minute | Billable s per recorded minute | Ratio | Cost per dictated hour |
  | --- | --- | --- | --- | --- | --- |
  | 60 s timer + 3 s pause | 65.5 s | 1 | 62.8–65.0 | 1.05–1.08 | $0.38–0.39 |
  | 20 s timer + 3 s pause | 23.4 s | 3 | 68.4–75.0 | 1.14–1.25 | $0.41–0.45 |
  | 15 s timer + 3 s pause | about 18 s (interpolated) | 4 | 71.2–80.0 | 1.19–1.33 | $0.43–0.48 |
  | pause-only, shorter threshold | none until a pause | one per pause | 60 × (1 + tail ÷ speech between pauses) | about 2 at one sentence per pause | about $0.72 |

  Stop-to-final text is about 3.96 s (seed, 2026-10-03 responsiveness
  baseline) under every candidate. Not measured: a 15 s chunk, a chunk ending
  in 3 s of silence, a sentence returned as one segment longer than the
  timer. Read from source, not measured: a chunk with one segment writes
  nothing and is sent again whole with the next one, so during a long silence
  after a pause each timer tick sends the held sentence plus all silence so
  far; this happens today and a 20 s timer makes those ticks three times as
  frequent (five silent minutes: about 900 billable seconds today, about
  2,550 at 20 s).
