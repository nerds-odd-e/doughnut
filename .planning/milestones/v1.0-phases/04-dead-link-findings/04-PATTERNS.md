# Phase 4: Dead-link findings - Pattern Map

**Mapped:** 2026-07-22
**Files analyzed:** 9
**Analogs found:** 9 / 9

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `backend/.../health/DeadWikiLinkHealthRule.java` | service | request-response | `EmptyFolderHealthRule.java` / `ReadmeOnlyFolderHealthRule.java` | exact (rule bean; nest `children` instead of top `items`) |
| `backend/.../services/WikiLinkResolver.java` | service | transform | `WikiLinkResolver.resolveWikiLinksForCache` (same class) | exact (invert resolved loop) |
| `backend/.../health/HealthRunContext.java` | utility | request-response | (empty record today; extend in place) | partial — no prior fields; carry `User` like controller `getCurrentUser()` call sites |
| `backend/.../controllers/NotebookHealthController.java` | controller | request-response | `NotebookHealthController.lint` + `NotebookController` auth+user | exact |
| `backend/.../repositories/NoteRepository.java` | model | CRUD | `findLiveNoteFolderIdsByNotebookId` / `findNotesInFolderOrderByIdAsc` | role-match |
| `backend/.../health/DeadWikiLinkHealthRuleTest.java` | test | request-response | `EmptyFolderHealthRuleTest.java` / `ReadmeOnlyFolderHealthRuleTest.java` | exact |
| `backend/.../health/EmptyFolderHealthRuleTest.java` | test | request-response | self (call-site only) | exact |
| `backend/.../health/ReadmeOnlyFolderHealthRuleTest.java` | test | request-response | self (call-site only) | exact |
| `backend/.../health/HealthRuleRunnerTest.java` | test | request-response | self (call-site + nested `children` fixture) | exact |

**Optional (smoke only):** `NotebookHealthControllerTest.java` — auth/coexistence already covered; extend only if planner wants HTTP-level `dead_wiki_links` presence.

**Unchanged (do not edit):** `HealthRuleRunner`, folder rules, DTOs (`HealthFindingGroup` / `HealthFindingItem` already have `children` / `wikiLinkToken` / `noteId`), `HealthRuleIds.DEAD_WIKI_LINKS` (already reserved), OpenAPI / frontend / E2E.

## Pattern Assignments

### `DeadWikiLinkHealthRule.java` (service, request-response)

**Analog:** `EmptyFolderHealthRule.java` (structure) + `ReadmeOnlyFolderHealthRule.java` (`autoFixable=false` precedent) + `HealthRuleRunnerTest` (nested `children` shape)

**Imports / Spring bean pattern** (`EmptyFolderHealthRule.java` lines 1–22):
```java
import com.odde.doughnut.controllers.dto.HealthFindingGroup;
import com.odde.doughnut.controllers.dto.HealthSeverity;
import com.odde.doughnut.entities.Notebook;
// ... inject NoteRepository + WikiLinkResolver
import org.springframework.stereotype.Service;

@Service
public class DeadWikiLinkHealthRule implements HealthRule {
  // constructor DI — same as folder rules
}
```

**Rule contract** (`HealthRule.java` lines 7–16; `HealthRuleIds.java` line 6):
```java
// id() → HealthRuleIds.DEAD_WIKI_LINKS ("dead_wiki_links")
// title() → "Dead wiki links"
// severity() → HealthSeverity.warning
// autoFixable() → false  // copy ReadmeOnlyFolderHealthRule, NOT EmptyFolderHealthRule
// evaluate(Notebook, HealthRunContext) → HealthFindingGroup
```

**Always-emit group metadata** (`EmptyFolderHealthRule.java` lines 50–58; adapt for `children`):
```java
HealthFindingGroup group = new HealthFindingGroup();
group.setRuleId(id());
group.setTitle(title());
group.setSeverity(severity());
group.setAutoFixable(autoFixable());
// Folder rules: group.setItems(...)
// Dead links: leave top items null/empty; group.setChildren(children)
return group;
```

**`autoFixable=false` precedent** (`ReadmeOnlyFolderHealthRule.java` lines 39–43):
```java
@Override
public boolean autoFixable() {
  return false;
}
```

