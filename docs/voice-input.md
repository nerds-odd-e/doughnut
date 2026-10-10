# Voice input

Authors dictate into an existing note's body with the one Voice input button
in the note toolbar; on a narrow screen it is in the "more options" menu.
Idle, the button shows a microphone and is named "Voice input".
Clicking it starts capture from the browser's current default microphone at
once: the button is highlighted like the toolbar's other active toggles, its
icon is a live waveform of the microphone's level (a still, quiet line in
silence), and it is named "Stop voice input". An author with several
microphones chooses one in the browser's own site settings; when the current
microphone disconnects, recording switches to the first one available. While
recording, and until the last speech has been added, a kept recording
included, the button stays in the toolbar even when it was started from the
"more options" menu. Clicking the
active button stops capture; the button is then unavailable, showing a
spinner and named "Voice input", until the last speech has been converted and
its text added to the note, and then it is idle again. A recording in which no
speech was recognized adds nothing to the note and shows no message. When a
conversion at Stop failed and audio is still not converted, the common error
toast says "Could not turn your speech into text. Your recording is kept until
you leave this note; click Voice input to try again." and the button holds the
kept recording: it is tinted as a warning, shows a retry arrow, and is named
"Retry turning your speech into text". Clicking it converts the kept recording
without the microphone; the button is unavailable with its spinner meanwhile.
On success the text is added and the button is idle; on failure the toast
shows again and the button keeps the recording. The kept recording lasts while
the author stays on the note; reloading or leaving the note drops it. Leaving
the note while recording, by moving to another note or another page, stops the
recording and adds the remaining speech to the note the author left; the
button of the note they arrive at is idle. When that last conversion fails,
nothing is kept and the common error toast says only "Could not turn your
speech into text." When recording cannot start, nothing is recorded, the
common error toast says "Could not use the microphone. Allow microphone access
in your browser, then try again.", and the button stays idle. When saving a
passage fails, the save's own error toast shows and the button is idle.
Dictated text shows in an open body editor as soon as it joins. Dictation
writes only to the note body. Readers who may not edit the note are not
offered the button.

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
the title as text and are not offered the control. Body dictation with Voice
input still writes only to the body and never changes the title.

## Adding dictated text to a note

Audio processing returns `segmentTexts`: the written transcription segments
for the uploaded chunk, in order. A mid-speech chunk is transcribed by
`whisper-1` as SRT; each segment contains the lines after its timestamp line,
with line breaks replaced by spaces. The conversion at Stop is
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
ends the dictation and does not redirect its result. The normal content-edit undo restores the prior body.

Timed chunks (every 20 s) and pause flushes (after more than 3 s of silence, once per pause)
are processed mid-speech. The 20-second timer
keeps the audio billed for mid-speech conversion within 1.5 times the recorded
audio: the held segment sent again is about 3 to 5 seconds per chunk (about
1.14 to 1.25 times, roughly $0.41 to $0.45 per dictated hour at $0.006 per
minute), and a chunk of up to 20 seconds returns from the transcription
service in about 3.5 seconds. Mid-speech processing never writes
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
with the later audio, with the next conversion: timed, pause, or Stop.
Text already written is not written again. A failure while recording shows
the common error toast "Could not turn your speech into text. Your recording
is kept." After a failed conversion at Stop, the author recovers the kept
recording from the button's retry: the kept audio is converted again and its
passage is added once. The kept recording lasts while the author stays on the
note and is dropped by a reload or by leaving the note. A
recording in progress stops when the author moves to another note or page, and
its remaining speech is converted into the note it was started on; when that
conversion fails its audio is dropped. Voice input started on the other note
dictates into that note.
The transcription service controls transcription quality. When a body editor for the note is
open, the passage is joined to the end of that editor's draft, including
unsaved typing, and that draft is saved right away; otherwise, including while
an image upload or note removal is pausing the editor, it is joined to the
note's saved body.

The mounted audio preservation tests assert exact saved content for long,
empty, and whitespace-ending bodies, repeated additions, and undo. Mounted
toolbar tests move to another note while recording and observe the remainder
saved to the note left, an idle button, a dropped kept recording, and the
toast of a failed last conversion. The
mocked recording journey supplies a transcription and observes the original
body, one space, and the transcription's text. A second mocked journey makes
the transcription fail at Stop, observes the unchanged body and the error
toast, then lets the transcription succeed and observes the button's retry
adding the passage once. Model tests with the real audio buffer cover kept
audio across failures, and mounted tests cover the button's states. The
real-OpenAI journey checks both its original text and
the dictated passage.


## Observations

Bounded discovery notes, timings, and positive comparisons live in
[voice-input observations](./voice-input-observations.md).
