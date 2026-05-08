# Agent Map

Use this as the first stop for code navigation. It is intentionally short so agents do not need to read generated API files or long docs for ordinary tasks.

## Work Areas

- Backend HTTP/API behavior: start in `backend/src/main/java/com/odde/doughnut/controllers/`, then follow services in `backend/src/main/java/com/odde/doughnut/services/` and entities/repositories in `backend/src/main/java/com/odde/doughnut/entities/`.
- Backend tests: prefer controller-level tests under `backend/src/test/java/com/odde/doughnut/controllers/`; use `makeMe` fixtures and real database transactions.
- Frontend pages and components: start in `frontend/src/pages/`, `frontend/src/components/`, `frontend/src/composables/`, and `frontend/src/store/`.
- Frontend tests: use `frontend/tests/`; mock generated API calls with `mockSdkService()` and build API-shaped data with `doughnut-test-fixtures/makeMe`.
- E2E behavior: start with `e2e_test/features/`, then the matching step definitions in `e2e_test/step_definitions/`, then page objects in `e2e_test/start/`.
- CLI behavior: start in `cli/src/`; run focused CLI tests from `cli/` rather than broad workspace verification.
- MCP server behavior: start in `mcp-server/`; use `.cursor/rules/mcp-server.mdc` only for MCP-specific build/test details.
- Database schema changes: add a new migration in `backend/src/main/resources/db/migration/`; never edit committed migrations.

## Generated API

Use `packages/generated/doughnut-backend-api/api-summary.md` as the default endpoint lookup. The larger generated API files are intentionally ignored by default indexing:

- `packages/generated/doughnut-backend-api/types.gen.ts`
- `packages/generated/doughnut-backend-api/sdk.gen.ts`
- `open_api_docs.yaml`

For frontend calls, import services from `@generated/doughnut-backend-api/sdk.gen`. For API-shaped fixtures, import `makeMe` from `doughnut-test-fixtures/makeMe`. Open `sdk.gen.ts` or `types.gen.ts` only when checking an exact generated signature. After backend controller signature or DTO changes, run:

```bash
CURSOR_DEV=true nix develop -c pnpm generateTypeScript
```

## Commands

Always run repo commands through Nix unless working in a documented Cloud VM path:

```bash
CURSOR_DEV=true nix develop -c <command>
```

Useful focused checks:

- Backend: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- Frontend single file: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/path/to/TestFile.spec.ts`
- E2E single feature: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/path/to.feature`
- Log inspection: `CURSOR_DEV=true nix develop -c pnpm logs:tail backend-e2e` (targets: `sut`, `backend-e2e`, `mountebank`)
- Lint all: `CURSOR_DEV=true nix develop -c pnpm lint:all`
- Format all: `CURSOR_DEV=true nix develop -c pnpm format:all`

Assume `pnpm sut` is already running. If unsure, check `CURSOR_DEV=true nix develop -c pnpm sut:healthcheck`. Do not ask developers to restart services after normal code changes; backend and frontend auto-reload.

## Ignored Reference Material

`docs/` and `ongoing/` are excluded from default indexing to reduce retrieval noise. Read them explicitly only when the user asks for docs/plans, a rule points to a specific document, or the task is about planning/history rather than current source behavior.
