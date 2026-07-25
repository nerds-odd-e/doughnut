# Phase 2: Empty-folder findings - Research

**Researched:** 2026-07-22
**Domain:** Spring Boot notebook Health lint — empty-folder rule + authorized report API
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** A folder is reported under `empty_folders` only when it is **fully empty**: entire subtree has **no non-deleted notes** (soft-deleted notes do not count as content) **and** the folder’s own `readmeContent` is blank (null, empty, or whitespace-only). Nested empty shells count (recursive).
- **D-02:** Note-empty folders with **non-blank** `readmeContent` are **not** listed under `empty_folders` in this phase — leave them unreported until Phase 3 adds `readme_only_folders`. Do not lump them into `empty_folders` as an interim.
- **D-03:** Soft-deleted notes never make a folder “non-empty.” Only live (`deletedAt IS NULL`) notes occupy a folder for this predicate.
- **D-04:** List **every** matching empty folder as its own `HealthFindingItem` (not only roots of maximal empty subtrees). Each item must set `folderId` and a short `label` (folder name). Optional `message` only if cheap; path-in-label is not required for v1.
- **D-05:** Use stable rule id `empty_folders` (`HealthRuleIds.EMPTY_FOLDERS`). Display title: human-readable e.g. “Empty folders”.
- **D-06:** Group severity = `warning`. Group `autoFixable` = `true` (reserved for Phase 7 bulk purge; lint remains report-only and must not delete).
- **D-07:** Implement as a Spring `@Component` / `@Service` `HealthRule` bean (e.g. `EmptyFolderHealthRule`) discovered by existing `HealthRuleRunner` `List<HealthRule>` injection. Runner must still never mutate.
- **D-08:** Expose `POST /api/notebooks/{notebook}/health/lint` that authorizes with **`assertAuthorization(notebook)`** (owner/write — not read-only/bazaar/subscriber), calls `NotebookHealthService.lint`, returns `NotebookHealthLintReport`. Foreign and anonymous callers are rejected.
- **D-09:** Phase 2 lint request body is empty/minimal — no run-option fields required yet (`HealthRunContext` may stay empty). Do not invent fix options or user-defaults fields here.
- **D-10:** Prefer methods on existing `NotebookController` under `/health/lint` **or** a thin dedicated controller colocated with notebook routes — whichever matches existing controller cohesion; capability-named types only (no phase numbers in product code).
- **D-11:** After the endpoint exists, regenerate the TypeScript OpenAPI client (`pnpm generateTypeScript` / generate-api-client skill). No frontend Health UI in this phase.
- **D-12:** Prove with focused backend tests: (1) unit/service tests for the empty-folder predicate (recursive emptiness, soft-delete ignored, blank vs non-blank readme excluded from this group); (2) MVC/API tests that authorized lint returns the empty-folder group/items and that unauthorized callers fail. No `@wip` E2E and no Health tab UI in this phase.

### Claude's Discretion
- Exact query strategy (load folders + note folder_ids once vs repository helpers) — follow research O(folders + notes) guidance; avoid N+1.
- Whether label is bare folder name vs light path hint — prefer folder name unless path is trivial from existing APIs.
- Exact controller class split (`NotebookController` vs `NotebookHealthController`) — pick the simplest cohesive option.

### Deferred Ideas (OUT OF SCOPE)
- Readme-only finding type / `readme_only_folders` rule — Phase 3
- Dead wiki-link rule — Phase 4
- Health tab UI / Run button — Phase 5
- User-level lint defaults — Phase 6
- Bulk empty-folder purge / fix endpoint — Phase 7 (must reuse this phase’s fully-empty predicate; never dissolve)
- Listing only maximal empty roots (instead of every empty folder) — rejected for v1; revisit only if UI noise becomes a problem after Phase 5
- Read-authorization for lint (bazaar/subscriber audit) — out of v1; Health is owner write tool
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| EFOL-01 | Lint reports folders whose **entire subtree has no notes** (recursive emptiness; soft-deleted notes do not count as content) | `EmptyFolderHealthRule` with memoized subtree occupancy over live `note.folder_id` only; blank own `readmeContent` required (D-01/D-03) |
| EFOL-02 | When auto-fix is not selected, user can see the list of empty folders under the empty-folder findings group | Report-only `POST .../health/lint` with empty body / empty `HealthRunContext`; no fix options; group `items` always populated from evaluation (D-08/D-09) |
</phase_requirements>

