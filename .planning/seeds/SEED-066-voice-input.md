---
id: SEED-066
status: dormant
planted: 2026-10-03
planted_during: owner shift of near-future direction from maintenance and bug fixing to audio tools, starting with voice input
trigger_when: selecting a queued voice-input story for refinement using the recorded findings
scope: large
---

# SEED-066: Make voice input fast, reliable, and easy to use

## Why This Matters

For Donut users who want to capture notes by speaking, the existing voice-input
feature should become responsive, trustworthy, and easy to discover and operate.
The owner reports that it has existed for a long time, is rarely used, and is
extremely buggy. Audio tools, beginning with usable voice input, are now the
[near-future direction](../PRODUCT-BACKLOG.md#near-future-direction).

## Parent Problem

<a id="usable-voice-input"></a>
### Make voice input fast, reliable, and easy to use

**Former epic identity:** SEED-066#usable-voice-input

On 2026-10-03 the owner accepted all nine proposed stories below and their
priority order. They replace this broad epic in the product backlog; the epic
is decomposed, not delivered. Keep this anchor for existing references.

For note authors, unreliable and slow spoken capture should become a dependable
way to add their thoughts, preserve their content and intent, and explicitly
author titles, with understandable controls.

- **For / why:** Let note authors capture their thoughts by voice without long
  waits, lost content, distracting title changes, or confusing controls.
- **Whole-epic outcome:** Voice input feels responsive, preserves the author's
  content and intent, and supports an intuitive note-authoring journey, including
  dictating titles and creating a note by speaking its title. The owner can
  evaluate the resulting experience by dictating and reviewing notes.
- **Evidence:** The recorded findings below inform decomposition. Owner reports
  remain distinct from reproduced observations and suspected causes.
- **Preparation:** The accepted decomposition is non-executable planning input.
  Each selected story still needs refinement and approach selection before
  execution planning or implementation.

## Alternatives and Decision

- **Defer or do nothing:** Leaves reproduced saved-content loss and title
  overwrites in the existing feature. The owner's current audio-tools direction
  makes repairing trustworthy capture valuable now.
- **Only stop automatic titles:** A small useful improvement, retained as its
  own story, but it does not address body preservation or responsiveness.
- **OS dictation or external transcription pasted into the editor:** The
  strongest simpler workflow. Its adequacy has not been assessed; compare it
  before investing heavily in explicit title controls. It supplies a useful
  benchmark but leaves the reported Donut voice-input defects unresolved.
- **Accepted direction:** Keep all nine stories, ordered around preservation,
  everyday dictation, then title authoring. Lower-priority stories can be dropped
  by a later owner decision. Start with safe short dictation to test whether
  useful spoken capture can preserve existing work; retain that value even if
  the remaining stories are cancelled.

## Known Reports and Discovery Evidence

These retain the original report and bounded observations. The story sections
below record the accepted scope; unresolved causes remain hypotheses.

#### Reported responsiveness problem and processing direction

- The feature is not responsive and takes a long time to show results. The owner
  expects contemporary voice input to be much faster.
- The particularly painful delay is after audio is submitted and before the
  resulting content appears in the note body.
- The owner suspects a Completion API retouch step adds latency, possibly because
  it uses a model with too much thinking effort. This is a hypothesis, not a
  verified diagnosis.
- The requested processing direction is lightweight thinking effort for voice
  input. Establish the actual delay and current processing path before choosing
  a model or implementation. No model choice or numeric latency target has been
  decided yet.

#### Reported content instability and acceptable revision

- Processing repeatedly goes back and modifies content that was already done.
- Some revision is intentional and acceptable: before a speaker finishes a
  sentence, its meaning may still be unclear, so revising a recent unfinished
  passage can be useful.
- The reported behavior goes much further: it frequently completely rewrites
  the whole content or erases the whole content. This makes dictation unreliable.
- Preserve completed content and the author's intent while allowing the limited
  revision needed to interpret speech. The exact revision boundary remains a
  question for later refinement, informed by manual observation.

#### Title behavior

- One-time automatic title generation for an `Untitled` note after enough
  dictation is deferred and has no queued story; adding it would require a
  later owner decision about its value and timing.
- Voice input should also be useful for explicitly dictating the title. In
  particular, being able to dictate the note title while creating a note would
  be valuable. Explicitly dictated or otherwise user-chosen titles must remain
  under the user's control.

#### Usability

- The owner describes the current UI as ugly and unintuitive.
- Make the voice-input journey easy to find, understand, and use. Specific UI
  changes should follow observation of the current experience rather than an
  assumed redesign.

<a id="manual-discovery-findings"></a>
#### Findings supplied by manual discovery

**2026-10-03 bounded discovery:** Chrome/macOS, `manual`, reused local Development at `localhost:5175`, disposable notebook `23`. Naturally paced
synthetic MediaStreams exercised the real recorder/worklet, transcription and retouch services. No service response or clock was simulated. The known backend
revision is `a02dbb2697…`; frontend/runtime revision was not fully established. The initial `127.0.0.1` automation-control failure has no established cause and is not evidence that a human click fails.

- **Completed dictated content lost:** In a 29.168 s passage with an 8.2 s pause, completed orchard facts appeared, Flush replaced them with a middle
  fragment, and the final update retained only the last sentences. Reload confirmed the loss while preexisting Harvard content survived. Reproduction
  and intermediate text are in the [voice-input documentation](../../docs/voice-input-observations.md#dictating-a-passage-with-a-pause-and-write-text-now).
- **Existing paragraph truncated during navigation journey:** Navigate from source `13726` while processing to destination `13727`, then return/reload.
  Both results persisted to the source, whose first paragraph became literal `...uesday.`; the other four paragraphs and destination sentinel survived.
  An independent fresh page confirmed the saved state. The [voice-input documentation](../../docs/voice-input-observations.md#existing-content-can-be-truncated-during-a-navigation-journey) retains before/after text. The causal role of navigation remains unproved.
- **Explicit voice-title entry was not discovered:** Inspecting supported title editing, Audio tools/Advanced Options and New note revealed no voice-title
  control. This is a bounded improvement opportunity, not a failed promise; OS dictation, extensions and processing instructions remain unobserved.
- **Responsiveness baseline:** First body text appeared 25.17 s after capture began, approximately 3.96 s after Stop, for the 18.356 s short input. Real
  audio requests took 4.42/4.61 s. Sustained requests took 5.25/3.14/3.53 s and the body settled before Stop. Other journeys lack
  reliable Stop-to-final measurements. Timings establish neither cause nor a numeric acceptance target.
- **Scoped preservation succeeded:** Two recognizable original paragraphs and a distinct lighthouse addition survived reload and the next distinct session;
  no duplication or stale prior addition was observed in those sessions. The navigation destination survived. These comparisons do not establish a general preservation guarantee or defect frequency.

**Material gaps:** Hardware capture, permission/device behavior, interruption, service-failure feedback and recovery were not exercised; secondary failures
were skipped to reserve time for saved-loss confirmation and cleanup. The planning-stage empty-content API HTTP 500 was not reproduced through the UI.
An empty-body UI baseline did succeed. Causes, frequency, the acceptable recent revision boundary, and quantitative latency expectations remain unresolved.


## Story Decomposition

The original nine-story decomposition is recoverable in Git history. On
2026-10-10 the owner supplied the following three stories, in this priority
order, for the top of the product backlog. These are non-executable story
records; capturing them does not start implementation.

Effort bands are S = 30–60 minutes, M = 1–2 hours, and L = 2–4 hours, including
delivery. These are comparative hypotheses, not a delivery schedule. Refine or
resplit any story likely to exceed L before execution.

Every story is evaluated by the note author through the product. Preservation
includes saved state after reload, rather than only a transient editor result.
The accepted order is global priority; it does not require serial technical
implementation of independent stories. No story authorizes a model choice,
technical redesign, or speculative infrastructure.

### Removal scope for these stories

Every removal below means deleting the feature and transitively deleting all
code, styles, helpers, dependencies, fixtures, and tests used only by it. Keep
shared code needed by surviving behavior. Sweep the whole product, including
backend, frontend, CLI, MCP, documentation, agent guidance, and generated
artifacts, for obsolete references. Verify surviving or replacement behavior
with positive tests; delete removed-feature tests instead of replacing them
with negative assertions, absence checks, or runtime guards. Record the final
one-time removal sweep as an acceptance reading, following principle 7 in
`AGENTS.md`; Git history retains the removed implementation.

<a id="simplify-voice-input-feedback"></a>
### Simplify voice input controls and feedback

**Identity:** SEED-066#simplify-voice-input-feedback
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/063-simplify-voice-input-feedback/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"4832be1b79e316353493b5cfb34fa5fd6f7b75ba822dbd6b046e8ee222226f6d","plan":"977d672824e05a476e02dffb2b06386935f1ac1be73903f333898ce583de62ba"}}
```

**Goal:** For note authors dictating into a note's body on the web app, make
everyday voice input quieter and simpler: the author watches their words
arrive in the note instead of reading status messages, hears about problems
the same way as everywhere else in Donut, and gets spoken text into the note
promptly at an affordable API cost.

**The author's journey today (read from source on 2026-10-10, `ca792965c2`):**
Audio tools opens from the note's More options. The panel shows a waveform,
a status line, Record, a disabled Save audio and Full screen. Record swaps in
a microphone selector, Stop and Write text now, and the status line says
“Recording. Speak now.”. Text appears in the body after each conversion
(a 60-second timer or a pause of about 3 seconds). After Stop, the status
line reads “Turning your speech into text…”, then “Added to your note.”,
“No speech was turned into text.” or a failure sentence, with Retry beside a
failed conversion. Mid-speech conversion failures and raw save errors appear
as an inline alert box; the microphone-permission failure replaces the status
line. Full screen shows a black screen holding only the latest error text.
Closing the panel while recording stops it and converts what was spoken.

**Scope:**

- Completely remove Save audio and Full screen, including their exclusively
  supporting code and tests under the removal scope above.
- Remove the status line and every normal-operation message: ready,
  recording, converting, added, and nothing added. A recording that produced
  no recognizable speech ends quietly; the live waveform during recording is
  the author's evidence that sound is being captured. Preserve the internal
  recording phases needed to operate recording correctly.
- Recording state stays perceivable from the controls alone: while idle the
  panel offers Record; while recording it offers Stop, Write text now and the
  microphone selector with a moving waveform; after Stop, Record stays
  unavailable until the final text has been added, then returns.
- Report every problem through the common error toast that the rest of Donut
  uses for failed requests: microphone unavailable, a failed conversion
  (mid-speech or at Stop), a failed microphone switch, and a failed save.
  Remove the inline status/alert presentation and its exclusive machinery.
  The toast explains what happened and, when applicable, that the recording
  is kept until Audio tools is closed. Each failed mid-speech conversion
  raises its own toast; its audio stays queued for the next conversion as
  today.
- Keep Retry as a panel control, not a message: it appears only while a
  recording that failed to convert at Stop is kept, converts that kept
  recording, and disappears once the text is added or the panel closes.
  A recovery action inside the toast is rejected because the scope forbids
  a second error UI; the shared toast stays message-only.
- Keep the current panel, its waveform, microphone selector, Write text now,
  and surviving recording behavior for this story; the next story owns the
  single-button interaction and removes the panel.
- Analyze feedback latency versus API cost before choosing processing cadence.
  Aim to show transcribed text as soon as an affordable pace allows. Recent
  unfinished speech may be revised if useful; preserve completed text, existing
  note content, and the author's intent. Here, the owner's “translation” means
  spoken audio becoming text, without adding language translation.

**Key examples:**

- The panel is open and idle → it shows the waveform and Record only; no
  text tells the author what to do.
- The author clicks Record and speaks → Stop, Write text now and the
  microphone selector appear and the waveform moves; nothing is written
  about recording. Completed passages appear in the body at the chosen
  cadence, after existing content, and are saved.
- The author clicks Stop → Record is unavailable while the last speech is
  converted; the text is added and saved through the existing flow, Record
  returns, and no message announces the result. After reload the note holds
  the existing content followed by the dictated passage.
- The author stops a recording that held no recognizable speech → the
  panel returns to idle with nothing added and no message.
- Microphone access is refused → the common error toast explains that the
  microphone could not be used and how to allow it; the panel stays idle with
  Record available.
- A mid-speech conversion fails → the common error toast reports it; recording
  continues and the unconverted speech joins the next conversion.
- The conversion at Stop fails → the common error toast says the speech could
  not be turned into text and the recording is kept until Audio tools closes;
  Retry appears in the panel. Clicking Retry converts the kept recording, the
  text is added, and Retry disappears.
- Saving the dictated text fails → the common error toast reports the failed
  save; no raw error object is shown in the panel.
- The author closes Audio tools while recording → recording stops and the
  remaining speech is converted and saved as today.

**Analysis to complete during planning:** Measure time to first visible text
and Stop-to-final-text, API calls and billable audio per recorded minute, and
the effect of resubmitted unfinished audio. Compare shorter bounded chunks,
pause-triggered processing, and streaming only where justified by measured
benefit and current provider pricing. From the author's side, the cadence
decides how long the body stays unchanged while they keep talking; the
constraint stays that written text is never revised and the last unfinished
segment is held back. Record the chosen cadence and cost estimate before
implementation. The affordable budget, latency target, and any model choice
remain to be established by that analysis; the measurement needs the real
transcription service, so plan it as an early probe slice.

**Current source reading (2026-10-10, `ca792965c2`):**
`audioProcessingScheduler.ts` uses a 60-second timer, pause-triggered flush,
and final processing at Stop; `rawSampleAudioBuffer.ts` triggers after three
seconds of silence. `AiAudioController` uses SRT transcription for mid-speech
chunks and plain text at Stop, with `whisper-1` and
`gpt-4o-mini-transcribe` respectively in `OtherAiServices`. Transcription
requests bypass the shared loading/error wrapper, so their failures reach the
common toast only through this story; body saves already go through the
shared client. This is source evidence, not a fresh latency measurement or
cost estimate. The earlier responsiveness observations above remain
historical evidence.

**Considered and excluded:** a message when no speech was recognized
(normal operation; the waveform already shows capture), suppressing repeated
mid-speech failure toasts (special-casing without a stated need), automatic
retry of a failed Stop conversion (recovery stays an author decision), and
any layout or component choice for the surviving panel (the next story
replaces it).

**Effort hypothesis:** L, low confidence until cadence/cost analysis bounds
the work. Reassess size before execution.

**Safe stopping point:** The simpler current panel remains useful even if the
single-button redesign is deferred.

<a id="single-button-voice-input"></a>
### Record voice input with one waveform button

**Identity:** SEED-066#single-button-voice-input
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"unselected"}
```

**Goal:** For note authors, start and stop body dictation directly from one
compact voice-input button with immediate visual feedback.

**Scope:**

- Replace the Audio tools concept with Voice input at the current button's
  location. Clicking it immediately starts microphone capture and dictation.
- Highlight the active SVG button and turn its icon into a waveform reacting
  to live voice input within the button's existing size.
- Clicking the same button again ends recording and completes the existing
  text-processing and saving behavior.
- Completely remove the Audio tools panel, its separate waveform display,
  Record, Stop, Write text now, and other panel controls, and their exclusive
  supporting code and tests under the removal scope above. Preserve recording
  and processing responsibilities needed by the new interaction.
- Preserve other behavior: dictated-content preservation, saving, final
  processing at Stop, microphone lifecycle, and applicable failure recovery.
  Use the common toast policy from the preceding story. Resolve how any
  necessary recovery action remains reachable within the compact interaction
  during refinement; do not silently remove recovery with its old control.

**Key examples:**

- An idle note author clicks Voice input once → recording starts immediately;
  the button is highlighted and its waveform responds to the microphone.
- The author clicks the active button → capture stops, remaining speech is
  processed and saved, and the button returns to its idle appearance.
- The author dictates after existing body text → the resulting note preserves
  existing content and adds the new dictated passage as before.
- Capture or conversion fails → the common toast reports the exception and
  applicable recovery can be performed through the simplified interaction.

**Effort hypothesis:** M, medium confidence; recovery interaction needs
refinement. Queue order expresses incremental delivery, not a technical block.

**Safe stopping point:** Body voice input is usable from one compact control
independently of improvements to title dictation.

<a id="unobtrusive-selection-aware-spoken-title"></a>
### Restore title styling and make spoken title editing unobtrusive

**Identity:** SEED-066#unobtrusive-selection-aware-spoken-title
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"unselected"}
```

**Goal:** For note authors, restore a visibly styled note title and make the
rarely used Speak the Title feature compact and respectful of editing intent.

**Scope:**

- Investigate and restore the title's intended heading styling; the owner
  reports that it currently looks like ordinary content text. The cause and
  regression point are unverified.
- Put a small Speak the Title button inline at the end of the title area,
  keeping title readability and editing primary. Apply the compact treatment
  to existing-note title editing and New note, where the shared control is
  currently used.
- Apply recognized speech according to the title's text selection or caret:
  replace selected text, insert at the caret, and append when the caret is at
  the end. Preserve the selection/caret intent when clicking the speech button
  moves focus. Preserve surrounding title text.
- For a title with no established caret/selection, refine the default before
  implementation; the current implementation replaces the entire title.
  Whole-title replacement should be available through selecting the whole
  title. Do not silently infer replacement when the author placed the caret
  at the end.
- Preserve existing title validation, explicit inbound-reference rename
  decisions, and persistence. Use common error toasts for exceptions and the
  shared removal scope for UI/supporting code superseded by this redesign.

**Key examples:**

- A note is displayed or its title is edited → the title has the intended
  heading typography and a small inline speech control.
- The title is “Orchard” with the caret at the end; the author dictates
  “notes” → the title becomes “Orchard notes”, preserving the original title.
- In “Orchard notes”, the author selects “Orchard” and dictates “Garden” →
  the title becomes “Garden notes”. Selecting the whole title instead replaces
  the whole title with recognized speech.
- The author places the caret between existing words and dictates → the
  recognized words are inserted there with appropriate word spacing; title
  text on both sides survives.
- A referenced title is edited by speech → the existing explicit reference
  choice and title-save behavior still apply.

**Refinement decision:** Agree the no-caret/no-selection default; recommended
default is append to a nonempty title and fill an empty title. Confirm against
the existing title editor's focus/selection behavior.

**Effort hypothesis:** M, medium confidence until styling and selection
behavior are inspected.

**Safe stopping point:** Styled titles and compact, intentional title dictation
remain useful independently of body dictation changes.

## Ordering and Scope Reduction

The owner accepted the original nine-story order on 2026-10-03. The product backlog
owns their global priority. Preservation of existing content and completed speech
comes first; title control is a small independent trust improvement. Concurrent
editing and failure recovery extend reliability before responsiveness and the
first-use journey. Title creation and renaming then extend explicit authoring.

Navigation is preservation evidence for the first two stories: source-note
content was damaged while the destination survived. Its causal role is unknown.
Recheck that journey after preservation changes and propose a separate navigation
story only if a distinct remaining problem is established. Background dictation
across notes is not a selected capability.

Keep remaining queued voice stories unless the owner later reduces scope.
Optional one-time automatic title generation has no queued story.

On 2026-10-10 the owner placed the three UI stories above at the top of the
backlog in their stated order: simpler controls/feedback with cadence-cost
analysis, one-button body dictation, then title styling and spoken editing.
No blocking dependency is inferred from shared recording code or that order.

## Open Decisions for Later Work

- The controlled browser route is established at `localhost:5175`; genuine
  hardware capture and permission behavior remain gaps. Resolve the deployed URL
  and account only if that alternative is used.
- Measurable responsiveness expectations and the actual contribution of audio
  capture, transcription, and applying results to the note.
- How to recognize the current unfinished sentence versus completed passages,
  including a long pause inside a sentence, while preserving author intent.
  Partly decided on 2026-10-03: the last transcription segment is held back
  until the next chunk or Stop, and written text is never revised. Sentence
  recognition itself remains undecided.
- Any distinct navigation problem left after source-content preservation fixes.
- The adequacy of OS dictation or external transcription for explicit title
  input: for New note the owner chose a native control on 2026-10-06 (recoverable
  at `d38952da4e9b635b567ddaf21ad444df537ee641:.planning/seeds/SEED-066-voice-input.md`);
  speaking a title on an existing note used the same native control (recoverable
  at `b4bb2f90a0d74a938dcadb98e8f279bbf09263e0:.planning/seeds/SEED-066-voice-input.md#rename-with-spoken-title`).
  Optional one-time automatic title generation stays deferred.

## Breadcrumbs

- Owner's voice-input problem report, 2026-10-03, in this conversation.
- Owner's acceptance of all nine proposed stories and their priority, with
  authorization to record them on main and sync origin, 2026-10-03.
- Owner's three UI simplification stories, complete-removal requirements, and
  request to include affordable transcription-feedback analysis in the first
  story, 2026-10-10; authorized capture directly on main, commit, and sync origin.
- Effort-band convention:
  [SEED-039](SEED-039-faster-ci-feedback.md#story-decomposition).
- Later decomposition workflow:
  [dough-story-decomposition](../../.agents/skills/dough-story-decomposition/SKILL.md).