**Nested children shape** (`HealthRuleRunnerTest.java` lines 38–52 — child mirrors severity/autoFixable; Phase 4 puts leaf items on children, not parent):
```java
HealthFindingGroup child = new HealthFindingGroup();
child.setRuleId("dead_wiki_links"); // same HealthRuleIds.DEAD_WIKI_LINKS
child.setTitle(/* note.getTitle() */);
child.setSeverity(HealthSeverity.warning);
child.setAutoFixable(false);
child.setItems(/* leaf HealthFindingItem list */);
// no further children
```

**Leaf item fields** (`HealthFindingItem.java` + `HealthRuleRunnerTest.java` lines 31–36):
```java
HealthFindingItem item = new HealthFindingItem();
item.setNoteId(note.getId());
item.setWikiLinkToken(token); // full inner, no [[ ]]
item.setLabel(token);         // same as token (D-06)
// do NOT set folderId or message
```

**Core evaluate flow** (compose from RESEARCH + analogs):
```java
List<Note> liveNotes =
    noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
List<HealthFindingGroup> children = new ArrayList<>();
for (Note note : liveNotes) {
  List<String> deadTokens =
      wikiLinkResolver.unresolvedWikiLinkTokens(note, context.viewer());
  if (deadTokens.isEmpty()) {
    continue;
  }
  // build child group with items from deadTokens
  children.add(child);
}
top.setChildren(children);
return top;
```

**Do not:** set top-level `items` for dead tokens; invent a second linker; use `resolveAnyTargetWikiLinkToken`; set `autoFixable=true`.

---

### `WikiLinkResolver.java` — `unresolvedWikiLinkTokens` (service, transform)

**Analog:** same class, `resolveWikiLinksForCache` (lines 47–64) + private `dedupePreserveOrder` / `resolveToken`

**Imports pattern** (lines 1–18 — already present; reuse):
```java
import com.odde.doughnut.algorithms.FrontmatterAliases;
import com.odde.doughnut.algorithms.NoteContentMarkdown;
// Note, User, ArrayList, List, …
```

**Core loop to invert** (lines 47–64):
```java
public List<ResolvedWikiLink> resolveWikiLinksForCache(Note focusNote, User viewer) {
  String content = focusNote.getContent();
  if (content == null || content.isBlank()) {
    return List.of();
  }
  List<String> linkTitlesOrdered = NoteContentMarkdown.wikiLinkInnersInOccurrenceOrder(content);
  if (linkTitlesOrdered.isEmpty()) {
    return List.of();
  }
  List<ResolvedWikiLink> out = new ArrayList<>();
  for (String token : dedupePreserveOrder(linkTitlesOrdered)) {
    Note target = resolveToken(token, viewer, focusNote);
    if (target != null) {
      out.add(new ResolvedWikiLink(token, target));
    }
  }
  return List.copyOf(out);
}
```

**New method pattern** (invert: collect when `target == null`; keep full token string as returned identity — matches `ResolvedWikiLink.linkText`):
```java
public List<String> unresolvedWikiLinkTokens(Note focusNote, User viewer) {
  // same null/blank/empty guards as resolveWikiLinksForCache
  List<String> dead = new ArrayList<>();
  for (String token : dedupePreserveOrder(linkTitlesOrdered)) {
    if (resolveToken(token, viewer, focusNote) == null) {
      dead.add(token); // full inner e.g. "Alpha|friendly alias"
    }
  }
  return List.copyOf(dead);
}
```

**Extract authority** (`NoteContentMarkdown.java` lines 147–161) — do not call `WikiLinkMarkdown.innerTitlesInOccurrenceOrder` on body alone:
```java
public static List<String> wikiLinkInnersInOccurrenceOrder(String content) {
  // frontmatter supportedValueStrings first, then body
}
```

**Dedupe authority** (lines 129–138) — must stay private and shared:
```java
private static List<String> dedupePreserveOrder(List<String> titles) {
  // FrontmatterAliases.normalizedLookupKey — first-occurrence string wins
}
```

**Viewer-readable resolve** (lines 38–40, 70–75) — Health must use this path, not any-target:
```java
public Optional<Note> resolveWikiLinkToken(String token, Note focusNote, User viewer) {
  return Optional.ofNullable(resolveToken(token, viewer, focusNote));
}
// DO NOT use for dead-link classification:
// resolveAnyTargetWikiLinkToken / resolveAnyTargetToken
```

