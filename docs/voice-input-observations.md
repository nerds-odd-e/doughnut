# Voice input observations

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
tests in [voice input](./voice-input.md); a failure of the real transcription service has not
been observed. Kept audio is sent again in one request; whether a long outage
makes it larger than the transcription service accepts has not been observed. An automation route at `127.0.0.1:5175`
could sign in but did not activate Note/New notebook; localhost worked. That
observation does not establish a human-click defect or its cause.

## Existing content can be truncated during a navigation journey

Prepare a source note with five saved paragraphs:

> Original paragraph one: The museum opens at nine each morning. Our tickets
> are booked for Tuesday.
>
> Original paragraph two: The blue notebook contains the garden measurements.
> Keep the oak tree map beside it.
>
> MANUAL EDIT: Keep this red bicycle sentence. The lighthouse keeper painted
> the front door bright yellow. Tomorrow we will bring fresh oranges to the
> beach.
>
> After reading several reviews and comparing different editions, is a gift
> from my sister because she enjoys learning about the history of gardens.
>
> The meeting is on Friday afternoon. We should bring a notebook and a pencil.

Prepare a second note with a distinct title and this saved body:

> DESTINATION ORIGINAL: The violet umbrella stays on shelf seven. No spoken
> addition belongs here.

Use this known passage at natural pace, retaining the eight-second pause
after “yesterday”:

> The orchard contains apple trees, peach trees, and a small wooden bench.
> These facts are finished. The book that I bought yesterday, [eight-second
> pause] after reading several reviews and comparing different editions, is
> a gift for my sister because she enjoys learning about the history of
> gardens. The meeting is on Friday afternoon. We should bring a notebook
> and a pencil.

The macOS input was synthesized at 150 words/minute, mono 48 kHz, duration
29.168 s. Start the recording on the source. When the first audio request is
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

In the 2026-10-03 lighthouse session, the addition and both original
paragraphs survived reload exactly once and unchanged. No duplication or stale
prior addition was identified in that session. The navigation destination
survived.
These comparisons establish neither a general preservation guarantee nor
failure frequency.

The empty-body UI baseline succeeded.
