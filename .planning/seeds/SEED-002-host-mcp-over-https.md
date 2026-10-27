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
[Dated baseline and candidate investigation](../slice-plans/073-assistant-account-linking/PREPARATION-EVIDENCE.md) retain revisions, actions, results and limits.

## Shared Constraints

- MCP and OAuth belong in the existing backend and ordinary HTTPS deployment.
- GitHub authenticates the person; Donut issues credentials and resolves the authorizing user and current notebook permissions on each request.
- First delivery is read-only title/alias candidate search, attributed full reads and bounded graph context; body search and editing are separate outcomes.
- Browser sign-in/consent establish linking; ordinary MCP uses access tokens with expiry, renewal and withdrawal under the author's control.
- Reuse backend search, attribution and focus context. Note contents are evidence, distinct from instructions; citations identify the note and accepted notebook revision.
- Follow [ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md), [ADR 0005](../../docs/adrs/0005-web-routes-accepted.md), and [ADR 0007](../../docs/adrs/0007-environments-and-isolation-accepted.md).
- Migration deletes the standalone local MCP package and everything only it used, with story 1's whole-product reading; retain dependencies shared by other callers.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours including verification and delivery,
following [SEED-039](SEED-039-faster-ci-feedback.md#story-decomposition).
Refinement challenges an L story likely to exceed those comparative bands.

<a id="story-2"></a>

### Link an assistant to Donut through GitHub sign-in

**Identity:** SEED-002#story-2
```json dough-story-dependencies
{"schemaVersion":1,"identity":"SEED-002#story-2","dependencies":[{"supplier":{"identity":"SEED-002#story-1","href":"seeds/SEED-002-host-mcp-over-https.md#story-1"},"implementation":"Build account linking against the existing direct HTTPS MCP knowledge service and reuse its user-scoped note operations.","rationale":"The selected OAuth journey must be exercised by a real client against Donut's actual protected MCP resource. Shared design or mocked tools cannot establish that resource's protocol, attribution, and current-user access boundary.","condition":"Donut's direct HTTPS MCP service has delivered candidate search, attributed full reads, and bounded graph context for a real authenticated client using existing Donut tokens.","state":"satisfied","resolution":{"revision":"2c06ba2245f8972af0e8e9e64a218e82561717ba","path":".planning/seeds/SEED-002-host-mcp-over-https.md#story-1","summary":"SEED-002#story-1 delivered HTTPS MCP at /api/mcp with list_note_candidates, read_note, and get_note_graph for token-authenticated clients. Proof: backend MCP boundary tests through tip b8c5d9ae and e2e_test/features/mcp/mcp_services.feature (9 scenarios) over verified TLS Streamable HTTP. Condition met without changing story-2 assumptions.\nCondition satisfied: Donut's direct HTTPS MCP service has delivered candidate search, attributed full reads, and bounded graph context for a real authenticated client using existing Donut tokens.\nAccepted integration: 1faeb6d81afc86fd45582cd99589e7a115286c9c on origin/refs/heads/main."}}]}
```
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/073-assistant-account-linking/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"f9ddafc467232ee4132690edbabe1fbed73f1d4b8cd3cbf13967e31c0c964f6b","plan":"33fccdc5faede9adbbfdf96674fc225b8a3cc2603287639c62eda4d659c7f8d3"}}
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
  Reuse the existing GitHub OAuth login implementation and identity mapping;
  no new GitHub scopes, APIs or provider capabilities are selected. The new
  return-to-consent integration is proved with an external GitHub HTTP fixture
  through the real security chain. A new GitHub test app or credentialed
  provider probe is unnecessary for this unchanged login behavior, as clarified
  by Terry Yin on 2026-10-27.
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

Use Spring Security's maintained authorization server in the existing backend:
authorization-code/PKCE, durable authorization/consent, and opaque reference
tokens checked locally. Withdrawal affects the next request; JWTs would still
need revocation checks. Do not issue personal tokens as assistant credentials
or call Donut introspection over loopback merely to read local grant state.

