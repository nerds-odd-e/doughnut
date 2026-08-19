# 0005 — Commissioned learning session protocol

**Status:** Proposed  
**Date:** 2026-08-07  
**Decision makers:** Terry Yin (approval pending advice)  
**Consulted:** To be filled by the decision maker

## Context

Doughnut is adding commissioned Learning Sessions (ADR 0001, commissioned
Learning Session terms):
the Learning Orchestrator commissions a Tutor to conduct a Learning Session
covering the due commissioned memory trackers of one notebook, then records the
Tutor's Learning Session Report.

The Tutor sits outside Doughnut — a person, or a general-purpose AI assistant the
learner talks to. There is no Doughnut client on the Tutor's side and, for the
MVP, no transport at all: the learner copies the Learning Session Request out and
pastes the Learning Session Report back.

Both documents are therefore written and read by people and by general-purpose
LLMs, with no schema validator anywhere. Doughnut still has to record scores
against the right Session Items without guessing. That needs a stated contract.

Existing learning-interoperability standards do not fit this shape:

| Standard | Why it does not fit |
|----------|---------------------|
| SCORM / xAPI | Track experiences from instrumented content into a record store; assume machine transport plus a package or activity model |
| Caliper Analytics | Event vocabulary and a Sensor API for analytics pipelines; its Feedback Profile models ratings and comments as events, not a tutoring brief |
| QTI | XML data model for assessment items, tests, and results — questions Doughnut is not asking here |
| LTI | Launch and integration protocol between a platform and an external tool |
| Ed API (2026, v0.1) | Closest in spirit, an OpenAPI interface for education microservices with an `/evaluate` capability, but still an HTTP API rather than a document a learner can hand to a Tutor |

Each presupposes a machine on the other end. None describes a human-and-LLM
readable brief that comes back carrying grades. Markdown rubrics are common
practice in LLM tooling, but practice, not a standard.

So the MVP defines its own minimal markdown contract, deliberately small enough
that a later machine transport can carry the same concepts unchanged.

## Decision

A Learning Session is the Request/Report **activity**, not a persisted aggregate.
There is no `learning_session` or `session_item` table. Session Item is Request
and Report document vocabulary (ADR 0001), not a row. Recording Feedback writes
RecallLogs and updates the Memory Tracker schedule (ADR 0003). The learner does
not paste a Report into a stored session, and Doughnut does not amend a session
in place.

### Exchange medium

1. The protocol is two markdown documents exchanged by copy and paste. Transport,
   authentication, and machine identity are not part of it.
2. Doughnut renders the Request for the learner to copy. The learner pastes the
   Report back into Doughnut for that notebook. Because there is no persisted
   session to target, neither document carries a session identifier.
3. Both documents are self-describing. A Tutor who has never seen Doughnut can act
   on the Request alone, including how to score.

### Learning Session Request

Doughnut states the notebook, how to report (with the rubric inline), and one
section per Session Item keyed by note title, carrying the learner's tutoring
status and a focus-note-only Focus Context block with the note body.

```markdown
# Learning Session Request

<instructions>
You are the tutor to help the learner to study Spanish conversation.

Focus on conversational phrases.

Wait for the learner's instruction before starting the learning session.
</instructions>

<session_item_titles>
- Hola
- Gracias
</session_item_titles>

<session_items>
### Hola
- Tutoring status: 1 previous session, last on 2026-08-06
<focus_context>
Purpose: Context around the focus note for AI use.
Max depth: 0

<focus_note>
Title: Hola
Notebook: Spanish conversation
Depth: 0

```doughnut-note-md
Hello
```
</focus_note>
</focus_context>

### Gracias
- Tutoring status: not yet tutored
<focus_context>
Purpose: Context around the focus note for AI use.
Max depth: 0

<focus_note>
Title: Gracias
Notebook: Spanish conversation
Depth: 0

```doughnut-note-md
Thank you
```
</focus_note>
</focus_context>
</session_items>

<how_to_report>
Teach the session items above, then return a Learning Session Report giving one
score from 1 to 4 per item:

- 4 — mastered the session item with full fluency
- 3 — mastered the session item with fluency
- 2 — mastered the session item but not fluent, or needed a reminder then showed mastery
- 1 — needed several reminders, or could not reach the session item even with help

Example of how to provide feedback:

# Learning Session Report

<session_item_scores>
Hola: 4
Gracias: 1
</session_item_scores>

Only score session items that were actually taught in this session. Do not list
items that were not taught in the session.
</how_to_report>
```

### Learning Session Report

The Tutor returns one score per note title inside a tagged block. Prose and
markdown headers outside the block are ignored.

