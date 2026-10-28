---
id: SEED-002
status: dormant
planted: 2026-08-06
planted_during: idle (after deploy/static consistency audit)
updated: 2026-10-28
trigger_when: extending Donut through a direct ChatGPT plugin
scope: large
---

# SEED-002: Use Donut knowledge through a direct ChatGPT connection

## Parent Problem

Donut users should connect an assistant to their deliberately captured knowledge
for authenticated search, reading and exploration while their computer is off.
On 2026-10-27 the owner selected direct HTTPS MCP from Donut's backend, Donut
OAuth authorization, existing GitHub login, and a ChatGPT plugin. This is
non-executable decomposition/backlog input, not implementation authority.

## Alternatives and Decision

- Defer: retains manual/local workflows but does not advance the selected goal.
- Copy notes: the strongest smaller option; users must select, transfer and refresh passages.
- Client bridge or separate connector: owner rejected; Donut should serve the connection.
- Selected order: token-authenticated HTTPS MCP → GitHub/Donut account linking → direct ChatGPT plugin.

Real MCP interoperability must retain user access and attribution.
The [hosted assistant account-linking guide](../../docs/assistant-oauth.md) describes the delivered authorization contract and operator configuration.

## Shared Constraints

- The MCP service and OAuth authorization capability belong in the existing
  Donut backend and use its ordinary HTTPS deployment. The connection should
  work while the user's computer is off.
- GitHub remains the user's login provider. Donut grants access to Donut
  knowledge and issues the assistant's credentials; each request resolves
  the authorizing Donut user and follows existing notebook permissions.
- The first release is read-only: candidate search, attributed full reads,
  and bounded graph context. Preserve current title/alias search semantics;
  body search and knowledge editing remain separate outcomes.
- Browser sign-in and consent belong to account linking. Ordinary MCP calls
  carry access tokens. Token expiry, renewal, and withdrawal of permission
  must preserve the author's control of access.
- Use the existing backend search, note attribution, and focus-context
  responsibilities. Retrieved note contents are evidence, distinct from
  application instructions; citations identify the note and its accepted
  notebook revision.