---

### `HealthRunContext.java` (utility, request-response)

**Analog:** currently empty (`HealthRunContext.java` line 3). Pattern for carrying caller is `authorizationService.getCurrentUser()` in controllers (e.g. `NotebookController.java` lines 140–141).

**Target shape** (from RESEARCH / CONTEXT D-01):
```java
public final class HealthRunContext {
  private final User viewer;

  public HealthRunContext(User viewer) {
    this.viewer = viewer;
  }

  public User viewer() {
    return viewer;
  }
}
```

**Call-site updates required** (all use `new HealthRunContext()` today):
| File | Lines |
|------|-------|
| `NotebookHealthController.java` | 37 |
| `EmptyFolderHealthRuleTest.java` | 115 |
| `ReadmeOnlyFolderHealthRuleTest.java` | 156, 169, 185 |
| `HealthRuleRunnerTest.java` | 25 |

Folder rules ignore `context`; tests may pass notebook owner / any non-null `User` / `null` only if constructor allows — **require non-null viewer** so dead-link rule cannot NPE.

---

### `NotebookHealthController.java` (controller, request-response)

**Analog:** self (`lint` method) + `NotebookController` auth-then-current-user pattern

**Current lint** (lines 31–38):
```java
@PostMapping("/{notebook}/health/lint")
@Transactional(readOnly = true)
public NotebookHealthLintReport lint(
    @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
    throws UnexpectedNoAccessRightException {
  authorizationService.assertAuthorization(notebook);
  return notebookHealthService.lint(notebook, new HealthRunContext());
}
```

**Change to** (mirror `NotebookController.java` lines 140–141):
```java
authorizationService.assertAuthorization(notebook);
return notebookHealthService.lint(
    notebook, new HealthRunContext(authorizationService.getCurrentUser()));
```

**Do not:** add endpoints, request body, OpenAPI schema changes, or drop `@Transactional(readOnly = true)`.

---

### `NoteRepository.java` — `findLiveNotesByNotebookIdOrderByIdAsc` (model, CRUD)

**Analog:** `findLiveNoteFolderIdsByNotebookId` (lines 87–94) for live filter; `findNotesInFolderOrderByIdAsc` (lines 80–85) for note entity + `ORDER BY n.id ASC`

**Live-notes filter pattern** (lines 87–94):
```java
@Query(
    """
    SELECT DISTINCT n.folder.id FROM Note n
    WHERE n.notebook.id = :notebookId
      AND n.deletedAt IS NULL
      AND n.folder IS NOT NULL
    """)
List<Integer> findLiveNoteFolderIdsByNotebookId(@Param("notebookId") Integer notebookId);
```

**New query** (notebook-scoped live notes; **not** root-only `findNotesInNotebookRootFolderScopeByNotebookId`):
```java
@Query(
    """
    SELECT n FROM Note n
    WHERE n.notebook.id = :notebookId
      AND n.deletedAt IS NULL
    ORDER BY n.id ASC
    """)
List<Note> findLiveNotesByNotebookIdOrderByIdAsc(@Param("notebookId") Integer notebookId);
```

**Do not:** walk folders with per-folder `findNotesInFolderOrderByIdAsc` (N+1); use root-only scope (misses folder notes).

---

### `DeadWikiLinkHealthRuleTest.java` (test, request-response)

**Analog:** `EmptyFolderHealthRuleTest.java` / `ReadmeOnlyFolderHealthRuleTest.java` for service-entry style; `WikiLinkResolverYamlAndBodyIntegrationTest.java` for wiki fixtures

**Class skeleton** (`EmptyFolderHealthRuleTest.java` lines 30–43):
```java
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DeadWikiLinkHealthRuleTest {
  @Autowired NotebookHealthService notebookHealthService;
  @Autowired MakeMe makeMe;
  // @Autowired NoteAliasIndexService for alias fixtures
  // @Autowired NoteRepository optional for mutation/count asserts

  private User owner;
  private Notebook notebook;

  @BeforeEach
  void setup() {
    owner = makeMe.aUser().please();
    notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
  }
}
```

