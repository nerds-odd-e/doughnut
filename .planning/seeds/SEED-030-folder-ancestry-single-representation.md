---
id: SEED-030
status: needs-refinement
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
> Story 2 is an unrefined candidate awaiting an owner decision.

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

Unrefined candidate. After the repair migration, production notebooks 4, 26,
191 and 309 hold live content their accepted Git trees lack.
`requireMatchingAcceptedTree` answers 409 projection drift, and
`AcceptedWebChangeService` commits nothing for a notebook that did not match
before a change, so web changes stop reaching Git. No recovery path exists.
A test confirmed the 409 for a gaining notebook. Notebooks 26, 191 and 309
publish today, so for them this is a regression the release introduces;
notebook 309 belongs to another user. A notebook that only loses rows publishes
normally after the repair.

- **Owner decision needed:** how an accepted head adopts the repaired content.
  Appending one system commit keeps existing clones valid. Rebaselining, as the
  retired 2026-09-17 fleet migration did, abandons history and orphans local
  unpublished commits, including the owner's pending notebook 4 proposal.
  NORTH-STAR currently says drift is not silently adopted, so either choice is
  a stated exception.
- **Open question:** how to select only the notebooks the repair touched, since
  the SQL migration leaves no record of them.
- **Refinement evidence, 2026-09-18:**
  - Appending an accepted commit already exists:
    `AcceptedWebChangeService.commitIfChanged` builds the live snapshot and
    calls `AcceptedSnapshotPersistence.persist`. Adoption is that same step
    without the "matched before the change" gate.
  - At Flyway time only JDBC is available. The retired fleet migration used a
    static JDBC helper, `NotebookGitBaselineRebuild`, deleted with plan 137
    (see commit `301184431f`). A startup migration would need its like again.
  - CLI pull rebases one unpublished commit only when accepted history advanced
    by note saves or note additions into already represented folders. An
    adoption commit that adds folders may fall outside that, so a clone with
    unpublished work may need a fresh clone.
  - Nothing reports drift today, so whether other production notebooks are
    already drifted is unknown.
- **Effort hypothesis:** S to M once decided.
- **Release gate, owner decision 2026-09-18:** the release that carries
  `V300000333__notebook_follows_folder_containment.sql` waits until this story
  is completed.
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
