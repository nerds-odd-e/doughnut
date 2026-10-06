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

<a id="prompt-dictation-results"></a>
### See submitted dictation promptly

**Identity:** SEED-066#prompt-dictation-results
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/007-see-submitted-dictation-promptly/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"231fb644ba4316fa83c6a2bd0b106510ac31950c9fa1e1843e4b61f59faea6cf","plan":"e61c58564fa8d06db9e924dc24f628be80335a8c62848fc7447ef456eaf4f433"}}
```

**Goal:** A note author who has finished a short spoken thought sees its text
in the note body soon after Stop, and can read it and go on working without a
frustrating wait. This is one part of voice input feeling responsive. The
story's outcome is the measured wait after Stop for a short dictation; text
that appears continuously while speaking is the wider ambition, not this story.

**Scope:**

- **Required:** For one short recording on an existing note, the wait from
  clicking Stop until the complete dictated passage is visible in the body
  meets the target the owner accepted on 2026-10-05: across five runs the
  middle wait is at most 2 seconds and none exceeds 3 seconds, measured on
  local Development with the real transcription service. "Short" is up to
  about 20 seconds of speech with no pause long enough to start a conversion.
- **Measure first:** The recorded timings predate deleting the model rewrite
  of transcriptions, so they are not the current baseline. Repeat the same
  naturally paced recordings (the 18.356 s Harvard passage and the 6.2827 s
  lighthouse addition) on local Development with the real transcription
  service, and record where the wait goes: waiting for a conversion already
  running, uploading the audio, the transcription request, and saving the
  note. Choose the change from that measurement. Excessive thinking was an
  owner hypothesis about the deleted rewrite, not an established cause.
- **Already fast enough:** If the fresh measurement meets the target, the
  story makes no product change; it records the measurement in the
  [voice-input documentation](../../docs/voice-input.md#responsiveness-and-positive-comparisons)
  and ends.
- **Not yet known to be reachable:** No current baseline exists, so the
  target may prove out of reach.
- **Cannot be reached:** If no bounded change meets the target, stop and
  report the measured breakdown. The owner then decides between a different
  target and a larger delivery.
- **Keep:** Every preservation behavior in the voice-input documentation: the
  written text is the transcription's own, joined once, the unfinished sentence
  is held back during speech, written text is never revised, a failed
  conversion keeps its audio and offers Retry, and titles are not changed.
- **Processing direction:** Keep processing light. This story adds no model
  step between the transcription and the note. A different transcription model
  or setting is allowed only when the measurement shows the transcription
  request is where the wait goes.
- **Deferred, not promised:** Text appearing while the author is still
  speaking, which the owner decided on 2026-10-05 to leave out of this story
  and to queue separately only if wanted after trying it; the wait after a pause or Flush; dictations
  longer than the agreed short length; production network conditions; and
  hardware microphone capture. Changes that naturally make these faster are
  welcome but are not measured here.

**Current path (read from the code, 2026-10-05):** After Stop the client waits
for any conversion already running, then sends all remaining audio in one
request as an uncompressed 16 kHz mono WAV file. The server asks `whisper-1`
for an SRT transcription and returns its segments; the client joins them to
the body and saves once. During speech a conversion starts only every 60
seconds, after more than 3 seconds of silence, or on Flush. A dictation
shorter than 60 seconds without a pause is therefore converted entirely after
Stop. This describes the path; it does not establish where the wait goes.

**Key examples:**

1. **Short thought:** A note has one saved paragraph. The author records the
   18.356 s Harvard passage and clicks Stop. The complete passage is visible
   in the body within 2 seconds. After reload the original paragraph and the passage
   each appear once.
2. **Very short addition:** The author records the 6.2827 s lighthouse
   addition and clicks Stop. The addition is visible within 2 seconds.
3. **Repeated runs:** The same Harvard recording is run five times against the
   real transcription service. The middle wait of the five is at most 2 seconds
   and none exceeds 3 seconds.
4. **Preservation boundary:** The 29.168 s orchard passage with its
   eight-second pause and a Flush still writes the three passages recorded in
   the voice-input documentation: nothing lost, repeated or revised.
5. **Failure boundary:** The transcription fails at Stop. The body is
   unchanged, the message and Retry appear, and Retry joins the passage once,
   as before.

- **Effort hypothesis:** L — low confidence; assumes a meaningful bounded
  improvement can meet the agreed target without a larger delivery.
- **Depends on:** No additional capability. Speed changes must retain accepted
  preservation behavior.
- **Safe stopping point:** Short submitted dictation meets its measured target
  without sacrificing content or title control; later UI/title stories add
  independent value.

<a id="understandable-first-dictation"></a>
### Complete a first dictation with understandable controls

**Identity:** SEED-066#understandable-first-dictation
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/008-complete-a-first-dictation-with-understandable-controls/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"3732eb8dc00ff931ac5ad1842ba8ddf760b84ef4faaa7cb75c1adf454b3aa671","plan":"d86affe093e4d96c62ec2a1151515c5dbee949ccfb8f17e08e8ea275f203fb59"}}
```

