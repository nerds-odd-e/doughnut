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
- **Visible manual typing lost during processing:** Supported body editing while a real audio request was pending displayed a recognizable sentence;
  the arriving result removed it. A later paste after settlement persisted. This demonstrates loss of pending visible typing; an already-saved edit race
  was not observed. The [voice-input documentation](../../docs/voice-input.md#visible-typing-can-be-lost-while-audio-processing-is-pending) retains the input, pending-edit boundary and reload comparison.
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

<a id="preserve-completed-speech"></a>
### Keep completed speech as dictation continues

**Identity:** SEED-066#preserve-completed-speech
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/001-keep-completed-speech/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"9eb09e72a73cf3d926b575a0e816ec5f3d6beb0b2806578bc44ed9f39fdf4b79","plan":"1665d6a9c0178f525dab992786335ea1491010450b229a4b62f94974ffb0d8f8"}}
```

- **Goal:** Let a note author develop a longer spoken thought, with pauses and
  Flush, and end up with every completed sentence once and each sentence whole,
  instead of earlier facts disappearing or a sentence broken at a pause. This
  makes longer dictation trustworthy, the next step after safe additions to
  existing content.
- **Scope (required):**
  - Completed passages written during a recording stay unchanged through later
    pauses, Flush, and Stop, and appear once after reload. The preceding
    [append-only addition](../../docs/voice-input.md#adding-dictated-text-to-a-note)
    likely already provides this; this story confirms it with the orchard
    journey rather than assuming it.
  - **Hold back the unfinished tail:** an automatic pause flush or a Flush click
    writes only the speech before the last transcription segment. That
    segment's audio is kept for the next chunk, as timed mid-speech processing
    already does. Only Stop writes all remaining speech.
  - Written dictated text is never revised later. The note stays add-only, as
    the append behavior promises.
- **Decision (owner, 2026-10-03):** Hold back the unfinished sentence instead of
  showing it early and revising it. Showing then revising would bring back
  replacement and the need to track which text may still be revised.
- **Boundary assumptions:** The held-back unit is the existing last
  transcription segment. Recognizing whether a sentence is finished is not
  promised, so a finished last sentence may also wait until the next chunk or
  Stop. A pause alone never forces an unfinished sentence to be written as
  finished. Author-made edits stay under the author's control.
- **Deferred:** Faster appearance of the held-back text belongs to
  [See submitted dictation promptly](#prompt-dictation-results). Typing during
  processing belongs to
  [Keep typed corrections](#preserve-typed-corrections). Correcting individual
  words the model mishears (such as "from my sister" instead of "for my
  sister") is not promised.
- **Key examples:**
  1. **Orchard journey:** A note has an existing paragraph. Dictate the
     [recorded orchard passage](../../docs/voice-input.md#completed-dictated-content-can-disappear)
     at natural pace, with the eight-second pause after "yesterday", click
     Flush about 22 seconds in, continue, then Stop. After reload: the original
     paragraph, then the orchard facts, the complete book sentence, and the
     meeting sentences, each once. "The book that I bought yesterday" is not
     written as a separate finished sentence.
  2. **Pause in the middle of a sentence:** Saying "The book that I bought
     yesterday," then staying silent until the automatic flush writes nothing
     from that half sentence. When speech continues and the next chunk
     finishes, the sentence appears whole.
  3. **Stop writes the rest:** Stopping right after a Flush writes the held-back
     segment once. Nothing that was spoken is left out of the saved body.
- **Evidence / learning:** Before the append change, Flush replaced the
  completed orchard facts with a middle fragment, and the final save kept only
  the last sentences. The pause (a silence-threshold flush) and Flush both
  processed audio as not mid-speech, which finished the half sentence early.
- **Effort hypothesis:** M, medium confidence. Assumes that holding back the
  last segment on pause or Flush reuses the existing mid-speech path, and that
  the real-service orchard journey confirms the append change preserves
  completed passages.
- **Depends on:** Safe addition to existing content, the outcome of
  [dictated-text additions](../../docs/voice-input.md#adding-dictated-text-to-a-note).
- **Safe stopping point:** Longer dictation keeps completed thoughts and
  original content once after reload, and pauses no longer break sentences,
  regardless of later speed or UI work.

<a id="preserve-typed-corrections"></a>
### Keep typed corrections when voice results arrive

**Identity:** SEED-066#preserve-typed-corrections
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/003-keep-typed-corrections/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"9d6a5b77e4a4a33deffd3b3965227ee1e83d4637f8464b740fb50a8a9d7c8e60","plan":"15ea1a67621a1e3c9020aa9f3404c52976daedd3d469a5bc309f52a22def9111"}}
```

