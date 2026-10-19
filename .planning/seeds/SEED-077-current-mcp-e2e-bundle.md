---
id: SEED-077
status: dormant
planted: 2026-10-20
planted_during: project findings phase of maintain-feedback-cycle
trigger_when: restoring trustworthy local MCP E2E feedback after source changes
scope: small
---

# SEED-077: Exercise current MCP code in local E2E

## Problem and evidence

[DD-250](../../DonutRetrospectiveFindings.md#dd-250)
records SEED-073#story-4, first implementation commit
`6c6eab31cd93e307606a8adad93e18fa456ffcf7`: a local E2E run reported
`Unknown tool` for `read_note` until a manual MCP bundle rebuild. The slice 3
learning at `1a852ab455` retains the execution evidence. This is one occurrence;
CI's fresh bundle was unaffected.

At `64583a0036`, `e2e_test/config/mcpClientTasks.ts` still skips bundling when
`mcp-server/dist/mcp-server.bundle.mjs` exists. A behavior change could therefore
pass against older code without the visible missing-tool failure. This remedy
belongs to Donut's local E2E adapter, rather than shared Open Dough guidance.

## Story

<a id="story-1"></a>

### Exercise current MCP source in local E2E runs

**Identity:** SEED-077#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** A developer changing MCP tools can trust a local E2E result
  to describe the checked-out implementation and avoid a manual rebuild.
- **Outcome:** The existing local MCP E2E entry point prepares and exercises
  the current MCP source even when a bundle from an earlier run exists.
- **Scope:** Correct the current bundle-preparation responsibility and retain
  the existing server startup/teardown journey. Choose the smallest adequate
  build or freshness mechanism during refinement; measure its cost before
  deciding that rebuilding every run is acceptable.
- **Key examples:** With an old bundle present, change a tool's observable
  behavior and run the normal local MCP E2E journey: it observes the changed
  behavior. With no bundle present, the same journey builds and runs. A build
  failure is reported before old code can supply a successful result.
- **Expected benefit / priority:** Trustworthy local proof for MCP changes
  supporting note retrieval; this integrity gap takes priority over the
  existing queued product work while retaining its relative order.
- **Effort hypothesis:** S, about 30–60 minutes including focused proof and
  delivery; medium confidence until bundle cost is measured.
- **Boundaries:** This story does not change MCP tool semantics or broaden
  the E2E harness beyond preparing the server it runs. CI's fresh-build path
  remains part of the proof to preserve.
- **Dependencies:** No blocking supplier story established.
- **Disposition:** Queued for refinement, not implemented or resolved.
