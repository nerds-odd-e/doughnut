# Create a note using a spoken title

## Source

- Story: [Create a note using a spoken title](../../seeds/SEED-066-voice-input.md#create-with-spoken-title)
- Identity: SEED-066#create-with-spoken-title
- Owner decisions, 2026-10-06: build a native "Speak the title" control in
  New note rather than relying on operating-system dictation; the author
  chooses Stop, listening does not end by itself; heard words stay exactly as
  transcribed, including a closing full stop.

## Goal and scope

A note author who is creating a note names it by speaking: in New note they
choose "Speak the title", speak, choose Stop, see the heard words in the title
field, correct them if needed, and submit as usual. The note is created once,
in the chosen location, with the title the author reviewed.

- One control named in words, "Speak the title", that becomes "Stop" while
  listening. The heard words arrive in the title field once, after Stop, as
  one line, exactly as transcribed, segments joined as body passages are
  joined. Nothing appears while the author is still speaking.
- The spoken words go where typing goes: they replace the untouched default
  "Untitled" and otherwise join the end of the title the author already has,
  including a title pattern the dialog opened with.
- A spoken title is an ordinary title: the same search for existing notes,
  Wikidata lookup, illegal character replacement, warnings, validation, and
  Submit. Nothing is created until the author submits.
- A status in words, announced by assistive technology, says recording, then
  turning speech into text, then nothing once the words are in the field;
  when nothing was heard it says so and the title is unchanged.
- While the dialog is listening or turning speech into text, Submit is not
  offered (the note must be created once with the reviewed title).
- When the microphone cannot be used, the first-dictation story's message is
  shown as a problem and "Speak the title" stays available. When the speech
  could not be turned into text, the message says so, the title is unchanged,
  and speaking again sends only the new recording. No Retry control.
- Body dictation on the created note never changes the title; every New note
  behavior not about speech is unchanged.

Excluded: listening ending by itself after silence; tidying the heard words;
speaking the folder or parent relationship; submitting by voice; a spoken
title in the existing-note title editor; keeping a failed recording for
Retry; a microphone chooser or "Write text now" in New note; observing
operating-system dictation or hardware capture.

Considered and left out:

- Reusing `NoteAudioTools` inside New note. It is bound to an existing note,
  writes to the body, and carries Retry, saving audio, full screen and the
  microphone chooser, all excluded here. The title control reuses the
  recorder and the transcription request beneath it, not the panel.
- Making the title callback throw on mid-speech chunks to keep their audio.
  That would run the title flow on the failure path; the recorder gets an
  explicit option instead (slice 2).
- A shared wording module with plan 008. Plan 008 is Taken but not yet
  executed; this plan writes the same words in the title control, and the
  post-change refactor of whichever lands second shares them.
- A probe slice for the mocked microphone inside the New note dialog. The
  mocks are installed on the window, not on the note page (see premises), so
  slice 3's journey is the first observation and its failure is contained
  in that slice.

## Architecture

- Existing solutions reused (PFE):
  - Capture: `createAudioRecorder` → `audioReceiver` → `audioProcessingScheduler`
    (`frontend/src/models/audio/`). The scheduler converts every 60 s, after
    3 s of silence, on Flush, and at Stop; a title needs only the Stop
    conversion, so slice 2 adds the option to skip the timed and silence
    conversions. Each `createAudioRecorder` call has its own buffer, so a
    fresh recorder per Record carries no audio from a failed attempt.
  - Transcription: `AiAudioController.audioToText` already carries only the
    audio and the mid-speech flag and returns `segmentTexts`; no backend, API
    or route change ([ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md)
    untouched).
  - Join rule: the CJK/space rule is a closure inside
    `appendDictatedText` (`frontend/src/store/noteTextEditing.ts`); slice 1
    extracts it as one function both the body and the title use.
  - Title handling: `PathNameEditor` applies illegal-character replacement
    and warnings only on the editor's own input events; `NoteNewForm` shows
    search results only after `hasTitleBeenEdited`. Spoken words enter
    through the same handling as typing (slices 3 and 4), not by writing the
    model directly.
  - Proof support: `noteNewFormTestSupport.ts`, `noteAudioToolsMocks.ts`
    (`audioRecorderMockExports`, browser spies), `mockSdkService(AiAudioController,
    "audioToText", …)`, the E2E `noteCreationForm` page object, and
    `openAiService.stubTranscription` / `browser.mockAudioRecording` /
    `receiveAudioFromMicrophone`.
- One component owns the title control's state (idle, listening, converting,
  nothing heard, failed) from the recorder's and request's real events, and
  emits the heard segments; `NoteNewForm` owns where they go and whether
  Submit is offered. The start-failure and conversion-failure messages are
  catches with a business outcome and a clearer message
  ([ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md)); other
  failures stay loud.