## Summary

Phase 1 already delivered the Health contract (`HealthRule`, `HealthRuleRunner`, `NotebookHealthService.lint`, findings DTOs, `HealthRuleIds.EMPTY_FOLDERS`). Phase 2 is a single **Behavior** slice: register one rule bean that detects fully empty folders, expose an authorized lint endpoint, regenerate the OpenAPI TS client, and prove it with backend unit + controller tests. No UI, no mutation, no readme-only rule.

The live codebase has everything needed to plug in without inventing a second model: `FolderRepository.findByNotebookIdOrderByIdAsc` loads the folder tree in one query; notes attach via `Note.folder` / `folder_id` and soft-delete via `deletedAt`; builders `makeMe.aFolder()` / `.folder(Folder)` / `.softDeleted()` / `.readmeContent(...)` support fixtures; notebook controllers authorize with `authorizationService.assertAuthorization(notebook)` and reject foreign/anon with `UnexpectedNoAccessRightException`.

**Primary recommendation:** Add `@Service EmptyFolderHealthRule` (O(folders+live notes), label = folder name), thin package-private `NotebookHealthController` under `/api/notebooks` with `POST /{notebook}/health/lint`, then `pnpm generateTypeScript` and backend tests only.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Empty-folder predicate (recursive, soft-delete aware, blank readme) | API / Backend | Database / Storage | Full folder tree + live note placement live in MySQL; client never loads whole graph on Settings |
| Findings report assembly (`HealthFindingGroup` / items) | API / Backend | — | Ephemeral DTOs from `HealthRuleRunner`; no findings table |
| Authorization (owner write only) | API / Backend | — | `AuthorizationService.assertAuthorization(notebook)` — same gate as folder mutate endpoints |
| OpenAPI / generated TS client | API / Backend → packages/generated | — | Wire shape for Phase 5 UI; no UI consumers this phase |
| Health tab / Run UX | Browser / Client | — | **Out of phase** (Phase 5) |

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot (existing) | repo-managed | `@Service` rule bean + `@RestController` | Phase 1 runner already injects `List<HealthRule>` [VERIFIED: codebase `HealthRuleRunner.java`] |
| Spring Framework DI | repo-managed | Inject all `HealthRule` beans into `List` | Official pattern: typed collection autowiring [CITED: docs.spring.io/spring-framework/reference/6.2/core/beans/annotation-config/autowired.html] |
| Spring Data JPA | repo-managed | Folder/note queries | Existing `FolderRepository` / `NoteRepository` |
| OpenAPI / springdoc (existing) | repo-managed | Expose lint operation on controllers | Regen via `pnpm generateTypeScript` |
| JUnit 5 + SpringBootTest | repo-managed | Controller + service tests | Existing `ControllerTestBase` pattern |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Lombok (DTO getters/setters) | existing | Findings DTOs already use `@Getter`/`@Setter` | Do not change DTO style |
| Hamcrest | existing | Assertions in controller tests | Match nearby notebook tests |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Thin `NotebookHealthController` | Methods on `NotebookController` | `NotebookController` is already **450 lines** (over 250-line post-change-refactor limit). Dedicated controller matches `NotebookBooksController` cohesion [VERIFIED: codebase] |
| Per-folder note-count queries | One notebook-scoped live `folder_id` set | N+1 is the main perf pitfall; batch is O(folders+notes) |
| Path-in-label | Bare `folder.getName()` | Path needs extra join/build; D-04 allows name-only for v1 |

**Installation:** None — no new packages.

**Version verification:** N/A (no new dependencies). Stack is the existing Doughnut backend.

## Package Legitimacy Audit

> No external packages are installed in this phase.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| — | — | — | — | — | — | N/A |

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
Authorized owner
    │
    ▼
POST /api/notebooks/{notebook}/health/lint   (empty/minimal body)
    │
    ▼
NotebookHealthController
    │  authorizationService.assertAuthorization(notebook)   // write/owner
    │
    ▼
NotebookHealthService.lint(notebook, new HealthRunContext())
    │
    ▼
