# Phase 5: Alias-as-wiki-link overlap declaration - Pattern Map

**Mapped:** 2026-07-24
**Files analyzed:** 14
**Analogs found:** 14 / 14

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `backend/.../algorithms/FrontmatterAliases.java` | utility | transform | self + `WikiLinkMarkdown.java` | exact |
| `backend/.../algorithms/FrontmatterAliasesTest.java` | test | transform | self | exact |
| `frontend/src/utils/authoredAliasesValidation.ts` | utility | request-response | self + `wikiPropertyValueField.ts` (whole-item `[[…]]`) | exact |
| `frontend/tests/utils/authoredAliasesValidation.spec.ts` | test | request-response | self | exact |
| `backend/.../services/NoteAliasIndexServiceTest.java` | test | CRUD | self | exact |
| `backend/.../controllers/TextContentControllerTests.java` | test | request-response | nested `rejectInvalidAuthoredAliasesTest` | exact |
| `backend/.../controllers/SearchControllerAliasTests.java` | test | request-response | self | exact |
| `backend/.../services/WikiLinkResolverYamlAndBodyIntegrationTest.java` | test | request-response | `wikiLinkResolver_resolvesUnambiguousFrontmatterAliasInFocusNotebook` | exact |
| `backend/.../controllers/RecallPromptControllerTests.java` | test | request-response | cloze/alias + accidental-match alias fixtures | exact |
| `backend/.../services/WikiTitleCacheServiceTest.java` | test | CRUD | self (optional awareness) | role-match |
| `frontend/tests/utils/noteContentPropertyRows.spec.ts` | test | request-response | aliases rows in `validatePropertyRowsForRichEdit` | exact |
| `frontend/tests/components/form/RichMarkdownEditor.aliasesProperty.spec.ts` | test | request-response | self | exact |
| `backend/.../services/NoteAliasIndexService.java` | service | CRUD | self — **do not change** if `fromNoteContent` stays plain-only | exact |
| `backend/.../validators/AuthoredNoteContent.java` | middleware | request-response | self — inherits message/behavior from `FrontmatterAliases` | exact |

**Reuse-only (not modified; patterns to call):**

| File | Role | Data Flow | Why |
|------|------|-----------|-----|
| `WikiLinkMarkdown.java` | utility | transform | `INNER_LINK_PATTERN` + `splitInner` for D-01 detection |
| `WikiLinkTargetReference.java` | utility | transform | Phase 6 resolve; optional Phase 5 |
| `Note.java` | model | request-response | `matchAnswer` / cloze already call plain-only accessors |
| `WikiTitleCacheService.java` | service | CRUD | refresh seam calls alias index; keep unchanged |
| `WikiLinkResolver.java` | service | request-response | reads index only; OVL-03 via plain-only index |

## Pattern Assignments

### `backend/.../algorithms/FrontmatterAliases.java` (utility, transform)

**Analog:** self (extend in place) + `WikiLinkMarkdown.java` for wiki-token detection

**Imports pattern** (lines 1–11 of `FrontmatterAliases.java`):
```java
import com.odde.doughnut.validators.DisplayNamePathSeparators;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
```
Add: `import java.util.regex.Matcher;` when using `INNER_LINK_PATTERN.matcher(…).matches()`.

**Wiki-link grammar to reuse** (`WikiLinkMarkdown.java` lines 15–44):
```java
public static final Pattern INNER_LINK_PATTERN = Pattern.compile("\\[\\[([^\\]]+)]]");

public record WikiInnerSplit(String target, String display) {}

public static WikiInnerSplit splitInner(String rawBetweenBrackets) {
  // first | separates target vs display
}
```

**Soft-parse choke point — keep plain-only** (lines 33–40, 89–98):
```java
public static List<String> fromFrontmatter(Frontmatter frontmatter) {
  // ...
  return frontmatter
      .getSequenceItemsIgnoreCase(ALIASES_KEY)
      .map(FrontmatterAliases::validAliasesFromRawItems)
      .orElse(List.of());
}

private static List<String> validAliasesFromRawItems(List<?> items) {
  // filter with isValidPlainAliasText (today isValidAliasText) — exclude wiki-link items
  // dedupePreserveOrder via normalizedLookupKey
}
```

