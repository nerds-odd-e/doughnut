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