Reuse `CurrentUserFetcherFromRequest`, `UserService` and `AuthorizationService`
for user identity, notebook permissions and attributed reads. OAuth grants own
client/consent/resource/scope/expiry/renewal, separately from `UserToken`.
CLI Gmail OAuth is an outbound client; personal-token settings are a navigation
precedent, not a grant store. No existing owner supplies OAuth grant issuance.

Validate resource at authorization, exchange and renewal, then stored resource
and read scope at MCP use; the [probe](../slice-plans/073-assistant-account-linking/PREPARATION-EVIDENCE.md)
found the library default insufficient. Resolve the GitHub user's identity,
not the client principal. Keep browser login/consent separate from bearer-only
MCP. Protect consent/withdrawal against CSRF; preserve deliberate protocol
errors and report unexpected failures under [ADR 0006](../../docs/adrs/0006-failure-handling-accepted.md).

Operational endpoints remain under `/api`; Vue screens use `routeMetadata`.
On 2026-10-27 Terry Yin approved only `/.well-known/oauth-authorization-server`
and `/.well-known/oauth-protected-resource/api/mcp` as exceptions to
[ADR 0005](../../docs/adrs/0005-web-routes-accepted.md). Route both to the backend;
advertise the latter in the MCP `401` challenge. Authorization, token,
revocation, grant management and MCP remain under `/api`. Preserve this narrow
exception in maintained OAuth docs at wrap-up; no ADR status or wider rule changes.

[ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
preserves accepted-revision attribution; [ADR 0007](../../docs/adrs/0007-environments-and-isolation-accepted.md)
requires isolated disposable proof. ADRs 0000–0007 have agreeing Accepted index/in-file
statuses and no relevant supersession; ADR 0001 lacks the accepted filename suffix.

**Owner decisions, 2026-10-27:** Terry Yin answered "as recommended" to the
two presented choices: the discovery-only ADR 0005 exception and the first
journey through one pre-registered confidential hosted client. These settle
the open refinement decisions. The recommended Spring/opaque-token design
above retains the full lifecycle promise.

During execution on the same date, Terry Yin clarified that GitHub OAuth login
is an existing feature to reuse cohesively. No new GitHub test-app approval is
needed unless implementation proposes a new GitHub OAuth capability; any such
proposal returns to the owner. This changes the proof prerequisite for the
unchanged provider behavior, while preserving callback/identity continuation
into Donut consent as required new integration proof.

**Effort hypothesis:** L (2–4 hours), medium confidence after the library and
persistence probe and the owner's decisions.
Maintained protocol and JDBC support reduce the unknown work. Explicit resource
binding, browser consent and return-to-flow integration, whole-client withdrawal,
and real-client proof still need bounded leaves. Required backend-suite time
and operator-owned release waits are accounted for separately in
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

Each supplier remains useful if a later story is cancelled. Record necessary
blocking supplier agreements in consumer story sections with the backlog tool.
Defer public-directory distribution, editing, custom ChatGPT UI and broader
search before reducing the three read capabilities or user access controls;
these expansions are not queued here.

## Refinement Handoff

Refinement settles library integration, client registration, token lifetimes/renewal,
and minimum consent/revocation UI. This decomposition provides no executable plan or readiness.
Public-directory publication remains an owner decision after a useful direct custom plugin.

## When to Surface

For direct HTTPS MCP, assistant linking or ChatGPT, see [backlog](../PRODUCT-BACKLOG.md), [learning/refinement/retrieval](SEED-073-learning-refinement-and-retrieval.md),
[knowledge-to-capability](SEED-074-knowledge-to-capability.md),
[OpenAI MCP](https://developers.openai.com/plugins/build/mcp-server) and
[account linking](https://developers.openai.com/plugins/build/auth).
