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

## Unresolved: the owner still reports the failure

The fix is demonstrably in the deployed release, so the persisting report needs
a cause. Candidate explanations, **none investigated**, roughly by likelihood:

1. **A different defect with the same symptom.** The repair covers exactly
   `folder == null` in `folderPath` reached via `requireOneNoteAtPath`. A
   different frame, message, or endpoint means a different bug. Obtaining the
   current stack trace and diffing it against the original is the cheapest
   discriminator and should come first.
2. **Wrong code path.** The original report's request line was
   `/api/notebooks/4/git-bundle?expectedHead=2a473e9a2bda77d18b0b30a568b3023788ecd4ee`,
   but the attached trace was from `publishNotebookGitProposal`
   (`NotebookController:505`). That mismatch was noticed and never explained.
   If the live failure is on the bundle-export path, neither shipped change
   touches it.
3. **Stale data on notebook 4.** The fix prevents the bad resolution; it does
   not repair a projection already left inconsistent by an earlier failed
   publish. Divergence between the accepted head and the live projection could
   keep failing, plausibly as projection drift rather than an NPE. Inspecting
   notebook 4's folder and note rows against its accepted tree would settle it.
4. **Not actually serving the new jar** — instance rollover or a cached build.
   Least likely given the successful deploy, but cheap to rule out.

**Never reproduced:** the reporter's actual proposal content for notebook 4 is
unknown. Both shipped changes rest on a mechanism reproduced from the stack
trace, not on replaying their bundle. This is the main reason a second cause is
plausible.

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
- **Likely splits at refinement:** the diagnosis-and-fix of the persisting
  production failure is a different outcome from the latent structural cleanup,
  and their evidence differs sharply — one has a live reporter, the other has
  none. Expect this to become two stories, with the diagnosis first. It is kept
  as one here only because it was recorded rather than refined.
- **First evaluable step:** obtain the current production stack trace and
  compare it to the original. That decides whether this is the same defect and
  therefore whether hypotheses 2–4 are worth pursuing.
- **Effort hypothesis:** structural half S (one class, three call sites,
  mirrors a change already made). Diagnosis half unknown until the trace is in
  hand — it may be minutes or a separate investigation.
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
