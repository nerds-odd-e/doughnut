# Isolated browser tests for a local checkout

Use ordinary `pnpm sut` in a local checkout (including a Git worktree) when you
want the application to run against its own E2E database and ports instead of
the shared `doughnut_e2e_test` / 5173 / 5174 / 9081 defaults.

This is a **temporary manual allocation**. Later work will create the E2E
database, choose ports, and initialize identity automatically. Do not treat
this file as the final workflow.

MySQL must already be listening on `127.0.0.1:3309` with the established
passwordless local root administration and the existing `doughnut` test user.
Isolated start does not start MySQL, does not create or repair a recorded
database, and does not start Mountebank.

`pnpm sut:restart` and ordinary Cypress runs remain refused in isolated
checkouts until later work enables them.

## Temporary manual allocation

1. Choose an identity matching `wt_[a-z0-9_]{1,32}` (for example `wt_a7c2`).
   Reuse this checkout's existing `.worktree.local.json` `id` when backend
   tests already created one. Do not change that identity or repair its
   unit-test database (`doughnut_<id>_test`).
2. Create an empty E2E database and grant the existing `doughnut` user. Use a
   name that is not `doughnut_e2e_test` or another checkout's database:

```bash
CURSOR_DEV=true nix develop -c mysql -h127.0.0.1 -P3309 -u root
```

```sql
CREATE DATABASE doughnut_e2e_wt_a7c2 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON doughnut_e2e_wt_a7c2.* TO 'doughnut'@'localhost';
GRANT ALL PRIVILEGES ON doughnut_e2e_wt_a7c2.* TO 'doughnut'@'127.0.0.1';
FLUSH PRIVILEGES;
```

3. Pick three free application ports (backend, Vite, local load balancer). Do
   not reuse 5173 / 5174 / 9081 if the primary checkout's SUT is running. Port
   2525 (Mountebank) is not part of isolated start.
4. At this checkout's root, write gitignored `.worktree.local.json`. Identity-only
   `{ "id": "wt_a7c2" }` files stay valid for backend tests but refuse `pnpm sut`
   until the `e2e` fields are present:

```json
{
  "id": "wt_a7c2",
  "e2e": {
    "database": "doughnut_e2e_wt_a7c2",
    "backendPort": 19081,
    "vitePort": 15174,
    "lbListenPort": 15173
  }
}
```

Replace the identity, database name, and ports with the values you allocated.
Confirm the ignore with `git check-ignore -v .worktree.local.json`.

## Start and health

Unset conflicting overrides first. Matching values for the assigned target
remain usable; a different datasource, Flyway URL, proxy, or server port
refuses before spawn:

```bash
unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL INPUT_DB_URL SERVER_PORT \
  LOCAL_LB_BACKEND LOCAL_LB_VITE_UPSTREAM LOCAL_LB_LISTEN_PORT \
  FRONTEND_DEV_PORT FRONTEND_BACKEND_ORIGIN SUT_RUNTIME_TARGET
CURSOR_DEV=true nix develop -c pnpm sut
CURSOR_DEV=true nix develop -c pnpm sut:healthcheck
```

`pnpm sut` prints `Selected database:` and `Browser origin:` for the recorded
allocation, starts backend + Vite + the local load balancer (no Mountebank),
and waits until this checkout's live owner is healthy on those ports. A second
start in the same checkout while that owner is live is refused. A recorded
missing database, occupied port, or migration failure is not permission to
adopt, delete, rebuild, or renumber.

`pnpm sut:healthcheck` verifies the live owner first. A foreign process that
happens to answer on the recorded ports is not a healthy owning stack.

Primary checkouts without `.worktree.local.json` keep the shared local defaults
documented in `docs/gcp/prod_env.md`. A primary checkout that has local
identity uses isolation.
