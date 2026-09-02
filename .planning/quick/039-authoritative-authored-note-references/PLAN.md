# Authoritative authored note references

**Status:** in progress  
**Resume:** next slice is 22 (assimilation gates on current reference resolution). Slices 1–21 done.  
**Source:** `.planning/notes/notebook-scope-wiki-refresh-on-title-and-create.md`  
**Architecture:** ADR 0004, “Links and attachments”  
**Goal:** Make authored semantic note references a domain-owned representation derived from note Markdown, and make every consumer resolve those references against current state without notebook-wide cache refreshes.

## Outcome

When this plan is complete:

- `AuthoredNoteReference` is the one domain type for wiki Portable-path targets and semantic Donut note-ID URLs.
- Stored note Markdown remains authoritative. A source-owned `authored_note_reference` index mirrors every distinct authored reference, including missing and ambiguous wiki targets.
- Resolution has one sealed result (`Resolved`, `Missing`, or `Ambiguous`) computed from the authored reference, source scope, current notebook state, and current viewer.
- Outgoing links, inbound references, rename/delete/move rewriting, focus context, and property assimilation all use one note-reference facade. They cannot query a resolved-link repository directly.
- Every production content mutation updates Markdown and its authored-reference children through `Note.replaceContent(AuthoredNoteDocument)`.
- Same-note body autosave completes before delete; no delayed content PATCH after deletion begins. Delete/restore do not walk the notebook to rebuild `resolved_wiki_link`.
- `resolved_wiki_link`, `ResolvedWikiLink*`, `NotePropertyIndex.targetNote`, and notebook-scope resolution refreshes are gone.

## Current code (remaining slices)

- **Index writes:** production create/save/rewrite go through `Note.replaceContent`. `AuthoredNoteContent.prepareDocumentForSave` also injects `type` via `NoteConceptType.ensureStoredType` — rewrites use `WikiLinkRewriteSupport.documentFromRewrittenContent` instead. Tests that need inbound discovery use `MakeMe.authorReferencingContent` (same parse, no type injection). Raw `Note.setContent` / builder `.content(...)` bypass the index.
- **Inject:** done (Slice 17) — `InjectNotesWorker` now also calls `Note.replaceContent(AuthoredNoteDocument)` per note after save, before the existing `refreshForNote` cache refresh.
- **Inbound facade:** `AuthoredNoteReferenceInboundFacade` in `entities.repositories` (must share the package-private `AuthoredNoteReferenceRowRepository`). Live-resolves candidates; exposes `distinctReferrerNotesForViewer`, `distinctReferrerIdsForViewer`, `distinctInboundReferencesForViewer` (referrer + authored link texts). `NoteRealmService` already uses it. Read rows in tests with `AuthoredNoteReferenceRowTestSupport.rowsFor(EntityManager, Note)` — do not open a new MockMvc context for the package-private repo.
- **Delete policies:** done (Slice 19) — `removeNoteLinksFromReferrerProperties` now reads referrer + link text from `AuthoredNoteReferenceInboundFacade.distinctInboundReferencesForViewer` alone; no more `ResolvedWikiLinkRepository` dependency in `NoteReferenceHandling`/`NoteService`. The now-unused `AuthoredNoteReferenceInboundFacade.distinctReferrerIdsForViewer` was removed too.
- **Notebook walks still in a write transaction:** alias-changing content save; cross-notebook move (`refreshCardinalityAcrossMovedNotebooks`). Plan 038 already skipped title/create; Slice 18 stopped `NoteService.destroy` / `restore`.
- **Frontend barrier (Slice 6, keep):** `noteContentMutationBarrier` flushes body autosave before delete. Do not extend it (no title autosave, no retries).
- **Property index:** `note_property_index.authored_note_reference_id` replaces `target_note_id` (Slice 21). Planner uses `sourceLocalKey`; service links to source-owned rows; backfill rebuilds property index after authored-reference backfill. `refreshForNote` breaks FK links before flush when `replaceContent` leaves new authored-reference children transient.
- **Still on the cache:** remaining `refreshForNote` / `refreshNotebookScope` callers. Focus-context sampling moved off the cache in Slice 20.

## Constraints for remaining work

- Controller `@Transactional` is the write command. Persist the changed aggregates and return. Do not start a notebook-wide derived-index rebuild in that transaction.
- Do not catch or retry lock timeouts (ADR 0006). Do not add `REQUIRES_NEW`, after-commit listeners, or lock retries.
- Keep `resolved_wiki_link` until the last consumer moves (Slice 23). Do not dual-write new resolved destinations. Do not revert the 038 title/create skip.
- `note_property_index` may point to an authored-reference row; it must not store its own resolved target note (Slices 21–22).
- Candidate lookup is an optimization, not a resolution verdict. Always live-resolve for the current viewer.
- Do not change Markdown/wiki syntax, candidate matching, visibility, or API response shapes. Do not add a new ADR.

