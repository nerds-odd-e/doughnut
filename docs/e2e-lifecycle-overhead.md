# E2E lifecycle overhead baseline (current commands)

Baseline of the CURRENT E2E lifecycle commands, captured before the
runner-owned lifecycle migration (SEED-015 Story 8). It separates one-time
provisioning/build cost from recurring focused-run overhead so later slices
can compare. No product performance threshold is selected; these are
observations, not targets.

## Commands used

Start (one-time per checkout, then reused) — the `cy:run` wrapper owns its
stack; the explicit start below is the legacy form captured by this baseline:

```bash
unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL INPUT_DB_URL SERVER_PORT \
  LOCAL_LB_BACKEND LOCAL_LB_VITE_UPSTREAM LOCAL_LB_LISTEN_PORT \
  FRONTEND_DEV_PORT FRONTEND_BACKEND_ORIGIN SUT_RUNTIME_TARGET
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/worktree_note_editing.feature
```

Focused run (reused the stack above, run three times):

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_creation_and_update/worktree_note_editing.feature
```

Healthcheck (internal module API, not a public script):

```bash
CURSOR_DEV=true nix develop -c node scripts/sut-healthcheck.mjs
```

Owned shutdown (no public `sut:stop` script exists today; the owner control
socket `/shutdown` endpoint is invoked through `beginSutOwnerShutdown`):

```bash
CURSOR_DEV=true nix develop -c node --input-type=module -e \
  "import { beginSutOwnerShutdown } from './scripts/sut-owner.mjs'; \
   await beginSutOwnerShutdown(process.cwd());"
```

## Machine and cache conditions

- OS: macOS (darwin 25.6.0), Apple Silicon.
- Shell: Nix dev shell (`CURSOR_DEV=true nix develop -c …`).
- Shared MySQL: `127.0.0.1:3309` (already listening, not started by the SUT).
- Shared Redis: `127.0.0.1:6380` (already listening, not started by the SUT).
- Worktree path: `/Users/terryyin/git/doughnut/.worktrees/105-runner-owned-e2e-lifecycle`
  (short disposable linked worktree; the known owner-socket path limit is a
  separate Story 9 concern).
- Gradle daemon: warm from prior primary-checkout use.
- Worktree-local `backend/build`: cold for the successful start (recompiled
  fresh after a corrupted partial build was removed; see notes below).
- Cypress / Electron: cold for the first focused run, warm for runs 2 and 3.
- No global build caches were deleted for this baseline.

## Allocated identity (first `pnpm cy:run` in this worktree)

- Worktree id: `wt_519dfcf7193a44c1866d82ba78a544f0`
- Selected database: `doughnut_e2e_wt_519dfcf7193a44c1866d82ba78a544f0`
- Browser origin: `http://127.0.0.1:63216`
- Allocated ports: backend `63214`, vite `63215`, local LB `63216`.

## First startup (one-time provisioning + build + readiness)

The first `pnpm cy:run` in a fresh worktree provisions the E2E database, allocates
ports, compiles the backend, starts backend + Vite + local LB, and waits for
readiness. This is a one-time cost; later focused runs reuse the running stack.

Observations from this session (wall-clock, `date` timestamps):

| Attempt | Result | Wall-clock | Notes |
| --- | --- | --- | --- |
| 1 (first ever) | FAILED | 125s | Gradle race: `backend:watch` and `backend:sut:ci` compiled into a fresh `backend/build` simultaneously; `:compileJava` could not delete a partially-written `classes/java/main`. DB was provisioned and ports allocated before the build failure. |
| 2 | FAILED | 123s | `backend/build` left corrupted by attempt 1; `:compileJava` reported UP-TO-DATE but `DonutApplication.class` was missing, so `bootRunE2E` threw `ClassNotFoundException`. |
| 3 (after removing corrupted worktree-local `backend/build`) | PASS | 15s | SUT healthy after 5 readiness polls. Backend `Started DonutApplication in 5.783s`; Tomcat bound on `63214`. |

The successful first startup (attempt 3) measured **15s** wall-clock from
`pnpm cy:run` to healthy. Of that, the Spring Boot application context took
**5.783s** (backend log). The Gradle daemon was already warm and the E2E
database was already provisioned by attempt 1, so this 15s primarily reflects
fresh worktree-local compilation + boot + readiness wait.

Note: the two failed attempts are not "startup time" — they are a build-race
failure mode of the current `backend:sut` (`run-p backend:watch backend:sut:ci`)
design on a fresh worktree build directory. Recovering required removing the
corrupted worktree-local `backend/build` (a local build artifact, not a global
cache). This race is observable with the current commands and is recorded
here as part of the baseline; it is out of scope for this measurement slice to
fix.

## Three repeated focused runs (reused stack)

