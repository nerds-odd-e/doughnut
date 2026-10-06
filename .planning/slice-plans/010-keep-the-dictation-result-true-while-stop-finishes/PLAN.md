# Keep the dictation result true while Stop finishes

## Source

- Story: [Keep the dictation result true while Stop finishes](../../seeds/SEED-066-voice-input.md#no-record-while-stopping)
- Identity: SEED-066#no-record-while-stopping
- Kind: retrospective correction of SEED-066#understandable-first-dictation
  (plan `0662bac730:.planning/slice-plans/008-complete-a-first-dictation-with-understandable-controls/PLAN.md`).
- Provenance: the execution of 008 produced commits 31b9b7b7, ef95c980, 708f43f4,
  18e10d46, ea48c9ef, 07c4e146 and 9f8a52c7, on top of 9525968b. Slice 1 kept Record
  visible while Stop finished, and its plan Learnings note that; slices 2–3
  later made Stop write the result.

## Findings

Read in `frontend/src/components/notes/widgets/NoteAudioTools.vue` at
9f8a52c7:

- Record is shown whenever `phase !== "recording"`, which includes
  `stopping` ("Turning your speech into text…").
- `startRecording` calls `startNewRecording()`, which clears the recording's
  passage saves, and then sets `phase = "recording"`.
- The `finally` of the pending `stopRecording` (or `retry`) then sets the
  phase from `writtenResult()` and the conversion error.

Consequences when Record is pressed while Stop or Retry is still running:

- Text this recording added can be reported as "No speech was turned into
  text."
- If the new recording starts, the earlier Stop overwrites "Recording. Speak
  now." with a result. Stop then disappears while the microphone is still
  recording.

Not observed in a browser; the trace is read from the code. No mounted test
presses Record while Stop is pending. Re-read on 2026-10-06 at e5d09b3b: the
component is unchanged since 9f8a52c7, and `NoteAudioTools.status.spec.ts`
already holds the recorder's `stopRecording` pending with a resolver, which
the proof below reuses.

## Preserved promises and constraints

- Record, then Stop, as the one main action; the status wording and its
  announcement.
- "Added to your note." only after the save of the body that holds a passage of
  this recording succeeds. "Written" restarts with each Record, and a Retry
  belongs to the recording it follows.
- After a failed conversion at Stop, Retry sits beside the message and Record
  stays available.
- Every preservation, hold-back and kept-audio rule in docs/voice-input.md.

## Outside-in proof

| Promise | Slice | Proof |
| --- | --- | --- |
| Record cannot be started while turning speech into text, after Stop or Retry; available again at the result | 1 | Mounted test in `NoteAudioTools.status.spec.ts`: the recorder's `stopRecording` mock held pending → the Record button is disabled, and the recorder's `startRecording` is not called when it is clicked → release → result shown and Record enabled. Same for Retry in `NoteAudioTools.retry.spec.ts`, holding the second `stopRecording` pending. |
| Existing behavior unchanged | 1 | Frontend audio command green; mocked journey green |

Commands (from the worktree root):

- `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/notes/NoteAudioTools tests/models/audio tests/notes/NoteToolbar.panels tests/common/FullScreen`
- Mocked journey (unset the overrides in
  [docs/worktree-browser-tests.md](../../../docs/worktree-browser-tests.md)):
  `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/record_live_audio.feature`

## Current decisions

- The smallest change: Record is disabled while `phase === "stopping"`.
  Hiding it would leave the panel with no main action shown.
- Update docs/voice-input.md where it describes Record during the status:
  the first paragraph names Record as the main action when ready and says
  "Record stays available" beside Retry; add that Record is unavailable while
  the status says "Turning your speech into text…".

## Decisive premises

Observed on 2026-10-06 at e5d09b3b, before readiness was recorded.

| Premise | Consumed by | Observation | Result |
| --- | --- | --- | --- |
| Record is clickable while `phase === "stopping"` | Slice 1's change | Read `NoteAudioTools.vue`: Record renders under `v-if="!isRecording"`, and `isRecording` is `phase === "recording"` | True |
| Pressing Record then discards this recording's result | Slice 1's goal | Read `startRecording` → `startNewRecording()` in `composables/useNoteAudioProcessing.ts`: `passageSaves = []`, so the pending Stop's `writtenResult()` returns `nothingAdded` | True |
| The mounted tests can hold Stop pending | Proof | `NoteAudioTools.status.spec.ts` already replaces `stopRecording` with a promise resolved by `finishStop`; `NoteAudioTools.retry.spec.ts` replaces it per test and clicks Retry | True |
| A disabled Record is not triggered by the test helper | Proof | `startRecording(wrapper)` triggers `click` on the "Record" button; vue-test-utils does not dispatch on a disabled element, and `.daisy-btn:disabled` styling exists | True |
| The mocked journey presses Record only when ready | Existing behavior unchanged | `audioToolsPage.ts` clicks Record and waits for "Recording. Speak now."; `stopRecording()` waits until the status is neither recording nor turning | True |
| The named frontend command runs green now | Proof baseline | Ran `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/notes/NoteAudioTools tests/models/audio tests/notes/NoteToolbar.panels tests/common/FullScreen` | 14 files, 107 tests passed, 6.4 s |

## Slices

### 1. Record waits until Stop has given its result
Type: Behavior
Status: planned
Proof: the mounted tests above, frontend audio command and mocked journey green.

Behavior: Record → Stop → while "Turning your speech into text…" shows,
Record cannot be started → the result shows ("Added to your note.", "No speech
was turned into text.", or the failure with Retry) → Record is available.
The same applies while Retry runs.

## Learnings

None yet.
