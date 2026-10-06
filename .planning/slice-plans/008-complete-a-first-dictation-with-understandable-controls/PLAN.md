# Complete a first dictation with understandable controls

## Source

- Story: [Complete a first dictation with understandable controls](../../seeds/SEED-066-voice-input.md#understandable-first-dictation)
- Identity: SEED-066#understandable-first-dictation
- Owner decisions, 2026-10-06: the mid-speech control stays under a plain
  name; the failure message after Stop says the recording is kept until Audio
  tools is closed; Retry is the suggested next step beside that message with
  Record still available; one start-failure message is part of this story.

## Goal and scope

A note author who has never used Audio tools adds a spoken passage to an
existing note. At each moment the panel says in words whether it is ready,
recording, turning speech into text, or finished, and whether the text is in
the note.

- One main action with its name in words: Record, then Stop.
- A status in words that assistive technology announces when it changes.
- After Stop the status says the text was added only when a passage from this
  recording was written and its save succeeded; when nothing was written it
  says no speech was turned into text.
- Text written while still speaking does not change the status to finished.
- When recording cannot start, one message says the microphone could not be
  used and to allow access in the browser, shown as a problem.
- After a failed conversion at Stop, the message and Retry are the suggested
  next step; Record stays available.
- Save Audio Locally, full screen, the microphone chooser and the mid-speech
  control keep working, each with a name in words, and none takes the main
  action's place.
- Every preservation, hold-back, failure and Retry behavior in
  [docs/voice-input.md](../../../docs/voice-input.md) stays, as does the way
  Audio tools is opened from the note toolbar.

Excluded: a new look for the whole panel; moving voice input out of the note
toolbar; choosing a microphone before recording; telling a refused permission
from a missing microphone; keeping an unconverted recording after Audio tools
closes; a sign of progress for each mid-speech passage; feedback after the
panel closes while recording; the wording of a failed note save; observing
real hardware capture.

Considered and left out:

- A separate Structure slice that first gathers the panel's flags into one
  state. Slice 1 needs that one place to show any status, so it is built
  there and extended by later slices, not prepared ahead.
- A refused-permission journey in the browser tests. The mocked browser
  always grants the microphone; the edge is proved with a mounted test.
- Changing the mid-speech failure wording. Recording goes on and retries by
  itself, so "Your recording is kept." stays true there.
- A wording for a failed note save. The existing error display stays; the
  panel only refrains from claiming the text was added.

## Architecture

- Existing solutions reused (PFE):
  - The dictation path is unchanged: `NoteAudioTools.vue` → `audioRecorder`
    → `audioProcessingScheduler` → `useNoteAudioProcessing` →
    `noteStore.appendDictatedText`. The status is derived from events this
    path already produces: recording started, Stop pending, a response with
    segments, the append finishing, a conversion failure.
  - Saving through an open body editor already has a way to wait for a save
    and learn its result (`flushAndWait` in `useDebouncedTextAutosave`,
    registered through `noteContentMutationBarrier`). Slice 3 uses that path;
    it adds no second save path.
  - The failure message, Retry, and kept audio stay as delivered; slice 4
    changes only the wording after Stop and where Retry sits.
  - The mocked recording journey, its page object
    (`e2e_test/start/pageObjects/audioToolsPage.ts`), and the mounted test
    support (`frontend/tests/notes/noteAudioToolsTestSupport.ts`) are
    extended, not replaced.
- One place in the frontend owns the dictation's state for the panel; the
  separate `isRecording`, `isProcessing`, `errors` and `audioFile` reads in
  the template go through it as slices need them. No backend, API or route
  change. No Accepted ADR is affected: the start-failure and
  conversion-failure messages are catches with a business outcome and a
  clearer message ([ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md)).
- [NORTH-STAR "One real-service audio test"](../../NORTH-STAR.md#one-real-service-audio-test)
  governs proof: new behavior is proved with the mocked journey and mounted
  tests. The real-service feature shares the page object's start and stop
  steps, so it follows the renamed controls without a new scenario.

## Outside-in proof

| Promise | Slice | Proof |
| --- | --- | --- |
| One main action named in words: Record, then Stop (example 1) | 1 | Mocked journey starts and stops through buttons named "Record" and "Stop"; mounted test: only one of them is present at a time |
| Status says ready, recording, turning speech into text, and is announced (example 1) | 1 | Mocked journey observes "Recording. Speak now." before Stop; mounted tests observe each status in an element with the status role |
| Text written mid-speech leaves the status on recording (example 2) | 1 | Mounted test: a mid-speech conversion finishes while recording; status and main action unchanged |
| After Stop, "Added to your note." when a passage was written (example 1) | 2 | Mocked journey observes it after Stop, then the saved content, in the first scenario |
| After Stop with nothing written, "No speech was turned into text." (example 3) | 2 | Mounted test: Stop with no conversion, and Stop with a conversion that returns no segments |
| "Added" waits for the save of the body that holds the passage, and is not claimed when that save fails | 3 | Mounted tests with an open body editor: save pending → still turning; save done → added; save failed → no added claim. Saved-body path: failed save → no added claim |
| Typing after the passage joined does not withhold the result | 3 | Mounted test with the typing-while-pending support |
| Failure at Stop: body unchanged, message "…kept until you close Audio tools." and Retry as the next step, Record available; Retry success joins once and says added (example 5) | 4 | Retry scenario of the mocked journey with the new wording and the added status after Retry; `NoteAudioTools.retry.spec.ts` |
| Mid-speech failure keeps "Your recording is kept." | 4 | `NoteAudioTools.processing.spec.ts` existing case stays green |
| Recording cannot start: microphone message shown as a problem, Record still the main action (example 4) | 5 | Mounted test: the recorder refuses to start |
| Mid-speech control named in words, shown only while recording, still converts | 6 | Mounted tests in `NoteAudioTools.recording.spec.ts` |
| Save audio and full screen named in words and working; the microphone chooser has a readable name; every control can be told apart by readable words (example 6) | 7 | Mounted tests: download and full-screen cases stay green under the new names; a test lists every control in the ready, recording and failed-after-Stop states and finds readable text or a label for each |
| Preservation, hold-back, kept audio, Retry rules unchanged | every slice | `tests/notes/NoteAudioTools` and `tests/models/audio` stay green |
| The documentation describes the panel as delivered | each slice | Reading docs/voice-input.md after the slice |

Commands (from the story worktree root):

- Frontend audio tests:
  `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/notes/NoteAudioTools tests/models/audio tests/notes/NoteToolbar.panels tests/common/FullScreen`
- Mocked journey (unset the overrides listed in
  [docs/worktree-browser-tests.md](../../../docs/worktree-browser-tests.md) first):
  `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/record_live_audio.feature`
- The real-service feature runs in CI after a push; it is not a local gate.

## Decisive premises

| Premise | Consumed by | Observation | Result |
| --- | --- | --- | --- |
| The mounted audio and toolbar-panel tests pass before any change | Every slice's preserved-behavior proof | Ran the frontend command above without `tests/common/FullScreen`, 2026-10-06: 11 files, 91 tests passed | Holds |
| The mocked journey passes before any change | Slices 1, 2, 4 extend it | Ran the mocked-journey command, 2026-10-06: 2 scenarios passed | Holds |
| Both audio features start and stop through the same page object | Renaming the controls in slice 1 reaches the real-service feature | Read both feature files: each uses "I start recording audio for the note" and "I stop recording audio"; `e2e_test/step_definitions/audio.ts` maps them to `audioToolsPage().startRecording()` / `stopRecording()` | Holds |
| Every reader of the control names is known | Slices 1, 6, 7 rename them | `grep` for the names across `e2e_test`, `frontend/src`, `frontend/tests`, `docs`: `audioToolsPage.ts`, `noteMoreOptionsForm.ts` (`openAudioTools` waits for "Record Audio"), `NoteAudioTools.vue`, `noteAudioToolsTestSupport.ts`, `NoteAudioTools.recording/advancedOptions/retry.spec.ts`, `NoteToolbar.panels.spec.ts`, `FullScreen.vue`, `FullScreen.spec.ts`, `docs/voice-input.md` | Holds |
| `assumeAudioTools` checks that "Stop Recording" exists, and is used after Stop | Slice 1 shows Stop only while recording, so this check moves to something always present in the panel | Read `audioToolsPage.ts` and `audio.ts` (stop, Retry and failure steps call it) | Holds; slice 1 changes the check |
| The page object's `stopRecording` waits for "Save Audio Locally" to be enabled as its completion sign | Slice 2 replaces that wait with the result status; slice 7 renames the control | Read `audioToolsPage.ts` | Holds |
| Silent audio is not sent, so Stop after silence runs no conversion and shows no failure or Retry | "No speech was turned into text." in slice 2 | Read `rawSampleAudioBuffer.ts` (`getProcessableData` returns null when every chunk is silent; `processUnprocessedData` then returns without calling back) and the Retry `v-if` (needs `errors.conversion`) | Holds |
| A conversion with no segments writes nothing | Slice 2 counts text as written only when segments were returned and the append finished | Read `noteTextEditing.ts` (`appendDictatedText` returns at once for an empty list) | Holds |
| A failed conversion at Stop still lets Stop finish | The status can leave "turning speech into text" after a failure | Read `audioProcessingScheduler.ts` (`processAndCallback` catches; `stop()` then returns the file) | Holds |
| With a body editor open, the append returns before the save completes | Slice 3 exists | Read `noteContentMutationBarrier.ts` (`changeOpenNoteContentDraft` calls `changeDraft` and returns true) and `TextContentWrapper.vue` (`changeDraft` calls `flush()` without waiting) | Holds; without slice 3 "added" would be claimed early |
| `flushAndWait` reports false when the author has typed again, even if the passage's save succeeded | Slice 3 must not use it unchanged for the result | Read `useDebouncedTextAutosave.ts` (`return saved && !hasUnsavedChanges()`) | Holds |
| Without an open editor the append already waits for the save | Saved-body path in slice 3 needs only the failed-save case | Read `noteTextEditing.ts` (`await this.updateTextField`) and `useNoteAudioProcessing.ts` (a save error is caught and shown, no text counted) | Holds |
| Every start failure reaches the panel as one error | One message in slice 5 | Read `audioRecorder.ts` (all start errors rethrown as one) and `NoteAudioTools.vue` (`startRecording` catch) | Holds |
| The mounted tests can make the recorder refuse to start | Slice 5's proof | Read `noteAudioToolsMocks.ts` (`startRecording` is a `vi.fn` on the mocked recorder) | Holds |
| Advanced Options holds only full screen, and only Audio tools uses `FullScreen` | Slice 7 names full screen directly and deletes the gear layer | Read `NoteAudioTools.vue`; `grep FullScreen frontend/src`: only `NoteAudioTools.vue`; `NoteAudioTools.advancedOptions.spec.ts` asserts it is the only advanced option | Holds |
| A refused permission in a real browser reaches the same catch | Example 4 in a real browser | Not observed; the mocked browser always grants. Read only | Not observed. The mounted test proves the panel's rule; real-browser capture stays the story's stated gap |

## Current decisions

- Wording, adjustable at the owner's review without a plan change:
  "Ready to record", "Recording. Speak now.", "Turning your speech into
  text…", "Added to your note.", "No speech was turned into text.",
  "Could not use the microphone. Allow microphone access in your browser,
  then try again.", and after Stop "Could not turn your speech into text.
  Your recording is kept until you close Audio tools."
- Button names are the visible words: "Record", "Stop", "Retry", "Write text
  now", and plain names for saving the audio and full screen.
- The status after Stop follows one rule: turning speech into text while
  Stop or Retry is still running; then the failure message when a conversion
  failed, "added" when a passage of this recording was written and saved,
  nothing claimed when its save failed, otherwise "no speech".
- "Written in this recording" starts again with each Record. A Retry belongs
  to the recording it follows.
- Each slice updates docs/voice-input.md for the behavior it delivers.
- No layout or component library choice is fixed here.

## Slices

### 1. The panel names its main action and says ready, recording, and turning speech into text
Type: Behavior
Status: done
Accepted proof: frontend audio command, 13 files, 96 tests passed (status cases in `NoteAudioTools.status.spec.ts`, Record/Stop and chooser in `NoteAudioTools.recording.spec.ts`); mocked journey 2 passing, `audioToolsPage.startRecording` asserts "Recording. Speak now." in the "Audio tools" region.
Proof: mocked journey green with "Record" and "Stop" and the recording status observed before Stop; mounted tests for the three statuses, the single main action, and a mid-speech conversion leaving the status on recording; frontend audio tests green.

Behavior: Audio tools open on a note, nothing recording → the status says
"Ready to record" and the one main action is "Record" → Record → the status
says "Recording. Speak now." and the main action is "Stop"; a conversion that
finishes mid-speech changes neither → Stop → the status says "Turning your
speech into text…" until Stop has finished. The status is in an element that
assistive technology announces. The microphone chooser stays available while
recording, beside the main action.

Interim: when Stop has finished the status returns to "Ready to record";
slice 2 replaces that with the result. The page object keeps waiting for
"Save Audio Locally" until slice 2. `assumeAudioTools` and `openAudioTools`
check something present in every state.

### 2. After Stop the panel says whether text was added
Type: Behavior
Status: done
Accepted proof: frontend audio command, 13 files, 99 tests passed (`NoteAudioTools.status.spec.ts` "after Stop" cases for added, no segments with no save, and a new Record counting nothing); mocked journey 2 passing, scenario 1 observes "Added to your note." after Stop, then the saved content.
Proof: first scenario of the mocked journey observes "Added to your note." after Stop and then the saved content; mounted tests for Stop with no conversion and for a conversion with no segments; frontend audio tests green.

Behavior: a recording in which at least one passage was written → Stop
finishes → "Added to your note." A recording in which nothing was written →
Stop finishes → "No speech was turned into text." and the body is unchanged.
A new Record starts from "Recording. Speak now." with nothing counted.

The page object's `stopRecording` waits for the status to leave "Turning your
speech into text…" in place of the "Save Audio Locally" wait.

Interim: with a body editor open, "added" can show before the save
completes; slice 3 removes that.

### 3. "Added to your note" waits for the save and is not claimed when it fails
Type: Behavior
Status: done
Accepted proof: `NoteAudioTools.savedStatus.spec.ts` (6 tests: pending, done, failed with and without an open editor, typing after the join, typing that replaces the passage's draft before it is sent); frontend audio and shared autosave consumers 44 files, 271 tests, plus image upload, removal and notebook readme 4 files, 24 tests; vue-tsc clean; mocked journey 2 passing.
Proof: mounted tests with an open body editor (save pending, done, failed; typing after the join) and without one (failed save); typing-while-pending and preservation tests green; mocked journey green.

Behavior: a body editor is open and the passage has joined its draft → while
that save is pending the status stays "Turning your speech into text…" → the
save succeeds → "Added to your note."; the save fails → no "added" and no
"no speech" claim, and the existing save error shows. Typing that the author
does after the passage joined does not withhold "added" once the body holding
the passage is saved. Without an open editor, a failed save likewise makes no
claim.

The dictated text still becomes visible in the body as soon as it joins; only
the status waits.

### 4. A failed conversion at Stop offers Retry as the next step
Type: Behavior
Status: done
Accepted proof: `NoteAudioTools.retry.spec.ts` (5 cases) and the mid-speech failure case in `NoteAudioTools.processing.spec.ts` in the frontend audio command, 14 files, 107 tests; vue-tsc clean; mocked journey 2 passing, the Retry scenario observes the new wording with Retry and Record, then "Added to your note." after Retry.
Proof: Retry scenario of the mocked journey with the new wording, then "Added to your note." after Retry; `NoteAudioTools.retry.spec.ts` and the mid-speech failure case in `NoteAudioTools.processing.spec.ts` green.

Behavior: the transcription fails at Stop → the body is unchanged; where the
result would be, the panel shows "Could not turn your speech into text. Your
recording is kept until you close Audio tools." with "Retry" beside it, and
"Record" is still available → Retry → "Turning your speech into text…" → on
success the passage is joined once and the status says "Added to your note.";
on failure the message and Retry stay. A failure while still recording keeps
"Your recording is kept." and offers no Retry.

### 5. A microphone that cannot be used is explained
Type: Behavior
Status: done
Accepted proof: `NoteAudioTools.status.spec.ts` "explains a microphone that cannot be used until a later Record starts" in the frontend audio command, 14 files, 108 tests; vue-tsc clean; mocked journey 2 passing after the recorder's start catch was deleted.
Proof: mounted test in which the recorder refuses to start; frontend audio tests green.

Behavior: Audio tools open → Record → recording cannot start → nothing is
recorded; the panel shows "Could not use the microphone. Allow microphone
access in your browser, then try again." styled as a problem; the main action
is still "Record"; a later successful Record clears the message.

"Failed to start recording" goes from the product.

### 6. The mid-speech control says what it does and appears only while recording
Type: Behavior
Status: done
Accepted proof: `NoteAudioTools.recording.spec.ts` (absent before Record, present while recording, "converts what has been said so far with Write text now", "disables Write text now during a conversion") in the frontend audio command, 14 files, 108 tests; vue-tsc clean. No browser test uses the control.
Proof: `NoteAudioTools.recording.spec.ts` cases for the control under its new name: absent when not recording, present while recording, unavailable during a conversion, and still triggering a conversion.

Behavior: not recording → no mid-speech control is shown. Recording → a
control named "Write text now" is shown beside, not in place of, Stop;
choosing it converts what has been said so far exactly as Flush does today.

The name "Flush Audio" and the tick picture go from the panel and from the
first paragraph of docs/voice-input.md; the documented passages keep
describing the same action under its new name.

### 7. Saving the audio, full screen, and the microphone chooser are named in words
Type: Behavior
Status: done
Accepted proof: frontend audio command, 14 files, 107 tests: "Save audio" download and enable cases in `NoteAudioTools.recording.spec.ts`, `NoteAudioTools.fullScreen.spec.ts`, `FullScreen.spec.ts`, and "names every control in words when ready, recording, and failed after Stop" in `NoteAudioTools.retry.spec.ts`; vue-tsc clean. No browser test reads these controls.
Proof: the download and full-screen mounted cases green under the new names; a mounted test that, in the ready, recording, and failed-after-Stop states, every control in the panel has readable text or a label; `tests/common/FullScreen` green.

Behavior: Audio tools open → saving the recorded audio and full screen are
each offered under a name shown in words, set apart from the main action;
full screen is reached directly, without an Advanced Options step; while
recording, the microphone chooser has a readable name. Each behaves as today:
saving is available once a recording has produced a file, and full screen
shows the same overlay with the current error.

Advanced Options, its gear, and its toggle state are deleted with their test
cases; `NoteAudioTools.advancedOptions.spec.ts` keeps the full-screen cases
under a name that matches.

## Learnings

- The panel state lives in `phase` with its wording in `statusByPhase`
  (`NoteAudioTools.vue`); later slices add a phase there. Status tests live in
  `NoteAudioTools.status.spec.ts`.
- The panel is a region named by `noteMoreOptionsTitles.audio`;
  `assumeAudioTools` checks that region, and `openAudioTools` calls it.
- The mocked journey's dev server stops on a Biome format error, so edits must
  be Biome-clean before that journey runs.
- While Stop is finishing, the main action already shows Record again.
- "Written" is the passage saves kept by `useNoteAudioProcessing`
  (`startNewRecording`, `writtenResult`); Stop waits for them. With an open
  editor each save is confirmed by `flushAndConfirmDraftSaved` in
  `useDebouncedTextAutosave`, which follows a newer draft that replaced the
  passage's draft. A failed save leaves the status at "Ready to record".
- A conversion failure at Stop is the `notConverted` phase: its message is
  the status, with Retry beside it; the error alert is hidden then.
- A start failure is the `micUnavailable` phase; `isProblem` styles it and
  `notConverted` as a problem. `audioRecorder.startRecording` no longer
  renames its errors.
- "Write text now" calls `audioRecorder.tryFlush()` directly and shares the
  `.labeled-action` style with Record and Stop.
- "Save audio" and "Full screen" sit in a `.secondary-actions` row styled by
  `NoteAudioTools.vue`; `FullScreen.vue` keeps only the overlay's styles.
- A mounted test that sets the note realm calls `refreshNoteRealm` after
  mounting, because mounting reloads the note.

## Execution complete

Product advice: Queue the correction
[SEED-066#no-record-while-stopping](../../seeds/SEED-066-voice-input.md#no-record-while-stopping)
([plan 009](../009-keep-the-dictation-result-true-while-stop-finishes/PLAN.md))
ahead of the spoken-title stories. Record can still be pressed while Stop is
turning speech into text, which can report a false result or hide Stop while
recording; it is a one-slice fix. The panel's state now lives in one place
(`phase` / `statusByPhase` in `NoteAudioTools.vue`), so later voice stories
should extend it rather than add flags. SEED-066#prompt-dictation-results is
also Taken and touches the same panel; expect to reconcile with it on
integration.
