---
id: SEED-090
status: dormant
planted: 2026-10-28
planted_during: owner-directed backlog addition for commissioned learning through the ChatGPT plugin
trigger_when: enabling learners to conduct Donut commissioned learning directly in ChatGPT through MCP
scope: medium
---

# SEED-090: Conduct commissioned learning through the ChatGPT plugin

## Why This Matters

A learner connected to Donut through the ChatGPT plugin should be able to ask
ChatGPT to run their commissioned learning and return the results to Donut.
The owner's current example is Japanese learning. The existing commissioned
learning protocol supplies the tutoring instructions, learning material, and
report format; MCP should carry that same interaction.

## Alternatives and Decision

- **Existing manual handoff:** Copy the Learning Session Request from Donut
  into ChatGPT and paste its Learning Session Report back into Donut. Keeps
  today's auth model; fails the "save directly" goal.
- **Requested direction (recommended):** Expose the existing Learning Session
  Request and Report recording through Donut HTTPS MCP so a connected ChatGPT
  tutor can take the first available learning session action, tutor from the
  Request, and record Feedback for completed Session Items only. Japanese is
  the initial example; selection follows Donut's available-action order for
  any notebook.
- **Structured JSON session tools instead of the markdown protocol:** Would
  redesign Request/Report for MCP schemas. Bigger change; breaks reuse of the
  existing protocol and web recording path. Deferred.
- **Reconstruct the session from note-read MCP tools only:** Cannot select due
  commissioned actions or record Feedback. Insufficient for the goal.
- **Borrowed mechanism — homework packet / clinical handover:** A structured
  brief leaves the system, interactive work happens outside, a structured
  return updates the record. Maps to Learning Session Request → ChatGPT
  tutoring → Learning Session Report. The analogy breaks where Donut would
  need mid-session orchestration (it does not) and where ChatGPT must call
  MCP to return the report rather than a human pasting it. Analogy is a
  candidate shape, not evidence.
- **Deferred promises:** Choosing among learning programs and automatically
  progressing through additional sessions can be considered later.

## Story Decomposition

<a id="story-1"></a>

### Conduct a commissioned learning session in ChatGPT through MCP

