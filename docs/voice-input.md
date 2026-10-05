# Voice input

Authors open Audio tools on an existing note to Record Audio, Flush Audio,
Stop Recording, or Save Audio Locally. Advanced Options offers full-screen
editing. Dictation writes only to the note body.

## Adding dictated text to a note

Audio processing returns `segmentTexts`: the written transcription segments
for the uploaded chunk, in order. Each segment contains the lines after its
timestamp line, with line breaks replaced by spaces. The client joins these
segments to the note using the same rule as successive passages, then saves
once per response. Nothing is left out, added, or reworded. Audio responses do not
use the conversation tool's `NoteContentCompletion`, which continues to
replace complete note content.

The audio request carries only the audio and the mid-speech flag. The client
retains the originating note id, and each returned segment is appended
deterministically to that note's current store body, loading its realm when
absent, and saved through the ordinary content PATCH. One join rule
serves both the saved body and an open editor's draft: text that does not end
in whitespace is followed by one space and then the segment, text already
ending in whitespace is followed directly by the segment, and an empty body
becomes the segment alone. Existing characters remain unchanged, and
successive additions follow earlier additions once. Navigating to another note
does not redirect the result. The normal content-edit undo restores the prior body.

Timed chunks, pause flushes (after more than 3 s of silence, once per pause)
and Flush clicks are processed mid-speech. Mid-speech processing never writes
the last transcription segment, because it may be an unfinished sentence. The
processed audio position advances to the end of the segment before it, and
the held segment's audio is sent again with the next chunk, so appending a
result does not re-add already processed audio. Blank lines at the end of the
transcription do not count as a segment. A lone segment writes nothing and all
its audio is kept. Only Stop writes everything that remains. Dictated text,
once written, is never revised: holding back the unfinished sentence replaces
revising it. Audio that is entirely silent is not sent.
The transcription service controls transcription quality. When a body editor for the note is
open, the passage is joined to the end of that editor's draft, including
unsaved typing, and that draft is saved right away; otherwise, including while
an image upload or note removal is pausing the editor, it is joined to the
note's saved body.

The mounted audio preservation tests assert exact saved content for long,
empty, and whitespace-ending bodies, repeated additions, originating-note targeting, and undo. The
mocked recording journey supplies a transcription and observes the original
body, one space, and the transcription's text. The real-OpenAI journey checks both its original text and
the dictated passage.

## Observation boundary

The observations below are from Chrome/macOS on 2026-10-03, signed in as
`manual` on local Development at `localhost:5175`. The observed backend revision
was `a02dbb2697…`; the frontend/runtime revision was not fully established.
They describe observed behavior, not a guarantee about later revisions.

Naturally paced prerecorded or synthesized speech entered a synthetic browser
MediaStream through AudioContext → MediaStreamDestination. The real recorder,
worklet, transcription and persistence services were exercised.
No clocks, worklets, request delays or service responses were simulated.
Hardware capture, permission/device behavior, interruption and service-failure
feedback/recovery remain unassessed. An automation route at `127.0.0.1:5175`
could sign in but did not activate Note/New notebook; localhost worked. That
observation does not establish a human-click defect or its cause.

## Dictating a passage with a pause and Flush

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
The cause and frequency are unknown.

The owner decided on 2026-10-03 to hold back the unfinished sentence rather
than revise written text. With append and hold-back in place, the same
passage at Development `1a981673` (Flush at 23 s, Stop at 31 s) wrote:

1. “The orchard contains apple trees, peach trees, and a small wooden bench.”
   The transcription also held “These facts are finished.”, and the
   “yesterday” segment was held back.
2. Flush: “The book that I bought yesterday after reading several reviews and
   comparing different editions”, with the next segment held back.
3. Stop: “is a gift from my sister because she enjoys learning about the
   history of gardens. The meeting is on Friday afternoon. We should bring a
   notebook and a pencil.”

After reload the original paragraph, the complete book sentence and the
meeting sentences appeared once, and nothing written was revised. The written
passage is the transcription's own text, joined as
[Adding dictated text to a note](#adding-dictated-text-to-a-note) describes,
so a completed sentence such as “These facts are finished.” reaches the note
whenever the transcription holds it.

In an earlier run of this passage, before the transcription's trailing blank
lines were handled, a short remainder of near-silent audio sent at Stop was
transcribed as “You” and appended. Speech models are known to invent such
words from near-silence; it has not been observed since hold-back.

## Typing while audio processing is pending

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
focus the body editor and type at the end:

> MANUAL EDIT: Keep this red bicycle sentence.

When the result arrives, the typed sentence stays visible and the orchard
passage follows it. A correction made elsewhere in the body, such as changing
"from" to "for", also stays, and the passage still goes at the end. The caret
stays where the author was typing. The draft with the passage is saved as
soon as the passage joins it, so both originals, the lighthouse addition, the
typed sentence and the passage are saved once each. The mounted
typing-while-pending tests cover the rich and Markdown editors.

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
targeted the source; no destination content PATCH appeared.

The first source PATCH contained only new orchard/book content, with none of
the old source body. The source was hidden then, so this is request-payload
evidence rather than a visible intermediate snapshot. After the second result,
returning and reloading showed the source's first paragraph as literal
`...uesday.`; its other four paragraphs were unchanged. The newly dictated
orchard content was absent. An independent fresh page confirmed the saved loss.
The destination body and both titles survived unchanged. Navigation's causal
role is unknown.

## Responsiveness and positive comparisons

| Input | Observed timing |
| --- | --- |
| 18.356 s Harvard passage, first recording | First body at 25.17 s from capture, approximately 3.96 s after Stop; audio request 4.42 s |
| Identical Harvard repeat | Audio request 4.61 s; body stayed identical and settled before Stop |
| 29.168 s orchard passage with Flush | Audio requests 5.25 / 3.14 / 3.53 s; visible results at 16.73 / 25.30 / 35.75 s from capture; final body settled before Stop |
| 6.2827 s lighthouse addition | Audio request 2.79 s; visible addition at 12.16 s from capture, 5.88 s after speech ended; body settled before Stop |

The lighthouse addition and both original paragraphs survived reload exactly
once and unchanged. They also survived the next distinct recording, despite
loss within that later recording. No duplication or stale prior addition was
identified in those two sessions. The navigation destination survived.
These comparisons establish neither a general preservation guarantee nor
failure frequency.

The empty-body UI baseline succeeded. Timing identifies stages,
not the cause of delay, model suitability or a numeric acceptance target.
