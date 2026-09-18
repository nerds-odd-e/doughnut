---
id: SEED-035
status: dormant
planted: 2026-09-18
planted_during: execution retrospective of SEED-034 story 1 (plan 146), owner judged the gap a problem
trigger_when: now, at the head of the product backlog
scope: medium
---

# SEED-035: Web folder rename and dissolve keep the accepted Git tree in step

## Why This Matters

A notebook owner who edits a Git-backed notebook in Obsidian or an AI IDE relies
on Donut's accepted Portable tree describing the same notebook the database
describes. Folder move, trash and permanent deletion all lock the notebook, apply
the whole domain operation and append one accepted commit. Folder **rename** and
**dissolve** change the same Portable paths but never reach that owner, so after
either action the accepted tree can describe a notebook that no longer exists.

## Alternatives and Decision

The operations already have a consistency owner and a shared seam; the question
is whether rename and dissolve join it. Giving either its own snapshot path
would contradict the single-consistency-owner direction. Leaving the gap in place
is the alternative the owner rejected when this was raised.

## Evidence

Gathered while reviewing plan 146; all of it predates that plan.

- In `backend/src/main/java/com/odde/donut/services/FolderRelocationService.java`,
  `acceptedWebChangeService.apply` is reached from exactly one place: the private
  `applyLiveFolderChange` helper. Its three callers are
  `moveFolderWithinNotebook`, `trashFolderWithinNotebook` and
  `permanentlyDeleteFolderWithinNotebook`.
- `renameFolder` and `dissolveFolder` return without ever reaching
  `acceptedWebChangeService`.
- Both change Portable paths. Rename changes a folder's own path and therefore
  the path of everything beneath it. Dissolve reparents the folder's subfolders
  and notes to its parent and removes the folder.
- There is no `NotebookGitWeb*` accepted-commit test class for web folder rename
  or dissolve. The `NotebookGit*Rename*` classes cover proposal and publication
  renames, which are a different path.

## Open question for the owner

[NORTH-STAR, One complete accepted web change](../NORTH-STAR.md#one-complete-accepted-web-change)
names note editing, ordinary note movement and same-notebook folder movement as
the operations sharing the accepted-change boundary. Rename and dissolve are
absent from that list. Whether that omission is deliberate scope — with the
resulting divergence covered by the existing per-notebook projection-drift
policy — or an unintended gap has not been decided. Settle that before planning
slices; the answer changes whether this is a defect to repair or a direction to
extend.

## Story Decomposition

<a id="story-1"></a>

### 1. Web folder rename and dissolve keep the accepted Git tree in step

- **Goal / beneficiary:** A notebook owner with a Git-backed notebook can rename
  or dissolve a folder on the web and still find the notebook's accepted Portable
  tree describing the same notebook the database describes.
- **Value:** Obsidian and AI-IDE round-tripping depends on the accepted tree
  being trustworthy after every web action. An owner cannot tell from the UI that
  two of the five folder operations leave it behind.
- **Scope:**
  - Web folder rename and web folder dissolve apply their complete domain
    operation and append one accepted commit in the same transaction, through the
    existing consistency owner rather than a second snapshot path.
  - Wiki-link capture and rewrite, sibling-name conflict resolution and merge
    behavior finish before the final snapshot, as they already do for move.
  - Preserve the existing per-notebook policy for pre-existing projection drift
    and all non-Git behavior.
- **Boundary assumptions:** This is about the web actions only. Proposal and
  publication renames already have their own covered path and are out of scope.
  A notebook with no Git binding continues to run the operation and append
  nothing.
- **Key examples:**
  - A Git-backed notebook has `Biology/Cells.md`. Renaming `Biology` to `Life`
    appends exactly one accepted commit whose tree contains `Life/Cells.md` and
    no `Biology/` path.
  - A Git-backed notebook has `Biology/Nested/Cells.md` and `Biology/Atoms.md`.
    Dissolving `Biology` into the notebook root appends exactly one accepted
    commit whose tree contains `Nested/Cells.md` and `Atoms.md`, with no
    `Biology/` path and no commit per moved note.
  - Dissolving a folder whose child name already exists at the destination, with
    merge requested, still appends exactly one accepted commit describing the
    merged result.
- **Open decisions:** the direction question above.
