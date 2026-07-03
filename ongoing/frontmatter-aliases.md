# Frontmatter Aliases

## Current Behavior

Note aliases live in the note content frontmatter as an `aliases:` YAML list. Aliases drive
wiki-link resolution, spelling-recall matching, and cloze masking.

## Product Rules

- `aliases:` is a one-level YAML list of scalar strings.
- Alias matching trims surrounding whitespace and compares case-insensitively.
- Alias lookup and dedupe use NFKC plus lowercase normalization.
- Save paths reject invalid authored `aliases:` shapes/items in backend validation and the
  frontend frontmatter editor.
- Invalid alias text includes `|`, `#`, `^`, `:`, `[[`, `]]`, newlines, and path separators.
- Exact note title matches take precedence over alias matches.
- Alias resolution is scoped to the focus notebook by default; cross-notebook links use a
  `Notebook:` prefix.
- When multiple readable notes share an alias, resolution uses the lowest readable note id.
- The note-title editor still normalizes raw `/`, `\`, and `:` to fullwidth path-safe characters.
- Wikidata "Add as alias" writes or merges the suggested label into frontmatter `aliases:`.

## Code Navigation

- Read/validate aliases: `FrontmatterAliases.java`, `AuthoredNoteContent.java`,
  `authoredAliasesValidation.ts`, `frontmatterAliases.ts`.
- Recall: `Note.matchAnswer` checks the literal title and frontmatter aliases.
- Cloze: `Note.createMaskedContentForRecall` passes frontmatter aliases to `ClozedString`.
- Wiki resolution: `WikiLinkResolver` resolves by title first, then `note_alias_index`.
- Index refresh: `WikiTitleCacheService.refreshForNote` rebuilds wiki-title cache, property index,
  and alias index together.
- Wikidata alias writes: `wikidataTitleActions.ts`, `useWikidataPropertyDialog.ts`,
  `SuggestTitle.vue`.
