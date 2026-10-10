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
2026-10-10 the owner supplied three UI stories, in priority order, for the
top of the product backlog; the first is delivered and the two below remain. These are non-executable story
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

<a id="single-button-voice-input"></a>
### Record voice input with one waveform button

**Identity:** SEED-066#single-button-voice-input
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/064-single-button-voice-input/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"f08b15b3d8fef7182708cd1efdc2b6c022362978bf165aabad7bfd2395d92607","plan":"7275acea3796af7e66bbf5078d88c9b0462865b27f43fd48dab7af8bb457ec39"}}
```

**Goal:** For note authors dictating into a note's body on the web app, start
and stop dictation from the one Voice input button in the note toolbar, and
see from that button alone that their voice is being captured, with no panel
to open, read, or close.

**The author's journey today (read from source on 2026-10-10,
`140752249c`):** The note toolbar offers Audio tools among its icon buttons;
on a narrow screen it sits in the "more options" menu. Choosing it opens a
panel under the toolbar with a wide waveform strip and Record. Record swaps
in a Microphone chooser, Stop and Write text now, and the strip scrolls the
sound level. Text joins the body after each conversion. After Stop, Record is
unavailable until the final text has been added. A failed conversion at Stop
keeps the recording and offers Retry beside Record while the panel stays
open. The panel stays open, and recording continues, when the author moves
to another note; results still go to the originating note.

**Scope:**

- Voice input replaces Audio tools: the same toolbar position, the same place
  in the "more options" menu on a narrow screen, named "Voice input". Clicking
  it starts microphone capture and dictation at once. There is no panel.
- While recording, the button is highlighted like the toolbar's other active
  toggles, and its icon is a live waveform drawn from the microphone within
  the button's existing size. Silence shows a still, quiet line: the highlight
  says the microphone is on, the movement says it hears the author. The active
  button stays in the toolbar rather than the "more options" menu, as the open
  panel does today, so it is visible on a narrow screen even when dictation was
  started from the menu.
- Clicking the active button stops capture. Until the remaining speech has
  been converted and added, the button is unavailable in a distinct finishing
  appearance, neither idle nor live; then it returns to idle. The button's
  accessible name says what a click does: start, stop, or retry.
- Completely remove the panel, its waveform strip, Record, Stop, Write text
  now, the Microphone chooser, Retry as a separate control, and their
  exclusive code, tests, page objects and documentation under the removal
  scope above. Capture uses the browser's current default microphone; an
  author with several microphones chooses one through the browser's own site
  settings. The automatic switch when the current microphone disconnects
  stays: it is recording lifecycle, not a panel control.
- Recovery after a failed conversion at Stop lives in the button: it shows a
  kept-recording appearance, its name offers the retry, and clicking it
  converts the kept recording without the microphone. On success the text is
  added and the button returns to idle; on failure the toast shows again and
  the button keeps the recording. The kept recording lasts while the author
  stays on the note; leaving the note or reloading drops it. The toast at Stop
  says "Could not turn your speech into text. Your recording is kept until you
  leave this note; click Voice input to try again." The mid-speech toast stays
  "Could not turn your speech into text. Your recording is kept."
- Leaving the note while recording, by navigating to another note or page,
  stops the recording and converts the remainder into the originating note,
  as closing the panel does today. A note's button never shows another note's
  recording. If that final conversion fails, nothing is kept and the toast says
  only "Could not turn your speech into text."
- Write text now has no replacement. The held-back last sentence appears with
  the next speech or at Stop; an author who wants to read everything stops,
  and one click starts again.
- Preserve everything else: dictated-content preservation and joining,
  saving, the processing cadence, final processing at Stop, no new recording
  while finishing, the wake lock while recording, the common error toasts for
  a refused microphone and a failed save, and readers who may not edit the
  note are not offered the button.

**UI:** One toolbar button with four appearances the author can tell apart
without text: idle (a microphone icon, named "Voice input"), recording
(highlighted, the icon replaced by the live waveform, named "Stop voice
input"), finishing (unavailable and visibly quiet, named "Voice input"), and
kept recording (visibly different from idle and recording, named "Retry
turning your speech into text"). The layout, exact styling and icon are the
implementer's choice within the toolbar's existing button size.

**Key examples:**

- An author with an editable note open clicks Voice input → the microphone
  starts at once; the button is highlighted and its waveform moves as they
  speak and settles when they pause; no panel opens and no message appears.
- The author clicks the active button → capture stops; the button is
  unavailable in its finishing appearance while the remaining speech is
  converted, the text is added after the existing content and saved, and the
  button returns to idle. After reload the note holds the existing content
  followed by the dictated passage.
- On a narrow screen Voice input is in the "more options" menu; the author
  chooses it → recording starts, the menu closes, and the active waveform
  button is in the toolbar; clicking it stops the recording.
- The author speaks for longer than one processing cadence → completed
  passages appear in the body while they keep speaking, as today, and the
  final sentence arrives at Stop.
- Microphone access is refused → the common error toast explains how to allow
  it; the button stays idle.
- A mid-speech conversion fails → the common error toast reports it;
  recording continues and the unconverted speech joins the next conversion.
- The conversion at Stop fails → the toast says the recording is kept and how
  to retry; the button shows the kept recording. Clicking it converts the kept
  recording without the microphone, the text is added, and the button returns
  to idle. A retry that fails shows the toast again and keeps the recording.
- The author navigates to another note while recording → recording stops and
  the remaining speech is added to the note they left; the new note's button
  is idle.
- A recording holds no recognizable speech → the button returns to idle with
  nothing added and no message.

**Considered and excluded:** a retry action inside the toast (the shared
toast stays message-only, as the preceding story decided); keeping a
recording or a kept recording across navigation (background dictation across
notes is not a selected capability); any replacement for Write text now; a
microphone chooser elsewhere in the note page; a keyboard shortcut for voice
input (none exists today and none was asked for).

**Effort hypothesis:** M, medium confidence. The button states are small; the
removal spans the panel, the waveform strip, their tests, the E2E page object
and steps, and the voice-input documentation.

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

On 2026-10-10 the owner placed the three UI stories at the top of the
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
- Silence after speech (read from `rawSampleAudioBuffer.ts` and
  `SRTProcessor` on 2026-10-10, not measured): the sentence a pause ends is
  held back, so while the author stays silent each 20-second tick sends that
  sentence plus all the silence so far again. Whether to send nothing while
  no new speech has arrived since the last conversion is undecided.
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
- Owner's UX/UI refinement request for the one-button story, 2026-10-10,
  through the established Shunka-chan preparation.
- Effort-band convention:
  [SEED-039](SEED-039-faster-ci-feedback.md#story-decomposition).
- Later decomposition workflow:
  [dough-story-decomposition](../../.agents/skills/dough-story-decomposition/SKILL.md).
