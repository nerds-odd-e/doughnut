# Verify a commissioned learning session directly in ChatGPT

**Source:** [Verify a commissioned learning session directly in ChatGPT](../../seeds/SEED-090-commissioned-learning-via-mcp.md#story-2)
**Source review:** [Independent source review](source-review.md)
**Identity:** SEED-090#story-2

## Goal and scope

Observe Terry's real ChatGPT OAuth connection requesting a commissioned
Learning Session and recording Feedback in Donut, including immediate end.
Terry's closure amendment waives the partial and no-session observation rounds;
neither waived example is claimed as verified. Retain reproducible operator steps in
[the existing guide](../../../docs/assistant-oauth.md) only after observing them.
The source owns the complete examples, boundaries and evidence requirements.
The original verification-only boundary was expanded by Terry on 2026-10-29 to
repair Donut's OAuth integration and release the tested correction.

New client activation, notebook selection, automatic further sessions, another
protocol, general editing and additional clients remain deferred; SDK/personal-token
results cannot replace ChatGPT observations.

## Existing solution and constraints

Reuse `request_learning_session(timezone)` and
`record_learning_session(notebookId, reportMarkdown)` in
`backend/src/main/java/com/odde/donut/mcp/`. They delegate
`LearningSessionService`; the learner inspects Feedback on the memory tracker's
recall-history page. The report tool requires `learning_session:record` for
hosted assistants and reports `recordedItems` and `rejectedEntries`.

The PFE decision is direct reuse of the existing tools, OAuth consent, markdown
protocol and history UI. No harness or new domain representation is needed. Slice 6
corrects the existing tool metadata and recording-permission challenge. Follow [ADR 0001 — Ubiquitous language](../../../docs/adrs/0001-ubiquitous-language.md#commissioned-learning-session)
and [ADR 0003 — Spaced-repetition scheduling policy](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md#commissioned-learning-session):
Feedback contains Grade and descriptive text; another Report is another Grade.
Do not replay a successful Report to collect evidence. Follow
[ADR 0007 — Environments and isolation](../../../docs/adrs/0007-environments-and-isolation-accepted.md):
identify the environment and owner before operating; production test controls
and resets are prohibited. The North Star has no topic governing this
verification; no architectural change or ADR exception is proposed.

## Decisive premises and prior evidence

[Preparation provenance and prior observations](execution-evidence.md#decisive-premises-and-prior-evidence).

## Proof ownership and observation contract

[Source mappings and the retained observation protocol](execution-evidence.md#proof-ownership-and-observation-contract).

## Ordered slices

Each leaf targets about five minutes of agent preparation, evidence review and
local cleanup. Terry's credential entry, environment handoff and real tutoring
responses are external waits; record them separately. If active work exceeds
ten minutes, stop and finer-decompose unless the recorded external-wait
exception explains it. Run one round per example; a failure retains its
evidence and stops affected follow-ups rather than starting a repair loop.

### 6. Advertise recording scopes and challenge a read-only connection
Type: Behavior
Status: done
Proof: Reproduce missing recording auth metadata/challenge through the real MCP
endpoint, then pass the complete backend suite. The wire descriptor declares
`notes:read` and `learning_session:record` for recording in primary
`securitySchemes` and its `_meta.securitySchemes` mirror; a read-only OAuth call
returns a structured insufficient-scope challenge with the canonical resource
metadata URL and both required scopes while preserving learning state. Existing
dual-scope recording, personal-token access and notebook authorization retain
their accepted behavior. Update setup instructions to request both scopes and
identify the actual Donut consent screen, distinguishing unobserved ChatGPT steps.

Behavior: Use the existing MCP tool, OAuth scope enforcement and canonical-origin
configuration. This is the bounded correction authorized by Terry's 2026-10-29
instruction. No new authorization protocol, general editing or silent scope
upgrade. Follow ADR 0005's existing API/discovery routes and ADR 0006's business
outcome rule: insufficient permission becomes an actionable OAuth challenge.
Agent active implementation targets five minutes; complete backend-suite and
required release CI waits are external proof time, recorded separately. If SDK
support or scope selection requires broader work, refine this leaf before expanding.

### 7. Release the tested OAuth correction
Type: Behavior
Status: done
Proof: Corrected source integrated on main, successful applicable CI and required
artifacts, next increasing immutable stable tag, Application Release publication
job success, and production smoke identifying the selected build and corrected
MCP wire metadata. Release success does not accept slices 1–5.

Behavior: Apply the existing release runbook under Terry's explicit release
authority. Choose the next unused increasing patch version and the exact tested
main commit. Preserve the execution checkout, branch, assignment and unfinished
verification while publishing this necessary increment. No database change or
new client activation is planned. Then resume the already-authorized empty-Report
check through the connected plugin; any required consent stays informed.

### 1. Authorize the real ChatGPT connection for report recording
Type: Behavior
Status: done
Proof: Actual connection setup and consent observations, requested scope string,
connection result, and visibility of the Request/Report tools. Slice 2 owns
functional confirmation that this same connection retains recording authority.

Behavior: Given an existing authorized HTTPS deployment and registered client,
the Donut plugin in this chat is connected to the intended account with
`notes:read` and `learning_session:record`, with observed consent describing
report recording. Reuse that connection when its grant is established; Terry
handles fresh authorization when it is needed.

Probe: First record origin/revision and inspect public authorization/resource
discovery for both scopes. Use the plugin tools exposed in this chat; opening a
separate ChatGPT app is unnecessary. If fresh authorization is needed, use the
connection's actual callback; an existing read-only grant needs both scopes. Record
the observed setup, consent wording and tool availability, updating only those
guide steps established by the observation. Missing candidate deployment,
registration, suitable data or permitted access stops here with the exact
prerequisite and owner. Failure to request/retain either scope stops dependent
rounds and returns the interoperability question for refinement. No deployment
or client activation is part of this probe.

### 2. Complete the Japanese session and inspect saved Feedback
Type: Behavior
Status: done
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

### 4. End immediately with an empty Report
Type: Behavior
Status: done
Proof: A real ChatGPT call to `record_learning_session` with empty
`session_item_feedback`, zero recorded items in its result, and unchanged
history/schedule for every item against the pre-call baseline.

Behavior: With an available Request and a saved baseline, Terry asks to end
before completing any item and submit an empty Learning Session Report.
Observe the empty call/result and Donut state. A skipped recording call alone
does not observe the promised empty-Report path. Add only the observed behavior
to the guide.

## Execution complete

Product advice: no change. The owner explicitly waives the two unobserved examples.
The product retrospective found no required correction after inspecting the narrow
metadata projection, shared scope owner, consent/old-token tests, existing SDK/E2E
journey and real native saved-Feedback proof. No tests reran for record cleanup.
Process review was enabled by `.planning/open-dough.json`; the four records-only
refactor returns are recorded as another ODF-141 occurrence in `DearDough.md`.
Related code revisions are 358ad5c and 8e90688; 3bcf245, 6cc7f56, 4b5780c,
64022c1 and bd9dde8 are release/manual-evidence provenance. Unrelated trunk work
is excluded. Accepted code CI and deployment remain distinct from pending closure CI.

## Current decisions and remaining concerns

- Slice 6's primary declaration is proved and deployed in `v1.3.62`.
  The existing client's stale scope allowlist was corrected; informed consent
  and the native empty Report now pass. Full tutoring and the returned Report
  match saved Feedback; G5 is proved. See [full-round proof](full-session-evidence.md).

- One Request/Report protocol and Feedback model serve all independently
  observable rounds; authorization first bounds the interoperability risk.
- Evidence and guide edits reuse accepted implementation proof only for its
  stated boundary. Slice 6 adds MCP/OAuth regression tests; no API generation is
  needed. Further product changes require refinement before execution and bring
  their own stack proof and delivery requirements.
- Execute Plan owns fresh refactor review, formatting once, commit and push;
  preparation alone does not accept manual observations.
- Original preparation had five observation rounds; Terry's 2026-10-29 amendment
  added repair/release slices 6–7. Full-session response capture matches saved
  Feedback. Terry's explicit closure amendment skips slices 3 and 5; their examples
  remain unobserved, and those leaves are removed from the required execution.

## Retained observations

- [Discovery and the first empty-Report probe](execution-evidence.md).
- [Reauthorization, retries and observed read-access consent](connection-evidence.md).
- [Accepted MCP/OAuth correction proof](oauth-correction-evidence.md).
- [Real authorization, empty Report and unchanged state](fresh-chat-evidence.md).
- [Completed Japanese tutoring and saved Feedback](full-session-evidence.md).

## Execution context

- 2026-10-29 continuation fast-forwarded to `c51f7b619a` from origin/main with
  local evidence preserved and locked setup passed. The source-only review includes
  Terry's repair/release amendment; fresh-chat proof observes authorization/empty end.
- Closure CI observer is bound to authorized trunk `main`, workflow 5208694
  **donut CI**, repository `nerds-odd-e/doughnut`, coordinator `aino-seed090`,
  directory `/tmp/dough-ci-501/watch-G36plk`, PID 22008, PTY 65371, cell 204.
  Runtime/checkout identities match. Earlier branch observer watch-ll1wzy stopped
  with zero unread events and confirmed exit; its documentation CI is unobserved.
  Final integration requires its own successful main completion/shutdown receipt.

- Story Branch Mode; identity `SEED-090#story-2`, publisher
  `codex-seed090-chatgpt-verification`, assigned agent Aino-chan.
- Execution checkout `/Users/terryyin/git/doughnut/.worktrees/verify-commissioned-learning-chatgpt`,
  branch `codex/verify-commissioned-learning-chatgpt`, from
  `7001846a8d141d6bbc48b48b0669c0db542bbe51`.
  Originating/integration checkout: `/Users/terryyin/git/doughnut`.
- Take accepted on `origin/main` and the execution branch at
  `4ef50afd67cdb5f3fe91242e30f41016d8d240d6`; increments target
  `origin` / `refs/heads/codex/verify-commissioned-learning-chatgpt`.
[Historical connection status](execution-evidence.md#historical-connection-status-before-the-oauth-repair) remains in the observations.

## Story obligations

### G4. Primary recording-scope declaration
Reported: slice 6 — "The deployed descriptors declare scopes in _meta.securitySchemes but omit the documented primary securitySchemes field; Terry still observed read-only consent after disconnecting and allowing the new connection."
Story clause: "Correct the existing integration's declaration of required recording scopes"
Disposition: proved by slice 6: oauth-correction-evidence.md primary descriptor correction; current servlet primary/mirror equality and standard SDK decoding proof

### G1. Recording authority after tool discovery (first reported in slice 1)
Reported: slice 1 — "The available Donut tool inventory in this chat contains only list_note_candidates, read_note and get_note_graph; request_learning_session and record_learning_session are unavailable."
Story clause: "resulting connection can both request and record a session."
Disposition: proved by slice 1: fresh-chat-evidence.md matching native Request and successful empty Report after informed dual-scope consent; completed-item recording remains owned by slice 2

### G3. The reauthorized plugin must remain callable
Reported: slice 1 — "After Terry reported reauthorization done, record_learning_session, request_learning_session and list_note_candidates all returned Unknown tool from the plugin runtime, while direct production tools/list still returned all five tools."
Story clause: "resulting connection can both request and record a session."
Disposition: proved by slice 1: fresh-chat-evidence.md fresh conversation's completed native record_learning_session call with zero items and zero rejections; historical stale-chat failures retained

### G2. Recording consent must be observed
Reported: slice 1 — "Terry answered I did not check the wording when asked whether Donut consent explicitly mentioned permission to record Learning Session Reports."
Story clause: "observe Donut consent describing report recording"
Disposition: proved by slice 1: fresh-chat-evidence.md parent-inspected recording-consent screenshot followed by Terry's explicit I clicked Allow observation

### G5. Capture the full round’s returned Report result
Reported: slice 2 — "The thread reader exposes the completed native Report call and exact arguments but hides its structured recordedItems/rejectedEntries response. Matching Grade and descriptive Feedback are saved and visible."
Story clause: "Compare the returned recording result with actual Feedback in Donut recall history"
Disposition: proved by slice 2: full-session-evidence.md original result supplied by Terry; sole Grade-2 item and tracker match the native Report and saved log 139509 at the exact recordedAt instant, with zero rejections
