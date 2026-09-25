---
name: backend-testing
description: "Backend Java tests, controller-level stable-boundary tests, JUnit, @Nested, MakeMe builders, @Transactional, parameterized tests. Use when writing or changing backend JUnit tests."
paths:
  - "backend/src/test/**/*.java"
---
# Backend Testing Rules

**Style ("small test" practice — stable boundary, data over mocks, focused assertions, concise makeMe):** the `unit-testing` skill — follow that first.

## Commands

Run backend verification from the repo root:

```bash
CURSOR_DEV=true nix develop -c pnpm backend:verify
```

When no database migration is involved, this is faster:

```bash
CURSOR_DEV=true nix develop -c pnpm backend:test_only
```

Always run all backend unit tests instead of a selected file or test case.

For `dough-test-optimization`, use `backend:test_only` as both the ordinary
feedback measurement and profiling run. Per-test timings are the `time`
attributes in `backend/build/test-results/test/TEST-*.xml`; keep any derived raw
profile outside committed files.

## Core Principles

Backend application of the "small test" style (`unit-testing` skill):

1. Prefer **controller** (or other HTTP-stable-boundary) tests for behavior users see through HTTP. These tests often do not reference the internal class you edited; cover services/repos via realistic `makeMe` preconditions and the real DB.
2. Test services and algorithms directly only when they are an independent, intentional domain-stable contract (pure logic or algorithms).
3. Keep tests small and focused: one behavior per test and descriptive names that explain the behavior.

Controller-style example:

```java
@Test
void shouldBeAbleToSaveNoteWhenValid() throws UnexpectedNoAccessRightException {
  Note note = makeMe.aNote().creatorAndOwner(userModel).please();
  final NoteRealm noteRealm = controller.show(note);
  assertThat(noteRealm.getId(), equalTo(note.getId()));
}
```

Prefer injecting the controller on `ControllerTestBase` (as above). Every Spring test of the `test` profile extends `SpringTestBase` (`testability` package), directly or through `ControllerTestBase`, so the suite shares one application context. Do not add `@AutoConfigureMockMvc` or extra `@MockitoBean` / `@TestBean` on a subclass unless that **exact** annotation mix already exists — each unique mix caches another ApplicationContext and Hikari pool; CI MySQL then fails with `Too many connections` while a local run still passes. To observe a package-private persistence table, query through `EntityManager` rather than opening a new MockMvc context.

Independent algorithm example:

```java
@ParameterizedTest
@CsvSource({
  "moon,     partner of earth,          partner of earth",
  "Sedition, word sedition means this,  word [...] means this"
})
void clozeDescription(String title, String markdown, String expectedClozeDescription) {
  assertThat(
      new ClozedString(clozeReplacement, markdown).hide(new NoteTitle(title)).maskedContentAsMarkdown(),
      containsString(expectedClozeDescription));
}
```

## Database Tests

- Tests use actual database interactions with `@Transactional`.
- This gives confidence in database operations and repository behavior.
- A `@Formula` field (for example `Note.trashedInDatabase`) is hydrated only when Hibernate loads the row from the database. An entity persisted or moved earlier in the same persistence context still carries the Java default, so a test that reads such a field through a loaded collection must `makeMe.refresh(entity)` first. Query-level predicates (`Note.JPA_AVAILABLE`) evaluate in SQL and need no refresh.

```java
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RestNoteControllerTests {
  // ...
}
```

## MakeMe Builders

- Use the central `makeMe` factory; chain methods; end with `please()` (or `please(boolean)` for persistence control).
- Builders handle relationships and defaults — see `unit-testing` skill for when to extend them vs set fields in the test.
- Ownership: prefer `notebookOwnedBy(user)` so `aMemoryTrackerFor(note)` inherits the owner.

```java
Note note = makeMe.aNote()
                 .notebookOwnedBy(user)
                 .title("title")
                 .content("description")
                 .please();
```

## Test Organization

- Group related tests with `@Nested`.
- Use `@BeforeEach` for common setup, keeping setup minimal and relevant to the group.
- Mocking policy: `unit-testing` skill. Backend exception: external services only — mock `OpenAIClient` structured Responses output with `OpenAiStructuredResponseMock` in controller tests.

## Assertions

- Use `assertThat` with descriptive matchers; `assertThrows` for exceptions; `@ParameterizedTest` when inputs vary but the assertion focus stays the same.
- Assertion **scope** and avoiding cross-test redundancy: `unit-testing` skill.
