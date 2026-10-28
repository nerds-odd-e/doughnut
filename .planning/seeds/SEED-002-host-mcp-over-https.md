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
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/075-direct-chatgpt-knowledge/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"0aa98d4b850569811168d6f197afc51849d413e17996ae98f02f91eed0230bbe","plan":"a37e3dbf1906631fe2cfb9e357a22755c7326f8a3a6bd65c212f05513fa744ba"}}
```

**Goal:** An existing Donut user can create or install a personal/custom ChatGPT
plugin, link their own Donut account through GitHub, and use current, attributable
Donut knowledge in an ordinary ChatGPT conversation while their computer is off.

**Scope:** Complete and document the real ChatGPT onboarding, consent, retrieval,
renewal and reconnection journey against Donut's direct HTTPS service. Preserve
the three delivered read capabilities: title/alias candidate search, attributed
full-note reads, and bounded graph context. The plugin creator/operator configures
the connection; each user grants access as their own Donut identity. Distinguish
connection configuration from a user's consent, and give usable feedback when
setup or reconnection cannot finish.

Required attribution identifies the note by title, notebook/folder and note ID,
with the accepted notebook revision returned by the full read. Explain that this
is the whole notebook's revision, and preserve a null revision when none exists.
Use full reads for cited evidence; graph excerpts can be truncated. Demonstrate
current access permissions, account isolation, automatic access-token renewal,
withdrawal and fresh informed consent when reconnecting. A later fresh read must
use current accepted content, rather than presenting an old conversation's text
as a new retrieval.

**Genuine constraints:** Use the backend and its existing GitHub identity,
authorization and notebook permission owners. The first release is read-only;
the assistant's grant is for `notes:read`. Retrieved content remains evidence,
not application instructions. Permission checks apply to every new request.
Withdrawal cannot erase information already returned to a conversation. These
constraints come from the shared owner-selected scope and the delivered
[authorization contract](../../docs/assistant-oauth.md).

**Deferred promises:** Public-directory publication, a custom ChatGPT UI,
body/semantic search, content editing, first-time Donut signup during linking,
offline copies/synchronization, and universal behavior across ChatGPT account
types. Terry selected the direct custom-MCP route on 2026-10-28 and wants to
avoid additional scripts. Use the existing backend, ChatGPT setup UI and written
instructions; add no connector script, bridge or packaged workflow. Existing
repository verification and delivery tools remain the means of doing the work.
If that route proves insufficient, return the observed incompatibility for a
decision rather than adding another runtime. These are delivery
boundaries, not additional runtime refusals. Deployment and real-client activation
retain their separate operator authority; this preparation authorizes neither.

**Key examples:**

1. An existing user has access to a configured custom plugin. They select it in
   ChatGPT, sign into Donut through GitHub and approve the displayed read access.
   ChatGPT can search their readable notebooks and subscriptions. Cancelling
   consent leaves the connection incomplete with a way to try again; GitHub
   login alone does not authorize reads.
2. Two accessible notes have the same title in different notebooks/folders.
   The user asks about a known topic; ChatGPT compares candidates, uses their
   context to select relevant evidence (asking when the request is ambiguous),
   reads it in full and answers with inspectable note/revision attribution.
   An empty title/alias search is explained as no matching candidates, not
   proof that no relevant content exists anywhere in Donut.
3. The user asks how that note connects to another idea. ChatGPT requests graph
   context with a token budget and reads any related note it relies on for a
   citation. The answer distinguishes source evidence from interpretation and
   does not present truncated graph excerpts as complete notes.
4. After an accepted note edit, a fresh request retrieves the new content and
   returned notebook revision. From another device, with the original computer
   switched off, a fresh conversation still completes a real search and read.
   A tunnel or a replay of previously disclosed text cannot establish this result.
5. A second Donut account links independently and asks about a private note
   available only to the first. Its new requests follow its own permissions,
   including direct note-ID reads and graph access. Existing notebook access
   controls remain the authoritative refusal, even when a model knows the ID.
6. After actual access-token expiry, another request succeeds through ordinary
   renewal without repeating browser consent. After the user withdraws the
   plugin's access in Donut Settings, new reads and renewal fail. ChatGPT offers
   an actionable route to connect again; a new grant requires informed consent.

**Exploration and borrowed mechanism:**

| Candidate | User effect and system consequence | Assessment against the same goal |
| --- | --- | --- |
| Copy selected notes into chat | Immediate, deliberate evidence selection; the user must repeat transfer and refresh, and withdrawal cannot affect copied text. | Strongest smaller workaround, but it misses the selected live authenticated connection. |
| Direct personal/custom MCP plugin | Creator/operator configures one connection, then the user links their Donut identity. Uses the existing backend, tool semantics and token lifecycle. | Selected by Terry on 2026-10-28; actual ChatGPT interoperability remains to be observed. |
| Packaged research workflow over the same MCP tools | A repeatable procedure guides retrieval and citations, but adds skill packaging, installation and maintenance alongside connection setup. | Considered but not selected; Terry prefers the direct route without additional scripts. |

The third candidate borrows a reference librarian's procedure: a question becomes
a catalog search (title/alias candidates), selected catalog entries become full
source reads, references become graph exploration, and the answer carries source
identifiers. Adapt that sequence first through the existing tool descriptions and
example prompts. The correspondence does not establish effectiveness: ChatGPT's
selection is variable, title/alias matching is not subject indexing or body search,
and neither a skill nor an analogy guarantees a faithful citation. Observe actual
tool selection and the final answer; keep guidance in existing tool descriptions
and written examples within the selected route.

**Architecture:** Keep OAuth protocol handling in Spring Authorization Server,
identity/consent in Donut, and the hosted client's credentials in ChatGPT. The
documented predefined-client path fits the delivered confidential client model;
CIMD/DCR would add registration and credential-lifecycle responsibilities with no
established need for this personal/custom outcome. Retain the current metadata
and exact-redirect contract unless an observed incompatibility requires a bounded
change. The exact callback must come from the connection being created, not a
copied example or an abandoned form.

Reuse `NoteSearchService`, `AttributedNoteContentService` and `NoteGraphService`
behind the current MCP tools. Do not create a second knowledge store, query
model or grant model for ChatGPT. The tool descriptors currently describe reading
in prose but have no explicit read-only annotations; establish accurate client
metadata in the existing descriptors as needed and evaluate its effect in ChatGPT.
Keep source attribution aligned with
[ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md),
routes with [ADR 0005](../../docs/adrs/0005-web-routes-accepted.md) and its two
owner-approved discovery exceptions in the authorization guide, and observation
ownership with [ADR 0007](../../docs/adrs/0007-environments-and-isolation-accepted.md).
The ADR index and record statuses agree. ADR 0001 is Accepted despite its
unmarked filename; this is an existing filename convention mismatch, not a status
conflict. No new ADR exception or North Star change is proposed.

**Investigation, 2026-10-28 (Asia/Singapore):** Readings were made from the
established worktree at `3d4da7a333d562d2b8ed3c60db2b32c8d2d2d918`. The satisfied
story-2 agreement above is inherited compatible-SDK evidence; it is not evidence
of a ChatGPT run or of production deployment. Source reading confirmed
`client_secret_basic`, S256, `notes:read`, exact configured redirects and rotating
refresh credentials. The public/UI readings below performed no authenticated
retrieval. A later local suite observation is recorded separately below.

- Official [custom MCP guidance](https://developers.openai.com/api/docs/guides/custom-mcp-server)
  fetched on this date describes creating and installing a custom plugin in
  ChatGPT web with OAuth and predefined credentials. The
  [authentication guide](https://developers.openai.com/plugins/build/auth)
  requires discovery and S256 and describes connection-specific callbacks when
  issuer-identification support is not advertised. Donut's current metadata
  customizer does not advertise that support. Actual callback/client compatibility
  remains unproved until linking succeeds.
- Read-only public requests used `curl --silent --show-error --max-time 20
  --dump-header <temporary-header-file> --output <temporary-body-file> <URL>`
  for `https://doughnut.odd-e.com/.well-known/oauth-protected-resource/api/mcp`,
  `https://doughnut.odd-e.com/.well-known/oauth-authorization-server` and
  `https://doughnut.odd-e.com/api/mcp`. Both discovery responses were HTTP 200
  `text/html` containing the SPA shell, not OAuth JSON. The MCP response was
  HTTP 401 JSON without `WWW-Authenticate`. These observations establish a
  deployment/discovery blocker, not its operational cause or a source-code defect.
