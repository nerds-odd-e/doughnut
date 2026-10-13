# Cancel content voice startup when the author leaves

**Identity:** SEED-066#recording-started-after-leaving
**Source:** [Stop a content recording that starts after the author has left the note](../../seeds/SEED-066-voice-input.md#recording-started-after-leaving)

## Goal and scope

A note author who leaves while content Voice input is starting ends that
attempt. When delayed startup finishes, its microphone and wake lock are
released and any late audio is discarded without transcription or a content
change. The destination remains available for its own voice session. Content
recording already underway still stops and converts into the original note.

The owner accepted discard on 2026-10-13. No new control or navigation prompt
is needed: the existing starting spinner belongs to the note being left, and
the destination offers its own Voice input. Preserve current title behavior,
normal Stop, retry, and failure feedback. Title lifecycle repairs, background
dictation, hardware compatibility investigation, and a broader audio redesign
are deferred, not runtime prohibitions.

## Existing owners and observed premises

Preparation checkout:
`/Users/terryyin/git/doughnut/.worktrees/stop-a-content-recording-that-starts-after-the-a`,
base `0e1736fdbf89509c852cb5b0323ac5cf7f8fc314`, observed 2026-10-13.
Execution uses the workspace established by its authorized start; recheck
changed code before reusing these observations.

- PFE search across frontend, backend, CLI, MCP, and E2E found browser capture
  owned by `frontend/src/models/audio/audioRecorder.ts`; its two production
  consumers are content and title voice composables. `useNoteVoiceInput.ts`
  already owns content phase, departure, wake lock, mutation-barrier entry,
  and current-page activity. Change that existing owner for cancellation;
  reuse recorder cleanup. No new cross-product abstraction is justified.
- `audioRecorder.stopRecording` disconnects the graph, stops tracks and stops
  the scheduler, whose Stop flushes audio through its callback. The existing
  title departure path discards title results, but has different conversion
  semantics and supplies no reusable pending-start cleanup guarantee.
- `useNoteAudioProcessing` must still convert speech from an established
  session after departure. Checking only `authorHasLeft` in all processing
  would destroy that behavior. Cancellation applies to the attempt that was
  still starting when the author left.
- Baseline reproduction and its limits are retained in the source story.
  A second disposable mounted `NoteToolbar` probe also held wake-lock request,
  and supplied 1,600 non-silent samples through the worklet callback during
  graph connection. Real recorder, receiver, buffer and scheduler ran with
  external browser audio/wake-lock stand-ins and mocked backend HTTP.
  `./scripts/run.sh pnpm frontend:test tests/notes/VoiceStartupObservation.spec.ts tests/notes/NoteToolbar.voiceInputLeavingNote.spec.ts`
  on unchanged product code: all three startup cases failed cleanup;
  the three existing departure tests passed.
- One scratch candidate kept cancellation with the content session, checked
  departure after asynchronous acquisition, reused Stop for a completed late
  startup, and completed cancelled audio callbacks without conversion.
  `./scripts/run.sh pnpm frontend:test tests/notes` passed 71 files / 387
  tests in 21.70 seconds, including the three startup probes, established
  recording departure, title use, retry, and editor consumers.
- Discrimination: with that candidate's discard condition disabled and its
  established-recording departure Stop disabled, rerunning the two-file command
  above failed both audio-discard cases (transcription and content-save calls)
  and two existing departure cases (Stop/save and failure feedback); two tests
  passed. These observations identify meaningful proof rather than a mock
  supplying cancellation. All scratch product and test edits were reverted
  before this plan was written. The candidate was not delivered or typechecked;
  its run establishes bounded feasibility, not accepted implementation proof.
- Hardware indicators, actual permission UI, real speech and backend services
  were not exercised. This plan proves the application lifecycle at browser
  resource/API boundaries; it promises no hardware-specific timing.

## Architecture and constraints

Keep one session rule: departure while starting cancels that attempt;
departure while recording finishes its speech. A cancelled attempt must not
enter the open-note mutation barrier after departure or overwrite the current
page's activity. Acquisition completions and cleanup belong to their original
session, including when the author has begun another session in the destination.
Do not introduce a generic cancellation framework or change the recorder's
shared contract solely to implement this rule.

[ADR 0006 — Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md)
governs deliberate cleanup and failure handling; add no blanket exception
suppression. [ADR 0007 — Environments and isolation](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
keeps observations in disposable tests. No Accepted ADR conflict was found.
[North Star: One real-service audio test](../../NORTH-STAR.md#one-real-service-audio-test)
supports mounted frontend proof with mocked services. No new architectural
direction or real-service E2E scenario is needed.

## Outside-in proof

All promises belong to slice 1. Cancellation is an active lifecycle rule,
not feature removal; no-conversion assertions prove the owner's discard
decision. No removal sweep is needed.

| Promise | Observation at the mounted boundary |
| --- | --- |
| Late microphone and audio initialization end with released resources | Click content Voice input on a mounted toolbar, hold `getUserMedia` or worklet initialization, change to another note, settle startup; observe track Stop, disconnected graph, released old wake lock, idle destination control |
| Late audio is discarded | Supply non-silent samples during late connection; settling cleanup makes no transcription or content-save request |
| Departure during wake-lock acquisition cancels startup | Hold wake-lock request, leave, resolve it; the returned lock is released and the destination stays usable |
| A newer destination session owns its activity | Start a destination recording before resolving the departed attempt; old cleanup releases only its resources while the destination retains its active control and can Stop normally |
| Established content recording still saves on leaving | Retain `NoteToolbar.voiceInputLeavingNote.spec.ts`: final text is sent to the origin note; conversion-failure feedback remains covered |
| Existing normal recording, title use, retry and editing remain coherent | Run the notes suite, which covers these consumers, plus both required frontend typechecks |

## Ordered slices

### 1. Leaving cancels a pending content recording
Type: Behavior
Status: planned
Proof: the mapped mounted scenarios above; `./scripts/run.sh pnpm frontend:test tests/notes`,
`./scripts/run.sh pnpm -C frontend exec vue-tsc --noEmit`, and
`./scripts/run.sh pnpm -C frontend exec tsc --noEmit` on the final implementation.

Behavior: content Voice input is starting → the author leaves → that attempt
discards late audio and releases acquired microphone/wake-lock resources when
startup settles, without disturbing the destination's session. A recording
already underway retains normal Stop-and-save on departure.

Extend the existing content lifecycle and mounted tests together. Keep the
real recorder/scheduler in cleanup and discard proof; mock external audio and
wake-lock boundaries only. The browser constructor stand-ins used by existing
helpers are arrow functions; the real-recorder probe needed constructible
functions. Use suitable focused stand-ins without broadly rewriting test
support. Keep lifecycle choices local and reuse existing cleanup. If a shared
contract must change, inspect all title and test-double consumers again and
adjust their proof before delivery.

Sizing: approximately 8 minutes for implementation, focused proof and cleanup,
scrutinized against the 5-minute target. This is one cancellation outcome with
one proof loop; splitting the acquisition boundaries would duplicate the same
rule. The measured notes suite is short. At the 10-minute hard boundary stop
and refine unless the only remaining time is a stated required-check wait.
Stop-safe: the complete cancellation rule and preserved recording behavior
ship together, with no interim product state.

## Current decisions and delivery

- Execute only under a later explicit execution instruction. This preparation
  neither takes the story nor delivers the scratch candidate.
- Follow AGENTS.md execution wrap-up: Jidoka, fresh
  `dough-post-change-refactor` agent, API generation only if signatures change,
  coordinator `./scripts/run.sh pnpm format:changed` once, update plan evidence,
  agent commit with the check-only hook, publish under `dough-execute-plan`.
- Required local checks come from `frontend` and `frontend-testing`; the notes
  suite covers the affected consumers at the measured cost. No backend, paid
  service, manual-testing or whole-product E2E run is required by this change.
- No slice-plan refinement pass is needed: one bounded behavior slice owns
  all proof, and the observed candidate requires no shared-contract change.
  No remaining slice-specific concern was identified in this review.

## Learnings

The defect is reproduced at more than microphone permission: asynchronous
audio initialization and wake-lock acquisition share the same departure rule.
The discard decision can stay in the existing content-session owner while
retaining recorder cleanup and established-recording conversion.
