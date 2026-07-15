# Testing Patterns

**Analysis Date:** 2026-07-15

## Test Framework

**Runner:**
- **Backend:** JUnit 5 (Jupiter) via Gradle `useJUnitPlatform()` in `backend/build.gradle`. Spring Boot `@SpringBootTest` + `@ActiveProfiles("test")` + `@Transactional`.
- **Frontend:** Vitest with Playwright browser mode (`@vitest/browser-playwright`) — config `frontend/vitest.config.ts`. Prefer real Chromium rendering; do not use jsdom.
- **CLI:** Vitest (Node) — `cli/vitest.config.ts`, include `tests/**/*.test.ts(x)`.
- **MCP server:** Vitest (Node) — `mcp-server/vitest.config.ts`, include `tests/**/*.test.ts`.
- **E2E:** Cypress 15.x + `@badeball/cypress-cucumber-preprocessor` — config `e2e_test/config/ci.ts`.
- **Shell scripts:** Bach — `scripts/test/vendor/bach.sh`, tests `scripts/test/*.test`.

**Assertion Library:**
- **Java:** Hamcrest `assertThat` + matchers; `assertThrows` for exceptions.
- **Frontend / CLI / MCP:** Vitest `expect`.
- **E2E:** Chai via Cypress (`expect(...).to.equal`); custom messages with expected/actual.

**Run Commands:**

```bash
# Backend (full suite — preferred; no single-file focus for agents)
CURSOR_DEV=true nix develop -c pnpm backend:test_only
# With migrations: pnpm backend:test  or  pnpm backend:verify

# Frontend (all unit tests; or single file while debugging)
CURSOR_DEV=true nix develop -c pnpm frontend:test
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/path/to/TestFile.spec.ts
CURSOR_DEV=true nix develop -c pnpm -C frontend test -t "test name pattern"
# UI / watch: pnpm frontend:test:ui  /  pnpm frontend:test:watch

# CLI / MCP
CURSOR_DEV=true nix develop -c pnpm cli:test
CURSOR_DEV=true nix develop -c pnpm mcp-server:test

# E2E — targeted specs only (default for agents)
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/path/to.feature
# Do not use: cypress run -- --spec  (empty -- drops --spec)
# Assume pnpm sut is already running; check: pnpm sut:healthcheck

# Lint/format related to tests
CURSOR_DEV=true nix develop -c pnpm cy:lint
CURSOR_DEV=true nix develop -c pnpm format:all
```

## Test File Organization

**Location:**
| Area | Production | Tests |
|------|------------|-------|
| Backend | `backend/src/main/java/` | `backend/src/test/java/` (mirror packages; prefer controller-level suites) |
| Frontend | `frontend/src/` | `frontend/tests/` (mirror domains: `components/`, `pages/`, `notes/`, `utils/`, …) |
| CLI | `cli/src/` | `cli/tests/` (`.test.ts` / `.test.tsx`; interactive under `cli/tests/interactive/` when present) |
| MCP | `mcp-server/src/` | `mcp-server/tests/` |
| E2E | app under test | `e2e_test/features/`, `step_definitions/`, `start/pageObjects/` |
| Shared API fixtures | — | `packages/doughnut-test-fixtures/` |
| Scripts | `scripts/` | `scripts/test/*.test` |

**Naming:**
- Backend: `*Test.java` / `*Tests.java` (both used — e.g. `BooksControllerTest`, `NoteControllerTests`).
- Frontend: `*.spec.ts`.
- CLI / MCP: `*.test.ts` / `*.test.tsx`.
- E2E features: capability snake_case `.feature` files.
- Name tests by **capability**, never by phase number.

**Structure:**
```
backend/src/test/java/com/odde/doughnut/
  controllers/          # Preferred behavior entry (extends ControllerTestBase)
  algorithms/           # Pure logic / parameterized contracts
  testability/          # MakeMe + builders + OpenAI mocks
  configs/, validators/, …

frontend/tests/
  helpers/              # mockSdkService, RenderingHelper, wrapSdkResponse
  components/, pages/, notes/, recall/, utils/, composables/, …

e2e_test/
  features/<domain>/*.feature
  step_definitions/*.ts
  start/pageObjects/    # fluent page objects; cli/ for CLI
  support/, config/
```

## Test Structure

**Suite Organization (backend):**

```java
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BooksControllerTest extends ControllerTestBase {
  @Autowired BooksController booksController;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Nested
  class GetBookFileByBook {
    @Test
    void rejectsNotebookWithoutReadAccess() {
      Notebook otherNb = otherUsersNotebookWithBook();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> booksController.getBookFile(webRequest(), bookOf(otherNb)));
    }
  }
}
```

