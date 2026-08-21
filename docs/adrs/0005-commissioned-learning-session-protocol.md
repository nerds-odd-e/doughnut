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
LLMs, with no schema validator anywhere. Doughnut still has to record Grades
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

This ADR owns the **domain meaning** of a commissioned Learning Session and the
**document protocol** for Request and Report. Source code owns rendering,
parsing, and persistence.

### Domain

A Learning Session is the Request/Report **activity**, not a persisted aggregate.
Session Item is Request and Report document vocabulary (ADR 0001), not a stored
row. The learner does not paste a Report into a stored session, and Doughnut
does not amend a session in place.

Recording Feedback applies a **Grade** to the matching commissioned memory
tracker. What that Grade does to the schedule is ADR 0003 — including whether a
later Grade re-grades from an earlier snapshot or applies on top of the state
the earlier Grade already produced.

### Exchange medium

1. The protocol is two markdown documents exchanged by copy and paste. Transport,
   authentication, and machine identity are not part of it.
2. Doughnut renders the Request for the learner to copy. The learner pastes the
   Report back into Doughnut for that notebook. Because there is no persisted
   session to target, neither document carries a session identifier.
3. Both documents are self-describing. A Tutor who has never seen Doughnut can act
   on the Request alone, including how to grade.

### Learning Session Request

The Request names the notebook, the Session Items to teach (keyed by note
title), tutoring status per item, the notes to teach from, related notes for
context, and how to report (with the rubric inline).

Document structure, in order:

1. Heading `# Learning Session Request`
2. `<instructions>` — tutor role for this notebook, any notebook-specific
   instruction, and to wait for the learner before starting
3. `<session_item_titles>` — the Session Item titles in this session
4. `<session_items>` — one `### {title}` section per Session Item, with tutoring
   status and a `<focus_note>` for that item
5. `<related_notes>` — notes related to the Session Items, for tutor context;
   Session Items themselves are not repeated here
6. `<how_to_report>` — the Grade 1–4 rubric and a worked Report example

Focus notes and related notes are **Focus Context** (ADR 0001), not a second
note format.

The rubric in `<how_to_report>` is:

- 4 — mastered the session item with full fluency
- 3 — mastered the session item with fluency
- 2 — mastered the session item but not fluent, or needed a reminder then showed mastery
- 1 — needed several reminders, or could not reach the session item even with help

Those four values are Grade (ADR 0001) and FSRS `G` (ADR 0003). The Tutor is
told to grade only Session Items actually taught in this session.

```markdown
# Learning Session Request

<instructions>
You are the tutor to help the learner to study Spanish conversation.
Wait for the learner's instruction before starting the learning session.
</instructions>

<session_item_titles>
- Hola
- Gracias
</session_item_titles>

<session_items>
### Hola
- Tutoring status: …
<focus_note>…</focus_note>

### Gracias
- Tutoring status: …
<focus_note>…</focus_note>
</session_items>

<related_notes>
…
</related_notes>

<how_to_report>
Teach the session items above, then return a Learning Session Report giving one
Grade from 1 to 4 per item.
…
</how_to_report>
```

### Learning Session Report

The Tutor returns one Grade per taught Session Item inside
`<session_item_grades>`. Line form is `{title}: {1–4}`. Prose and markdown
outside the block are not Feedback.

```markdown
# Learning Session Report

Thanks for a great session today.

<session_item_grades>
Hola: 4
Gracias: 1
</session_item_grades>
```

### Matching

1. Entries match Session Items by note title within the notebook.
2. Duplicate titles in one notebook are ambiguous: they do not match, and
   Doughnut does not guess.
3. Unmatched entries and Grades that are not integers from 1 to 4 are rejected
   and reported to the learner. Recording is not all-or-nothing: a partly usable
   Report still records Feedback for the Session Items it matched.
4. A Session Item with no matching entry receives no Feedback; its tracker is
   unchanged.
5. A further Report is another recording. Doughnut does not keep a session bag
   to amend.

### Out of scope

- Descriptive feedback and recommendations as recorded Feedback — the protocol
  tolerates prose, but only Grades are recorded for now
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
  item; the learner sees it as unmatched rather than silently losing a Grade.
- Growing to descriptive feedback, recommendations, or a machine transport is
  additive — Session Item (in the documents) and Feedback already carry the
  concepts.
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
- People and LLMs can follow a short rubric well enough for a 1–4 Grade to mean
  something.
- The tutoring status the Request exposes is enough for a Tutor to pitch the
  session appropriately.

## Options considered

- **Tutor Grades 1–4 identical to FSRS G** (`1` Again, `2` Hard, `3` Good,
  `4` Easy) — accepted (Decision above). The Request rubric is those four
  lines.
- **A 0–5 rubric with a shifted Good/Hard/Easy map** — rejected: valid report
  Grades are 1, 2, 3, and 4; the numeric value is G.

## Related

- Supersedes: (none)
- Superseded by: (none)
- Links: ADR 0001 [ubiquitous language](./0001-ubiquitous-language.md);
  ADR 0003 [spaced-repetition scheduling policy](./0003-spaced-repetition-scheduling-policy-accepted.md);
  [Ed API: Towards a Shared API for Education Microservices](https://aet.cit.tum.de/research/publications/soelch2026las.pdf);
  [Caliper Analytics 1.2](https://www.imsglobal.org/spec/caliper/v1p2);
  [QTI 3.0 overview](https://www.imsglobal.org/spec/qti/v3p0/oview);
  [xAPI / Caliper comparison](https://www.imsglobal.org/initial-xapicaliper-comparison)
