# Keep a local note edit across an accepted folder move

Source: [SEED-009 Story 15](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-15).
Status: planned; planning only, implementation not authorized by this request.

## Goal and scope

An owner can pull one accepted exact folder relocation beside one unpublished
local descendant-note content edit, then explicitly publish that edit onto the
same learned note at its new path. The refined source story owns the contract.

Require a clean bound main, one single-parent local content commit, and exactly
one accepted single-parent move directly from the shared base. The same-name,
README-backed subtree retains all relative paths, bytes, and modes. Destination
is root or an existing represented parent. Only the local edited descendant
changes content after replay; nested descendants and identical-content files
are included. Refuse unsupported or non-unique mapping before changing the
checkout. Pull never publishes and accepted commit IDs remain unchanged.

Exclude edits outside the moved subtree, multi-note/local-commit batches,
multiple accepted commits, accompanying remote edits, folder renames,
note-only moves, new parents, unrepresented descendants, README authoring,
collisions, deletion, dirty trees, drift repair, link rewriting, and Story 10.
No new API, command, UI, identity metadata, or general structural merge engine.

## Execution context and decisions

- CLI eligibility is in `cli/src/commands/notebook/notebookLocalCandidate.ts`;
  structural history is currently rejected. `notebookAcceptedCommitChanges.ts`
  intentionally reads diffs with rename detection disabled.
- `notebookPull.ts` downloads into a temporary accepted repository, inspects
  local history there, then imports accepted objects and changes the checkout.
  Reuse its clean-main/race checks, temporary-repository lifecycle, and existing
  content-only rebase behavior.
- The server's `NotebookGitProposalFolderShape` already recognizes one complete
  same-name subtree mapping; use that contract, including whole-tree absence
  of unrelated changes. An accepted move already passed server placement and
  identity checks. No database experiment or persistence change is needed.
- For this exact-move branch, derive the new path by source-prefix replacement
  with the same relative path. Replay the local blob onto the accepted tree
  at that path; its accepted blob equals the base blob, so no content merge
  is needed. Construct one child commit in the temporary repository, retaining
  the original author and message. Install only a fully constructed result
  into the still-clean checkout; preserve the original commit through Git's
  normal history/reflog mechanisms. Do not use similarity-based rename pairing
  to choose the destination. Ordinary content-conflict rebase stays unchanged.
- Reuse existing content publication after replay; it sees an unchanged-path
  content edit relative to accepted main. Backend changes are not anticipated.
  Existing validation still rejects invalid content or a subsequently stale head.
