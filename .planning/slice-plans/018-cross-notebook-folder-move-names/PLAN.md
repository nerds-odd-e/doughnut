# Moving a folder to another notebook respects the folder's one set of names

## Source

- Story: [SEED-009#story-47](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-47)
- **Identity:** SEED-009#story-47
- Found by the closing review of the local AI notebook effort (SEED-048#story-1),
  report at `ca15b7d7c1:.planning/slice-plans/016-review-and-close-local-ai-notebook-effort/report.html#arch-names`.
- Direction: [North Star — One set of names per folder](../../NORTH-STAR.md#one-set-of-names-per-folder);
  refusals are clear conflicts, not database errors (ADR 0006).

## Goal and scope

A notebook owner who moves a folder into another notebook gets the same name
protection and the same refusals as a move within one notebook, so the
destination never holds entries whose names differ only by letter case, and a
clash is a clear conflict instead of a server error.

Included: the move to another notebook, with and without merge, is checked by
`FolderSiblingNameValidation` against the destination notebook before any row
changes; `mergeTargetOrRejectConflict` is deleted.

Excluded: other users of the exact-match sibling check (`requireNoConflictingSibling`
in folder creation and `NotebookGitProposalFolderPlacement`); one "first free
name" operation; repair of existing case-variant siblings; carrying files
across notebooks (stays refused); UI and E2E changes. Git needs nothing beyond
the existing one accepted commit per notebook.

## Starting facts (checked 2026-09-27 at `0a6b4717aa`)

- `FolderMoveRelocation.moveFolderToAnotherNotebook` calls
  `mergeTargetOrRejectConflict`, which uses `findCandidateChildContainers`:
  exact `=` on `folder.name` (`utf8mb4_bin`, case-sensitive), folders only.
- Its merge calls `FolderSubtree.mergeInto`, which skips
  `FolderContentsPlacementCheck.requireContentsFit`. A clashing note then hits
  `uk_note_notebook_folder_title` (`lower(title)`) as a server error.
- `requireContentsFit` looks names up in `source.getNotebook()`, so it cannot
  yet check a destination in another notebook.
- Moves within one notebook already use `mergeTargetOrRefuse` and
  `FolderSubtree.mergeWithinNotebook` (checked); their clash cases live in
  `NotebookFolderMoveNameClashControllerTest`.

## Outside-in proof

Controller-level tests through `folderController.moveFolder` with a source and a
destination notebook, added beside the same-notebook cases in
`NotebookFolderMoveNameClashControllerTest`. Existing
`NotebookGitWebFolderCrossNotebookMoveControllerTest` (one commit in each
notebook, merge, undo, files refused) and the `folder_organization.feature`
cross-notebook scenarios stay green unchanged.

| Seed example | Slice |
| --- | --- |
| 1. `shared` onto another notebook's `Shared`, no merge → `FOLDER_NAME_CONFLICT`, nothing moves | 1 |
| 1b. same with merge → merged into `Shared`, which keeps its name | 1 |
| 2. `shared` onto another notebook's root file `shared` → `RESOURCE_CONFLICT` naming `shared` | 1 |
| 3. merge where `Intro` meets `intro` → `RESOURCE_CONFLICT` naming `Shared/intro.md`, nothing changes | 2 |
| 4. merge where `Deep` meets `deep` → merged into `deep` | 2 |
| 5. no clash → unchanged | existing tests |

## Slices

### 1. The destination notebook's folder name check is the one owner's

Type: Behavior
Status: done
Accepted proof: `NotebookFolderMoveNameClashControllerTest`
`refusesMovingOntoACaseVariantFolderInAnotherNotebook`,
`mergesIntoACaseVariantFolderInAnotherNotebookKeepingItsName` and
`refusesMovingOntoAFileNameInAnotherNotebook` failed first, then passed; the
`*CrossNotebookMove*` classes and the full `pnpm backend:test_only` (2676) passed.
Proof: new cross-notebook cases for examples 1, 1b and 2 in
`NotebookFolderMoveNameClashControllerTest` fail first (1 and 2 currently
succeed with a case-variant sibling or a folder beside a same-named file; 1b
creates a sibling instead of merging), then pass; the existing cross-notebook
move tests stay green.

Behavior: another notebook holds, at the destination, a folder or file whose
name matches the moved folder's ignoring case → move the folder there →
a folder is `FOLDER_NAME_CONFLICT` ("A folder with this name already exists
here.") without merge and the merge target with merge; a file is
`RESOURCE_CONFLICT` naming its path; no row changes on refusal.

Change: `moveFolderToAnotherNotebook` calls
`mergeTargetOrRefuse(destinationNotebook, newParent, folder, merge)`; delete
`mergeTargetOrRejectConflict`.

### 2. Merging into another notebook checks every entry first

Type: Behavior
Status: planned
Proof: new cross-notebook merge cases for examples 3 and 4 in the same test
class; example 3 fails first (server error today), then passes with nothing
changed in either notebook; existing merge tests stay green.

Behavior: a merge into another notebook where a moved note, file or subfolder
clashes (ignoring case) with a non-folder entry at any level → refused with
`RESOURCE_CONFLICT` naming the destination path before any row changes;
subfolders meeting folders still merge.

Change: `FolderContentsPlacementCheck` resolves names in the destination's
notebook rather than the source's; the cross-notebook merge uses the checked
merge (`mergeWithinNotebook`, renamed to fit both uses, for example
`mergeChecked`), leaving unchecked `mergeInto` only for recursion inside a
checked merge.

## Current decisions

- One common rule for both moves: the owner answers from the destination
  notebook's live rows. No cross-notebook special case in the validator.
- Moving a folder that holds files to another notebook stays refused, checked
  before the name checks as today.

## Learnings

- A cross-notebook move into the folder itself or a descendant is refused with
  400 before the 404 for a parent in another notebook
  (`rejectsCrossNotebookMoveIntoItself`), so the two moves' destination checks
  cannot yet share `validateDestinationAndFindMergeTarget` without changing
  that order on purpose.
