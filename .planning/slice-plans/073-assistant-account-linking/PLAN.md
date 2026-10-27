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
Slice 11 published `81d5879fca427614836ceac60f96d79bad92639e`.
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
| Withdrawal/race/reconnection | 12, 13 | Browser withdrawal, SDK/refresh refusal, fresh consent, unaffected other user, controlled race |
| Durable consent/grants | 2, 4, 14, 15 | MySQL storage then owned backend process restart with retained credentials/database |
| Documented confidential-client journey | 16, 17 | Real SDK over HTTPS from discovery through renewal/withdrawal |

Mock only external GitHub HTTP, never internal issuance/permission/renewal/revocation.
Injected principals prove filters, not callback/continuation. Use controlled clocks/short test lifetimes, not production sleeps. Story 1 retains retrieval semantics; this adds delegated authorization.

## Ordered slices

Target ~5 minutes including proof/cleanup; scrutinize 6–10, finer-decompose >10.
Full backend suites, E2E cold starts and operator release waits are exceptions.

### 1. Reuse the GitHub login and identity boundary
Type: Behavior
Status: done
Proof: [Accepted execution evidence](EXECUTION-EVIDENCE.md#1-reuse-the-github-login-and-identity-boundary).

### 2. Persist assistant authorization state
Type: Structure
Status: done
Proof: [Accepted execution evidence](EXECUTION-EVIDENCE.md#2-persist-assistant-authorization-state).

### 3. Establish the owned HTTPS test origin
Type: Structure
Status: done
Proof: [Accepted execution evidence](EXECUTION-EVIDENCE.md#3-establish-the-owned-https-test-origin).

### 4. Bootstrap configured assistant clients and stores
Type: Structure
Status: done
Proof: [Accepted execution evidence](EXECUTION-EVIDENCE.md#4-bootstrap-configured-assistant-clients-and-stores).

### 5. Discover the configured authorization service
Type: Behavior
Status: done
Proof: [Accepted execution evidence](EXECUTION-EVIDENCE.md#5-discover-the-configured-authorization-service).

### 6. Establish the owned GitHub callback and HTTPS continuation
Type: Structure
Status: done
Proof: [Accepted execution evidence](EXECUTION-EVIDENCE.md#6-establish-the-owned-github-callback-and-https-continuation).

### 7. Decide validated assistant consent through the protocol
Type: Behavior
Status: done
Proof: [Accepted execution evidence](EXECUTION-EVIDENCE.md#7-decide-validated-assistant-consent-through-the-protocol).

### 8. Show informed assistant consent in the browser
Type: Behavior
Status: done
Proof: [Accepted execution evidence](EXECUTION-EVIDENCE.md#8-show-informed-assistant-consent-in-the-browser).

### 9. Exchange a code for a constrained credential
Type: Behavior
Status: done
Proof: [Accepted execution evidence](EXECUTION-EVIDENCE.md#9-exchange-a-code-for-a-constrained-credential).

### 10. Read MCP knowledge under the delegated permission
Type: Behavior
Status: done
Proof: [Accepted execution evidence](EXECUTION-EVIDENCE.md#10-read-mcp-knowledge-under-the-delegated-permission).

### 11. Renew access without enlarging permission
Type: Behavior
Status: done
Proof: [Accepted execution evidence](EXECUTION-EVIDENCE.md#11-renew-access-without-enlarging-permission).

### 12. Withdraw a user's whole client grant atomically
Type: Behavior
Status: done
Proof: Real management HTTP lists only the current user's clients, requires CSRF/ownership, and atomically removes every matching grant and remembered consent; existing SDK/refresh fail while the other user works.

Use existing identity, JDBC authorization/consent and registered clients. Return public client identity/name/read scope only, never credentials. Framework CSRF context protects the management mutation. Share a durable per-user grant transaction/lock with maintained token-grant handlers (code/refresh), rechecking after lock so an in-flight issuance cannot recreate withdrawn state. Prove a controlled overlapping refresh/withdrawal through real HTTP/database boundaries, not mocked collaborators. Existing provider-list hook is observed; use current tables/transactions, no process cache or parallel grant model. Sizing: 5–10 minutes source plus required backend/generated-client/HTTP proof; stop/refine a model or budget overrun.
Accepted proof: [execution evidence](EXECUTION-EVIDENCE.md#12-withdraw-a-users-whole-client-grant-atomically).

### 13. Manage connected assistants in Settings
Type: Behavior
Status: planned
Proof: Mounted component/types and actual fixture-backed browser settings withdrawal observe client identification, token/renewal refusal and reconnect requiring consent.

Use generated management API/loading wrappers and framework CSRF context in existing named Settings navigation; retain personal-token controls. Show no credential/secret values. Reuse accepted backend isolation/race proof and actual GitHub callback fixture; no new provider feature. Sizing: 5–10 minutes source plus frontend/browser cold proof.

### 14. Establish an owned process-restart proof seam
Type: Structure
Status: planned
Proof: Public owner/lifetime tests and one actual owned restart show a new backend JVM while retaining canonical HTTPS origin, database and cleanup ownership; foreign/Development services remain untouched.

Current supervised group exits on child close and has no restart operation. Add the smallest supported owned application restart seam, authenticating the active proof owner/lease. Restarting the owned application group is allowed when it is the existing indivisible ownership unit; retain outer TLS proxy and MySQL. No classloader-only surrogate, detached orphan or shared-development restart. Sizing: 5–10 minutes source plus lifecycle suites/cold waits; stop/refine an ownership model overrun.

### 15. Keep authorization valid across a backend restart
Type: Behavior
Status: planned
Proof: Real credentials survive an actual owned JVM replacement with the same MySQL/origin; renewal and cookie-free SDK read preserve user/resource/scope, and remembered consent remains effective.

Use seam14. Withdraw and restart again: old MCP/refresh fail and reconnection requires consent; no reset between process instances. Observe ordinary grant and consent behavior, not same-JVM repositories. Sizing: ~5 minutes source/probe plus two owned startup waits.

### 16. Establish the hosted SDK client fixture
Type: Structure
Status: planned
Proof: Maintained SDK authProvider/finishAuth follows actual HTTPS discovery and browser GitHub/consent through an exact hosted callback, validating state/S256 and saving issued credentials outside the browser.

Own callback listener/client secret/state/verifier/tokens before backend startup and configured client bootstrap; reuse GitHub/TLS/runner ownership. Expose only the minimum private Node-task controls needed by Cypress, preserving server-held confidential credentials. Resolve the concrete SDK callback/control seam here, with cleanup and one real initial authorization as result-bearing proof; no handwritten OAuth engine or internal issuance mocks. Sizing: 5–10 minutes source plus fixture/runtime suites/cold proof; stop/refine a harness overrun.

### 17. Connect through the documented OAuth journey
Type: Behavior
Status: planned
Proof: Permanent isolated assistant_account_linking.feature uses the real hosted SDK client through discovery → GitHub/informed consent → exchange → attributed search/read/bounded graph → expiry/automatic renewal → Settings withdrawal/reconnection.

Use fixture16 and accepted state/callback, isolation, constraint/race/restart proof; require actual SDK credential replacement before retry and no ordinary-call browser login. Run the new feature and existing mcp_services.feature over verified TLS. Document client registration/encoded server secrets, canonical issuer/resource/exact callback, default finite lifetimes/rotation, existing GitHub reuse, consent/withdrawal/reconnect, schema-first release and narrow discovery ADR exception; retain personal-token instructions. ChatGPT remains story3. No real-client activation/deployment authority. Sizing: 5–10 minutes source plus mandatory E2E cold proof; stop/refine incompatible client or source overrun.

## Consumer trace and verification

Trace security/principal/user/permission, login continuation/token settings and supplier HTTP ingress/client.
Per diff run installed `consumer-trace.mjs --checkout <execution-checkout> --base HEAD` with `CurrentUserFetcherFromRequest`, `login/continue`, `Authorization`, `generateToken` and MCP connection phrases; follow callers/journeys and classify run/reused/excluded.

- Run all backend tests, including retained login/profile/personal tokens and new real-filter/MySQL authorization; use SpringTestBase MockMvc/session cleanup, without redundant contexts. Reuse literal commands in the linked execution evidence.
- Run mounted consent/settings tests and both frontend typechecks on same generated API; use generated SDK/loading wrappers, existing navigation and Vite component declarations.
- Run routing classifier/rendered production map/validation; generate/lint OpenAPI from source for API contracts, never hand-edit generated artifacts.
- Run focused OAuth and existing MCP E2E; extend runner/client lifecycle tests for changed TLS/provider/restart ownership. No real GitHub credentials in CI.
- CLI extra behavior excluded while personal tokens unchanged; revise/run affected boundaries if shared auth reaches CLI. Full E2E suite is not a local gate.

- `CURSOR_DEV=true nix develop -c env SUT_TIMEOUT_MS=240000 pnpm cy:run --spec e2e_test/features/mcp/assistant_account_linking.feature,e2e_test/features/mcp/mcp_services.feature` after isolated registration; wrapper owns data/ports/fixtures.

Delivery: Jidoka → fresh refactor → API generation when needed → coordinator `./scripts/run.sh pnpm format:changed` once → plan update → agent commit/check-only hook → push.
Execute Plan owns CI/Story Wrap Up. Inspect host load/memory/competitors and sequence heavy proof/build output. Reuse unchanged proof; narrow passes cannot replace failed required suites. No removal is planned.

## Sizing assessment

2026-10-27 refinement at `81d5879fca427614836ceac60f96d79bad92639e`: preserve completed1–11 and their accepted evidence. Split former12 into atomic backend withdrawal12 and Settings13. Make the observed missing owned-restart seam14 explicit before durability15; make hosted SDK callback/fixture16 explicit before permanent journey17. Maintained7.1.2 token endpoint exposes authenticationProviders; existing JPA row-lock patterns and JDBC stores support a shared durable grant transaction without a new model. SDK1.32.2 exposes authProvider/finishAuth, state/verifier/token persistence; no new provider feature is selected. Readiness follows the shared recorder after current-basis review.
Seventeen leaves; source target5minutes, scrutinize6–10/hardfinerdecompose>10 except declared fullsuite/cold-start waits. Story outcome/authority are unchanged. Story resplit recommended:17slices; use [dough-resplit-story](../../../.agents/skills/dough-resplit-story/SKILL.md). This recommendation is not an execution-readiness gate; resplitting waits for Terry's instruction, while the established Execute Plan authority continues the selected full outcome.

## Story obligations

### G1. Clear E2E OAuth state when runtime storage begins
Reported: slice 2 — "E2E JDBC-only cleanup must include OAuth tables when runtime use begins."
Story clause: "Reuse the user's existing Donut identity and current notebook permissions."
Disposition: proved by slice 4: TestabilityDbResetTest.resetRestoresConfiguredClientsWithFreshAuthorizationState exercises real reset, restores two configured clients/settings, and reloads cleared grant/consent through the JDBC stores.

### G2. E2E compiler observation is limited
Reported: slice 3 — "E2E typecheck remains at 236 diagnostics. Comparing the changed fixture import/compiler option against baseline found zero new diagnostics; this does not constitute a whole-checkout baseline."
Story clause: "A Donut user can grant, maintain, and withdraw a compatible assistant's read access by signing into Donut through GitHub, without manually copying a Donut personal access token into that client."
Disposition: no user cost "A Donut user can grant, maintain, and withdraw a compatible assistant's read access by signing into Donut through GitHub, without manually copying a Donut personal access token into that client.": The optional whole-E2E static check is not claimed green. Changed TS import/options add no diagnostics in the bounded comparison; the changed JS setup/environment/cleanup paths pass their public runtime suites. Actual SDK discovery and the complete linking journey retain slices 5 and 17 as proof owners.

### G3. Observe credential scope independently of grant scope
Reported: slice 10 — "Acceptance found the scope refusal changes the stored grant scope while the access credential itself still has notes:read; token-scope refusal needs a distinct observation."
Story clause: "A credential for a different resource or without the read scope is refused;"
Disposition: proved by slice 10: AssistantMcpAccessTest.invalidDelegationCannotUseAnAccompanyingBrowserSession(token-scope) persists an otherwise-valid grant/token binding with access scope notes:write and observes MCP401 despite its live cookie; the distinct current-grant-scope refusal remains. Full backend63211 passed2943/0failures/0errors.

### G4. Bearer scheme casing must preserve credential authority
Reported: slice 10 — "Final reading found lowercase/mixed-case OAuth bearer could fall through to cookies on general APIs; valid MCP bearer schemes must also authenticate case-insensitively under RFC 9110 section 11.1."
Story clause: "the assistant credential cannot be used to create a personal token or edit a note, including when a browser session accompanies the request."
Disposition: proved by slice 10: ApiBearerAuthenticationFilter detects bearer schemes case-insensitively before session/direct-Principal controllers; AssistantMcpAccessTest observes lowercase/mixed-case MCP read success as the consenting account and corresponding OAuth+cookie general-API personal-token-creation401. Actual SDK content PATCH401 and unchanged owner read cover the mutation continuation. Full backend63211 passed2943/0failures/0errors; standard-Bearer SDK5642 boundary is unchanged.

### G5. Preserve authorization validators through transaction decoration
Reported: slice 12 — "Backend40011 retained failures showed custom resource and exact-loopback authorization validators were bypassed after wrapping the raw provider list."
Story clause: "A mismatched callback, invalid PKCE proof, or reused code cannot issue a usable credential."
Disposition: proved by slice 12: AssistantAuthorizationServerConfiguration post-processes authorization/consent provider registration while retaining original list entries for the maintained filter's validator composite; retained AssistantConsentTest invalid-resource/S256/exact-loopback cases pass in full backend16398 (2958 tests, 0 failures/errors, 2 existing skips). Installed7.1.2 configurer lambda$init$0 offsets0–43 was inspected at /tmp/dough-authorization-endpoint-configurer.txt; deliberate validation is active before transactional delegation.

### G6. Preserve the frontend's unauthorized-context continuation
Reported: slice 12 — "Acceptance traced anonymous management/context GET302 through generated fetch to login HTML200; the existing frontend login interceptor requires HTTP401, so a logged-out settings/context request could appear successful."
Story clause: "A Donut user can grant, maintain, and withdraw a compatible assistant's read access by signing into Donut through GitHub, without manually copying a Donut personal access token into that client."
Disposition: proved by slice 12: AssistantConsentTest observes missing-session context401, real logout followed by ended-session401 for both JSON routes, and valid anonymous AUTHORIZE302 to /login; AssistantManagementTest observes anonymous list401. Final backend9926 passed2961/0failures/0errors/2skips. The existing frontend clientSetup GET401 sign-in test passed in unchanged frontend73910; its interceptor/helper/return-location continuation was inspected, with all reached consumers classified in /tmp/dough-story2-json-context-consumers.md. Authenticated grant/DTO/race paths remain unchanged.
