# Concurrent backend tests with explicit worktree configuration

Status: resume at leaf 9. Slices 1–8 done and pushed.

## Resume (new session)

Work only in this checkout:

- Path: `/Users/terryyin/git/doughnut-wt-054`
- Branch: `feat/configured-worktree-backend-tests` (tracks origin)
- Plan: `.planning/quick/054-configured-worktree-backend-tests/PLAN.md`
- Skill: `.agents/skills/execute-plan/SKILL.md` (wrap-up per slice; do not write `.planning/STATE.md`)
- Do not edit `/Users/terryyin/git/doughnut` except the final merge to `main`

Leaf 5 accepted after raising instance `max_connections` to 1000. Leaf 6 missing-database failure stops before tests. Leaf 7: interrupting A leaves B and MySQL usable. Leaf 8: A-only next migration stayed off B. Continue leaf 9. When the plan is complete: merge the feature branch to `main`, push `main`, delete the feature branch, and drop worktrees `doughnut-wt-054`, `-a`, and `-b`. Do not drop MySQL databases unless this PLAN later says to.

Disposable proof environments (keep until leaves 6–8 finish):

| Role | Path | ID | Database |
|---|---|---|---|
| A | `/Users/terryyin/git/doughnut-wt-054-a` | `wt_054a` | `doughnut_wt_054a_test` |
| B | `/Users/terryyin/git/doughnut-wt-054-b` | `wt_054b` | `doughnut_wt_054b_test` |

A/B are detached at `49d4f30544` (implementation). Shared MySQL is `127.0.0.1:3309` using `/Users/terryyin/git/doughnut/mysql/data`, started with `--max-connections=1000`.

