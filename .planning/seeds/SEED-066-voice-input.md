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

- **Existing paragraph truncated during navigation journey:** Navigate from source `13726` while processing to destination `13727`, then return/reload.
  Both results persisted to the source, whose first paragraph became literal `...uesday.`; the other four paragraphs and destination sentinel survived.
  An independent fresh page confirmed the saved state. The [voice-input documentation](../../docs/voice-input-observations.md#existing-content-can-be-truncated-during-a-navigation-journey) retains before/after text. The causal role of navigation remains unproved.
- **Explicit voice-title entry was not discovered:** Inspecting supported title editing, Audio tools/Advanced Options and New note revealed no voice-title
  control. This is a bounded improvement opportunity, not a failed promise; OS dictation, extensions and processing instructions remain unobserved.
- **Responsiveness baseline:** First body text appeared 25.17 s after capture began, approximately 3.96 s after Stop, for the 18.356 s short input. Real
  audio requests took 4.42/4.61 s. Sustained requests took 5.25/3.14/3.53 s and the body settled before Stop. Other journeys lack
  reliable Stop-to-final measurements. Timings establish neither cause nor a numeric acceptance target.
- **Scoped preservation succeeded:** Two recognizable original paragraphs and a distinct lighthouse addition survived reload;
  no duplication or stale prior addition was observed in that session. The navigation destination survived. These comparisons do not establish a general preservation guarantee or defect frequency.

**Material gaps:** Hardware capture, permission/device behavior, interruption, service-failure feedback and recovery were not exercised; secondary failures
were skipped to reserve time for saved-loss confirmation and cleanup. The planning-stage empty-content API HTTP 500 was not reproduced through the UI.
An empty-body UI baseline did succeed. Causes, frequency, the acceptable recent revision boundary, and quantitative latency expectations remain unresolved.


## Story Decomposition

The original nine-story decomposition is recoverable in Git history. On
2026-10-10 the owner supplied three UI stories, in priority order, for the
top of the product backlog; all three are delivered. These are non-executable story
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

<a id="unified-voice-input-control"></a>

### Use one reactive voice-input button for note titles and content

**Identity:** SEED-066#unified-voice-input-control
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/002-shared-voice-input-button/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"e93ffca4910ac9b2d8b84f9f8fca905cc22e5f82d119806c7f2ea4d3c675a9e5","plan":"0d8a118f702f9fd893dd5fc864a2c30d632202117614478b0916ff6e4418f191"}}
```

- **Goal:** A note author who speaks a new note's title, an existing note's
  title, or a note's content sees the same button behave the same way: it
  shows that the click was taken, then a live waveform that moves with their
  voice while it records, then that their speech is being turned into text.
  This serves the seed's aim of voice input that is easy to understand, and
  keeps later changes to recording feedback in one implementation.
- **Reported behavior:** On 2026-10-12 the owner reported that content dictation
  turns its button into a waveform that responds to speech, while creating a
  note by speaking its title and updating a title do not show that feedback.
  Whether those title journeys already share a button was uncertain.
- **Scope — required:**
  - One shared button implementation serves the New note title field and the
    note toolbar. It has four faces, the same size in both places: a
    microphone when idle; a spinner, unavailable, while the microphone starts
    and again from Stop until the last speech has become text; a live waveform
    on a highlighted, pressed button while recording; and the retry arrow with
    its warning tint while a content recording is kept.
  - The waveform shows in all three journeys and is drawn from the microphone
    level of the recording that is running. In silence it is a still, quiet
    line, as in content dictation today.
  - Startup looks the same in all three (owner decision, 2026-10-12): from the
    click until the microphone is ready the button is unavailable and shows
    the spinner. Content dictation gains this state; today its button stays
    idle and clickable during startup.
  - Names stay with the place (owner decision, 2026-10-12): in New note
    "Speak the title" and "Stop speaking the title"; in the toolbar
    "Voice input", "Stop voice input" and "Retry turning your speech into
    text". While unavailable the button carries its idle name, as today.
  - Each place keeps its own frame: in New note the button stays inside the
    title field, before the Wikidata button, at the field's height; in the
    toolbar it stays a toolbar button.
  - Delete `SpeakTitleControl.vue` and everything only it used. Record the
    one-time sweep for the deleted names as an acceptance reading.
  - `docs/voice-input.md` and `docs/voice-input-spoken-title.md` describe the
    button as it then is.
- **Scope — kept as it is:** the choice between title and content by title
  focus; where the words go and the read-only field during a session; the
  pending marker; title speech turning into text only at Stop; title wording
  refinement; content preservation, joining and retry; the error toasts; the
  toolbar button staying in the toolbar while a session runs on a narrow
  screen; Submit unavailable in New note during a session.
- **Scope — not promised:** a retry for title recordings; a different look
  for the waveform; a reduced-motion version of the waveform; choosing a
  microphone; any change to transcription or its timing.
- **Key examples:**
  - *New note.* The title field shows a microphone named "Speak the title".
    The author clicks it → the button is unavailable with a spinner until the
    microphone is ready → it is highlighted, named "Stop speaking the title",
    and its waveform rises and falls with the author's voice and lies still
    when they pause. They click it → spinner, unavailable → the words are in
    the title and the microphone is back.
  - *Existing title.* The caret is in the title. The author clicks
    "Voice input" in the toolbar → spinner, unavailable → highlighted
    waveform, named "Stop voice input", moving with their voice (today it
    shows a microphone that does not move). They click it → spinner → the
    words are at the caret and the button is idle.
  - *Content.* The title is not focused. The author clicks "Voice input" →
    spinner, unavailable, until the microphone is ready (today: an idle
    button that takes another click) → the waveform as today. Stop behaves as
    today.
  - *Microphone refused, any of the three.* The author clicks the button →
    spinner → the error toast "Could not use the microphone…" shows, the
    button is an idle microphone again, and nothing was made read-only.
  - *Failed conversion at Stop.* For content the button shows the retry arrow
    with the warning tint, as today. For either title it returns to the idle
    microphone and the title is unchanged, as today.
- **Evaluation:** The owner starts speaking in each of the three journeys, on
  a real microphone, and sees the same startup spinner, the same waveform
  following their voice, and the same spinner after Stop; the spoken text
  reaches the intended title or content. The sweep for the deleted control's
  names over the product is a reading, not a test.
- **Evidence, read on 2026-10-12 at `5a80de7bda`, not run:**
  - *Facts.* The three journeys use two buttons. `NoteNewForm.vue` uses
    `SpeakTitleControl.vue` (microphone, spinner; no waveform). The toolbar's
    `NoteVoiceInputButton.vue` serves both the existing title and content,
    and leaves the waveform out for title recordings because it reads the
    level from the content recorder only. No recorded decision asked for that
    (commit `d82ffb11d9`).
  - *Settled premise.* The live level is available while a title is recorded:
    title and content recording both use `createAudioRecorder`, whose
    `getAudioData` reads the microphone receiver directly, whether or not
    conversion waits for Stop (`audioRecorder.ts`,
    `audioProcessingScheduler.ts`). `useTitleVoiceInput.ts` holds its recorder
    privately, so the shared button needs it handed over.
  - *Startup today.* Toolbar title: unavailable with a spinner. New note:
    unavailable with a microphone. Content: no startup state.
  - *Still a hypothesis.* A second click on the content button during startup
    may open the microphone twice; read from `useNoteVoiceInput.ts`, not run.
    The startup state this story adds removes the second click either way.
  - *Not observed.* The waveform on real hardware in the title journeys; the
    owner's evaluation covers it.
- **Architecture:** The button owns only what the author sees and the click;
  it is told the recording's state and where to read its level. Title and
  content recording keep their own rules and stay with their editors. Title
  recording must now give out its live level, and content recording gains a
  starting state. No Accepted ADR (0000–0007, read on 2026-10-12) governs
  this front-end structure and none is contradicted; the failure toasts are
  unchanged.
- **Effort hypothesis:** M. The premise that made the earlier estimate
  uncertain is settled.
- **Depends on:** No blocking story dependency established.
- **Safe stopping point:** Consistent voice controls stand alone while each
  editor retains its existing text-application behavior.

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

On 2026-10-12 the owner placed the shared reactive voice-input control story
first, followed by the mobile toolbar layout and spelling-cloze repair stories.
This capture authorizes backlog maintenance only.

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
- Inserting a link from the link dialog writes into a body editor that voice
  input has made read-only (`NoteEditableContent.vue` inserter; read from the
  code on 2026-10-11, not run), which can shift where the dictated text lands.
  Whether such an insert is refused, held until the session ends, or allowed
  with the place following it is undecided.
- When the page moves to another note while a title is being spoken, the
  session ends and the words heard so far are dropped. Whether the note left
  should receive them is not confirmed by the owner.
- The pending marker's backing patch covers about one character of the text
  after the target for the length of a session. The owner has not confirmed
  that look.
- Not run, found on 2026-10-11: the paste-choice bar may be placed from Quill
  bounds measured from the editor's container while the code treats them as
  viewport coordinates (`QuillEditor.pasteInsertionViewportRect`,
  `pasteChoicePosition.ts`); and the two rename scenarios in
  `record_live_audio.feature` can deliver audio before the recorder has
  started.

## Breadcrumbs

- Owner's investigate, UX/UI and architecture refinement request for the
  shared voice-input button story, 2026-10-12, through the established
  Kirara-chan preparation; the owner chose the New note names and the common
  startup state in that session.
- Owner's three-story request, 2026-10-12: unify the voice button and reactive
  waveform across title creation, title editing, and content dictation; give
  the mobile toolbar full width with one row and a visible options menu; mask
  the spelling answer before a trailing parenthesized title qualifier.
  Authorized capture at the top of the backlog directly on main and sync origin.
- Owner's two-story request, 2026-10-11: remove the existing-title microphone
  and route toolbar voice input by title focus; separately instruct the AI to
  refine speech as a concise title with suitable punctuation, without authority
  to truncate its content. Authorized capture directly on main and sync origin.
- Owner's voice-input problem report, 2026-10-03, in this conversation.
- Owner's acceptance of all nine proposed stories and their priority, with
  authorization to record them on main and sync origin, 2026-10-03.
- Owner's three UI simplification stories, complete-removal requirements, and
  request to include affordable transcription-feedback analysis in the first
  story, 2026-10-10; authorized capture directly on main, commit, and sync origin.
- Owner's UX/UI refinement request for the one-button story, 2026-10-10,
  through the established Shunka-chan preparation.
- Owner's investigate, UX/UI and architecture refinement request for the
  title styling and spoken-title story, 2026-10-10, through the established
  Hitomi-chan preparation; observation on this worktree's isolated stack.
- Effort-band convention:
  [SEED-039](SEED-039-faster-ci-feedback.md#story-decomposition).
- Later decomposition workflow:
  [dough-story-decomposition](../../.agents/skills/dough-story-decomposition/SKILL.md).