## Done (1–20)

| # | Type | Capability |
|---|---|---|
| 1 | Structure | Domain `NoteReferenceResolution` + source-local key |
| 2 | Behavior | Outgoing links live-resolve from authored Markdown |
| 3 | Structure | `authored_note_reference` + `Note.replaceContent` (`V300000313`) |
| 4 | Behavior | Content save indexes references |
| 5 | Behavior | Create/extract same index |
| 6 | Behavior | Frontend same-note body autosave barrier (keep; do not extend) |
| 7 | Behavior | Relationship reduction writes through `replaceContent` |
| 8 | Behavior | Property-removal rewrite through the same helper |
| 9 | Behavior | Identity rewrites through `replaceContent` (`documentFromRewrittenContent`) |
| 10 | Behavior | Startup backfill (`V300000314`) |
| 11 | Structure | Inbound candidate facade |
| 12 | Behavior | NoteRealm inbound via facade |
| 13 | Behavior | Ambiguous inbound excluded live |
| 14 | Behavior | Title-rename guard/rewrite via facade |
| 15 | Behavior | Delete referrer ids via facade; cache still supplies link text |
| 16 | Behavior | Relocation via facade; facade now returns authored link texts |
| 17 | Behavior | Injected notes index authored references like product saves |
| 18 | Behavior | Delete/restore stop calling `refreshNotebookScope` |
| 19 | Behavior | Property-removal deletion rewrites from live authored link text |
| 20 | Behavior | Focus context samples only currently resolved inbound references |
| 21 | Structure | Property index points to authored references, not cached targets |

## Remaining slices

### 22. Assimilation gates on current reference resolution

- **Type:** Behavior
- **Status:** pending
- **Scenario:** A property pointing at another note blocks assimilation only while that authored reference currently resolves for the learner to a target whose required tracker is incomplete; rename, ambiguity, deletion, property removal, and viewer visibility take effect without rebuilding the property's stored target.
- Drive `AssimilationController` with real notes, trackers, and database rows.
- Replace JPQL that joins `NotePropertyIndex.targetNote` with candidate retrieval plus domain resolution at the assimilation boundary. Preserve exact-key dedupe and result ordering.
- **Verify:** full backend tests.

### 23. Retire resolved-link state and notebook-wide refreshes

- **Type:** Behavior
- **Status:** pending
- **Scenario:** Title, alias, creation, deletion/restoration, and cross-notebook location changes immediately affect outgoing, inbound, rewrite, focus-context, and assimilation results while touching no unrelated source-note reference rows.
- Add one controller-level regression that changes target identity while retaining an unrelated source entry and observes the current result. Existing scenario tests supply the other deltas; do not duplicate them.
- Remove every `refreshNotebookScope` / `refreshCardinalityAcrossMovedNotebooks` caller and implementation, then delete `ResolvedWikiLink`, its repository/refresh/inbound services, and the old table with a new Flyway migration.
- Rename the surviving facade and DTO comments from “resolved wiki link” to the domain term “note reference”. Make its raw repository internal so future consumers cannot reintroduce persisted wiki destinations.
- Regenerate the ERD. Update or remove the temporary diagnosis note when execution is fully shipped; keep ADR 0004 as the only permanent documentation change.
- **Verify:** `rg 'ResolvedWikiLink|resolved_wiki_link|refreshNotebookScope|refreshCardinalityAcrossMovedNotebooks|targetNote' backend/src/main backend/src/test` has no obsolete reference-cache usage, then run `backend:verify` and the full backend tests.

## Execution rules

- Execute slices in order. A Structure slice may be committed only with its named immediate Behavior slice ready to follow.
- Each slice begins with the failing scenario or compile-time migration pressure it is intended to satisfy. If a slice exceeds about ten focused minutes, stop, split it by consumer or migration boundary, and update this plan before continuing.
- After each slice: apply Jidoka, run `post-change-refactor` over the implicated concept, run the relevant full package suite, update this status, commit atomically, and push as required by `execute-plan`.
- Schema slices also run `backend:verify` and the `database-erd` skill. Slices 17–19 verify against the existing `note_deletion.feature` (Slice 17 makes it green; 18 and 19 must not regress it). Do not add duplicate deletion scenarios.
