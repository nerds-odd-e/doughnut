<!-- refreshed: 2026-07-15 -->
# Architecture

**Analysis Date:** 2026-07-15

## System Overview

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Client surfaces                                      │
├──────────────────┬──────────────────┬───────────────────┬───────────────────┤
│  Vue SPA         │  Doughnut CLI    │  MCP server       │  E2E (Cypress)    │
│  `frontend/`     │  `cli/`          │  `mcp-server/`    │  `e2e_test/`      │
└────────┬─────────┴────────┬─────────┴─────────┬─────────┴─────────┬─────────┘
         │                  │                   │                   │
         │   `@generated/doughnut-backend-api` + `packages/donut-api`
         ▼                  ▼                   ▼                   ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  Local LB (dev) / GCP URL map (prod) → `/api/*` → Spring Boot backend       │
│  `scripts/local-lb.mjs` · `infra/gcp/` · `backend/`                         │
└────────────────────────────────────┬────────────────────────────────────────┘
                                     │
         ┌───────────────────────────┼───────────────────────────┐
         ▼                           ▼                           ▼
┌─────────────────┐      ┌─────────────────────┐      ┌─────────────────────┐
│ Controllers     │──────▶│ Services / Algos    │──────▶│ JPA entities + repos│
│ `/api/...`      │      │ Authorization, AI,  │      │ MySQL 8.4 (+VECTOR) │
│ OpenAPI DTOs    │      │ notes, recall, …    │      │ Flyway migrations   │
└─────────────────┘      └──────────┬──────────┘      └─────────────────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
              OpenAI APIs     Wikidata APIs     Redis / GitHub
              (AI features)   (entity lookup)   (cache / issues)
