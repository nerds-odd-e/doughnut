# Phase 8: Resolve pull/export (story 1) - Pattern Map

**Mapped:** 2026-08-03
**Files analyzed:** 9
**Analogs found:** 8 / 9

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `backend/.../notebookExport/NotebookZipBuilder.java` | utility | file-I/O + transform | self (strengthen in place) | exact |
| `backend/.../notebookExport/ExportNoteMarkdown.java` *(NEW, discretion)* | utility | transform | `NotebookExportFilenames` + current `noteFileContent` + `WikiLinkMarkdown` matcher loop | role-match |
| `backend/.../NotebookExportService.java` | service | CRUD → zip bytes | self (`exportNotebookAsZip`) | exact |
| `backend/.../controllers/NotebookController.java` (`exportNotebook`) | controller | request-response | self + `UserController` `HttpServletRequest` injection | exact / partial (origin) |
| `backend/.../notebookExport/NotebookZipBuilderTest.java` | test | transform | self | exact |
| `backend/.../notebookExport/ExportNoteMarkdownTest.java` *(NEW if helper extracted)* | test | transform | `NotebookExportFilenamesTest` / `WikiLinkMarkdownTest` | role-match |
| `backend/.../NotebookExportServiceTest.java` | test | CRUD | self (extend only if origin/notebook-name plumbing is observable) | exact |
| `e2e_test/features/cli/cli_export.feature` | test | request-response | self | exact |
| `e2e_test/step_definitions/cli_export.ts` + `exportDestination.ts` | test glue / page object | request-response | self (thin glue; reuse `destinationFileShouldHold`) | exact |

**Out of scope (do not pattern-edit):** `cli/src/sync/writeNotebookExport.ts`, `applyPull`, `Frontmatter.java` (D-06/D-07/HYG-02). CLI only proves zip shape via `/export`.

## Pattern Assignments

### `NotebookZipBuilder.java` (utility, file-I/O + transform)

**Analog:** self — `backend/src/main/java/com/odde/doughnut/services/notebookExport/NotebookZipBuilder.java`

**Imports pattern** (lines 1–12):
```java
package com.odde.doughnut.services.notebookExport;

import com.odde.doughnut.algorithms.NoteLeadingFrontmatter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
```

**Core zip walk + filenames** (lines 20–67) — keep; extend `build(...)` signature to accept notebook name + `publicOrigin`; before/while writing notes, also build `noteId → zipRelativePath` for wiki rewrite:
```java
public static byte[] build(
    String notebookReadmeContent, List<ExportFolderRow> folders, List<ExportNoteRow> notes) {
  // … grouping …
  writeDirectory(zos, "", notebookReadmeContent, /* children */, childFoldersByParent, notesByFolder);
}

Map<Integer, String> noteFileNames =
    NotebookExportFilenames.uniqueFileNames(
        notesHere.stream().map(n -> Map.entry(n.id(), n.title())).toList(), ".md");
for (ExportNoteRow note : notesHere) {
  writeEntry(zos, pathPrefix + noteFileNames.get(note.id()), noteFileContent(note));
}
```

**Current note assembly (gap to close)** (lines 94–99) — replace with identity merge + wiki/attachment rewrites (prefer extract to `ExportNoteMarkdown`):
```java
private static String noteFileContent(ExportNoteRow note) {
  String rawContent = note.content() == null ? "" : note.content();
  String heading = "# " + note.title() + "\n\n";
  return NoteLeadingFrontmatter.splitVerbatim(rawContent)
      .map(split -> split.frontmatterBlock() + "\n\n" + heading + split.body().stripLeading())
      .orElseGet(() -> heading + rawContent.stripLeading());
}
```

**Error handling:** wrap zip I/O in `UncheckedIOException` (lines 44–46) — keep.

**Reuse unchanged:** `NotebookExportFilenames.uniqueFileNames` / `sanitize` for deterministic collision-safe paths (`NotebookExportFilenames.java` lines 14–34).

---

### `ExportNoteMarkdown.java` (utility, transform) — NEW

**Analogs:**
1. Package helper shape: `NotebookExportFilenames.java`
2. Frontmatter split: `NoteLeadingFrontmatter.splitVerbatim`
3. Wiki scan/rewrite loop: `WikiLinkMarkdown.sanitizePathSeparatorsInWikiLinks` / `replaceWikiLinksMatchingTrimmedInner`
4. Target parse: `WikiLinkTargetReference.forToken`
5. Attachment path detect: `NoteContentMarkdown.attachmentImageIdFromPath`

