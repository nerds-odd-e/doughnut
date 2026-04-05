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
- **Local LB** on **5173** (`pnpm local:lb:vite` inside `sut`) proxying to Vite on **5174** and Spring on **9081** — browser **http://localhost:5173** (ports, env, readiness: `docs/gcp/prod_env.md` Local dev / Cypress)
- Mountebank (mock external services)

**DO NOT restart services after code changes** - they auto-reload.

### Local MySQL and Redis (direnv / `nix develop` and process-compose)

The Nix shell hook ([`scripts/nix_shell_hook.sh`](scripts/nix_shell_hook.sh)) loads [`process-compose.yaml`](process-compose.yaml), which defines supervised **`mysql`** and **`redis`**. It sets `MYSQL_*` and `REDIS_*` via `setup_mysql_env` / `setup_redis_env` in [`scripts/shell_setup.sh`](scripts/shell_setup.sh) (nix **`mysql84`** and **`redis`** from [`flake.nix`](flake.nix)).

**MySQL:** When **`CURSOR_DEV` is unset**, if **nothing is listening on TCP 3309**, the hook runs `process-compose -f process-compose.yaml up -D mysql`, waits until MySQL answers, then applies [`scripts/sql/init_doughnut_db.sql`](scripts/sql/init_doughnut_db.sql). If **3309 is already in use**, the hook **does not** start or restart MySQL.

**Redis:** When **`CURSOR_DEV` is unset**, if **nothing is listening on TCP 6380**, the hook runs `process-compose -f process-compose.yaml up -D redis`, then waits until Redis answers (`check_redis_ready`). If **6380 is already in use** (including a stray `redis-server` not started by process-compose), the hook **does not** start supervised Redis; free the port or stop the other process first.

With **`CURSOR_DEV=true`**, those blocks are skipped (agents); start MySQL and/or Redis yourself only if you need them.

**Stop MySQL and Redis** (stops the detached process-compose project and all processes it supervises):

```bash
# From repo root, inside an active nix dev shell, or:
CURSOR_DEV=true nix develop -c process-compose down
```

**Manual start** (after `down`, or if the hook did not run, with **3309** / **6380** free and the usual `MYSQL_*` / `REDIS_*` from [`scripts/shell_setup.sh`](scripts/shell_setup.sh)—already set after entering `nix develop`):

```bash
process-compose -f process-compose.yaml up -D mysql
process-compose -f process-compose.yaml up -D redis
```

**Debug — MySQL:**

- Server log: **`mysql/mysql.log`** (`$MYSQL_HOME/mysql.log`).
- Quick check: `mysqladmin ping -h127.0.0.1 -P3309`.
- Live supervisor UI: `process-compose attach` (same `PATH` as above).
- Buffered process logs: `process-compose process logs mysql`.

**Debug — Redis:**

- Server log: **`redis/redis.log`** (`$REDIS_LOG_FILE`).
- Quick check: `redis-cli -p 6380 ping`.
- Buffered process logs: `process-compose process logs redis`.

**Restart after a bad start or crash — MySQL:**

1. Read `mysql/mysql.log` (datadir, socket, or port conflicts show up there).
2. Run `process-compose down` (or `process-compose process stop mysql` to stop only the `mysql` process while the supervisor stays up).
3. If **3309** is still in use, stop the stray server: `pgrep mysqld | xargs kill` (or `pkill mysqld`), then confirm with `lsof -i :3309`.
4. Start again: re-enter the directory so direnv/`nix develop` runs the hook with a free **3309**, or run `process-compose -f process-compose.yaml up -D mysql` from the repo in a shell where `MYSQL_*` is already configured.

**Restart after a bad start or crash — Redis:**

1. Read `redis/redis.log` (port conflicts show up there).
2. Run `process-compose down` or `process-compose process stop redis`.
3. If **6380** is still in use, stop the stray `redis-server`, then confirm with `lsof -i :6380`.
4. Start again with a free **6380** via the hook or `process-compose -f process-compose.yaml up -D redis` from the repo in a shell where `REDIS_*` is already configured.