- [NORTH-STAR "One real-service audio test"](../../NORTH-STAR.md#one-real-service-audio-test)
  governs proof: the journey is proved with the mocked transcription and
  mounted tests; no real-service scenario is added.
- Plan 008 changes `NoteAudioTools.vue` and its tests; this plan does not
  touch them. Slice 1 touches `noteTextEditing.ts`, which plan 008 reads but
  does not change.

## Outside-in proof

| Promise | Slice | Proof |
| --- | --- | --- |
| Body passages still join by the same rule after extraction | 1 | `NoteAudioTools.languageJoining` and `NoteAudioTools.preservation` stay green; a focused test of the extracted function on the documented examples |
| A recorder created for one Stop conversion never converts mid-speech; Stop converts everything with the mid-speech flag false | 2 | Model test in `tests/models/audio`: with the option, the timer tick and the silence threshold cause no callback; `stop()` calls back once with `isMidSpeech: false`; without the option the existing tests stay green |
| "Speak the title" in New note, "Stop" while listening, status "Recording. Speak now." then "Turning your speech into text…", words replace the untouched "Untitled", Submit creates the note once with that title (example 1) | 3 | New scenario in the mocked recording journey: open New note from the notebook page, Speak the title, deliver `lecture.wav`, Stop, the Title field reads the stubbed transcript, Submit, the note exists with that title; mounted test on `NoteNewForm` with the audio mocks for the two statuses, the single control, the role that assistive technology announces, and no status once the words are in |
| The search for existing notes runs for the heard title; closing New note while listening stops the microphone | 4 | Mounted tests: search results shown after the words arrive; unmount while listening calls the recorder's `stopRecording` |
| Spoken words join an existing or patterned title with the body join rule (example 3); illegal characters are replaced and warnings shown as for typing; corrected by typing before Submit (example 2) | 4 | Mounted tests: title "2026-10-06 " + segments → "2026-10-06 weekly review"; a typed title plus speech joins with one space; a segment with `/` shows the replacement warning and the fullwidth character; typing after speech and submitting sends the typed title |
| Submit is not offered while listening or converting; offered again afterwards | 4 | Mounted tests: Submit disabled in both states and enabled after the words arrive; Enter in the title field while listening does not submit |
| Nothing heard: "No speech was turned into text.", title unchanged, Submit offered (example 4) | 5 | Mounted test: Stop with a recorder that runs no conversion (silent buffer), and Stop with a response of no segments |
| Microphone cannot be used: the message as a problem, "Speak the title" still offered | 5 | Mounted test: the recorder refuses to start |
| Conversion fails: "Could not turn your speech into text.", title unchanged, Submit offered; speaking again sends only the new recording and the title reads its words only (example 5) | 5 | Mounted test: `audioToText` rejects once, then resolves; a second recorder is created for the second attempt and only its Stop conversion reaches the title |
| Every control in New note has a readable name on a touch screen (example 6) | 3 | Mounted test: the control's visible text in idle and listening states; no title-only name |
| Body dictation never changes the title (example 7) | — | Already delivered and documented ([voice-input documentation](../../../docs/voice-input.md): dictation writes only to the note body); unchanged code, no new proof |
| The documentation describes the dialog as delivered | each slice | `docs/voice-input.md` gains a "Speaking a title in New note" section in slice 3 and is extended in slices 4 and 5 |

Commands (from the story worktree root):

- Frontend tests:
  `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/notes/NoteNewForm tests/notes/NoteNewButton tests/notes/NoteAudioTools.languageJoining tests/notes/NoteAudioTools.preservation tests/models/audio`
  plus the new spec files; the `frontend` skill's proof rule adds the
  typecheck, reported with an unpiped exit code.
- Mocked journey (unset the overrides listed in
  [docs/worktree-browser-tests.md](../../../docs/worktree-browser-tests.md) first):
  `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/record_live_audio.feature`
- The real-service feature runs in CI after a push; it is not a local gate.

## Decisive premises

| Premise | Consumed by | Observation | Result |
| --- | --- | --- | --- |
| The New note and audio mounted tests pass before any change | Every slice's preserved-behavior proof | Ran the frontend command above, 2026-10-06: 8 files, 73 tests passed | Holds |
| The mocked recording journey passes before any change | Slice 3 extends it | Plan 008 ran it green on 2026-10-06 at d5f7249c4b; `git diff --stat d5f7249c4b HEAD -- frontend e2e_test backend` is empty, so the product and tests are identical | Holds |
| The join rule is a closure inside `appendDictatedText`, not exported | Slice 1 exists | Read `noteTextEditing.ts` (`join` defined inside the method) | Holds |
| The scheduler has no way to skip mid-speech conversions; `createAudioRecorder` takes only the callback | Slice 2 exists | Read `audioProcessingScheduler.ts` (`start()` sets the 60 s interval; `wireAudioProcessingScheduler` wires `setOnSilenceThresholdReached` to `tryFlush`) and `audioRecorder.ts` | Holds |
| A callback returning `undefined` marks the whole snapshot processed; a throw keeps the audio | Slice 2's option must skip the conversion, not fake a result | Read `rawSampleAudioBuffer.ts` `processUnprocessedData` | Holds |
| Each `createAudioRecorder` call owns a new buffer | Slice 5: a fresh recorder per Record carries no failed attempt's audio | Read `audioRecorder.ts` (`createAudioReceiver()` per call, buffer from it) and docs/voice-input.md (the same recorder carries kept audio into the next recording) | Holds |
| Silent audio is not sent, so Stop after silence runs no conversion | Slice 5 "no speech" after a silent recording | Read `rawSampleAudioBuffer.ts` (`getProcessableData` returns null when every chunk is silent) | Holds |
| Setting the title model from code bypasses illegal-character replacement and warnings | Slice 4 routes spoken words through `PathNameEditor`'s own handling | Read `PathNameEditor.vue` (`processIllegalPathChars` runs in `onModelUpdate`, bound to the editor's `update:modelValue` only) and `SeamlessTextEditor.vue` (`watch(modelValue)` writes `innerText` without emitting) | Holds |
| Search results show only after `hasTitleBeenEdited`, set by the editor's events or a Wikidata selection | Slice 3 marks the title edited when words arrive | Read `NoteNewForm.vue` (`effectiveSearchKey`, `onTitleChange`) | Holds |
| Enter in the title field submits the form | Slice 4 guards Submit while listening against Enter too | Read `SeamlessTextEditor.vue` (`onEnter` → `form.requestSubmit()`) | Holds |
| Closing the dialog unmounts the form | Slice 3 stops the recorder on unmount | Read `PopButton.vue` (`<Modal v-if="show">`) | Holds |
| The E2E microphone and recording mocks work on any page | Slice 3's scenario opens New note from the notebook page, not a note page | Read `browserService.ts`: `mockAudioRecording` installs on `window:before:load` and the current window; `receiveAudioFromMicrophone` posts to the worklet port captured when the recorder connects | Holds by reading; first run is in slice 3 |
| The mocked journey's Background creates a notebook and the sidebar page object opens New note | Slice 3's scenario | Read `record_live_audio.feature` Background and `noteSidebar.ts` `addingNewNoteFromToolbar()` returning `noteCreationForm` | Holds |
| `audioToText` is mocked in mounted tests through `mockSdkService` and can reject once | Slice 5's failure-then-success test | Read `NoteAudioTools.processing.spec.ts` (`mockResolvedValueOnce(wrapSdkError(...))`) | Holds |
| A refused permission in a real browser reaches the recorder's one start error | Example "microphone refused" in a real browser | Not observed; the mocked browser always grants; `audioRecorder.ts` rethrows every start error as one | Not observed. The mounted test proves the dialog's rule; real-browser capture stays the story's stated gap |