**Authored validation — widen accept** (lines 67–79, 101–106):
```java
private static Optional<String> authoredValidationErrorForRawItems(List<?> items) {
  for (Object item : items) {
    // ...
    if (trimmed.isBlank() || !isValidAliasText(trimmed)) {
      return Optional.of(AUTHORED_ALIASES_MESSAGE);
    }
  }
  return Optional.empty();
}

private static boolean isValidAliasText(String trimmed) {
  if (trimmed.contains("[[") || trimmed.contains("]]")) {
    return false;
  }
  return !INVALID_ALIAS_CHARACTERS.matcher(trimmed).find();
}
```
**Phase 5 change:** split into `isValidPlainAliasText` / `isWikiLinkAliasItem` / `isAcceptableAuthoredAliasItem`; authored path uses the union; soft `validAliasesFromRawItems` keeps plain-only filter.

**Dedupe pattern** (lines 108–121) — reuse for overlap tokens:
```java
public static String normalizedLookupKey(String alias) {
  return Normalizer.normalize(alias, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
}

private static List<String> dedupePreserveOrder(List<String> items) {
  // HashSet of normalizedLookupKey; preserve first occurrence
}
```

**Recommended classification helpers** (from RESEARCH — copy into `FrontmatterAliases`):
```java
private static boolean isWikiLinkAliasItem(String trimmed) {
  Matcher m = WikiLinkMarkdown.INNER_LINK_PATTERN.matcher(trimmed);
  if (!m.matches()) { // entire string — not find()
    return false;
  }
  String inner = m.group(1).trim();
  if (inner.isEmpty()) {
    return false;
  }
  return !WikiLinkMarkdown.splitInner(inner).target().trim().isEmpty();
}
```

**Additive overlap API shape** (new; mirror soft-parse entry points):
```java
public static List<String> overlapWikiLinkTokensFromNoteContent(String content);
public static List<String> overlapWikiLinkTokensFromFrontmatter(Frontmatter frontmatter);
// Return raw "[[…]]" tokens; skip malformed (lenient); dedupe by normalizedLookupKey of full token
```

**Message constant** (lines 18–20) — update copy in lockstep with frontend:
```java
public static final String AUTHORED_ALIASES_MESSAGE =
    "aliases must be a one-level YAML list of nonblank strings that can safely be used as"
        + " wiki-link text.";
// Update to mention wiki-link overlap declarations (D-04); keep single stable string
```

---

### `backend/.../algorithms/FrontmatterAliasesTest.java` (test, transform)

**Analog:** self

**Imports / style** (lines 1–9):
```java
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import org.junit.jupiter.api.Test;

class FrontmatterAliasesTest {
```

**Soft-parse skip-invalid pattern** (lines 32–45) — extend with wiki-link items:
```java
@Test
void fromFrontmatter_skips_blank_and_invalid_items_in_list() {
  Frontmatter fm =
      Frontmatter.parse(
          """
          aliases:
            - color
            - "   "
            - bad|alias
            - [oops]
            - good
          """);
  assertThat(FrontmatterAliases.fromFrontmatter(fm), equalTo(List.of("color", "good")));
}
```
Add capability-named cases e.g.:
- `fromFrontmatter_returns_only_plain_aliases_when_wiki_link_overlap_declared`
- `overlapWikiLinkTokensFromFrontmatter_returns_wiki_link_tokens`
- `authoredValidationError_accepts_well_formed_wiki_link_alias_items`
- `authoredValidationError_rejects_embedded_or_malformed_wiki_link_items`

**Authored reject pattern** (lines 104–137):
```java
assertThat(
    FrontmatterAliases.authoredValidationErrorForNoteContent(pipeItem).orElseThrow(),
    equalTo(FrontmatterAliases.AUTHORED_ALIASES_MESSAGE));
```

---

### `frontend/src/utils/authoredAliasesValidation.ts` (utility, request-response)

**Analog:** self (must stay parity with backend) + whole-item wiki match from `wikiPropertyValueField.ts`

**Current reject-`[[` rule** (lines 1–30):
```typescript
export const AUTHORED_ALIASES_MESSAGE =
  "aliases must be a one-level YAML list of nonblank strings that can safely be used as wiki-link text."

const INVALID_ALIAS_CHARACTERS = /[|#^:]|\\|\/|＼|／|[\r\n]/

function isValidAliasText(trimmed: string): boolean {
  if (trimmed.includes("[[") || trimmed.includes("]]")) return false
  return !INVALID_ALIAS_CHARACTERS.test(trimmed)
}

export function authoredAliasesValidationErrorForPropertyValue(
  value: PropertyValue
): string | undefined {
  if (value.kind === "scalar") {
    return AUTHORED_ALIASES_MESSAGE
  }
  for (const item of value.items) {
    const trimmed = item.trim()
    if (trimmed === "" || !isValidAliasText(trimmed)) {
      return AUTHORED_ALIASES_MESSAGE
    }
  }
  return
}
```

