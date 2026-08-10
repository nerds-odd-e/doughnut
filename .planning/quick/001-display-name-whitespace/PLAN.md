# Display names never carry surrounding whitespace

**Status:** in-progress — phase 1 done
**Scope:** note titles, notebook names, folder names (backend)

## Problem

Leading/trailing whitespace (ASCII and Unicode, e.g. `\u3000`) can be stored in note
titles, notebook names, and folder names through the real HTTP API.

Verified by probe (6 endpoints, MockMvc, deleted after measuring):

| Endpoint | Trimmed? |
|---|---|
| `POST /api/notebooks/{id}/create-note` | no — stored `　 spaced 　` |
| `PATCH /api/text_content/{note}/title` | no — stored `  After  ` |
| `POST /api/notebooks/{id}/folders` | no — stored `  Inbox  ` |
| `PATCH /api/notebooks/{nb}/folders/{f}` | no — stored `  New  ` |
| `POST /api/notebooks/{nb}` (rename) | no — stored `  Renamed  ` |
| `POST /api/notebooks/create` | **yes** |

### Root cause

Trimming is implemented as `@JsonDeserialize(using = DisplayNameTrimmingDeserializer.class)`
on four DTO fields. That annotation and `JsonDeserializer` are **Jackson 2**
(`com.fasterxml.jackson`). Spring Boot 4.1 binds request bodies with **Jackson 3**
(`tools.jackson`, visible in `ObjectMapperConfig`). Jackson 3 ignores the Jackson 2
annotation, so **all four trimming annotations are dead on the HTTP path**.

The one endpoint that works — notebook create — is the one that trims in domain code
(`Ownership.prepareNotebookForNewNotebook`) rather than via the annotation. That is the
whole thesis of this plan: **the invariant must live in the domain, not in the binding layer.**

### Why tests did not catch it

Tests asserting the trim call `objectMapper.readValue(...)` and pass the resulting DTO
straight to the controller method. That `ObjectMapper` bean *is* Jackson 2, so the
annotation fires and the assertion passes — the test proves the annotation works, not that
the product works. `persistsTitleWithoutSurroundingUnicodeWhitespaceFromJson`
(`NotebookNoteCreateControllerTest`) and `NotebookUpdateControllerTest` (line 76) are both
self-fulfilling this way. No test crosses the HTTP boundary, which is exactly the boundary
that broke.

## Design

### Invariant in a value type

`DisplayName` — a record whose single canonical constructor normalizes surrounding
whitespace. There is no other way to construct one, so **an untrimmed display name cannot
be represented**:

```java
public record DisplayName(String value) {
  public DisplayName(String value) {
    this.value = DisplayNamePathSeparators.trimSurroundingWhitespace(value == null ? "" : value);
  }
}
```

Normalization delegates to the existing regex in `DisplayNamePathSeparators` — one
implementation, no duplication.

`DisplayNameConverter implements AttributeConverter<DisplayName, String>`, applied with
explicit `@Convert` on the three columns.

### Entities accept only the type

| Entity | Field | Setter | Getter |
|---|---|---|---|
| `Note` | `DisplayName title` | `setTitle(DisplayName)` | `String getTitle()` |
| `Notebook` | `DisplayName name` | `setName(DisplayName)` | `String getName()` |
| `Folder` | `DisplayName name` | `setName(DisplayName)` | `String getName()` |

Getters keep returning `String`, so no reader, JSON, or OpenAPI change — only the ~10
writer sites move. Narrow write surface, wide read surface. A future contributor cannot
reintroduce the bug: there is no `setTitle(String)` to call.

Bean Validation constraints (`@NotBlank`, `@Size(max = …)`) move from the field to the
`String` getter so entity-level validation keeps working unchanged.

### `DisplayName` permits empty; blankness stays a validation policy

Deliberate. If the constructor rejected empty, the JPA converter would **throw while
reading legacy rows** (`Notebook.name` can be `''` today). Keeping the type total means the
whitespace invariant is unconditional and safe on every read path. Non-blankness is a
per-field policy and belongs in Bean Validation, where it already produces a clean 400.

