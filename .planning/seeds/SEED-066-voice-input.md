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
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/001-discover-voice-input-problems/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"f5120459373a4f39c5bb88108b68cb723490e396e31b1c4143c5afa8dbe5d0be","plan":"ee2671b080a8e2de00fab4f2dc2306467e955ed298e52dde17385b7b947a62d4"}}
```

- **Goal:** Give the product owner evidence of how voice input actually
  behaves, including problems beyond those already reported, so the next
  product decisions rest on observed user journeys.
- **Outcome / evaluation:** A manual exploration report with expected versus
  actual behavior, reproduction steps, relevant timings and evidence, and
  explicit coverage gaps. Feed confirmed findings and unresolved questions into
  the epic below before its later decomposition. Keep owner reports distinct
  from newly reproduced observations and suspected causes.
- **Scope:** Explore the existing web note-authoring journey through Audio tools.
  Prioritize responsiveness, preservation of content, title behavior, and the
  discoverability and clarity of controls. Record findings in the epic's
  [manual-discovery section](#manual-discovery-findings), linking a separate
  report only when needed to retain useful evidence. Each finding distinguishes
  the source of its expectation, actual observation, reproduction steps,
  environment and service mode, and supporting evidence. An unreproduced owner
  report remains an owner report; an ambiguous expectation remains a question.
- **Coverage priority:** Observe the core journeys in the examples below first;
  deepen the most consequential surprises within the exploration budget.
  Permission, device selection, interruption, and failure feedback are secondary
  probes as the environment and remaining time allow. Report skipped journeys.
- **Creative testing:** Real speech and microphone input may be difficult for an
  agent to exercise. Try the cheapest credible observation route, such as
  prerecorded speech through a browser's test microphone or a temporary audio
  harness, alongside actual UI interaction. State which parts use real services,
  controlled audio, or simulated responses. Simulated responses cannot establish
  real transcription quality or end-to-end service latency. Record any remaining
  gap rather than claiming full manual coverage.
- **Key examples:** These describe useful discovery evidence, rather than
  acceptance of a repaired product.
  - Given a signed-in author with a disposable note, when they find Audio tools
    and try a short spoken passage, record the route, visible recording and
    processing states, resulting text, audio duration, time to first text, and
    time from stopping or submission to the final visible result. State which
    services were real and which timings were observable.
  - Given a longer passage with pauses and a sentence whose meaning becomes
    clear near its end, when intermediate and final results appear, retain
    enough before/after evidence to distinguish recent-sentence revision from
    changes to completed passages. Treat the exact acceptable revision boundary
    as unresolved, while recording any whole-body erasure or replacement.
  - Given a note with recognizable existing text, when the author dictates,
    manually edits while processing, or repeats a supported start/stop journey,
    compare the final content with those inputs and report any lost edits,
    duplication, or stale result, including results after navigating away.
    Record unsupported interactions as gaps or
    improvement opportunities, without inventing a promise to support them.
  - Given an `Untitled` note and a note with a user-chosen title, when dictation
    results arrive, record the title sequence for each. Explore explicit title
    dictation and note creation; absence of those capabilities is an improvement
    opportunity. Automatic-title policy remains a later product decision.
  - Given denied microphone permission, an interruption, or an unavailable
    service that the environment permits exercising, when dictation cannot
    proceed, record the visible feedback and recovery options. If a journey
    cannot be observed, record the blocked step and the resulting coverage gap;
    simulated transcription does not count as real-service coverage.
- **Boundaries:** Discovery and documentation only. Do not repair product code,
  make permanent test-tool changes, or decompose the epic in this item. The
  coverage suggestions are questions to explore, not additional reported bugs.
  Numeric latency targets, model selection, redesign, and choosing an automatic
  title policy are deferred to later work. Device/browser matrices and exhaustive
  failure coverage are not commitments of this bounded exploration.
- **Environment and budget:** The owner accepts local Development or the deployed
  app. Use one local browser first against the
  primary checkout's Development app at `http://127.0.0.1:5175/`, using the
  documented test sign-in `manual` / `password` and disposable notes, with a
  60-minute total exploration budget including preparation and cleanup. The
  deployed app is an acceptable alternative once its URL and access are resolved;
  this does not commit to testing both environments. Live checks on 2026-10-03
  confirmed `manual` sign-in and a real transcription-plus-retouch request in that
  app; a browser controlled-audio route remains to be established at the start
  of exploration. Real
  transcription and retouching are needed to assess actual quality and latency.
  If access is unavailable, stop the affected observation and retain a coverage
  gap instead of substituting simulated-service conclusions.
