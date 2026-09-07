# Automatic first-use worktree backend tests

Status: planned; slice plan refined 2026-09-07.
Source: [SEED-015 story 1b](../../seeds/SEED-015-concurrent-worktree-environments.md#story-1b).

## Goal and scope

A developer or AI task can run the existing `pnpm backend:test:worktree`
workflow in a fresh local checkout without manually choosing an identity,
creating its database, or granting access. The first successful invocation
persists one isolated environment; later invocations reuse it. Different fresh
worktrees can initialize and test concurrently, while one checkout never runs
two test processes against its database at once.

Use the existing local MySQL server on `127.0.0.1:3309`, its established
passwordless local root administration, and the existing `doughnut` test user.
Preserve valid 1a configuration exactly: when `.worktree.local.json` already
exists, select it without database creation or identity replacement. Missing or
inaccessible explicitly configured databases continue to fail rather than being
silently repaired.

Excluded: ordinary `backend:test`, `backend:test_only`, direct Gradle entry
points, AI/worktree-creation hooks, a machine-wide allocation registry, ports,
application/E2E services, Cloud VM/CI behavior, automatic schema rollback,
database or allocation retirement, orphan cleanup, and recovery from duplicate
operator-supplied IDs. Do not change shared MySQL lifecycle or serialize runs in
different worktrees.

## Outside-in proof

| Promise | Owning slice | Observable signal |
|---|---:|---|
| One checkout has at most one active worktree test invocation | 2 | A second command-boundary process refuses before reading mutable checkout state or reaching Gradle while the owner remains active |
| Missing configuration provisions a new identity/database and runs requested tests | 4 | Process fixture records the bounded ID, root administration SQL, durable config, selected JDBC target, and Gradle execution |
| First-use overlap leaves one complete environment | 5 | Two command processes against one missing-config fixture produce one owner, one visible refusal, and one usable config/database mapping |
| Existing and generated configurations are reused without provisioning | 6 | Repeated command-boundary runs retain the ID/database and perform no administration call |
| A later invocation can reclaim a dead owner's lock | 7 | A valid configured command replaces only a stale PID record and reaches Gradle against the same database |
| Database administration failure has no fallback or test run | 8 | Failed create/grant exits nonzero with no config, Gradle invocation, or alternate target |
| A competing config publication never overwrites an identity | 9 | Exclusive-write collision preserves the established config and exits before Gradle |
| Different fresh worktrees initialize and test independently | 11 | Two real overlapping focused test runs select distinct new MySQL databases and both pass against their own Flyway histories |
| A later real shell reuses the automatically allocated environment | 12 | A fresh-shell focused rerun retains the ID/database and passes without manual setup |

## Current decisions

- Keep `pnpm backend:test:worktree` and
  `scripts/backend-test-worktree.sh` as the only supported entry point. Missing
  configuration on this explicitly isolated workflow opts into automatic setup
  in any local checkout; story 1c still owns whether ordinary commands retain
  legacy defaults in a primary checkout.
- Generate `wt_<32 lowercase hexadecimal characters>` from Node's UUID support,
  which remains within 1a's accepted `wt_[a-z0-9_]{1,32}` contract. MySQL is the
  allocation arbiter: issue `CREATE DATABASE` without `IF NOT EXISTS`, followed
  by the existing grants for `doughnut` at `localhost` and `127.0.0.1`. An
  existing name or any administration error fails visibly; it is never adopted.
- Write `.worktree.local.json` only after database creation and grants succeed,
  using exclusive creation so an established identity cannot be overwritten.
  A crash before the write may leave an unreferenced database; cleanup is out of
  scope and the next attempt allocates a new candidate.
- Acquire one checkout-local PID lock before reading or creating configuration
  and hold it through the foreground Gradle run. Use exclusive file creation in
  a small Node command, record the launcher's PID, and retain the existing shell
  `exec` so Gradle inherits that PID and terminal/process-group behavior. A later
  invocation may reclaim the ignored stale file only when its recorded PID is no
  longer alive. An active or malformed owner record causes a clear nonzero
  refusal; do not wait and later start a second test run.
- Continue rejecting conflicting `SPRING_DATASOURCE_URL`, `DB_URL`, and
  `SPRING_FLYWAY_URL` before migration/tests. Print whether the command allocated
  or reused its environment and always print the selected database.
- Follow [ADR 0006 — Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md):
  prevent adoption/overwrite with atomic operations, propagate native database
  failures, and add context only where the raw operation would not identify the
  failed environment. No retry, repair, fallback, or broad native-error matrix.
- Matching storage evidence already exists in
  [quick/054](../054-configured-worktree-backend-tests/PLAN.md): MySQL 8.4.11
  successfully created separate `utf8mb4` / `utf8mb4_unicode_ci` databases and
  granted the local `doughnut` user access before independent migrations/tests.
  This plan changes ownership of that setup, not its storage semantics.

## Execution context

- `scripts/backend-test-worktree.sh` already validates configuration, rejects
  conflicting URLs, exports one selected datasource, and foregrounds one Gradle
  migration/test invocation.
- `scripts/backend-test-worktree.test.mjs` drives that public command boundary.
  `scripts/backend-test-worktree-test-fixtures.mjs` already supplies a temporary
  checkout and controllable Gradle stand-in; extend this boundary instead of
  exporting allocation helpers for tests.
- `docs/worktree-backend-tests.md` documents the manual 1a setup and must end
  with first-use setup as the primary workflow while retaining explicit-config
  compatibility and current limits.
- The real final proof can use a representative focused database-backed backend
  test in each fresh worktree. Quick/054 already owns and records the full-suite
  concurrency proof; repeating two full suites would not add first-use evidence.

## Ordered slices

### 1. Let the launcher fixture hold one active Gradle owner
Type: Structure
Status: done
Proof: Existing launcher tests remain green after the temporary checkout
fixture gains an asynchronous invocation mode and a controllable foreground
Gradle stand-in. The fixture can wait for an explicit Gradle-reached marker and
release that process without changing production behavior.

Internal change: Extend only the command-boundary fixture needed to observe one
active owner in slice 2. Preserve the synchronous helper and every existing
configured-workflow assertion. Immediate next Behavior: slice 2.

Learning: Added `runLauncherAsync` (spawn-based) plus a `GRADLE_HOLD`-gated
hold/release protocol (poll for a release file after writing a reached-marker
file) in the fake gradlew stand-in written by `makeCheckout`. Extracted a
shared `sanitizedChildEnv` helper so `runLauncher` and `runLauncherAsync` don't
duplicate env sanitization. The synchronous `runLauncher` and all 14 existing
tests are unchanged.
`CURSOR_DEV=true nix develop -c node --test scripts/backend-test-worktree.test.mjs`
passes.

Sizing: about 5 minutes, high confidence; fixture capability only, one existing
test command, no production orchestration.

### 2. Refuse a second test invocation in the same checkout
Type: Behavior
Status: done
Proof: Start the launcher with valid explicit configuration and hold its Gradle
stand-in active. A second launcher process exits nonzero with an owner-specific
message before reading a deliberately malformed replacement config or reaching
Gradle; a malformed owner record also refuses rather than being reclaimed. A
command in a different temporary checkout can still reach its own Gradle
stand-in. Release the owner and observe its original exit signal. Verify with
`node --test scripts/backend-test-worktree.test.mjs`.

Learning: `backend-test-worktree.sh` now creates
`<checkout_root>/.worktree.local.lock` with atomic `mkdir` before reading any
config, recording the launcher's own PID in `owner.pid`. An existing lock
always refuses (owner-specific message for a numeric PID, "invalid record"
otherwise) — no liveness check, no reclaim; slice 7 owns reclaiming a stale
(dead-PID) owner. No trap/cleanup: the lock directory is left behind after the
run. New lock tests live in `scripts/backend-test-worktree-lock.test.mjs`
(split out during refactor to keep `backend-test-worktree.test.mjs` at its
line-count limit); `package.json`'s `test:backend-test-worktree` script now
runs both files.
`CURSOR_DEV=true nix develop -c node --test scripts/backend-test-worktree.test.mjs scripts/backend-test-worktree-lock.test.mjs`
passes (17/17).

Behavior: A configured checkout already has an active worktree test command →
another command starts in that checkout → the second refuses without sharing
the database, while commands in other checkout roots remain independent.

Implement atomic checkout ownership around the existing foreground run. Preserve
the shell-to-Gradle `exec` and terminal/process-group cancellation; do not
introduce traps, a detached supervisor, a cross-worktree lock, or a wait queue.

Sizing: about 5 minutes, medium confidence; one exclusive PID-file decision and
process proof loop, with no child supervision or signal cleanup path.

### 3. Let the launcher fixture observe database provisioning
Type: Structure
Status: done
Proof: Existing launcher tests remain green after the temporary checkout gains
a recording MySQL stand-in whose exit status can be selected. Existing
configured invocations still make no administration call. No production
behavior changes.

Learning: `makeCheckout` now writes a recording `mysql` CLI stand-in to
`<root>/mysql-stand-in` (mirrors the existing `gradlew` stand-in, extracted
through a shared `writeStandIn` helper), recording CLI args and piped stdin to
`<root>/mysql-invocation` and exiting `${FAKE_MYSQL_EXIT:-0}`. New
`readMysqlInvocation(checkout)` reads it back. `backend-test-worktree.sh` is
untouched — nothing invokes the stand-in yet; slice 4 wires it in.
`CURSOR_DEV=true nix develop -c node --test scripts/backend-test-worktree.test.mjs scripts/backend-test-worktree-lock.test.mjs`
passes (17/17).

Internal change: Add only the command-boundary fixture capability required to
observe automatic database creation in slice 4. Keep MySQL behavior controlled
through the child process rather than exporting allocation helpers for direct
tests. Immediate next Behavior: slice 4.

Sizing: about 5 minutes, high confidence; one process stand-in and the existing
launcher test command.

### 4. Provision the first isolated environment and run its tests
Type: Behavior
Status: done
Proof: Invoke the launcher in a fixture with no local configuration. Its MySQL
stand-in receives one create-and-grant request for a generated bounded target;
only after success does the checkout contain the matching config. The launcher
prints allocation and selection, then the existing Gradle stand-in receives the
same JDBC URL. Verify the complete outcome through
`node --test scripts/backend-test-worktree.test.mjs`.

Learning: When `.worktree.local.json` is absent, `backend-test-worktree.sh`
generates `wt_<32-lowercase-hex>` (Node's `crypto.randomUUID()` sans dashes),
runs `mysql -u root -h 127.0.0.1 -P 3309 -e "..."` (CREATE DATABASE
utf8mb4/utf8mb4_unicode_ci + GRANT to `doughnut`@`localhost`/`127.0.0.1` +
FLUSH PRIVILEGES, matching quick/054's semantics via shared
`database_for_worktree()`/`mysql_host`/`mysql_port`), and — relying on
`set -euo pipefail` for nonzero propagation on failure — writes
`.worktree.local.json` exclusively (`fs.writeFileSync(..., { flag: 'wx' })`)
only after success, printing "Allocated new worktree environment: wt_xxxx"
before the existing "Selected database: ..." line. An existing config skips
this block entirely. The fixture's mysql stand-in moved to `<root>/bin/mysql`
on `PATH` via a new `launcherChildEnv` helper. New tests live in
`scripts/backend-test-worktree-provisioning.test.mjs`; the now-obsolete
"missing configuration refuses" test was removed (superseded by this slice's
intentional behavior change) with equivalent coverage in the new file.
`CURSOR_DEV=true nix develop -c pnpm test:backend-test-worktree` passes
(19/19).

Behavior: A local checkout has no worktree configuration → the developer runs
the opt-in command, optionally with its existing `--tests` filter → one new
database and persistent identity are created before migration and the requested
tests run against that target.

Keep generated values within the existing validation contract, use the current
test database character set/collation and grants, and exclusively create the
config after successful provisioning. Do not add a global registry or alter an
existing config.

Sizing: about 5 minutes, medium confidence; one cohesive first-use path and one
command proof loop. The allocator reuses existing Node/MySQL dependencies and
has no search/retry policy or separate registry.

### 5. Converge overlapping first-use commands on one environment
Type: Behavior
Status: done
Proof: Start two launcher processes against one missing-config fixture, holding
the owner's Gradle stand-in after provisioning. The owner leaves one complete
config/database mapping and reaches Gradle; the overlapping process refuses
before MySQL and Gradle. Release the owner and verify the fixture remains usable.
Run `node --test scripts/backend-test-worktree.test.mjs`.

Learning: No production ordering gap existed — the ownership lock (slice 2)
is already acquired before provisioning (slice 4), so an overlapping launcher
refuses immediately without touching mysql or the config. Added the
integration test proving this in
`scripts/backend-test-worktree-provisioning.test.mjs`, and fixed a real latent
fixture bug: `runLauncherAsync` (used by `spawn`, unlike `runLauncher`'s
`spawnSync`) left the child's stdin open indefinitely, which hangs the mysql
stand-in's EOF wait — only exposed once this slice combined
`runLauncherAsync` with missing-config provisioning. Fixed with
`child.stdin.end()`. "Remains usable" is interpreted as: after the owner
exits, `.worktree.local.json` stays complete/parseable and names the exact
database the owner migrated/tested against (a bare re-run without reclaim is
out of scope — slice 7).
`CURSOR_DEV=true nix develop -c pnpm test:backend-test-worktree` passes
(20/20).

Behavior: One unconfigured checkout receives overlapping first-use commands →
one command initializes and tests → the other refuses, leaving one complete
reusable environment rather than two test owners or competing identities.

Use the lock behavior already proved in slice 2; this leaf owns only its
integration with missing-config initialization. Repair only a demonstrated
ordering gap between ownership, configuration, and provisioning.

Sizing: about 5 minutes, high confidence; one concurrent command scenario using
the fixture capabilities already established.

### 6. Reuse an established identity without provisioning
Type: Behavior
Status: done
Proof: Run the command fixture with both a manually supplied 1a config and the
config produced by slice 4. Each selects its existing database and reaches
Gradle without calling MySQL administration or changing the config. Run
`node --test scripts/backend-test-worktree.test.mjs`.

Learning: A manually supplied 1a config was already fully proven (slice 4's
"existing configuration skips provisioning entirely" test asserts no mysql
invocation). The real gap was a config *produced by slice 4's own
provisioning*, reused on a second launcher run: added a test proving the
second run reuses the same id/database with no new mysql call and a
byte-identical config, clearing the ownership lock directory between
invocations (test-local, commented) since lock reclaim is a separate,
not-yet-implemented concern (slice 7). Malformed/invalid config refusal
remains proven by the untouched existing 1a tests. No production code change
was needed.
`CURSOR_DEV=true nix develop -c pnpm test:backend-test-worktree` passes
(21/21).

Behavior: A checkout already has a valid persistent environment → a later
command starts → it reuses the same identity/database without allocation,
identity replacement, or fallback.

Keep malformed/invalid `.worktree.local.json` behavior under the existing 1a
tests. Do not repair missing databases for operator-supplied identities.

Sizing: about 5 minutes, medium confidence; variants share one reuse outcome and
focused command-boundary proof loop.

### 7. Reclaim checkout ownership after its process exits
Type: Behavior
Status: planned
Proof: Leave a PID record for a process that has exited, then invoke the
configured command. It atomically replaces that stale ownership, selects the
same database, and reaches Gradle. The active and malformed records covered by
slice 2 remain refusals. Run
`node --test scripts/backend-test-worktree.test.mjs`.

Behavior: A previous launcher owner has exited but its ignored PID record
remains → a later command starts → it reclaims checkout ownership and reuses the
configured environment.

Do not treat PID reuse, malformed ownership, or a live process as stale. Fail
visibly when ownership cannot be established safely.

Sizing: about 5 minutes, high confidence; one stale-owner precondition and one
command-boundary proof loop.

### 8. Stop when database administration fails
Type: Behavior
Status: planned
Proof: Make the MySQL stand-in fail the create/grant request. The first-use
command exits nonzero, writes no config, records no Gradle invocation or
fallback URL, and reports the generated target/stage without hiding the native
failure. Run `node --test scripts/backend-test-worktree.test.mjs`.

Behavior: The fresh checkout's database cannot be created or granted → automatic
setup fails → no identity is published and no migration/test starts.

Prefer native nonzero propagation; enrich only the target/stage context needed
to diagnose setup. An already created but unreferenced database may remain if a
later SQL statement failed; do not catch for cleanup, retry, repair, or fallback.

Sizing: about 5 minutes, high confidence; one administration-process failure
path using slice 3's stand-in.

### 9. Preserve an identity published during first-use setup
Type: Behavior
Status: planned
Proof: Arrange for an external writer in the command fixture to create a valid
config after MySQL provisioning but before the launcher's exclusive config
write. The command exits nonzero, preserves that exact config, never reaches
Gradle, and does not select either database as fallback. Run
`node --test scripts/backend-test-worktree.test.mjs`.

Behavior: Another actor publishes checkout configuration while first-use setup
is completing → the launcher loses the exclusive write → the established
identity remains untouched and the launcher refuses to test against ambiguous
state.

The generated database may remain unreferenced. Do not adopt it, overwrite the
winning config, or broaden this into duplicate operator-ID recovery.

Sizing: about 5 minutes, high confidence; one exclusive-publication collision
and focused process proof.

### 10. Prepare two fresh worktrees for real first-use proof
Type: Structure
Status: planned
Proof: Two disposable Git worktrees at the current implementation commit have
normal dependencies, neither has `.worktree.local.json`, and neither has a
preselected target. Record their explicit paths, clean status, MySQL 8.4 version,
and absence of configuration before testing. Do not create their databases
manually.

Internal change: Create only the two isolated local proof environments needed
immediately by slice 11. Use explicit temporary paths and do not create their
databases manually. Immediate next Behavior: slice 11.

Sizing: about 5 minutes active setup, medium confidence; dependency installation
is an external-wait exception rather than implementation scope.

### 11. Run first backend tests concurrently in two fresh worktrees
Type: Behavior
Status: planned
Proof: Start the same representative focused database-backed test through
`pnpm backend:test:worktree`, selecting
`com.odde.donut.controllers.NoteTitlePersistenceTest`, in both slice-10
worktrees with overlapping execution. Capture each command's allocated ID,
selected database, success result, and target-specific Flyway history. The two
IDs and databases differ; neither legacy database nor the other worktree's
configuration changes.

Behavior: Two fresh local worktrees start their first opt-in backend tests at
the same time → both automatically provision distinct environments and both
tests pass against their own code and database.

Repair only demonstrated first-use isolation failures. If another shared
mutable resource prevents the representative tests from overlapping, return to
story review rather than serializing or silently narrowing the test category.

Sizing: about 5 minutes active launch/observation, medium confidence; Nix,
Gradle, and focused-test runtime are explicit external-wait exceptions.

### 12. Reuse the real allocated environment from a later shell
Type: Behavior
Status: planned
Proof: In one environment produced by slice 11, record the config and target,
clear inherited datasource overrides, and rerun the same focused command from a
fresh shell. It retains the ID/database and passes. Update the guide and
agent-map wording to make automatic first use the primary opt-in workflow and
explicit configuration the compatibility path.

Behavior: A checkout has completed automatic first use → a later shell invokes
the worktree test command → it reuses the same isolated environment without
manual setup or another allocation.

The configured worktrees are outcomes of slice 11, not hidden preparation from
slice 10. Remove the two temporary Git worktrees through normal Git worktree
cleanup only after evidence is recorded. Retain their databases because
retirement and destructive cleanup are excluded.

Sizing: about 5 minutes active work, high confidence; one reuse proof loop plus
bounded documentation. Focused test runtime is an external-wait exception.

## Refinement record

No execution attempt preceded this refinement; all original slices were still
planned and no completed evidence was replaced.

- Original slice 1 prepared both concurrency and provisioning fixtures for
  several later behaviors. It became immediate Structure slices 1 and 3.
- Original slice 2 remains Behavior slice 2 with its fixture setup isolated in
  slice 1.
- Original slice 3 combined first-use success, same-checkout overlap, configured
  compatibility, and stale-owner recovery. Those are now Behavior slices 4–7.
- Original slice 4 combined administration failure and exclusive-publication
  collision. Those are now Behavior slices 8 and 9.
- Original slices 5–6 became slices 10–11. Slice 10 prepares only the immediate
  concurrent proof; the configured environments delivered by slice 11 become
  slice 12's precondition.
- Original slice 7's fixture-level reuse moved to slice 6; slice 12 now owns
  only the real fresh-shell reuse, bounded documentation, and temporary
  worktree cleanup.

Every remaining leaf now has one Behavior/Structure gate, one proof loop, and a
cohesive target-sized path. The only sizing exceptions are dependency setup and
focused Gradle/MySQL runtime in slices 10–12. Execution can resume directly at
slice 1.