```

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| Spring Boot app | Boot, OpenAPI generation tasks, migrate-test-DB tasks | `backend/src/main/java/com/odde/doughnut/DoughnutApplication.java` |
| REST controllers | HTTP `/api/*` surface; auth checks; prefer entity/DTO returns | `backend/src/main/java/com/odde/doughnut/controllers/` |
| Authorization | Read/write access by ownership, circle, bazaar, admin | `backend/src/main/java/com/odde/doughnut/services/AuthorizationService.java` |
| Domain services | Note/notebook/recall/AI/book/search business logic | `backend/src/main/java/com/odde/doughnut/services/` |
| Pure algorithms | Cloze, markdown/frontmatter, spaced repetition math | `backend/src/main/java/com/odde/doughnut/algorithms/` |
| Persistence | JPA entities, Spring Data repositories, `EntityPersister` | `backend/src/main/java/com/odde/doughnut/entities/`, `.../factoryServices/EntityPersister.java` |
| Vue SPA | Pages, components, composables, client-side note cache | `frontend/src/` |
| Generated SDK | TypeScript client from OpenAPI | `packages/generated/doughnut-backend-api/` |
| Shared API wrapper | CLI/MCP-friendly SDK setup + re-exports | `packages/donut-api/src/index.ts` |
| Test fixtures | `makeMe` builders for API-shaped data | `packages/donut-test-fixtures/src/` |
| CLI | Interactive Ink TTY + non-interactive subcommands | `cli/src/` |
| MCP server | Stdio MCP tools over backend API | `mcp-server/src/` |
| E2E harness | Cucumber features, page objects, Mountebank mocks | `e2e_test/` |

## Pattern Overview

**Overall:** Modular monorepo with a Spring Boot API backend and multiple TypeScript clients that share a generated OpenAPI SDK.

**Key Characteristics:**
- Controllers stay thin: authorize via `AuthorizationService`, delegate to services, return entities or view DTOs (e.g. `NoteRealm`).
- Path variables bind JPA entities by id (`@PathVariable Note note`) via Spring Data domain conversion.
- Frontend talks only through `@generated/doughnut-backend-api/sdk.gen`, wrapped by `apiCallWithLoading` for UX loading/errors.
- CLI and MCP reuse the same generated types via `donut-api` / direct SDK, not ad-hoc HTTP shapes.
- Dev stack assumes `pnpm sut` (backend + frontend + local LB + Mountebank) under Nix.

## Layers

**Client presentation:**
- Purpose: User UI (web, terminal, IDE agents)
- Location: `frontend/src/`, `cli/src/`, `mcp-server/src/`
- Contains: Vue pages/components, Ink React UI, MCP tool handlers
- Depends on: Generated SDK (`packages/generated/doughnut-backend-api/`), optionally `packages/donut-api/`
- Used by: End users, Cypress E2E, Cursor/IDE MCP hosts

**API / transport:**
- Purpose: HTTP contract and auth gate
- Location: `backend/src/main/java/com/odde/doughnut/controllers/`, security in `configs/ProductionConfiguration.java` / `NonProductConfiguration.java`
- Contains: `@RestController` classes under `/api/...`, OpenAPI annotations, DTOs in `controllers/dto/`
- Depends on: Services, `AuthorizationService`, entities
- Used by: All clients via local LB or GCP path routing

**Application / domain services:**
- Purpose: Use-case orchestration and domain rules
- Location: `backend/src/main/java/com/odde/doughnut/services/` (+ subpackages `ai/`, `book/`, `focusContext/`, `search/`, `openAiApis/`, `wikidataApis/`)
- Contains: Note construction/motion, recall, assimilation, embeddings, bazaar, circles, conversations
- Depends on: Repositories, `EntityPersister`, algorithms, external HTTP clients
- Used by: Controllers and scheduled jobs (e.g. `EmbeddingMaintenanceJob`)

**Domain algorithms (pure):**
- Purpose: Stateless text/SRS helpers without Spring wiring
- Location: `backend/src/main/java/com/odde/doughnut/algorithms/`
- Contains: `ClozedString`, `NoteContentMarkdown`, wiki-link parsing, property indexing planners
- Depends on: Minimal JDK / shared types
- Used by: Entities and services

**Persistence:**
- Purpose: MySQL schema and object mapping
- Location: `entities/`, `entities/repositories/`, Flyway under `backend/src/main/resources/db/migration/`
- Contains: `Note`, `Notebook`, `Folder`, `MemoryTracker`, `Book*`, embeddings, indexes
- Depends on: JPA / Hibernate, MySQL 8.4 with VECTOR
- Used by: Services and test fixtures (`makeMe` on backend tests)

**Cross-cutting config & testability:**
- Purpose: Security, scheduling, OpenAPI, exception mapping, E2E hooks
- Location: `backend/src/main/java/com/odde/doughnut/configs/`, `testability/`
- Contains: OAuth2 login (prod), exception handlers, ShedLock, `TestabilityRestController`
- Used by: Runtime and Cypress setup steps

## Data Flow

### Primary Request Path (web note show)

1. Browser hits SPA route (e.g. note show) via Vue Router (`frontend/src/routes/routes.ts`).
2. Page/composable calls generated SDK, typically through `apiCallWithLoading` (`frontend/src/managedApi/clientSetup.ts`).
3. Local LB / GCP routes `/api/*` to Spring Boot.
4. `NoteController.showNote` loads `Note` from path id, calls `authorizationService.assertReadAuthorization`, builds `NoteRealm` (`backend/.../controllers/NoteController.java`).
5. JSON `NoteRealm` returns; frontend stores/displays via note components (`frontend/src/components/notes/NoteShow.vue`, `NoteRealmLoader.vue`) and optional client cache (`frontend/src/store/`).

### CLI / MCP path

1. CLI (`cli/src/main.ts` → `run.ts`) or MCP (`mcp-server/src/index.ts`) configures API base URL + bearer token.
2. Calls go through `donut-api` / generated controllers (e.g. `RecallsController`, note search tools).
3. Same backend authorization and services as the web app.

### OpenAPI → TypeScript codegen

1. Backend controllers + DTOs define the contract.
2. `pnpm generateTypeScript` runs Gradle `generateOpenAPIDocs` → `open_api_docs.yaml` → `@hey-api/openapi-ts` into `packages/generated/doughnut-backend-api/`.
3. Clients must import generated SDK; never hand-edit generated files.

**State Management:**
- Backend: request/session-scoped current user (`CurrentUserFetcherFromRequest`), transactional services, JPA persistence context via `EntityPersister`.
- Frontend: Vue refs/composables for UI; `frontend/src/store/` for note editing history and in-memory note API cache (`createNoteStorage.ts`, `StoredApiCollection.ts`); no global Vuex/Pinia store as the primary pattern.
- CLI: Ink React component state + session scrollback (`cli/src/sessionScrollback/`).

## Key Abstractions

**Note / Notebook / Folder:**
- Purpose: Core zettelkasten graph — notes live in notebooks, optionally under folders
- Examples: `backend/.../entities/Note.java`, `Notebook.java`, `Folder.java`
- Pattern: JPA entities with soft-delete (`deletedAt`); controllers return rich views when needed

**NoteRealm:**
- Purpose: Viewer-facing aggregate for a note (topology, sidebar notebook realm, references, wiki titles)
- Examples: `backend/.../controllers/dto/NoteRealm.java`, `services/NoteRealmService.java`
- Pattern: Built in service layer after authorization; primary note-show API body

**AuthorizationService:**
- Purpose: Single gate for write vs read rights across entity types
- Examples: `backend/.../services/AuthorizationService.java`
- Pattern: Controllers call `assertAuthorization` / `assertReadAuthorization` before mutations/reads

**EntityPersister:**
- Purpose: Thin persist/merge/remove/find facade over `EntityManager`
- Examples: `backend/.../factoryServices/EntityPersister.java`
- Pattern: Prefer this (or repositories) over raw EM in new code; keep persistence decisions in services

**Generated SDK + apiCallWithLoading:**
- Purpose: Typed HTTP client with consistent loading bar / block-UI / toast behavior
- Examples: `packages/generated/doughnut-backend-api/sdk.gen.ts`, `frontend/src/managedApi/clientSetup.ts`
- Pattern: Import `*Controller` from sdk.gen; wrap user-initiated calls with `apiCallWithLoading`

**makeMe fixtures:**
- Purpose: One builder API for backend and frontend/CLI tests matching OpenAPI shapes
- Examples: `packages/donut-test-fixtures/src/makeMe.ts`
- Pattern: Prefer `makeMe.aNoteRealm()` etc. over hand-built literals

## Entry Points

**Backend HTTP:**
- Location: `backend/src/main/java/com/odde/doughnut/DoughnutApplication.java`
- Triggers: `pnpm backend:sut` / Gradle `bootRunE2E`, production JVM
- Responsibilities: Spring context, scheduled jobs, OpenAPI/migrate tasks via `DoughnutTaskRunner`

**Frontend SPA:**
- Location: `frontend/src/main.ts` → `DoughnutApp.vue`
- Triggers: Vite dev (`pnpm frontend:sut`) or static assets served behind LB
- Responsibilities: Router, toast plugin, autofocus directive, mount app

**CLI:**
- Location: `cli/src/index.ts` → `main.ts` → `run.ts`
- Triggers: `pnpm cli`, bundled `cli/dist/doughnut-cli.bundle.mjs`, install script from `/install`
- Responsibilities: Arg routing, interactive Ink session vs non-interactive commands

**MCP:**
- Location: `mcp-server/src/index.ts`
- Triggers: IDE MCP host over stdio after `pnpm mcp-server:bundle`
- Responsibilities: Register tools (`get_note_graph`, `find_most_relevant_note`), call backend API

**E2E:**
- Location: `e2e_test/features/**/*.feature` + `cypress.config.ts`
- Triggers: `pnpm cy:run` / `pnpm test` with SUT health
- Responsibilities: Cucumber scenarios against real stack + Mountebank for externals

## Architectural Constraints

- **Threading:** Spring Boot request threads for HTTP; scheduled/maintenance jobs use ShedLock (`configs/ShedLockConfig.java`) for distributed locking. Frontend and CLI are single-threaded JS event loops; Ink CLI uses React effects carefully for TTY ordering.
- **Global state:** Frontend module-level `apiStatusHandler` in `clientSetup.ts`; CLI credentials under config dir; avoid new process-wide mutable singletons in backend — prefer Spring beans and request scope (`CurrentUserFetcherFromRequest` is `@RequestScope`).
- **Circular imports:** Frontend uses `@/` path alias; keep pages → components → composables → managedApi one-way. Backend packages: controllers → services → entities/repos (do not import controllers from services).
- **Generated artifacts:** `open_api_docs.yaml` and `packages/generated/doughnut-backend-api/**` are regenerated only; never edit by hand.
- **Schema changes:** New Flyway scripts only under `backend/src/main/resources/db/migration/`; never edit committed migrations (see `db-migration` rule).
- **Auth models:** Production uses OAuth2 login (`ProductionConfiguration`); API clients use Bearer tokens; non-prod has relaxed security + testability endpoints.

## Anti-Patterns

### Hand-rolling API types in the frontend or CLI

**What happens:** Defining local interfaces that duplicate OpenAPI note/recall shapes.
**Why it's wrong:** Drift from backend DTOs breaks at runtime; fixtures and SDK already encode the contract.
**Do this instead:** Import types from `@generated/doughnut-backend-api` and build data with `donut-test-fixtures/makeMe`.

### Skipping AuthorizationService in controllers

**What happens:** Loading an entity and mutating it without `assertAuthorization` / `assertReadAuthorization`.
**Why it's wrong:** Ownership, bazaar, and circle rules are centralized; bypasses create security holes.
**Do this instead:** Follow `NoteController` — authorize first, then call services (`backend/.../controllers/NoteController.java`).

### Calling generated SDK without understanding loading wrappers

**What happens:** Using `apiCallWithLoading` for silent background polls, or raw SDK for user clicks with no error UX.
**Why it's wrong:** Users miss errors or get spurious blocking UI.
**Do this instead:** User actions → `apiCallWithLoading` (optional `{ blockUi: true }`); silent refresh → direct SDK call (see `.cursor/rules/frontend-api.mdc`).

### Editing generated OpenAPI or SDK files

**What happens:** Manual tweaks to `sdk.gen.ts` or `open_api_docs.yaml`.
**Why it's wrong:** Next `pnpm generateTypeScript` overwrites them; CI expects regeneration from controllers.
**Do this instead:** Change controller/DTO Java, then regenerate.

## Error Handling

**Policy:** `.cursor/rules/error-handling.mdc` (prevent → propagate → enrich → deliberate catch; never swallow).

**Strategy:** Map domain/API failures to structured `ApiError` JSON; log unexpected failures as failure reports; frontend toasts from wrapped calls.

**Patterns:**
- `CustomRestExceptionHandler` — validation, conflicts (e.g. duplicate note title), `ResponseStatusException` (`configs/CustomRestExceptionHandler.java`)
- `ControllerSetup` — catch-all → `FailureReportFactory`; multipart and `ApiException` / OpenAI unauthorized (`configs/ControllerSetup.java`)
- Frontend: SDK `{ error }` handled in `apiCallWithLoading` → toast + optional login redirect (`managedApi/clientSetup.ts`, `openApiError.ts`)
- CLI: map failures to user-visible assistant text via `userVisibleSlashCommandError` (`cli/src/userVisibleSlashCommandError.ts`)

## Cross-Cutting Concerns

**Logging:** Logback (`backend/src/main/resources/logback-spring.xml`); SUT logs via `pnpm logs:tail` / `scripts/logs-tail.mjs`.
**Validation:** Jakarta Validation on DTOs (`@Valid`); bean validation errors → `ApiError` binding type.
**Authentication:** OAuth2 session for browser (prod); Bearer access tokens for CLI/MCP/API; `CurrentUserFetcherFromRequest` resolves token or principal (`controllers/currentUser/`).
**Observability:** Micrometer registry config (`configs/MeterRegistryConfiguration.java`); health at `/api/healthcheck`.
**External AI:** OpenAI config (`configs/OpenAiApiConfig.java`) and `services/openAiApis/`, `services/ai/`.

---

*Architecture analysis: 2026-07-15*