## Current decisions

- Wording, adjustable at the owner's review without a plan change: control
  "Speak the title" / "Stop"; statuses "Recording. Speak now.", "Turning your
  speech into text…", "No speech was turned into text."; messages "Could not
  use the microphone. Allow microphone access in your browser, then try
  again." and "Could not turn your speech into text."
- The words arrive once, after Stop: the title recorder converts only at
  Stop, and the response's segments are joined with the body join rule into
  one line.
- "Untouched default" means the dialog opened with "Untitled" and the author
  has not edited it; then the words replace it. Otherwise they join the end
  of the current title. The heard words are never changed by the product.
- Every Record creates a fresh recorder, so a failed attempt's audio is not
  sent again; there is no Retry.
- Submit is unavailable while listening or converting, and Enter in the
  title field does nothing then.
- Each slice updates docs/voice-input.md for the behavior it delivers.
- No layout or component library choice is fixed here; the control sits with
  the title field.

## Slices

### 1. The dictation join rule is one function used by the body
Type: Structure
Status: done
Proof: accepted — `joinDictatedSegments` in `frontend/src/models/audio/joinDictatedSegments.ts`; `appendDictatedText` delegates. Focused `tests/models/joinDictatedSegments.spec.ts` covers the documented examples; `NoteAudioTools.languageJoining` and `NoteAudioTools.preservation` green; `vue-tsc --noEmit` green.
```bash
env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/notes/NoteAudioTools.languageJoining tests/notes/NoteAudioTools.preservation tests/models/joinDictatedSegments.spec.ts
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
```

