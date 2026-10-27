# Assistant account-linking preparation evidence

Retained observations from 2026-10-27, reorganized during execution on that
same date. These are dated evidence, not completed implementation proof.
The canonical [story](../../seeds/SEED-002-host-mcp-over-https.md#story-2) and
[execution plan](PLAN.md) retain scope, decisions and current proof ownership.

Slice numbers in the dated observations below refer to the ten-leaf plan retained
at `d488c64dbc69a00c51f2cc9237f71ae9fda016a1`. Refinement inserts current slice 3;
original slices 3–10 correspond to current 4–11. Observed actions are unchanged.

## Decomposition baseline

Repository reading on 2026-10-27 at
`8598fe004c6aff8adc2ca9d83cccf6e566c23ae2` establishes the starting point:

- The local MCP server offers title/alias candidate search, a full attributed
  note read, and bounded graph context through Donut's existing backend API.
- Donut already accepts personal bearer tokens and enforces notebook access.
- Donut's production Spring Security configuration consumes GitHub login;
  it does not issue OAuth grants to assistant clients.
- The existing backend already owns note search, content attribution, and
  focus-context retrieval. Keep those responsibilities there.

## Candidate comparison and library probe

**Explored approaches and borrowed mechanism:**

| Candidate | Fit to the goal and constraints | Consequence |
| --- | --- | --- |
| Continue using personal tokens or copy notes | Smallest existing solution | Fails the selected account-linking goal and preserves manual credential handling |
| Wrap personal-token issuance in a custom OAuth flow | Could provide a similar user journey | Donut would own protocol machinery and would still need a separate constrained, expiring delegation model |
| Spring authorization server with JWT access tokens | Fits the backend and interoperable client flow | Next-request withdrawal still needs shared grant/revocation state; stateless validation alone is insufficient |
| Spring authorization server with opaque reference tokens | Fits the same backend and next-request withdrawal | Recommended; a local durable grant lookup joins token lifecycle to current permissions |

Borrow a revocable visitor badge from physical access control. The Donut user
is the host authorizing a visitor; the assistant is the visitor; the
authorization server issues the badge; the opaque token identifies an entry in
the access roster; notebook permission checks are the door checks. Renewal
extends a permitted visit, and removing permission from the roster stops later
entry. This is the opaque-token candidate, not independent evidence for it.
The consequential limit is that an assistant can retain information it has
already read: revocation prevents future access, not prior disclosure. A badge
also does not establish the OAuth callback, PKCE, or resource-binding rules;
those need protocol proof. A separately hosted identity broker remains outside
the owner's selected in-backend boundary.

**Investigation, 2026-10-27:**

Repository inspection at `9c830fadb68b3299766a644dfa92ed48a39f3c73` found
Spring Boot 4.1.2, GitHub `oauth2Login` only in the production security chain,
browser identity mapped by `externalIdentifier`, and personal tokens carrying
only user, token, and label. Existing Cypress login supplies a nonproduction
Basic-auth session; it cannot prove a GitHub callback. Story 1 is still Taken
in this checkout and its required HTTPS MCP outcome remains a recorded supplier
dependency, not an observation made by this preparation.

A disposable probe in this owned worktree used the Boot 4.1.2 dependency BOM,
which resolved Spring Security authorization server 7.1.2. Command:
`CURSOR_DEV=true nix develop -c ./backend/gradlew -p .tmp-oauth-probe run`.
It drove the library's real security filters through MockMvc with synthetic
GitHub-shaped principals, exact HTTPS callback URIs, S256 PKCE, `notes:read`,
opaque tokens, and refresh rotation. It observed:

- Root authorization-server metadata returned 200 and advertised the configured
  `/api` token endpoint.
- A public client received an access token but no refresh token. A confidential
  client received both and preserved the synthetic GitHub principal name.
- Default access-token claims named the client as audience, and a request for
  a different resource still exchanged successfully. Explicit MCP resource
  binding remains required work.
- Renewal returned a new access token and rotated refresh token. Revoking the
  refresh token made both current tokens inactive; another renewal returned 400.
- On local MySQL 8.4.11, the released JDBC authorization schema and service
  saved that grant; a newly constructed service loaded it by access token with
  its principal preserved and refresh token active. A uniquely named disposable
  database was created for this operation and dropped in `finally`.

The final probe passed. Initial probe setup errors (duplicate synthetic client
secret, then GET parameters not encoded as a query) were corrected before these
observations. Consent was disabled in the probe, authentication was supplied,
and it did not call Donut's MCP service. Thus it establishes library mechanism
and JDBC serialization, not browser consent, real GitHub login, full-process
restart recovery, whole-client withdrawal, or end-to-end MCP interoperability.
Those remain execution proof obligations. No production or Development data
was touched. All disposable source/build files were removed before this write.
The final probe source SHA-256 was
`0e73908ac52f7c23994e49b47198084226306e7f4c29b011b3e8500cca196608`.

Primary references checked on that date:
[MCP authorization and discovery](https://modelcontextprotocol.io/specification/2025-11-25/basic/authorization),
[Spring Security authorization-server setup](https://docs.spring.io/spring-security/reference/servlet/oauth2/authorization-server/getting-started.html),
[Spring authorization components](https://docs.spring.io/spring-security/reference/servlet/oauth2/authorization-server/core-model-components.html),
and the released
[7.1.2 refresh-token generator](https://raw.githubusercontent.com/spring-projects/spring-security/7.1.2/oauth2/oauth2-authorization-server/src/main/java/org/springframework/security/oauth2/server/authorization/token/OAuth2RefreshTokenGenerator.java).
This uses the supplier's 2025-11-25 interoperability baseline; it does not promise
certification against every newer protocol version.

## Additional preparation observations

The following narrative is retained from the original plan; its source-probe
record now appears above. References to future proof retain their preparation
date and are governed by the current plan and owner clarification.

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
claim of real GitHub login or Donut MCP integration follows. The owner's later
execution clarification selects reuse of existing GitHub login; the new
callback-to-consent continuation is owned by slice 4 with external HTTP
provider fixtures. The real MCP resource is
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
TLS, or note operation. Server enforcement, browser continuation, network TLS
and note operations remain the mapped execution proofs; existing provider
behavior follows the owner's later reuse clarification. No source or
credential file was created by this observation.

Repository reading confirmed `FlyWayFreeVersionRealMigration.actualMigration`
is an ApplicationReadyEvent listener, matching the migration skill's explicit
two-release requirement. Release/migration confirmation is state-changing and
operator-owned; slice 2 retains the schema-only boundary and withholds dependent
runtime release until that confirmation. No secret or deployment access has
been observed or assumed available.
