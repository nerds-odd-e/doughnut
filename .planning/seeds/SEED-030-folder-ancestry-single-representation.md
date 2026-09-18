---
id: SEED-030
status: needs-refinement
planted: 2026-09-18
planted_during: Portable-path representation repair after a publication NullPointerException
trigger_when: a notebook owner cannot publish, or a publication reorganizes folders while placing content
scope: M
---

# SEED-030: Folder ancestry during publication — finish the fix and keep one representation

> **Unrefined.** This seed was assembled at the end of a long session so nothing
> would be lost. Goal and scope below are carried evidence, not an agreed story.
> Refine before planning further. The existing plan
> [142-live-folder-ancestry-materialization](../quick/142-live-folder-ancestry-materialization/PLAN.md)
> covers only the structural half and remains valid on its own terms.

## Why This Matters

A notebook owner publishing from a local checkout expects the publication to
succeed and every note to land under the folder the proposal shows. One owner
(user 788834, notebook 4) hit a `NullPointerException` instead, and **reports
the failure still occurring in production after the fix shipped.**

## What was already done

Two changes shipped to `main` and are in release **v1.3.10**, whose deploy run
completed successfully at 2026-09-18 14:20 (+0800):

- `07e9ee0434` — repaired the reported crash. `applyModificationsAndRenames`
  resolved live-note Portable paths against an `ExportFolderRow` snapshot taken
  before `ensureAncestry` created rename-destination folders, so a note moved
  into a folder created by the same publication had no row and path resolution
  dereferenced null. Regression test:
  `NotebookGitComposedMoveEditControllerTest.publishesMoveIntoNewlyCreatedFolderAlongsideAnotherNoteEdit`,
  which failed with the reporter's exact message and frames before the fix.
- `83a7434798` — removed the cause rather than the symptom. A live note's
  Portable path is now derived from its own folder ancestry
  (`NotebookGitLivePortablePath`) instead of a snapshot index, so the two
  representations can no longer disagree. `requireOneNoteAtPath`, `applyRename`
  and `applyDeletions` each lost their folder-row parameter.

Net across both: −9 production lines. The full `NotebookGit*` controller suite
(98 classes / 318 tests) is green, and CI passed on both commits.

## Diagnosis: notebook 4 holds rows whose folder ancestry crosses notebooks

Investigated 2026-09-18 in a second session. **Reproduced by experiment and
confirmed against production rows** (read-only queries, 07:27 UTC).

**Evidence**

- The deploy script checks the rollout and probes health for the release SHA
  before succeeding (14:28 +0800). Failure report 856 counted its second
  occurrence at 14:31, so the recurrence ran on v1.3.10. Stale jar is ruled out.
- A Failure report only absorbs a recurrence with the same fingerprint:
  exception class, normalized request, and first application frame. The
  recurrence therefore still has `NotebookGitAcceptedTree.folderPath` as its
  first application frame. Only the first occurrence's trace is stored.
- `POST /api/notebooks/{id}/git-bundle` *is* `publishNotebookGitProposal`. The
  request-line "mismatch" recorded earlier was not a mismatch.
- The owner ported notebook 4 to a development notebook through Git and
  published the same change successfully. The cause is therefore in state the
  Portable tree does not carry.
- The original trace has **one** `folderPath` frame, called from
  `portablePath`, inside the `MODIFIED` branch. So a plain note edit met a
  note whose own folder was absent from the notebook's folder rows. Folder
  rows are loaded by `notebook_id` alone, and `fk_note_folder` guarantees the
  folder exists, so that folder has a different `notebook_id`.

**Mechanism.** Every notebook-tree walker assumes a note's folder and a
folder's parent share the notebook. Nothing enforces it: the schema has
single-column foreign keys only. Two walkers treat a violating row differently:

- `PortableTreeSnapshot.build` walks **top-down from the root**. Rows
  unreachable from this notebook's root silently vanish. ZIP export, cutover,
  bundle download and `requireMatchingAcceptedTree` all use it, so they
  succeed and agree with each other, and the stray content never reaches Git,
  the clone, or a ported copy.
- `NotebookGitAcceptedTree.folderPath` walks **bottom-up** through an id→row
  map of this notebook's folders. A foreign ancestor is a missing key and the
  walk dereferences null.

