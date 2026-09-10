# Consolidate root container publication proof

Status: executed — retrospective correction complete.
Source: [SEED-017 Story 11](../../seeds/SEED-017-cohesive-design-corrections.md#story-11).

## Finding and bounded outcome

Current whole-suite review found one overlapping method:
`publishesInitialNotebookAndRootFolderReadmesAsTheExactAuthoredCommit` in
`NotebookGitProposalInitialNotebookStructureControllerTest` repeats the same
controller, database and Git-bundle round trip for a notebook Readme plus root
Folder Readmes and zero Notes.

The retained container-tree proof already covers that observable contract more
strongly through its notebook-Readme-enabled root-folder scenario: exact
authored head, tree, paths and content; root Folder ancestry and Readme content;
and zero live Notes. The overlap is test-suite residue rather than a
product-behavior defect.

Give container-only initial publication one controller proof owner without
changing product behavior. Preserve the notebook-structure test class's mixed
Readme-and-Note cases, rejection behavior and document-role coverage. Preserve
the notebook-Readme-only controller proof and the installed CLI initial-tree
journey because they own distinct empty-root and end-to-end boundaries.

## Outside-in proof ownership

All preservation checks below belong to slice 1.

| Preserved promise | Owning proof |
| --- | --- |
| Notebook and root Folder Readmes publish with exact authored Git state and content | `NotebookGitProposalInitialContainerTreeControllerTest.publishesContainerTreesAsTheExactAuthoredCommit`, row `A\|B\|C, true` |
| Container-only publication creates root Folders and zero live Notes | The same retained controller scenario's Folder-path and Note-row assertions |
| Mixed initial notebook structures and their refusals remain supported | Remaining `NotebookGitProposalInitialNotebookStructureControllerTest` scenarios |
| Notebook Readme publication without any Folder retains its distinct proof | `NotebookGitProposalInitialNotebookReadmeControllerTest.publishesInitialNotebookReadmeAsTheExactAuthoredCommit` |
| Installed publication still round-trips through the CLI | Existing `Publishing an initial nested tree round-trips the authored checkout` E2E scenario remains unchanged |

The CLI row is preservation of the existing proof, not a new E2E execution
promise: inspect the final diff to confirm the feature and its supporting code
are untouched. The backend suite exercises the retained controller boundaries.

## 1. Remove the remaining duplicate root-container round trip

Type: Structure
Status: done
Proof: the retained container-tree controller proof continues to cover notebook
and root Folder Readmes, exact accepted and downloaded Git state and content,
root ancestry, and zero Notes; the full backend unit suite passes after the
overlapping notebook-structure method is removed.

Remove only
`publishesInitialNotebookAndRootFolderReadmesAsTheExactAuthoredCommit` from
`NotebookGitProposalInitialNotebookStructureControllerTest`. Do not add a
single-root count case: one versus several root Folders is example arrangement,
not a separately justified product rule, and the retained scenario already
exercises each Folder through the common collection path. Remove imports only
when the remaining methods no longer use them. Make no production, API, schema,
CLI, E2E or backlog changes.

Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`. Follow the normal
post-change refactor, one coordinator `./scripts/run.sh pnpm format:changed`
pass, plan update, commit, push and asynchronous CI-observation gates. No API
generation trigger exists.

Sizing: approximately 5 minutes active work plus the required backend-suite
runtime, high confidence. The full-suite runtime is the stated exception to the
active slice target. If active implementation, focused verification work and
cleanup exceed 10 minutes, stop and refine before continuing; the exception
covers suite waiting time only. A failed retained assertion is new evidence to
investigate, not permission to remove coverage or expand the correction.

## Current decisions

- Folder counts in examples do not create separate product contracts.
- This correction owns only the duplicated successful container-only proof;
  rejection, mixed-concept, root-without-Folder and installed-CLI boundaries
  remain distinct.

## Refinement assessment

Source inspection reconfirmed the duplicate and retained observations. One
Structure slice directly owns the evidenced test-suite correction; no preparatory
Behavior or additional slice is needed. The common collection proof covers root
Folders independently of fixture count, so the cumulative design introduces no
new case recognizer or parallel representation. Mixed-concept, rejection and
empty-root boundaries keep their existing owners.

No slice-specific concern was found in this assessment. No product code was
changed and no tests were run during planning. Execution remains unstarted.

## Execution

Removed `publishesInitialNotebookAndRootFolderReadmesAsTheExactAuthoredCommit`
from `NotebookGitProposalInitialNotebookStructureControllerTest`. No other
production, API, schema, CLI, E2E or backlog changes. Post-change refactor
review found no residue: no stale references to the removed method, all
imports and constants in the file remain used by the four remaining test
methods, and no duplication of the removed scenario exists elsewhere.

```
proof:
  command: CURSOR_DEV=true nix develop -c pnpm backend:test_only
  covers: full backend unit test suite, including the remaining
    NotebookGitProposalInitialNotebookStructureControllerTest scenarios,
    NotebookGitProposalInitialContainerTreeControllerTest, and
    NotebookGitProposalInitialNotebookReadmeControllerTest
  result: pass
```