**direnv / `nix develop` “hanging” or “taking a while”:** The flake shellHook does a lot of work (pnpm, Biome, Cypress, MySQL, Redis). **`direnv`** often runs that hook **more than once** per `cd`; [`scripts/dev_setup.sh`](scripts/dev_setup.sh) **skips** **`corepack prepare`**, **`corepack use`**, and **`pnpm recursive install`** when **`pnpm-lock.yaml`**, root **`package.json`**, and **`pnpm-workspace.yaml`** are unchanged (fingerprint in **`.doughnut-pnpm-lock.sha256`**, gitignored). **`corepack use`** was still triggering a full workspace install even when **`pnpm install` was skipped**, which kept direnv past its default warn timeout. Force the full PNPM path with **`DOUGHNUT_SHELL_HOOK_FORCE_PNPM=1`**. After you see **`Environment setup complete!`**, Nix may still print **`removing profile version …`** while it updates your dev profile — that step is **outside** the repo hook. **`DIRENV_WARN_TIMEOUT`** in [`.envrc`](.envrc) uses a duration with a unit (**`120s`**); direnv’s timer may still start before `.envrc` runs — if the **5s** warning persists, set **`warn_timeout`** in **`~/.config/direnv/direnv.toml`** under **`[global]`**. For faster reloads after the first load, consider **[nix-direnv](https://github.com/nix-community/nix-direnv)** (`use flake` integration caches the environment).

### Unsure if SUT is running?

1. Run **`CURSOR_DEV=true nix develop -c pnpm sut:healthcheck`** (agents and developers outside an active `nix develop` shell).
2. If not OK, run **`CURSOR_DEV=true nix develop -c pnpm sut:restart`**, or start **`pnpm sut`** in a terminal if nothing was listening.
3. Do **not** restart `pnpm sut` after normal code edits — services auto-reload (see above).

**Cloud VM** (no Nix): run **`pnpm sut:healthcheck`** and **`pnpm sut:restart`** from the repo root; **`sut:restart`** needs **`lsof`** on `PATH`. See `.cursor/rules/cloud-agent-setup.mdc`.

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
| Check SUT (ports + LB readiness) | `CURSOR_DEV=true nix develop -c pnpm sut:healthcheck` |
| Stop stray listeners on 5173/5174/9081 and start SUT again | `CURSOR_DEV=true nix develop -c pnpm sut:restart` |
| Local LB only (static + backend, no Vite) | `pnpm local:lb` |
| Local LB with Vite upstream (as in `pnpm sut`) | `pnpm local:lb:vite` |
| Run backend tests | `pnpm backend:verify` |
| Run backend tests (no migration) | `pnpm backend:test_only` |
| Run frontend tests | `pnpm frontend:test` |
| Run single frontend test | `pnpm -C frontend test tests/path/to/TestFile.spec.ts` |
| Run E2E test (single feature) | `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/path/to.feature` |
| Open Cypress IDE | `pnpm cy:open` |
| Format all code | `pnpm format:all` |
| Lint all code | `pnpm lint:all` |
| Format shared API test fixtures only (Biome) | `pnpm test-fixtures:format` |
| Lint shared API test fixtures only (Biome) | `pnpm test-fixtures:lint` |
| Regenerate TypeScript from OpenAPI | `pnpm generateTypeScript` |
| Connect to local DB | `mysql -S $MYSQL_HOME/mysql.sock -u doughnut -p` (password: doughnut) |
| Stop local MySQL and Redis (process-compose) | `CURSOR_DEV=true nix develop -c process-compose down` |
| Attach to process-compose (supervisor TUI) | `CURSOR_DEV=true nix develop -c process-compose attach` |
| Redis logs (process-compose) | `CURSOR_DEV=true nix develop -c process-compose process logs redis` |

Unless you are already inside `nix develop`, wrap `pnpm` / Gradle invocations like the E2E row: `CURSOR_DEV=true nix develop -c <command>`. (Cloud VM: no wrapper — see above.)

**Frontend single-file Vitest:** The path is relative to `frontend/`. From the repo root you can use the same path with `pnpm frontend:test tests/path/to/TestFile.spec.ts`. Do not put `--` before the file path: pnpm would forward it to Vitest and the suite filter is ignored (every spec runs).

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
│   ├── generated/
│   │   └── doughnut-backend-api/  # Auto-generated API client
│   └── doughnut-test-fixtures/    # Shared `makeMe` API-shaped builders; **public import:** `doughnut-test-fixtures/makeMe` only
├── e2e_test/                   # Cucumber E2E tests
│   ├── features/               # Gherkin feature files
│   ├── step_definitions/       # Step implementations
│   └── start/                  # Page objects
└── mcp-server/                 # MCP server for Claude integration
```

### Frontend-Backend Integration

- Frontend API client is auto-generated from OpenAPI spec
- After changing backend controller signatures, run `pnpm generateTypeScript`
- Frontend production build outputs to `frontend/dist/`; CLI bundle is `cli/dist/doughnut-cli.bundle.mjs` (`pnpm cli:bundle`). Prod serves `/doughnut-cli-latest/doughnut` from GCS; local dev / Cypress serves that path from `scripts/local-lb.mjs` reading `cli/dist` (not Spring on 9081). The CLI bundle aliases optional Ink DevTools import `react-devtools-core` to a stub under `cli/src/shims/`; see **Build output** in `.cursor/rules/cli.mdc`. **CLI:** multiline shell is **TTY-only**; scripts use **`doughnut version`** or **`doughnut update`**. Interactive shell shows a **`/ commands`** hint only (no `/help` or completion UI).
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
- API-shaped test and Storybook data: `makeMe` — import **`doughnut-test-fixtures/makeMe`** (the package’s only supported export; builders under `src/` are internal). `@tests/*` still maps to `frontend/tests/` for helpers.

### E2E Tests
- Keep step definitions lightweight, delegate to page objects
- Use tags like `@usingMockedOpenAiService`, `@mockBrowserTime` for test configuration
- Backend logs at `backend/logs/doughnut-e2e.log`

### Database Migrations
- Location: `backend/src/main/resources/db/migration/`
- Naming: `V{version}__{description}.sql` (e.g., `V300000176__description.sql`)
- Never modify committed migrations; create new ones

### CLI (`cli/`)
## Planning and phased delivery

- Informal plans for active work: `ongoing/<short-name>.md`
- **How to phase work, E2E vs unit tests, TDD workflow, deploy gate, and interim behavior:** `.cursor/rules/planning.mdc`

## Development principles

1. **High cohesion** — Minimize duplication; keep related code together (see also `.cursor/rules/general.mdc`).
2. **Keep it simple** — Avoid defensive programming; use the minimum code that fits the task.
3. **No historical documentation** — Code and comments reflect the current state only; temporary notes may live in `ongoing/`.
4. **Test observable behavior, not internal structure** — Assert what users or callers can see (responses, UI, terminal output, exit codes) by driving **high-level entry points** (controllers, mounted UI, `run` / `runInteractive`, etc.). Those tests often **do not import** the code under change directly; they still exercise it through the real path. Avoid tests that mainly **mirror the codebase** (low-level functions + internal-only parameters or adapter shapes): they refactor badly and can miss real wiring. Prefer **fewer** tests at observable surfaces over a **1:1** map from implementation files to tests; keep assertions for **one user-visible behavior** grouped in one place when practical. **Testing strategy and layer roles:** `.cursor/rules/planning.mdc`. Stack habits: `backend-development.mdc`, `frontend.mdc`, `cli.mdc`, `e2e_test.mdc`.
