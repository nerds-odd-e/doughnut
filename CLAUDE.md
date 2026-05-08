# CLAUDE.md

Guidance for Claude Code and other AI coding agents working in this repository.

## Project Overview

Doughnut is a Personal Knowledge Management tool combining zettelkasten-style note capture, spaced repetition, and knowledge sharing.

## Start Here

Use `.cursor/agent-map.md` for repo navigation, generated API guidance, focused test commands, and default indexing notes. This file keeps only the baseline context that should be available in most sessions.

## Environment

- Use `CURSOR_DEV=true nix develop -c <command>` for repo commands unless you are in a documented Cloud VM path without Nix.
- Assume `pnpm sut` has already started backend, frontend, local LB, and Mountebank. It exits after services are healthy and writes service output to `sut.log`.
- Browser app origin in local development is `http://localhost:5173`.
- Do not restart services after normal code changes; backend and frontend auto-reload.
- If service state matters, run `CURSOR_DEV=true nix develop -c pnpm sut:healthcheck`. If not OK, run `CURSOR_DEV=true nix develop -c pnpm sut:restart` or `CURSOR_DEV=true nix develop -c pnpm sut`.

## Common Commands

| Task | Command |
|------|---------|
| Check SUT | `CURSOR_DEV=true nix develop -c pnpm sut:healthcheck` |
| Backend tests | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` |
| Frontend tests | `CURSOR_DEV=true nix develop -c pnpm frontend:test` |
| Single frontend test | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/path/to/TestFile.spec.ts` |
| Single E2E feature | `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/path/to.feature` |
| Generate frontend API client | `CURSOR_DEV=true nix develop -c pnpm generateTypeScript` |
| Lint all | `CURSOR_DEV=true nix develop -c pnpm lint:all` |
| Format all | `CURSOR_DEV=true nix develop -c pnpm format:all` |

Do not run the full Cypress suite locally unless the user explicitly asks or CI reproduction requires it.

## Architecture Pointers

- Backend: `backend/src/main/java/com/odde/doughnut/` contains controllers, services, entities, repositories, and configs.
- Frontend: `frontend/src/` contains Vue pages, components, composables, and Pinia stores.
- E2E: `e2e_test/features/`, `e2e_test/step_definitions/`, and `e2e_test/start/` hold Cucumber features, steps, and page objects.
- CLI: `cli/` contains the command-line app.
- MCP server: `mcp-server/` contains the Doughnut MCP integration.
- Generated backend API client: `packages/generated/doughnut-backend-api/`. These files are excluded from default indexing; open them only for exact generated signatures.

## Frontend-Backend Integration

- Frontend services come from `@generated/doughnut-backend-api/sdk.gen`.
- API-shaped test and Storybook data comes from `doughnut-test-fixtures/makeMe`.
- After backend controller signature or DTO changes, run `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`.

## Local MySQL and Redis

The Nix shell hook uses `process-compose.yaml` plus `scripts/shell_setup.sh` to configure supervised MySQL on port `3309` and Redis on port `6380` when `CURSOR_DEV` is unset. With `CURSOR_DEV=true`, agents skip those startup blocks.

Useful service commands:

```bash
CURSOR_DEV=true nix develop -c process-compose down
CURSOR_DEV=true nix develop -c process-compose process logs mysql
CURSOR_DEV=true nix develop -c process-compose process logs redis
```

If a local database service fails, inspect `mysql/mysql.log` or `redis/redis.log`, then stop the supervised process or free the port before starting it again.

## Code Conventions

- Backend tests should drive controllers where practical; use `makeMe` factories and real database transactions.
- Frontend components use `<script setup lang="ts">`, DaisyUI classes with the `daisy-` prefix, and `apiCallWithLoading()` for user-triggered API calls.
- Frontend tests should avoid `getByRole` for performance; prefer `getByText`, `getByLabelText`, and `mockSdkService()`.
- E2E steps stay lightweight and delegate UI details to page objects.
- Database migrations live in `backend/src/main/resources/db/migration/`; never modify committed migrations.

## Development Principles

1. Keep high cohesion: minimize duplication and keep related concepts together.
2. Keep it simple: use the minimum code that satisfies the prompt.
3. Name permanent artifacts by capability, not development history.
4. Test observable behavior through high-level entry points when practical.

Planning notes belong in `ongoing/`, which is excluded from default indexing. Read it explicitly only for active planning/history tasks.
