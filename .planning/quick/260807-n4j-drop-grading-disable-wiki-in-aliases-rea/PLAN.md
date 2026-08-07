# Drop wiki-in-aliases read bridge

## Goal

Overlap grading and UI overlap checks use authored `overlaps` only. Legacy wiki-link items under `aliases` are no longer read as overlaps, and are no longer migrated on save/edit. No production data migration.

## Design decisions

1. **No migration** — user confirmed no production wiki-in-`aliases` data.
2. **Reject, don’t rewrite** — remove `LegacyAliasOverlapMigration` / frontend migrate util; plain-only `aliases` validation rejects wiki-link items on save.
3. **Grading = `overlaps` only** — drop the aliases wiki-link union in backend and frontend token helpers.
4. **Delete bridge-only APIs** — `FrontmatterAliases.overlapWikiLinkTokens*` and related tests go once unused.

## Phases

### Phase 1 — Behavior: overlap grading reads `overlaps` only

**Status:** done
**Type:** Behavior

**Observable:** Answering / overlap checks treat only frontmatter `overlaps` as overlap declarations; wiki-link strings under `aliases` do not contribute.

**Done:** Backend/frontend grading tokens read `overlaps` only; `gradingOverlapWikiLinkTokensFromNoteContent` delegates to `overlapWikiLinkTokensFromNoteContent`. Fixtures/tests rewired to `overlaps:`. Wiki-in-`aliases` alone no longer yields OVERLAP / disable Add as overlapped.

### Phase 2 — Behavior: save/edit no longer migrates wiki-in-aliases

**Status:** planned
**Type:** Behavior

**Observable:** Saving note content with a well-formed wiki-link under `aliases` is rejected (validation error), not rewritten into `overlaps`. Edit UI no longer rewrites legacy rows.

- Replace controller migrate-on-save test with reject-on-save.
- Remove `LegacyAliasOverlapMigration` (+ test) and frontend `migrateLegacyAliasWikiLinksToOverlaps` (+ spec); simplify `AuthoredNoteContent.prepareContentForSave` and `richFrontmatterPropertyRowsFromMarkdown`.
- Delete dead `FrontmatterAliases.overlapWikiLinkTokens*` (+ tests) if unused.
- Drop this tech-debt line from `.planning/STATE.md` / `PROJECT.md` Active/deferred notes.
- Optionally collapse thin `gradingOverlapWikiLinkTokensFromNoteContent` aliases onto `overlapWikiLinkTokens*` call sites if still only a rename (post-Phase-1 leftover).

**Done when:** unit tests pass; migration classes gone; wiki-in-`aliases` rejected on save.

## Out of scope

- Flyway / batch notebook content migration
- SEED-001 MCQ / fuzzy / `Notebook:Title`
- ADR 0001 OpenAPI / bare-wiki rename debt
- ADR 0002 git-native notebooks