Original request: execute quick plan 54 in a new worktree/branch, merge to main when done, drop worktree and branch.
Source: [SEED-015, story 1a](../../seeds/SEED-015-concurrent-worktree-environments.md#story-1a).
Stories 1a, 1b, and 1c occupy the first three product-backlog positions.
This plan covers only 1a. No implementation or database experiment was performed
while writing it.

## Goal and scope

Two developers or AI tasks with distinct, manually provisioned local databases
can migrate and execute backend unit tests concurrently against their own
worktree's code and schema. Provide one opt-in migration-and-test command,
including full-suite and focused invocation, using reusable gitignored local
configuration. Preserve the supported workflow's database selection across
shells, stage boundaries, failures, and cancellation.

Preconditions: MySQL is already available on `127.0.0.1:3309`; normal checkout
dependencies are installed. Operators supply distinct identities and create
the corresponding empty databases with access for the existing `doughnut` user.
One test runner per worktree; no performance speedup commitment.

Excluded: identity allocation, database creation/grants by the command, changes
to existing command entry points, hooks/skills, port allocation, application
servers, E2E, database deletion, CI/Cloud VM integration, shared-service startup,
and a generic environment manager. Do not acquire a machine-wide test lock.

## Current decisions

- Planning choice: root `.worktree.local.json` contains one field, for example
  `{"id":"wt_a7c2"}`. Accept an ID matching `wt_[a-z0-9_]{1,32}` and derive
  `doughnut_<id>_test`. The bounded format yields a valid database identifier
  distinct from the legacy database names. Add a root-scoped ignore entry.
  There are no service-port fields, stored credentials, or automatic ID edits.
- Planning choice: `pnpm backend:test:worktree`, backed by
  `scripts/backend-test-worktree.sh`, is the single opt-in entry point. Run
  from the checkout root through Nix or the existing `scripts/run.sh` wrapper.
  No arguments runs all tests; `--tests '<pattern>'` selects tests. Do not
  forward arbitrary Gradle tasks, profiles, or datasource flags.
- Resolve paths from this launcher's checkout, not the primary Git directory.
  Construct the JDBC URL using the existing local host/port and UTC parameters
  in `db-test.properties`. Keep existing local credentials and the test profile.
  Supply the same resolved target to migration and tests. Treat conflicting
  ordinary datasource overrides (`SPRING_DATASOURCE_URL` / `DB_URL`) or a
  separate conflicting Flyway URL as an error before launching either stage;
  matching values are harmless. Custom Spring configuration injection is
  outside this bounded workflow, not an isolation/security sandbox promise.
- Use a thin foreground shell launcher with Node only for JSON parsing, and
  `exec` one Gradle invocation requesting `migrateTestDB` then `test`. Keep both
  tasks on the same resolved environment. Supply a private opt-in Gradle
  property, `-PworktreeTestRun`, and apply `test.mustRunAfter(migrateTestDB)`
  only for that invocation. Ordinary task ordering and dependencies stay
  unchanged; normal `test` must not invoke migration or opt into configuration. Do not
  allow `--continue` or arbitrary tasks. Test execution must not be skipped as
  UP-TO-DATE or restored FROM-CACHE: pass `--rerun-tasks --no-build-cache` only
  in this workflow. Preserve tokens without interpolating IDs/patterns as code.
- Use `--no-daemon` for this opt-in run so it does not reuse another task's
  long-lived Gradle daemon. Gradle may still start its own single-use JVM.
  Delegate task failure and worker ownership to Gradle; do not create a Node
  process supervisor, detached app service, or machine-wide kill operation.
- Emit the selected database before executing. Native configuration errors
  and task failures remain nonzero, without fallback or database cleanup. An
  unmatched filter retains Gradle's failure. Verify the actual migration task's
  exit signal before calling this workflow's stage-gating proof complete.
- Cancellation means interrupting the owned foreground command (the usual
  terminal/process-group interrupt), including its test worker. Other worktrees
  and MySQL must remain usable. Signal behavior of the real Gradle invocation
  has a dedicated observation below; killing arbitrary ancestor PIDs or
  recovering after SIGKILL is not a new process-management feature.
- The existing commands and datasource defaults are unchanged. No controller,
  API, Flyway product migration, or generated client changes are intended.
  Existing `migrateTestDB` includes repair/migrate behavior; this story does not
  redesign it. Verify its real failure signal before relying on stage gating.
- Follow [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md): native
  errors may propagate; catch only to improve context or preserve the command
  lifecycle. No exhaustive test matrix for native JSON/filesystem exceptions.

## Execution context and evidence

- `package.json` separates the existing `migrateTestDB` and `backend:test_only`
  workflow; the existing combined command also formats. The new opt-in command
  does not need to format the user's source as part of test execution.
- `backend/src/main/resources/application.yml` resolves the test datasource
  from `db-test.properties`. `scripts/cloud_agent_setup.sh` already supplies
  `SPRING_DATASOURCE_URL` to test tooling; this is a configuration entry point
  to reuse, not evidence that this new workflow has been tested.
- `backend/build.gradle` configures `migrateTestDB` as a JavaExec task with the
  test profile and random HTTP port. `DonutTaskRunner` invokes Flyway repair and
  migrate. `ControllerTestBase` uses real transactional database interactions.
- `scripts/run.sh` supplies an existing shell `exec` pattern.
  `scripts/sut-start.test.mjs` and `scripts/sut-restart.test.mjs` demonstrate
  Node's built-in test runner and process fixtures. Reuse the style, not the
  detached SUT lifecycle or helper-by-helper test organization.
- Stable proof boundary: invoke the opt-in command as a process. A controllable
  external Gradle stand-in proves orchestration only; actual MySQL/Gradle runs
  in separate worktrees prove database independence. Do not call stubbed runs
  evidence of real migration or fixture isolation.
- Nix selects MySQL 8.4. Separate MySQL databases and normal existing migrations
  need no speculative storage experiment during planning. Record actual engine
  version and results with the real proofs below. If an uncertain new storage
  operation becomes necessary, perform the skill's isolated representative
  proof before declaring that affected work ready.

## Refinement assessment

The former six leaves were all planned; no completed evidence or implementation
was replaced. Former leaf 2 needed refinement: it combined configuration,
sequential process supervision, failure gating, and interruption. Former leaf 4
also mixed concurrency, fresh-shell reuse, and failure/cancellation trials.
The focused-selection, divergent-schema, and documentation outcomes remain,
with their proof ownership updated below.

Replace the standalone general process-fixture leaf with the smallest fixture
inside its first Behavior. Simplify the launch path to existing shell/Gradle
ownership instead of adding a process lifecycle abstraction. The only interim
behavior is explicit refusal of valid execution after the precondition slice;
the very next leaf removes it. No intermediate command reports tests passing
when it has not executed them.

## Ordered leaves

### 1. Refuse unusable worktree configuration before database work
Type: Behavior
Status: done
Proof: Invoke the new command in a temporary checkout fixture. Missing or
malformed configuration, invalid IDs, and conflicting ordinary URL overrides
exit nonzero before the fixture Gradle executable is reached. A valid ID shows
its derived database and explicitly reports that execution is not enabled yet;
matching URL overrides reach this same temporary refusal. Observe outcomes at
the command boundary, not helper return values. Use native parse/read errors
without adding exhaustive tests of Node itself.

Behavior: An operator invokes the opt-in command with unusable configuration →
the command refuses database work visibly and never chooses a fallback target.
Include the root-local config lookup, bounded ID mapping, root ignore entry,
package command, minimal command fixture, and short usage/temporary-refusal
note. Accept no command arguments yet. Add the command tests to the existing
check-only script-test coverage. Do not read the primary checkout's config.
Safe stopping point: valid runs also fail explicitly without starting database
work until leaf 2; existing commands continue to work as before.
Sizing: about 5 minutes, medium confidence; one synchronous precondition path
and command test loop, with no Gradle orchestration or background fixture.

### 2. Execute the full suite through the owned Gradle invocation
Type: Behavior
Status: done
Proof: Extend that command fixture with a controllable foreground Gradle
stand-in. Observe the same selected environment and checkout, ordered task
arguments, force-execution flags, and inherited output/exit status. A child
failure remains nonzero. Check the opt-in-only Gradle ordering declaration
without asserting that a stand-in proves real migration failure behavior.

Behavior: An operator supplies valid configuration → invokes the no-argument
command → one foreground Gradle run requests migration followed by the complete
suite against the selected database. Replace leaf 1's temporary refusal and
its corresponding assertion. Use shell `exec` and the task-order constraint
from Current decisions. Add no detached processes, per-stage Node listeners,
retry loops, or auto-provisioning. Configuration guards remain in force.
Safe stopping point: the full-suite workflow is available; focused filters
still reject. Failures are delegated to Gradle, never turned into success.
Sizing: about 5 minutes, medium confidence; one exec handoff plus one task-order
constraint, using leaf 1's proven configuration and fixture. If Gradle requires
custom supervision, stop and refine this leaf rather than extending its scope.

### 3. Select focused tests without changing the configured database
Type: Behavior
Status: done
Proof: Command-boundary fixture receives a literal `--tests` pattern on the
`test` task, with migration and datasource arguments unchanged. Wildcards stay
one token; missing values and unrelated task/target arguments refuse execution.
A no-match child failure stays a failure. Do not reassert every canonical URL
or default invocation field in each variant.

Behavior: The operator supplies one supported filter → the command requests
that test selection after migration using the same worktree database. Update
its usage example. Focused support is product behavior; actual backend
verification below still runs all tests as required by backend rules.
Sizing: about 5 minutes, high confidence; one argument-selection proof loop.

### 4. Prepare the two disposable environments for the real concurrent run
Type: Structure
Status: done
Proof: Two disposable worktrees contain the implementation and installed normal
checkout dependencies; each configured database is reachable by the local test
user and its root config is ignored. Record worktree paths, distinct IDs,
provisioning commands, engine version, and initial database state in this PLAN.
No product tests or legacy databases are modified by setup.

Internal change: Prepare only the two manually provisioned environments needed
immediately by leaf 5, using normal Git, Nix, and SQL tools. Use the existing
character set, collation, and local user grants from the project's init SQL.
Do not create a reusable environment framework, automatic ID allocator, or a
new production provisioning command. Immediate next Behavior: leaf 5.
Sizing: about 5 minutes active setup, medium confidence; dependency installation
or Nix downloads are external waits, not hidden implementation work. If setup
requires a new harness, refine this leaf. Preserve these disposable environments
for subsequent proofs rather than repeating setup or deleting evidence.

Recorded setup (leave these environments in place for leaves 5–8; do not delete):

- Source checkout: `/Users/terryyin/git/doughnut-wt-054` remained on
  `feat/configured-worktree-backend-tests` at
  `49d4f305448deecacd7f2527a42656aaf1ed69b4`.
- Worktree A: `/Users/terryyin/git/doughnut-wt-054-a` (detached HEAD at the same
  commit). ID `wt_054a` → database `doughnut_wt_054a_test`.
- Worktree B: `/Users/terryyin/git/doughnut-wt-054-b` (detached HEAD at the same
  commit). ID `wt_054b` → database `doughnut_wt_054b_test`.

Provisioning commands (from `/Users/terryyin/git/doughnut-wt-054` unless noted):

```
git worktree add --detach /Users/terryyin/git/doughnut-wt-054-a
git worktree add --detach /Users/terryyin/git/doughnut-wt-054-b
```

```
CURSOR_DEV=true nix develop -c mysql -h127.0.0.1 -P3309 -u root
```

SQL (new databases only; doughnut password unchanged):

```
CREATE DATABASE doughnut_wt_054a_test DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE doughnut_wt_054b_test DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON doughnut_wt_054a_test.* TO 'doughnut'@'localhost';
GRANT ALL PRIVILEGES ON doughnut_wt_054a_test.* TO 'doughnut'@'127.0.0.1';
GRANT ALL PRIVILEGES ON doughnut_wt_054b_test.* TO 'doughnut'@'localhost';
GRANT ALL PRIVILEGES ON doughnut_wt_054b_test.* TO 'doughnut'@'127.0.0.1';
FLUSH PRIVILEGES;
```

Root config in each worktree: A `.worktree.local.json` is `{"id":"wt_054a"}`;
B is `{"id":"wt_054b"}`. `git check-ignore -v .worktree.local.json` in both:

```
.gitignore:158:/.worktree.local.json	.worktree.local.json
```

Dependencies (from each worktree; no backend tests run):

```
CURSOR_DEV=true nix develop -c pnpm install
```

A finished in 11.1s and B in 10.8s (pnpm v11.26.0; lockfile up to date; 1285
packages). Each worktree has `scripts/backend-test-worktree.sh`, `backend/`,
and `node_modules/`. Worktree working trees stayed clean (config ignored).

Engine and initial database state (`SELECT VERSION()` / `SELECT 1` /
`SHOW DATABASES` as `doughnut` over TCP, selected target = the new DB, never
a legacy name):

- Engine: MySQL `8.4.11` on `127.0.0.1:3309`.
- `doughnut_wt_054a_test` and `doughnut_wt_054b_test`: charset `utf8mb4`,
  collation `utf8mb4_unicode_ci`, `table_count` 0, no
  `flyway_schema_history` table (empty initial state).
- Legacy databases unmodified vs pre-setup snapshot:
  `doughnut_test` 42 tables / 20 flyway rows / `utf8mb4_unicode_ci`;
  `doughnut_e2e_test` 42 tables / 20 flyway rows / `utf8mb4_0900_ai_ci`;
  `doughnut_development` 0 tables / `utf8mb4_unicode_ci`. No DROP or writes
  to those names.

### 5. Obtain independent full-suite results in two configured worktrees
Type: Behavior
Status: done
Proof: Overlap complete suite runs in the leaf-4 worktrees, including the same
fixture names. Capture literal commands, start/end overlap, selected targets,
active MySQL database connections, per-database Flyway history, and JUnit
counts/results. Require actual Gradle test execution, not cached or up-to-date
results. Both worktrees must obtain correct results against their own code.

Behavior: Two configured worktrees run at the same time → both verify their
backend code without sharing schema history or test fixtures. This is the
real-engine proof of the command, not another runner implementation. Repair
only a demonstrated breach of this boundary within scope. If other shared
state or capacity prevents the promised concurrency, return to story review;
do not serialize runs or skip failing tests to claim isolation.
Sizing: about 5 minutes active launch/observation, medium confidence. Full-suite
and cold-build duration is an explicit test-runtime exception. Use the prepared
worktrees; this leaf does not include their creation or a failure trial.

Recorded overlapping run (2026-09-07; not accepted as leaf completion):

```
# in each disposable worktree, started within the same second
unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL
CURSOR_DEV=true nix develop -c pnpm backend:test:worktree
```

- A `/Users/terryyin/git/doughnut-wt-054-a` started 2026-09-07T09:09:26Z,
  selected `doughnut_wt_054a_test`, `migrateTestDB` succeeded, ended
  09:10:42Z (`BUILD FAILED`, 1m 14s, 7 tasks executed, not cache).
- B `/Users/terryyin/git/doughnut-wt-054-b` started 2026-09-07T09:09:26Z,
  selected `doughnut_wt_054b_test`, `migrateTestDB` succeeded, ended
  09:10:42Z (`BUILD FAILED`, 1m 14s, 7 tasks executed). Runs overlapped
  for the whole duration.
- At 09:10:09Z both databases had active `doughnut` sessions (on the order
  of 90 connections each, including live SELECTs on both).
- Each run: `2246 tests completed, 4 failed` —
  `StructuredResponseCreateParamsSerializerTest` context load,
  Flyway/`entityManagerFactory`, MySQL 1040 `Too many connections`.
- Server: MySQL 8.4.11 `max_connections=300`. Test profile uses distinct
  Spring contexts with Hikari pools; two concurrent suites exhausted the
  shared server.
- Diagnostic solo rerun on A at 09:12:55Z: same command,
  `BUILD SUCCESSFUL` in 1m 2s, 7 tasks executed. Flyway history after
  both overlap migrations: 22 rows on A and on B; legacy `doughnut_test`
  also 22 (untouched as the selected target).

Schema isolation held far enough to migrate and run most tests against
distinct databases. Shared connection capacity blocked the promised
concurrent correct results until the instance ceiling was raised.

Accepted overlapping retry (2026-09-07T09:47Z) after `scripts/mysql_nix_shared.sh`
starts mysqld with `--max-connections=1000` (running instance reports 1000):

```
# in each disposable worktree
unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL
CURSOR_DEV=true nix develop -c pnpm backend:test:worktree
```

- A started 2026-09-07T09:47:02Z, selected `doughnut_wt_054a_test`,
  `migrateTestDB` succeeded, ended 09:48:12Z (`BUILD SUCCESSFUL` in 1m 8s,
  `7 actionable tasks: 7 executed`, exit 0).
- B started 2026-09-07T09:47:03Z, selected `doughnut_wt_054b_test`,
  `migrateTestDB` succeeded, ended 09:48:14Z (`BUILD SUCCESSFUL` in 1m 9s,
  `7 actionable tasks: 7 executed`, exit 0). Runs overlapped for the whole
  duration.
- JUnit XML: A 2246 tests / 0 failures / 0 errors / 0 skipped (441 files);
  B the same. Sample suite timestamps overlap (A 09:47:36Z, B 09:47:38Z).
- Flyway: 22 rows on A and on B (unchanged from the first overlap
  migrations). Both databases have 42 tables. Legacy `doughnut_test` still
  22 Flyway rows / 42 tables and was not the selected target.
- `performance_schema.events_statements_summary_by_digest`: 37294 statements
  on `doughnut_wt_054a_test`, 37268 on `doughnut_wt_054b_test`, 244 on
  `doughnut_test`. A live `SHOW PROCESSLIST` sampler this retry failed
  (`mysql` not on PATH outside Nix); concurrent distinct-DB use is shown by
  the overlapping JUnit timestamps, per-schema statement counts, and the
  earlier 09:10:09Z processlist (~90 sessions on each selected database).

### 6. Stop before tests when real database preparation fails
Type: Behavior
Status: done
Proof: In a disposable configured worktree, temporarily select a fresh valid ID
whose database has not been created. Invoke the real workflow. Capture the
migration task's failure, nonzero command status, and that Gradle never starts
`test`; legacy databases are not selected. Restore the saved valid local
configuration without deleting or recreating any database.

Behavior: The selected database cannot be migrated because it is absent → the
command fails before executing tests, with no fallback. This specifically
checks the existing migration task's real exit contract. Do not infer it from
a stubbed Gradle exit. If that task masks its failure, record the actual path
and refine a bounded correction before continuing; do not redesign all task
error handling or change unrelated API-document generation.
Sizing: about 5 minutes active work, medium confidence; one failure invocation
on the already prepared environment, with process startup as an external wait.
No newly generated invalid product migration is required.

Recorded missing-database run in A (`/Users/terryyin/git/doughnut-wt-054-a`):
saved `{"id":"wt_054a"}`, wrote `{"id":"wt_054missing"}` (DB confirmed absent),
then:

```
unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL
CURSOR_DEV=true nix develop -c pnpm backend:test:worktree
```

Selected `doughnut_wt_054missing_test`. `:migrateTestDB FAILED` (JVM exit 1)
during Flyway/Spring startup with MySQL 1044 access denied to that missing
database. No `> Task :test`. `BUILD FAILED in 9s`, `4 actionable tasks: 4 executed`
(full suite is 7). Command exit 1. Did not select `doughnut_wt_054a_test` or
`doughnut_test`. Restored A's config to `{"id":"wt_054a"}`. Missing DB still
absent; A 42 tables / 22 Flyway; legacy `doughnut_test` 42 / 22. No product
code change; no stage-gating correction.

### 7. Interrupt one owned run while another environment remains usable
Type: Behavior
Status: done
Proof: Reuse the configured worktrees. While A has an active real Gradle test
worker, interrupt A's owned foreground run through its terminal/process group.
Observe termination of A's active worker/run, failing interruption status,
and B's continuing independent run or successful database operation. Confirm
MySQL remains available. Do not equate a sent signal or `.killed` flag with
observed process termination, and do not kill by common process name or port.

Behavior: The operator cancels A → A stops without stopping shared MySQL or B.
The implementation delegates lifecycle to foreground Gradle; this observation
validates that delegation. If a worker survives, refine a scoped ownership fix
before claiming the condition met. Do not turn this into a background process
manager, broad signal compatibility feature, or cleanup of unrelated daemons.
Sizing: about 5 minutes active launch/observation, medium confidence; waiting
for an actual test worker is an external runtime exception. Existing prepared
environments suffice; no new lifecycle harness is expected.

Recorded interrupt (2026-09-07): A and B ran

```
unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL
CURSOR_DEV=true nix develop -c pnpm backend:test:worktree
```

- A started 10:05:30Z, selected `doughnut_wt_054a_test`, migrate succeeded.
  At 10:05:46Z `Gradle Test Executor 1` PID 19755 was live for A's tests
  (tmpdir `doughnut-wt-054-a/backend/build/tmp/test/work`). SIGINT to A's
  owned process group `-18831` (wrapper/pnpm/gradlew). A ended 10:05:47Z;
  pnpm `[ELIFECYCLE] Command failed with exit code 130`. Worker 19755 gone;
  group 18831 empty at +5s. Log stopped after `:testClasses` (no BUILD).
- B started 10:05:41Z, selected `doughnut_wt_054b_test`, still running after
  A's interrupt (gradlew + B's Test Executor). Ended 10:06:46Z
  `BUILD SUCCESSFUL` in 1m 4s, `7 actionable tasks: 7 executed`, exit 0.
