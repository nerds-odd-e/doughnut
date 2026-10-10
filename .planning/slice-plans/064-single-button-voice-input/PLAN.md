# Record voice input with one waveform button

Work item: **SEED-066#single-button-voice-input**

**Source:** [story](../../seeds/SEED-066-voice-input.md#single-button-voice-input)

## Goal and scope

A note author dictating into a note's body starts and stops dictation from
the one Voice input button in the note toolbar and sees from that button
alone that their voice is being captured. The Audio tools panel, its
waveform strip, Record, Stop, Write text now, the Microphone chooser and the
separate Retry control are gone without trace. Recovery after a failed
conversion at Stop lives in the button, and leaving the note ends that note's
dictation.

Included: the four slices below. Each removal follows the story's removal
scope and principle 7 in `AGENTS.md`: delete the thing and everything only it
used, sweep the product, keep no absence check.

Excluded (see the story): a retry action inside the toast, a recording or
kept recording that survives navigation, any replacement for Write text now,
a microphone chooser elsewhere, a keyboard shortcut, Speak the Title changes
(next story), backend or generated API changes, any change to the processing
cadence, the hold-back rule, or the joining rule.

Assumptions: the toolbar's existing on/off styling (`daisy-btn-soft
daisy-btn-primary` for a pressed toggle, ghost otherwise) is the highlight;
the common error toast is `useToast().showErrorToast`; the button draws its
waveform from `audioRecorder.getAudioData()` as the strip does today.

## Known architecture

- **Entry point today.** `noteMoreOptionsPlainActions.ts` lists `audio` as a
  toggleable plain action (icon `Mic`, title `noteMoreOptionsTitles.audio` =
  "Audio tools"); `NoteMoreOptionsActions.vue` renders it as a toolbar
  button or an overflow-menu item; `NoteToolbarMoreOptions.vue` pins `audio`
  while `useNoteToolbarPanel().isAudioOpen`; `NoteToolbar.vue` renders
  `NoteAudioTools.vue` inside `NoteToolbarPanelShell.vue` while the panel is
  open. `useNoteToolbarPanel.ts` holds `none | audio | assimilation`;
  Assimilate uses the same shell and stays.
- **Recording today.** `NoteAudioTools.vue` owns the phases `ready |
  recording | stopping | notConverted`, the recorder (`createAudioRecorder`
  with `useNoteAudioProcessing(note).processAudio`), the wake lock, the
  Microphone chooser (`switchAudioDevice`, with its failure toast), Write
  text now (`tryFlush`), Retry (`stopRecording` again while `notConverted`),
  and stops the recorder on unmount. `Waveform.vue` draws a scrolling canvas
  strip. `useNoteAudioProcessing.ts` converts, appends through
  `noteStore.appendDictatedText`, and raises the two conversion-failure
  toasts. `audioRecorder.ts` keeps the `devicechange` listener that switches
  to the first microphone when the current one disconnects; it stays.
  `SpeakTitleControl.vue` has its own recorder and does not change.
- **Navigation today.** `NoteShow.vue` renders one `NoteToolbar` whose
  `note` prop changes when the author moves to another note; the panel and
  its recorder survive, and results go to the originating note (specs
  "targets the originating note's current body after the prop changes" and
  "adds the passage to the saved body of a note the author has left").
  Leaving the note page unmounts the toolbar and stops the recorder.
- **Design for the button.** The waveform needs its own rendering, so the
  button is a dedicated component mounted in `NoteToolbar.vue`'s button
  group at the audio action's place, hidden while overflowed and idle, as
  the wiki `PopButton` is. The overflow-menu item starts it through the
  toolbar the way `open-wiki` and `open-new` reach their refs. The toolbar's
  pin reads the recording state the way it reads `isAudioOpen` today; a
  module-level composable like `useNoteToolbarPanel` or a prop is the
  implementer's choice. The button carries `aria-pressed` while recording
  and its `title`/`aria-label` names what a click does. After the panel
  goes, `useNoteToolbarPanel` holds only Assimilate; folding it into
  `useAssimilationView` is post-change refactoring, not a slice.
- **Proof entry points.** Mounted specs `frontend/tests/notes/NoteAudioTools.*.spec.ts`
  mount the panel through `noteAudioToolsTestSupport.ts`
  (`mountNoteAudioTools`, `startRecording`, `stopRecording`,
  `findButtonByText`, `processAudio`) with `noteAudioToolsMocks.ts`,
  `noteAudioToolsSavedContentTestSupport.ts` and
  `noteAudioToolsTypingTestSupport.ts`; toasts through
  `toastTestSupport.ts`. Toolbar specs `NoteToolbar.panels.spec.ts`,
  `NoteToolbar.overflow.spec.ts`, `NoteMoreOptionsForm.spec.ts` and
  `noteToolbarOverflow.spec.ts` through `noteToolbarTestHelpers.ts`. E2E
  `record_live_audio.feature` (mocked) and
  `record_live_audio_with_real_open_ai_service.feature` share the steps in
  `audio.ts`, `audioToolsPage.ts`, `noteMoreOptionsForm.openAudioTools` and
  `notePage.audioTools`. North Star "One real-service audio test": no new
  real-service scenario; the real-service feature follows the page-object
  change only.
- **Documentation.** `docs/voice-input.md` describes the panel, its
  controls, Retry's lifetime and the toast wording; each slice rewrites the
  paragraphs it changes.

## Premises observed (2026-10-10, this worktree at `140752249c` plus the refined seed)

- Baseline:
  `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteAudioTools tests/notes/NoteToolbar tests/composables/noteToolbarOverflow tests/notes/NoteMoreOptionsForm tests/models`
  passes (25 files, 175 tests, 25 s);
  `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/record_live_audio.feature`
  passes (5 scenarios, 28 s, exit 0).
- Kept audio joins the next recording (slice 2's interim recovery):
  `audioProcessingScheduler.stop.spec.ts` "keeps the audio of a failed
  conversion at Stop for the next recording" passes in that baseline.
- The toolbar survives a note change with recording on: the two navigation
  specs named above pass, so slice 4 adds the stop on note change rather
  than relying on an unmount.
- The pin exists: `NoteToolbar.overflow.spec.ts` "pins audio on a narrow
  toolbar then returns it to overflow when turned off" passes; slice 2
  re-keys it to the recording state.
- Not observed: how the live waveform reads inside a toolbar-sized button.
  It is a visual judgment, checked by eye during slice 2 on the dev server;
  the specs prove the states, not the look.

## Outside-in proof

| Promise | Proof | Slice |
| --- | --- | --- |
| Clicking Voice input starts capture at once; the button is highlighted with a live waveform; no panel | recording spec: idle name and no pressed style → click → `aria-pressed`, pressed style, canvas drawn from the recorder; toolbar panels spec: no panel shell for voice input; E2E scenario 1 starts from the toolbar button | 2 |
| Clicking the active button stops; button unavailable in a finishing appearance until the text is added, then idle | controlsDuringConversion spec (today's "keeps Record unavailable from Stop until the last text has been added" retargeted); E2E scenario 1 content assertions | 2 |
| Started from the "more options" menu on a narrow screen, the active button is in the toolbar | NoteToolbar.overflow spec (today's pin test retargeted); NoteMoreOptionsForm spec: menu item starts recording and closes the menu, menu omits it while active | 2 |
| Panel, waveform strip, Record, Stop, Write text now, Microphone chooser gone | sweep reading: `NoteAudioTools`, `Waveform`, `Audio tools`, `Write text now`, `tryFlush` callers, `switchAudioDevice` callers other than the `devicechange` listener, `audioToolsPage` return nothing | 2 |
| Microphone refused → toast, button idle; mid-speech failure → toast, recording continues; silent recording ends quietly; failed save → toast only | recording, processing and controlsDuringConversion specs retargeted | 2 |
| Dictated text preserved, joined and saved as before | preservation, languageJoining, typingWhilePending specs retargeted; E2E scenario 1 | 2 |
| Failed Stop conversion → kept recording in the button, toast with the retry wording; click retries without the microphone; success → idle; failure → toast again, still kept | retry spec retargeted (names, no `getUserMedia` on retry); E2E scenario 2 retries from the button | 3 |
| Leaving the note stops recording and converts into the note left; new note's button idle; kept recording dropped; failure on leaving toasts without "kept" | preservation and typingWhilePending navigation specs retargeted; a toolbar spec changes the note prop while recording | 4 |
| Reload drops a kept recording | nothing to build: state lives in the page; stated in `docs/voice-input.md` | 3 |

## Ordered slices

### 1. The recording session is separable from the panel
Type: Structure
Status: planned
Proof: the nine `NoteAudioTools` specs, `NoteToolbar.panels.spec.ts` and
`record_live_audio.feature` stay green with the panel unchanged.

Internal change: move the panel's phases, recorder, wake lock, `start`,
`stop`, `hasKeptRecording`, `retry`, `tryFlush` and `switchAudioDevice`
calls out of `NoteAudioTools.vue` into one composable bound to the note
(name the implementer's), leaving the panel a renderer of that session.
Keep `processAudio` reachable from the mounted component for the test
support. Enables slice 2, whose button renders the same session. About
5 min. Stop-safe: nothing visible changes.

### 2. One toolbar button starts and stops dictation; the panel is gone
Type: Behavior
Status: planned
Proof: retarget `noteAudioToolsTestSupport.ts` to mount the button
component and click it by its accessible name; rename the
`NoteAudioTools.*.spec.ts` files and the three support files to the
component's name, keeping each test whose behavior survives.
Recording spec: idle button named "Voice input" without the pressed style
→ click → `getUserMedia`, wake lock, `aria-pressed="true"`, pressed style,
name "Stop voice input", the canvas drawn from `getAudioData` → click →
recorder stopped, wake lock released, name back to "Voice input"; "can start
a second recording after stop"; "explains a microphone that cannot be used"
leaves the idle button; "stops recording when unmounted while recording";
delete the Microphone chooser and Write text now tests.
controlsDuringConversion spec: the button is disabled from the stop click
until the final text is added, then enabled and idle; "returns to idle,
adding nothing, when Stop found no speech" with no toast; "offers the idle
button again when saving the dictated text fails"; delete the Write text now
tests. processing, preservation, languageJoining, typingWhilePending specs:
same assertions through the new entry point. retry spec, interim: the Stop
failure toasts "Could not turn your speech into text. Your recording is
kept." and the button returns to idle; starting and stopping again converts
the kept audio once (the scheduler spec already proves the buffer; this
test observes the body). `NoteToolbar.panels.spec.ts`: "starts and stops
voice input from the inline button" (pressed style and `aria-pressed`, no
panel shell), delete "hides assimilation when audio opens and vice versa".
`NoteToolbar.overflow.spec.ts`: "pins Voice input on a narrow toolbar while
recording then returns it to overflow when stopped" (menu item click →
button in the toolbar, pressed). `NoteMoreOptionsForm.spec.ts`: the menu
item starts recording and closes the menu; the menu omits it while
recording. `noteToolbarOverflow.spec.ts` unchanged except the id name if it
changes. E2E: `audioToolsPage.ts` becomes a voice-input page object that
clicks the toolbar button by name through `clickToolbarOverflowAction` and
waits for the idle name to be enabled after stopping; scenario 1 unchanged
in wording; scenario 2, interim: after the toast and the unchanged body,
`Given the OpenAI transcription service now returns …`, `When I start
recording audio for the note …`, `And I stop recording audio`, then the
content assertions; delete the Retry steps until slice 3. Sweep reading:
`NoteAudioTools`, `Waveform`, `NoteToolbarPanelShell` for audio, `Audio
tools`, `Write text now`, `Microphone` chooser, `audioToolsPage`,
`openAudioTools`, `isAudioOpen`, `toggleAudio` return nothing over the
product.

Behavior: an author with an editable note open clicks Voice input → capture
starts at once, the button is highlighted and its icon is the live waveform;
clicking it again stops capture, the button is unavailable in a finishing
appearance until the remaining speech is added and saved, then idle. From
the "more options" menu the item starts recording, the menu closes, and the
active button sits in the toolbar. Delete `NoteAudioTools.vue`,
`Waveform.vue`, the `audio` plain action, `noteMoreOptionsTitles.audio`
(add the new titles), the audio branch of `useNoteToolbarPanel` and
`NoteToolbar.vue`, the Microphone chooser's switch call and its "Failed to
switch audio device" toast, `tryFlush` if nothing else calls it,
`audioToolsPage.ts`, `notePage.audioTools`, `assumeAudioTools`,
`openAudioTools`. Rewrite `docs/voice-input.md`'s first paragraph and the
Retry paragraphs for the interim. Interim behavior, replaced by slice 3: a
kept recording has no retry control and joins the next recording's first
conversion. Interim behavior, replaced by slice 4: recording continues
across a note change as today. About 10 min: the button, the toolbar
wiring and the panel's deletion are one change the product cannot hold
halfway, and the spec retargeting runs through one support file.
Stop-safe: body voice input works from the button with the interim
recovery.

### 3. The button keeps a failed recording and retries it
Type: Behavior
Status: planned
Proof: retry spec: a failed conversion at Stop toasts "Could not turn your
speech into text. Your recording is kept until you leave this note; click
Voice input to try again.", the button is enabled, named "Retry turning your
speech into text", with neither the idle nor the pressed style; clicking it
calls no `getUserMedia`, converts once, adds the passage, and the button is
idle; a retry that fails toasts again and keeps the name; a Stop whose
audio was fully converted leaves the idle button; the button is unavailable
while the retry runs. E2E scenario 2: after the toast, `Given the OpenAI
transcription service now returns …`, `When I retry converting my speech`
(clicks the button by its retry name and waits for the idle name), then the
content assertions; delete "I should be offered Retry for my recording" or
re-point it to the button's name.

Behavior: the conversion at Stop fails → the button holds the kept
recording and offers the retry by name; click → the kept audio is converted
without the microphone, the text joins the body once, the button returns to
idle. `docs/voice-input.md`: the kept-recording paragraph says it lasts
while the author stays on the note and is dropped by reload or leaving.
About 6 min. Stop-safe: full recovery from the button; navigation still as
today.

### 4. Leaving the note ends that note's dictation
Type: Behavior
Status: planned
Proof: preservation spec "converts the remainder into the note the author
left when the note changes" (the prop changes while recording → the
recorder is stopped, the final chunk's text lands in the originating note's
body, the button is idle for the new note); typingWhilePending "adds the
passage to the saved body of a note the author has left" keeps its
assertion through that stop; a retry spec case: a kept recording is dropped
when the note changes, and the button is idle; a case where the final
conversion on leaving fails toasts "Could not turn your speech into text."
with nothing kept. `NoteToolbar.overflow.spec.ts` or panels spec: the
button is not pinned after the note changes. No E2E change (the mounted
boundary observes the journey; North Star prefers mounted proof).

Behavior: the author navigates to another note or page while recording →
recording stops, the remaining speech is converted into the note they left,
and the new note's button is idle; a kept recording does not follow. The
unmount path stays as today. `docs/voice-input.md` states it. About 5 min.

## Current decisions

- Button names: "Voice input" while idle and while finishing; "Stop voice
  input" while recording; "Retry turning your speech into text" while
  holding a kept recording. `aria-pressed` only while recording. The
  highlight is the toolbar's pressed toggle style; the finishing and kept
  appearances are the implementer's, distinct from idle and recording.
- Toast wording: mid-speech failure "Could not turn your speech into text.
  Your recording is kept."; Stop failure, from slice 3, "Could not turn your
  speech into text. Your recording is kept until you leave this note; click
  Voice input to try again."; a failed final conversion when leaving the
  note, from slice 4, "Could not turn your speech into text."; microphone
  refused unchanged. No action inside a toast.
- Slice 2's interim Stop-failure wording is the mid-speech sentence.
- Spec and support files are renamed to the new component's capability
  name; test names that describe surviving behavior are kept.
- The Microphone chooser's two tests go with it; the `devicechange`
  auto-switch keeps today's coverage (none beyond the recorder's own code).
- The real-service feature changes only through the shared page object.

## Learnings

(none yet)