Change: the `join` closure in `appendDictatedText` becomes an exported
function, for example `joinDictatedSegments(base, segments)` in
`frontend/src/models/audio/` or beside `noteTextEditing.ts`, and
`appendDictatedText` calls it. External behavior is unchanged. Enables
slice 3, which joins segments into a title with the same rule.

### 2. A recorder can be created to convert only at Stop
Type: Structure
Status: done
Proof: accepted — `convertOnlyAtStop` on `createAudioRecorder` / `wireAudioProcessingScheduler` skips timer and silence wiring; mid-speech `processAndCallback` also returns early. `audioProcessingScheduler.convertOnlyAtStop.spec.ts`: 60 s tick and silence cause no callback; Flush mid-speech does not convert; `stop()` once with `isMidSpeech: false`. `tests/models/audio` green; `vue-tsc --noEmit` green.
```bash
env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/models/audio
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
```

Change: `createAudioRecorder(callback, options?)` passes an option through
`wireAudioProcessingScheduler` that leaves the timer unstarted and the
silence hook unwired, so Stop makes the only conversion. Audio tools passes
nothing and behaves as today. Enables slice 3.

### 3. New note listens for the title and shows the heard words
Type: Behavior
Status: done
Proof: accepted — `SpeakTitleControl` + `NoteNewForm` / `PathNameEditor.applyExternalValue`; shared `audioChunkToText`. Mounted `NoteNewForm.spokenTitle`: idle/listening names, statuses on `role="status"`, words then clear status. E2E `record_live_audio.feature` "Create a note by speaking the title". `NoteAudioTools.recording`/`status` green after mock/`audioChunkToText` extract. `vue-tsc` green. Docs: "Speaking a title in New note" in `docs/voice-input.md`.
```bash
env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/notes/NoteNewForm tests/notes/NoteNewButton tests/models/audio tests/notes/NoteAudioTools.recording tests/notes/NoteAudioTools.status
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/record_live_audio.feature
```

Behavior: New note open with the untouched "Untitled" → "Speak the title"
→ the control reads "Stop", the status says "Recording. Speak now." →
Stop → "Turning your speech into text…" → the response's segments, joined
by slice 1's rule, replace "Untitled" through the title editor's own
handling; the status clears; Submit creates the note once with it.

Interim: a title the author already typed is replaced too, Submit stays
offered while listening, the search for existing notes does not yet run for
the heard title, and closing the dialog mid-listening does not yet stop the
recorder; slice 4 changes all four. The documentation gains
"Speaking a title in New note".

### 4. A spoken title is an ordinary title, and Submit waits for it
Type: Behavior
Status: planned
Proof: mounted tests: "2026-10-06 " + segments → "2026-10-06 weekly review"; a typed title plus speech joins with one space; a segment containing `/` shows the fullwidth replacement and its warning; search results shown for the heard title; typing after speech then Submit sends the typed title; Submit disabled while listening and converting, enabled once the words are in; Enter in the title field while listening does not call the create request; unmount while listening calls `stopRecording`; frontend tests green.

Behavior: New note open with a title pattern or typed title → Speak the
title → Stop → the heard segments join the end of the current title by the
body join rule, through the title editor's own handling, and the search for
existing notes runs for the result; while listening or converting, Submit
is not offered and Enter does nothing; afterwards Submit is offered and the
author may type corrections before submitting. Closing the dialog while
listening stops the recorder.

### 5. Nothing heard and failures leave the title alone
Type: Behavior
Status: planned
Proof: mounted tests: Stop after a silent recording (no conversion) and Stop with a response of no segments both show "No speech was turned into text." with the title unchanged and Submit offered; the recorder refuses to start → the microphone message styled as a problem, "Speak the title" still offered; `audioToText` rejects at Stop → "Could not turn your speech into text.", title unchanged, Submit offered; a second Speak the title creates a second recorder and its words alone reach the title; frontend tests green.

Behavior: New note → Speak the title → Stop with nothing heard → "No speech
was turned into text.", title unchanged. Speak the title when the microphone
cannot be used → the microphone message as a problem, control still
offered. A failed conversion at Stop → its message, title unchanged, no
Retry; Speak the title again → only the new recording is converted and its
words go to the title as slices 3 and 4 define. The documentation records
the three outcomes.

## Learnings

None yet.
