# Isolated backend tests for a local checkout

Use `pnpm backend:test:worktree` when a local checkout (including a Git
worktree) should migrate and run backend unit tests against its own MySQL
database instead of the shared `doughnut_test` default.

MySQL must already be listening on `127.0.0.1:3309`. The command does not
start MySQL, create databases, allocate identities, or clean up afterward.

## Setup

1. Choose a unique identity matching `wt_[a-z0-9_]{1,32}` (for example
   `wt_a7c2`). Two checkouts that reuse an ID share a database; that is
   operator error.

2. Create an empty database named `doughnut_<id>_test` with the project's
   test collation, then grant the existing `doughnut` user. Do not invent
   credentials.

```bash
CURSOR_DEV=true nix develop -c mysql -h127.0.0.1 -P3309 -u root
```

```sql
CREATE DATABASE doughnut_wt_a7c2_test DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON doughnut_wt_a7c2_test.* TO 'doughnut'@'localhost';
GRANT ALL PRIVILEGES ON doughnut_wt_a7c2_test.* TO 'doughnut'@'127.0.0.1';
FLUSH PRIVILEGES;
```

Replace `wt_a7c2` / `doughnut_wt_a7c2_test` with the chosen identity.

3. At this checkout's root, write gitignored `.worktree.local.json`:

```json
{"id":"wt_a7c2"}
```

The ignore entry is `/.worktree.local.json`. Confirm with
`git check-ignore -v .worktree.local.json`.

## Invoke

From this checkout root. Git has no Nix prefix; other tooling does. Unset
conflicting datasource URLs first — a conflicting `SPRING_DATASOURCE_URL`,
`DB_URL`, or `SPRING_FLYWAY_URL` refuses before launch.

```bash
unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL
CURSOR_DEV=true nix develop -c pnpm backend:test:worktree
CURSOR_DEV=true nix develop -c pnpm backend:test:worktree --tests '<pattern>'
```

The full form migrates then runs the complete suite against the selected
database. The focused form keeps the same database. The command prints
`Selected database: doughnut_<id>_test` before Gradle runs.

## Limits

- `pnpm backend:test`, `pnpm backend:test_only`, and direct Gradle `test` /
  `migrateTestDB` are not automatically isolated; they keep the legacy default
  (`doughnut_test`).
- No automatic database creation, identity allocation, or cleanup.
- Conflicting `SPRING_DATASOURCE_URL` / `DB_URL` / `SPRING_FLYWAY_URL` refuse
  before launch.
- Concurrent full suites on one mysqld need enough `max_connections`. Local
  nix mysqld starts with 1000 via `scripts/mysql_nix_shared.sh`. Do not
  serialize concurrent suites as the supported workaround.
- Cancellation interrupts the owned foreground command (the usual
  terminal/process-group interrupt).
