# External Integrations

**Analysis Date:** 2026-07-15

## APIs & External Services

**AI (OpenAI):**
- OpenAI API — question generation, conversation AI (Responses API + SSE), embeddings (semantic search), audio transcription (Whisper), transcription→markdown, batch question generation
  - SDK/Client: `com.openai:openai-java` via `OpenAIOkHttpClient` in `backend/src/main/java/com/odde/doughnut/configs/OpenAiApiConfig.java`
  - Auth: `OPENAI_API_TOKEN` / `spring.openai.token`
  - Default base: `https://api.openai.com/v1/` (overridable for test imposters)
  - E2E: Mountebank OpenAI imposter (`e2e_test/start/mock_services/`)

**Knowledge graph (Wikidata):**
- Wikidata EntityData + MediaWiki API — note entity enrichment and search
  - Client: custom HTTP via `backend/src/main/java/com/odde/doughnut/services/wikidataApis/` and `WikidataService`
  - Endpoints: `https://www.wikidata.org/wiki/Special:EntityData/{id}.json`, `https://www.wikidata.org/w/api.php`
  - Auth: none (public API)
  - Backend routes: `WikidataController` at `/api/wikidata`

**GitHub Issues API:**
- Creates/lists failure-report issues against configured repos
  - Client: `RealGithubService` (`backend/src/main/java/com/odde/doughnut/services/RealGithubService.java`)
  - Auth: `GITHUB_FOR_ISSUES_API_TOKEN` / `spring.github_for_issues.token`
  - Prod repo: `nerds-odd-e/doughnut`; non-prod sandbox: `nerds-odd-e/doughnut_sandbox`

**Google Gmail (CLI only):**
- OAuth2 + Gmail API for CLI `gmail` command
  - Implementation: `cli/src/commands/gmail/gmail.ts`
  - Auth: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` (device/browser OAuth)
  - Scopes: `https://www.googleapis.com/auth/gmail.readonly`
  - E2E: Google OAuth simulation + Mountebank Google stub (`e2e_test/start/mock_services/googleService.ts`)

**PDF outline (MinerU, local/optional):**
- MinerU Python pipeline for PDF heading outlines; EPUB uses spine h1–h3 (no MinerU)
  - Script: `cli/python/mineru_book_outline.py` (embedded into CLI bundle)
  - Not a hosted SaaS call; optional local install `mineru[pipeline]`
  - CI/E2E: fake package under `e2e_test/python_stubs/mineru_site/`

**Static CLI updates:**
- CLI self-update downloads from GCS public URL
  - Default: `https://storage.googleapis.com/dough-frontend-01` (`cli/src/commands/update.ts`)
  - Install scripts served by backend `InstallController` (`/install`, `/install?win32=true`)

## Data Storage

**Databases:**
- MySQL 8.4
  - Connection: Spring `spring.datasource.*` from profile property files / prod `jdbc:mysql://db-server:3306/doughnut`
  - Credentials: `db.user`/`db.password` (dev/test/e2e use `doughnut`/`doughnut`); prod `MYSQL_PASSWORD` from Secret Manager
  - Client/ORM: Spring Data JPA + Hibernate; Flyway migrations in `backend/src/main/resources/db/migration/`
  - Prod: Cloud SQL instance `doughnut-db-instance` with VECTOR (`cloudsql_vector=on`); local/dev uses `embedding_raw VARBINARY` Flyway placeholder instead of native VECTOR
  - Local ports: `3309` (dev/e2e DBs via Nix MySQL)

**File Storage:**
- Non-prod book PDFs / attachments: MySQL `AttachmentBlob` via `DbBookStorage` (`backend/src/main/java/com/odde/doughnut/services/book/DbBookStorage.java`)
- Prod book PDFs: Google Cloud Storage via `GcsBookStorage` (`BookStorageConfiguration.java`)
  - Bucket config: `doughnut.book-pdf.gcs.bucket` = `doughnut-book-pdf-carbon-syntax-298809`
  - Auth: Application Default Credentials / GCE service account (`StorageOptions.getDefaultInstance()`)
- Prod SPA + CLI binary: GCS bucket `dough-frontend-01` (public LB backend)
- Deploy artifacts (fat JAR, `deploy/last-successful-deploy.json`): GCS bucket `dough-01`
- Multipart max size: 100MB (`spring.servlet.multipart` in `application.yml`)

**Caching:**
- Redis 8.4 on `127.0.0.1:6380` (all Spring profiles in `application.yml`; CI/local via process-compose / GitHub Actions Redis service)
- `spring-boot-starter-data-redis` and `spring-session-core` declared; no application Java code references Redis APIs directly — treat as infrastructure dependency for session/runtime stack
- Frontend uses browser `sessionStorage` for UI prefs only (e.g. note sidebar sort)

