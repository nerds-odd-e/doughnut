# Phase 2: Empty-folder findings - Pattern Map

**Mapped:** 2026-07-22
**Files analyzed:** 6
**Analogs found:** 6 / 6

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `backend/.../services/health/EmptyFolderHealthRule.java` | service | transform (folder tree → findings DTO) | `HealthRule.java` + `HealthRuleRunner.java` + `HealthRuleRunnerTest.java` (DTO assembly) | exact (contract) / role-match (impl) |
| `backend/.../controllers/NotebookHealthController.java` | controller | request-response | `NotebookBooksController.java` | exact |
| `backend/.../entities/repositories/NoteRepository.java` | repository (modify) | CRUD / query | `NoteRepository.findNotesInFolderOrderByIdAsc` + `findNotesInNotebookRootFolderScopeByNotebookId` | exact |
| `backend/.../services/health/EmptyFolderHealthRuleTest.java` | test | transform / algorithm | `NoteMotionServiceTest.java` + `HealthRuleRunnerTest.java` | role-match |
| `backend/.../controllers/NotebookHealthControllerTest.java` | test | request-response | `NotebookBooksControllerTestBase` + `NotebookBooksAttachControllerTest` + `NotebookCrudControllerTest` auth rejection | exact |
| `packages/generated/doughnut-backend-api/**` (via `pnpm generateTypeScript`) | config / generated client | transform | Existing generate-api-client skill / prior Phase 1 OpenAPI regen | exact |

## Pattern Assignments

### `EmptyFolderHealthRule.java` (service, transform)

**Analog (contract):** `backend/src/main/java/com/odde/doughnut/services/health/HealthRule.java`

**Interface to implement** (lines 7–16):
```java
public interface HealthRule {
  String id();
  String title();
  HealthSeverity severity();
  boolean autoFixable();
  HealthFindingGroup evaluate(Notebook notebook, HealthRunContext context);
}
```

**Analog (discovery):** `backend/src/main/java/com/odde/doughnut/services/health/HealthRuleRunner.java`

**Spring `List<HealthRule>` injection** (lines 8–19):
```java
@Service
public class HealthRuleRunner {
  private final List<HealthRule> rules;

  public HealthRuleRunner(List<HealthRule> rules) {
    this.rules = List.copyOf(rules);
  }

  public NotebookHealthLintReport run(Notebook notebook, HealthRunContext context) {
    NotebookHealthLintReport report = new NotebookHealthLintReport();
    report.setGroups(rules.stream().map(rule -> rule.evaluate(notebook, context)).toList());
    return report;
  }
}
```

**Rule id constant** — `HealthRuleIds.java` lines 3–6:
```java
public static final String EMPTY_FOLDERS = "empty_folders";
```

**Analog (DTO assembly):** `HealthRuleRunnerTest.java` lines 31–52

**Finding group / item construction** (copy shape; Phase 2 severity = `warning`, flat items, no children):
```java
HealthFindingItem item = new HealthFindingItem();
item.setFolderId(10);
item.setLabel("orphan folder");

HealthFindingGroup parent = new HealthFindingGroup();
parent.setRuleId("empty_folders");
parent.setTitle("Empty folders");
parent.setSeverity(HealthSeverity.error); // Phase 2: use HealthSeverity.warning
parent.setAutoFixable(true);
parent.setItems(List.of(item));
parent.setChildren(List.of(child)); // Phase 2: empty list or null — flat items only
```

**Analog (folder load):** `FolderRepository.findByNotebookIdOrderByIdAsc` lines 85–92:
```java
@Query(
    """
    SELECT f FROM Folder f
    LEFT JOIN FETCH f.parentFolder
    WHERE f.notebook.id = :notebookId
    ORDER BY f.id ASC
    """)
List<Folder> findByNotebookIdOrderByIdAsc(@Param("notebookId") Integer notebookId);
```

