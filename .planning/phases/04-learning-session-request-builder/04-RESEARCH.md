# Phase 04: Learning Session and Request builder - Research

**Researched:** 2026-08-08
**Domain:** Backend persistence (Learning Session / Session Item) + ADR 0005 Request markdown builder (Structure; no commission UI)
**Confidence:** HIGH

## Summary

Phase 4 is a **Structure** slice: introduce durable **Learning Session** and **Session Item** entities (Flyway migration), a domain service to **commission** a notebook’s due commissioned trackers into a session, and a **Learning Session Request** markdown builder per ADR 0005. No recall-page commission dialog, no frontend wiring, and no change to existing E2E-visible behavior — prior `learning_session` scenarios stay green without graduating the commission scenario.

The codebase has **no** `LearningSession`, `SessionItem`, or `Feedback` persistence today `[VERIFIED: grep backend/**/*.java — zero matches]`. Foundation is ready: `MemoryTrackerType.COMMISSIONED` `[VERIFIED: backend/src/main/java/com/odde/doughnut/entities/MemoryTrackerType.java:3-7]`, due-COMMISSIONED selection via `byUserIdCommissionedFrom` `[VERIFIED: backend/src/main/java/com/odde/doughnut/entities/repositories/MemoryTrackerRepository.java:80-85]`, and frontend potential-session grouping from `dueCommissioned` (Phase 3) unchanged.

**Primary recommendation:** Add Flyway `V300000240__learning_session_and_session_item.sql` (tip after `V300000239`), JPA entities `LearningSession` + `SessionItem`, `LearningSessionService.commission(notebook)` (abandon prior unfinished sessions for that notebook per milestone lifecycle), `LearningSessionRequestMarkdownBuilder` with ADR-verbatim rubric, and a logged-in `LearningSessionController` POST endpoint returning session id + request markdown — prove via controller-level JUnit only; regenerate OpenAPI; run `pnpm backend:verify` + existing `learning_session` Cypress spec; no new E2E scenarios this phase.

<user_constraints>
## User Constraints (from CONTEXT.md)

**No Phase 4 `*-CONTEXT.md` exists yet.** Constraints below are locked from ROADMAP Phase 4, milestone CONTEXT (Phase 1), and Phase 3 decisions — planner should treat these as binding until `/gsd-discuss-phase 4` adds a CONTEXT file.

### Locked Decisions

| Source | Decision |
|--------|----------|
| ROADMAP Phase 4 | **Structure** — persist Learning Session / Session Item and build Request markdown; **no commission UI** (Phase 5) |
| ROADMAP Phase 4 | **No user-visible behavior change** — prior E2E still green; do not graduate commission/record/amend scenarios |
| ROADMAP Phase 4 | Request markdown must include session items, expected learning content, learning status, and 0–5 rubric (ADR 0005) |
| Phase 3 D-02 | Potential learning session remains **frontend-derived** from `dueCommissioned`; **Learning Session entity created on commission** (this phase builds that path at API/domain level) |
| Phase 1 CONTEXT | Session lifecycle: potential session derived in FE; Learning Session exists only once commissioned; **old unfinished sessions and Session Items without Feedback are abandoned (deleted)** on new commission for the same notebook |
| Phase 1 CONTEXT | Protocol: ADR 0005 markdown; score→schedule ADR 0003 (apply in Phase 6, not here) |
| Phase 1 CONTEXT | Session identity codes not in protocol documents |
| ADR 0005 | Request lists notebook, inline rubric, one `### {note title}` section per Session Item with expected learning content + learning status |
| ADR 0005 | Learning status examples: `not yet tutored`; `1 previous session, last on 2026-08-06` |
| ADR 0005 | Matching/reporting deferred to Phase 6; builder only **renders** Request |

### Claude's Discretion