## Authentication & Identity

**Auth Provider:**
- Production web: GitHub OAuth2 Login (`spring.security.oauth2.client.registration.github` in `application.yml` prod profile)
  - Implementation: `ProductionConfiguration.java` + Spring Security OAuth2 client
  - Secrets: `OAUTH2_github_client_id`, `OAUTH2_github_client_secret` (Secret Manager)
- Non-prod (test/e2e): HTTP Basic + in-memory users (`NonProductConfiguration.java`); frontend `/users/identify` flow; remember-me always on
- API tokens for CLI/MCP: user-generated Bearer tokens (`UserController`, `CurrentUserFetcherFromRequest.java`)
  - Clients set `DOUGHNUT_API_AUTH_TOKEN` and `DOUGHNUT_API_BASE_URL` (`packages/doughnut-api/src/index.ts`)
- CLI Google OAuth: separate from web login; used for Gmail command only

## Monitoring & Observability

**Error Tracking:**
- Failure reports persisted in DB and optionally filed as GitHub issues (`FailureReportFactory`, `FailureReportController`)
- No Sentry/Datadog/New Relic detected

**Logs:**
- Logback (`backend/src/main/resources/logback-spring.xml`) — console + rolling files under `logs/` for `dev`/`e2e`
- Prod MIG: stdout/stderr captured by Cloud Logging agent (`mig-zulu25-openai-app-instance-startup.sh`)
- Local log tail: `pnpm logs:tail` (`scripts/logs-tail.mjs`)

**Metrics:**
- Micrometer `SimpleMeterRegistry` for question-generation batch counters (`QuestionGenerationBatchMetrics.java`) — in-process only, not exported to a remote backend

## CI/CD & Deployment

**Hosting:**
- Google Cloud Platform — Compute Engine MIG, Cloud SQL, GCS, Global HTTPS LB, Secret Manager, Cloud Logging
- Packer + SaltStack for MIG base images (`infra/gcp/doughnut.pkr.hcl`, `infra/gcp/salt/`)
- Legacy DigitalOcean Packer artifact present (`infra/digital_ocean/packer.json`) — not the active prod path

**CI Pipeline:**
- GitHub Actions: `.github/workflows/ci.yml` (lint, backend/frontend/other unit tests, E2E, package artifacts, conditional MIG deploy)
- Related: `cli-release.yml`, `mig_status_check.yml`
- Notifications: Slack webhook on CI failure (`SLACK_WEBHOOK_URL`); Discord webhook env present but action commented out
- Auth to GCP in CI: `GCP_CREDENTIALS` secret via `google-github-actions/auth`

## Environment Configuration

**Required env vars (critical):**
- `OPENAI_API_TOKEN` — OpenAI
- `MYSQL_PASSWORD` — prod DB (Secret Manager)
- `OAUTH2_github_client_id` / `OAUTH2_github_client_secret` — prod web login
- `GITHUB_FOR_ISSUES_API_TOKEN` — failure-report issues
- `DOUGHNUT_SPA_PUBLIC_BASE_URL` — prod SPA public URL (default `https://doughnut.odd-e.com`)
- `DOUGHNUT_API_BASE_URL` / `DOUGHNUT_API_AUTH_TOKEN` — CLI and MCP → backend
- `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` — CLI Gmail OAuth (and CI jar reproducibility)
- CI-only: `GCS_BUCKET`, `GCS_FRONTEND_BUCKET`, `GCP_CREDENTIALS`, DB user/password secrets, Slack/Discord webhooks

**Secrets location:**
- Local team files: git-secret encrypted (see `docs/secrets_management.md`)
- Production runtime: Google Secret Manager (fetched in MIG startup script)
- CI: GitHub Actions secrets / variables
- Never commit `.env` or credential JSON; `GOOGLE_APPLICATION_CREDENTIALS` path may be used for local gcloud ops (documented in `docs/gcp/prod_env.md`)

## Webhooks & Callbacks

**Incoming:**
- None detected as first-class app webhook endpoints (OAuth redirects handled by Spring Security OAuth2 client for GitHub login)

**Outgoing:**
- GitHub Issues API calls from failure reporting (`RealGithubService`)
- CI → Slack (and optionally Discord) failure notifications (workflow-level, not application runtime)
- OpenAI / Wikidata / Gmail HTTP clients as request/response (not webhook subscriptions)

---

*Integration audit: 2026-07-15*
