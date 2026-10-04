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
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/004-hold-worktree-e2e-stack/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"8dfae608d4fd7fd729d04e20fcbca343d37803b2bd0cb88b53a556fdb9ef3206","plan":"dacaeb113bb84ed6e59f50ca667e80a92b5a622fad27fce688d3a84aedf311cf"}}
```

- **Goal:** A developer or executing agent in a linked worktree can observe its
  unmerged code in a running app, against real services when the slice needs
  them, without moving the primary checkout or writing temporary scaffolding.
  This removes the improvised routes recorded in the instances below; it does
  not change what plans prescribe (shared ODF-190).
- **Scope:**
  - One repo command, run from a linked worktree, starts that worktree's own
    disposable E2E stack and keeps it up until interrupted. It prints the
    browser origin and stops the stack cleanly on interrupt. It follows
    [ADR 0007](../../docs/adrs/0007-environments-and-isolation-accepted.md):
    the worktree's own E2E database and ports, runner-owned and disposable.
  - The held app is ready to use: the seeded accounts (for example
    `old_learner` / `password`) can sign in without a further step.
  - Paid calls are off by default: the held stack starts without the OpenAI
    token even when the shell has one. An explicit option passes the token
    through. Agent guidance says that option needs the owner's authorization
    each time.
  - `.agents/agent-map.md` names the hold command where it says linked
    worktrees refuse the Development stack, and names CI as the route for
    real-service Cypress specs on branch code (CI runs on every pushed branch
    with the OpenAI secret). `docs/worktree-browser-tests.md` describes the
    hold command beside `cy:run` and `cy:open`.
  - Unchanged: linked worktrees still refuse the Development stack and
    real-service Cypress specs.
  - Deferred: running real-service Cypress specs in a linked worktree; driving
    a real browser against the held app (the route gives the origin; who
    scrolls or speaks is the observer's concern).
  - Out of scope: shared Open Dough planning guidance (ODF-190 in DearDough),
    and rebuilding a `.venv-mineru` whose Python was garbage-collected (DD-161).
- **Key examples:** each replays a recorded instance.
  - CLI against the held app (DD-161, 2026-09-29, SEED-059#story-3 slice 6):
    a linked worktree on branch code → the agent runs the hold command, points
    the CLI at the printed origin, attaches a PDF, and interrupts the command →
    the stack is gone and no `hold-stack.mjs` was written.
  - Manual browser observation (2026-09-29, SEED-059#story-5): a slice needs a
    real mouse wheel on branch code → the hold command prints an origin a real
    browser opens and signs in to as `old_learner` → the observation happens on
    branch code from the worktree.
  - Paid journey with authorization (2026-10-03,
    SEED-066#preserve-completed-speech): the owner authorizes a real dictation
    run → the agent runs the hold command with the paid option from the
    worktree → dictation reaches real OpenAI, and the primary checkout stays
    on `main` with Development untouched.
  - Paid calls off by default: the shell has `OPENAI_API_TOKEN` → the hold
    command runs without the paid option → an AI feature used in the held app
    makes no call that OpenAI accepts.
  - Real-service spec (2026-10-04, SEED-066#keep-every-transcribed-sentence):
    an agent reads the agent map before running
    `record_live_audio_with_real_open_ai_service.feature` on branch code → it
    finds CI named as the route and pushes the branch instead of running the
    refused local command or calling OpenAI directly.
- **Instances:** grouped in
  [Observing branch code against real services from an execution worktree](../../DonutRetrospectiveFindings.md#queued-observing-branch-code-against-real-services-from-an-execution-worktree);
  the last three are the Donut-side occurrences of
  [ODF-190](../../DearDough.md#odf-190--the-plan-prescribed-production-observations-whose-access-route-or-log-source-did-not-exist-and-whose-results-could-not-change-the-approach).

  | Date | Execution | What happened | Cost | This story's route |
  | --- | --- | --- | --- | --- |
  | 2026-09-29 | SEED-059#story-3, plan 051 slice 6 (after `6f36cb2952`) | Agent wrote a temporary `hold-stack.mjs` around `runE2eInteractive` to `/attach` real PDFs through the CLI | About 28 minutes, most of it scaffolding and venv repair | Hold command (venv repair stays out of scope) |
  | 2026-09-29 | SEED-059#story-5, plan 053 slice 1 (`5989892325`) | Real mouse-wheel observation had no route from the worktree; a throwaway Cypress spec scrolled 0 px | About 15 minutes; key example shipped unobserved | Hold command plus a real browser |
  | 2026-10-03 | SEED-066#preserve-completed-speech, plan `2b34db5abb:.planning/slice-plans/001-keep-completed-speech/PLAN.md` slice 4 | Paid dictation journey on branch code had no route; primary checkout detached at `fd904bc2d3` and `1a9816737d` | About 30 minutes waiting for the owner, about 7 minutes restoring Development | Hold command with the paid option |
  | 2026-10-04 | SEED-066#keep-every-transcribed-sentence, plan `9ad1cedcc0:.planning/slice-plans/008-keep-every-transcribed-sentence/PLAN.md` slice 4 | Local real-service `cy:run` refused in the worktree; two direct whisper-1 calls | One owner round-trip, two paid calls not specifically authorized, proof moved to CI | Agent map names CI |

- **Observed during refinement (2026-10-05, this worktree):**
  - `runE2eInteractive` with a waiting child in place of Cypress provisioned
    the worktree's E2E database, served the app healthy in about one minute,
    and on SIGINT stopped every process (exit code 1).
  - A fresh held database has no users: `old_learner` signed in only after
    `POST /api/testability/clean_db_and_reset_testability_settings`.
  - The owner's shell exports `OPENAI_API_TOKEN`, and the `e2e` profile reads
    it, so a held stack inherits paid access unless the command removes it.
  - `ci.yml` runs on pushes to every branch.
  - Commit `8d1d77fb39` (2026-09-12) removed the detached `sut` start, restart
    and health commands so that each stack has one owning command. The hold
    command keeps that rule: it owns its stack for its own lifetime, as
    `cy:open` does.
  - Every OpenAI operation in `OpenAiApiHandler` refuses with "OpenAI is not
    available (no API key configured)" before any network call when the token
    is empty (read during slice planning).
- **Plan:** [Hold a worktree's E2E stack for observing unmerged branch code](../slice-plans/004-hold-worktree-e2e-stack/PLAN.md)
- **Effort hypothesis:** S to M — medium confidence; the stack lifetime
  already exists, so the work is the command, seeding, the token option, and
  guidance.
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
