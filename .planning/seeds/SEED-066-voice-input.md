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

Effort bands are S = 30–60 minutes, M = 1–2 hours, and L = 2–4 hours, including
delivery, following this project's existing seed convention. The distribution
is one S, four M, and four L. These are comparative hypotheses, not a delivery
schedule. Refine or resplit any story likely to exceed L before execution.

Every story is evaluated by the note author through the product. Preservation
includes saved state after reload, rather than only a transient editor result.
The accepted order is global priority; it does not require serial technical
implementation of independent stories. No story authorizes a model choice,
technical redesign, or speculative infrastructure.

<a id="rename-with-spoken-title"></a>
### Change an existing note's title by speaking

**Identity:** SEED-066#rename-with-spoken-title
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"unselected","assessment":"not-ready","reasons":["No approach selected yet: the story needs slice planning before execution."],"basis":{"document":"6b402909eca610dd07f5e08d00bf12fc10b4c16609091d4e97a6601387815b51"}}
```

**Goal:** A note author who can edit a note renames it by speaking instead of
typing, reviews or corrects the heard title, and it is saved by the same rules
as a typed title: the note body stays as it was, and a note that other notes
link to still asks how those links should change. This completes the
"dictating titles" part of voice input after
[Create a note using a spoken title](../../docs/voice-input.md#speaking-a-title-in-new-note)
landed; it does not infer a title from body speech.

**The user:** The same note author as the earlier voice stories, now on an
existing note, perhaps on a phone, who finds its title wrong or missing and
would rather say the new one than type it. A title is a few words, so they
expect to speak once and see the whole new title.

**What the author meets today (read from the code, 2026-10-07):** The note
page shows the title as a heading the author edits in place when they may edit
the note; readers who may not edit see plain text. A typed title is saved after
a one-second pause or when the author leaves the field; Enter does nothing; a
blank title is ignored; illegal path characters are replaced and warnings are
shown. When other notes link to the note, typing instead shows a panel, "This
note is linked from other notes. Choose how wiki links to this note should
change:", with "Update visible reference text" and "Keep visible reference
text"; nothing is saved until one is chosen, and leaving the title area
discards the draft. Audio tools on the toolbar writes only to the body, and
nothing on the note page offers to listen for a title.

**Native control, as in New note:** The seed asked whether operating-system
dictation is adequate for an existing note's title. The comparison recorded
for New note applies unchanged: an author with OS dictation turned on can
already speak into the title heading, but it is absent on many machines and
browsers, is a second dictation with its own language setting beside Donut's
own, and the product can neither name it nor help when it is missing. This
refinement applies the owner's 2026-10-06 choice for New note to the existing
note as well, so one voice journey with one set of words covers creating,
renaming, and the body. That extension is this refinement's proposal, not a
new owner decision; the owner can overrule it before planning.

**Scope:**

- **Required — speak the title on the note page:** When the author may edit
  the note, the title offers one control named in words, "Speak the title",
  the same control New note has. The author chooses it, allows the
  microphone, speaks, and chooses Stop. The heard words arrive once, after
  Stop, as one line, exactly as the transcription heard them, segments joined
  as body passages are joined. Nothing appears while the author is still
  speaking. Readers who may not edit the note are not offered the control.
- **Required — the heard words replace the title:** Renaming means a
  replacement, so the heard words become the whole title, whatever the field
  held at Stop, including a title the author had started typing. They do not
  join the old title: an existing title is the author's own, not an untouched
  default, and "Old title New title" is never the rename they meant. The
  author corrects the heard title by typing, or speaks again, which replaces
  it again.
- **Required — a spoken title is an ordinary title:** It goes through the
  same illegal character replacement, warnings, blank-title refusal, and
  saving as a typed title. For a note no other note links to, it is saved as
  a typed title is, after the pause or on leaving the field, and the sidebar
  and page show it as after typing. For a note other notes link to, the
  heard title shows with the same reference panel, the author chooses how
  the links change, and the rename and links are saved together; moving away
  without choosing discards the heard title exactly as it discards a typed
  one. After reload the saved title is what the author reviewed, and the body
  is unchanged.
- **Required — the state in words:** The control becomes Stop while
  listening, and a status that assistive technology announces reads
  "Recording. Speak now.", then "Turning your speech into text…" until the
  words are in the title; once they are, the title itself is the result and
  there is no status. When nothing was heard, it says "No speech was turned
  into text." and the title is unchanged. Wording is the New note wording.
- **Required — failure leaves the title alone:** When the microphone cannot
  be used, "Could not use the microphone. Allow microphone access in your
  browser, then try again." is shown as a problem and "Speak the title"
  stays available. When the speech could not be turned into text, "Could not
  turn your speech into text." is shown as a problem, the title is unchanged,
  nothing is saved, and speaking again sends only the new recording. There
  is no Retry control; speaking again is the retry.
- **Keep:** Every title-editing behavior above that is not about speech is
  unchanged, including undo of "edit title". Body dictation with Audio tools
  still writes only to the body and never changes the title; speaking a
  title never changes the body. New note's "Speak the title" is unchanged.
- **Deferred, not promised:** Speaking a title from the sidebar, for a
  folder, or in the book-reading block dialog; speaking while Audio tools is
  recording the body (one recorder at a time is enough; what happens when
  both are started is not promised either way); listening ending by itself
  on silence; tidying the heard words, such as a closing full stop; choosing
  the reference handling by voice; a microphone chooser or Write text now
  for the title; keeping a failed recording for Retry; and observing
  operating-system dictation or hardware capture, which stay gaps.
- **No other rejection constraints:** Nothing else makes the product refuse
  a rename the title editor accepts today.

**Key examples:**

1. **Rename a note nothing links to:** On the note "Orchard notes", which no
   other note links to, the author chooses "Speak the title", allows the
   microphone, says "Apple orchard care", and chooses Stop. The status says
   the speech is being turned into text; then the title reads "Apple orchard
   care" and is saved as a typed title is. The sidebar shows "Apple orchard
   care"; after reload the title is "Apple orchard care" and the body is as
   before.
2. **Correct after speaking:** The title reads "Apple orchid care". The
   author fixes "orchid" by typing; the corrected title is saved.
3. **Speak again replaces:** The title reads "Apple orchard care". The author
   chooses "Speak the title" again, says "Pear orchard care", and chooses
   Stop. The title reads "Pear orchard care" only.
4. **Rename a linked note:** The note "WikiLinks CI" is linked from
   "WikiLinks Tech" as `[[WikiLinks CI]]`. The author speaks "WikiLinks CI
   Renamed" and chooses Stop. The title reads "WikiLinks CI Renamed" and the
   panel asks how wiki links to this note should change. The author chooses
   "Keep visible reference text". The title is saved; "WikiLinks Tech" still
   shows the link text "WikiLinks CI", which opens "WikiLinks CI Renamed".
5. **Walk away from a linked rename:** As in example 4, but the author
   clicks into the body without choosing. The heard title is discarded and
   the title still reads "WikiLinks CI", as after an abandoned typed rename.
6. **Nothing heard:** The author chooses "Speak the title", says nothing,
   and chooses Stop. The status says no speech was turned into text; the
   title still reads "Orchard notes"; nothing is saved.
7. **Failure, then success:** The transcription fails at Stop. The message
   says the speech could not be turned into text; the title is unchanged.
   The author chooses "Speak the title" once more and says "Lighthouse
   keepers". The title reads "Lighthouse keepers" only.
8. **Reader without edit rights:** Someone viewing a note they may not edit
   sees the title as text and no "Speak the title".
9. **Body stays separate:** After renaming to "Lighthouse keepers", the
   author dictates a paragraph with Audio tools; it goes to the body and the
   title stays "Lighthouse keepers".

**UI:** Words only; layout and exact wording are for execution and the
owner's review. "Speak the title" sits with the title heading on the note
page, where the heard title, its warnings, and the reference panel are
reviewed and saved; placing it inside Audio tools was considered and set
aside because it would separate the result from where it is reviewed. It
becomes "Stop" while listening. Status wording is New note's, listed in the
[voice-input documentation](../../docs/voice-input.md#speaking-a-title-in-new-note).

- **Evidence / learning:** New note's "Speak the title" control already
  listens, converts only at Stop, and hands over the heard segments; the
  existing title editor accepts a proposed value through the same path typing
  uses, which shows the reference panel when needed (code, 2026-10-07). The
  linked-rename journey, including discarding on leaving, is exercised by the
  existing wiki-link E2E scenarios, and New note's spoken title is exercised
  by the record-live-audio scenarios, so both journeys have test support to
  extend. Operating-system dictation in Donut remains unobserved.
- **Effort hypothesis:** M — good confidence; assumes the New note control
  can be placed with the existing title editor and feed it a replacement
  value without a backend change or a rename redesign. The one point to
  observe early is that choosing Stop inside the title area does not count as
  leaving it for a linked note, so the heard title is not discarded before
  the reference panel can be used.
- **Depends on:** Existing title editing and the landed New note spoken
  title. Not a product prerequisite for any other story.
- **Safe stopping point:** Authors can rename an existing note by speaking,
  with body content and reference handling intact, even if nothing else
  about voice input changes.

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

Keep remaining queued voice stories unless the owner later reduces scope.
First-to-drop among remaining extensions starts with existing-note title
dictation. Preserve the dependable capture outcomes if that extension is
cancelled. Optional one-time automatic title generation has no queued story.

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
  [Change an existing note's title by speaking](#rename-with-spoken-title)
  applies the same choice as a refinement proposal the owner can overrule.
  Optional one-time automatic title generation stays deferred.

## Breadcrumbs

- Owner's voice-input problem report, 2026-10-03, in this conversation.
- Owner's acceptance of all nine proposed stories and their priority, with
  authorization to record them on main and sync origin, 2026-10-03.
- Effort-band convention:
  [SEED-039](SEED-039-faster-ci-feedback.md#story-decomposition).
- Later decomposition workflow:
  [dough-story-decomposition](../../.agents/skills/dough-story-decomposition/SKILL.md).
