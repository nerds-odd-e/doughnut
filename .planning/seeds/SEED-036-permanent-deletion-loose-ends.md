---
id: SEED-036
status: dormant
planted: 2026-09-18
planted_during: execution retrospective of permanent deletion of trashed content, owner grouped the remaining follow-ups
predecessor: SEED-034 story 1 and plan 146, recoverable at commit 8b8b83e962
trigger_when: after SEED-035, in second place in the product backlog
scope: small
---

# SEED-036: Loose ends left by permanent deletion of trashed content

## Why This Matters

Delivering permanent deletion of trashed notes and folders left four follow-ups
that were deliberately out of that story's scope. The owner grouped them into one
story, knowing they are different kinds of work, so that they are carried
together rather than lost.

Only item (a) is visible to a notebook owner. Items (b) to (d) are maintenance of
Donut's own consistency. Whoever plans this should order the work accordingly and
may stop after (a) if priorities change.

## Alternatives and Decision

Each item could be its own queue entry. The owner chose one grouped story to keep
the queue readable, accepting that the items do not share a single beneficiary.

## Story Decomposition

<a id="story-1"></a>

### 1. Close the loose ends left by permanent deletion of trashed content

- **Goal / beneficiary:** A notebook owner stops meeting a broken Undo after
  permanently deleting a note (item a); Donut's maintainers get one wording, one
  refusal and three oversized files resolved (items b to d).
- **Evaluable outcome:** each of the four items below is resolved in code, or
  explicitly declined by the owner with the reason recorded here.
- **Scope:**

  **(a) Undo after permanent deletion — the only user-visible item.**
  Trashing a note records an undo entry through
  `StoredApiCollection.trashNote` and `noteEditingHistory`. Permanently deleting
  that note records none, by design, but does not clear the earlier trash entry.
  Pressing Undo afterwards therefore acts on a note that no longer exists and
  fails. This was explicitly excluded from the permanent-deletion story, but it
  became **newly reachable** by that story: before it, the interface offered no
  way to permanently delete a note at all.

  **(b) One wording for the folder Trash offer.**
  In `frontend/src/composables/folderRemovalOffer.ts`, `trashDescription` and
  `trashConfirmation` state the same consequences in two different wordings
  ("Its contents leave active use" versus "Its complete subtree will leave active
  use"; "until the folder is recovered with Move" versus "until you recover the
  folder with Move"). The permanent-delete offer beside them composes both texts
  from one subject and one consequence. Unifying the Trash pair is a small change
  but alters copy a user reads on a pre-existing flow, so it needs the owner's
  decision on the surviving wording.

  **(c) One home and one status for "Folder not in notebook."**
  Copies remain in `FolderMoveRelocation`, `FolderConstructionService` and
  `NoteConstructionService`. `NoteConstructionService` answers **400** where the
  others answer **404** for the same words, which is a real inconsistency rather
  than only duplication. The permanent-deletion story collapsed the four copies
  that lived inside `FolderRelocationService` into one
  `requireFolderInNotebook`, but deliberately did not cross into the construction
  services.

  **(d) Files over the 250-line rule.**
  All three predate the permanent-deletion story and were left untouched by it:
  `frontend/src/store/StoredApiCollection.ts` (613),
  `backend/src/main/java/com/odde/donut/controllers/NotebookController.java`
  (545), and
  `frontend/src/components/notes/widgets/NoteMoreOptionsActions.vue` (272).
  `StoredApiCollection` implements the broad `StoredApi` interface and shares
  state across roughly twenty methods, so splitting it is the largest of the
  three.

- **Boundary assumptions:** Item (a) is about the undo entry left behind, not
  about making permanent deletion undoable — permanent deletion is irreversible
  by definition in [ADR 0001](../../docs/adrs/0001-ubiquitous-language.md).
  Items (c) and (d) are behavior-preserving unless the owner decides (c)'s status
  code should change, which would be a behavior change needing its own decision.
- **Key examples:**
  - Trash a note, then permanently delete it, then press Undo: nothing fails and
    nothing claims to restore a note that no longer exists.
  - A folder's Settings tab states the consequences of Trash once, in one
    wording, in both the description and the confirmation.
- **Open decisions:** (b)'s surviving wording, and whether (c)'s
  `NoteConstructionService` status should become 404 or the others 400.
