# Spring Boot 4.2 idioms (high-value follow-ups)

Handoff for later sessions. Boot 4.1.2 → 4.2.0 landed in `802d4b2ce8`; RestTemplate → RestClient after it.
Each task: baby-step TDD (Red → Green → Refactor), then **stop**, recommend files to stage + commit msg, await instruction.
Do one task per session turn. Update the task's **Status** line when done.

## Ground rules

- Commands: `CURSOR_DEV=true nix develop -c …`; backend tests
  `./backend/gradlew -p backend test -Dspring.profiles.active=test --build-cache --parallel`;
  frontend `pnpm frontend:test`; after DTO/controller shape change `pnpm generateTypeScript`.
- Before authoring a test, run
  `node .agents/skills/dough-execute-plan/scripts/test-entry-rules.mjs --project . --source <test path>`.
- Tests assert observable behavior; no tests asserting something is absent (CLAUDE.md principle 7).
- Proof for any task = new/kept focused test + full backend suite green (+ frontend if API types change).

## Task 1 — Align Jackson 2 hibernate module with Jackson core

**Status:** Done in `9bc7f09a25`.

Problem: `jackson-datatype-hibernate7:2.23.0` pinned in `backend/build.gradle` runs on
`jackson-databind:2.21.8` (Boot BOM `jackson-2-bom`). Module newer than core = unsupported.

- Red: `ObjectMapperConfigTest.hibernateModuleMatchesJacksonCoreVersion` — module major.minor == `ObjectMapper.version()` major.minor. Fails 2.23 vs 2.21.
- Green: drop the version from `com.fasterxml.jackson.datatype:jackson-datatype-hibernate7` (BOM manages it).
- Refactor: none expected; confirm `dependencyInsight` resolves 2.21.8; full backend suite.
- Note: Task 2 deletes the Jackson 2 hibernate module, and this test goes with it.

## Task 2 — Single Jackson 3 mapper (drop Jackson 2 `JsonMapper` bean)

**Status:** Not started. Large; consider `dough-slice-planning` to split by package.

Problem: web JSON uses Boot's Jackson 3 mapper (`ObjectMapperConfig.webJson()`), but a second Jackson 2
`JsonMapper` bean (`ObjectMapperConfig.objectMapper()`) is injected in ~38 main / ~46 test files.
Two mappers = two config sets (NON_NULL, dates, hibernate) that can drift.

Baby steps (repeat per package, smallest first):
1. Red: pick one consumer; write/adjust its test to inject `tools.jackson.databind.json.JsonMapper`
   and assert the same JSON output (NON_NULL, ISO dates) it produces today.
2. Green: switch that consumer's imports to `tools.jackson.*` (annotations `com.fasterxml.jackson.annotation.*` stay).
   Jackson 3 API diffs: unchecked `JacksonException`, `JsonNode.asText()`→`asString()`,
   `ObjectMapper` immutable (builder only), `TypeReference` in `tools.jackson.core.type`.
3. Refactor: shared settings (NON_NULL, dates) moved into `webJson()` customizer if hand-written
   serialization needs them — check wire output unchanged with `RobotsTests.openApiDocsMatchCommittedYaml`
   and controller tests.
4. Last step: delete `objectMapper()` bean, `com.fasterxml.jackson.datatype:jackson-datatype-hibernate7`
   dependency, Task 1's test; sweep `git grep "com.fasterxml.jackson.databind"` (expect none in backend; record reading here).

## Task 3 — Replace RxJava 2 in OpenAI streaming

**Status:** Done in `e4a2531229`.

Problem: `io.reactivex.rxjava2:rxjava` (EOL 2021) used only by
`OpenAiApiHandler.streamResponseAsLegacyChatChunks`, `NoteConversationAiReplyService`,
`ConversationAiReplySseStream`.

Decision: kept the async openai-java client (no threading change); `Flowable` replaced by
`OpenAiApiHandler.ChunkListener` (onChunk/onError/onComplete), implemented by
`ConversationAiReplySseStream`. A `finished` flag keeps RxJava's terminal-once guarantee
(an error never also persists a partial reply). Blocking iteration deferred to Task 4.

1. Characterize (pure structural change, so no behavioral Red): `ConversationAiReplyStreamTest`
   (MockMvc) pins SSE wire output — chunk events then `done`; OpenAI 401 → `error: Bad Request`.
   `OpenAiResponseStreamMocker.failingWith(Throwable)` added. Mutation check: breaking the
   error payload turned the test red; restored → green.
