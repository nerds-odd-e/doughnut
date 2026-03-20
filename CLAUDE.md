# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Doughnut is a Personal Knowledge Management (PKM) tool combining zettelkasten-style note capture with spaced-repetition learning features and knowledge sharing capabilities.

## Development Environment

**This project uses Nix for environment management.** With `direnv` configured, the environment auto-loads when entering the directory.

### Starting Development

```bash
pnpm sut
```

This starts all services with auto-reload:
- Backend (Spring Boot with Gradle continuous build)
- Frontend (Vite with HMR)
- Mountebank (mock external services)

**DO NOT restart services after code changes** - they auto-reload.

### Running Commands

Prefix commands with the nix wrapper:
```bash
CURSOR_DEV=true nix develop -c <command>
```

**Cursor Cloud VM** has no Nix — run commands without that prefix. See `.cursor/rules/cloud-agent-setup.mdc`.

## Common Commands

| Task | Command |
|------|---------|
| Start all services | `pnpm sut` |
| Run backend tests | `pnpm backend:verify` |
| Run backend tests (no migration) | `pnpm backend:test_only` |
| Run frontend tests | `pnpm frontend:test` |
| Run single frontend test | `pnpm -C frontend test tests/path/to/TestFile.spec.ts` |
| Run E2E test (single feature) | `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/path/to.feature` |
| Open Cypress IDE | `pnpm cy:open` |
| Format all code | `pnpm format:all` |
| Lint all code | `pnpm lint:all` |
| Regenerate TypeScript from OpenAPI | `pnpm generateTypeScript` |
| Connect to local DB | `mysql -S $MYSQL_HOME/mysql.sock -u doughnut -p` (password: doughnut) |

Unless you are already inside `nix develop`, wrap `pnpm` / Gradle invocations like the E2E row: `CURSOR_DEV=true nix develop -c <command>`. (Cloud VM: no wrapper — see above.)

## Architecture

### Tech Stack
- **Backend:** Spring Boot 3.x, Java 25, MySQL, Redis, JPA/Hibernate, Flyway migrations
- **Frontend:** Vue 3, TypeScript, Vite, Pinia, Tailwind CSS, DaisyUI
- **Testing:** JUnit 5 (backend), Vitest (frontend), Cypress + Cucumber (E2E)
- **External Services:** OpenAI API, Wikidata API

### Directory Structure

```
doughnut/
├── backend/                    # Spring Boot backend
│   └── src/main/java/com/odde/doughnut/
│       ├── controllers/        # REST endpoints + DTOs
│       ├── services/           # Business logic, AI integration
│       ├── entities/           # JPA entities + repositories
│       └── configs/            # Spring configurations
├── frontend/                   # Vue 3 frontend
│   └── src/
│       ├── pages/              # Page components
│       ├── components/         # Reusable components
│       ├── composables/        # Vue 3 composables
│       ├── store/              # Pinia state management
│       └── (imports from packages/generated/doughnut-backend-api/)
├── packages/
│   └── generated/
│       └── doughnut-backend-api/  # Auto-generated API client
├── e2e_test/                   # Cucumber E2E tests
│   ├── features/               # Gherkin feature files
│   ├── step_definitions/       # Step implementations
│   └── start/                  # Page objects
└── mcp-server/                 # MCP server for Claude integration
```

### Frontend-Backend Integration

- Frontend API client is auto-generated from OpenAPI spec
- After changing backend controller signatures, run `pnpm generateTypeScript`
- Frontend builds output to `backend/src/main/resources/static/`
- Dev server proxies `/api/`, `/attachments/`, `/logout/`, `/testability/` to backend

## Code Conventions

### Backend (Java)
- Test through controllers when possible
- Use `makeMe` factory pattern for test data (e.g., `makeMe.aNote().creatorAndOwner(user).please()`)
- Tests use real database with `@Transactional`
- Always use proper imports, never inline fully qualified class names

### Frontend (Vue/TypeScript)
- Use `<script setup lang="ts">` for components
- Import API services from `@generated/doughnut-backend-api/sdk.gen`
- Use `apiCallWithLoading()` for user-initiated actions with loading/error handling
- DaisyUI classes use `daisy-` prefix
- Avoid `getByRole` queries in tests (performance) - use `getByText`, `getByLabelText`, etc.
- Use `mockSdkService()` helper for mocking API calls in tests

### E2E Tests
- Keep step definitions lightweight, delegate to page objects
- Use tags like `@usingMockedOpenAiService`, `@mockBrowserTime` for test configuration
- Backend logs at `backend/logs/doughnut-e2e.log`

### Database Migrations
- Location: `backend/src/main/resources/db/migration/`
- Naming: `V{version}__{description}.sql` (e.g., `V300000176__description.sql`)
- Never modify committed migrations; create new ones

### CLI (`cli/`)
- Optional recall-load delay for manual testing or stable Vitest coverage: set `DOUGHNUT_CLI_SLOW_RECALL_LOAD_MS` to a positive number of milliseconds (capped at 60000). Applies only when the TTY passes an abortable `recallNext` load (`/recall` and recall session continuations that use the same path). Default is off. See `cli/src/recall.ts` and `.cursor/rules/cli.mdc`.

## Planning and phased delivery

- Informal plans for active work: `ongoing/<short-name>.md`
- **How to phase work, E2E vs unit tests, TDD workflow, deploy gate, and interim behavior:** `.cursor/rules/planning.mdc`

## Development principles

1. **High cohesion** — Minimize duplication; keep related code together (see also `.cursor/rules/general.mdc`).
2. **Keep it simple** — Avoid defensive programming; use the minimum code that fits the task.
3. **No historical documentation** — Code and comments reflect the current state only; temporary notes may live in `ongoing/`.
4. **Test behavior, not implementation** — Tests verify pre/post state transitions; stack-specific practices are in `.cursor/rules/backend-development.mdc`, `.cursor/rules/frontend.mdc`, and `.cursor/rules/e2e_test.mdc`.
