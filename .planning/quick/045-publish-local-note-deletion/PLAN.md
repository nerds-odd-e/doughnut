# Publish one local note deletion

Source: [SEED-009 Story 5](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-5).
Status: planned; no implementation started.

## Goal and scope

An owner deletes one Markdown note in one local commit, publishes it, and sees
that note become inactive in Donut without transferring its private data.
Use the existing publish and receive flows to learn from this first deletion.

Accept exactly one deleted regular note file in a direct-child commit of
accepted `main`, from an eligible clean bound checkout. Root and existing-folder
notes are supported. Reuse soft deletion and deactivate the note's trackers;
retain private associations on the deleted identity. Leave authored references
untouched. Preserve the authored Git commit and accept it with the deletion
atomically. Existing addition/edit publication remains supported.

A later, separately published addition at another available path gets fresh
identity even with identical content. The existing deleted-title collision
continues to reject same-path recreation. These are the conservative defaults
from the refinement selected for planning, with no additional product policy.

Exclude bulk or mixed deletions, rename/move inference, folder/README deletion,
attachment cleanup, purge, undo/restore, web structural synchronization,
multiple unpublished commits, rebase/conflicts, drift repair, preview, and new
confirmation UI. Do not create follow-on stories or implement deferred ideas
as part of this plan.

## Execution context and current decisions

- `NotebookController.publishNotebookGitProposal` is the stable publication
  boundary. `NotebookGitProposalTreeShape` currently rejects every removed
  path; `NotebookGitProposalPublisher` handles additions and modifications.
  Extend that existing acceptance path, without a new API or transport.
- A deletion must pass the accepted file's regular-mode and ordinary-note-path
  checks. Validate the complete diff before mutating notes: any deletion plus
  another change rejects the entire proposal, including exact-content moves.
  Keep existing non-deletion rules and whole-tree Markdown validation.
- Resolve the deleted path against the accepted live projection, then reuse
  `NoteService.destroy` with `LEAVE_DEAD_LINKS`. Do not read the missing file
  from the proposed tree as Markdown. The post-projection comparison must use
  the remaining live notes, so accepted content equals the authored tree.
- Reuse current owner, ancestry, expected-head, projection-drift, and retry
  checks. No changes to transaction ownership, storage schema, binding locks,
  asynchronous work, or identity inference are proposed.
- Existing `NotebookGitBundleControllerTestBase` uses committed fixtures and
  `Propagation.NOT_SUPPORTED` around the publisher's `REQUIRES_NEW` boundary.
  Its `proposalBundleBytes` takes the complete proposed file list: omission
  already expresses deletion, including an empty tree. Reuse
  `inCommittedTransaction` for associated fixtures and fresh observations.
  `NotebookGitPublicationAtomicControllerTest` already exercises late binding
  save failure with this transaction arrangement. Matching fixtures and proof
  exist; no speculative storage experiment is required during planning.
- Reuse `NotebookGitCopyIdentityControllerTest` fixtures for trackers, questions,
  and conversations, including committed-fixture cleanup. Soft deletion needs
  no hard-delete migration or new FK-closure machinery.