HealthRuleRunner.run  ──►  for each HealthRule bean: evaluate(...)
    │
    ├── EmptyFolderHealthRule (this phase)
    │     │
    │     ├── FolderRepository.findByNotebookIdOrderByIdAsc
    │     ├── NoteRepository: live folder_ids (deletedAt IS NULL)
    │     ├── parent→children map + memoized subtreeHasLiveNotes
    │     └── HealthFindingGroup(ruleId=empty_folders, items=[...])
    │
    ▼
NotebookHealthLintReport { groups: [ empty_folders group ] }
    │
    └── (no DB writes; no dissolve / purge)
```

### Recommended Project Structure

```
backend/src/main/java/com/odde/doughnut/
├── controllers/
│   ├── NotebookHealthController.java     # NEW — POST .../health/lint only
│   └── dto/                              # existing NotebookHealthLintReport, HealthFinding*
├── services/
│   ├── NotebookHealthService.java        # existing — call as-is
│   └── health/
│       ├── HealthRule.java               # existing
│       ├── HealthRuleRunner.java         # existing
│       ├── HealthRuleIds.java            # EMPTY_FOLDERS already reserved
│       ├── HealthRunContext.java         # empty placeholder — reuse
│       └── EmptyFolderHealthRule.java    # NEW
└── entities/repositories/
    ├── FolderRepository.java             # findByNotebookIdOrderByIdAsc exists
    └── NoteRepository.java               # ADD notebook-scoped live folder_id query

backend/src/test/java/com/odde/doughnut/
├── services/health/
│   └── EmptyFolderHealthRuleTest.java    # predicate behaviors
└── controllers/
    └── NotebookHealthControllerTest.java # auth + report shape
```

### Pattern 1: Spring `List<HealthRule>` bean discovery

**What:** Implement `HealthRule` as a `@Service` / `@Component`. Spring injects all implementations into `HealthRuleRunner(List<HealthRule> rules)`.

**When to use:** Always for Health rules (Phase 1 contract).

**Example:**

```java
// Source: Phase 1 HealthRuleRunner + Spring autowired collection beans
// https://docs.spring.io/spring-framework/reference/6.2/core/beans/annotation-config/autowired.html
@Service
public class EmptyFolderHealthRule implements HealthRule {
  private final FolderRepository folderRepository;
  private final NoteRepository noteRepository;

  // id() -> HealthRuleIds.EMPTY_FOLDERS
  // title() -> "Empty folders"
  // severity() -> HealthSeverity.warning
  // autoFixable() -> true
  // evaluate(...) -> group with items
}
```

### Pattern 2: O(folders + notes) emptiness detection

**What:** One load of all folders + one load of live note `folder_id`s; in-memory tree; memoized “subtree has live notes.”

**When to use:** Empty-folder rule and (later) Phase 7 purge re-check.

**Algorithm (prescriptive):**

1. `folders = folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId())` [VERIFIED: `FolderRepository`]
2. `occupiedFolderIds = noteRepository.findLiveNoteFolderIdsByNotebookId(notebookId)` — **add** this query (no existing notebook-wide method):
   ```java
   @Query("""
     SELECT DISTINCT n.folder.id FROM Note n
     WHERE n.notebook.id = :notebookId
       AND n.deletedAt IS NULL
       AND n.folder IS NOT NULL
     """)
   List<Integer> findLiveNoteFolderIdsByNotebookId(@Param("notebookId") Integer notebookId);
   ```
3. Build `Map<Integer /*parentId or null*/, List<Folder>> childrenByParent`.
4. Memoized DFS/BFS: `subtreeHasLiveNotes(folder)` is true if `folder.id ∈ occupied` **or** any descendant is.
5. Report folder if `!subtreeHasLiveNotes(folder)` **and** `isBlankReadme(folder.getReadmeContent())` where blank = `null || isBlank()` (Java `String.isBlank()` covers empty and whitespace-only; null must be guarded).
6. For each match: `HealthFindingItem` with `folderId = folder.getId()`, `label = folder.getName()` (no path).
7. Always return a `HealthFindingGroup` (even if `items` is empty) so the runner’s one-group-per-rule contract stays stable.

### Pattern 3: Thin capability controller (not bloating `NotebookController`)

**What:** Package-private `NotebookHealthController` with `@RequestMapping("/api/notebooks")`, same style as `NotebookBooksController`.

**When to use:** Health endpoints (lint now; fix in Phase 7).

**Example:**

```java
@RestController
@SessionScope
@RequestMapping("/api/notebooks")
class NotebookHealthController {
  private final AuthorizationService authorizationService;
  private final NotebookHealthService notebookHealthService;

