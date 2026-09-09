# Publish one initial note inside the new README-backed folder

Source: [SEED-016 Story 2](../../seeds/SEED-016-initial-notebook-and-folder-readmes.md#story-2).
Status: done.

## Goal and scope

From a notebook with no folders or live notes and an empty accepted Portable
tree, accept one direct-child commit adding exactly the notebook Readme, one
root Folder Readme, and one ordinary Note directly inside that Folder. Store
all authored content, create one fresh root Folder and one fresh ordinary Note
inside it, and accept the exact commit atomically. Preserve sole-folder and
two-README initial publication. Exclude every fourth path, root notes,
additional notes or folders, nested folders, existing notebook content including
empty folders, relationships, attachments, multiple unpublished commits,
stale/divergent history, and bulk import.

## Ordered slices

### 1. Share ordinary-note addition with initial-tree acceptance
Type: Structure
Status: done

Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` stayed green,
including ordinary-note addition and initial README publication.

Learnings: Addition lives in package-private `NotebookGitProposalNoteAddition`.
`apply` is the reusable entry; destination and document helpers stay on that
class. Publisher owns orchestration and the SERIALIZABLE `REQUIRES_NEW`
transaction.

### 2. Accept the exact three-path initial notebook tree
Type: Behavior
Status: done

Proof: `NotebookGitProposalFolderCreationControllerTest` publishes
`README.md`, `New Folder/README.md`, and `New Folder/First note.md`, then
observes both Readmes, the fresh root Folder, one ordinary Note titled
`First note` inside it, all authored content, and exact accepted/downloaded
head and tree. `CURSOR_DEV=true nix develop -c pnpm backend:test_only` stayed
green, including the two smaller creation shapes.

Learnings: Recognize the three-path tree as its own candidate. Create the
Folder first, refresh Folder rows, then call `NoteAddition.apply` with the
proposal head so destination resolution sees the Folder in both the
projection and the proposed Git tree.
