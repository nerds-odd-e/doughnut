# Authoritative authored note references

**Status:** in progress
**Source:** `.planning/notes/notebook-scope-wiki-refresh-on-title-and-create.md`  
**Architecture:** ADR 0004, “Links and attachments”  
**Goal:** Make authored semantic note references a domain-owned representation derived from note Markdown, and make every consumer resolve those references against current state without notebook-wide cache refreshes.

## Outcome

When this plan is complete:

- `AuthoredNoteReference` is the one domain type for wiki Portable-path targets and semantic Donut note-ID URLs.
- Stored note Markdown remains authoritative. A source-owned `authored_note_reference` index mirrors every distinct authored reference, including missing and ambiguous wiki targets.
- A wiki reference never stores an authoritative destination note. A note-ID URL stores its authored note ID, not a cached resolution result.
- Resolution has one sealed result (`Resolved`, `Missing`, or `Ambiguous`) computed from the authored reference, source scope, current notebook state, and current viewer.
- Outgoing links, inbound references, rename/delete/move rewriting, focus context, and property assimilation all use one note-reference facade. They cannot query a resolved-link repository directly.
- Every production content mutation updates Markdown and its authored-reference children through one domain boundary; raw `Note.setContent(String)` is unavailable to production callers.
- A user-initiated note deletion is a same-note mutation barrier: pending and in-flight content autosaves complete successfully before the delete request starts, an autosave failure aborts deletion, and no delayed autosave starts after deletion has begun.
- `resolved_wiki_link`, `ResolvedWikiLink*`, `NotePropertyIndex.targetNote`, and notebook-scope resolution refreshes are gone.

## Design contract

### Domain authority

- Keep and strengthen `algorithms/AuthoredNoteReference` rather than introduce another link model.
- Give each reference a stable source-local key and its parsed locator:
  - wiki: optional authored notebook qualifier, note portion, optional encoded property key, and display text;
  - note URL: authored note ID, href, and display text.
- Add a top-level sealed `NoteReferenceResolution` with `Resolved(Note)`, `Missing`, and `Ambiguous`. Remove `WikiLinkResolver.CandidateCardinality` after adapters have migrated.
- Add one resolver entry point accepting `AuthoredNoteReference`, source note, and viewer. Consumers must not switch on stored destination state.

### Aggregate and persistence mapping

- Introduce an `AuthoredNoteDocument` value that carries the validated Markdown plus `AuthoredNoteReferences.uniquePreserveOrder(...)` from the same parse.
- `Note.replaceContent(AuthoredNoteDocument)` changes the Markdown and replaces its source-owned authored-reference children in the same aggregate operation. During migration, keep the raw setter only until every production caller uses this boundary; then remove it.
- Map those children to `authored_note_reference`. Persist authored kind and parsed locator fields needed for candidate lookup, plus document order. Do not map a destination `Note` relation for wiki references. Persist a note-ID URL's numeric ID as authored data even when no live note currently has that ID.
- Keep the repository internal to the note-reference package. Expose domain references and resolutions, never persistence rows, to consumers.

### Mutation ordering

- Coordinate note-body autosave and deletion by note ID at the frontend mutation boundary. Confirming ordinary deletion or relationship reduction must flush and successfully await a pending/in-flight content save before issuing `DELETE /api/notes/{id}/delete`; a save failure leaves the note undeleted and editable.
- Once deletion has entered that barrier, cancel or reject delayed autosave admission for that note so component unmount cannot issue a late `PATCH /api/text_content/{id}/content`.
- Do not catch or retry the observed lock timeouts. Prevent the invalid overlapping user workflow, then remove the legacy derived-index lock footprint through the remaining consumer migrations.

### Query model

- Outgoing queries may read the aggregate's authored references directly, then resolve them live.
- Inbound queries use the derived index only to select possible authored references:
  - note-ID URLs by authored note ID;
  - wiki paths by current target keys (notebook, display name/aliases, and Portable path), with source-notebook fallback for unqualified paths.
- Candidate rows are always resolved again through the domain resolver for the current viewer. Candidate lookup is an optimization, not a resolution verdict.
- `note_property_index` may point to an authored-reference row when a property value contains a semantic reference. It must not store its own resolved target note.

