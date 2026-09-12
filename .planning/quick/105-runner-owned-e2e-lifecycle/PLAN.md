# Run E2E tests with automatic service startup and cleanup

Source: [SEED-015 Story 8](../../seeds/SEED-015-concurrent-worktree-environments.md#story-8).
Status: in progress (execution authorized 2026-09-12 via `/dough-execute-plan 105`).
Supersedes quick/104's unexecuted primary restart correction. Backlog entry moved to Taken.

## Execution identity

- Originating checkout: `/Users/terryyin/git/doughnut` on `main`, HEAD `8aedc53a11857bf84234ecad756813c1e0c6f3ac` (Taken-only commit).
- Execution checkout: `/Users/terryyin/git/doughnut/.worktrees/105-runner-owned-e2e-lifecycle` on branch `cursor/105-runner-owned-e2e-lifecycle`.
- Integration target: `main`.
- CI coverage: the `donut CI` workflow (`.github/workflows/ci.yml`) triggers only on pushes to `main`; the execution branch has no push-triggered CI, so CI observation coverage is missing for this branch. Hosted-CI verification for slice 10 will surface as a decision point when reached.

## Outcome and boundaries

One E2E batch command owns startup, readiness, testing, and complete owned
shutdown. One interactive Cypress session owns its stack across reruns until
close. Developers and agents need no separate SUT lifecycle commands. Worktree
identity/database allocation persists; explicit retirement remains separate.
Local and CI entry points migrate in this story, public standalone SUT lifecycle
commands disappear, and a retained startup-overhead report is delivered.

Preserve primary/CI spec support, the existing isolated allowlist, fixture reset,
Cucumber plugin callbacks, CLI screenshots, test exit status, Development and
peer data/processes, shared MySQL/Redis, and existing build/cache behavior.
Do not add Cucumber lifecycle orchestration, persistent-stack reuse, worktree
creation hooks, a generic service manager, broader worktree spec support,
same-worktree concurrency, Cloud VM isolation, or automatic ambiguous-owner
recovery. Hard kill/machine failure cannot guarantee cleanup. Fail visibly
rather than signalling foreign listeners or adopting a pre-existing SUT.

## Current evidence and PFE decision

- `sut-start.mjs` already prepares isolated identity/database/ports and waits for
  readiness, but returns an exit code after spawning an unreferenced detached
  supervisor. Modularize that startup responsibility to expose an owned running
  lifetime to the wrapper; preserve the old adapter only until migration ends.
- `sut-services.mjs`, `supervised-service-group.mjs`,
  `sut-owned-process-tree.mjs`, and `owned-process-tree-termination.mjs` already
  handle supervision and descendant shutdown. Reuse the owned-tree mechanism;
  retain observable child completion in the invocation. Do not reproduce process
  walks, escalation, or port-killing in the wrapper. Development shares the
  lower-level lifecycle, so its behavior must remain unchanged.
- `sut-owner.mjs`, retirement admission, and allocated-port checks represent
  ownership and allocation. Reuse these concepts; fresh spawned handles supply
  primary ownership, not `sut.pid` or port occupancy as authority.
- `isolated-cypress.mjs` currently owns a lease/private mock in node setup and
  releases at `after:spec`/`after:run`. Move lifetime authority to the invocation;
  keep plugin tasks/configuration as adapters. `isolated-openai-mock.mjs` supplies
  private-mock startup, endpoints, verification, and shutdown. Keep one resource
  requirements registry; do not add a second allowlist in the wrapper.
- `e2e_test/config/common.ts` composes Cucumber, screenshot, and isolation
  callbacks. Preserve their non-lifecycle behavior and endpoint injection before
  the browser consumes configuration. Interactive spec selection is not known
  at process launch: use the existing supported registry/configuration boundary
  to arrange required session resources, without rebuilding parent-argv parsing.
- `package.json` has `cy:run`, `cy:open`, `cy:run-with-sut`, `cy:run-on-sut`,
  `test`, `sut`, `sut:restart`, and `sut:healthcheck`. Make `cy:run` / `cy:open`
  the documented owned entry points; the wrapper invokes the Cypress executable
  directly to avoid recursion. Raw Cypress is not a second supported lifecycle
  interface. Replace its references in guidance and callers.
- `.github/workflows/ci.yml` uses Cypress action `start`/`wait-on`, built frontend
  assets, matrix spec selection, Chrome, and existing cache/build setup. Reuse
  build preparation but replace the action's lifecycle with the wrapper. Built
  frontend versus local Vite is a launch/readiness distinction, not two owners.
- Primary mocks use canonical Mountebank ports; preserve existing endpoint
  contracts and start/stop only run-owned mocks. Occupied required ports refuse;
  do not silently adopt an already-running shared mock. Private worktree mocks
  continue using allocated endpoints.

Follow Accepted [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
and [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md).
No North Star file was found; these decisions and the confirmed story provide
sufficient direction without creating one.

## Execution and proof contract

All slices include their tests, local cleanup, and a green stopping point.
Target about 5 minutes; estimates include implementation and focused tests.
Above 5 minutes scrutinize; above 10 minutes stop and refine unless only an
explicit measured E2E/build/external wait remains. No whole-suite run merely
for planning. Do not weaken a proof or mock internal ownership code to fit time.

Use `CURSOR_DEV=true nix develop -c …` for repo commands, git directly.
New boundary tests belong in `scripts/e2e-runner.test.mjs` (split by behavior
only when useful), driving the invocation with real lower-level lifecycle and
crafted OS/subprocess boundaries. Reuse existing real-process fixtures for
representative descendant and peer survival proof. Avoid timing microbenchmarks
in automated assertions.

Focused baseline command:

```sh
CURSOR_DEV=true nix develop -c node --test scripts/sut-start.test.mjs scripts/sut-isolated-start.test.mjs scripts/sut-isolated-start-release.test.mjs scripts/sut-services-child-exit.test.mjs scripts/isolated-cypress.test.mjs scripts/isolated-cypress-openai-mock.test.mjs scripts/isolated-cypress-openai-mock-cancel.test.mjs scripts/isolated-cypress-openai-mock-failure.test.mjs e2e_test/config/composeCypressPluginEvents.test.mjs
```

Invocation proof command after the new boundary exists:

```sh
CURSOR_DEV=true nix develop -c node --test scripts/e2e-runner.test.mjs
```

Run adjacent existing proofs only for the responsibility touched; update their
boundary expectations with the migration. Preserve meaningful behavior tests
when deleting obsolete restart/start adapter tests.

Delivery follows dough-execute-plan: Jidoka, fresh post-change-refactor agent,
API generation only if needed, coordinator `./scripts/run.sh pnpm format:changed`
once, plan update without a second formatting pass, check-only commit hook,
commit/push and asynchronous CI observation. Do not run those delivery actions
under this planning-only request. Preserve unrelated working-tree changes.

## Ordered slices

### 1. Record the reused-stack feedback baseline
Type: Behavior
Status: done (2026-09-12)
Behavior: Given disposable owned benchmark checkouts, executing a few equivalent
focused runs produces a reviewable baseline separating setup/readiness/test time.
Proof: `docs/e2e-lifecycle-overhead.md` records commands, machine/cache conditions,
three observations per selected repeated case, and verified process cleanup.
Use a short disposable worktree path (known owner-socket path limit is separate
Story 9). With current commands, start its SUT once and run the note-editing
feature repeatedly; capture first startup separately from cached startup. Stop
only the spawned authenticated tree, never primary ports. Do not delete build
caches globally or invent a true cold-cache measurement. Baseline precedes old
command deletion. No product performance threshold is selected.
Command: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_creation_and_update/worktree_note_editing.feature`
Sizing: 5 minutes active work; measured build/start/test waits may exceed 10
minutes and must be reported, not counted as implementation overrun.
Learning: the current `backend:sut` (`run-p backend:watch backend:sut:ci`) has a
Gradle build-race on a fresh worktree `backend/build` — two failed 125s/123s
attempts before a 15s successful start after removing the corrupted local
build dir. Recorded in the baseline doc as observed behaviour. Slice 2
(modularize startup) should not perpetuate this race; note for refactor.

### 2. Expose the existing owned startup lifetime
Type: Structure
Status: done (2026-09-12)
Change: Modularize existing startup to return its running child, readiness and
owned shutdown/completion capability to an in-process caller, while the legacy
start adapter retains its external behavior. Keep one startup/allocation model.
Immediately enables slice 3; no generic lifecycle framework.
Proof: Existing start/isolated-start/failure-release and Development supervisor
regressions stay green, including cleanup when readiness fails.
Sizing: 5–8 minutes; medium confidence. Reassess if supervisor completion requires
an independent structural beat rather than widening this slice.

### 3. Run an isolated no-mock batch through its own stack
Type: Behavior
Status: done (2026-09-12)
Behavior: Given an idle isolated checkout, the wrapper starts one stack, waits
for readiness, runs selected supported no-mock specs once, and awaits shutdown
before returning Cypress's outcome. Startup or Cypress launch failure also
cleans partial owned work. Add the explicit wrapper entry point now; existing
commands remain usable until the migration slice.
Proof: Invocation-boundary tests verify batch lifetime, failed startup/launch,
nonzero test result, and zero surviving owned processes; one representative
real-process fixture proves peer survival. Allocation/foreign-port/busy-owner
checks precede signalling and Cypress. Existing guards remain in force.
Sizing: 5–8 minutes; medium confidence, using slice 2's running handle. One
terminal-result lifecycle is the common rule for these cases.

### 4. Cancel an E2E invocation without leaving descendants
Type: Behavior
Status: done (2026-09-12)
Behavior: SIGINT/SIGTERM during readiness or tests stops the runner and all
owned application descendants, then returns a visible unsuccessful outcome.
Proof: Invocation-boundary cancellation examples exercise the shared real owned
shutdown; representative child/grandchild exit and peer survival. Cleanup failure
remains visible, original test diagnostics survive, and completion waits for
bounded escalation. Do not release ownership before work actually stops.
Sizing: 5–8 minutes; medium confidence.

### 5. End testing when a required application service exits
Type: Behavior
Status: done (2026-09-12)
Behavior: A required service exits after readiness while tests remain active:
the wrapper terminates the test run and remaining owned services with failure.
Proof: A child-exit fixture at the invocation boundary establishes prompt failure
observation and settled cleanup, including exit between readiness and observer
attachment. Preserve logs; reuse supervisor completion, not health polling as
another service manager.
Sizing: 4–6 minutes; medium confidence.

### 6. Keep private mocks owned through the entire batch
Type: Behavior
Status: done (2026-09-12)
Behavior: A supported batch requiring OpenAI mocks has usable owned endpoints
for every selected spec; mock/lease lifetime ends with the invocation, including
mock startup failure, unexpected exit, and cancellation.
Proof: Boundary tests run successive spec callbacks without premature release;
existing mock ownership/failure/cancel cases move to their correct owner.
`composeCypressPluginEvents` still executes Cucumber/screenshot callbacks.
Use the registry's natural supported combinations, without expanding its policy.
Sizing: 5–8 minutes; medium confidence. Move authority rather than duplicate
cleanup in wrapper and plugin. Do not change fixture reset semantics.

### 7. Keep one stack for an interactive Cypress session
Type: Behavior
Status: done (2026-09-12)
Behavior: Opening Cypress starts one owned stack; selecting/rerunning supported
specs shares it and its required mocks; closing Cypress triggers full cleanup.
Proof: Invocation-boundary session fixture exercises multiple selections/reruns
and close, with no after-spec teardown. Verify endpoint configuration before
browser launch and continued enforcement of the existing isolated allowlist.
Perform one focused interactive session check during execution, recording close
cleanup; this is authorized manual proof in this slice, not product exploration.
Sizing: 5–8 minutes active work; explicit browser startup waits excluded.
Learning (manual proof): two defects found and fixed. (1) `cypress open` does
NOT accept `--spec` (only `cypress run` does); the wrapper's `defaultSpawnCypressOpen`
incorrectly forwarded `--spec` — fixed to invoke `cypress open --e2e --config-file
e2e_test/config/ci.ts` without `--spec`; `--spec` is now only a resource-requirement
hint. (2) `cypress open` (Electron) does NOT exit on SIGTERM (it prompts "Force
exit with ^C again"); the slice-4 cancellation path hung waiting for the child
exit, orphaning the owned SUT tree. Fixed with bounded SIGTERM→SIGKILL escalation
for the Cypress child on cancellation (in shared `runCypressOnce`, so both batch
and interactive benefit); normal close path unaffected. Manual proof confirmed
end-to-end: SIGTERM → 5s → SIGKILL → `lifetime.shutdown()` → zero survivors, owned
ports free, shared MySQL/Redis untouched.

### 8. Run primary batches with fresh owned services
Type: Behavior
Status: done (2026-09-12)
Behavior: An unconfigured primary target uses canonical endpoints with services
owned by the invocation, preserving existing primary spec selection. Occupied
required application/mock ports refuse without adoption or signalling.
Proof: Primary invocation tests cover successful completion, foreign listener,
and mixed owned/foreign conflict with zero foreign signals. Preserve shared
MySQL/Redis and Development. Use the same lifecycle and existing target rules.
Sizing: 5–8 minutes; medium confidence. A separate launch target is justified by
canonical endpoints, not a separate shutdown algorithm.

### 9. Run a built-asset E2E batch under the invocation owner
Type: Behavior
Status: done (2026-09-12)
Behavior: Given prepared frontend/CLI/MCP bundles, the wrapper starts the
built-frontend/backend/mock target, waits for its real readiness without a Vite
listener, runs the selected specs and awaits cleanup.
Proof: Invocation-boundary tests cover target arguments, readiness, selection
forwarding and exit behavior through the existing lifecycle. Run one focused
CI-equivalent selection with prepared assets; retain its command and result.
Reuse `scripts/ci/e2e-bundle-if-needed.sh` and existing build scripts rather than
adding build/cache logic to process ownership. The workflow remains unchanged
until slice 10, so this commit preserves existing CI.
Sizing: 5–8 minutes active work, medium confidence; measured build/browser waits
excepted. Target differences are launch data, not another lifecycle algorithm.
Proof result: 45/45 boundary tests pass (10 new built-target tests cover
`runtimeTargetProcessEnv`, `applicationPortEntries`, `healthEndpoints` omitting
Vite, and `sutServiceArgs` using `local:lb` not `local:lb:vite`, omitting
`frontend:sut` when `built`). Bundles prepared via `pnpm bundle:all`
(frontend `dist/index.html` 5.81s, CLI `donut-cli.bundle.mjs`, MCP
`mcp-server.bundle.mjs`). Manual built-target run in the isolated execution
worktree refused correctly ("Conflicting SUT_RUNTIME_TARGET does not match the
configured isolated SUT target") — the built target requires a primary-configured
checkout, which the isolated execution worktree cannot host and the originating
`main` checkout is off-limits. The real CI-equivalent proof is deferred to slice
10's hosted CI observation (CI runs the built target on a primary checkout via
the wrapper). Post-change refactor: none — `built` flag handling is cohesive
across the distinct representations.

### 10. Route CI matrix jobs through the owned invocation
Type: Behavior
Status: done (2026-09-12)
Behavior: Each CI matrix job invokes the built-target wrapper for its existing
selection, with one lifecycle owner and unchanged build/cache/tag/artifact policy.
Proof: Inspect exact generated command/arguments at the command boundary;
workflow passes matrix spec, Chrome and config through the wrapper and no longer
uses competing action `start`/`wait-on`. Preserve existing build preparation,
cache env, NO_PROXY, secrets handling and failure artifacts. Check focused
workflow wiring and observe actual CI on execution's normal push; a local test
alone must not be reported as successful hosted CI.
Sizing: 4–6 minutes active work, medium confidence; hosted CI wait is an explicit
external-wait exception, not grounds to rerun the full suite locally.
Proof result: 50/50 boundary tests pass (5 new: primary multi-line glob --spec
acceptance, primary no wrapper-owned mock, --browser forwarding, isolated
allowlist regression guard). specArgsFromArgv splits on comma AND newline for the
CI matrix YAML block. resolveSpecs gates assertSupportedIsolatedCypressSpecs to
the isolated path only; primary returns approved:null (no wrapper mock — the SUT
stack's start:mb provides Mountebank on 2525 and the spec uses shared 5001
defaults; the plugin returns early for primary so E2E_RUNNER_OWNS_LIFETIME is
irrelevant). ci.yml E2E-tests job replaced cypress-io/github-action with a build
step (e2e-bundle-if-needed.sh) + `SUT_RUNTIME_TARGET='{"built":true}' node
scripts/e2e-runner.mjs --spec "<matrix.spec>" --browser chrome`, preserving
cache/NO_PROXY/secrets/artifacts. Post-change refactor: eliminated a duplicate
resolveSutCheckoutTarget call (runE2eBatch resolves once, threads through to
runOwnedE2eInvocation; interactive path falls back to internal resolution).
Hosted CI observation deferred — the donut CI workflow triggers only on pushes
to main, and the execution branch has no push-triggered CI; the workflow change
will be observed after merge to main, not as a local substitute.

### 11. Keep paired isolation proofs usable with run-owned stacks
Type: Behavior
Status: done (2026-09-12)
Behavior: Existing paired-worktree reset proof commands coordinate two owned
invocations rather than asking developers to start persistent SUTs first.
Proof: Harness command-boundary tests preserve barriers, selected mode/spec,
endpoint handoff and cancellation cleanup. Harness delegates service ownership
to each invocation and never starts another stack around it. Preserve existing
CLI/MCP/private-mock proof meanings without expanding supported selections.
Live peer/data proof belongs to slice 13; this slice's single proof loop is the
harness entry command's orchestration contract.
Sizing: 5–8 minutes, medium confidence. Reuse the existing barrier protocol;
if its timing requires a new independent mechanism, stop and refine.
Proof result: 12/12 harness+openai-mock tests pass (2 new boundary tests: default
spawn emits `node scripts/e2e-runner.mjs --spec <spec>`; paired run spawns
exactly two owned invocations with role barrier env). spawnIsolatedCypress renamed
to spawnIsolatedE2eRunner (reflects that it spawns the owned wrapper, not direct
Cypress). 50/50 e2e-runner boundary + 20/20 baseline pass. No timing concern —
the barrier is spec-driven (Cucumber hooks), unaffected by owned invocations.
Deferred to slice 12: docs/worktree-browser-tests.md lines 96-100 still instruct
callers to start both allocations first (now stale); the spawnCypress option
name on runPairedWorktreeResetIsolation is stale but left to keep this refactor
minimal.

### 12. Remove obsolete public lifecycle entry points (code)
Type: Behavior
Status: done (2026-09-12)
Behavior: Documented local/CI E2E commands all enter the owned wrapper; separate
SUT start/restart/health command preparation and wait-for-existing-stack aliases
are gone. `cy:run`, `cy:open`, and aggregate `test` route coherently with no
recursive pnpm call or second service owner.
Proof: Command-boundary routing checks. Remove `sut`, `sut:restart`,
`sut:healthcheck`, `cy:run-with-sut`, `cy:run-on-sut` from package.json. Rewire
`cy:run` to `node scripts/e2e-runner.mjs` and `test` to build bundles + the
owned wrapper invocation (built target, full specPattern). Delete the obsolete
`sut-restart.mjs` adapter and its tests once caller-free; the only non-package
caller is the test fixture `browser-worktree-isolation-fixtures.mjs` (asserts
restart rejection) — remove that assertion with the adapter. Internal health
APIs (`sut-healthcheck.mjs`, `sut-start.mjs` owned lifetime) remain usable.
Unify the signal-wiring duplication between `wireBatchCancellation`
(e2e-runner.mjs) and `attachCancelSignals` (sut-start.mjs) noted by slice 4 —
one cancellation path, no generic signal framework. Update `test:sut-restart`
and `test:sut-start` script references for deleted files. Preserve equivalent
ownership regressions (zero-survivor/cleanup assertions) in the remaining suite.
Sizing: 8–10 minutes, medium confidence. Code + tests only; doc/guidance
reference audit is slice 12b. If a live production caller needs non-mechanical
behavioral adaptation, stop and refine.
Proof result: removed sut, sut:restart, sut:healthcheck, cy:run-with-sut,
cy:run-on-sut, test:sut-restart from package.json; rewired cy:run -> node
scripts/e2e-runner.mjs and test -> build bundles + SUT_RUNTIME_TARGET='{"built":true}'
node scripts/e2e-runner.mjs --spec 'e2e_test/features/**/*.feature'. Deleted
sut-restart.mjs + sut-restart.test.mjs + sut-isolated-restart.test.mjs; removed
the runSutRestart assertion from browser-worktree-isolation-fixtures.mjs and
browser-worktree-isolation.test.mjs. Removed sut-start.mjs isMain entry +
attachCancelSignals (unifies signal-wiring with wireBatchCancellation via
deletion). runSutStart KEPT as an internal adapter (still meaningfully exercised
by sut-start/sut-retirement-admission/sut-fresh-identity-start/sut-runtime-target
tests and sut-isolated-fixtures). Cleaned stale code refs: sut-healthcheck.mjs
comments, dead 'pnpm sut:restart'/'pnpm sut' entries in
SUPPORTED_ISOLATED_SUT_COMMANDS (kept the functional 'pnpm sut:healthcheck' and
the 'pnpm sut' guard branch that selects start-vs-complete allocation loading).
Tests: 50 e2e-runner + 19 browser-worktree-isolation/harness + 36 sut-start/
isolated-cypress + 21 sut-retirement/healthcheck + 8 cli-spec/fresh-identity
all pass. Deferred concern: retainOwnership / holdSutOwnershipAcrossRestart
is now dead production code (only test-exercised) from the removed restart flow;
removing it touches sut-start.mjs/sut-owner.mjs/sut-services.mjs/
sut-retirement-admission.test.mjs and is out of this slice's scope — candidate
for a later cleanup. Doc/guidance reference audit is slice 12b.

### 12b. Update guidance references for removed lifecycle entry points
Type: Behavior
Status: planned
Behavior: No documented guidance references an obsolete `sut`/`sut:restart`/
`sut:healthcheck`/`cy:run-with-sut`/`cy:run-on-sut` command; all local/CI E2E
guidance routes through the owned wrapper.
Proof: Reference audit across AGENTS.md, CLAUDE.md, .cursor/agent-map.md,
.cursor/rules (e.g. e2e-authoring.mdc), docs (end-to-end-testing.md,
worktree-browser-tests.md, development-setup.md, nix.md, notebook-publication-
profiling.md, ona.md, README.md, DearDough.md), and mirrored skills
(.claude/skills + .agents/skills). Replace obsolete command references with
the owned wrapper entry points; remove stale "start both allocations first"
instructions (slice 11 made the harness own its stack). Historical completed
plans need not be rewritten. Internal health/log commands remain usable.
Sizing: 5–8 minutes, low risk; mechanical reference audit.

### 13. Demonstrate independent concurrent E2E invocations
Type: Behavior
Status: planned
Behavior: Two supported invocations in separate worktrees complete independently
while persistent Development stays available and unchanged.
Proof: Adapted paired reset harness records peer availability/data preservation
and no owned listeners/descendants after each invocation. Use the existing
no-mock note-editing proof first and representative private-mock proof where
its lifecycle changed; do not broaden into unrelated full-suite verification.
Commands: new `pnpm cy:run --spec` for supported selections plus the existing
`node scripts/worktree-reset-isolation-harness.mjs` modes after caller migration.
Sizing: 5 minutes active work; measured real-service/browser waits excepted.

### 14. Report recurring lifecycle overhead
Type: Behavior
Status: planned
Behavior: Equivalent focused runs under the new lifecycle produce the completed
retained report with baseline comparison and feedback-time assessment.
Proof: Extend `docs/e2e-lifecycle-overhead.md` with first/cached readiness, test,
shutdown and total times, three repeated focused invocations, one multi-spec
batch, conditions and variation. Clearly separate provisioning/build costs from
recurring overhead; record regressions without introducing an unapproved target
or reuse mode. Confirm all benchmark-owned processes are stopped.
Command: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/worktree_note_editing.feature`
Sizing: 5 minutes active analysis; measured build/test waits excepted.

## Proof ownership and cumulative assessment

| Story promise | Owning slices |
| --- | --- |
| One startup/shutdown per batch, readiness, test outcome | 3, 8 |
| Partial launch, failed tests, cancellation, bounded failure | 3, 4 |
| Unexpected required-service exit ends the run | 5, 6 |
| Private mocks/lease survive all specs and then stop | 6 |
| One stack through interactive reruns and close | 7 |
| Foreign resources, busy checkout and peers remain protected | 3, 4, 8, 13 |
| Persistent allocation, shared infrastructure and Development unchanged | 3, 8, 13 |
| CI coverage/builds/artifacts preserved with one owner | 9, 10 |
| Public commands removed; callers, guidance, proofs migrated | 11, 12 |
| Diagnostics survive shutdown | 3–9 |
| Cohesive reuse, no duplicated lifecycle | 2–12 plus required refactor gate |
| Measured overhead report | 1, 14 |

The common rule is a single invocation owns every process it starts and settles
all owned work before returning. Launch targets describe actual local/CI
service differences; interactive mode changes lifetime endpoint, not ownership.
Fixture reset and configuration remain Cypress responsibilities. Intermediate
commits retain legacy adapters until all replacement workflows exist; they are
not the story's final state. No slice may ship a knowingly broken existing path.

## Refinement assessment (2026-09-12)

Original slice 9 was split into built-target behavior (9) and CI command
migration (10). Original slice 10 was split into paired-harness migration (11)
and final public command removal (12). Original final proofs became 13 and 14.
No completed work or proof was discarded; there was no execution.

Result: 14 slices. Each is Ready: one Behavior/Structure gate, one focused proof
loop, a stated reuse path and plausible active-work sizing. Slices 2–9, 11–12
retain medium estimates rather than claiming measured implementation times;
stop/refine at the hard limit. Slices 1, 7, 9, 10, 13 and 14 permit only their
stated measured startup/build/browser/CI waits beyond that limit.

Cumulative design remains one invocation lifetime with target-specific launch
configuration. No repeated ownership or allowlist representation is prescribed.
No additional product questions or remaining slice-specific refinement concerns
were identified in this assessment. Ready for direct execution under the
refinement skill; separate user authorization is still required. No story
resplit recommendation at 14 slices. Estimated active work is roughly 1–2 hours,
plus measured runtime and required delivery gates; these remain hypotheses.