**Package helper shape** (`NotebookExportFilenames.java` lines 9–18):
```java
public final class NotebookExportFilenames {
  private NotebookExportFilenames() {}

  public static String sanitize(String raw) {
    // …
  }
}
```

**Verbatim frontmatter API** (`NoteLeadingFrontmatter.java` lines 18–26):
```java
public record VerbatimSplit(String frontmatterBlock, String body) {}

public static Optional<VerbatimSplit> splitVerbatim(String content) {
  return scan(content).map(s -> new VerbatimSplit(s.frontmatterBlock(), s.body()));
}
```

**Identity merge (D-01/D-02)** — textual inject into fenced block; do **not** use `Frontmatter.parse`/`fenced` dump (HYG-02 / D-02). Target shape from RESEARCH:
```markdown
---
wikidata_id: Q123
doughnut_id: 3
---

# My Note

Actual body text
```
Algorithm: if no fence → `---\ndoughnut_id: {id}\n---`; if fence → replace existing `doughnut_id` line (case-insensitive) or insert before closing `---`; preserve all other YAML lines verbatim.

**Wiki matcher loop to copy** (`WikiLinkMarkdown.java` lines 162–188):
```java
Matcher matcher = INNER_LINK_PATTERN.matcher(markdown);
StringBuilder out = new StringBuilder();
int last = 0;
while (matcher.find()) {
  out.append(markdown, last, matcher.start());
  String rawInner = matcher.group(1);
  // resolve via WikiLinkTargetReference.forToken + noteId→path map;
  // if resolvable: append [display](percent-encoded relative path)
  // else: append matcher.group(0) unchanged
  last = matcher.end();
}
out.append(markdown.substring(last));
return out.toString();
```

**Wiki target parse** (`WikiLinkTargetReference.java` lines 12–25):
```java
public static Optional<WikiLinkTargetReference> forToken(String token, String focusNotebookName) {
  String resolutionKey = WikiLinkMarkdown.splitInner(token).target();
  // qualified Notebook:Title or unqualified → focusNotebookName
}
```

**Display + relative path:** use `WikiLinkMarkdown.splitInner(inner).display()`; relativize with `Path.of(sourceParent).relativize(Path.of(targetPath))`; percent-encode spaces (`%20`).

**Attachment detect** (`NoteContentMarkdown.java` lines 25–26, 78–87):
```java
private static final Pattern ATTACHMENT_IMAGE_PATH_PREFIX =
    Pattern.compile("^/attachments/images/(\\d+)/");

public static Optional<Integer> attachmentImageIdFromPath(String path) {
  Matcher m = ATTACHMENT_IMAGE_PATH_PREFIX.matcher(path.trim());
  if (!m.find() || m.start() != 0) {
    return Optional.empty();
  }
  return Optional.of(Integer.parseInt(m.group(1)));
}
```

**Attachment rewrite target:** stored shape `/attachments/images/{id}/{file}` (`NoteService.java` ~333); exported `{publicOrigin}/attachments/images/{id}/{file}` served by `AttachmentController` (`@RequestMapping("/attachments")` + `@GetMapping("/images/{image}/{fileName}")`, lines 9–18). Do not add attachment blobs to the zip.

---

### `NotebookExportService.java` (service, CRUD → zip)

**Analog:** self — `backend/src/main/java/com/odde/doughnut/services/NotebookExportService.java`

**Imports + DI** (lines 1–21):
```java
@Service
public class NotebookExportService {
  private final FolderRepository folderRepository;
  private final NoteRepository noteRepository;

  public NotebookExportService(FolderRepository folderRepository, NoteRepository noteRepository) {
    this.folderRepository = folderRepository;
    this.noteRepository = noteRepository;
  }
```

**Core orchestration** (lines 23–41) — extend to pass notebook name + `publicOrigin` into `NotebookZipBuilder.build`:
```java
public byte[] exportNotebookAsZip(Notebook notebook) {
  List<ExportFolderRow> folders = /* findByNotebookIdOrderByIdAsc → ExportFolderRow */;
  List<ExportNoteRow> notes = /* findLiveNotesByNotebookIdOrderByIdAsc → ExportNoteRow */;
  return NotebookZipBuilder.build(notebook.getReadmeContent(), folders, notes);
}
```

**Row mapping** stays as today (`ExportNoteRow(id, folderId, title, content)`). Keep service free of rewrite logic (D-06: zip builder owns shape).

