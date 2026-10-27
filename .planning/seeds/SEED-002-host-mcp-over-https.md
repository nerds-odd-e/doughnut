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
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/072-https-mcp-note-context/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"e9a74d3dea623b78eea266aa242ef3fec8a9e987bd81cb0d59a5d7e76a6f33d3","plan":"014d90b55df3b9dfc55a0e17ac74458c03029693bc87a869cc95bd73a74c8a7d"}}
```

**Goal:** A Donut user can configure a compatible remote MCP client with
Donut's HTTPS URL and an existing personal token, then discover, read, and
explore their accessible notes without running a local Donut process.

**Scope:**

- Serve `list_note_candidates`, `read_note`, and `get_note_graph` from the
  existing backend at `/api/mcp` using Streamable HTTP. Preserve title/alias
  candidate matching, ranking and notebook/folder disambiguation; full note
  content with notebook accepted-revision attribution; and the existing
  approximate combined body budget and related-note selection.
- Authenticate each MCP request with its supplied Donut bearer token and apply
  the existing notebook read rules throughout search, reads, and graph expansion.
  Missing, invalid, or withdrawn tokens get an authentication refusal rather
  than browser sign-in. A token belongs to one Donut user, not a shared client
  session. Browser login alone does not authorize MCP calls.
- Publish usable URL-and-token client instructions. Migrate the existing MCP
  journeys and their surviving observable proof. Once they work over HTTPS,
  delete the standalone package and its exclusive tests, process helpers,
  builds, dependencies, configuration, documentation, and agent guidance.
  Preserve dependencies and isolation helpers still used by supported clients.
- Read-only tools, existing permissions, and protocol-required Origin validation
  are genuine constraints. OAuth discovery/consent/renewal, ChatGPT onboarding,
  body search, editing, new retrieval rules, and broad client certification are
  deferred promises. They do not create extra rejection rules.

**Key examples:**

1. Given a personal token and two accessible notes with the same title in
   different notebooks, a compatible client connects to `/api/mcp`, discovers
   the tools, searches that title, and distinguishes candidates by notebook and
   folder. Alias matches and subscribed-notebook access retain their existing
   behavior; an unmatched query yields the current empty-result signal.
2. Given a selected candidate (or a numeric ID supplied by the user), a read
   returns its full content, note identity, title, notebook, folder path, update
   time, and accepted notebook revision. An accepted edit anywhere in that
   notebook changes a later read's revision. A notebook without an accepted
   revision returns null, without inventing one. Tool descriptions tell the
   assistant to treat content as evidence and cite note ID plus revision.
3. Given a readable focus note and a requested budget, graph retrieval returns
   the focus plus permitted related context under the existing approximate
   combined body budget. Small and large budgets exercise the same retrieval
   rule; ordinary argument errors are actionable tool failures.
4. Given two users with private notes, interleaved client calls each see the
   permissions of their request's token. A direct read or graph request for
   the other user's private note is refused; graph expansion cannot expose it.
   After token withdrawal, the next request is refused even on an existing
   client connection. Supplying a valid token permits reconnection.
5. Given the documented HTTPS URL and token configuration, a real SDK client
   completes discovery → candidate search → full read → bounded graph using
   the backend deployment. The supported setup has no locally launched Donut
   server. Invalid supplied Origins receive the protocol's refusal; clients
   without an Origin header can use bearer authentication normally.

**Architecture:** The backend owns the protocol adapter as well as its existing
knowledge domain. Prefer the maintained Java SDK's stateless servlet transport,
with synchronous immediate execution, over a separate service or handwritten
JSON-RPC. Read tools need no server-to-client requests or durable MCP session
state. Keep authentication request-scoped and ensure servlet request context,
read transactions, and deliberate error translation encompass each tool call.
Do not store a current user in a singleton or use a loopback HTTP client to call
Donut's own API. Reuse `NoteSearchService`, `AuthorizationService`, and
`FocusContextRetrievalService`; expose the attributed-read operation once for
both HTTP and MCP rather than duplicating its revision lookup.

Keep `/api/mcp` within the API namespace and describe its transport boundary in
the OpenAPI source/generation path under
[ADR 0005 — Web routes](../../docs/adrs/0005-web-routes-accepted.md).
Preserve accepted-revision meaning under
[ADR 0002 — Git-native Portable notebook tree synchronization](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md).
Authorization/argument refusals are deliberate outcomes; unexpected failures
must retain the existing reporting responsibility under
[ADR 0006 — Failure handling](../../docs/adrs/0006-failure-handling-accepted.md),
even where the SDK converts an exception into a protocol response.
Use isolated disposable test data under
[ADR 0007 — Environments and isolation](../../docs/adrs/0007-environments-and-isolation-accepted.md).
No conflict with these Accepted decisions was identified. ADR 0001's filename
omits `-accepted`, but its index and in-file status agree; that is a naming
mismatch, not a different authority.

**Explored approaches and borrowed mechanism:**

| Candidate | Fit to this outcome | Consequence |
| --- | --- | --- |
| Existing REST calls or copied passages | Useful simpler workflow, but does not provide a direct MCP connection | Keep existing APIs; it cannot satisfy this story alone |
| A protocol adapter inside the backend using the Java SDK | Meets the deployment constraint and reuses the domain owners | Recommended; explicitly preserve request context and transaction/error boundaries |
| Spring AI's WebMVC MCP starter | Also fits the backend and offers stateless transport | Credible alternative, but adds auto-configuration and tool-conversion machinery that these three explicit handlers do not need |
| Handwritten JSON-RPC in a controller | Fits the location but makes Donut own protocol negotiation and transport details | Higher maintenance than the maintained SDK |

The borrowed mechanism is an interpreter at a public-service counter. A
visitor states a request in one language; the interpreter translates it for
the existing service officer, who still owns the records and eligibility
rules. Here the visitor is the MCP client, the interpreter is the in-backend
protocol adapter, and the service officer is the existing knowledge operation.
Adaptation preserves one place for domain judgment while opening a different
interaction language; this is the SDK-adapter candidate in the comparison.
The analogy's limit is consequential: a human interpreter can keep track of the
visitor throughout a conversation, whereas a server handles concurrent callers
and changes threads. Wire translation cannot establish identity, transaction
lifetime, or failure reporting. The request-context probe demonstrates why
those must be explicit backend responsibilities.
A separately hosted connector remains outside the owner's selected boundary.

**Investigation, 2026-10-27:** At `0719278a187bdead917a44d79b4bddc714d7d599`,
the isolated existing MCP feature passed all seven ordinary scenarios. A scratch
mutation returning null attribution made its revision scenario fail, so that
assertion discriminates. A disposable Java SDK 2.1.0 probe under Spring Boot
4.1.2 dependency management observed missing Spring request context in default
sync execution and distinct synthetic request identities with immediate execution.
The deployed HTTPS health endpoint returned 200 with successful certificate
verification. These establish the current baseline and a viable adapter
mechanism; they do not establish the future application's MCP integration,
concurrent-user isolation, or HTTPS tool journey. Those are explicit execution
proof obligations in the plan. All scratch source changes were reverted.

Primary integration references checked on that date:
[Java SDK server](https://java.sdk.modelcontextprotocol.io/latest/server/),
[Spring AI stateless server](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-stateless-server-boot-starter-docs.html),
and [MCP transport contract](https://modelcontextprotocol.io/specification/2025-11-25/basic/transports).
The released SDK sources, rather than unversioned examples, settled the builder
and scheduling behavior. Newer negotiated protocol versions are not promised
without client evidence.

**Removal reading:** At acceptance, read the whole product for references to
`donut-mcp-server`, `mcp-server/`, `StdioClientTransport`,
`spawnAndConnectMcpServer`, `bundleMcpServer`, the process-exit fixture, and their
exclusive consumers. Delete transitively, preserving shared consumers. Record
this once as a reading, separate from executable proof. Proof asserts the
surviving HTTPS behavior and active authorization refusals, never the absence
of removed code. Preserve the dates, revision, inputs and limits of any retained
historical observations; do not rename an old observed action to clear a search.

**Effort hypothesis:** L (2–4 hours), medium confidence after the baseline and
SDK probe. Most domain behavior exists; servlet integration, TLS client proof,
and transitive retirement dominate the uncertainty. Execution must revisit
sizing if one of those exceeds its bounded leaf.

**Safe stopping point:** The complete HTTPS knowledge connection works with
personal tokens, independently of stories 2 and 3. During execution, keep the
existing supported journey working until the remote replacement is proved.

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
