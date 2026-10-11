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

<a id="title-aware-voice-refinement"></a>
### Refine dictated titles with title-aware wording and punctuation

**Identity:** SEED-066#title-aware-voice-refinement
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/068-title-aware-voice-refinement/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"fd0276ec093be1ebfa76c18a46456a6cb3fdd7edbe1a2aa83db6598da7ac1d8b","plan":"5562908e8aa8c5aa2442bada25feed9a2bd2cd5e8774e63fea64f889fb79f648"}}
```

**Goal:** For authors speaking a note title, produce wording and punctuation
suited to a concise title while preserving their intended meaning and control
over the existing title.

**Scope:**

- Tell the AI explicitly that the dictated text is for a note title, including
  when it will be inserted into or replace a selected part of an existing title.
- Ask for concise title wording while preserving the speaker's meaning and
  substantive details. Concision is guidance for refining the dictated words,
  not authority to truncate the title, impose a length limit, or discard ideas.
- Ordinarily omit a sentence-ending period. Preserve appropriate punctuation,
  including a question mark when the author speaks a question as the title;
  do not apply a blanket punctuation-removal rule.
- Keep existing title text outside the selected replacement range intact.
  A caret insertion or partial selection does not authorize rewriting or
  shortening the whole title.
- Apply the title-aware instruction to supported title voice-input journeys,
  including New note. Content dictation keeps its existing transcription behavior.
  Toolbar targeting follows the current [voice-input behavior](../../docs/voice-input.md).

**Key examples:**

- Speak "Orchard harvest planning" as a whole title → a concise faithful title
  such as "Orchard harvest planning", without an added terminal period.
- Speak "Why do apples ripen unevenly?" as a title → retain the question and
  its question mark.
- Title "Spring orchard notes", select "Spring", dictate "Autumn" → refine
  the spoken replacement as title text and save "Autumn orchard notes".
- Title "Orchard notes", caret after "Orchard", dictate "harvest" → insert
  title-appropriate words to produce "Orchard harvest notes"; preserve both
  surrounding words. The spoken fragment need not become a standalone title.
- In New note, speak "Orchard harvest planning" into the initial title, then
  submit → the created note retains the refined title after reload.
- Dictate "Autumn apple harvest planning for the north orchard after the
  September frost" → retain the season, crop, location, and frost timing even
  if the resulting title is longer than a typical title.

**Investigation (2026-10-11):** Read in this story's prepared checkout at
`96e85acfcfee1ec098d20323413b67484fc37079`; no application or paid model run.

- Both [New note](../../frontend/src/components/notes/NoteNewForm.vue) and
  [existing-title editing](../../frontend/src/components/notes/core/NoteEditableTitle.vue)
  use [SpeakTitleControl](../../frontend/src/components/notes/SpeakTitleControl.vue),
  which converts at Stop. The title editor owns insertion and saving separately
  from transcription. This supports one title instruction for both journeys
  without coupling this story to toolbar targeting.
- [audioChunkToText](../../frontend/src/composables/audioChunkToText.ts) sends
  only audio and `midSpeech`; [AudioUploadDTO](../../backend/src/main/java/com/odde/donut/controllers/dto/AudioUploadDTO.java)
  carries no target-field intent. [AiAudioController](../../backend/src/main/java/com/odde/donut/controllers/AiAudioController.java)
  selects mid-speech SRT or final text transcription. [OtherAiServices](../../backend/src/main/java/com/odde/donut/services/ai/OtherAiServices.java)
  uses `gpt-4o-mini-transcribe` for final text; [OpenAiApiHandler](../../backend/src/main/java/com/odde/donut/services/openAiApis/OpenAiApiHandler.java)
  supplies no prompt. There is no separate retouch call in this path. Historical
  reports about retouch latency do not describe this checkout's implementation.
- [OpenAI's transcription guidance](https://developers.openai.com/api/docs/guides/speech-to-text#prompting),
  consulted on 2026-10-11, documents prompt support for that model, including
  punctuation and formatting guidance. This establishes a supported mechanism,
  not the quality of concise title wording on this story's speech examples.
- **Unsettled hypothesis:** Guidance in the existing transcription request can
  satisfy concision, punctuation, and detail preservation together. The cheapest
  decisive observation is a bounded comparison on the same recordings with and
  without title guidance, covering the examples above. Local response mocks can
  verify instruction routing and insertion, but cannot settle model quality.
  The current [real-service audio scenario](../../e2e_test/features/note_creation_and_update/record_live_audio_with_real_open_ai_service.feature)
  exercises content, not these title examples. A paid evaluation needs separate
  authorization; no model-quality or latency result is claimed here.

**Architecture:**

- Accepted initial direction: carry title intent through the existing audio API
  and let the backend own the title instruction, first trying the existing
  transcription request. Keep caret/selection application and ordinary title
  saving in the editor. Treat the generated words as the insertion fragment,
  not as a replacement for the entire existing title.
- A separate text-refinement request would add a sequential service operation
  and another failure point. Its necessity remains unproved; compare it only if
  the bounded evaluation shows transcription guidance insufficient. No model
  migration or numeric latency promise is part of this refinement.
- [ADR 0005 — Web routes](../../docs/adrs/0005-web-routes-accepted.md)
  keeps the HTTP contract under `/api` and described by OpenAPI; target intent
  crosses that contract and its generated client. [ADR 0006 — Failure handling](../../docs/adrs/0006-failure-handling-accepted.md)
  requires a business outcome for any new recovery: this story does not imply
  silently accepting unrefined text after a new refinement operation fails.
  Future runtime evaluation must use the isolated worktree environment under
  [ADR 0007 — Environments and isolation](../../docs/adrs/0007-environments-and-isolation-accepted.md).
  The index and these records agree on Accepted status; no conflict was found.

**Decision (2026-10-11):** The owner accepted trying guidance in the existing
transcription request first, with a bounded quality evaluation before choosing
additional processing, and requested slice planning. The paid observation is
an early probe; accepting this direction does not establish model quality.

**Plan:** [Title-aware voice refinement](../slice-plans/068-title-aware-voice-refinement/PLAN.md).

- **Evaluation:** Review saved spoken titles for ordinary phrases, questions,
  partial edits, and detailed speech; verify title-appropriate wording preserves
  meaning and the text outside the replacement range.
- **Value / learning:** Spoken titles need less punctuation cleanup while the
  author retains control of their wording and information.
- **Effort hypothesis:** S (30–60 minutes), medium confidence; verify the
  title instruction across the API and both title callers, with quality
  evaluation needed before accepting a prompt-only approach. A separate
  refinement operation would require reassessing this estimate.
- **Depends on:** none; the existing spoken-title journey can receive the
  instruction independently of toolbar targeting.
- **Safe stopping point:** Title-aware refinement improves existing title
  voice input independently of which control starts recording.

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

On 2026-10-11 the owner queued title-aware voice refinement. The product
backlog owns its priority; no blocking dependency is recorded.

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
