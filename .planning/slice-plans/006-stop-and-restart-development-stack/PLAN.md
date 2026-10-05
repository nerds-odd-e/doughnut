# Stop and restart the Development stack from the process table

## Source

- Story: [Stop and restart the Development stack without hunting for its processes](../../seeds/SEED-069-agent-development-tooling.md#reliable-development-stack-lifecycle)
- Identity: SEED-069#reliable-development-stack-lifecycle
- Provenance: the story's Instances table links the two recorded executions
  (DD-203 and the reused-PID restart failure, both 2026-10-03).

## Goal and scope

A developer or executing agent stops, starts, and restarts the primary
checkout's Development stack with one repo command each, whatever an earlier
run left behind.

- `pnpm dev:stop` ends this checkout's Development application processes and
  returns once they are gone. With no stack running it says so and succeeds.
- One ownership rule for `pnpm dev`, `pnpm dev:stop`, and `pnpm dev:restart`:
  this checkout's stack is the running Development services process started
  from this checkout, with everything descending from it, found in the process
  table.
- `dev.pid` is deleted with everything only it used; guidance reads without it.
- A process the rule does not cover is never signalled. When one holds a
  Development port, `pnpm dev` and `pnpm dev:restart` fail, name the port, and
  leave it running.
- A linked worktree refuses `pnpm dev:stop`, as it refuses the other two.
- `.agents/agent-map.md` and `docs/development-setup.md` name `pnpm dev:stop`.

Excluded: Development processes that outlived their services process (treated
like any other process on the port); how `dev.pid` came to hold `1034265`;
DD-159's stale compiled classes; any rule about when an agent may stop the
owner's stack.

Considered and left out: keeping `dev.pid` and validating it against the
process table (two sources for one fact); proving ownership of each listener
separately (the rule already covers every descendant, and start already
refuses on an occupied port); matching leftover processes by working directory
(no recorded instance).

## Architecture

- Existing solutions reused (PFE):
  - `stopOwnedDevelopmentProcessTree` in
    `scripts/development-owned-process-tree.mjs` already ends a Development
    tree from its services PID: descendants by parent walk, then the process
    and its group, TERM then KILL. DD-203's coordinator used exactly this
    through `node -e`. The stop command calls it; nothing new is written for
    termination.
  - `ps -axo pid=,ppid=,command=` is already read and parsed in
    `scripts/worktree-retirement-checkout-processes.mjs` (`listProcessTable`).
    Finding the services process uses that one reader. Do not add another `ps`
    parser.
  - `runDevStart` keeps its occupied-port refusal. Restart becomes stop, then
    start; it adds no check of its own.
- Gap filled: one function that answers "which Development services processes
  were started from this checkout", by matching the services script path under
  the checkout root in the process table. The spawn in `scripts/dev-start.mjs`
  and this finder build that path the same way.
- Removed, with their tests and fixtures: `scripts/development-pid.mjs`'s
  recorded-PID readers, `verifyOwnedDevelopmentGroup` and the per-listener
  ownership pass in `scripts/dev-restart.mjs`, the `writePidFile` call in
  `scripts/dev-start.mjs`, `pidFile` on the Development runtime target, and the
  `dev.pid` line in `.gitignore`. Shared helpers that the E2E stack still uses
  (`writePidFile`, `getListenerPids`, `isOwnedByApplicationTree`) stay.
- [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md):
  Development is persistent and developer-owned. The commands signal only this
  checkout's own stack and never touch Development data.
  [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md): an
  occupied port and a tree that survives KILL fail loudly. No North Star topic
  governs this work.
- Expected complexity change: fewer production lines and fewer injected seams
  in `scripts/dev-restart.mjs` and `scripts/dev-start.mjs`. The execution
  report states the before and after production line counts for the
  `scripts/dev-*.mjs` and `scripts/development-*.mjs` files (821 lines
  outside `*.test.mjs` today, counting `dev-restart-fixtures.mjs`).

## Outside-in proof

The stable entry points are `runDevStop`, `runDevRestart`, and `runDevStart`
with a temporary primary checkout. The stand-in stack is real: a
`scripts/development-services.mjs` file written into the temporary checkout
and spawned detached, whose child spawns a detached grandchild that listens on
an allocated port. Only the parent walk connects the listener to the services
process, as in the real stack. Tests observe real processes and real ports, not
injected liveness or kill stubs.

| Promise | Slice | Proof |
| --- | --- | --- |
| Stop ends a running stack that no file names: success, processes gone, port free | 1 | Stand-in stack with no `dev.pid` in the checkout → `runDevStop` → returns 0; services, child, and listener PIDs are dead; the port accepts no connection |
| Stop with nothing running says so and succeeds | 1 | Empty temporary checkout → `runDevStop` → returns 0 and logs that Development is not running |
| Stop leaves other checkouts' and unrelated processes alone | 1 | A second stand-in stack from another temporary checkout stays alive and listening after the first checkout's `runDevStop` |
| Linked worktree refuses stop and signals nothing | 1 | `makeLinkedWorktreeCheckout` → `runDevStop` rejects with the primary-checkout message |
| Guidance names `pnpm dev:stop` | 1 | Read `.agents/agent-map.md` "Development vs E2E" and `docs/development-setup.md` "Persistent Development" |
| The Development script tests run in CI | 1 | `pnpm test:development-stack` exists and `lint:all` calls it |
| Restart ends the running stack, then starts | 2 | Stand-in stack → `runDevRestart` with a start spy → the old PIDs are dead before the spy is called once with the checkout and target |
| Restart with no stack running just starts | 2 | Existing "idle free Development target starts without signalling" case, kept |
| Another program on a Development port: restart fails naming the port and leaves it | 2 | Real listener from the test process on the backend port, no stand-in stack → `runDevRestart` with the real `runDevStart` (spawn spy) rejects naming the port; the listener still accepts; nothing was spawned |
| Start beside an unrelated live process whose PID a leftover `dev.pid` holds | 3 | Temporary checkout whose `dev.pid` holds the test process's own PID → `runDevStart` (spawn spy, healthy stub) → returns 0 and prints the browser origin |
| Start refuses when this checkout's stack is running | 3 | Stand-in stack → `runDevStart` rejects with "already running" naming the services PID; spawn spy not called; stand-in still alive |
| Start writes no `dev.pid` | 3 | Reading: `git grep -n 'dev\.pid\|pidFile' -- scripts/dev-*.mjs scripts/development-*.mjs docs .agents .gitignore` returns nothing. No test asserts the file's absence (principle 7) |
| Development data survives a restart; the real stack obeys all three commands | 4 | Real run in the primary checkout (see slice 4) |
| Linked worktrees still refuse `pnpm dev` and `pnpm dev:restart` | all | Existing cases in `scripts/dev-start.test.mjs` and `scripts/dev-restart.test.mjs` stay green |

Commands (run from the repository root of the execution worktree):

- Focused: `CURSOR_DEV=true nix develop -c node --test scripts/dev-stop.test.mjs scripts/dev-start.test.mjs scripts/dev-restart.test.mjs scripts/dev-restart-owned.test.mjs scripts/development-services.test.mjs scripts/development-runtime.test.mjs`
  (file names follow whatever the slices leave; after slice 1 this is
  `CURSOR_DEV=true nix develop -c pnpm test:development-stack`)
- When `scripts/worktree-retirement-checkout-processes.mjs` changes:
  `CURSOR_DEV=true nix develop -c node --test scripts/worktree-retirement*.test.mjs`

## Decisive premises

| Premise | Consumed by | Observation | Result |
| --- | --- | --- | --- |
| The services process's command line names the checkout it was started from | Finder, slices 1–3 | 2026-10-05, primary checkout: `ps -axo pid=,command=` piped to `grep development-services.mjs` printed `21603 …/bin/node /Users/terryyin/git/doughnut/scripts/development-services.mjs`, untruncated through a pipe | Holds |
| Every Development listener descends from that services process by parent PID | Stop covers the whole stack | Same day: walked `ps -o pid=,ppid=,pgid=` from listeners 30931 (8081, `java`), 22394 (5175), 22901 (5176); each reached 21603. Their process groups differ (23479, 22194, 22852), so only the parent walk connects them | Holds |
| The services process leads its own process group | `stopOwnedDevelopmentProcessTree(pid)` signals `-pid` | Same `ps`: PID 21603, PPID 1, PGID 21603; `scripts/dev-start.mjs` spawns it `detached: true` | Holds |
| `stopOwnedDevelopmentProcessTree` on the services PID ends the real stack | Slice 1 approach | DD-203 record (2026-10-03): the coordinator called it through `node -e` on the live stack and "stopping it took one call". Not repeated today, because it stops the owner's stack; slice 4 observes it again | Holds on recorded evidence |
| A reused PID makes `pnpm dev` refuse today | Slice 3 reproduces the symptom | 2026-10-05: PID 597 is `accountsd`; `node -e 'process.kill(597,0)'` succeeds, and `runDevStart` throws "already running" when `readLiveDevelopmentPid` returns a PID (`scripts/dev-start.mjs:76-85`) | Holds; slice 3 first writes the failing case with the test process's own PID |
| A stale or non-PID `dev.pid` makes `dev:restart` refuse a running stack today | Slice 2 | Read `scripts/dev-restart.mjs:49-76`: `absent`, `invalid`, and not-alive each return a refusal when a port is occupied | Holds |
| Only the Development commands read or write `dev.pid` | Slice 3 deletion | `git grep -n 'dev\.pid\|pidFile'` outside tests and planning: writers and readers are `scripts/dev-start.mjs`, `scripts/dev-restart.mjs`, `scripts/development-pid.mjs`, `scripts/development-runtime.mjs`, `scripts/dev-restart-fixtures.mjs`; mentions in `.agents/agent-map.md:92`, `docs/development-setup.md:55`, `.gitignore:160`. No ADR names it | Holds |
| No Cypress feature, CI step, or other script runs the Development commands or imports their modules | Proof stays in the script tests | `grep -rl` for `development-pid`, `development-owned-process-tree`, `dev-restart`, `dev-start`, `verifyOwnedDevelopmentGroup`, `readLiveDevelopmentPid` over `scripts`, `e2e_test`, `.github`, `docs`, `.agents`, `package.json`: only the `scripts/dev-*` and `scripts/development-*` files and `package.json`'s two script lines | Holds |
| The existing Development script tests pass and are in no test group | Slice 1 adds the group | 2026-10-05, preparation worktree: `node --test` on the five existing files: 22 pass, 0 fail, 1.3 s. No `test:*` script in `package.json` lists them; CI runs `pnpm lint:all` (`.github/workflows/ci.yml:51`), which calls only the listed groups | Holds; these tests do not run in CI today |
| Tests can run real stand-in process trees with listeners | Proof design | `scripts/sut-owned-supervisor-fixtures.mjs` already writes peer scripts into a temporary checkout that spawn real listeners, used by the E2E runner cases in CI | Holds by reading; slice 1 writes the Development stand-in |

## Current decisions

- Command name `pnpm dev:stop`, script `scripts/dev-stop.mjs`.
- `pnpm dev:stop` does not run `pnpm install`; it only signals processes.
- When several services processes from this checkout are running, stop ends
  all of them. No message distinguishes that case.
- Stop returns after the owned tree is gone. It does not wait on ports held by
  other processes. Restart relies on `runDevStart`'s occupied-port refusal.
  If slice 2's real-process case shows a port still held by the stopped tree
  when start checks, keep the existing bounded wait for free ports in restart
  and record that under Learnings.
- The stop command prints one line: what it stopped (services PID) or that
  Development is not running.
- Slice 4 runs after this story branch lands on `main` (owner decision,
  2026-10-05): the primary checkout gets the commits by landing, not by switching
  branches. The run still waits for the owner's go-ahead because it stops their
  Development stack.

## Slices

### 1. `pnpm dev:stop` ends this checkout's Development stack
Type: Behavior
Status: done
Proof: new `scripts/dev-stop.test.mjs` with the real stand-in stack (rows 1–4); reading the two guidance files; `pnpm test:development-stack` green and called from `lint:all`.

Behavior: a primary checkout whose Development stack is running, with no file
naming its PID → `pnpm dev:stop` → the command prints the services PID it
stopped and exits 0; every process of that stack is gone and its ports are
free. With no stack running it prints that Development is not running and
exits 0. In a linked worktree it refuses and signals nothing.
`.agents/agent-map.md` and `docs/development-setup.md` name the command beside
`pnpm dev:restart`.

Interim: `pnpm dev` and `pnpm dev:restart` still read `dev.pid` until slices 2
and 3, and the guidance still mentions the file until slice 3.

Sizing: the largest slice. It is one proof loop; most of the work is the
stand-in stack fixture that slices 2 and 3 reuse. The guidance lines and the
test group are a few lines each and stay here so the command never exists
unnamed or untested in CI.

### 2. `pnpm dev:restart` stops by the same rule, then starts
Type: Behavior
Status: done
Proof: rewritten restart cases on the real stand-in stack (rows 7–9). The cases about missing, stale, invalid, and foreign-listener `dev.pid` are deleted with the code they covered.

Behavior: a running Development stack, whatever `dev.pid` holds → `pnpm
dev:restart` → the old stack's processes are gone and a new stack is started.
With no stack running it starts one. When another program holds a Development
port and no stack is running, it fails naming the port and that program keeps
running.

Interim: until slice 3, the start half still refuses when `dev.pid` names a
live unrelated process.

### 3. `pnpm dev` recognises a running stack from the process table, and `dev.pid` is gone
Type: Behavior
Status: done
Proof: first a failing case for the reused-PID symptom (row 10), then rows 11–12; the sweep reading in row 12.

Behavior: no stack running and a leftover `dev.pid` naming a live unrelated
process → `pnpm dev` → Development starts healthy and prints its browser
origin. With this checkout's stack already running → `pnpm dev` refuses,
naming the services PID, and starts nothing. No command reads or writes
`dev.pid`; the file's code, fixtures, `.gitignore` line, and guidance mentions
are deleted.

### 4. The real Development stack obeys the three commands
Type: Behavior
Status: planned — after landing on `main`, with the owner's go-ahead
Proof: one real run in the primary checkout, recorded in this plan.

Behavior: the primary checkout holds this story's commits and Development is
running with at least one note → `pnpm dev:stop` → ports 8081, 5175 and 5176
refuse connections and `ps` shows no `development-services.mjs` for the
checkout → `pnpm dev` → healthy, browser origin printed → `pnpm dev:restart` →
a new services PID, healthy, and the note is still returned by the app.
Finally delete the leftover untracked `dev.pid` from the primary checkout.

This run stops the owner's Development stack and needs the primary checkout to
hold the story's commits, so it runs only with the owner's go-ahead at that
time. A linked worktree cannot make this observation (it refuses Development).
If the run disagrees with slices 1–3, stop and change the plan.

## Learnings

- Slice 1 (accepted proof): `CURSOR_DEV=true nix develop -c pnpm test:development-stack`
  → 26 pass; `scripts/dev-stop.test.mjs` covers rows 1–4 on the real stand-in stack
  from `startStandInDevelopmentStack` in `scripts/dev-stack-fixtures.mjs` (reuse it in
  slices 2–3). The finder is `findDevelopmentServicesPids` in `scripts/dev-stop.mjs`;
  the services path is `developmentServicesScript` in `scripts/development-runtime.mjs`;
  the linked-worktree refusal for all three commands is
  `refuseDevelopmentInLinkedWorktree` in `scripts/development-primary-checkout.mjs`
  (its own module: putting it in `development-runtime.mjs` makes an import cycle
  through `browser-worktree-isolation` and `sut-e2e-ports`). `listProcessTable` is
  now exported from `scripts/worktree-retirement-checkout-processes.mjs`.
- Production lines after slice 1: 821 → 986 (`dev-stop.mjs`, the stand-in fixture,
  and `development-primary-checkout.mjs` added; slices 2–3 delete the `dev.pid` code).
- Slice 2 (accepted proof): `CURSOR_DEV=true nix develop -c pnpm test:development-stack`
  → 17 pass; `scripts/dev-restart.test.mjs` covers rows 7–9 and the linked-worktree
  refusal. `runDevRestart` is refuse → `stopDevelopmentServices` (exported from
  `scripts/dev-stop.mjs`) → `runDevStart`. No wait for free ports was needed: the
  listener's port is free when stop returns, so the bounded wait was deleted.
  The stop rule now lives entirely in `scripts/dev-stop.mjs`;
  `scripts/development-owned-process-tree.mjs`, `scripts/dev-restart-fixtures.mjs`
  and `scripts/dev-restart-owned.test.mjs` are deleted. The SIGTERM→SIGKILL
  escalation stays in the shared `terminateOwnedProcessTree`, exercised by the
  E2E stack's tests. Slice 3: `dev-stop.mjs` still imports `isProcessAlive` from
  `scripts/development-pid.mjs`; give it a home when that module's readers go.
  `targetFor` is in `scripts/dev-stack-fixtures.mjs`.
- Production lines after slice 2: 707.
- Slice 3 (accepted proof): failing first, `CURSOR_DEV=true nix develop -c node --test
  --test-name-pattern=row10 scripts/dev-start.test.mjs` with `dev.pid` holding the test
  process's PID failed "Development is already running (live process …)", then passed
  after the change. That case was then deleted: once nothing reads `dev.pid`, a test
  writing one only proves a removed thing has no effect (principle 7); row 10's
  outcome stays covered by "free unconfigured primary starts Development, prints
  browser origin when healthy". Row 11 is "this checkout's running stack refuses
  start, naming its services pid" on the real stand-in stack.
  `CURSOR_DEV=true nix develop -c pnpm test:development-stack` → 17 pass. Row 12
  reading returns nothing. `scripts/development-pid.mjs` is deleted.
- The ownership rule (`findDevelopmentServicesPids`, `stopDevelopmentServices`) now
  lives in `scripts/development-stack-processes.mjs`; `dev-start`, `dev-stop` and
  `dev-restart` import it. Earlier learnings naming `scripts/dev-stop.mjs` for these
  refer to that module now.
- Production lines (`scripts/dev-*.mjs` + `scripts/development-*.mjs`, excluding
  `*.test.mjs`, counting the stand-in fixture): 821 before → 672 after slice 3.
