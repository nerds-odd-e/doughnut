# Test Optimization Candidates

## Live candidates

### e2e-batch-cold-start

- **Affected test or fixture:** the owned stack that `pnpm cy:run` starts for a session's first local batch (`scripts/e2e-runner.mjs`, `scripts/sut-services.mjs`); seen on `e2e_test/features/cli/cli_notebook_lfs.feature`.
- **Observed cost and conditions:** focused local wall time. 2026-11-01 at `ee6ecf2549`: cold first LFS batch ~4m18s vs warm under ~1 minute. 2026-11-02 at `6c045ccfd5`: first run in a fresh checkout with a warm `~/.gradle` build cache 55.0s vs warm 41.9/43.3s, 10/10 scenarios each; of the ~12.4s gap, Cypress healthy→run-start +6.8s (unattributed) and backend readiness +4.2s (Gradle first configuration plus `compileJava` FROM-CACHE, Flyway first migration ~0.7s). macOS, 16 cores, shared host load 3–20.
- **Unique protection:** a fresh owned stack with cold caches and boot on a session's first batch, with real readiness, cleanup, HMR and Cucumber proof.
- **Alternatives tried:** SEED-039#candidate-resolution measured warm readiness polling only. SEED-098#remaining-test-costs attributed stages from existing logs and found no removable stage, so no startup change was made. Flyway first migration of the per-worktree database is inherent under ADR 0007.
- **Decision or evidence needed:** a stage breakdown of the Cypress launch window (config, `setupNodeEvents`, browser launch) on a genuine first run in another freshly prepared worktree versus a warm run, and a first run with a `compileJava` build-cache miss, to show whether real compilation explains the historical ~4m18s.

### frontend-sibling-spec-setup

- **Affected test or fixture:** remaining cohesive sibling Vitest families under `frontend/tests/` (historically ~25 families of 2–3 siblings), including the BookReadingPage marking, notebookSwitch, readingPosition and base specs beside the merged visibility spec.
- **Observed cost and conditions:** retained merges historically saved ~70–250ms per removed file on the focused ordinary command. 2026-11-02 at `4ae0329ed8`: merging the mounted `AiResponse` streaming/unknown-tool pair saved ~0.2s focused (6/6 alternating pairs); merging the `aiReplyState` content/toolCalls protocol pair showed no gain. The full suite's ~10s load-following spread cannot resolve either.
- **Unique protection:** each family's distinct component behavior, harness, mock boundaries and real/fake timer requirements; per-file browser isolation stays.
- **Alternatives tried:** responsibility-based merges (346→295 files); the reading-panel pair merged 2026-11-01; the mounted reply merge kept and the protocol merge undone 2026-11-02. Do not retry the `aiReplyState` protocol pair without new evidence.
- **Decision or evidence needed:** alternating focused ordinary-command timings for each unpicked family, judging a merge only by that, not by the full suite.

### hosted-assistant-access-expiry

- **Affected test or fixture:** `e2e_test/features/mcp/assistant_account_linking.feature::A hosted SDK assistant renews access until I withdraw permission`; the wait in `e2e_test/support/hosted_assistant_client.mjs` `waitForAccessExpiry`, with the 10s lifetime set in `e2e_test/support/hosted_assistant_fixture.mjs`.
- **Observed cost and conditions:** CI case time 12.6s and 12.8s (runs 43259460645 at `a13ab0cb63` and 43254480442 at `67ad873c49`, 2026-11-12); about 11s of it is waiting for the real 10s access lifetime to pass.
- **Unique protection:** the hosted SDK assistant's first reads use its initial grant, then a real server-side expiry forces an automatic renewal before withdrawal.
- **Alternatives tried:** none tried. A shorter lifetime would save time only if connecting and the first reads still finish before expiry on slow CI runners; otherwise the first reads would silently renew or the scenario would flake. No controllable clock reaches Spring Authorization Server's token expiry.
- **Decision or evidence needed:** CI step timings of the connect and first-read steps to choose a shorter lifetime with margin, or a check that a premature renewal fails loudly.

### cli-git-round-trip-scenarios

- **Affected test or fixture:** the installed-CLI Git round-trip features under `e2e_test/features/cli/` (`cli_notebook_*`: web/local reconciliation, note and folder moves, trash, renames, clone, LFS, publish to clean clone, folder relocation, existing note edits).
- **Observed cost and conditions:** CI case times, run 43259460645 at `a13ab0cb63` (2026-11-12): 32 scenarios at 3.3–5.7s each, together ~142s (from the job logs). The CLI shard is CI's slowest E2E job (7m47s).
- **Unique protection:** real clone, pull, rebase and publish subprocess protocol behavior between the installed CLI, Git and the backend.
- **Alternatives tried:** earlier passes halved CLI E2E time and retired duplicate single-commit and single-file-rebase journeys. This pass found no fixed waits in these scenarios.
- **Decision or evidence needed:** per-step timings (CI keeps only per-feature Cucumber fragments) to separate the per-scenario CLI install, seeding, Git binding snapshot, and each CLI subprocess before choosing an experiment.

### frontend-full-suite-browser-flakes

