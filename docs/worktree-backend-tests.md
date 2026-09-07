# Isolated backend tests for a local checkout

Use `pnpm backend:test:worktree` when a local checkout (including a Git
worktree) should migrate and run backend unit tests against its own MySQL
database instead of the shared `doughnut_test` default.

MySQL must already be listening on `127.0.0.1:3309` with the established
passwordless local root administration and the existing `doughnut` test user.
The command does not start MySQL and does not clean up databases afterward.

## First use (automatic, primary workflow)

Just run the command in any fresh local checkout — no setup step required.
Unset conflicting datasource URLs first, since a conflicting
`SPRING_DATASOURCE_URL`, `DB_URL`, or `SPRING_FLYWAY_URL` refuses before
launch:

```bash
unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL
CURSOR_DEV=true nix develop -c pnpm backend:test:worktree
CURSOR_DEV=true nix develop -c pnpm backend:test:worktree --tests '<pattern>'
```

When this checkout has no `.worktree.local.json` yet, the command:

1. Generates an identity matching `wt_[a-z0-9_]{1,32}` (`wt_` plus 32 lowercase
   hexadecimal characters).
2. Creates database `doughnut_<id>_test` with `utf8mb4` /
   `utf8mb4_unicode_ci`, then grants the existing `doughnut` user access at
   both `localhost` and `127.0.0.1`. A generated identity that collides with
   an existing database fails visibly and is never adopted.
3. Persists `{"id":"<id>"}` to gitignored `.worktree.local.json` (ignore entry
   `/.worktree.local.json`) only after provisioning succeeds, printing
   `Allocated new worktree environment: <id>`. Git also ignores the leftover
   checkout lock with `/.worktree.local.lock`.

The command then prints `Selected database: doughnut_<id>_test`, migrates it,
and runs the requested tests (the full form runs the complete suite; the
`--tests` form keeps the same database and filters tests). Repository-wrapper
`migrateTestDB` and `test` in a fresh Git linked worktree allocate the same
way (see Ordinary commands in a fresh linked worktree).

Later invocations in the same checkout find `.worktree.local.json` already
present, skip provisioning entirely (no database administration call, no
identity change), and reuse the same database — from a fresh shell, after a
restart, or from a different terminal, as long as it is the same checkout on
disk.

## Explicit configuration (compatibility path)

Hand-writing `.worktree.local.json` remains supported, for example to reuse a
specific already-provisioned database (including one created by an earlier
automatic first use, or a one-time manual database setup):

1. Choose an identity matching `wt_[a-z0-9_]{1,32}` (for example `wt_a7c2`).
   Two checkouts that reuse an ID share a database; that is operator error.
2. If the database doesn't already exist, create an empty
   `doughnut_<id>_test` with the project's test collation and grant the
   existing `doughnut` user:

```bash
CURSOR_DEV=true nix develop -c mysql -h127.0.0.1 -P3309 -u root
```

```sql
CREATE DATABASE doughnut_wt_a7c2_test DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON doughnut_wt_a7c2_test.* TO 'doughnut'@'localhost';
GRANT ALL PRIVILEGES ON doughnut_wt_a7c2_test.* TO 'doughnut'@'127.0.0.1';
FLUSH PRIVILEGES;
```

3. At this checkout's root, write gitignored `.worktree.local.json`:

```json
{"id":"wt_a7c2"}
```

Replace `wt_a7c2` / `doughnut_wt_a7c2_test` with the chosen identity. Confirm
the ignore with `git check-ignore -v .worktree.local.json`. The leftover
checkout lock is ignored the same way (`/.worktree.local.lock`).

With this config present, the command selects it directly: it never creates
the database, never replaces the identity, and does not repair a missing or
inaccessible explicitly configured database — it fails rather than being
silently provisioned or adopted.

## One invocation at a time per checkout

Regardless of which workflow started it, only one worktree test invocation
runs at a time per checkout: an overlapping second command in the same
checkout refuses immediately rather than sharing the run. If a previous
invocation's process has exited without releasing ownership, the next
invocation reclaims it automatically. Commands in different checkouts remain
fully independent and may run concurrently.

## Ordinary migration in a configured checkout

When `.worktree.local.json` is already present, the repository Gradle wrapper
migrates the assigned database and does not run tests:

