# Isolated browser tests for a local checkout

Use ordinary `pnpm sut` in a local checkout (including a Git worktree) when you
want the application to run against its own E2E database and ports instead of
the shared `doughnut_e2e_test` / 5173 / 5174 / 9081 defaults.

This is a **temporary manual identity**. Later work will initialize identity
automatically. Do not treat this file as the final workflow. Already recorded
E2E databases and application ports are preserved.

MySQL must already be listening on `127.0.0.1:3309` with the established
passwordless local root administration and the existing `doughnut` test user.
Isolated start does not start MySQL and does not start Mountebank. On first
`pnpm sut` with an existing identity, it prepares `doughnut_e2e_<id>` (CREATE +
GRANT, no `IF NOT EXISTS`) and allocates three distinct application ports.
Allocated ports are never 5173, 5174, 9081, or 2525. It does not create or
repair a database that is already recorded, adopt a name that already exists
in MySQL, or modify `doughnut_test` / `doughnut_e2e_test`.

`pnpm sut:restart` replaces the idle live owner on this checkout's recorded
allocation. Ordinary Cypress is supported only for the focused note-editing
spec below.

## Temporary manual identity

1. Choose an identity matching `wt_[a-z0-9_]{1,32}` (for example `wt_a7c2`).
   Reuse this checkout's existing `.worktree.local.json` `id` when backend
   tests already created one. Do not change that identity or repair its
   unit-test database (`doughnut_<id>_test`).
2. At this checkout's root, write gitignored `.worktree.local.json` if it is
   missing. Identity-only `{ "id": "wt_a7c2" }` is enough for `pnpm sut` and
   remains valid for backend tests. Omit `e2e.database` and the three ports on
   first use; start records `doughnut_e2e_<id>` and the allocated ports after
   they succeed. If ports are already recorded, first use keeps them.

```json
{
  "id": "wt_a7c2"
}
```

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
start in the same checkout while that owner is live is refused. A name that
already exists in MySQL before this checkout records it is a collision, not
adoption. A recorded missing database, occupied port, or migration failure is
not permission to adopt, delete, rebuild, or renumber.

`pnpm sut:healthcheck` and `pnpm sut:restart` need the complete recorded
allocation after that first start. Health verifies the live owner first. A
foreign process that happens to answer on the recorded ports is not a healthy
owning stack.

## Restart

With that owning SUT idle (no Cypress runner lease), restart it in place.
The recorded identity, database, and ports stay the same:

```bash
CURSOR_DEV=true nix develop -c pnpm sut:restart
```

Restart asks the verified live owner to stop its own children, holds the
lifecycle claim across stop/start, and starts again on the same allocation.
It does not discover or signal listeners by port. A second restart, a Cypress
run in progress, or a stale/unverifiable owner record is refused before any
process is signalled. After a refused stale owner, use `pnpm sut` once the
claim can be reclaimed.

## Focused Cypress run

With that owning SUT healthy, run only this spec — mixed or other features are
refused before reset:

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_creation_and_update/worktree_note_editing.feature
```

Cypress node configuration sets `baseUrl` to this checkout's browser origin
(`http://127.0.0.1:<lbListenPort>`). The browser uses that origin for named
navigation and for the Before-order-0 testability reset; it does not read local
files. A second Cypress runner, or `CYPRESS_baseUrl` / `baseUrl` that does not
match the allocated origin, is refused before reset. Completion or cancellation
releases the runner so a later run can start. Restart while that runner is
held is refused.

Primary checkouts without `.worktree.local.json` keep the shared local defaults
documented in `docs/gcp/prod_env.md`, including Cypress origin
`http://localhost:5173`. A primary checkout that has local identity uses
isolation.
