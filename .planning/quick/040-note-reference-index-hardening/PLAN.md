# Note reference index hardening

**Status:** in progress  
**Architecture:** ADR 0001 Wiki link, ADR 0004 OKF Markdown  
**Reviews the range:** `93ac5738f3..HEAD` (the 24 commits that shipped `.planning/quick/039-authoritative-authored-note-references/PLAN.md`, plus mixed-in test-isolation fixes)

**Goal:** Close the correctness, cost, and cohesion gaps that the authored-note-reference migration left behind, so the live-resolution design holds up on real notebooks and carries no cache-era leftovers.

## Findings this plan acts on

| # | Kind | Finding | Slice |
|---|---|---|---|
| 1 | Bug | Startup backfill `persist`s rows without clearing, so a note saved through `Note.replaceContent` during the backfill window (or any re-run of a batch) gets duplicate `authored_note_reference` rows. No `UNIQUE` constraint catches it. | 1 |
| 2 | Digression | Inbound discovery reads **every** wiki reference row in the notebook and live-resolves each one (a DB query per row). It runs on every `showNote` via `NoteRealmService.build`, replacing what `resolved_wiki_link` did with one destination-indexed lookup. | 2, 3 |
| 3 | Dead code | `WikiLinkCandidateClassifier.resolveAnyTarget` has zero callers anywhere. | 4 |
| 4 | Dead code | `WikiLinkResolver.resolveWikiLinksForCache` + the `WikiLinkResolution` record have no production caller; the 180-line `WikiLinkResolverYamlAndBodyIntegrationTest` exists only to exercise them. | 5 |
| 5 | Dead code | `WikiLinkRewriteSupport.applyInboundReferrerRewrite` takes a `User viewer` it never reads; both call sites thread it through. | 6 |
| 6 | Smell | `AuthoredNoteReferences.uniquePreserveOrder(inOccurrenceOrder(content, origin))` is spelled out in 6 production places — shotgun surgery on "the authored references of this content". | 7 |
| 7 | Coverage | Deleting `ResolvedWikiLinkTitleResolutionTest` / `ResolvedWikiLinkServiceTest` dropped the guards for case-insensitive title/notebook resolution and for kana spellings that collide under the `utf8mb4_0900_ai_ci` collation. Nothing replaced them. | 8 |
| 8 | Redundant test | `AuthoredNoteReferenceRowRepositoryTest` re-proves, at the aggregate, exactly what `TextContentControllerAuthoredReferencePersistenceTest` proves through the controller. 3 of 5 `AuthoredNoteReferenceInboundFacadeTest` scenarios duplicate `NoteControllerShowTests` / `NoteRealmServiceTest`. | 9 |
| 9 | Improvement | Planning end-state is not clean: `STATE.md` still says 039 is mid-flight, links a deleted note, and names the wrong Flyway tip; `.planning/codebase/CONCERNS.md` still describes `ResolvedWikiLinkService`. | 10 |
| 1 (end) | Structure | One-time backfill (`authored_note_reference_backfill_progress`, startup listener, `AuthoredNoteReferenceBackfillTx`) is spent once production has completed it. | 11 (gated) |
| 10 | Smell | 6 of 8 `WikiLinkRewriteService` methods are verbatim pass-throughs that only add `@Transactional`. | 12 (optional) |

## Decisions

1. **ADR 0004** — keep the current Accepted wording (`b3dc500f7c` is the human amendment). No slice touches `docs/adrs/`.
2. **Backfill** — this plan runs until the one-time repair is gone. Slice 1 makes it idempotent so the first production deploy cannot duplicate rows. Slice 11 drops the machinery. **Mandatory stop before slice 11** — do not start it until production (and any other long-lived DB) shows `authored_note_reference_backfill_progress.completed_at` set. V315 left `note_property_index.authored_note_reference_id` null until `refreshForNote`; removing the backfill before that runs leaves assimilation half-migrated.

---

### 1. Backfill writes authored references through the note aggregate

- **Type:** Behavior
- **Status:** done

The backfill now routes stored content through `Note.replaceContent`, replacing any rows already indexed by a live save instead of appending duplicates. The committed-transaction regression reproduced four rows before the fix and confirms two afterward; the full backend suite passes. No database constraint was added.

