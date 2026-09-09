# Publish the initial notebook README with one README-only folder

Source: [SEED-016 Story 1](../../seeds/SEED-016-initial-notebook-and-folder-readmes.md#story-1).
Status: planned; not executed.

## Goal and scope

From an empty accepted notebook and Portable tree, accept one direct-child
commit adding exactly regular-file `README.md` and
`New Folder/README.md`, both valid nonblank `type: Readme` Markdown. Store the
authored notebook Readme, create one fresh root Folder with its authored
Readme, create no ordinary Note, and accept the exact commit atomically.

Exclude every third path, ordinary notes, additional or nested folders,
non-empty accepted notebooks, existing-container README edits, attachments,
multiple unpublished commits, stale/divergent history, and bulk import.

## Current decisions and evidence

- Keep `NotebookController.publishNotebookGitProposal` as the stable proof
  boundary and the existing publication transaction as the atomicity owner. No
  endpoint, CLI, API schema, migration, or transport change.
- Extend only the existing root-folder-creation route. The already delivered
  sole `Folder/README.md` case must remain unchanged.
- Treat the two authored Readmes as one exact initial container-tree outcome;
  do not generalize root README edits or multi-path folder publication.
- Reuse strict typed-Markdown, authored-content, folder-name, sibling-collision,
  Portable-projection, ancestry, ownership, and exact bundle acceptance gates.
- Verified failure: a temporary controller test against an empty accepted tree
  added only these two README paths. `CURSOR_DEV=true nix develop -c pnpm
  backend:test_only` ran 2,330 tests and failed only that example with `400 BAD_REQUEST`
  for the reserved `New Folder/README.md`; the diagnostic test was then removed.
- Accepted ADR 0001 supplies Notebook, Folder, Note, and Readme terminology.
  Accepted ADR 0004 defines `README.md` and `Folder/README.md` as the Portable
  container representations with `type: Readme`. No ADR change is needed.

## Outside-in proof

At the controller boundary, start with an empty matching Git-backed notebook.
Publish one direct-child proposal adding only root `README.md` and
`Field Notes/README.md`. Observe the authored notebook Readme, one fresh root
Folder named `Field Notes` with its authored Readme, zero ordinary Notes, and
accepted/downloaded head and tree exactly equal to the proposal.

## Ordered slices

### 1. Recognize the exact initial two-README shape
Type: Structure
Status: planned
Proof: Existing backend controller tests remain green and the verified two-path
example retains its current reserved folder-README refusal. Run
`CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

Structure: Extend the existing root-folder-creation classification to identify
exactly one added root `README.md` plus exactly one added root-level
`Folder/README.md`, with no accepted files and no other changed path. Preserve
the existing one-folder-only candidate, relocation recognition, and all wider
refusals. Route the new candidate through the current refusal until slice 2
consumes it. Do not introduce a general container-change hierarchy.

Sizing hypothesis: about five minutes for one classifier branch and one
existing-suite proof loop.

### 2. Accept both authored Readmes as the exact initial tree
Type: Behavior
Status: planned
Proof: A controller test observes both stored Readmes, one root Folder, zero
ordinary Notes, and exact accepted/downloaded proposal head and tree. Then run
`CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

Behavior: An owner of an empty matching notebook publishes one direct-child
commit adding only the notebook Readme and one root folder Readme → Donut stores
both Readmes, creates that Folder, and atomically accepts the exact authored
commit.

Use the existing folder-creation acceptance transaction. Validate both blobs
as nonblank `type: Readme`, apply the root Readme to the Notebook and the folder
Readme to one freshly constructed root Folder, refresh the projection, and
require exact proposed-tree equality before the shared binding write. Keep the
sole-folder case working and do not admit any third path or non-empty baseline.

Sizing hypothesis: about five minutes for one controller-first behavior loop;
the full backend-suite runtime is an external-wait exception.

## Contract-to-proof map

| Story promise | Owning proof |
| --- | --- |
| Initial notebook and folder Readmes publish together | Slice 2 controller publication |
| Both authored contents and one fresh root Folder are preserved | Slice 2 notebook/folder observations |
| No ordinary Note is inferred | Slice 2 note-repository observation |
| Exact authored commit is accepted atomically | Slice 2 downloaded head/tree plus existing transaction |
| Third paths and broader initial trees remain excluded | Slice 1 refusal suite and slice 2 full backend suite |

## Slice-plan refinement

Both slices are Ready. Slice 1 has one internal classification gate and one
unchanged-behavior proof loop; Slice 2 has one externally observable outcome
and one controller-first proof loop. No slice was replaced or split. Neither
has an unexplained path beyond the ten-minute hard limit; backend-suite runtime
is the stated external-wait exception.

Resulting slice count: 2. Story resplit is not recommended. The refined plan is
ready for direct execution when separately authorized.

## Learnings

None beyond the verified two-path failure recorded above; implementation has
not started.
