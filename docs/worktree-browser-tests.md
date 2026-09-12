# Isolated browser tests for a local checkout

Use `pnpm cy:run --spec <feature>` in a local checkout (including a Git worktree)
when you want the application to run against its own E2E database and ports
instead of the shared `doughnut_e2e_test` / 5173 / 5174 / 9081 defaults.

A Git linked worktree, or a primary checkout that already has
`.worktree.local.json`, isolates automatically. On first `pnpm cy:run` in a
linked worktree with no local configuration, Donut creates a canonical identity
(`wt_...`), prepares `doughnut_e2e_<id>`, and allocates three application ports.
Reuse an existing checkout identity when backend tests already created one; do
not change that identity or repair its unit-test database (`doughnut_<id>_test`).
Unconfigured primary checkouts (no identity file and not a linked worktree) keep
the shared local defaults.

MySQL must already be listening on `127.0.0.1:3309` with the established
passwordless local root administration and the existing `doughnut` test user.
Isolated provisioning does not start MySQL; the runner starts private mocks
when selected features require them. On first
`pnpm cy:run` it prepares `doughnut_e2e_<id>` (CREATE + GRANT, no `IF NOT EXISTS`)
and allocates three distinct application ports. Allocated ports are never 5173,
5174, 9081, or 2525. It does not create or repair a database that is already
recorded, adopt a name that already exists in MySQL, or modify `doughnut_test` /
`doughnut_e2e_test`. Confirm the identity file is ignored with
`git check-ignore -v .worktree.local.json`.

Each `pnpm cy:run --spec <features>` invocation starts its own application stack,
waits for readiness, runs the selected features, and shuts down its owned
application and mock processes before returning. `pnpm cy:open` keeps its stack
for the interactive session and stops it when the session closes. Worktree
identity, database, and allocated application ports persist between invocations.
There is no separate SUT start, stop, or restart command.

## Start and health

Unset conflicting overrides first. Matching values for the assigned target
remain usable; a different datasource, Flyway URL, proxy, or server port
refuses before spawn:

```bash
unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL INPUT_DB_URL SERVER_PORT \
  LOCAL_LB_BACKEND LOCAL_LB_VITE_UPSTREAM LOCAL_LB_LISTEN_PORT \
  FRONTEND_DEV_PORT FRONTEND_BACKEND_ORIGIN SUT_RUNTIME_TARGET
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/worktree_note_editing.feature
```

`pnpm cy:run` prints `Selected database:` and `Browser origin:` for the recorded
allocation, starts backend + Vite + the local load balancer,
and waits until this checkout's live owner is healthy on those ports. A second
invocation in the same checkout while that owner is live is refused.

The live owner binds its control socket in a private short directory under
`/tmp` and records that lexical address in this checkout's
`.sut.local.lock/owner.json`. Checkout path length does not require a
socket-path override. Duplicate starts, runner leases, owned shutdown, and
dead-owner recovery still use that recorded endpoint; cleanup removes only
the owned socket directory, never another checkout's resources. A name that
already exists in MySQL before this checkout records it is a collision, not
adoption. A recorded missing database, occupied port, or migration failure is
not permission to adopt, delete, rebuild, or renumber.

Readiness uses the internal healthcheck to verify the live owner and that each
application listener belongs to its process tree. A foreign listener answering
on an allocated port does not establish a healthy owning stack. Inspect retained
logs with `pnpm logs:tail sut` after a failed invocation. Invalid or ambiguous
ownership fails visibly; retrying never authorizes killing listeners by port.

## Focused Cypress run

Pass comma-separated feature paths to `pnpm cy:run --spec` to run several
supported features sequentially with one stack. The invocation starts a private
OpenAI or Wikidata mock if any selected feature needs it, then cleans up once
at the end.

Select one or more supported specs; the command owns startup and cleanup. The
admitted set — application-only active features plus the CLI, MCP,
OpenAI-mock, and Wikidata-mock focused specs — is declared in the
isolated-runner registry
(`scripts/isolated-cypress-spec-selection.mjs`). Selections containing
unsupported features are refused before reset. For example:

```bash
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/worktree_note_editing.feature
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/ai_generated_content/note_content_completion.feature
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_web_created_note.feature
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/mcp/mcp_services.feature
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/wikidata/note_create_with_wikidata_id.feature
```

Each approved spec's resource requirements are declared in the same registry.
OpenAI-mock specs require a runner-owned private OpenAI mock; Wikidata-mock
specs require a runner-owned private Wikidata mock; application-only, CLI, and
MCP specs use the owning SUT and runner lease without a mock. A mixed file
(such as `associate_wikidata.feature`) may provision a mock even when its
selected scenario does not use it; Cucumber's scenario tag selection still
chooses mocked versus real service URLs per scenario. The mock process does
not own feature paths. Selections containing unknown specs refuse before reset
so they do not use shared defaults.

The CLI command is scoped to the assessed active CLI workflows (web-created
note, install-and-run, clone, existing-note edits, folder relocation); each
uses the selected app origin for spawned CLI processes and temporary
config/install/clone destinations with checkout-local bundles. The MCP
command is scoped to that one search/graph feature. Each invocation prepares
and owns its checkout's E2E allocation; do not share the unconfigured primary
checkout. To prove the CLI notebook survives a peer worktree's fixture
reset, invoke the paired harness directly — each role owns its own stack:

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
match the allocated origin, is refused before reset. A second invocation while
that runner is held is refused.

Primary checkouts without `.worktree.local.json` keep the shared local defaults
documented in `docs/gcp/prod_env.md`, including Cypress origin
`http://localhost:5173` and shared Mountebank 2525/5001 for OpenAI mocks.
A primary checkout that has local identity uses isolation.

Before removing a linked checkout, inspect and reclaim its disposable databases
with `pnpm worktree:retire` — see
[`docs/worktree-retire-databases.md`](worktree-retire-databases.md).