### Migration and operations

- Creating the new table and backfilling it are separate, restart-safe steps. Backfill parses each existing note with the configured canonical Donut origin, in bounded transactions, outside a user write request. Readiness must not report complete until the one-time backfill succeeds.
- Keep `resolved_wiki_link` only while consumers are being moved. Do not dual-write new resolved destinations. Drop it after the last consumer moves.
- Regenerate `docs/database-erd.md` after each final schema shape change, following the `database-erd` skill.

## Non-goals

- Do not change Markdown or wiki-link syntax, candidate matching rules, visibility rules, or API response shapes.
- Do not introduce async eventual resolution or a notebook-wide invalidation job.
- Do not add a new ADR. ADR 0004 now carries the durable constraint; this plan owns implementation detail.
- Do not revert the working quick fix while the old cache exists. The completed design makes that fix obsolete by removing the refresh API and remaining callers.

## Slices

### 1. Promote reference resolution to a domain contract

- **Type:** Structure
- **Status:** done
- **Enables:** Slice 2 only.
- Add `NoteReferenceResolution` and one resolver entry point for both `AuthoredNoteReference` variants.
- Move the dedupe/source-local key onto the authored-reference model so persistence and property indexing cannot invent their own identity rules.
- Keep the existing `WikiLinkResolver` public methods as thin adapters until their consumers migrate; this slice must not change observable resolution.
- Add focused algorithm tests only where the new domain-stable contract is not already exercised through a controller.
- **Verify:** full backend tests.

### 2. Resolve outgoing references from authored Markdown, not cached rows

- **Type:** Behavior
- **Status:** done
- **Scenario:** A note contains `[[Future]]` while no target exists. After a target named `Future` is created, showing the original note returns that resolved wiki link without refreshing or rewriting the original note.
- Add the scenario at `NoteController`'s show boundary. A sibling scenario may assert only the delta when a previously unique target becomes ambiguous.
- Make outgoing note-realm links and graph traversal iterate authoritative authored references and resolve each for the current viewer.
- Stop reading `resolved_wiki_link` for outgoing references; preserve document order, dedupe, ambiguous-link shape, note-ID URL behavior, and authorization.
- **Verify:** full backend tests.

### 3. Add the source-owned authored-reference aggregate mapping

- **Type:** Structure
- **Status:** done
- **Enables:** Slice 4 only.
- Add one Flyway migration above the current migration tip to create `authored_note_reference` and its source/candidate indexes.
- Add `AuthoredNoteDocument`, the JPA child mapping, and `Note.replaceContent(AuthoredNoteDocument)` alongside the temporary raw content setter.
- The row must reconstruct the domain type without a wiki destination relation. Its constraints must make invalid kind/locator combinations fail loudly.
- Keep the table unused by readers until Slice 4; do not alter `resolved_wiki_link` yet.
- **Verify:** `backend:verify`, full backend tests, and regenerated database ERD.

### 4. Content save records every authored reference note-locally

- **Type:** Behavior
- **Status:** done
- **Scenario:** Saving content containing one resolved wiki path, one missing wiki path, one ambiguous wiki path, and one semantic note-ID URL leaves one source-owned entry for every distinct authored reference; changing the content replaces only that source note's entries.
- Exercise the HTTP content-save boundary with realistic notes and the real database.
- Make validation produce `AuthoredNoteDocument`, and persist Markdown plus its child entries through `Note.replaceContent(...)` in the same transaction.
- Keep alias/property/level refresh behavior for the changed note. Do not resolve a destination while projecting the authored-reference index and do not accept a viewer parameter.
- **Verify:** full backend tests.

### 5. Note creation and extraction establish the same invariant

- **Type:** Behavior
- **Status:** done
- **Scenario:** Creating a note with authored references, including creation from an extracted suggestion, persists the same reference entries as a later content save would.
- Drive the existing construction/controller boundaries. Assert only the source-index delta not already covered in Slice 4.
- Route initial content through the same `AuthoredNoteDocument` / `Note.replaceContent(...)` boundary; remove construction-specific reference refresh.
- **Verify:** full backend tests.

