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
  and intermediate text are in the [voice-input documentation](../../docs/voice-input.md#dictating-a-passage-with-a-pause-and-flush).
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

<a id="join-dictated-passages"></a>
### Join dictated passages to the note in a way that fits the language

**Identity:** SEED-066#join-dictated-passages
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/001-join-dictated-passages/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"e4d8759845dfbffb2508a9f7e8e4c1d5da7ab4368cbc7a2c5caacdf353ed3764","plan":"2baa03fbcecd5422b8e79f57e930b2bab358e0c92a5313a2e0bb74ff5e5999de"}}
```

- **Goal:** An author adding speech to a note gets text joined the way its
  language is written, so no join needs fixing by hand. English already joins
  with one space; this story makes Japanese and Chinese dictation usable, one
  step toward dependable everyday dictation.
- **Scope:**
  - One join rule decides, in code, every place dictated text meets other
    text: a passage joining the note's existing text (the saved body or an
    open editor's draft), successive passages of one recording, and the
    transcription segments inside one passage.
  - Kept as today: nothing is added to an empty body or after text that ends
    in whitespace, and two texts written with spaces (such as English) are
    joined by one space.
  - New: no space is added when the join touches Japanese or Chinese writing,
    meaning a kanji/hanzi, hiragana, or katakana character, or full-width
    punctuation such as `。`, `、`, `！`, `？`, `「`, `」`.
  - A mixed join looks at both sides: a space is added only when neither the
    character before nor the character after the join is Japanese or Chinese
    writing (owner decision, 2026-10-04).
  - Other scripts need no separate handling: they are joined with one space, as
    today. Korean is written with spaces and so fits that naturally.
  - Deferred: paragraph breaks, spacing inside a transcription segment (the
    transcription's own text is written as is), and changing characters that
    are already written. Language detection of the note or of the speech is
    not needed and not added.
  - Assumption, not observed: the transcription service returns Japanese and
    Chinese segments without spaces of its own. It has only been observed with
    English, and this story does not depend on it.
- **Key examples:**
  - Body `The bell rings every hour.`, dictated `The orchard is old.` →
    `The bell rings every hour. The orchard is old.` (unchanged behavior).
  - Body `鐘は毎時間鳴ります。`, dictated `果樹園は古いです。` →
    `鐘は毎時間鳴ります。果樹園は古いです。`; the saved body after reload
    matches.
  - Body `钟每小时响一次。`, dictated `果园很古老。` →
    `钟每小时响一次。果园很古老。`
  - One recording whose transcription holds the segments `果樹園は古いです。`
    and `ベンチがあります。` in one passage → `果樹園は古いです。ベンチがあります。`,
    and a later passage of the same recording joins the same way.
  - Empty body, dictated `果樹園は古いです。` → the body is exactly the passage.
  - Mixed: body `私はPython`, dictated `が好きです。` →
    `私はPythonが好きです。`; body `鐘は毎時間鳴ります。`, dictated
    `The orchard is old.` → `鐘は毎時間鳴ります。The orchard is old.`; body
    `The bell rings every hour.`, dictated `果樹園は古いです。` →
    `The bell rings every hour.果樹園は古いです。`
- **Evidence:** The real-service orchard runs on 2026-10-03 (notes
  13728–13731) all saved "every hour.The orchard". The mocked recording journey
  pinned the same join. The client now joins every passage with one space
  (none on an empty body or after trailing whitespace), and a passage's
  segments are joined by one space, so English already joins correctly;
  Japanese, Chinese, and mixed-language joining remain.
- **Effort hypothesis:** S to M — medium confidence.
- **Plan:** [001-join-dictated-passages](../slice-plans/001-join-dictated-passages/PLAN.md)
- **Depends on:** None.
- **Safe stopping point:** Dictated passages join existing text correctly for
  space-separated languages and for Japanese and Chinese.

<a id="recover-failed-transcription"></a>
### Recover a failed transcription without repeating the speech

**Identity:** SEED-066#recover-failed-transcription
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/005-recover-failed-transcription/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"a41aa74ac9d575f7d547e224823d8778756b48b427b839e95312872a2dce3048","plan":"d6930319b7aadbd672e3996e0c3c7e7805a1ac4e36294a766ce60603e7d66772"}}
```

- **Goal:** An author whose speech could not be turned into text keeps that
  speech and gets its text without saying it again. Today a failed conversion
  silently drops the passage, so the author must notice the gap and recreate
  the thought from memory. This makes dictation dependable when the
  transcription service or the network fails for a moment.
