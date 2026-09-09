# Publish the initial notebook README with one README-only folder

Source: [SEED-016 Story 1](../../seeds/SEED-016-initial-notebook-and-folder-readmes.md#story-1).
Status: in progress (slice 1 done).

## Goal and scope

From an empty accepted notebook and Portable tree, accept one direct-child
commit adding exactly regular-file `README.md` and
`New Folder/README.md`, both valid nonblank `type: Readme` Markdown. Store the
authored notebook Readme, create one fresh root Folder with its authored
Readme, create no ordinary Note, and accept the exact commit atomically.

Exclude every third path, ordinary notes, additional or nested folders,
non-empty accepted notebooks, existing-container README edits, attachments,
multiple unpublished commits, stale/divergent history, and bulk import.

## Current decisions and evidence

- Keep `NotebookController.publishNotebookGitProposal` as the stable proof
  boundary and the existing publication transaction as the atomicity owner.
- Extend only the existing root-folder-creation route. Sole `Folder/README.md`
  remains unchanged until slice 2 accepts the two-README shape.
- Root-folder creation recognition lives in
  `NotebookGitProposalFolderCreationShape`; relocation stays in
  `NotebookGitProposalFolderShape`.
- Reuse strict typed-Markdown, authored-content, folder-name, sibling-collision,
  Portable-projection, ancestry, ownership, and exact bundle acceptance gates.
- Accepted ADR 0001 / ADR 0004 terminology and Portable README representations;
  no ADR change.

## Outside-in proof

At the controller boundary, start with an empty matching Git-backed notebook.
Publish one direct-child proposal adding only root `README.md` and
`Field Notes/README.md`. Observe the authored notebook Readme, one fresh root
Folder named `Field Notes` with its authored Readme, zero ordinary Notes, and
accepted/downloaded head and tree exactly equal to the proposal.

## Ordered slices

### 1. Recognize the exact initial two-README shape
Type: Structure
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` (pass), including
controller refusal for empty tree adding only `README.md` + `Folder/README.md`
as reserved folder README.

### 2. Accept both authored Readmes as the exact initial tree
Type: Behavior
Status: planned
Proof: A controller test observes both stored Readmes, one root Folder, zero
ordinary Notes, and exact accepted/downloaded proposal head and tree. Then run
`CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

Behavior: An owner of an empty matching notebook publishes one direct-child
commit adding only the notebook Readme and one root folder Readme → Donut stores
both Readmes, creates that Folder, and atomically accepts the exact authored
commit.

Use the existing folder-creation acceptance transaction. Validate both blobs
as nonblank `type: Readme`, apply the root Readme to the Notebook and the folder
Readme to one freshly constructed root Folder, refresh the projection, and
require exact proposed-tree equality before the shared binding write. Keep the
sole-folder case working and do not admit any third path or non-empty baseline.
Consume `InitialNotebookAndRootFolderCreation` instead of the temporary reserved
refusal.

## Contract-to-proof map

| Story promise | Owning proof |
| --- | --- |
| Initial notebook and folder Readmes publish together | Slice 2 controller publication |
| Both authored contents and one fresh root Folder are preserved | Slice 2 notebook/folder observations |
| No ordinary Note is inferred | Slice 2 note-repository observation |
| Exact authored commit is accepted atomically | Slice 2 downloaded head/tree plus existing transaction |
| Third paths and broader initial trees remain excluded | Slice 1 refusal suite and slice 2 full backend suite |

## Learnings

- Creation-shape recognition extracted to `NotebookGitProposalFolderCreationShape`
  so relocation stays cohesive after the initial two-README classifier landed.
- Slice 1 still refuses the recognized two-path candidate with the reserved
  folder-README message until slice 2 wires acceptance.
