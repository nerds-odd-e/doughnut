---
id: SEED-030
status: dormant
planted: 2026-09-18
planted_during: Portable-path representation repair after a publication NullPointerException
trigger_when: a publication reparents a folder while also placing or moving content under it
scope: S
---

# SEED-030: One representation of folder ancestry during publication

## Why This Matters

A notebook owner publishing from a local checkout expects every note and
container in the proposal to land under the folder the proposal shows, even
when the same publication reorganizes folders.

Publication currently holds folder ancestry in two representations: live
`Folder` entities, and flat `ExportFolderRow` snapshot rows loaded once at the
start. Mixing them across a mutation point already produced one defect: a note
moved into a folder that the same publication had just created had no row in
the snapshot, and path resolution dereferenced null. That path was repaired by
deriving a live note's Portable path from the note's own folder ancestry, so
the snapshot can no longer disagree with the note.

One instance of the same mixing remains.
`NotebookGitProposalFolderMaterialization.foldersByPath` builds a map whose
**key** is a path computed from the snapshot rows and whose **value** is the
live `Folder` entity loaded by id. If a folder is reparented earlier in the
same publication — the folder-relocation path — the row-derived key is stale
while the entity it points at is current. `ensureAncestry` then matches a
destination path against ancestry that no longer describes where that folder
sits.

No owner-visible failure has been demonstrated: newly created folders are put
into the map directly, and existing rows are always found by id, so this
cannot reproduce the original crash. The concern is that the arrangement makes
a wrong-destination outcome representable at all, in the one publication path
that changes folder ancestry mid-flight.

## Alternatives and Decision

Recommended direction: give `ensureAncestry` a single representation, so a
destination path is resolved against live folder ancestry rather than against
snapshot rows keyed to live entities. This follows the repair already made for
note paths, where `NotebookGitLivePortablePath` walks the entity's parent
chain.

Strongest simpler alternative: leave the code as it is and rely on the
folder-relocation path loading its rows late enough that the key is never
stale. This is cheaper and may well be correct today, but it keeps the
correctness dependent on load ordering that no test states, which is the
property that produced the original defect.

Assumption to check before selecting: whether any current relocation scenario
actually reparents a folder before `ensureAncestry` runs. If none can, the
value here is preventive rather than corrective, and the story should be
ranked accordingly.

## Story Decomposition

### 1. A publication that reparents a folder still places content correctly under it

- **For / why:** the notebook owner reorganizing folders and moving content in
  one publication, who would otherwise get content attached to a path that no
  longer exists.
- **Evaluation:** a controller-boundary publication test that reparents a
  folder and, in the same proposal, adds or moves content under the relocated
  destination; assert the published Portable tree places that content under
  the new ancestry. Write it first and confirm what the current code does
  before changing anything — an evidenced no-change conclusion is a valid
  outcome.
- **Value / learning:** establishes whether the remaining mixed representation
  is a live defect or only a latent one, which decides whether the structural
  change is worth making.
- **Effort hypothesis:** S — the change mirrors one already made in the same
  subsystem, and `ensureAncestry` has few callers. Confidence is moderate:
  changing its contract to take entities touches
  `NotebookGitProposalFolderMaterialization` and its callers.
- **Depends on:** none.
- **Safe stopping point:** if the test shows current behavior is already
  correct, keep the test as the statement that ancestry must be resolved
  live, and stop. The test alone retains the value.

## Ordering and Scope Reduction

This is preventive work behind any queued owner-facing story. Drop it first if
capacity is short; the evidence that it matters is structural, not a reported
failure. Do not extend it into a general rewrite of `ExportFolderRow`: the
row-based `folderPath` is correct where its callers compare snapshots against
snapshots, and only live-entity-against-snapshot mixing is in scope.

## Open Decisions

Whether a reachable scenario exists in which a folder is reparented before
`ensureAncestry` runs. Story 1's test answers this, and the answer decides
whether the structural change proceeds or the test alone is the deliverable.

## When to Surface

When selecting further Git-backed publication work, especially folder
relocation or subtree composition, or if an owner reports content landing
under an unexpected folder after a publication that also moved folders.
