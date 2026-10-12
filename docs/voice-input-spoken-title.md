# Speaking a title

An author can speak a note's title in New note and on an existing note's
page. Dictating into a note's body, the pending marker's appearance, the
CJK/space rule of body passages, and the tests of all of these are described
in [voice input](./voice-input.md).

## Title wording and conversion

Both title journeys send optional title intent through the existing audio API.
The backend supplies one title instruction to the final transcription request,
using `gpt-4o-mini-transcribe`; the response remains the dictated fragment.
The instruction asks for concise, faithful wording, preserves every substantive
detail, omits an ordinary sentence-ending period, and retains appropriate
punctuation, including a question mark for a question. It supplies no title
length cap and gives no authority to rewrite the surrounding title. The editor
applies the returned fragment at its captured caret or replacement range and
uses ordinary title saving. No additional refinement request is made.

Ordinary name handling maps ASCII `?` to the fullwidth question mark `？`, as
it does for path separators, so a spoken question retains its punctuation and
can be saved under the portable name rules. The author can review and correct
the returned wording. Content requests omit title intent and keep their existing
transcription behavior.

## Speaking a title in New note

In New note, a small microphone button inside the title field, just before
the Wikidata button, listens for a title. It is the same button as the
toolbar's Voice input, with its own names and the title field's frame. Idle,
it shows a microphone and is named "Speak the title". After a click it is
unavailable and shows a spinner, still named "Speak the title", until the
microphone is ready. While listening it is highlighted as a pressed toggle,
shows a waveform that follows the level of the recording, and is named "Stop
speaking the title". After the author clicks it to stop, it is unavailable
and shows a spinner, named "Speak the title", until the speech has become
text; then it is idle again. Listening ends only when
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
For the whole session the same pending marker as in the body's rich editor
sits right after the place the words go to: after the untouched "Untitled",
after the caret, after the end of a selection the words will replace, or where
the first character of an empty title will be drawn. It stays while the
button shows its spinner after Stop and is gone when the session ends: the
words arrived, nothing heard, a failed conversion, and closing New note
included. It is not shown when the microphone cannot be used. It is drawn over
the title field and is never part of the title's text.
The search for existing notes runs for the resulting title. The author may type
corrections before Submit. While the dialog is listening or turning speech into
text, Submit is not offered and Enter in the title field does nothing;
afterwards Submit is offered again. Closing New note while listening stops the
recorder. When nothing was heard (a silent recording that runs no conversion, or
a response of no segments), the button returns to idle with no message, the
title is unchanged, and Submit is offered. When the microphone cannot be used,
the common error toast says "Could not use the microphone. Allow microphone
access in your browser, then try again.", the button is idle, and the title
is not made read-only. When the
conversion fails, the common error toast says "Could not turn your speech into
text.", the title is unchanged, the button is idle, and Submit is offered.
Speaking again creates a fresh recorder so only the new recording's words
reach the title. The author reviews the title and chooses Submit as usual; the
note is created once with that title.

## Speaking a title on an existing note

Place a caret or selection in an editable title, then activate **Voice input**
in the note toolbar. On narrow screens reach it through "more options".
The title remains the destination while navigating through the toolbar with
Tab, Enter or Space, or opening, closing and reopening its menu. Moving to the
content editor or another unrelated destination clears that intent: the next
fresh recording uses content. Opening a note and activating Voice input without
first focusing its title also uses content.

The field and insertion range are captured before microphone startup. Focus or
selection changes during startup, listening or conversion do not redirect the
session. Stop voice input finishes that same recording. Title speech converts
only on Stop; nothing appears while listening. The title is read-only from
startup until the session ends, and its pending marker shows the captured caret
or the end of the selection. The marker is hidden from assistive technology,
takes no clicks and is never title text; its dots stand still with reduced
motion. During listening the toolbar button is highlighted, named "Stop voice
input", shows a waveform that follows the level of the title recording, and
remains available in the toolbar even on a narrow screen. During startup and
conversion it is disabled with a spinner, named "Voice input".

A selection is replaced, including the whole title if selected. A caret inserts
at that position, with the body passage joining rule on both sides: spaces
between Latin-script words, direct joining beside Japanese or Chinese writing,
and no extra space beside existing whitespace. "Orchard notes" with a caret
after "Orchard" and "harvest" becomes "Orchard harvest notes". After success
the title is editable with focus and the caret after the dictated words, so a
typed correction or another recording continues there.

The current typed title draft and selected range survive the toolbar journey
and recording. The result follows ordinary title validation and saving: a note
without inbound references saves normally; a linked title shows its reference
panel and saves only after the author chooses how reference text should change.
Leaving without that choice discards the proposed rename.

When no words are returned, the title is unchanged and editing resumes without
a message. Microphone startup failure restores editing and shows "Could not use
the microphone. Allow microphone access in your browser, then try again."
Conversion failure restores editing and shows "Could not turn your speech into
text." The captured caret or selection is restored, and the author can type or
start a fresh recording. Title recordings have no retained-audio retry; content
retains its ongoing conversion and retry policies. A fresh recording chooses
its field again from current editing intent.

Moving to another note while title recording is active stops the recorder and
ends its insertion session. Its speech writes to neither note. The destination
note shows its own editable title and an idle toolbar, without taking title
focus. Readers see the title as text; editable authors have the toolbar action.
