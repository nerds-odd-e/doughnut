# Require an empty notebook for initial Readme publication

Source: [SEED-016 Story 1](../../seeds/SEED-016-initial-notebook-and-folder-readmes.md#story-1), completed quick/079.
Status: planned; not executed.

## Goal and scope

Keep initial two-Readme publication within its original empty-notebook boundary.
A notebook with existing empty folders must not qualify merely because those
folders have no Portable files. Preserve successful publication from a genuinely
empty notebook and the existing sole-folder publication behavior.

Exclude broader initial imports, changes to Portable export representation,
soft-deleted-note policy, new endpoints, schema changes, and unrelated cleanup.

## Review evidence and current decisions

- Reviewed implementation commits `d1aa6205ca` (shape recognition and extraction)
  and `034be9f99d` (acceptance and controller proof), plus `e9b56195eb` (completion
  cleanup). Original execution-ready plan: `d67bce4a34`. Aggregate boundary:
  `d67bce4a34..e9b56195eb`; current HEAD remains the reviewed boundary.
- `NotebookGitProposalFolderCreationShape` checks accepted/proposed file paths.
  `PortableTreeSnapshot.collectDirectory` emits no entry for a folder without
  Readme or notes. Thus an existing empty folder passes accepted-tree equality.
- `NotebookGitProposalFolderAcceptance.acceptInitialCreation` never rejects
  existing folder rows. A differently named empty folder does not conflict with
  `FolderConstructionService` sibling validation; publication creates a second
  folder and accepts the proposal, outside the source story's scope.
- Finding is established by code tracing, not a newly executed regression test.
  The execution PLAN records passing backend tests for the original happy path.
- Retain the existing publisher transaction as atomicity owner. Apply the
  eligibility correction only to initial two-Readme acceptance, before writes.
- ADR 0004 explicitly omits empty folders from Portable files; do not change
  that representation to enforce this story's narrower eligibility rule.

## Ordered slices

### 1. Refuse initial Readmes when the notebook already contains an empty folder
Type: Behavior
Status: planned

Behavior: A Git-backed notebook contains an empty folder named `Existing`, with
an empty accepted Portable tree → its owner publishes a direct-child commit
adding valid `README.md` and `Field Notes/README.md` → publication refuses the
non-empty notebook without changing its Readme, folders, or accepted bundle/head.

Proof: Add a controller regression alongside
`NotebookGitProposalFolderCreationControllerTest`, using real folder and Git
fixtures. First observe the currently incorrect acceptance. After correction,
assert rejection and unchanged persisted state outside the failed transaction.
Keep the existing genuinely empty two-Readme and sole-folder success examples
green. Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only` during execution.

Implementation seam: Use the locked notebook state's folder rows to enforce
the initial-only eligibility rule before Readme writes. Keep the tree and
projection checks intact; do not extend the correction to sole-folder creation.

Sizing hypothesis: roughly five minutes active implementation and focused
review; backend-suite runtime is an external-wait exception. Scrutinize at five
minutes and finer-decompose before ten active minutes if hidden work appears.
One outcome and one controller proof loop; ready for direct execution.

Execution wrap-up: Jidoka, fresh post-change-refactor agent, coordinator's one
`./scripts/run.sh pnpm format:changed` pass, plan update, commit and push per
execute-plan. No API generation is expected. This retrospective does not execute
any of those steps.

## Contract-to-proof map

| Promise | Owning proof |
| --- | --- |
| Empty notebook, not merely empty Portable tree, is required | Slice 1 existing-empty-folder rejection |
| Refusal leaves persisted notebook and Git binding unchanged | Slice 1 post-failure observations |
| Supported initial and sole-folder publication remain usable | Existing controller success examples in slice 1 verification |
