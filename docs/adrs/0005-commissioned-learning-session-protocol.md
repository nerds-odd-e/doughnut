# 0005 — Commissioned learning session protocol

**Status:** Proposed  
**Date:** 2026-08-07  
**Decision makers:** Terry Yin (approval pending advice)  
**Consulted:** To be filled by the decision maker

## Context

Doughnut is adding commissioned learning (ADR 0001, commissioned learning terms):
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

### Exchange medium

1. The protocol is two markdown documents exchanged by copy and paste. Transport,
   authentication, and machine identity are not part of it.
2. Doughnut renders the Request for the learner to copy. The learner pastes the
   Report into the Learning Session it belongs to. Because the learner chooses the
   target session, neither document carries a session identifier.
3. Both documents are self-describing. A Tutor who has never seen Doughnut can act
   on the Request alone, including how to score.

### Learning Session Request

Doughnut states the notebook, how to report (with the rubric inline), and one
section per Session Item keyed by note title, carrying the learner's learning
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
- Learning status: 1 previous session, last on 2026-08-06
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
- Learning status: not yet tutored
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
score from 0 to 5 per item:

- 5 — mastered the learning point with full fluency
- 4 — mastered the learning point with fluency
- 3 — mastered the learning point, but not fluent
- 2 — needed a reminder at first, then showed signs of mastering it
- 1 — needed several reminders
- 0 — could not reach the learning point even with help

Example of how to provide feedback:

# Learning Session Report

<session_item_scores>
Hola: 5
Gracias: 1
</session_item_scores>

Only score session items that were actually taught in this session. Do not list
items that were not learnt in the session.
</how_to_report>
```

### Learning Session Report

The Tutor returns one score per note title inside a tagged block. Prose and
markdown headers outside the block are ignored.

```markdown
# Learning Session Report

Thanks for a great session today.

<session_item_scores>
Hola: 5
Gracias: 1
</session_item_scores>
```

If `<session_item_scores>` is absent, Doughnut falls back to parsing the whole
document (minus the optional `# Learning Session Report` header) so older pasted
reports still work.

Descriptive prose may accompany a score line inside the block; the MVP tolerates
and ignores it rather than rejecting the Report.

### Matching and recording

1. Entries match Session Items by note title within the session's notebook.
2. Doughnut records Feedback for every matched entry and rejects the entries it
   cannot match, reporting the rejected ones to the learner. Recording is not
   all-or-nothing: a partly usable Report still moves the trackers it matched.
3. A matched entry whose score is not an integer from 0 to 5 is rejected and
   reported the same way.
4. A Session Item with no matching entry receives no Feedback, and its tracker is
   unchanged.
5. A further Report for the same Learning Session amends it: matched items take
   the new Feedback and reschedule again. Sessions holding recorded Feedback are
   visibly marked among the learner's sessions.
6. What a score does to the schedule is ADR 0003, not this ADR — including
   whether an amended score re-grades from the tracker's pre-session state or
   applies on top of the state the earlier score already produced.

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
  additive — Session Item and Feedback already carry the concepts.
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
- People and LLMs can follow a short rubric well enough for a 0–5 score to mean
  something.
- The learning status the Request exposes is enough for a Tutor to pitch the
  session appropriately.

## Related

- Supersedes: (none)
- Superseded by: (none)
- Links: ADR 0001 [ubiquitous language](./0001-ubiquitous-language.md);
  ADR 0003 [spaced-repetition scheduling policy](./0003-spaced-repetition-scheduling-policy.md);
  [Ed API: Towards a Shared API for Education Microservices](https://aet.cit.tum.de/research/publications/soelch2026las.pdf);
  [Caliper Analytics 1.2](https://www.imsglobal.org/spec/caliper/v1p2);
  [QTI 3.0 overview](https://www.imsglobal.org/spec/qti/v3p0/oview);
  [xAPI / Caliper comparison](https://www.imsglobal.org/initial-xapicaliper-comparison)
