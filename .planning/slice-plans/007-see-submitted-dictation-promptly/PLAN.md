# See submitted dictation promptly

## Source

- Story: [See submitted dictation promptly](../../seeds/SEED-066-voice-input.md#prompt-dictation-results)
- Identity: SEED-066#prompt-dictation-results
- Owner decisions, 2026-10-05: the target wait, and leaving text-while-speaking
  out of this story.

## Goal and scope

A note author who clicks Stop after a short spoken thought sees the complete
passage in the note body soon afterwards.

- Target: for up to about 20 seconds of speech with no pause long enough to
  start a conversion, across five runs the middle wait from the Stop click to
  the complete passage being visible is at most 2 seconds and none exceeds 3
  seconds, on local Development with the real transcription service.
- Measure first, then choose the change from where the wait goes.
- If the wait already meets the target, make no product change.
- If no bounded change meets the target, stop and report the breakdown to the
  owner.
- Every preservation behavior in [docs/voice-input.md](../../../docs/voice-input.md)
  stays: the transcription's own text, joined once, unfinished sentence held
  back during speech, nothing revised, failed conversions kept with Retry,
  titles untouched.
- No model step is added between the transcription and the note. A different
  transcription model or setting is allowed only when the measurement shows
  the transcription request is where the wait goes.

Excluded: text appearing while still speaking; the wait after a pause or
Flush; longer dictations; production network conditions; hardware microphone
capture.

Considered and left out:

- A timing assertion in an automated test. The wait depends on a paid external
  service; [NORTH-STAR "One real-service audio test"](../../NORTH-STAR.md#one-real-service-audio-test)
  allows no second real-service scenario, and tightening the existing
  feature's "within 20 seconds" would make CI fail on service slowness.
- Choosing the change now. Nothing measured since the model rewrite was
  deleted says where the wait goes; slice 1 settles it.
- Measuring the orchard passage's timing. It has a pause and a Flush, which
  are outside the target; it is kept only as a preservation check.

## Architecture

- Existing solutions reused (PFE):
  - The whole dictation path stays as it is: `NoteAudioTools.vue` →
    `audioRecorder` → `audioProcessingScheduler.stop()` →
    `rawSampleAudioBuffer.processUnprocessedData` → `useNoteAudioProcessing`
    → `POST /api/audio/audio-to-text` (`AiAudioController` →
    `OpenAiApiHandler.getTranscription`, `whisper-1`, SRT → `SRTProcessor`)
    → `noteStore.appendDictatedText` → content PATCH. Slice 2 changes the one
    step slice 1 shows to be slow; it adds no parallel path.
  - Measurement reuses the 2026-10-03 method recorded in
    [docs/voice-input.md "Observation boundary"](../../../docs/voice-input.md#observation-boundary):
    naturally paced audio enters a synthetic browser MediaStream
    (AudioContext → MediaStreamDestination) so the real recorder, worklet,
    transcription and saving run. The input is `e2e_test/fixtures/harvard.wav`.
  - Branch code is observed against real services through
    `pnpm e2e:hold --paid-openai`
    ([docs/worktree-browser-tests.md](../../../docs/worktree-browser-tests.md));
    a linked worktree cannot run Development.
- No new consequential architectural choice. No Accepted ADR is affected.
  The North Star topic above governs proof: new audio behavior is proved with
  mocked services or mounted frontend tests.

## Outside-in proof

| Promise | Slice | Proof |
| --- | --- | --- |
| The current wait and where it goes are known | 1 | Five Harvard runs and five lighthouse runs on the held stack with the real transcription service; table of Stop → request start, request duration, request end → passage visible, and total, recorded in this plan |
| Short thought: Harvard passage visible within target after Stop (examples 1, 3) | 2, then 3 | Slice 2: five runs on the held stack with branch code, middle ≤ 2 s, none > 3 s. Slice 3: the same five runs on Development after landing |
| Very short addition: lighthouse visible within 2 s (example 2) | 2, then 3 | Same runs with the lighthouse recording |
| Original paragraph and passage each appear once after reload (example 1) | 2, 3 | Reload after one Harvard run in each measurement session |
| Orchard passage with pause and Flush writes the documented three passages (example 4) | 2 | Existing mounted and model tests stay green (`NoteAudioTools.preservation`, `audioBuffer`, `audioProcessingScheduler.flush`/`.stop`); one orchard run on the held stack with branch code, compared with the passages in docs/voice-input.md |
| Failure at Stop keeps the body, shows the message and Retry, Retry joins once (example 5) | 2 | Existing `NoteAudioTools.retry.spec.ts` and the Retry scenario in `record_live_audio.feature` stay green |
| The measured result is in the voice-input documentation | 3 | Reading docs/voice-input.md "Responsiveness and positive comparisons" |

Commands (from the story worktree root):

- Frontend audio tests:
  `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/notes/NoteAudioTools tests/models/audio`
- Backend audio tests, when backend code changes:
  `CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test -Dspring.profiles.active=test --build-cache --tests '*AiAudioControllerTests' --tests '*SRTProcessorTests'`
- Mocked journey:
  `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/record_live_audio.feature`
- Held stack with real services (owner's authorization each time):
  `CURSOR_DEV=true nix develop -c pnpm e2e:hold --paid-openai`
- The real-service feature runs in CI after a push; it is not a local gate.

### How a run is measured

In the browser page of the stack under measurement, signed in, on a note with
one saved paragraph, Audio tools open:

1. Replace `navigator.mediaDevices.getUserMedia` with a function returning the
   stream of a `MediaStreamDestination` fed by the decoded recording, played
   once at natural pace.
2. Click Record Audio; when the recording has played to its end, click Stop
   Recording and note `performance.now()` at the click.
3. Note the time the complete passage is visible in the body.
4. Read the `audio-to-text` request's start and end from the page's resource
   timing entries.

A run counts only when exactly one `audio-to-text` request was sent, after
Stop. The lighthouse recording is the sentence pair in docs/voice-input.md
("The lighthouse keeper … to the beach."), synthesized at 150 words/minute;
record its actual duration with the results.

## Decisive premises

| Premise | Consumed by | Observation | Result |
| --- | --- | --- | --- |
| For a dictation under 60 s with no 3 s pause, all audio is converted in one request after Stop | "A run counts only when…"; the Stop wait is one request plus saving | Read 2026-10-05: `audioProcessingScheduler.ts` (`PROCESSOR_INTERVAL = 60 * 1000`; `stop()` awaits any running conversion, then `processAndCallback(false)`), `rawSampleAudioBuffer.ts` (`SILENCE_DURATION_THRESHOLD = 3 * sampleRate`) | Holds |
| The upload is uncompressed 16 kHz mono 16-bit WAV, so 20 s is about 640 kB | Candidate "upload" in slice 2 | Read `rawSampleReceiver.ts` (`SAMPLE_RATE = 16000`) and `createAudioFile.ts` (PCM header, 2 bytes per sample) | Holds |
| The server makes one `whisper-1` SRT request and no other model call | The request duration is the transcription | Read `AiAudioController.audioToText` and `OpenAiApiHandler.getTranscription` | Holds |
| Mid-speech hold-back needs segment end times from the transcription | Any transcription change in slice 2 must keep them for mid-speech requests | Read `AiAudioController` (`SRTProcessor.process(…, isMidSpeech)`, `endTimestamp`) and `rawSampleAudioBuffer.processUnprocessedData` (advances by the returned timestamp) | Holds |
| The Harvard recording exists and is 18.356 s | Measurement input | `afinfo e2e_test/fixtures/harvard.wav`: 18.356190 s, 44.1 kHz stereo Int16 | Holds |
| No lighthouse recording is in the repository | Slice 1 synthesizes it | `find` for `*lighthouse*` outside `node_modules`: nothing | Holds |
| The primary checkout's Development does not run the current audio code today | Baseline is measured on the held stack from the story worktree, not on Development | Primary `HEAD` is `77df78ed15`, an ancestor of this branch; `git diff --stat 77df78ed15 HEAD` shows changes under `frontend/src/models/audio` | Holds |
| The audio preservation, retry and scheduler tests exist | Slice 2's preserved-behavior proof | Listed `frontend/tests/notes/NoteAudioTools.*.spec.ts`, `frontend/tests/models/audio*.spec.ts`, `record_live_audio.feature`, `AiAudioControllerTests`, `SRTProcessorTests`. Not run during planning | Present; slice 2 runs them before changing code |
| A held stack with `--paid-openai` reaches the real transcription service, and the synthetic MediaStream method works on it | Slices 1 and 2 | Not observable without a paid call. Cheap parts observed: the command and flag are documented in docs/worktree-browser-tests.md and `.agents/agent-map.md`; `OPENAI_API_TOKEN` is set in the owner's shell; the method worked on Development on 2026-10-03 | Slice 1 is the probe |
| Where the wait after Stop goes, and whether it already meets the target | Slice 2's change | Only a paid observation settles it. The 2026-10-03 timings predate deleting the model rewrite | Slice 1 is the probe |

## Current decisions

- Baseline and after-change runs both use the held stack from the story
  worktree, so they are comparable. The accepted target is defined on local
  Development; slice 3 confirms it there after landing.
- Every paid run (held stack with `--paid-openai`, and Development in slice 3)
  waits for the owner's go-ahead.
- "Middle wait" is the median of the five runs.
- Slice 1's result decides slice 2:
  - Target already met on the held stack → slice 2 is removed from this plan
    and slice 3 follows.
  - Target missed → slice 2's change is written into this plan from the
    breakdown before any product code changes, picking one candidate:
    - most of the wait is before the request starts → remove that wait;
    - most is the request → find whether upload or the transcription call
      dominates (backend log timing around `getTranscription`), then either
      send less data or change the transcription request, keeping segment end
      times for mid-speech requests;
    - most is after the response → make the join and save faster.
  - No bounded change can meet the target → stop and report the breakdown to
    the owner; slices 2 and 3 do not run.
- No timing value is asserted in an automated test.

## Slices

### 1. The current wait after Stop and where it goes are measured
Type: Behavior
Status: done — measured 2026-10-06 with the owner's go-ahead
Proof: the results table below this slice, filled from five Harvard and five lighthouse runs.

Behavior: the held stack runs this branch's code with the real transcription
service → an author records the Harvard passage (and, separately, the
lighthouse addition) and clicks Stop, five times each → for every run the plan
records the wait from Stop to the complete passage, split into Stop → request
start, request duration, and request end → passage visible; and states the
median, the slowest run, and whether the target is met.

This is the probe for the last two premises. If the held stack cannot reach
the transcription service, or the synthetic MediaStream method does not work
on it, stop and change the plan before slice 2. No product code changes here.

#### Slice 1 results

Held stack (`pnpm e2e:hold --paid-openai`) from this worktree at
`52dff779b9`, real `whisper-1`, headless Chromium (Playwright) with the
synthetic MediaStream method. Stop was clicked about 250 ms after the
recording ended. "Visible" is the last change to the note body after Stop.
Every run sent exactly one `audio-to-text` request, after Stop. Times in ms.

| Recording | Run | Stop → request start | Request | Request end → visible | Total |
| --- | --- | --- | --- | --- | --- |
| Harvard 18.356 s | 1 | 5 | 2456 | 46 | 2506 |
| Harvard | 2 | 4 | 3045 | 29 | 3077 |
| Harvard | 3 | 4 | 3583 | 31 | 3618 |
| Harvard | 4 | 7 | 2189 | 35 | 2232 |
| Harvard | 5 | 6 | 2557 | 38 | 2600 |
| Lighthouse 6.283 s | 1 | 5 | 2728 | 62 | 2794 |
| Lighthouse | 2 | 3 | 2940 | 29 | 2971 |
| Lighthouse | 3 | 2 | 1786 | 26 | 1815 |
| Lighthouse | 4 | 2 | 1880 | 25 | 1908 |
| Lighthouse | 5 | 3 | 1936 | 28 | 1967 |

- Harvard: median 2600, slowest 3618 — target missed.
- Lighthouse: median 1967, slowest 2971 — target met, narrowly.
- About 98% of the wait is the `audio-to-text` request. Before it: under
  10 ms. After it (join, render): under 70 ms.
- Upload is not the cost: macOS `networkQuality` measured 75 Mbps uplink, so
  the ~590 kB WAV takes about 60 ms to send to OpenAI. The time is the
  `whisper-1` transcription call itself.
- Reload after Harvard run 1 showed "This is class 1." and the passage once
  each.
- The lighthouse recording was synthesized with `say -r 150`, converted to
  mono 48 kHz WAV, 6.282667 s.

### 2. A short dictation is visible within the target after Stop
Type: Behavior
Status: done — target met on the held stack, 2026-10-06

Change: the conversion made at Stop (and Retry, which runs the same Stop
conversion; `midSpeech=false`) asks `gpt-4o-mini-transcribe` for plain text
instead of `whisper-1` SRT. The returned text, stripped, is the one segment
appended. That request returns no end timestamp, so the frontend treats all
sent audio as converted, as it already does when no timestamp comes back.
Mid-speech conversions (the 60 s timer and Flush; `midSpeech=true`) keep
`whisper-1` SRT and hold-back unchanged. The mocked OpenAI service answers
the Stop request with plain text, so `record_live_audio.feature` keeps its
expected note content.

Probe (direct calls to OpenAI with the 16 kHz mono Harvard WAV, 591 494
bytes, from this machine; seconds, five calls each):

| Request | Calls | Median | Slowest |
| --- | --- | --- | --- |
| `whisper-1`, SRT (today) | 2.56, 1.43, 1.36, 2.35, 2.68 | 2.35 | 2.68 |
| `gpt-4o-mini-transcribe`, text | 1.37, 1.38, 1.01, 1.14, 0.97 | 1.14 | 1.38 |
| `gpt-transcribe`, text | 1.53, 1.07, 1.08, 1.10, 1.04 | 1.08 | 1.53 |

All fifteen returned the same six Harvard sentences word for word.
`gpt-4o-mini-transcribe` was chosen over `gpt-transcribe`: the same speed in
this probe, and the lower price.

#### Slice 2 results

Held stack from this worktree with the slice 2 change (uncommitted, on
`99962fbf99`), the same method as slice 1, owner's go-ahead 2026-10-06. Times in ms;
every run sent exactly one `audio-to-text` request, after Stop.

| Recording | Stop → request start | Request | Request end → visible | Total |
| --- | --- | --- | --- | --- |
| Harvard run 1 | 5 | 2284 | 88 | 2377 |
| Harvard run 2 | 23 | 1366 | 84 | 1473 |
| Harvard run 3 | 4 | 1138 | 42 | 1185 |
| Harvard run 4 | 4 | 1319 | 53 | 1376 |
| Harvard run 5 | 5 | 1129 | 35 | 1169 |
| Lighthouse run 1 | 3 | 603 | 51 | 657 |
| Lighthouse run 2 | 2 | 618 | 41 | 662 |
| Lighthouse run 3 | 3 | 853 | 32 | 887 |
| Lighthouse run 4 | 3 | 909 | 33 | 945 |
| Lighthouse run 5 | 4 | 980 | 24 | 1007 |

- Harvard: median 1376, slowest 2377 — target met.
- Lighthouse: median 887, slowest 1007 — target met.
- Reload after run 1 of each showed "This is class 1." and the passage once.
- Orchard (synthesized at 150 words/minute, 8 s pause after "yesterday,",
  29.21 s; Flush 22 s after Record; Stop after the end): three requests. The
  pause wrote "The orchard contains apple trees, peach trees, and a small
  wooden bench. These facts are finished."; Flush wrote "The book that I
  bought yesterday after reading several reviews and comparing different
  editions"; Stop wrote "Is a gift for my sister because she enjoys learning
  about the history of gardens. The meeting is on Friday afternoon. We should
  bring a notebook and a pencil.", visible 945 ms after Stop. Nothing was
  revised; reload showed each once. Unlike `whisper-1`, the Stop passage
  starts with a capital ("editions Is a gift").

Behavior: a note with one saved paragraph, and an author who has recorded up
to about 20 seconds of speech without a long pause → Stop → the complete
passage is visible in the body within 2 seconds in the median run and within
3 seconds in every run. After reload the original paragraph and the passage
each appear once. Pause, Flush, hold-back, failure and Retry behave as
docs/voice-input.md describes.

A new or changed rule (for example what is sent, or which transcription
request is made) gets a mounted or controller test for that rule, with mocked
services. If the chosen change does not reach the target, record the measured
result and return to "Current decisions" rather than stacking a second change
without a new breakdown.

### 3. Development meets the target, and the documentation states the measured wait
Type: Behavior
Status: done — measured 2026-10-06 on Development at `b86f649322` (synced to `main` by the owner), owner's go-ahead

Results (ms, total from Stop to visible; every run one request after Stop):
Harvard 1844, 1028, 1855, 1392, 1270 — median 1392, slowest 1855; lighthouse
749, 1090, 1647, 869, 1448 — median 1090, slowest 1647. Target met for both.
Reload after run 1 of each showed the original paragraph and the passage
once. docs/voice-input.md "Responsiveness and positive comparisons" now
states these results, their conditions, the breakdown and the change.
Proof: five Harvard and five lighthouse runs on Development at the landed revision; reading docs/voice-input.md.

Behavior: Development runs the landed revision → the same runs as slice 1 →
median ≤ 2 s and none > 3 s for both recordings, and reload shows the original
paragraph and the passage once. The "Responsiveness and positive comparisons"
section of docs/voice-input.md states the current wait after Stop and its
breakdown, the conditions of the measurement, and what changed, replacing the
rows that describe the wait before the model rewrite was deleted.

If Development misses the target while the held stack met it, record both
results and report to the owner; do not change code under this slice.

## Learnings

- The synthetic MediaStream method works in headless Playwright Chromium
  against the held stack (`--autoplay-policy=no-user-gesture-required`; log
  in by calling `/api/healthcheck` with Basic auth to get a session cookie).
- The Stop wait is almost entirely the `whisper-1` call, and it varies by
  about 1.4 s from run to run for the same audio.
- `gpt-4o-mini-transcribe` capitalizes the first word of a Stop passage that
  continues a sentence held back or flushed earlier. Whether to address it
  (for example with the transcription `prompt`) is an owner decision outside
  this story's target.

## Execution complete

Product advice: The target is met on Development (Harvard median 1.39 s,
slowest 1.86 s; lighthouse 1.09 s, 1.65 s). No correction is needed. One owner
decision goes to story wrap-up: with `gpt-4o-mini-transcribe` at Stop, a
passage that continues a sentence held back or flushed earlier starts with a
capital letter ("…editions Is a gift…"). Accept it, or address it in a
separate story (for example by passing the preceding text as the
transcription `prompt`). Text that appears while speaking stays the wider
ambition, outside this story.
