# Skip notebook-scope wiki refresh on title save and note create (probe)

**Status:** planned  
**Goal:** Deploy the smallest change that can confirm the production symptom: title save and note create are slow in large notebooks, and a concurrent content save lock-waits.

This is an **interim** probe. If deploy confirms the symptom, a later pass will decide how (or whether) notebook-scope cache rebuild should happen without holding the write transaction.

## Diagnosis (confirmed)

`refreshNotebookScope` re-resolves **every live note** in the notebook inside the same `@Transactional` request that already updated/created a `note` row.

| Write | Calls `refreshNotebookScope`? | Observed |
|-------|-------------------------------|----------|
| Content (no alias change) | no | fast |
| Title change | yes | slow; holds `note` row lock |
| Note create | yes | slow |
| Content while title request still open | — | `Lock wait timeout` on `UPDATE note` |

Show already re-classifies wiki tokens live (`WikiLinkResolver.classifyToken` in `wikiLinksForViewer`), so **note show can still report unique/ambiguous/missing without a fresh cache**.

## Interim decision

Stop calling `refreshNotebookScope` from **title update** and **note create**. Keep `refreshForNote` on create (that note's own outgoing rows + indexes). Leave other callers (alias-changing content, delete, restore, cross-notebook move) unchanged for this probe.

**Accepted gap until a later pass:** `resolved_wiki_link` rows on *other* notes can stay stale (inbound-reference lists, health, focus-context sampling). Note **show** wiki-link resolution should stay correct via live classify.

## Slices

### 1. Title save and note create do not rebuild the notebook wiki cache

- **Type:** Behavior
- **Status:** planned
- **Pre-condition:** A notebook with many notes; at least one note with a unique shorthand `[[Target]]`.
- **Trigger:** User renames a note, or creates a note (including a namesake title in another folder).
- **Post-condition:** The write returns without walking every live note's wiki cache. Opening the referrer still shows the live cardinality (resolved vs ambiguous). A content save of another field on the same note is not blocked by a long title transaction.

**Do:**

- Remove `resolvedWikiLinkService.refreshNotebookScope(...)` from:
  - `TextContentController.updateNoteTitle` (title-changed branch)
  - `NoteConstructionService.createRootNoteWithWikidataService`
  - `NoteConstructionService.createNoteFromExtractedSuggestion`
- Keep `refreshForNote` on create paths.
- Do not change `refreshNotebookScope` itself or other call sites.

**Tests (existing, must stay green):**

- `TextContentControllerUpdateNoteTitleTests.shouldReresolveNotebookShorthandsWhenRenameIntroducesOrRemovesACollision` — show after rename
- `NotebookNoteCreateControllerTest.creatingANamesakeMakesAnExistingUniqueShorthandAmbiguous` — show after create
- Broader wiki-link show ambiguity tests

**Not in this slice:** async/after-commit rebuild, incremental “only notes that mention this title”, client-side serializing title+content saves, alias/delete/move paths.

## Deploy check

After CD: in a large notebook, time a title rename and a new note. Edit body while a title save is in flight — content PATCH should not lock-timeout. If both hold, the diagnosis is confirmed and we plan the durable cache strategy next.
