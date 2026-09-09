# Publish the initial notebook README with one README-only folder

Source: [SEED-016 Story 1](../../seeds/SEED-016-initial-notebook-and-folder-readmes.md#story-1).
Status: done.

## Goal and scope

From an empty accepted notebook and Portable tree, accept one direct-child
commit adding exactly regular-file `README.md` and one root
`Folder/README.md`, both valid nonblank `type: Readme` Markdown. Store both
authored Readmes, create one fresh root Folder, create no ordinary Note, and
accept the exact commit atomically. Sole-folder creation remains unchanged.

## Ordered slices

### 1. Recognize the exact initial two-README shape
Type: Structure
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` (pass), including
controller refusal of the two-path shape before acceptance.

### 2. Accept both authored Readmes as the exact initial tree
Type: Behavior
Status: done
Proof: Controller test
`publishesInitialNotebookAndRootFolderReadmesAsTheExactAuthoredCommit` plus
`CURSOR_DEV=true nix develop -c pnpm backend:test_only` (pass).

## Learnings

- Creation-shape recognition lives in `NotebookGitProposalFolderCreationShape`;
  relocation stays in `NotebookGitProposalFolderShape`.
- Initial recognition admits only an otherwise empty tree (no unchanged paths).
