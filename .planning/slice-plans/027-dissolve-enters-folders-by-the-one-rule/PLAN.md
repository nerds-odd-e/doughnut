# Folder dissolve and merge enter subfolders by the one entry rule

## Source

- Story: [SEED-050#story-9](../../seeds/SEED-050-local-ai-notebook-technical-debt.md#story-9)
- **Identity:** SEED-050#story-9
- Correction of SEED-050#story-8; provenance
  `da1e269ba5:.planning/slice-plans/026-one-folder-entry-rule/PLAN.md`,
  commits 649e162dd4 (slice 1), cba24a706e (slice 2).

### Finding (retrospective of plan 026, 2026-09-27 at cba24a706e)

`FolderContentsPlacementCheck.firstFolderMeetingAFolder` (`:70-76`) inlines
`folderHolding`, else `entryHolding` → `refuseTaken` — the rule
`FolderSiblingNameValidation.folderToEnter(notebook, parentOrNull, name,
excludedFolderIds)` now holds. Introduced by 38bd1e731e and 47891402c2
(2026-09-26), before plan 026; plan 026 did not scan for it.

## Outside-in proof

Structure: existing tests stay green unchanged in intent.

| Preserved promise | Guarding tests |
| --- | --- |
| merge-move onto a nested note name is refused naming it; dissolve onto a note or file name is refused | `NotebookGitFolderDissolveGuardControllerTest` |
| dissolve merges case-variant subfolders and carries contents | `NotebookFolderDissolveControllerTest`, `NotebookGitFolderDissolveControllerTest`, `NotebookGitFolderDissolveAtomicControllerTest` |

## Slices

### 1. Dissolve placement check uses folderToEnter

Type: Structure
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests 'com.odde.donut.controllers.NotebookGitFolderDissolveGuardControllerTest' --tests 'com.odde.donut.controllers.NotebookFolderDissolveControllerTest' --tests 'com.odde.donut.controllers.NotebookGitFolderDissolveControllerTest' --tests 'com.odde.donut.controllers.NotebookGitFolderDissolveAtomicControllerTest'`
green; `git diff --stat` shows net fewer lines in `FolderContentsPlacementCheck.java`.

Change: in the subfolder loop, `existing = folderToEnter(notebook,
destinationOrNull, new DisplayName(child.getName()), excludedFolderIds)`;
drop the inline `takenBy(...).ifPresent(refuseTaken)` for subfolders. Keep the
note and file loops.

Accepted proof: the four named classes passed (Guard 8, FolderDissolve 9, GitFolderDissolve 5, GitFolderDissolveAtomic 1 — 23 tests, 0 failures); `FolderContentsPlacementCheck.java` 6+/8−. Refactor pass: no edits.

## Execution complete

Product advice: no change. The correction leaves the folder-entry rule only in
`FolderSiblingNameValidation.folderToEnter`; the queue order (SEED-050#story-10
next) stands.
