---
id: SEED-069
status: dormant
planted: 2026-10-05
planted_during: owner-directed triage of Donut-specific retrospective findings
trigger_when: selecting a queued story that makes Donut's local stacks cheaper for executing agents to use
scope: small
---

# SEED-069: Let executing agents run and observe Donut's local stacks without improvising

## Why This Matters

Agents executing Donut stories repeatedly lost time and owner attention
because Donut's local stack tooling has no supported route to stop or restart
the Development stack. The evidence is grouped in
[Donut Retrospective Findings](../../DonutRetrospectiveFindings.md). The gap
sits in Donut's own scripts and agent guidance, not in shared Open Dough
skills.

## Parent Problem

### Executing agents improvise around Donut's local stacks

A developer or agent cannot stop or reliably restart the primary checkout's
Development stack without hand-written scaffolding or the owner's help.

## Story Decomposition

Effort bands follow this project's seed convention: S = 30–60 minutes,
M = 1–2 hours, L = 2–4 hours, including delivery. These are hypotheses.

<a id="reliable-development-stack-lifecycle"></a>
### Stop and restart the Development stack without hunting for its processes

**Identity:** SEED-069#reliable-development-stack-lifecycle
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **Goal:** A developer or executing agent can stop and restart the primary
  checkout's Development stack with one repo command each, even when `dev.pid`
  is stale.
- **Scope (to refine):**
  - A stop command beside `pnpm dev` and `pnpm dev:restart`, sharing
    `dev:restart`'s ownership checks.
  - Stop and restart act only on the stack this checkout owns when `dev.pid`
    holds a non-PID value or a PID the OS has reused for another process.
  - Out of scope: DD-159's stale compiled classes, whose cause is unknown.
- **Key examples (tentative):**
  - The stack is running and `dev.pid` holds `1034265`, which `ps` rejects: stop
    ends the running stack (DD-203).
  - `dev.pid` names PID 597, now `accountsd`: restart leaves that process alone
    and starts Development.
- **Evidence:** [Starting, stopping and restarting the Development stack](../../DonutRetrospectiveFindings.md#queued-starting-stopping-and-restarting-the-development-stack)
  — DD-203 and the reused-PID restart failure (2026-10-03).
- **Effort hypothesis:** S to M — medium confidence.
- **Depends on:** None.