- Exact table/column names and enum literals (must map to glossary terms in code identifiers)
- Whether `Feedback` is columns on `session_item` vs separate table for MVP score-only (recommend columns on `session_item` until descriptive feedback in v2)
- Controller URL shape and response DTO naming (must regenerate OpenAPI)
- Ordering of Session Items in markdown (recommend same `next_recall_at` order as due query)
- Exact derivation of “expected learning content” from note body (recommend `NoteContentMarkdown.bodyWithoutLeadingFrontmatter` trimmed — matches draft E2E fixture `Hello`)
- Service package placement (`services/learning/` vs flat `services/` — follow smallest cohesive extension)

### Deferred Ideas (OUT OF SCOPE)

- Commission dialog from recall progress bar — Phase 5 (COM-01)
- Copyable Request UI — Phase 5 (COM-02 user-facing)
- Awaiting-report UX — Phase 5 (COM-03)
- Record report, parse Report, schedule trackers — Phase 6 (REC-*)
- Amend — Phase 7 (AMD-01)
- Excluding commissioned-awaiting-report trackers from `dueCommissioned` / potential sessions — not Phase 4; decide in Phase 5 if needed
- Descriptive feedback, smart request, MCP transport — v2 (PROT-*)
</user_constraints>

<phase_requirements>
## Phase Requirements

Phase 4 is **Structure** — no COM-* behavior IDs close yet; this phase **unlocks** them for Phase 5–6.

| ID | Description | Research Support |
|----|-------------|------------------|
| COM-01 (unlock) | User can commission a Learning Session from recall progress bar dialog | Phase 4: `LearningSessionService.commission` + POST API; Phase 5 wires UI to same endpoint |
| COM-02 (unlock) | Commissioning produces Request markdown per ADR 0005 | `LearningSessionRequestMarkdownBuilder`; controller returns markdown string; unit assertions on Hola/Gracias fixture |
| COM-03 (unlock) | After commissioning, session awaits Tutor's report | `LearningSession` status `AWAITING_REPORT` (or `recorded_at` null); persisted session retrievable by id |
| *(Structure SC)* | Existing product behavior unchanged | No FE changes; no new E2E; `dueCommissioned` / ordinary recall paths untouched |
| *(Structure SC)* | Learning Session creatable with Session Items for due commissioned trackers of one notebook | Commission service filters `getCommissionedMemoryTrackersNeedToRepeat` by `notebookId` |
| *(Structure SC)* | Request includes items, content, status, rubric | Builder tests assert ADR sections verbatim |
</phase_requirements>

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| `learning_session` / `session_item` schema | Database / Storage | API / Backend | New Flyway migration; FK to `user`, `notebook`, `memory_tracker` |
| Commission + abandon lifecycle | API / Backend | Database / Storage | Business rules; transactional delete of prior unfinished sessions per notebook |
| Due commissioned selection for one notebook | API / Backend | Database / Storage | Reuse `UserService.getCommissionedMemoryTrackersNeedToRepeat` + notebook filter |
| Learning status aggregation | API / Backend | Database / Storage | Query prior **recorded** session items per tracker |
| Request markdown rendering | API / Backend | — | Pure builder from session + note data; ADR 0005 is source of truth |
| REST commission endpoint | API / Backend | — | Structure-phase API proof; auth via `AuthorizationService` |
| Potential learning sessions display | Browser / Client | — | **Unchanged** — Phase 3 `useRecallData` grouping |
| Report parse / schedule | API / Backend | — | **Phase 6** — out of scope |

## Standard Stack