**Goal:** A note author who has never used Audio tools adds a spoken passage
to an existing note without having to work out what the controls mean. At
each moment the product tells them in words whether it is listening, working
on their speech, or finished, and whether their text is in the note. This is
the "easy to use" part of voice input. The story's outcome is one first
dictation that the author can complete and trust; a wider redesign of Audio
tools is the broader ambition, not this story.

**The user:** Someone who writes notes in Donut and wants to speak a thought
instead of typing it. They have not seen Audio tools before, may be on a
phone or tablet where a pointer cannot hover, and do not know the words
"flush", "conversion" or "transcription".

**What the author meets today (read from the code, 2026-10-06):** Audio tools
is a note toolbar action; in a narrow toolbar it moves into "more options".
It opens a panel with a sound-level strip and five round buttons that show
only a picture: a microphone (Record Audio), a tick (Flush Audio), a square
(Stop Recording), a download arrow (Save Audio Locally) and a gear (Advanced
Options). Each name appears only when a pointer rests on the button, so a
touch screen never shows it. The tick looks like "done" but means "turn what
I have said so far into text now". While recording, a microphone chooser
takes the place of the microphone button, and the only sign of recording is
the moving sound-level strip. After Stop nothing says that the speech is
being turned into text, and nothing says when the text has been saved; the
text simply appears in the body some seconds later. The end-to-end test
waits for Save Audio Locally to become available because the product gives
no other sign of completion. Every failure to start, including a refused
microphone permission, shows "Failed to start recording" in the style of an
ordinary notice. The gear reveals one more button, which opens a black full
screen.

**Scope:**

- **Required — one clear next step:** In each state the panel offers one
  main action with its name shown in words: Record when nothing is being
  recorded, Stop while recording. The author never has to choose among
  several unlabeled buttons to start or finish.
- **Required — the state in words:** A status the author can read, and that
  assistive technology announces when it changes, says which state the
  dictation is in: ready to record, recording, turning speech into text, and
  the result after Stop. Colour or movement alone never carries the state.
- **Required — a truthful result:** After Stop, the status says the text was
  added to the note only when a passage from this recording was written and
  its save succeeded. When nothing was written, it says that no speech was
  turned into text and does not claim an addition.
- **Required — recording comes first:** Text written while the author is
  still speaking does not change the status to finished. The status stays
  on recording until Stop.
- **Required — a refused or missing microphone:** When recording cannot
  start, the message says the microphone could not be used and that the
  author should allow microphone access in the browser. It is shown as a
  problem, not as an ordinary notice, and Record stays available. One
  message covers every reason; the product does not tell them apart.
- **Required — Retry's place:** After Stop with speech still not turned into
  text, the failure message and Retry appear together as the suggested next
  step, where the result would have been. The message then says the
  recording is kept until Audio tools is closed. Record stays available; a new
  recording still carries the kept audio, as today.
- **Required — other controls step back:** Save Audio Locally, full screen,
  the microphone chooser and the mid-speech "write now" control keep working
  as they do today, each with a name shown in words, and none of them takes
  the place of the main action.
