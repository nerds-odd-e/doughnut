# Voice input

Authors open Audio tools on an existing note to Record Audio, Flush Audio,
Stop Recording, or Save Audio Locally. Advanced Options exposes Processing
Instructions and full-screen editing. Body processing and automatic title
updates settle separately. No explicit voice-title control was found in the
existing-note title editor, Audio tools, Advanced Options, or New note form.
OS dictation, browser extensions and title instructions were not assessed.

## Adding dictated text to a note

Audio processing returns `DictatedText.dictatedText`: only the new passage,
formatted as Markdown, including any leading whitespace needed to join it to
the note. Existing content is context only; the model is instructed never to
repeat or revise it. Audio responses do not use the conversation tool's
`NoteContentCompletion`, which continues to replace complete note content.

The client retains the originating note id. It reads that note's current store
body, loading its realm when absent, and sends only the last 500 characters as
context, prefixed with `...` when truncated. The full body stays in the store;
the excerpt never becomes the saved replacement.

Each returned passage is appended deterministically to the originating note's
current body and saved through the ordinary content PATCH. Existing characters
remain unchanged, an empty body becomes the passage, and successive additions
follow earlier additions once. Navigating to another note does not redirect the
result. The normal content-edit undo restores the prior body.

Mid-speech processing excludes the last transcription segment and advances the
processed audio position to that segment's start. Its audio is retained for the
next chunk, so appending a result does not re-add already processed audio.
The model controls transcription quality and passage whitespace. Recent
unfinished-sentence revision and unsaved editor drafts have separate behavior;
the append operates on current store content. Automatic title suggestions
continue on the existing schedule.

The mounted audio preservation tests assert exact saved content for long and
empty bodies, repeated additions, originating-note targeting, and undo. The
mocked recording journey supplies only new text and observes the original body
plus that addition. The real-OpenAI journey checks both its original text and
the dictated passage.

## Observation boundary

The observations below are from Chrome/macOS on 2026-10-03, signed in as
`manual` on local Development at `localhost:5175`. The observed backend revision
was `a02dbb2697…`; the frontend/runtime revision was not fully established.
They describe observed behavior, not a guarantee about later revisions.

Naturally paced prerecorded or synthesized speech entered a synthetic browser
MediaStream through AudioContext → MediaStreamDestination. The real recorder,
worklet, transcription, retouch, persistence and title services were exercised.
No clocks, worklets, request delays or service responses were simulated.
Hardware capture, permission/device behavior, interruption and service-failure
feedback/recovery remain unassessed. An automation route at `127.0.0.1:5175`
could sign in but did not activate Note/New notebook; localhost worked. That
observation does not establish a human-click defect or its cause.

## Completed dictated content can disappear

With an existing six-sentence paragraph, record this known passage at natural
pace, retaining the eight-second pause after “yesterday”:

> The orchard contains apple trees, peach trees, and a small wooden bench.
> These facts are finished. The book that I bought yesterday, [eight-second
> pause] after reading several reviews and comparing different editions, is
> a gift for my sister because she enjoys learning about the history of
> gardens. The meeting is on Friday afternoon. We should bring a notebook
> and a pencil.

The macOS input was synthesized at 150 words/minute, mono 48 kHz, duration
29.168 s. Record Audio produced an intermediate paragraph; activate Flush
about 22 seconds into capture, continue until the source ends, then Stop.
The preexisting paragraph survived every update and reload, but new content
changed as follows:

1. “The orchard contains apple trees, peach trees, and a small wooden bench.
   These facts are finished. The book that I bought yesterday.”
2. Flush replaced that paragraph with “After reading several reviews and
   comparing different editions, is a gift from my sister because she”.
3. The final update replaced it with “She enjoys learning about the history
   of gardens. The meeting is on Friday afternoon. We should bring a notebook
   and a pencil.” Reload confirmed only this final paragraph remained.

Completed orchard facts were lost, and the complete book sentence was never
recovered. The intermediate “from my sister” differs from the input “for my
sister”. This is loss within the current recording, not whole-note erasure.
The cause and frequency are unknown. The acceptable boundary for revising a
recent unfinished sentence remains a product decision.

## Visible typing can be lost while audio processing is pending

