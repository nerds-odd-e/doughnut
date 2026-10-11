# Migration-aware HTTP readiness

**Source:** [Start E2E sessions after database migration completes](../../seeds/SEED-067-reliable-e2e-startup.md#start-e2e-after-migrations)
**Identity:** SEED-067#start-e2e-after-migrations

## Goal and scope

A contributor's first isolated E2E invocation waits for application startup and
Flyway before resetting test data. Terry chose application healthcheck ownership:
`GET /api/healthcheck` returns 503 while starting, then 200 with the existing
`OK. Active Profile: <profiles>. Commit: <commit>` text. The runner keeps polling
HTTP through the local load balancer.

Include the real HTTP migration interval, ready-body/access compatibility,
successful held-session and Cypress continuations, and existing failure and
ownership handling. Defer database repair, orphan reclamation, migration/SQL
changes, new operational endpoints, and timeout/autohealing policy changes.

Preparation observations were made in
`/Users/terryyin/git/doughnut/.worktrees/seed-067-e2e-readiness-refinement`, branch
`codex/seed-067-e2e-readiness-refinement`. Maki-chan's preparation assignment was
announced at `b833c8cdb55198bacd8399ae303289f9e6a175e9`. Execution uses the workspace
established by its authorized execution start; the recorded preparation location
is evidence provenance. This plan grants no execution or publication authority.

## Existing solution and constraints

- Change `HealthCheckController` to consume Spring Boot's existing
  `ApplicationAvailability` / `ReadinessState.ACCEPTING_TRAFFIC`. One startup
  state owns the rule; add neither a migration flag nor runner SQL polling.
- `FlyWayFreeVersionIgnoreMigrationStrategyConfig` defers non-test migration.
  `FlyWayFreeVersionRealMigration` synchronously repairs/migrates on the
  highest-precedence `ApplicationReadyEvent` listener. Installed Boot 4.1.1
  publishes accepting-traffic readiness after that event's listeners return.
  Source reading supports this choice; slice 1 must observe the consuming HTTP
  outcome before the production change.
- Reuse the current chain: application → `scripts/local-lb.mjs` readiness →
  `scripts/sut-healthcheck.mjs` → owned invocation → hold reset or Cypress.
  Existing ownership checks, deadlines, cancellation and shutdown stay with
  their current owners.
- Production LB checks and autohealing use status. Deployment verification in
  `infra/gcp/scripts/app-instance-healthcheck.sh` also recognizes `OK` in the
  body independently of status: the 503 body is `Starting`, without a success
  marker. The ready body and anonymous healthcheck access remain compatible.
  Frontend sign-in/profile parsing and Development health also consume the
  ready text. Generated `ping` remains a string response.
- Follow [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md): allow
  migration failure to propagate; do not catch it to declare readiness or
  rebuild the database. Follow [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md):
  backend proof owns Unit Test data; full-stack proof owns isolated E2E data.
  Never use persistent Development/Production or shared default databases.
- Existing North Star topics do not govern this startup rule. The established
  endpoint, availability state and HTTP consumers suffice; no new topic,
  service boundary or ADR is needed.

## Outside-in proof ownership

| Source promise | Observable proof | Owner |
| --- | --- | --- |
| HTTP is reachable while real migration is running, but readiness is pending | Background Boot startup, real Flyway callback held inside migration, actual HTTP GET returns 503 `Starting` before release | 1 establishes baseline and route; 2 retains regression |
| Startup completion changes pending to ready | Release migration; await actual Boot startup completion; same HTTP endpoint returns 200 with exact profile/commit text | 2 |
| Ready `dev`/`prod` text and access remain compatible | HTTP contract cases using active-profile values and actual build commit; consumer profile/commit parsers remain green; anonymous endpoint access retained | 2 |
| A fresh held session waits before reset and announcement, then is usable | App/LB 503 during the migration barrier, HTTP access trace has no reset before release; migration completes, reset succeeds, seeded login works and hold announces ready | 3 |
| First Cypress invocation on a fresh allocation waits, resets and completes its feature | App/LB pending during the barrier, no Cypress launch/reset before release; real selected feature passes after migration and owned services stop | 4 |
| Startup failure stays unsuccessful, diagnostic and owned | Existing startup-timeout, service-exit, cancellation, reset-failure and cleanup tests observe nonzero outcomes and stopped owned processes | 3, 4 |
| Resource isolation persists | Recorded database/ports and real ownership checks on both invocations; existing health/lifetime suites; only owned disposable resources reclaimed | 3, 4 |
| Maintained guidance describes health readiness | Update the health/start sections in existing operational guides to describe 503 until startup completes and unchanged success text | 2 |

## Ordered slices

### 1. Observe HTTP during the real migration interval
Type: Behavior
Status: done
Proof: reproduce the current premature HTTP 200 while a real Flyway migration
call is blocked, then show 200 after release and completed startup. This is a
baseline observation, not passing proof of the requested 503 behavior.

Behavior: the planner/executor starts an isolated Boot HTTP startup with the
actual controller, Boot availability and deferred migration listener → a real
Flyway callback signals entry and waits on a release barrier → an HTTP client
reaches `/api/healthcheck` before release and records the current success.

Reuse the real-Flyway callback pattern in `NotebookGitStartupServicesProbeTest`.
Use a scoped test-owned embedded Boot web startup against the backend command's
isolated Unit Test datasource, explicitly exercising the actual deferred
strategy/listener rather than the ordinary early test migration strategy. Do
not run the full E2E application on Unit Test data. No mocked Flyway, manually
published accepting-traffic event, or stand-in health response can establish
this premise. A MockMvc readiness-state case alone is insufficient.

Release the barrier in `finally`, await migration/startup settlement and close
the probe context. Keep any baseline-only assertions disposable; retain the
command, profile/configuration, callback entry, HTTP status/body and completion
ordering here. Establish the smallest test-only callback route for full-stack
observations as well, without a retained production delay option.

**Gate:** if HTTP cannot answer inside migration, the current response is not
the reported early 200, Boot accepts traffic before migration returns, isolation
is uncertain, or the harness needs broader infrastructure work, stop slices
2–4 and refine this plan from the evidence. Do not implement around an
unreproduced symptom. Target 5–8 minutes of active work; scrutinize at 5 and
stop/redecompose at 10. Required backend test/boot waits are the stated exception.
Stop-safe: record learning and remove scratch changes; no product change ships.

### 2. The application reports startup readiness through HTTP
Type: Behavior
Status: done
Proof: slice 1's actual HTTP pending → ready case fails with unconditional
success and passes with the readiness gate; ordinary ready HTTP cases preserve
status, text, profile, commit and access. Run all backend tests and affected
consumer checks below.

Behavior: HTTP is reachable while startup/migration is incomplete → return 503
`Starting`; actual Boot startup completes successfully → return the existing
200 text. Implement this in `HealthCheckController` using application
availability and a string HTTP response, such as `ResponseEntity<String>`.
Retain the real HTTP startup regression from slice 1. Ordinary contract cases
reuse `ControllerTestBase` / its MockMvc context; do not add another cached
application context or readiness mock. Scope and close the extra embedded
startup probe required to observe this boundary.

Regenerate OpenAPI/client output in this same slice when changing the controller
signature; keep the generated string response and align any affected callers.
Update `docs/worktree-browser-tests.md` and the existing healthcheck description
in `docs/gcp/prod_env.md`; retain operator grace/deadline settings. Target about
5 minutes of active change after the probe; required full backend, generation
and consumer-suite waits can exceed 10 minutes and cannot be reduced by a
layer/file split. Other active-work overrun requires refinement. Stop-safe:
one truthful health signal works with existing consumers.

### 3. A fresh held session starts after migration and seeds usable data
Type: Behavior
Status: planned
Proof: first `CURSOR_DEV=true nix develop -c pnpm e2e:hold` on the execution worktree's
unallocated E2E database, with the test-only migration barrier established in
slice 1. Capture actual backend `/api/healthcheck` and LB `/__lb__/ready` as 503
while blocked, and an HTTP access trace showing reset only after migration
release/completion. Then both readiness endpoints return 200, the real reset
seeds users, `old_learner` can authenticate, and the runner announces a held
browser session. SIGINT the owning hold process and verify its services stop.

Behavior: a contributor invokes hold on a fresh isolated allocation → the
runner stays in HTTP readiness until migration completes → fixture reset and
the held session succeed without recovery. Use the real runner and MySQL, not
an injected healthy lifetime. Temporary callback/access-trace support belongs
only to this owned observation and is removed afterwards; verify ordinary hold
still starts without that support. Do not introduce a product delay flag or
change runner readiness responsibility.

Inspect `.worktree.local.json` before starting. If the E2E allocation already
exists because of an earlier attempt, preserve it; use an owned fresh acceptance
checkout at the validated revision for this fresh precondition, rather than
dropping/recreating an allocation as a workaround. Run the relevant existing
failure/ownership suites. Target about 5 minutes of active observation; cold
build/startup and required suite waits are external-wait exceptions. Stop on an
unsettled failure and record it. Stop-safe: retain truthful evidence and settle
only this invocation's owned services.

### 4. A fresh Cypress invocation waits and completes its selected feature
Type: Behavior
Status: planned
Proof: in a separately owned fresh acceptance checkout of the same validated
revision, run `CURSOR_DEV=true nix develop -c pnpm cy:run --spec
e2e_test/features/notebooks/notebook_creation.feature`. Use the same test-only
migration barrier: actual app/LB HTTP readiness is pending and Cypress/reset
have not begun before release; after completion, the real feature passes,
returns exit 0, and the runner stops its owned services.

Behavior: a contributor starts the first Cypress invocation on a fresh E2E
allocation → the existing HTTP wait precedes Cypress's real fixture reset →
the notebook-creation journey succeeds without manual database recreation.
The fresh checkout prevents slice 3's already-migrated database from supplying
this precondition. Keep source changes in the execution worktree; the acceptance
checkout only consumes the same revision. The admitted feature requires no
paid service or private mock. Remove temporary probe support, then verify its
ordinary invocation. Reclaim that temporary checkout's exact disposable
databases via the retirement command after idle verification, and remove only
the owned acceptance checkout. Keep the execution worktree for delivery.

Target about 5 minutes of active observation; worktree preparation, cold startup
and browser/test waits are external-wait exceptions. Stop on a missing route,
ownership ambiguity or failed promised outcome and refine before retrying.
Stop-safe: accepted evidence is retained and owned services/resources settled.

## Verification and delivery

- Backend rules require **all** backend tests:
  `CURSOR_DEV=true nix develop -c pnpm backend:test_only`. Linked-worktree wrappers
  provision the owned Unit Test datasource. No SQL migration changes are planned.
- Controller-signature changes require
  `CURSOR_DEV=true nix develop -c pnpm generateTypeScript` in the same change;
  never hand-edit generated output. The generated-client and frontend skills
  require `CURSOR_DEV=true nix develop -c pnpm frontend:test`,
  `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`, and
  `CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit`
  against the resulting content.
- HTTP and lifecycle consumers:
  `CURSOR_DEV=true nix develop -c pnpm test:sut-healthcheck`,
  `CURSOR_DEV=true nix develop -c pnpm test:sut-start`,
  `CURSOR_DEV=true nix develop -c pnpm test:browser-worktree-isolation`,
  `CURSOR_DEV=true nix develop -c node --test scripts/dev-healthcheck.test.mjs`,
  and `CURSOR_DEV=true nix develop -c bash scripts/test/app-instance-healthcheck.sh.test`.
  Inspect the latter's pending/ready response assertions; add a Starting → ready
  example if its current fixtures do not exercise this body's consumer outcome.
  The existing lifetime cases cover handled failure/cleanup; no extra unit test
  of uncaught Flyway failure is required by ADR 0006.
- User AGENTS.md / dough-execute-plan require each delivery's Jidoka → fresh
  post-change refactor agent → API generation when needed → coordinator runs
  `./scripts/run.sh pnpm format:changed` once → update plan → agent-authored
  commit with check-only lint hook → push. Keep unresolved observations as story
  obligations; status alone does not establish publication or acceptance.

## Preparation observations and remaining premises

All observations below ran in the named preparation worktree at `b833c8cdb5`
with only the seed draft, and no retained product changes.

- `CURSOR_DEV=true nix develop -c pnpm test:sut-healthcheck`: 17/17 passed.
  Scratch-disabling the 2xx gate in `checkHttpReady` and rerunning that same
  command failed the HTTP-503 case (`true == false`, 16/17 passed). Reverting
  and rerunning passed 17/17. This proves HTTP status rejection and ownership
  discrimination, not the application's migration timing.
- Actual local LB plus runner HTTP probe: inline Node launched
  `scripts/local-lb.mjs` against a disposable loopback HTTP backend; consumed
  `/__lb__/ready` with the real `checkHttpReady`. Backend 503 →
  `{ok:false,status:503}`; backend 200 → `{ok:true,status:200}`. All temporary
  listeners stopped. This settles the existing status-consumption chain only;
  it supplies neither Flyway nor a successful fixture reset.

  Literal observation command:

  ```bash
  CURSOR_DEV=true nix develop -c node --input-type=module <<'JS'
  import assert from 'node:assert/strict';
  import http from 'node:http';
  import { once } from 'node:events';
  import { spawn } from 'node:child_process';
  import { checkHttpReady } from './scripts/sut-healthcheck.mjs';
  let status = 503;
  const backend = http.createServer((req, res) => {
    assert.equal(req.url, '/api/healthcheck');
    res.writeHead(status, {'content-type':'text/plain'});
    res.end(status === 503 ? 'Starting' : 'OK. Active Profile: e2e. Commit: fixture');
  });
  backend.listen(0, '127.0.0.1');
  await once(backend, 'listening');
  const reserve = http.createServer();
  reserve.listen(0, '127.0.0.1');
  await once(reserve, 'listening');
  const port = reserve.address().port;
  await new Promise(resolve => reserve.close(resolve));
  const lb = spawn(process.execPath, ['scripts/local-lb.mjs'], {
    cwd:process.cwd(),
    env:{...process.env, LOCAL_LB_LISTEN_PORT:String(port), LOCAL_LB_BACKEND:'http://127.0.0.1:'+backend.address().port, LOCAL_LB_VITE_UPSTREAM:'http://127.0.0.1:'+backend.address().port},
    stdio:['ignore','pipe','pipe']
  });
  try {
    await once(lb.stdout, 'data');
    const url = 'http://127.0.0.1:'+port+'/__lb__/ready';
    const pending = await checkHttpReady({url});
    assert.deepEqual(pending, {ok:false,status:503});
    status = 200;
    const ready = await checkHttpReady({url});
    assert.deepEqual(ready, {ok:true,status:200});
    console.log(JSON.stringify({pending,ready,result:'actual local LB and runner HTTP probe consume backend status'}));
  } finally {
    const exited = once(lb, 'exit');
    lb.kill('SIGTERM');
    await exited;
    await new Promise(resolve => backend.close(resolve));
  }
  JS
  ```
- Unpaid, side-effect-free Cypress selection prefix executed:

  ```bash
  CURSOR_DEV=true nix develop -c node --input-type=module <<'JS'
  import assert from 'node:assert/strict';
  import { selectedCypressSpecs, assertSupportedIsolatedCypressSpecs } from './scripts/isolated-cypress-spec-selection.mjs';
  const spec = 'e2e_test/features/notebooks/notebook_creation.feature';
  const selected = selectedCypressSpecs({argv:['--spec',spec],checkoutRoot:process.cwd(),parentSpecArgs:[]});
  const requirements = assertSupportedIsolatedCypressSpecs(selected);
  assert.deepEqual(selected,[spec]);
  assert.deepEqual(requirements,{requiresPrivateOpenAiMock:false,requiresPrivateWikidataMock:false});
  console.log(JSON.stringify({selected,requirements,result:'accepted before provisioning or reset'}));
  JS
  ```

  It accepted the exact spec with both private-mock requirements false. Stopped
  before provisioning, application startup or reset. Actual DB/startup/reset,
  seeded login and browser outcomes are state-changing observations, owned by
  the early probe and its gated acceptance slices, not claimed complete here.
- `HealthCheckControllerTest`'s existing direct-call ready-body assertion and
  `NotebookGitStartupServicesProbeTest`'s real migration/later-listener ordering
  do not discriminate pending HTTP readiness. Slice 1 replaces that missing
  proof premise with a consuming HTTP observation before dependent work.
- The real non-test timing, test-only startup route and callback harness sizing
  remain unobserved state-changing premises, explicitly bounded by slice 1's
  failure/overrun stop. No application, backend suite or migration ran during
  this preparation.

## Current decisions and plan review

One common rule governs every example: traffic is accepted only when Boot says
startup is complete. No Structure slice is warranted. The early probe owns the
consequential timing/harness learning; the HTTP change and its regression stay
together; hold and Cypress have independent continuation proof loops and retain
separate acceptance slices. Stopping after the HTTP change retains usable value.

Slice-plan refinement is not needed: these boundaries are cohesive, proof is
mapped, and required test/startup waits have explicit sizing exceptions. No
blocking concern remains outside the permitted early probe. Readiness is recorded
in the source story after reviewing this plan; it does not authorize execution.

## Execution context

Story Branch Mode in `.worktrees/start-e2e-sessions-after-database-migration-comp`,
branch `claude/start-e2e-sessions-after-database-migration-comp`, remote `origin`,
target `main`. The Take was accepted on `main` at `2f17bb1f1f`. Agent: Nana-chan.

## Learnings

### Slice 1: the early success is reproduced and the gate passes

Observed at `2f17bb1f1f` with
`unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL; CURSOR_DEV=true nix develop -c pnpm backend:test:worktree --tests 'com.odde.donut.controllers.HealthCheckStartupReadinessTest'`
(one test selected, passed). The probe starts `DonutApplication` on a real port
with `--spring.profiles.active=e2e`, so the deferred strategy and
`FlyWayFreeVersionRealMigration` run, and holds a real Flyway `BEFORE_MIGRATE`
callback open. Database: `doughnut_wt_9b333ef56a6f404f8fd58e684563eda4_test`.

- While the migration was held: `GET /api/healthcheck` returned 200
  `OK. Active Profile: e2e. Commit: <commit>`; Boot readiness was
  `REFUSING_TRAFFIC`.
- After release: the same 200; readiness `ACCEPTING_TRAFFIC`.
- Order: migration entered, healthcheck 200, migration completed, startup
  completed, healthcheck 200.

Every gate condition held, so slices 2-4 continue unchanged. The probe is kept
uncommitted in the execution worktree as
`backend/src/test/java/com/odde/donut/controllers/HealthCheckStartupReadinessTest.java`;
slice 2 turns it into the retained pending-to-ready regression.

Harness facts slice 2 relies on:

- The profile must be the command-line argument `--spring.profiles.active=e2e`.
  `.profiles("e2e")` adds to Gradle's `test` profile, and the early test
  migration then runs before the web server exists.
- Read the port from `WebServerInitializedEvent`. Asking the web server for its
  port before it starts creates a default connector on 8080.
- The callback holds `migrate()` open on an already-migrated database; it is a
  real migration call, not a pending versioned migration.

Full-stack route for slices 3 and 4, with no product or test code: put a
`beforeMigrate.sql` in a temporary directory outside the checkout and start the
runner with `SPRING_FLYWAY_LOCATIONS='classpath:db/migration,filesystem:<dir>'`.
The file takes and releases a uniquely named MySQL lock
(`SELECT GET_LOCK(...); SELECT RELEASE_LOCK(...);`) that a separate `mysql`
session holds until release. Observed: a `SELECT SLEEP(4);` callback supplied
this way delayed the same probe from 5.68s to 10.79s. Not yet observed: the
named-lock hold and release, and the variable reaching the `bootRunE2E` JVM
through `pnpm e2e:hold` / `pnpm cy:run`; slice 3 confirms both before relying
on them.

### Slice 2: the healthcheck follows Boot readiness

`HealthCheckController.ping()` returns 503 `Starting` until
`ApplicationAvailability` reports `ACCEPTING_TRAFFIC`, then 200 with the
unchanged text. Accepted proof on the delivered content:

- `HealthCheckStartupReadinessTest` over real HTTP with the `e2e` profile: 503
  `Starting` while the migration is held, 200 ready text after startup, in the
  order migration entered, healthcheck 503, migration completed, startup
  completed, healthcheck 200. Before the controller change it failed with
  `Expected: is <503> but: was <200>`.
- `HealthCheckControllerTest` through MockMvc with no credentials: 200 and the
  exact `OK. Active Profile: test. Commit: <build commit>`. The shared test
  context is really in `ACCEPTING_TRAFFIC`; no readiness mock was needed.
- `CURSOR_DEV=true nix develop -c pnpm backend:test_only`: 2736 tests, 0
  failures. Frontend tests (2105), both frontend typechecks,
  `test:sut-healthcheck` (17), `test:sut-start` (72),
  `test:browser-worktree-isolation` (112), `scripts/dev-healthcheck.test.mjs`
  (4), `test:development-stack` (18) and both infra shell tests passed.
- `pnpm generateTypeScript` produced no diff: `ping` stays a 200 string
  response and the 503 is not declared in OpenAPI.

The refactor pass gave the Unit Test datasource URL one test-support home,
`com.odde.donut.testability.UnitTestDatasource`, used by the readiness probe,
`DevelopmentAuthenticationConfigurationTest` and `NotebookGitJdbcFixture`.

For slices 3 and 4: the dev-login page and the E2E login steps call the
healthcheck and would see 503 before readiness. The runner's wait comes first,
so check there first if a login step fails.

## Story obligations

### G1. Ready dev and prod text is not requested over HTTP
Reported: slice 2 — "dev/prod ready text is not exercised over HTTP."
Story clause: "A ready `dev` or `prod` application is checked → HTTP 200 still carries that application's active profile and deployed commit in the current format."
Disposition: proved by slice 2: `HealthCheckController.ping()` builds the text from the active profiles and the build commit for every profile; `HealthCheckControllerTest` (`test`) and `HealthCheckStartupReadinessTest` (`e2e`) observe that one path over HTTP, and `scripts/dev-healthcheck.test.mjs` plus the frontend sign-in specs read `dev` and `prod` bodies.

### G2. Production anonymous access is not requested in a test
Reported: slice 2 — "Production anonymous access is not exercised."
Story clause: "Preserve profile and deployed-commit reporting and the endpoint's existing access policy."
Disposition: proved by slice 2: the delivered change leaves every security configuration untouched, including the `/api/healthcheck` rule in `ProductionConfiguration`, and both HTTP tests request the endpoint without credentials.

### G3. Cypress login consumers of the healthcheck were not run
Reported: slice 2 — "These are Cypress full-stack consumers; they run after readiness and belong to slices 3–4."
Story clause: "`pnpm cy:run --spec <feature>` waits for readiness, then runs the selected feature successfully against the fully migrated, seeded database without a manual drop/recreate."
Disposition: receiving slice 4

### G4. Production health-check configuration has no local suite
Reported: slice 2 — "These are status-based production GCP configuration with no local suite."
Story clause: "Terry approved making these consumers wait for startup."
Disposition: excluded "new operational probes, and startup/autohealing timeout changes"