**Analog (subtree awareness — anti-pattern to avoid for occupancy):** `FolderRelocationService.collectSubtreeFolders` lines 196–208 uses per-parent child queries (N+1). For Health, prefer **one** notebook folder list + in-memory parent→children map + occupied-id set (research Pattern 2). Do **not** call `findNotesInFolderOrderByIdAsc` per folder.

**Blank-string check** — common in services, e.g. `NoteRealmService` / `BookService`:
```java
if (content == null || content.isBlank()) {
  // treat as blank readme
}
```

**Facade call path** — `NotebookHealthService.java` lines 17–19 (controller calls this; rule does not):
```java
public NotebookHealthLintReport lint(Notebook notebook, HealthRunContext context) {
  return healthRuleRunner.run(notebook, context);
}
```

**Core evaluate sketch (compose analogs):**
```java
@Service
public class EmptyFolderHealthRule implements HealthRule {
  private final FolderRepository folderRepository;
  private final NoteRepository noteRepository;

  @Override
  public String id() {
    return HealthRuleIds.EMPTY_FOLDERS;
  }

  @Override
  public String title() {
    return "Empty folders";
  }

  @Override
  public HealthSeverity severity() {
    return HealthSeverity.warning;
  }

  @Override
  public boolean autoFixable() {
    return true;
  }

  @Override
  public HealthFindingGroup evaluate(Notebook notebook, HealthRunContext context) {
    // 1. folders = folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId())
    // 2. occupied = noteRepository.findLiveNoteFolderIdsByNotebookId(...)
    // 3. build childrenByParent; memoize subtreeHasLiveNotes
    // 4. for each folder: !subtreeHasLiveNotes && blank readme → item(folderId, name)
    // 5. always return a HealthFindingGroup (items may be empty)
  }
}
```

---

### `NotebookHealthController.java` (controller, request-response)

**Analog:** `backend/src/main/java/com/odde/doughnut/controllers/NotebookBooksController.java`

**Imports / class shell** (lines 45–61 — package-private, session-scoped, `/api/notebooks`):
```java
@RestController
@SessionScope
@RequestMapping("/api/notebooks")
class NotebookBooksController {

  private final AuthorizationService authorizationService;
  private final BookService bookService;
  // ...

  NotebookBooksController(
      AuthorizationService authorizationService,
      BookService bookService,
      BookFileDownloadCacheControl bookFileDownloadCacheControl) {
    this.authorizationService = authorizationService;
    // ...
  }
```

**Write-auth + mutate POST** (lines 63–88) — copy auth gate; Health uses write auth not read:
```java
@PostMapping(value = "/{notebook}/attach-book", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
@Transactional
public ResponseEntity<Book> attachBook(
    @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
    ...)
    throws UnexpectedNoAccessRightException, IOException {
  authorizationService.assertAuthorization(notebook);
  // ...
}
```

**Read-only `@Transactional` sibling** (lines 101–108) — prefer for lint:
```java
@GetMapping("/{notebook}/book/reading-records")
@Transactional(readOnly = true)
public List<BookBlockReadingRecordListItem> getBookReadingRecords(
    @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
    throws UnexpectedNoAccessRightException {
  authorizationService.assertReadAuthorization(notebook); // lint: use assertAuthorization instead
  return bookService.listReadingRecordsForBook(notebook, authorizationService.getCurrentUser());
}
```

**Folder write gate on `NotebookController`** (lines 160–164) — same `assertAuthorization(notebook)` as Health lint:
```java
throws UnexpectedNoAccessRightException {
  authorizationService.assertAuthorization(notebook);
  return folderConstructionService.createFolder(notebook, request);
}
```

**Prescribed controller body (from research, matching Books pattern):**
```java
@RestController
@SessionScope
@RequestMapping("/api/notebooks")
class NotebookHealthController {
  private final AuthorizationService authorizationService;
  private final NotebookHealthService notebookHealthService;

  @PostMapping("/{notebook}/health/lint")
  @Transactional(readOnly = true)
  public NotebookHealthLintReport lint(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    return notebookHealthService.lint(notebook, new HealthRunContext());
  }
}
```

