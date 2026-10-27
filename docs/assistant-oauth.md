# Hosted assistant account linking

A registered confidential hosted assistant connects to Donut's HTTPS MCP service
at `/api/mcp` without asking the user to copy a personal access token. GitHub
authenticates the person through Donut's existing login and `read:user` identity
mapping. Donut, rather than GitHub, grants the assistant access to knowledge.

## Operator configuration

Set the deployment's trusted canonical HTTPS origin with `donut.canonical-origin`.
Discovery, issuer and resource identifiers come from this
configuration, never the incoming Host header. For an origin such as
`https://doughnut.example`, the issuer is that origin and the resource is exactly
`https://doughnut.example/api/mcp`.

Register each hosted client through `donut.oauth.clients` before offering it to
users. For example, the deployment's private Spring configuration can contain:

```yaml
donut:
  oauth:
    clients:
      - client-id: hosted-assistant
        client-name: My hosted assistant
        client-secret: "{bcrypt}<encoded-server-secret>"
        redirect-uris:
          - https://assistant.example/oauth/donut/callback
```

Use an encoded secret accepted by Spring Security's delegating password encoder;
the example is a placeholder, not an encoded value. Keep the corresponding raw
secret on the assistant server. Register the exact HTTPS callback, including its
path; redirect matching is exact. The supported initial journey uses
`client_secret_basic`, authorization code with S256 PKCE and state, and
`notes:read`. Multiple configured clients are supported. Dynamic registration,
public-client renewal and ChatGPT-specific onboarding are separate work.

The assistant begins with the MCP URL and registered client information. A real
MCP 401 points to protected-resource metadata; the client discovers authorization
and token endpoints and sends the canonical resource through authorization,
exchange and renewal. Keep client secrets, PKCE verifiers, access credentials and
refresh credentials server-held, outside browser storage and logs.

## Consent and continued access

The user signs in through existing GitHub login, then sees the assistant name,
Donut account, current readable-notebook access, automatic renewal and where to
withdraw permission. Allow authorizes the exact callback and code exchange. Deny
returns an authorization refusal; the assistant can offer a fresh attempt.
GitHub sign-in alone grants no assistant permission. This linking journey supports
existing Donut users. Create an account through Donut's existing registration
workflow before linking; first-time signup during assistant linking is deferred.

Access tokens are opaque and expire after five minutes by default. Refresh
tokens expire after 30 days by default and rotate on successful renewal. A
compatible client saves replacement access and refresh credentials before retrying
the protected call; ordinary calls and successful renewal need no browser login.
An expired or invalid refresh credential requires fresh authorization. Lifetimes
are configurable with `donut.oauth.access-token-time-to-live` and
`donut.oauth.refresh-token-time-to-live`; the disposable browser fixture's short
lifetime is test configuration.

Every call checks the current user/client/resource/read grant and current
notebook permissions. The assistant can search candidates, read attributed notes
and request bounded graph context. It gains no general API access, personal-token
creation or content editing. Losing notebook permission affects subsequent reads.

In **Settings → Connected Assistants**, the user identifies the assistant and
withdraws access. Withdrawal removes that user's whole client grant and remembered
consent, invalidating outstanding access and renewal credentials. The next call
through an existing connection fails; reconnecting requires informed consent
again. Other users' grants are independent. An already authorized in-flight read
may finish, and information already disclosed cannot be recalled. Grants and
consent persist across backend restarts.

## Authorization implementation

Spring Security Authorization Server owns the protocol handlers. Registered
clients, authorizations and remembered consent use its maintained JDBC stores;
assistant grants are separate from personal `UserToken` credentials. The
authorization-service interface proxy preserves the singleton JDBC target while
honoring lazy startup for database-free API metadata generation.

MCP resolves opaque credentials against local grant state and the existing Donut
user and notebook permission services. It uses the authorizing GitHub user's
identity, rather than the OAuth client principal. Authorization, renewal and
withdrawal share a per-user database lock so completed withdrawal invalidates
all of that user's credentials and pending consent for the client atomically.
Consent and grant management enforce CSRF protection. Configured clients are
bootstrapped on application readiness after the Flyway readiness listener.

## Routing and release order

Terry Yin approved two exact discovery exceptions to
[ADR 0005 — Web routes](adrs/0005-web-routes-accepted.md) on 2026-10-27:
`/.well-known/oauth-authorization-server` and
`/.well-known/oauth-protected-resource/api/mcp`. Both route to the backend.
Authorization, token exchange, revocation, grant management and MCP stay under
`/api`; these exceptions do not extend to other operational routes.

Before releasing dependent runtime code to production, separately authorize,
apply and verify schema-only release candidate
`d488c64dbc69a00c51f2cc9237f71ae9fda016a1`. Runtime migrations run on application
readiness, so dependent code must not be released ahead of these tables. Ordinary
Git publication does not authorize deployment or activation of a real client.

Existing personal bearer tokens remain available through **Settings → Access
Tokens**; see the [MCP connection guide](../.agents/skills/mcp-server/SKILL.md).
