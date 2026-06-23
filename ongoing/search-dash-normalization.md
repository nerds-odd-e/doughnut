# Search Dash Normalization Plan

## Goal

Search for note titles, folder names, and notebook names should treat common dash/minus lookalike Unicode characters as equivalent to plain ASCII `-`, without changing stored user-entered titles or names.

Examples to support:

- Stored `alpha-beta`, search `alpha−beta` => match.
- Stored `alpha–beta`, search `alpha-beta` => match.
- The same behavior applies to note title, folder name, notebook name, and the folder picker search by name/path.

## Current Behavior

- Backend literal search is centralized in `NoteSearchService`, then delegated to:
  - `NoteRepository` for `n.title`
  - `FolderRepository` for `f.name`
  - `NotebookRepository` for `nb.name`
- Those repositories each duplicate the same pattern:
  - `LOWER(field) LIKE LOWER(:pattern)`
  - `LOWER(field) = LOWER(:key)`
- Frontend folder destination search in `FolderSearchForm.vue` performs a separate in-memory search:
  - `path.toLowerCase().includes(q)`
  - `name.toLowerCase().includes(q)`

## Design Decision

Use query-time normalization for this change.

Rationale:

- This is a search behavior problem, not a storage requirement.
- Existing names should remain byte-accurate; changing write-time values would alter user data and could create duplicate-resolution work.
- Schema-backed normalized columns/indexes are more invasive and should be driven by performance evidence. Existing partial search already uses `%term%`, so the baseline path is not strongly index-backed.
- A single backend repository query helper can remove the duplicated SQL/JPQL normalization expression while keeping the existing repository boundaries.

The normalization set should be intentionally narrow: dash/minus punctuation and presentation forms only. Do not normalize characters with strong language-specific semantics such as the Japanese prolonged sound mark.

Proposed dash-equivalent set:

```text
U+2010 ‐ HYPHEN
U+2011 ‑ NON-BREAKING HYPHEN
U+2012 ‒ FIGURE DASH
U+2013 – EN DASH
U+2014 — EM DASH
U+2015 ― HORIZONTAL BAR
U+2212 − MINUS SIGN
U+FE58 ﹘ SMALL EM DASH
U+FE63 ﹣ SMALL HYPHEN-MINUS
U+FF0D － FULLWIDTH HYPHEN-MINUS
```

## Phase 1 - Backend Literal Search Normalizes Dash-Like Characters

Type: Behavior

Precondition:

- A user has accessible notes, folders, and notebooks whose title/name uses either ASCII `-` or one of the supported dash/minus lookalikes.

Trigger:

- The user searches with the visually equivalent opposite form through `/api/notes/search` or `/api/notes/{note}/search`.

Postcondition:

- Note title, folder name, and notebook name literal results match as if those dash-like characters were ASCII `-`.
- Exact matches still get distance `0.0`; partial matches still get distance `0.9`.
- Existing case-insensitive behavior is preserved.

Implementation shape:

- Add one repository-level helper for normalized string comparison fragments, for example under `backend/src/main/java/com/odde/doughnut/entities/repositories/`.
- Keep annotation strings compile-time constant friendly by exposing string fragments such as:
  - normalize-expression prefix/suffix using MySQL `REGEXP_REPLACE`
  - normalized `:key` and `:pattern` parameter expressions
- Update `NoteRepository`, `FolderRepository`, and `NotebookRepository` to use the shared helper in their `LIKE` and exact clauses.
- Keep `NoteSearchService.getPattern()` unchanged unless tests reveal an issue; normalizing the SQL parameter expression should handle both stored-value and query-value variants.

Testing:

- Extend `SearchControllerTests` or `NoteSearchServiceExactMatchTest` at the observable search boundary.
- Add focused cases proving:
  - note stored with ASCII `-` matches search with `−` or `–`
  - folder stored with one dash lookalike matches ASCII `-`
  - notebook stored with one dash lookalike matches ASCII `-`
  - exact-note match gets distance `0.0`
- Run:

```bash
CURSOR_DEV=true nix develop -c pnpm backend:test_only
```

## Phase 2 - Frontend Folder Picker Uses The Same Search Semantics

Type: Behavior

Precondition:

- The folder picker has loaded a folder index containing folder paths/names with ASCII `-` or supported dash/minus lookalikes.

Trigger:

- The user opens the folder search dialog and types the visually equivalent opposite form.

Postcondition:

- Folder picker results include matching folder names and paths regardless of dash/minus variant.
- Existing path search, root-row behavior, and sort order remain unchanged.

Implementation shape:

- Add a small frontend utility, for example `frontend/src/utils/searchTextNormalization.ts`, with the same dash-equivalent regex and a `normalizeSearchText` function.
- Use it in `FolderSearchForm.vue` for `q`, `path`, and `name` before `includes`.
- Keep this utility focused on search matching, not general note-content normalization.

Testing:

- Extend existing folder selector/folder search tests rather than creating phase-named tests.
- Add a case where a folder named/path-displayed with one dash variant is found by searching the other variant.
- Run the focused frontend test file, likely:

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/FolderSelector.spec.ts
```

## Phase 3 - Search Surface Regression Sweep

Type: Behavior

Precondition:

- Backend and folder-picker normalization are implemented.

Trigger:

- Users search through the main note/folder/notebook search dialog and the folder-picker search dialog.

Postcondition:

- Main search still displays note, folder, and notebook hits correctly.
- Frontend merge/sort behavior is unchanged for normalized backend hits.
- No duplicate normalization code appears in backend repositories.

Implementation shape:

- Review `SearchResults.vue`, `SearchResultListItem.vue`, and `SearchResultsModel` only for test impact; no production changes expected unless tests reveal a mismatch.
- Confirm backend repository code uses the shared helper consistently.
- Avoid adding a DB migration unless a measured performance regression appears.

Testing:

- Run existing focused search UI tests:

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/search/SearchResults.spec.ts
```

- Optionally run the relationship-search E2E feature if the backend/frontend integration looks risky:

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_view/search_note.feature
```

## Not In This Plan

- Write-time mutation of note titles, folder names, or notebook names.
- Collation changes.
- New generated normalized columns or indexes.
- Broad Unicode confusable normalization beyond dash/minus lookalikes.

Those are larger product decisions because they can affect uniqueness, links, display, and language-specific meaning.