Base class: `backend/src/test/java/com/odde/doughnut/controllers/ControllerTestBase.java` (`MakeMe`, `CurrentUser` `@TestBean`, testability cleanup).

**Suite Organization (frontend):**

```typescript
import { describe, it, expect, beforeEach } from "vitest"
import { render } from "@testing-library/vue"
import helper, { mockSdkService } from "@tests/helpers"
import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import makeMe from "doughnut-test-fixtures/makeMe"

beforeEach(() => {
  mockSdkService(NoteController, "showNote", makeMe.aNoteRealm.please())
})

describe("LoadingModal", () => {
  it("should render with spinner and message when show is true", () => {
    const { getByText } = render(LoadingModal, {
      props: { show: true, message: "AI is creating note..." },
    })
    expect(getByText("AI is creating note...")).toBeTruthy()
  })
})
```

**Patterns:**
- **Observable behavior first:** Drive controllers, mounted pages/components, CLI `run` / `runInteractive`, or E2E — not a 1:1 map of production classes (`.cursor/skills/phased-planning/SKILL.md`, backend/frontend testing rules).
- Group with `@Nested` (Java) or `describe` (TS); minimal `@BeforeEach` / `beforeEach` shared setup.
- One behavior per test; descriptive names.
- Frontend: use `data-testid` selectors; avoid `getByRole` / `findByRole` (slow visibility checks). Prefer `getByText`, `getByLabelText`, `getByTitle`, or `querySelector`.
- Prefer `@testing-library/vue` `render()`; use `@vue/test-utils` `mount()` only for emits/slots/provide-inject edges. Query the DOM, not child component instances.
- Components needing note storage: `helper.component(X).withStorageProps({...}).mount()` via `frontend/tests/helpers/RenderingHelper.ts`.

## Mocking

**Framework:**
- Backend: Mockito `@MockitoBean` (e.g. `OpenAIClient`); `OpenAiStructuredResponseMock` / stream mockers under `backend/src/test/java/com/odde/doughnut/testability/`.
- Frontend / CLI: Vitest `vi.spyOn`.
- E2E: Mountebank + `mock_services` fluent stubs in step defs; tags like `@usingMockedOpenAiService`, `@mockBrowserTime`.

**Patterns:**

```typescript
// Frontend — type-safe SDK mock (auto-wraps { data, error, request, response })
import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"

const spy = mockSdkService(NoteController, "showNote", makeMe.aNoteRealm.please())
spy.mockResolvedValue(wrapSdkResponse(differentNote))

// Custom async logic
mockSdkServiceWithImplementation(TextContentController, "updateNoteContent", async (options) => {
  return await someAsyncOperation(options)
})
```

```typescript
// CLI — spy generated doughnut-api controllers; build data with makeMe
vi.spyOn(RecallsController, "recalling").mockResolvedValue({
  data: makeMe.aDueMemoryTrackersList.please(),
} as Awaited<ReturnType<typeof RecallsController.recalling>>)
```

**What to Mock:**
- External services (OpenAI, Wikidata, browser time, etc.).
- Generated HTTP SDK controllers in frontend/CLI unit tests — not transport servers for happy paths.
- E2E: mock external deps; use real app + DB against SUT.

**What NOT to Mock:**
- Backend: prefer real DB transactions over mocking repositories for behavior tests.
- Frontend: prefer browser rendering over mocking Vue/DOM; stop using jsdom.
- CLI: do not use `http.createServer` to fake `/api/…` for ordinary command behavior; do not use fixed `sleep`/`setTimeout(N)` waits — poll with `setImmediate` / helpers in `cli/tests/inkTestHelpers.ts` until an observable condition holds.
- Do not mock internals just to mirror a new private module if an entry-point test already covers the behavior.

## Fixtures and Factories

**Test Data:**

```java
// Backend MakeMe (persisting builders)
Note note = makeMe.aNote()
    .creatorAndOwner(userModel)
    .titleConstructor("title")
    .content("description")
    .please(); // please(false) to skip persist
```

```typescript
// Frontend / CLI — API-shaped builders
import makeMe from "doughnut-test-fixtures/makeMe"

const note = makeMe.aNoteRealm
  .topicConstructor("Dummy Title")
  .content("Description")
  .please()
```

**Location:**
- Backend: `MakeMe` / builders in `backend/src/test/java/com/odde/doughnut/testability/` (`NoteBuilder`, `NotebookBuilder`, `UserBuilder`, …).
- Shared TS: `packages/doughnut-test-fixtures/` (`makeMe.ts` + `*Builder.ts`). Import only `doughnut-test-fixtures/makeMe`.
- E2E: Given steps + data tables; inject via testability APIs; clean up after each test. Notebook note tables place children under the previous row when batch-created (no `Folder` column).

