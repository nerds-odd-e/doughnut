# Voice-input pending-navigation evidence

**Source:** [Discovery story](SEED-066-voice-input.md#discover-voice-input-problems).
**Capture setup:** [Literal baseline harness](SEED-066-voice-input-manual-evidence.md#recording-route-and-short-speech-baseline).

## Late result after supported navigation

**2026-10-03, 02:11–02:14 UTC:** Chrome 155 on macOS, signed-in `manual`
session at `http://localhost:5175/`, disposable notebook `23`. Evidence checkout
base `f968a52233f4682ae97e1d405817824e421f6ebc`. Reused preparation's running
backend revision observation `a02dbb2697…`; no fresh revision read or service
restart. Transcription, retouch and persistence used the real Development
services. No clock, request delay, response, worklet or router was mocked.

**Discrepancy:** The discovery story expects existing content to survive voice
processing. After recording on source note `13726`, navigating to destination
`13727` during a live pending audio request, and returning after settlement,
the source's saved first paragraph was truncated to literal `...uesday.`.
The remaining four saved paragraphs were unchanged. The destination's distinct
original body and title were unchanged. This demonstrates source-content loss
in this journey; it does not establish that navigation caused the loss, defect
frequency, or a general guarantee about which note receives results.

**Saved before texts:** Source title was “Sample Paragraphs for Preservation”.
Its body was exactly:

> Original paragraph one: The museum opens at nine each morning. Our tickets are booked for Tuesday.
>
> Original paragraph two: The blue notebook contains the garden measurements. Keep the oak tree map beside it.
>
> MANUAL EDIT: Keep this red bicycle sentence. The lighthouse keeper painted the front door bright yellow. Tomorrow we will bring fresh oranges to the beach.
>
> After reading several reviews and comparing different editions, is a gift from my sister because she enjoys learning about the history of gardens.
>
> The meeting is on Friday afternoon. We should bring a notebook and a pencil.

Create destination through New note (n), enter title “Navigation destination
sentinel”, Submit, then fill the rich body with:

> DESTINATION ORIGINAL: The violet umbrella stays on shelf seven. No spoken addition belongs here.

Reload verified the destination sentinel before returning through the source's
sidebar link. No source-body edit was made in this slice.

**Input and reproduction:** Reuse the [sustained report's literal `say` command](SEED-066-voice-input-sustained-evidence.md#sustained-speech-and-revision-evidence)
with only output filename changed to `.voice-discovery-nav.wav`, and use its
same-worktree Vite fetch URL ending in that filename. Input is the identical
orchard/book/garden/meeting passage with an eight-second pause: 29.168 s,
48 kHz. Baseline `getUserMedia` replacement feeds naturally paced AudioContext
→ MediaStreamDestination audio through the actual worklet, device label
`Controlled navigation speech`. This is synthesized speech and synthetic media
capture; hardware, permission prompts and real device selection remain gaps.

Reuse the sustained report's DOM MutationObserver for URL, visible body and
title changes, plus CDP Network events. On source, open Audio tools and Record
Audio. Read `Network.requestWillBeSent` from a cursor captured before recording;
as soon as `/api/audio/audio-to-text` appears, activate the visible sidebar
“Navigation destination sentinel” link through Playwright. Observe destination
until the remaining real requests settle, Stop Recording there, and return
through the source sidebar link. No programmatic navigation or delay was used.

| UTC | Signal |
| --- | --- |
| 02:12:28.815 | Controlled capture starts on source `13726` |
| 02:12:40.199 | First audio request starts, CDP ID `26956.1403` |
| 02:12:40.200 | Live request observed, immediately followed by sidebar click |
| 02:12:40.245 | Observer sees destination URL/body/title; request still pending |
| 02:12:43.620 | First audio HTTP 200 response, 3.421 s request |
| 02:12:43.621 → 02:12:43.692 | Source `/api/text_content/13726/content` PATCH → HTTP 200 |
| 02:12:43.697 → 02:12:47.000 | Source suggest-title request → HTTP 200 |
| 02:12:57.983 | Natural end of 29.168 s source playback |
| 02:13:00.964 → 02:13:04.493 | Second audio request → HTTP 200, 3.529 s |
| 02:13:04.494 → 02:13:04.560 | Second source-content PATCH → HTTP 200 |
| 02:13:04.566 → 02:13:07.127 | Second source suggest-title request → HTTP 200 |
| 02:13:15.536 | Stop marker on destination, after all observed result responses |
| 02:13:15.568 | Return to source displays truncated first paragraph |
| 02:13:29.294 / 02:13:29.730 | Source / destination reload comparisons confirm saved bodies |

Navigation was about 46 ms after the request began and 3.375 s before its
response. Destination showed its unchanged sentinel throughout. Flush was
temporarily disabled during processing; Stop remained enabled after navigation,
and no new Record control appeared until Stop. The same capture continued into
the second audio request while destination was selected. Both content PATCHes
targeted source `13726`; no destination content PATCH was observed. Title
requests targeted source too, but no title PATCH or visible title change was
observed; after reload both original titles remained. Response timestamps above
are reconstructed from each request's wall time and CDP monotonic interval.

**Content evidence:** The first source-content PATCH's exact JSON `content`
was `\n\nThe orchard contains apple trees, peach trees, and a small wooden bench.\n\nThese facts are finished.\n\nThe book that I bought yesterday.`;
it contained none of the source's saved-before body. Source was not visible
then, so this is request-payload/HTTP evidence, not an intermediate visible-body
snapshot. The second PATCH began `...uesday.`, then the four other paragraphs
quoted above. Return and reload confirmed exactly that final body: the entire
museum opening-time sentence and most of the ticket sentence were absent;
the new orchard paragraph was also absent. Destination reload confirmed its
exact sentinel once, with no source or dictated text. Tool screenshots captured
both persisted notes; no standalone screenshot file was created. No cause was
investigated. Stop occurred after settlement, so post-Stop processing latency
is unmeasured.

At 02:13:28.818 the harness restored both media-device methods, disconnected its
observer, stopped owned sources/stream tracks, closed AudioContext, and deleted
its page variable. Network observation was disabled; reload cleared page state.
The owned WAV and tab were removed/closed. Source `13726` and destination
`13727`, both in notebook `23`, remain disposable for final-session cleanup.
No product source changed. Observation and evidence took about four minutes.

## Explicit title-entry discovery

**2026-10-03, 02:22–02:23 UTC:** Same Chrome/macOS Development environment
and signed-in `manual` session; evidence checkout base
`a2f401883067fd244837ed12ea1a1603e5a623d5`. Reused the earlier backend revision
observation; no new service or recording request was made.

**Improvement opportunity:** The owner's [title-entry goal](SEED-066-voice-input.md#usable-voice-input)
includes dictating a title and creating a note by speaking its title. No explicit
voice-title action or title destination selector was discoverable in the inspected
existing-note title, Audio tools, Advanced Options, or New note form. This is a
bounded UI capability observation, not a regression or an exhaustive absence claim.

**Steps and signals:** Open Note → “Voice discovery 20261003 baseline” →
“Sample Paragraphs for Preservation” (`/n13726`). Click its displayed title:
the text gains focus without opening a separate form. DOM inspection identifies
the title as `contenteditable="true"`, `role="title"`, `data-test="note-title"`;
no title-specific microphone action is shown. Open Audio tools: Record Audio is
enabled; Flush Audio, Stop Recording, and Save Audio Locally are disabled while
idle. Advanced Options reveals “Processing Instructions:” and Toggle Full Screen,
with no explicit title destination. Prior [baseline evidence](SEED-066-voice-input-manual-evidence.md#recording-route-and-short-speech-baseline)
already establishes body transcription and separate automatic title generation;
that journey was not repeated or interpreted as explicit title dictation.

Click New note (n): the modal shows Folder (“Notebook root”), Search folders,
Relationship (None / Under current), a focused Title containing `Untitled`,
Wikidata Id, Recently updated notes, and Submit. Its Title has the same editable
semantics and an `aria-label="Title"`. Filling it with `Explicit title entry probe`
changes the visible Title value. No recording or voice-title control is present
in this form; the existing Audio tools panel remains behind the modal. Dismiss
with the visible × close button without Submit. The modal disappears and the
existing note title remains “Sample Paragraphs for Preservation”.

Tool screenshots captured the existing-note view and creation form; no standalone
screenshot artifact was retained. OS dictation, browser extensions, title entry
through processing instructions, and speaking-to-create remain unobserved.
No new note or audio artifact was created, no microphone stream was started, and
the owned tab was closed. Existing disposable notes remain for final cleanup.
Observation plus recording of evidence took about three minutes.