```markdown
# Learning Session Report

Thanks for a great session today.

<session_item_scores>
Hola: 4
Gracias: 1
</session_item_scores>
```

If `<session_item_scores>` is absent, Doughnut falls back to parsing the whole
document (minus the optional `# Learning Session Report` header) so older pasted
reports still work.

Descriptive prose may accompany a score line inside the block; the MVP tolerates
and ignores it rather than rejecting the Report.

### Matching and recording

1. Entries match Session Items by note title within the notebook.
2. Doughnut records Feedback for every matched entry by writing a RecallLog
   (`answer_id` null) on that commissioned tracker and scheduling it. Unmatched
   entries are rejected and reported to the learner. Recording is not
   all-or-nothing: a partly usable Report still moves the trackers it matched.
3. A matched entry whose score is not an integer from 1 to 4 is rejected and
   reported the same way.
4. A Session Item with no matching entry receives no Feedback, and its tracker is
   unchanged.
5. A further Report is another recording: new RecallLogs and another schedule
   update. Doughnut does not keep a session bag to amend, and does not mark
   recorded sessions in a list.
6. What a score does to the schedule is ADR 0003, not this ADR — including
   whether a later score re-grades from an earlier snapshot or applies on top of
   the state the earlier score already produced.
7. The Memory Tracker's latest tutor feedback score is the latest tutor RecallLog
   on that commissioned tracker (`answer_id` null, excluding CONFUSION), mapped
   **4 / 3 / 2 / 1** from `EASY` / `GOOD` / `HARD` / `AGAIN`.

### Out of scope

- Descriptive feedback and recommendations as recorded Feedback — the protocol
  tolerates prose, but only scores are recorded for now
- Machine transport (HTTP, MCP), Tutor authentication, Tutor identity in the record
- Learning Sessions spanning more than one notebook
- AI-assisted request shaping (the "smart" generator)
- Any Tutor-driven change to note content

## Consequences

- A Tutor needs nothing but the pasted Request, so a person and ChatGPT are
  interchangeable without Doughnut knowing which one answered.
- Note titles become protocol identifiers within a notebook. Duplicate titles in
  one notebook are ambiguous and must be reported as unmatched, never guessed.
- Renaming a note between commissioning and recording breaks matching for that
  item; the learner sees it as unmatched rather than silently losing a score.
- Growing to descriptive feedback, recommendations, or a machine transport is
  additive — Session Item (in the documents) and Feedback (as RecallLog) already
  carry the concepts.
- There is no past session to reopen and amend; a later Report appends history.
- No document versioning: the Request restates the rubric every time, so an old
  copy stays interpretable on its own.

## Pros

- Smallest contract that closes the loop, with no dependency on the Tutor's tooling.
- Human-readable, so a bad exchange is visible to the learner instead of silent.
- Concepts map onto a future HTTP or MCP surface without renaming anything.

## Cons

- Title matching is fragile against renames and duplicate titles.
- Free-form LLM output will sometimes need re-pasting.
- Copy-paste keeps the learner in the loop of every exchange.
- No provenance: Doughnut cannot tell which Tutor produced a Report.

## Prerequisites / Assumptions

- Note titles are unique within a notebook in practice.
- Learners can copy and paste between Doughnut and their Tutor's channel.
- People and LLMs can follow a short rubric well enough for a 1–4 score to mean
  something.
- The tutoring status the Request exposes is enough for a Tutor to pitch the
  session appropriately.

## Options considered

- **Tutor scores 1–4 identical to FSRS G** (`1` Again, `2` Hard, `3` Good,
  `4` Easy; `score = G`) — accepted (Decision above). The Request rubric is
  those four lines; latest tutor feedback is **4/3/2/1** from `EASY` / `GOOD` /
  `HARD` / `AGAIN`.
- **A 0–5 rubric with a shifted Good/Hard/Easy map** — rejected: valid report
  scores are 1, 2, 3, and 4; the score is G.

## Related

- Supersedes: (none)
- Superseded by: (none)
- Links: ADR 0001 [ubiquitous language](./0001-ubiquitous-language.md);
  ADR 0003 [spaced-repetition scheduling policy](./0003-spaced-repetition-scheduling-policy.md);
  [Ed API: Towards a Shared API for Education Microservices](https://aet.cit.tum.de/research/publications/soelch2026las.pdf);
  [Caliper Analytics 1.2](https://www.imsglobal.org/spec/caliper/v1p2);
  [QTI 3.0 overview](https://www.imsglobal.org/spec/qti/v3p0/oview);
  [xAPI / Caliper comparison](https://www.imsglobal.org/initial-xapicaliper-comparison)
