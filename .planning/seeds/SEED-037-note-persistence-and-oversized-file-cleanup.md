---
id: SEED-037
status: dormant
planted: 2026-09-18
planted_during: execution retrospective of plan 148 (cohesive accepted web folder changes), owner grouped the two flagged observations
predecessor: SEED-035 story 1 and plan 148, recoverable at commit 1639ccd158
trigger_when: top of the product backlog
scope: small
---

# SEED-037: Note-persistence duplication and oversized files left by plan 148

## Why This Matters

Delivering plan 148's accepted-web-change consolidation surfaced two cleanup
items that were out of that story's scope because neither was introduced or
aggravated by its change. The owner grouped them into one story and placed it
at the top of the queue so they are carried together rather than lost.

Both items are maintenance of Donut's own consistency; neither is directly
visible to a notebook owner.

## Alternatives and Decision

Each item could be its own queue entry. The owner chose one grouped story to
keep the queue readable, accepting that the items do not share a single root
cause.

## Story Decomposition

<a id="story-1"></a>

### 1. Clean up note-persistence duplication and oversized files flagged by plan 148

- **Goal / beneficiary:** Donut's maintainers get one representation of the
  save/finalize sequence for AI-extracted notes (item a), and the two files
  plan 148 pushed over or left at this project's 250-line guidance resolved
  or explicitly declined (item b).
- **Evaluable outcome:** each item below is resolved in code, or explicitly
  declined by the owner with the reason recorded here.
- **Scope:**

  **(a) Duplicated save/finalize sequence for AI-extracted notes.**
  `NoteConstructionService.createNoteFromExtractedSuggestion` repeats the same
  `deleteOrphanImagesForPersistedContent` + `refreshDerivedIndexesForNote` pair
  that `AuthoredNoteDocumentPersistence.persist` already bundles alongside its
  own save step, for a different caller shape. Plan 148's slice 4 introduced
  the sibling `NoteConstructionService.finalizeAndRespond` helper for ordinary
  and Wikidata-enriched root-note creation, but `createNoteFromExtractedSuggestion`
  was pre-existing and untouched, so consolidating it was out of that slice's
  scope.

  **(b) Files over the 250-line rule.**
  `backend/src/main/java/com/odde/donut/controllers/NotebookController.java`
  is already tracked as item (d) of [SEED-036](SEED-036-permanent-deletion-loose-ends.md#story-1)
  (545 lines when that story was planted; grew further during plan 148, which
  added and later removed lines across several of its endpoints). Whoever
  picks up this item should resolve `NotebookController.java` together with
  SEED-036's item (d) rather than duplicating that work.
  `backend/src/test/java/com/odde/donut/controllers/NotebookGitBundleControllerTestBase.java`
  is a new item plan 148 flagged: its slice 1 refactor pass consolidated a
  duplicated test helper into this shared fixture base, pushing it to 257
  lines (from 248) because splitting a 62-caller shared fixture base was
  unrelated restructuring outside that slice's concept.

- **Boundary assumptions:** Both items are behavior-preserving refactors; no
  observable outcome should change. Item (b) may reasonably decide
  `NotebookController.java` is large by its coupled-endpoint nature and needs
  a substantive split design decision rather than a small correction — sizing
  should confirm before committing to one story.
- **Key examples:**
  - Saving an AI-extracted note still cleans up orphaned images and refreshes
    derived references, sharing code with ordinary and Wikidata-enriched note
    creation rather than repeating the pair inline.
  - `NotebookGitBundleControllerTestBase.java` and `NotebookController.java`
    are at or under this project's file-size guidance, or the owner has
    explicitly accepted their size.
- **Open decisions:** resolved below.

## Resolution

**(a) Resolved.** `NoteConstructionService.createNoteFromExtractedSuggestion` now
delegates each note's save to `AuthoredNoteDocumentPersistence.persist`, the
same class other callers (`WebNoteEditService`,
`NotebookGitProposalNoteAddition`, `NotebookGitProposalOrdinaryNoteApplication`)
already use, instead of hand-rolling the save-then-cleanup pair. The
save/finalize sequence for AI-extracted notes now has one representation.

**(b) `NotebookController.java` — declined for this story.** It is coupled to
[SEED-036](SEED-036-permanent-deletion-loose-ends.md#story-1) item (d), a
separate, not-yet-taken story that groups this file with two other oversized
files (`StoredApiCollection.ts`, `NoteMoreOptionsActions.vue`) needing one
coherent split design. Splitting it here in isolation would duplicate or
fragment that design decision. It stays tracked only under SEED-036 item (d).

**(b) `NotebookGitBundleControllerTestBase.java` — declined.** Plan 148
deliberately grew it from 248 to 257 lines by consolidating a test helper
duplicated across 62 callers into this one shared base. Splitting it now to
land under 250 lines would recreate the duplication that consolidation just
removed, trading a line-count rule for less cohesion. Accepted as-is.
