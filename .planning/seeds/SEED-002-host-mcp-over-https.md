---
id: SEED-002
status: dormant
planted: 2026-08-06
planted_during: idle (after deploy/static consistency audit)
updated: 2026-10-27
trigger_when: extending Donut through a direct ChatGPT plugin or migrating local MCP clients to the Donut HTTPS service
scope: large
---

# SEED-002: Use Donut knowledge through a direct ChatGPT connection

## Parent Problem

For Donut users who want an assistant to use their deliberately captured
knowledge, manually moving note contents or running a local MCP process should
change to an authenticated connection to Donut that can find, read, and explore
the user's accessible notes. ChatGPT should be able to use that connection
while the user's computer is off.

The owner selected this direction on 2026-10-27: serve MCP directly from the
existing Donut backend over HTTPS, let Donut authorize assistant access through
OAuth, retain GitHub as the user's Donut login, and connect the service to
ChatGPT as a plugin. This is non-executable decomposition and backlog input,
not permission to implement these stories.

## Why This Matters

The near-future direction prioritizes extending Donut's abilities through
ChatGPT. A direct connection makes the knowledge useful during ordinary
questions and tasks without asking the user to operate another service.

Repository reading on 2026-10-27 at
`8598fe004c6aff8adc2ca9d83cccf6e566c23ae2` establishes the starting point:

- The local MCP server offers title/alias candidate search, a full attributed
  note read, and bounded graph context through Donut's existing backend API.
- Donut already accepts personal bearer tokens and enforces notebook access.
- Donut's production Spring Security configuration consumes GitHub login;
  it does not issue OAuth grants to assistant clients.
- The existing backend already owns note search, content attribution, and
  focus-context retrieval. Keep those responsibilities there.

## Alternatives and Decision

- **Defer:** preserves the current manual and local-client workflows but does
  not advance the owner's selected ChatGPT direction.
- **Copy selected notes into a chat:** the strongest smaller alternative for
  an occasional task, but the user must choose and transfer every passage and
  later retrieve fresh content themselves.
- **Operate a client-side bridge or separately hosted connector:** rejected
  by the owner because the connection should be served by Donut itself.
- **Selected direction:** deliver the three usable outcomes below. First make
  the existing knowledge capabilities available to an HTTPS MCP client using
  a Donut token. Then enable account linking through GitHub login and Donut
  authorization. Finally prove and document the direct ChatGPT plugin journey.

The first story tests the highest current learning question: can a real MCP
client use the existing knowledge capabilities directly from Donut's HTTPS
service with correct attribution and user access?

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
- Complete migration includes deleting the standalone local MCP package and
  everything used only by it, with the whole-product reading described in
  story 1. Shared client or test dependencies stay where other callers need them.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including verification and
