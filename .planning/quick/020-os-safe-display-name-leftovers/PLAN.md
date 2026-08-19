# Plan: OS-safe display-name leftovers

**Status:** in progress

**Goal:** After filename = display name, tests and the title editor match names users can actually save. No new filename policy.

**Feeds:** Shipped by `.planning/quick/017-os-safe-display-names/` (directory removed). Proposed [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown.md).

## Locked for this plan

- Create/rename still **rejects** `\ / : * ? " < > |` and ASCII controls. PathNameEditor may still **convert** `\ / :` to fullwidth as today — do not unify convert-vs-reject without a human value decision.
- Do not invent ZIP collision suffixes. Do not restore sanitize-driven `title:` wrap.
- Canonical OS-invalid **message text** lives in `DisplayNamePathSeparatorsValidationTest` (and production `MESSAGE`). Do not pin that full string in a second test.

## Findings (017 follow-through)

**Export fixtures (slice 2 done):** ZIP/catalog tests now use savable `Q&A Notes` / `Q&A What Why` (`&` legal). E2E `Recipe` / `Recipe＊` sibling-uniqueness unchanged.

**Wiki-title cache (slice 1 done):** `rewriteWikiTitleCache` already converted `link_text` and deleted same-note duplicates. Tests plant stale rows via JDBC INSERT (`Recipe*` cannot go through entities).

**Redundant tests:** `AiControllerExtractNotePreviewTest.shouldConvertOsInvalidCharactersInExtractionPreview` (title `Recipe*` only) is covered by extract preview already calling `normalizeDisplayName` (`shouldSanitizePathSeparatorsInExtractionPreview`) plus `DisplayNamePathSeparatorsTrimTest.normalizeDisplayNameConvertsOsInvalidCharacters`. E2E `Then I should see that the title is rejected as OS-invalid` re-pins the full `MESSAGE` string.

**Misleading editor warning:** PathNameEditor `LINK_BREAK_CHARS` includes `|`, which is now OS-invalid. Typing `|` shows the wiki-link warning; save then rejects as OS-invalid. `|` should behave like `*` (no wiki warning; reject on save).

## Slices

### 1. Wiki-title cache follows converted spellings

- **Type:** Behavior
- **Status:** done

Production already rewrote cache `link_text` (`*` → `＊`) and deleted same-note duplicates. Extended `DisplayNameOsInvalidCharsBackfillTest` with convert + duplicate-delete; JDBC INSERT for stale cache rows.

### 2. Export contract uses savable display names

- **Type:** Behavior
- **Status:** done

Replaced illegal `Q&A: What/Why?` / `Q&A: Notes` with `Q&A What Why` / `Q&A Notes`. ZIP entry remains `{title}.md`; download `{notebook name}.zip`. Dropped unused `creatorAndOwner` on the exportFileName fixture.

### 3. One OS-invalid copy; drop redundant extract `*`

- **Type:** Structure
- **Status:** planned

Structure change: delete `shouldConvertOsInvalidCharactersInExtractionPreview`. Rename `shouldSanitizePathSeparatorsInExtractionPreview` so it says convert, not sanitize. E2E OS-invalid Then asserts rejection without repeating `MESSAGE` (unit test owns the sentence).

Unlocks slice 4: `|` can reuse the same reject path without a third copy of the message.

### 4. Pipe in a title is OS-invalid, not a wiki-link warning

- **Type:** Behavior
- **Status:** planned

**Pre-condition:** user is editing a note/folder/notebook name in PathNameEditor.

**Trigger:** type `|`.

**Post-condition:** the wiki-link warning (`#^[]|`) is not shown for `|`. Save is still rejected as OS-invalid (same as `Recipe*`). `#` `^` `[` `]` still warn as wiki-breakers.

Unit: `PathNameEditor.spec.ts`. Do not add a second E2E if slice 3 left `Recipe*` as the create-reject scenario.

## Not this plan

- Unify PathNameEditor fullwidth-convert of `\ / :` with backend reject (value fork).
- Rename `DisplayNamePathSeparators` / shotgun rename of “sanitize” in AI extract service.
- Book-file `sanitizeFileName` (different download).
- Edit Proposed ADR 0002’s Git `Recipe (2).md` example (human-owned).
- Extra folder/notebook E2E for `*` (shared `REGEXP` already on those DTOs; note create is the user path).
