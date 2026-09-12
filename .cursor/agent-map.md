# Agent Map

Repository navigation and non-obvious tooling notes. Consult this map when the
location or command you need is unclear. Skills are discovered automatically
and are intentionally not cataloged here.

## Work Areas

- Backend HTTP/API behavior: start in `backend/src/main/java/com/odde/donut/controllers/`, then follow `services/` and `entities/`; backend tests start in `backend/src/test/java/com/odde/donut/controllers/`.
- Frontend pages and components: start in `frontend/src/pages/`, `frontend/src/components/`, `frontend/src/composables/`, and `frontend/src/store/`; frontend tests live in `frontend/tests/`.
- E2E behavior: start with `e2e_test/features/`, then matching step definitions in `e2e_test/step_definitions/` and page objects in `e2e_test/start/`.
- CLI behavior: start in `cli/src/`.
- MCP server behavior: start in `mcp-server/`.
- Database schema changes: add a new migration in `backend/src/main/resources/db/migration/`; never edit committed migrations.

## Generated API

Use `packages/generated/donut-backend-api/api-summary.md` as the default endpoint lookup. The larger generated API files are intentionally ignored by default indexing:

- `packages/generated/donut-backend-api/types.gen.ts`
- `packages/generated/donut-backend-api/sdk.gen.ts`
- `open_api_docs.yaml`

For frontend calls, import services from `@generated/donut-backend-api/sdk.gen`. For API-shaped fixtures, import `makeMe` from `donut-test-fixtures/makeMe`. Open `sdk.gen.ts` or `types.gen.ts` only when checking an exact generated signature. After backend controller signature or DTO changes, run:

```bash
CURSOR_DEV=true nix develop -c pnpm generateTypeScript
```

Never hand-edit `packages/generated/donut-backend-api/**` or `open_api_docs.yaml`; regenerate them. For whitespace hygiene, use `scripts/check_diff_whitespace.sh` instead of raw `git diff --check` so generated artifacts are not manually "fixed".

## Commands

Run repo tooling through Nix unless working in a documented Cloud VM path:

```bash
CURSOR_DEV=true nix develop -c <command>
```

**Exception:** `git` commands do not need the Nix prefix — run them directly (e.g. `git status`, `git diff`, `git commit`).

Useful focused checks:

- Backend: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- Isolated worktree backend tests: [`docs/worktree-backend-tests.md`](../docs/worktree-backend-tests.md). Linked worktrees isolate ordinary `pnpm backend:test` / `backend:test_only` and wrapper `test` / `migrateTestDB` automatically. Opt-in remains: `CURSOR_DEV=true nix develop -c pnpm backend:test:worktree`
- Frontend single file: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/path/to/TestFile.spec.ts`
- E2E single feature: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/path/to.feature`
- Log inspection: `CURSOR_DEV=true nix develop -c pnpm logs:tail backend-e2e` (targets: `sut`, `backend-e2e`, `mountebank`, `dev`)
- Diff whitespace: `scripts/check_diff_whitespace.sh` or `scripts/check_diff_whitespace.sh --cached`
- Coordinator format of changed working-tree components:
  `./scripts/run.sh pnpm format:changed`
- Pre-commit hook lint of changed staged components (not a routine standalone
  wrap-up command): `./scripts/run.sh pnpm lint:changed`
- Lint all: `CURSOR_DEV=true nix develop -c pnpm lint:all`
- Format all: `CURSOR_DEV=true nix develop -c pnpm format:all`

**Development vs E2E:** For manual product feedback from the unconfigured primary,
prefer `pnpm dev` (http://127.0.0.1:5175/, profile `dev`,
`doughnut_development`, `dev.log` / `dev.pid`; restart with `pnpm dev:restart`;
local sign-in e.g. `manual` / `password`). `pnpm cy:run` is the disposable E2E
stack (http://localhost:5173/, profile `e2e`). Assume the stack you need is
already running. If unsure for E2E, check
`CURSOR_DEV=true nix develop -c node scripts/sut-healthcheck.mjs`. Do not ask
developers to restart services after normal code changes; backend and frontend
auto-reload.

## Architectural decisions (ADRs)

- Human propose / discuss / approve: `docs/adrs/README.md`
- Current recommendations: `docs/adrs/*-accepted.md` (read explicitly — under `docs/`)
- Rule pointer: `.cursor/rules/architecture-decisions.mdc`

## Ignored Reference Material

`docs/` and leftover `ongoing/` files are excluded from default indexing to reduce retrieval noise. Active planning lives in `.planning/` (GSD `phases/`, `quick/`, `STATE`, … — see `gsd-coexistence.mdc`). Test-optimization candidates: `.planning/test-optimization-blacklist.md`. Read `docs/` explicitly when the user asks for docs, a rule points to a document, or an ADR check is required (`docs/adrs/`).
