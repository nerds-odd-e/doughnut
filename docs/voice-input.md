# Voice input

Authors open Audio tools on an existing note to Record, Stop, and after a
failed conversion to Retry. The one main action is Record when ready and Stop
while recording; only while recording, the microphone chooser (named
"Microphone") and Write text now (converts what has been said so far,
unavailable during a conversion) sit beside Stop. Below, set apart, Save audio
downloads the last recording once one has produced a file, and Full screen
shows the current error in a full-screen overlay. A status
that assistive technology announces says "Ready to record", "Recording. Speak
now." (a mid-speech conversion does not change it), or "Turning your speech
into text…" until Stop has finished and the body holding the recording's
passages has been saved; Record is unavailable while it says so. Then it
says "Added to your note." when a passage of the recording was written and
saved, or "No speech was turned into text." when it wrote nothing.
When a conversion at Stop failed and audio is still
not converted, it says, shown as a problem, "Could not turn your speech into
text. Your recording is kept until you close Audio tools." with Retry beside
it; Record stays available. When recording cannot start, nothing is recorded
and it says, shown as a problem, "Could not use the microphone. Allow
microphone access in your browser, then try again."; Record stays the main
action, and a later Record that starts clears it. When every save of the recording's passages
failed (the save error shows as usual), it says "Ready to record". Dictated
text shows in an open body editor as soon as it joins; typing after that does not hold back "Added to your note." once the body
holding the passage is saved. Each Record starts counting afresh. Dictation
writes only to the note body.

## Adding dictated text to a note

Audio processing returns `segmentTexts`: the written transcription segments
for the uploaded chunk, in order. A mid-speech chunk is transcribed by
`whisper-1` as SRT; each segment contains the lines after its timestamp line,
with line breaks replaced by spaces. The conversion at Stop (and Retry) is
transcribed by `gpt-4o-mini-transcribe` as plain text, which is faster, and
its stripped text is the one segment. The client joins these
segments to the note using the same rule as successive passages, then saves
once per response. Nothing is left out, added, or reworded. Audio responses do not
use the conversation tool's `NoteContentCompletion`, which continues to
replace complete note content.

The audio request carries only the audio and the mid-speech flag. The client
retains the originating note id, and each returned segment is appended
deterministically to that note's current store body, loading its realm when
absent, and saved through the ordinary content PATCH. One join rule
serves both the saved body and an open editor's draft, successive passages,
and segments within a passage. An empty body becomes the segment alone;
after existing whitespace the segment joins directly. Otherwise the client
looks at the two characters on either side of the join. If either is
kanji/hanzi, hiragana, katakana (including `ー`), or punctuation in
U+3000–303F or U+FF00–FFEF, the segment joins directly. All other joins,
including English and Korean, add one space. For example, `私はPython` plus
`が好きです。` becomes `私はPythonが好きです。`, while `私はPython` plus
`is useful.` becomes `私はPython is useful.`. Existing characters remain unchanged, and
successive additions follow earlier additions once. Navigating to another note
does not redirect the result. The normal content-edit undo restores the prior body.

Timed chunks, pause flushes (after more than 3 s of silence, once per pause)
and Write text now clicks are processed mid-speech. Mid-speech processing never writes
the last transcription segment, because it may be an unfinished sentence. The
processed audio position advances to the end of the segment before it, and
the held segment's audio is sent again with the next chunk, so appending a
result does not re-add already processed audio. Blank lines at the end of the
transcription do not count as a segment. A lone segment writes nothing and all
its audio is kept. Only Stop writes everything that remains. Dictated text,
once written, is never revised: holding back the unfinished sentence replaces
revising it. Audio that is entirely silent is not sent.
A conversion that fails (an error answer or no answer) keeps its audio as
not yet converted, and recording goes on. That audio is sent again, together
with the later audio, with the next conversion: timed, pause, Write text now, or Stop,
including the first conversion of a new recording in the same Audio tools.
Text already written is not written again. A failure while recording shows
"Could not turn your speech into text. Your recording is kept." without Retry
until a later conversion succeeds. After Stop, while audio is still not
converted, the status shows the failure message and Retry beside it. Retry
belongs to the recording it follows: the status says "Turning your speech
into text…", Retry converts everything that remains as Stop does, nothing
held back, joins the passage once, and then says "Added to your note.". A
Retry that fails leaves the body, the message and Retry in place. Recovery
lasts while Audio tools stays open; it does not survive a reload or closing
Audio tools.
The transcription service controls transcription quality. When a body editor for the note is
open, the passage is joined to the end of that editor's draft, including
unsaved typing, and that draft is saved right away; otherwise, including while
an image upload or note removal is pausing the editor, it is joined to the
note's saved body.