---

### `NotebookController.exportNotebook` (controller, request-response)

**Analog:** self — `NotebookController.java` lines 438–450; request injection style from `UserController.java` lines 117–118.

**Auth + zip response** (lines 438–450):
```java
@Operation(operationId = "exportNotebook", summary = "Export notebook as a Markdown zip")
@GetMapping(value = "/{notebook}/export", produces = "application/zip")
@Transactional(readOnly = true)
public ResponseEntity<byte[]> exportNotebook(
    @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
    throws UnexpectedNoAccessRightException {
  authorizationService.assertReadAuthorization(notebook);
  byte[] zipBytes = notebookExportService.exportNotebookAsZip(notebook);
  String filename = notebookExportService.exportFileName(notebook);
  return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
      .contentType(MediaType.valueOf("application/zip"))
      .body(zipBytes);
}
```

**Request injection pattern** (`UserController.java` lines 117–118):
```java
@GetMapping("/token-info")
public UserToken getTokenInfo(HttpServletRequest request) {
  // …
}
```

**Apply:** add `HttpServletRequest request` to `exportNotebook`; derive `publicOrigin` (scheme + host [+ port if non-default], no trailing slash) and pass into service. No in-repo `ServletUriComponentsBuilder` usage found — implement per RESEARCH Pattern 3; verify against E2E `DOUGHNUT_API_BASE_URL` / LB Host.

---

### `NotebookZipBuilderTest.java` (test, transform)

**Analog:** self — `backend/src/test/java/com/odde/doughnut/services/notebookExport/NotebookZipBuilderTest.java`

**Zip fixture helper** (lines 18–27):
```java
private Map<String, String> readZipEntries(byte[] zipBytes) throws IOException {
  Map<String, String> entries = new LinkedHashMap<>();
  try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
    ZipEntry entry;
    while ((entry = zis.getNextEntry()) != null) {
      entries.put(entry.getName(), new String(zis.readAllBytes(), StandardCharsets.UTF_8));
    }
  }
  return entries;
}
```

**Property-preservation baseline to update** (lines 60–71) — today’s test pins **no** identity; Phase 8 must assert `doughnut_id` **merged** with author props:
```java
@Test
void keepsThePropertiesVerbatimAndAddsNoIdentity() throws IOException {
  String contentWithFrontmatter = "---\nwikidata_id: Q123\n---\n\nActual body text";
  ExportNoteRow note = new ExportNoteRow(3, null, "My Note", contentWithFrontmatter);
  // … assert equalTo("---\nwikidata_id: Q123\n---\n\n# My Note\n\nActual body text");
}
```
Rename/replace expectation to include `doughnut_id: 3` while keeping `wikidata_id`. Add cases: no-FM → minimal id block; wiki → relative MD link + unresolved `[[…]]` fallback; nested relativize; attachment absolute URL; zip has no `/attachments/…` entries.

**Call site:** after signature change, pass notebook name + origin into `NotebookZipBuilder.build` in every test (e.g. `"My Notebook"`, `"http://localhost:9081"`).

---

### `ExportNoteMarkdownTest.java` (test, transform) — NEW if helper extracted

**Analogs:** `NotebookExportFilenamesTest.java`, `WikiLinkMarkdownTest.java`

**Pure unit style** (`NotebookExportFilenamesTest.java` lines 8–16):
```java
class NotebookExportFilenamesTest {
  @Test
  void sanitizeReplacesFilesystemInvalidCharactersWithSpaces() {
    String result = NotebookExportFilenames.sanitize("Q&A: What/Why?");
    assertThat(result, equalTo("Q&A What Why"));
  }
}
```

Prefer behavior-named methods; Hamcrest `assertThat`/`equalTo`. Edge cases for merge/rewrite live here or in `NotebookZipBuilderTest` (RESEARCH: units for merge edges, collision paths, nested relativize, attachment rewrite).

---

### `cli_export.feature` + step glue (E2E)

**Analog:** self — `e2e_test/features/cli/cli_export.feature`, `cli_export.ts`, `exportDestination.ts`

**Feature tags + background** (lines 11–26):
```gherkin
@withCliConfig
@interactiveCLI
@disableOpenAiService
Feature: Export a notebook to a local Markdown tree
  Background:
    Given I am logged in as an existing user
    And I set the access token for "old_learner" in the interactive CLI
```

**Content assertion already available** (feature line 55; step `cli_export.ts` 36–40):
```gherkin
And the file "Ben Notebook/LeSS in Action/team.md" in the export destination "./ExportTarget" should hold "Sprint"
```