- **Depends on:** No prior product delivery. Environment and access verification
  remain preparation premises for the later exploration.
- **Safe stopping point:** The evidence remains useful even if implementation is
  deferred; leave product behavior unchanged and remove owned temporary artifacts.
- **Execution plan:** [Bounded manual exploration](../slice-plans/001-discover-voice-input-problems/PLAN.md).

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

<a id="manual-discovery-findings"></a>
#### Findings supplied by manual discovery

**2026-10-03 bounded discovery:** Chrome/macOS, `manual`, reused local Development at `localhost:5175`, disposable notebook `23`. Naturally paced
synthetic MediaStreams exercised the real recorder/worklet, transcription and retouch services. No service response or clock was simulated. The known backend
revision is `a02dbb2697…`; frontend/runtime revision was not fully established. The initial `127.0.0.1` automation-control failure has no established cause and is not evidence that a human click fails.

- **Completed dictated content lost:** In a 29.168 s passage with an 8.2 s pause, completed orchard facts appeared, Flush replaced them with a middle
  fragment, and the final update retained only the last sentences. Reload confirmed the loss while preexisting Harvard content survived. Reproduction
  and intermediate text are in the [sustained report](SEED-066-voice-input-sustained-evidence.md).
- **Visible manual typing lost during processing:** Supported body editing while a real audio request was pending displayed a recognizable sentence;
  the arriving result removed it. A later paste after settlement persisted. This demonstrates loss of pending visible typing; an already-saved edit race
  was not observed. The [baseline and preservation report](SEED-066-voice-input-manual-evidence.md) retains exact input, edit timing and reload comparison.
- **Existing paragraph truncated during navigation journey:** Navigate from source `13726` while processing to destination `13727`, then return/reload.
  Both results persisted to the source, whose first paragraph became literal `...uesday.`; the other four paragraphs and destination sentinel survived.
  Fresh independent navigation confirmed the saved state at 02:30:26 UTC. The [navigation and controls report](SEED-066-voice-input-navigation-evidence.md)
  retains before/after text. The causal role of navigation remains unproved.
- **Titles repeatedly change and overwrite author choice:** Repeating the identical 18.356 s Harvard input changed an initially `Untitled` note twice
  despite identical final bodies. A later recording overwrote “Author chosen preservation title” twice. This reproduces the owner's loss of title control;
  one-time generation versus disabling titles remains undecided. Title sequences
  are in the baseline report and [proof mapping](../slice-plans/001-discover-voice-input-problems/EXECUTION.md#automatic-title-proof-mapping).
- **Explicit voice-title entry was not discovered:** Inspecting supported title editing, Audio tools/Advanced Options and New note revealed no voice-title
  control. This is a bounded improvement opportunity, not a failed promise; OS dictation, extensions and processing instructions remain unobserved.
- **Responsiveness baseline:** First body text appeared 25.17 s after capture began, approximately 3.96 s after Stop, for the 18.356 s short input. Real
  audio requests took 4.42/4.61 s; titles settled separately. Sustained requests took 5.25/3.14/3.53 s and the body settled before Stop. Other journeys lack
  reliable Stop-to-final measurements. Timings establish neither cause nor a numeric acceptance target.
- **Scoped preservation succeeded:** Two recognizable original paragraphs and a distinct lighthouse addition survived reload and the next distinct session;
  no duplication or stale prior addition was observed in those sessions. The navigation destination survived. These comparisons do not establish a general preservation guarantee or defect frequency.

**Material gaps:** Hardware capture, permission/device behavior, interruption, service-failure feedback and recovery were not exercised; secondary failures
were skipped to reserve time for saved-loss confirmation and cleanup. The planning-stage empty-content API HTTP 500 was not reproduced through the UI.
An empty-body UI baseline did succeed. Causes, frequency, the acceptable recent revision boundary, and quantitative latency expectations remain unresolved.

**Clean stopping point:** By 02:31:09 UTC, owned notes `13726` and `13727` were verified in recoverable `_trash` folder `4965`; no purge occurred. Notebook
`23` (“Voice discovery 20261003 baseline”) remains because no recoverable notebook deletion was verified. Owned temporary WAVs/harnesses were removed, streams
released and owned tabs closed. Product source remains unchanged. Full coverage is not claimed; the three linked reports preserve actionable findings and gaps.


## Open Decisions for Later Work

- The controlled browser route is established at `localhost:5175`; genuine
  hardware capture and permission behavior remain gaps. Resolve the deployed URL
  and account only if that alternative is used.
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
