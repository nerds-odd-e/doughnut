# Codebase Structure

**Analysis Date:** 2026-07-15

## Directory Layout

```
doughnut/
├── backend/                 # Spring Boot Java API (Gradle subproject)
├── frontend/                # Vue 3 + Vite SPA
├── cli/                     # TypeScript/Ink terminal client
├── mcp-server/              # MCP stdio server for note tools
├── packages/
│   ├── generated/doughnut-backend-api/  # OpenAPI → TS SDK (generated)
│   ├── doughnut-api/        # Thin shared client wrapper for CLI/MCP
│   └── doughnut-test-fixtures/  # makeMe builders for API-shaped data
├── e2e_test/                # Cypress + Cucumber E2E
├── scripts/                 # Dev SUT, LB, logs, codegen helpers
├── infra/                   # GCP, Salt, Nix, path-routing
├── mysql/ · redis/          # Local data dirs for Nix process-compose
├── docs/                    # Human docs (not default-indexed)
├── ongoing/                 # Active plans (not default-indexed)
├── .cursor/                 # Agent map, rules, skills
├── open_api_docs.yaml       # Generated OpenAPI (do not hand-edit)
├── package.json             # pnpm workspace root scripts
├── pnpm-workspace.yaml      # Workspace package list
├── flake.nix                # Nix flake (dev shell)
└── settings.gradle          # Includes `backend` Gradle project
```

## Directory Purposes

**backend:**
- Purpose: HTTP API, domain logic, JPA persistence, Flyway, OpenAPI source of truth
- Contains: Java sources under `src/main/java/com/odde/doughnut/`, resources, Gradle build
- Key files: `DoughnutApplication.java`, `controllers/`, `services/`, `entities/`, `src/main/resources/db/migration/`

**frontend:**
- Purpose: Browser SPA for notes, recall, bazaar, circles, books
- Contains: `src/pages/`, `src/components/`, `src/composables/`, `src/managedApi/`, `src/routes/`, Vitest under `tests/`
- Key files: `src/main.ts`, `src/DoughnutApp.vue`, `src/routes/routes.ts`

**cli:**
- Purpose: Interactive and non-interactive Doughnut CLI
- Contains: `src/commands/`, Ink UI, `backendApi/`, session scrollback
- Key files: `src/index.ts`, `src/main.ts`, `src/run.ts`, `src/InteractiveCliApp.tsx`

**mcp-server:**
- Purpose: MCP tools that read note graph/search via backend API
- Contains: `src/tools/`, server assembly, context/API factory
- Key files: `src/index.ts`, `src/server.ts`, `src/tools/index.ts`

**packages:**
- Purpose: Shared TypeScript libraries across frontend/cli/mcp/e2e
- Contains: Generated SDK, `doughnut-api`, test fixtures
- Key files: `packages/generated/doughnut-backend-api/api-summary.md`, `packages/doughnut-test-fixtures/src/makeMe.ts`

**e2e_test:**
- Purpose: Behavior specs and Cypress automation
- Contains: `features/` (Gherkin), `step_definitions/`, `start/pageObjects/`, fixtures, Mountebank helpers
- Key files: `features/**/*.feature`, `start/pageBase.ts`, `config/`

**scripts:**
- Purpose: Local SUT lifecycle, LB, log tailing, API summary generation
- Contains: Node/bash helpers invoked by root `package.json`
- Key files: `sut-start.mjs`, `local-lb.mjs`, `logs-tail.mjs`, `generate-api-summary.mjs`

**infra:**
- Purpose: Deployment and edge routing
- Contains: `gcp/` (Packer, Salt, URL maps, path-routing), `digital_ocean/`, `nix/`, `ona/`
- Key files: `infra/gcp/url-maps/`, `infra/gcp/path-routing/`

**.cursor:**
- Purpose: Agent navigation and conventions
- Contains: `agent-map.md`, `rules/*.mdc`, `skills/*/SKILL.md`
- Key files: `.cursor/agent-map.md`

## Key File Locations

**Entry Points:**
- `backend/src/main/java/com/odde/doughnut/DoughnutApplication.java`: Spring Boot main
- `frontend/src/main.ts`: Vue app bootstrap
- `cli/src/index.ts`: CLI process entry
- `mcp-server/src/index.ts`: MCP stdio entry
- `cypress.config.ts` + `e2e_test/`: E2E entry

**Configuration:**
- `backend/src/main/resources/application.yml`: Spring config
- `backend/src/main/resources/db-*.properties`: DB profiles (dev/test/e2e)
- `frontend/` Vite + Vitest configs (under `frontend/`)
- `biome.json`: Repo TS/JS lint/format
- `openapi-ts.config.ts`: SDK generation
- `flake.nix` / `process-compose.yaml`: Nix dev services
- `pnpm-workspace.yaml`: Workspace membership

**Core Logic:**
- Backend controllers: `backend/src/main/java/com/odde/doughnut/controllers/`
- Backend services: `backend/src/main/java/com/odde/doughnut/services/`
- Backend algorithms: `backend/src/main/java/com/odde/doughnut/algorithms/`
- Backend entities: `backend/src/main/java/com/odde/doughnut/entities/`
- Frontend pages: `frontend/src/pages/`
- Frontend note UI: `frontend/src/components/notes/`
- Frontend API glue: `frontend/src/managedApi/`
- CLI commands: `cli/src/commands/`
- MCP tools: `mcp-server/src/tools/`

