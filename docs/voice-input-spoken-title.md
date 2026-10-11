# Speaking a title

An author can speak a note's title in New note and on an existing note's
page. Dictating into a note's body, the pending marker's appearance, the
CJK/space rule of body passages, and the tests of all of these are described
in [voice input](./voice-input.md).

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
clicking and selecting do not change it or move that place, and the pending
marker sits right after that place, after the caret or after the end of the
selection, until the session ends for any reason, moving to another note
included. The CJK/space rule of body passages applies
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
