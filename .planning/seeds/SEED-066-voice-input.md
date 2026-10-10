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
  and intermediate text are in the [voice-input documentation](../../docs/voice-input-observations.md#dictating-a-passage-with-a-pause-and-a-requested-mid-speech-conversion).
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
top of the product backlog; the first two are delivered and the one below remains. These are non-executable story
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

<a id="unobtrusive-selection-aware-spoken-title"></a>
### Restore title styling and make spoken title editing unobtrusive

**Identity:** SEED-066#unobtrusive-selection-aware-spoken-title
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/065-unobtrusive-spoken-title/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"14bafa66330619f581a19cbe663179daacfbdccaa2e1fd7550e001b2e1ca85f7","plan":"eec22a208f250c4b8e984d211d86480c878e37c30fd7f2732d2c6220fd2f0cab"}}
```

**Goal:** For note authors on the web app, the note title reads as the
note's heading again, and speaking a title becomes a small control at the
end of the title that puts the heard words where the author's caret or
selection is, on an existing note and in New note, so dictation never
replaces a title the author did not choose to replace.

**What was observed (2026-10-10, this worktree at `f6fa23a17e`, isolated
stack, Chromium through Playwright; source read at the same revision):**

- The title is 24 px at normal weight (400). Body paragraphs are 16 px at
  normal weight; a body `##` heading is 24 px bold. The title is therefore
  the same size as a body section heading but lighter, which is what reads
  as ordinary content text. This is not a recent regression: the scoped rule
  `font-size: 1.5rem; font-weight: 400` in `NoteEditableTitle.vue` dates
  from the Tailwind migration (`ac2014f354`, 2024-12-07); before it the
  title took Bootstrap's heading weight. The one-button story plan
  (`064`) does not touch it.
- Speak the title today is a text-labelled button under the title on the
  note page and under the title field in New note, with a status line above
  it while listening, converting, after nothing was heard, and after a
  failure. On an existing note the heard words replace the whole title; in
  New note they join the end of the current title and replace an untouched
  default "Untitled".
- The title editor is a single-text-node contenteditable
  (`SeamlessTextEditor`). Its paste handler already inserts clipboard text at
  the caret or over the selection and puts the caret after the pasted text.
  No spoken path uses it yet.
- Clicking Speak the title moves focus to the button, but the document
  selection stays in the title: with "Orchard" selected in "Orchard notes",
  the selection still read "Orchard" (offsets 0–7) after the click. New note
  opens with "Untitled" focused and wholly selected. Observed in Chromium
  only; Safari and Firefox remain a hypothesis, so the design must not
  depend on the browser keeping the selection (see Architecture).

**Scope:**

- **Title styling.** The note title is bold and larger than a body section
  heading, so it reads as the page heading above the content, in view and
  while editing, on the note page for editors and readers alike. The exact
  size is the implementer's choice (for example the app's `text-2xl` to
  `text-3xl` with `font-bold`); the sidebar, note cards and other title
  renderings are unchanged.
- **One small control.** Speak the title is a small icon button, the same
  compact size as the toolbar's icon buttons, at the end of the title line
  on the note page and inside the New note title field before the Wikidata
  button. It has three appearances the author can tell apart without
  reading: idle (named "Speak the title"), listening (highlighted like the
  toolbar's active toggles, named "Stop speaking the title"), and converting
  (unavailable and visibly quiet, named "Speak the title"). The accessible
  name says what a click does. There is no status line. Readers who may not
  edit the note are not offered the button.
- **Words go where the author's caret or selection is**, on the note page
  and in New note alike. A selection is replaced by the heard words; a caret
  inserts them there with the dictation joining rule applied on both sides
  (one space in Latin scripts, none next to Japanese or Chinese writing); a
  caret at the end appends. The caret, or the selection, the author had
  when they pressed the button is the target even though the click moves
  focus. After the words are placed, the title has focus and the caret sits
  after the inserted words, so typing a correction continues naturally.
- **No caret in the title.** When the author has placed neither caret nor
  selection in the title, the words join the end of a nonempty title and
  fill an empty one. In New note an untouched default "Untitled" is still
  replaced: it is a placeholder, not an authored title, so this holds even
  when the author clicked elsewhere first and lost the initial selection.
  Replacing a whole authored title is done by selecting it all (triple
  click or select all) and speaking.
- **Errors in the common error toast**, as the body button uses: a refused
  microphone keeps its existing wording; a failed conversion at Stop says
  "Could not turn your speech into text." and leaves the title alone. A
  recording in which nothing was heard returns the button to idle with the
  title unchanged and no message, as the body button does. There is no
  retry control; speaking again starts a fresh recording.
- **Preserve everything else:** illegal-character replacement and the
  existing warnings, the explicit reference choice and discard-on-leaving
  for a title other notes link to, title autosave, Submit unavailable and
  Enter inert in New note while listening or converting, the recorder
  stopped when the dialog closes or the note page unmounts, the search for
  existing notes with the resulting title, and body dictation untouched.
- **Remove without trace**, under the removal scope above: the status line
  and its wording, the text-labelled "Speak the title" and "Stop" buttons,
  the New note join rule that this story's placement rule subsumes (keep
  the untouched-default rule), their exclusive tests, test support, E2E
  steps and page-object methods, and the two title sections of
  `docs/voice-input.md`, which are rewritten for the new control.

