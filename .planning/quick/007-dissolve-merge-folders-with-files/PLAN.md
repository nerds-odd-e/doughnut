# Dissolve and merge folders that contain files

Status: **awaiting story refinement — not ready for slice-plan refinement or execution**.
Work item: **SEED-035#story-11**.
Source: [mapped story](../../seeds/SEED-035-ai-workspace-supporting-files.md#story-11).
Depends on delivered nested attachment continuity. Its deleted story and plan
history are recoverable from commit
`653f5ce5a7365a4296c3206dcdf554bdccd28fb0`; no implementation or completed
evidence is carried into this plan.

## Mapped outcome and boundaries

An owner can dissolve or merge a folder that contains files on the web: the
files move with the notes, and a filename clash refuses the whole operation.
This replaces the current temporary dissolve/merge refusal.

It adds no cross-notebook behavior (SEED-035#story-10), web file controls,
renaming on clash, or reference rewriting.

## Resume requirement

Run `dough-story-refinement` on story 11 to settle its goal, scope and key
examples — especially the merge arrangements (dissolve with merge, move with
merge) and what the refusal message lists. Realign this plan to that
understanding before `dough-slice-plan-refinement` or execution. The inputs
below are **provisional mapped inputs**, not dispatchable leaves; their earlier
readiness does not carry across the split.

## Architecture carried forward

Follow the delivered model: a file refers to its folder the way a note does,
and `FolderSubtree.dissolveInto` / `mergeInto` own rehoming. Add one rehome rule
used by both, which refuses an occupied destination path before any mutation.
No second attachment representation and no per-operation file copier.

Owner decision (2026-09-21): on a filename clash, refuse the whole dissolve or
merge, name the clashing path, and change nothing.

## Provisional mapped inputs

### 1. Dissolving a folder moves its files up
Mapped from the 14-slice draft's slice 4. Given `physics/old/sketch.png` and no
`physics/sketch.png`, dissolving `old` yields `physics/sketch.png`, same bytes,
and no `old`. Dissolving a top-level folder places its files at the root.
Proof location proposed: `NotebookGitFolderDissolveControllerTest`.

### 2. Merging folders brings the source folder's files along
Mapped from slice 5. Given `physics/diagrams/a.png` and
`physics/old/diagrams/b.png`, dissolving `old` with merge yields both files in
`physics/diagrams/`. The same rehome rule as input 1.

### 3. A filename clash refuses the whole dissolve or merge
Mapped from slice 6. Different pictures at `physics/force.png` and
`physics/old/force.png`: dissolving `old` is refused, naming `physics/force.png`;
folders, files and the accepted head are unchanged. Likewise through a merge.
One rule, not one per arrangement. Proof location proposed:
`NotebookGitFolderDissolveGuardControllerTest`.

### 4. Remove the temporary dissolve/merge refusal
New from the split. The refusal and its two guard cases go away in the same
change that makes input 1 work, never before; the cross-notebook refusal stays
until SEED-035#story-10. Decide during refinement whether this is its own leaf
or part of input 1.

## Evidence, priority and safety

The completed predecessor's "Resplit mapping" table at commit
`653f5ce5a7365a4296c3206dcdf554bdccd28fb0` accounts for every slice of the
earlier 14-slice draft. No product tests were run and no slices are done. Queued
after the attachment journeys and before the cross-notebook move: the refusal
it replaces loses nothing and a local workaround exists.
