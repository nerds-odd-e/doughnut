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
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/006-stop-and-restart-development-stack/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"70913318b2c80a1c6468403a164a8b21484538a07092f9871637d0e43ca0764a","plan":"29ea5e6ae6672d07c509528c9823be4ea1822654a04d4f4fa0638b059f078f00"}}
```

- **Goal:** A developer or executing agent can stop, start, and restart the
  primary checkout's Development stack with one repo command each, whatever an
  earlier run left behind. This removes the process hunting and failed
  restarts recorded in the instances below; Development stays the owner's
  persistent stack
  ([ADR 0007](../../docs/adrs/0007-environments-and-isolation-accepted.md)).
- **Scope:**
  - `pnpm dev:stop`, beside `pnpm dev` and `pnpm dev:restart`, ends this
    checkout's Development application processes (backend, Vite, and the local
    load balancer) and returns once they are gone and their ports are free.
    Development data, MySQL, and Redis are untouched. With no stack running it
    says so and succeeds.
  - One ownership rule for all three commands: this checkout's Development
    stack is the running Development services process started from this
    checkout, with everything descending from it. The commands find it in the
    process table. `pnpm dev` refuses when it is running, `dev:stop` ends it,
    and `dev:restart` ends it and then starts.
  - `dev.pid` is deleted with everything only it used, and the guidance reads
    without it (principle 7). A recorded PID is a second copy of what the
    process table already says, and both recorded failures came from that copy
    being wrong.
  - A process the rule does not cover is never signalled. When such a process
    holds a Development port, `pnpm dev` and `dev:restart` fail, name the port,
    and leave it running (ADR 0007;
    [ADR 0006](../../docs/adrs/0006-failure-handling-accepted.md)).
  - Unchanged: a linked worktree refuses `dev:stop` as it refuses `pnpm dev`
    and `dev:restart`. An agent in a worktree runs the command in the primary
    checkout.
  - `.agents/agent-map.md` and `docs/development-setup.md` name `pnpm dev:stop`
    beside `pnpm dev:restart`.
  - Deferred: Development processes that outlived their services process and
    still hold a port. They are not covered by the rule, so the commands treat
    them like any other process holding the port. No recorded instance.
  - Out of scope: how `dev.pid` came to hold `1034265` (unknown, and no longer
    relevant once the file is gone); DD-159's stale compiled classes, whose
    cause is unknown; and any new rule about when an agent may stop the
    owner's stack.
- **Key examples:**
  - Stop a stack no record names (DD-203, 2026-10-03,
    SEED-067#stacks-survive-other-builds slice 2): Development runs under
    services process 10342 and nothing in the checkout names that PID → an
    agent runs `pnpm dev:stop` in the primary checkout → the command returns
    success, ports 8081, 5175 and 5176 are free, and no process of that stack
    remains.
  - Start beside an unrelated process (2026-10-03,
    SEED-066#preserve-completed-speech): no Development stack is running and a
    leftover `dev.pid` names PID 597, now `accountsd` → `pnpm dev:restart` →
    Development starts healthy and prints its browser origin; `accountsd`
    keeps running. `pnpm dev` gives the same result.
  - Restart a running stack: Development is running with notes in
    `doughnut_development` → `pnpm dev:restart` → the old processes are gone,
    a new stack is healthy on the same ports, and the notes are still there.
  - Nothing to stop: no Development stack is running → `pnpm dev:stop` → it
    reports that nothing is running and succeeds.
  - Another program on a Development port: a process not started by this
    checkout's Development services listens on 8081 and no stack is running →
    `pnpm dev:restart` → it fails naming port 8081, and that process keeps
    running.
  - Linked worktree: an agent runs `pnpm dev:stop` in a linked worktree → the
    command refuses, says Development belongs to the primary checkout, and
    signals nothing.
- **Instances:** grouped in
  [Starting, stopping and restarting the Development stack](../../DonutRetrospectiveFindings.md#queued-starting-stopping-and-restarting-the-development-stack).

  | Date | Execution | What happened | Cost | This story's route |
  | --- | --- | --- | --- | --- |
  | 2026-10-03 | SEED-066#preserve-completed-speech, plan `2b34db5abb:.planning/slice-plans/001-keep-completed-speech/PLAN.md` slice 4 | `dev:restart` failed because `dev.pid` named PID 597, which the OS had given to `accountsd` | About 7 minutes restoring Development | `dev:restart` finds the stack in the process table |
  | 2026-10-03 | SEED-067#stacks-survive-other-builds, plan 005 slice 2 (DD-203) | `dev.pid` held `1034265` while the stack ran under PID 10342; the coordinator walked the process tree and called `stopOwnedDevelopmentProcessTree` through `node -e` | About six extra tool calls | `pnpm dev:stop` |

- **Observed during refinement (2026-10-05, primary checkout, read-only):**
  - The running stack's services process shows in `ps` as
    `node /Users/terryyin/git/doughnut/scripts/development-services.mjs`, so
    its command line names the checkout it was started from.
  - Each of the three listeners (8081 `java`, 5175 `node scripts/local-lb.mjs`,
    5176 Vite) reaches that services process by walking parent PIDs. They sit
    in different process groups, so the parent walk, not the group, is what
    connects them.
  - PID 597 is `accountsd` again today, and `process.kill(597, 0)` succeeds:
    `pnpm dev` would refuse with "Development is already running" for a
    `dev.pid` holding `597`.
  - `dev.pid` is written with `writeFile`, which truncates, so a partial
    overwrite does not explain `1034265`.
  - Only `scripts/dev-start.mjs` writes `dev.pid`, and only it and
    `scripts/dev-restart.mjs` read it. No ADR names the file.
- **Plan:** [Stop and restart the Development stack from the process table](../slice-plans/006-stop-and-restart-development-stack/PLAN.md)
- **Effort hypothesis:** M — medium confidence; the stop and ownership pieces
  exist, and the work is replacing the recorded PID with the process-table
  rule across three commands and their tests.
- **Depends on:** None.
