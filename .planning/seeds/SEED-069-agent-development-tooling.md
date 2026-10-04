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

Agents executing Donut stories in linked worktrees repeatedly lost time, owner
attention, and planned proof because Donut's local stack tooling has no
supported route for two common needs: observing unmerged branch code against
real services, and stopping or restarting the Development stack. The evidence
is grouped in [Donut Retrospective Findings](../../DonutRetrospectiveFindings.md).
Both gaps sit in Donut's own scripts and agent guidance, not in shared Open
Dough skills.

## Parent Problem

### Executing agents improvise around Donut's local stacks

A developer or agent working on unmerged code cannot reach a real-service or
manual observation from its worktree, and cannot stop or reliably restart the
primary checkout's Development stack, without hand-written scaffolding or the
owner's help.

## Story Decomposition

Effort bands follow this project's seed convention: S = 30–60 minutes,
M = 1–2 hours, L = 2–4 hours, including delivery. These are hypotheses.

<a id="observe-branch-code-against-real-services"></a>
### Observe unmerged branch code against real services from an execution worktree

**Identity:** SEED-069#observe-branch-code-against-real-services
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **Goal:** A developer or executing agent in a linked worktree can observe its
  unmerged code in a running app, against real services when the slice needs
  them, without moving the primary checkout or writing temporary scaffolding.
- **Scope (to refine):**
  - One supported, documented route from a linked worktree to a running app
    that stays up for manual or real-service observation, and stops cleanly.
  - `.agents/agent-map.md` names that route where it currently says linked
    worktrees refuse the Development stack.
  - Open: whether the route is a held disposable E2E stack, a worktree-scoped
    Development stack, or a deliberate live-spec opening; and how paid live
    calls stay owner-authorized. Refinement decides with the owner.
  - Out of scope: shared Open Dough planning guidance (ODF-190 in DearDough),
    and rebuilding a `.venv-mineru` whose Python was garbage-collected (DD-161).
- **Key examples (tentative):**
  - A slice needs a real-PDF `/attach` through the CLI against real MinerU:
    the agent starts the route from its worktree, observes, and stops it; no
    `hold-stack.mjs` is written (DD-161).
  - A slice's proof is a real-service dictation journey on branch code: it runs
    from the worktree, and the primary checkout stays on `main`.
- **Evidence:** [Observing branch code against real services from an execution worktree](../../DonutRetrospectiveFindings.md#queued-observing-branch-code-against-real-services-from-an-execution-worktree)
  — DD-161 and the Donut-side occurrences of ODF-190 (2026-09-29 to
  2026-10-04): about 28, 15, and 30 minutes, one owner round-trip, and
  unplanned paid calls.
- **Effort hypothesis:** M — low confidence until the route is chosen.
- **Depends on:** None.

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
