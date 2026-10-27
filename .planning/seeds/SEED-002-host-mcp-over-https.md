---
id: SEED-002
status: dormant
planted: 2026-08-06
planted_during: idle (after deploy/static consistency audit)
updated: 2026-10-28
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
- Complete migration includes deleting the standalone local MCP package and
  everything used only by it, with the whole-product reading described in
  story 4. Shared client or test dependencies stay where other callers need them.

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

<a id="story-4"></a>

### Complete the retirement of the local MCP server

**Identity:** SEED-002#story-4
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/074-https-mcp-maintenance/PLAN.md","assessment":"not-ready","reasons":["The owner selected a separate Cursor-agent migration of private clients to HTTPS. Its endpoint/tool-discovery and consumer-release result is not yet observed; obtain that result and freshly reassess before taking Story 4 or deleting the generated directory."],"basis":{"document":"3e9f3f0353811de5843fbfcce70ae0e6233c66de44701631558aae426623b309","plan":"a53a0cb188fd223410020ff789992f695fd6e7be0b184a085222e80b79483a2b"}}
```

**Goal:** Donut users connect their MCP clients directly to the backend's HTTPS
service, and maintainers support that service with one coherent set of behavior
tests after completely deleting the standalone local server and its dependents.

- **For / why:** Users and maintainers need the supported connection, setup
  instructions, build tooling, and E2E coverage to agree on the backend-owned
  HTTPS service.
- **Scope:** Delete every remaining part of the standalone local MCP server,
  recursively including its exclusive tests, fixtures, helpers, dependencies,
  package and lockfile entries, build/distribution outputs, scripts, CI wiring,
  configuration, documentation, comments, agent guidance, and generated
  artifacts. Sweep backend, frontend, CLI, MCP, tooling, and documentation;
  keep shared code and dependencies that surviving callers need. Document the
  supported connection directly to the existing backend `/api/mcp` endpoint.
- **E2E disposition:** Redirect any tests still using the local process or stdio
  transport to the backend's HTTPS service. Where coverage already exercises
  HTTPS, compare the promised behaviors and delete redundant tests and their
  exclusive support. Retain meaningful proof of each distinct surviving
  behavior, including token-scoped access, candidate search, attributed reads,
  bounded graph context, and isolated E2E operation. The E2E TLS proxy is test
  transport infrastructure forwarding to the backend, not the standalone MCP
  implementation targeted for deletion.
- **Evaluation:** A client configured with the Donut HTTPS URL and a personal
  token discovers and uses the backend tools. The retained E2E journeys exercise
  those behaviors through verified HTTPS against their own disposable stack.
  Local-only coverage moves to that boundary; duplicate HTTPS coverage is
  deleted without discarding distinct behavior proof.
- **Removal reading:** At acceptance, perform a one-time whole-product search
  for the retired package, process, bundle, commands, and setup names, and
  inspect exclusive dependents transitively. Record the search and classified
  result as a reading in the execution plan, separately from surviving-behavior
  tests. Add no absence assertion, permanent checker, compatibility guard, or
  historical removal note. Preserve the provenance of retained dated evidence;
  resolve obsolete dependent observations with their consumers rather than
  relabeling an observed action to clear a search.
- **Boundary:** Reuse the delivered HTTPS service and existing tool semantics.
  OAuth account linking and ChatGPT onboarding remain with stories 2 and 3.
  Follow [ADR 0005 — Web routes](../../docs/adrs/0005-web-routes-accepted.md)
  for the API boundary and
  [ADR 0007 — Environments and isolation](../../docs/adrs/0007-environments-and-isolation-accepted.md)
  for disposable E2E ownership.
- **Value / learning:** Finish the retirement and remove maintenance duplication
  while preserving confidence in the connection users actually use.
- **Effort hypothesis:** S (30–60 minutes), medium confidence because tracked
  package deletion and HTTPS E2E migration have already landed; refinement
  should size any newly discovered dependents before execution.
- **Safe stopping point:** The existing HTTPS service and its distinct behavior
  coverage remain useful independently of later OAuth or ChatGPT work.

**Key examples:**

1. A user has a personal token and follows the supported HTTPS setup. Their
   client discovers the three note tools, finds matching candidates, reads a
   candidate with its accepted notebook revision, and obtains graph context
   within the requested budget. An unmatched query gives the existing empty
   result. The setup and feature description describe these read operations.
2. Two users have different private notebooks. Each client's personal token
   permits searching its user's readable notes; the other user's private notes
   stay inaccessible. Existing token withdrawal and notebook-access refusals
   remain supported behavior, not removal assertions.
3. A maintainer runs the focused MCP feature in an isolated checkout. It uses
   that checkout's disposable application through verified HTTPS and retains
   distinct candidate, attribution, access, and graph-budget observations.
   Equivalent positive-search examples share the richer candidate proof.
   The separate paired-worktree isolation scenario and its support remain.
4. A maintainer uses the supported setup and build guidance after cleanup.
   It describes the backend service directly. The integration checkout's
   generated package outputs and exclusive installation links are included in
   the deletion; shared dependency targets and authored work remain intact.
   The deletion itself is accepted by the separate one-time removal reading.

**Current evidence, 2026-10-27:** Repository reading at
`3d32688d4a040ab4e57e8a5a03d48963e4dc157f` found that
`b8c5d9ae302d4be49c63edeaea06e5b7ffe7b158` had already deleted the tracked
`mcp-server` package and updated workspace/build/CI/setup references.
`e2e_test/support/mcp_client.ts` already uses `StreamableHTTPClientTransport`
through a trusted TLS proxy to the backend `/api/mcp` endpoint;
`e2e_test/features/mcp/mcp_services.feature` uses that client. This checkout
still contains the untracked generated bundle
`mcp-server/dist/mcp-server.bundle.mjs`. These are source and filesystem
observations, not a new test run or a finding that existing HTTPS scenarios
are redundant. Start from the delivered behavior and classify remaining work
rather than repeating the completed migration.

**Investigation, 2026-10-28:** In the established preparation checkout at
`d65e2550331fea6682c0405813085516a4164c98`, tracked-file and explicit ignored-file
readings confirmed the delivered HTTPS transport and package deletion. The
generated directory is absent in this fresh worktree but remains at
`/Users/terryyin/git/doughnut/mcp-server`: compiled `build/` files,
`dist/mcp-server.bundle.mjs`, and package-local `node_modules` links/cache.
These observations distinguish the two checkouts; they do not revise the
dated observation above. The root MCP SDK is still consumed by the HTTPS E2E
client, and the TLS proxy forwards requests to the real backend.

The generic positive-search outline rows overlap richer candidate scenarios.
A disposable edit replaced that outline with its unmatched-query case. Running
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/mcp/mcp_services.feature`
passed all seven remaining ordinary cases, including token-user isolation,
revision-changing reads, and both graph budgets. A second disposable run
focused the all-candidates scenario and replaced the real search response's
content with `[]` at the client boundary: it failed at the candidate assertion.
Both source files were restored byte for byte before this refinement write.
The paired-worktree case was excluded by ordinary runner selection; this is
not a fresh concurrent-worktree observation. Full commands, setup, results,
and proof limits belong in the associated plan.

