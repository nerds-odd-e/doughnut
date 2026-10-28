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
`notes:read`, optionally with `learning_session:record` for saving Learning Session
Reports. The record grant requires `notes:read`; general note editing is not
granted. Multiple configured clients are supported. Dynamic registration
and public-client renewal are separate work.

The assistant begins with the MCP URL and registered client information. A real
MCP 401 points to protected-resource metadata; the client discovers authorization
and token endpoints and sends the canonical resource through authorization,
exchange and renewal. Keep client secrets, PKCE verifiers, access credentials and
refresh credentials server-held, outside browser storage and logs.

## Personal ChatGPT setup

In ChatGPT, open **Plugins → Add → Add custom MCP server**. Use a name such as
**Donut**, the server URL `https://doughnut.odd-e.com/api/mcp`, and **OAuth**.
Open **Advanced OAuth settings** and copy the exact displayed callback for the
operator to register. Each created connection may have a different callback;
use its actual value rather than copying another installation's callback.

After registration, select **User-Defined OAuth Client**, enter the client ID
and its raw secret directly in ChatGPT, select `client_secret_basic`, and select
both `notes:read` and `learning_session:record` for commissioned learning.
A connection requesting only `notes:read` supports notebook reading without
recording Learning Session Reports. Create/install the plugin, connect
the intended Donut account, and review the requested permissions on **Donut's**
consent screen. A recording request adds **and record learning sessions** to
the heading and explains that Reports save Feedback and update schedules. Keep the
secret out of prompts and source control. Existing GitHub/Donut sign-in and an
operator-registered confidential client are prerequisites.

Try: “Find my Donut note titled <title>, read it in full, and explain it. Cite
the returned title, notebook, folder path, note ID and acceptedRevision.”
`acceptedRevision` identifies the accepted commit of the whole notebook.
Search matches titles and aliases; use a more precise title when no match is
found. A full read supplies citation evidence; graph context guides further
reading and may contain truncated excerpts.

## Consent and continued access

The user signs in through existing GitHub login, then sees the assistant name,
Donut account, current readable-notebook access, requested Learning Session Report
recording permission, automatic renewal and where to
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
and request bounded graph context. With `learning_session:record`, it can also
record Learning Session Reports, saving Feedback and updating schedules for
completed items. Existing read-only connections need fresh authorization requesting
both scopes to gain recording permission. It gains no general API access, personal-token
creation or content editing. Losing notebook permission affects subsequent reads.

The MCP tool descriptors declare their OAuth scopes in `_meta.securitySchemes`:
the reading and Request tools require `notes:read`, and the Report tool requires
both scopes. A Report attempted through a read-only connection returns a tool
error with an `mcp/www_authenticate` insufficient-scope challenge identifying
both scopes and the canonical resource metadata. A compatible client can use
that response to begin fresh authorization; an existing token keeps its original
permissions. This follows the [ChatGPT authentication contract](https://developers.openai.com/plugins/build/auth#triggering-authentication-ui)
and its [tool metadata compatibility field](https://developers.openai.com/apps-sdk/reference#_meta-fields-on-tool-descriptor).
In ChatGPT, refresh the plugin's tools after deploying changed
metadata before testing that permission upgrade. This describes the supported
wire contract; the actual ChatGPT permission-upgrade and recording journey must
be observed separately from backend tests. A connection completing after Donut
showed only notebook-reading consent has not established recording permission.

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