- **Affected test or fixture:** `frontend/tests/notes/sidebar/SidebarRowReveal.spec.ts` (`shows the whole opened note row with nothing drawn over it`, `scrolls a row cut off at the bottom just enough to show all of it`: `expected false to be true`); Vitest browser worker connection for `frontend/tests/store/noteStore.spec.ts`, `frontend/tests/notes/NoteNewButton.spec.ts` and `frontend/tests/components/recall/ViewMemoryTrackerLink.spec.ts` (`Failed to run the test … Cannot connect to the server in 60 seconds`).
- **Observed cost and conditions:** `pnpm frontend:test` (chromium headless), macOS local. 2026-11-22 at `d133deca2a` + Boot 4.2 working changes, concurrent with the backend Gradle suite: NoteNewButton connect timeout, 2576/2576 tests passed, run exit 1. 2026-11-23 at `e4a2531229`, `pnpm frontend:verify` alone: SidebarRowReveal 2 failures + noteStore connect timeout (2568/2570, ~306s); both files then passed in isolation (15/15) and an immediate full rerun passed 2580/2580. 2026-11-23 at `92aa3d067f` + virtual-threads working changes, run right after the backend suite and an E2E feature: ViewMemoryTrackerLink connect timeout, 2578/2578 tests passed, run exit 1; the file passed alone. The changes under test were backend-only (no frontend, generated client or OpenAPI diff), so these are suite-load flakes. A failed run also skips the chained `frontend:build`.
- **Unique protection:** SidebarRowReveal: real-browser geometry of the opened sidebar row (unobscured, minimally scrolled into view). The connect-timeout files: their own spec content; the failure is runner infrastructure.
- **Alternatives tried:** none.
- **Decision or evidence needed:** repeated full-suite runs to measure failure rate; for SidebarRowReveal, whether its geometry assertions wait for layout/scroll to settle (vs reading once); for connect timeouts, Vitest browser worker count and connect timeout under full-suite load.

## Spring Boot 4.2 idiom follow-ups

### Task 6 — Remaining compiler deprecations (medium)

**Status:** Not started. Find with a Gradle init script adding
`tasks.withType(JavaCompile).configureEach { options.compilerArgs += ['-Xlint:deprecation,removal'] }`.

- MCP SDK (`backend/src/main/java/com/odde/donut/mcp/`): `Tool.builder()` in 6 tools
  (`GetNoteGraphTool`, `ListNoteCandidatesTool`, `ListRecallAnswersTool`, `ReadNoteTool`,
  `RecordLearningSessionTool`, `RequestLearningSessionTool`) and `new TextContent(String)` in 5
  places (`GetNoteGraphTool`, `ListNoteCandidatesTool`, `McpJsonToolResult`, `McpNoteLookup`,
  `RequestLearningSessionTool`) → SDK's newer builder/factory methods (check SDK 2.2.0 javadoc).
- Hibernate `Interceptor.onSave`/`onDelete` in `services/notebookGit/ProjectionChangeCapture.java`
  → `onPersist`/`onRemove`.
- JGit `RefDatabase.getRefs(String)` in `services/notebookGit/objectstore/JdbcNotebookRefDatabase.java`
  → `getRefsByPrefix`.
- Tests: `HttpStatus.PAYLOAD_TOO_LARGE` → `CONTENT_TOO_LARGE` (`ControllerSetupMultipartExceptionTest`);
  `JsonNode.asText()` → `asString()` (Jackson 3) or `textValue()` (`MemoryTrackerQuestionPersistenceControllerTest`);
  log `Level.ALL` (`AiControllerNoteRefinementTest`); `FileObject.Status` (`testability/OpenAiBatchApiMock`).

TDD: existing tests cover each call site (MCP endpoint tests, notebook Git tests); per group:
confirm the covering test, swap the API, run it, then the full suite. Proof of done: lint build shows no
deprecation warnings for these files.

### Task 7 — HTTP service clients instead of raw `java.net.http.HttpClient` (medium)

**Status:** Not started.

`services/RealGithubService.java` and `services/httpQuery/HttpClientAdapter.java` build requests by
hand. Boot 4 auto-configures `@HttpExchange` interfaces (base URL, timeouts via properties) that are
easier to stub. Tests mock `HttpClientAdapter` and `GithubService` (`SpringTestBase` `@MockitoBean`),
so keep those seams; Red = a test at the adapter boundary for the request each makes today.

### Task 8 — build.gradle cleanup (low)

**Status:** Not started.

- Delete the dead `buildscript { repositories }` block (plugins resolve via `plugins {}`).
- Drop `gradlePluginPortal()` from project `repositories`.
- Optional: `apply plugin: 'io.spring.dependency-management'` →
  `implementation platform(SpringBootPlugin.BOM_COORDINATES)` (Gradle native; both supported).
  Check resolved versions unchanged (`dependencyInsight` for BOM-managed artifacts) before/after.

Proof: build + full suite green; no resolved-version change.

### Task 9 — Smaller code idioms (low)

**Status:** Not started. Each bullet is its own commit.

- 6 `@Value` uses → `@ConfigurationProperties` records.
- `JdbcTemplate` (3 main files) → `JdbcClient`.
- `System.out`/`printStackTrace` in `configs/DonutTaskRunner.java` → a logger.
- `hibernate.enable_lazy_load_no_trans: true` (in `application.yml`, 3 profiles) is a Hibernate
  anti-pattern (new connection per lazy load, hides N+1). Not 4.2-specific; needs its own plan —
  turning it off surfaces `LazyInitializationException` wherever lazy loads happen outside a transaction.
