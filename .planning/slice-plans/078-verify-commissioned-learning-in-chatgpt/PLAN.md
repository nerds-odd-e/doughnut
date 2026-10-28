# Verify a commissioned learning session directly in ChatGPT

**Source:** [Verify a commissioned learning session directly in ChatGPT](../../seeds/SEED-090-commissioned-learning-via-mcp.md#story-2)
**Identity:** SEED-090#story-2

## Goal and scope

Observe Terry's real ChatGPT OAuth connection requesting a commissioned
Learning Session and recording Feedback in Donut, including a partial session,
immediate end, and no available session. Retain reproducible operator steps in
[the existing guide](../../../docs/assistant-oauth.md) only after observing them.
The source owns the complete examples, boundaries and evidence requirements.
This plan verifies existing behavior; an interoperability failure stops the
dependent path for refinement rather than authorizing implementation repairs.

Deployment, new real-client activation, notebook selection, automatic further
sessions, another protocol, general editing and additional clients are deferred.
SDK or personal-token results cannot replace any ChatGPT observation below.

## Existing solution and constraints

Reuse `request_learning_session(timezone)` and
`record_learning_session(notebookId, reportMarkdown)` in
`backend/src/main/java/com/odde/donut/mcp/`. They delegate
`LearningSessionService`; the learner inspects Feedback on the memory tracker's
recall-history page. The report tool requires `learning_session:record` for
hosted assistants and reports `recordedItems` and `rejectedEntries`.

The PFE decision is direct reuse of the existing tools, OAuth consent, markdown
protocol and history UI. No harness, new domain representation or product-code
change is planned. Follow [ADR 0001 — Ubiquitous language](../../../docs/adrs/0001-ubiquitous-language.md#commissioned-learning-session)
and [ADR 0003 — Spaced-repetition scheduling policy](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md#commissioned-learning-session):
Feedback contains Grade and descriptive text; another Report is another Grade.
Do not replay a successful Report to collect evidence. Follow
[ADR 0007 — Environments and isolation](../../../docs/adrs/0007-environments-and-isolation-accepted.md):
identify the environment and owner before operating; production test controls
and resets are prohibited. The North Star has no topic governing this
verification; no architectural change or ADR exception is proposed.

## Decisive premises and prior evidence

Preparation checkout:
`/Users/terryyin/git/doughnut/.worktrees/refine-commissioned-learning-chatgpt`,
branch `codex/refine-commissioned-learning-chatgpt`, prepared from
`0a1b38a49584d7135413c0d3dfc3dd2bb7f8cb4c` on 2026-10-28.
This checkout identifies preparation evidence, not the deployment to be tested.

- The original implementation's accepted MCP/OAuth/SDK observations are retained
  at `75fece3754879b7d3f840b002d58adfa7beafc91:.planning/slice-plans/077-commissioned-learning-session-in-chatgpt/execution-evidence.md`.
  On 2026-10-28 the final backend run passed 2,981 tests; the record names
  `LearningSessionRecordMcpEndpointTest` for A/B/C and empty Reports and
  `AssistantLearningSessionTest` for consent-issued dual-scope recording.
  These are historical implementation observations, not a new test run or
  ChatGPT acceptance evidence.
- That record's credentialed attempt on 2026-10-28 observed production at
  `ff368082aedaf3c1ca8cf2449298e8ef15eb28f8`, advertising only `notes:read`.
  Its CUA browser host was unavailable and access to `com.openai.codex` was
  refused. No UI or credential action occurred. This is dated evidence, not a
  claim about the current deployment or current tool access.
- The actual HTTPS deployment/revision, registered client, permitted ChatGPT
  surface and acceptance of both scopes remain unobserved. Slice 1 is the
  permitted early, owner-operated probe: establish these before slices 2–5.
  No deploy, grant, connection or runtime success is inferred from repository
  files. Terry supplies the existing environment and uses the connection UI;
  no agent workaround for a refused UI surface is required.

## Proof ownership and observation contract

| Source promise | Owner | Required observation |
| --- | --- | --- |
| Real ChatGPT connection requests both scopes and informed consent permits recording | 1, confirmed by 2 | Actual scope string and consent, then successful request and recording through that connection |
| Japanese's Request, timezone and tutoring instructions lead to saved Feedback | 2 | Request/tool transcript, interactive tutoring, Report response and matching Donut history |
| Ending after A saves A while B/C retain history and schedule | 3 | A/B/C Request, completed-only Report, recorded result and before/after learning state |
| Immediate end submits an empty Report and records nothing | 4 | Empty Report call/result and unchanged baseline for all session items |
| No due session is explained in ChatGPT | 5 | No-session tool result and ChatGPT's response |
| Evidence is attributable and guide steps were observed | Each slice | Dated origin/revision, preconditions, tool/UI observations, limitations and corresponding guide edit |

Terry operates every credentialed round. At execution's first request, provide
the whole bundle of rounds below once; reuse answered setup and earlier
observations rather than asking again between rounds. Request the HTTPS origin,
deployed revision, environment/owner and usable registered connection then.
Record the account reference without credentials and arrange each round's due
items through supported operations on owner-approved data. Save the baseline
before each write; baseline data is setup, while the actual ChatGPT call and
Donut result are the observation. A notebook used in an example imposes no
product limit on how many notebooks may exist.

For each round append retained proof to this plan: promise, covered boundary,
date/revision and inspected setup, actual owner-performed steps, observation
locations, result and limitations. Include literal tool inputs/outputs and
before/after Feedback and schedule values where promised. Redact secrets, codes
and credentials. A prose success claim without the tool/history observations
leaves that proof open. Persist lasting setup/usage facts in the guide before
eventual story-history cleanup.

## Ordered slices

Each leaf targets about five minutes of agent preparation, evidence review and
local cleanup. Terry's credential entry, environment handoff and real tutoring
responses are external waits; record them separately. If active work exceeds
ten minutes, stop and finer-decompose unless the recorded external-wait
exception explains it. Run one round per example; a failure retains its
evidence and stops affected follow-ups rather than starting a repair loop.

### 1. Authorize the real ChatGPT connection for report recording
Type: Behavior
Status: planned
Proof: Terry's actual setup and consent observations, requested scope string,
connection result, and visibility of the Request/Report tools. Slice 2 owns
functional confirmation that this same connection retains recording authority.

Behavior: Given an existing authorized HTTPS deployment and registered client,
Terry links the intended Donut account in ChatGPT with `notes:read` and
`learning_session:record` and sees consent describing report recording.

Probe: First record origin/revision and inspect public authorization/resource
discovery for both scopes. Use the current permitted ChatGPT interface and its
actual callback; an existing read-only grant needs fresh authorization. Record
the observed setup, consent wording and tool availability, updating only those
guide steps established by the observation. Missing candidate deployment,
registration, suitable data or permitted access stops here with the exact
prerequisite and owner. Failure to request/retain either scope stops dependent
rounds and returns the interoperability question for refinement. No deployment
or client activation is part of this probe.

### 2. Complete the Japanese session and inspect saved Feedback
Type: Behavior
Status: planned
Proof: Real ChatGPT Request → tutoring → Report transcript plus matching Grade
and descriptive Feedback in Donut recall history for each completed item.

Behavior: With Japanese the sole notebook having a Potential learning session,
Terry asks ChatGPT to start commissioned learning with the learner's recorded
IANA timezone. Inspect the returned notebook identity, Session Items and
instructions; ChatGPT waits for the learner to start and tutors from that
Request. Complete the items and finish. Inspect the actual
`record_learning_session` call for the returned notebook, its recorded/rejected
result, and the corresponding new Feedback in Donut. This completes slice 1's
functional scope probe. Add the observed start/finish and Feedback-review steps
to the guide. If recording fails, retain the partial journey and stop the
dependent recording rounds; it does not establish full-session success.

### 3. End after A and preserve B/C's learning state
Type: Behavior
Status: planned
Proof: A/B/C in the actual Request, A only in the Report/recorded result, A's
new Feedback in history, and B/C's history and schedule matching their baseline.

Behavior: Arrange due commissioned items A/B/C with distinct titles and capture
their history and schedule. Terry obtains their Request through ChatGPT,
completes A, then asks to end before B/C. ChatGPT reports A's Grade/text only;
compare Donut's actual state with the baseline. Add the observed early-end
instruction and outcome to the guide. Do not manufacture the observation by
submitting a Report through another client.

### 4. End immediately with an empty Report
Type: Behavior
Status: planned
Proof: A real ChatGPT call to `record_learning_session` with empty
`session_item_feedback`, zero recorded items in its result, and unchanged
history/schedule for every item against the pre-call baseline.

Behavior: With an available Request and a saved baseline, Terry asks to end
before completing any item and submit an empty Learning Session Report.
Observe the empty call/result and Donut state. A skipped recording call alone
does not observe the promised empty-Report path. Add only the observed behavior
to the guide.

### 5. Explain that no commissioned session is available
Type: Behavior
Status: planned
Proof: `request_learning_session` returns the explicit no-session result for
the recorded timezone and ChatGPT explains that outcome to Terry.

Behavior: With no due commissioned items in the connected account, Terry asks
ChatGPT to start learning. Inspect the actual tool result and conversation
response. Use naturally eligible data or supported owner operations; do not
reset persistent data to create this precondition. Add the observed no-session
response to the guide and review the final guide against retained observations.

## Current decisions and remaining concerns

- One coherent Request/Report protocol and one Feedback model serve every
  example. Slices separate independently observable operator rounds, with
  authorization first to bound the external interoperability risk.
- No additional automated tests or API generation are planned for evidence and
  guide edits. Reuse accepted implementation proof only for its stated boundary.
  Product changes discovered as necessary require refinement before execution;
  they bring their own stack proof and delivery requirements.
- Execute Plan owns the required fresh refactor review, formatting once,
  commit and push after authorized changes. Preparation does not perform the
  manual rounds or mark any slice done.
- Slice-plan refinement was not needed: five bounded rounds have distinct
  proof loops and the uncertain external prerequisites are in the first probe.
  No remaining slice-specific concern was identified beyond that permitted
  credentialed probe; its failure leaves the dependent observations open.
