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
When a body editor is open, the text goes where the author's caret or
selection was when they clicked Voice input, or where it was when focus left
the editor; with neither, to the end of the body. From that click until the
session ends the body editor is read-only: it takes no typing or pasting, and
clicking or selecting in it does not move the place the text goes to. Each
passage shows there as soon as it arrives, after the one before. The session
ends when the last text has arrived, when nothing was heard, or when the
conversion at Stop failed; the editor is then editable again, with focus and
the caret after the dictated text. Retrying a kept recording is a session of
its own: the editor is read-only again and the text goes where the caret or
selection is at the retry click. When recording cannot start, the editor is
not made read-only. In the rich editor a pending marker, three softly pulsing
dots the size of the text on a small patch of the page's background, sits
right after that place for the whole session: after the caret, or after the
end of a selection the words will replace. It follows to the end of each
passage that arrives, stays while the button shows its spinner after Stop and
during a retry, and is gone when the session ends, a failed conversion,
nothing heard, and leaving the note included. It is not shown when recording
cannot start. The dots stand still for an author who prefers reduced motion.
The marker is drawn over the editor and is not text: it is not saved, copied,
or exported, it takes no clicks, and it is hidden from assistive technology,
since the button's name already tells the session's state. The Markdown editor
shows no marker; there the button is the only sign of a running session.
Dictation writes only to the note body. Readers who may
not edit the note are not offered the button.

## Speaking a title in New note

In New note, a small microphone button inside the title field, just before
the Wikidata button, listens for a title. It is named for what a click does:
"Speak the title" while idle, and "Stop speaking the title" while listening,
when it is highlighted as a pressed toggle. After the author clicks it to
stop, it is unavailable and shows a spinner, named "Speak the title", until
the speech has become text; then it is idle again. Listening ends only when
the author clicks the button; nothing appears in the title while they are
still speaking. The recorder converts only at that click. From the moment
listening starts until the session ends, the title is read-only: it takes no
typing or pasting, and clicking or selecting in it does not move the place the
words go to, which is fixed when listening starts. An untouched
default "Untitled" is a placeholder: the whole of it is that place and the
heard words replace it, whether or not it is still selected, for example after
the author chose a folder first.
Any other title, including a title pattern the dialog opened with and a title
the author typed, takes the words where the author's caret or selection is,
by the same rule as on an existing note: a selection is replaced, a caret
takes the words at that place with the CJK/space rule of body passages on
both sides, and a caret at the end appends. For example, the pattern
"2026-10-06 " with its caret at the end and "weekly review" gives
"2026-10-06 weekly review"; with the caret after "Project" in
"Project review", "weekly" gives "Project weekly review". The target is the
selection the title has while it has focus, or the one it had when focus left
it. When the author has placed neither caret nor selection in
such a title, the words join its end, and fill an empty title. The session
ends when the words have arrived, when nothing was heard, or when the
conversion failed; the title is then editable again and has focus. After words
arrived the caret sits after them, so a typed correction continues from there;
otherwise the caret or selection is the one the session started with. Illegal
characters are replaced and warnings shown as for typing.
The search for existing notes runs for the resulting title. The author may type
corrections before Submit. While the dialog is listening or turning speech into
text, Submit is not offered and Enter in the title field does nothing;
afterwards Submit is offered again. Closing New note while listening stops the
recorder. When nothing was heard (a silent recording that runs no conversion, or
a response of no segments), the button returns to idle with no message, the
title is unchanged, and Submit is offered. When the microphone cannot be used,
the common error toast says "Could not use the microphone. Allow microphone
access in your browser, then try again.", the button stays idle, and the title
is not made read-only. When the
conversion fails, the common error toast says "Could not turn your speech into
text.", the title is unchanged, the button is idle, and Submit is offered.
Speaking again creates a fresh recorder so only the new recording's words
reach the title. The author reviews the title and chooses Submit as usual; the
note is created once with that title. Body dictation on an existing note never
changes the title.

