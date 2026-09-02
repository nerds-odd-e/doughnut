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
- **Status:** planned

`TextContentController.assertReferencedTitleRenameIsUnambiguous` materializes and hydrates every inbound referrer only to call `.isEmpty()`, and it does so before `assertAuthorization`. Replace it with an existence query on the same live-resolution path (`NoteReferenceService`), so the gate expresses "is this note referenced?".

Behavior is unchanged from the API's perspective — `TextContentControllerUpdateNoteTitleTests` and `TextContentControllerUpdateNoteTitleInboundWikiReferencesTests` must stay green with no edits.

Note for the record: the gate became viewer-scoped in this migration (it was viewer-blind against the cache). Keep viewer-scoped — the rewrite it guards was always viewer-scoped, so a rename can no longer be blocked by a referrer the user cannot see or fix.

### 4. Retire `resolveAnyTarget`

- **Type:** Structure
- **Status:** planned

Delete `WikiLinkCandidateClassifier.resolveAnyTarget` and the private chain that exists only for it (`resolveParsedLink`, `uniqueNotebookMatch`, the `Note`-overload of `resolveRef`, the `BiFunction` import). Zero callers in production or tests; nothing else changes.

### 5. Retire the cache-era outgoing resolution API

- **Type:** Structure
- **Status:** planned

`resolveWikiLinksForCache` survived the cache it was named for. Its only remaining exerciser is `WikiLinkResolverYamlAndBodyIntegrationTest`.

- Point that test file at `resolveReference`, and drop the scenarios already covered by `NoteControllerShowWikiLinkTests` (alias resolution, pipe display text, qualified links) and `AuthoredNoteReferencesTest` (parsing). What is worth keeping is the YAML-frontmatter-vs-body reach that no controller test exercises; rename the file to what it then tests.
- Delete `WikiLinkResolver.resolveWikiLinksForCache` and the now-unused `WikiLinkResolution` record.

### 6. Drop the unread viewer from inbound referrer rewriting

- **Type:** Structure
- **Status:** planned

Remove the `User viewer` parameter from `WikiLinkRewriteSupport.applyInboundReferrerRewrite` and from its two call sites (`TitleRenameWikiLinkRewrite`, `WikiLinkRelocationRewrite`). The referrer set was already viewer-filtered at capture time; carrying the viewer into the rewrite implies a second authorization decision that does not happen.

### 7. One factory for the authored references of a note's content

- **Type:** Structure
- **Status:** planned

`uniquePreserveOrder(inOccurrenceOrder(content, origin))` is written out in `AuthoredNoteContent`, `NoteReferenceService`, `WikiLinkRewriteSupport`, `AuthoredNoteReferenceBackfillTx`, and `InjectNotesWorker` (slice 5 removes the sixth copy). Give it one named home — an `AuthoredNoteDocument` factory reads best, since four of the five sites immediately build that record — and route every site through it.

Constraint: the factory must not validate or normalize. `AuthoredNoteContent.prepareDocumentForSave` keeps owning validation and stored-type normalization for user saves; rewrite and backfill paths deliberately skip both.

### 8. Wiki links resolve by the collation's title identity

- **Type:** Behavior
- **Status:** planned

Regression coverage for behavior that exists but lost its tests when the `ResolvedWikiLink*` suites were deleted. Both assertions belong at the `showNote` boundary, in `NoteControllerShowWikiLinkTests`.

- A link whose spelling differs from the target title only by case resolves to it — the `LOWER()` matching in `NoteRepository.findByNotebookNameAndNoteTitleOrderByIdAsc` is currently unpinned.
- `[[ごろ]]` and `[[ゴロ]]` in one note resolve to the `ごろ` note and the `ゴロ` note respectively, not to each other. This is the guard that `utf8mb4_0900_ai_ci` does not fold kana together; it is the highest-value of the deleted assertions because a collation change would silently cross-link notes.

Write these first and watch them pass for the right reason (make one of them fail by hand before trusting it) — they assert existing behavior, so a green-on-arrival test proves nothing on its own.

### 9. Remove authored-reference tests that re-prove the controller

- **Type:** Structure
- **Status:** planned

- Delete `AuthoredNoteReferenceRowRepositoryTest`. Both of its cases (one row per reference in document order, both reference variants reconstructed, previous rows cleared on re-save) are asserted through `TextContentControllerAuthoredReferencePersistenceTest`.
- Trim `AuthoredNoteReferenceInboundFacadeTest` to the two behaviors that need controlled note ids and cannot be reached through a controller: referrer ordering, and excluding a candidate row that resolves to a different note. Its dedupe, alias, and viewer-readability cases are already covered by `NoteRealmServiceTest` and `NoteControllerShowWikiLinkTests`.
- Drop the duplicate facade assertion at the tail of `TextContentControllerUpdateNoteTitleInboundWikiReferencesTests`, which re-checks inbound through the facade after the response already asserted it.

### 10. Planning and codebase map reflect the shipped design

- **Type:** Structure
- **Status:** planned

- `.planning/STATE.md`: keep 040 as the active plan; fix the Flyway tip (`V300000316`, not `V300000300`); remove remaining links to the deleted `notes/notebook-scope-wiki-refresh-on-title-and-create.md`; remove the dangling `quick/037-openai-transaction-boundary-followup` pointer or recreate that plan; reconcile 038 (its own PLAN says done, STATE still lists it as planned).
- `.planning/quick/038-.../PLAN.md`: drop the dead diagnosis link; point the deploy check at the live-resolution design instead.
- `.planning/codebase/CONCERNS.md`: rewrite the derived-index-coherence concern around `Note.replaceContent` and `NoteReferenceService.refreshDerivedIndexesForNote`; delete the `ResolvedWikiLinkService` paths.
- Leave 039's PLAN as the short complete outcome it already is. Do not prune this plan yet — slice 11 is still ahead.

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
- **Status:** planned — blocked on the stop above
- **Gated:** yes

Externally identical once `completed_at` is set: live saves already write `authored_note_reference` through `Note.replaceContent`; the startup scan is a no-op.

- Delete `AuthoredNoteReferenceBackfillStartup`, `AuthoredNoteReferenceBackfillTx`, `AuthoredNoteReferenceBackfillProgress`, its repository, and `AuthoredNoteReferenceBackfillTxTest`.
- Drop `@Order(1)` / backfill comments from `FlyWayFreeVersionRealMigration`.
- New Flyway migration: `DROP TABLE authored_note_reference_backfill_progress`.
- Regenerate `docs/database-erd.md`.
- After this slice, this plan is spent — prune per `planning.mdc`.

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