- **Scope:**
  - A conversion that fails keeps its audio: that audio counts as not yet
    converted, exactly like speech that has not been sent.
  - While recording, a failure does not stop the recording. The kept audio is
    sent again with the next conversion, whichever comes first: the timed
    one, a pause, a Flush click, or Stop. The author needs no new control
    here; Flush already asks for a conversion now.
  - After Stop, when audio is still not converted, Audio tools offers Retry.
    Retry converts everything that is still not converted and writes all of
    it, the same way Stop does.
  - The recovered text is joined to the note once, in the order it was
    spoken, by the same rule as any dictated passage. Text already written
    is not written again, and the note's saved content is unchanged by a
    failure.
  - Feedback: a failure shows a plain sentence that says the speech was not
    turned into text and that the recording is kept. It replaces today's raw
    `Error: Failed to process audio`. The message goes away when a later
    conversion succeeds, and stays, with Retry after Stop, while conversion
    keeps failing.
  - Follows from the rule, not a separate feature: starting a new recording
    instead of pressing Retry does not drop the kept audio; it is converted
    together with the new recording's first conversion.
  - "Failure" here means the conversion request did not return a
    transcription: an error answer from the server or no answer at all.
  - Unchanged: Save Audio Locally still gives the whole recording after Stop.
  - Deferred: recovery after a reload, a crash, closing Audio tools, or
    leaving the page; working offline; automatic retries on a timer; a
    failure while saving the note after conversion succeeded, including a
    save whose outcome is unknown; different messages for different causes;
    and any wider change to the Audio tools controls, which
    [Complete a first dictation with understandable controls](#understandable-first-dictation)
    owns.
  - Assumption, not observed: the kept audio stays small enough to send in
    one request. It grows by about 2 MB a minute (16 kHz mono WAV) and the
    server accepts 100 MB, but the transcription service's own limit was not
    observed. A long outage during a long recording is not covered.
  - The interaction above was chosen during refinement on 2026-10-05 from the
    observed failure journey; the owner has not yet reviewed it.
- **Key examples:** Each uses a note with the body `This is class 1.` and
  speech whose transcription is `its talk about dada struct day.`
  - Failure at Stop, then recovery: the author records and presses Stop, and
    the conversion fails → the body is still `This is class 1.`, the message
    says the speech was not turned into text and the recording is kept, and
    Retry is offered. The service works again and the author presses Retry →
    the body is `This is class 1. its talk about dada struct day.`, the
    message and Retry are gone, and the saved body after reload matches.
  - Retry fails again: the author presses Retry while the service is still
    failing → the body is unchanged and the message and Retry remain. A later
    Retry that succeeds adds the passage once.
  - Failure while recording: a conversion fails during recording → recording
    continues and the message is shown. The author keeps speaking, and the
    next conversion succeeds → the speech from before the failure and the
    speech after it are both in the note, once each, in spoken order, and
    the message is gone.
  - No duplicate: one passage was already written to the note, then a later
    conversion fails and is recovered → the earlier passage appears once, not
    twice.
- **UI:** The message sits where the Audio tools error is shown today, styled
  as an error rather than as information. Proposed wording: "Could not turn
  your speech into text. Your recording is kept." Retry is one more control
  in Audio tools, shown only after Stop while audio is still not converted.
- **Evidence:** Observed on 2026-10-05 at revision `9e74df4a28` with a
  throwaway mounted test and the real audio buffer; a real service failure
  through the product was not observed.
  - A failed conversion showed `Error: Failed to process audio` in an
    information-styled box, with no toast. Nothing clears it until the next
    Record click.
  - The buffer treated the failed audio as converted: after a failed
    one-second chunk, nothing was left to convert, and the next conversion
    sent only the next second. The failed passage therefore never reaches the
    note, and no control sends it again.
  - The whole recording stays in memory while Audio tools is open, which is
    why Save Audio Locally works after Stop. The premise that the captured
    speech is still available for a retry holds.
- **Effort hypothesis:** M — medium confidence.
- **Plan:** [005-recover-failed-transcription](../slice-plans/005-recover-failed-transcription/PLAN.md)
- **Depends on:** None.
- **Safe stopping point:** A failed conversion keeps its speech and has a
  recovery path; recovering neither drops the passage nor repeats text that
  was already written.

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
- **Boundary:** Own diagnosis and user-visible improvement together. The
  recorded timings predate deleting the model rewrite of transcriptions, so
  re-measure the baseline first; excessive thinking was an owner hypothesis,
  not an established cause.
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
  capture, transcription, and applying results to the note.
- How to recognize the current unfinished sentence versus completed passages,
  including a long pause inside a sentence, while preserving author intent.
  Partly decided on 2026-10-03: the last transcription segment is held back
  until the next chunk or Stop, and written text is never revised. Sentence
  recognition itself remains undecided.
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