  @PostMapping("/{notebook}/health/lint")
  @Transactional // read-only path; follow nearby notebook endpoints if they use @Transactional
  public NotebookHealthLintReport lint(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    return notebookHealthService.lint(notebook, new HealthRunContext());
  }
}
```

No request DTO this phase (D-09). Do **not** add fix options or user defaults fields.

### Anti-Patterns to Avoid

- **N+1 `findNotesInFolderOrderByIdAsc` per folder:** Use one notebook-scoped live-folder-id query.
- **Leaf-only or “direct notes only” emptiness:** Must be recursive subtree (PITFALLS + EFOL-01).
- **Lumping readme-bearing folders into `empty_folders`:** Phase 3 owns `readme_only_folders` (D-02).
- **Calling dissolve / deleting folders from lint:** Lint is report-only (success criterion 3).
- **`assertReadAuthorization` for lint:** Subscribers/bazaar would pass; D-08 requires write/`assertAuthorization`.
- **Phase numbers in product type names:** Use `EmptyFolderHealthRule`, not `Phase2Rule`.
- **Hand-editing generated OpenAPI/TS client:** Fix Java controller, then `pnpm generateTypeScript`.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Rule registry | Custom scanner / static list | Spring `List<HealthRule>` injection | Phase 1 already wires this |
| Report DTO shape | New JSON model | Existing `NotebookHealthLintReport` / group / item | Phase 1 contract; Phase 5 UI depends on it |
| Folder tree load | Ad-hoc SQL walk | `FolderRepository.findByNotebookIdOrderByIdAsc` | Already JOIN FETCH parent, ordered |
| Write authorization | Custom owner checks | `AuthorizationService.assertAuthorization(notebook)` | Same gate as folder create/move/rename/dissolve |
| Soft-delete semantics | Ignore `deletedAt` or invent flags | `deletedAt IS NULL` only (as other repos) | Soft-deleted notes must not occupy folders |
| Blank readme check | Custom trim loops | `readmeContent == null \|\| readmeContent.isBlank()` | Matches D-01 wording |
| OpenAPI client | Hand-written TS types | `CURSOR_DEV=true nix develop -c pnpm generateTypeScript` | generate-api-client skill |

**Key insight:** Phase 2 is almost entirely composition of Phase 1 + existing folder/note repositories. Risk is predicate bugs (recursive / soft-delete / readme split), not framework choice.

## Common Pitfalls

### Pitfall 1: Leaf-only or direct-notes emptiness
**What goes wrong:** Nested empty shells (`A/B/C`) partially reported; or parent with notes only in a deep child is flagged empty.
**Why it happens:** Checking only `findNotesInFolderOrderByIdAsc(folderId)` without descendants.
**How to avoid:** Memoized subtree occupancy from live note folder_ids.
**Warning signs:** Fixture `Parent/Child` both note-free but only one appears (or neither).

### Pitfall 2: Soft-deleted notes count as content
**What goes wrong:** Empty shells with only soft-deleted notes never appear.
**Why it happens:** Forgetting `deletedAt IS NULL` in the occupied-folder query.
**How to avoid:** Query only live notes; tests with `.softDeleted()`.
**Warning signs:** Soft-deleted note in folder suppresses finding.

### Pitfall 3: Readme-only folders lumped into `empty_folders`
**What goes wrong:** Phase 3 boundary blurs; Phase 7 might purge readme content.
**Why it happens:** Treating “no notes” alone as fully empty.
**How to avoid:** Exclude non-blank own `readmeContent` (null / `""` / whitespace-only are blank).
**Warning signs:** Folder with `readmeContent("hello")` appears under `empty_folders`.

### Pitfall 4: Mutating on lint
**What goes wrong:** Folders deleted or dissolved when user only wanted a report.
**Why it happens:** Premature wiring of Phase 7 applicator or reuse of dissolve.
**How to avoid:** Rule and controller only read; assert folder count unchanged after lint in tests.
**Warning signs:** Lint endpoint code path touches `FolderRelocationService.dissolveFolder`.

### Pitfall 5: Wrong auth gate
**What goes wrong:** Subscribers/bazaar readers can run lint; or anon/foreign succeed.
**Why it happens:** Using `assertReadAuthorization` (read includes bazaar/subscription).
**How to avoid:** `assertAuthorization(notebook)` only; tests for foreign user and `currentUser.setUser(null)`.
**Warning signs:** Foreign user gets 200 with report.

### Pitfall 6: Parent listed while readme-only child exists (Phase 7 awareness)
**What goes wrong:** Later purge deletes a parent that has a note-empty but readme-bearing child, destroying readme content.
**Why it happens:** D-01 checks **own** readme only for Phase 2 listing; a parent can be “fully empty” for notes while a child has readme.
**How to avoid:** Phase 2 still follows D-01. Phase 7 must re-check emptiness and refuse folders whose subtree contains non-blank readme (document for later; do not change Phase 2 listing).
**Warning signs:** Parent in `empty_folders` items while child only appears after Phase 3 under `readme_only_folders`.

## Code Examples

### Blank readme predicate

```java
// Matches D-01: null, empty, or whitespace-only
private static boolean isBlankReadme(String readmeContent) {
  return readmeContent == null || readmeContent.isBlank();
}
```

### Finding item construction

```java
HealthFindingItem item = new HealthFindingItem();
item.setFolderId(folder.getId());
item.setLabel(folder.getName());

