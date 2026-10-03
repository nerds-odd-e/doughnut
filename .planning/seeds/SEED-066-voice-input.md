---
id: SEED-066
status: dormant
planted: 2026-10-03
planted_during: owner shift of near-future direction from maintenance and bug fixing to audio tools, starting with voice input
trigger_when: selecting voice-input discovery, then returning to decompose the improvement epic with the discovery evidence
scope: large
---

# SEED-066: Make voice input fast, reliable, and easy to use

## Why This Matters

For Donut users who want to capture notes by speaking, the existing voice-input
feature should become responsive, trustworthy, and easy to discover and operate.
The owner reports that it has existed for a long time, is rarely used, and is
extremely buggy. Audio tools, beginning with usable voice input, are now the
[near-future direction](../PRODUCT-BACKLOG.md#near-future-direction).

## Owner's Requested Order

Queue the two items below in this order. First discover more problems through
creative manual testing and add the evidence to the epic. Keep the second item
as one broad epic documenting the whole problem; return to it for decomposition
later. Capturing these items does not start testing, implementation, or
decomposition, and does not prescribe an implementation sequence within the epic.

## Backlog Items

<a id="discover-voice-input-problems"></a>
### Discover voice-input problems through manual testing

**Identity:** SEED-066#discover-voice-input-problems
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** Give the product owner evidence of how voice input actually
  behaves, including problems beyond those already reported, so the next
  product decisions rest on observed user journeys.
- **Outcome / evaluation:** A manual exploration report with expected versus
  actual behavior, reproduction steps, relevant timings and evidence, and
  explicit coverage gaps. Feed confirmed findings and unresolved questions into
  the epic below before its later decomposition. Keep owner reports distinct
  from newly reproduced observations and suspected causes.
- **Coverage to consider when selecting the mission:**
  - Find Audio tools and start dictating into a note; observe whether controls,
    recording state, processing state, and resulting content are understandable.
  - Measure time to first visible text, subsequent updates, and especially the
    time from submission or stopping to seeing the resulting note content.
    Record the audio duration and journey used for each measurement.
  - Dictate short and long passages, pauses, and sentences whose meaning becomes
    clear only near the end. Observe which recent words are revised and whether
    completed passages or the whole body are rewritten or erased.
  - Try a note with existing content, manual edits during processing, and repeated
    start/stop or submission where supported; look for lost text, duplication,
    stale results, and results arriving after the user has moved on.
  - Observe title changes with an `Untitled` title and with a title chosen by the
    user. Explore whether voice input is available and usable for title editing
    and while creating a note; record absent capabilities as improvements rather
    than inventing existing promises.
  - Explore microphone permission, device selection, interruption, and failure
    feedback as the available environment and agreed mission budget allow.
- **Creative testing:** Real speech and microphone input may be difficult for an
  agent to exercise. Try the cheapest credible observation route, such as
  prerecorded speech through a browser's test microphone or a temporary audio
  harness, alongside actual UI interaction. State which parts use real services,
  controlled audio, or simulated responses. Simulated responses cannot establish
  real transcription quality or end-to-end service latency. Record any remaining
  gap rather than claiming full manual coverage.
- **Boundaries:** Discovery and documentation only. Do not repair product code,
  make permanent test-tool changes, or decompose the epic in this item. The
  coverage suggestions are questions to explore, not additional reported bugs.
- **Depends on:** None. Resolve the supported environment, access, and a bounded
  exploration budget when this item is selected.
- **Safe stopping point:** The evidence remains useful even if implementation is
  deferred; leave product behavior unchanged and remove owned temporary artifacts.

<a id="usable-voice-input"></a>
### Make voice input fast, reliable, and easy to use

**Identity:** SEED-066#usable-voice-input
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** Let note authors capture their thoughts by voice without long
  waits, lost content, distracting title changes, or confusing controls.
- **Whole-epic outcome:** Voice input feels responsive, preserves the author's
  content and intent, and supports an intuitive note-authoring journey, including
  dictating titles and creating a note by speaking its title. The owner can
  evaluate the resulting experience by dictating and reviewing notes.
- **Depends on:** The manual-discovery item above supplies additional evidence
  before this epic is decomposed. The owner's report is captured now and does
  not depend on reproduction to remain recorded.
- **Effort / preparation:** Broad epic; sizing, story decomposition, refinement,
  and execution planning are deferred until the discovery evidence is available.

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

- A title is automatically generated from the dictated content, and its ongoing
  changes are annoying. Continuous automatic title generation or updating should
  stop.
- Disabling automatic titles is acceptable. The owner also accepts an alternative:
  only if the title starts with `Untitled`, wait until some amount of dictation
  has accumulated, choose a title once for that note, and never automatically
  update it again, including in later dictation sessions.
- The owner has not prescribed which of those choices must come first. Whether
  to retain the one-time option and its delay remain decisions for later
  refinement.
- Voice input should also be useful for explicitly dictating the title. In
  particular, being able to dictate the note title while creating a note would
  be valuable. Explicitly dictated or otherwise user-chosen titles must remain
  under the user's control.

#### Usability

- The owner describes the current UI as ugly and unintuitive.
- Make the voice-input journey easy to find, understand, and use. Specific UI
  changes should follow observation of the current experience rather than an
  assumed redesign.

#### Findings supplied by manual discovery

No manual exploration has been performed for these queued items yet. Add the
first item's observed findings, evidence, unresolved expectations, and material
coverage gaps here before decomposition, with links to any separate report.
Do not replace the owner's original report with unverified causal assumptions.

## Open Decisions for Later Work

- The manual-testing environment, access, supported surfaces, and time budget.
- Measurable responsiveness expectations and the actual contribution of audio
  capture, transcription, retouching, and applying results to the note.
- The boundary between useful recent-sentence revision and destructive rewriting.
- No automatic title versus a one-time title for an `Untitled` note, and the
  timing of that one-time behavior.
- The interaction for explicit title dictation, including during note creation.

## Breadcrumbs

- Owner's voice-input problem report and explicit two-item backlog order,
  2026-10-03, in this conversation.
- Manual exploration workflow:
  [dough-manual-testing](../../.agents/skills/dough-manual-testing/SKILL.md).
- Later decomposition workflow:
  [dough-story-decomposition](../../.agents/skills/dough-story-decomposition/SKILL.md).
