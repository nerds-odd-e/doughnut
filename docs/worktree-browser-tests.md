# Isolated browser tests for a local checkout

Use ordinary `pnpm sut` in a local checkout (including a Git worktree) when you
want the application to run against its own E2E database and ports instead of
the shared `doughnut_e2e_test` / 5173 / 5174 / 9081 defaults.

A Git linked worktree, or a primary checkout that already has
`.worktree.local.json`, isolates automatically. On first `pnpm sut` in a linked
worktree with no local configuration, Donut creates a canonical identity
(`wt_...`), prepares `doughnut_e2e_<id>`, and allocates three application ports.
Reuse an existing checkout identity when backend tests already created one; do
not change that identity or repair its unit-test database (`doughnut_<id>_test`).
Unconfigured primary checkouts (no identity file and not a linked worktree) keep
the shared local defaults.

MySQL must already be listening on `127.0.0.1:3309` with the established
passwordless local root administration and the existing `doughnut` test user.
Isolated start does not start MySQL and does not start Mountebank. On first
`pnpm sut` it prepares `doughnut_e2e_<id>` (CREATE + GRANT, no `IF NOT EXISTS`)
and allocates three distinct application ports. Allocated ports are never 5173,
5174, 9081, or 2525. It does not create or repair a database that is already
recorded, adopt a name that already exists in MySQL, or modify `doughnut_test` /
`doughnut_e2e_test`. Confirm the identity file is ignored with
`git check-ignore -v .worktree.local.json`.

`pnpm sut:restart` replaces the idle live owner on this checkout's recorded
allocation. Ordinary Cypress is supported only for the focused specs
below.

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
allocation after that first start. Health verifies the live owner first, then
that each recorded listener is in the published application process group or
reachable from it by parent-process ancestry (Gradle may fork the backend JVM
into its own group). A foreign process that happens to answer on the recorded
ports is not a healthy owning stack.

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

With that owning SUT healthy, run exactly one of these specs — mixed or other
features are refused before reset:

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_creation_and_update/worktree_note_editing.feature
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/ai_generated_content/note_content_completion.feature
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_web_created_note.feature
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/mcp/mcp_services.feature
```

The CLI command is scoped to that one web-created-note feature. The MCP
command is scoped to that one search/graph feature. Each checkout must
already have its own healthy `pnpm sut` allocation; do not share the
unconfigured primary checkout. To prove the CLI notebook survives a peer
worktree's fixture reset, start both allocations first, then:

```bash
CURSOR_DEV=true nix develop -c node scripts/worktree-reset-isolation-harness.mjs --mode cli --peer <cli-checkout> --resetter <browser-checkout>
```

The note-editing spec does not start Mountebank. The note-content completion
spec starts a runner-owned private Mountebank after the Cypress lease, with
temporary management and OpenAI serving ports that exclude shared defaults
(2525/5001) and this checkout's application ports. Ownership of both listeners
is verified before fixture reset and again before mock install. Endpoints are
passed through Cypress `expose` config; isolated mode never falls back to
2525/5001.
Normal completion or cancellation stops that owned mock before releasing the
runner lease so a later run can start. The identity file and application
allocation are not modified for mock ports.

Cypress node configuration sets `baseUrl` to this checkout's browser origin
(`http://127.0.0.1:<lbListenPort>`). The browser uses that origin for named
navigation and for the Before-order-0 testability reset; it does not read local
files. A second Cypress runner, or `CYPRESS_baseUrl` / `baseUrl` that does not
match the allocated origin, is refused before reset. Restart while that runner
is held is refused.

Primary checkouts without `.worktree.local.json` keep the shared local defaults
documented in `docs/gcp/prod_env.md`, including Cypress origin
`http://localhost:5173` and shared Mountebank 2525/5001 for OpenAI mocks.
A primary checkout that has local identity uses isolation.

Before removing a linked checkout, inspect and reclaim its disposable databases
with `pnpm worktree:retire` — see
[`docs/worktree-retire-databases.md`](worktree-retire-databases.md).
