# Direct ChatGPT knowledge: live observations

[Active plan](PLAN.md) · SEED-002#story-3 · 2026-10-28 (Asia/Singapore)

## Authority and retained execution

Terry instructed: "Land the changes and release the app, then continue with
the ChatGPT setup." This authorizes the release and a retained landing increment
while the live story continues. The created execution worktree and branch,
Taken entry, profile and all unfinished slices remain in place. No story
completion or worktree retirement is claimed. The originating/default checkout
`/Users/terryyin/git/doughnut` advanced cleanly to the landed revision.

## Landing and release receipt

- History-preserving publisher accepted
  `46c1729c0a11e64c2a77b09fccda1306df280f9d` on `origin/refs/heads/main`,
  suffix base `926b0d57041a46396c46a5439bc537ed92df0034`. The queued-closure
  guard was clear, no story closed, and the implementation tree was unchanged.
- Exact main CI [40612206555, attempt 1](https://github.com/nerds-odd-e/doughnut/actions/runs/40612206555)
  succeeded. Backend jar, frontend-dist and cli-dist artifacts were available
  and unexpired before tagging. Branch CI had independently passed earlier.
- Immutable tag `v1.3.56`, raw ref object
  `4755e14e5066b062e96ae35a788567a3e818b09c`, release SHA and build SHA both
  `46c1729c0a11e64c2a77b09fccda1306df280f9d`.
- [Application Release 40612961347, attempt 1](https://github.com/nerds-odd-e/doughnut/actions/runs/40612961347)
  succeeded, including [publication job 122568909647](https://github.com/nerds-odd-e/doughnut/actions/runs/40612961347/job/122568909647).
  `gs://dough-01/deploy/application-release.json` records that tag, raw ref,
  release/build SHAs, selected CI run/attempt and `outcome: succeeded`.
- `gs://dough-01/deploy/last-successful-deploy.json` records build SHA above
  at `2026-10-28T01:12:18Z`, jar SHA-256
  `48fd62d6ea1506f119cb74c3ac8aeeb3f8236c4f6f1ca125e72c40af15b4e95c`.
  The MIG reports stable and target reached. No tag was moved or retried.

Before this release, read-only production SQL confirmed successful Flyway rows
`300000354` (conversation correction) and `300000355` (assistant authorization).
The existing production startup log also reported current schema `300000355`
and no pending migration. This establishes the schema prerequisite for this
release; it does not reconstruct the historical deployment order of older tags.

Production observations after publication:

| Request/signal | Result |
| --- | --- |
| GET `/api/healthcheck` | 200, prod profile, exact selected build SHA. |
| GET `/` and its first two actual JS asset URLs | 200 HTML and 200 JavaScript; main asset `/assets/main-CDCSIgl-.js`. |
| HEAD `/doughnut-cli-latest/doughnut` | 200 JavaScript. |
| GET both approved OAuth well-known endpoints | 200 JSON, canonical production issuer/resource. |
| Anonymous GET `/api/mcp` | 401 with Bearer `resource_metadata` challenge pointing to the protected-resource document. |
| Login/consent and authenticated tool result | Not yet observed; owner-operated steps below remain required. |

These are operational observations, not proof of ChatGPT linking or renewal.

## Slice 2: client configuration and pending owner action

Chrome's existing signed-in profile: `https://chatgpt.com/plugins` → Add →
Add custom MCP server. Entered name **Donut**, description **Read and cite notes
from your Donut account, with bounded context from related notes.**, Server URL
`https://doughnut.odd-e.com/api/mcp`, OAuth → Advanced OAuth settings.
Actual discovery succeeded and displayed the canonical authorization/token
endpoints and resource, User-Defined OAuth Client, default `notes:read` checked,
and this connection's callback:
`https://chatgpt.com/connector/oauth/3NC-kjDLBDdP`.
Selected `client_secret_basic`. No anonymous mode, tunnel or different client
registration model was substituted. Official auth guidance was fetched from
`https://developers.openai.com/plugins/build/auth` on this date.

Terry reported no existing client and asked how to register it. A read-only
production registry query independently found no clients. The operator then
applied `AssistantClientProperties` through the existing
`AssistantClientBootstrap` and `JdbcRegisteredClientRepository` against the
production database. A temporary local Java source launcher reused the exact
checkout's compiled backend classes and locked Gradle runtime classpath; it
introduced no product runtime, connector script, endpoint or grant model.
The database connection used a temporary localhost-only SSH forward. After the
rollout changed the VM host key, its replacement was verified against GCP's
authenticated guest attributes; strict checking remained enabled and the user's
shared known-hosts file was preserved. The forward was closed after registration.

Saved client `chatgpt-personal`, display name **Donut in ChatGPT**, exact callback
above, `notes:read`, `client_secret_basic`, required S256 and informed consent,
five-minute opaque access tokens, rotating thirty-day refresh credentials.
Repository readback confirmed callback, scope, PKCE, consent and secret matching.
The raw secret and private YAML backup are owner-readable files with mode 0600
under `/Users/terryyin/.config/donut/chatgpt-3NC-kjDLBDdP/`; no secret value was
printed in the conversation or retained in this source. Only the public client
ID was entered in ChatGPT.

The owner completed client creation and the existing connection now appears as
**Terry's Donut account (Primary)**, using OAuth. Current connection, consent,
expiry and read-result observations are in [connection evidence](CONNECTION-EVIDENCE.md).
This supersedes the earlier pending-form handoff; the dated registration and
release receipts above remain applicable. No production note fixtures changed.
