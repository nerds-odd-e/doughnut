# Stay signed in across deploys and browser restarts

## Source

- Story: [Return to Donut without repeating GitHub login while browser authorization remains valid](../../seeds/SEED-040-remember-browser-login.md#story-1)
- **Identity:** SEED-040#story-1

## Goal and scope

A signed-in user stays signed in across deploys, idle periods, and browser
restarts until logout or 30 days without use. GitHub OAuth stays the only way to
sign in; CLI and MCP bearer tokens are unchanged.

Excluded (see the seed): new credentials or user-token reuse, session
management UI, GitHub revocation handling, the save-while-signed-out confirm,
user-token hardening, and enabling CSRF protection.

## Approach

Replace in-memory HTTP sessions with Spring Session JDBC in the existing MySQL
database, for every profile, so production, development, backend tests, and E2E
all use one session store. Spring Boot's own properties configure it:

- `spring-boot-starter-session-jdbc` (Boot 4.1.1 publishes it; it brings
  `spring-session-jdbc` 4.1.1) replaces the unused `spring-session-core`
  dependency.
- A Flyway migration creates Spring Session's MySQL tables; schema
  initialization by Spring Session stays off.
- Server-side session timeout: 30 days. Spring Session updates the last access
  time on each request, which gives "renewed by use".
- Cookie: `Max-Age` 400 days (the longest lifetime browsers keep), so the
  30-day server timeout decides expiry. Spring Session writes the cookie only
  when a session is created or its ID changes, so a cookie `Max-Age` equal to
  30 days would end sessions 30 days after sign-in even with daily use.
- Cookie: `SameSite=Lax` and `Secure` in all profiles. Browsers treat
  `http://localhost` as a secure context, so local development and E2E keep
  working.

Stored session contents must be serializable. Today 17 controllers and the
non-production OpenAI client are `@SessionScope`, so each one is stored in every
user's session. None of these controllers holds per-user state: their fields are
injected dependencies. Slice 1 therefore makes the controllers singletons and the
non-production OpenAI client request-scoped (it only needs the current
testability URL).

## Decisive premises

| Premise | Observation | Result |
| --- | --- | --- |
| Production sessions are in-memory, 30 minutes idle, with a browser-session cookie | Code/config read: `backend/build.gradle:83` (only `spring-session-core`), no `SessionRepository` or `server.servlet.session.*` anywhere, `configs/ProductionConfiguration.java:28` plain `oauth2Login` | Confirmed |
| Every deploy replaces all production instances | `infra/gcp/scripts/update-mig-startup-script.sh:75-78` rolling replace of `doughnut-app-group` (size 2); 15 release tags from 2026-09-21 to 2026-09-28 | Confirmed |
| Boot 4.1.1 offers a JDBC session starter | `curl https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-session-jdbc/maven-metadata.xml` | 4.1.1 published; `spring-boot-session-jdbc` 4.1.1 depends on `spring-session-jdbc` 4.1.1 |
| Session-scoped beans would be stored in the session | `grep -rl @SessionScope backend/src/main` | 17 controllers plus `configs/OpenAiApiConfig.java:29` (non-prod client) |
| Those controllers keep no per-user state | Scan of non-final fields in the 17 controllers | Only injected dependencies (for example `AiAudioController.otherAiServices`, assigned in the constructor) |
| `TestabilitySettings` is not session-scoped | `testability/TestabilitySettings.java:17-18` | `@ApplicationScope` |
| Slice 1's E2E proof exercises the swapped OpenAI URL | `head e2e_test/features/messages/conversation_about_a_note.feature`; `e2e_test/start/testability.ts:436` | Tagged `@usingMockedOpenAiService`; the URL is set through testability before the scenario |
| A MockMvc sign-in test exists to extend | `configs/DevelopmentAuthenticationConfigurationTest.java` (dev profile, Basic sign-in via `/login/continue`) | Present |
| Flyway owns schema; next version is above `300000348` | `.agents/skills/db-migration/SKILL.md`, `db/migration/` listing | Confirmed; choose the next free version at execution time |
| E2E sign-in keeps working with a `Secure` cookie over `http://localhost` | Bounded by slice 4's E2E proof | Unobserved; if it fails, set `Secure` only in `application-prod.yml` and prove it there |

The production journey (staying signed in across a real release and a GitHub
OAuth callback under `SameSite=Lax`) belongs to the owner and is checked after
the next release, not as slice proof.

## Slices

### 1. Controllers and the non-production OpenAI client no longer live in the HTTP session
Type: Structure
Status: done
Proof: full backend suite
(`./backend/gradlew -p backend test -Dspring.profiles.active=test --build-cache --parallel`)
plus a focused E2E run of an AI feature that uses the replaced OpenAI URL
(`e2e_test/features/messages/conversation_about_a_note.feature`).

Internal change: remove `@SessionScope` from the 17 controllers; change the
non-production `officialOpenAiClient` bean to `@RequestScope`. External
behavior is unchanged. Enables slice 2, whose store requires serializable
session contents.

Accepted proof: `SUT_TIMEOUT_MS=360000 ./scripts/run.sh pnpm cy:run --spec
e2e_test/features/messages/conversation_about_a_note.feature` → 1 passing;
`./scripts/run.sh pnpm backend:test:worktree` → 2712 tests, 0 failures.

Learnings:
- The request-scoped client is `@Bean(destroyMethod = "")`: otherwise Spring
  closes the SDK client at request end while streaming AI calls are still in
  flight (`RejectedExecutionException` from OkHttp, no AI reply in E2E). The
  SDK closes an unreachable client itself.
- No `@SessionScope` remains in `backend/src`; slice 2 still needs to confirm
  the remaining session attributes (security context) serialize.

### 2. A signed-in session is kept in the database, shared by all instances
Type: Behavior
Status: planned
Proof: extend `DevelopmentAuthenticationConfigurationTest` (dev profile,
MockMvc against the real test database) or a sibling test beside it; run that
class, then the E2E sign-in and note-editing features
(`e2e_test/features/users/*`, `e2e_test/features/note_creation_and_update/mcq_management.feature`) through
`pnpm cy:run --spec`.

Behavior:
- Basic sign-in → the response sets the session cookie; a later request carrying
  only that cookie is signed in (`/api/user/current-user-info` returns the user),
  and the session is stored in the database.
- `POST /logout` with the cookie → a later request with that cookie is signed
  out.
- An unknown session cookie → signed out.

Implementation: starter dependency, Flyway migration with Spring Session's
MySQL schema (its attribute table's `ON DELETE CASCADE` to its session table is
Spring Session's own delete path, not notebook content), and
`spring.session.jdbc.initialize-schema: never`. Tests that sign in must delete
their session rows (Spring Session commits in its own transaction, outside
`SpringTestBase`'s rollback).

Stopping here is safe: sessions survive deploys; idle timeout and cookie
lifetime are unchanged.

### 3. A session ends only after 30 days without use
Type: Behavior
Status: planned
Proof: extend the slice 2 test; run that class.

Behavior:
- After sign-in, the stored session's maximum inactivity is 30 days.
- The stored session's last access is set to 29 days ago → a request with the
  cookie is still signed in and the last access moves to now.
- The stored session's last access is set to 31 days ago → a request with the
  cookie is signed out.

Implementation: session timeout of 30 days.

Stopping here is safe: the cookie still ends when the browser closes.

### 4. The session cookie survives a browser restart and is Secure and SameSite=Lax
Type: Behavior
Status: planned
Proof: extend the slice 2 test with the `Set-Cookie` header after sign-in; run
that class and the E2E sign-in feature through `pnpm cy:run --spec`.

Behavior: sign-in → the session cookie carries `Max-Age` of 400 days, `Secure`,
`HttpOnly`, and `SameSite=Lax`; E2E sign-in and a signed-in page still work.

## Current decisions

- One session store (MySQL through Spring Session JDBC) for every profile; no
  profile-specific session code.
- The server-side 30-day inactivity timeout decides expiry; the cookie lifetime
  is kept at the browser maximum so it never ends a session early.
- `Secure` applies in all profiles unless slice 4's E2E proof disproves the
  localhost premise.
- The first release with this change signs every user out once; later releases
  do not.

## Evaluation after release

After the release containing slice 4, the owner stays signed in across the next
production deploy and a browser restart, and logout still signs out.