### One boundary check replaces the dead deserializer

`@NotBlank` on a raw String uses `String.trim()`, which does not strip `\u3000` — so
`"\u3000"` passes today. Replace `@NotBlank` on the four display-name DTO fields with a
single constraint that delegates to `DisplayName` (e.g. `@NotBlankDisplayName`, matching
the existing `@NotReservedNoteTitle` style), and delete `DisplayNameTrimmingDeserializer`.

`@Size(max = …)` and `@Pattern(DisplayNamePathSeparators.REGEXP)` stay untouched: they
carry the OpenAPI `maxLength`/`pattern` metadata. Current spec already emits
`minLength: 0` for these fields, so dropping `@NotBlank` is expected to leave
`open_api_docs.yaml` unchanged — verify, and regenerate the client if it moves.

### Regression net at the HTTP boundary

One capability-named MockMvc class, `DisplayNameNormalizationMvcTest`, asserting normalized
titles/names through real requests for all six endpoints. This is what makes the solution
survive future changes — a Jackson 4, a converter swap, or a new binding path would fail
this test instead of silently regressing. Mirror the annotations of
`SoftDeletedTitleConflictMvcTest` so Spring reuses its cached context rather than starting
a new one (probe run: 6 tests in ~6s).

No E2E scenario is added: input hygiene is not a user journey, and the HTTP boundary is the
level where the defect actually lives. No frontend change either — the API returns the
normalized value, so the UI displays it.

## Phases

### Phase 1 — Note titles are stored trimmed on every path (Behavior)

**Status:** done

Delivers the reported bug fix.

1. **Red:** add `DisplayNameNormalizationMvcTest` with note create and note rename
   scenarios; delete `persistsTitleWithoutSurroundingUnicodeWhitespaceFromJson`. Confirm
   both fail on stored value, not on wiring.
2. **Green:** add `DisplayName` + `DisplayNameConverter`; retype `Note.title`; move
   `@NotBlank`/`@Size` to `getTitle()`; update writers — `Note.initializeNewNote`,
   `TextContentController.updateNoteTitle`, `WikiLinkRewriteService`,
   `TestabilityRestController.buildNote`; and test sites `NoteBuilder`,
   `NotebookBooksControllerTestBase`, `EmbeddingServiceTest`,
   `AiNoteAutomationServiceExtractRequestTest`.

Extract-note is fixed by the same change — it reaches `initializeNewNote` via
`NoteConstructionService.createNote(…, aiResult.newNoteTitle)` — which is the current gap
where `NoteExtractionResult` bypasses trimming entirely.

**Learnings:** Refactor fixed `TestabilityRestController.buildIndividualNotes` map keying —
must use raw `injection.title`, not trimmed `note.getTitle()`, for whitespace-padded test
data.

### Phase 2 — Folder names are stored trimmed on create and rename (Behavior)

**Status:** done

Extend `DisplayNameNormalizationMvcTest` with the two folder scenarios; retype
`Folder.name`; update `FolderConstructionService`, `FolderRelocationService.renameFolder`,
`TestabilityRestController.resolveOrCreateFolderPath`, `FolderBuilder`.

**Learnings:** `FolderRepository.findCandidateChildContainers` and
`FolderSiblingNameValidation` need `DisplayName` params for Hibernate equality queries.
Added `DisplayName` overloads on validation to avoid duplicate construction.

### Phase 3 — Notebook names are stored trimmed on create and rename (Behavior)

**Status:** done

Extend the MockMvc test with the two notebook scenarios; retype `Notebook.name`; update
`NotebookController.updateNotebook`, `Ownership.prepareNotebookForNewNotebook`,
`NotebookBuilder`, `NoteBuilder` (line 47). Drop `Ownership`'s now-redundant manual trim.

Fix the self-fulfilling `readValue` assertion in `NotebookUpdateControllerTest` here — it
only passes because the Jackson 2 mapper applies the annotation.