- In the signed-in Chrome session, opened `https://chatgpt.com/plugins` → Add →
  Add custom MCP server, entered only the public MCP URL, and inspected Advanced
  OAuth settings. ChatGPT reported that OAuth discovery failed. The form exposed
  User-Defined OAuth Client, a connection-specific callback, client ID/secret,
  scopes and token authentication choices including `client_secret_basic`.
  The form was cancelled without creation, credentials, login or consent. This
  establishes form availability and the discovery failure; it does not establish
  installation, account linking, renewal or model behavior. The cancelled form's
  callback is not a configuration value to reuse.

**Acceptance route, selected by Terry on 2026-10-28:** Use
`https://doughnut.odd-e.com/api/mcp` after the separately authorized schema-first
OAuth release and real-client configuration described in the authorization guide.
The selection authorizes completing preparation, not deployment or a real grant.
The executable plan bounds credentialed linking and lifecycle compatibility with
early owner-operated probes. Actual accepted-content retrieval from another device
with the original computer off remains required proof. A local tunnel alone cannot
fulfil that promise. There is no remaining refinement question.

**Local premise observation, 2026-10-28:** In the same preparation checkout,
a temporary `McpEndpointTest` assertion expected
`$.result.tools[0].annotations.readOnlyHint` to be true in the real `tools/list`
HTTP response. `CURSOR_DEV=true nix develop -c pnpm backend:test_only` provisioned
this worktree's disposable database and ran 2,961 tests. The only failure was
the temporary assertion: the serialized tool had no `annotations` property;
two tests were skipped. This establishes the observable metadata gap and the
test's ability to detect it, not a passing implementation. The assertion was
removed and `git diff --exit-code -- backend/src/test/java/com/odde/donut/mcp/McpEndpointTest.java`
confirmed byte-for-byte restoration before writing this record. No product or
test change remains from the observation.

**Execution plan:** [Direct ChatGPT knowledge](../slice-plans/075-direct-chatgpt-knowledge/PLAN.md).

**Effort hypothesis:** M (1–2 hours) of implementation and focused verification
if the existing OAuth client model interoperates; low confidence until the real
linking probe. Operator release work and account/device observation waits are
unmeasured prerequisites, not hidden inside this estimate. A compatibility failure
must be understood before expanding implementation or revising the story boundary.

**Safe stopping point:** A documented, personally installable direct connection
with the examples above demonstrated. Public-directory distribution remains a
separate owner decision. Preparation alone does not reach this stopping point.

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