- MySQL `SELECT 1` succeeded after A's stop; Flyway still 22 rows on A and B.
  Unrelated primary-checkout Gradle processes were not killed.

### 8. Keep migration history independent on a later invocation
Type: Behavior
Status: done
Proof: In A's disposable worktree only, add a temporary valid next-version
migration creating one benign proof table; B retains the previous code. Open
fresh shells and run both complete suites concurrently. Observe that their
existing configured targets are reused, A alone gains the table/Flyway entry,
and both suites pass. Record commands, target and schema observations, and
actual test results. Do not commit the temporary migration to the product or
edit any committed migration.

Behavior: A changes its migration set and invokes tests again → only A's
existing database evolves while B verifies its original schema. This one repeat
invocation also proves fresh-shell configuration reuse. Existing schema
incompatibility may fail; do not add automatic rollback or destructive repair.
Sizing: about 5 minutes active work, medium confidence; reuse previous setup.
Complete-suite runtime is an explicit exception. The ordinary isolated proof
table is not a production schema change or a new transactional DDL contract.

Recorded later invocation (2026-09-07): uncommitted
`V300000321__worktree_concurrency_proof.sql` in A only (not in B, not committed).
Fresh shells:

```
unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL
CURSOR_DEV=true nix develop -c pnpm backend:test:worktree
```