```typescript
Then(
  'the file {string} in the export destination {string} should hold {string}',
  (relativePath: string, name: string, expected: string) => {
    exportDestination.destinationFileShouldHold(name, relativePath, expected)
  }
)
```

**Page-object assert** (`exportDestination.ts` lines 77–89) — substring match via `includes` / `.should('contain', expectedBody)`:
```typescript
export function destinationFileShouldHold(
  name: string,
  relativePath: string,
  expectedBody: string
) {
  return readUntil(
    () => cy.task<string>('readCliWorkspaceFile', { workspace: resolveDestinationDir(name), relativePath }),
    (found) => found.includes(expectedBody)
  ).should('contain', expectedBody)
}
```

**Apply:** add three scenarios (tag `@wip` until green) asserting:
1. exported note file contains `doughnut_id:`
2. one ordinary Markdown link (not only `[[wiki]]`) for a resolvable internal ref
3. absolute attachment URL containing API origin + `/attachments/images/`

Keep steps one-line glue; do not put rewrite logic in CLI. Inventory assertions (`should hold only:`) stay for hierarchy / no secrets.

---

## Shared Patterns

### Auth on export
**Source:** `NotebookController.exportNotebook` line 444  
**Apply to:** controller only (unchanged contract)
```java
authorizationService.assertReadAuthorization(notebook);
```

### Pure zip algorithm (no Spring in builder)
**Source:** `NotebookZipBuilder` + `NotebookExportFilenames`  
**Apply to:** all identity/link/attachment rewrite code  
Keep rewrites in `notebookExport/` helpers; pass `notebookName` + `publicOrigin` as plain strings from service/controller.

### Verbatim frontmatter
**Source:** `NoteLeadingFrontmatter.splitVerbatim`  
**Apply to:** identity merge  
Do not strip author properties; do not edit Terry-owned `Frontmatter.java`.

### Wiki syntax reuse
**Source:** `WikiLinkMarkdown` + `WikiLinkTargetReference`  
**Apply to:** export wiki→MD rewrite  
Do not invent a second `[[…]]` regex; fallback leave original wiki when unresolved (same-notebook title resolve only this phase).

### Attachment path canon
**Source:** `NoteContentMarkdown.ATTACHMENT_IMAGE_PATH_PREFIX` / `attachmentImageIdFromPath`; `AttachmentController`  
**Apply to:** attachment URL rewrite  
Only rewrite paths matching `/attachments/images/{digits}/…`; prefix with `publicOrigin`; never copy blobs into zip.

### Deterministic filenames
**Source:** `NotebookExportFilenames.uniqueFileNames`  
**Apply to:** path map for links  
Always wiki → note id → path map entry (collision suffix ` (id)`).

### E2E capability surface
**Source:** `cli_export.feature` + thin `cli_export.ts`  
**Apply to:** Story 1 proofs  
Extend existing feature; run `pnpm cypress run --spec e2e_test/features/cli/cli_export.feature`; units via `pnpm backend:test_only`.

### Failure / sync-state (keep)
**Source:** existing export destination missing-dir + `.doughnut-sync/baseline.json` inventory  
**Apply to:** do not regress; out of strengthen gaps.

## No Analog Found

| File / concern | Role | Data Flow | Reason |
|----------------|------|-----------|--------|
| Public origin derivation (`ServletUriComponentsBuilder` / Host+scheme) | controller utility | request-response | No existing controller extracts a public base URL for absolute links; use RESEARCH Pattern 3 + `HttpServletRequest` injection from `UserController`, prove with E2E against LB / `DOUGHNUT_API_BASE_URL` |

## Anti-pattern reminders (from CONTEXT/RESEARCH)

- Do not rewrite in `writeNotebookExport` / CLI unzip (D-06)
- Do not change `applyPull` / preview (D-07)
- Do not restore id-only strip-properties export
- Do not edit `Frontmatter.java` (HYG-02)

## Metadata

**Analog search scope:** `backend/src/main/java/com/odde/doughnut/{services/notebookExport,algorithms,controllers,services}`, `backend/src/test/java/.../notebookExport`, `e2e_test/features/cli`, `e2e_test/step_definitions/cli_export.ts`, `e2e_test/start/pageObjects/cli/exportDestination.ts`  
**Files scanned:** ~25 primary + greps across controllers/algorithms  
**Pattern extraction date:** 2026-08-03