### Core
| Library / layer | Version | Purpose | Why Standard |
|-----------------|---------|---------|--------------|
| Spring Boot + JPA | existing backend | Entities, repositories, `@Transactional` services | Same as `QuestionGenerationBatch` parent/child pattern `[VERIFIED: QuestionGenerationBatchRequest.java:17-25]` |
| Flyway SQL migration | tip `V300000239` → **`V300000240`** | `learning_session`, `session_item` tables | `db-migration.mdc` — version > `300000230` |
| OpenAPI → `pnpm generateTypeScript` | repo skill | New controller DTOs | `agent-map.md` — never hand-edit generated client |
| JUnit + MakeMe | existing | Controller-boundary proofs | `backend-testing.mdc` / `unit-testing.mdc` |
| `NoteContentMarkdown` | existing | Expected learning content from note body | `[VERIFIED: NoteContentMarkdown.bodyWithoutLeadingFrontmatter` used in tests]` |

### Supporting
| Library / asset | When to Use |
|-----------------|-------------|
| `AuthorizationService.assertAuthorization(notebook)` | Before commission — notebook access |
| `RecallStatsAggregator` ISO date pattern | Learning status date formatting (`DateTimeFormatter.ISO_LOCAL_DATE`) `[VERIFIED: RecallStatsAggregator.java:26]` |
| `database-erd` skill | After migration — refresh `docs/database-erd.md` |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| New tables | JSON blob on notebook | Breaks Session Item / Feedback model; conflicts with ADR glossary |
| Separate `feedback` table now | `feedback_score` on `session_item` | Extra join for MVP score-only; defer separate entity until PROT-01 |
| Frontend request preview | Backend-only builder | Violates Structure “no UI change” |
| Hand-built markdown in controller | Dedicated builder class | Untestable; rubric drift from ADR |

**Installation:** None — no new npm/Maven packages.

**Version verification:** N/A (in-repo stack only).

## Package Legitimacy Audit

> No external packages installed this phase.

| Package | Verdict | Disposition |
|---------|---------|-------------|
| — | N/A | N/A |

**Packages removed due to [SLOP] verdict:** none  
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```text
POST /api/learning-sessions/commission  (notebookId)     [Phase 4 — tests only; no FE wire]
        │
        ▼
LearningSessionController
        │ assertLoggedIn + assertAuthorization(Notebook)
        ▼
LearningSessionService.commission(user, notebook, now, zoneId)
        ├── abandonUnfinishedSessions(user, notebook)     → DELETE prior awaiting-report sessions + items
        ├── dueTrackers = getCommissionedMemoryTrackersNeedToRepeat(user, …)
        │       .filter(mt -> mt.getNote().getNotebook().equals(notebook))
        ├── persist LearningSession (AWAITING_REPORT, commissioned_at)
        ├── persist SessionItem per tracker (memory_tracker_id, note_title snapshot)
        └── LearningSessionRequestMarkdownBuilder.build(session) → markdown String
        │
        ▼
Response DTO { learningSessionId, requestMarkdown, status: AWAITING_REPORT }

(unchanged) GET /api/recalls/recalling → dueCommissioned → FE potentialLearningSessions
```

### Recommended Project Structure

```text
backend/src/main/resources/db/migration/
  V300000240__learning_session_and_session_item.sql

backend/src/main/java/com/odde/doughnut/entities/
  LearningSession.java
  LearningSessionStatus.java          # AWAITING_REPORT, RECORDED
  SessionItem.java

backend/src/main/java/com/odde/doughnut/entities/repositories/
  LearningSessionRepository.java
  SessionItemRepository.java

backend/src/main/java/com/odde/doughnut/services/
  LearningSessionService.java
  LearningSessionRequestMarkdownBuilder.java

backend/src/main/java/com/odde/doughnut/controllers/
  LearningSessionController.java

backend/src/main/java/com/odde/doughnut/controllers/dto/
  CommissionLearningSessionRequest.java   # notebookId
  LearningSessionCommissionResponse.java  # id, requestMarkdown, status

backend/src/test/java/com/odde/doughnut/controllers/
  LearningSessionControllerTests.java

backend/src/test/java/com/odde/doughnut/testability/builders/
  LearningSessionBuilder.java             # extend MakeMe