**Lookup by ruleId, never index** (`EmptyFolderHealthRuleTest.java` lines 114–122; update context):
```java
private HealthFindingGroup deadWikiLinksGroup() {
  NotebookHealthLintReport report =
      notebookHealthService.lint(notebook, new HealthRunContext(owner));
  return report.getGroups().stream()
      .filter(g -> HealthRuleIds.DEAD_WIKI_LINKS.equals(g.getRuleId()))
      .findFirst()
      .orElseThrow(() -> new AssertionError("missing dead_wiki_links group"));
}
```

**Always-emit metadata** (`ReadmeOnlyFolderHealthRuleTest.java` lines 139–148 — `autoFixable=false`):
```java
assertThat(group.getRuleId(), equalTo(HealthRuleIds.DEAD_WIKI_LINKS));
assertThat(group.getTitle(), equalTo("Dead wiki links"));
assertThat(group.getSeverity(), equalTo(HealthSeverity.warning));
assertThat(group.isAutoFixable(), equalTo(false));
// assert children empty (not null); top items empty/null
```

**Coexistence** (`ReadmeOnlyFolderHealthRuleTest.java` lines 152–161):
```java
assertThat(ruleIds, hasItem(HealthRuleIds.EMPTY_FOLDERS));
assertThat(ruleIds, hasItem(HealthRuleIds.README_ONLY_FOLDERS));
assertThat(ruleIds, hasItem(HealthRuleIds.DEAD_WIKI_LINKS));
```

**No-mutation** (`ReadmeOnlyFolderHealthRuleTest.java` lines 164–174 — adapt to notes/content):
```java
// count notes / capture content before lint; assert unchanged after
notebookHealthService.lint(notebook, new HealthRunContext(owner));
```

**Wiki body / FM / alias / qualified / pipe fixtures** (`WikiLinkResolverYamlAndBodyIntegrationTest.java`):

Body live link (lines 40–48):
```java
Note parent = makeMe.aNote().title("Alpha").notebook(notebook).please();
Note child = makeMe.aNote().title("Child").notebook(notebook).content("See [[Alpha]]").please();
```

Frontmatter property link (lines 27–34):
```java
child.setContent("---\nparent: \"[[Alpha]]\"\n---\n\nBody line.");
makeMe.entityPersister.merge(child);
makeMe.entityPersister.flush();
// Dead case: use "[[Missing]]" instead of live title
```

Pipe display text — full inner as token (lines 52–69):
```java
.content("See [[Alpha|friendly alias]]")
// resolved.linkText() == "Alpha|friendly alias" → wikiLinkToken/label same when dead
```

Alias (lines 73–80):
```java
String aliasTargetMarkdown = "---\naliases:\n  - color\n---\n\nbody";
Note aliasTarget =
    makeMe.aNote().title("colour").notebook(notebook).content(aliasTargetMarkdown).please();
noteAliasIndexService.refreshForNote(aliasTarget);
Note linker = makeMe.aNote().notebook(notebook).content("See [[color]]").please();
```

Qualified `Notebook:Title` (lines 91–114):
```java
Notebook otherNotebook =
    makeMe.aNotebook().creatorAndOwner(owner).name("Other Notebook").please();
// target note in otherNotebook; linker content "See [[Other Notebook:LinkedAlias]]"
// or "See [[Other Notebook:Title]]" for title-based qualified live link
```

Soft-deleted source (`EmptyFolderHealthRuleTest.java` line 74):
```java
makeMe.aNote("gone").notebook(notebook).content("See [[Missing]]").softDeleted().please();
// assert no child group for that note
```

**Nested shape assertions** (from CONTEXT D-08/D-09 + `HealthRuleRunnerTest` children retention):
```java
assertThat(group.getChildren(), is(not(nullValue())));
// one child per note with ≥1 dead token; child.title == note title
// child.items: noteId, wikiLinkToken, label; no folderId
// top items empty/null
```

**Recommended test method names** (behavior-oriented, capability not phase): body dead link; FM dead link; live alias not reported; qualified live not reported; soft-deleted source excluded; soft-deleted/missing target dead; distinct-token dedupe; nested children shape; always emit; no mutation; coexists with folder groups.

---

### Call-site test updates (Empty / ReadmeOnly / HealthRuleRunner tests)