With the SUT already healthy (no readiness wait per run), the note-editing
feature was run three times consecutively. Timings are wall-clock for the full
`pnpm cypress run …` command and the Cypress-reported `Duration` (test
execution only, excluding Cypress/Electron startup).

| Run | Position | Wall-clock | Cypress Duration | Test (scenario) |
| --- | --- | --- | --- | --- |
| 1 | first-after-startup | 15s | 5s | 5846ms |
| 2 | repeated | 7s | 3s | 3390ms |
| 3 | repeated | 7s | 3s | 3456ms |

- Readiness per run: 0s — the stack was already up and healthy; each run only
  acquired the runner lease, reset fixtures, ran the scenario, and released.
- Run 1 includes Cypress/Electron cold start (browser launch, node setup);
  runs 2 and 3 benefit from warm Cypress/Electron caches, which is the main
  reason their wall-clock is about half of run 1's.
- The scenario itself ("Created note content remains after edit and reload")
  stabilises around 3.4–3.5s after warmup; run 1's 5.8s includes first-run
  browser/page warmup inside the scenario.

## Separating one-time vs recurring cost

- One-time (per checkout, per fresh stack): provisioning + build + readiness ≈
  15s when the Gradle daemon is warm and the DB is already provisioned. A
  truly cold start (cold Gradle daemon + fresh DB provisioning) costs more and
  was not measured here; the failed attempts above show the current design also
  carries a build-race risk on a fresh build directory.
- Recurring (reused stack, warm Cypress): ≈ 7s wall-clock per focused run, of
  which ≈ 3s is the scenario and the rest is Cypress/Electron launch + nix
  shell + lease acquire/release.
- First-after-startup run adds ≈ 8s of one-time Cypress/Electron warmup on top
  of the recurring cost.

## Verified process cleanup

Owned shutdown was invoked through the owner control socket (`/shutdown`).
After ~8s:

- No owned application descendants survived: `ps aux | grep
  105-runner-owned-e2e-lifecycle` returned no processes (the backend JVM on
  `63214`, the Vite dev server on `63215`, the local LB on `63216`, and the
  `sut-services.mjs` supervisor were all gone).
- No listeners remained on the allocated ports `63214`, `63215`, `63216`
  (`lsof` returned nothing).
- `verifyLiveSutOwner` returned `{"ok":false}` — the owner control server was
  gone.
- Shared services untouched: MySQL `3309` (PID 12220) and Redis `6380`
  (PID 2586) remained listening; they were never signalled.
- The primary checkout's own dev/e2e SUTs were not touched.

## Variation notes

- These timings are from one machine on one day with a warm Gradle daemon and
  pre-provisioned DB; they are not universal. Cypress/Electron warmup dominates
  the first-after-startup gap; scenario timing varies with page load and DB
  fixture reset.
- The build-race failure on a fresh worktree build directory is a real
  failure mode of the current `backend:sut` design (parallel `backend:watch`
  and `backend:sut:ci` writing the same output directory). It is recorded as
  observed behaviour, not a threshold.
- No numeric performance target is selected. This document is a baseline for
  comparison by later slices (especially slice 14), not an acceptance gate.

## New lifecycle (runner-owned) comparison — slice 14

After the runner-owned migration (SEED-015 Story 8), a single `pnpm cy:run`
owns the whole lifecycle: it provisions (if needed), starts the SUT, waits for
readiness, runs Cypress, and shuts the owned process tree down before returning.
There is no separate start step and no reused long-lived stack — every
invocation is self-contained and isolated.

Command (unchanged from the baseline focused run):

```bash
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/worktree_note_editing.feature
```

### Conditions for these measurements

- Same machine and shared MySQL `3309` / Redis `6380` as the baseline.
- Worktree already provisioned by slice 13 (`wt_519dfcf7193a44c1866d82ba78a544f0`,
  ports 63214/63215/63216, DB `doughnut_e2e_wt_519dfcf7193a44c1866d82ba78a544f0`).
- Gradle build cache warm (`~/.gradle/caches/build-cache-1` populated); the
  `--no-daemon` `bootRunE2E` process restores `compileJava` from the build cache
  rather than recompiling.
- Cypress / Electron warm after the first run in this set.

### Three repeated focused invocations (each owns the full lifecycle)

| Run | Position | Wall-clock | Readiness polls | Cypress Duration | Spring Boot context |
| --- | --- | --- | --- | --- | --- |
| 1 | first in set (cold Cypress) | 23.76s | 5 (~12s) | 5s | 5.813s |
| 2 | repeated (warm Cypress) | 21.81s | 5 (~12s) | 5s | 5.287s |
| 3 | repeated (warm Cypress) | 21.81s | 5 (~12s) | 5s | 5.423s |

