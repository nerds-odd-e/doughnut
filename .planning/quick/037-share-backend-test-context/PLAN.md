# Share one Spring context across backend tests

## Source

- Identity: test-optimization pass on the backend unit test suite, requested by
  the owner on 2026-09-25 (`/dough-test-optimization backend unit test`). No
  story or backlog entry; no time target was given.
- Execution: Story Branch Mode, worktree
  `.worktrees/037-share-backend-test-context`, branch
  `story/037-share-backend-test-context`, base `origin/main` `5c8bb75741`.

## Goal and scope

Developers wait less for `backend:test_only` because the suite boots far fewer
Spring application contexts, with the same behavioral proof and simpler test
bases (fewer per-class mock declarations, no internal-collaborator mocks).

Scope: backend JUnit tests under `backend/src/test/java` and their test bases.
No production code change is expected; if a production seam must change, stop
and record it. Excluded: the four contexts that exist on purpose (`dev`
profile, `prod` profile, and the two failure-injection profiles
`notebook-git-publication-atomic-test` and `batch-row-import-atomic-test`);
Gradle/compile time; running tests in parallel forks.

Preserved promises: every currently passing behavior stays proven; the 2
skipped cases stay as they are; controller tests keep mocking only external
services (`OpenAIClient` "officialOpenAiClient", `HttpClientAdapter`,
`GithubService`) per the `backend-testing` skill.

## Baseline (2026-09-25, revision `5c8bb75741`)