Do **not** extend 450-line `NotebookController` (250-line post-change-refactor discipline).

---

### `NoteRepository.java` — add live folder-id query (repository, query)

**Analog:** existing soft-delete-aware notebook/folder note queries in `NoteRepository.java` lines 72–85:

```java
@Query(
    value =
        selectFromNote
            + " WHERE n.notebook.id = :notebookId AND n.deletedAt IS NULL AND n.folder IS NULL"
            + " ORDER BY n.id ASC")
List<Note> findNotesInNotebookRootFolderScopeByNotebookId(
    @Param("notebookId") Integer notebookId);

@Query(
    value =
        selectFromNote
            + " WHERE n.folder.id = :folderId AND n.deletedAt IS NULL"
            + " ORDER BY n.id ASC")
List<Note> findNotesInFolderOrderByIdAsc(@Param("folderId") Integer folderId);
```

**New method (research-prescribed; no existing DISTINCT folder_id query):**
```java
@Query("""
  SELECT DISTINCT n.folder.id FROM Note n
  WHERE n.notebook.id = :notebookId
    AND n.deletedAt IS NULL
    AND n.folder IS NOT NULL
  """)
List<Integer> findLiveNoteFolderIdsByNotebookId(@Param("notebookId") Integer notebookId);
```

**Imports pattern** (file head lines 1–11): keep `@Query` / `@Param` / `CrudRepository` style; no FQCNs.

---

### `EmptyFolderHealthRuleTest.java` (test, algorithm)

**Analog (Spring + MakeMe service test):** `NoteMotionServiceTest.java` lines 19–37:

```java
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class NoteMotionServiceTest {
  @Autowired NoteMotionService noteMotionService;
  @Autowired NoteRepository noteRepository;
  @Autowired MakeMe makeMe;

  @Test
  void executeMoveIntoFolder_setsFolderAndNotebook() {
    User user = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
    Folder folder = makeMe.aFolder().notebook(notebook).name("Dest").please();
    // ...
  }
}
```

**Analog (DTO assertions):** `HealthRuleRunnerTest.java` — Hamcrest `assertThat` / `equalTo` / `hasSize` on `HealthFindingGroup`.

**Fixture patterns:**
- Nested folders: `NotebookFolderManagementControllerTest` MoveFolder — `makeMe.aFolder().notebook(nb).name("Parent")` + `makeMe.aFolder().parentFolder(parent).name("Child")`
- Note in folder: `makeMe.aNote("live").folder(child).please()`
- Soft-delete: `makeMe.aNote("gone").folder(child).softDeleted().please()` (`NoteBuilder.softDeleted` lines 153–157; used in `UserModelSearchTest` / `NotebookTest`)
- Readme: `FolderBuilder.readmeContent` — `makeMe.aFolder().notebook(nb).name("HasReadme").readmeContent("keep me").please()`

**Required behaviors (one test each):** recursive empty both listed; live note clears ancestor occupancy; soft-deleted ignored; non-blank readme excluded from `empty_folders`; always emit group with `ruleId=empty_folders`.

---

### `NotebookHealthControllerTest.java` (test, request-response)

**Analog (autowire package-private controller):** `NotebookBooksControllerTestBase.java` lines 38–70:

```java
abstract class NotebookBooksControllerTestBase extends ControllerTestBase {
  @Autowired NotebookBooksController controller;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  Notebook myNotebook() {
    return makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
  }

  Notebook otherUsersNotebook() {
    return makeMe.aNotebook().creatorAndOwner(makeMe.aUser().please()).please();
  }
}
```

**Base stack:** `ControllerTestBase` — `@SpringBootTest` + `@ActiveProfiles("test")` + `@Transactional` + `MakeMe` + `CurrentUser` `@TestBean`.