- Accepted [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  permits empty folders to have no tracked representation. Deleting the last
  note leaves its Donut container intact and does not manufacture a README.
  Earlier Git commits retain the file; Portable content gains no private IDs.
  [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md) permits loud
  failures. The rollback proof below tests the promised unchanged business
  state, not merely that an exception is thrown. ADR 0002 remains Proposed.

## Outside-in proof ownership

| Contract promise / story example | Owning slice | Observable proof |
| --- | --- | --- |
| One isolated deletion makes the note inactive and its trackers unavailable for recall; other notes retain identity/data (example 1) | 1 | Publication controller, fresh persisted state and existing recall boundary |
| Reject mixed/bulk deletion, moves, README and invalid file shapes; keep existing add/edit capability (example 4) | 1 | Controller acceptance/rejection cases plus existing publication regressions |
| Authored links remain unchanged and the deleted destination becomes unresolved (example 2) | 2 | Read referrer through `NoteController.showNote` after publication |
| Root/folder placement, last-note deletion, retained containers, exact accepted tree | 3 | Controller cases and downloaded bundle tree/ancestry |
| Stale, drifted, or failed publication leaves remote state unchanged (example 4) | 4 | Existing rejection gates plus fresh state after late binding-save failure |
| Accepted retry is unchanged (example 1) | 5 | Same head/bundle and unchanged deletion/tracker timestamps |
| Later copied content gets new identity; deleted-path reuse stays rejected (example 3) | 6 | Separate accepted deletion/addition commits and private association IDs |
| Owner uses real local Git and installed CLI; guidance explains the supported deletion; rejected local work is preserved | 7 | Focused existing CLI notebook E2E plus current CLI readiness/submission suites |
| Another eligible checkout receives exact deletion without Portable metadata or history loss (example 1) | 8 | CLI `run` with real Git checkout and accepted bundle |

## Ordered slices

### 1. Publish an isolated deletion of a learned note
Type: Behavior
Status: planned
Proof: Controller publication accepts one deletion, removes that note from live
results and recall, and preserves unrelated note/tracker state; unsupported
deletion shapes reject before mutation. Run backend verification below.

Behavior: A notebook matches accepted `main` and contains a learned root note
and an unrelated learned note → publish a direct-child commit deleting only
the target → accept its exact head and soft-delete only that note and its
trackers. Its private records retain their original identity associations.

Extend the existing tree classifier and publisher together. Replace the obsolete
blanket-deletion rejection test with a valid matching-projection success case;
do not let its currently untyped/drifted fixture masquerade as proof of the new
boundary. In the same acceptance change, keep rejection coverage for two
deletions, deletion+edit, deletion+addition/move, reserved README, and invalid
modes/paths. Use data variations for the shared rejection outcome. No preliminary
Structure slice is needed: the existing fixture can already build these trees.

### 2. Leave referring content unchanged
Type: Behavior
Status: planned
Proof: Publish deletion through the controller, then read a referrer through
`NoteController.showNote`; its authored body/frontmatter is identical and the
exact target link is unresolved. Run backend verification.

Behavior: A note contains body and property references to the exact target path
→ publish only the target's deletion → retain the referrer's Markdown and let
existing resolution report the missing destination. Reuse existing reference
fixtures; fix only a demonstrated gap in the selected leave-dead-links flow.

### 3. Delete the last note without deleting its container
Type: Behavior
Status: planned
Proof: Parameterized controller cases for the only root note and the only note
in an existing folder; downloaded bundle has the exact remaining tree and
retained parent history, while the notebook/folder still exists. Run backend
verification.

Behavior: The target is the last note at the root or in a folder → publish its
isolated deletion → retain the Donut container without adding a README. Include
an empty final tree. Check the parent commit still contains the deleted file;
do not add folder lifecycle behavior.

### 4. Keep failed deletion publication atomic
Type: Behavior
Status: planned
Proof: Extend the existing late-binding-save failure fixture with a learned
note deletion; observe fresh committed note/tracker deletion flags and
timestamps, accepted head, bundle, and binding timestamp unchanged. Existing
stale/drift cases gain deletion proposals as needed. Run backend verification.

Behavior: A deletion encounters a stale accepted parent, projection drift, or
a failure after projection but before binding save → publication fails → no
part of that deletion becomes accepted or deactivates learning. Keep the
existing transaction mechanism; do not introduce compensation or catch-and-hide
behavior. Reuse concurrency regressions rather than build a new race harness.

### 5. Retry an accepted deletion without changing it
Type: Behavior
Status: planned
Proof: Controller accepts a deletion, time advances, and the identical head is
submitted again; head/bundle and note/tracker deletion timestamps remain equal
to the first acceptance. Run backend verification.

Behavior: The already accepted deletion still matches the current projection
→ retry the original proposal → report the existing accepted head without
another deletion or commit. Retain existing rejection when projection has drifted.

### 6. Keep later additions independent of the deleted identity
Type: Behavior
Status: planned
Proof: Controller publishes a deletion, then a separate same-content addition
at an available path; the new note has a different ID and no inherited tracker,
question, or conversation associations. A same-deleted-path variant rejects
without changing remote state. Run backend verification.

Behavior: A note with private associations has an accepted deletion → publish
identical text in a later commit → create fresh identity at a different available
path, or retain the existing deleted-title conflict at its old path. Use a root
destination or a still-represented folder; do not turn an emptied, unrepresented
folder into a new-parent-authoring requirement. Reuse the creation and
deleted-destination policies without changing them.

### 7. Publish a local deletion through the installed CLI
Type: Behavior
Status: planned
Proof: Extend `e2e_test/features/cli/cli_notebook_clone.feature`: clone, commit
the removal of `Recipes/Pasta.md`, publish, observe the authored accepted head
and the remaining note tree in Donut. Run the focused E2E and relevant CLI tests.

Behavior: The owner has an eligible bound checkout → removes one file with Git,
commits and runs the existing publish command → Donut displays the deletion
and the CLI reports its accepted head. Update clone/help guidance that lists
supported publication shapes to include isolated deletion and leaving links
untouched; do not imply support for mixed deletion or same-path recreation.

Add only the missing explicit deletion-commit task/page-object/step to the
existing temporary-checkout harness. Preserve its explicit file selection;
no generic filesystem-action framework or second PTY harness. Update the
existing exact guidance assertion with the product copy. Reuse existing
readiness and rejected-submission tests for preservation of local commits,
index and files; add a deletion variant only where current proof is insufficient.

### 8. Receive the accepted deletion in another checkout
Type: Behavior
Status: planned
Proof: Extend `cli/tests/notebookPull.fastForward.suite.ts` through `run` with
real Git: clone at the accepted parent, supply the accepted deletion bundle,
pull, and observe the absent file, exact head/tree, clean `main`, retained
ancestry, unchanged other files, and no Portable metadata. Run focused CLI tests.

Behavior: A second clean bound checkout is at the deletion's accepted parent
→ owner runs `donut notebook pull <directory>` → its working tree receives the
deletion without losing history. Backend bundle proof in slice 3 supplies the
server side of this existing transport boundary. No new multi-checkout E2E
framework or pull implementation is planned unless this proof exposes a gap.

## Verification and delivery

- Backend changes: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
  The backend rule requires all backend unit tests, even for a focused change.
- CLI changes/receive proof:
  `CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookClone.test.ts tests/notebookPublish.test.ts tests/notebookPull.test.ts`.
- Real installed-CLI publication:
  `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_clone.feature`.
  This inspected feature has no `@ignore` tag. Keep any temporarily multi-beat
  new scenario `@wip` until green; never close a slice with a CI-breaking test.
- At execution, use the required execute-plan wrap-up for each leaf: Jidoka,
  fresh post-change-refactor agent, API regeneration only if actually needed,
  coordinator `./scripts/run.sh pnpm format:changed` once, plan update, commit,
  push and asynchronous CI observation. No API signature change is expected.
- Reuse earlier passing evidence unless subsequent edits invalidate its promise.
  No full E2E suite, manual-testing session, or mutation testing is requested.

## Sizing, readiness, and learning checkpoint

Ready for direct execution. Each leaf owns one behavior or data variation and
one focused verification loop using inspected existing boundaries. Target about
five minutes of change and verification per leaf; these are hypotheses, not
guarantees. Backend suite or focused E2E runtime may exceed that target on its
own; record actual test-runtime exceptions rather than hiding implementation
overruns. Slice 1 has two closely coupled production edits; slice 7 adds one
explicit harness action. Neither needs a speculative preparatory subsystem.

At five minutes scrutinize hidden outcomes/preparation. At ten minutes of
non-exempt work, preserve evidence/WIP and finer-decompose in this PLAN under
the repository's learning-escalation rule. Do not widen the story to fix an
unrelated synchronization limitation. A changed product boundary returns to
Story 5 refinement; repeated overruns require the prescribed story review.

Stop when deletion, receipt, and subsequent fresh identity are demonstrated.
Report the observed workflow and remaining restrictions before considering any
follow-on capability. On completion, remove this spent plan and reduce the home
story to delivered goal/scope while preserving its anchor and sibling stories.

Planning validation: inspected implementation and existing test fixtures; no
product tests or storage experiments were run for this documentation-only task.
