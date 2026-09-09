# Require an ordinary Note in the initial three-path publication

Source: [SEED-016 Story 2](../../seeds/SEED-016-initial-notebook-and-folder-readmes.md#story-2)
and the execution retrospective of completed
[quick/081](../081-initial-folder-with-one-note/PLAN.md).
Status: done.

## Goal and scope

A notebook owner publishing the exact initial notebook Readme, root Folder
Readme, and one direct-child note gets the promised ordinary-Note-only
acceptance boundary. Require the third document to carry `type: Note`;
refuse a non-Note type without changing notebook content or the Git binding.
Preserve the valid three-path publication and the two smaller delivered shapes.

## Ordered slices

### 1. Refuse a non-Note third document atomically
Type: Behavior
Status: done

Proof: `NotebookGitProposalFolderCreationControllerTest` refuses third-path
`Relationship`, `Readme`, and `CustomType` with a contextual 400 naming path
and type; notebook Readme, binding, Folder, and Note stay unchanged.
`CURSOR_DEV=true nix develop -c pnpm backend:test_only` stayed green for that
controller class (worktree-isolated).

Learnings: Eligibility lives in package-private `NotebookGitProposalTypedPath`
(`requireOrdinaryNote` / `requireReadme`); Folder acceptance delegates. Shared
note-addition seam and publisher transaction ownership unchanged.