### 2. Inbound candidate rows are selected by the target's addressable keys

- **Type:** Structure
- **Status:** done

`findWikiCandidatesForNotebookScope` now selects notebook-scoped rows by the target's normalized title/alias keys or a path suffix ending in `Title` / `Title.md`. The redundant Java reverse matcher was removed; every candidate still goes through `WikiLinkResolver.resolveReference`, so the query remains an optimization rather than a resolution verdict. The full backend suite and focused inbound/rewrite/delete/focus-context coverage pass unchanged.

### 3. Title rename asks whether the note is referenced, not for every referrer

- **Type:** Structure
- **Status:** done

Title rename now asks `NoteReferenceService.isReferencedForViewer`, which short-circuits on the first visible candidate that live-resolves to the target instead of hydrating every referrer. The shared inbound predicate preserves viewer-scoped behavior; the full backend suite passes without controller-test changes.

### 4. Retire `resolveAnyTarget`

- **Type:** Structure
- **Status:** done

Deleted `WikiLinkCandidateClassifier.resolveAnyTarget` and its private-only parsing/matching chain and import. No callers remain; the full backend suite and focused reference-resolution test pass.

### 5. Retire the cache-era outgoing resolution API

- **Type:** Structure
- **Status:** done

Deleted `resolveWikiLinksForCache`, `WikiLinkResolution`, and cache-era wording. The duplicate 180-line integration suite became one focused `WikiLinkResolverFrontmatterAndBodyResolutionTest` against `resolveReference`, retaining the unique YAML/frontmatter-versus-body reach. The full backend suite passes.

### 6. Drop the unread viewer from inbound referrer rewriting

- **Type:** Structure
- **Status:** done

Removed the unread `viewer` parameter from `applyInboundReferrerRewrite` and the inbound-only forwarding call chain. Capture and outgoing classification retain `viewer` where authorization is real. The full backend suite passes.

### 7. One factory for the authored references of a note's content

- **Type:** Structure
- **Status:** done

`AuthoredNoteDocument.fromContent` now owns parsing and ordered deduplication for all five production paths without validating or normalizing. `AuthoredNoteContent.prepareDocumentForSave` still owns user-save validation/type normalization, and the record component is now accurately named `content`. The full backend suite passes.

### 8. Wiki links resolve by the collation's title identity

- **Type:** Behavior
- **Status:** done

Regression coverage for behavior that exists but lost its tests when the `ResolvedWikiLink*` suites were deleted. Both assertions live at the `showNote` boundary, in `NoteControllerShowWikiLinkTests`: `shouldResolveWikiLinkTitleIgnoringCase` and `shouldResolveHiraganaAndKatakanaTitlesToTheirDistinctNotes`. The kana test was hand-verified to fail for the right reason (swapped expected destination order) before being reverted to the correct assertion. No production code changed; the full test class passes.

### 9. Remove authored-reference tests that re-prove the controller

- **Type:** Structure
- **Status:** done

- Deleted `AuthoredNoteReferenceRowRepositoryTest`. Both of its cases were already asserted through `TextContentControllerAuthoredReferencePersistenceTest`.
- Trimmed `AuthoredNoteReferenceInboundFacadeTest` to three tests: referrer ordering, excluding a candidate row that resolves to a different note, and the alias-match case. **Deviation from plan:** the alias case was kept, not dropped — it is the only coverage in the repo for the alias-key branch of `findWikiCandidatesForNotebookScope`; `NoteRealmServiceTest`/`NoteControllerShowWikiLinkTests` only exercise alias resolution for outgoing links, never inbound. A short inline comment on the test explains why it stays.
- Dropped the duplicate facade-recheck tail block from two tests in `TextContentControllerUpdateNoteTitleInboundWikiReferencesTests`.

Focused tests green; full backend suite not re-run (not required by wrap-up).

### 10. Planning and codebase map reflect the shipped design

- **Type:** Structure
- **Status:** done