HealthFindingGroup group = new HealthFindingGroup();
group.setRuleId(HealthRuleIds.EMPTY_FOLDERS);
group.setTitle("Empty folders");
group.setSeverity(HealthSeverity.warning);
group.setAutoFixable(true);
group.setItems(items); // possibly empty list, never null preferred
group.setChildren(List.of()); // or null — flat items only for this rule
```

### Controller auth rejection (test pattern)

```java
// Source: NotebookCrudControllerTest / NotebookBooksRetrievalControllerTest patterns
@Test
void rejectsForeignUser() {
  User owner = makeMe.aUser().please();
  Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
  currentUser.setUser(makeMe.aUser().please());
  assertThrows(UnexpectedNoAccessRightException.class, () -> controller.lint(nb));
}

@Test
void rejectsAnonymous() {
  User owner = makeMe.aUser().please();
  Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
  currentUser.setUser(null);
  assertThrows(UnexpectedNoAccessRightException.class, () -> controller.lint(nb));
}
```

### Fixture patterns (MakeMe)

```java
Folder parent = makeMe.aFolder().notebook(notebook).name("Parent").please();
Folder child = makeMe.aFolder().parentFolder(parent).name("Child").please();
// both fully empty → both items

makeMe.aNote("live").folder(child).please();
// neither empty

makeMe.aNote("gone").folder(child).softDeleted().please();
// still empty (soft-delete ignored)

makeMe.aFolder().notebook(notebook).name("HasReadme").readmeContent("keep me").please();
// not in empty_folders group items
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| No Health API | Phase 1 contract + Phase 2 rule + lint endpoint | 2026-07-22 | First observable Health signal via authorized API |
| Folder removal via dissolve only | Dedicated purge later (Phase 7); lint never removes | product decision | Dissolve promotes children — wrong for empty cleanup |

**Deprecated/outdated:**
- Research docs that say `indexContent` for folders — product uses **`readmeContent`** only [VERIFIED: `Folder.java`, Phase 1/2 CONTEXT].

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| — | *(none material)* | — | All implementation claims verified against live Phase 1 code and repositories |

Blank-readme uses `String.isBlank()` which is standard Java (11+); Doughnut already uses `isBlank()` widely in services [VERIFIED: codebase grep].

## Open Questions

1. **Should `EmptyFolderHealthRule` extract a shared predicate helper for Phase 7?**
   - What we know: Phase 7 must reuse the fully-empty predicate and must not dissolve.
   - What's unclear: Whether to extract now or duplicate briefly.
   - Recommendation: Keep evaluate logic in the rule class; if a small package-private `EmptyFolderPredicate` (or static helpers) is ≤ ~40 lines and used only for “has live notes in subtree + blank own readme,” extract it so Phase 7 can call the same code. Do not build a purge applicator now.