**UI:** On the note page the title line is the bold heading followed, on
the same line, by the small microphone button; nothing sits between the
title and "Add property" any more. In New note the title field keeps its
bordered surface and the Wikidata button at its right end; the microphone
button sits inside the field surface just before it. Listening highlights
the button; converting greys it. Toasts appear where the app's other error
toasts do. Icon, exact size and highlight styling are the implementer's
choice within the toolbar's compact button size; the body voice button's
appearances are the reference.

**Key examples:**

- A note "Orchard notes" is displayed → its title is bold and larger than
  the body's "Pruning" section heading, and a small microphone button ends
  the title line; no text button or status line is below it.
- The title is "Orchard" and the author clicks at its end, then the
  microphone; they say "notes" and click it again → the title is "Orchard
  notes", the caret is after "notes", and the title is saved as a typed
  title would be.
- In "Orchard notes" the author selects "Orchard", speaks "Garden" and stops
  → the title is "Garden notes".
- In "Orchard notes" the author puts the caret between "Orchard" and
  "notes", speaks "harvest" and stops → the title is "Orchard harvest
  notes".
- In "りんご園" the author puts the caret at the end, speaks "の手入れ" and
  stops → the title is "りんご園の手入れ", with no space added.
- The author selects the whole title and speaks "Pear orchard care" → the
  title is "Pear orchard care".
- The author never clicked into "Orchard notes" and speaks "today" → the
  title is "Orchard notes today".
- New note opens with "Untitled" selected; the author speaks "Photosynthesis
  in desert plants" and stops → the title field reads "Photosynthesis in
  desert plants", existing notes matching it are listed, and Submit creates
  the note once with that title. The same happens when the author first
  chose a folder and so lost the selection on "Untitled".
- New note opened with the title pattern "2026-10-06 " and the author speaks
  "weekly review" → the title reads "2026-10-06 weekly review".
- A note other notes link to is renamed by speech → the reference panel
  appears with the new title; choosing how links should change saves it,
  leaving without choosing restores the old title.
- The author clicks the microphone while listening and the conversion fails
  → the common error toast says "Could not turn your speech into text."; the
  title and the caret are as before; the button is idle.
- The author speaks nothing and stops → the button returns to idle, the
  title is unchanged, and no message appears.
- Microphone access is refused → the common error toast explains how to
  allow it; the button stays idle.

**Architecture:** Placing text at the caret or selection, and restoring the
caret afterwards, is the title editor's responsibility: the spoken words go
through the same insertion the paste handler already performs in
`SeamlessTextEditor`, so caret bookkeeping lives in one place and the New
note form stops computing a joined title itself. The dictation joining rule
generalizes from "join a segment onto a base" to "insert a segment between
a before and an after" in `joinDictatedSegments`, one rule for body
passages, title appends and mid-title inserts. The target selection must be
captured before the click moves focus (for example on the button's
`mousedown`, which the reference panel already prevents from stealing
focus) or remembered from the editor's last selection, rather than read
from the document after the click, because selection persistence across a
button click was observed only in Chromium. The button's idle, listening
and converting appearances are the same concept as the body's voice button
(landed 2026-10-11): that component is bound to the body's recording
session, so the title control keeps its own stop-only session and reuses
the body button's classes, icons, naming pattern and toast rather than the
component. No Accepted ADR constrains this story beyond ADR 0006 (failure
handling): the toast is the deliberate business outcome of a failed
conversion, with no retry or recovery machinery added.

**Considered and excluded:** a status line in any form (the owner asked
for a compact control; the accessible name carries the state in words and
the appearance carries it visually, as the body button does, which
supersedes the New note story's readable status line); a retry control for
the title; a keyboard shortcut; voice title entry from the sidebar or for
folders; changing how the sidebar or note cards render titles.

**Effort hypothesis:** M, medium confidence. The styling is one rule; the
control, the insertion rule and its tests span two surfaces, the E2E steps
and the documentation.

**Safe stopping point:** Styled titles and compact, intentional title
dictation remain useful independently of body dictation changes.

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
- Owner's investigate, UX/UI and architecture refinement request for the
  title styling and spoken-title story, 2026-10-10, through the established
  Hitomi-chan preparation; observation on this worktree's isolated stack.
- Effort-band convention:
  [SEED-039](SEED-039-faster-ci-feedback.md#story-decomposition).
- Later decomposition workflow:
  [dough-story-decomposition](../../.agents/skills/dough-story-decomposition/SKILL.md).
