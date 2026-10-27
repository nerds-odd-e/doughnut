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
Slice 4 published `84e2b4eda9562369af774636edb490d2d1bdeb12`; slice 5 published `20d28229aabc6a4747cb8e1a39f433f170a71dad` on that branch.
Slice 6 published `4df752991fbb75653481bbf9a24471e1953c3146`; slice 7 published `0fb9548eb93b320b71f3e02eb9be06e86b4c444d`.
Slice 8 published `890d5be26955d8d2ab456d6ad0de7f0015622709`.
Slice 9 published `47add57de8a497e44734cf4a462735079cf65d66`.
Slice 10 published `5057876408a583ee2ee27f1f2282969ec0b976b5`.
Slice 2's SHA is the immutable schema-only release candidate; no deployment is authorized.

`./scripts/run.sh bash scripts/worktree_setup.sh` succeeded with the checkout's
current dependency fingerprint. `CURSOR_DEV=true nix develop -c pnpm validate:path-routing`
passed 11 backend probes/five static paths; this establishes command readiness only.
No live GitHub observation, production deployment, real-account authorization or paid call occurred.

## Scope, decisions and safety

Deliver the canonical source's full contract/approved architecture against story 1's
real HTTPS MCP. [Preparation evidence](PREPARATION-EVIDENCE.md) retains dated inputs,
revisions, commands, results, comparisons, digest and limits.

- Terry's 2026-10-27 clarification selects cohesive reuse of existing GitHub login/identity mapping. No new GitHub scope/API/provider feature is selected; a proposed expansion returns to the owner. Slices 6–8 prove new callback-to-consent integration with external HTTP fixtures; no live test-app gate applies to unchanged login.
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
| Existing GitHub login supplies same Donut identity to consent | 1, 6, 8 | Reuse inspection; real chain/provider HTTP fixture callback and saved continuation |
| Public HTTPS discovery | 3, 5 | SDK follows MCP 401 metadata pointer through verified TLS/routing |
| Informed Allow/Deny/retry, CSRF | 7, 8 | Browser drives real consent; code or access_denied |
| Client/PKCE/redirect/code reuse/resource binding | 7, 9 | Real security filters and persisted grants; good exchange and changed-input refusals |
| Current read permission, isolation, read-only scope | 10 | Interleaved SDK search/read/graph; permission loss/foreign-note/general API refusals |
| Expiry/renewal without widening | 11 | Expired access refused; real refresh restores SDK read; invalid/widened requests refused |
| Withdrawal/race/reconnection | 12 | Browser withdrawal, SDK/refresh refusal, fresh consent, unaffected other user, controlled race |
| Durable consent/grants | 2, 4, 13 | MySQL storage then owned backend process restart with retained credentials/database |
| Documented confidential-client journey | 14 | Real SDK over HTTPS from discovery through renewal/withdrawal |

Mock only external GitHub HTTP, never internal issuance/permission/renewal/revocation.
Injected principals prove filters, not callback/continuation. Use controlled clocks/short test lifetimes, not production sleeps. Story 1 retains retrieval semantics; this adds delegated authorization.

## Ordered slices

Target ~5 minutes including proof/cleanup; scrutinize 6–10, finer-decompose >10.
Full backend suites, E2E cold starts and operator release waits are exceptions.

### 1. Reuse the GitHub login and identity boundary
Type: Behavior
Status: done
Proof: Inspect existing provider/continuation/identity mapping; slices 6–8 own new consent integration.