```

### Pattern 1: Parent/child persistence (mirror QuestionGenerationBatch)

**What:** `LearningSession` 1—N `SessionItem`, cascade delete on session abandon.  
**When to use:** Any commissioned unit spanning multiple memory trackers.

**Schema sketch** (planner finalizes names):

```sql
-- learning_session
--   id, user_id, notebook_id, status, commissioned_at, recorded_at (nullable)
-- session_item
--   id, learning_session_id, memory_tracker_id, note_title,
--   feedback_score (nullable 0-5), feedback_recorded_at (nullable)
-- FKs: ON DELETE CASCADE for session → items; restrict or cascade tracker per product rule
```

Follow baseline conventions: `int unsigned AUTO_INCREMENT`, `timestamp(3)`, `utf8mb4_unicode_ci`, indexed `(user_id, notebook_id, status)` for abandon queries `[VERIFIED: V100000000__baseline.sql:615-629 question_generation_batch pattern]`.

### Pattern 2: Commission from due commissioned trackers

**What:** Reuse existing due selection; add notebook scope.

```java
// Reuse UserService.getCommissionedMemoryTrackersNeedToRepeat — same cutoff as recalls
// [VERIFIED: UserService.java:72-77]
stream
  .filter(mt -> mt.getNote().getNotebook().getId().equals(notebook.getId()))
  .filter(MemoryTracker::isNoteLevelTracker)  // MVP UI only creates note-level COMMISSIONED
```

Empty due set → fail visibly (4xx), do not create empty session.

### Pattern 3: Learning Session Request markdown (ADR 0005 verbatim)

**What:** Builder emits exact structure from ADR — planner tests against fixture notebook “Spanish conversation” / notes Hola, Gracias.

```markdown
# Learning Session Request

Notebook: Spanish conversation

## How to report

Teach the session items below, then return a Learning Session Report giving one
score from 0 to 5 per item:

- 5 — mastered the learning point with full fluency
- 4 — mastered the learning point with fluency
- 3 — mastered the learning point, but not fluent
- 2 — needed a reminder at first, then showed signs of mastering it
- 1 — needed several reminders
- 0 — could not reach the learning point even with help

## Session Items

### Hola
- Expected learning content: Hello
- Learning status: not yet tutored

