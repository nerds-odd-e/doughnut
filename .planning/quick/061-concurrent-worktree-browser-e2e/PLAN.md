# Concurrent browser verification in local worktrees

## Source

[SEED-015, story 2](../../seeds/SEED-015-concurrent-worktree-environments.md#story-2)
— Run browser E2E scenarios concurrently without external-service mocks.

Status: planned. Refinement recommended for leaves 2 and 3 before execution.
This plan authorizes no implementation by itself.

## Goal and scope

Two local worktrees can each start their own app, run one focused browser
note create/edit scenario, and reset fixtures without changing the other's
state. Restarting an idle environment leaves the other environment usable.

Support local Nix `pnpm sut`, `pnpm sut:healthcheck`, `pnpm sut:restart`, and
`pnpm cypress run --spec` for the focused feature. Reuse the existing worktree
identity and shared MySQL; automatically prepare the E2E database and endpoints
on first SUT use. Keep unconfigured primary defaults and CI behavior.

One SUT and one Cypress runner per worktree. SUT and its Cypress runner coexist;
duplicate starts/runs and restart during a Cypress run are refused. Backend-test
ownership is separate. This does not promise concurrent compilation in one
checkout. Unsupported specs fail before reset or mock/client setup.

Exclude mocked/live external services, CLI/MCP, full-suite coverage, Cypress
open/alias coverage, direct Gradle boot entry points, development profiles,
worktree hooks, cleanup, copied-ID recovery, schema rollback, Cloud VM, and
capacity scheduling. No second worktree identity or separate MySQL server.

## Execution context

- `scripts/backend-test-worktree-owner.sh` combines identity allocation,
  database provisioning, and backend-test lifetime locking. Preserve 1a–1c
  behavior while extracting only identity initialization needed by the next
  Behavior. Its current config is `{ "id": "wt_..." }`; preserve old files.
- `scripts/sut-start.mjs` detaches `sut-services.mjs`, records only a PID, and
  polls fixed endpoints. `sut-services.mjs` starts Mountebank unconditionally.
  `sut-restart.mjs` signals listeners on fixed ports; isolated mode must never
  use that ownership assumption.
- `scripts/local-lb.mjs` already accepts listen/backend/Vite environment
  settings. `package.json`'s `local:lb:vite` hardcodes the Vite URL;
  `frontend/vite.config.ts` fixes Vite and proxy ports. Change their environment
  selection together while preserving ADR 0005 routes.
- Backend `bootRunE2E` selects the e2e profile; `application.yml` accepts
  `INPUT_DB_URL`, and ordinary Spring environment properties can supply server
  and datasource settings. Do not extend the backend-test task parser merely
  to launch the SUT. Flyway must finish against the selected E2E database
  before readiness succeeds.
- `e2e_test/config/common.ts` owns Cypress node setup; both root config and
  `ci.ts` use it. `config/constants.ts` fixes browser/backend origins. Supply
  browser-safe settings from node configuration; do not read local files in
  browser code. Follow generated-client setup through its actual consumers.
- `step_definitions/hook.ts` resets through testability at Before order 0;
  mock and client hooks follow. Guard supported scope before that reset.
- Reuse note creation/editing page objects and named navigation from
  `features/note_creation_and_update/{note_creation,note_edit}.feature` in one
  focused `worktree_note_editing.feature`. Existing full features contain more
  behavior than this story promises; do not widen the allowlist implicitly.
- Redis settings exist in the profile, but no Redis/session integration was
  found in the searched Java code. Verify the selected login/reset/save path's
  actual mutable storage before declaring isolation; configuration alone is
  not evidence that Redis requires allocation. Do not introduce speculative
  Redis management. Revisit scope if an actual additional shared store blocks
  the promised path.

## Current decisions

1. Persist one environment identity. Brief initialization coordination may be
   shared with backend tests; SUT/Cypress have separate lifetime ownership.
   Existing identity-only configs can gain an E2E allocation without replacing
   their ID or repairing their unit-test database.
2. E2E allocation owns a distinct database, three application ports, and local
   process/log records. Coordinate concurrent allocation; verify actual binds
   and process ownership at launch. An occupied port fails visibly, without
   replacing the recorded allocation or terminating the listener.
3. Isolated mode omits Mountebank startup and readiness. An explicit supported
   feature allowlist keeps later mocks/clients from silently reaching defaults.
4. Reject conflicting datasource, Flyway, proxy, server, or Cypress origin
   overrides rather than mixing targets. Matching overrides can remain usable.
   Print database and browser origin. A recorded missing database or migration
   failure is not permission to adopt, delete, or rebuild a database.
5. Preserve primary defaults only when no identity is configured. Gate local
   isolation consistently so CI remains unchanged. A configured primary uses
   isolation. Update the local-origin guidance in `docs/gcp/prod_env.md` and
   `.cursor/rules/e2e-authoring.mdc` to distinguish these environments.
6. The service supervisor owns child processes until shutdown. Surface a child
   failure in the worktree log as soon as its exit is observed, mark health
   failed, and release owned peers. Startup timeout/cancellation also releases
   owned children. Do not kill an unverifiable PID from a stale record.

Relevant accepted ADRs: [0005](../../../docs/adrs/0005-web-routes-accepted.md)
(route semantics unchanged) and
[0006](../../../docs/adrs/0006-failure-handling-accepted.md)
(visible failures, no silent fallback). No ADR change is proposed.

## Outside-in proof and promise ownership

| Contract promise | Owner | Observation |
|---|---|---|
| Existing identity and backend command behavior survive extraction | 1 | Existing launcher/lock/provisioning/ordinary-command tests stay green |
| First SUT use creates and reuses a distinct E2E environment | 2 | Two real SUTs report different databases/origins; fixture marker fetched through each app belongs to that app; shell restart reuses allocation |
| Allocation/startup cannot adopt foreign state | 2 | Concurrent initialization and occupied-port/preparation-failure boundary cases preserve foreign resources; recorded missing DB fails |
| Browser creates, edits, reloads its own note | 3 | Focused ordinary Cypress spec passes through each worktree's proxy |
| A reset leaves B's fixtures and edits intact | 3 | Coordinated two-worktree browser run observes B marker after A reset and vice versa; shared/unit-test sentinels survive |
| Unsupported specs never reset or start external services/clients | 3 | Guard rejection observed before reset/mock/client invocation; no Mountebank startup required |
| Concurrent use within one worktree respects ownership | 2, 3, 4 | Duplicate SUT (2), duplicate Cypress (3), and restart during run (4) visibly refuse |
| Restart affects only owning processes | 4 | Restart A while B keeps its original processes and saved note; foreign PID/port record is refused |
| Health reports the correct environment | 2, 4 | Readiness fails when owning child is absent even with unrelated healthy listeners |
| Child failure/cancellation releases owned peers promptly | 5 | Running supervisor observes forced child exit, logs failure without another command, releases peers; unrelated process survives |
| Overrides cannot redirect work to shared targets | 2, 3 | Conflicting SUT target (2) and Cypress origin (3) refuse before mutations; matching values work |
| Primary/CI compatibility and no new route semantics | 2, 3 | Default launcher/config regression tests plus focused app navigation |
| Developer can repeat the supported workflow | 3, 4 | Capability guide includes exact ordinary commands, origin output, exclusions, and restart behavior |

## Ordered slices

### 1. Share identity initialization without changing backend verification

Type: Structure
Status: planned
Proof: Existing backend worktree command-boundary tests remain green, including
concurrent first use, stale-lock reclaim, configured environments, and primary
compatibility: `CURSOR_DEV=true nix develop -c pnpm test:backend-test-worktree`.

Internal change: Separate reusable identity/config initialization from the
backend-test run lock only as needed for leaf 2. Preserve validation, provisioning
and config publication guarantees. Keep one canonical initializer; no general
environment framework. Backend invocation behavior remains unchanged.

Immediate next Behavior: First ordinary SUT start obtains a persistent isolated
browser environment (leaf 2).

Sizing: about 5 minutes, medium confidence, one existing regression loop.
If extraction grows into provisioning/lifecycle redesign, refine this leaf.

### 2. Start and identify an isolated application in each worktree

Type: Behavior
Status: planned
Proof: Drive ordinary SUT start/health boundaries in focused Node tests, then
start two real worktrees and read distinct injected fixture markers through
their proxies. Record literal commands, IDs, origins, and observations here.

Behavior: A fresh linked worktree (or identity-only configured checkout) with
MySQL running → `pnpm sut` → a healthy, identifiable app backed by its own
migrated E2E database and persisted endpoints, without Mountebank.

Implementation scope: Reuse leaf 1 initialization; provision only the new E2E
allocation; consistently configure backend, Vite, local proxy and healthchecks.
Acquire owning startup state before spawning; reject conflicts and duplicate
starts. Include basic failed-start cleanup and ownership validation so this
commit never exposes the old port-killing restart to an isolated environment.
Until leaf 4, isolated restart should refuse explicitly. Preserve primary/CI
defaults and old identity files. Document the supported startup command.

Proof variations: Fresh versus configured identity, concurrent initialization,
reuse from a new shell, foreign listener, conflicting override, missing recorded
DB, provisioning/migration failure. These establish one startup contract but
contain separable implementation and verification beats.

Sizing: low confidence; likely above 10 minutes of implementation. **Refinement
recommended** before execution. Do not call this target-sized or run one giant
startup slice. Split around narrower usable startup conditions and immediately
enabling Structure while preserving first-use scope in the final story.

### 3. Run a focused browser note edit without cross-worktree reset

Type: Behavior
Status: planned
Proof: Ordinary `CURSOR_DEV=true nix develop -c pnpm cypress run --spec
e2e_test/features/note_creation_and_update/worktree_note_editing.feature` in
both worktrees; synchronize the proof so B reads its marker after A's reset,
then reverse roles. A simple pair of independently passing runs is insufficient.

Behavior: Two healthy owning SUTs → each runs the focused note create/edit spec
with overlapping execution → each sees its own saved note after reload despite
the other runner resetting fixtures.

Implementation scope: Resolve persisted environment in Cypress node setup,
propagate both browser and backend targets to the actual request consumers,
and validate target consistency before any reset. Add focused feature with
existing page objects and a bounded two-worktree orchestration proof. Require
owning SUT readiness; Cypress alone need not start/provision services. Acquire
one Cypress-run lease, release it on completion/cancellation, and fail duplicate
runs. In isolated mode reject unapproved specs before hooks, including mixed
supported/unsupported selections; do not silently filter and report success.
Keep default primary/CI selection unchanged. Add the repeatable browser command
and exclusions to `docs/worktree-browser-tests.md` and update origin guidance.

Sizing: low confidence; multiple integration/proof beats remain. **Refinement
recommended** before execution (ordinary spec safety/ownership and coordinated
reset proof need smaller leaves). Keep the full coordinated scenario `@wip`
until green; do not commit failing active tests.

### 4. Restart only the idle owning application

Type: Behavior
Status: planned
Proof: Focused restart command tests using real disposable child processes;
restart A in the two-worktree fixture and verify B retains its PID set, marker,
and browser edit ability while A recovers on its recorded origin.

Behavior: A is idle and B is running → `pnpm sut:restart` in A → only A's
verified owned application processes are replaced, and A becomes healthy.

Implementation scope: Replace isolated-mode refusal from leaf 2 with owner-
verified termination and restart. A live Cypress lease refuses restart before
signalling anything. Missing/unverifiable ownership fails visibly; port lookup
never confers ownership. Extend the guide with restart and conflict recovery
limits; no database deletion or standalone retirement command.

Sizing: about 5 minutes of implementation, medium confidence if leaf 2 supplies
ownership records; real startup time is an explicit focused-test exception.

### 5. Observe and release a failed running application

Type: Behavior
Status: planned
Proof: Run the real supervisor over disposable child processes. Force one child
to exit after readiness; observe the failure log while the supervisor still
owns the run, failed health, and peer termination without a follow-up command.
Also cancel startup and verify child release; an unrelated child stays alive.

Behavior: An owned SUT is running → one application child exits unexpectedly
→ failure is visible immediately on exit observation, and owned peers stop.

Implementation scope: Complete supervisor post-readiness failure observation
and cleanup; share basic cleanup supplied by leaf 2. Do not add restart retries
or recovery into another allocation. Keep evidence at the owning process
boundary, rather than asserting only an awaited startup throw.

Sizing: about 5 minutes, medium confidence, one supervisor proof loop. If
existing process-group behavior requires redesign, refine before changing it.

## Verification and delivery

- Use focused existing SUT start/service/health/restart Node tests as each area
  changes, and add command-boundary tests for configuration/ownership outcomes.
  Run tools via `CURSOR_DEV=true nix develop -c ...`.
- The real two-worktree browser proof is required; no broad E2E suite or manual
  browser testing is requested. Use temporary linked worktrees and isolated
  data. Never reset the shared database as part of proof; read-only sentinels
  can establish preservation. Record exact commands and observed postconditions
  during execution, not invented pass claims here.
- No novel SQL/transaction assumption is introduced: reuse proven MySQL
  database provisioning and standard startup Flyway migration. The two-app
  capacity/reset proof remains pending, not established by earlier backend
  suites. No new migration or destructive DDL experiment is planned.
- Each leaf follows execute-plan's Jidoka, fresh refactor agent, API generation
  if needed, one coordinator `./scripts/run.sh pnpm format:changed`, plan update,
  commit and push. No product/API change is currently anticipated.
- Enforce ~5-minute scrutiny and >10-minute decomposition; focused real startup
  duration may be recorded as an exception, implementation complexity may not.
  On changed story scope or a larger-than-L finding, return to the home story.

## Learnings

Planning inspection found fixed origins in both Cypress and service commands,
unconditional Mountebank startup/readiness, and restart by listener port rather
than owner. These explain why a database-only change cannot deliver this story.
No implementation or runtime verification has been performed for this plan.
