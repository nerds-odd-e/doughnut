# Keep attachments inside notebook folders through local and web changes

## Source and outcome

- Source: [SEED-035#story-9](../../seeds/SEED-035-ai-workspace-supporting-files.md#story-9),
  refined and resplit 2026-09-21. Work item: **SEED-035#story-9**.
- Authority: the owner asked for a refined slice plan. Planning only; no
  execution is authorized.
- Goal: an owner can publish a real local checkout with non-Markdown files inside
  its folders, and keep using Web Donut folder operations without losing them.
- Scope: nested publication (add, edit, rename, remove, files-only folders, last
  file removed dissolves the folder); files follow web folder rename, move, trash
  and recover; permanent deletion removes contained files; dissolving, merging or
  moving to another notebook a folder that contains files is refused.
- Exclusions: dissolve and merge of folders with files
  ([SEED-035#story-11](../../seeds/SEED-035-ai-workspace-supporting-files.md#story-11),
  [plan 007](../007-dissolve-merge-folders-with-files/PLAN.md)); files following
  a folder to another notebook (SEED-035#story-10); ZIP export proof
  (SEED-009#story-46 removes it); web file controls; image rendering; reference
  rewriting; size limits and save cost (SEED-034#story-3); special dot-folder
  handling. History reset and Git cutover read the one live tree, so they
  include folder files without a delivery or proof commitment.
- State: slices 1–7 are done with accepted proof; 5 Behavior slices remain
  planned. Remaining concerns are listed at the end.
- Execution identity (started 2026-09-21): Story Branch Mode; originating and
  integration checkout `/Users/terryyin/git/doughnut` on `main`; execution
  checkout `/Users/terryyin/git/doughnut-worktrees/notebook-folder-attachments`
  on `codex/notebook-folder-attachments`; integration publication target
  `origin/main`; Story Branch delivery target
  `origin/codex/notebook-folder-attachments`; published Taken claim
  `418f9d914813f29964fbed297c6dfa5eafc7ae67`. Replanning was not explicitly
  selected; preserve the plan's existing planning authority.
- CI observer: GitHub Actions workflow `ci.yml` / `donut CI`, repository
  `nerds-odd-e/doughnut`, target branch `codex/notebook-folder-attachments`,
  coordinator `/root`, checkout-bound runtime in this execution worktree;
  observer directory `/tmp/dough-ci-501/watch-dlxcD4`, PID 6546, stream session
  6399, yielded cell 35. The Taken claim on `main` is `pendingCi: unobserved`
  because this Story Branch observer covers only implementation deliveries.

## Resplit mapping

The owner found the 14-slice story big and asked for two stories. That 14-slice
draft was never committed, so this table is its trace; the original 15-slice
mapping is in `.planning/quick/002-notebook-attachment-continuity/PLAN.md` at
`6b906462dd`.

| Earlier slice | Now |
| --- | --- |
| 1 folder placement (Structure) | this plan, 1 |
| 2 files follow web rename | this plan, 2 |
| 3 trash and back | this plan, 3 |
| 4 dissolve moves files up | plan 007, input 1 |
| 5 merge brings files along | plan 007, input 2 |
| 6 clash refuses dissolve/merge | plan 007, input 3 |
| 7 permanent deletion | this plan, 4 |
| 8 cross-notebook move refused | this plan, 6 |
| 9 projection by full path (Structure) | this plan, 7 |
| 10 file beside a note | this plan, 8 |
| 11 files-only folder | this plan, 9 |
| 12 local edits; last file removed | this plan, 10 |
| 13 local folder rename | this plan, 11 |
| 14 CLI round trip | this plan, 12 |
| — (new, from the split) dissolve/merge refused while a folder contains files | this plan, 5; removed by plan 007, input 4 |

## Existing solution and constraints

PFE finding — every responsibility already has an owner; extend those owners:

| Responsibility | Existing owner | Decision |
| --- | --- | --- |
| Where content sits | `Note.folder`: a note refers to its Folder and stores no path, so rename, move, trash (a move under the trash parent) and recover never touch notes | **Reuse the rule**: `NotebookAttachment` refers to its Folder the same way; null means the notebook root |
| Sibling-name uniqueness with a nullable parent | `uk_folder_notebook_parent_name (notebook_id, (ifnull(parent_folder_id,0)), name)` | **Reuse the convention** for `(notebook_id, (ifnull(folder_id,0)), filename)`, binary collation kept |
| Live Portable tree | `NotebookLivePortableTree` → `PortableTreeSnapshot` | **Change**: place attachments by folder exactly as notes are; the root-only `attachmentsHere` parameter goes away |
| Accepting a tip's files | `NotebookGitProposalAcceptance.projectRootAttachments` (one final-set rule) | **Change**: key the same rule by full path |
| Creating folders for proposed paths | `NotebookGitProposalFolderMaterialization.ensureAncestry` (path-general) | **Reuse** for attachment paths |
| Dissolving a folder whose last path is gone | `reconcileUnrepresentedFolders` + `representedInTree` (any entry path) | **Reuse unchanged**; a file is one more represented path |
| Operations that rehome a folder's direct contents | `FolderSubtree.dissolveInto`, `mergeInto`, `reassignToNotebook` — reached by dissolve, move-with-merge and cross-notebook move | **Change**: one refusal rule in `FolderSubtree` while the subtree contains files |
| Permanent deletion | `FolderRelocationService.permanentlyDeleteFolderWithinNotebook` removes folder rows descendant-first | **Reuse**: `folder_id ON DELETE CASCADE` removes the files |
| Local folder relocation | `NotebookGitProposalFolderShape` (complete path/blob evidence) + `NotebookGitProposalFolderRelocation` (reparents the Folder row) | **Reuse**; files follow the row |

This follows the [North Star](../../NORTH-STAR.md) topics *One notebook tree*
and *One accepted-change boundary*, and Accepted
[ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md#notebook--note-structure),
[ADR 0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md),
[ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
and [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md). No new
North Star topic or ADR is needed.

**Common rule the slices exercise:** an attachment is a named file placed in a
folder or at the root; its path is derived from folder ancestry. A folder
operation either carries files through that placement or, where it would have
to move files to another folder, refuses. No slice adds a second representation
(stored path strings, a folder-attachment type, a per-operation file copier).

**The refusal is deliberately broad and temporary:** any file anywhere in the
source subtree refuses dissolve, merge and cross-notebook move. A narrower test
(only direct files, only merged subfolders) would be more code for behavior that
stories 11 and 10 delete.

**Safety order (story constraint):** nested admission stays refused until slices
1–7 are done. Until slice 8 no public entry can create a folder file, so slices
2–6 set up their starting state by storing a folder attachment row and then
calling the test base's `snapshotCurrentPortableTree`, which gives a consistent
accepted tip. That seeding is a starting precondition for web-operation proof;
it is never proof of admission, which slices 8–11 establish by publication.

## Outside-in proof

Backend boundary: the existing controller tests built on
`NotebookGitWebContentControllerTestBase` (publish with
`controller.publishNotebookGitProposal` + `proposalBundleBytes`; observe with
`acceptedTip(notebook).entries()`). Prefer adding cases to the existing
attachment and folder test classes over new classes, and one scenario per rule
over one per arrangement.

- Focused backend: `CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests '<TestClass>'`
- Schema: `CURSOR_DEV=true nix develop -c pnpm backend:verify`, then the
  `database-erd` skill.
- E2E: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_publish_to_clean_clone.feature`

| Story promise (key example) | Owning slice |
| --- | --- |
| Web folder work keeps files (2) | 2, 3 |
| Permanent deletion (5) | 4 |
| Dissolve refused for now (4); merge likewise | 5 |
| Cross-notebook move refused (6) | 6 |
| Publish a real checkout (1) | 8, 9; end to end 12 |
| Local edit/rename/removal; last file removed (3) | 10 |
| Local folder rename keeps note identities and files (scope: local rename) | 11 |
| Interim nested refusal removed only when safe (constraint) | 8, after 1–7 |

## Slices

### 1. Attachments take folder placement
Type: Structure
Status: done
Internal change: migration adds nullable `notebook_attachment.folder_id`
(`ON DELETE CASCADE` to `folder`) and replaces the filename key with
`(notebook_id, (ifnull(folder_id,0)), filename)`; `NotebookAttachment` gains its
`folder`. Follow the `db-migration` skill; regenerate the ERD.
Enables: slice 2. Unchanged behavior: every existing root attachment test.
Proof: `backend:verify`; `NotebookGitRootAttachment*ControllerTest` and
`NotebookExportRootAttachmentControllerTest` stay green.
Accepted proof (2026-09-21):
`CURSOR_DEV=true nix develop -c pnpm backend:verify` passed the migration and
all 2,569 backend tests. The migrated isolated test schema was the setup;
`NotebookGitRootAttachmentPublicationControllerTest` (5),
`NotebookGitRootAttachmentLocalChangeControllerTest` (5),
`NotebookGitRootAttachmentIndependenceControllerTest` (5), and
`NotebookExportRootAttachmentControllerTest` (1) observed unchanged root-file
behavior with no failures. `CURSOR_DEV=true
DONUT_ERD_SCHEMA=doughnut_wt_0f09c575e6774885b61cc53b7c547f97_test nix
develop -c pnpm export:database-erd` passed against that migrated schema;
`docs/database-erd.md` records `folder_id` and the folder-to-attachment
`ON DELETE CASCADE` edge.
Sizing: migration plus verify likely exceeds the 5-minute target; the external
wait (migration/verify runtime) is the stated exception, not extra scope.

### 2. A folder's files appear at its path and follow a web folder rename
Type: Behavior
Status: done
Behavior: given an accepted notebook holding `physics/diagrams/force.png`, when
the owner renames `physics` to `mechanics` on the web, the new accepted tip has
`mechanics/diagrams/force.png` with the same bytes, no `.keep` in `diagrams`,
and notes under it keep their ids.
Change: `ExportAttachmentRow` carries its folder id; `PortableTreeSnapshot`
groups attachments by folder like notes and drops the root-only parameter.
Trap: the export query must left-join the folder, or root files vanish.
Test setup: one small helper on the Git controller test base that stores a file
in a folder and then snapshots the Portable tree; slices 3–6 reuse it.
Proof: one case in `NotebookGitFolderRenameControllerTest`; update
`PortableTreeSnapshotTest` for the changed signature only.
Accepted proof (2026-09-21): `CURSOR_DEV=true nix develop -c pnpm
backend:test_only` passed all 2,570 backend tests. Setup at
`NotebookGitWebContentControllerTestBase.storeFolderAttachmentAndSnapshot`
stores the folder attachment and snapshots only the starting tree. Observation
at
`NotebookGitFolderRenameControllerTest.webFolderRenameCarriesNestedAttachmentsAndNoteIdentity`
invokes the real web folder rename and asserts the exact accepted tree contains
the renamed note plus the byte-exact attachment, contains no `.keep`, and keeps
the note queryable by its original id. The existing root-attachment publication
tests stayed green, covering the export query's root-preserving left join.

### 3. Files follow their folder into trash and back
Type: Behavior
Status: done
Behavior: given the same notebook, trashing `physics` then recovering it leaves
the accepted tip with `physics/diagrams/force.png`, same bytes, and the trashed
tip shows the file under the trash path.
Expected: no production change (trash and move re-parent the folder). One
scenario covers trash and recover; a within-notebook move is the same re-parent
rule and gets no separate case. If production code proves necessary, stop and
reassess the boundary assumption before continuing.
Proof: one case in `NotebookGitWebFolderTrashControllerTest`.
Accepted proof (2026-09-21): `CURSOR_DEV=true nix develop -c pnpm
backend:test_only` passed all 2,571 backend tests. Setup uses
`storeFolderAttachmentAndSnapshot` only to establish
`physics/diagrams/force.png`; observation at
`NotebookGitWebFolderTrashControllerTest.webFolderTrashAndRecoveryCarryNestedAttachmentBytes`
invokes the real trash and recovery controllers, then reads the intermediate
accepted commit and recovered head to assert byte-exact attachment entries at
the trash and restored paths. No production change was needed.

### 4. Permanently deleting a trashed folder removes its files
Type: Behavior
Status: done
Behavior: given a trashed folder holding `a.pdf`, permanent deletion yields an
accepted tip without `a.pdf`; the stored row is gone; the earlier commit still
contains the file.
Expected: no production change (FK cascade from slice 1). If the cascade does
not hold under the persistence context, remove the files explicitly beside the
note removal already there.
Proof: one case in `NotebookGitWebFolderPermanentDeleteControllerTest`.
Accepted proof (2026-09-21): `CURSOR_DEV=true nix develop -c pnpm
backend:test_only` passed all 2,571 backend tests after the cohesion pass.
The existing complete delete fixture now includes a folder attachment and
retains its direct/nested notes and surviving sibling. Observation at
`NotebookGitWebFolderPermanentDeleteControllerTest.permanentDeleteOfATrashedFolderAppendsOneAcceptedChildWithoutTheSubtree`
invokes the real permanent-delete controller and proves the new exact tree
omits the deleted subtree, the attachment row is gone by its captured id, and
the parent accepted commit retains byte-exact `_trash/Topic/a.pdf`. The FK
cascade required no production fallback.

### 5. Dissolving or merging a folder that contains files is refused
Type: Behavior
Status: done
Behavior: given `physics/old/sketch.png`, dissolving `old` is refused with a
message saying folders that contain files cannot be dissolved or merged yet;
folders, stored files and the accepted head are unchanged. Moving a folder that
contains files onto a same-name folder with merge is refused the same way. A
folder without files still dissolves and merges.
Change: one rule in `FolderSubtree` — the source subtree holds no file — required
by `dissolveInto` and `mergeInto` before any mutation.
Interim behavior: SEED-035#story-11 (plan 007) replaces it with rehoming.
Proof: two cases (dissolve; move with merge) in
`NotebookGitFolderDissolveGuardControllerTest`, using the unchanged-state
pattern already there.
Accepted proof (2026-09-21): `CURSOR_DEV=true nix develop -c pnpm
backend:test_only` passed all 2,573 backend tests after the cohesion pass.
`NotebookGitFolderDissolveGuardControllerTest.folderContainingAFileCannotBeDissolved`
and `.folderContainingAFileCannotBeMerged` seed direct and descendant files,
invoke the real controller operations, and observe the shared clear refusal,
unchanged folder/file placement, and unchanged accepted head. Existing
file-free dissolve and merge coverage stayed green.

### 6. Moving a folder that contains files to another notebook is refused
Type: Behavior
Status: done
Behavior: given `refs/paper.pdf` (or a file in any descendant folder), moving
`refs` to another notebook is refused with a clear message; both notebooks'
folders, files and accepted heads are unchanged. A folder without files still
moves.
Change: `FolderSubtree.reassignToNotebook` requires the slice 5 rule; the
cross-notebook merge branch is already covered through `mergeInto`.
Interim behavior: SEED-035#story-10 replaces it.
Proof: one case in `NotebookGitWebFolderMoveControllerTest`.
Accepted proof (2026-09-21): `CURSOR_DEV=true nix develop -c pnpm
backend:test_only` passed all 2,574 backend tests after the cohesion pass.
Setup at `NotebookGitWebFolderCrossNotebookMoveControllerTest` creates accepted
source and destination trees with a descendant source file and an existing
destination file. Its controller scenario invokes the real cross-notebook move
and observes the shared clear refusal, unchanged source/destination folder and
file placement, and both accepted heads unchanged. Existing file-free
cross-notebook move coverage stayed green.

### 7. Acceptance projects files by their full path
Type: Structure
Status: done
Internal change: the final-set rule in `NotebookGitProposalAcceptance` compares
the tip's attachment paths with each stored file's path derived from its folder
ancestry (`NotebookGitAcceptedTree.folderPath`), and stores a new file in the
folder its path names. Admission still allows root files only, so nothing
nested can arrive yet.
Enables: slice 8. Unchanged behavior: all root attachment publication and
local-change tests stay green.
Proof: `NotebookGitRootAttachmentPublicationControllerTest`,
`NotebookGitRootAttachmentLocalChangeControllerTest`,
`NotebookGitRootAttachmentIndependenceControllerTest`.
Accepted proof (2026-09-21): `CURSOR_DEV=true nix develop -c pnpm
backend:test_only` passed all 2,574 backend tests. The three existing root
attachment controller classes each passed five tests, covering publication,
final-set local changes, and web-operation independence. The existing
`aNestedFileIsStillRefusedAndLeavesTheAcceptedFilesUnchanged` case stayed
green, proving the full-path projection did not open nested admission early.

### 8. Publish a file beside a note in a folder
Type: Behavior
Status: planned
Behavior: given an accepted tip with notes only, publishing
`physics/diagrams/Force.md` together with `physics/diagrams/force.png` is
accepted; the tip holds both with exact bytes; the next web note save keeps the
file.
Change: the non-Markdown classification stops requiring the root
(`isRootAttachment` becomes the attachment predicate at any depth; a nested
`.keep` stays a marker). Replace
`aNestedFileIsStillRefusedAndLeavesTheAcceptedFilesUnchanged` with this case;
keep the invalid-tip atomic refusal case. Rename the three
`NotebookGitRootAttachment*` test classes to drop "Root", since they now cover
files anywhere.
Interim: a folder holding only files is refused loudly by the tip comparison
until slice 9; nothing is lost.
Proof: the renamed publication test class.

### 9. Publish a folder that holds only files
Type: Behavior
Status: planned
Behavior: given an empty accepted notebook (the real first-publish situation),
publishing only `tools/cache/reference.json` is accepted; `tools` and
`tools/cache` exist as folders on the web with no invented README and no
`.keep`; the tip matches byte-for-byte.
Change: attachment paths join the destination paths given to `ensureAncestry`
before the folder state used by the tip comparison is loaded.
Also: update `docs/notebook-git-synchronization.md` "Root attachments today" to
describe folder placement and the interim refusals, and the
`NotebookAttachment`/tree-shape Javadoc.
Proof: one case in the publication test class.

### 10. Local file edits, renames and removals in folders; the last file removed dissolves the folder
Type: Behavior
Status: planned
Behavior: given accepted `tools/cache/reference.json` and
`physics/diagrams/force.png`, one publication that edits the JSON bytes and
renames the picture yields exactly that tip; a second publication deleting
`reference.json` leaves no `tools/cache` and no `tools` folder on the web.
Both observations are one rule — the stored files become exactly the tip's set —
so they share one scenario. Expected: no production change (final-set rule plus
`reconcileUnrepresentedFolders`, which already counts any entry path).
Proof: one case in the renamed local-change test class.

### 11. A local folder rename keeps note identities and carries files
Type: Behavior
Status: planned
Behavior: given accepted `physics/` holding a note and `diagrams/force.png`, a
publication that renames `physics` to `mechanics` (`git mv`) is accepted; the
note keeps its id and learning data; the file is at
`mechanics/diagrams/force.png`.
Expected: no production change. `NotebookGitProposalFolderShape` already detects
an exact relocation from complete path/blob evidence over all regular files, the
relocation reparents the Folder row, and the final-set projection then finds
nothing to change. Identical file bytes must not count as ambiguous note
correspondence. If detection needs a change, record the learning and reassess
before extending this slice.
Proof: one case in `NotebookGitProposalFolderRelocationControllerTest`.

### 12. Round trip an organized checkout through the installed CLI
Type: Behavior
Status: planned
Behavior: with the installed CLI and native Git, publish a nested text file and
a nested binary file, rename the containing folder on the web, then pull into a
second clean clone: exact paths and bytes, clean worktree, linear ancestry.
Proof: extend `cli_notebook_publish_to_clean_clone.feature` with one scenario,
reusing the root story's byte fixtures and steps. E2E runtime is the stated
focused-test exception to the slice time limit.

## Refinement assessment

Cumulative design: all slices exercise the one placement rule; four (3, 4, 10,
11) expect no production code and are proof of that rule, which is the evidence
that the model is coherent rather than a pile of cases. The only special case is
the temporary refusal, stated once and used from three call sites.

| Slice | Result | Note |
| --- | --- | --- |
| 1 | Ready | External-wait exception for migration and verify |
| 2 | Ready | Carries the shared test helper; one proof loop |
| 3, 4, 10, 11 | Ready | Proof-only if the boundary assumption holds |
| 5, 6 | Ready | One rule; 6 adds one call site |
| 7 | Ready | Split from admission so slice 8 is a single beat |
| 8 | Ready | Predicate change plus mechanical test-class rename |
| 9 | Ready, medium-low sizing confidence | Exact call point for `ensureAncestry` relative to the state load was not located |
| 12 | Ready | E2E runtime exception |

The story delivers its own outcome if stories 11 and 10 are deferred: every
folder operation either carries files or refuses loudly.

## Current decisions

- Owner decision 2026-09-21: the nullable `folder_id` migration and its entity
  mapping may ship in the same commit/release; accept the brief production
  window created by post-ready Flyway migration for this change.
- Dissolve, merge and cross-notebook move of a folder containing files are
  refused for now under one rule (owner, 2026-09-21: cross-notebook refusal and
  the two-story split). If slice 5 or 6 shows that carrying the files is no more
  code than refusing, stop and ask the owner; do not decide in execution.
- The clash rule (refuse, name the path, change nothing) belongs to
  SEED-035#story-11 and needs nothing here.
- Dot-folders: default handling. Last file removed locally: folder dissolved.
- ZIP export gets no proof or change here.

## Learnings

- Slice 1's cohesion pass updated `NotebookAttachment` Javadoc to describe
  folder placement, so slice 9 no longer needs that documentation edit; its
  synchronization-contract documentation remains planned.
- Slice 5 made `FolderSubtree` the single Spring-managed owner of subtree
  traversal and the temporary attachment guard; slice 6 reuses that owner
  rather than adding another repository check.