### 6. Deletion waits for same-note content autosave

- **Type:** Behavior
- **Status:** done
- **Scenario:** A note has a pending or in-flight body autosave. When the user confirms either ordinary deletion or relationship reduction, the content save succeeds before the delete request starts, and no content patch for that note starts after deletion begins; if saving fails, deletion does not start.
- Drive the mounted note-page composition with deferred `updateNoteContent` and `deleteNote` responses; assert request ordering rather than reproducing a timing-dependent database timeout. Cover relationship reduction as the primary observed case and ordinary deletion only as the shared-barrier delta.
- Add one note-ID-scoped mutation barrier shared by note-body autosave and `useNoteDeleteFlow`. It must flush queued debounce work, await the in-flight persist chain, close admission for delayed/unmount saves, and leave other notes independent.
- Preserve the current confirmation choices, blocking progress message, successful navigation, draft error behavior, and ordinary autosave behavior when no deletion occurs. If deletion itself fails, reopen mutation admission so the still-mounted note remains editable.
- **Verify:** focused frontend autosave/delete tests, then the full frontend unit suite.

### 7. Relationship reduction updates the source aggregate atomically

- **Type:** Behavior
- **Status:** done
- **Scenario:** After reducing a relationship whose target is an authored note reference, showing the source immediately contains the new property and returns that target as the source's outgoing reference, while the relationship note is deleted.
- Extend the existing delete-controller relationship scenario with real authored-reference rows, asserting only the source-reference delta beyond the established reduction behavior.
- Route `NoteReferenceHandling.reduceRelationNoteToSourceProperty` through `AuthoredNoteContent` and `Note.replaceContent(...)`; do not use raw `setContent` followed by a cascading `merge`. Keep the legacy property/resolution refresh only while its readers still require it, and preserve timestamp, tracker-rehoming, and orphan-image behavior.
- **Verify:** full backend tests.

### 8. Property-removal deletion updates every referrer aggregate

- **Type:** Behavior
- **Status:** done
- **Scenario:** After deleting a target with “remove from properties”, showing an affected referrer no longer contains or returns that property reference, while an unrelated authored reference remains current.
- Extend the existing delete-controller property-removal scenario with complete FK/reference fixtures and source-owned row assertions.
- Route the content rewrite in `NoteReferenceHandling.removeNoteLinksFromReferrerProperties` through the aggregate boundary. Preserve the current resolved-row candidate selection until Slice 15 changes resolution authority, plus timestamps and orphan-image cleanup.
- **Verify:** full backend tests.

### 9. Identity-preserving rewrites maintain authored references

- **Type:** Behavior
- **Status:** done
- **Scenario:** Renaming or relocating a referenced note rewrites the affected Markdown and authored-reference entries together, without changing the established visible rewrite result.
- Migrate `WikiLinkRewriteSupport` and any remaining production content mutation to `AuthoredNoteDocument` / `Note.replaceContent(...)`.
- Remove unused `Note.prependContent`. When the last production caller is migrated, remove the raw String content setter; keep fixture-only hydration explicit and outside production paths.
- Preserve timestamps, refresh behavior still needed by old readers, and orphan-image behavior.
- **Verify:** full backend tests and `rg '\.setContent\(' backend/src/main/java/com/odde/donut` shows no production note-content write outside explicit fixture hydration.

### 10. Backfill existing notes before indexed reads are enabled

- **Type:** Behavior
- **Status:** done
- **Scenario:** Starting the upgraded application with pre-existing notes produces authored-reference entries for resolved, missing, ambiguous, and semantic URL references before the application becomes ready; a restart is a no-op.
- Add a one-time, restart-safe backfill that uses the configured canonical origin and bounded transactions. Record completion separately from reference rows so notes with zero references are not repeatedly scanned.
- A failed batch must fail readiness loudly and be safely resumable; do not perform this scan inside an HTTP write transaction.
- Test the backfill through its application/migration boundary with real database rows, including a zero-reference note.
- **Verify:** full backend tests and `backend:verify`.