## Speaking a title on an existing note

On an editable note page, the same small microphone button ends the title
heading's line, with the same names and the same idle, listening and
converting appearances as in New note. The heard words go where the author's
caret or selection is in the title when listening starts: a selection is
replaced by them, and a caret takes them at that place. As in New note, the
title is read-only from then until the session ends, so typing, pasting,
clicking and selecting do not change it or move that place. The CJK/space rule of body passages applies
on both sides of the words: one space towards a
neighbouring Latin-script character, none next to Japanese or Chinese
writing, and no second space where whitespace is already present. For
example, with the caret between "Orchard" and "notes", "harvest" gives
"Orchard harvest notes"; with the caret at the end of "りんご園", "の手入れ"
gives "りんご園の手入れ". The target is the selection the title has while it
has focus, or the one it had when focus left it, so pressing the button does
not lose it. When the author has placed neither caret nor selection in the
title being shown, the words join its end, and fill an empty title. Replacing
the whole title is done by selecting all of it and speaking. Afterwards the
title has focus and the caret sits after the heard words, so a typed
correction continues from there, and speaking again without moving the caret
continues there too. The result is proposed through the same path as typing:
a note nothing links to saves as a typed title does; a note other notes link
to shows the reference panel and saves only when the author chooses how links
should change, and leaving without choosing discards the heard words. When
nothing was heard, the button returns to idle with no message; when the
microphone cannot be used or the conversion fails, the common error toast
says so in the same words as in New note. In each case the title is unchanged,
editable, and nothing is saved; a microphone that cannot be used never makes
the title read-only. Moving to another note while listening stops the
recorder and ends the session: the heard words go to neither note, and the
title of the note the author arrives at is editable, shows its own text, is
not given focus, and has an idle button. Speaking again creates a fresh recorder so only the new
recording's words reach the title. Readers who may not edit the note see the
title as text and are not offered the button.
Body dictation with Voice input still writes only to the body and never
changes the title.

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
retains the originating note id, and each returned segment is added
deterministically to that note, at the session's place in an open body editor
or else at the end of its current store body, loading its realm when
absent, and saved through the ordinary content PATCH. One join rule
serves both the saved body and an open editor, successive passages,
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
open, the rich editor or the Markdown editor, the passage is put at the
session's place in that editor and saved right away: a selection is replaced
by the first passage, a caret takes it at that place, and the join rule
applies on both sides of the passage as it does for a spoken title. A rich
editor that cannot edit the body takes the passage at the end of the body.
When the author switches between the rich and the Markdown editor during a
session, it goes on in the editor they switched to, at the caret that editor
last had or else at the end of the body.
Otherwise, including while an image upload or note removal is pausing the
editor, and for the remaining speech of a note the author left, the passage is
joined to the end of the note's saved body.

The mounted audio preservation tests assert exact saved content for long,
empty, and whitespace-ending bodies, repeated additions, and undo. Mounted
tests of both body editors with the toolbar button place a caret or a
selection and observe the read-only editor, the text at that place, the caret
after it, and the retry's own place. Mounted tests of the rich editor
compare the marker's drawn position with the editor's own measure of that
place at the start, after a passage, during the conversion of Stop, and during
a retry, and observe it gone at each ending and absent in the Markdown editor. Mounted toolbar tests move to another
note while recording and observe the remainder
saved to the note left, an idle button, a dropped kept recording, and the
toast of a failed last conversion. The
mocked recording journey supplies a transcription and observes the original
body, one space, and the transcription's text. It also sees the marker
while recording and not after Stop. A second mocked journey makes
the transcription fail at Stop, observes the unchanged body and the error
toast, then lets the transcription succeed and observes the button's retry
adding the passage once. Model tests with the real audio buffer cover kept
audio across failures, and mounted tests cover the button's states. The
real-OpenAI journey checks both its original text and
the dictated passage.


## Observations

Bounded discovery notes, timings, and positive comparisons live in
[voice-input observations](./voice-input-observations.md).
