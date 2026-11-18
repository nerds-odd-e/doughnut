---
name: backend-testing
description: "Backend Java tests, controller-level stable-boundary tests, JUnit, @Nested, MakeMe builders, @Transactional, parameterized tests. Use when writing or changing backend JUnit tests."
paths:
  - "backend/src/test/**/*.java"
---
# Backend Testing Rules

Follow the [unit-testing skill](../unit-testing/SKILL.md) for shared guidance
and Donut proof conventions.

## Commands

Run backend verification from the repo root. For focused feedback, use a
Gradle test filter:

```bash
CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests '<pattern>'
```

When broader verification is needed, run the whole suite:

```bash
CURSOR_DEV=true nix develop -c pnpm backend:test_only
```

For database migration verification, use:

```bash
CURSOR_DEV=true nix develop -c pnpm backend:verify
```

See [worktree backend tests](../../../docs/worktree-backend-tests.md) for the
checkout's test database and runner options.

For `dough-test-optimization`, use `backend:test_only` as both the ordinary
feedback measurement and profiling run. Per-test timings are the `time`
attributes in `backend/build/test-results/test/TEST-*.xml`; keep any derived raw
profile outside committed files. To count Spring context boots, temporarily add
`logging.level.org.springframework.test.context.cache=DEBUG` to
`backend/src/test/resources/application.properties` (a `logback-test.xml`
logger is overridden by the test profile's `logging.level.root=OFF`); each rise
in the logged `missCount` is one boot. Revert it afterwards.

## Controller and Spring setup

Prefer injecting the controller on `ControllerTestBase`. Every Spring test of the `test` profile extends `SpringTestBase` (`testability` package), directly or through `ControllerTestBase`, so the suite shares one application context. Do not add `@AutoConfigureMockMvc` or extra `@MockitoBean` / `@TestBean` on a subclass unless that **exact** annotation mix already exists — each unique mix caches another ApplicationContext and Hikari pool; CI MySQL then fails with `Too many connections` while a local run still passes. To observe a package-private persistence table, query through `EntityManager` rather than opening a new MockMvc context.

Calling a controller method directly skips request binding. When a multipart or form DTO field name matters (for example a boolean the frontend sends by name), prove it with a request through an `@Autowired MockMvc` (`multipart(...)` with the real field name); `SpringTestBase` already configures MockMvc, so this adds no context.

## Database Tests

- Tests use actual database interactions inside the transaction that `SpringTestBase` rolls back after each test.
- A `@Formula` field (for example `Note.trashedInDatabase`) is hydrated only when Hibernate loads the row from the database. An entity persisted or moved earlier in the same persistence context still carries the Java default, so a test that reads such a field through a loaded collection must `makeMe.refresh(entity)` first. Query-level predicates (`Note.JPA_AVAILABLE`) evaluate in SQL and need no refresh.
- A test that signs in through the security filters creates a `SPRING_SESSION` row in Spring Session's own transaction, outside that rollback. Such a test class is non-transactional and deletes its session rows after each test (see `DevelopmentAuthenticationConfigurationTest`). Session cookie attributes apply only on the embedded server, so a test asserting them uses `webEnvironment = RANDOM_PORT`.

Example (`TextContentValidatorTest`): no restated Spring annotations; `makeMe` comes from the base.

```java
class TextContentValidatorTest extends SpringTestBase {
  @Autowired Validator validator;

  @Test
  void defaultNoteIsValid() {
    assertThat(validator.validate(makeMe.aNote().inMemoryPlease()), is(empty()));
  }
}
```

## Test-data guidance and Java construction

Use [Dough MakeMe](../dough-makeme/SKILL.md) to select shared fixture guidance
for the test source.

Backend `MakeMe` is supplied by `SpringTestBase`. Java factory access calls a
method such as `makeMe.aNote()`. Entity builders complete with `please()` to
persist, or `inMemoryPlease()` / `please(false)` for in-memory construction;
`please(boolean)` passes the persistence choice through related construction.
`notebookOwnedBy(user)` sets notebook ownership, which `aMemoryTrackerFor(note)`
uses for its tracker owner. Preserve [destructive-fixture integrity](../unit-testing/SKILL.md#destructive-operations-hard-deletes-delete-migrations).

```java
Note note = makeMe.aNote()
                 .notebookOwnedBy(user)
                 .title("title")
                 .content("description")
                 .please();
```

## Test Organization

- Group related tests with `@Nested`.
- Use `@BeforeEach` for common setup.
- When a controller test needs an OpenAI substitute, use
  `OpenAiStructuredResponseMock` for the structured Responses output from
  `OpenAIClient`.

## Assertions

Use `assertThat` with descriptive matchers, `assertThrows` for exceptions, and
`@ParameterizedTest` to parameterize cases.
