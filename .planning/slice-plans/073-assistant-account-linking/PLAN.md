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
Established claim/startup candidate: `3d32688d4a040ab4e57e8a5a03d48963e4dc157f`,
confirmed on both remote main and execution branch at startup.

`./scripts/run.sh bash scripts/worktree_setup.sh` succeeded with the checkout's
current dependency fingerprint. `CURSOR_DEV=true nix develop -c pnpm validate:path-routing`
passed 11 backend probes/five static paths; this establishes command readiness only.
No live GitHub observation, production deployment, real-account authorization or paid call occurred.

## Scope, decisions and safety

The canonical source owns Goal, Scope, five examples, exclusions, approved
architecture and ADR constraints. Deliver its full consent/credential lifecycle
against story 1's delivered HTTPS MCP service; a protocol stub is insufficient.
[Dated preparation evidence](PREPARATION-EVIDENCE.md) retains candidate comparisons,
probe inputs, revisions, commands, results, source digest and limitations.

- Terry's 2026-10-27 clarification selects cohesive reuse of existing GitHub login/identity mapping. No new GitHub scope/API/provider feature is selected; a proposed expansion returns to the owner. Slice 4 proves new callback-to-consent integration with external HTTP fixtures; no live test-app gate applies to unchanged login.
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
| Existing GitHub login supplies same Donut identity to consent | 1, 4 | Reuse inspection; real chain/provider HTTP fixture callback and saved continuation |
| Public HTTPS discovery | 3 | SDK follows MCP 401 metadata pointer through verified TLS/routing |
| Informed Allow/Deny/retry, CSRF | 4 | Browser drives real consent; code or access_denied |
| Client/PKCE/redirect/code reuse/resource binding | 4, 5 | Real security filters and persisted grants; good exchange and changed-input refusals |
| Current read permission, isolation, read-only scope | 6 | Interleaved SDK search/read/graph; permission loss/foreign-note/general API refusals |
| Expiry/renewal without widening | 7 | Expired access refused; real refresh restores SDK read; invalid/widened requests refused |
| Withdrawal/race/reconnection | 8 | Browser withdrawal, SDK/refresh refusal, fresh consent, unaffected other user, controlled race |
| Durable consent/grants | 2, 9 | MySQL storage then owned backend process restart with retained credentials/database |
| Documented confidential-client journey | 10 | Real SDK over HTTPS from discovery through renewal/withdrawal |

Mock GitHub only at its external HTTP boundary; internal mocks cannot supply
issuance/permission/renewal/revocation proof. Injected principals prove filter
behavior, not callback/continuation. Use controlled clocks/short test lifetimes,
not production sleeps. Story 1 retains retrieval semantics; this adds delegated authorization.

## Ordered slices

Target ~5 minutes per leaf including proof/cleanup; scrutinize 6–10 minutes,
finer-decompose >10 minutes unless a stated exception applies. Full backend
suites, E2E cold starts and operator release waits are explicit exceptions.

### 1. Reuse the GitHub login and identity boundary
Type: Behavior
Status: done
Proof: Inspect existing provider/continuation/identity mapping; slice 4 owns new consent integration.

Inspection at `3d32688d4a040ab4e57e8a5a03d48963e4dc157f`: production
`oauth2Login(Customizer.withDefaults())`/`application-prod.yml` GitHub registration;
`signInRedirectHref` → authenticated `/login/continue?from=…` →
`ApplicationController.loginContinue`; `CurrentUserFetcherFromRequest` maps
principal name to existing `externalIdentifier`. Preserve this implementation.
Source-only review `/root/source_promises_1` received original Goal/Scope/examples/
exclusions plus owner's reuse amendment; commitments C06/C12/C14 select these
existing boundaries. This is repository reading, not live-provider/consent proof.
No product behavior changed; no product test rerun. Sizing: ~5 minutes.

### 2. Persist assistant authorization state
Type: Structure
Status: planned
Proof: MySQL migration/JDBC grant-consent round-trip, all backend tests, regenerated ERD; login/personal-token behavior retained.