### Gracias
- Expected learning content: Thank you
- Learning status: not yet tutored
```

`[CITED: docs/adrs/0005-commissioned-learning-session-protocol.md:59-85]`

**Learning status rules:**
- No prior recorded Feedback for tracker → `not yet tutored` `[CITED: ADR 0005:84]`
- N prior recorded sessions with Feedback → `{N} previous session(s), last on {ISO date}` `[CITED: ADR 0005:80]` — use learner timezone for date; pluralize “session” when N > 1 `[ASSUMED]` (ADR shows singular only)

**Expected learning content:** `NoteContentMarkdown.bodyWithoutLeadingFrontmatter(note.getContent())` trimmed; empty → empty string or visible placeholder — match E2E `Hello` `[VERIFIED: commissioned_learning_session.feature:10-13,34]`.

### Pattern 4: Abandon unfinished sessions on commission

**What:** Before creating a new session, delete (hard delete per CONTEXT) prior `LearningSession` for same `user_id` + `notebook_id` where `recorded_at IS NULL` / status `AWAITING_REPORT`, including child `session_item` rows.

**When:** Every successful commission — locked Phase 1 lifecycle.

### Anti-Patterns to Avoid

- **Wiring commission button in Phase 4:** Violates Structure boundary; breaks “no user-visible change”.
- **Graduating E2E “Commissioning a learning session…”:** Phase 5; scenario already drafted `[VERIFIED: commissioned_learning_session.feature:28-36]`.
- **Applying ADR 0003 score→schedule on commission:** Recording is Phase 6; commission must not change `next_recall_at`.
- **Matching Session Items by tracker id in markdown:** ADR uses **note title** as protocol identifier `[CITED: ADR 0005:103]`.
- **Hand-editing `packages/generated/**`:** Regenerate after controller/DTO add.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Markdown templating engine | Custom DSL / template lib | `StringBuilder` or small private methods per ADR section | ADR is fixed; no external dep; easy verbatim tests |
| Due tracker query | Duplicate SQL | `getCommissionedMemoryTrackersNeedToRepeat` + notebook filter | Single cutoff semantics with recall page |
| Notebook access check | Ad-hoc ownership | `AuthorizationService.assertAuthorization(notebook)` | Established pattern |
| Date in learning status | Locale-specific ad-hoc format | `DateTimeFormatter.ISO_LOCAL_DATE` in user zone | Matches ADR example `2026-08-06` |
| ORM-free JDBC | Raw SQL inserts | JPA entities + repositories | Repo standard |

**Key insight:** Phase 4’s complexity is **lifecycle + faithful ADR markdown**, not algorithms — keep builder dumb and testable.

## Common Pitfalls

### Pitfall 1: Regressing Phase 3 recall payloads
**What goes wrong:** Commission path mutates `dueCommissioned` or ordinary `toRepeat`.  
**Why:** Shared service misuse or accidental filter change.  
**How to avoid:** Do not touch `RecallService.getDueMemoryTrackers`; run existing `RecallsControllerTests` + `learning_session` Cypress spec.  
**Warning signs:** `dueCommissioned` count changes in tests without recording.

### Pitfall 2: Rubric drift from ADR 0005
**What goes wrong:** Paraphrased score descriptions break copy-paste Tutor contract.  
**How to avoid:** Copy rubric lines verbatim; assert full markdown snapshot in one canonical test.  
**Warning signs:** E2E Phase 5 step “instruct the tutor to report one score per session item” fails substring match.

### Pitfall 3: Empty commission
**What goes wrong:** Session with zero Session Items when no due commissioned trackers for notebook.  
**How to avoid:** Reject commission with clear 4xx when filtered stream empty.  
**Warning signs:** Orphan `learning_session` rows in DB.

### Pitfall 4: Forgetting abandon lifecycle
**What goes wrong:** Multiple awaiting-report sessions per notebook; Phase 5/6 ambiguity.  
**How to avoid:** Implement delete in `commission` before insert; test with two commission calls.  
**Warning signs:** More than one `AWAITING_REPORT` row per user+notebook.

### Pitfall 5: Property commissioned trackers in session
**What goes wrong:** Session items for property-key trackers MVP UI never creates.  
**How to avoid:** Filter `isNoteLevelTracker()` on commission; TRK-04 deferred.  
**Warning signs:** Session items with non-empty `property_key`.

## Code Examples

### Controller commission (sketch)

```java
@PostMapping("/commission")
@Transactional
public LearningSessionCommissionResponse commission(
    @RequestBody CommissionLearningSessionRequest body,
    @RequestParam String timezone) {
  authorizationService.assertLoggedIn();
  Notebook notebook = notebookRepository.findById(body.getNotebookId())...;
  authorizationService.assertAuthorization(notebook);
  ZoneId zoneId = TimezoneUtils.parseTimezone(timezone);
  Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
  return learningSessionService.commission(
      authorizationService.getCurrentUser(), notebook, now, zoneId);
}
```

### Learning status query (sketch)

```java
long priorCount = sessionItemRepository.countRecordedByMemoryTrackerId(trackerId);
if (priorCount == 0) return "not yet tutored";
LocalDate lastOn = sessionItemRepository.findLastRecordedDate(trackerId, zoneId);
return priorCount + " previous session" + (priorCount == 1 ? "" : "s")
    + ", last on " + ISO_DATE.format(lastOn);
```

### MakeMe extension (sketch)

```java
// LearningSessionBuilder — commissioned session with items from existing trackers
makeMe.aLearningSession()
  .forNotebook(notebook)
  .by(user)
  .withSessionItems(tracker1, tracker2)
  .please();
```

Draft E2E assertions Phase 5 will need `[VERIFIED: commissioned_learning_session.feature:32-35]`:

```gherkin
Then the learning session request should list session items for notes "Hola, Gracias"
And the learning session request should include the learning status of "Hola"
And the learning session request should include the expected learning content "Hello"
And the learning session request should instruct the tutor to report one score per session item
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Potential session only (FE derived) | + persisted Learning Session on commission | Phase 4 | Enables Request + later recording |
| Protocol in ADR only | Executable markdown builder | Phase 4 | COM-02 backend ready |
| `COMMISSIONED` type + due feed | + session linkage on commission | Phase 4 | Learning status can query history |

**Deprecated/outdated:**
- Persisting “Potential Learning Session” rows — explicitly rejected Phase 3 D-02

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Plural learning status uses `sessions` when count > 1 | Pattern 3 | Minor copy mismatch vs ADR singular example |
| A2 | Expected learning content = frontmatter-stripped body, not cloze-masked | Pattern 3 | Complex notes may differ from tutor-facing “learning point” |
| A3 | Hard delete for abandoned sessions (not soft-delete) | Pattern 4 | If product wants audit trail, schema needs revision |
| A4 | `dueCommissioned` unchanged after commission until Phase 6 recording | Pitfall 1 | Potential sessions may still show commissioned-awaiting-report notebooks until Phase 5 policy |
| A5 | `feedback_score` / `feedback_recorded_at` on `session_item` suffices until PROT-01 | Standard Stack | Migration churn if separate Feedback table required earlier |

## Open Questions

1. **Exclude awaiting-report trackers from `dueCommissioned`?**
   - What we know: Phase 3 shows all due commissioned; commission does not reschedule.
   - What's unclear: Whether learner should still see potential session after commissioning.
   - Recommendation: **Defer to Phase 5 discuss**; Phase 4 does not change recall DTO.

2. **Session item ordering in markdown**
   - What we know: ADR unordered; due query orders by `next_recall_at`.
   - Recommendation: Match due order for stable tests.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Nix shell | `pnpm backend:verify` | ✓ | local | `cloud-vm-setup` skill |
| MySQL + Flyway | Migration + tests | assume `pnpm sut` | — | `pnpm sut:healthcheck` |
| New external packages | — | N/A | — | — |

**Missing dependencies with no fallback:** none  
**Step 2.6:** Existing doughnut stack only — no new CLIs.

## Validation Architecture

> `workflow.nyquist_validation` is true `[VERIFIED: .planning/config.json:24]`.

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit (backend); Cypress 15.20.0 for regression only |
| Config file | backend Gradle; `e2e_test/features/learning_session/commissioned_learning_session.feature` |
| Quick run command | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` |
| Full suite command | `CURSOR_DEV=true nix develop -c pnpm backend:verify`; regression E2E: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/learning_session/commissioned_learning_session.feature` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| Structure SC1 | No user-visible regression | e2e (existing) | `cypress run --spec …/commissioned_learning_session.feature` | ✅ |
| Structure SC2 | Commission creates session + items for notebook due COMMISSIONED | unit (controller) | `pnpm backend:test_only` | ❌ Wave 0 |
| Structure SC3 | Request markdown: items, content, status, rubric | unit (controller or builder) | `pnpm backend:test_only` | ❌ Wave 0 |
| COM-02 (prep) | Markdown matches ADR + feature fixture | unit snapshot | `pnpm backend:test_only` | ❌ Wave 0 |
| COM-03 (prep) | Session status awaiting report | unit | `pnpm backend:test_only` | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** `pnpm backend:test_only` (full backend suite per `backend-testing.mdc`)
- **Per wave merge:** `pnpm backend:verify` + targeted `learning_session` Cypress spec
- **Phase gate:** No new E2E scenarios; existing Phase 2–3 scenarios green

### Wave 0 Gaps
- [ ] `V300000240__learning_session_and_session_item.sql`
- [ ] `LearningSession` / `SessionItem` entities + repositories
- [ ] `LearningSessionService` + `LearningSessionRequestMarkdownBuilder`
- [ ] `LearningSessionController` + DTOs + OpenAPI regen
- [ ] `LearningSessionControllerTests` — commission Spanish notebook fixture; assert markdown sections
- [ ] `makeMe.aLearningSession()` / builder for Phase 6 prep
- [ ] Optional: `LearningSessionRequestMarkdownBuilderTest` if builder extracted

## Security Domain

> `security_enforcement` enabled `[VERIFIED: .planning/config.json:47]`.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes | `assertLoggedIn()` on commission endpoint |
| V3 Session Management | yes (unchanged) | Existing Spring session |
| V4 Access Control | yes | `assertAuthorization(notebook)`; queries scoped by `user_id` |
| V5 Input Validation | yes | Validate `notebookId`; reject empty commission; score validation deferred to Phase 6 |
| V6 Cryptography | no | — |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Commission another user's notebook | Elevation | Notebook authorization before write |
| IDOR on learning session id | Information disclosure | Load session only when `session.user == currentUser` (Phase 5 GET if added) |
| Mass assignment via DTO | Tampering | Request body only `notebookId`; server sets status/timestamps |

## Project Constraints (from .cursor/rules/)

- **Nix prefix:** `CURSOR_DEV=true nix develop -c …` for tooling; git direct
- **Structure phase:** No user-visible change; targeted E2E regression only — `planning.mdc`
- **Behavior vs Structure:** One structure change enabling commission behavior in Phase 5
- **Small tests:** Controller boundary + MakeMe; real DB `@Transactional` — `unit-testing.mdc`, `backend-testing.mdc`
- **Error handling:** Fail visibly on empty due set; no silent swallow — `error-handling.mdc`
- **Migrations:** New file only; version > `300000230`; regen ERD — `db-migration.mdc`
- **API client:** Regenerate after OpenAPI change — `generate-api-client` skill / `agent-map.md`
- **Capability naming:** Product code uses `LearningSession`, not phase numbers — `general.mdc`
- **ADRs:** ADR 0005 Proposed guides protocol; 0001 glossary terms for identifiers — `architecture-decisions.mdc`
- **No commit unless asked:** Research file written; commit optional via `gsd_run query commit` if orchestrator requests

## Sources

### Primary (HIGH confidence)
- `docs/adrs/0005-commissioned-learning-session-protocol.md` — Request format, rubric, learning status, matching rules
- `docs/adrs/0001-ubiquitous-language.md` §3 — Learning Session, Session Item, Feedback glossary
- `backend/.../MemoryTrackerRepository.java` — `byUserIdCommissionedFrom` due query
- `backend/.../RecallService.java` — due commissioned DTO mapping (unchanged this phase)
- `.planning/phases/01-commissioned-tracker-model/CONTEXT.md` — session lifecycle / abandon
- `.planning/phases/01-.../commissioned_learning_session.feature` — fixture data + future E2E assertions

### Secondary (MEDIUM confidence)
- `.planning/phases/03-potential-learning-sessions/03-RESEARCH.md` — Structure-phase patterns, test commands
- `QuestionGenerationBatch` / `QuestionGenerationBatchRequest` — parent/child FK pattern

### Tertiary (LOW confidence)
- Pluralization of “previous session(s)” — ADR shows singular only `[ASSUMED]`

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — in-repo patterns only
- Architecture: HIGH — ADR + existing due-COMMISSIONED seam verified
- Pitfalls: MEDIUM — awaiting-report vs potential-session UX deferred Phase 5

**Research date:** 2026-08-08  
**Valid until:** 2026-09-08 (stable domain)

## RESEARCH COMPLETE