Detailed accepted observations: [execution evidence](EXECUTION-EVIDENCE.md#1-reuse-the-github-login-and-identity-boundary).

### 2. Persist assistant authorization state
Type: Structure
Status: done
Proof: MySQL migration/JDBC grant-consent round-trip, all backend tests, regenerated ERD; login/personal-token behavior retained.

Detailed accepted observations: [execution evidence](EXECUTION-EVIDENCE.md#2-persist-assistant-authorization-state).

### 3. Establish the owned HTTPS test origin
Type: Structure
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm test:sut-start` and `CURSOR_DEV=true nix develop -c pnpm -C cli exec tsx --test ../e2e_test/support/mcp_tls_proxy.test.ts` observe pre-spawn configuration, CA/forwarding and success/failure cleanup.

Detailed accepted observations: [execution evidence](EXECUTION-EVIDENCE.md#3-establish-the-owned-https-test-origin).

### 4. Bootstrap configured assistant clients and stores
Type: Structure
Status: done
Proof: Real MySQL store/configured-client bootstrap and reset HTTP tests observe client restoration with cleared grants/consent; full `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

Detailed accepted observations: [execution evidence](EXECUTION-EVIDENCE.md#4-bootstrap-configured-assistant-clients-and-stores).

### 5. Discover the configured authorization service
Type: Behavior
Status: done
Proof: SDK follows unauthenticated MCP 401 → protected-resource → issuer metadata through verified TLS/shared routing.

Detailed accepted observations: [execution evidence](EXECUTION-EVIDENCE.md#5-discover-the-configured-authorization-service).

### 6. Establish the owned GitHub callback and HTTPS continuation
Type: Structure
Status: done
Proof: LB/lifetime tests and real security-chain fixture callback observe the existing Donut identity and saved request remaining on the owned HTTPS origin.

Detailed accepted observations: [execution evidence](EXECUTION-EVIDENCE.md#6-establish-the-owned-github-callback-and-https-continuation).

### 7. Decide validated assistant consent through the protocol
Type: Behavior
Status: done
Proof: Real-filter Allow/Deny/retry, account/client/resource/scope binding and tamper/foreign-principal/invalid-redirect/missing-CSRF refusals through persisted pending authorization.

Spring consentPage(...) supplies client/scope/state lookup, not trusted display or bindings. Generated GET context
derives account/client/scope/resource/redirect from persisted pending grant and current principal. Framework cookie
CSRF storage supplies a GET token for the hidden field; protect consent POST specifically, without blanket
authorization-matcher ignore or app session attributes. Native form POST uses the maintained handler and follows
external callbacks; generated fetch/302 cannot own this navigation. Persist consent; code/access_denied targets
only the validated callback. Retain login/signup; no UI yet. Sizing: 5–10 minutes plus backend/API proof.
Accepted implementation/proof and independent refactor: [execution evidence](EXECUTION-EVIDENCE.md#7-decide-validated-assistant-consent-through-the-protocol).

### 8. Show informed assistant consent in the browser
Type: Behavior
Status: done
Proof: Mounted consent tests, both frontend typechecks and actual fixture-backed browser Allow/Deny/retry observe the same Donut identity, informed decision and callback.

Add named consent route in routeMetadata/routes; use namedLocationHref navigation. Show client/account, current
readable-notebook access, renewal and withdrawal, loading context with generated SDK/loading wrappers. Native
Allow/Deny protocol forms carry returned CSRF/state; this is protocol navigation, not a handwritten OAuth client.
Backend consent-page URI is standard redirect configuration; invent no JVM/Vue route compiler. Reuse fixture 6,
with no live GitHub probe or Basic/injected-principal callback claim. Generate normal component declarations.
Sizing: 5–10 minutes plus mounted/types/browser proof and E2E cold waits.
Accepted proof: [execution evidence](EXECUTION-EVIDENCE.md#8-show-informed-assistant-consent-in-the-browser).

### 9. Exchange a code for a constrained credential
Type: Behavior
Status: done
Proof: Real HTTP exchange issues opaque user/client/read/resource-bound credentials; invalid secret/PKCE, wrong resource and code reuse fail.

Use maintained exchange/common resource validation, persisting GitHub-backed Donut subject rather than client audience.
Set finite access/refresh lifetimes and rotation with deterministic short tests; retain credential/error rules. Sizing: ~5 minutes plus full backend verification.
Accepted proof: [execution evidence](EXECUTION-EVIDENCE.md#9-exchange-a-code-for-a-constrained-credential).

### 10. Read MCP knowledge under the delegated permission
Type: Behavior
Status: done
Proof: Interleaved two-user OAuth SDK calls to all tools; current/foreign-note permissions and general mutation/token-creation refusals.

Accept active OAuth reference tokens for the correct resource/read scope, resolving existing user/knowledge operations.
Search/read/graph retain current notebook permissions and accepted attribution. Expired/revoked/wrong-resource tokens cannot fall through to sessions; OAuth cannot authorize non-MCP mutation/personal tokens even with cookies.
Retain ordinary sessions/personal tokens. Sizing: 6–10 minutes plus full backend/MCP proof.
Accepted proof: [execution evidence](EXECUTION-EVIDENCE.md#10-read-mcp-knowledge-under-the-delegated-permission).

### 11. Renew access without enlarging permission
Type: Behavior
Status: done
Proof: Actual token endpoint refreshes expired access and SDK reads again; expired/reused/invalid refresh and resource/scope widening are refused.

Authenticate confidential client against the active grant; rotate refresh while preserving user/client/resource/read scope/current state.
Save replacement before retry without login; unusable refresh offers reconnect. Use controlled clock/test lifetime, never production sleeps or fixture-token replacement as renewal proof. Sizing: ~5 minutes plus backend/client verification.

Accepted proof: [execution evidence](EXECUTION-EVIDENCE.md#11-renew-access-without-enlarging-permission).

### 12. Withdraw a connected assistant
Type: Behavior
Status: planned
Proof: Browser withdrawal stops every owner's client grant next call/refresh; other user works, reconnect needs consent, controlled refresh race leaves no active replacement.

Settings show client/read permission without tokens. CSRF-protected owner-only withdrawal atomically removes all authorizations/consent; repeated withdrawal stays effective.
Serialize with refresh and prove HTTP owner refusal; retain personal-token management. Stop/refine if racing renewal needs a model beyond the shared grant transaction. Sizing: 6–10 minutes plus backend/frontend proof.

### 13. Keep authorization valid across a backend restart
Type: Behavior
Status: planned
Proof: Obtain credentials, restart only owned E2E backend with same MySQL database, renew/read retained credentials; withdraw then restart again to prove persisted effect.

Cover consent/subject/resource binding without ordinary-call browser sessions; runner owns restore/stop on all paths, never Development/peers/production.
Same-JVM instances are insufficient; stop/refine lifecycle-seam overrun. Sizing: ~5 minutes plus two owned startup waits.

### 14. Connect through the documented OAuth journey
Type: Behavior
Status: planned
Proof: Real SDK confidential client follows documented HTTPS discovery → GitHub/consent → exchange → attributed search/read/graph → expiry/renewal → withdrawal.

Add `e2e_test/features/mcp/assistant_account_linking.feature` to isolated selection;
reuse TLS/client lifecycle and slices 6/8 callback proof. Fake only external GitHub responses;
use attributed note/graph data, SDK state/callback checks and renewed credentials.
Document registration/secrets, issuer/resource/callback, schema-first release, consent/
withdrawal/reconnect and discovery exception; demonstrate with SDK, retaining personal-token instructions.
ChatGPT stays story 3. Activate real clients only after authorized release/full proof.
Stop/revise incompatibility; handwritten OAuth, mocked tools or HTTP-only proof are insufficient. Sizing: 6–10 minutes plus E2E startup.

## Consumer trace and verification

Trace security/principal/user/permission, login continuation/token settings and supplier HTTP ingress/client.
Per diff run installed `consumer-trace.mjs --checkout <execution-checkout> --base HEAD` with `CurrentUserFetcherFromRequest`, `login/continue`, `Authorization`, `generateToken` and MCP connection phrases; follow callers/journeys and classify run/reused/excluded.

- Run all backend tests, including retained login/profile/personal tokens and new real-filter/MySQL authorization; use SpringTestBase MockMvc/session cleanup, without redundant contexts. Reuse literal commands above.
- Run mounted consent/settings tests and both frontend typechecks on same generated API; use generated SDK/loading wrappers, existing navigation and Vite component declarations.
- Run routing classifier/rendered production map/validation; generate/lint OpenAPI from source for API contracts, never hand-edit generated artifacts.
- Run focused OAuth and existing MCP E2E; extend runner/client lifecycle tests for changed TLS/provider/restart ownership. No real GitHub credentials in CI.
- CLI extra behavior excluded while personal tokens unchanged; revise/run affected boundaries if shared auth reaches CLI. Full E2E suite is not a local gate.

- `CURSOR_DEV=true nix develop -c env SUT_TIMEOUT_MS=240000 pnpm cy:run --spec e2e_test/features/mcp/assistant_account_linking.feature,e2e_test/features/mcp/mcp_services.feature` after isolated registration; wrapper owns data/ports/fixtures.

Delivery: Jidoka → fresh refactor → API generation when needed → coordinator `./scripts/run.sh pnpm format:changed` once → plan update → agent commit/check-only hook → push.
Execute Plan owns CI/Story Wrap Up. Inspect host load/memory/competitors and sequence heavy proof/build output. Reuse unchanged proof; narrow passes cannot replace failed required suites. No removal is planned.

## Sizing assessment

2026-10-27: discovery's TLS/store/bootstrap/metadata loops delivered as 3–5. Consent now has independent fixture/HTTPS continuation (6), protocol decision (7), and browser/UI (8) loops; combined setup/API/UI proof credibly exceeds 10 minutes.
Retain later lifecycle outcomes as 9–14. Fourteen leaves, one outcome/proof loop each; 2–4 hour hypothesis excludes waits. Reassess overruns without dropping lifecycle or real-client proof.

## Story obligations

### G1. Clear E2E OAuth state when runtime storage begins
Reported: slice 2 — "E2E JDBC-only cleanup must include OAuth tables when runtime use begins."
Story clause: "Reuse the user's existing Donut identity and current notebook permissions."
Disposition: proved by slice 4: TestabilityDbResetTest.resetRestoresConfiguredClientsWithFreshAuthorizationState exercises real reset, restores two configured clients/settings, and reloads cleared grant/consent through the JDBC stores.

### G2. E2E compiler observation is limited
Reported: slice 3 — "E2E typecheck remains at 236 diagnostics. Comparing the changed fixture import/compiler option against baseline found zero new diagnostics; this does not constitute a whole-checkout baseline."
Story clause: "A Donut user can grant, maintain, and withdraw a compatible assistant's read access by signing into Donut through GitHub, without manually copying a Donut personal access token into that client."
Disposition: no user cost "A Donut user can grant, maintain, and withdraw a compatible assistant's read access by signing into Donut through GitHub, without manually copying a Donut personal access token into that client.": The optional whole-E2E static check is not claimed green. Changed TS import/options add no diagnostics in the bounded comparison; the changed JS setup/environment/cleanup paths pass their public runtime suites. Actual SDK discovery and the complete linking journey retain slices 5 and 14 as proof owners.

### G3. Observe credential scope independently of grant scope
Reported: slice 10 — "Acceptance found the scope refusal changes the stored grant scope while the access credential itself still has notes:read; token-scope refusal needs a distinct observation."
Story clause: "A credential for a different resource or without the read scope is refused;"
Disposition: proved by slice 10: AssistantMcpAccessTest.invalidDelegationCannotUseAnAccompanyingBrowserSession(token-scope) persists an otherwise-valid grant/token binding with access scope notes:write and observes MCP401 despite its live cookie; the distinct current-grant-scope refusal remains. Full backend63211 passed2943/0failures/0errors.

### G4. Bearer scheme casing must preserve credential authority
Reported: slice 10 — "Final reading found lowercase/mixed-case OAuth bearer could fall through to cookies on general APIs; valid MCP bearer schemes must also authenticate case-insensitively under RFC 9110 section 11.1."
Story clause: "the assistant credential cannot be used to create a personal token or edit a note, including when a browser session accompanies the request."
Disposition: proved by slice 10: ApiBearerAuthenticationFilter detects bearer schemes case-insensitively before session/direct-Principal controllers; AssistantMcpAccessTest observes lowercase/mixed-case MCP read success as the consenting account and corresponding OAuth+cookie general-API personal-token-creation401. Actual SDK content PATCH401 and unchanged owner read cover the mutation continuation. Full backend63211 passed2943/0failures/0errors; standard-Bearer SDK5642 boundary is unchanged.
