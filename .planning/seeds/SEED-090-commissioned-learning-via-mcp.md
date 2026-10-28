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
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/077-commissioned-learning-session-in-chatgpt/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"ef60098724cec238c4677c940ecc980a66b255636418bae3060304ba3e7d7242","plan":"d08b9a7b1af206b7f6601ddf8f39ab2d5d373ec860ce59acaab95191c3e081ef"}}
```

- **Goal:** A Donut learner can obtain the first available commissioned Learning
  Session Request and record Feedback for completed Session Items through MCP,
  including a partial or empty Report.
- **Scope:** Expose the existing Request/Report markdown protocol through MCP.
  Select the first available Potential learning session in Donut's due order,
  return its notebook-specific instructions and Session Items, and record only
  the Report's completed items through existing Feedback recording and scheduling.
  Preserve incomplete items' learning state. Return an explicit no-session
  outcome when none is available. Expand hosted-assistant OAuth and consent with a narrow grant for recording Learning Session Reports
  (`learning_session:record`) alongside `notes:read`; refuse recording under
  `notes:read` alone and grant no general note editing. Tutoring instructions
  direct the connected assistant to submit completed items on normal or early
  finish without automatically starting another session.
- **Key examples:**
  1. **Japanese Request:** With one due Japanese notebook, MCP returns that
     notebook's identity, instructions, Session Items and Report format.
  2. **Several actions:** With several due notebooks, MCP selects the first in
     Donut's returned due order.
  3. **End early:** A Request contains A/B/C. A Report for A records its Grade
     and descriptive Feedback; B/C retain their histories and schedules. An
     empty Report records nothing.
  4. **Nothing available:** MCP returns an explicit no-session outcome.
  5. **Narrow recording grant:** Real OAuth consent/code exchange grants both
     scopes; an assistant can record only with `learning_session:record`.
- **Deferred promises:** Real ChatGPT interoperability, interactive tutoring,
  and visible history verification are owned by SEED-090#story-2. Choosing among
  programs/notebooks, automatically starting another session, replacing markdown
  with MCP-only schemas, general note editing and `notes:write` remain deferred.
- **Rejection constraints:** Preserve existing notebook authorization and Report
  matching/recording rules, including reported unmatched entries/invalid grades.
- **Value / learning:** Make the existing learning protocol available to a
  connected assistant with narrowly authorized progress recording.
- **Safe stopping point:** The MCP Request/Report and OAuth boundaries are
  implemented and verified; live ChatGPT verification is a separate story.

**Owner amendment (2026-10-28):** Split former execution slice 4 into a new
first-priority backlog story. The owner will perform that probe directly from
ChatGPT; finish execution and wrap up this MCP capability story now. The
amendment changes the current story's proof boundary; it does not claim the
live ChatGPT journey has been observed.

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

<a id="story-2"></a>

### Verify a commissioned learning session directly in ChatGPT

**Identity:** SEED-090#story-2
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"unselected"}
```

- **Goal:** A learner using Donut in ChatGPT can complete a commissioned
  Learning Session and see Feedback saved in Donut, including when ending early.
- **Scope:** The owner operates a real ChatGPT connection to an HTTPS deployment
  containing the MCP Request/Report tools. Request `notes:read` and
  `learning_session:record`, observe consent describing recording, retrieve the
  first Request, tutor from its instructions, and submit a Report for completed
  items only. Verify Feedback in learning history and incomplete items unchanged.
  Observe the no-session response and an empty Report when ending immediately.
  Record origin/revision, actual scope string, consent wording, tool calls,
  history observations and limitations. Update the hosted-assistant guide only
  with steps actually observed. If ChatGPT cannot request or retain both scopes,
  retain the evidence and refine the remaining interoperability work.
- **Key examples:** Japanese is the sole available notebook and its completed
  items receive Feedback; with A/B/C, ending after A records A only; ending
  immediately records nothing; with no due session, ChatGPT explains none exists.
- **Exclusions:** Personal-token-only or SDK-fixture proof cannot establish this
  ChatGPT journey. Program choice, automatic further sessions, a replacement
  protocol and general note editing remain outside scope.
- **Owner / operation:** Terry will conduct the credentialed probe directly in
  ChatGPT. Environment and permitted UI access are setup prerequisites; no
  credentials belong in prompts or source control.
- **Provenance:** Moved from former slice 4 of the commissioned-learning MCP
  execution by owner instruction on 2026-10-28. Existing MCP/OAuth automated
  proof is reusable; the real ChatGPT journey remains unobserved.
  Original probe contract: `75fece3754879b7d3f840b002d58adfa7beafc91:.planning/slice-plans/077-commissioned-learning-session-in-chatgpt/PLAN.md` (former slice 4).

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