- A 10:09:43Z–10:10:52Z selected `doughnut_wt_054a_test`, `BUILD SUCCESSFUL`
  1m 7s, 7 executed, 2246/0. Flyway 23; version `300000321` at 18:09:54;
  table `wt_054_concurrency_proof` present. Config still `{"id":"wt_054a"}`.
- B 10:09:44Z–10:10:53Z selected `doughnut_wt_054b_test`, `BUILD SUCCESSFUL`
  1m 8s, 7 executed, 2246/0. Flyway 22; no `300000321`; no proof table.
- Incidental: `QuestionGenerationBatchMaintenanceConcurrencyTest` hardcodes
  `doughnut_test`, so Flyway also applied `300000321` there at 18:10:41.
  Restored `doughnut_test` (drop table, delete that Flyway row → 22 rows).
  `doughnut_e2e_test` untouched. Temp SQL removed from A after the run.

### 9. Repeat the supported workflow from its setup instructions
Type: Behavior
Status: planned
Proof: Cross-check the guide with setup commands and target observations from
leaves 4–8; verify its agent-map link. Show the full and focused forms and the
explicit limitation on legacy commands. Documentation alone does not require
another round of backend test runs.

Behavior: A developer follows the manual instructions → can prepare and repeat
the proven opt-in workflow without hidden configuration. Document choosing a
unique ID, creating database/grants with the existing collation, writing ignored
config, and invoking tests. State that duplicate manual IDs are operator error,
old commands are not automatically isolated, and no cleanup is automatic.
Sizing: about 5 minutes, high confidence; document observed behavior only.

