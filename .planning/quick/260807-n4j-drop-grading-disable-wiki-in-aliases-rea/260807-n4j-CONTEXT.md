# Quick Task 260807-n4j: Drop wiki-in-aliases read bridge - Context

**Gathered:** 2026-08-07
**Status:** Ready for planning

<domain>
## Task Boundary

Drop grading / disable the wiki-in-`aliases` read bridge. Overlap matching and UI overlap checks must use authored `overlaps` only.

No data migration: there is no production data with wiki links in `aliases`.

</domain>

<decisions>
## Implementation Decisions

### Data migration
- **None.** Do not write a Flyway/batch migrator. No production notebooks have wiki-link items under `aliases`.

### Save / edit path
- Remove save-time and edit-time rewrite that moves wiki-link items from `aliases` → `overlaps` (`LegacyAliasOverlapMigration`, `migrateLegacyAliasWikiLinksToOverlaps`).
- Wiki-link items under `aliases` remain invalid for authored save (plain-only `aliases` validation already exists) — reject instead of migrate.

### Grading / UI
- `gradingOverlapWikiLinkTokens*` (backend + frontend) reads `overlaps` only — no union with wiki links still sitting in `aliases`.
- Delete dead helpers that only exist for the bridge (`FrontmatterAliases.overlapWikiLinkTokens*`, merge used solely for that union when unused elsewhere).

### Claude's Discretion
- Update tests that asserted bridge/migration behavior to assert overlaps-only grading and reject-on-save for wiki-in-aliases.
- Rewire any fixtures that put wiki links under `aliases` for overlap semantics to use `overlaps:` instead (e.g. stem-masking recall tests).

</decisions>

<specifics>
## Specific Ideas

- Locked by user when leaving `/gsd-new-milestone` for a quick task.
- Tech debt already listed in `.planning/STATE.md` / `PROJECT.md`.

</specifics>

<canonical_refs>
## Canonical References

- `.planning/PROJECT.md` — tech debt: drop grading/disable wiki-in-`aliases` read bridge
- Backend: `FrontmatterOverlaps.gradingOverlapWikiLinkTokensFromFrontmatter`
- Frontend: `gradingOverlapWikiLinkTokens.ts`, `migrateLegacyAliasWikiLinksToOverlaps.ts`
- Save: `AuthoredNoteContent.prepareContentForSave` → `LegacyAliasOverlapMigration`

</canonical_refs>