**Experiment** (temporary worktree, since removed). A bound notebook with one
root note, plus a row corrupted by native SQL; snapshot, download, then publish
a plain edit of the root note through `publishNotebookGitProposal`:

| Stray row | Before `07e9ee0434` | Current `main` |
|---|---|---|
| Note whose folder is in another notebook | NPE, **frame-for-frame the production trace** (`folderPath:90` ← `portablePath:55` ← `requireOneLiveNoteAtPath:188` ← `applyModificationsAndRenames:99` ← `publish:178`) | publishes; the stray note stays invisible to Git |
| Folder whose parent is in another notebook | NPE in `reconcileUnrepresentedFolders` | NPE, same message: `folderPath:81` ← `folderPath:83` ← `foldersByPath:103` ← `ensureAncestry:53` ← `applyModificationsAndRenames:86` ← `publish:178` |

Snapshot and download succeeded in every case. The two shipped changes moved
live-note paths off the row map, which cured the first row of the table only.
On current `main` every publication computes a path for **every** folder row
(`foldersByPath`, then `reconcileUnrepresentedFolders`), so one stray folder
blocks every publication to that notebook. The shipped regression test
reproduces a different, also real, way to reach the same frames; it was not the
owner's case if the owner's change had no rename.

**Confirmed production data** (ids only; this repository is public). 26,761
notes and 8,883 folders hold 6 stray notes and 4 stray folders, one level deep,
in four notebook pairs. Counting the notes inside the stray folders, 12 notes
and 4 folders sit outside their root ancestor's notebook. None of them is in
any accepted tree, clone or export today.

| Row notebook | Container notebook | Stray notes | Stray folders | Notes inside those folders | Owners |
|---|---|---|---|---|---|
| 12 | 4 | 2 | 2 | 3 | circle 2 → user 1 |
| 4 | 26 | 2 | 1 | 1 | user 1 → circle 2 |
| 86 | 191 | 1 | 1 | 2 | user 1 → user 1 |
| 56 | 309 | 1 | 0 | 0 | user 3 → user 3 |

- Folder 2543 of notebook 26 holds notes 9949 and 21009 of notebook 4 (the
  original trace) and is the parent of folder 6623 of notebook 4 (the crash on
  current `main`). This is exactly the predicted shape.
- A notebook that owns a stray **folder** cannot publish at all: 4, 12 and 86.
- All seven notebooks have a Git binding, all cut over on 2026-09-17.
- A containment repair hits no unique title or folder-name constraint.

**Origin: legacy, no live writer.** All four stray folders were created in the
same second, 2026-04-28 08:26:33, and are named after notes: the one-time
conversion of parent notes into folders. It faithfully preserved parent-child
note links that already crossed notebooks in 2021 and 2022. Every stray row's
`updated_at` falls in a bulk touch (2026-04-30, 2026-08-17), not an edit. That
conversion is squashed into the baseline, so this is inferred from timestamps.

**What the web app already treats as true.** Folder contents are listed by
folder id alone (`findNotesInFolderOrderByIdAsc`,
`findChildFoldersByParentFolderIdOrderByIdAsc`), so the owner sees stray rows
under the *containing* folder's notebook. Containment is the representation in
use; the stray `notebook_id` is the stale copy.

**Proposed fix (for refinement, not agreed):**

