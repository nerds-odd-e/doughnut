---
id: SEED-002
status: dormant
planted: 2026-08-06
planted_during: idle (after deploy/static consistency audit)
trigger_when: when shipping a public MCP download URL or restoring /mcp-server.bundle.mjs on the prod LB
scope: medium
---

# SEED-002: Host MCP server bundle on GCS for production download

## Why This Matters

Prod once advertised `/mcp-server.bundle.mjs` via the URL map (rewrite to `frontend/<SHA>/mcp-server.bundle.mjs`), but CI never uploaded the artifact — MCP builds to `mcp-server/dist/` while SPA upload only rsyncs `frontend/dist/`. The orphan route was removed (option 2). Restoring a real public download would let IDEs/users fetch a versioned bundle from the live site instead of only building from a local checkout.

## When to Surface

**Trigger:** when shipping a public MCP download URL or restoring `/mcp-server.bundle.mjs` on the prod LB

Also surface when changing Package-artifacts / GCS frontend upload / path-routing for static downloads.

## Scope Estimate

**Medium** — a phase or two:

1. Decide prefix: copy into `frontend/<SHA>/` vs dedicated GCS object (CLI-style `doughnut-mcp-latest/` or SHA-scoped).
2. Upload from CI (`mcp-server/dist/mcp-server.bundle.mjs`) after `pnpm mcp-server:bundle`.
3. Re-add URL-map (and local-LB) rule; document IDE install from prod URL; smoke-check in `prod-frontend-static-lb.md`.

## Breadcrumbs

- `infra/gcp/path-routing/doughnut-routing.json` — static path rules (MCP rule removed 2026-08-06)
- `infra/gcp/scripts/upload-frontend-static-to-gcs.sh` — SPA-only rsync today
- `infra/gcp/scripts/upload-cli-binary-to-gcs.sh` — pattern for a dedicated download object
- `mcp-server/package.json` — `outfile=./dist/mcp-server.bundle.mjs`
- `.cursor/rules/mcp-server.mdc` — IDE config uses local `mcp-server/dist/...` only
- `.github/workflows/ci.yml` — Package-artifacts builds SPA+CLI for GCS (not MCP)
- `docs/gcp/prod-frontend-static-lb.md` — SPA/CLI release model; no MCP section
- `.gitignore` — removed stale `frontend/public/mcp-server.bundle.mjs` ignore

## Notes

Captured after audit: option 2 dropped the broken prod route; option 1 (this seed) is wire-up when we want hosting again. Local/E2E MCP remains filesystem-based and unaffected.
