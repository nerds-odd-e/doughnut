# Use one reactive voice-input button for note titles and content

**Identity:** SEED-066#unified-voice-input-control

**Source:** [refined story](../../seeds/SEED-066-voice-input.md#unified-voice-input-control)

## Goal and scope

A note author sees the same button behave the same way when speaking a new
note's title, an existing note's title, or a note's content: a spinner while
the microphone starts, a live waveform while it records, a spinner until the
speech has become text, then the microphone again.

Included: one shared button implementation for the New note title field and
the note toolbar; the waveform in both title journeys; the startup spinner in
all three journeys, which content dictation gains; deleting
`SpeakTitleControl.vue` and everything only it used; the two voice-input
documents.

Kept as it is: names by place ("Speak the title" / "Stop speaking the title"
in New note; "Voice input" / "Stop voice input" / "Retry turning your speech
into text" in the toolbar), each place's frame, the title-or-content choice by
title focus, where words go, the read-only field and pending marker, title
conversion only at Stop, content retry, the error toasts, and Submit being
unavailable in New note during a session.

Not promised: a retry for titles, another waveform look, a reduced-motion
waveform, choosing a microphone, any transcription change.

No execution is authorized by this preparation.

## Architectural approach and existing solutions

The existing solution is the waveform button face inside
`NoteVoiceInputButton.vue`: the `<button>`, its four faces, and the canvas
drawing. Move that face into one shared button component and use it from both
places. The shared button owns only what the author sees and the click. It is
told the state (idle, busy, recording, kept recording), where to read the
live level, its names, and its frame classes. It knows nothing about titles,
content, or notes.

`NoteVoiceInputButton.vue` stays the toolbar's owner of the title-or-content
choice and of content recording; `NoteNewForm.vue` uses the shared button
directly with its title dictation. Title recording
(`useTitleVoiceInput.ts`) gives out the live level of its current recorder;
content recording (`useNoteVoiceInput.ts`) gains a starting state. Text
application stays with each editor.

No Accepted ADR (0000–0007) governs this front-end structure, and no North
Star topic is needed: the change is local to the note voice controls. The
[one real-service audio test](../../NORTH-STAR.md#one-real-service-audio-test)
topic applies to proof: no new real-service journey.

## Outside-in proof

Mounted Vitest browser specs under `frontend/tests/notes/` are the proof
boundary: they mount the New note form, the note page, and the toolbar with
a mocked recorder. The whole folder runs in seconds, so every slice runs it
instead of hand-picking consumers:

```bash
env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit
```

The typechecks are a local requirement of the `frontend` skill's
"Frontend proof" rule. Run the commands from the repository root.

| Promise | Slice | Observation |
| --- | --- | --- |
| Existing title shows a waveform that follows the title recording | 2 | `NoteShow.toolbarTitleVoiceInput.spec.ts`: while recording, the "Stop voice input" button holds a canvas and the title recorder's `getAudioData` was read |
| New note shows the same button: startup spinner, waveform, spinner after Stop, names, place before Wikidata | 3 | `NoteNewForm.spokenTitle.spec.ts` with `spokenTitleTestSupport.ts`: startup held → unavailable with spinner; listening → highlighted, canvas, the recorder's `getAudioData` read, "Stop speaking the title"; converting → unavailable with spinner; then idle |
| One implementation; `SpeakTitleControl.vue` and its exclusive dependents are gone | 3 | Acceptance reading: a search of the product for the deleted names returns nothing. A reading, not a test |
| Content shows the startup spinner and takes no second click | 4 | `NoteVoiceInputButton.recording.spec.ts`: startup held → unavailable with spinner, named "Voice input"; microphone ready → waveform |
| Microphone refused returns to idle in all three, nothing read-only | 3, 4 | Existing refusal specs for New note, toolbar title and content stay green; content's also sees the spinner gone |
| Kept: retry face for content, title failure returns to idle, toolbar pinning, markers, Submit | 1–4 | Existing specs in `tests/notes` stay green |
| The journeys still work through the product | hosted CI | `record_live_audio.feature` (mocked service) finds the buttons by their names, which do not change; it runs after publication and is not a local gate |
| Waveform follows a real voice in all three | after execution | Owner's evaluation on a real microphone; not automated |

## Decisive premises

Observed on 2026-10-12 in this workspace at `5a80de7bda`, with scratch
assertions that were reverted. Command: the `frontend:test` line above, for
the three named specs; result 3 failed, 23 passed, each failure the scratch
assertion.

- **The New note button has no waveform while listening.** Scratch assertion
  in `NoteNewForm.spokenTitle.spec.ts` that the "Stop speaking the title"
  button holds a canvas: failed, `expected false to be true`.
- **The toolbar button has no waveform while a title is recorded.** Scratch
  assertion in `NoteShow.toolbarTitleVoiceInput.spec.ts` that the
  "Stop voice input" button holds a canvas after starting with the caret in
  the title: failed, `expected false to be true`.
- **The content button stays available during microphone startup.** Scratch
  test in `NoteVoiceInputButton.recording.spec.ts` holding `startRecording`
  unresolved and expecting the button disabled: failed, no `disabled`
  attribute.
- **The specs can tell recorders apart and reach the level read.** The mocked
  `createAudioRecorder` returns a fresh recorder with its own `getAudioData`
  spy per call (`noteVoiceInputButtonMocks.ts`), and the existing content
  spec already passes on `getAudioData` having been called while the canvas
  is shown. The three specs run and pass here apart from the scratch lines.
- **The live level exists during title recording.** Read, not run:
  `getAudioData` reads the microphone receiver directly and does not depend
  on `convertOnlyAtStop`. Real-hardware confirmation is the owner's
  evaluation.

## Ordered slices

### 1. Shared button face behind the toolbar voice-input button
Type: Structure
Status: done
Proof: `tests/notes` and both typechecks green with no spec changed.
Accepted on 2026-10-12: `tests/notes` 69 files / 376 tests passed, `vue-tsc`
and `tsc` exit 0, no spec changed. The shared button is
`frontend/src/components/commons/VoiceInputButton.vue`.

Move the `<button>`, its faces (microphone, spinner, waveform canvas, retry
arrow) and the canvas drawing out of `NoteVoiceInputButton.vue` into one
shared button component that receives its state, a level reader, its names,
and its frame classes. `NoteVoiceInputButton.vue` renders it and behaves
exactly as before, including no waveform for title recordings. Enables
slice 2 and slice 3.

### 2. Existing-title recording shows the live waveform
Type: Behavior
Status: planned
Proof: `NoteShow.toolbarTitleVoiceInput.spec.ts` observes the canvas in the
"Stop voice input" button and the title recorder's `getAudioData` read while
recording, and the spinner after Stop; `tests/notes` and typechecks green.

Behavior: the caret is in an existing note's title → the author clicks
"Voice input" → after the startup spinner the button is highlighted, named
"Stop voice input", and shows the waveform drawn from the title recording's
level → Stop shows the spinner, then the words are at the caret and the
button is idle.

Title recording gives out the live level of its current recorder.
`docs/voice-input-spoken-title.md` describes the waveform for an existing
title.

### 3. New note uses the shared button
Type: Behavior
Status: planned
Proof: `NoteNewForm.spokenTitle.spec.ts` observes, through
`spokenTitleTestSupport.ts`: idle microphone named "Speak the title" before
the Wikidata button at the field's height; startup held → unavailable with a
spinner; listening → highlighted, "Stop speaking the title", a canvas, and
the recorder's `getAudioData` read; converting → unavailable with a spinner;
then idle with the words in the title. The microphone-refused and
failed-conversion specs stay green. `tests/notes` and typechecks green.
Acceptance reading recorded here: a search of the product (frontend, E2E,
docs, agent guidance, generated artifacts such as `components.d.ts`) for
`SpeakTitleControl` and `fieldJoinAppendToggleButtonClass` returns nothing.

Behavior: New note is open → the author clicks "Speak the title" → spinner,
unavailable, until the microphone is ready → highlighted waveform following
their voice, named "Stop speaking the title" → they click it → spinner,
unavailable → the words are in the title and the microphone is back.

`NoteNewForm.vue` renders the shared button with its title dictation. Delete
`SpeakTitleControl.vue` and what only it used
(`fieldJoinAppendToggleButtonClass`; `FIELD_JOIN_APPEND_BUTTON_CLASS` stays,
the Wikidata and folder buttons use it). `docs/voice-input.md` and
`docs/voice-input-spoken-title.md` describe the New note button as it then
is.

### 4. Content dictation shows the startup spinner
Type: Behavior
Status: planned
Proof: `NoteVoiceInputButton.recording.spec.ts` holds microphone startup and
observes the button unavailable with a spinner, named "Voice input", then the
waveform once the microphone is ready; the refused-microphone spec observes
the toast and an idle, available button; `NoteToolbar.voiceInputOverflow.spec.ts`
observes the button in the toolbar, not the menu, from a click in
"more options" on a narrow screen while startup is held. `tests/notes` and
typechecks green.

Behavior: an existing note's title is not focused → the author clicks
"Voice input" → the button is unavailable with a spinner until the
microphone is ready, so another click does nothing → then the waveform as
today. When the microphone is refused → the error toast, an idle microphone,
and the body editor was not made read-only.

Content recording gains a starting state; the body editor still becomes
read-only only once the microphone has started. `docs/voice-input.md`
describes the startup state.

## Current decisions

- Names stay with the place; startup is a spinner, unavailable, in all three
  (owner, 2026-10-12).
- The shared button is told its state and level; it does not choose between
  title and content and holds no recorder of its own.
- Each behavior slice updates its own part of the documents; there is no
  separate documentation slice.

## Learnings

- The shared button takes `state` (`idle`, `busy`, `recording`,
  `keptRecording`), an optional `readLevel`, and `names`; its frame classes
  arrive as the caller's `class`. It draws the waveform whenever it is
  recording and has a level reader, so slice 2 only changes what the toolbar
  passes as the reader.
- `names.retry` is required today; slice 3 makes it optional, because New
  note has no retry.
- `frontend/components.d.ts` is regenerated by
  `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vite build --emptyOutDir`;
  slice 3 runs it again after deleting `SpeakTitleControl.vue`.

## Execution

Story Branch Mode in this workspace on
`claude/use-one-reactive-voice-input-button-for-note-tit`; claim published on
`origin/main` at `b10f1df0da`.