1. Repair the affected rows once (16 at most), as an application operation inside
   `AcceptedWebChangeService.apply` over both notebooks of each pair. **Not a
   SQL migration:** whichever direction is chosen, one notebook gains content
   its accepted tree lacks, and an accepted head only advances through that
   service. A notebook drifted by raw SQL stays drifted, because the service
   commits only for notebooks that matched before the change.
   **Owner decision, direction of the repair:**
   - *Follow containment* (the row joins its container's notebook). Matches
     what the tree shows today. For pairs 12→4 and 4→26 it changes who can
     read the notes: five circle notes become private, three private notes
     become circle-visible.
   - *Keep the notebook* (the row detaches to its own notebook's root). Nobody
     gains or loses access. Root-level name collisions are unchecked.
   The same-owner pairs (86→191, 56→309) are safe either way.
2. No null guard in `folderPath` and no tolerant skip: once the invariant
   holds, the code that assumes it is correct, and a guard would hide the next
   violation the way the top-down walker hid this one.
3. Enforcing the invariant with composite foreign keys is **not** recommended
   now: `fk_note_folder ... ON DELETE SET NULL` would null `notebook_id` too,
   and InnoDB checks row by row, which breaks the folder-by-folder
   cross-notebook move. The deeper cause is that `notebook_id` is stored at
   every level of a tree that already determines it; removing that redundancy
   is a separate, larger story.
4. Plan 142 alone would not fix production. Walking live parent entities
   removes the NPE in `foldersByPath`, but `reconcileUnrepresentedFolders`
   still walks rows. A fully live walk would judge the stray folder
   unrepresented and try to remove it (reasoned from the code, not run).

## Remaining structural work

One instance of the original defect class survives.
`NotebookGitProposalFolderMaterialization.foldersByPath` keys its map by a path
computed from **snapshot rows** while the value is the **live `Folder` entity**
re-loaded by id.

Tracing every caller showed this is **latent, not live**: rows are re-read
immediately before each materialization (`NotebookGitProposalFolderRelocation`
returns state carrying `foldersOf(...)` after reparenting,
`NotebookGitProposalDocumentApplication` re-reads after materializing, and only
`applyDeletions` — notes only — runs in between). So it fixes no reproducible
failure today; the value is that the guarantee stops depending on every caller
remembering to re-read.

The waste is concrete: `FolderRepository.findByNotebookIdOrderByIdAsc` already
returns `List<Folder>` with `LEFT JOIN FETCH f.parentFolder`,
`NotebookExportRows.folders` flattens those entities into rows, and
`foldersByPath` rebuilds an id→row index **and re-finds every `Folder` by id**.
Entities → rows → entities, once per folder.

Already covered and green: `NotebookGitComposedFolderRelocationControllerTest.publishesRelocateThenDescendantEditAndAddRetainingIdentitiesWithEarlierParent`
relocates `Topics/` under `Archive/` and adds `Archive/Topics/Extra.md` in the
same publication, asserting the added note's folder is the relocated `Topics`.
Reuse it; do not duplicate it.

**Execution trap:** the `folders` local at
`NotebookGitProposalFolderRelocation` is *not* dead — it still feeds
`requireNoUnrepresentedEmptySourceDescendants` and
`requireMatchingAcceptedTree`. Only the `ensureAncestry` argument is removed.

## Story Decomposition

### 1. Publishing notebook 4's proposal succeeds, and folder ancestry has one representation

**Executable plan (structural half only):**
[142-live-folder-ancestry-materialization](../quick/142-live-folder-ancestry-materialization/PLAN.md)
— status `planned`, one Structure slice, not taken.

- **Goal (carried, unrefined):** the owner who reports publication failing on
  notebook 4 can publish; and folder ancestry inside the one final application
  is resolved from live entity state only, so the failure class cannot recur
  through a stale snapshot.
- **Split found at diagnosis:** the production failure is a data-invariant
  repair (see Diagnosis); the structural cleanup is independent and fixes
  nothing the owner sees. Expect two stories, repair first.
- **First evaluable step:** the owner chooses the repair direction (see
  Diagnosis). Production rows are already collected and confirm the diagnosis.
- **Effort hypothesis:** structural half S (one class, three call sites,
  mirrors a change already made). Repair half S to M: one
  application-level repair of at most 16 known rows through the accepted web change
  path, plus a regression test built from the experiment.
- **Safe stopping point:** the structural half can ship alone; the existing
  relocation test already states the guarantee it protects.

## Constraints carried

- Do not touch `ExportFolderRow`,
  `NotebookGitAcceptedTree.folderPath(row, map)`, accepted-tree or
  proposed-tree comparison, ZIP export, or `LockedNotebookState`'s shape. Those
  compare snapshots against snapshots, which is correct. Only
  live-entity-against-snapshot mixing is in scope.
- Materialization runs inside the publication transaction, so walking a lazy
  `parentFolder` chain initializes rather than throwing. A caller found outside
  a transaction is a stop for human judgment, not a null guard (ADR 0006).
- Nothing publication accepts today may stop being accepted.

## Related

- ADR 0002 — *Apply one final projection atomically*. A short constraint stating
  that the final application resolves Portable paths from live entity state was
  drafted this session and is committed but **unpushed**, bundled into another
  session's commit `f31cf631ee`. Confirm the owner accepts it.

## When to Surface

Now — the owner reports an active production failure.
