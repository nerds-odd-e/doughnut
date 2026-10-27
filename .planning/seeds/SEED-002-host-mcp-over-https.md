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

<a id="story-2"></a>

### Link an assistant to Donut through GitHub sign-in

**Identity:** SEED-002#story-2
```json dough-story-dependencies
{"schemaVersion":1,"identity":"SEED-002#story-2","dependencies":[{"supplier":{"identity":"SEED-002#story-1","href":"seeds/SEED-002-host-mcp-over-https.md#story-1"},"implementation":"Build account linking against the existing direct HTTPS MCP knowledge service and reuse its user-scoped note operations.","rationale":"The selected OAuth journey must be exercised by a real client against Donut's actual protected MCP resource. Shared design or mocked tools cannot establish that resource's protocol, attribution, and current-user access boundary.","condition":"Donut's direct HTTPS MCP service has delivered candidate search, attributed full reads, and bounded graph context for a real authenticated client using existing Donut tokens.","state":"satisfied","resolution":{"revision":"2c06ba2245f8972af0e8e9e64a218e82561717ba","path":".planning/seeds/SEED-002-host-mcp-over-https.md#story-1","summary":"SEED-002#story-1 delivered HTTPS MCP at /api/mcp with list_note_candidates, read_note, and get_note_graph for token-authenticated clients. Proof: backend MCP boundary tests through tip b8c5d9ae and e2e_test/features/mcp/mcp_services.feature (9 scenarios) over verified TLS Streamable HTTP. Condition met without changing story-2 assumptions.\nCondition satisfied: Donut's direct HTTPS MCP service has delivered candidate search, attributed full reads, and bounded graph context for a real authenticated client using existing Donut tokens.\nAccepted integration: 1faeb6d81afc86fd45582cd99589e7a115286c9c on origin/refs/heads/main."}}]}
```
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/073-assistant-account-linking/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"3aaeda0024031bd2c478d9999b7c99a14bcee3900a6d603ed89c4fee74afc228","plan":"f4ecc758d631a846c58e2178abe9d97cfc311db44c962a0b2d22509cc64195b3"}}
```

**Goal:** A Donut user can grant, maintain, and withdraw a compatible assistant's
read access by signing into Donut through GitHub, without manually copying a
Donut personal access token into that client.

**Scope:**

- Deliver discovery, GitHub-backed Donut sign-in, informed read-access consent,
  authorization-code exchange with PKCE, authenticated MCP use, token expiry
  and renewal, and user withdrawal of the assistant's access. Reuse the user's
  existing Donut identity and current notebook permissions. GitHub authenticates
  the person; Donut grants access and issues credentials for its MCP service.
- Consent names the client and Donut account, explains that the assistant can
  search, read, and explore notebooks the account can currently read, and offers
  Allow and Deny. It explains continued access through renewal and where to
  withdraw permission. A GitHub session by itself is not consent.
- Bind credentials to the authorizing user, registered client, read scope, and
  canonical MCP resource. Check the current grant and notebook permissions on
  every tool request. These credentials do not grant general Donut API access,
  personal-token creation, or content mutation. This enforces the seed's
  read-only, user-scoped delegation requirement.
- Keep grants and consent durable across ordinary backend restarts. In Donut
  settings, the user can identify a connected assistant and withdraw its access.
  Withdrawal invalidates that user's outstanding credentials and remembered
  consent for that client; subsequent access needs a fresh authorization.
  Finishing an already authorized in-flight read cannot recall information
  the assistant has received.
- One predefined OAuth client is sufficient, as selected in decomposition.
  The first journey uses a confidential hosted client with an exact
  registered callback and server-held secret, approved by Terry Yin on
  2026-10-27. Public-client renewal, client registration self-service, DCR, client-metadata-document
  registration, a client certification matrix, and ChatGPT-specific onboarding
  are deferred promises. No arbitrary one-client runtime limit follows from
  the example.
- Existing personal-token workflows remain available. Body search, editing,
  new retrieval rules, notebook-specific consent selection, and changing the
  GitHub login provider are outside this delivery. Existing Donut users are
  the beneficiary; account creation remains the existing registration workflow,
  with first-time signup during assistant linking a deferred promise.

**Key examples:**

1. An existing Donut user starts connecting a registered assistant with only
   the service URL and its registered client configuration. The client discovers
   the authorization server, sends the user through GitHub sign-in, and shows
   Donut's read-access consent. Allow completes the callback and code exchange;
   the assistant searches, reads an attributed note, and obtains bounded graph
   context using the same identity and permissions as the user. No personal
   Donut token is copied into the assistant.
2. A signed-in user sees the client and account before deciding. Deny returns
   an authorization refusal to the client; retry can ask again. A mismatched
   callback, invalid PKCE proof, or reused code cannot issue a usable credential.
   These are OAuth authorization constraints, not limits inferred from examples.
3. A valid grant permits subsequent calls without another browser login. When
   its short-lived access token expires, the client renews it and can read again
   under the same resource and scope. An expired or invalid refresh credential
   requires reconnection; renewal cannot enlarge the authorization.
4. Two users authorize the same assistant. Each sees only their readable notes.
   Losing notebook permission affects the next read or graph expansion. A
   credential for a different resource or without the read scope is refused;
   the assistant credential cannot be used to create a personal token or edit
   a note, including when a browser session accompanies the request.
5. The user withdraws the assistant in Donut settings. Its next tool call and
   renewal fail, including from an existing connection; the other user's grant
   still works. Connecting again requires consent. A backend restart between
   authorization and renewal preserves the valid grant and its user binding.

**Architecture:**

Use Spring Security's maintained authorization-server support in the existing
backend, with authorization-code/PKCE and durable authorization and consent
storage. Prefer opaque reference tokens checked against the local grant store:
the same backend owns issuance and consumption, and withdrawal must affect the
next request. JWTs would still need a revocation check or accept a withdrawal
delay; the latter does not satisfy this story. Do not issue existing personal
tokens as assistant credentials or call Donut's own introspection endpoint over
loopback HTTP merely to read local authorization state.

The existing `CurrentUserFetcherFromRequest`, `UserService`, and
`AuthorizationService` own identity resolution and notebook authorization.
Reuse that domain meaning through an explicit MCP authentication boundary.
An OAuth grant is a different lifecycle from `UserToken`: it includes a client,
consent, resource, scope, expiring credentials, and renewal. The CLI's Gmail
OAuth code is an outbound Google client, not an authorization server. The
existing personal-token settings UI is a navigation precedent, not a grant
store. This whole-product comparison found no existing owner for OAuth grant
issuance; add it without duplicating notebook access or attributed reads.

Use explicit resource validation during authorization, exchange, and renewal,
and validate the stored resource/scope on MCP use. The probe below establishes
that adding the Spring starter alone does not supply MCP audience binding.
Map the GitHub principal to the existing Donut user; do not confuse a client ID
or a Spring token principal with the authorizing user's identity. Keep browser
session login and consent separate from bearer-only MCP authentication. Protect
new consent and withdrawal mutations against CSRF; preserve OAuth protocol
errors deliberately and do not convert unexpected failures into success.

Keep operational endpoints under `/api` and Vue screens in `routeMetadata`.
The approved discovery-only paths are
`/.well-known/oauth-authorization-server` and
`/.well-known/oauth-protected-resource/api/mcp`, with the latter advertised by
the MCP `401` challenge. Both need backend routing in the shared routing
manifest. On 2026-10-27, Terry Yin approved this narrow exception to
[ADR 0005 — Web routes](../../docs/adrs/0005-web-routes-accepted.md), whose
HTTP API namespace includes authentication. The approval covers these two
metadata routes; authorization, token, revocation, grant management, and MCP
operations stay under `/api`. Record this exception with the maintained OAuth
documentation at implementation wrap-up. No ADR status or wider namespace rule
is changed by this story.

[ADR 0002 — Git-native Portable notebook tree synchronization](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
preserves accepted-revision attribution through the supplied MCP operations.
[ADR 0006 — Failure handling](../../docs/adrs/0006-failure-handling-accepted.md)
constrains intentional OAuth refusals and unexpected-failure reporting.
[ADR 0007 — Environments and isolation](../../docs/adrs/0007-environments-and-isolation-accepted.md)
requires isolated disposable proof. The ADR index and in-file statuses identify
0000–0007 as Accepted, with no supersession affecting this work. ADR 0001's
filename lacks the accepted suffix although both authority signals agree.

**Explored approaches and borrowed mechanism:**

| Candidate | Fit to the goal and constraints | Consequence |
| --- | --- | --- |
| Continue using personal tokens or copy notes | Smallest existing solution | Fails the selected account-linking goal and preserves manual credential handling |
| Wrap personal-token issuance in a custom OAuth flow | Could provide a similar user journey | Donut would own protocol machinery and would still need a separate constrained, expiring delegation model |
| Spring authorization server with JWT access tokens | Fits the backend and interoperable client flow | Next-request withdrawal still needs shared grant/revocation state; stateless validation alone is insufficient |
| Spring authorization server with opaque reference tokens | Fits the same backend and next-request withdrawal | Recommended; a local durable grant lookup joins token lifecycle to current permissions |

Borrow a revocable visitor badge from physical access control. The Donut user
is the host authorizing a visitor; the assistant is the visitor; the
authorization server issues the badge; the opaque token identifies an entry in
the access roster; notebook permission checks are the door checks. Renewal
extends a permitted visit, and removing permission from the roster stops later
entry. This is the opaque-token candidate, not independent evidence for it.
The consequential limit is that an assistant can retain information it has
already read: revocation prevents future access, not prior disclosure. A badge
also does not establish the OAuth callback, PKCE, or resource-binding rules;
those need protocol proof. A separately hosted identity broker remains outside
the owner's selected in-backend boundary.

**Investigation, 2026-10-27:**

Repository inspection at `9c830fadb68b3299766a644dfa92ed48a39f3c73` found
Spring Boot 4.1.2, GitHub `oauth2Login` only in the production security chain,
browser identity mapped by `externalIdentifier`, and personal tokens carrying
only user, token, and label. Existing Cypress login supplies a nonproduction
Basic-auth session; it cannot prove a GitHub callback. Story 1 is still Taken
in this checkout and its required HTTPS MCP outcome remains a recorded supplier
dependency, not an observation made by this preparation.

A disposable probe in this owned worktree used the Boot 4.1.2 dependency BOM,
which resolved Spring Security authorization server 7.1.2. Command:
`CURSOR_DEV=true nix develop -c ./backend/gradlew -p .tmp-oauth-probe run`.
It drove the library's real security filters through MockMvc with synthetic
GitHub-shaped principals, exact HTTPS callback URIs, S256 PKCE, `notes:read`,
opaque tokens, and refresh rotation. It observed:

- Root authorization-server metadata returned 200 and advertised the configured
  `/api` token endpoint.
- A public client received an access token but no refresh token. A confidential
  client received both and preserved the synthetic GitHub principal name.
- Default access-token claims named the client as audience, and a request for
  a different resource still exchanged successfully. Explicit MCP resource
  binding remains required work.
- Renewal returned a new access token and rotated refresh token. Revoking the
  refresh token made both current tokens inactive; another renewal returned 400.
- On local MySQL 8.4.11, the released JDBC authorization schema and service
  saved that grant; a newly constructed service loaded it by access token with
  its principal preserved and refresh token active. A uniquely named disposable
  database was created for this operation and dropped in `finally`.

The final probe passed. Initial probe setup errors (duplicate synthetic client
secret, then GET parameters not encoded as a query) were corrected before these
observations. Consent was disabled in the probe, authentication was supplied,
and it did not call Donut's MCP service. Thus it establishes library mechanism
and JDBC serialization, not browser consent, real GitHub login, full-process
restart recovery, whole-client withdrawal, or end-to-end MCP interoperability.
Those remain execution proof obligations. No production or Development data
was touched. All disposable source/build files were removed before this write.
The final probe source SHA-256 was
`0e73908ac52f7c23994e49b47198084226306e7f4c29b011b3e8500cca196608`.

Primary references checked on that date:
[MCP authorization and discovery](https://modelcontextprotocol.io/specification/2025-11-25/basic/authorization),
[Spring Security authorization-server setup](https://docs.spring.io/spring-security/reference/servlet/oauth2/authorization-server/getting-started.html),
[Spring authorization components](https://docs.spring.io/spring-security/reference/servlet/oauth2/authorization-server/core-model-components.html),
and the released
[7.1.2 refresh-token generator](https://raw.githubusercontent.com/spring-projects/spring-security/7.1.2/oauth2/oauth2-authorization-server/src/main/java/org/springframework/security/oauth2/server/authorization/token/OAuth2RefreshTokenGenerator.java).
This uses the supplier's 2025-11-25 interoperability baseline; it does not promise
certification against every newer protocol version.

**Owner decisions, 2026-10-27:** Terry Yin answered "as recommended" to the
two presented choices: the discovery-only ADR 0005 exception and the first
journey through one pre-registered confidential hosted client. These settle
the open refinement decisions. The recommended Spring/opaque-token design
above retains the full lifecycle promise.

**Effort hypothesis:** L (2–4 hours), medium confidence after the library and
persistence probe and the owner's decisions.
Maintained protocol and JDBC support reduce the unknown work. Explicit resource
binding, browser consent and return-to-flow integration, whole-client withdrawal,
and real-client proof still need bounded leaves. Required backend-suite time
and operator-owned credential/release waits are accounted for separately in
the plan. Reassess the band if reducible implementation exceeds it; do not
silently split or shrink the lifecycle promise.

**Safe stopping point:** The delivered compatible client can connect, renew,
and lose access through the user's decision, independently of ChatGPT-specific
packaging. During execution, preserve existing login and personal-token use;
offer the new connection only when its promised lifecycle works.

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
