---
id: SEED-002
status: dormant
planted: 2026-08-06
planted_during: idle (after deploy/static consistency audit)
updated: 2026-08-07
trigger_when: when shipping a production HTTPS MCP endpoint (remote / streamable HTTP) for IDE clients
scope: medium-large
supersedes_direction: GCS downloadable mcp-server.bundle.mjs (dropped)
---

# SEED-002: Host MCP over HTTPS for production IDE clients

## Why This Matters

MCP today is stdio + a local `mcp-server/dist/mcp-server.bundle.mjs` (IDE config in `.cursor/rules/mcp-server.mdc`). CLI already has a production story: CI uploads a versioned binary to GCS and the HTTPS LB serves `/doughnut-cli-latest/doughnut`. An earlier idea was to mirror that for MCP (upload the `.mjs` bundle and restore `/mcp-server.bundle.mjs`). That orphan LB route was removed (2026-08-06) because CI never uploaded the artifact.

**Chosen direction (2026-08-07):** do **not** ship a downloadable MCP bundle. Instead, expose MCP as an **HTTPS endpoint** so IDEs connect by URL (remote / streamable HTTP transport) to the live site — same class of “install from prod” UX as CLI, without requiring users to download or run a Node process locally.

## When to Surface

**Trigger:** when shipping a production HTTPS MCP endpoint (or designing auth/routing for remote MCP clients)

Also surface when changing MCP transport (stdio → HTTP), Package-artifacts / path-routing / backend edge for long-lived MCP sessions, or product install copy that currently only documents local stdio.

## Scope Estimate

**Medium–large** — several Behavior phases, likely including Structure for transport:

1. HTTPS MCP transport (streamable HTTP or current MCP remote convention) reachable behind the prod LB (or equivalent public URL).
2. Auth: reuse / adapt Bearer-token model used by CLI and local MCP (`DOUGHNUT_API_AUTH_TOKEN` today); decide how remote clients present credentials over HTTPS.
3. Deploy wiring: run the MCP HTTP surface with the app (or a dedicated service), not as a static GCS object.
4. Document IDE config from the prod HTTPS URL; drop any remaining “download bundle” framing; smoke-check alongside `prod-frontend-static-lb.md` / install docs.
5. Keep local stdio for E2E/dev unless/until HTTP is the only supported path.

**Explicitly out of scope for this seed:** restoring `/mcp-server.bundle.mjs`, uploading `mcp-server/dist/` to `frontend/<SHA>/`, or CLI-style `doughnut-mcp-latest/` static download objects.

## Breadcrumbs

- `mcp-server/src/index.ts` — stdio transport today (`StdioServerTransport`)
- `.cursor/rules/mcp-server.mdc` — IDE config is local `node …/mcp-server.bundle.mjs` only
- `infra/gcp/path-routing/doughnut-routing.json` — MCP download rule removed 2026-08-06 (do not restore for static bundle)
- `infra/gcp/scripts/upload-cli-binary-to-gcs.sh` — CLI download pattern (contrast; not the MCP model)
- `docs/gcp/prod-frontend-static-lb.md` — SPA/CLI static release model
- `.github/workflows/ci.yml` — Package-artifacts builds SPA+CLI for GCS (not MCP HTTP deploy)
- `e2e_test/` MCP features — filesystem/stdio-based today

## Notes

- **Superseded direction:** option 1 of the 2026-08-06 audit (wire GCS + restore download URL). Replaced by HTTPS remote MCP.
- Local/E2E stdio MCP can remain until HTTPS is ready; changing transport is the product work, not static hosting.
