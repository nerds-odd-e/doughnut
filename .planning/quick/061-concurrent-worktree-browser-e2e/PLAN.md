# Concurrent browser verification in local worktrees

## Source

[SEED-015, story 2](../../seeds/SEED-015-concurrent-worktree-environments.md#story-2)
— Run browser E2E scenarios concurrently without external-service mocks.

Status: planned; refined into 13 ordered leaves, ready for execution.
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
  browser code. `support/e2eAppUrl.ts` already reads Cypress baseUrl and
  `start/clientConfig.ts` uses relative generated-client requests. Verify
  pre-navigation reset resolution; leave unused CLI/MCP constants alone.
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

## Refinement assessment

All five original leaves are unstarted; no runtime evidence or execution overrun
exists to preserve. Original leaves 1–5 are **Refine**: identity extraction is
premature before the configured workflow, startup and Cypress contain several
beats, restart mixes ownership checks with real-stack verification, and failure
handling mixes startup cancellation with post-readiness failure.

Keep the same story. Deliver a temporarily operator-configured environment
first, then remove database/port/identity setup in leaves 10–13. This is an
interim usable workflow, not a new requirement or a reduced final promise.
Until a command is supported, isolated mode refuses it before side effects.
Primary defaults and CI continue to work throughout.

No story escalation is indicated by inspection: the original L hypothesis
remains plausible for these bounded leaves. Execution must reassess it if the
actual integration work invalidates that hypothesis.

## Outside-in proof and promise ownership

| Contract promise | Owner | Observable proof |
|---|---|---|
| Primary defaults and CI remain usable; configured primary isolates | 1, 3, 6 | Command/config boundary regression cases select legacy defaults only in the documented contexts |
| Invalid or incomplete isolation never falls back | 1; enabled paths in 3, 6, 10–13 | Refusal before spawn/reset/provisioning against a foreign target |
| Backend identity/allocation semantics remain compatible | 9, 13 | Existing backend launcher, lock, provisioning and ordinary-command cases remain green; SUT-first then backend uses the same ID |
| Owning application uses its allocated database and three ports | 2–3 | Real configured SUT serves its injected marker through its proxy and reports its database/origin |
| No Mountebank dependency | 3, 6 | Supported start/spec works with no mock listener; no mock process is launched |
| Foreign listeners are never adopted or terminated; duplicate SUT refused | 3 | Occupied-port and concurrent-start command cases leave the foreign listener and first owner alive |
| Health belongs to the live owning stack | 3, 5 | Foreign healthy listener cannot satisfy owning health; exited owner is unhealthy |
| Startup timeout/cancellation releases owned processes | 4 | Actual child processes disappear; foreign process survives |
| Background failure is observed without another command | 5 | Supervisor logs forced child exit when observed and releases owned peers while its lifecycle is exercised |
| Browser creates, edits, reloads its own note | 6 | Ordinary focused Cypress feature asserts saved content after reload |
| Unsupported/mixed specs refuse before reset/mock/client setup | 1, 6 | No reset or tagged setup side effects on rejected selection |
| Cypress run ownership and target consistency | 6 | Second runner/conflicting origin refuses before reset; completion/cancellation permits later run |
| A reset preserves B and vice versa | 7 | Barrier-controlled overlapping browser runs read the peer marker after each reset; both edits finish |
| Shared E2E and unit-test data remain unchanged | 7 | Read-only observations of existing sentinels before/after the paired run |
| Only idle owning SUT can restart | 8 | A recovers; B keeps its process identity and marker; active Cypress/unverifiable owner refuses |
| Existing identity gains an E2E database without changing unit-test state | 9–10 | First setup launches app on new E2E DB; existing unit database is untouched |
| First allocation never adopts an existing database | 10 | Name collision fails before config completion or app launch |
| Endpoint allocation is distinct across concurrent worktrees | 11–12 | Concurrent ordinary starts persist different claims and both apps bind their recorded endpoints |
| Fresh SUT-first worktree obtains the canonical identity | 13 | SUT then backend command reuses one ID; overlapping initialization publishes one complete config |
| Persisted settings survive shells/restarts; missing DB/migration errors fail | 3, 10–13 | Second invocation reuses values; recorded missing DB is not recreated and migration failure does not become healthy |
| Conflicting overrides are rejected, matching ones remain usable | 3, 6 | SUT target/origin cases refuse before mutation; matching settings use the assigned target |
| Route semantics and repeatable instructions | 2, 6, 8, 10–13 | Existing named routes/proxy tests remain green; guide evolves with each delivered ordinary command |

## Ordered slices

Every leaf includes its implementation, focused proof, and local cleanup.
Numbers below replace the original leaves; no completed evidence is discarded.

### 1. Refuse unsupported isolated commands before shared-state effects

Type: Behavior
Status: planned
Proof: Command-boundary cases for linked, configured-primary, unconfigured-
primary and CI contexts. Observe refusal before service spawn, listener signals,
or Cypress reset; existing primary/CI commands retain their defaults.

Behavior: A local isolated checkout has no supported browser environment yet →
start, health, restart or ordinary Cypress run → actionable refusal instead of
contacting shared defaults.

Scope: One common isolation applicability/validation gate wired at the four
command boundaries. Do not allocate, spawn, or parse every shell alias. Malformed
local JSON fails clearly. Initially refuse all isolated runs; leaves 3, 6 and 8
replace that refusal for their supported cases. This creates a safe stopping
point before wiring new runtime behavior.

Sizing: ~5 minutes, medium confidence; one command-selection matrix.

### 2. Feed one runtime target through the existing application launcher

Type: Structure
Status: planned
Proof: Existing SUT service/start/health and proxy routing tests remain green
with legacy settings. Exercise the launcher boundary with one explicit target
fixture; no test-only exports of individual configuration helpers.

Internal change: Pass one resolved runtime target through backend environment,
Vite port/proxy, local-LB upstream/listen settings, and readiness. Remove the
package-script literal that overrides the selected Vite upstream. Keep the
legacy target as the current default. No allocation, ownership redesign, or
Cypress changes in this leaf; isolated command gate stays closed.

Immediate next Behavior: Launch an already configured isolated app (leaf 3).
This is parameter plumbing through existing seams, not a new environment
manager. It does not change route paths or generated APIs.

Sizing: ~5 minutes, medium confidence; one launcher regression loop. Restrict
changes to target propagation; do not absorb leaf 3.

### 3. Start an already configured isolated application

Type: Behavior
Status: planned
Proof: Start/health command-boundary tests and one real configured SUT smoke
case: inject/read a unique marker through its proxy, observe selected database
and origin, then start again from a fresh shell using the same allocation.

Behavior: A checkout has a valid identity, deliberately provisioned E2E database
and recorded free application ports → ordinary `pnpm sut` → its own healthy
application starts without Mountebank.

Scope: Consume the runtime target from leaf 2. Add a separate atomic SUT
ownership claim and a live supervisor identity. Refuse duplicate starts,
foreign port occupancy, mismatched overrides, missing DB, and invalid migration
before reporting health. Verify own binds as well as readiness; a foreign
ready endpoint is not success. Do not recreate a recorded DB or renumber ports.
Use the existing run-p process tree; no alternate process manager.

Ownership implementation boundary: bind ownership to the living supervisor,
not a stored PID alone. A checkout-local control endpoint with a per-run token
can verify that owner; later restart must ask that live owner to stop its own
children. Keep isolated restart and Cypress refused for now. Document the
temporary manual allocation in `docs/worktree-browser-tests.md`; leaves 10–13
remove it. Failure remains visible; cleanup improvements follow immediately.

Sizing: ~5–8 minutes, medium confidence; one configured-start proof loop.
Real JVM startup duration is a focused-test exception. If implementing live
ownership requires a separate framework, stop and refine instead.

### 4. Release a startup that is cancelled or cannot become ready

Type: Behavior
Status: planned
Proof: Start the real launcher with disposable service child processes. Trigger
timeout/cancellation while waiting for readiness and observe owned children
exit and the ownership claim release; an unrelated process remains alive.

Behavior: An owning SUT startup has spawned children but is not ready →
cancellation or readiness failure → the startup exits with diagnostics and
leaves no owned service processes.

Scope: One startup-finalization path for timeout, early exit and signal
cancellation. Signal only the owned child tree and await termination; never
discover ownership by port. Reuse the start seam and owner from leaf 3.
No database or port-allocation retirement.

Sizing: ~5 minutes, medium confidence; one real-process startup-failure loop.

### 5. Release the running stack when a service fails

Type: Behavior
Status: planned
Proof: Keep the real supervisor running, force a disposable application child
to exit after readiness, and observe the failure log and peer exit without
issuing another command. The owning health check then fails; foreign peers live.

Behavior: The SUT has become healthy → an application child exits unexpectedly
→ the running owner reports failure on exit observation and releases its peers.

Scope: Reuse leaf 4 child-tree cleanup from the supervisor's post-readiness exit
path. Verify actual run-p propagation, not a fake EventEmitter alone. No retry,
new allocation, or automatic recovery. If the existing supervisor already
delivers this, retain boundary evidence and make only necessary changes.

Sizing: ~5 minutes, medium confidence; one supervisor failure loop.

### 6. Run the supported note edit through the owning Cypress environment

Type: Behavior
Status: planned
Proof: Ordinary `pnpm cypress run --spec
e2e_test/features/note_creation_and_update/worktree_note_editing.feature`
creates a note, edits it, and observes saved content after reload. Command
boundary variations establish rejection before reset for a conflicting origin,
unsupported/mixed spec selection, or a duplicate runner.

Behavior: An owning SUT is healthy → the focused ordinary Cypress command →
the browser completes a saved note edit against that same environment.

Scope: Replace the blanket gate from leaf 1 only for this exact supported spec.
Set the browser origin in Cypress node configuration; use the existing
`e2eAppBaseUrl()` and relative generated-client requests where they already
follow it. Verify the reset request before the first SPA visit too. Do not
rewrite unused CLI/MCP endpoint consumers.

Acquire one runner lease before hooks and release on run completion/cancellation;
serialize lease acquisition with owner shutdown so later restart cannot race a
new runner. Require the verified owning SUT. Fail the whole mixed selection,
rather than silently skipping specs. Reuse existing page objects in the focused
feature, without an orchestration harness yet. Update the guide and local-origin
guidance with the working command. Primary/CI selection remains unchanged.

Sizing: ~5–8 minutes, medium confidence; one focused feature loop. Cypress
runtime alone may exceed ten minutes and must be recorded separately.

### 7. Keep the peer's note when the other browser resets fixtures

Type: Behavior
Status: planned
Proof: Run the focused browser scenario in two configured real worktrees with
a barrier at reset. B seeds its marker before A resets; B reads and edits it
afterwards. Repeat with roles exchanged. Both browsers reload saved content.
Record read-only before/after values for existing shared/unit-test sentinels.

Behavior: Both worktrees have independently saved notes → one runner resets
its scenario fixtures during the other's run → the peer retains and edits its
note successfully.

Scope: Add only the bounded paired-run coordination needed for this observation,
reusing leaf 6's feature and API/page objects. Use a temporary harness-local
barrier to establish ordering, not arbitrary sleeps or a reusable scheduler.
Extend environment wiring only if this proof exposes leakage within the promised
note path. Record literal commands, targets and observations in this PLAN.
Independent passing runs are not proof of reset isolation.

Sizing: ~5 minutes of harness/assertion work, medium confidence; one paired-run
proof loop. Two JVM startups and Cypress runtime are explicit runtime exceptions.
A new shared-store dependency changes the story assumption: stop for review.

### 8. Restart only the idle live owner

Type: Behavior
Status: planned
Proof: Drive restart against disposable live owners: idle A restarts, busy A or
an unverifiable owner refuses before signals. Reuse the paired fixture to
observe B retain its original processes and marker while A returns healthy.

Behavior: A has a verified live owner and no Cypress lease → ordinary restart
in A → only A's app is replaced on its existing allocation.

Scope: Replace isolated restart refusal from leaf 1. Ask the verified supervisor
from leaf 3 to stop its own children, using the cleanup from leaves 4–5; hold
the lifecycle claim across stop/start to exclude a new runner or second restart.
Do not send signals to a PID discovered from a file/port. An unverified stale
owner remains a visible refusal. Add restart instructions to the guide.

Sizing: ~5 minutes, medium confidence; one restart proof loop. Reuse existing
process fixtures and paired stack; JVM startup is a runtime exception.

### 9. Separate identity initialization from backend-test lifetime ownership

Type: Structure
Status: planned
Proof: Existing `pnpm test:backend-test-worktree` command-boundary coverage
remains green: original and configured identities, provisioning failure, stale
lock reclaim, concurrent first use, and ordinary-command compatibility.

Internal change: Extract only canonical identity/config initialization needed
by leaf 10, with a brief shared initialization lock. Preserve backend-test run
locking and old identity-only JSON. The initializer returns identity to its
caller; it does not hold the backend run lock for SUT lifetime or interpret SUT
datasource overrides as backend-test overrides.

Immediate next Behavior: Add the E2E database to an existing identity (leaf 10).
Do not create a general registry or port allocator here.

Sizing: ~5 minutes, medium confidence; one existing command-regression loop.
Move only the identity initialization seam, not the complete backend launcher.

### 10. Prepare the E2E database on first SUT use of an existing identity

Type: Behavior
Status: planned
Proof: Ordinary SUT start with an existing identity and selected ports but no
E2E database allocates its new DB, migrates and serves its marker. Boundary
cases observe collision/preparation failure without adopting existing data;
two same-checkout starts publish one complete allocation.

Behavior: A configured identity has no recorded completed E2E database setup →
`pnpm sut` → a new identity-derived database is prepared and the app starts.

Scope: Reuse shared identity initialization from leaf 9; provision E2E under
initialization ownership and record success only after provisioning. Keep unit
DB state untouched. A pre-existing unrecorded database name is a collision;
a missing previously recorded database is an error. No IF-NOT-EXISTS adoption
or destructive repair. Ports are still explicitly configured at this interim
boundary. Remove manual database setup from the guide.

Sizing: ~5 minutes, medium confidence; one first-use database proof loop.
Reuse proven MySQL CREATE/GRANT behavior, not a new storage experiment.

### 11. Centralize port claims for the immediately following first-use path

Type: Structure
Status: planned
Proof: Existing configured start/conflict command cases from leaf 3 remain
green through the same launcher. No automatic port selection is exposed yet.

Internal change: Put recorded port validation and claim publication behind one
bounded allocator seam, with machine-local serialization for cooperating
worktrees. Preserve explicitly configured ports and visible conflict refusal.
Claiming metadata never confers ownership of a listener. No cleanup UI,
reclamation policy, development ports, or external mock ports.

Immediate next Behavior: Start an identity-only checkout without manual port
selection (leaf 12). Use one atomic claim mechanism for the three application
ports; do not create independently allocating service loaders.

Sizing: ~5 minutes, medium confidence; one configured-start regression loop.

### 12. Start an existing identity without choosing ports

Type: Behavior
Status: planned
Proof: Two identity-only worktrees invoke ordinary SUT start concurrently;
their persisted port claims differ, both bind those endpoints, and both serve
their own markers. A later shell invocation reuses its claim.

Behavior: The identity exists but has no E2E endpoint allocation →
`pnpm sut` → application ports are allocated automatically and the app starts.

Scope: Enable the allocator from leaf 11 and compose the first-use database
path from leaf 10. Publish only a complete E2E allocation; retain collision
refusal and real bind validation. Interruption may leave owned orphan resources,
but never silently adopts another worktree's claim. Reuse one checkout's
initialization ownership for competing starts. Remove manual ports from the
guide; preserve already configured allocations.

Sizing: ~5 minutes, medium confidence; one concurrent-first-use loop. Real
dual-stack boot time is an explicit runtime exception.

### 13. Start a fresh worktree before any backend-test invocation

Type: Behavior
Status: planned
Proof: Ordinary SUT start in a fresh linked worktree creates one canonical ID
and completes the same focused browser edit. A subsequent ordinary backend-test
invocation uses that ID. A synchronized SUT/backend first-use boundary case
shows one complete identity, with distinct purpose databases.

Behavior: A linked worktree has no local configuration → `pnpm sut` →
the identity and E2E allocation are prepared without operator setup.

Scope: Compose leaf 9's shared initializer with leaf 12; retain the delivered
backend-first path and configured-primary behavior. Do not launch backend tests
from SUT or hold their lifetime lock. Shared initialization cannot publish a
partial or competing identity. Remove the final manual-identity prerequisite
from the guide and rerun the paired browser workflow through automatic setup.

Sizing: ~5 minutes, medium confidence; one fresh-first-use composition loop.
Existing backend regression suite and real stack runtime may dominate elapsed
time; record those focused-test exceptions, not an implementation exemption.

## Readiness and stopping points

Ready for execution as a sizing hypothesis. Leaves 3 and 6 target 5–8 minutes
because they connect existing seams; all other leaves target about five minutes.
At five minutes inspect for hidden work; at ten minutes of implementation
stop/refine unless a concrete exception was recorded. Do not count this estimate
as an execution-time guarantee.

The safe interim states are explicit: leaf 1 refuses isolated use; leaf 3
supports manually configured startup; leaf 6 supports the focused browser run;
leaf 8 supports owned restart; leaves 10, 12 and 13 remove manual database,
ports and identity setup respectively. Unsupported commands keep their earlier
refusal until enabled. Final scope and proof promises remain unchanged.

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
Refinement also found reusable Cypress origin handling and a supervisor that
already delegates service-tree handling to run-p. Use real-process evidence to
check its failure propagation; do not assume EventEmitter-only tests prove it.
No implementation or runtime verification has been performed for this plan.
