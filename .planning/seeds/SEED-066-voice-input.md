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
top of the product backlog; all three are delivered. These are non-executable story
records; capturing them does not start implementation.

On 2026-10-11 the owner added the story below at the top of the product backlog.

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

<a id="read-only-voice-input-with-insertion-feedback"></a>
### Show where voice text will arrive while keeping its input read-only

**Identity:** SEED-066#read-only-voice-input-with-insertion-feedback
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/067-read-only-voice-input-with-insertion-feedback/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"9eb5612540ab1b515ece593b747c9890c69dcc77074e2470a95e5431dccee5bc","plan":"773b5c7ff66a720c86ff8a19730052d5d969f3b563e0963130ae808356a7daa3"}}
```

**Goal:** For note authors dictating into a note body or speaking a title,
fix the place where the spoken text will land at the moment they start, and
show that place with inline pending feedback until the text has arrived, so
they watch the right spot instead of guessing where the result went.

**User and context (UX/UI examination, 2026-10-11):** The author edits an
existing note or fills in New note. They place the caret or a selection, press
the microphone button (Voice input in the toolbar for the body, Speak the
title beside the title), speak, press it again to stop, and wait. Today the
button is the only feedback. Body dictation joins the end of the body whatever
the caret, and the author may keep typing into the same editor meanwhile, so
they look for the result in the wrong place and the two kinds of input mix.
The title already takes the words at the caret or selection. Read from
[voice-input documentation](../../docs/voice-input.md) and the editor code on
2026-10-11; not observed in the browser.

**Scope:**

Required behavior:

- **Insertion point.** When voice input starts, the target is the caret or
  selection the input has, or had when focus left it; with neither, the end of
  the text. An untouched "Untitled" title in New note stays a placeholder that
  the heard words replace. Clicks and caret moves after the start do not move
  the target.
- **Body text lands at the insertion point**, no longer at the end of the
  body, in both the rich editor and the Markdown editor. The words join with
  the CJK/space rule the title uses on both sides, and a selection is replaced
  by them as in the title. Each passage of one session lands where the
  previous one ended, so mid-speech passages that arrive while recording build
  up in place. Titles still convert only at Stop.
- **Read-only.** From the start of voice input until its session ends, the
  targeted body editor or title takes no typing, pasting, or other edits;
  selecting and copying its text still work. The other field and the rest of
  the page stay as they are; body and title voice input remain independent.
- **Pending indicator.** An animated indicator sits immediately after the
  target (the caret, or the end of the selection or placeholder to be
  replaced) in the rich editor and in the title, from the start through
  recording, the conversion after Stop, and a retry conversion, until the
  session's last text has arrived. After each arriving passage it follows to
  the end of that passage. It is temporary feedback: never saved, never part of
  copied or exported text.
- **Session end.** When the last text has arrived, or nothing was heard, the
  indicator clears and editing resumes with the caret after the inserted words
  in both surfaces. When a conversion fails and the error toast shows, the
  indicator clears and editing resumes; the kept body recording stays on the
  button as today. Leaving the note or closing New note while recording ends
  the session as today.
- **Retry of a kept body recording is a session of its own.** It takes the
  target at the moment of the retry click, makes the editor read-only, and
  shows the indicator there until its text arrives or it fails again.
- **Microphone cannot start:** nothing becomes read-only and no indicator
  shows; the existing toast is the only feedback.

Removed with this story, deleted outright with a product-wide sweep and no
trace: typing into the open body editor while a passage is pending, its tests
and support helpers, and the documented end-of-body join for an open editor.

Deferred promises (not built or verified here):

- The inline indicator inside the Markdown editor, a plain text area. That
  editor is read-only and takes the words at the caret like the rich editor;
  the toolbar button stays its only pending feedback.
- Where the remainder lands when the author leaves the note while recording:
  it keeps joining the end of that note's saved body, as today, since the
  editor that held the target is gone.
- Any change to the buttons, their names, the toasts, or title generation.

Boundary assumptions:

- No cancel control exists and none is added; Stop, leaving the note, and
  closing New note remain the ways out.
- Normal content undo restores the body before a session's insertion, as today.

**Key examples:**

- Body, caret between two passages, rich editor: start Voice input → the
  editor takes no typing and an animated indicator appears between the
  passages. A mid-speech passage arrives → it appears there and the indicator
  moves after it. Stop → the remaining text follows it, the indicator clears,
  and the caret sits after the dictated text. After reload the body has the
  dictated text between the two passages.
- Body, page just opened, no caret placed: Voice input → the indicator appears
  at the end of the body and the text joins the end.
- Body, during recording the author clicks into another paragraph and types →
  nothing changes; the text still lands at the original point when it arrives.
- Title "Orchard notes" on an existing note, caret after "Orchard": Speak the
  title → the title takes no typing and the indicator sits after "Orchard".
  Stop, "harvest" heard → "Orchard harvest notes", indicator gone, caret after
  "harvest", saved through the existing title path.
- New note, untouched "Untitled": Speak the title → the indicator sits after
  "Untitled". Stop → the heard words replace "Untitled" and the indicator is
  gone.
- Stop while conversion is pending → the input stays read-only with the
  indicator until the text has arrived, then editing resumes.
- Body conversion fails at Stop → the error toast shows, the indicator clears,
  editing resumes, and the button offers retry. The author corrects a sentence
  and leaves the caret there, then clicks retry → the editor is read-only
  again with the indicator at that caret, and the kept recording's text lands
  there.
- Nothing heard → the indicator clears, nothing is inserted, no message.

**UI:** Words only; no layout or component is chosen here. The indicator marks
the exact insertion location inside the text, in the text's own style, with an
animated ellipsis as the owner's example; it stands still when the author
prefers reduced motion. The read-only input keeps its ordinary appearance; the
indicator and the active button are the only signs that voice input is
running. The indicator is decorative to assistive technology, since the
button names already announce the session's state.

- **Evaluation:** Dictate into a body (rich editor) and a title from a visible
  caret position; observe read-only editing and inline pending feedback through
  recording and processing, then confirm editing resumes with the caret after
  the words and the saved content holds the text at that place.
- **Value / learning:** Authors can anticipate where spoken text will land
  while pending voice input keeps its insertion target stable.
- **Effort hypothesis:** L (2–4 hours), medium confidence; revised from M on
  2026-10-11 because body text moves from end-of-body to caret insertion in
  two editors, three surfaces become read-only with an inline indicator in two
  of them, and the typing-while-pending behavior is removed with its tests.
  Resplit before execution if planning shows more than the L band.
- **Depends on:** none; both body and title voice-input journeys are delivered.
- **Safe stopping point:** Both current voice-input journeys provide clear
  pending feedback and restore ordinary editing independently of later audio
  improvements.

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

On 2026-10-11 the owner prioritized read-only voice input with insertion-point
feedback above the remaining queued work.

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

- Owner's UX/UI refinement request for the read-only voice input and
  insertion-feedback story, 2026-10-11, through the established Honoka-chan
  preparation; current behavior read from documentation and code.
- Owner's request for read-only body/title voice input and an animated indicator
  at the insertion point, 2026-10-11; authorized capture at the top of the backlog
  directly on main and sync with origin.

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