### 11. Introduce indexed inbound candidate selection behind the facade

- **Type:** Structure
- **Status:** done
- **Enables:** Slice 12 only.
- Add internal repository queries that select possible authored references for a target by authored note ID or current wiki destination keys.
- Add the inbound facade that live-resolves candidates for the viewer and returns distinct referrer notes or authored references in deterministic order.
- Keep existing inbound consumers on `ResolvedWikiLinkService` until Slice 12. No consumer may receive persistence rows from the new facade.
- **Verify:** full backend tests.

### 12. A newly created target immediately gains current inbound references

- **Type:** Behavior
- **Status:** done
- **Scenario:** A source note authors `[[Future]]` while it is missing. Creating `Future` and showing it immediately lists the source as an inbound reference, without refreshing the source note.
- Add the scenario at the note-show boundary and migrate `NoteRealmService` inbound references to the new facade.
- Live-resolve each indexed candidate for the current viewer; preserve visibility, dedupe, and note-ID ordering.
- **Verify:** full backend tests.

### 13. Inbound references disappear when current resolution becomes ambiguous

- **Type:** Behavior
- **Status:** pending
- **Scenario:** A source's `[[Target]]` resolves uniquely and appears inbound; creating a namesake makes it ambiguous and removes it from both targets' inbound lists without changing the source entry.
- Add only the ambiguity delta at the note-show boundary. The production path should already be the facade from Slice 12; fix any candidate-key or validation gap revealed by the test.
- Include a viewer-visibility variant only if current readable-candidate behavior is not already covered by existing tests.
- **Verify:** full backend tests.

### 14. Rename decisions and preservation use current authored references

- **Type:** Behavior
- **Status:** pending
- **Scenario:** Title rename requires a reference-handling choice exactly when a currently resolved authored reference points at the target; choosing preservation rewrites that authored wiki reference and leaves semantic note-ID URLs unchanged.
- Migrate the inbound guard and `WikiLinkRewriteService` title-rename path to the facade's live-resolved authored references.
- Keep all existing display-text, frontmatter, property-selector, alias, and folder-path rewrite cases passing; replace assertions on cache rows with assertions on the authored-reference aggregate.
- **Verify:** full backend tests.

### 15. Delete policies use current authored property references

- **Type:** Behavior
- **Status:** pending
- **Scenario:** Removing a target from referrer properties edits only property values whose authored references currently resolve to that target; missing, ambiguous, unauthorized, and note-ID URL cases retain their defined behavior.
- Migrate `NoteReferenceHandling` from resolved rows to the facade and keep source/referrer content updates inside the aggregate boundaries established by Slices 7-8.
- Preserve memory-tracker rehoming and orphan-image cleanup.
- **Verify:** full backend tests.

### 16. Relocation rewriting uses current authored references

- **Type:** Behavior
- **Status:** pending
- **Scenario:** Moving a note or folder rewrites the currently resolved inbound/outgoing wiki paths needed to preserve identity, without rewriting already ambiguous paths or semantic note-ID URLs.
- Migrate note move, folder move/reparent/rename/dissolve, and cross-notebook rewrite candidate discovery to the note-reference facade.
- Remove direct `ResolvedWikiLinkRepository` dependencies from relocation and rewrite code.
- **Verify:** full backend tests.

### 17. Focus context samples only currently resolved inbound references

- **Type:** Behavior
- **Status:** pending
- **Scenario:** When an indexed candidate has become ambiguous, missing, deleted, or unreadable, focus-context retrieval excludes it and continues through ordered/seeded candidates until it fills the requested cap or exhausts valid candidates.
- Migrate both focus-context inbound entry points and BFS expansion to the facade.
- Preserve exclusion sets, deterministic seeded ordering, dedupe, and visibility. Candidate limiting must not treat an invalid candidate as consuming the result cap.
- **Verify:** full backend tests.

### 18. Property tracking points to authored references, not cached targets

