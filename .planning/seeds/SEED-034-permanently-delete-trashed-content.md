---
id: SEED-034
status: dormant
planted: 2026-09-18
planted_during: owner request to capture and refine permanent deletion from trash
trigger_when: now, at the head of the product backlog
scope: medium
---

# SEED-034: Permanently delete trashed notes and folders

## Why This Matters

Notebook owners who have already discarded content need to remove it permanently
from Donut. Offering Trash again on an already trashed note or folder does not
express that outcome. The existing action should become permanent deletion,
including dependent data and, for folders, the complete contained subtree.

## Alternatives and Decision

The owner requested a state-dependent action in the existing note and folder
controls. Keeping content in trash does not deliver permanent removal. A separate
bulk trash-management screen is unnecessary for this selected outcome.

## Story Decomposition

<a id="story-1"></a>

### 1. Permanently delete trashed notes and folders

- **Goal / beneficiary:** A notebook owner can permanently remove a discarded
  note or folder and its dependent data through its existing delete/trash action.
- **Value:** Finish discarding unwanted content without leaving its learning
  records or other owned dependencies behind.
- **Scope:**
  - When a note or folder is in trash, its existing delete/trash action becomes
    **Permanently delete**, clearly identifying the irreversible operation.
    Trash membership includes placement beneath a trashed ancestor folder.
  - Activating that action permanently removes the selected note and all of its
    dependent data. It cannot be recovered through Donut's trash recovery flow.
  - For a folder, remove the folder, its README/content, all nested folders and
    notes, and the dependencies of every removed note. Empty folders are covered
    by the same rule.
  - Preserve existing ownership/authorization requirements. Active content keeps
    its existing trash behavior.
- **Boundary assumptions:** Cascading means removing data owned by the deleted
  content, including learning records, questions, conversations, and images;
  it does not mean deleting other notes merely because they link to it. Preserve
  authored links in surviving notes as unresolved references, consistent with
  the existing folder-trash and permanent-removal capability. Confirm this
  interpretation if the owner intends a different meaning of dependencies.
- **Key examples:**
  - A trashed note has learning history and a conversation. Its action reads
    Permanently delete; completing the action removes the note and its dependent
    records, and the note can no longer be opened or restored from trash.
  - A trashed folder contains a README, a note, and a nested folder containing
    another note. Permanently deleting it removes the entire subtree and each
    note's dependent data; sibling folders and their notes remain.
  - A note is in trash because its containing folder was trashed. Its action
    becomes Permanently delete too; deleting that note leaves other content in
    the containing folder intact.
  - An empty trashed folder can be permanently deleted.
  - A surviving note links to the deleted content. The surviving note and its
    authored text remain; the link no longer resolves to the deleted content.
  - A note or folder outside trash still offers the existing trash operation.
- **UI proposal:** Keep the existing confirmation step, naming the selected
  item and warning that dependent data and all folder contents are permanently
  deleted and cannot be restored. Cancelling leaves everything unchanged.
  Confirmation versus immediate deletion is awaiting the owner's preference.
- **Deferred promises:** No bulk Empty trash feature, retention timer, new CLI
  delete command, or erasure of historical Git commits, backups, or external
  copies. Existing notebook publication/history contracts still apply.
- **Effort hypothesis:** M (1–2 hours), low confidence; assumes reuse of existing
  permanent note removal. Reassess during planning once folder deletion and
  complete dependent-data coverage are understood. Bands follow SEED-033.
- **Depends on:** No known product prerequisite.
- **Safe stopping point:** Both note and folder actions remove their selected
  content and complete dependency closure without removing surviving content.
- **Open decisions:** Confirmation versus immediate deletion; the UI proposal
  above is not yet a human decision.

## Ordering and Scope Reduction

Queue at the beginning by explicit owner request. A concurrently added first
item may precede or follow this one; preserve both and all unrelated queue order.
Keep the near-future direction unchanged. This is one selected story, without
an executable plan or authorization to implement it.

## When to Surface

Now for refinement; execution planning follows when requested.

## Breadcrumbs

- Owner request, 2026-09-18: turn the delete/trash button into permanent delete
  for trashed notes and folders; cascade dependencies and all folder contents.
- Existing note and folder trash flows already ask for confirmation.
- Existing `NoteService.permanentlyRemove` removes a note and its dependent data
  and is used by Git publication; this is a reuse lead, not a prescribed design.
- Existing trash membership follows the notebook-root `_trash` folder.
