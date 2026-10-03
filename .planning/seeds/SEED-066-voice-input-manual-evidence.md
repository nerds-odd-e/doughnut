# Voice-input manual discovery evidence

**Source:** [SEED-066 discovery story](SEED-066-voice-input.md#discover-voice-input-problems).
**Date:** 2026-10-03. Findings are observations, with service and capture limits
explicitly retained. Owner expectations remain in the source epic.

## Recording route and short-speech baseline

**2026-10-03 recording-route observation, 01:34–01:37 UTC:** Chrome on macOS,
local Development at `http://127.0.0.1:5175/`, signed in through
`/users/identify` as the documented `manual` account. The homepage visibly
confirmed “Welcome Manual Tester!” and direct navigation to `/notebooks`
showed the notebook list. This reused the running Development app; no service
was restarted. The preparation's observed backend revision was
`a02dbb2697…`; this browser probe did not independently re-read the revision.

**Recording baseline coverage gap:** The expected entry route was to create a
disposable notebook/note before opening Audio tools (the selected story's
short-speech example). In the dedicated browser tab, activating the visible
“Add New Notebook” control left the notebook list unchanged and opened no
dialog. Accessibility activation, a semantic Playwright click, a screenshot
coordinate click, and keyboard Enter all produced the same visible state.
The DOM reported `disabled: false`, title “New notebook”; the screenshot
showed the ordinary notebook list without a blocking overlay or read-only
notice. The button's center hit target was the button itself. The homepage's
Note link likewise did not navigate through automated activation; direct
navigation to its observed `/notebooks` URL worked. No browser console error
was captured. These are observations of the available automation route, not
proof that a human's click fails or a diagnosis of a product defect.

**Reproduction and boundary:** Open a fresh Chrome tab at the local URL, use
the Development sign-in page, verify the greeting, navigate to `/notebooks`,
then activate “Add New Notebook”. Browser tab `137231145` supplied the visible
state and DOM signals above. The baseline stopped before note creation;
Audio tools, recording/processing feedback, first/final body text, persistence,
and later title updates were not reached. No microphone stream, synthetic
audio harness, audio request, or mocked service response was created. Empty
and existing-body dictation timings and real transcription quality remain
unobserved by this attempt. The successful real-service API request in the
preparation remains separate evidence and does not fill this UI gap.

**Same-app alternative, 01:38–01:42 UTC:** A fresh tab at
`http://localhost:5175/users/identify`, signed in as `manual`, did navigate
through Note and open Add New Notebook. This changes the existing localhost
browser session to the documented manual account. The original `127.0.0.1`
observations remain scoped to that origin. The author created notebook
`23`, “Voice discovery 20261003 baseline”, and note `13726`, initially
`Untitled`, using the visible UI. Its body was visually empty, with the
default `type: Note` property. Audio tools exposed Record Audio, Flush Audio,
Stop Recording, Save Audio Locally, and Advanced Options. A temporary page
capture harness made this a usable recording route; no backend responses,
audio worklets, or clocks were mocked.

**Short-passage baseline:** Both recordings used the same
`e2e_test/fixtures/harvard.wav`, 18.3561875 seconds, decoded at 48 kHz and played
at its natural pace into a synthetic browser MediaStream. Hardware capture,
actual device selection and microphone permission remain gaps. Real
Development transcription/retouch and persistence/title endpoints responded
HTTP 200; the preparation established that these are real external services.
Network timestamps below identify stages, not causes.

| Stage (UTC) | Visually empty body | Existing body from first recording |
| --- | --- | --- |
| Controlled capture begins | 01:40:26.708 | 01:41:05.341 |
| Fixture speech ends (start + duration) | 01:40:45.064 | 01:41:23.697 |
| Audio request begins | 01:40:47.325 | 01:41:25.945 |
| Stop activated | 01:40:47.9 approximately | 01:41:34.7 approximately |
| Audio response | 01:40:51.743 (4.42 s request) | 01:41:30.558 (4.61 s request) |
| Content PATCH completes | 01:40:51.858 | 01:41:30.645 |
| First visible body result | 01:40:51.877 (25.17 s from capture; approximately 3.96 s after Stop) | Body stayed identical; persisted PATCH contains the same passage |
| Final visible title | 01:40:56.033 | 01:41:33.405 (before Stop) |

The first title changed from `Untitled` to “Sensory Notes on Food and Drink”.
The second became “Sensory Impressions of Foods and Drinks”, despite the final
body remaining identical. This reproduces ongoing automatic title changes
(the owner's reported unwanted behavior); it does not decide the later title
policy. This second baseline speaks an identical passage, so its unchanged
body cannot establish preservation of a distinct addition or a duplication
defect. Both results began after the complete fixture, with no intermediate
body updates observed. Recording showed a waveform and an enabled Stop/Flush
control; title settlement occurred separately from body persistence. Reloading
the note confirmed the final body and second title persisted.

The final saved passage was: “The stale smell of old beer lingers. It takes
heat to bring out the odor. A cold dip restores health and zest. A salt pickle
tastes fine with ham. Tacos al pastor are my favorite. A zestful food is the
hot cross bun.” Screenshot tool observations captured the second recording
with waveform at 01:41:19 and persisted final note after reload at 01:41:49.
No standalone screenshot file was created. Exact timing comes from a temporary
DOM MutationObserver and CDP Network events, rather than screenshot timestamps.

**Literal temporary capture setup:** Through the documented tab CDP
capability, send `Runtime.evaluate` with `awaitPromise: true`,
`returnByValue: true`, and the following expression; then activate Record
Audio through the UI. The fixture fetch is same-origin Vite access to the
primary checkout's existing file. The fixture was not uploaded through a
separate endpoint.

```javascript
(async () => {
  const ctx = new AudioContext()
  const r = await fetch('/@fs/Users/terryyin/git/doughnut/e2e_test/fixtures/harvard.wav')
  if (!r.ok) throw new Error('fixture HTTP ' + r.status)
  const audio = await ctx.decodeAudioData(await r.arrayBuffer())
  window.__voiceDiscovery = {
    ctx, audio, originalGet: navigator.mediaDevices.getUserMedia,
    originalEnum: navigator.mediaDevices.enumerateDevices,
    streams: [], sources: [], events: []
  }
  navigator.mediaDevices.getUserMedia = async () => {
    const h = window.__voiceDiscovery
    await h.ctx.resume()
    const dest = h.ctx.createMediaStreamDestination()
    const src = h.ctx.createBufferSource()
    src.buffer = h.audio
    src.connect(dest)
    src.start()
    h.sources.push(src)
    h.streams.push(dest.stream)
    h.events.push({ captureStart: new Date().toISOString(), duration: h.audio.duration })
    return dest.stream
  }
  navigator.mediaDevices.enumerateDevices = async () => [{
    kind: 'audioinput', deviceId: 'synthetic-harvard',
    label: 'Controlled Harvard fixture', groupId: 'synthetic'
  }]
  return { fixtureStatus: r.status, duration: audio.duration, sampleRate: audio.sampleRate }
})()
```

At 01:41:46 the harness restored both media-device methods, disconnected its
observer, stopped sources and every synthetic stream track, closed its
AudioContext, and deleted its page variable. Reload cleared all temporary page
state and verified persistence; the owned tab was then closed. No harness file
or product-code change remains. The disposable notebook/note remain useful
for subsequent observation. Slice 1 spent approximately nine minutes including
the initial failed origin route and cleanup, within its audio-setup exception.

[Sustained speech and revision evidence](SEED-066-voice-input-sustained-evidence.md#sustained-speech-and-revision-evidence)
records the subsequent longer-passage observation.