- [Accepted ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  constrains the Portable tree; keep server IDs and auxiliary files out of it.
  [Accepted ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md)
  permits loud unexpected failures; handled refusal must give useful guidance.
  ADR 0002 remains Proposed; the seed supplies the selected workflow constraints.

## Outside-in proof

| Promise | Owner | Observation |
|---|---|---|
| One local descendant edit survives one exact move | 2 | CLI `run` with real Git: resulting parent is accepted head; changed path is the mapped descendant, with exact local bytes |
| Nested/root destinations and duplicate bytes map correctly | 2 | Data variants through the same CLI boundary; only the relative-path counterpart changes |
| Other files, authored links, accepted commits remain unchanged; pull never publishes | 2 | Compare complete resulting tree against accepted tree plus that one blob; inspect accepted SHA and network calls |
| Explicit publication keeps identity and learning data | 2 | Installed-CLI move/pull/publish journey plus controller-boundary identity/association proof |
| Unsupported/ambiguous inputs preserve local work | 2 | Real-Git checkout snapshot equality and readable refusal for shape/history counterexamples |
| Original author/message and recoverable original commit survive | 2 | Inspect new commit metadata and original commit/reflog after successful pull |
| Owner can discover limits and recovery | 3 | Clone/pull help output names the exact supported move and accurate refusal steps |

## Ordered slices

### 1. Separate exact move correspondence from checkout mutation
Type: Structure
Status: planned
Proof: Existing real-Git pull boundary regressions remain green; structural
intervals are still refused and checkout snapshots remain unchanged.

Internal change: Give the existing history inspector a cohesive internal
representation of one exact accepted subtree mapping, derived from full tree
entries and the single accepted edge. Keep move acceptance disabled until
slice 2. Reuse current ancestry and no-rename Git reads; isolate only the
mapping calculation needed by that next Behavior. No public testing exports,
new wire format, generic diff framework, or database mapping.

Immediate next Behavior: slice 2 uses this mapping to retain the local edit.
The structure commit alone intentionally changes no external behavior.
Sizing hypothesis: about 5 minutes for tree correspondence and existing focused
CLI verification, medium confidence. Blob/mode equality and prefix mapping are
already the server's bounded contract. If this demands additional identity
policy, stop for story review. Do not add replay machinery in this leaf.

### 2. Keep and publish the descendant edit after the move
Type: Behavior
Status: planned
Proof: One folder-move editing journey, driven through CLI `run` with real Git
and the installed CLI; backend controller evidence verifies retained identity.
The variants below share that same mapped-edit outcome and proof loop.

Behavior: One local descendant content edit and one accepted exact folder move
→ pull, inspect, explicitly publish → the original learned note receives the
local edit at the mapped path, with all other accepted content preserved.

Consume slice 1's correspondence only for one local descendant content edit.
Build the accepted tree plus the one local blob at its mapped path in the
existing temporary repository, create one child, then import/install that
result after existing readiness checks. Keep local main recoverable through
Git and retain author/message. No content merge is required for this branch.
Use the existing content-publication flow without changing its validation.

Outside-in cases: direct descendant; represented nested descendant and move
to root; identical-content descendants distinguished by relative path. Check
one canonical full resulting tree and parent, with variant-only assertions.
Unsupported shape/history counterexamples must still refuse before checkout
mutation. Initial refusal may retain the existing structural message; slice 3
replaces it with actionable recovery wording. Accepted history is never changed
by pull. Reuse fixtures; add no internal-helper tests.

Close the identity promise with the existing folder-relocation controller test
extended through content publication, and the existing installed-CLI feature
with the local edit committed before the move. These observe the same journey,
not additional product outcomes. Keep a developing multi-beat E2E scenario
`@wip` until green; do not commit red tests or claim completion from CLI alone.
Sizing hypothesis: about 5 minutes of implementation and slice-local cleanup
plus focused verification, medium confidence after correspondence is separated.
Backend-suite and E2E runtime can exceed the target; record actual runtime as
an exception when it does. If replay/install work itself reaches 10 minutes,
stop and refine; do not exempt implementation time as testing time.

### 3. Discover the bounded move workflow and safe refusal recovery
Type: Behavior
Status: planned
Proof: CLI clone and pull-help assertions; refusal output in the pull suite.

Behavior: An owner reads clone next steps or pull usage, or encounters an
unsupported structural interval → the output explains one local descendant
edit across one exact folder move, separate publication, and recovery by
reconciling/recreating a supported child of accepted history.

Update existing canonical help text and its callers/assertions. Keep refusal
wording accurate: work is preserved, direct publication of the divergent
commit is not a recovery action. Do not broaden batch/addition guidance.
Sizing hypothesis: about 5 minutes including focused CLI tests and cleanup.

## Verification commands and evidence boundaries

- CLI proof loop: `CURSOR_DEV=true nix develop -c pnpm --dir cli exec vitest run tests/notebookPull.test.ts tests/notebookPublish.test.ts`.
  Register new pull cases under the existing single entrypoint, sharing the
  real-Git checkout fixtures. Add clone tests for the guidance slice only.
- Extend `e2e_test/features/cli/cli_notebook_folder_relocation.feature` so the
  second checkout commits its Pasta edit BEFORE the first publishes the move.
  Pull the second checkout, then publish it and observe the new path/content.
  Reuse current steps/page objects; do not reset the Git binding mid-journey.
  Run `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_folder_relocation.feature`.
- Identity cannot be inferred from CLI file contents. Extend the existing
  controller folder-relocation private-association scenario through a later
  content publication at the moved path, checking the original note/tracker,
  schedule and private associations, with an identical-content control note.
  Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only` per backend rules.
  The CLI tree/parent proof, controller proof and installed-CLI journey jointly
  close the same contract; a successful rebase alone is insufficient.
- Refusal data: extra remote edit/commit, two moves, changed subtree bytes/mode,
  folder rename, missing README, non-unique mapping, and local edit outside
  the subtree. Retain existing dirty/merge/multiple-local-commit regressions.
- At execution, perform required execute-plan wrap-up per leaf: Jidoka, fresh
  post-change-refactor agent, API generation only if needed, coordinator's one
  selective-format pass, plan update, commit and push. Do not execute now.

## Refinement assessment

The original combined mapping/replay leaf was **Refine**: it mixed a new tree
correspondence calculation with checkout mutation and publication evidence.
Replaced it in this same PLAN with Structure 1 immediately enabling Behavior 2.
The guidance leaf is now Behavior 3. No story scope or evidence was dropped;
all contract promises above now belong to Behavior 2 or 3. Structure 1 proves
unchanged behavior only; it cannot claim the story delivered.

All three leaves are ready as sizing hypotheses, with one cohesive proof loop
each. Ready for execution when authorized; execution is not part of this task.
No permanent sizing exception is granted; the runtime policy below applies.

## Current questions and learnings

No open product questions. Exact subtree correspondence is supported by the
existing folder-publication contract. Do not add a general identity mechanism.

Sizing is a hypothesis: scrutinize at 5 minutes; stop and refine at 10 minutes
unless one focused verification run explains the excess. Backend-suite or
installed-CLI test runtime may warrant a recorded exception at execution;
implementation complexity does not. Follow Learning escalation on repeated
non-exempt overruns. No tests have been run for this planning-only change.