**Learnings:** `@JsonProperty("name")` on getters required when field is `@JsonIgnore` (Folder
and Notebook). `NotebookRepository` needs `DisplayName` for equality queries. OpenAPI regen
restored Folder `name` schema drift from phase 2.

### Phase 4 — Whitespace-only names are rejected with 400 (Behavior)

**Status:** done

Add `@NotBlankDisplayName` to the four DTO fields, replacing `@NotBlank`; delete
`DisplayNameTrimmingDeserializer`. Extend the MockMvc test with `"\u3000"`-only bodies for
note title, folder name, notebook name.

Then remove what became unreachable — this is the deduplication the change is for:

- `FolderConstructionService.createFolder` `name.isEmpty()` → 400
- `FolderRelocationService.renameFolder` `name.isEmpty()` → 400
- `NotebookController.updateNotebook` `name.isEmpty()` → 400
- `Ownership`'s "skip setName when trimmed is empty" branch

Tests asserting those specific messages move to expecting a binding error. Also fixes a
latent 500: a `"\u3000"` notebook name currently leaves `Notebook.name` null and violates
the `NOT NULL` column.

**Learnings:** `NotBlankDisplayNameValidatorTest` removed as duplicate of DTO/MockMvc tests;
`allowNull` case for optional notebook rename lives in `NotebookUpdateControllerTest`.

### Phase 5 — Existing rows are normalized (Behavior)

**Status:** jidoka-stop — awaiting developer decision

**Start with Jidoka.** Local `doughnut_development` has no `note` table and
`doughnut_test` is wiped, so the affected row count is unknown. Measure against
production-like data first, because all three tables have scoped unique keys —
`uk_note_notebook_folder_title` on `(notebook, folder, lower(title))`,
`uk_folder_notebook_parent_name`, `uk_notebook_ownership_name` — and none include
`deleted_at`. Trimming can therefore collide (`"  A"` + `"A"` in one folder). Bring the
counts and the collision list to the developer before choosing fail-loud vs. deterministic
disambiguation.

**Local measurement (2026-08-10):** `doughnut_e2e_test` — 0 ASCII-untrimmed rows
(`note` 3 rows, `notebook` 1, `folder` 0). `doughnut_test` same. Unicode-only
surrounding whitespace (e.g. `\u3000`) not measured locally — requires Java using
`DisplayName` against production/staging dump.

**Decision needed:** fail-loud migration vs deterministic disambiguation when trim
creates unique-key collisions.

Implement as a **Java** Flyway migration under `backend/src/main/java/db/migration/` (next
version > `300000243`) reusing `DisplayName`, so migration and application share one
normalization rule. MySQL `TRIM()` cannot express the Unicode set, which rules out plain
SQL. Regenerate `docs/database-erd.md` only if the schema changes (it should not).

## Out of scope / follow-ups

- **Path-separator policy in the type.** `DisplayName` intentionally covers whitespace
  only. Two policies exist today — user input *rejects* `\ / :` (`@Pattern`), while
  AI/import paths *convert* them to fullwidth (`normalizeDisplayName`). Folding both into
  the type is a later Structure phase, justified only if repetition appears.
- **Silent truncation** in `Ownership.prepareNotebookForNewNotebook` (notebook names longer
  than 150 are cut without telling the user). Pre-existing; separate decision.
- **Audit remaining Jackson 2-only annotations** for the same class of silent no-op under
  Jackson 3. `@JsonDeserialize` occurs only in these four DTOs, and the Jackson 2
  `objectMapper` bean is used deliberately for non-HTTP work, so nothing else is known to be
  affected — worth one confirming sweep.

## Notes for the executor

- Every phase closes with the local wrap-up: Jidoka, post-change-refactor, plan update,
  commit, push.
- Targeted tests per phase, not the full suite. Phases 1–4 each touch backend unit tests
  plus the one MockMvc class.
- `open_api_docs.yaml` should not move; if it does, follow the `generate-api-client` skill.