- `.planning/STATE.md`: Flyway tip fixed to `V300000316`; dead links to the deleted `notes/notebook-scope-wiki-refresh-on-title-and-create.md` removed; dangling `quick/037-openai-transaction-boundary-followup` pointer removed (no plan file existed to recreate from); 038 reconciled to done; 040 references updated from "planned" to "in progress".
- `.planning/quick/038-.../PLAN.md`: dead diagnosis link dropped; deploy check now points at the live-resolution design (039's PLAN).
- `.planning/codebase/CONCERNS.md`: derived-index-coherence concern rewritten around `Note.replaceContent` and `NoteReferenceService.refreshDerivedIndexesForNote`; `ResolvedWikiLinkService` paths removed.
- 039's PLAN left untouched. This plan (040) not pruned yet — slice 11 is still ahead, gated on developer confirmation.

### STOP — confirm the one-time backfill finished

**Mandatory. Do not start slice 11 until the developer confirms this.** Execute-plan / any agent must halt here and wait.

Check every long-lived database that has applied V313–V316 (production at minimum):

```sql
SELECT id, last_processed_note_id, completed_at
FROM authored_note_reference_backfill_progress
WHERE id = 1;
```

`completed_at` must be non-null. If it is still null, leave the backfill in place — either let startup finish, or investigate — and do not drop anything. Fresh installs that already completed are fine; the risk is an environment that has the schema but not the row fill.

### 11. Drop the one-time authored-reference backfill

- **Type:** Structure
- **Status:** done

Developer confirmed `authored_note_reference_backfill_progress.completed_at` was set on production before this slice ran.

Deleted `AuthoredNoteReferenceBackfillStartup`, `AuthoredNoteReferenceBackfillTx`, `AuthoredNoteReferenceBackfillProgress`, `AuthoredNoteReferenceBackfillProgressRepository`, and `AuthoredNoteReferenceBackfillTxTest`. Dropped the now-meaningless `@Order(0)` and its comment from `FlyWayFreeVersionRealMigration`. Added `V300000317__drop_authored_note_reference_backfill_progress.sql` and regenerated `docs/database-erd.md`. `migrateTestDB` applies cleanly; full recompile and focused authored-reference tests pass. One comment reference to `AuthoredNoteReferenceBackfillTx` remains inside the immutable `V300000315` migration, left untouched per the never-edit-committed-migrations rule.

### 12. Collapse the wiki-link rewrite pass-through facade (optional)

- **Type:** Structure
- **Status:** planned — take only if slices 1–11 left the concept worth touching

Six `WikiLinkRewriteService` methods forward verbatim to `WikiLinkRelocationRewrite` / `WikiLinkReferenceCapture`, adding only `@Transactional`. Move `@Transactional` onto the collaborators and let `FolderRelocationService`, `FolderMoveRelocation`, and `RelationController` call them directly, keeping title rename (the one method with real orchestration) where it is. Roughly 90 lines of delegation disappear.

## Not planned (considered and rejected)

- **`UNIQUE(source_note_id, document_order)` migration** — slice 1 removes the write path that could violate it; a migration is deploy risk for a hypothesis.
- **Gating traffic until the backfill finishes** — Flyway itself already runs on `ApplicationReadyEvent` in this repo (`FlyWayFreeVersionRealMigration`), so "schema work after the port opens" is the established posture, not something this migration introduced.
- **Indexed `wiki_note_title_key` column on `authored_note_reference`** — slice 2's SQL narrowing should be enough. Reconsider only with a measurement showing the key-matched query is still the bottleneck.
- **Batching resolution in `UnassimilatedPropertyService.isGated`** — the live gate is new correctness the plan did not ask for but should keep; optimize it when assimilation streaming is observed to be slow.
- **Extending the frontend mutation barrier to title edits and property edits** — the plan scoped it to same-note body autosave, which is the race that authored-reference indexing created.
- **`TextContentWrapper` keeping a stale barrier registration** when the user edits note A, navigates to B, and never types. The component only learns a note id from an edit event, so fixing it properly means threading a note id prop; the exposure (deleting A from elsewhere after visiting B) is not worth that change today.
- **Moving `AuthoredNoteReferenceInboundFacade` out of `entities.repositories`** — it sits there so `AuthoredNoteReferenceRowRepository` can stay package-private, which is the stronger encapsulation.

## SLICE PLAN WRITTEN
