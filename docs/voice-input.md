# Voice input

Authors open Audio tools on an existing note to Record, Stop, and after a
failed conversion to Retry. The one main action is Record when ready and Stop
while recording; only while recording, the microphone chooser (named
"Microphone") and Write text now (converts what has been said so far,
unavailable during a conversion) sit beside Stop. The controls alone show
the state of the recording. After Stop, Record is unavailable until the last
speech has been converted and its text added to the note, then it is
available again. A recording in which no speech was recognized adds nothing
to the note, and Record is then the only control. When a conversion at Stop
failed and audio is still not converted, the panel says, shown as a problem,
"Could not turn your speech into text. Your recording is kept until you close
Audio tools." with Retry beside it; Record stays available. When recording cannot start, nothing is recorded
and the panel says, shown as a problem, "Could not use the microphone. Allow
microphone access in your browser, then try again."; Record stays the main
action, and a later Record that starts clears it. When saving a passage
fails, the save error shows as usual and Record is available. Dictated
text shows in an open body editor as soon as it joins. Dictation
writes only to the note body.

## Speaking a title in New note

In New note, "Speak the title" listens for a title. It becomes "Stop" while
listening. A status that assistive technology announces says "Recording. Speak
now.", then "Turning your speech into text…" until Stop has finished; once the
heard words are in the title field, there is no status. Listening ends only
when the author chooses Stop; nothing appears in the title while they are still
speaking. The recorder converts only at Stop. The response's segments are joined
by the same CJK/space rule as body passages through the title editor's ordinary
handling: they replace an untouched default "Untitled", and otherwise join the
end of the title the author already has (including a title pattern the dialog
opened with). Illegal characters are replaced and warnings shown as for typing.
The search for existing notes runs for the heard title. The author may type
corrections before Submit. While the dialog is listening or turning speech into
text, Submit is not offered and Enter in the title field does nothing;
afterwards Submit is offered again. Closing New note while listening stops the
recorder. When nothing was heard (a silent recording that runs no conversion, or
a response of no segments), the status says "No speech was turned into text.",
the title is unchanged, and Submit is offered. When the microphone cannot be
used, the status says, shown as a problem, "Could not use the microphone. Allow
microphone access in your browser, then try again." and "Speak the title" stays
available. When conversion at Stop fails, the status says, shown as a problem,
"Could not turn your speech into text.", the title is unchanged, and Submit is
offered; there is no Retry. Speaking again creates a fresh recorder so only the
new recording's words reach the title. The author reviews the title and chooses
Submit as usual; the note is created once with that title. Body dictation on an
existing note never changes the title.

## Speaking a title on an existing note

On an editable note page, "Speak the title" sits with the title heading and
behaves as in New note for listening, Stop, and status wording. The heard
words replace the whole title (they do not join the end of what was there).
The result is proposed through the same path as typing: a note nothing links
to saves as a typed title does; a note other notes link to shows the
reference panel and saves only when the author chooses how links should
change, and leaving without choosing discards the heard title. The author may
type corrections after speaking. When nothing was heard, or conversion at
Stop fails, the status says so, the title is unchanged, and nothing is saved.
Speaking again creates a fresh recorder so only the new recording's words
replace the title. There is no Retry. Readers who may not edit the note see
the title as text and are not offered the control. Body dictation with Audio
tools still writes only to the body and never changes the title.

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
converted, the panel shows the failure message and Retry beside it. Retry
belongs to the recording it follows: it converts everything that remains as
Stop does, nothing held back, joins the passage once, and then disappears;
Record is unavailable meanwhile. A
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
the transcription fail at Stop, observes the unchanged body, the message
with Retry and Record, then lets the transcription succeed and
observes Retry joining the passage once. Model
tests with the real audio buffer cover kept audio across failures, and mounted tests cover when Retry is
offered. The real-OpenAI journey checks both its original text and
the dictated passage.


## Observations

Bounded discovery notes, timings, and positive comparisons live in
[voice-input observations](./voice-input-observations.md).