2. Green: callback listener through the 3 classes.
3. Refactor: rxjava dependency + build.gradle comment deleted.
   Sweep reading 2026-11-23: `git grep -i "rxjava|io.reactivex|Flowable"` → only unrelated
   "overflowable" in frontend test helper comments.

## Task 4 — Enable virtual threads

**Status:** Done.

Decision: `spring.threads.virtual.enabled: true` in the base `application.yml` document (all profiles).
Boot then builds `SimpleAsyncTaskScheduler` instead of `ThreadPoolTaskScheduler`, so the prod
`ThreadPoolTaskSchedulerCustomizer` (scheduled-job failure reports) would silently stop applying —
replaced by `SimpleAsyncTaskSchedulerCustomizer` (Boot 4.2 `SimpleAsyncTaskSchedulerBuilderConfiguration`
applies these under `Threading.VIRTUAL`, checked in the autoconfigure jar). Prod
`spring.task.scheduling.pool.size: 3` and its test assertion deleted: no pool under virtual threads.

1. Red A: `VirtualThreadsTest` — `applicationTaskExecutor` runs tasks on a virtual thread. Was false.
   Green A: property on.
2. Red B: `SchedulingConfigTest` on a virtual-thread `SimpleAsyncTaskScheduler`: uncaught job failure
   still saved as a `scheduled-job` failure report. Did not compile. Green B: customizer type swapped.
3. Refactor: no other executor/thread settings in `backend/src/main`. Real Tomcat SSE proven by
   E2E `e2e_test/features/messages/conversation_about_a_note.feature`.
   Remaining risk before prod: Hikari pool (prod max 12) becomes the concurrency limit; load-test.

## Task 5 — JSpecify instead of Spring's deprecated nullness annotations

**Status:** Not started. Small.

Files: `configs/NullToNotFoundResponseBodyAdvice.java`, `controllers/dto/NoteTopology.java`,
`entities/Note.java`, `entities/Notebook.java` (`org.springframework.lang.Nullable/NonNull`).

1. Red: none behavioral — "test" is compiler: build with `-Xlint:deprecation` shows these warnings
   (init script: `options.compilerArgs += ['-Xlint:deprecation,removal']`).
2. Green: swap to `org.jspecify.annotations.Nullable/NonNull` (jspecify comes with Spring Framework 7).
   Check springdoc still marks `NoteTopology`/`Notebook` fields required: `pnpm generateTypeScript` → no diff.
3. Refactor: full backend suite.

## Task 6 — Remaining compiler deprecations (medium)

**Status:** Not started. Find with the `-Xlint:deprecation,removal` init script (Task 5).

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

## Task 7 — HTTP service clients instead of raw `java.net.http.HttpClient` (medium)

**Status:** Not started.

`services/RealGithubService.java` and `services/httpQuery/HttpClientAdapter.java` build requests by
hand. Boot 4 auto-configures `@HttpExchange` interfaces (base URL, timeouts via properties) that are
easier to stub. Tests mock `HttpClientAdapter` and `GithubService` (`SpringTestBase` `@MockitoBean`),
so keep those seams; Red = a test at the adapter boundary for the request each makes today.

## Task 8 — build.gradle cleanup (low)

**Status:** Not started.

- Delete the dead `buildscript { repositories }` block (plugins resolve via `plugins {}`).
- Drop `gradlePluginPortal()` from project `repositories`.
- Optional: `apply plugin: 'io.spring.dependency-management'` →
  `implementation platform(SpringBootPlugin.BOM_COORDINATES)` (Gradle native; both supported).
  Check resolved versions unchanged (`dependencyInsight` for BOM-managed artifacts) before/after.

Proof: build + full suite green; no resolved-version change.

## Task 9 — Smaller code idioms (low)

**Status:** Not started. Each bullet is its own commit.

- 6 `@Value` uses → `@ConfigurationProperties` records.
- `JdbcTemplate` (3 main files) → `JdbcClient`.
- `System.out`/`printStackTrace` in `configs/DonutTaskRunner.java` → a logger.
- `hibernate.enable_lazy_load_no_trans: true` (in `application.yml`, 3 profiles) is a Hibernate
  anti-pattern (new connection per lazy load, hides N+1). Not 4.2-specific; needs its own plan —
  turning it off surfaces `LazyInitializationException` wherever lazy loads happen outside a transaction.