2. **Transactional annotation on lint?**
   - What we know: Nearby notebook mutate endpoints use `@Transactional`; lint is read-only.
   - Recommendation: Use `@Transactional(readOnly = true)` if the project commonly does that; otherwise plain `@Transactional` matching sibling controllers is fine. Prefer consistency with `NotebookBooksController` read methods.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Nix + `CURSOR_DEV=true nix develop -c …` | Backend tests, OpenAPI regen | ✓ (repo contract) | — | Cloud VM skill if no Nix |
| MySQL (test profile) | `@SpringBootTest` / `@Transactional` tests | ✓ via process-compose / migrateTestDB | — | `pnpm backend:test` runs migrate |
| Spring Boot test context | Controller + rule tests | ✓ | — | — |

**Missing dependencies with no fallback:** none for this phase.

**Missing dependencies with fallback:** none.

Step 2.6: External tooling limited to existing backend test + generateTypeScript pipeline.

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Spring Boot Test (`@SpringBootTest`, `@ActiveProfiles("test")`, `@Transactional`) |
| Config file | `backend/` Gradle + Spring `test` profile |
| Quick run command | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` |
| Full suite command | `CURSOR_DEV=true nix develop -c pnpm backend:verify` (formats + migrateTestDB + tests) |

Per backend-testing rule: run **all** backend unit tests (not a single class) when verifying.

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| EFOL-01 | Recursive empty folders reported; soft-deleted notes ignored | service/unit | `pnpm backend:test_only` | ❌ Wave 0 — `EmptyFolderHealthRuleTest` |
| EFOL-01 | Non-blank `readmeContent` excluded from `empty_folders` | service/unit | `pnpm backend:test_only` | ❌ Wave 0 |
| EFOL-02 | Authorized lint returns group/items without fix options | controller | `pnpm backend:test_only` | ❌ Wave 0 — `NotebookHealthControllerTest` |
| Auth SC-4 | Foreign + anonymous rejected | controller | `pnpm backend:test_only` | ❌ Wave 0 |
| No-mutate SC-3 | Folder count unchanged after lint | controller/service | `pnpm backend:test_only` | ❌ Wave 0 |

### Sampling Rate

- **Per task commit:** `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- **Per wave merge:** `CURSOR_DEV=true nix develop -c pnpm backend:verify`
- **Phase gate:** Backend green + OpenAPI client regenerated; no E2E required

### Wave 0 Gaps

- [ ] `backend/src/test/java/com/odde/doughnut/services/health/EmptyFolderHealthRuleTest.java` — recursive emptiness, soft-delete ignored, blank vs non-blank readme, every nested empty folder listed
- [ ] `backend/src/test/java/com/odde/doughnut/controllers/NotebookHealthControllerTest.java` — owner success; foreign/anon fail; report `ruleId=empty_folders`; no mutation
- [ ] Extend existing `HealthRuleRunnerTest` only if needed (optional: Spring context smoke that one rule bean is registered — prefer controller-level integration instead)

Existing infrastructure (`ControllerTestBase`, `MakeMe`, `FolderBuilder`, `NoteBuilder.softDeleted`) covers fixtures — no new test framework.

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes (session user) | Existing `CurrentUser` / session scope controllers |
| V3 Session Management | yes (indirect) | Existing session-scoped controllers — do not invent new session handling |
| V4 Access Control | **yes** | `AuthorizationService.assertAuthorization(notebook)` — owner write only |
| V5 Input Validation | minimal | Empty body; path variable notebook binding via existing converters |
| V6 Cryptography | no | — |

### Known Threat Patterns for notebook Health lint

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Unauthorized lint of another user’s notebook | Information Disclosure | `assertAuthorization` (write/owner); reject foreign + anon |
| Subscriber/bazaar read access used as write | Elevation of Privilege | Do **not** use `assertReadAuthorization` |
| Lint endpoint mutates / deletes folders | Tampering | Report-only service path; no fix applicator in this phase |
| Mass assignment of fix flags in body | Tampering | No request body / no fix fields in Phase 2 |

## Project Constraints (from .cursor/rules/)

