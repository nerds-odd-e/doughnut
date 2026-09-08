# Refuse unverified isolated browser allocations

## Source, goal, and scope

[SEED-015 story 2a](../../seeds/SEED-015-concurrent-worktree-environments.md#story-2a),
correcting the allocation/health promises of delivered story 2 (quick 061).
Status: ready for execution continuation after story and slice refinement.
The pre-existing local `in-progress` marker on slice 1 is preserved; this
planning pass supplies no implementation or completion evidence.

Developers and AI tasks must not receive a successful isolated browser check
against unrelated listeners or a silently replaced invalid allocation.
Retain normal first-use setup, valid recorded allocation reuse, and primary
defaults. No new mock/client support, retirement, automatic repair, copied-ID
recovery, or ownership-framework redesign. Follow ADR 0006's visible failures.

## Review provenance

Original plan: `8fcd829945`; execution-ready 13-leaf refinement: `822ac5d37b`.
Completion: all 13 leaves done at `ef7044e07c`. The five-to-thirteen refinement
kept the same story; temporary manual setup was replaced by automatic setup.
Aggregate product boundary: `f161df1416^..ef7044e07c`. Included commits:

| Commit | Reason |
| --- | --- |
| `f161df1416` | Early isolated-command refusal |
| `0ace5cc22f` | Runtime target propagation |
| `7bd849df42` | Configured isolated SUT start |
| `e7d8a54463` | Failed/cancelled startup cleanup |
| `291b32de75` | Background child-exit cleanup |
| `26af83b61e` | Owning Cypress origin and lease |
| `91ccc12652` | Paired fixture-reset proof |
| `41a87312a2` | Idle owner restart |
| `5461a860ed` | Shared identity initialization |
| `85395b2317` | E2E database first use |
| `2220b78604` | Coordinated endpoint claims |
| `87b6901c50` | Automatic port allocation |
| `ef7044e07c` | SUT-first identity and completion |

Intervening `ac20c551a5` only records unrelated CI provenance; excluded from
product findings. Later notebook-sync work, merge, and backlog wording are not
part of the execution diff. Current affected scripts are unchanged since the
review boundary. No process retrospective was performed, per user request.

## Findings and focused evidence

1. **P1: Owning health does not establish listener ownership.**
   `runSutHealthcheck` verifies the owner control socket, then accepts ordinary
   TCP/HTTP success. A listener acquired after the pre-start occupancy check,
   or while an owned child is failing, can satisfy health without belonging to
   the stack. Cypress relies on this result before destructive fixture reset.
   The existing foreign-listener test only covers an absent owner.
2. **P2: Invalid recorded allocation is silently replaced.**
   `ensureIsolatedE2eDatabase` treats a present invalid database as absent;
   `refusePartialIsolatedE2ePorts` accepts all-invalid ports as all-missing.
   Start then provisions and rewrites rather than refusing. The parser also
   needs to reject recorded ports outside the legal range or duplicated among
   services; these cannot describe three distinct application endpoints.

Disposable probes through `CURSOR_DEV=true nix develop -c node --input-type=module`
on 2026-09-08 observed:

- Real `startLiveOwner` control socket, two unrelated TCP listeners and an
  unrelated HTTP-200 LB stand-in, no application children: ordinary
  `runSutHealthcheck({checkoutRoot})` returned `ok: true`.
- Config `{id: 'wt_retro', e2e: {database: 'invalid-name', backendPort: 'bad',
  vitePort: 'bad', lbListenPort: 'bad'}}`: `runConfiguredStart` returned 0,
  invoked fake MySQL once, spawned once, and replaced the database/three ports.
  Only MySQL/spawn were faked; temporary files/listeners were cleaned up.

No browser suite was rerun in this retrospective. Original paired-run evidence
is preserved in Git; its shared E2E note count changed from 3 to 4 from reported
concurrent primary use, so it is not a clean unchanged-count observation.
No separate consequential refactor finding survived filtering; the barrier
and single-spec allowlist still serve the supported workflow.

## Current decisions and execution context

Use the refined story's five key examples without enlarging its boundary.
No open product questions. Follow [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md):
refuse visibly; do not repair or fall back.

- `startOwnedSutSupervisor` already owns a detached application child group.
  Expose that live group's identity through the existing authenticated owner
  control response; do not derive ownership from HTTP success, port claims,
  the supervisor PID alone, or an unrelated PID file. Retain lifecycle ownership.
- Compare the recorded endpoints' listener PIDs with that group using the local
  process tools. `getListenerPids` in `sut-restart.mjs` supplies existing `lsof`
  behavior; move a shared primitive only if needed to avoid the existing
  restart → start → health import cycle. No generic process manager.
- Validate present allocation values before `runSutStart` provisions its E2E
  database or publishes ports. Only omitted fields are first use. A present
  `e2e` must be a non-null, non-array object; a present database uses the existing
  name rule. Ports are either all omitted or three distinct integers 1–65535.
  Reuse these rules in start and complete-allocation readers (health, restart,
  Cypress). No new database naming policy or port reservation policy.
- Existing valid first-use/reuse, primary defaults, runner leases, and owned
  cleanup remain regression constraints. Listener checks occur at verification
  time; continuous monitoring or malicious local replacement is excluded.

## Ordered slices

### 1. Expose the live application group to owning health

Type: Structure
Status: in-progress
Change: Extend the existing owner-control response with the application group
identity from the supervisor's current child, without changing health decisions,
lease semantics, shutdown, or process ownership. Supply no group before the
child exists. Enables immediately following Behavior 2.
Proof: Through the existing supervisor/control boundary, observe the actual
spawned child's group identity. Keep existing supervisor child-exit and shutdown
observations green; no helper-only mock of ownership establishes this contract.
Focused command:
`CURSOR_DEV=true nix develop -c node --test scripts/sut-services.test.mjs scripts/sut-services-child-exit.test.mjs scripts/sut-isolated-restart.test.mjs`
Sizing: about 5 minutes, medium confidence; one additive control-response path,
including regression proof and cleanup. No lifecycle redesign is permitted.

### 2. Report foreign application listeners as unhealthy

Type: Behavior
Status: planned
Behavior: A live control owner exists but a recorded application listener is
outside its application group or ownership cannot be established → ordinary
`runSutHealthcheck` → unhealthy, regardless of TCP/HTTP readiness.
Proof: Extend `sut-isolated-start.test.mjs` with real disposable child listeners
and the live owner/control boundary: separately owned ready listeners fail;
listeners in the actual application group pass when ready. Exercise each of the
three recorded endpoints as the foreign endpoint through one parameterized
behavior test. A control-only owner fails; foreign listeners remain alive.
Replace health fixtures that equate live control with application ownership.
Focused command:
`CURSOR_DEV=true nix develop -c node --test scripts/sut-isolated-start.test.mjs scripts/sut-healthcheck.test.mjs`
Scope: Consume slice 1's identity and existing listener discovery, checking the
recorded endpoints before reporting success. Keep primary defaults unchanged.
Sizing: about 5 minutes, medium confidence; one health-result proof loop using
real process groups. If platform PID/group lookup does not converge, record the
specific issue and refine this leaf at the time limit instead of adding a manager.

### 3. Refuse browser verification before reset on foreign endpoints

Type: Behavior
Status: planned
Behavior: The supported isolated spec targets an allocation with a live owner
but foreign ready listeners → `guardCypressNodeSetup` → refuse before fixture
reset. A healthy owned allocation still passes setup.
Proof: Extend `isolated-cypress.test.mjs` through real `runSutHealthcheck`, using
slice 2's process fixtures. Observe no reset hook reached and the foreign
listener still alive; retain the healthy setup/lease behavior. Reuse the
existing startup health-wait tests to confirm unhealthy results cannot produce
successful start readiness. Extend the wait boundary only if its existing
observations do not cover that refusal.
Focused command:
`CURSOR_DEV=true nix develop -c node --test scripts/isolated-cypress.test.mjs scripts/sut-start-health-wait.test.mjs`
Scope: Existing health consumers should need no new policy; this proof closes
the user-visible path. Do not expand the supported spec allowlist.
Sizing: about 5 minutes, medium confidence; one consumer refusal proof loop,
reusing the preceding real-listener fixtures.

### 4. Refuse present invalid database allocation

Type: Behavior
Status: planned
Behavior: The saved `e2e` container or database value is present but invalid →
ordinary SUT start → visible refusal before provisioning or configuration changes.
Proof: Extend `sut-isolated-e2e-database.test.mjs` through `runSutStart` with null,
wrongly typed, empty, and malformed database values, plus invalid `e2e` shapes.
Observe unchanged config and no MySQL, port-claim, or launch side effects.
Use temporary config/claim directories and the existing fake external MySQL
boundary. Retain absent-field first use, valid reuse, and recorded-missing-DB
refusal. Shared allocation-reader cases verify invalid values also refuse in
health/restart/Cypress without requiring application launch.
Focused command:
`CURSOR_DEV=true nix develop -c node --test scripts/sut-isolated-e2e-database.test.mjs scripts/browser-worktree-isolation.test.mjs`
Scope: Presence validation at the shared pre-provisioning boundary; no name
policy change, repair, or storage experiment.
Sizing: about 5 minutes, medium confidence; one early validation/refusal path.

### 5. Refuse present invalid application-port allocation

Type: Behavior
Status: planned
Behavior: Recorded application ports are partial or invalid → ordinary SUT
start → refusal before allocating a missing database or publishing any ports.
Proof: Extend `sut-e2e-port-allocation.test.mjs` through start with all-invalid,
null, fractional, nonpositive, >65535, duplicated, and partial port shapes.
Observe unchanged config/claims and no MySQL/spawn. Existing all-omitted first
use and valid reuse stay green. Complete-allocation reader cases establish the
same rejection policy for health/restart/Cypress; do not duplicate validator logic.
Focused command:
`CURSOR_DEV=true nix develop -c node --test scripts/sut-e2e-port-allocation.test.mjs scripts/browser-worktree-isolation.test.mjs`
Scope: One port-validation rule before first-use mutation. No renumbering or
new service-port categories.
Sizing: about 5 minutes, medium confidence; one parameterized refusal proof loop.

## Promise ownership and proof

| Contract promise | Owning leaf | Observation |
| --- | --- | --- |
| Application group comes from the live owner; lifecycle unchanged | 1 | Supervisor/control identity; existing child-exit cleanup and restart proof |
| Recorded backend, Vite and LB belong to that group; foreign/unverifiable is unhealthy | 2 | Real owned/foreign listener cases, including control-only owner |
| Valid owning health and primary defaults remain usable | 2 | Positive owning case and primary health regressions |
| Cypress refuses before reset; startup cannot report foreign endpoints ready | 3 | Real-health setup refusal and health-wait result |
| Refusal leaves foreign listeners running | 2, 3 | Listener still accepts connections after refusal |
| Invalid container/database refuses without mutations; absent/valid fields work | 4 | Start side effects and existing first-use/reuse/missing-DB cases |
| Partial/invalid ports refuse before any allocation; omitted/valid ports work | 5 | Config/claims unchanged, no MySQL/spawn; first-use/reuse cases |
| Supported allocation readers agree on recorded validity | 4, 5 | Shared reader rejection cases |

## Refinement result and resume notes

2026-09-08: Refined the existing plan in place after refining the home story.
The old first leaf changed public health behavior despite its Structure label;
replace it with Structure 1 immediately followed by Behavior 2. Old Cypress
leaf 2 becomes 3; old database/port leaves 3–4 become 4–5 with explicit
presence rules and proof ownership. All five leaves are Ready planning
hypotheses; the existing slice-1 in-progress marker is preserved, not evidence
that the new Structure is already implemented. No completed-slice evidence
was supplied or invalidated by this planning pass.

2026-09-08 execute-plan reconcile: An earlier attempt implemented health via
`sut.pid` listener pgid checks (conflicts with Current decisions: do not
derive ownership from an unrelated PID file; Structure 1 must not change
health). That attempt-owned script WIP was reverted. Planning artifacts
(this PLAN + SEED-015 story 2a) kept. Resume Structure 1 as specified here.

Ready for direct execution/continuation. Before continuing slice 1, reconcile
any attempt-owned WIP with this boundary; do not discard another task's work.
No product implementation, tests, commit, or push performed in this pass.
Each leaf has one cohesive proof loop, with about-five-minute sizing including
focused verification and local cleanup. There are no pre-authorized sizing
exceptions: scrutinize at five minutes and stop/refine at ten unless one
focused test or external wait supplies a recorded exception. Execution still
requires the repository's normal per-leaf wrap-up. No full browser suite,
manual test, new environment, or database experiment is needed for this plan.
