# Require an empty notebook for initial Readme publication

Source: [SEED-016 Story 1](../../seeds/SEED-016-initial-notebook-and-folder-readmes.md#story-1), completed quick/079.
Status: done.

## Goal and scope

Keep initial two-Readme publication within its original empty-notebook boundary.
A notebook with existing empty folders must not qualify merely because those
folders have no Portable files. Preserve successful publication from a genuinely
empty notebook and the existing sole-folder publication behavior.

## Ordered slices

### 1. Refuse initial Readmes when the notebook already contains an empty folder
Type: Behavior
Status: done

Proof: `NotebookGitProposalFolderCreationControllerTest` rejects the
existing-empty-folder case without mutating Readme, folders, or accepted
binding; genuinely empty two-Readme and sole-folder success examples remain
green.

Learnings: Eligibility uses locked notebook folder rows before Readme writes;
Portable tree emptiness alone is insufficient (ADR 0004 omits empty folders).