| Source | Directive |
|--------|-----------|
| `backend-code.mdc` | Run `CURSOR_DEV=true nix develop -c pnpm backend:verify` (or `backend:test_only` when no migration); prefer entities/DTOs already used as API bodies; introduce DTOs when wire ≠ entity (Health findings already DTO-only); use import statements, not FQCNs |
| `backend-testing.mdc` | Prefer controller/stable-boundary tests for HTTP-visible behavior; service/algorithm tests only for intentional contracts (empty-folder predicate qualifies); `@SpringBootTest` + `@Transactional`; MakeMe builders; one behavior per test; **always run all backend unit tests** (not a single class) |
| `planning.mdc` | Phase is **Behavior** (one observable behavior); stop-safe; no Structure prep beyond immediate need; no phase numbers in product artifacts; targeted tests only (no full E2E); post-change-refactor before commit including **250-line file split** |
| `gsd-coexistence.mdc` | Local Behavior/Structure grammar, Jidoka, commit+push wrap-up, Nix prefix — apply at execute time |
| `generate-api-client` skill | After controller signature change: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`; never hand-edit `packages/generated/doughnut-backend-api` |
| general | Capability naming; high cohesion; no defensive layers unless observed; no comments about past implementations |

## Planner guidance (prescriptive)

1. **One Behavior phase** — do not split into a Structure sub-phase; Phase 1 already supplied structure.
2. **Suggested plan split (stop-safe within phase):**
   - **Plan A:** `NoteRepository` live folder-id query + `EmptyFolderHealthRule` + service tests for EFOL-01 predicate cases (still no public API — but rule is injectable; optional: call `NotebookHealthService.lint` in service test).
   - **Plan B:** `NotebookHealthController` + controller auth/report/no-mutate tests + `pnpm generateTypeScript`.
3. Prefer **thin `NotebookHealthController`** over extending 450-line `NotebookController` (250-line discipline).
4. Label = **folder name** only.
5. Group metadata: `ruleId=empty_folders`, title `"Empty folders"`, `severity=warning`, `autoFixable=true`.
6. Do not implement Phase 3/4/5/6/7 artifacts.
7. After phase: post-change-refactor, backend tests green, OpenAPI client committed, plan updated.

## Sources

### Primary (HIGH confidence)

- Live Phase 1 sources: `HealthRule.java`, `HealthRuleRunner.java`, `HealthRuleIds.java`, `NotebookHealthService.java`, findings DTOs [VERIFIED: codebase]
- `Folder.java` (`readmeContent`, `parentFolder`, `name`), `FolderRepository.findByNotebookIdOrderByIdAsc` [VERIFIED: codebase]
- `Note.java` (`folder` / `folder_id`, `deletedAt`), `NoteRepository.findNotesInFolderOrderByIdAsc` pattern with `deletedAt IS NULL` [VERIFIED: codebase]
- `AuthorizationService.assertAuthorization` → owner-only `hasFullAuthority` [VERIFIED: codebase]
- `NotebookBooksController` package-private colocated controller pattern [VERIFIED: codebase]
- MakeMe: `FolderBuilder`, `NoteBuilder.folder`, `.softDeleted()`, `.readmeContent` [VERIFIED: codebase]
- Spring collection autowiring [CITED: docs.spring.io/spring-framework/reference/6.2/core/beans/annotation-config/autowired.html]
- `.planning/phases/02-empty-folder-findings/02-CONTEXT.md`, Phase 1 CONTEXT, REQUIREMENTS EFOL-01/02, ROADMAP Phase 2
- `.planning/research/ARCHITECTURE.md` empty detection data flow; `.planning/research/PITFALLS.md` recursive emptiness / soft-delete / dissolve / auth

### Secondary (MEDIUM confidence)

- Milestone research ordering (lint before fix) — consistent with locked CONTEXT

### Tertiary (LOW confidence)

- None material

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — no new libraries; Phase 1 + Spring DI verified
- Architecture: HIGH — plug-in rule + thin controller is fully constrained by CONTEXT and live code
- Pitfalls: HIGH — milestone PITFALLS + explicit D-01..D-04 predicates

**Research date:** 2026-07-22
**Valid until:** 2026-08-21 (stable domain; re-check if Phase 1 contract files move)
