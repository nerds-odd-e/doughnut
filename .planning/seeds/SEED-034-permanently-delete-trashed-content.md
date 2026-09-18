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
- **Value (owner, 2026-09-18):** A consistent interface, since a trashed note
  should not offer Trash again, and peace of mind for people who like to keep
  their trash clean: discarded content leaves Donut together with its learning
  records and other owned dependencies. Removing sensitive content is **not**
  the purpose, because earlier Git history keeps the text.
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
- **Effort hypothesis:** M (1–2 hours), medium confidence. Owner assumption,
  2026-09-18: because deleting on the Git side and publishing already works, the
  web action should be simple, so notes and folders stay one story. The
  refinement evidence below supports this. Split into a note story and a folder
  story only if planning disproves it. Bands follow SEED-033.
- **Refinement evidence, 2026-09-18:**
  - Git publication already performs the same removal: each deleted file
    becomes `NoteService.permanentlyRemove` with dead links left in place
    (`NotebookGitProposalOrdinaryNoteApplication.applyDeletions`); afterwards
    folders no longer present in the tree are removed deepest first, and the
    live data must match the published tree
    (`NotebookGitProposalAcceptance`). Publishing a deletion of several learned
    notes, and of the last note in a folder, is covered by controller tests.
  - Removal order matters. `fk_note_folder` is `ON DELETE SET NULL` and
    `fk_folder_parent` is `ON DELETE CASCADE`, so removing a folder row while it
    still holds notes would leave those notes active at the notebook root. The
    Git path never does this because it removes notes first. The web folder
    action must follow the same order; this is an implementation rule, not
    extra product scope.
  - Web trash already runs inside the accepted-change boundary
    (`NoteTrashService` → `WebNoteEditService.edit`), and `_trash/...` files are
    part of the Portable tree, so in a Git-synchronized notebook a permanent
    deletion appends one accepted commit per NORTH-STAR.
  - A trashed folder already hides its Trash button (`FolderSettings.vue`); a
    trashed note still offers "Trash note (d)".
  - `_trash` is recreated on the next trash
    (`FolderConstructionService.ensureTrashParentFor`), so permanently deleting
    the `_trash` folder itself would behave as emptying the trash without
    special handling.
- **Depends on:** No known product prerequisite.
- **Safe stopping point:** Both note and folder actions remove their selected
  content and complete dependency closure without removing surviving content.
- **Owner decisions, 2026-09-18:**
  - Always confirm before deleting, because the `d` keyboard shortcut would
    otherwise trigger an irreversible action. This settles the UI proposal
    above.
  - Erasing earlier Git history stays deferred. The confirmation says plainly
    that earlier Git history still contains the text, instead of promising
    complete erasure.
  - The `_trash` folder itself may be permanently deleted under the ordinary
    folder rule, with no special handling. A dedicated Empty trash feature stays
    deferred.
  - The story keeps its first position in the queue. The owner states that
    SEED-030 story 2 no longer blocks the next release.
- **Plan:** [146](../quick/146-permanently-delete-trashed-content/PLAN.md).

## Ordering and Scope Reduction

Queue at the beginning by explicit owner request. A concurrently added first
item may precede or follow this one; preserve both and all unrelated queue order.
Keep the near-future direction unchanged. This is one selected story with an
executable plan; the plan alone does not authorize implementing it.

## When to Surface

Now for refinement; execution planning follows when requested.

## Breadcrumbs

- Owner request, 2026-09-18: turn the delete/trash button into permanent delete
  for trashed notes and folders; cascade dependencies and all folder contents.
- Existing note and folder trash flows already ask for confirmation.
- Existing `NoteService.permanentlyRemove` removes a note and its dependent data
  and is used by Git publication; this is a reuse lead, not a prescribed design.
- Existing trash membership follows the notebook-root `_trash` folder.