**Pattern:** replace `new HealthRunContext()` with `new HealthRunContext(owner)` (or any `User` from `makeMe.aUser()` / notebook creator).

`EmptyFolderHealthRuleTest` already creates `User user` in `@BeforeEach` but does not store it — promote to field and pass to context.

`HealthRuleRunnerTest` has no user — add `makeMe` is not available (pure unit test). Options:
1. Pass `null` only if constructor permits (prefer **not**)
2. Use `new User()` without persist for runner-only tests that never evaluate dead-link logic
3. Construct a minimal `User` stub

Prefer a non-null `new User()` instance for `HealthRuleRunnerTest` since it only exercises runner/DTO retention, not resolve.

## Shared Patterns

### Spring HealthRule discovery
**Source:** `HealthRuleRunner.java` lines 9–18; `EmptyFolderHealthRule` / `ReadmeOnlyFolderHealthRule` `@Service`
**Apply to:** `DeadWikiLinkHealthRule`
```java
@Service
public class HealthRuleRunner {
  private final List<HealthRule> rules;
  // Spring injects all HealthRule beans — no manual registration
  public NotebookHealthLintReport run(Notebook notebook, HealthRunContext context) {
    report.setGroups(rules.stream().map(rule -> rule.evaluate(notebook, context)).toList());
  }
}
```
Do **not** edit `HealthRuleRunner` to register the new rule.

### Always-emit group by ruleId
**Source:** `EmptyFolderHealthRule` / `ReadmeOnlyFolderHealthRule` + tests
**Apply to:** `DeadWikiLinkHealthRule` (empty `children` when no dead links)
```java
// Always return a group even when findings list/children empty
// Tests filter by HealthRuleIds.*, never groups.get(N)
```

### Write-auth lint transport
**Source:** `NotebookHealthController.java` lines 31–38
**Apply to:** Phase 4 controller change only (pass viewer)
```java
authorizationService.assertAuthorization(notebook);
// @Transactional(readOnly = true) — report-only
```

### Viewer-readable resolve authority (no second linker)
**Source:** `WikiLinkResolver.java` lines 47–64, 70–75
**Apply to:** `unresolvedWikiLinkTokens` + `DeadWikiLinkHealthRule`
- Extract: `NoteContentMarkdown.wikiLinkInnersInOccurrenceOrder`
- Dedupe: `dedupePreserveOrder` / `FrontmatterAliases.normalizedLookupKey`
- Resolve: private `resolveToken(token, viewer, focusNote)` only

### Report-only / no mutation
**Source:** controller `@Transactional(readOnly = true)`; `ReadmeOnlyFolderHealthRuleTest.lintDoesNotChangeFolderCount`
**Apply to:** dead-link tests — assert note content / counts unchanged after lint

### Nested findings DTO (already wired)
**Source:** `HealthFindingGroup.java` lines 26–30; `HealthFindingItem.java` lines 14–24
**Apply to:** dead-link children + leaf items — no OpenAPI regen

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| — | — | — | All planned files have in-repo analogs |

**Note:** Nested `children` by note is reserved in DTOs and exercised in `HealthRuleRunnerTest`, but no production rule yet emits per-note children. Closest production analog is folder rules' always-emit group; closest nesting analog is the runner test fixture — combine both.

## Metadata

**Analog search scope:**
- `backend/src/main/java/com/odde/doughnut/services/health/`
- `backend/src/main/java/com/odde/doughnut/services/WikiLinkResolver.java`
- `backend/src/main/java/com/odde/doughnut/algorithms/NoteContentMarkdown.java`
- `backend/src/main/java/com/odde/doughnut/entities/repositories/NoteRepository.java`
- `backend/src/main/java/com/odde/doughnut/controllers/NotebookHealthController.java`
- `backend/src/main/java/com/odde/doughnut/controllers/dto/HealthFinding*.java`
- `backend/src/test/java/com/odde/doughnut/services/health/`
- `backend/src/test/java/com/odde/doughnut/services/WikiLinkResolverYamlAndBodyIntegrationTest.java`
- `backend/src/test/java/com/odde/doughnut/controllers/NotebookHealthControllerTest.java`

**Files scanned:** ~20
**Pattern extraction date:** 2026-07-22