- **Type:** Structure
- **Status:** pending
- **Enables:** Slice 19 only.
- Add a Flyway migration replacing `note_property_index.target_note_id` with a nullable authored-reference relation for the semantic reference selected from that property value.
- Rebuild the derived property index from note Markdown after the authored-reference backfill; do not translate old target pointers into new authority.
- Make `NotePropertyIndexPlanner` identify the domain reference key while `NotePropertyIndexService` links to the source-owned entry. Remove viewer-blind `resolveAnyTargetWikiLinkToken` from indexing.
- **Verify:** `backend:verify`, full backend tests, and regenerated database ERD.

### 19. Assimilation gates on current reference resolution

- **Type:** Behavior
- **Status:** pending
- **Scenario:** A property pointing at another note blocks assimilation only while that authored reference currently resolves for the learner to a target whose required tracker is incomplete; rename, ambiguity, deletion, property removal, and viewer visibility take effect without rebuilding the property's stored target.
- Drive `AssimilationController` with real notes, trackers, and database rows.
- Replace JPQL that joins `NotePropertyIndex.targetNote` with candidate retrieval plus domain resolution at the assimilation boundary. Preserve exact-key dedupe and result ordering.
- **Verify:** full backend tests.

### 20. Retire resolved-link state and notebook-wide refreshes

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
- After each slice: apply Jidoka, run `post-change-refactor` over the implicated concept, run the relevant full package suite (frontend for Slice 6; backend for backend slices), update this status, commit atomically, and push as required by `execute-plan`.
- Schema slices also run `backend:verify` and the `database-erd` skill. Do not add E2E coverage unless an API observable changes; the expected change is backend semantics with a stable response shape.

## Key decisions

- Amend Accepted ADR 0004 instead of creating a new ADR.
- Treat Markdown as authority and the authored-reference table as a rebuildable source-owned index.
- Persist authored addresses, never a wiki resolution verdict.
- Use live resolution after indexed candidate selection rather than async cache invalidation.
- Enforce index maintenance through the `Note` content aggregate boundary, not documentation or caller convention.
- Treat deletion as a frontend same-note mutation barrier; prevent autosave/delete overlap instead of retrying database lock failures.
- Retain the quick fix until the old refresh mechanism is deleted; no revert is needed during this plan.

## Discoveries

