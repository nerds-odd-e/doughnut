# Frontmatter Aliases (Obsidian-compatible)

## Status

**Shipped:** Phases 1–12.3 (core alias behavior, admin migration, cleanup). Phase 11 skipped.
Post-plan structure cleanup: `ongoing/frontmatter-aliases-cleanup.md` (Phases 1–2, 4–5 done; Phase 3
deferred).

**Remaining:** Phases 13–14 are fuzzy placeholders — refine before `execute-plan`.

## Goal (achieved)

Note aliases live in frontmatter `aliases:` (Obsidian-compatible YAML list). Aliases drive
wiki-link resolution, spelling-recall matching, and cloze masking. Legacy title-segment aliases
were migrated via admin batches (machinery since removed). Plain `／` title-alias authoring is
gone; `／` is a literal title character.

## Settled decisions (current product)

- **Storage:** plain aliases in frontmatter `aliases:`; primary name, qualifier `(...)`, and
  `~`/`〜`/`～` cloze-suffix fragments stay in the title.
- **Alias YAML shape:** one-level YAML list of scalar strings (Obsidian-compatible).
- **Alias normalization:** trim surrounding whitespace, case-insensitive match, NFKC + lowercase
  lookup/dedupe key.
- **Authored aliases:** invalid shapes/items rejected on save (backend + frontmatter editor).
  Read paths ignore invalid list items silently.
- **Invalid alias characters:** `|`, `#`, `^`, `:`, `[[`, `]]`, newlines, path separators.
- **What `aliases` drives:** wiki-link resolution, spelling recall, cloze masking. Not
  quick-switcher.
- **Resolution scope:** focus notebook by default; cross-notebook uses `Notebook:` prefix.
- **Ambiguity:** exact title wins over alias; ambiguous aliases → lowest readable note id.
- **Title `／`:** legal literal character; no alias semantics. Editor still normalizes raw
  `/`, `\`, `:` to fullwidth equivalents.
- **Wikidata Append:** writes/merges frontmatter `aliases`, not title segments.

## Key code (navigation)

- **Read/validate aliases:** `FrontmatterAliases.java`, `AuthoredNoteContent.java`; frontend
  `authoredAliasesValidation.ts`, `frontmatterAliases.ts`.
- **Recall:** `Note.matchAnswer` → `NoteTitle.matchesForRecall` + frontmatter aliases;
  `RecallTitleSegments` for primary + `~` suffix fragments in cloze.
- **Cloze:** `Note.createMaskedContentForRecall` → `ClozedString.hideAliases`.
- **Wiki resolution:** `WikiLinkResolver` (title then alias index), `NoteAliasIndexService`,
  `note_alias_index` table (`alias_display`, `alias_lookup_key`; index on lookup key).
- **Cache refresh:** `WikiTitleCacheService.refreshForNote` rebuilds wiki-title cache, property
  index, and alias index together.
- **Wikidata append:** `wikidataTitleActions.ts`, `useWikidataPropertyDialog.ts`,
  `SuggestTitle.vue`.
- **Title parsing:** `NoteTitle.java`, `TitleFragment.java`.

## Completed work (summary)

| Area | Delivered |
|------|-----------|
| Recall & cloze | Frontmatter aliases accepted/masked; title plain-alias reading removed |
| Alias index & resolution | `note_alias_index`; unambiguous + ambiguous `[[alias]]` within notebook |
| Wikidata | Append writes/merges `aliases` in frontmatter |
| Data migration | Admin batch migration title → frontmatter + inbound link rewrites (machinery removed in 12.3) |
| Validation | Backend save rejection + frontend editor copy for `aliases` |
| Cleanup | No plain title-alias authoring; migration code removed; see cleanup plan for dead-code/schema tidy |

Phase 11 (generic frontmatter property constraint registry) was skipped — Phase 10 local
validation was cohesive enough.

---

## Phase 13 — Fuzzy: folder-path disambiguation for colliding targets

Type: Behavior (placeholder; refine before execution).

- Precondition: two notes share a bare title or alias within the resolution scope.
- Trigger: inserting a link to the colliding title, or a new collision appearing.
- Postcondition: stored links use the shortest hierarchical path that disambiguates
  (`Notebook:parent/child`), mirroring Obsidian's "shortest path when possible".

Notes:
- Requires a new folder-path wiki target grammar and resolution rule. Optional; decompose as its
  own plan before execution.
- Until then, ambiguous bare aliases/titles resolve to the lowest readable note id after
  exact-title precedence.
- Not ready for `execute-plan`; refine into stop-safe phases first.

Tests:
- Resolution/rewrite tests for collision qualification and existing-link auto-update.

## Phase 14 — Fuzzy: alias-aware link insertion suggestions

Type: Behavior (placeholder; refine before execution).

- Precondition: alias resolution is live and aliases are validated.
- Trigger: the user searches/selects an alias when inserting or editing a wiki link.
- Postcondition: Doughnut offers alias matches in the link insertion UI and stores
  `[[Canonical Title|alias]]`; a selected alias is never stored as a bare target.

Notes:
- This is intentionally late and fuzzy because Doughnut does not currently have Obsidian-style
  inline `[[...]]` autocomplete. Before implementation, split this into concrete scenarios around
  the existing search dialog, selected text/dead-link relinking, and possible future inline
  suggestions.
- Quick-switcher behavior remains out of scope unless explicitly re-planned.
- Not ready for `execute-plan`; refine into stop-safe phases first.

Tests:
- Frontend wiki-link insert/edit tests; rich/markdown round-trip.
