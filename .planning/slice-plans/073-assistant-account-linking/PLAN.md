# Link an assistant to Donut through GitHub sign-in

**Identity:** SEED-002#story-2
**Source:** [Refined story](../../seeds/SEED-002-host-mcp-over-https.md#story-2)
**Preparation checkout:** `/Users/terryyin/git/doughnut/.worktrees/link-an-assistant-to-donut-through-github-sign-i`
**Observed revision:** `9c830fadb68b3299766a644dfa92ed48a39f3c73`
**Prepared:** 2026-10-27; execution separately authorized by the established start.

## Execution resume

Agent: Yua-chan; publisher: `dashboard-territory.local-doughnut`.
The preparation checkout is the owned execution checkout, branch
`codex/link-an-assistant-to-donut-through-github-sign-i`, Story Branch Mode.
Integration/default checkout: `/Users/terryyin/git/doughnut`; remote: `origin`;
eventual target: `main`. Ordinary increments publish the execution branch.
Starting revision: `83615a792fe3aa3107badb07d883a70398d75127`.
Startup claim `3d32688d4a040ab4e57e8a5a03d48963e4dc157f` was confirmed on remote main/execution branch.
Published on the execution branch: slice 1 `f0390693df002168b2ce6a81f038bcb5f90931b8`, slice 2 `d488c64dbc69a00c51f2cc9237f71ae9fda016a1`, slice 3 `9562d7c1ae2e965e95aa70afaa0d01f97c98406f`.
Slice 2's SHA is the immutable schema-only release candidate; no deployment is authorized.

`./scripts/run.sh bash scripts/worktree_setup.sh` succeeded with the checkout's
current dependency fingerprint. `CURSOR_DEV=true nix develop -c pnpm validate:path-routing`
passed 11 backend probes/five static paths; this establishes command readiness only.
No live GitHub observation, production deployment, real-account authorization or paid call occurred.

## Scope, decisions and safety

Deliver the canonical source's full contract/approved architecture against story 1's
real HTTPS MCP. [Preparation evidence](PREPARATION-EVIDENCE.md) retains dated inputs,
revisions, commands, results, comparisons, digest and limits.

- Terry's 2026-10-27 clarification selects cohesive reuse of existing GitHub login/identity mapping. No new GitHub scope/API/provider feature is selected; a proposed expansion returns to the owner. Slice 6 proves new callback-to-consent integration with external HTTP fixtures; no live test-app gate applies to unchanged login.
- Reuse Spring Security authorization-server handlers and JDBC stores, S256 PKCE, exact redirects, confidential-client authentication and opaque tokens; initially observed Boot 4.1.2/Security 7.1.2. Never wrap personal tokens as OAuth.
- One grant binds user/client/canonical resource/read scope/token lifecycle. Share resource/scope validation across authorization, exchange, refresh and consumption; use explicit trusted issuer/resource configuration, not Host. Library defaults were insufficient.
- Resolve users with `CurrentUserFetcherFromRequest`/`UserService`, notebook access with `AuthorizationService`, and story 1's request-scoped MCP ingress/operations. Keep personal-token lifecycle separate; no singleton user, second permission system or loopback introspection.
- Consent names client/account/current readable notebooks, renewal and withdrawal. Protect consent/withdrawal with CSRF; preserve deliberate OAuth refusals and ADR 0006 unexpected-failure reporting. Never expose secrets/token values in grant lists or logs.
- Withdraw current user's whole client grant/remembered consent atomically. Serialize against refresh using durable checks, without stale process caches. Requests starting after completed withdrawal fail; already authorized reads/prior disclosure cannot be recalled.
- The source's two exact discovery-route exceptions to ADR 0005 stay narrow; operational paths remain `/api`, Vue screens use named `routeMetadata`. Preserve exception in maintained OAuth docs. ADR 0002 attribution and ADR 0007 isolation remain effective; content authority/North Star are unchanged.
- Non-test Flyway runs on ApplicationReadyEvent. Retain slice 2's schema-only commit as an immutable release candidate; separately release/verify its tables before dependent runtime code is shipped. Local implementation may continue on migrated disposable data. No ordinary Git publication grants production deployment or changes migration startup ownership; release uses separately authorized `release-application`.
- Keep intermediate client registration test-owned until full lifecycle proof; activate a real client only after separately authorized release. No arbitrary one-client runtime limit follows from the first example.

## Outside-in proof ownership

| Promise | Slices | Observable proof |
| --- | --- | --- |
| Existing GitHub login supplies same Donut identity to consent | 1, 6 | Reuse inspection; real chain/provider HTTP fixture callback and saved continuation |
| Public HTTPS discovery | 3, 5 | SDK follows MCP 401 metadata pointer through verified TLS/routing |
| Informed Allow/Deny/retry, CSRF | 6 | Browser drives real consent; code or access_denied |
| Client/PKCE/redirect/code reuse/resource binding | 6, 7 | Real security filters and persisted grants; good exchange and changed-input refusals |
| Current read permission, isolation, read-only scope | 8 | Interleaved SDK search/read/graph; permission loss/foreign-note/general API refusals |
| Expiry/renewal without widening | 9 | Expired access refused; real refresh restores SDK read; invalid/widened requests refused |
| Withdrawal/race/reconnection | 10 | Browser withdrawal, SDK/refresh refusal, fresh consent, unaffected other user, controlled race |
| Durable consent/grants | 2, 4, 11 | MySQL storage then owned backend process restart with retained credentials/database |
| Documented confidential-client journey | 12 | Real SDK over HTTPS from discovery through renewal/withdrawal |

Mock GitHub only at its external HTTP boundary; internal mocks cannot supply
issuance/permission/renewal/revocation proof. Injected principals prove filter
behavior, not callback/continuation. Use controlled clocks/short test lifetimes,
not production sleeps. Story 1 retains retrieval semantics; this adds delegated authorization.

## Ordered slices

Target ~5 minutes including proof/cleanup; scrutinize 6–10, finer-decompose >10.
Full backend suites, E2E cold starts and operator release waits are exceptions.

### 1. Reuse the GitHub login and identity boundary
Type: Behavior
Status: done
Proof: Inspect existing provider/continuation/identity mapping; slice 6 owns new consent integration.

Reading at `3d32688d4a040ab4e57e8a5a03d48963e4dc157f`: preserve production `oauth2Login(Customizer.withDefaults())`/`application-prod.yml` registration.
`signInRedirectHref` → authenticated `/login/continue?from=…` → `ApplicationController.loginContinue`; `CurrentUserFetcherFromRequest` maps principal to existing `externalIdentifier`.
Source-only `/root/source_promises_1` received original Goal/Scope/examples/exclusions plus owner's amendment; C06/C12/C14 select these boundaries.
Reading is not live-provider/consent proof. No product change/test rerun. Sizing: ~5 minutes.

### 2. Persist assistant authorization state
Type: Structure
Status: done
Proof: MySQL migration/JDBC grant-consent round-trip, all backend tests, regenerated ERD; login/personal-token behavior retained.

V300000355 stores the released models with withdrawal indexes and user/client
cascade FKs, without runtime consumers or personal-token migration. Slice 4 uses
migrated disposable data; shipping runtime code retains the schema-first gate.
Sizing: 6–10 minutes plus migration/ERD proof. Source-only `/root/source_promises_2`
separates durable storage from restart; `AssistantAuthorizationPersistenceTest` fresh JDBC stores
reload bound state. Full command: `CURSOR_DEV=true nix develop -c env GRADLE_OPTS='-Dorg.gradle.workers.max=2 -Dorg.gradle.internal.http.socketTimeout=20000 -Dorg.gradle.internal.http.connectionTimeout=20000' pnpm backend:verify`.
Passed 2,901 tests (0 failures/errors, 2 existing skips); the first attempt hit a dependency-fetch timeout before compilation.
ERD command: `CURSOR_DEV=true nix develop -c env DONUT_ERD_SCHEMA=doughnut_wt_c5528f397af6467b8cdbe326355f44f1_test pnpm export:database-erd`.
No production consumer was added. Restart remains slice 11; enforcement remains 6–10.

### 3. Establish the owned HTTPS test origin
Type: Structure
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm test:sut-start` and `CURSOR_DEV=true nix develop -c pnpm -C cli exec tsx --test ../e2e_test/support/mcp_tls_proxy.test.ts` observe pre-spawn configuration, CA/forwarding and success/failure cleanup.

Implemented `startOwnedSutLifetime({ https: true, env })` and invocation `sutEnv`: bind the shared proxy before spawn, explicitly pass `DONUT_CANONICAL_ORIGIN`, retain `tlsProxy`, and close it on shutdown/startup failure; caller environment stays intact.
`sut-https-origin.cases.mjs` observes public batch forwarding, configured origin, cancellation/failure cleanup and readable foreign upstream; source-only `/root/source_promises_3` leaves actual SDK discovery with slice 5.
`CURSOR_DEV=true nix develop -c pnpm test:sut-start`: 77 passed.
`CURSOR_DEV=true nix develop -c pnpm test:browser-worktree-isolation`: 112 passed.
`CURSOR_DEV=true nix develop -c pnpm -C cli exec tsx --test ../e2e_test/support/mcp_tls_proxy.test.ts`: 1 passed.
Approximately 10 minutes implementation plus a bounded 4-minute focused-verification exception for compiler-baseline diagnosis and invalidated-suite reruns; no further scope was added. Compiler limit is G2.

### 4. Bootstrap configured assistant clients and stores
Type: Structure
Status: done
Proof: Real MySQL store/configured-client bootstrap and reset HTTP tests observe client restoration with cleared grants/consent; full `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

`AssistantAuthorizationConfiguration` supplies real JDBC stores without constructor SQL; `AssistantClientBootstrap` runs after migration's highest-priority Ready listener and after reset.
Configured client lists preserve IDs, encoded secrets, exact redirects, confidential Basic authentication, PKCE/consent/read scope/reference-token/rotation settings; no one-client cap or protocol handler was added.
Accepted: source-only `/root/source_promises_4` retains physical restart with slice 11. `TestabilityDbResetTest.resetRestoresConfiguredClientsWithFreshAuthorizationState` drives real MockMvc reset, reloads both configured clients/settings and observes old grant/consent cleared (G1).
Full `CURSOR_DEV=true nix develop -c pnpm backend:test_only`: 2,902 tests, 0 failures/errors, 2 existing skips; reset boundary 2/2. Connector/J 26.10.0 bytecode confirms `preserveInstants=true`; existing UTC URLs remain.
Approximately four minutes implementation plus full-suite verification; slice 5 owns protocol discovery. HTTP signatures and generated contracts are unchanged.

### 5. Discover the configured authorization service
Type: Behavior
Status: planned
Proof: SDK follows unauthenticated MCP 401 → protected-resource → issuer metadata through verified TLS/shared routing.

Use configured client/stores (4) and HTTPS origin (3) for issuer/protected-resource
metadata, approved exact routes and MCP challenge. Describe `/api` operational
endpoints through OpenAPI generation; advertise supported scopes only, no DCR.
Preserve personal-token auth/invalid-Origin handling and MCP auth challenges without
login pages. Curl/route checks alone are insufficient. Sizing: 6–10 minutes plus backend/routing/SDK proof.

### 6. Decide an assistant's read access after GitHub sign-in
Type: Behavior
Status: planned
Proof: Browser Allow/Deny/retry through real authorization chain; external GitHub HTTP fixture callback resumes saved request as same existing user.

Show Vue consent with client/account/current reading/renewal/withdrawal. Bind pending
consent to authenticated session and validated client/redirect/resource/read scope.
Allow issues short-lived code; Deny returns access_denied only to valid callback;
persist library consent. Refuse invalid redirects, missing CSRF and tampering.
Keep existing profiles/sign-in/registration; add no parallel signup. Basic auth or
injected principal cannot prove callback/continuation. Reuse slice 1's existing login.
Sizing: 6–10 minutes plus backend/frontend proof and bounded browser journey.

### 7. Exchange a code for a constrained credential
Type: Behavior
Status: planned
Proof: Real HTTP exchange issues opaque user/client/read/resource-bound credentials; invalid secret/PKCE, wrong resource and code reuse fail.

Use maintained exchange handler/common resource rule rather than default client-ID
audience. Persist GitHub-backed Donut subject; consumer resolves user, not client.
Set finite access/refresh lifetimes and rotation with deterministic short test settings.
Retain global credential/error rules. Sizing: ~5 minutes plus full backend verification.

### 8. Read MCP knowledge under the delegated permission
Type: Behavior
Status: planned
Proof: Interleaved two-user OAuth SDK calls to all tools; current/foreign-note permissions and general mutation/token-creation refusals.

Extend request-scoped bearer gate for active OAuth reference tokens with correct
resource/read scope, using existing user and knowledge operations. Search/read/graph
retain current notebook permissions and accepted attribution. Expired/revoked/wrong-
resource tokens cannot fall through to browser sessions; OAuth cannot authorize
non-MCP mutations or personal-token creation even with cookies. Retain ordinary
sessions and personal tokens. Sizing: 6–10 minutes plus full backend/MCP proof.

### 9. Renew access without enlarging permission
Type: Behavior
Status: planned
Proof: Actual token endpoint refreshes expired access and SDK reads again; expired/reused/invalid refresh and resource/scope widening are refused.

Authenticate confidential client against active grant; rotate refresh, preserve
user/client/resource/read scope/current state. Client saves replacement and retries
without login; unusable refresh offers reconnect. Use controlled clock/test lifetime,
never production sleep or fixture-token replacement as renewal proof.
Sizing: ~5 minutes plus backend/client verification.

### 10. Withdraw a connected assistant
Type: Behavior
Status: planned
Proof: Browser withdrawal stops every owner's client grant next call/refresh; other user works, reconnect needs consent, controlled refresh race leaves no active replacement.

Settings show client/read permission without tokens. CSRF-protected owner-only
withdrawal atomically removes all authorizations/remembered consent; repeated
withdrawal stays effective. Serialize with refresh, drive owner refusal through
HTTP, retain personal-token management. Stop/refine if race needs a new model
beyond the shared grant transaction. Sizing: 6–10 minutes plus backend/frontend proof.

### 11. Keep authorization valid across a backend restart
Type: Behavior
Status: planned
Proof: Obtain credentials, restart only owned E2E backend with same MySQL database, renew/read retained credentials; withdraw then restart again to prove persisted effect.

Cover consent/subject/resource binding without ordinary-call browser sessions.
Runner owns restore/stop on success/failure; never touch Development/peers/production.
New same-JVM instances are insufficient. Stop/refine lifecycle-seam overrun.
Sizing: ~5 minutes plus two owned startup waits.

### 12. Connect through the documented OAuth journey
Type: Behavior
Status: planned
Proof: Real SDK confidential client follows documented HTTPS discovery → GitHub/consent → exchange → attributed search/read/graph → expiry/renewal → withdrawal.

Add `e2e_test/features/mcp/assistant_account_linking.feature` to isolated selection;
reuse TLS/client lifecycle and slice 6 callback proof. Fake only external GitHub responses;
use attributed note/graph data, SDK state/callback checks and renewed credentials.
Document registration/secrets, issuer/resource/callback, schema-first release, consent/
withdrawal/reconnect and discovery exception; demonstrate with SDK, retaining personal-token instructions.
ChatGPT stays story 3. Activate real clients only after authorized release/full proof.
Stop/revise incompatibility; handwritten OAuth, mocked tools or HTTP-only proof are insufficient. Sizing: 6–10 minutes plus E2E startup.

## Consumer trace and verification

Trace security/principal/user/permission, login continuation and token settings.
Reuse supplier's delivered HTTP ingress/client. Per diff run installed
`consumer-trace.mjs --checkout <execution-checkout> --base HEAD` with terms
`CurrentUserFetcherFromRequest`, `login/continue`, `Authorization`, `generateToken`
and MCP connection phrases; follow callers/journeys and classify run/reused/excluded.

- Run existing login/profile/personal-token plus new real-filter/MySQL authorization tests; use SpringTestBase MockMvc and session cleanup, without redundant contexts.
- Run mounted consent/settings tests and both frontend typechecks on same generated API; use generated SDK/loading wrappers, existing navigation and Vite component declarations.
- Run routing classifier/rendered production map/validation; generate/lint OpenAPI for API contracts.
- Run focused OAuth and existing MCP E2E; extend runner/client lifecycle tests for changed TLS/provider/restart ownership. No real GitHub credentials in CI.
- CLI extra behavior excluded while personal tokens unchanged; revise/run affected boundaries if shared auth reaches CLI. Full E2E suite is not a local gate.

- `CURSOR_DEV=true nix develop -c pnpm backend:verify` for schema; `CURSOR_DEV=true nix develop -c pnpm backend:test_only` otherwise. Both run all backend tests with makeMe/real DB.
- `CURSOR_DEV=true nix develop -c pnpm frontend:test`; focused files only during iteration.
- Frontend types: `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit` and `CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit`.
- Routing: `CURSOR_DEV=true nix develop -c pnpm test:path-routing` and `CURSOR_DEV=true nix develop -c pnpm validate:path-routing`.
- API changes: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`, then `CURSOR_DEV=true nix develop -c pnpm openapi:lint`; never hand-edit generated artifacts.
- `CURSOR_DEV=true nix develop -c env SUT_TIMEOUT_MS=240000 pnpm cy:run --spec e2e_test/features/mcp/assistant_account_linking.feature,e2e_test/features/mcp/mcp_services.feature` after isolated registration; wrapper owns data/ports/fixtures.

Delivery: Jidoka → fresh refactor → API generation when needed → coordinator
`./scripts/run.sh pnpm format:changed` once → plan update → agent commit/check-only hook → push.
Execute Plan owns CI/Story Wrap Up. Before heavy proof inspect host load/memory/
competitors and sequence build output. Reuse unchanged proof; narrow passes cannot
replace failed required suites. No removal is planned.

## Sizing assessment

2026-10-27: original discovery has TLS lifetime (3), store/client bootstrap/reset (4), and metadata/SDK (5) loops.
Reset truncates client rows; shared bootstrap restores them after reset and ordered migration.
MySQL/reset HTTP and SDK metadata are independent loops credibly exceeding 10 minutes together.
Retain grant-lifecycle leaves as 6–12: twelve leaves, estimate 2–4 hours excluding waits. Reassess overruns without dropping lifecycle/real-client proof.

## Story obligations

### G1. Clear E2E OAuth state when runtime storage begins
Reported: slice 2 — "E2E JDBC-only cleanup must include OAuth tables when runtime use begins."
Story clause: "Reuse the user's existing Donut identity and current notebook permissions."
Disposition: proved by slice 4: TestabilityDbResetTest.resetRestoresConfiguredClientsWithFreshAuthorizationState exercises real reset, restores two configured clients/settings, and reloads cleared grant/consent through the JDBC stores.

### G2. E2E compiler observation is limited
Reported: slice 3 — "E2E typecheck remains at 236 diagnostics. Comparing the changed fixture import/compiler option against baseline found zero new diagnostics; this does not constitute a whole-checkout baseline."
Story clause: "A Donut user can grant, maintain, and withdraw a compatible assistant's read access by signing into Donut through GitHub, without manually copying a Donut personal access token into that client."
Disposition: no user cost "A Donut user can grant, maintain, and withdraw a compatible assistant's read access by signing into Donut through GitHub, without manually copying a Donut personal access token into that client.": The optional whole-E2E static check is not claimed green. Changed TS import/options add no diagnostics in the bounded comparison; the changed JS setup/environment/cleanup paths pass their public runtime suites. Actual SDK discovery and the complete linking journey retain slices 5 and 12 as proof owners.
