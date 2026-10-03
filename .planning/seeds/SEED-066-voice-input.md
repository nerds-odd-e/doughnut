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
  and intermediate text are in the [voice-input documentation](../../docs/voice-input.md#completed-dictated-content-can-disappear).
- **Existing paragraph truncated during navigation journey:** Navigate from source `13726` while processing to destination `13727`, then return/reload.
  Both results persisted to the source, whose first paragraph became literal `...uesday.`; the other four paragraphs and destination sentinel survived.
  An independent fresh page confirmed the saved state. The [voice-input documentation](../../docs/voice-input.md#existing-content-can-be-truncated-during-a-navigation-journey) retains before/after text. The causal role of navigation remains unproved.
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

Effort bands are S = 30–60 minutes, M = 1–2 hours, and L = 2–4 hours, including
delivery, following this project's existing seed convention. The distribution
is one S, four M, and four L. These are comparative hypotheses, not a delivery
schedule. Refine or resplit any story likely to exceed L before execution.

Every story is evaluated by the note author through the product. Preservation
includes saved state after reload, rather than only a transient editor result.
The accepted order is global priority; it does not require serial technical
implementation of independent stories. No story authorizes a model choice,
technical redesign, or speculative infrastructure.

<a id="keep-every-transcribed-sentence"></a>
### Keep every transcribed sentence when dictated text is written

**Identity:** SEED-066#keep-every-transcribed-sentence
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** An author dictating a passage expects every sentence they
  finished to reach the note, not only the sentences the text-writing step
  chooses to keep.
- **Evaluation:** With real services, dictate the orchard passage from the
  [voice-input documentation](../../docs/voice-input.md#completed-dictated-content-can-disappear).
  Every transcribed completed sentence, including "These facts are finished.",
  appears once in the saved note after reload.
- **Evidence:** In the second real-service orchard run after hold-back
  (2026-10-03, note 13731), the pause flush's transcription held "These facts
  are finished.", but the dictated text returned by `transcriptionToTextAiTool`
  left it out, so it never reached the note. Two earlier runs kept it, so the
  frequency is unknown.
- **Boundary:** The step that turns a transcription into dictated text.
  Correcting misheard words and transcription quality stay out of scope.
- **Effort hypothesis:** M — low confidence; the cause and frequency are
  unknown.
- **Depends on:** None.
- **Safe stopping point:** Completed transcribed sentences are not silently
  dropped before they reach the note.

<a id="join-dictated-passages"></a>
### Join dictated passages to the note in a way that fits the language

**Identity:** SEED-066#join-dictated-passages
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** An author adding speech to a note expects each new passage
  to join the existing text the way that language is written, without fixing
  the join by hand.
- **Evaluation:** Dictate into a note whose body ends with an English sentence:
  the new passage starts after one space, not "every hour.The orchard". Dictate
  Japanese or Chinese after Japanese or Chinese text: no space is inserted
  between sentences. Successive passages in one recording join the same way.
  The saved body after reload matches.
- **Evidence:** The real-service orchard runs on 2026-10-03 (notes
  13728–13731) all saved "every hour.The orchard". The mocked recording journey
  pins the same join ("This is class 1.Let's talk about data structure
  today."). Leading whitespace is currently left to the text-writing model.
- **Boundary:** How a dictated passage is joined to the note's existing text,
  including when the note is empty. Be mindful of the language: languages
  written with spaces between words and sentences (such as English) need a
  space, while Japanese and Chinese do not, and mixed-language notes exist.
  Paragraph breaks are not decided here, and nothing already written is changed.
- **Effort hypothesis:** M — low confidence; the rule for mixed or unknown
  languages, and whether the join is decided in code or by the model, are
  refinement questions.
- **Depends on:** None.
- **Safe stopping point:** Dictated passages join existing text correctly for
  space-separated languages and for Japanese and Chinese.

<a id="typed-corrections-side-effects"></a>
### Fix two side effects of keeping typed corrections

**Identity:** SEED-066#typed-corrections-side-effects
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/007-typed-corrections-side-effects/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"113f997940f078bde472fe054f14a13ebc41f235d432f71941ea824bcf43081d","plan":"947a300771d52946319aa8c6d85619a22360858aedc969acf728b6b32dc001b5"}}
```

- **Goal:** A note author who pastes formatted text into the Markdown editor
  has the caret right after the pasted text again, and a dictated passage that
  arrives while an image upload or note removal is pausing the editor is saved
  as it was before
  keeping typed corrections (`.planning/seeds/SEED-066-voice-input.md#preserve-typed-corrections` at `34cfbd43`), instead of being
  dropped without a trace. Retrospective correction; no new feature promise.
- **Scope:** Only these two regressions from that story's delivery. The
  save-then-change overlap stays "nothing added" as that story decided: the
  passage goes to the saved body, with the same race as before.
- **Plan:** [slice plan](../slice-plans/007-typed-corrections-side-effects/PLAN.md)

<a id="recover-failed-transcription"></a>
### Recover a failed transcription without repeating the speech

**Identity:** SEED-066#recover-failed-transcription
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** Let an author recover already-spoken material after conversion
  fails rather than recreating the thought from memory.
- **Evaluation:** When conversion fails before its text is inserted, show
  understandable feedback and let the author retry the captured passage in the
  same open session. The recovered addition appears once and saved content
  remains intact.
- **Evidence / learning:** A risk-driven reliability candidate. Discovery did
  not exercise service failures or recovery; observe the actual failure journey
  during refinement before prescribing a recovery interaction.
- **Boundary:** Conversion failure before successful text insertion, within the
  same open session. Refresh/crash recovery, offline operation, and an ambiguous
  save outcome are outside this candidate's promise.
- **Effort hypothesis:** L — low confidence; assumes captured speech remains
  available for a bounded retry journey.
- **Depends on:** The safe successful-addition outcome.
- **Safe stopping point:** A failed conversion has an actionable recovery path;
  retrying neither discards the passage nor duplicates successful additions.

<a id="prompt-dictation-results"></a>
### See submitted dictation promptly

**Identity:** SEED-066#prompt-dictation-results
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** Let an author finish a short spoken thought and use its text
  without a frustrating wait after submission or Stop.
- **Evaluation:** Agree a measurable submission/Stop-to-useful-body target,
  then compare the same naturally paced recordings across repeated real-service
  runs. Useful text appears within that agreed delay while preservation remains
  intact. Separate first visible text from final settled content when needed.
- **Evidence / learning:** The first 18.356-second Harvard recording showed body
  text at 25.17 seconds from capture, approximately 3.96 seconds after Stop.
  Audio requests took 4.42/4.61 seconds and titles settled separately. Other
  journeys do not supply a reliable Stop-to-final baseline.
- **Boundary:** Own diagnosis and user-visible improvement together. The retouch
  stage and excessive thinking are owner hypotheses, not established causes.
  Preserve the requested lightweight-processing direction; choose a model or
  processing change only after understanding the path. No numerical target or
  continuous live-text promise has been decided.
- **Effort hypothesis:** L — low confidence; assumes a meaningful bounded
  improvement can meet the agreed target without a larger delivery.
- **Depends on:** No additional capability; the earlier integrity stories are
  priority choices. Speed changes must retain accepted preservation behavior.
- **Safe stopping point:** Short submitted dictation meets its measured target
  without sacrificing content or title control; later UI/title stories add
  independent value.

<a id="understandable-first-dictation"></a>
### Complete a first dictation with understandable controls

**Identity:** SEED-066#understandable-first-dictation
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** Let an author unfamiliar with Audio tools complete spoken
  capture without deciphering confusing controls.
- **Evaluation:** From an existing note, find voice input, start recording, stop,
  and recognize when the resulting text is saved. Recording, processing, and
  completion states are understandable through the product.
- **Evidence / learning:** The owner reports an ugly and unintuitive UI. The
  observed journey uses Record Audio, Flush Audio, Stop Recording, Save Audio
  Locally, and Advanced Options. Observe the journey to decide the smallest
  useful interaction change rather than assuming a full redesign.
- **Boundary:** One first-dictation journey on an existing note. Hardware capture
  and permission behavior remain discovery gaps to check during refinement;
  this does not preselect new device-management or advanced-processing features.
- **Effort hypothesis:** M — medium confidence; assumes the existing workflow
  can become understandable through a bounded interaction change.
- **Depends on:** An existing usable capture workflow; broader promotion should
  follow the earlier integrity outcomes rather than expose silent loss.
- **Safe stopping point:** A newcomer can complete and recognize a saved spoken
  addition without needing the later title-authoring capabilities.

<a id="create-with-spoken-title"></a>
### Create a note using a spoken title

**Identity:** SEED-066#create-with-spoken-title
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** Let an author capture a new note's subject by speaking its title
  during creation, an explicitly requested valuable journey.
- **Evaluation:** In New note, speak a title, review or correct it, and submit.
  The note is created once in the chosen location with that title.
- **Evidence / learning:** No explicit voice-title control was discovered in
  New note. OS dictation and extensions remain unassessed; compare their adequacy
  before investing heavily in a native control.
- **Boundary:** Explicit title input within the existing creation journey.
  Preserve ordinary title validation and location selection. This does not
  introduce voice commands for folder choice or automatic note submission.
- **Effort hypothesis:** M — medium confidence; assumes a short title capture
  can fit the existing creation workflow.
- **Depends on:** Existing note creation; existing-note voice renaming is not
  a product prerequisite. Body dictation must respect the chosen title.
- **Safe stopping point:** Authors can create a note with a reviewed spoken
  title even if existing-note title dictation is cancelled.

<a id="rename-with-spoken-title"></a>
### Change an existing note's title by speaking

**Identity:** SEED-066#rename-with-spoken-title
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** Let an author explicitly name or rename an existing note by
  speaking, with the same control they have when typing.
- **Evaluation:** Dictate a replacement title, review or correct it, then save
  through the normal rename rules. The title survives reload and the note body
  remains unchanged, including when the note has inbound references.
- **Evidence / learning:** No explicit voice-title control was discovered in
  the existing title editor or Audio tools. Compare the simpler OS-dictation
  workflow during refinement; its adequacy is unknown.
- **Boundary:** Explicit author-controlled rename, preserving ordinary title
  validation and reference-handling choices. It does not infer a title from body
  speech or change the rules for linked notes.
- **Effort hypothesis:** M — medium confidence; assumes short-title capture can
  reuse the normal title-editing journey without a larger rename redesign.
- **Depends on:** Existing title editing; spoken-title creation is independently
  useful and is not a product prerequisite.
- **Safe stopping point:** Authors can rename an existing note by speaking and
  keep body content and normal reference handling intact.

## Ordering and Scope Reduction

The owner accepted the nine-story order above on 2026-10-03. The product backlog
owns their global priority. Preservation of existing content and completed speech
comes first; title control is a small independent trust improvement. Concurrent
editing and failure recovery extend reliability before responsiveness and the
first-use journey. Title creation and renaming then extend explicit authoring.

Navigation is preservation evidence for the first two stories: source-note
content was damaged while the destination survived. Its causal role is unknown.
Recheck that journey after preservation changes and propose a separate navigation
story only if a distinct remaining problem is established. Background dictation
across notes is not a selected capability.

Keep all nine queued unless the owner later reduces scope. First-to-drop order
starts with existing-note title dictation, then spoken-title creation. Preserve
the dependable capture outcomes if those extensions are cancelled. Optional
one-time automatic title generation has no queued story.

## Open Decisions for Later Work

- The controlled browser route is established at `localhost:5175`; genuine
  hardware capture and permission behavior remain gaps. Resolve the deployed URL
  and account only if that alternative is used.
- Measurable responsiveness expectations and the actual contribution of audio
  capture, transcription, retouching, and applying results to the note.
- How to recognize the current unfinished sentence versus completed passages,
  including a long pause inside a sentence, while preserving author intent.
  Partly decided on 2026-10-03: the last transcription segment is held back
  until the next chunk or Stop, and written text is never revised. Sentence
  recognition itself remains undecided.
- The actual conversion-failure journey and the bounded same-session retry
  interaction; service failures remain unobserved.
- Any distinct navigation problem left after source-content preservation fixes.
- The adequacy of OS dictation or external transcription for explicit title
  input. Optional one-time automatic title generation stays deferred.
- The interaction for explicit title dictation, including during note creation.

## Breadcrumbs

- Owner's voice-input problem report, 2026-10-03, in this conversation.
- Owner's acceptance of all nine proposed stories and their priority, with
  authorization to record them on main and sync origin, 2026-10-03.
- Effort-band convention:
  [SEED-039](SEED-039-faster-ci-feedback.md#story-decomposition).
- Later decomposition workflow:
  [dough-story-decomposition](../../.agents/skills/dough-story-decomposition/SKILL.md).
