# Link an assistant to Donut through GitHub sign-in

**Identity:** SEED-002#story-2
**Source:** [Refined story](../../seeds/SEED-002-host-mcp-over-https.md#story-2)
**Preparation checkout:** `/Users/terryyin/git/doughnut/.worktrees/link-an-assistant-to-donut-through-github-sign-i`
**Prepared:** 2026-10-27; execution separately authorized by the established start.

## Execution resume

Agent: Yua-chan; publisher: `dashboard-territory.local-doughnut`.
The preparation checkout is the owned execution checkout, branch `codex/link-an-assistant-to-donut-through-github-sign-i`, Story Branch Mode. Integration/default checkout: `/Users/terryyin/git/doughnut`; remote: `origin`; eventual target: `main`. Ordinary increments publish the execution branch.
Starting revision: `83615a792fe3aa3107badb07d883a70398d75127`; startup claim `3d32688d4a040ab4e57e8a5a03d48963e4dc157f` was confirmed on remote main/execution branch.
Implementation 18 `b5afee6a6b5d6f27fc787ed88ebc0326b90697a3`; completion `33454cf0d682c918f1f8f6761b2c2784feb516d0`; navigation repair `4fa3ad93d52c23d9a0dc587917d2d85c063d426b`; [publication history](PUBLICATION-EVIDENCE.md) and [CI recovery](HOSTED-SDK-EVIDENCE.md#ci-api-generation-repair).
Slice 2 `d488c64dbc69a00c51f2cc9237f71ae9fda016a1` is the immutable schema-only release candidate; no deployment is authorized.

`./scripts/run.sh bash scripts/worktree_setup.sh` succeeded with the checkout's current dependency fingerprint. `CURSOR_DEV=true nix develop -c pnpm validate:path-routing` passed 11 backend probes/five static paths; this establishes command readiness only. No live GitHub observation, production deployment, real-account authorization or paid call occurred.

## Scope, decisions and safety

Deliver the canonical source's full contract/approved architecture against story 1's real HTTPS MCP. [Preparation evidence](PREPARATION-EVIDENCE.md) retains dated inputs, revisions, commands, results, comparisons, digest and limits.

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
| Documented confidential-client journey | 16–18 | Real SDK over HTTPS from discovery through renewal/withdrawal |

Mock only external GitHub HTTP, never internal issuance/permission/renewal/revocation. Injected principals prove filters, not callback/continuation. Use controlled clocks/short test lifetimes, not production sleeps. Story 1 retains retrieval semantics; this adds delegated authorization.

## Ordered slices

Target ~5 minutes including proof/cleanup; scrutinize 6–10, finer-decompose >10. Full backend suites, E2E cold starts and operator release waits are exceptions.

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
Proof: [execution evidence](EXECUTION-EVIDENCE.md#12-withdraw-a-users-whole-client-grant-atomically).

### 13. Manage connected assistants in Settings
Type: Behavior
Status: done
Proof: [execution evidence](EXECUTION-EVIDENCE.md#13-manage-connected-assistants-in-settings).

### 14. Establish an owned process-restart proof seam
Type: Structure
Status: done
Proof: [execution evidence](EXECUTION-EVIDENCE.md#14-establish-an-owned-process-restart-proof-seam).

### 15. Keep authorization valid across a backend restart
Type: Behavior
Status: done
Proof: [execution evidence](EXECUTION-EVIDENCE.md#15-keep-authorization-valid-across-a-backend-restart).

### 16. Support OAuth clients over the owned TLS fixture
Type: Structure
Status: done
Proof: [execution evidence](EXECUTION-EVIDENCE.md#16-support-oauth-clients-over-the-owned-tls-fixture).

### 17. Complete hosted SDK authorization through the browser
Type: Structure
Status: done
Proof: [execution evidence](EXECUTION-EVIDENCE.md#17-complete-hosted-sdk-authorization-through-the-browser).

### 18. Connect through the documented OAuth journey
Type: Behavior
Status: done
Proof: [execution evidence](EXECUTION-EVIDENCE.md#18-connect-through-the-documented-oauth-journey); full original brief and subsequent CI navigation repair in [hosted SDK evidence](HOSTED-SDK-EVIDENCE.md#ci-navigation-gate-repair).

## Execution complete

Product advice: no change. The amended existing-user assistant lifecycle is delivered; keep ChatGPT onboarding in the existing SEED-002#story-3. No correction plan or new product priority is recommended. Aggregate review covers the attributable publication set, current whole-product identity/permission/content boundaries and retained tests. The developer authorized one fresh CI observer after the original missing-registration handoff; navigation repair is published, its subsequent database-free generation failure is repaired locally, and CI completion remains required.

## Consumer trace and verification

Accepted 1–18 observations and literal commands are in the linked execution/hosted SDK evidence, including the one-time aggregate removal reading and original verification instructions. Each repair traces actual consumers, reuses unchanged proof, runs sufficient affected boundaries, and retains failures and limitations.

Delivery remains Jidoka → fresh refactor → needed API generation → coordinator format:changed once → plan evidence/agent commit with check-only hook → managed publication. Execute Plan owns CI and Story Wrap Up; sequence heavy work using actual host/ownership evidence. No production deployment or real-client activation is authorized.

## Sizing assessment

2026-10-27 refinement at `81d5879fca427614836ceac60f96d79bad92639e`: preserve completed1–11 and their accepted evidence. Split former12 into atomic backend withdrawal12 and Settings13. Make the observed missing owned-restart seam14 explicit before durability15; make hosted SDK callback/fixture16 explicit before permanent journey17. Maintained7.1.2 token endpoint exposes authenticationProviders; existing JPA row-lock patterns and JDBC stores support a shared durable grant transaction without a new model. SDK1.32.2 exposes authProvider/finishAuth, state/verifier/token persistence; no new provider feature is selected. Readiness follows the shared recorder after current-basis review.
Eighteen leaves after the bounded fixture attempt exposed a separate Cypress authorization continuation; source target5minutes, scrutinize6–10/hardfinerdecompose>10 except declared fullsuite/cold-start waits. Story outcome/authority are unchanged. Story resplit recommended:18slices; use [dough-resplit-story](../../../.agents/skills/dough-resplit-story/SKILL.md). This recommendation is not an execution-readiness gate; resplitting waits for Terry's instruction, while the established Execute Plan authority continues the selected full outcome.

## Story obligations

### G1. Clear E2E OAuth state when runtime storage begins
Reported: slice 2 — "E2E JDBC-only cleanup must include OAuth tables when runtime use begins."
Story clause: "Reuse the user's existing Donut identity and current notebook permissions."
Disposition: proved by slice 4: TestabilityDbResetTest.resetRestoresConfiguredClientsWithFreshAuthorizationState exercises real reset, restores two configured clients/settings, and reloads cleared grant/consent through the JDBC stores.

### G2. E2E compiler observation is limited
Reported: slice 3 — "E2E typecheck remains at 236 diagnostics. Comparing the changed fixture import/compiler option against baseline found zero new diagnostics; this does not constitute a whole-checkout baseline."
Story clause: "A Donut user can grant, maintain, and withdraw a compatible assistant's read access by signing into Donut through GitHub, without manually copying a Donut personal access token into that client."
Disposition: no user cost "A Donut user can grant, maintain, and withdraw a compatible assistant's read access by signing into Donut through GitHub, without manually copying a Donut personal access token into that client.": The optional whole-E2E static check is not claimed green. Changed TS import/options add no diagnostics in the bounded comparison; the changed JS setup/environment/cleanup paths pass their public runtime suites. Actual SDK discovery and the complete linking journey retain slices 5 and 18 as proof owners.

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

### G7. Live Development preservation was not directly observed
Reported: slice 14 — "The actual restart probe had no live Development listener; its preservation was not directly observed."
Story clause: "A Donut user can grant, maintain, and withdraw a compatible assistant's read access by signing into Donut through GitHub, without manually copying a Donut personal access token into that client."
Disposition: no user cost "A Donut user can grant, maintain, and withdraw a compatible assistant's read access by signing into Donut through GitHub, without manually copying a Donut personal access token into that client.": The new control operation is confined to the authenticated SUT owner/application tree and matching active lease. Inspected paths never address Development ownership; shared supervisor's default unexpected-exit/log behavior passes19 Development tests. Actual foreign process and HTTP sentinels survive, and owner/lifetime tests preserve foreign peers. Port5175 was empty before/after; no stronger live-Development claim is made or required for the OAuth outcome. Actual OAuth grant/consent restart remains15.

### G8. Keep baseline signal forwarding during cleanup
Reported: slice 14 — "Acceptance found new signal-listener removal also ran before ordinary asynchronous cleanup, changing the shared supervisor's default failure path."
Story clause: "A backend restart between authorization and renewal preserves the valid grant and its user binding."
Disposition: proved by slice 14: removed only the new normal-finish listener removal, restoring baseline signal forwarding through onBeforeExit while retaining removal for the planned expected-close branch. Development19 and SUT80 passed after this one-line restoration, including default forced-exit/bounded-cleanup and authorized restart cases. Actual new-JVM/HTTPS/database/foreign-sentinel proof remains valid because its planned branch is unchanged.

### G9. Support the maintained SDK's form body through verified TLS
Reported: slice 16 — "installed SDK sends URLSearchParams for token exchange; shared CA fetch rejects that body type."
Story clause: "Allow completes the callback and code exchange;"
Disposition: proved by slice 17: actual maintained SDK callback/URLSearchParams token exchange completes in permanent assistant_account_linking.feature; unchanged TLS3 directly proves form bytes. Final explicit Chrome helper-state feature1/1 and hosted default1/1, linked hosted SDK evidence.

### G11. Complete browser authorization continuation
Reported: slice 16 — "Permanent Cypress initial scenario reached login but after GitHub redirects landed chrome-error://chromewebdata (screenshot inspected), while pinned Playwright path passes."
Story clause: "Allow completes the callback and code exchange;"
Disposition: proved by slice 17: external failed trace observes request-session loss in HTTP-top Cypress iframe; owned canonical HTTPS browser origin after worktree guard and unchanged SDK URL preserve the GitHub session and continue into actual consent/callback/exchange/private read. Final permanent Chrome1/1 and hosted default1/1 pass, /tmp/dough-story2-slice17-cypress-helper-final.log. Original Electron/Chrome failures remain retained; explicit Electron journey is not claimed. Source selects no Cypress browser; Chrome matches documented default and actual CI entry.

### G12. Start the hosted fixture for CI's selected glob
Reported: slice 16 — "CI primary uses the mcp/** glob. The fixture selector must recognize glob inclusion, not just exact feature strings, or CI's new feature lacks its pre-bootstrap client."
Story clause: "The client discovers the authorization server, sends the user through GitHub sign-in, and shows Donut's read-access consent."
Disposition: proved by slice 17: public exact and primary mcp/** runner cases observe configured confidential callback/client before startup and owned cleanup in runtime116. CI matrix and explicit Chrome entry were inspected; final permanent Chrome feature1/1 proves the selected consumer, without a claim of actual CI execution.

### G10. Keep admitted initial journey executable
Reported: slice 16 — "Runtime114 showed113pass/1inventory fail because admitting leaf17's feature before its file exists violates existing registry inventory."
Story clause: "The client discovers the authorization server, sends the user through GitHub sign-in, and shows Donut's read-access consent."
Disposition: proved by slice 17: actual initial assistant_account_linking.feature exists and passes; public runtime116 includes registry inventory reconciliation and all ordinary selections. See hosted SDK evidence17 and /tmp/dough-story2-slice17-runtime.log.

### G13. Observe actual SDK renewal after finite expiry
Reported: slice 18 — "hosted scenario reached automatic-renewal assertion but generation remained1 after expiry wait"
Story clause: "When its short-lived access token expires, the client renews it and can read again under the same resource and scope."
Disposition: proved by slice 18: final permanent pair observes actual MCP401, maintained SDK refresh200, private saved replacement generation1→2 before MCP200 and equal attributed read; positive canonical issuer/token endpoint/resource/read scope and unchanged GitHub visits. Safe failed timing observations and final /tmp/dough-story2-slice18-cypress-final.log are retained in hosted SDK evidence18.

### G14. Preserve personal-token MCP in the composed fixture
Reported: slice 18 — "existing nine MCP scenarios all attempted and failed because their TLS proxy only accepts HTTP upstream, while hosted selection changes Cypress baseUrl to canonical HTTPS."
Story clause: "Existing personal-token workflows remain available."
Disposition: proved by slice 18: preserve guard-selected HTTP Node MCP forwardOrigin before hosted canonical HTTPS browser override. Final exact pair passes all nine ordinary MCP scenarios together with hosted lifecycle, /tmp/dough-story2-slice18-cypress-final.log; runtime116 and actual config→mcpClientTasks→PAT client consumers are inspected/classified. Earlier inherited17 host-origin coupling is retained as the first failed composition, not relabeled unaffected.

### G15. Paired-worktree scenario has a separate harness
Reported: slice 18 — "wrapper's pre-existing mcpIsolationProof filter deliberately excludes its paired-worktree-only scenario when no peer barrier is configured"
Story clause: "Existing personal-token workflows remain available."
Disposition: no user cost "A Donut user can grant, maintain, and withdraw a compatible assistant's read access by signing into Donut through GitHub, without manually copying a Donut personal access token into that client.": Inspected applyMcpIsolationCypressExpose and all-or-none own/foreign/barrier inputs: the default focused batch deliberately selects nine ordinary MCP scenarios; the paired-only rendezvous needs its own peer harness. No new filter was added. The changed Node binding preserves the guard-selected HTTP origin before the hosted browser override; ordinary and paired-only selections without hosted fixture use the same origin as before. Public runtime/owner isolation proof remains unchanged. Final composed proof must truthfully report one hosted plus nine ordinary MCP scenarios; it claims no live paired-worktree observation. The coordinator withdrew its overbroad all-scenarios instruction without changing the source's two-user/current-permission promise, already proved through actual SDK and backend boundaries.

### G16. Existing E2E compiler diagnostics
Reported: slice 18 — "Whole E2E typecheck remains red236 existing diagnostics outside this repair; not a green whole-project typecheck claim."
Story clause: "A Donut user can grant, maintain, and withdraw a compatible assistant's read access by signing into Donut through GitHub, without manually copying a Donut personal access token into that client."
Disposition: no user cost "A Donut user can grant, maintain, and withdraw a compatible assistant's read access by signing into Donut through GitHub, without manually copying a Donut personal access token into that client.": Same actual E2E compiler configuration observes baseline238/current236, zero new diagnostics and both edited files clear after correcting two owned unsupported query options. Remaining diagnostics are outside the repair; no whole-typecheck success is claimed. Actual compiled hosted lifecycle plus nine ordinary MCP scenarios pass10/10; unchanged backend/frontend/runtime proof remains accepted. Corrected harness observations and failed reduced-root probes are retained in hosted SDK evidence.

### G17. Preserve database-free API generation
Reported: slice 18 — "Failure is owned eager construction, not infrastructure."
Story clause: "Reuse the user's existing Donut identity and current notebook permissions."
Disposition: proved by slice 18: one canonical singleton authorization-service interface proxy preserves the maintained JDBC target while honoring the existing lazy metadata-generation context. The actual closed-database generator now saves byte-identical OpenAPI output; all backend2961 tests and actual hosted/PAT10 scenarios pass. Initial wrong-environment probes, the rejected filter-only change/HTTP500 despite process0, original incorrect constructor-SQL claim and final positive-output evidence remain in CI repair evidence. No alternate identity, store, handler, permission path or generated contract is introduced; normal startup and all current-grant/consent/renewal/withdrawal boundaries are observed.
