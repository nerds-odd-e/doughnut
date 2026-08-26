# 0005 — Rename internal naming to Donut

**Status:** Accepted  
**Date:** 2026-08-26  
**Decision makers:** Terry Yin  
**Consulted:** None

## Context

The codebase's internal naming — workspace and build identifiers, the
generated API client, the Java backend package, backend configuration
namespace, the CLI, the MCP server, frontend and E2E identifiers, scripts,
CI workflow names, and internal docs — has used `doughnut` throughout,
matching the public product name.

`doughnut` is long, easy to misspell, and clutters identifiers (package
paths, CLI flags, config keys, generated type names) without adding
information once "Doughnut" is already established as the product's public
name. `donut` is shorter, spells consistently, and is an accepted informal
spelling of the same word — it reads as the same brand internally while
being cheaper to type and grep across every layer of the stack.

This ADR records the decision to rename internal naming to `donut` and,
just as importantly, which naming stays `doughnut` and why — so later
slices and future contributors don't have to re-derive the boundary or
re-litigate it file by file.

## Decision

Every internal artifact, identifier, path, config key, command, and doc
reference is renamed to use `donut`. This is an unconditional rename: no
dual-naming, no backward-compatible aliases, no transition period. The
rename spans roughly 13 slices covering workspace/build identity, the
generated API client, the Java backend package, the backend configuration
namespace, the CLI, the MCP server, the frontend, E2E tests, scripts, CI
workflows, and internal docs.

Internal artifact *descriptions* (npm `description` fields, the MCP
server's identity string, log messages) are renamed to describe "Donut
..." — these describe the internal tool, not the public-facing brand, so
they follow the internal identifier, not the exclusion list below.

### What stays `doughnut`, and why

Only the following keep the `doughnut` spelling. Each is either the
external brand/URL, or the literal identifier of a live external resource
that this rename does not migrate:

- **Public product/repo/brand name "Doughnut"** — the README title, the
  GitHub repository name `nerds-odd-e/doughnut`, and the
  `frontend/index.html` `<title>`. The product's public name is a
  separate decision from its internal naming; this ADR does not change
  what the product is called.
- **The CLI's hardcoded external API URL** `https://doughnut.odd-e.com` —
  a live external endpoint, not an internal identifier.
- **Production external resource identifiers** — the prod MySQL
  database/username `doughnut`, the GCS bucket
  `doughnut-book-pdf-carbon-syntax-298809`, the GitHub repo paths
  `nerds-odd-e/doughnut` and `nerds-odd-e/doughnut_sandbox`, and the
  Gitpod base image `yeongsheng/doughnut-gitpod:...`. These name real,
  already-provisioned external resources. Renaming them means migrating
  live data or re-provisioning infrastructure, which is a separate,
  coordinated effort, not a naming edit.
- **Everything under `infra/gcp/**`** (Salt states/pillars, the Packer
  template, deploy scripts) — these name real, currently-deployed GCP
  resources. A rename here requires a coordinated infrastructure
  migration and is out of scope for this rename.

Everything not on this list — including internal artifact descriptions —
renames to `donut` unconditionally.

## Consequences

- Internal code, config, and docs consistently use the shorter `donut`
  spelling; the public product name and live external resources are
  unaffected and keep reading as "Doughnut".
- Because the rename is unconditional with no compatibility aliases,
  each of the ~13 slices is a hard cutover for its layer: any code or
  docs left referring to internal `doughnut` naming after a slice lands
  is a bug in that slice, not an intentional transitional state.
- Contributors reading any file touched by a later slice see only the
  current, clean `donut` naming — no "renamed from doughnut" language
  anywhere except here.
- **This ADR is the sole place in the repo allowed to describe the
  history of this change** (e.g. "renamed from", "previously", "used to
  be", "no longer"). Every other file touched by this rename's slices
  must describe only the current state, not the fact that a rename
  happened.
- Future contributors who need to know why an internal identifier reads
  `donut` while the product is called "Doughnut" — or why a specific
  external identifier still reads `doughnut` — should be pointed here
  rather than have that reasoning re-explained inline elsewhere.

## Related

- Links: `.planning/quick/001-rename-doughnut-to-donut/PLAN.md` — the
  slice plan executing this decision across workspace/build identity,
  the generated API client, the Java backend package, backend config,
  the CLI, the MCP server, the frontend, E2E, scripts, CI, and docs.
