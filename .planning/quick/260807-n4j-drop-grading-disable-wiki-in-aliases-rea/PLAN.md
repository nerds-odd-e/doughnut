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

**Status:** planned
**Type:** Behavior

**Observable:** Answering / overlap checks treat only frontmatter `overlaps` as overlap declarations; wiki-link strings under `aliases` do not contribute.

- Change tests that assert union/dedupe across `aliases`+`overlaps` to assert `overlaps`-only (and that wiki-in-`aliases` alone yields no grading tokens).
- Make backend `gradingOverlapWikiLinkTokens*` and frontend `gradingOverlapWikiLinkTokensFromNoteContent` read `overlaps` only.
- Rewire recall fixtures that stuffed wiki links under `aliases` for overlap semantics to use `overlaps:` (e.g. stem masking).

**Done when:** targeted backend + frontend unit tests pass; grading no longer unions aliases wiki links.

### Phase 2 — Behavior: save/edit no longer migrates wiki-in-aliases

**Status:** planned
**Type:** Behavior

**Observable:** Saving note content with a well-formed wiki-link under `aliases` is rejected (validation error), not rewritten into `overlaps`. Edit UI no longer rewrites legacy rows.

- Replace controller migrate-on-save test with reject-on-save.
- Remove `LegacyAliasOverlapMigration` (+ test) and frontend `migrateLegacyAliasWikiLinksToOverlaps` (+ spec); simplify `AuthoredNoteContent.prepareContentForSave` and `richFrontmatterPropertyRowsFromMarkdown`.
- Delete dead `FrontmatterAliases.overlapWikiLinkTokens*` (+ tests) if unused.
- Drop this tech-debt line from `.planning/STATE.md` / `PROJECT.md` Active/deferred notes.

**Done when:** unit tests pass; migration classes gone; wiki-in-`aliases` rejected on save.

## Out of scope

- Flyway / batch notebook content migration
- SEED-001 MCQ / fuzzy / `Notebook:Title`
- ADR 0001 OpenAPI / bare-wiki rename debt
- ADR 0002 git-native notebooks