From the checkout root:

```bash
unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL
CURSOR_DEV=true nix develop -c backend/gradlew -p backend migrateTestDB
```

From `backend/`:

```bash
unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL
CURSOR_DEV=true nix develop -c ./gradlew migrateTestDB
```

These commands reuse the checkout's assigned database and the same exclusive
invocation lock as `pnpm backend:test:worktree`. They do not allocate a
first-use environment.

## Ordinary tests in a configured checkout

When `.worktree.local.json` is already present, the repository Gradle wrapper
migrates the assigned database once, then runs the requested tests against it
with the test profile and actual-run flags (`--rerun-tasks --no-build-cache
--no-daemon`). A failed migration stops the command before tests start, even
when the caller passed `--continue`.

From the checkout root:

```bash
unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test --tests '<pattern>'
```

From `backend/`:

```bash
unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL
CURSOR_DEV=true nix develop -c ./gradlew test
CURSOR_DEV=true nix develop -c ./gradlew test --tests '<pattern>'
```

These commands reuse the checkout's assigned database and the same exclusive
invocation lock as `pnpm backend:test:worktree`. They do not allocate a
first-use environment. The caller does not need to pass
`-Dspring.profiles.active=test`.

## Ordinary commands in a fresh linked worktree

A Git linked worktree (`git worktree add`) with no `.worktree.local.json` yet
allocates its persistent isolated database on the first repository-wrapper
`migrateTestDB` or `test` command, then uses that database for the workload.
Allocation follows the same first-use steps as `pnpm backend:test:worktree`
above. Migration-only does not run tests; `test` migrates once then runs
tests, as in a configured checkout.

From the checkout root:

```bash
unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL
CURSOR_DEV=true nix develop -c backend/gradlew -p backend migrateTestDB
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test
```

Later invocations in the same linked checkout skip provisioning and reuse the
same identity.

## Ordinary commands on a primary checkout

A **primary** checkout is a normal clone or the main worktree: `.git` is a
directory (`git-dir` equals `git-common-dir`). Topology alone does not isolate
it.

- **Unconfigured** (no `.worktree.local.json`): repository-wrapper
  `migrateTestDB` and `test` keep the established default target. They do not
  allocate an identity, do not create `.worktree.local.json`, and do not take
  the checkout lock. An explicit `SPRING_DATASOURCE_URL` or `DB_URL` stays as
  the caller set it. Existing CI-shaped forms such as
  `backend/gradlew -p backend test -Dspring.profiles.active=test --build-cache --parallel`
  (`pnpm backend:test_only`) stay on this path.
- **Configured** (`.worktree.local.json` already present): `migrateTestDB` and
  `test` use that assigned ID, as in Ordinary migration / Ordinary tests
  above.

Unrelated wrapper tasks (`spotlessApply`, `help`, `generateOpenAPIDocs`, and
other non-`test` / non-`migrateTestDB` tasks) never allocate, never set a
worktree datasource URL, and never take the lock — including in a configured
primary or a linked worktree.

## Limits

- An unconfigured primary checkout keeps the established default
  (`doughnut_test`) and any caller-supplied URL; it does not allocate.
  `pnpm backend:test` and `pnpm backend:test_only` follow that path.
  Configured-checkout and fresh linked-worktree `migrateTestDB` and `test`
  through the repository wrapper are isolated as described above.
- No automatic cleanup, retirement, or orphan removal: a provisioned database
  (and an unreferenced one left behind by a failed or interrupted first use)
  persists until removed manually. There is no machine-wide allocation
  registry and no recovery from duplicate operator-supplied IDs.
- Conflicting `SPRING_DATASOURCE_URL` / `DB_URL` / `SPRING_FLYWAY_URL`, or a
  conflicting Gradle `-Dspring.datasource.url=` / `-Dspring.flyway.url=`,
  refuse before launch. Matching URLs remain valid.
- Concurrent full suites on one mysqld need enough `max_connections`. Local
  nix mysqld starts with 1000 via `scripts/mysql_nix_shared.sh`. Do not
  serialize concurrent suites as the supported workaround.
- Cancellation interrupts the owned foreground command (the usual
  terminal/process-group interrupt).
