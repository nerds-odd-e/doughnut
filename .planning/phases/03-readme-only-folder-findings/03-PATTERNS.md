# Phase 3: Readme-only folder findings - Pattern Map

**Mapped:** 2026-07-22
**Files analyzed:** 3 (new) + 1 (edit EmptyFolderHealthRule)
**Analogs found:** 3 / 3

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `…/health/ReadmeOnlyFolderHealthRule.java` | service | request-response (lint evaluate) | `…/health/EmptyFolderHealthRule.java` | exact |
| `…/health/FolderSubtreeLiveNotes.java` | utility | transform (tree scan) | methods inside `EmptyFolderHealthRule` (+ `EpubPackageIo` shape) | exact / role-match |
| `…/health/ReadmeOnlyFolderHealthRuleTest.java` | test | request-response via `NotebookHealthService.lint` | `…/health/EmptyFolderHealthRuleTest.java` | exact |
| `…/health/EmptyFolderHealthRule.java` | service | request-response | self (refactor to call helper) | — |

## Pattern Assignments

### `ReadmeOnlyFolderHealthRule.java` (service, request-response)

**Analog:** `backend/src/main/java/com/odde/doughnut/services/health/EmptyFolderHealthRule.java`

**Shape to copy:** `@Service` + `implements HealthRule` + ctor-inject `FolderRepository` / `NoteRepository`; Spring `List<HealthRule>` picks it up — **do not** edit `HealthRuleRunner`.

**Metadata deltas vs empty rule:**

| Method | EmptyFolder | ReadmeOnly |
|--------|-------------|------------|
| `id()` | `HealthRuleIds.EMPTY_FOLDERS` | `HealthRuleIds.README_ONLY_FOLDERS` (already reserved) |
| `title()` | `"Empty folders"` | `"Readme-only folders"` |
| `severity()` | `warning` | `warning` |
| `autoFixable()` | `true` | **`false`** (critical — do not copy-paste `true`) |

**Evaluate loop** (lines 49–73 of analog) — same load + always-emit group; invert blank gate:

```java
// Empty:  noteEmpty && isBlankReadme(...)
// Readme: noteEmpty && !isBlankReadme(...)
item.setFolderId(folder.getId());
item.setLabel(folder.getName());
// group.setRuleId(id()); setTitle; setSeverity; setAutoFixable; setItems
```

**Imports pattern** (lines 1–16 of analog): DTOs + `Folder`/`Notebook` + both repositories + `org.springframework.stereotype.Service`.

---

### `FolderSubtreeLiveNotes.java` (utility, transform)

**Primary analog (algorithm):** private static methods in `EmptyFolderHealthRule.java` lines 76–110 — **move as-is**, do not reimplement.

Extract:

- `childrenByParentId(List<Folder>)` (76–83)
- `subtreeHasLiveNotes(...)` memoized DFS (85–106)
- `isBlankReadme(String)` — `null || isBlank()` (108–110)

**Package shape analog:** `backend/src/main/java/com/odde/doughnut/services/book/EpubPackageIo.java`

```java
final class FolderSubtreeLiveNotes {
  private FolderSubtreeLiveNotes() {}
  static Map<Integer, List<Folder>> childrenByParentId(...) { ... }
  static boolean subtreeHasLiveNotes(...) { ... }
  static boolean isBlankReadme(String readmeContent) {
    return readmeContent == null || readmeContent.isBlank();
  }
}
```

Package-private (`final class`, no `public`) in `services/health/`. Both rules call the same statics so soft-delete / recursion cannot drift.

**After extract:** `EmptyFolderHealthRule.evaluate` keeps blank gate + `autoFixable=true`; load still:

```java
folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId());
noteRepository.findLiveNoteFolderIdsByNotebookId(notebook.getId());
```

---

### `ReadmeOnlyFolderHealthRuleTest.java` (test)

**Analog:** `backend/src/test/java/com/odde/doughnut/services/health/EmptyFolderHealthRuleTest.java`

**Scaffold** (lines 30–43, 114–122): `@SpringBootTest` + `@ActiveProfiles("test")` + `@Transactional`; `@Autowired NotebookHealthService` + `MakeMe`; filter groups by `ruleId` (never `groups.get(0)`).

```java
private HealthFindingGroup readmeOnlyGroup() {
  NotebookHealthLintReport report = notebookHealthService.lint(notebook, new HealthRunContext());
  return report.getGroups().stream()
      .filter(g -> HealthRuleIds.README_ONLY_FOLDERS.equals(g.getRuleId()))
      .findFirst()
      .orElseThrow(() -> new AssertionError("missing readme_only_folders group"));
}
```

**Mirror / invert these cases:**

| EmptyFolderHealthRuleTest | ReadmeOnly equivalent |
|---------------------------|------------------------|
| `listsEveryNestedFullyEmptyFolder` | nested note-empty + non-blank own readme → both listed |
| `liveNoteInDescendantClearsAncestorOccupancy` | same → neither group |
| `softDeletedNoteDoesNotOccupyFolder` | soft-delete + non-blank readme → still in readme_only |
| `nonBlankReadmeExcludesFolderFromEmptyFolders` | invert: non-blank → readme_only; blank/whitespace/null → empty only |
| `alwaysEmitsEmptyFoldersGroupWithMetadata` | always emit; `autoFixable == false`; title `"Readme-only folders"` |

**Add mutual-exclusion** (both groups from one `lint`): look up `EMPTY_FOLDERS` and `README_ONLY_FOLDERS`; assert disjoint `folderId`s; parent non-blank / child blank own-readme partition (D-02).

Fixtures: `makeMe.aFolder()…readmeContent("…")`, `makeMe.aNote(…).folder(…).softDeleted()`.

Hamcrest: same static imports as analog (lines 3–11).

## Shared Patterns

### HealthRule bean discovery
**Source:** `HealthRuleRunner.java` lines 9–19 — `List<HealthRule>` → map `evaluate`. New `@Service` only; no runner / controller / OpenAPI changes.

### Always-emit finding group
**Source:** `EmptyFolderHealthRule.evaluate` lines 67–73 — set metadata from `id()`/`title()`/`severity()`/`autoFixable()` even when `items` empty.

### Lint entry for tests
**Source:** `EmptyFolderHealthRuleTest` — behavior through `notebookHealthService.lint(notebook, new HealthRunContext())`; assert by `HealthRuleIds.*`.

### Blank readme threshold
**Source:** `EmptyFolderHealthRule.isBlankReadme` — share in helper; whitespace-only → empty_folders, not readme_only.

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| — | — | — | All three targets have exact in-package analogs |

## Metadata

**Analog search scope:** `backend/.../services/health/`, package-private `final class` helpers under `services/`
**Files scanned:** ~10
**Pattern extraction date:** 2026-07-22

## PATTERNS COMPLETE
