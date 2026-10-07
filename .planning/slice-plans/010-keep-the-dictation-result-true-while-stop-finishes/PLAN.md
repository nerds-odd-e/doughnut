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

Reproduced on 2026-10-06 at 1e6f720e (component unchanged since 9f8a52c7)
with a temporary mounted test, removed afterwards, built from the existing
fixtures of `NoteAudioTools.status.spec.ts` and `NoteAudioTools.retry.spec.ts`:

- **Stop:** Record → one mid-speech passage saved → Stop held pending
  ("Turning your speech into text…", Record shown and enabled) → Record
  clicked → "Recording. Speak now.", the recorder's `startRecording` called a
  second time → Stop finishes → "No speech was turned into text." although a
  passage was saved; Stop is gone and Record shows while the second recording
  runs.
- **Retry:** failed Stop → Retry held pending (same status, Record enabled) →
  Record clicked → "Recording. Speak now.", recorder started a second time →
  Retry finishes → "Added to your note."; Stop is gone while the second
  recording runs.

Not observed in a browser. No committed test presses Record while Stop or
Retry is pending (searched `frontend/tests` and `e2e_test` for Record
presses: every one follows a shown result or the ready state).

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
| Stop tells the true result of its recording even when Record is pressed while it finishes | 1 | Mounted test in `NoteAudioTools.status.spec.ts`, the reproduction above: one mid-speech passage saved → the recorder's `stopRecording` held pending → Record is disabled; clicking it leaves "Turning your speech into text…" and the recorder's `startRecording` called once → release → "Added to your note." and Record enabled. Red today: it says "No speech was turned into text.". |
| Record cannot be started while Retry runs; available again at the result | 1 | Mounted test in `NoteAudioTools.retry.spec.ts`: failed Stop → Retry with the second `stopRecording` held pending → Record is disabled; clicking it leaves the status and the recorder's `startRecording` called once → release → "Added to your note." and Record enabled. Red today: the status becomes "Recording. Speak now.". |
| Existing behavior unchanged | 1 | Frontend audio command green; mocked journey green |

Commands (from the worktree root):

- `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/notes/NoteAudioTools tests/models/audio tests/notes/NoteToolbar.panels tests/common/FullScreen`
- Mocked journey (unset the overrides in
  [docs/worktree-browser-tests.md](../../../docs/worktree-browser-tests.md)):
  `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/record_live_audio.feature`

## Current decisions

- One rule: a recording is in progress from Record until Stop, or a Retry
  that follows it, has shown its result, and Record starts only when none is
  in progress. Today Record is offered whenever the microphone is not
  capturing, which leaves the `stopping` phase out.
- The smallest change: Record is disabled while `phase === "stopping"`.
  Hiding it would leave the panel with no main action shown. Expected
  production change: one `:disabled` binding on Record in
  `NoteAudioTools.vue`; no new state, phase, or branch in the script.
- Proof stays in mounted tests and the mocked journey, following
  [One real-service audio test](../../NORTH-STAR.md#one-real-service-audio-test).
- Update docs/voice-input.md where it describes Record during the status:
  the first paragraph names Record as the main action when ready and says
  "Record stays available" beside Retry; add that Record is unavailable while
  the status says "Turning your speech into text…".

## Decisive premises

Observed on 2026-10-06 at 1e6f720e (no product file differs from e5d09b3b),
before readiness was recorded.

| Premise | Consumed by | Observation | Result |
| --- | --- | --- | --- |
| Record is clickable while `phase === "stopping"`, after Stop and after Retry | Slice 1's change | Temporary mounted test: with Stop, then Retry, held pending, the Record button is rendered without `disabled`, and clicking it calls the recorder's `startRecording` a second time | True |
| Pressing Record then makes Stop tell a wrong result | Slice 1's goal | Same test: one passage saved mid-speech, Record clicked while Stop is pending, Stop released → "No speech was turned into text.", Stop hidden, Record shown. Cause read in `startRecording` → `startNewRecording()` (`passageSaves = []`) and the `finally` of `stopRecording` | True |
| The mounted tests can hold Stop and Retry pending | Proof | Same test held `stopRecording` pending with a resolver after a mid-speech `processAudio`, and held the second `stopRecording` pending after clicking Retry, using only `noteAudioToolsTestSupport.ts` helpers | True |
| Clicking a disabled Record through the test helper starts nothing | Proof | Same run: `trigger("click")` on a mounted `<button disabled>` called its click handler 0 times; `.daisy-btn:disabled` styling exists in the component | True |
| The mocked journey presses Record only when ready or at a shown result | Existing behavior unchanged | Read `e2e_test/start/pageObjects/audioToolsPage.ts`: `startRecording()` clicks Record then waits for "Recording. Speak now."; `stopRecording()` waits until the status is neither recording nor turning; after a failed Stop it expects Record visible, which a `notConverted` phase keeps | True |
| The mocked journey is green now | Proof baseline | `gh run list --branch main`: "donut CI" succeeded on 2f42f414, the last commit that changed `frontend`, `e2e_test` or `backend`, and on 56718609 after it. Not run locally | True |
| The named frontend command runs green now | Proof baseline | Ran `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/notes/NoteAudioTools tests/models/audio tests/notes/NoteToolbar.panels tests/common/FullScreen` | 14 files, 107 tests passed, 5.4 s |

## Slices

### 1. Record waits until Stop has given its result
Type: Behavior
Status: done
Proof: the two mounted tests above, red before the change and green after;
frontend audio command and mocked journey green.

Behavior: Record → Stop → while "Turning your speech into text…" shows,
Record cannot be started → the result of this recording shows ("Added to your
note.", "No speech was turned into text.", "Ready to record" after failed
saves, or the failure with Retry) → Record is available. The same applies
while Retry runs. docs/voice-input.md says so.

Accepted proof (2026-10-07): the two new mounted tests, "tells the result of
its recording when Record is pressed while Stop finishes"
(`NoteAudioTools.status.spec.ts`) and "does not let Record start while Retry
finishes" (`NoteAudioTools.retry.spec.ts`), failed before the change on the
Record-disabled assertion and pass after it; the frontend audio command passed
(14 files, 109 tests) and the mocked journey passed (2 scenarios).

## Learnings

- The mocked journey's Vite dev server refuses to start while a changed file is
  not Biome-formatted; format changed files before running it.