- **Goal:** Let a note author keep typing in a note, for example fixing a
  misheard word or adding a thought, while their speech is still being
  processed, and end up with both their typing and the dictated passage.
  Today the arriving passage replaces what the editor shows, so unsaved typing
  disappears and the caret jumps. Typing while waiting becomes safe, which is
  the next step after keeping completed speech.
- **Scope (required):**
  - **Join the typing:** when a dictated passage arrives for a note whose body
    editor is open, it is added to the end of what that editor currently
    shows, including unsaved typing, and saved with it through the ordinary
    autosave. The author is never blocked from typing while a result is
    pending or arriving.
  - Typing that is already saved, or whose save is still in flight when the
    passage arrives, is also kept. The passage follows it once.
  - **Caret stays put:** after the passage lands, the author's caret and
    selection stay where they were, so continued typing goes where the author
    was typing.
  - When no body editor for the originating note is open (the author moved to
    another note), the passage is added to the saved body as today.
  - Update the [voice-input documentation](../../docs/voice-input.md), which
    currently says the append ignores unsaved editor drafts.
- **Decision (owner, 2026-10-03):** Join the typing rather than pausing the
  editor to save the typing first and then add the passage. The pause would
  reuse the existing save-then-change pause used by note removal and image
  upload, but it would lock the editor each time a result arrives.
- **Decision (owner, 2026-10-03):** Keeping the caret where the author was
  typing is part of this story, not deferred. Without it, a correction can end
  up split across the note even though nothing is erased.
- **Boundary assumptions:** The passage always goes at the end of the body,
  after any text the author typed there, in the order things arrive. It is
  not put back where earlier dictation ended. Both body editors (rich and
  Markdown) are covered, because they share the same draft and autosave.
  Undo follows ordinary editing saves: one undo may remove typing and a
  passage that were saved together. Undoing only the passage is not promised.
- **Deferred:** The model gets its "previous content" excerpt when the request
  starts, so typing added at the end afterwards may join the passage with
  imperfect spacing. Fixing that is not promised. Several authors editing the
  same note at once is out of scope. If saving the typing fails, the
  existing editor error shows; no new recovery is added (failure handling,
  [ADR 0006](../../docs/adrs/0006-failure-handling-accepted.md)).
- **Key examples:**
  1. **Red bicycle:** A note with the two original paragraphs and the
     lighthouse addition. Start the orchard recording. While the first audio
     request is pending, type "MANUAL EDIT: Keep this red bicycle sentence."
     at the end of the body in the rich editor. When the result arrives, the
     typed sentence stays visible and the orchard passage follows it. After
     reload: both originals, the lighthouse addition, the red-bicycle
     sentence, and the orchard passage, each once.
  2. **Fix a misheard word:** An earlier dictated sentence says "a gift from
     my sister". While the next request is pending, change "from" to "for"
     and keep typing. When the passage arrives, "for my sister" stays, the new
     passage is at the end, and the caret is still right after the edit, so
     the next typed character lands there.
  3. **Edit already saved:** Type a sentence and wait until it autosaves while
     a request is still pending (or let the result arrive while that save is
     still in progress). After reload, the sentence and the passage are both
     there, the passage once, after it.
  4. **Moved to another note:** Start recording on one note, open another
     note while the request is pending. The passage is added to the
     originating note's saved body, as today, and the other note is
     unchanged.
- **Architecture:** While a body editor for the note is open, its draft is the
  one owner of that note's in-progress body. A dictated passage is applied to
  that draft as an edit and saved by its autosave, instead of being saved
  separately and pushed into the editor, which today discards the draft. The
  saved-body append stays only for a note with no open editor. This adds a
  way to hand a passage to the open editor for a given note; the existing
  per-note registration of open body editors (used by the save-then-change
  pause) is a likely place for it. No Accepted ADR is contradicted; ADR 0006
  applies to save failures.
- **Evidence:** The visible red-bicycle sentence was erased by the arriving
  result, and the final typed period landed at the start of an older
  paragraph. No earlier manual-content save was observed, so the
  already-saved race was not reproduced
  ([recorded journey](../../docs/voice-input.md#visible-typing-can-be-lost-while-audio-processing-is-pending)).
  Code reading shows the cause: the append saves store content and the
  editor then replaces its unsaved draft with the new saved body.
- **Effort hypothesis:** M, medium confidence (was L). Assumes the existing
  draft and autosave can take the passage as an ordinary edit, and that
  keeping the caret in the rich editor needs no editor redesign.
- **Depends on:** The safe existing-content addition outcome (delivered).
- **Safe stopping point:** Authors can type while waiting for speech results;
  delayed processing cannot silently erase their visible or saved corrections.

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
