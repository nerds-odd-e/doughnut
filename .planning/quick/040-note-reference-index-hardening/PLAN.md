# Note reference index hardening

**Status:** complete
**Architecture:** ADR 0001 Wiki link, ADR 0004 OKF Markdown
**Goal:** Close the correctness, cost, and cohesion gaps that the authored-note-reference migration (`.planning/quick/039-authoritative-authored-note-references/PLAN.md`) left behind.

## Outcome (shipped)

- The startup backfill routes stored content through `Note.replaceContent`, so a re-run or a concurrent live save can no longer duplicate `authored_note_reference` rows.
- Inbound discovery selects candidate rows by the target's normalized title/alias keys or a path suffix, instead of live-resolving every wiki reference row in the notebook; title rename asks `NoteReferenceService.isReferencedForViewer` (short-circuits on first match) instead of hydrating every referrer.
- Dead cache-era code removed: `WikiLinkCandidateClassifier.resolveAnyTarget`, `WikiLinkResolver.resolveWikiLinksForCache` + `WikiLinkResolution`, the unread `viewer` parameter on `applyInboundReferrerRewrite`.
- `AuthoredNoteDocument.fromContent` is the one factory for an authored note's referenced content (parsing + ordered dedup) across all five production call sites.
- Regression coverage restored for collation-dependent title resolution (case-insensitive match, distinct `ごろ`/`ゴロ` kana notes) in `NoteControllerShowWikiLinkTests`, replacing what the deleted `ResolvedWikiLink*` suites covered.
- Redundant tests removed: `AuthoredNoteReferenceRowRepositoryTest` (fully duplicated by `TextContentControllerAuthoredReferencePersistenceTest`); `AuthoredNoteReferenceInboundFacadeTest` trimmed to the cases not reachable through a controller (kept the alias-match case — it's the only coverage for that branch of `findWikiCandidatesForNotebookScope`, contrary to the plan's original assumption); a duplicate facade recheck dropped from `TextContentControllerUpdateNoteTitleInboundWikiReferencesTests`.
- Planning docs reconciled: `STATE.md`, 038's plan, and `.planning/codebase/CONCERNS.md` now describe the live-resolution design instead of the removed `ResolvedWikiLinkService`.
- The one-time backfill machinery (`AuthoredNoteReferenceBackfillStartup`/`Tx`/`Progress`, its repository and table) is deleted now that production has completed it; `V300000317` drops the tracking table.
- `WikiLinkRewriteService` collapsed from 179 to 104 lines: six `@Transactional`-only pass-through methods removed, with `@Transactional` moved onto `WikiLinkRelocationRewrite`, which callers (`RelationController`, `FolderRelocationService`, `FolderMoveRelocation`) now call directly.

## Not planned (considered and rejected)

- **`UNIQUE(source_note_id, document_order)` migration** — the fixed write path removed the hazard; a migration would be deploy risk for a hypothesis.
- **Indexed `wiki_note_title_key` column on `authored_note_reference`** — reconsider only if the key-matched query is measured as a bottleneck.
- **Batching resolution in `UnassimilatedPropertyService.isGated`** — optimize only if assimilation streaming is observed to be slow.
- **Extending the frontend mutation barrier to title/property edits** — scoped to the same-note body-autosave race that authored-reference indexing created.
