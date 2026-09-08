# Retire worktree databases before removing the checkout

Source: [SEED-015 story 6](../../seeds/SEED-015-concurrent-worktree-environments.md#story-6).
Status: in progress; story scope accepted 2026-09-08.

## Goal and scope

Explicitly reclaim one idle linked worktree's disposable unit-test and allocated
E2E databases while its checkout and identity still exist. Protect running work,
peer data, primary checkouts, and persistent development data. Leave the checkout
in place for the developer to remove afterward.

No automatic cleanup, database inventory, recovery after checkout deletion,
database reuse, process termination, port-claim cleanup, hooks, copied/moved
checkout recovery, Cloud VM/CI changes, or development-profile cleanup.

## Current decisions and execution context

- Command: `pnpm worktree:retire --check` reports the current linked checkout's
  exact targets and refusals without mutation; `pnpm worktree:retire` explicitly
  retires them. No arbitrary database/path argument, force mode, or interactive
  confirmation. Run from the checkout being retired, through local Nix.
- Identity: reuse `scripts/worktree-identity.mjs` and `.worktree.local.json`.
  Unit target is `doughnut_<id>_test`. An allocated E2E target must equal
  `doughnut_e2e_<id>`; reject custom targets rather than guessing ownership.
  Validate the existing Git linked-worktree relationship. Refuse a duplicate
  identity in another registered checkout; do not repair or scan MySQL by prefix.
- Absence of a live parent or a healthy owner is not proof of idleness.
  Existing `.worktree.local.lock`, `.sut.local.lock`, startup ownership, and
  Cypress leases identify busy or uncertain state. Retirement must not use
  their permissive stale-lock reclamation paths. Inspect recorded listeners
  and MySQL sessions as additional vetoes, never as ownership authorization.
- Detect surviving supported backend JVMs using checkout working-directory /
  command-line evidence as well as listeners and database sessions. Include a
  disconnected orphan in proof: no MySQL session alone cannot establish idle.
  Inspection failure or ambiguous relevant process evidence refuses cleanup;
  no new process supervisor or continuous tracking. Exclude the retirement
  invocation and its ancestors from checkout-process checks, not other runners.
- A short checkout-local admission gate serializes retirement with the start
  of backend tests/migrations and SUT startup/restart. Runners check retirement
  state and establish their existing ownership before releasing that gate;
  retirement holds it across final validation, marking, and deletion. Preserve
  supported simultaneous SUT + Cypress and backend verification. Cypress already
  requires a live SUT owner, so that owner prevents retirement throughout its lease.
- Keep identity and write a durable retirement marker before the first DROP.
  Supported starts refuse that marker, including after a failed/interrupted
  drop, instead of recreating databases. Retry retirement of the same identity
  to complete missing drops; never clear the marker automatically. An abandoned
  admission gate refuses visibly; automatic crash repair/unretirement is excluded.
- MySQL DROP DATABASE is not an atomic pair. Report partial progress honestly;
  retain the marker and exact targets on failure. No rollback or compensation.
  Rechecking a completed retirement is harmless. Missing allocated schemas count
  as already absent; never provision during retirement.
- Relevant Accepted constraint: [ADR 0006 — Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md),
  Usage: propagate failures, add useful context, and test deliberate refusals.
  No ADR conflict or new architecture decision is required.
- Existing code: `backend-test-worktree-owner.sh::backend_test_worktree_prepare`
  recreates missing unit schemas, so deleting config or databases alone is unsafe.
  `sut-start.mjs::runSutStart` provisions before `claimSutOwnership`; admission
  must start before provisioning. `sut-restart.mjs` retains ownership across
  restart. `sut-owner-control.mjs` owns Cypress leases. Port claims are unrelated.
- Use existing Node command-boundary fixtures from
  `backend-test-worktree-linked-fixtures.mjs`, `sut-isolated-fixtures.mjs`, and
  MySQL stand-ins. Do not introduce a new test framework or backend/API changes.

## Outside-in proof

| Promise | Owner | Observable proof |
|---|---|---|
| Existing linked identity determines exact disposable targets; primary, absent, invalid, duplicate, and custom targets refuse | 1 | Public command output and no mutating MySQL call |
| Busy/uncertain allocation and surviving children refuse without interruption | 2a, 2b | Command refusal; child/peer still running; no DROP |
| Starts cannot race retirement or recreate retired data; ordinary concurrency survives | 3 → 4 | Barrier-controlled public runner invocations; marker refusal before provisioning/launch |
| Idle unit-test-only allocation is reclaimed and peer data preserved | 4 | Command success and actual schema absence / peer sentinel present |
| Unit + recorded E2E allocation reclaimed; unallocated E2E untouched | 5 | Both exact schemas absent, unrelated sentinel present |
| Partial failure/interruption remains blocked and retry completes truthfully | 4 → 5 | Inject failure after marking / between drops, retry, no recreation |
| No process stop, checkout removal, identity removal, or port-claim mutation | 4 → 5 | Live peer endpoint and unchanged checkout/config/claim fixtures |

Use `CURSOR_DEV=true nix develop -c node --test
scripts/worktree-retirement*.test.mjs` for the new command proof. Add focused
existing backend-wrapper, SUT-start/restart, and Cypress tests only where their
admission behavior changes. Each leaf adds its tests with its behavior; never
commit red tests alone.

For actual schema deletion proof, use a disposable MySQL instance in temporary
storage on an unused port, with the same Nix MySQL 8.4 version as local tooling.
Bind the command's process-level MySQL test adapter to that instance; do not add
a product target override. Seed target tables with data and a separate peer
sentinel; execute the real SQL and query resulting schema/data state. No shared
or developer databases may be dropped for verification. Ordinary DROP semantics
require no preliminary storage experiment. Record engine version, literal test
command, and observed postconditions during execution; mock call assertions
alone do not prove reclamation.

## Ordered slices

### 1. Inspect the disposable database targets for this checkout
Type: Behavior
Status: done
Proof: `CURSOR_DEV=true nix develop -c node --test scripts/worktree-retirement*.test.mjs` (8/8) — exact unit/E2E targets for linked identity; refusals for primary, absent/invalid identity, duplicate registered id, custom E2E, and mutation without `--check`. No DB mutation.

Behavior: Existing linked checkout with recorded identity → run `--check` →
see its exact disposable targets, or a visible ownership/configuration refusal.

Add the small command and package alias; initially expose only `--check` and
label this as target inspection, not authorization or verified idleness.
Refuse mutation mode until slice 4. Document the inspection boundary alongside
the existing worktree guide. Reuse Git/config helpers, validating canonical
database names and registered peer IDs without a persistent inventory.

Sizing: ~5 minutes, medium confidence; one command-selection proof loop.

### 2a. Explain recorded evidence that prevents retirement
Type: Behavior
Status: done
Proof: `CURSOR_DEV=true nix develop -c node --test scripts/worktree-retirement*.test.mjs` (16/16) — refuses live/stale backend lock, live/stale SUT owner, Cypress lease, foreign listener, active sessions, and session inspection failure; clear path still incomplete; fixtures remain alive; no DROP.

Behavior: Valid targets → inspect recorded ownership/listeners/database sessions
→ report the busy/uncertain evidence preventing retirement.

Extend the same command's assessment with these bounded vetoes. Continue to
label a clear result as incomplete until orphan inspection in slice 2b. Do not
add killing or stale-record repair.

Sizing: ~5 minutes, medium-high confidence; existing ownership and listener
entry points, one refusal proof loop.

### 2b. Refuse retirement when a backend outlives its owner
Type: Behavior
Status: done
Proof: `CURSOR_DEV=true nix develop -c node --test scripts/worktree-retirement*.test.mjs` (21/21) — refuses reparented JVM with session and disconnected orphan without listener/session; failed/ambiguous inspection refuses; verified idle reports idle snapshot; fixtures remain alive.

Behavior: No live recorded owner → inspect remaining checkout process evidence
→ a surviving backend still prevents retirement.

Add the bounded working-directory / command-line inspection described above.
Use real fixture processes to verify reparenting and identity discovery, not
only a stub returning a PID. No process discovery framework. If supported
orphan identity cannot be distinguished with these existing runtime signals,
record that learning and refine this leaf before enabling deletion; do not
replace the promise with a sessions-only test.

Sizing: ~5 minutes, medium confidence; one eligibility proof loop. Process
inspection is the main sizing risk; scrutinize at 5 minutes and finer-decompose
at 10 if it fails to converge. No timing guarantee.

### 3. Coordinate runner admission with retirement
Type: Structure
Status: done
Proof: Admission suites `node --test scripts/worktree-retirement-admission.test.mjs scripts/backend-test-worktree-admission.test.mjs scripts/sut-retirement-admission.test.mjs scripts/worktree-retirement*.test.mjs scripts/sut-owner-application-group.test.mjs` (32/32); `pnpm test:backend-test-worktree` (74/74); `pnpm test:sut-start` (56/56); `pnpm test:sut-restart` (8/8). Gate/marker refuse before provision/launch; restart with retainOwnership skips gate; gate/marker gitignored.

Internal change: add one small checkout admission/marker contract shared by
the existing shell and Node entry points. Keep their existing lifetime ownership;
do not merge all runners under one long-held lock. Ignore the new local gate and
marker in Git. Enables immediately following Behavior 4's safe unit retirement.

Place backend admission before prepare/provision and release after backend
ownership is established. Place SUT admission before allocation and retain until
ownership is established; account for retained-owner restart without recursively
taking the gate. Keep health read-only. A present gate is a refusal, not a new
queue, lease recovery service, or automatic timeout-based deletion permission.

Sizing: ~5–8 minutes, medium confidence; small shared contract and two runner
admission paths, with existing restart reuse. Scrutinize above 5 minutes; stop
and split at 10 if integration needs additional independent preparation.

### 4. Retire an idle unit-test-only worktree allocation
Type: Behavior
Status: planned
Proof: Invoke mutation mode with one actual disposable schema and a peer sentinel;
target is absent, peer remains usable, and later backend/SUT starts refuse.
Barrier-test a competing runner. Inject interruption after marker publication:
retry completes the same retirement while starts remain blocked.

Behavior: Verified idle linked allocation without recorded E2E database →
explicitly retire → unit schema is reclaimed and the allocation stays retired.

Under the admission gate rerun slices 1/2a/2b validation, persist retirement state,
then drop only the unit target. Release only the invocation-owned gate on normal
exit. Preserve config/identity and report target and result. Refuse recorded E2E
allocations until slice 5, before marking or dropping anything. Retrying a marked
allocation validates the same identity and only completes its recorded targets.
Document command use before checkout removal and failure/retry behavior.

Sizing: ~5 minutes implementation/command proof, medium confidence. The first
isolated MySQL boot and schema proof may exceed 10 minutes: external test-runtime
exception only, record actual elapsed time rather than disguising coding overrun.

### 5. Retire the same worktree's allocated E2E database too
Type: Behavior
Status: planned
Proof: Same real command/database harness with both allocated targets, populated
tables, peer sentinel and port-claim fixture. Both targets disappear, peer and
claims survive. Fail the second DROP once: report partial retirement, block starts,
then retry to completion. An unrecorded E2E-shaped database stays untouched.

Behavior: Verified idle allocation with its canonical recorded E2E database →
explicitly retire → its unit and E2E schemas are reclaimed as one retirement.

Extend slice 4's exact target list and retry path; remove its temporary E2E
refusal. Reuse validation, gate, and marker. Update the same guide's supported
boundary. No all-or-nothing SQL claim, new journal, or port cleanup.

Sizing: ~5 minutes, medium-high confidence; one target-list extension and proof
loop. Isolated MySQL runtime is the same explicit exception as slice 4.

## Learnings

Code inspection identified missing-unit-schema reprovisioning and provisioning
before SUT ownership. These require admission coordination and durable retirement
state; they do not expand the accepted product scope. Existing process ancestry
helpers cannot discover already-orphaned children and some return empty results
on inspection failure; do not reuse those as proof of retirement eligibility.

## Refinement review

Replaced initial slice 2 with 2a (known busy evidence) and 2b (orphan evidence).
This isolates the uncertain process-inspection work from existing owner checks.
All original promises remain mapped above; no story scope changed.

| Leaf | Review | Sizing rationale |
|---|---|---|
| 1 | Ready | One command target-selection loop; existing config/Git helpers |
| 2a | Ready | One busy-refusal loop using existing runtime boundaries |
| 2b | Ready | One bounded orphan-refusal loop, isolated from ownership plumbing |
| 3 | Ready | Cohesive admission contract enables only immediate Behavior 4; 5–8 minute estimate explicitly scrutinized |
| 4 | Ready | Unit-only retirement uses prepared checks and admission; one deletion/retry loop |
| 5 | Ready | Canonical E2E target variation reuses the same retirement loop |

Ready for execution, with the normal 5-minute scrutiny / 10-minute hard
decomposition rule. Only isolated MySQL test runtime has a stated duration
exception. No elapsed-time guarantee, implementation, or product verification
is claimed. No open product questions. Implementation, commit, and push remain
outside this planning request.