## Coverage

**Requirements:**
- Frontend Vitest: coverage **disabled** in `frontend/vitest.config.ts` (`coverage.enabled: false`).
- MCP: optional Vitest v8 coverage config present in `mcp-server/vitest.config.ts` — not a gate for normal development.
- Backend: no Jacoco gate required for routine PR work; confidence comes from Spring + DB controller tests.
- CLI: Stryker mutation testing available (`pnpm test:mutation` under `cli/` via `mutation-testing` skill) — use only when explicitly requested or a plan phase includes it.
- Philosophy: observable behavior and E2E for happy paths; unit tests for pure algorithms, edges, and non–happy-path. Production happy-path code should be justified by E2E or equivalent, not unit tests alone (phased-planning skill).

**View Coverage:**
```bash
# MCP example (if needed)
CURSOR_DEV=true nix develop -c pnpm -C mcp-server exec vitest run --coverage
```

## Test Types

**Unit Tests:**
- **Backend algorithms:** Direct tests under `backend/src/test/java/com/odde/doughnut/algorithms/` (e.g. `ClozeDescriptionTest` with `@ParameterizedTest` / `@CsvSource`).
- **Frontend utils/composables/models:** `frontend/tests/utils/`, `composables/`, `models/` — pure inputs → outputs.
- **CLI:** argv routing via `run` (`cli/tests/index.test.ts`); interactive via `runInteractive` + mock TTY / Ink helpers.
- **MCP:** tool registry shape and utils in `mcp-server/tests/`.

**Integration Tests:**
- Backend controller tests with real MySQL transactions (`ControllerTestBase`) — primary backend style.
- Frontend component/page tests in browser mode with mocked SDK + real Vue render.
- Infra/path-routing Node tests under `infra/` (also run from `lint:all` scripts).

**E2E Tests:**
- Cypress + Cucumber under `e2e_test/`. Default tag filter: `not @ignore` (CI also excludes `@wip`). Do not commit `@focus` / `@only` (`scripts/check_focus_tags.sh`).
- Thin step defs → fluent page objects in `e2e_test/start/pageObjects/`. CLI E2E uses `cli.*` page objects; interactive TTY depth belongs in `cli/tests/interactive/` Vitest, not a second Cypress PTY stack.
- Origin: `http://localhost:5173` through local load balancer.
- Prefer dumb automation: one path per scenario; distinguish variants with different steps/methods, not runtime mode flags.
- Before hooks: DB reset at `order: 0`; long-lived side effects after that (see e2e-authoring rule).

## Common Patterns

**Async Testing:**

```typescript
// Prefer explicit assertions over if-guards
expect(vm.searchResults).toBeDefined()
expect(vm.searchResults.length).toBeGreaterThan(0)

// CLI Ink: wait for observable frames (no wall-clock sleep)
import { waitForLastFrameToInclude, renderInkWhenCommandLineReady } from "./inkTestHelpers"
```

**Error Testing:**

```java
assertThrows(
    UnexpectedNoAccessRightException.class,
    () -> controller.show(note));
```

```typescript
// Frontend SDK error shape
spy.mockResolvedValue(wrapSdkError("message"))
// or wrapSdkError({ errors: { field: "required" } })
```

**Parameterized (backend):**

```java
@ParameterizedTest
@CsvSource({
  "moon,     partner of earth,          partner of earth",
  "Sedition, word sedition means this,  word [...] means this"
})
void clozeDescription(String title, String markdown, String expected) { … }
```

**E2E Gherkin + fluent steps:**

```typescript
When("I start a conversation about the note {string}", (noteTopology: string) => {
  start.jumpToNotePage(noteTopology).startAConversationAboutNote()
})
```

**TDD / phase workflow:**
1. Add/change failing test (or `@wip` E2E scenario).
2. Confirm failure reason is correct.
3. Implement smallest change to pass.
4. Refactor; remove `@wip` when green.
5. Run targeted E2E `--spec` for touched behavior, not the full suite unless asked.

**Frontend CI flake controls:** `retry: 1` on CI, `fileParallelism: !isCI`, `orchestratorGcReporter` in `frontend/vitest.config.ts`.

**Determinism:**
- Each test creates its own data; avoid shared mutable state.
- No hardcoded waits in E2E; no fixed-time sleeps in CLI unit tests.
- Use assertions instead of conditional branches that hide failures.

---

*Testing analysis: 2026-07-15*