delivery, following [SEED-039](SEED-039-faster-ci-feedback.md#story-decomposition).
These are comparative hypotheses. Refinement must challenge either L story
if concrete learning makes it likely to exceed that band.

<a id="story-1"></a>

### Use Donut note context from an HTTPS MCP client

**Identity:** SEED-002#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** A Donut user can connect a compatible remote MCP client directly to
Donut, authenticate with an existing Donut token, and use the same note
discovery and context capabilities that local MCP clients have today.

- **For / why:** A Donut user configuring an IDE or assistant can supply a
  Donut HTTPS URL and token and immediately use their accessible knowledge.
- **Scope:** Deliver candidate search, full attributed note reads, and bounded
  graph context through Donut's HTTPS MCP service, using the existing backend
  domain behavior. Give users the client connection instructions. Migrate
  the current local-MCP journeys and their externally observable proof to
  that service. Once those capabilities work through HTTPS, delete the local
  MCP package, its exclusive tests and helpers, and its exclusive build,
  dependency, client-configuration, documentation, and agent-guidance material.
- **Evaluation:** Given two accessible notes with a matching title in different
  notebooks, the user connects a real compatible MCP client with their token,
  distinguishes the candidates, reads the chosen note with its accepted
  revision, and retrieves its context within the requested budget. Another
  user's private note remains protected by the existing read rule.
- **Removal reading:** At acceptance, perform one product-wide reading for
  orphaned local-server references across production code, tests, CLI/MCP
  integration, package and deployment configuration, docs, agent guidance,
  and generated artifacts. Record it as a reading, separate from executable
  proof. The retained proof exercises HTTPS note discovery, reading, context,
  and active access refusals.
- **Value / learning:** Existing MCP users gain a direct, usable connection;
  validate the backend protocol and identity boundary before adding account linking.
- **Effort hypothesis:** L, low confidence until refinement checks the supported
  Java MCP integration and the extent of exclusive local-client scaffolding.
- **Safe stopping point:** HTTPS MCP clients can use their existing Donut tokens
  without waiting for OAuth or ChatGPT integration. Normal notebook access and
  content attribution hold on every call.

<a id="story-2"></a>

### Link an assistant to Donut through GitHub sign-in

**Identity:** SEED-002#story-2
```json dough-story-dependencies
{"schemaVersion":1,"identity":"SEED-002#story-2","dependencies":[{"supplier":{"identity":"SEED-002#story-1","href":"seeds/SEED-002-host-mcp-over-https.md#story-1"},"implementation":"Build account linking against the existing direct HTTPS MCP knowledge service and reuse its user-scoped note operations.","rationale":"The selected OAuth journey must be exercised by a real client against Donut's actual protected MCP resource. Shared design or mocked tools cannot establish that resource's protocol, attribution, and current-user access boundary.","condition":"Donut's direct HTTPS MCP service has delivered candidate search, attributed full reads, and bounded graph context for a real authenticated client using existing Donut tokens.","state":"waiting"}]}
```
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** A Donut user can grant, maintain, and withdraw a compatible assistant's
read access by signing into Donut through GitHub, without manually copying a
Donut personal access token into that client.

- **For / why:** Users can link an account through a recognizable login and
  permission flow while keeping control over the assistant's access.
- **Scope:** Deliver the complete OAuth account-linking journey against the
  HTTPS MCP service: client discovery, GitHub-backed Donut login, informed
  read-access consent, credentials scoped to the user and service, authenticated
  tool use, expiry and renewal, and revocation. Retain the same Donut user and
  notebook permissions. A predefined OAuth client is sufficient for the first
  interoperable journey; broader client-registration policy awaits refinement.
- **Evaluation:** A user links a compatible MCP client, signs in through
  GitHub, grants read access, and reads one of their Donut notes. Later calls
  use the grant's access token without another browser login. Access renews
  when the token expires while the grant remains valid; after withdrawal,
  further access requires the user's authorization again.
- **Value / learning:** A real assistant can use Donut through account linking,
  independently of ChatGPT-specific packaging; establish the authorization
  contract with a real client before relying on ChatGPT's connection flow.
- **Effort hypothesis:** L, low confidence until refinement checks maintained
  Spring authorization-server support, existing login integration, persistence,
  and the smallest useful consent/revocation journey.
- **Safe stopping point:** Compatible OAuth MCP clients can connect securely.
  Users can refuse or withdraw access, and expiry cannot silently widen it.

<a id="story-3"></a>

### Use Donut knowledge directly in ChatGPT

**Identity:** SEED-002#story-3
```json dough-story-dependencies
{"schemaVersion":1,"identity":"SEED-002#story-3","dependencies":[{"supplier":{"identity":"SEED-002#story-2","href":"seeds/SEED-002-host-mcp-over-https.md#story-2"},"implementation":"Connect ChatGPT to the delivered Donut OAuth and HTTPS MCP service, including its existing credential lifecycle.","rationale":"ChatGPT's authenticated connection requires a reachable resource and a working authorization contract before its onboarding and tool-use journey can be evaluated. Metadata sketches, a local bridge, or a stub authorization server would evaluate a different connection.","condition":"Donut has delivered and verified GitHub-backed OAuth account linking, user-scoped read access to its HTTPS MCP tools, token renewal, and revocation with a compatible client.","state":"waiting"}]}
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

Refinement must settle the supported backend MCP and OAuth library integration,
client-registration policy, token lifetime/renewal policy, and the smallest
useful consent and revocation interface. These details affect sizing; no
executable plan or readiness assessment is supplied here.

## Open Decisions

Whether a public-directory release is worth its publication and review work
remains an owner decision after the direct custom-plugin journey is useful.
The three selected outcomes do not require that expansion.

## When to Surface

Use this seed when selecting direct HTTPS MCP access, assistant account
linking, or the ChatGPT extension of Donut.

## Breadcrumbs

- [Product backlog](../PRODUCT-BACKLOG.md)
- [Learning, refinement, and retrieval](SEED-073-learning-refinement-and-retrieval.md)
- [Knowledge-to-capability experiment](SEED-074-knowledge-to-capability.md)
- [OpenAI MCP server guidance](https://developers.openai.com/plugins/build/mcp-server)
- [OpenAI account-linking guidance](https://developers.openai.com/plugins/build/auth)