Remaining work is cleanup within the existing scope: the observed generated
directory, redundant search rows, the feature's unsupported write-operation
description, removal wording in current MCP guidance, and obsolete current
transport assumptions in planning consumers. Preserve needed dated evidence
with its actual date, revision, action, result, and limits; replace obsolete
current guidance without changing what a historical observation says.
A final process-ownership observation found seven live bundle processes owned
by Cursor extension hosts, Cursor's MCP process, and cursor-agent sessions.
The `doughnut` entry in `/Users/terryyin/.cursor/mcp.json` still launches that
exact bundle. Only command/path and environment-key names were inspected for
reporting; token values were not exposed. The directory therefore has active
consumers and cannot be treated as unused generated output.

**Local client disposition, selected by Terry Yin on 2026-10-28:** Terry will
ask a Cursor agent to migrate his private clients to HTTPS separately, using
the handoff supplied in chat. Private configuration migration is outside
this story's implementation; its verified completion is a prerequisite to
taking this cleanup story. The handoff must verify the remote endpoint and
tool discovery using the existing personal token privately, update the
`doughnut` client entry, and reconnect affected clients while preserving
unrelated work. It reports any still-active bundle consumer or unavailable
HTTPS endpoint rather than deleting files or claiming success.

No client configuration or running session was changed by this preparation,
and migration completion has not been observed. Preparation may land as
requested; execution waits for the Cursor result and a fresh readiness
assessment. Artifact deletion remains part of this story and rechecks
ownership, preserving any new authored or foreign-owned work.

## Ordering and Scope Reduction

The order delivers an immediately usable remote knowledge service, then
user-controlled account linking, then the specific ChatGPT experience. Keep
each supplier's working outcome available to compatible clients even if a
later story is cancelled. Necessary blocking supplier agreements belong in
the consumers' canonical sections and must be recorded with the backlog tool.

Story 4 closes the local-server retirement against the delivered HTTPS
service. It is queued next and can proceed independently of account linking;
it adds no prerequisite to stories 2 or 3.

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
