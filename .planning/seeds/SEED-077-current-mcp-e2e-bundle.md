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
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/002-current-mcp-e2e-source/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"6e75ef8bfd5ca95e19d36a874d01501caadd1292f074c7d36078cf29874a5184","plan":"e35df8da658c318434f997088e6b143544aea3f4a14a496c1a259105b070967e"}}
```

- **Goal:** A developer changing MCP tools can trust the normal local MCP
  E2E result to describe the checked-out implementation, supporting reliable
  note-retrieval changes without a manual rebuild.
- **Scope:** The existing MCP E2E preparation task builds current source before
  the scenario starts its server, whether its bundle already exists or is
  missing. Preserve the existing connection, tool-call and teardown journey,
  including CI runs with a freshly prepared bundle. A build failure surfaces
  through the task and stops that journey before a tool result can succeed.
- **Key examples:**
  - A bundle exists from an earlier source revision → edit a tool's response
    and run `pnpm cy:run --spec e2e_test/features/mcp/mcp_services.feature`
    → the client observes the current response.
  - The bundle is missing → run that same command → preparation builds it
    and the existing tool scenarios run successfully.
  - A usable earlier bundle exists but current source cannot build → run the
    same journey → preparation reports the build failure before connecting.
  - CI has prepared a current bundle → its normal MCP feature run → search,
    graph and note-reading behavior still work with normal teardown.
- **Preparation decision:** Rebuild at each existing MCP preparation hook.
  On 2026-10-20 at `18d34c42064e5a1a704ae2da958d6ac689fbf5c0` in the
  established worktree, three `pnpm -C mcp-server bundle` runs inside Nix took
  0.652, 0.072 and 0.072 seconds, including process startup but excluding Nix
  entry and dependency installation. The actual task took 73.2–74.4 ms in
  scratch observations. This cost supports unconditional rebuilding without
  a second cache or input-freshness policy. The CLI's source-mtime mechanism
  also owns CLI installation/version and Gmail configuration concerns; those
  do not belong to this MCP preparation responsibility.
- **Expected benefit / priority:** Trustworthy local proof for MCP changes
  supporting note retrieval; this integrity gap takes priority over the
  existing queued product work while retaining its relative order.
- **Effort hypothesis:** S, about 30–60 minutes including focused proof and
  delivery; bundle cost is measured, with isolated E2E startup still to probe.
- **Boundaries:** This story does not change MCP tool semantics or broaden
  the E2E harness beyond preparing the server it runs. CI's fresh-build path
  remains part of the proof to preserve. Watch mode, shared build caches,
  generalized build orchestration and changes to CLI bundle policy are deferred.
- **Dependencies:** No blocking supplier story established.
