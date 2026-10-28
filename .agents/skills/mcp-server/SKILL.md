---
name: mcp-server
description: Donut HTTPS MCP at /api/mcp — tools, auth, IDE URL+token setup. Use when changing or connecting to Donut MCP.
paths:
  - "backend/src/main/java/com/odde/donut/mcp/**"
  - "backend/src/test/java/com/odde/donut/mcp/**"
---
# Donut HTTPS MCP

Donut exposes MCP on the backend over Streamable HTTP at `/api/mcp`. One backend
service owns the tools.

Implementation lives under `backend/src/main/java/com/odde/donut/mcp/`. Proof is
the MCP boundary tests and `e2e_test/features/mcp/mcp_services.feature`. Style
for backend unit tests: the `unit-testing` skill and `backend` / `backend-testing`
skills.

## How to use this MCP server

Connect any MCP client that speaks Streamable HTTP with a personal bearer token.
Registered confidential hosted assistants can instead use
[GitHub-backed OAuth account linking](../../../docs/assistant-oauth.md), including
consent, automatic renewal and Settings withdrawal. Hosted assistants may request
`notes:read` with the narrow `learning_session:record` permission to save Learning
Session Reports; read-only grants do not permit report recording. Existing
read-only connections need authorization with both scopes to gain that permission. Personal-token connections
need no OAuth setup.

1. Open Donut in the browser and sign in.
2. Go to **Settings → Access Tokens**, generate a token, and copy it once.
3. Point the client at your deployment HTTPS URL ending in `/api/mcp` (for
   production, `https://doughnut.odd-e.com/api/mcp`).
4. Send `Authorization: Bearer <token>` on every request.

### Cursor

Add to `~/.cursor/mcp.json` (or project `.cursor/mcp.json`):

```json
{
  "mcpServers": {
    "donut": {
      "url": "https://doughnut.odd-e.com/api/mcp",
      "headers": {
        "Authorization": "Bearer ${env:DONUT_API_AUTH_TOKEN}"
      }
    }
  }
}
```

Set `DONUT_API_AUTH_TOKEN` in the environment Cursor inherits, or paste the
token directly in a private config you do not commit. Save and restart Cursor
(or reload MCP) so the client reconnects.

### Compatible SDK / HTTP clients

Use the MCP TypeScript client with Streamable HTTP against the same URL and
bearer header:

```ts
import { Client } from '@modelcontextprotocol/sdk/client/index.js'
import { StreamableHTTPClientTransport } from '@modelcontextprotocol/sdk/client/streamableHttp.js'

const client = new Client({ name: 'example', version: '1.0.0' }, { capabilities: {} })
const transport = new StreamableHTTPClientTransport(
  new URL('https://doughnut.odd-e.com/api/mcp'),
  {
    requestInit: {
      headers: { Authorization: `Bearer ${process.env.DONUT_API_AUTH_TOKEN}` },
    },
  }
)
await client.connect(transport)
await client.listTools()
```

### Token withdrawal and auth failure recovery

- Delete the token under **Settings → Access Tokens**. Withdrawal takes effect
  on the next MCP request (HTTP 401). Update or remove the client header.
- If a personal-token client fails authentication, generate a new token, replace
  the bearer value in its config, and reconnect.

## Available Tools

- `list_note_candidates` - List every note whose title or alias matches a query, with notebook and folder to tell candidates apart
- `get_note_graph` - Fetch graph context for a note by ID and token limit
- `read_note` - Read one note in full by ID: the user's note text (treated as evidence) with notebook, folder, and the whole notebook's accepted Git revision (null when none)