Add minimum compatible registered-client/authorization/consent schema at a version
above actual maximum. Index user/client withdrawal; identity linkage follows user
deletion rules. This commit has no runtime consumers or personal-token data migration;
it enables slice 3 and is the immutable schema-first release point described above.
Record schema commit and separately authorized release/table verification before
shipping dependent runtime code. Sizing: 6–10 minutes plus migration/ERD proof.

### 3. Discover the configured authorization service
Type: Behavior
Status: planned
Proof: SDK follows unauthenticated MCP 401 → protected-resource → issuer metadata through verified TLS/shared routing.

Configure backend issuer/confidential client/exact callback/read scope/PKCE;
wire durable stores only on migrated databases. Add source-approved exact metadata
routes and MCP challenge. Operational endpoints remain `/api`, described through
OpenAPI source generation. Advertise supported scopes only, no dynamic registration.
Preserve personal-token auth/invalid-Origin handling; ordinary MCP gets an auth
challenge, not a login page. Curl/route assertions alone are insufficient.
Sizing: 6–10 minutes plus backend/routing/SDK proof.

### 4. Decide an assistant's read access after GitHub sign-in
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

### 5. Exchange a code for a constrained credential
Type: Behavior
Status: planned
Proof: Real HTTP exchange issues opaque user/client/read/resource-bound credentials; invalid secret/PKCE, wrong resource and code reuse fail.

Use maintained exchange handler/common resource rule rather than default client-ID
audience. Persist GitHub-backed Donut subject; consumer resolves user, not client.
Set finite access/refresh lifetimes and rotation with deterministic short test settings.
Keep credentials out of consent screen/logs; distinguish protocol from unexpected failures.
Sizing: ~5 minutes plus full backend verification.

### 6. Read MCP knowledge under the delegated permission
Type: Behavior
Status: planned
Proof: Interleaved two-user OAuth SDK calls to all tools; current/foreign-note permissions and general mutation/token-creation refusals.

Extend request-scoped bearer gate for active OAuth reference tokens with correct
resource/read scope, using existing user and knowledge operations. Search/read/graph
retain current notebook permissions and accepted attribution. Expired/revoked/wrong-
resource tokens cannot fall through to browser sessions; OAuth cannot authorize
non-MCP mutations or personal-token creation even with cookies. Retain ordinary
sessions and personal tokens. Sizing: 6–10 minutes plus full backend/MCP proof.

### 7. Renew access without enlarging permission
Type: Behavior
Status: planned
Proof: Actual token endpoint refreshes expired access and SDK reads again; expired/reused/invalid refresh and resource/scope widening are refused.

Authenticate confidential client against active grant; rotate refresh, preserve
user/client/resource/read scope/current state. Client saves replacement and retries
without login; unusable refresh offers reconnect. Use controlled clock/test lifetime,
never production sleep or fixture-token replacement as renewal proof.
Sizing: ~5 minutes plus backend/client verification.

### 8. Withdraw a connected assistant
Type: Behavior
Status: planned
Proof: Browser withdrawal stops every owner's client grant next call/refresh; other user works, reconnect needs consent, controlled refresh race leaves no active replacement.

Settings show client/read permission without tokens. CSRF-protected owner-only
withdrawal atomically removes all authorizations/remembered consent; repeated
withdrawal stays effective. Serialize with refresh, drive owner refusal through
HTTP, retain personal-token management. Stop/refine if race needs a new model
beyond the shared grant transaction. Sizing: 6–10 minutes plus backend/frontend proof.

### 9. Keep authorization valid across a backend restart
Type: Behavior
Status: planned
Proof: Obtain credentials, restart only owned E2E backend with same MySQL database, renew/read retained credentials; withdraw then restart again to prove persisted effect.

Cover consent and subject/resource binding without browser sessions for ordinary
calls/refresh. Runner owns lifecycle and restore/stop on success/failure. Do not
touch Development, peers or production. New same-JVM instances are insufficient.
Stop/refine if runner work exceeds bounded lifecycle seam. Sizing: ~5 minutes
plus two owned startup waits.