**Identity:** SEED-090#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/077-commissioned-learning-session-in-chatgpt/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"7d55eadd1f4ef90a34afbd41e53b8caf709875e223f9a3b3a0b77c946df2136f","plan":"bff3455145cc970df4a8a4ce7f85c753ccbb6f87d6e184afd9407eda00265911"}}
```

- **Goal:** A Donut learner connected through the ChatGPT plugin can ask
  ChatGPT to conduct one commissioned Learning Session and have Feedback for
  the Session Items they completed saved in Donut, including when they end
  early.
- **Scope:** On the learner's request in the connected ChatGPT conversation,
  obtain the first available Potential learning session for that learner in
  Donut's returned order. Load its Learning Session Request (notebook-specific
  instructions and Session Items) through MCP and tutor interactively from
  that Request. When the session finishes or the learner asks to end it,
  submit a Learning Session Report through MCP for completed Session Items
  only, using the existing commissioned learning protocol (Grade 1–4 and
  descriptive Feedback, title matching within the notebook, existing
  Feedback recording and scheduling). Incomplete items receive no Feedback
  and keep their current learning state. If no Potential learning session is
  available, tell the learner there is none. Reuse the existing
  request/record domain behavior; do not invent a second protocol or a
  program-picker. Expand hosted-assistant OAuth and consent with a narrow
  grant for recording Learning Session Reports (distinct from general note
  editing and from `notes:write`), so the ChatGPT plugin path can save
  Feedback; do not allow report recording under `notes:read` alone.
- **Key examples:**
  1. **Japanese session:** Japanese is the only available Potential learning
     session. The learner asks ChatGPT to start learning from Donut. ChatGPT
     retrieves that session's Request through MCP, follows its tutoring
     instructions, teaches interactively, and submits Feedback for the
     completed items. The learner can see that Feedback in Donut's learning
     history.
  2. **Several actions:** Donut supplies multiple Potential learning
     sessions. ChatGPT uses the first in Donut's returned order and tutors
     from that session's Request.
  3. **End early:** A Request contains Session Items A, B, and C. After
     completing A, the learner ends before completing B or starting C.
     ChatGPT submits Feedback only for A; B and C remain unscored with their
     existing schedules. Ending before completing any item records no
     Feedback.
  4. **Nothing available:** Donut supplies no Potential learning session.
     ChatGPT explains that there is no available session.
- **Deferred promises:** Choosing among programs or notebooks when several
  are available; automatically starting a further session; replacing the
  markdown Request/Report protocol with MCP-only structured schemas;
  general note editing or a broad `notes:write` grant for assistants.
- **Rejection constraints:** None beyond existing notebook authorization and
  Report matching/recording rules (unmatched or invalid grades are rejected
  and reported; unmatched Session Items stay unchanged).
- **Value / learning:** Make commissioned practice usable inside the
  connected conversation, with progress retained when time is limited.
- **Effort hypothesis:** M–L (2–5 hours) including the MCP surface and the
  narrow hosted-assistant report-recording grant. Confidence medium after
  investigation below.
- **Safe stopping point:** One requested session can be taught and its
  completed items recorded, including an early finish. Further selection and
  multi-session workflows remain deferred.

#### Refinement — 2026-10-28

**Investigate (facts and sources).**

- MCP today exposes only note-read tools (`list_note_candidates`, `read_note`,
  `get_note_graph`) at `/api/mcp`
  ([`.agents/skills/mcp-server/SKILL.md`](../../.agents/skills/mcp-server/SKILL.md),
  `McpServerConfiguration`).
- Learning Session Request and Report recording already exist over HTTP
  (`GET /api/learning-sessions/request`, `POST /api/learning-sessions/record`)
  and reuse the
  [commissioned learning session protocol](../../docs/commissioned-learning-session-protocol.md).
  Partial reports already record only matched taught items
  ([ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md#commissioned-learning-session)).
- Potential learning sessions are due commissioned trackers grouped by
  notebook in first-seen order (`useRecallData.potentialLearningSessions`);
  there is no MCP or HTTP auto-pick of the first action today.
- Hosted ChatGPT linking supports only `notes:read`
  (`AssistantAuthorizationConfiguration.READ_SCOPE`,
  `AssistantResourcePolicy`,
  [hosted assistant guide](../../docs/assistant-oauth.md)). Requests for
  `notes:write` are rejected. Consent and docs state the assistant gains no
  content editing. Personal bearer tokens authenticate as the full user on
  MCP and are a separate connection path from ChatGPT's OAuth plugin setup.

**Hypotheses settled by observation.** "MCP already supports conducting a
Learning Session" is false. "A ChatGPT OAuth grant can currently record
Feedback" is false under the present `notes:read`-only policy.

**Explore comparison.**

| Candidate | Meets goal? | Limit |
| --- | --- | --- |
| Protocol-preserving MCP tools over existing request/record + first action | Yes, if ChatGPT can authorize report recording | Needs hosted-assistant write grant beyond today's read-only consent |
| Manual copy/paste (status quo) | No direct save | No MCP change |
| Note-read tools only | No | Cannot select due sessions or record Feedback |
| New structured MCP session schemas | Possible later | Redesigns the protocol; out of requested minimal reuse |

**Recommended direction:** Protocol-preserving MCP tools that reuse
`LearningSessionService` (and the same first-available-action order the UI
derives), with ChatGPT tutoring from Request markdown. Include a narrow
hosted-assistant authorization for recording Learning Session Reports so the
ChatGPT OAuth path can meet the goal without granting general note editing.

**Architecture.**

- Responsibilities to add on the existing MCP server: obtain the first
  Potential learning session (or report none), return its Learning Session
  Request, and record a Learning Session Report for that notebook. Keep
  tutoring conversation in ChatGPT; Donut remains Learning Orchestrator for
  request/report only ([ADR 0001](../../docs/adrs/0001-ubiquitous-language.md#commissioned-learning-session)).
- Recording continues to apply Grades and descriptive Feedback under
  [ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md#commissioned-learning-session);
  unmatched items stay unchanged.
- MCP stays under `/api/mcp` ([ADR 0005](../../docs/adrs/0005-web-routes-accepted.md);
  existing OAuth discovery exceptions unchanged).
- No-session and rejected Report entries are business outcomes for the
  learner; loud failure remains legitimate for unexpected faults
  ([ADR 0006](../../docs/adrs/0006-failure-handling-accepted.md)).
- Consequential concern: today's hosted-assistant model is read-only. Adding
  a record tool without a new grant would either fail ChatGPT or silently
  widen `notes:read` into scheduling writes against documented consent.
  Prefer a narrow report-recording scope and consent text, not `notes:write`
  and not write under `notes:read`. No Accepted ADR forbids that expansion;
  it is a product authorization change, not an ADR conflict. Filename
  hygiene: ADR 0001 is Accepted in the index and in-file status without an
  `-accepted` filename suffix.

**Decision (owner, 2026-10-28):** Expand hosted-assistant OAuth/consent with a
narrow Learning Session Report recording grant, as recommended. General note
editing and `notes:write` stay out of scope. Slice planning may continue.

## Breadcrumbs

- Owner request in this chat, 2026-10-28: queue this story first, use the first
  learning session action, reuse the commissioned learning protocol, and report
  only finished items when the learner ends early.
- Owner decision in this chat, 2026-10-28: accept the recommended narrow
  hosted-assistant grant for recording Learning Session Reports.
- [Commissioned learning session protocol](../../docs/commissioned-learning-session-protocol.md).
- [Commissioned learning vocabulary](../../docs/adrs/0001-ubiquitous-language.md#commissioned-learning-session).
- [Feedback and scheduling](../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md#commissioned-learning-session).
- [Hosted assistant account linking](../../docs/assistant-oauth.md) — expand
  beyond today's `notes:read` only for report recording.