**Whole-item wiki-link regex analog** (`wikiPropertyValueField.ts` lines 163–165):
```typescript
const closed = /^\[\[([^\[\]\r\n]*)\]\]$/.exec(raw)
```
Prefer anchoring the whole string (`^…$` / `.matches()` equivalent), not `includes("[[")` or global `find`.

**Backend grammar mirror for frontend** — align with `WikiLinkMarkdown.INNER_LINK_PATTERN` (`\\[\\[([^\\]]+)]]`) and require non-empty target after `splitInner` semantics (first `|`).

**Do not change:** `frontend/src/utils/frontmatterAliases.ts` (merge/lookup only; no validation).

---

### `frontend/tests/utils/authoredAliasesValidation.spec.ts` (test, request-response)

**Analog:** self

**Structure** (lines 1–17, 49–70):
```typescript
import { describe, expect, it } from "vitest"
import {
  AUTHORED_ALIASES_MESSAGE,
  authoredAliasesValidationErrorForPropertyValue,
  // ...
} from "@/utils/authoredAliasesValidation"
import { listPropertyValue, scalarPropertyValue } from "@/utils/noteProperties"

it("rejects invalid wiki-link characters", () => {
  for (const item of [
    "bad|alias",
    // ...
    "[[nested",
    "brackets]]",
  ]) {
    expect(
      authoredAliasesValidationErrorForPropertyValue(listPropertyValue([item])),
      item
    ).toBe(AUTHORED_ALIASES_MESSAGE)
  }
})
```
Add accept cases for `[[Title]]`, `[[Notebook:Title]]`, pipe forms; keep reject for bare `[[`, embedded `see [[X]]`, `bad|alias`.

---

### `backend/.../services/NoteAliasIndexService.java` (service, CRUD) — prefer **no code change**

**Analog:** self — consumer of plain-only `fromNoteContent`

**Core refresh pattern** (lines 26–50):
```java
@Transactional
public void refreshForNote(Note note) {
  // pessimistic lock + deleteByNoteIdInBulk + flush
  List<String> aliases = FrontmatterAliases.fromNoteContent(note.getContent());
  if (aliases.isEmpty()) {
    return;
  }
  for (String aliasDisplay : aliases) {
    NoteAliasIndex row = new NoteAliasIndex();
    row.setAliasDisplay(aliasDisplay);
    row.setAliasLookupKey(FrontmatterAliases.normalizedLookupKey(aliasDisplay));
    noteAliasIndexRepository.save(row);
  }
}
```
**Phase 5:** leave this file alone if `fromNoteContent` excludes wiki-link items. Do **not** filter `[[` here (anti-pattern: duplicate classification).

**Refresh orchestration** (`WikiTitleCacheService.java` lines 213–218):
```java
public void refreshForNote(Note note, User viewer) {
  rebuildWikiTitleCache(note, viewer);
  notePropertyIndexService.refreshForNote(note);
  noteAliasIndexService.refreshForNote(note);
}
```

---

### `backend/.../services/NoteAliasIndexServiceTest.java` (test, CRUD)

**Analog:** self

**MakeMe + refresh + assert rows** (lines 34–48):
```java
String markdown = "---\naliases:\n  - color\n  - hue\n---\n\nbody";
Note note = makeMe.aNote().title("colour").notebook(notebook).content(markdown).please();

noteAliasIndexService.refreshForNote(note);

List<NoteAliasIndex> rows = noteAliasIndexRepository.findByNote_IdOrderByIdAsc(note.getId());
assertThat(rows, hasSize(2));
assertThat(rows.get(0).getAliasDisplay(), equalTo("color"));
```
Add: mixed plain + `"[[Other Note]]"` → only plain rows; assert no `alias_display` containing `[[`.

---

### `backend/.../controllers/TextContentControllerTests.java` (test, request-response)

**Analog:** nested `rejectInvalidAuthoredAliasesTest` (lines 443–511)

**HTTP binding-error pattern:**
```java
@Test
void accepts_valid_alias_list() throws UnexpectedNoAccessRightException {
  String content =
      """
      ---
      aliases:
        - color
        - hue
      ---

      body
      """;
  noteUpdateContentDTO.setContent(content);
  NoteRealm response = controller.updateNoteContent(note, noteUpdateContentDTO);
  assertThat(response.getNote().getContent(), equalTo(content));
}

@Test
void rejects_invalid_alias_list_item() {
  // ApiException BINDING_ERROR; errors.get("aliases") == AUTHORED_ALIASES_MESSAGE
}
```
Add accept for well-formed wiki-link list items; reject malformed `[[`.