Command (the `backend-testing` skill's ordinary feedback and profiling run):
`CURSOR_DEV=true nix develop -c pnpm backend:test_only`, i.e.
`backend/gradlew -p backend test -Dspring.profiles.active=test --build-cache --parallel`.
One test JVM (no `maxParallelForks`), no filters, Gradle build cache on (test
results cleared before each run; the test task always executed). Linked
worktree runs use their own isolated database. Another session was running
backend work at the same time (load average 11–18), so wall times are noisy;
compare context boots and test-phase time alongside wall time.

| Run | Wall | Test phase | Cases | Context boots |
| --- | --- | --- | --- | --- |
| main checkout | 93.4s | 66.2s | 2,650 (2 skipped) | 20 |
| worktree base1 | 86.8s | 62.4s | 2,650 | 20 (25.4s) |
| worktree base2 | 104.9s | 83.2s | 2,650 | 21 (28.4s) |

Profile: test-case bodies sum to ~35–38s; ~26–28s of the test phase is class
setup outside test cases, almost all Spring context boots at ~1.0–1.9s each
(the first also carries JVM warm-up). A run with
`logging.level.org.springframework.test.context.cache=DEBUG` (temporary, not
committed) counted exactly 21 cache misses. Measurement helper (local, not
committed): run the command, then read `backend/build/test-results/test`
timestamps and per-class overhead.

## Family analysis: the 21 contexts

Every context differs from `ControllerTestBase` (`@SpringBootTest`,
`@ActiveProfiles("test")`, `@Transactional`, `@Import(SqlStatementCallLogDataSourceConfig)`,
`@TestBean CurrentUser`, `@MockitoBean GithubService`) by accident, except four.

| First class booting it | Difference from `ControllerTestBase` | Kind |
| --- | --- | --- |
| `AdminQuestionGenerationBatchControllerTest` | none (the base context) | keep |
| `AiAudioControllerTests` (+ `RecallPromptControllerTestBase`, `NotebookBooksControllerTestBase`, Ai* tests) | + `OpenAIClient` mock | external mock, per class |
| `NoteControllerAiContextMarkdownTests` (+ other NoteController*) | + `HttpClientAdapter` mock | external mock, per class |
| `NotebookLfsTransferControllerTest` | + `@AutoConfigureMockMvc` | MockMvc toggle |
| `LegacyBookSourceFileMoveControllerTest` (all `NotebookControllerTestBase` → NotebookGit* ~200 classes) | + `EmbeddingService` mock | internal-collaborator mock |
| `DisplayNameNormalizationMvcTest` | + MockMvc + `EmbeddingService` | both |
| `NotebookRootNoteCreationWithWikidataTests` | + `EmbeddingService` + `HttpClientAdapter` | both |
| `HealthCheckControllerTest` | + `@TestBean BuildProperties` | fixture bean |
| `ControllerSetupTest` | standalone: no Import, no CurrentUser | restated base |
| `DatabaseTimeZoneTest` | standalone: no mocks | restated base |
| `ApplicationControllerTest` | standalone + MockMvc | restated base |
| `McqTest` | standalone + `OpenAIClient` mock | restated base |
| `QuestionGenerationRequestBuilderTests` | standalone + own `CurrentUser` factory | restated base |
| `StructuredResponseCreateParamsSerializerTest` | standalone + another own `CurrentUser` factory | restated base |
| `UserDTOTest` | no profile at all; boots the app only for a `Validator` | needs no Spring |
| `QuestionGenerationBatchMaintenanceServiceTest` (+ 12 QuestionGenerationBatch* classes) | standalone + `OpenAiApiHandler` mock | internal-collaborator mock |
| `QuestionGenerationBatchSubmitDueUsersTest` | + `OpenAiApiHandler` + spy `QuestionGenerationBatchPlanningService` + `@DirtiesContext` | internal mocks |
| `DevelopmentAuthenticationConfigurationTest` | `dev` profile | on purpose |
| `ShedLockConfigProdTest` | `prod` profile | on purpose |
| `NotebookGitComposedRangePublicationAtomicControllerTest` (+10) | failure-injection profile | on purpose |
| `QuestionGenerationBatchRowImportServiceAtomicTest` | failure-injection profile, `@DirtiesContext` | on purpose |

Hypothesis: one shared test context declaring the three external-service mocks
and MockMvc once in `ControllerTestBase`, used by every Spring test of the
`test` profile, removes ~15 of 21 boots (~15–18s of the ~62–83s test phase)
and deletes per-class mock declarations. `@MockitoBean` mocks are reset after
each test, so sharing them adds no order dependence; state other than mocks
that a merged class changes (for example `TestabilitySettings`) must be reset
the way `ControllerTestBase.cleanupSharedTestState` already does.

Risks to watch: tests that today rely on the *real* `OpenAIClient` or
`HttpClientAdapter` (a null-returning mock may change their failure path);
tests that need embeddings will now go through the real `EmbeddingService`
and must stub `officialClient.embeddings()` instead.

## Experiment loop (every slice)

1. Hypothesize the boots the slice removes.
2. Measure: the focused baseline is the most recent full run above (the rule is
   to run all backend tests, not a filter); make the change; run
   `/Users/terryyin/.claude/jobs/ad680564/tmp/measure.sh <label>` equivalent
   (`CURSOR_DEV=true nix develop -c pnpm backend:test_only` from the worktree,
   then count boots and test-phase time from the XML results).
3. Decide: keep only if all tests pass, boots drop as predicted, and the test
   code is simpler (fewer declarations, no new special cases). Otherwise revise
   or undo and record why.
4. Reassess the remaining boots and whether the next slice is still worth it.

## Slices

### 1. Controller tests share the external-service mocks and MockMvc
Type: Structure
Status: done
Expected: −3 boots (OpenAIClient, HttpClientAdapter, MockMvc variants merge
into the base context).
Result: boots ~21 → ~15 (two more than expected:
`NotebookRootNoteCreationWithWikidataTests` and `DisplayNameNormalizationMvcTest`
now match `NotebookControllerTestBase`'s context). 2650 cases, 0 failures,
2 skipped; wall 73.5s / test phase 52.0s at lower load (not a clean time
comparison). 27 subclasses dropped their own declarations (+35/−134 lines,
including the refactor pass moving one test from `NoteControllerShowWikiLinkTests`
to `NoteControllerShowWikiLinkAmbiguityTests` to bring the file under 250
lines). No test needed new stubbing.
Proof: full `backend:test_only` passes with 2,650 cases (2 skipped); boot
count drops by 3.

Change: `ControllerTestBase` declares `@AutoConfigureMockMvc` and
`@MockitoBean(name = "officialOpenAiClient") OpenAIClient` and
`@MockitoBean HttpClientAdapter` once; every subclass and intermediate base
drops its own declaration of these and uses the inherited field (rename
local field references as needed). Enables slices 2 and 3.

### 2. Notebook controller tests stub the OpenAI embeddings boundary, not `EmbeddingService`
Type: Structure
Status: done
Result: boots ~15 → ~14 (the other two targets already merged in slice 1).
2650 cases, 0 failures, 2 skipped. The blanket `EmbeddingService` stub was
dead: no test in the family reaches embeddings, so it was deleted (−39 lines)
and no boundary stub was needed. Embeddings stay proven by
`services/NotebookReindexingServiceTests`, `EmbeddingServiceTests`,
`EmbeddingMaintenanceJobTests` and `SearchControllerSemanticTests`.
Expected: −3 boots (the ~200-class `NotebookControllerTestBase` family,
`DisplayNameNormalizationMvcTest`, `NotebookRootNoteCreationWithWikidataTests`
join the shared context).
Proof: full `backend:test_only` passes with the same case count; every test
that observed embeddings still observes them through the real
`EmbeddingService`.

Change: remove `@MockitoBean EmbeddingService` from
`NotebookControllerTestBase` and `DisplayNameNormalizationMvcTest`; tests that
need embeddings stub `officialClient.embeddings().create(...)` (reuse the
existing `SearchControllerSemanticTests`/`EmbeddingServiceTests` idiom or a
single helper on the base if several need it). Do not stub embeddings for
tests that never reach them.

### 3. Standalone Spring tests use the shared context
Type: Structure
Status: done
Result: boots ~14 → ~6; test phase 43.0s / 42.7s, wall 66.5s / 65.6s (two
runs, load ~5). 2650 cases, 0 failures, 2 skipped. 77 plain "test"-profile
classes (about 60 more than first listed) joined; +173/−891 lines. Refactor
extracted the shared context into `testability/SpringTestBase` (non-controller
tests extend it; `ControllerTestBase` extends it with only controller helpers)
and moved the `useRealGithub` reset into its `@AfterEach` because
`TestabilityDbResetTest`'s `init()` would otherwise leak `false`.
`UserDTOTest` runs without Spring; `HealthCheckControllerTest` asserts the
real build-info commit. `FailureReportFactoryTest` keeps its `doReturn(null)`
stub (a Mockito `Integer` answer is 0, not null).
Expected: −7 boots.
Proof: full `backend:test_only` passes with the same case count.

Change: `ControllerSetupTest`, `DatabaseTimeZoneTest`,
`ApplicationControllerTest`, `McqTest`, `QuestionGenerationRequestBuilderTests`,
`StructuredResponseCreateParamsSerializerTest` extend `ControllerTestBase` and
drop their restated annotations, mocks and `CurrentUser` factories;
`ControllerSetupTest`'s `setUseRealGithub(true)` must not leak (reset it with
the other shared testability state). `UserDTOTest` validates with a plain
Bean Validation `Validator` and no Spring context. `HealthCheckControllerTest`:
assert the commit from the real `BuildProperties` bean if the build provides
one, otherwise keep its fixture bean and record it.

### 4. Question-generation batch tests stub the OpenAI client, not `OpenAiApiHandler`
Type: Structure
Status: done
Result: boots ~6 → ~4; test phase 41.1s, wall 61.9s (load ~4). 2650 cases,
0 failures, 2 skipped. New `testability/OpenAiBatchApiMock` stubs the client's
Files/Batches APIs; the handler's parsing and file decoding now run for real
(the maintenance test feeds a real batch success line). The spy and
`@DirtiesContext` base was folded into `QuestionGenerationBatchSubmitDueUsersTest`,
which now uses the real eligibility query; it assumes no other committed user
is due in its fixed 2024-08-03 window (it is `@Isolated` and cleans its own
prefix). Only weakening: the failing batch-creation stub no longer matches the
uploaded file id. Refactor added `stubCompletedOpenAiBatch` for a sequence
repeated in six places.
Expected: −2 boots, and removal of `@DirtiesContext` and the planning-service
spy if they only exist to isolate those mocks.
Proof: full `backend:test_only` passes with the same case count.

Change: 13 `services/QuestionGenerationBatch*` classes mock the internal
`OpenAiApiHandler` (52 uses). Reassess after slice 3: convert them to the
shared context by stubbing `officialOpenAiClient` batch/file calls only if
that reads as simply as today; otherwise stop and record them as a candidate.

### 5. Re-profile the suite
Type: Structure
Status: planned
Proof: two comparable full runs of `backend:test_only` from the worktree, with
wall time, test phase, cases and context boots recorded against the baseline;
a temporary cache-DEBUG run confirms the final miss count.

## Current decisions

- Keep the four on-purpose contexts; do not merge failure-injection profiles.
- Measurement compares context boots and test-phase time as well as wall time
  because of concurrent machine load.

## Learnings

(none yet)