- Follow [ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
  for accepted content and revision attribution,
  [ADR 0005](../../docs/adrs/0005-web-routes-accepted.md) for the API boundary,
  and [ADR 0007](../../docs/adrs/0007-environments-and-isolation-accepted.md)
  for observation and test environments.
- The supported MCP surface is the backend HTTPS `/api/mcp` service. Shared
  client or test dependencies stay where other callers need them.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours including verification and delivery,
following [SEED-039](SEED-039-faster-ci-feedback.md#story-decomposition).
Refinement challenges an L story likely to exceed those comparative bands.

<a id="story-3"></a>

### Use Donut knowledge directly in ChatGPT

**Identity:** SEED-002#story-3
```json dough-story-dependencies
{"schemaVersion":1,"identity":"SEED-002#story-3","dependencies":[{"supplier":{"identity":"SEED-002#story-2","href":"seeds/SEED-002-host-mcp-over-https.md#story-2"},"implementation":"Connect ChatGPT to the delivered Donut OAuth and HTTPS MCP service, including its existing credential lifecycle.","rationale":"ChatGPT's authenticated connection requires a reachable resource and a working authorization contract before its onboarding and tool-use journey can be evaluated. Metadata sketches, a local bridge, or a stub authorization server would evaluate a different connection.","condition":"Donut has delivered and verified GitHub-backed OAuth account linking, user-scoped read access to its HTTPS MCP tools, token renewal, and revocation with a compatible client.","state":"satisfied","resolution":{"revision":"5a83d8d3589f81de67ee2ac3e40177ee633caa80","path":".planning/seeds/SEED-002-host-mcp-over-https.md#story-2","summary":"SEED-002#story-2 delivered the existing GitHub login/identity mapping into informed Donut consent and S256 authorization-code exchange for a registered confidential hosted client. Actual maintained SDK over verified HTTPS discovers metadata, reads attributed private notes and bounded graph context, renews after real access expiry with replacement credentials, loses MCP and refresh access after Settings withdrawal, and reconnects through fresh consent. Final composed hosted1 plus ordinary personal-token9 scenarios pass10/10; all2961 backend tests pass with no failures/errors and2 existing skips, including two-user/current-permission/read-only/resource/scope/CSRF/atomic withdrawal and persistent JDBC reload boundaries. The last implementation repair c46126a57cecf9cd3b56ce40ff9b6a56953252dc passed CI40599762327/attempt1. Before-cleanup PLAN.md and EXECUTION-EVIDENCE.md/HOSTED-SDK-EVIDENCE.md at this revision retain all18 completed leaves, original source qualifications, physical restart proof and literal observations. The whole supplier condition is fulfilled with a compatible hosted SDK client; actual ChatGPT-specific onboarding/compatibility and separately authorized deployment remain consumer/operator work. No new GitHub provider capability or test app was selected. The original consumer agreement and scope are unchanged.\nCondition satisfied: Donut has delivered and verified GitHub-backed OAuth account linking, user-scoped read access to its HTTPS MCP tools, token renewal, and revocation with a compatible client.\nAccepted integration: 61f0d210d1b53200cf6ac040c56fc83fed70385f on origin/refs/heads/main."}}]}
```
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** A Donut user can create or install a Donut ChatGPT plugin, link their
account, and ask ChatGPT to find and use current Donut notes through the direct
HTTPS connection.

- **For / why:** Donut users can apply their authored knowledge during ordinary
  ChatGPT work while their own computer is off.
- **Scope:** Complete and document the ChatGPT plugin onboarding and real
  account-linking journey against Donut's service. Present accurate read-only
  tools and usable source attribution so ChatGPT can find a candidate, read
  its full contents, and use its graph context when relevant. Prove account
  isolation, ordinary token renewal, and actionable reconnection after access
  is withdrawn. Use the server outcomes already owned by stories 1 and 2.
- **Evaluation:** A user follows the plugin instructions, signs into Donut
  through GitHub, approves access, and asks about a known topic. ChatGPT
  searches the user's accessible notes, reads the selected evidence, and
  answers with inspectable note/revision attribution. Repeat the journey
  with the user's computer off. A second Donut account sees only the notes
  its own grant permits.
- **Value / learning:** Demonstrate the owner's requested ChatGPT outcome,
  including real tool selection and attribution rather than protocol success alone.
- **Effort hypothesis:** M, medium confidence assuming the preceding service
  and OAuth journeys interoperate; ChatGPT account availability and current
  connection requirements must be checked at refinement.
- **Safe stopping point:** Users can connect their own account through a
  personal/custom plugin. A public-directory listing is a separate product
  decision and is not required for this outcome.

## Ordering and Scope Reduction

The order delivers an immediately usable remote knowledge service, then
user-controlled account linking, then the specific ChatGPT experience. Keep
each supplier's working outcome available to compatible clients even if a
later story is cancelled. Necessary blocking supplier agreements belong in
the consumers' canonical sections and must be recorded with the backlog tool.

If scope must shrink, defer public-directory distribution, editing tools,
custom ChatGPT UI, and broader search before changing the three read
capabilities or the user's access controls. Those expansions are not queued
by this decomposition.

## Refinement Handoff

Refinement checks current ChatGPT connection requirements, onboarding and attribution against the delivered HTTPS MCP and OAuth service. This decomposition provides no executable plan or readiness.
Public-directory publication remains an owner decision after a useful direct custom plugin.

## When to Surface

For direct HTTPS MCP, assistant linking or ChatGPT, see [backlog](../PRODUCT-BACKLOG.md), [learning/refinement/retrieval](SEED-073-learning-refinement-and-retrieval.md),
[knowledge-to-capability](SEED-074-knowledge-to-capability.md),
[OpenAI MCP](https://developers.openai.com/plugins/build/mcp-server) and
[account linking](https://developers.openai.com/plugins/build/auth).
