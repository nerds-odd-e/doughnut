# Read-only voice input with insertion feedback

**Source:** [Show where voice text will arrive while keeping its input read-only](../../seeds/SEED-066-voice-input.md#read-only-voice-input-with-insertion-feedback)
**Identity:** SEED-066#read-only-voice-input-with-insertion-feedback

## Goal and scope

For note authors dictating into a note body or speaking a title, the place
where the spoken text will land is fixed when voice input starts, the targeted
input takes no edits until the session ends, and inline pending feedback marks
that place until the last text has arrived.

Included: body dictation lands at the author's insertion point in the rich
editor and the Markdown editor instead of the end of the body, with the
title's selection and CJK/space rules; the targeted body editor or title is
read-only for the session; an animated pending marker sits after the insertion
point in the rich editor and the title through recording, the conversion after
Stop, and a retry conversion, following each passage that arrives; the session
ends by restoring editing with the caret after the words, on failure, or when
nothing was heard; the retry of a kept recording is a session of its own;
typing into the open body editor while a passage is pending is removed with
its tests and helpers; `docs/voice-input.md` describes the new behavior.

Excluded (deferred by the story): a marker inside the Markdown text area; a
new landing place for the remainder when the author leaves the note while
recording (it keeps joining the end of the saved body); changes to the
buttons, their names, the toasts, or title generation; a cancel control.

Assumptions: the author stays on the note for the session; normal content
undo restores the body before a session's insertion as today; body and title
voice input stay independent sessions.

Preparation observations were made in
`/Users/terryyin/git/doughnut/.worktrees/show-where-voice-text-will-arrive-while-keeping`,
branch `claude/show-where-voice-text-will-arrive-while-keeping`. Honoka-chan's
preparation assignment was announced at
`88846d66b53bd94f6bd761b6b57db44df69d3f3b`. This plan grants no execution or
publication authority.

## Existing solution and constraints

- **Insertion at a selection exists on every surface (PFE).** The title's
  `SeamlessTextEditor.insertAtSelection(compose)` composes
  `dictatedInsertion(before, segments, after)` at the remembered selection and
  leaves the caret after the words. The body's `useContentCursorInserter`
  inserts plain text at the last known Quill range (`insertTextAtCursor`) or
  the captured textarea selection, falling back to the end when none. Reuse
  these: give the body editors the title's composer shape and the
  `dictatedInsertion` rule, and drop the separate end-of-body join for an open
  editor.
- **One dictation-target contract for all three surfaces.** A session begins
  by asking the targeted input for a dictation target: the input captures its
  current caret or selection (or the end), becomes read-only, and returns a
  handle that inserts composed segments at the anchor, advances the anchor past
  each insertion, reports the anchor for the marker, and ends by restoring
  editing with the caret after the words. The body reaches its target through
  the existing note-to-open-editor registration in
  `noteContentMutationBarrier.ts` (today `changeDraft`), whose only consumer is
  `NoteTextEditing.appendDictatedText`; when no editor is open or admission is
  closed, the saved-body end join stays. The title reaches it through the
  `busy` signal `SpeakTitleControl` already emits to `NoteEditableTitle` and
  `NoteNewForm`.
- **Capture the anchor before locking.** Observed 2026-10-11 in this
  workspace with a scratch mounted test (file removed): after
  `quill.setSelection(12, 0)`, `getSelection()` is `{index: 12, length: 0}`;
  after `quill.enable(false)` it is `null`, `root` has `contenteditable="false"`,
  `insertText(12, "Inserted. ", "api")` still changes the text to
  `Alpha beta.\nInserted. Gamma delta.\n`, `getBounds(22)` returns
  `{left: 83.17, top: 36.72, height: 18, width: 0}`, and `enable(true)`
  restores `contenteditable="true"`. The user agent was a Macintosh browser:
  the frontend unit suite runs mounted tests in a real browser, so the marker's
  position can be asserted against `getBounds`.
- **The marker is an overlay, never editor content.** `syncQuillFromModel`
  rewrites `root.innerHTML` whenever the model changes and `SeamlessTextEditor`
  keeps a single text node, so an inline element in either editor would be
  wiped or break the editor's invariant and could leak into saved Markdown. The
  marker is a positioned element beside the editor, placed from the anchor's
  bounds (`quill.getBounds`, already used by the paste action bar; a DOM
  `Range` rect in the title) and repositioned after each insertion. The
  implementer chooses a visual that stays readable over following text, with an
  animated ellipsis as the owner's example, still under
  `prefers-reduced-motion`, and `aria-hidden`.
- **New note placeholder folds into the selection rule.** `heardWordsReplaceTitle`
  decides that an untouched `Untitled` is replaced. The target for that case is
  the whole title, so one rule (selection replaced, marker after the target)
  covers it; the helper's rule moves into the target choice.
- **Audio proof stays mocked.** Follow the North Star topic
  [One real-service audio test](../../NORTH-STAR.md#one-real-service-audio-test):
  mounted tests with mocked `AiAudioController` and the
  `@usingMockedOpenAiService` feature `record_live_audio.feature` prove
  everything here. Its body scenarios never place a caret, so their end join
  is unchanged; running the feature at the slice-1 boundary confirms this.
- **Removal leaves no trace** (`AGENTS.md` principle 7): the typing-while-pending
  tests in `NoteVoiceInputButton.typingWhilePending.spec.ts` and the typing
  parts of `noteVoiceInputButtonTypingTestSupport.ts` are deleted. Four tests
  in that file are not about typing (saves as soon as it joins; saved body
  while a save-then-change pause holds the editor; saved body of a note the
  author has left; Japanese passage joined without a space) and move to the
  preservation spec with the held-dictation mount support they need.
- No Accepted ADR governs this editor behavior; ADR 0006 keeps conversion and
  save failures propagating to their existing toasts. No new North Star topic.

## Outside-in proof ownership

| Source promise | Observable proof | Owner |
| --- | --- | --- |
| Body text lands at the caret between two passages in the rich editor; successive passages follow in place; saved body holds them there | Mounted rich editor + toolbar button, mocked audio: caret set between passages, two mid-speech passages then Stop; editor text and last saved content exact | 1 |
| Body: a selection is replaced, and no caret means the end | Mounted tests: selection replaced with CJK/space rule on both sides; fresh mount without a caret joins the end (existing preservation tests stay green) | 1 |
| Body editor is read-only during the session and clicks or typing do not move the target | Mounted test: `contenteditable="false"` while pending; a programmatic selection change during pending does not change where the passage lands; editable again after Stop with the caret after the words | 1 |
| Same in the Markdown editor | Mounted textarea tests: `readonly` while pending, text lands at the captured selection, caret after the words | 1 |
| Failed conversion at Stop releases the editor; retry locks again and lands at the caret at the retry click | Mounted test: failure toast, editable again; move caret, retry, passage lands there | 1 |
| Existing body E2E journeys unchanged (end join, retry) | `record_live_audio.feature` body scenarios green | 1 |
| Typing while pending removed without trace | Removal sweep reading over `frontend/`, `docs/`, `.agents/` for the removed tests, helper names and the "unsaved typing"/"end of that editor's draft" wording returns nothing; retained non-typing tests green | 1 |
| Title on an existing note is read-only while speaking; words land at the anchor captured at start; caret after them | Mounted `NoteEditableTitle` tests: `contenteditable="false"` while busy; selection moved during listening does not change landing; existing placement tests green | 2 |
| New note: untouched `Untitled` is the whole-title target; other titles use caret/selection; Submit withheld while busy as today | Mounted `NoteNewForm` spoken-title tests (existing outcomes stay green, placeholder through the target rule) | 2 |
| Title E2E journeys unchanged | `record_live_audio.feature` title scenarios green | 2 |
| Marker after the body insertion point from start through Stop processing, following each passage, cleared at end | Mounted test in the real browser: marker present with position matching `getBounds(anchor)`; moves after a passage; absent after Stop completes | 3 |
| Marker cleared on failure, nothing heard; present during retry conversion | Mounted tests for each ending | 3 |
| Marker visible in the real journey | `record_live_audio.feature` lone-segment scenario: marker visible while recording, absent after Stop | 3 |
| Marker after the title target (existing note and after `Untitled` in New note), cleared when words land | Mounted spoken-title tests; E2E "Create a note by speaking the title": marker visible while listening, absent after | 4 |
| `docs/voice-input.md` describes read-only sessions, caret insertion, and the marker | Doc updated in the slice that delivers each behavior | 1–4 |

## Ordered slices

### 1. Body dictation lands at the author's insertion point in a read-only editor
Type: Behavior
Status: done
Proof: mounted rich-editor and Markdown-editor tests with the toolbar button
and mocked audio (caret between passages, selection replaced, no caret → end,
read-only while pending, selection change ignored, retry at the new caret,
save right after each passage); `record_live_audio.feature` body scenarios;
removal sweep reading.

Behavior: an editable note with a body editor open and the caret between two
passages → the author clicks Voice input → the editor is read-only; each
passage that arrives, mid-speech or at Stop, is inserted at the anchor with
`dictatedInsertion` and saved right away, the anchor moving past it; when the
session ends the editor is editable with the caret after the dictated text,
and the saved body holds the text between the passages. A failed conversion at
Stop ends the session (toast as today) and the retry begins a new session at
the caret at that moment. Without an open editor the saved-body end join is
unchanged. The Markdown editor behaves the same through its captured
selection. Delete the typing-while-pending tests and helper code, move the
four non-typing tests, and update `docs/voice-input.md` (open-editor join,
read-only session, retry).

Sizing note: one contract with two editor implementations and the removal; it
exceeds the usual leaf because splitting the editors would leave the open-editor
contract half replaced. Stop and reassess if it passes the L band.

### 2. The title keeps its target and takes no edits while being spoken
Type: Behavior
Status: planned
Proof: mounted `NoteEditableTitle` and `NoteNewForm` spoken-title tests
(read-only while busy, target fixed at start, placeholder as whole-title
target, caret after the words); `record_live_audio.feature` title scenarios.

Behavior: an editable title with a caret, selection, or the untouched New note
placeholder → the author clicks Speak the title → the title is read-only and
its target is captured; stopping converts and the words replace the target or
land at the caret, then the title is editable with focus and the caret after
the words. Nothing heard or a failed conversion ends the session with the
title unchanged and editable. `heardWordsReplaceTitle` becomes the whole-title
target choice. Update `docs/voice-input.md`.

### 3. A pending marker shows where body text will arrive
Type: Behavior
Status: planned
Proof: mounted real-browser tests comparing the marker's position with
`getBounds(anchor)` and observing it follow a passage and clear at Stop,
failure, nothing heard, and through a retry; the lone-segment E2E scenario
sees the marker while recording and not after Stop.

Behavior: a body session in the rich editor begins → an animated, `aria-hidden`
marker appears immediately after the anchor, repositioned after each inserted
passage, present through recording, Stop processing, and retry conversion →
it disappears when the session ends for any reason. The Markdown editor shows
no marker (deferred). Update `docs/voice-input.md`.

### 4. A pending marker shows where the spoken title will land
Type: Behavior
Status: planned
Proof: mounted spoken-title tests on an existing note and in New note (marker
after the caret, after the selection, after `Untitled`; gone when the words
land or the session fails); E2E "Create a note by speaking the title" sees the
marker while listening and not after.

Behavior: a title session begins → the same marker appears after the title's
target, using the selection's DOM rect → it disappears when the words land or
the session ends otherwise. Update `docs/voice-input.md`.

## Current decisions

- One dictation-target contract (begin → handle with insert, anchor, end) on
  the rich editor, Markdown editor, and title; the body reaches it through the
  existing open-editor registration, the title through the existing `busy`
  signal.
- The anchor is captured before the input is locked; Quill drops its
  selection when disabled.
- The marker is an overlay positioned from the anchor's bounds, never editor
  content.
- Retry begins a new session at the caret at the retry click.
- Remainder after leaving the note keeps the saved-body end join.
- The dictation-target contract is `DictationTarget` (`insert`, `end(placeCaret)`)
  in `frontend/src/models/audio/dictationTarget.ts`. The rich editor's target is
  `quillDictationTarget.ts`; the body's editor choice and Markdown target are
  `useBodyEditorDictation.ts`; the open-editor session lives in
  `noteContentMutationBarrier.ts` (begin, insert with immediate save, end).
- A body session begins once the recorder has started, so a microphone that
  cannot start locks nothing. Leaving the note ends the session without moving
  focus (`end(false)`).

## Learnings

- Slice 1: `TextContentWrapper.vue`, `NoteEditableContent.vue`, and
  `RichMarkdownEditor.vue` sit at the 250-line limit. The marker's anchor
  belongs in `quillDictationTarget.ts` and `useBodyEditorDictation.ts`; the
  anchor today is a closure variable in `quillDictationTarget`, and the
  Markdown and end-of-text targets have none to report.
- Slice 1 accepted proof: `NoteVoiceInputButton.insertionPoint.spec.ts` (where
  text lands, saved content), `NoteVoiceInputButton.readOnlyEditor.spec.ts`
  (read-only session, session end, retry), whole frontend suite, both
  typechecks, and `record_live_audio.feature` 5 of 5, all on the delivered
  code. Removal sweep reading over `frontend/src frontend/tests docs .agents
  e2e_test` for the removed test and helper names and the "unsaved typing" /
  "end of that editor's draft" wording returned nothing.

## Execution

Story Branch Mode in
`/Users/terryyin/git/doughnut/.worktrees/show-where-voice-text-will-arrive-while-keeping`
on `claude/show-where-voice-text-will-arrive-while-keeping`; claim published on
`origin/main` at `783d3f96cd19a2fcdab03355f31008c1e033164d`. Increments publish
to the remote execution branch. CI source: GitHub Actions on that branch.

## Story obligations

### G1. Edits during a session are observed through the read-only attribute
Reported: slice 1 — "Real keystrokes, paste, or mouse clicks during a session; only the read-only attribute and a programmatic selection change are observed."
Story clause: "the targeted body editor or title takes no typing, pasting, or other edits"
Disposition: proved by slice 1: `frontend/tests/notes/NoteVoiceInputButton.readOnlyEditor.spec.ts` asserts `contenteditable="false"` and `readOnly` for the whole session in a real browser, which is the browser's own refusal of typing and pasting

### G2. A CJK selection is replaced in the Markdown editor only
Reported: slice 1 — "A CJK selection replaced in the rich editor (Latin only there; the rule itself is in `joinDictatedSegments.spec.ts`)."
Story clause: "The words join with the CJK/space rule the title uses on both sides"
Disposition: proved by slice 1: both editors compose through `dictatedInsertion`; `frontend/tests/notes/NoteVoiceInputButton.insertionPoint.spec.ts` observes the Latin selection in the rich editor and the Japanese selection in the Markdown editor

### G3. The session outlives a Voice input button that unmounts on the same note
Reported: slice 1 — "The voice button unmounting while the editor stays on the same note: the session then runs until that Stop finishes and focuses the editor."
Story clause: "Stop while conversion is pending → the input stays read-only with the indicator until the text has arrived, then editing resumes."
Disposition: no user cost "fix the place where the spoken text will land at the moment they start": the unmounting button stops the recording, so the session ends as it does at Stop and editing resumes

### G4. Switching editors during a session
Reported: slice 1 — "Switching rich/Markdown during a session continues at the end of the new editor, read-only."
Story clause: "Clicks and caret moves after the start do not move the target."
Disposition: no user cost "fix the place where the spoken text will land at the moment they start": the editor that held the target is gone after the switch, so the text is kept, saved, and shown in the editor now open, at that editor's last caret or its end, as `docs/voice-input.md` states

### G5. The target is taken when recording has started
Reported: slice 1 — "Session begins after the recorder starts, not at the click."
Story clause: "When voice input starts, the target is the caret or selection the input has"
Disposition: no user cost "fix the place where the spoken text will land at the moment they start": recording starting is the start of voice input, and it keeps a microphone that cannot start from locking the editor
