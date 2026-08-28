# Technology Stack

**Analysis Date:** 2026-07-15

## Languages

**Primary:**
- Java 25 (source/target compatibility) — Spring Boot backend in `backend/`
- TypeScript 6.0.3 — frontend (`frontend/`), CLI (`cli/`), MCP server (`mcp-server/`), shared packages (`packages/`), E2E (`e2e_test/`)

**Secondary:**
- Python 3.14 — MinerU PDF outline tooling (`cli/python/mineru_book_outline.py`), Poetry project (`pyproject.toml`), E2E MinerU stubs (`e2e_test/python_stubs/`)
- Vue SFC / JSX — UI in `frontend/src/` (Vue 3.5.39)
- Shell / HCL / Salt — Nix flake (`flake.nix`), process-compose, GCP Packer (`infra/gcp/doughnut.pkr.hcl`), Salt states (`infra/gcp/salt/`)
- SQL — Flyway migrations in `backend/src/main/resources/db/migration/`

## Runtime

**Environment:**
- JVM: Azul Zulu OpenJDK 25 (`zulu25` via Nix; prod MIG uses `/usr/lib/jvm/zre-25-amd64`)
- Node.js >= 26.3 (`nodejs_26` via Nix; `package.json` `engines.node`)
- Python 3.14 (Nix `python314` + Poetry-managed venv)

**Package Manager:**
- pnpm 11.11.0 (`packageManager` / `engines.pnpm`; only-allow enforced)
- Lockfile: `pnpm-lock.yaml` present (lockfileVersion 9.0)
- Workspace: `pnpm-workspace.yaml` — `frontend`, `mcp-server`, `cli`, `packages/donut-api`
- Gradle 9.6.1 (wrapper in `backend/gradle/wrapper/`) for backend
- Poetry (`pyproject.toml`) for Python deps used by scripts/CLI MinerU path

## Frameworks

**Core:**
- Spring Boot 4.1.0 — HTTP API, JPA, Security, Flyway, OAuth2 (`backend/build.gradle`)
- Vue 3.5.39 + Vue Router 5.1.0 — SPA (`frontend/`)
- Vite 8.1.4 — frontend build/dev (`frontend/vite.config.ts`)
- Ink 7 + React 19 — interactive CLI TTY UI (`cli/`)
- Model Context Protocol SDK (`@modelcontextprotocol/sdk`) — `mcp-server/` stdio server
- Hono 4.12.30 — local load balancer (`scripts/local-lb.mjs`)

**Testing:**
- JUnit 5 (Spring Boot starter-test) — backend
- Vitest 4.1.10 + Playwright browser mode — frontend, CLI, MCP
- Cypress 15.18.1 + Cucumber (`@badeball/cypress-cucumber-preprocessor`) — E2E in `e2e_test/`
- Mountebank (`@mbtest/mountebank`, `@anev/ts-mountebank`) — external API stubs for E2E
- Stryker — CLI mutation testing (`cli/`)
- Storybook 10.5.0 — frontend component stories

**Build/Dev:**
- Nix flake (`flake.nix`, nixpkgs `nixos-26.05`) + Determinate Nix — reproducible shell; run tooling with `CURSOR_DEV=true nix develop -c …`
- process-compose (`process-compose.yaml`) — local MySQL + Redis
- esbuild — CLI and MCP single-file bundles
- Spotless + Google Java Format — Java style (`backend/build.gradle`)
- Biome 2.5.3 — TS/JS/Vue format+lint (`biome.json`)
- syncpack — workspace dependency alignment
- `@hey-api/openapi-ts` 0.99.0 — OpenAPI → TypeScript client (`openapi-ts.config.ts`)
- springdoc-openapi 3.0.3 — OpenAPI generation from controllers
- Redocly CLI — OpenAPI lint (`pnpm openapi:lint`)

## Key Dependencies

