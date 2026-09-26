---
id: SEED-030
status: dormant
planted: 2026-09-18
planted_during: Portable-path representation repair after a publication NullPointerException
trigger_when: a notebook owner cannot publish, or a publication reorganizes folders while placing content
scope: M
---

# SEED-030: Folder ancestry during publication

> Both planned stories are delivered. Their plans, diagnoses and evidence are
> recoverable from commit `92af81eadab9b509ffcdf5d1f0c52e51516c3aaf`
> (plan `142-live-folder-ancestry-materialization`) and
> commit `feac68c8b7befc601392278b6154408cfc274724`
> (plan `145-reset-notebook-git-history`).

## Why This Matters

A notebook owner publishing from a local checkout expects the publication to
succeed. Production held legacy rows whose folder ancestry crossed notebooks:
a folder under a parent of another notebook, or a note in a folder of another
notebook. Publication crashed on them while export silently dropped them, so
those rows were never in any accepted Git tree, clone or export.

`V300000333__notebook_follows_folder_containment.sql` repairs the rows by
containment: every folder takes its root ancestor's notebook and every note
takes its folder's. It is ungated and applies with the next release.

A notebook that gains repaired rows holds live content its accepted Git tree
lacks, so publication answers 409 projection drift until its history is reset.
Resetting a notebook's Git history from the notebook settings is the recovery
path, and it is allowed in any state, drifted or not.

## Production rows the repair moves

Read-only queries, 2026-09-18 07:27 UTC. Ids only; this repository is public.

| Row notebook | Container notebook | Stray notes | Stray folders | Notes inside those folders | Owners |
|---|---|---|---|---|---|
| 12 | 4 | 2 | 2 | 3 | circle 2 → user 1 |
| 4 | 26 | 2 | 1 | 1 | user 1 → circle 2 |
| 86 | 191 | 1 | 1 | 2 | user 1 → user 1 |
| 56 | 309 | 1 | 0 | 0 | user 3 → user 3 |

All seven notebooks have a Git binding. The owner accepted the visibility
change for pairs 12→4 and 4→26.

## Open acceptance work

- Once the release carrying the reset button reaches production, the owner
  resets notebooks 4, 26 and 191 from the notebook settings. Notebook 309 is
  its own owner's to reset. No data is lost: the reset snapshots the entire
  current notebook, including web edits made meanwhile.
- `NotebookFollowsFolderContainmentMigrationTest` is migration-only. Remove it
  after production has applied `V300000333`.

## Deferred, owner direction, not planned

- Reconciling an existing clone that has unpublished commits with the new
  initial commit. Git can replay those commits onto the new root.
- When a Portable-format change drifts every notebook at once, refusing to
  clone a drifted notebook with a message that says to reset its Git history
  first. The owner expects resets to be needed more often than is known today,
  because information not yet in the Portable format may have to enter it
  later, which raises this item's value.
- Any admin reset of another user's notebook.

## Known residue, not planned

- Nothing enforces that a folder's parent and a note's folder share the
  notebook, and `PortableTreeSnapshot` still drops rows unreachable from the
  notebook root without a signal. No current writer is known to break the rule.
  Composite foreign keys clash with `fk_note_folder ... ON DELETE SET NULL` and
  with the folder-by-folder cross-notebook move.
- The folder rows on the state passed into
  `NotebookGitProposalDocumentApplication.apply` are unused input, since `apply`
  re-reads them. Dropping them changes `LockedNotebookState`'s shape.
- The Proposed ADR 0002 says v1 rejects deletion or rewind of accepted `main`,
  and the backlog's near-future direction calls Git history append-only. The
  owner chose the reset knowingly as an exception and left both texts as they
  are; whoever accepts ADR 0002 decides whether to record the exception or
  change the rule.
- `CUTOVER_COMMIT_MESSAGE` ("Cutover: snapshot existing notebook content into
  Git") is now inaccurate at its only remaining call site, notebook creation,
  where there is no existing content, and "cutover" in
  `NotebookGitCutoverService`'s name no longer describes a class that also
  owns history reset. No test asserts either commit message.

## When to Surface

When a publication reorganizes folders while placing content, when a notebook
owner cannot publish, or when a Portable-format change drifts notebooks widely
enough to need the deferred clone-refusal message.