- Readiness: each invocation waits for the SUT to become healthy (backend TCP +
  local LB HTTP `/__lb__/ready` + Vite). Five 3s polls (~12s) per run; the
  Spring Boot context starts in ~5.3–5.8s and the remaining readiness wait is
  poll alignment plus local LB / Vite startup.
- Test: Cypress reports `Duration: 5 seconds` for the single scenario in every
  run.
- Shutdown: each invocation sends SIGTERM to the owned process tree; `sut.log`
  ends with `Forced SUT service child exit (signal SIGTERM); releasing owned
  peers` and the supervisor PID in `sut.pid` is no longer alive. Allocated
  ports (63214/63215/63216) are free after each run.
- Run 1 is ~2s slower than runs 2/3 because of first-set Cypress/Electron
  warmup; runs 2 and 3 are within 10ms of each other.

### Cold build-asset run (cleared `backend/build`, Gradle cache warm)

Clearing the worktree-local `backend/build` and running once measured 22.50s
wall-clock — essentially the same as the cached runs. The Gradle build cache
restores `compileJava` without recompiling, so a missing worktree build
directory is not a meaningful cold start. A truly cold build (empty Gradle
build cache) would add recompilation time; that condition was not re-measured
here (the baseline's failed attempts show the cold-build race risk on a fresh
build directory, which is a separate concern).

### Multi-spec batch

Isolated Cypress supports exactly one allowlisted spec
(`assertSupportedIsolatedCypressSpecs` requires `specs.length === 1`, slice 10),
so a multi-spec batch is refused in an isolated worktree by design. A multi-spec
batch is therefore a primary-checkout scenario (the primary target accepts any
spec, `approved: null`); it was not measured here to avoid disturbing the
primary checkout, which carries pre-existing orphan SUTs from earlier sessions.
The recurring overhead comparison below is based on the single-spec focused
invocation, which is the isolated-workload path the migration targets.

### Separating provisioning/build cost from recurring overhead

- One-time provisioning (per fresh worktree): E2E database + port allocation.
  Already complete from slice 13; not re-timed here. It is a small fixed cost
  paid once per checkout, not per invocation.
- Build: with a warm Gradle build cache, `compileJava` is restored in ~1–2s
  inside the `--no-daemon` `bootRunE2E` process. A cold Gradle build cache would
  add recompilation; a cold worktree `backend/build` alone does not (cache hit).
- Recurring per-invocation overhead (warm cache, warm Cypress): ~22s wall-clock,
  split roughly as readiness ~12s (Spring Boot context ~5.4s + poll alignment +
  LB/Vite) + Cypress test 5s + Cypress/Electron launch + nix shell + shutdown
  ~5s.

### New vs baseline comparison

| Aspect | Baseline (old) | New lifecycle (runner-owned) |
| --- | --- | --- |
| Start step | separate one-time `pnpm sut` (~15s first) | none — each `pnpm cy:run` starts its own SUT |
| Recurring focused run | ~7s (reused stack, no readiness wait) | ~22s (full lifecycle per invocation) |
| Readiness per run | 0s (stack already up) | ~12s (boot + poll alignment) |
| Shutdown per run | manual `sut:stop` / owner socket | automatic SIGTERM of owned tree before return |
| Isolation | shared stack can leak/conflict | own ports, DB, process tree per invocation |
| Multi-spec batch | one stack for many specs | isolated: one allowlisted spec; primary: any specs |

The new lifecycle trades a lower recurring per-run wall-clock (the baseline's
~7s reused-stack run) for a self-contained ~22s invocation that owns startup,
readiness, test, and shutdown. The extra cost is dominated by Spring Boot
context init (~5.4s) and readiness poll alignment (~12s at 3s granularity),
plus Cypress/Electron launch. The benefit is that no long-lived shared SUT is
left running, no separate start/stop step is needed, and each invocation is
fully isolated and cleans up before returning.

### Verified process cleanup after the benchmark

After the three runs and the cold-build run, all benchmark-owned processes
were stopped: no listeners on 63214/63215/63216, the `sut.pid` supervisor PIDs
were dead, and `sut.log` showed the SIGTERM shutdown for each run. Two stale
orphan Cypress processes left from earlier slice-13 proof attempts (before the
provisioning defect was fixed) were terminated; none remained from these
benchmark runs. Pre-existing orphan SUTs from earlier sessions (Sep 5 / Sep 9)
in the primary checkout were not touched.

### Variation notes (new lifecycle)

- The ~12s readiness is coarse-grained because the 3s poll interval rounds up
  to the next poll after the backend is ready; a shorter poll interval would
  reduce the alignment loss but increase polling load.
- Spring Boot context init (~5.4s) is the largest single readiness component
  and is independent of the wrapper.
- Cypress/Electron warmup accounts for the ~2s gap between run 1 and runs 2/3.
- These timings are from one machine on one day with a warm Gradle build cache
  and an already-provisioned worktree; they are observations, not targets.