- **Keep:** Every preservation, hold-back, failure and Retry behavior in the
  [voice-input documentation](../../docs/voice-input.md), the measured wait
  from [See submitted dictation promptly](#prompt-dictation-results), and
  the way Audio tools is opened from the note toolbar.
- **Deferred, not promised:** A new look for the whole panel; moving voice
  input out of the note toolbar; choosing or remembering a microphone before
  recording; telling a refused permission from a missing microphone; keeping
  an unconverted recording after Audio tools closes or the page reloads; a
  sign of progress for each mid-speech passage; feedback after the author
  closes Audio tools or leaves the note while recording; the wording of a
  failed note save; and observing real hardware capture, which stays a gap.
- **No rejection constraints:** Nothing here makes the product refuse an
  action it accepts today.

**Borrowed mechanism — the card payment terminal:** A terminal runs a hidden
process for someone who uses it rarely. It works because one status line
always says the current stage and what to do ("Insert card", "Processing, do
not remove card", "Approved"), only the action that is valid now is offered,
and a failure says what happened to the person's money ("Declined, you were
not charged"). Mapped to this story: the card holder is the author; the
stage line is the status; "Processing" is turning speech into text;
"Approved" is the text added and saved; "Declined, not charged" is the
existing "Could not turn your speech into text. Your recording is kept."
with Retry. The first three required behaviors above are this mechanism
adapted. **Where it breaks:** a payment is one transaction with one final
answer, and the terminal makes the person wait. A dictation writes several
passages while the author is still speaking, and the author may type in the
note meanwhile. So recording must outrank the working and finished stages
until Stop, the result is declared once, after Stop, and nothing in the note
is blocked. The mechanism is a candidate design, not evidence that newcomers
will understand it; the owner's first dictation after delivery is the test.

**Key examples:**

1. **First dictation:** A note has one saved paragraph. The author opens
   Audio tools and reads that it is ready, with one main action named
   Record. They choose Record and allow the microphone. The status says it
   is recording and the main action is now Stop. They speak a sentence and
   choose Stop. The status says their speech is being turned into text. The
   sentence appears in the body and the status says it was added to the
   note. After reload the paragraph and the sentence each appear once.
2. **Text arrives while speaking:** The author pauses for more than three
   seconds mid-dictation and a first passage is written to the body. The
   status still says recording and the main action is still Stop.
3. **Nothing was heard:** The microphone picks up only silence. The author
   chooses Stop. The body is unchanged and the status says no speech was
   turned into text.
4. **Microphone refused:** The author chooses Record and refuses the
   browser's permission request. Nothing is recorded. The message says the
   microphone could not be used and to allow access in the browser. Record
   is still the main action.
5. **Failure at Stop:** The transcription fails when the author chooses
   Stop. The body is unchanged. The failure message and Retry appear as the
   next step. Retry succeeds; the passage is joined once and the status says
   it was added to the note.
6. **Touch screen:** On a device without a pointer, every control in the
   panel can be told apart by a name the author can read.

**UI:** Words only; layout, components and exact wording are for execution
and the owner's review. Proposed status wording: "Ready to record",
"Recording. Speak now.", "Turning your speech into text…", "Added to your
note.", "No speech was turned into text." Proposed start-failure wording:
"Could not use the microphone. Allow microphone access in your browser, then
try again." Advanced Options today holds only full screen, so that control
can carry its own name instead of a gear.

**Architecture:** No Accepted decision is in conflict. The work stays in the
note page's Audio tools panel; it adds no route
([ADR 0005](../../docs/adrs/0005-web-routes-accepted.md)) and no backend or
API change. The start-failure and conversion-failure messages are catches
with a business outcome and a clearer message, as
[ADR 0006](../../docs/adrs/0006-failure-handling-accepted.md) allows; other
failures stay loud. [ADR 0001](../../docs/adrs/0001-ubiquitous-language.md)
names no audio terms, so "Audio tools" stays the product's name. Under the
[north star](../NORTH-STAR.md#one-real-service-audio-test) the new behavior
is proved with the mocked recording journey and mounted tests, not a second
real-service test. The result status gives those tests a real completion
sign in place of waiting for Save Audio Locally.

**Decided by the owner on 2026-10-06:**

1. **Mid-speech control:** "Flush Audio" stays, under a plain name such as
   "Write text now", shown only while recording.
2. **Failure message after Stop:** "Could not turn your speech into text.
   Your recording is kept until you close Audio tools." Mid-speech the
   existing wording stays, because recording goes on and retries by itself.
3. **Retry's place:** the suggested next step beside the message, with
   Record still available.
4. **Microphone message:** the one start-failure message is part of this
   story.

**System consequence:** The result status follows the real save, so the
panel needs one place that knows the dictation's state from real events:
recording started, conversion running, text written, save succeeded or
failed. Today that is spread over separate flags.

- **Evidence / learning:** The owner reports an ugly and unintuitive UI. The
  account of today's panel above is read from the code; refused permission
  and real hardware capture have not been observed in a browser.
- **Effort hypothesis:** M — medium confidence; assumes the panel's state
  can be derived from events the recorder and the save already produce.
- **Depends on:** The existing capture workflow and its delivered
  preservation and recovery behavior. No other story.
- **Safe stopping point:** A newcomer can complete and recognize a saved spoken
  addition without needing the later title-authoring capabilities.

<a id="create-with-spoken-title"></a>
### Create a note using a spoken title

**Identity:** SEED-066#create-with-spoken-title
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/009-create-a-note-using-a-spoken-title/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"2f1125acaf59cbbcec2cc34abe4aeb454ecbd08cca6255abacbbae303e9bff3b","plan":"d106c12dc330094fb40c186deee427d0e6429b2817f6a02641819f884edc4d1e"}}
```

**Goal:** A note author who is creating a note names it by speaking instead
of typing, sees the heard words in the title field, corrects them if needed,
and submits as usual. The note is created once, in the chosen location, with
the title the author reviewed. This is the "dictating titles" part of voice
input; renaming an existing note by speaking is the sibling story, not this
one.

**The user:** The same note author as
[Complete a first dictation](#understandable-first-dictation): someone who
opens New note from the notebook sidebar or from a note's toolbar, perhaps on
a phone, and would rather say a title than type it. A title is a few words,
so they expect to speak once and see the whole title, not fragments.

**What the author meets today (read from the code, 2026-10-06):** New note is
a dialog with a folder chooser, an optional parent relationship, a title
field, a Wikidata lookup beside the title, a list of existing notes whose
titles match what has been typed, and Submit. The title field opens as
"Untitled" with the text selected, so typing replaces it; when the parent
folder's README sets a title pattern, the field opens with that rendered
pattern followed by a space for the author to continue. The title field is an
editable text area, so dictation built into the operating system can already
type into it; nothing in the dialog offers to listen. Audio tools exists only
on an existing note's toolbar and writes only to the body.

**Compared alternative — operating-system dictation:** The seed asked for
this comparison before building a native control. Dictation built into
macOS, iOS, Android and Windows types into any focused text field, so an
author who has it turned on can already speak a title in New note; this has
not been observed in Donut. It costs nothing to build, shows words as they
are spoken, and uses the author's own setup. Against it: it is absent or
turned off on many machines and browsers, its language is set in the
operating system rather than heard, it is a second, different dictation next
to Donut's own within one note-writing session, and the product can neither
name it nor help when it is missing. Recommendation: build the native
control, so one voice-input journey with one set of words and one
transcription quality covers both the title and the body. The owner accepted
this on 2026-10-06.

**Scope:**

- **Required — speak the title in New note:** New note offers one control,
  named in words, that listens for the title: "Speak the title". The author
  chooses it, allows the microphone, speaks, and chooses Stop. The heard
  words arrive in the title field once, after Stop, as one line, exactly as
  the transcription heard them, its segments joined as body passages are
  joined. No part appears while the author is still speaking.
- **Required — the spoken words go where typing goes:** They replace the
  untouched default "Untitled" and otherwise join the end of the title the
  author already has, including a title pattern the dialog opened with. The
  author corrects the title by typing or by speaking again, as with a typed
  title.
- **Required — a spoken title is an ordinary title:** It drives the same
  search for existing notes, the same Wikidata lookup, the same illegal
  character replacement, warnings and validation, and is submitted by the
  same Submit. Nothing is created until the author submits; the note is
  created once, in the chosen folder with the chosen parent relationship,
  with the title shown at that moment.
- **Required — the state in words:** While listening, the control becomes
  Stop and a status the author can read, and that assistive technology
  announces when it changes, says the dialog is recording; after Stop it
  says the speech is being turned into text until the words are in the
  field. When nothing was heard, it says so and the title field is
  unchanged. Colour or movement alone never carries the state. Wording
  follows [Complete a first dictation](#understandable-first-dictation).
- **Required — submit after the title is heard:** While the dialog is
  listening or turning speech into text, Submit is not offered.
  Justification: the note must be created once with the title the author
  reviewed; submitting then would create it under the old title and lose the
  spoken one.
- **Required — failure leaves the title alone:** When the microphone cannot
  be used, the first-dictation story's message is shown as a problem and
  "Speak the title" stays available. When the speech could not be turned
  into text, the message says so, the title field is unchanged, and speaking
  again sends only the new recording: a failed attempt's audio is not
  carried into the next one. There is no Retry control; speaking again is
  the retry.
- **Keep:** Body dictation on the created note behaves as the
  [voice-input documentation](../../docs/voice-input.md) describes and never
  changes the title. Every New note behavior above that is not about speech
  is unchanged, including a title prefilled from an unresolved wiki link.
- **Deferred, not promised:** Listening ending by itself when the author
  falls silent; tidying the heard words, such as removing a closing full
  stop or changing capitals; speaking the folder or parent relationship;
  submitting by voice; a spoken title in the existing-note title editor (the
  sibling story); keeping a failed recording for Retry; a microphone chooser
  or Write text now in New note; and observing operating-system dictation or
  hardware capture, which stay gaps.
- **No other rejection constraints:** Nothing else makes the product refuse
  an action New note accepts today.

**Key examples:**

1. **Spoken title:** From the sidebar, the author opens New note; the title
   field shows "Untitled". They choose "Speak the title", allow the
   microphone, say "Photosynthesis in desert plants", and choose Stop. The
   status says the speech is being turned into text; then the title field
   reads what was heard, "Photosynthesis in desert plants", and existing
   notes matching it are listed, as after typing. The author chooses Submit.
   The note exists once in the notebook root with that title, and after
   reload the title is unchanged.
2. **Correct before submitting:** The field reads "Photosynthesis in dessert
   plants". The author fixes "dessert" by typing and submits. The note has
   the corrected title.
3. **Continue a title pattern:** New note opens with "2026-10-06 " from the
   folder's title pattern. The author speaks "weekly review" and the title
   reads "2026-10-06 weekly review".
4. **Nothing heard:** The author chooses "Speak the title", says nothing, and
   chooses Stop. The status says no speech was turned into text; the title
   still reads "Untitled"; Submit is offered again.
5. **Failure, then success:** The transcription fails at Stop. The message
   says the speech could not be turned into text; the title is unchanged;
   Submit is offered again. The author chooses "Speak the title" once more
   and says "Lighthouse keepers". The title reads "Lighthouse keepers" only.
6. **Child note on a phone:** From a note's toolbar on a touch screen, the
   author opens New note, and every control, including "Speak the title",
   can be told apart by a name they can read. The spoken title is created
   under the chosen folder with the chosen parent relationship.
7. **Body stays separate:** After creating "Lighthouse keepers", the author
   dictates a paragraph with Audio tools. The paragraph goes to the body and
   the title stays "Lighthouse keepers".

**UI:** Words only; layout and exact wording are for execution and the
owner's review. "Speak the title" sits with the title field inside New note
and becomes "Stop" while listening. Status wording reuses the first-dictation
story: "Recording. Speak now.", "Turning your speech into text…", "No speech
was turned into text.", "Could not use the microphone. Allow microphone
access in your browser, then try again.", and "Could not turn your speech
into text." Once the words are in the field, the field itself is the result;
no "added" status is needed.

**Decided by the owner on 2026-10-06:**

1. **Native control:** build "Speak the title" in New note rather than
   relying on operating-system dictation, as compared above.
2. **The author chooses Stop:** listening does not end by itself after a
   silence, consistent with Audio tools and with no silence threshold to
   tune. Automatic stop stays deferred.
3. **Heard words stay as transcribed:** the transcription commonly ends
   speech with a full stop; it stays in the field and the author reviews
   before Submit. Tidying is decided after the owner has tried it.

- **Evidence / learning:** No voice-title control exists in New note (code,
  2026-10-06). The audio request carries only the audio and the mid-speech
  flag and returns segment texts without touching a note, so title capture
  can reuse it. Operating-system dictation in Donut remains unobserved.
- **Effort hypothesis:** M — medium confidence; assumes the recorder and
  transcription request that Audio tools uses can be driven from New note,
  converting only at Stop, without a backend change.
- **Depends on:** Existing note creation. Not a prerequisite, but reuse the
  status wording and state handling from
  [Complete a first dictation](#understandable-first-dictation) once it has
  landed. Renaming by speech is independent.
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
  input: compared in [Create a note using a spoken title](#create-with-spoken-title),
  where the owner chose a native control on 2026-10-06; still open for
  renaming an existing note. Optional one-time automatic title generation
  stays deferred.
- The interaction for explicit title dictation on an existing note; the New
  note interaction is proposed in
  [Create a note using a spoken title](#create-with-spoken-title).

## Breadcrumbs

- Owner's voice-input problem report, 2026-10-03, in this conversation.
- Owner's acceptance of all nine proposed stories and their priority, with
  authorization to record them on main and sync origin, 2026-10-03.
- Effort-band convention:
  [SEED-039](SEED-039-faster-ci-feedback.md#story-decomposition).
- Later decomposition workflow:
  [dough-story-decomposition](../../.agents/skills/dough-story-decomposition/SKILL.md).