### 10. Connect through the documented OAuth journey
Type: Behavior
Status: planned
Proof: Real SDK confidential client follows documented HTTPS discovery → GitHub/consent → exchange → attributed search/read/graph → expiry/renewal → withdrawal.

Add focused `e2e_test/features/mcp/assistant_account_linking.feature` to assessed
isolated selection; reuse supplier TLS/client lifecycle and slice 4 callback proof.
Only GitHub responses are externally faked. Use actual attributed note/graph data,
SDK state/callback checks and renewed-credential persistence, not handwritten OAuth.
Document registration/secret provisioning, issuer/resource/callback, schema-first
release, consent/withdrawal/reconnect and approved discovery exception; demonstrate
with supported SDK and retain personal-token instructions. ChatGPT stays story 3.
Activate real client only after authorized release/full proof. Stop/revise client
incompatibility; mocked tools or HTTP-only proof are insufficient.
Sizing: 6–10 minutes plus E2E startup.

## Consumer trace and verification

Trace security configuration → principal → `CurrentUserFetcherFromRequest` →
`UserService`/`AuthorizationService`, login continuation and token settings/generated
UserController. Reinspect supplier's delivered HTTP client/ingress; preparation's
stdio client is dated evidence, not code to copy. For every diff run installed
`consumer-trace.mjs --checkout <execution-checkout> --base HEAD` with terms
`CurrentUserFetcherFromRequest`, `login/continue`, `Authorization`, `generateToken`
and MCP connection step phrases; follow callers/journeys and classify run/reused/excluded.

- Run existing login/profile/personal-token plus new real-filter/MySQL authorization tests; use SpringTestBase MockMvc and session cleanup, without redundant contexts.
- Run mounted consent/settings tests and both frontend typechecks on same generated API; use generated SDK/loading wrappers, existing navigation and Vite component declarations.
- Run routing classifier/rendered production map/validation; generate/lint OpenAPI for API contracts.
- Run focused OAuth and existing MCP E2E; extend runner/client lifecycle tests for changed TLS/provider/restart ownership. No real GitHub credentials in CI.
- CLI extra behavior excluded while personal tokens unchanged; revise/run affected boundaries if shared auth reaches CLI. Full E2E suite is not a local gate.

Literal commands from this checkout:

- `CURSOR_DEV=true nix develop -c pnpm backend:verify` for schema; `CURSOR_DEV=true nix develop -c pnpm backend:test_only` otherwise. Both run all backend tests with makeMe/real DB.
- `CURSOR_DEV=true nix develop -c pnpm export:database-erd` after migrations.
- `CURSOR_DEV=true nix develop -c pnpm frontend:test`; focused files only during iteration.
- `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`
- `CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit`
- `CURSOR_DEV=true nix develop -c pnpm test:path-routing`
- `CURSOR_DEV=true nix develop -c pnpm validate:path-routing`
- `CURSOR_DEV=true nix develop -c pnpm generateTypeScript` after API changes; never hand-edit generated artifacts.
- `CURSOR_DEV=true nix develop -c pnpm openapi:lint`
- `CURSOR_DEV=true nix develop -c env SUT_TIMEOUT_MS=240000 pnpm cy:run --spec e2e_test/features/mcp/assistant_account_linking.feature,e2e_test/features/mcp/mcp_services.feature` after isolated registration; wrapper owns data/ports/fixtures.

Delivery: Jidoka → fresh refactor → required API generation → coordinator
`./scripts/run.sh pnpm format:changed` once → plan update → agent commit/check-only
hook → push. Execute Plan owns CI observation and eventual Story Wrap Up.
Inspect target-host load/memory/competing work before heavy backend/E2E proof;
sequence shared build output. Reuse unchanged accepted proof; a narrow pass cannot
replace a failed required suite. No removal is planned.

## Sizing assessment

The ten leaves follow one grant lifecycle and immediate enabling structure.
No extra refinement was needed; estimate 2–4 hours excluding operator waits.
Reassess disproved sizing assumptions; preserve renewal, withdrawal, persistence
and real-client proof. The owner's reuse clarification settles the GitHub gate;
slice 4 owns new continuation proof and schema deployment retains its release gate.