**Foreign-user write rejection** — `NotebookBooksAttachControllerTest` lines 127–130:
```java
Notebook otherNb = otherUsersNotebook();
assertThrows(
    UnexpectedNoAccessRightException.class,
    () -> controller.attachBook(otherNb, attachRequest(node("A")), pdfFile(STUB_PDF_BYTES)));
```

**Foreign-user rejection (CRUD write)** — `NotebookCrudControllerTest` lines 178–185:
```java
User owner = makeMe.aUser().please();
Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
currentUser.setUser(makeMe.aUser().please());
assertThrows(
    UnexpectedNoAccessRightException.class,
    () -> controller.updateNotebookReadmeContent(nb, dto));
```

**Anonymous:** `currentUser.setUser(null)` then `assertThrows(UnexpectedNoAccessRightException.class, () -> controller.lint(nb))` — note write endpoints throw `UnexpectedNoAccessRightException` (not always `ResponseStatusException`; prefer matching `assertAuthorization` behavior).

**Owner success:** set `currentUser` to owner; create empty folders via MakeMe; call `controller.lint(nb)`; assert group `ruleId` / items / folder count unchanged (no-mutate).

---

### Generated OpenAPI / TS client (config, transform)

**Analog:** prior Phase 1 OpenAPI regen for Health DTOs already in `packages/generated/doughnut-backend-api`.

**Do not hand-edit.** After controller signature exists:
```bash
CURSOR_DEV=true nix develop -c pnpm generateTypeScript
```
Follow `.cursor/skills/generate-api-client/SKILL.md`.

## Shared Patterns

### Authentication (write / owner only)
**Source:** `AuthorizationService.assertAuthorization` + `NotebookBooksController` / `NotebookController` folder mutate
**Apply to:** `NotebookHealthController`
```java
authorizationService.assertAuthorization(notebook);
// Do NOT use assertReadAuthorization — bazaar/subscribers would pass
```

### Error / auth rejection
**Source:** `UnexpectedNoAccessRightException` thrown by `assertAuthorization`
**Apply to:** controller tests for foreign + anonymous
```java
assertThrows(UnexpectedNoAccessRightException.class, () -> controller.lint(nb));
```

### Soft-delete awareness
**Source:** `NoteRepository` queries with `n.deletedAt IS NULL`; `NoteBuilder.softDeleted()`
**Apply to:** live folder-id query + predicate tests
```java
AND n.deletedAt IS NULL
```

### Findings DTO contract (Phase 1 — do not reinvent)
**Source:** `HealthFindingGroup`, `HealthFindingItem`, `NotebookHealthLintReport`, `HealthSeverity`
**Apply to:** `EmptyFolderHealthRule.evaluate`
- Group: `ruleId`, `title`, `severity`, `autoFixable`, `items`, optional `children`
- Item: `folderId` + `label` (name); optional `message`
- Always return one group per rule (empty `items` OK)

### Test harness
**Source:** `ControllerTestBase` / `NotebookBooksControllerTestBase` / `@SpringBootTest` service tests
**Apply to:** both new test classes
- MakeMe builders; one behavior per test; Hamcrest `assertThat`
- Run all backend unit tests: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`

### Import style
**Source:** `.cursor/rules/backend-code.mdc`
**Apply to:** all new Java — import statements, no FQCNs

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| — | — | — | None material. Closest gaps are compositional: no existing `HealthRule` bean yet (Phase 1 left interface + runner only); no `SELECT DISTINCT n.folder.id` query — extend `NoteRepository` using soft-delete query style above. |

## Metadata

**Analog search scope:** `backend/src/main/java/com/odde/doughnut/{controllers,services,entities/repositories}`, `backend/src/test/java/com/odde/doughnut/{controllers,services}`, Health Phase 1 package
**Files scanned:** ~25 primary analogs (Health contract, NotebookBooks/Notebook controllers, Note/Folder repositories, FolderRelocationService, ControllerTestBase family, MakeMe builders)
**Pattern extraction date:** 2026-07-22