- `AuthoredNoteReference` and `AuthoredNoteReferences` already distinguish wiki Portable paths from semantic note-ID URLs and are the correct seed for the domain model.
- `WikiLinkResolver.CandidateCardinality` already models the needed tri-state, but being nested under a wiki-specific service makes it easy for other consumers to bypass.
- `resolved_wiki_link` contains only successful resolutions, so it cannot discover a missing/ambiguous reference that becomes resolved after target identity changes.
- Note show partly reclassifies stored rows live, but it cannot synthesize a newly resolved link when no row exists.
- `NotePropertyIndex.targetNote` is a second independently resolved destination cache and currently resolves without a viewer.
- Production content writes occur in `TextContentController`, `NoteConstructionService`, `NoteReferenceHandling`, and `WikiLinkRewriteSupport`; the public raw setter allows future paths to omit derived-index maintenance.
- The quick fix in `76f0482bdbeb61564facb9638369b507cd22cead` removed title/create notebook refreshes and has been confirmed in production. Other alias/delete/restore/move callers still perform the same notebook walk.
- Slice 1: `WikiLinkResolver.resolveReference(AuthoredNoteReference, sourceNote, viewer)` is now the one entry point returning `NoteReferenceResolution` for both wiki and note-ID-URL variants; it duplicates `ResolvedWikiLinkService.authorizedNoteIdUrlTarget`'s note-ID-URL authorization logic by design until Slice 2 migrates outgoing resolution onto it. `AuthoredNoteReference.sourceLocalKey()` now owns dedupe identity. `WikiLinkResolver` was split to extract `WikiLinkCandidateClassifier` (notebook/title candidate matching) to stay under the file-size limit; `CandidateCardinality` still lives on `WikiLinkResolver` pending removal after adapters migrate.
- Slice 2: `ResolvedWikiLinkService.wikiLinksForViewer` (outgoing) now parses the note's live content into authored references and resolves each via `resolveReference`, no longer reading `resolved_wiki_link` rows at all for outgoing; the old duplicated authorization helpers and `AuthoredNoteReferences.fromStoredAuthoredLink` were deleted as dead code. `outgoingWikiLinkTargetNotesForViewer` and `FocusContextWikiBfsExpander` graph traversal needed no changes — they were already built on `wikiLinksForViewer`. `resolved_wiki_link` rows/repository/refresh machinery remain in place for inbound and other not-yet-migrated consumers.
- Slice 3: new `authored_note_reference` table (migration `V300000313`) with a `kind` discriminator, wiki locator columns (`wiki_notebook_qualifier`, `wiki_note_portion`, `wiki_encoded_property_key`), note-ID-url columns (`note_id_url_note_id` — plain int, no FK — and `note_id_url_href`), `document_order`, and a `chk_authored_note_reference_kind_locator` CHECK constraint enforcing exactly one locator group is populated per row (verified by direct-INSERT testing, not just SQL inspection). Persistence entity is `AuthoredNoteReferenceRow` (distinct from the domain `algorithms.AuthoredNoteReference` sealed type it round-trips via `toDomainReference()`); its Spring Data repository `AuthoredNoteReferenceRowRepository` is package-private (kept internal, per the design contract). `AuthoredNoteDocument` (validated Markdown + parsed references from the same parse) lives in `algorithms`. `Note.replaceContent(AuthoredNoteDocument)` uses a JPA `orphanRemoval` collection (Note has no injected repository/EntityManager, unlike the `@Service`-based refresh classes) alongside the still-present raw `setContent`. The table has no readers yet by design — wiring starts at Slice 4.
- Slice 4: `AuthoredNoteContent.prepareDocumentForSave(content, canonicalOrigin)` delegates to the existing `prepareContentForSave` (validation) and wraps its result through `NoteConceptType.ensureStoredType(...)` before parsing into an `AuthoredNoteDocument` — pure parse, no resolution, no viewer. `TextContentController.updateNoteContent` now saves via `note.replaceContent(document)` and gained a direct `CanonicalDonutOrigin` constructor dependency (no cleaner existing path was found — other collaborators reach it only through a package-private accessor on `WikiLinkResolver`). Old alias/property/level refresh (`resolvedWikiLinkService.refreshForNote`/`refreshNotebookScope`) is untouched and still runs alongside the new persistence. Title-rename, note creation, and rewrite paths still write content the old way; they are covered by Slices 5 and 7-9.
- Slice 4/5 test placement (do not copy the first attempt): asserting `authored_note_reference` rows via `ControllerTestBase` + `@AutoConfigureMockMvc` + the package-private repository created a unique Spring context and exhausted CI MySQL (`Too many connections`) while local UT stayed green. Keep the repository package-private. Drive the controller as sibling tests do; read rows with `AuthoredNoteReferenceRowTestSupport.rowsFor(EntityManager, Note)`. Content-save coverage lives on `TextContentControllerTestBase`; creation/extraction coverage is folded into `NotebookNoteCreateControllerTest` and `AiControllerCreateExtractedNoteTest`. Later slices that need the index must reuse that helper, not a new MockMvc context.
- Slice 5: `NoteConstructionService.persistNoteContent(Note, String)` is the single choke point for both note-creation paths (root-note creation and creation from an extracted suggestion, including the extracted note's rewrite of the original note's content); routing it through `AuthoredNoteContent.prepareDocumentForSave(...)` + `Note.replaceContent(...)` (same shape as Slice 4's `TextContentController` change) covered both without any separate "reference index refresh" to remove — none existed on the construction path (`resolvedWikiLinkService.refreshForNote` handles alias/level/property indexing, a distinct concern, and was left untouched). Relationship reduction, property-removal deletion, and path rewrites still write content through the raw setter; Slices 7-9 migrate those paths before backfill and indexed reads.
- Slice 6: `noteContentMutationBarrier` owns note-ID-scoped autosave admission. Deletion closes admission, flushes and awaits the registered content autosave, aborts when saving fails, and reopens only when deletion leaves the note in the client cache. The mounted note-page scenarios cover relationship reduction ordering, the ordinary-delete shared-barrier delta, delete-failure recovery, and save-failure abortion; the full frontend suite remained green.
- Slice 7: `NoteReferenceHandling.reduceRelationNoteToSourceProperty` now prepares the rewritten source Markdown through `AuthoredNoteContent` and applies it with `Note.replaceContent(...)` before the existing refresh/cleanup work. The delete-controller relationship scenario verifies both the source-owned authored-reference row and the immediately resolved outgoing target; the full backend suite remained green.
- Slice 8: already satisfied by Slice 7's implementation — `reduceRelationNoteToSourceProperty` and `removeNoteLinksFromReferrerProperties` share one private `persistReplacedAuthoredContent` helper that routes both through `AuthoredNoteContent.prepareDocumentForSave(...)` + `Note.replaceContent(...)`, preserving timestamps, orphan-image cleanup, and the legacy resolved-row refresh. `NoteControllerDeleteTests.shouldRemoveDeletedNoteLinksFromReferrerPropertiesOnly` already carries the extended fixtures (an unrelated authored reference alongside the removed one) and asserts source-owned rows via `AuthoredNoteReferenceRowTestSupport.rowsFor(...)`. No code change was needed; confirmed via full backend suite green.
- Slice 9: `WikiLinkRewriteSupport`'s two rewrite call sites (inbound referrer rewrite, outgoing notebook-move rewrite) now write through `Note.replaceContent(...)`, but via a dedicated private `documentFromRewrittenContent(content, canonicalDonutOrigin)` helper rather than `AuthoredNoteContent.prepareDocumentForSave(...)` — the latter also runs `NoteConceptType.ensureStoredType`, which injects a frontmatter block into previously frontmatter-less content and broke the established rewrite output; `documentFromRewrittenContent` parses authored references only. `applyOutgoingNotebookMoveRewrite` gained a `CanonicalDonutOrigin` parameter to support this. `NoteConstructionService` gained a shared private `applyContent(Note, String)` helper (added during post-change-refactor to remove triplicated prepare+replace pairs across `createNote`, `persistNoteContent`, and the new `prependAndPersistWikidataDescription`); `WikidataIdWithApi.extractWikidataInfoToNote`/`associateNoteToWikidata` were replaced by a pure `fetchWikidataDescription(): Optional<String>`, with the note mutation now living solely in `NoteConstructionService`. `Note.prependContent` is removed. The raw `Note` content setter itself was deliberately **not** removed in this slice: `rg '\.setContent\(' backend/src/main/java/com/odde/donut` is clean of production writes (only `SwaggerConfig`'s unrelated `ApiResponse.setContent` and `NotesTestData`'s documented fixture hydration remain), but the setter is still used directly by ~30 test files' arrangement code and `NoteBuilder`; removing it is deferred to whichever later slice migrates or accepts touching that fixture surface.
- Slice 10: new migration `V300000314` adds a single-row `authored_note_reference_backfill_progress` table (`last_processed_note_id` watermark, `completed_at`), tracked separately from `authored_note_reference` rows so a zero-reference note is not rescanned on every restart. `AuthoredNoteReferenceBackfillTx.processNextBatch` is `REQUIRES_NEW`-transactional per batch (paged via `NoteRepository.findByIdGreaterThanAndDeletedAtIsNullOrderByIdAsc`), so a mid-run failure only loses the in-flight batch and resumes from the last committed watermark; it reuses `AuthoredNoteReferences`/`CanonicalDonutOrigin` for parsing and now calls the (widened-to-public during refactor) `AuthoredNoteReferenceRow.forSource` shared factory directly instead of duplicating its kind/locator switch, still bypassing `Note.replaceContent`'s full aggregate-save bookkeeping by design. `AuthoredNoteReferenceBackfillStartup` (`@Profile("!test")`, `@EventListener(ApplicationReadyEvent.class)`, `@Order(1)`) loops batches to completion at startup, ordered after `FlyWayFreeVersionRealMigration` (`@Order(0)`, unchanged fail-loudly-on-`ApplicationReadyEvent` precedent reused as the readiness-blocking mechanism — no Actuator/other mechanism existed in the codebase). ERD regenerated to include the new table.
- Slice 11: added candidate-selection queries to (still package-private) `AuthoredNoteReferenceRowRepository` — `findNoteIdUrlCandidatesForTarget` and a coarse notebook-scoped `findWikiCandidatesForNotebookScope` (SQL narrows by notebook with source-notebook fallback; exact title/alias/Portable-path matching happens in Java) — plus a new public `AuthoredNoteReferenceInboundFacade` (`@Service`, in `entities.repositories` specifically because it must sit in the same package as the package-private repository it depends on — verified as a genuine compile-time constraint, not mere convention, during refactor). Its `distinctReferrerNotesForViewer(Note target, User viewer)` reuses `PathShapedTarget`/`FrontmatterAliases` matching primitives in reverse (candidate row → target identity, vs. the existing forward token → candidate notes direction in `WikiLinkNoteCandidates`), live-resolves every candidate via `WikiLinkResolver.resolveReference(...)`, and returns distinct referrer notes ordered by note id ascending — mirroring `ResolvedWikiLinkService.referrerNotesForViewer`'s existing shape/order. Nothing consumes this facade yet; `ResolvedWikiLinkService` and its inbound consumers are untouched, wiring is Slice 12's job. Known gap carried into Slice 12: the facade does not yet replicate the old path's separate referrer-notebook-visibility filter (`InboundResolvedWikiLinkService.inboundReferrerVisible`), only the resolver's own target-side viewer-readability check.
- Slice 12: `NoteRealmService` now calls `AuthoredNoteReferenceInboundFacade.distinctReferrerNotesForViewer(...)` for inbound references instead of `ResolvedWikiLinkService.referencesNotesForViewer`. The facade gained a `referrerVisibleToViewer` filter closing two gaps: the referrer's own notebook-visibility (not just the target's, mirroring the old `inboundReferrerVisible`) and soft-deleted-referrer exclusion (a second gap found while migrating, not originally flagged). Because that made `ResolvedWikiLinkService.referencesNotesForViewer`/`inboundReferrerNotesForViewer` and `InboundResolvedWikiLinks`'s `referrerNotesForViewer`/`distinctReferrersFromTargetRows`/`inboundReferrerVisible` unreachable (verified against Slices 13-20's actual target methods first), post-change-refactor deleted them rather than leave dead code; `InboundResolvedWikiLinks` shrank to just its two still-live methods (`hasRowsFromNonDeletedReferrers`, `sampledReferencesNotesForFocusContext`, used by later slices). Outgoing `wikiLinksForViewer` is untouched and still on `ResolvedWikiLinkService`. Full backend suite re-verified green after the refactor's cascaded deletions.
- Exception diagnosis (2026-09-01): both production failures concern note `21860`: the pasted report is from `PATCH /api/text_content/21860/content` and the attached report is from `DELETE /api/notes/21860/delete`. Before Slice 6, opening/confirming deletion blurred the editor and flushed its debounce, but `useDebouncedTextAutosave` and `useNoteDeleteFlow` had no shared awaitable barrier, so the UI permitted those requests to overlap.
- The content request's observed wait is in `EntityPersister.save`/Hibernate merge while its cascade inserts replacement `authored_note_reference` children. The relationship-reduction delete request's observed wait is in `ResolvedWikiLinkService.refreshForNote` while it wholesale deletes `note_property_index` after rewriting the source note; the delete transaction later performs notebook-scope refresh work as well. The reports do not include timestamps or an InnoDB lock graph, so the exact row-level wait cycle cannot be reconstructed, but the two wait sites and the missing frontend ordering explain the repeatable contention without suggesting malformed authored-reference data.
- The completed slices did not create the autosave/delete race. Slice 4 made `authored_note_reference` insertion part of content save, so that table is now one visible participant. Slice 6 prevents the invalid request overlap; Slices 7-9 make all affected rewrites maintain the aggregate; Slices 15, 18, and 20 remove the legacy resolved/property destination writes and notebook-wide refresh lock footprint.
- Per ADR 0006, the plan prevents the race and removes obsolete lock-taking work. It deliberately does not hide the defect with lock-timeout retries or exception catches.
