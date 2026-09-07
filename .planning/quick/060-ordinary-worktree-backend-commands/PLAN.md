# Ordinary backend commands use the owning worktree database

Source: [SEED-015 story 1c](../../seeds/SEED-015-concurrent-worktree-environments.md#story-1c).
Status: in progress; slices 1–9 done.

## Goal and scope

Developers and AI tasks use `pnpm backend:test`, `pnpm backend:test_only`, and
repository Gradle-wrapper `test` / `migrateTestDB` in local Nix worktrees without
manual setup or remembering an opt-in command. Test-only and focused `--tests`
invocations prepare and migrate before tests; migration-only does not run tests.
Commands and fresh shells reuse one persistent environment per checkout.

Accepted 2026-09-07: an unconfigured primary checkout retains existing defaults
and explicit-URL behavior; fresh linked worktrees initialize isolation. Configured
primary checkouts use their assigned environment. In isolation, conflicting
`SPRING_DATASOURCE_URL`, `DB_URL`, or `SPRING_FLYWAY_URL` refuses visibly;
matching overrides remain valid. No product questions remain.

Retain opt-in compatibility, allocation, invalid-config refusal, no repair of
operator-supplied identities/databases, and one owner per checkout. MySQL 8.4
already runs on port 3309; dependencies are installed. No global serialization.

Excluded: application/development/E2E isolation, ports, hooks, Cloud VM/CI changes,
database cleanup/rebuild/rollback, duplicate-ID recovery, system-installed Gradle,
IDE launchers, Windows wrapper, and general Gradle command normalization. The
supported direct forms are the repository wrapper from root with `-p backend`
or from `backend/`, with explicit `test` / `migrateTestDB` tasks (including their
root-qualified forms). Preserve unrelated task behavior. Document this boundary.

## Execution context and decisions

- `scripts/backend-test-worktree.sh` owns preparation, validation, lock, and a
  foreground Gradle exec. Extract only enough to share this implementation with
  ordinary commands; do not create a second allocator or lifecycle manager.
- A small hook in `backend/gradlew`, after `APP_HOME` resolution and before Java
  argument assembly, can route supported invocations to the shared owner.
  Put policy in a capability-named script. Preserve cwd and argument tokens.
  An invocation-local handoff prevents recursive routing and self-lock refusal;
  do not persist that marker in config or shell setup.
- Use the existing foreground exec / no-daemon lifetime for isolated commands.
  Do not record a long-lived Gradle daemon PID as the invocation owner.
- `backend/build.gradle` currently adds `mustRunAfter` with `worktreeTestRun`;
  it does not make standalone tests migrate. Request migration once before
  isolated tests and preserve filters. Failed migration must prevent testing,
  including when `--continue` is supplied. Retain actual execution rather than
  cached/up-to-date avoidance as the opt-in workflow does.
  Select the test profile for isolated `test` even without the caller's `-D`
  profile argument; do not let a conflicting profile silently select other data.
- `backend:test` currently formats, invokes migration, then test_only. Preserve
  formatting but use one owned isolated migration/test command. The unconfigured
  primary path keeps its existing behavior.
- Tests live in `scripts/backend-test-worktree*.test.mjs`, with stand-in,
  launcher, and lock fixture modules. Extend the public command boundary.
  Replace the source-regex Gradle ordering assertion with observable evidence
  when replacing that contract. Do not expose allocator internals to tests.
- Quick/058 is complete; reuse exclusive stale reclaim and the ignored lock.
  Stories 1a–1b already prove shared-MySQL isolation and first-use provisioning.
  No new DDL or uncertain transaction semantics require a planning experiment.
- Follow [ADR 0006 — Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md):
  visible failures, no fallback database or speculative recovery.

## Outside-in proof and ownership

| Promise | Leaf | Observable evidence |
|---|---:|---|
| Shared owner and opt-in compatibility | 1–2 | Real wrapper fixture and existing external command suite remain green |
| Ordinary configured migration does not test | 2 | Real wrapper boundary selects assigned URL and migration only |
| Ordinary configured tests migrate once and preserve filters/execution | 3 | Command fixture; real Gradle execution in 9 and 11 |
| Fresh linked worktree first use | 4 | Actual Git topology fixtures and real runs in 9 |
| Primary defaults, configured primary, unrelated/nonlocal commands | 5 | Compatibility boundary matrix without DB side effects |
| Conflicting URLs cannot bypass assignment; matching URLs work | 6 | Refusal before workload / successful matching-target launch |
| Ordinary pnpm workflow uses one owned migration/test run | 7 | Actual package entry-point fixture and real runs in 9 |
| Cross-entry-point overlap and stale reclaim stay exclusive | 8 | Held workload plus competing invocation: one admitted owner |
| Real concurrent data/schema independence | 9 | Executed test reports and independent DB/Flyway observations |
| Migration failure prevents tests | 10 | Real failing migration, no new test results or test task execution |
| Reuse across shells/commands, standalone first-use migration and focused tests | 11 | Same ID/DB, migration-only followed by actual filtered execution |
| Cancellation stops owned workload without stopping MySQL/other run | 12 | Process exit, other runner success, MySQL query succeeds |

## Ordered slices

### 1. Exercise the real wrapper at the command boundary
Type: Structure
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm test:backend-test-worktree` stays green.

Internal change: Extend the fixture to copy the real wrapper and intercept its
Java endpoint, rather than replacing the wrapper itself. Reuse recording/hold
helpers. Prove current wrapper pass-through and retain the opt-in fixture.
Immediate next Behavior: leaf 2. No production routing change yet.

Sizing: ~5 minutes, medium confidence; one fixture capability, no production
extraction or real Gradle process. This removes hidden preparation from leaf 2.

### 2. Run ordinary migration in a configured checkout
Type: Behavior
Status: done
Proof: Real wrapper routing with a stand-in Java/Gradle endpoint observes assigned
URL and migration-only execution, with no recursive handoff or lock reacquisition.
Exercise root `-p backend` and backend-directory forms. Extend the usage guide.

Behavior: Configured checkout → repository-wrapper `migrateTestDB` → migrate the
assigned database under its owner, without running tests.

Implementation: Share the existing preparation/owner code through a sourced
shell module or equivalent small extraction and route migration-only into it.
Keep the opt-in caller on that same code. The preceding fixture supplies proof;
no independent preparatory framework is needed.

Sizing: ~5 minutes, medium confidence; small extraction plus one routing case in
one green loop. If argument routing grows beyond this concrete case, refine
before extending it; do not implement a general Gradle parser.

### 3. Run ordinary focused tests after preparing the configured database
Type: Behavior
Status: done
Proof: Wrapper fixture observes one migration before tests, the test profile
without requiring caller flags, a preserved `--tests` token, actual-run settings,
and nonzero unmatched-filter behavior. Migration failure prevents tests even
with caller `--continue`. Real task proofs are leaves 9–11.

Behavior: Configured checkout with pending migrations → ordinary `test` → the
requested tests execute only after its database is ready.

Sizing: ~5 minutes, medium confidence; one isolated task-dispatch policy.

### 4. Initialize fresh linked worktrees through ordinary commands
Type: Behavior
Status: done
Proof: Fixtures use real Git primary/linked topology. Fresh migration-only and
test-only commands allocate once before workload; subsequent entry points reuse
the ID. Existing invalid-config/provision-failure/no-repair proofs stay green.

Behavior: Linked worktree without config → ordinary migration/test → prepare and
use its persistent isolated database automatically.

Sizing: ~5 minutes, medium confidence; topology selection reuses first-use logic.

### 5. Preserve ordinary-command compatibility outside isolation
Type: Behavior
Status: done
Proof: Command matrix: unconfigured primary default and explicit URL pass through;
configured primary uses its ID; unrelated tasks and existing CI forms do not
allocate. No actual shared database mutation. Document the accepted policy.

Behavior: Invocation has no configured or automatically selected isolation →
ordinary command → retain established target and execution behavior.

Sizing: ~5 minutes, medium confidence; routing guard/regression matrix.
Do not mistake `CURSOR_DEV` quiet mode for the only form of local Nix usage.

### 6. Reject explicit target conflicts on ordinary isolated commands
Type: Behavior
Status: done
Proof: Each conflicting URL environment variable refuses before migration/tests;
matching URLs succeed. Relevant CLI datasource/Flyway URL system-property forms
also cannot override the selected target silently; reject conflicting forms.

Behavior: Isolation applies with conflicting explicit target → ordinary command
→ visible refusal without a database workload against another target.

Sizing: ~5 minutes, medium confidence; reuse URL validation at argument forwarding.

### 7. Keep pnpm verification in one isolated invocation
Type: Behavior
Status: done
Proof: Invoke actual package entry points with external process stand-ins:
backend:test still formats and executes one owned migration/test run; test_only
also prepares before testing. Primary legacy path remains green.

Behavior: Isolated checkout → either ordinary pnpm test command → complete its
requested verification against the assigned database under one invocation owner.

Sizing: ~5 minutes, medium confidence; eliminate redundant isolated migration
dispatch. Update obsolete guide limits with the supported command forms.

### 8. Refuse competing entry points in one checkout
Type: Behavior
Status: done
Proof: Hold an ordinary workload; opt-in or ordinary migration/test competitor
refuses. Reuse stale-reclaimer overlap proof with shared ownership. Another
checkout remains admissible. Run the command suite.

Behavior: One invocation owns checkout → another supported entry point overlaps
→ only the owner enters its database workload.

Sizing: ~5 minutes, medium confidence; extend existing asynchronous fixture.

### 9. Prove real concurrency through ordinary pnpm commands
Type: Behavior
Status: done
Proof: Two fresh disposable linked worktrees overlap `pnpm backend:test` and
`pnpm backend:test_only`. Record executed counts (not cached/skipped), distinct
DBs/Flyway histories, literal commands, and overlap observations. Observe a
checkout-specific fixture marker in each DB to establish data independence;
read shared development/test/E2E sentinel state before and after without changing
it. Never mutate shared DBs. Both complete suites must pass.

Behavior: Two fresh linked worktrees run ordinary commands concurrently → each
actually verifies its own code/data/schema without disturbing another target.

Sizing: ~5 minutes active setup, medium confidence; real full-suite runtime is an
explicit >10-minute exception. Backend rules require full suites for backend
verification; focused filter checks supplement rather than replace them.

### 10. Stop ordinary tests after a real migration failure
Type: Behavior
Status: planned
Proof: Use only an attempt-owned disposable worktree/database. Supply an invalid
new, unapplied migration, invoke ordinary wrapper `test --continue`, and observe
nonzero migration failure with no test execution/new test report. Remove only
the temporary migration afterward; do not repair or alter shared schemas.
Record literal command, migration error, and task/report observations.

Behavior: An isolated checkout's migration fails → ordinary test invocation →
tests never start and the failure remains visible.

Sizing: ~5 minutes active work, medium confidence; one real failure proof.
Gradle startup/migration runtime is an explicit possible timing exception.

### 11. Reuse an environment across migration and focused-test commands
Type: Behavior
Status: planned
Proof: In another fresh disposable linked checkout, start with
`CURSOR_DEV=true nix develop -c backend/gradlew -p backend migrateTestDB -Dspring.profiles.active=test`.
In a new shell use
`CURSOR_DEV=true nix develop -c backend/gradlew -p backend test --tests 'com.odde.donut.controllers.*' -Dspring.profiles.active=test`.
Observe first command migration without tests; second command actually executes
matching tests with unchanged config/DB and no second provisioning. Repeat the
test command from `backend/` to cover cwd resolution. Record exact reports and
IDs; compare Flyway history. Existing 1b branch-change identity evidence remains
valid because config identity semantics do not change.

Behavior: An environment was created with ordinary migration → change shell and
entry point to focused tests → use the same prepared persistent environment.

Sizing: ~5 minutes active work, medium confidence; focused backend runtime is an
explicit possible exception. Complete-suite evidence is already owned by leaf 9.

### 12. Cancel one ordinary run without disturbing another
Type: Behavior
Status: planned
Proof: Reuse the two isolated checkouts from leaf 9. Start overlapping ordinary
test runs, interrupt one owned foreground process group, and observe its Gradle
and test workload exit promptly. The other runner completes and a MySQL query
still succeeds. Record command/PID, interrupt, exit, other report, and DB query.
Do not stop MySQL or delete databases. Existing stale-owner recovery proof in
leaf 8 establishes that a later invocation can reclaim the stopped checkout.

Behavior: Two isolated workloads overlap → cancel one → its owned workload
stops while the other worktree and shared MySQL remain usable.

Sizing: ~5 minutes active work, medium confidence; one lifecycle proof. Remaining
suite runtime is an explicit exception. No new background supervisor is planned.

## Execution discipline and learnings

Keep all new supported paths validated/locked from their first leaf; later proof
leaves do not permit interim unsafe behavior. Keep opt-in usable at every
stopping point.

- Slice 1: the real wrapper execs `$JAVA_HOME/bin/java` when JAVA_HOME is set
  (Nix sets it). The fixture copies `backend/gradlew` and intercepts that
  Java endpoint; PATH-only `java` would miss it. Reuse this for ordinary-command
  routing in leaf 2.
- Slice 2: `backend/gradlew` is a symlink to root `gradlew`, so APP_HOME is
  the checkout root. Isolation policy lives in
  `scripts/backend-worktree-gradle-route.sh`; an invocation-local
  `DONUT_WORKTREE_HANDOFF` prevents recursive lock.
- Slice 3: ordinary configured `test` runs a separate migrate invocation
  first (so `--continue` cannot start tests after migrate failure), then
  execs the original test args with the test profile and actual-run flags.
- Slice 4: linked vs primary is `git-dir` ≠ `git-common-dir`. Ordinary
  migrate/test on a linked worktree without config call the same prepare
  path; unconfigured primary still passes through.
- Slice 5: unconfigured primary (git-dir == git-common-dir, no config) keeps
  default/explicit URLs and CI-shaped test flags; isolation stays config or
  linked-worktree plus migrate/test.
- Slice 6: prepare refuses conflicting env and `-D`/`--` datasource/Flyway URL
  args; matching values still launch against the assigned database.
- Slice 7: `pnpm backend:test` always formats then `test_only`; isolated
  checkouts skip the extra migrate step because wrapper `test` already
  migrates once. Isolation predicate is shared (`backend_worktree_isolation_applies`).
- Slice 8: ordinary wrapper and opt-in share `.worktree.local.lock`; held
  migrate refuses overlapping opt-in or ordinary migrate; stale reclaim works
  on both entry points.
- Slice 9: overlapping `pnpm backend:test` (A) and `pnpm backend:test_only`
  (B) in linked worktrees
  `/Users/terryyin/.cursor/worktrees/doughnut/q060-conc-a` and `q060-conc-b`
  each executed 2263 tests (0 fail/skip) against distinct DBs
  `doughnut_wt_bc303497f9664f2cb862e5295749a3d2_test` and
  `doughnut_wt_d7458993b9304d83ab323dd31129a91e_test`. Independent Flyway
  histories (22 rows, distinct `installed_on`). Shared
  `doughnut_development` / `doughnut_test` / `doughnut_e2e_test` sentinels
  unchanged. Leave these two checkouts for leaf 12.

Quick/058 completed during refinement; no remaining lock-fix prerequisite.
No new storage experiment is needed. Repository-wrapper routing is the remaining
integration seam; command fixtures alone cannot establish real Gradle execution.

## Sizing review

Refinement performed in place on 2026-09-07:

- Replaced preparatory leaf 1 with the real-wrapper fixture needed immediately
  by leaf 2; bounded production sharing stays inside the migration outcome.
- Split original leaf 9 into real concurrency (9) and migration refusal (10).
- Split original leaf 10 into environment reuse (11) and cancellation (12).
- Repointed every proof promise. Existing first-use failure, identity, and lock
  coverage is reused; changed entry-point boundaries receive new observations.
- All leaves now have one cohesive green loop and a ~5-minute active-work
  hypothesis, medium confidence. Real Gradle/backend runtime exceptions are
  explicit in 9–12. No execution-time guarantee or unexplained >10-minute path.

Ready for direct execution when implementation is requested. No further open
questions. On an execution overrun follow problem-decomposition learning
escalation; keep compatible evidence and refine this same PLAN.
