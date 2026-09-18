---
id: SEED-030
status: refined
planted: 2026-09-18
planted_during: Portable-path representation repair after a publication NullPointerException
trigger_when: a notebook owner cannot publish, or a publication reorganizes folders while placing content
scope: M
---

# SEED-030: Folder ancestry during publication

> Story 1 is delivered. Its plan, diagnosis and evidence are recoverable from
> commit `92af81eadab9b509ffcdf5d1f0c52e51516c3aaf`
> (`.planning/seeds/SEED-030-folder-ancestry-single-representation.md` and
> `.planning/quick/142-live-folder-ancestry-materialization/PLAN.md`).
> Story 2 is refined and planned in
> [145-reset-notebook-git-history](../quick/145-reset-notebook-git-history/PLAN.md).

## Why This Matters

A notebook owner publishing from a local checkout expects the publication to
succeed. Production held legacy rows whose folder ancestry crossed notebooks:
a folder under a parent of another notebook, or a note in a folder of another
notebook. Publication crashed on them while export silently dropped them, so
those rows were never in any accepted Git tree, clone or export.

`V300000333__notebook_follows_folder_containment.sql` repairs the rows by
containment: every folder takes its root ancestor's notebook and every note
takes its folder's. It is ungated and applies with the next release.

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

## Story Decomposition

### 2. Notebooks that gained repaired content can publish again

**Executable plan:**
[145-reset-notebook-git-history](../quick/145-reset-notebook-git-history/PLAN.md).

After the repair migration, production notebooks 4, 26, 191 and 309 hold live
content their accepted Git trees lack. `requireMatchingAcceptedTree` answers
409 projection drift, and `AcceptedWebChangeService` commits nothing for a
notebook that did not match before a change, so web changes stop reaching Git.
No recovery path exists. A test confirmed the 409 for a gaining notebook.

**Owner decision, 2026-09-18:** recover by resetting, not by appending. The
owner expects resets to be needed more often than is known today, because
information not yet in the Portable format may have to enter it later.

- **Goal:** someone who can edit a notebook can reset its Git history from the
  notebook settings. The accepted history is replaced by one initial commit of
  the entire current notebook, so the notebook can be cloned and published
  again whatever state its history was in.
- **Scope:** one reset operation, its endpoint, and a settings button guarded
  by a warning. Reset is allowed in any state, drifted or not. It reuses the
  snapshot replacement that today exists only for test fixtures.
- **Key examples:**
  - A notebook holds a note its accepted history lacks, and publishing a plain
    edit is refused as projection drift. After a reset the accepted history is
    a single parentless commit whose tree equals the current notebook,
    including that note, and the same kind of edit publishes on top of it.
  - From the notebook settings the owner presses "Reset Git history", reads a
    warning that the history is discarded and existing clones must be cloned
    again, and confirms. A fresh `donut notebook clone` then contains the whole
    notebook in one commit. Cancelling the warning changes nothing.
  - Any member of the circle that owns a notebook can reset it.
- **Rejection constraint:** a user who cannot edit the notebook is refused and
  the accepted history is unchanged.
- **Already true, reused:** a clone made before the reset no longer shares
  history. `donut notebook pull` already answers that the checkout does not
  share Git history and says to clone again
  (`cli/tests/notebookPull.localCandidate.suite.ts`). No CLI change.
- **Deferred, owner direction, not in scope:**
  - Reconciling an existing clone that has unpublished commits with the new
    initial commit. Git can replay those commits onto the new root.
  - When a Portable-format change drifts every notebook at once, refusing to
    clone a drifted notebook with a message that says to reset its Git history
    first.
  - Any admin reset of another user's notebook. Notebook 309's owner resets it.
- **Stated exception:** the Proposed ADR 0002 says v1 rejects deletion or
  rewind of accepted `main`, and the backlog direction calls history
  append-only. The owner chose this reset knowingly and left that text as is.
- **Refinement evidence, 2026-09-18:**
  - `NotebookGitCutoverService.resnapshotForTestability` already replaces a
    binding with a fresh parentless snapshot. It reads the binding without the
    writer lock that download and publish take.
  - The settings page already has a confirm-then-call pattern: "reset index".
  - Nothing reports drift today, so whether other production notebooks are
    already drifted is unknown.
- **Effort hypothesis:** S.
- **Release gate, owner decision 2026-09-18:** the release that carries
  `V300000333__notebook_follows_folder_containment.sql` waits until this story
  is completed. After that release the owner resets notebooks 4, 26 and 191.
- **Carried obligation:** `NotebookFollowsFolderContainmentMigrationTest` is
  migration-only. Remove it after production has applied `V300000333`.

## Known residue, not planned

- Nothing enforces that a folder's parent and a note's folder share the
  notebook, and `PortableTreeSnapshot` still drops rows unreachable from the
  notebook root without a signal. No current writer is known to break the rule.
  Composite foreign keys clash with `fk_note_folder ... ON DELETE SET NULL` and
  with the folder-by-folder cross-notebook move.
- The folder rows on the state passed into
  `NotebookGitProposalDocumentApplication.apply` are unused input, since `apply`
  re-reads them. Dropping them changes `LockedNotebookState`'s shape.

## When to Surface

Before the next application release.