Save two recognizable paragraphs and a distinct spoken addition:

> Original paragraph one: The museum opens at nine each morning. Our tickets
> are booked for Tuesday.
>
> Original paragraph two: The blue notebook contains the garden measurements.
> Keep the oak tree map beside it.
>
> The lighthouse keeper painted the front door bright yellow. Tomorrow we
> will bring fresh oranges to the beach.

Start the longer orchard passage. When its first real audio request is pending,
focus the supported rich body editor and type:

> MANUAL EDIT: Keep this red bicycle sentence.

Typing began 1.245 s after the request started. The whole sentence without its
period was visible before the audio content PATCH completed; the arriving
result erased it. The final typed period remained at the beginning of the old
paragraph. No manual-content PATCH preceded that result, so this establishes
loss of visible in-progress typing, not overwrite of an already-saved edit.

A complete paste after audio settlement persisted through reload, along with
both original paragraphs and the prior lighthouse addition. New orchard
content was absent. An already-saved edit race remains unassessed.

## Existing content can be truncated during a navigation journey

Prepare a source note with five saved paragraphs: the two originals above,
the manual sentence prefixed to the lighthouse paragraph, then:

> After reading several reviews and comparing different editions, is a gift
> from my sister because she enjoys learning about the history of gardens.
>
> The meeting is on Friday afternoon. We should bring a notebook and a pencil.

Prepare a second note with a distinct title and this saved body:

> DESTINATION ORIGINAL: The violet umbrella stays on shelf seven. No spoken
> addition belongs here.

Start the orchard recording on the source. When the first audio request is
pending, click the destination's sidebar link. Recording continued while the
destination was selected, and Stop remained available. Both content PATCHes
and title requests targeted the source; no destination content PATCH appeared.

The first source PATCH contained only new orchard/book content, with none of
the old source body. The source was hidden then, so this is request-payload
evidence rather than a visible intermediate snapshot. After the second result,
returning and reloading showed the source's first paragraph as literal
`...uesday.`; its other four paragraphs were unchanged. The newly dictated
orchard content was absent. An independent fresh page confirmed the saved loss.
The destination body and both titles survived unchanged, despite successful
source title-suggestion responses. Navigation's causal role is unknown.

## Automatic titles can overwrite author choice

Recording `e2e_test/fixtures/harvard.wav` (18.3561875 s) into an initially
visually empty body with its default `type: Note` property changed `Untitled`
to “Sensory Notes on Food and Drink”. Repeating the same passage changed it
to “Sensory Impressions of Foods and Drinks”, although the saved final bodies
were identical. Reload confirmed the second title.

Setting “Author chosen preservation title” before the orchard recording did
not protect it: processing changed it to “Example Paragraphs for Preservation”,
then “Sample Paragraphs for Preservation”. Reload confirmed the last title.
The choice between disabling automatic titles and delayed one-time generation
for an `Untitled` note remains unresolved. Explicitly chosen titles need a
product policy that preserves author control.

## Responsiveness and positive comparisons

| Input | Observed timing |
| --- | --- |
| 18.356 s Harvard passage, first recording | First body at 25.17 s from capture, approximately 3.96 s after Stop; audio request 4.42 s; title settled separately |
| Identical Harvard repeat | Audio request 4.61 s; body stayed identical; body/title settled before Stop |
| 29.168 s orchard passage with Flush | Audio requests 5.25 / 3.14 / 3.53 s; visible results at 16.73 / 25.30 / 35.75 s from capture; final body settled before Stop |
| 6.2827 s lighthouse addition | Audio request 2.79 s; visible addition at 12.16 s from capture, 5.88 s after speech ended; body/title settled before Stop |

The lighthouse addition and both original paragraphs survived reload exactly
once and unchanged. They also survived the next distinct recording, despite
loss within that later recording. No duplication or stale prior addition was
identified in those two sessions. The navigation destination survived.
These comparisons establish neither a general preservation guarantee nor
failure frequency.

The empty-body UI baseline succeeded. A separate request with an empty
serialized previous-content value returned HTTP 500 during an API probe;
that failure was not reproduced through the UI. Timing identifies stages,
not the cause of delay, model suitability or a numeric acceptance target.
