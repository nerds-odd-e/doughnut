# Discover voice-input problems through manual testing

**Identity:** SEED-066#discover-voice-input-problems
**Source:** [Refined discovery story](../../seeds/SEED-066-voice-input.md#discover-voice-input-problems).

## Goal and scope

Give the product owner observed voice-input problems, reproduction evidence,
timings, unresolved expectations, and coverage gaps before decomposing the
improvement epic. Explore existing web note authoring, prioritizing latency,
content preservation, title behavior, and understandable controls.

The owner accepts local Development or the deployed app and a 60-minute total
budget. Start at `http://127.0.0.1:5175/`, with the documented `manual` / `password`
sign-in and disposable notes. Keep checkout-bound work and records in the
established preparation workspace. Reuse the primary checkout's running app
without changing its source or restarting its services. The deployed app is an
alternative only after resolving its exact URL and account; observing both
environments is not required.

Discovery only: no product repairs, permanent test-tool changes, model selection,
numeric latency acceptance target, redesign, or epic decomposition. The later
automatic-title policy and exact acceptable recent-sentence revision boundary
remain undecided. Explicit title dictation and note creation are exploration
questions; absent capabilities are improvements, not failed existing promises.

## Observation and proof

Use the [manual-testing workflow](../../../.agents/skills/dough-manual-testing/SKILL.md).
The externally visible outcome of each slice is evidence useful to the owner,
not a change to product behavior. Keep findings in the epic's
[manual-discovery section](../../seeds/SEED-066-voice-input.md#manual-discovery-findings).
Link a separate evidence report only if needed. Do not represent unperformed
journeys as covered or owner reports as newly confirmed.

For a discrepancy, record the expectation and its source, observed result,
reproduction steps, browser/environment and application revision when known,
input audio/transcript and duration, service mode, and supporting screenshots or
before/after text. Separate observations from suspected causes. Record ordinary
observations only where needed for timing, coverage, or a subsequent comparison.

Time real elapsed recording and service responses. Capture start, first visible
text, subsequent updates, flush/stop, and final visible note content, distinguishing
note persistence from a button becoming enabled. Record final title updates
separately when they outlast body updates. If result settlement cannot be observed
within the slice, record a lower bound and the outstanding request; do not call it
complete. Network timing may help separate stages but does not establish a cause.

Real speech or paced prerecorded audio through browser media capture is preferred.
A temporary UI audio harness may be used if cheaper and credible. Label synthetic
capture, permission mocks, time manipulation, and service mocks precisely.
Simulated service responses cannot prove real transcription quality or latency.
Permission/device behavior requires a genuine browser/media path. Remove only
owned temporary harness files and release owned microphone streams.

### Existing solutions and decisive premises

PFE decision: reuse the existing Audio tools journey and available audio fixtures;
add no permanent recorder or testing abstraction. Inspection found:

| Premise and consuming operation | Observation and result | Remaining probe |
| --- | --- | --- |
| Local endpoint can be reached for slice 1 | `curl --max-time 3 -s -o /dev/null -w 'Development HTTP status: %{http_code}\n' http://127.0.0.1:5175/` returned HTTP 200; a fresh `curl --max-time 3 -s http://127.0.0.1:5175/ \| rg '<title>\|/@vite/client'` returned the Donut page title and Vite client | Slice 1 verifies the session, current runtime revision, and note access; the homepage alone proves none of these |
| Existing UI has a recording journey | Read `frontend/src/components/notes/widgets/NoteAudioTools.vue`, `frontend/src/composables/useNoteAudioProcessing.ts`, and `frontend/src/store/noteStore.ts`: Record Audio → recorder → audio-to-text → completeContent → persisted content; title suggestions run separately | Slice 1 observes this full journey, including real-service access and a resulting note update |
| Existing controlled audio can support a temporary observation route | Read `e2e_test/step_definitions/audio.ts`, `e2e_test/start/mock_services/browserService.ts`, and `audioToolsPage.ts`; `lecture.wav` and `harvard.wav` exist. The helper mocks media/worklet objects and delivers all decoded samples at once | Slice 1 establishes a usable route; this helper alone cannot prove hardware capture, paced streaming, or real end-to-end latency |
| Existing automated evidence has limited scope | Read `record_live_audio.feature`, `record_live_audio_with_real_open_ai_service.feature`, `NoteToolbar.panels.spec.ts`, and `NoteAudioTools.processing.spec.ts`: mocked-service continuous append, one real-service fixture journey, toolbar toggling, and SDK-mocked title/content behavior | No tests were run for this plan. These are reuse candidates, not current coverage; the feature's 20-second assertion is not an owner-approved latency target |
| Sign-in and note access in the running app | 2026-10-03 against `http://127.0.0.1:5175/`: `curl -u manual:password /api/healthcheck` returned `OK. Active Profile: dev. Commit: a02dbb2697…`; with that session `/api/notebooks` returned 200 and `/api/user/current-user-info` returned user `manual` | Settled. The running backend's commit is behind checkout `HEAD` only by planning records |
| Real transcription and retouch services | Same session, `POST /api/audio/audio-to-text` with `e2e_test/fixtures/harvard.wav` (18.4 s) and non-empty `previousNoteContentToAppendTo`: HTTP 200 in 4.7 s with real Whisper SRT and retouched content appended to the previous text. The backend process has `OPENAI_API_TOKEN` set | Settled for the service path. Endpoint timing excludes browser capture, chunking, and applying results to the note |
| Empty previous content | The same request with empty `previousNoteContentToAppendTo` returned HTTP 500: OpenAI 400, `One of "input" or "previous_response_id" or 'prompt' or 'conversation' must be provided`. Transcription succeeded first; `OtherAiServices.getTextFromAudio` adds a user message only for non-empty previous content | API-level observation, not yet a UI finding. Slice 1 dictates into an empty body as well as a non-empty one and records what the author sees |
| Browser route for the UI | One local Chrome (macOS) is connected to Claude in Chrome. Agents cannot speak into a hardware microphone | Slice 1 feeds a fixture through a temporary page-level `getUserMedia` replacement, labelled as controlled audio; hardware capture and permission prompts remain gaps unless the owner speaks |
| Persistent Development is reused, not started | The established preparation is the default checkout on `main` at `ae23d73c9e`, which hosts the running Development stack | Reuse the live app; do not restart it. If it becomes unsuitable, stop the affected path |

Login and credentialed OpenAI calls were observed live on 2026-10-03. Slice 1 owns
the remaining premise: a usable controlled-audio route through the browser UI. A failed premise stops dependent
real-service observations and requires revising the route or reporting the gap.
Independent UI observations may continue within the budget, labelled accurately.

This plan adds no product structure or architectural choice. The current
[North Star](../../NORTH-STAR.md) concerns notebook/attachment architecture and
does not prescribe changes for this discovery. No ADR exception is required.

## Ordered slices

Each slice is Behavior, with one observation outcome, target ~5 minutes including
evidence and local cleanup. Ten targets total 50 minutes; reserve 10 minutes for
setup and confirming the most consequential surprise. Targets are hypotheses,
not repeated mandatory runs. Start with breadth, then spend reserve selectively.
The first probe may take up to 10 minutes for browser/audio setup or an external
service wait; charge that extra time to reserve. Under AGENTS.md, scrutinize any
slice exceeding 5 minutes, and at 10 minutes stop and split/reassess unless a
specific external-wait exception is recorded. Do not extend the 60-minute mission;
retain gaps and use the final slice for evidence and cleanup.

### 1. Usable recording route and short-speech baseline
Type: Behavior
Status: done
Proof: Identified app/session/audio route, visible controls, one short passage's
result or exact blocked step, and a labelled timing baseline.
Accepted: [Paced real-service baseline and persisted text/title](../../seeds/SEED-066-voice-input-manual-evidence.md#recording-route-and-short-speech-baseline);
~9 min including the bounded origin retry. Root inspected the saved note's
DOM, passage, and title. Hardware capture remains a labelled gap.

Behavior: Given the selected app and disposable note, sign in, find Audio tools,
feed a known short passage, and stop, once into an empty body and once after
existing text. Observe recording/processing feedback,
first text, final persisted body, and any later title update. Confirm the service
mode and input path. Failure stops dependent journeys; preserve a useful access
or capability gap, without counting the unsuccessful journey as covered.

### 2. Sustained speech and revision evidence
Type: Behavior
Status: done
Proof: Audio duration plus intermediate/final text snapshots and update timings.
Accepted: [A → B → C erasure, Flush and Stop evidence](../../seeds/SEED-066-voice-input-sustained-evidence.md);
~4 min observation. Root inspected persisted baseline plus C; exact input,
29.168 s duration, pause, network and DOM timings retain the scoped proof.

Behavior: Given the verified route, dictate a longer passage with pauses and a
sentence resolved near its end. Compare recent-sentence revisions with changes
to completed passages; record rewriting/erasure if observed. Try Flush Audio as
well as the final stop where available, separating their timings.

### 3. Existing-content preservation evidence
Type: Behavior
Status: done — [Accepted original/addition/reload comparison](../../seeds/SEED-066-voice-input-manual-evidence.md#existing-content-preservation), ~3 min; scoped timing gaps retained.
Proof: Original body, spoken addition, and final saved body comparison.

Behavior: Given a note with recognizable existing paragraphs, dictate an addition
and inspect whether old text is preserved, replaced, erased, or duplicated.

### 4. Manual-edit preservation during processing
Type: Behavior
Status: done — [Accepted pending visible-edit loss and saved-edit race gap](../../seeds/SEED-066-voice-input-manual-evidence.md#manual-edit-preservation-during-processing), ~5 min.
Proof: Identifiable manual edit, its timing relative to pending audio processing,
and the final saved text, or an explicit inability to reach that precondition.

Behavior: Given a pending audio result, edit the body through the supported UI.
When the result arrives, observe whether that edit survives. Do not simulate a
service delay without labelling the narrower race observation.

### 5. Repeated-session content evidence
Type: Behavior
Status: done — [Accepted reuse of consecutive distinct inputs and saved states](EXECUTION.md#repeated-session-proof-mapping), ~1 min; no new recording needed.
Proof: Two distinguishable dictated inputs and saved content after each session.

Behavior: Given a completed session, start and stop another on the same note.
Observe accumulation, duplication, or stale content across those sessions.

### 6. Late-result behavior after navigation
Type: Behavior
Status: done — [Accepted pending navigation, destination preservation and source truncation](../../seeds/SEED-066-voice-input-navigation-evidence.md), ~4 min; causality remains unproved.
Proof: Original/destination note identities and text before navigation and after
the pending result settles, or a gap if no pending result can be reached.

Behavior: Given a pending result on a disposable note, navigate to another note,
then return. Observe where results arrive and whether either note's content is
unexpectedly changed. Record the supported navigation and recording state.

### 7. Automatic-title behavior evidence
Type: Behavior
Status: done — [Accepted Untitled/repeat and user-chosen title sequences](EXECUTION.md#automatic-title-proof-mapping), ~1 min reuse; title policy remains undecided.
Proof: Timestamped title sequences for `Untitled` and user-chosen titles across
updates, with a repeat session where the budget permits.

Behavior: Given those two title starting states, dictate and observe changes.
Compare observations with the owner's unwanted ongoing changes; leave the choice
of disabling titles versus one-time generation to the improvement epic.

### 8. Explicit title-entry discovery
Type: Behavior
Status: planned
Proof: Available route or absent capability for explicit title dictation,
including the note-creation journey, and the usability evidence supporting it.

Behavior: Given an author entering a title, inspect editing and creation controls
and attempt supported voice entry. Record absence as an improvement requirement,
not a regression against an invented existing promise.

### 9. Available interruption or failure feedback
Type: Behavior
Status: planned
Proof: One safely reachable denied-permission, device, interruption, or service
failure journey, visible feedback/recovery, and remaining unobserved cases.

Behavior: Given the available environment, choose the cheapest relevant failure
probe without changing shared service configuration. Observe what the author sees
and can do next. This secondary probe may be skipped to confirm a higher-value
finding; record the gap and the reason. No exhaustive failure matrix is promised.

### 10. Reviewable findings and clean stopping point
Type: Behavior
Status: planned
Proof: Epic contains the actionable observed findings, unresolved expectations,
timings and material gaps with traceable evidence; owned temporary artifacts and
microphone streams are removed/released, and product source is unchanged.

Behavior: Given the accumulated evidence or a blocked route, update the epic's
manual-discovery section without replacing the owner's reports. Confirm only the
most consequential surprises using remaining reserve. Retain all material gaps
and distinguish unavailable observation from absence of a defect. Use the
manual-testing report convention; `Good.` is permitted only for complete planned
coverage with no actionable findings or material uncertainty.

## Current decisions and execution gates

- [Execution identity, setup, budget clock, and publication base](EXECUTION.md)
  are retained for resume; execution is authorized by the established start.
- Preparation is a one-shot refinement in the default checkout
  `/Users/terryyin/git/doughnut` on `main`, with no preparation assignment; remote
  target remains `origin/main`. This planning request authorizes no testing,
  Take, implementation, commit, or publication.
- Real-service observations are gated by slice 1. Gaps remain visible and do not
  establish transcription quality or latency. Preserve useful independent evidence
  if access fails; revise the plan before extending a failed approach.
- During execution retain accepted proof, environment/revision, literal setup or
  measurement commands when used, input and observation locations, and elapsed
  time in this plan. Record overrun learning before resplitting affected work.
- No product-code change or new automated test is planned. Required verification
  is the mapped manual evidence and `scripts/check_diff_whitespace.sh` for records.
  Automated suites are not substitutes for these observations. Follow the normal
  execution wrap-up from AGENTS.md for retained record changes: fresh post-change
  refactor review, coordinator formatting once, plan update, commit/hook and
  authorized publication. API generation is not triggered by documentation.
- Planning disposition follows the story-refinement preparation workflow: retain
  this plan and seed for review until an explicit keep or discard decision.

## Learnings

Slice 1 recording-route and short-speech evidence is recorded in the linked
[manual evidence report](../../seeds/SEED-066-voice-input-manual-evidence.md#recording-route-and-short-speech-baseline).
Proof and the record refactor were accepted; whitespace check passed. The earlier live probes and static inspections
above remain planning evidence with their narrower boundaries.

## Preparation review

The earlier access concern is cleared by fresh live observation on 2026-10-03:
`manual` sign-in and note access work in the running Development app, and a real
transcription-plus-retouch request succeeds. The remaining premise, a controlled
audio route through the browser UI, is bounded by slice 1, whose failure stops
dependent journeys and keeps the gap. No slice-boundary, proof-mapping, or
product-design concern remains. The plan itself grants no execution authority.
