# Use Donut note context from an HTTPS MCP client

**Identity:** SEED-002#story-1  
**Source:** [Refined story](../../seeds/SEED-002-host-mcp-over-https.md#story-1)  
**Preparation checkout:** `/Users/terryyin/git/doughnut/.worktrees/use-donut-note-context-from-an-https-mcp-client`  
**Observed revision:** `0719278a187bdead917a44d79b4bddc714d7d599`  
**Prepared:** 2026-10-27. Preparation only; execution requires a separate instruction.

## Goal and boundaries

A token-bearing remote MCP client connects directly to Donut's HTTPS backend,
discovers accessible note candidates, reads attributed full content, and obtains
bounded graph context. Complete the migration to that connection, including
client instructions and transitive deletion of the standalone package.

Preserve the three tool names, input meanings, candidate ordering and shape,
empty search signal, full-read attribution, and existing focus-context model.
Authenticate every request and enforce existing read permission for every note.
The token's user is request-local; withdrawal takes effect on the next request.
Validate supplied Origins as required by the transport protocol. No added
restrictions arise from the number or arrangement of example notes.

OAuth account linking, ChatGPT setup, editing, body search, new graph rules,
protocol versions beyond verified client negotiation, public-directory
publication, and broad client certification are outside this delivery.
No production deployment or paid service call is authorized by this plan.
Production release remains the existing release workflow; the HTTPS acceptance
journey uses isolated E2E data and a trusted test TLS boundary.

## Current decisions and reuse

- Use the Java SDK's stateless servlet transport with immediate synchronous
  execution, starting from the observed released SDK 2.1.0. Register it inside
  the existing Spring Boot 4.1.2 application at `/api/mcp`. Avoid adding Spring AI
  tool conversion or implementing JSON-RPC. Negotiate the SDK/client-supported
  protocol; the observed SDK lists versions through 2025-11-25.
- Ensure a Spring request context is bound around servlet dispatch. The probe
  manually bound it and therefore does not prove application registration.
  Do not rely on inheritable thread locals, singleton identity, or a cached
  session user. Keep calls on the servlet thread; the first slice proves the
  actual application's binding and token gate before domain integration.
- Reuse the token lookup owned by `UserService` and the existing user model.
  An MCP-specific token-only ingress must not fall through to browser cookies
  when a bearer token is missing or invalid. Reuse authorization rules rather
  than introducing an MCP permission store or changing ordinary browser login.
- PFE: `NoteSearchService.searchForNotes` already owns title/alias search over
  the user's notebooks/subscriptions. Map its note hits to the existing MCP
  candidate shape. `AuthorizationService` owns read permission.
  `FocusContextRetrievalService.retrieve` and `RetrievalConfig.forGraphApi`
  own graph selection and budget. Reuse them directly.
- PFE: `AttributedNoteContentController` currently combines authorization,
  content projection, and accepted-revision lookup in one read transaction.
  Expose this operation as one shared backend responsibility for REST and MCP,
  preserving `AttributedNoteContent`; do not duplicate its lookup or make an
  internal HTTP round trip. Keep entity hydration and serialization inside the
  needed read transaction for servlet calls; MVC's open-session behavior is not
  evidence that a separate servlet has it.
- Translate deliberate validation/access outcomes to useful MCP errors. Preserve
  the existing `FailureReportFactory` reporting responsibility for unexpected
  failures even when the SDK catches them. Add no blanket successful fallback.
- `/api/mcp` is covered by existing `/api/` production routing. Describe the
  HTTP transport in the OpenAPI generation path; MCP schemas remain the protocol
  tool contract. Regenerate rather than hand-edit generated API artifacts.
- Accepted constraints: [ADR 0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
  (accepted content/revisions), [ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md)
  (API namespace and contract), [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md)
  (deliberate failures/reporting), and [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
  (disposable isolated observations). Index and relevant record statuses agree;
  no conflict or exception. The existing North Star's accepted-change boundary
  applies to attribution; no new product-wide architectural direction is needed.

## Outside-in proof ownership

| Promise | Owner | Observable proof |
| --- | --- | --- |
| Real MCP negotiation at `/api/mcp`, tools discovered with a valid token | 1 | SDK client initializes and lists tools through the running backend; input binding is exercised through the servlet |
| Every request requires a current valid token; invalid Origin is refused | 1 | HTTP requests with missing/invalid/deleted tokens receive authentication failure, including a browser cookie; supplied invalid Origin gets 403 |
| Search retains title/alias ranking, distinguishable equal-title candidates, subscription access and empty results | 2 | MCP boundary tests with real backend fixtures; selected candidates expose the correct notebook/folder and no private foreign candidate |
| Full content and precise notebook attribution; null when unbound; revision changes after accepted edit | 3 | MCP calls use existing Git-backed notebook fixtures; canonical full result once, delta/null cases separately; retained REST boundary proof stays green |
| Budgeted focus/related context, argument errors, and read authorization | 4 | MCP boundary calls with existing focus-context fixtures and small/large budgets; private focus and related-note access exercise actual refusal/filtering |
| Two users remain isolated across calls and token withdrawal | 2–4 | Interleaved/concurrent real HTTP calls use distinct issued tokens; search, direct reads, and graph exercise their respective authorization boundaries |
| Client → HTTPS → backend → real domain journey with no protocol stub | 5 | Existing Cypress MCP feature migrated to the SDK Streamable HTTP client with verified test TLS; tool discovery plus search/read/graph assertions and client cleanup |
| Documented URL/token setup is the supported connection and build/test consumers remain usable | 6 | Execute documented client configuration against the owned TLS test endpoint, run migrated feature and affected build/script consumers |

Removal has no executable absence assertion. Its product-wide reading is below.
Expected authentication and authorization refusals are active product behavior,
so their negative assertions remain. A client process-exit assertion observes
retired behavior and is deleted, not translated into another absence assertion.

## Ordered slices

All slices are planned. Each has one capability and a bounded proof loop. The
~5-minute target includes edits, focused proof and local cleanup; 6–10 minutes
requires scrutiny. Stop and refine at >10 minutes of reducible work. The
required all-backend test run and an isolated E2E cold start are explicit
verification-time exceptions, not permission for unbounded implementation.
Reuse unchanged accepted proof under the execution workflow; never claim a
partial test selection satisfies the backend skill's full-suite requirement.

### 1. Discover the service with a personal token
Type: Behavior
Status: planned
Proof: Real SDK initialization and `tools/list` through the application's servlet;
HTTP-level token, Origin, and request-context assertions; all backend tests.

Given a valid Donut personal token, connecting to `/api/mcp` negotiates MCP and
lists the currently registered tools. Every request validates its token; a
missing, invalid, or withdrawn token gets 401 without a sign-in redirect.
A deliberately invalid supplied Origin gets 403. Cookies cannot substitute for
the token. Register SDK lifecycle, servlet request context, protocol boundary,
and deliberate failure mapping together. Start with an empty tool registry;
later slices add capabilities without placeholder handlers. The running-app
check is the first integration gate: if context/transaction assumptions fail,
stop dependent slices and refine this same plan rather than widening auth.

Sizing: 6–10 minutes of implementation, plus the required backend suite. The
SDK scheduling choice is already observed; servlet/filter integration is new
behavior owned here. No note data can be served before the gate is proved.

### 2. Find accessible note candidates
Type: Behavior
Status: planned
Proof: MCP search calls using real search/permission fixtures, including alias,
subscription, equal-title disambiguation, empty search, and two-token isolation;
all backend tests.

Given a user's token and query, `list_note_candidates` delegates to existing
search semantics and returns the existing MCP candidate shape in ranking order.
Exercise parallel or interleaved authenticated calls at the actual HTTP boundary
so a test-injected `CurrentUser` cannot conceal an identity leak. Keep the
current local client journey green during this additive step.

Sizing: ~5 minutes of adapter work plus required backend verification. Reuse
existing data builders; do not create a parallel search algorithm or suite for
internal mapping helpers.

### 3. Read a selected note with its accepted revision
Type: Behavior
Status: planned
Proof: MCP full read and REST regression through the shared operation, using
Git-backed note fixtures; refused foreign private note; all backend tests.

Given an accessible note ID, `read_note` returns the canonical attributed
content. Extract only the existing read responsibility required by this second
caller and retain its transaction boundary. Prove an accepted edit changes a
later notebook revision, an unbound notebook has null revision, and another
user's private note is refused. Keep content as evidence in tool descriptions
and use the current note-ID/revision citation contract.

Sizing: 6–10 minutes plus backend verification. Shared extraction belongs in
this behavior slice because its immediate consumer and proof are here.

### 4. Retrieve bounded context for a readable note
Type: Behavior
Status: planned
Proof: MCP graph calls at small/large body budgets and active argument/access
refusals; reuse the existing domain fixtures; all backend tests.

Given a readable focus ID and token limit, `get_note_graph` delegates to the
existing retrieval configuration and service. Preserve focus truncation,
permitted related context and budget meaning. Enclose data access in the needed
read transaction. Prove a foreign private focus is refused and foreign private
related content is filtered under the same existing rule. Keep the established
argument-error outcomes useful without inventing new content restrictions.

Sizing: ~5 minutes of adapter work plus backend verification.

### 5. Use the complete knowledge journey over HTTPS
Type: Behavior
Status: planned
Proof: Migrated `e2e_test/features/mcp/mcp_services.feature` using a real
TypeScript SDK Streamable HTTP client over verified TLS, the owning E2E origin,
and real issued tokens; new discovery and user-isolation observations.

Replace the E2E connection mechanism with URL-and-token HTTP transport. Keep
candidate, read/revision, and graph assertions at their current external
boundary, adding equal-title disambiguation and identity cases that the baseline
did not establish. Supply a test-owned TLS endpoint forwarding to the isolated
application origin, with a test CA trusted only by that client; do not disable
TLS verification globally or use a real user's credentials. Reuse existing
runner lifecycle/port ownership, with cleanup after failure and disconnect.
The TLS endpoint is a test fixture, not a product service. Migrate worktree
isolation proof to the remote connection; delete its process-exit expectation.

Sizing: 6–10 minutes of cohesive client/fixture work plus E2E startup. A failure
of TLS/protocol interoperability stops retirement. Do not count an HTTP-only
pass, mocked tool response, or health check as this slice's acceptance.
Interim: the local package remains supported until slice 6; the duplicated
transport is temporary and is removed there.

### 6. Configure the supported HTTPS connection
Type: Behavior
Status: planned
Proof: Execute the documented SDK/compatible-client URL and token setup against
the owned TLS endpoint; migrated MCP feature and affected build/script checks.
Separate acceptance reading: transitive product-wide cleanup below.

Given the connection instructions, a user supplies the deployment HTTPS URL
ending in `/api/mcp` and their personal bearer token and can complete the proved
journey. Document token withdrawal and authentication failure recovery without
introducing OAuth setup. Delete the standalone package, its exclusive tests,
stdio process/bundle fixtures, commands, CI bundle/cache entries, workspace
membership, dependencies and instructions transitively. Repair README, both
agent instruction files, stack guidance, format/build scripts and lockfile as
needed. Keep the MCP client SDK and shared API package wherever surviving
callers need them. End the supported setup with one backend service.

Sizing: 6–10 minutes of mechanical retirement/docs, plus affected verification.
If the consumer trace exposes a new functional owner or longer implementation,
stop and refine before continuing. Do not deliver dangling package references.

## Consumer trace and local verification

The inspected connection path is feature → `mcpAgentActions.connect` →
`mcpClientTasks` → `support/mcp_client.ts` → stdio transport → tool handlers →
generated backend API. The read assertion parses the returned `acceptedRevision`
and checks a Git SHA, then compares a later read after an accepted edit. Graph
assertions inspect focus/related output at two budgets. These survive migration.

- Run: migrated MCP feature, `mcpAgentActions`, connection task and SDK-client
  consumers. Replace process-only setup/teardown and delete exclusive fixture
  tests (`mcp_stdio_fixture.mjs`, bundle/process-exit tests). Retain the client
  lifecycle assertions justified by the new HTTP connection.
- Run: the `scripts/mcp-isolation-proof.mjs` / isolated-Cypress consumers and
  applicable browser-worktree script suite. Their purpose is owned-origin and
  data isolation, not a local-server subprocess. Preserve marker/barrier
  semantics and carry paired isolation proof where those observations change.
- Run: root package scripts, workspace/lockfile, formatting selection and CI
  references affected by package deletion. Keep shared `donut-api` and client
  SDK dependencies where CLI or E2E consumers still use them.
- Reuse: unchanged domain assertions for search, attribution, permissions and
  retrieval, through the required backend suite. New MCP boundary tests prove
  the adapter rather than duplicating every internal algorithm test.
- Excluded from extra local UI suites: no Vue behavior changes are planned.
  If implementation changes a shared consumer, revise this disposition before
  accepting proof. Hosted CI still owns its full configured checks.

At each implementation diff, run the installed `consumer-trace.mjs` with
`--checkout <execution-checkout> --base HEAD` and explicit terms including
`spawnAndConnectMcpServer`, `bundleMcpServer`, the connection step phrase, and
`the spawned MCP server process has exited`; follow returned phrases/callers
until the observed journeys are accounted for. Classify each as run, reused,
or excluded for a stated boundary reason. A removal sweep is not a test.

Commands (through Nix; use the actual execution checkout's isolated allocation):

- Backend: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` — all backend
  tests required by `.agents/skills/backend/SKILL.md`; use `makeMe`, real DB and
  MockMvc/servlet dispatch as appropriate under backend-testing/unit-testing.
- MCP journey: `CURSOR_DEV=true nix develop -c env SUT_TIMEOUT_MS=240000 pnpm cy:run --spec e2e_test/features/mcp/mcp_services.feature`.
- Changed script consumers: `CURSOR_DEV=true nix develop -c pnpm test:browser-worktree-isolation`,
  after its package-only test entry is deleted with that owner. Select other
  script tests from the actual consumer trace, not a global suite by default.
- Generated API, when endpoint/DTO descriptions change:
  `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`.
- Execution delivery follows AGENTS.md: Jidoka → fresh post-change-refactor
  agent → needed API generation → coordinator runs
  `./scripts/run.sh pnpm format:changed` once → update this plan → agent commit
  with independent hook → push. Implementers/refactorers do not format or run
  standalone `lint:changed`. Execution owns asynchronous CI and final wrap-up.

## Retained preparation observations

All observations below were made on 2026-10-27 in the named preparation
checkout at the recorded revision, with only the stated disposable mutation.
They are preparation evidence, not completed implementation slices.

1. **Existing domain journey and proof route.** Inspected setup:
   `e2e_test/features/mcp/mcp_services.feature`, `mcpAgentActions`, connection
   tasks and client support. Literal command:
   `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/mcp/mcp_services.feature`.
   Initial 120-second startup timed out during backend/JPA startup, before any
   scenario. No MySQL connection error was observed. A single bounded retry with
   `CURSOR_DEV=true nix develop -c env SUT_TIMEOUT_MS=240000 pnpm cy:run --spec e2e_test/features/mcp/mcp_services.feature`
   reached the consuming assertions: seven passed, zero failed (9 seconds of
   scenarios). Environment: isolated E2E DB
   `doughnut_e2e_wt_d550998681224fa4bf34f8a3bd3c36f8`, origin
   `http://127.0.0.1:63941`, Electron 146/Cypress 16.2.0. The paired
   `@mcpIsolationProof` scenario was excluded by normal selection; concurrent
   worktrees, parallel users, and HTTPS were not observed by this run.
2. **Attribution proof discrimination.** In `read-note.ts`, temporarily replaced
   `jsonResponse(response.data)` with a copied response whose `acceptedRevision`
   was null. Repeated the exact 240-second command above: six passed, the read
   scenario failed at `expected null to match /^[0-9a-f]{40}$/`. Restored the
   original bytes and rebuilt with
   `CURSOR_DEV=true nix develop -c pnpm -C mcp-server bundle`. No product changes
   remain. This proves revision-presence discrimination; it does not claim that
   every search/graph assertion was mutation-tested.
3. **SDK scheduling and dependency compatibility.** Disposable standalone Gradle
   probe inside `.mcp-refinement-probe`, using Boot BOM 4.1.2, Java 25.0.3, MCP
   SDK/core/Jackson3 2.1.0 and Spring Mock servlet requests. Literal command:
   `CURSOR_DEV=true nix develop -c ./backend/gradlew -p .mcp-refinement-probe run --console=plain --max-workers=1`.
   Its first compile identified the released builder names (`messageEndpoint`,
   map-valued `inputSchema`); corrected the probe from the downloaded sources.
   It bound `ServletRequestAttributes`, called the real servlet's `service` with
   JSON-RPC `tools/call` and successive synthetic A/B Authorization headers.
   Default sync execution returned `MISSING` twice; `immediateExecution(true)`
   returned each request's correct header, all HTTP 200. No Donut token, DB,
   embedded app, TLS, concurrency, or TypeScript-client compatibility was proved.
   Probe files were deleted before the seed/state write. Sources inspected:
   released `mcp-core-2.1.0-sources.jar`, `HttpServletStatelessServerTransport`,
   `McpStatelessServerFeatures`, `McpServer`; direct dependency support is
   observed, full application integration belongs to slice 1.
4. **Existing deployment TLS and API routing.** Literal command:
   `CURSOR_DEV=true nix develop -c curl --fail --silent --show-error --max-time 20 --output /dev/null --write-out 'HTTPS health: %{http_code}; TLS verification: %{ssl_verify_result}\n' https://doughnut.odd-e.com/api/healthcheck`.
   Result: HTTP 200, TLS verification 0. Read
   `infra/gcp/path-routing/doughnut-routing.json`: `/api/` belongs to backend.
   This establishes existing HTTPS/API routing only, not the future MCP route.

Host decision before the baseline: local machine load ~10 and 47% free memory;
no duration baseline for this fresh checkout. One feature selected, no paid
services; one E2E owner. A small single-worker disposable Gradle probe also ran
while the first stack started. After timeout, the retry ran with cached build
and a bounded longer startup window; no unrelated processes were stopped.
Recheck load and competing work before execution's backend/E2E proof.

## Removal acceptance reading

After HTTPS behavior passes, read the whole product for the retired package
name/path, stdio connection and bundle symbols, process fixture/tag/step, and
exclusive uses found transitively. Cover backend/frontend/CLI/MCP, E2E and
runner scripts, manifests/lockfile, CI/deployment, README/docs/comments, agent
skills, both AGENTS.md and CLAUDE.md, and generated artifacts. Delete obsolete
dependent material and repair consumers. Retained observation provenance stays
literal: classify planning/history matches separately from live product
references. No renamed historical action, permanent absence test, runtime guard,
or "used to" product note is acceptable. Record the search and classified
result here once as a reading at acceptance.

## Concern and sizing assessment

Slice-plan refinement was not needed: the six leaves each own one external
capability/proof loop, and the common model remains token → current user →
existing knowledge operation → protocol result. No speculative registry,
permission system, retrieval fork, or independent host is introduced.

The new servlet integration and HTTPS journey remain implementation obligations,
with stop gates in slices 1 and 5; neither is claimed as already delivered.
Current-state premises supporting reuse and proof routing were observed as above.
No remaining source decision, unmapped promise, or unresolved slice-boundary
concern was identified. Story estimate remains 2–4 hours including repeated
required backend verification, independent refactoring and delivery. If cleanup
or a gate exceeds its reducible-work budget, refine this plan before proceeding;
if the story exceeds that band, reassess its boundary with the owner.
