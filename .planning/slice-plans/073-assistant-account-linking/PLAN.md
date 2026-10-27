# Link an assistant to Donut through GitHub sign-in

**Identity:** SEED-002#story-2

**Source:** [Refined story](../../seeds/SEED-002-host-mcp-over-https.md#story-2)

**Preparation checkout:** `/Users/terryyin/git/doughnut/.worktrees/link-an-assistant-to-donut-through-github-sign-i`

**Observed revision:** `9c830fadb68b3299766a644dfa92ed48a39f3c73`

**Prepared:** 2026-10-27. Preparation only; execution needs a separate instruction.

## Goal and scope

An existing Donut user links a pre-registered confidential assistant through
GitHub sign-in and Donut consent, uses the HTTPS knowledge tools, renews access,
and withdraws the assistant's permission. No personal Donut token is copied.
Credentials represent that user's current read permission, for this client and
the canonical MCP resource; they cannot authorize general API mutations.

Deliver the seed's five examples, including consent refusal, current notebook
permissions, user isolation, restart durability, and fresh consent after
withdrawal. Preserve existing personal tokens and ordinary browser sign-in.
The supplier agreement in the seed owns the prerequisite: story 1 must deliver
the actual HTTPS MCP service before this story starts. Do not replace it with
a protocol stub or treat library probing as satisfaction of the dependency.

Public-client renewal, dynamic/self-service registration, first-time signup
during linking, notebook-specific consent selection, editing, new retrieval
semantics, and ChatGPT packaging are deferred. There is no invented runtime
limit of one client. This plan authorizes neither implementation by itself nor
production deployment, real-account authorization, or paid calls.

## Decisions and reuse

- Terry Yin approved both presented recommendations on 2026-10-27: a
  pre-registered confidential hosted client, and two discovery-only exceptions
  to [ADR 0005 — Web routes](../../../docs/adrs/0005-web-routes-accepted.md):
  `/.well-known/oauth-authorization-server` and
  `/.well-known/oauth-protected-resource/api/mcp`. Keep authorization, exchange,
  revocation, grant management, and MCP under `/api`; Vue screens use the named
  route registry. Carry this human-owned exception into maintained OAuth docs
  before deleting this plan. No other ADR change is approved.
- Use Spring Security authorization-server support, initially the observed
  7.1.2 under Boot 4.1.2, with S256 PKCE, exact registered redirect URIs,
  confidential-client authentication, and opaque reference tokens. Reuse the
  maintained protocol handlers and JDBC authorization/consent storage; do not
  implement OAuth as a wrapper around personal tokens.
- PFE: `CurrentUserFetcherFromRequest` / `UserService` resolve Donut users;
  `AuthorizationService` owns notebook access. Consume story 1's bearer-only
  MCP ingress and user-scoped operations. Add OAuth credential validation at
  that boundary without creating a second note-permission system. Keep valid
  personal tokens supported. No singleton current user or loopback introspection.
- PFE: `UserToken` has user/token/label, not OAuth consent, resource, scope or
  expiry. Keep its lifecycle separate. The CLI's Gmail flow is an outbound
  client, not an issuer. Existing access-token settings provide a navigation
  precedent; the connected-assistant view reads OAuth grants, not token labels.
- One grant model binds user, client, canonical resource, read scope, and token
  lifecycle. Reuse one resource/scope rule at authorization, code exchange,
  renewal and resource consumption. The observed library defaults do not
  enforce MCP resource binding. Validate the requested resource, preserve its
  binding in authorization state, and prevent subsequent exchange/renewal from
  widening it. Use explicit issuer/resource configuration, not an untrusted Host.
- Consent identifies the Donut account and registered client and describes
  current readable notebooks, renewal and withdrawal. Protect consent and
  withdrawal against CSRF. Treat deliberate OAuth refusals as protocol outcomes;
  unexpected failures retain [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md)
  reporting. Secrets and token values never appear in browser grant lists or logs.
- Withdrawal is for the current user and client, across its outstanding grants
  and remembered consent. Serialize withdrawal against renewal so a racing
  refresh cannot resurrect access. A request starting after completed withdrawal
  must fail; already authorized reads and previously disclosed data cannot be
  recalled. Use durable checks without a stale per-process permission cache.
- [ADR 0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
  preserves accepted-revision attribution; [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
  governs isolated tests. The North Star's accepted-content boundary applies;
  this story changes no content authority and needs no new North Star topic.
- Schema release safety follows `db-migration`: non-test Flyway runs on
  `ApplicationReadyEvent`. Retain the schema-only commit from slice 2 as an
  immutable release candidate. Release and verify its tables before shipping
  code that initializes or uses those tables. Later implementation may proceed
  locally on migrated disposable data; it does not waive the two-release rule.
  Deployment uses separately authorized `release-application`, not this plan's
  ordinary Git publication. Do not change migration startup ownership here.

## Outside-in proof ownership

| Promise | Slice | Observable proof |
| --- | --- | --- |
| Actual GitHub sign-in returns to the expected existing Donut identity | 1, 4 | Credentialed early probe at existing login boundary, then preserved continuation into real consent; provider fixtures cover repeatable callbacks |
| OAuth discovery through the public HTTPS routing boundary | 3 | SDK follows MCP 401 resource-metadata pointer to issuer metadata and configured endpoints, with TLS verification |
| Recognizable account/client/read consent, Allow/Deny and retry | 4 | Browser drives real consent; client receives a bound code or access_denied; CSRF refusal at HTTP boundary |
| Client, PKCE, redirect, code reuse and resource binding | 4, 5 | Real Spring filter-chain requests with real persisted grants; good exchange succeeds, changed security inputs fail |
| Scoped read-only MCP use with current permissions and two-user isolation | 6 | Interleaved SDK search/read/graph for two users, foreign-note refusal, permission loss, and attempted general-API mutation/token creation |
| Expiry and renewal preserve client/resource/scope; invalid refresh requires reconnect | 7 | Expired access is refused, valid refresh restores MCP read; widening and invalid refresh fail through token endpoint |
| User withdraws all access for that assistant and later consents again | 8 | Browser withdrawal then SDK call/refresh refusal, fresh consent, unaffected second user's access, and a coordinated refresh/withdraw race |
| Durable consent/grants across process restart | 2, 9 | Real MySQL persistence; stop/restart only the owned E2E backend with the same database and resume using retained client credentials |
| Documented confidential-client setup completes the full journey | 10 | Real SDK consumes documented metadata and PKCE flow, then search/read/graph, expiry/renewal and withdrawal over HTTPS |

No internal service mock may supply issuance, permission, renewal or revocation
that a row claims to prove. Mock GitHub only at its external HTTP boundary for
automated runs. Synthetic OAuth principals prove filter behavior, not a real
GitHub callback. Use deterministic token clocks/short test lifetimes rather
than sleeping through production lifetimes. Existing retrieval tests remain
owned by story 1; this story adds proof of the new authorization path.

## Ordered slices

Target ~5 minutes of edits, focused verification and cleanup per leaf; scrutinize
6–10 minutes and refine at >10 minutes of reducible work. Required full backend
tests, E2E cold starts, and operator-owned credential/release waits are explicit
time exceptions. None authorizes accumulating unbounded implementation.
Keep registration limited to test-owned clients until the complete lifecycle
is proved; no production client is activated by an intermediate slice.

### 1. Observe the GitHub return and identity boundary
Type: Behavior
Status: planned
Proof: One bounded credentialed observation of existing browser sign-in returning
to a same-origin continuation and the expected Donut user in isolated E2E data.

This is the permitted early probe for an owner-held premise, not a claim that
account linking is implemented. After the supplier is available, establish one
owned HTTPS E2E origin and a GitHub test application whose callback exactly
matches it. Reuse the existing GitHub login mechanism and production-equivalent
provider settings without enabling production testability. Seed the expected
existing Donut identity in the disposable database. Use the supplier's TLS and
stack lifecycle; no public tunnel or production deployment is implied.

The coordinator first requests the complete operator round: provide approved
test-app configuration by the normal secret channel, sign in with the named
test account in a fresh browser session, and report the returning Donut account
and continuation. The agent records the callback response and resolved user
without secrets. The question is whether real GitHub identity/return behavior
works for this deployment shape; a fixture login cannot answer it. If access,
callback registration or identity return is unavailable, stop dependent slices
and revise this plan; do not substitute Basic auth and call the premise settled.
Reuse this evidence later unless the callback or identity path changes.

Sizing: one observation, ~5 minutes of agent setup plus operator/credential wait.

### 2. Persist assistant authorization state
Type: Structure
Status: planned
Proof: MySQL migration and JDBC grant/consent round-trip using the released model;
all backend tests and regenerated ERD; existing login/token behavior unchanged.

Add the minimum compatible schema for registered clients, authorizations and
consents, using a new Flyway version after the actual current maximum. Include
the storage/indexing needed to withdraw grants by user/client coherently. Keep
the account identity linkage consistent with user deletion rules. Do not add
runtime database consumers in this schema-only commit. It enables slice 3's
configured issuer/client and is the safe release point described above.
Record the schema commit and separately authorized release/verification before
code depending on it is released; lack of deployment authority is not permission
to combine the releases. No data migration from personal tokens is needed.

Sizing: 6–10 minutes plus mandatory backend migration proof and ERD generation.

### 3. Discover the configured authorization service
Type: Behavior
Status: planned
Proof: Unauthenticated MCP request → 401 challenge → protected-resource metadata
→ authorization-server metadata through verified TLS and the routing manifest.

Configure the in-backend issuer and one confidential client with a registered
callback, read scope and PKCE. Wire persistent stores only against migrated
databases. Add the two approved exact metadata routes to the shared routing
manifest and expose issuer/resource metadata plus the MCP challenge. Keep
operational paths under `/api` and describe their boundary in OpenAPI's source
generation path. Do not advertise dynamic registration or unsupported scopes.
The SDK must consume discovery; a curl 200 or route-table assertion alone is
insufficient. Preserve personal-token MCP authentication and invalid-Origin
handling. Ordinary MCP requests still get an auth challenge, not a login page.

Sizing: 6–10 minutes plus backend, routing and focused SDK checks.

### 4. Decide an assistant's read access after GitHub sign-in
Type: Behavior
Status: planned
Proof: Real browser consent through the authorization filter chain, both Allow
and Deny/retry; saved request resumes after provider callback with the same user.

A valid authorization request resumes through GitHub sign-in to a Vue consent
screen. Show client, account, permitted reading and ongoing access/withdrawal.
Bind pending consent to the authenticated session and validated client,
redirect, resource and requested read scope. Allow issues a short-lived code;
Deny returns access_denied to the valid callback. Persist consent through the
library's model. An invalid redirect is refused without forwarding a code to it.
Missing CSRF protection or tampering cannot approve consent. Existing profiles
and normal sign-in remain intact; no parallel account registration is added.

Automate callback behavior with a test-owned external GitHub fixture while
retaining the real OAuth client/security chain. Reuse slice 1's native evidence
for unchanged provider identity; if continuation wiring changes that path,
repeat only the affected return observation during this slice. A test-injected
principal or Basic-auth shortcut does not prove the callback/continuation.

Sizing: 6–10 minutes plus backend/frontend proof and the bounded browser round.

### 5. Exchange a code for a constrained credential
Type: Behavior
Status: planned
Proof: HTTP code exchange issues an opaque credential bound to user, client,
read scope and MCP resource; invalid secret/PKCE, wrong resource and reuse fail.

Complete the confidential-client code exchange through the maintained handler.
Use the common bound-resource rule rather than the library's default client-ID
audience. Preserve the GitHub-backed Donut subject in the grant; the resource
consumer must resolve that subject, not the client principal. Configure finite
access/refresh lifetimes and rotation, with shorter deterministic test settings.
Do not return credentials to the consent screen or log them. Keep deliberate
protocol failures distinct from unexpected server failures.

Sizing: ~5 minutes of binding/configuration plus full backend verification.

### 6. Read MCP knowledge under the delegated permission
Type: Behavior
Status: planned
Proof: Real OAuth bearer calls to the three MCP tools, two-user isolation and
changed notebook permission; misuse at general API mutation/token endpoints fails.

Extend story 1's request-scoped bearer gate to accept active OAuth reference
tokens for this resource/read scope, resolving the existing user and reusing all
knowledge operations. Interleave users through real HTTP calls. Search, direct
read and graph each honor current notebook permissions and accepted attribution.
An expired/revoked/wrong-resource credential cannot fall through to a browser
session. OAuth credentials cannot become general personal tokens or authorize
non-MCP mutations even when a session cookie accompanies them. Retain ordinary
browser-session and personal-token use through their existing paths.

Sizing: 6–10 minutes plus full backend suite and focused MCP proof.

### 7. Renew access without enlarging permission
Type: Behavior
Status: planned
Proof: The actual token endpoint renews expired access and the SDK reads again;
expired/reused/invalid refresh and scope/resource widening produce OAuth refusal.

The confidential client authenticates a refresh request against an active grant.
Rotate the refresh credential, keep user/client/resource/read scope unchanged,
and enforce current grant state. The client saves the replacement and retries
MCP without browser login. An unusable refresh response gives a reconnect path.
Exercise expiry with a controlled clock or test lifetime; do not wait on a
production expiry or manufacture renewal by replacing the client token fixture.

Sizing: ~5 minutes plus backend and focused client verification.

### 8. Withdraw a connected assistant
Type: Behavior
Status: planned
Proof: User withdraws in settings; all their grants for that client fail on next
call/refresh, another user's grant works, and reconnect requires fresh consent.

Add connected-assistant settings with a client name and read permission, never
token values. The browser's CSRF-protected withdrawal acts only for the current
user and chosen client, invalidating all outstanding authorizations and removing
remembered consent atomically. Repeated withdrawal leaves access withdrawn.
Serialize against refresh and prove a controlled race cannot leave an active
replacement token after withdrawal completes. Drive owner refusal through the
HTTP boundary. Keep personal-token management separate and functioning.

Sizing: 6–10 minutes plus backend/frontend proof. If race handling needs a new
model instead of using the shared grant transaction, stop and refine this leaf.

### 9. Keep authorization valid across a backend restart
Type: Behavior
Status: planned
Proof: Obtain credentials, restart only the owned E2E backend without resetting
its MySQL database, then renew and read with the retained client credential.

Exercise the promised durability at a process boundary, including persisted
consent and subject/resource binding. Browser sessions are not required for
normal MCP calls or refresh. Follow with withdrawal and another fresh backend
process to prove its persisted effect. Use runner-owned lifecycle controls and
restore/stop the test stack on success and failure. Do not touch Development,
another worktree, or production. A new service instance in one JVM is useful
library evidence but cannot stand in for this process-restart proof.

Sizing: ~5 minutes of test integration plus two owned startup waits; stop and
refine if runner changes exceed the bounded lifecycle seam.

### 10. Connect through the documented OAuth journey
Type: Behavior
Status: planned
Proof: A real SDK confidential client follows the published setup over HTTPS:
discovery → GitHub/consent → exchange → search/read/graph → renewal → withdrawal.

Complete a focused `e2e_test/features/mcp/assistant_account_linking.feature`
through the real SDK transport/auth provider and actual backend resource,
registered as an assessed isolated feature. Reuse the supplier's TLS/client
lifecycle. The automated external-provider fixture owns only GitHub responses;
reuse the native provider observation from slices 1/4 for real GitHub coverage.
Read actual note text with accepted revision and graph context, not a stub tool.
Add client-side state/callback validation and persistence of renewed credentials
where the SDK integration requires them; do not handwrite the OAuth client.

Document operator client registration/secret provisioning, issuer/resource and
callback configuration, schema-first release, user consent/withdrawal and
reconnect behavior. Demonstrate those instructions with the supported SDK
client; ChatGPT packaging stays with story 3. Keep personal-token instructions
usable. Preserve the explicit discovery-route exception in feature documentation.
Activate a real client only after separately authorized release and complete
lifecycle evidence; this slice creates no deployment authority.

Sizing: 6–10 minutes of cohesive client/journey/docs work plus E2E startup. If
there is a client incompatibility, stop here and revise; neither mocked tools
nor an HTTP-only run counts as the HTTPS acceptance journey.

## Consumer trace and verification commands

The inspected paths are security configuration → GitHub principal →
`CurrentUserFetcherFromRequest` → `UserService`/`AuthorizationService`;
`/login/continue` → `ApplicationController.loginContinue`; and token settings
→ generated `UserController` client. MCP's current E2E client is stdio at the
observed revision; story 1 owns its replacement. Reinspect the supplied HTTP
client and ingress after that dependency lands instead of copying old support.

- Run: existing browser login/profile and personal-token boundary tests, plus
  new authorization MockMvc tests through real security filters and real MySQL.
  `SpringTestBase` already provides MockMvc; do not multiply contexts with
  redundant annotations. Follow its session-row cleanup rules for authenticated
  nontransactional tests. Cover notebook authorization through the new bearer path.
- Run: mounted consent/connected-assistant tests and both frontend typechecks;
  backend calls use generated SDK wrappers. Reuse existing settings navigation
  and load-state conventions. New components require the normal Vite-generated
  component declaration update.
- Run: routing classifier, rendered production URL map and routing validation
  for exact discovery paths; OpenAPI generation/lint for changed API contracts.
- Run: the new focused OAuth E2E feature and existing MCP feature. Extend
  runner/client lifecycle tests when TLS, external-provider or restart ownership
  changes. No real GitHub credential enters ordinary CI.
- Exclude extra CLI behavior work: personal token semantics remain unchanged.
  If a shared API/auth change reaches CLI consumers, revise this disposition
  and run their affected boundary tests. No full E2E suite is a local gate.

At each execution diff, run the installed `consumer-trace.mjs --checkout
<execution-checkout> --base HEAD` with terms `CurrentUserFetcherFromRequest`,
`login/continue`, `Authorization`, `generateToken`, and the MCP connection step
phrases. Follow returned callers to observable journeys and classify each as
run, reused or excluded with a boundary reason. No removal is planned here.

Commands use the actual owned execution checkout:

- `CURSOR_DEV=true nix develop -c pnpm backend:verify` for schema changes;
  `CURSOR_DEV=true nix develop -c pnpm backend:test_only` otherwise. Both run all
  backend tests as required by the backend skills. Use `makeMe` and the real DB.
- `CURSOR_DEV=true nix develop -c pnpm export:database-erd` after migrations.
- `CURSOR_DEV=true nix develop -c pnpm frontend:test`, then
  `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit` and
  `CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit` on the same
  generated API content. A focused file is allowed during iteration only.
- `CURSOR_DEV=true nix develop -c pnpm test:path-routing` and
  `CURSOR_DEV=true nix develop -c pnpm validate:path-routing` for routing edits.
- `CURSOR_DEV=true nix develop -c pnpm generateTypeScript` after API changes;
  never hand-edit generated artifacts. Run `pnpm openapi:lint` through Nix.
- `CURSOR_DEV=true nix develop -c env SUT_TIMEOUT_MS=240000 pnpm cy:run --spec e2e_test/features/mcp/assistant_account_linking.feature,e2e_test/features/mcp/mcp_services.feature`
  after registering the new feature with isolated selection. The existing
  wrapper owns E2E data/ports and required external fixtures.
- AGENTS.md delivery: Jidoka → fresh post-change-refactor agent → required API
  generation → coordinator `./scripts/run.sh pnpm format:changed` once → update
  plan → agent commit with independent hook → push. Execution owns CI observation
  and eventual Story Wrap Up; preparation does not start an observer.

Inspect host load/memory and competing owned work before full backend or E2E
runs; sequence those against the checkout's build output. Reuse accepted proof
on unchanged content. A failed required suite cannot be replaced by a narrow pass.

## Retained preparation observations and remaining premises

The source story retains the exact probe inputs, command, revision, source
digest, results and limits from 2026-10-27 in this preparation checkout. The
consuming operations actually reached were Spring's security-filter metadata,
authorization/code exchange, refresh and revocation, plus MySQL 8.4.11 JDBC
save/reload. Boot 4.1.2 resolved Security 7.1.2. A confidential client received
refresh credentials; a public client did not. Default resource/audience behavior
was insufficient, so slices 4–7 explicitly own resource binding. JDBC retained
the synthetic GitHub-shaped principal and active refresh token; process restart
and whole-client withdrawal remain slices 8–9, not proved by that observation.

Consent was disabled and a synthetic principal supplied in the probe. No
claim of real GitHub login or Donut MCP integration follows. Actual GitHub
configuration/account access is owner-held and bounded by early probe slice 1;
the future callback continuation is owned by slice 4. The real MCP resource is
the supplier agreement, not a readiness reason duplicated in this plan.

After the routing approval, an in-memory mutation of the routing hints was
observed from this checkout with `CURSOR_DEV=true nix develop -c node
--input-type=module` (stdin program). It loaded `doughnut-routing.json`, called
`pathGoesToBackend` for both specified paths, added those paths to a cloned
`exactPaths`, then called `pathGoesToBackend` and
`backendMigRouteRulesFromHints`. Both baseline paths selected non-backend;
both amended paths selected backend and emitted exact rules targeting the
configured backend service. No source file changed. This settles shared
classification/rule generation only; SDK discovery through the actual HTTPS
deployment belongs to slice 3. Production routing is not claimed changed.

The installed TypeScript SDK 1.32.2 was also exercised in this checkout with
`CURSOR_DEV=true nix develop -c node --input-type=module` (stdin program).
Its public `auth` entry point consumed synthetic protected-resource and issuer
metadata via a supplied fetch function, using one pre-registered confidential
client and an HTTPS callback. Observed `REDIRECT` with S256, the requested
resource and state; then `AUTHORIZED` for code exchange with Basic client
authentication and the saved verifier; then `AUTHORIZED` for refresh, sending
the same resource and saving the replacement refresh credential plus issuer.
All assertions passed. This settles client API support for the chosen flow;
synthetic responses prove no server enforcement, browser, real GitHub, network
TLS, or note operation. Those remain the mapped execution proofs. No source
or credential file was created by this observation.

Repository reading confirmed `FlyWayFreeVersionRealMigration.actualMigration`
is an ApplicationReadyEvent listener, matching the migration skill's explicit
two-release requirement. Release/migration confirmation is state-changing and
operator-owned; slice 2 retains the schema-only boundary and withholds dependent
runtime release until that confirmation. No secret or deployment access has
been observed or assumed available.

## Concern and sizing assessment

Slice-plan refinement was not needed: the ten leaves have bounded outcomes and
proof loops. Schema preparation directly enables the configured service;
discovery, consent, exchange, resource use, renewal, withdrawal and restart
exercise one grant lifecycle rather than accumulating special-case models.
The final journey composes that same protocol using the actual resource.

The user settled the two source decisions. Credentialed GitHub observation is
bounded by the permitted early probe, and schema deployment has an explicit
safe release gate; neither is represented as already observed. No remaining
unmapped promise, disputed constraint or slice-boundary concern was identified.
Estimate remains 2–4 hours of implementation/verification, excluding operator
waits. Reassess if reducible work exceeds that band or any leaf's hard limit;
do not drop renewal, withdrawal, persistence or real-client proof to fit it.