## Contract coverage and verification

| Promise / story example | Owner | Observable evidence |
|---|---|---|
| One reusable ID, bounded database name, checkout-local lookup | 1, 8 | Command-boundary selection and same target on real repeat invocation |
| Configuration is gitignored | 1, 4 | Root ignore entry and `git check-ignore` in configured disposable worktrees |
| Visible target and same environment for migration/full tests | 2, 5 | Exec boundary and real database connection/history observations |
| Test results reflect execution, not cache reuse | 2, 5 | Opt-in flags and actual Gradle/JUnit output |
| Independent concurrent fixtures and code verification | 5 | Overlapping full-suite runs in separate configured worktrees |
| Different branch migration histories remain independent | 8 | A-only proof table/history, both full suites pass |
| Focused selection without arbitrary task/config forwarding | 3 | Literal filter arguments and refused unsupported input |
| Native configuration errors and contradictory targets do not fall back | 1 | Refusal before external command invocation |
| Failed preparation stops before tests | 2, 6 | Task ordering/failure delegation and real missing-database failure |
| Test/no-match failure stays nonzero | 2, 3 | External command failure exits through the foreground launcher |
| Owned completion/cancellation; other runs/MySQL survive | 2, 7 | Exec handoff and observed real worker termination/other-environment availability |
| Existing commands/defaults and non-test databases unaffected | 2, 5–8 | Conditional ordering only; scoped diff and real selected targets, with no legacy-target writes or service-stop operations |
| Manual setup and limits are reviewable | 9 | Guide matches recorded commands, mapping, and excluded behaviors |