**Authored save seam** (`AuthoredNoteContent.java` lines 12–19) — unchanged:
```java
public static void assertAliasesValidForSave(String content) {
  FrontmatterAliases.authoredValidationErrorForNoteContent(content)
      .ifPresent(
          message -> {
            ApiError apiError = new ApiError(message, ApiError.ErrorType.BINDING_ERROR);
            apiError.add("aliases", message);
            throw new ApiException(apiError);
          });
}
```

---

### `backend/.../controllers/SearchControllerAliasTests.java` (test, request-response)

**Analog:** self

**Fixture helper** (lines 90–115):
```java
private Note createAliasNote(String title) {
  Note note =
      makeMe.aNote(title).creatorAndOwner(currentUser).content(aliasMarkdown()).please();
  noteAliasIndexService.refreshForNote(note);
  return note;
}

private String aliasMarkdown() {
  return "---\naliases:\n  - color\n---\n\nbody";
}
```
Add regression: note with only/mixed wiki-link alias items is **not** returned when searching the token or inner title as alias.

---

### `backend/.../services/WikiLinkResolverYamlAndBodyIntegrationTest.java` (test, request-response)

**Analog:** `wikiLinkResolver_resolvesUnambiguousFrontmatterAliasInFocusNotebook` (lines 72–87)

```java
String aliasTargetMarkdown = "---\naliases:\n  - color\n---\n\nbody";
Note aliasTarget =
    makeMe.aNote().title("colour").notebook(notebook).content(aliasTargetMarkdown).please();
noteAliasIndexService.refreshForNote(aliasTarget);
Note linker = makeMe.aNote().notebook(notebook).content("See [[color]]").please();

var resolved = wikiLinkResolver.resolveWikiLinksForCache(linker, owner);
assertThat(resolved.getFirst().targetNote().getId(), equalTo(aliasTarget.getId()));
```
Add: note whose aliases contain only `"[[Other]]"` is **not** resolvable via alias target `Other` / raw token; plain alias resolve still works.

**Index-reader pattern** (`WikiLinkResolver.java` lines 184–191) — do not modify; safety is index-side:
```java
private List<Note> aliasTargetCandidates(String notebookName, String linkToken) {
  String lookupKey = FrontmatterAliases.normalizedLookupKey(linkToken);
  List<NoteAliasIndex> rows =
      noteAliasIndexRepository.findByNotebookNameAndAliasLookupKeyOrderByNoteIdAsc(
          notebookName, lookupKey);
  // ...
}
```

---

### `backend/.../controllers/RecallPromptControllerTests.java` (test, request-response)

**Analog:** spelling cloze + alias answer + accidental-match alias fixtures

**Cloze masks plain aliases** (lines 486–508):
```java
void spellingQuestionMasksFrontmatterAliasesInStem() {
  makeMe.theNote(answerNote).title("colour")
      .content("""
          ---
          aliases:
            - color
          ---
          The color of the sky is blue
          """)
      .please();
  // stem contains <mark>, not "color"
}
```

**matchAnswer via plain alias** (lines 511–531):
```java
void answerOneOfTheFrontmatterAliases() {
  // aliases: that → spellingAnswer "that" is correct
}
```

**Accidental-match alias leg** (lines 932–957):
```java
Note aliasBearingNote =
    makeMe.aNote().notebook(otherNotebook).title("Unrelated Note Title")
        .content("---\naliases:\n  - " + alias + "\n---\n\nbody")
        .please();
noteAliasIndexService.refreshForNote(aliasBearingNote);
// outcome ACCIDENTAL_MATCH
```
Add OVL-03 cases: overlap wiki-link item does not cloze-mask target title; answering with target title / raw `[[…]]` is not correct via alias; accidental-match alias leg ignores overlap wiki-link items.

**Production cloze / match path** (`Note.java` lines 103–140) — do not modify:
```java
.hideAliases(FrontmatterAliases.fromNoteContent(getContent()));

public boolean matchAnswer(String spellingAnswer) {
  if (getNoteTitle().matchesForRecall(spellingAnswer)) {
    return true;
  }
  return FrontmatterAliases.matchesFromNoteContent(getContent(), spellingAnswer);
}
```