**Critical:**
- `com.openai:openai-java:4.42.0` — chat/responses, embeddings, audio transcription, batch APIs (`backend/src/main/java/com/odde/donut/configs/OpenAiApiConfig.java`)
- Spring Data JPA + Hibernate — persistence (`spring-boot-starter-data-jpa`)
- Flyway + `flyway-mysql` — schema migrations
- `com.mysql:mysql-connector-j:9.7.0` — MySQL driver
- Spring Security + OAuth2 client — GitHub login in prod (`ProductionConfiguration.java`, `application.yml`)
- `com.google.cloud:google-cloud-storage` (BOM `libraries-bom:26.85.0`) — prod book PDF storage
- `@generated/donut-backend-api` — generated SDK consumed by frontend, CLI, MCP via `packages/donut-api`
- Quill (`@dotwee/quill`), PDF.js, epub.js — note/book reading editors (`frontend/package.json`)
- Tailwind CSS 4.3.2 + DaisyUI 5.6.18 — frontend styling
- ShedLock (JDBC) — distributed scheduled job locking (`ShedLockConfig.java`)

**Infrastructure:**
- Redis 8.4 — configured in `application.yml` (host `127.0.0.1:6380`); `spring-boot-starter-data-redis` on classpath (no direct Redis API usage in application Java sources)
- Micrometer SimpleMeterRegistry — in-process metrics for question-generation batches (`MeterRegistryConfiguration.java`)
- RxJava2 — OpenAI Flowable streaming support
- Lombok — Java boilerplate
- Zod — MCP server validation

## Configuration

**Environment:**
- Spring profiles: `test` (default in `application.yml`), `e2e`, `dev`, `prod`
- DB credentials via imported property files: `db-test.properties`, `db-e2e.properties`, `db-dev.properties`
- Prod secrets injected at MIG startup from Google Secret Manager (`infra/gcp/scripts/mig-zulu25-openai-app-instance-startup.sh`)
- Critical env/system properties: `OPENAI_API_TOKEN`, `MYSQL_PASSWORD`, `OAUTH2_github_client_id` / `OAUTH2_github_client_secret`, `GITHUB_FOR_ISSUES_API_TOKEN`, `DONUT_API_BASE_URL`, `DONUT_API_AUTH_TOKEN`
- CLI Google OAuth (Gmail): `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` (bundled via esbuild defines)
- Team secrets collaboration: git-secret + GnuPG (`docs/secrets_management.md`)
- `.env*` files not present at repo root (CLI may use optional `cli/.env.local` for bundle defines — do not commit secrets)

**Build:**
- Root: `package.json`, `pnpm-workspace.yaml`, `tsconfig.json`, `biome.json`, `openapi-ts.config.ts`, `cypress.config.ts`
- Backend: `backend/build.gradle`, Gradle wrapper
- Frontend: `frontend/vite.config.ts`, Tailwind via `@tailwindcss/vite`
- Nix: `flake.nix` / `flake.lock`
- CI versions via GitHub Actions repo vars (`JAVA_VERSION`, `NODE_VERSION`, `PNPM_VERSION`)

## Platform Requirements

**Development:**
- Nix develop shell with Zulu 25, Node 26, pnpm 11.11.0, MySQL 8.4, Redis, process-compose, poetry
- Assume `pnpm sut` (system under test) already running; healthcheck via `pnpm sut:healthcheck`
- Local ports: backend e2e `9081`, frontend Vite `5174`, local LB `5173`, MySQL `3309`, Redis `6380`, Mountebank imposters for OpenAI/Google/Wikidata

**Production:**
- Google Cloud project `carbon-syntax-298809`
- Compute Engine Managed Instance Group (MIG) running Spring Boot fat JAR (Zulu 25)
- Cloud SQL MySQL 8.4 with `cloudsql_vector=on` (VECTOR embeddings); DNS alias `db-server`
- Global HTTPS load balancer + GCS backend bucket for SPA/CLI static assets (`dough-frontend-01`)
- Deploy/private ops bucket `dough-01` (JARs, deploy record)
- Book PDFs: GCS bucket `doughnut-book-pdf-carbon-syntax-298809` (prod only)
- Public site: `https://doughnut.odd-e.com`

---

*Stack analysis: 2026-07-15*