Focused script verification after relevant changes:
`CURSOR_DEV=true nix develop -c node --test scripts/backend-test-worktree.test.mjs`.
Real verification uses `CURSOR_DEV=true nix develop -c pnpm backend:test:worktree`
in each disposable worktree. Record literal commands and observations in this
PLAN when executed. Backend rules require complete suites; filter-support tests
and fake Gradle processes do not replace real backend verification. Do not add
browser testing, mutation testing, API generation, or a benchmark project.

The real runs each answer a distinct unresolved promise: normal concurrency,
preparation failure, cancellation, and independent schema evolution. Reuse
observations when a run already covers a later promise; mark evidence at its
owning leaf rather than rerunning unchanged suites just to follow numbering.
Do not repeat successful verification after documentation-only edits.

Use execute-plan's wrap-up when execution is separately authorized: Jidoka,
fresh post-change-refactor agent, coordinator's one changed-component formatting
pass, PLAN update, commit/check-only hook, and push. Preserve unrelated ongoing
CLI work and its plan. Planning alone does not trigger that delivery workflow.

## Readiness and learnings

Slice 1: command-boundary fixture proves missing/malformed/invalid IDs and
conflicting URL overrides refuse before a fixture Gradle wrapper; a valid ID
prints `doughnut_<id>_test` and the temporary execution refusal. Native
JSON/ENOENT errors propagate. No remaining-slice adjustment.

Slice 2: the same fixture's Gradle stand-in records one exec of migrateTestDB
then test with `-PworktreeTestRun`, `--rerun-tasks --no-build-cache --no-daemon`,
and the resolved JDBC URL; child failure stays nonzero. `mustRunAfter` is
gated on the opt-in property. Temporary refusal is gone.

Slice 3: `--tests '<pattern>'` is one Gradle token after `test`; missing values
and unrelated args refuse before Gradle; unmatched filter keeps child failure.

Slice 4: disposable worktrees `/Users/terryyin/git/doughnut-wt-054-a` (`wt_054a`)
and `/Users/terryyin/git/doughnut-wt-054-b` (`wt_054b`) with empty utf8mb4
databases on MySQL 8.4.11; configs ignored; legacy DBs unmodified. Preserve
them for leaves 5–8.

Slice 8: A-only V300000321 created `wt_054_concurrency_proof` (Flyway 23);
B stayed at 22. Both suites passed. doughnut_test incidental write restored.
Next: leaf 9.

If an actual run contradicts a lifecycle/storage assumption, preserve its
observations and stop at that leaf for the repository's learning escalation.
Remove spent planning history only after the complete story is delivered.