---

### `frontend/tests/utils/noteContentPropertyRows.spec.ts` (test, request-response)

**Analog:** aliases rows (lines 218–240)

```typescript
it("accepts valid aliases list rows", () => {
  expect(
    validatePropertyRowsForRichEdit([
      { key: "aliases", value: listPropertyValue(["color", "hue"]) },
    ])
  ).toEqual({ ok: true })
})

it("rejects invalid alias list items", () => {
  const r = validatePropertyRowsForRichEdit([
    { key: "aliases", value: listPropertyValue(["good", "bad|alias"]) },
  ])
  expect(r.ok).toBe(false)
  if (!r.ok) expect(r.message).toBe(AUTHORED_ALIASES_MESSAGE)
})
```
Add accept for wiki-link list items once `authoredAliasesValidation` is updated (inherits automatically).

---

### `frontend/tests/components/form/RichMarkdownEditor.aliasesProperty.spec.ts` (test, request-response)

**Analog:** self — asserts `AUTHORED_ALIASES_MESSAGE` in popup/row validation.

Extend only if UI path still rejects valid wiki-link aliases after util change; prefer capability-named accept cases using existing `aliasesPropertyTestSupport` helpers.

---

### `backend/.../services/WikiTitleCacheServiceTest.java` (test, CRUD) — optional

**Analog:** self. Prefer asserting plain alias index still populated after refresh; optional awareness that a resolvable `"[[Other]]"` in aliases may create a wiki-title-cache edge (side effect, not OVL-03 regression — do not exclude aliases from wiki extraction).

## Shared Patterns

### Classification at parse time (not per consumer)
**Source:** `FrontmatterAliases` soft vs authored split (existing) + RESEARCH Pattern 1  
**Apply to:** all OVL-03 consumers  
Keep `fromNoteContent` / `fromFrontmatter` / `matchesFromNoteContent` as **plain-only**. Index, cloze, matchAnswer, wiki-resolve alias targets inherit safety without per-service filters.

### Whole-item wiki-link detection
**Source:** `WikiLinkMarkdown.INNER_LINK_PATTERN` + Oracle `Matcher.matches()`; frontend `^\[\[…\]\]$` in `wikiPropertyValueField.ts`  
**Apply to:** `FrontmatterAliases` + `authoredAliasesValidation.ts`  
```java
Matcher m = WikiLinkMarkdown.INNER_LINK_PATTERN.matcher(trimmed);
boolean wholeWikiLink = m.matches(); // not m.find()
```

### Authored validation → BINDING_ERROR on `"aliases"`
**Source:** `AuthoredNoteContent.java` lines 12–19; `TextContentControllerTests` nested class  
**Apply to:** HTTP accept path tests; no new validator class

### Derived index refresh under lock
**Source:** `NoteAliasIndexService.refreshForNote` + `WikiTitleCacheService.refreshForNote`  
**Apply to:** all index/search/resolve regressions — call existing refresh; never add parallel alias refresh call sites

### Deduping
**Source:** `FrontmatterAliases.dedupePreserveOrder` / `normalizedLookupKey`  
**Apply to:** plain aliases (existing) and overlap token lists (new)

### Capability-named tests (no phase numbers)
**Source:** existing suites (`indexes_valid_frontmatter_aliases_…`, `spellingQuestionMasksFrontmatterAliasesInStem`)  
**Apply to:** all new cases — e.g. `indexes_only_plain_aliases_when_wiki_link_overlap_declared`

### Frontend ↔ backend message lockstep
**Source:** identical `AUTHORED_ALIASES_MESSAGE` strings today  
**Apply to:** any message copy update (D-04)

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| — | — | — | All Phase 5 touch files have exact or role-match analogs. Overlap accessors are **additive methods** on existing `FrontmatterAliases` (mirror soft-parse entry points), not a new module. |

**Explicitly out of scope (do not invent analogs / do not touch):**
- `MemoryTrackerService` OVERLAP grading / `AnswerOutcome.OVERLAP` wiring
- Flyway / `NoteAliasIndex` schema type column
- OpenAPI / generated client
- Accidental-match offer-link UI (Phases 2–4)

## Metadata

**Analog search scope:** `backend/src/main/java/com/odde/doughnut/{algorithms,services,entities,validators}`, `backend/src/test/java/...`, `frontend/src/utils`, `frontend/tests`  
**Files scanned:** ~25 primary sources + consumer inventory from RESEARCH  
**Pattern extraction date:** 2026-07-24