**Testing:**
- Backend: `backend/src/test/java/com/odde/doughnut/` (prefer `controllers/` tests)
- Frontend: `frontend/tests/` (mirrors `src/` domains; `*.spec.ts`)
- CLI: `cli/tests/` (`*.test.ts`)
- MCP: `mcp-server/tests/`
- E2E: `e2e_test/features/` + `e2e_test/step_definitions/`
- Shared fixtures: `packages/doughnut-test-fixtures/src/`

## Naming Conventions

**Files:**
- Backend Java: PascalCase class files matching type (`NoteController.java`, `NoteService.java`)
- Vue components/pages: PascalCase `.vue` (`NoteShow.vue`, `HomePage.vue`)
- Frontend TS modules: camelCase or descriptive (`clientSetup.ts`, `createNoteStorage.ts`)
- Frontend tests: match subject + `.spec.ts` under `frontend/tests/`
- CLI/MCP tests: `*.test.ts` under package `tests/`
- E2E features: snake_case `.feature` under domain folders (`note_creation_and_update/`)
- Flyway: `V{version}__{description}.sql` under `backend/src/main/resources/db/migration/`

**Directories:**
- Backend packages by layer/role: `controllers`, `services`, `entities`, `configs`, `algorithms`
- Frontend by UI role: `pages`, `components/<domain>`, `composables`, `layouts`, `store`
- E2E by capability folder under `features/` matching product language
- Permanent artifacts named by capability, not phase number (planning rule)

## Where to Add New Code

**New Feature (full stack):**
- API endpoint: `backend/src/main/java/com/odde/doughnut/controllers/` + service in `services/`
- Persistence if needed: entity/repo under `entities/` + new Flyway migration only
- Regenerate SDK: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`
- Web UI: page under `frontend/src/pages/`, components under `frontend/src/components/<domain>/`, composable under `frontend/src/composables/`
- Route: add metadata in `frontend/src/routes/routeMetadata.ts` and wire in `routes.ts`
- Tests: backend controller test; frontend `frontend/tests/.../*.spec.ts`; targeted E2E feature under `e2e_test/features/<domain>/`

**New Backend Service Only:**
- Implementation: `backend/src/main/java/com/odde/doughnut/services/` (subpackage if cohesive: `ai/`, `book/`, `search/`, …)
- Pure helpers: `algorithms/` when no Spring/DB needed
- Tests: alongside related controller/service tests under `backend/src/test/java/`

**New Vue Component:**
- Implementation: `frontend/src/components/<domain>/PascalCase.vue`
- Shared chrome/modals: `frontend/src/components/commons/` (use `Modal.vue` for dialogs)
- Stories (if needed): colocated `*.stories.ts` or under Storybook paths per `frontend-storybook` rule

**New CLI Command:**
- Implementation: `cli/src/commands/<area>/`
- Wire help/routing from existing command aggregation under `cli/src/commands/`
- Tests: `cli/tests/` preferring `run` / `runInteractive` observable behavior

**New MCP Tool:**
- Implementation: `mcp-server/src/tools/` + register in `tools/index.ts`
- Schemas: `mcp-server/src/schemas.ts` (or shared schemas module)
- Tests: `mcp-server/tests/`

**Utilities:**
- Frontend shared helpers: `frontend/src/utils/` or `frontend/src/lib/`
- Backend utils: `backend/src/main/java/com/odde/doughnut/utils/`
- Repo scripts: `scripts/` (prefer existing script patterns; follow `script.mdc` for shell)

**API-shaped test data:**
- Add builders in `packages/doughnut-test-fixtures/src/` and export via `makeMe.ts`

## Special Directories

**packages/generated/doughnut-backend-api:**
- Purpose: Hey API OpenAPI TypeScript client (`sdk.gen.ts`, `types.gen.ts`, `api-summary.md`)
- Generated: Yes (`pnpm generateTypeScript`)
- Committed: Yes — regenerate after controller/DTO changes; never hand-edit

**backend/src/main/resources/db/migration:**
- Purpose: Flyway SQL migrations (baseline + forward-only scripts)
- Generated: No
- Committed: Yes — add new files only; do not rewrite history

**backend/src/main/java/com/odde/doughnut/testability:**
- Purpose: Non-prod hooks for E2E (time, tokens, DB clean, GitHub stub)
- Generated: No
- Committed: Yes — keep out of production behavior paths

**ongoing/:**
- Purpose: Informal phased plans for active work
- Generated: No
- Committed: Yes — excluded from default agent indexing; read when executing/planning

**docs/:**
- Purpose: Human architecture/tech/practice documentation
- Generated: Partial (e.g. `docs/database-erd.md` via export script)
- Committed: Yes — excluded from default indexing

**mysql/ · redis/ · logs/:**
- Purpose: Local runtime data and SUT logs
- Generated: Runtime
- Committed: Generally not (data/logs); use for debugging via log files

**dist/ · */node_modules · backend/build:**
- Purpose: Build outputs and dependencies
- Generated: Yes
- Committed: No

---

*Structure analysis: 2026-07-15*