The mounted audio preservation tests assert exact saved content for long,
empty, and whitespace-ending bodies, repeated additions, originating-note targeting, and undo. The
mocked recording journey supplies a transcription and observes the original
body, one space, and the transcription's text. A second mocked journey makes
the transcription fail at Stop, observes the unchanged body, the message in
the status with Retry and Record, then lets the transcription succeed and
observes "Added to your note." and Retry joining the passage once. Model
tests with the real audio buffer cover kept audio across failures, and mounted tests cover when Retry is
offered. The real-OpenAI journey checks both its original text and
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
Hardware capture, permission/device behavior and interruption remain
unassessed. Failure feedback and recovery are covered by the mocked and model
tests described above; a failure of the real transcription service has not
been observed. Kept audio is sent again in one request; whether a long outage
makes it larger than the transcription service accepts has not been observed. An automation route at `127.0.0.1:5175`
could sign in but did not activate Note/New notebook; localhost worked. That
observation does not establish a human-click defect or its cause.

## Dictating a passage with a pause and Write text now

With an existing six-sentence paragraph, record this known passage at natural
pace, retaining the eight-second pause after “yesterday”:

> The orchard contains apple trees, peach trees, and a small wooden bench.
> These facts are finished. The book that I bought yesterday, [eight-second
> pause] after reading several reviews and comparing different editions, is
> a gift for my sister because she enjoys learning about the history of
> gardens. The meeting is on Friday afternoon. We should bring a notebook
> and a pencil.

The macOS input was synthesized at 150 words/minute, mono 48 kHz, duration
29.168 s. Record produced an intermediate paragraph; activate Write text now
about 22 seconds into capture, continue until the source ends, then Stop.
The preexisting paragraph survived every update and reload, but new content
changed as follows:

1. “The orchard contains apple trees, peach trees, and a small wooden bench.
   These facts are finished. The book that I bought yesterday.”
2. Write text now replaced that paragraph with “After reading several reviews and
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
passage at Development `1a981673` (Write text now at 23 s, Stop at 31 s) wrote:

1. “The orchard contains apple trees, peach trees, and a small wooden bench.”
   The transcription also held “These facts are finished.”, and the
   “yesterday” segment was held back.
2. Write text now: “The book that I bought yesterday after reading several reviews and
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

Wait from the Stop click to the complete passage being visible, measured on
2026-10-06 on local Development at `b86f649322` (Chrome via Playwright on
macOS, signed in as `manual`, a note with one saved paragraph, the real
transcription service). Naturally paced recordings entered the synthetic
MediaStream described under [Observation boundary](#observation-boundary)
with no pause long enough to start a conversion; Stop was clicked about
250 ms after the recording ended. The accepted target for up to about 20
seconds of such speech is a middle wait of at most 2 s across five runs, with
none above 3 s. No automated test asserts it, because the wait depends on the
paid transcription service. Five runs each:

| Input | Median | Slowest | Runs (s) |
| --- | --- | --- | --- |
| 18.356 s Harvard passage | 1.39 s | 1.86 s | 1.84, 1.03, 1.86, 1.39, 1.27 |
| 6.283 s lighthouse addition | 1.09 s | 1.65 s | 0.75, 1.09, 1.65, 0.87, 1.45 |

Nearly all of the wait is the one `audio-to-text` request: under 10 ms passes
before it starts and under 10 ms after it ends. Before the Stop conversion
moved from `whisper-1` SRT to `gpt-4o-mini-transcribe` plain text, the same
Harvard runs on a held stack took a median 2.60 s (slowest 3.62 s), and the
lighthouse runs 1.97 s (slowest 2.97 s); uploading the 16 kHz WAV took about
60 ms of that. After reload, the original paragraph and the passage each
appeared once.

With the faster Stop transcription, a Stop passage that continues a sentence
held back or written earlier starts with a capital letter, for example
"…comparing different editions Is a gift for my sister…".

In the 2026-10-03 sessions, the lighthouse addition and both original
paragraphs survived reload exactly once and unchanged. They also survived the next distinct recording, despite
loss within that later recording. No duplication or stale prior addition was
identified in those two sessions. The navigation destination survived.
These comparisons establish neither a general preservation guarantee nor
failure frequency.

The empty-body UI baseline succeeded.
