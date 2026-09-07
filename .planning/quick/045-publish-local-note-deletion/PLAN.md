# Publish one local note deletion

Source: [SEED-009 Story 5](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-5).
Status: blocked at slice 1 verification; implementation is uncommitted.

## Open Dough ADR assessment (v0.2.1, 2026-09-07)

Source: public Open Dough `v0.2.1` (`22b65e63090836b5441c8b4c9a4b3e8afd271d50`),
native Codex `$dough-adr-awareness`.

Conclusion: the planned isolated single-note deletion respects current Accepted
[ADR 0004 — OKF-compatible notebook Markdown profile](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
and [ADR 0006 — Failure handling](../../docs/adrs/0006-failure-handling-accepted.md).
[ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization.md)
remains Proposed and non-binding. No architectural exception or supersession
is required. Index and in-file statuses agree.

Next step: do not begin slice 2 or further deletion implementation yet. Slice 1
verification is blocked by unrelated committed-fixture deadlocks. Repairing
those deadlocks is a delivery-scope decision, not an ADR-policy decision. If
authorized, repair them, rerun the required backend verification, then complete
slice 1 wrap-up.

## Execution handoff

Slice 1 implements complete-diff isolation guidance, removed-path/mode checks,
and explicit modification dispatch; isolated deletion remains rejected.
Changes are in `NotebookGitProposalTreeShape`, `NotebookGitProposalPublisher`,
and new `NotebookGitDeletionRejectionControllerTest`. No slice is delivered;
refactor, formatting, commit, and push have not run.

Required command: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
All nine new rejection cases and eight existing tree-shape cases passed in
both implementation runs. Initial red proof: 2221 tests, exactly eight expected
assertion failures, 48 seconds. Implementation runs: 2222 tests, one deadlock in
52 seconds, then five deadlocks in 48 seconds. No further retry was performed.

Bounded failure evidence: InnoDB at 08:06:34 showed recall fixture `INSERT mcq`
holding a note lock while waiting for an mcq FK-index gap; concurrent
`DELETE note ... external_identifier LIKE 'batch-prune-committed-%'` held the
gap and waited for that note. The cleanup belongs to untouched
`QuestionGenerationBatchRetentionWithoutTransactionTest`. At 08:08:16,
committed Git fixture cleanup held a question-generation-batch index gap while
waiting for a note, and batch insertion held that note while waiting for the
gap. Retry failures were fixture insert/cleanup `PessimisticLockException`s,
including existing publication-concurrency cleanup, with no distinct assertion
failure. The recurring committed-fixture concurrency defect remains unrepaired;
the required full backend proof is blocked. A passing retry would not repair it.

Next action requires developer scope decision: repair the unrelated committed
fixture deadlocks, then resume slice 1 verification and normal wrap-up. Preserve
the existing implementation and unrelated release-planning work. Approximate
elapsed time: ten minutes; implementation about four minutes, backend runs
148 seconds plus Nix startup, remainder bounded diagnosis. No commands remain
running from the implementer.

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

- CI observation: Codex coordinator `/root`, checkout `/Users/terryyin/git/doughnut`,
  repository `nerds-odd-e/doughnut`, branch `main`. Sandbox blocked initial Nix
  startup. Escalated bridge cell 7 failed to publish a receipt/session handle;
  terminated the bridge after bounded inspection. No exact observer ownership
  could be recovered, so no guessed process termination or replacement launch.
  Notification coverage is unavailable; observer shutdown is unconfirmed and
  pending CI is unobserved.

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

## Refinement assessment

No slice has started, so there is no completed execution evidence or overrun
history to invalidate. This is a sizing correction to the original plan, not
new story scope. All original promises are retained below.

| Original slice | Assessment | Replacement / reason |
| --- | --- | --- |
| 1. Isolated learned-note deletion | Refine | 1–2 separate rejection policy from enabling acceptance; guards remain before mutation |
| 2. Referring content | Ready | 8 retains its single reference-resolution proof loop |
| 3. Last note/container | Ready | 9 retains root/folder data variations of the same container-preservation outcome |
| 4. Atomic failure | Refine | 3, 10, 11 separate late rollback, stale head, and projection drift fixtures |
| 5. Accepted retry | Ready | 12 retains one repeated-submission proof loop |
| 6. Later identity | Refine | 13–14 separate successful fresh creation from deleted-path rejection |
| 7. Installed CLI | Refine | 4–5 isolate immediately enabling harness work; 6 owns guidance independently |
| 8. Receive deletion | Ready | 7 retains real-Git CLI receive proof and moves earlier for learning |

## Outside-in proof ownership

| Contract promise / story example | Owning slice | Observable proof |
| --- | --- | --- |
| Reject mixed/bulk deletions and moves with isolation guidance; reject reserved/invalid files before mutation | 1, retained by 2 | Controller rejection matrix; fresh binding and live-note state unchanged |
| Isolated deletion preserves the authored commit and deactivates only the target and its trackers (example 1) | 2 | Publication controller, exact head/tree and fresh note/tracker state; recall boundary excludes target |
| Failed publication rolls back deletion (example 4) | 3 | Existing late binding-save failure harness, fresh committed state unchanged |
| Real installed-CLI publication shows the deletion in Donut | 5 (enabled by 4) | Local Git removal → installed CLI → accepted head and Donut note tree |
| CLI guidance states the supported deletion and its limitations | 6 | Existing clone output and its exact output assertions |
| Another eligible checkout receives the deletion without history loss or Portable metadata (example 1) | 7 | CLI `run`, real Git checkout, exact accepted head/tree and absent file |
| References stay authored and the exact deleted destination is unresolved (example 2) | 8 | Referrer's Markdown and `NoteController.showNote` resolution |
| Root/folder placement, empty final tree, containers retained, no generated README, parent file retained | 9 | Controller location variants and downloaded bundle/history |
| Stale proposal cannot delete newer accepted content (example 4) | 10 | New accepted web edit followed by old deletion proposal; winner unchanged |
| Projection drift prevents deletion without absorbing web changes (example 4) | 11 | Unsupported web creation followed by deletion proposal; current notes and accepted binding unchanged |
| Accepted retry is unchanged, including timestamps (example 1) | 12 | Original proposal repeated after time advances; same head/bundle and deletion state |
| Later copied content receives fresh identity, never old private associations (example 3) | 13 | Separate deletion/addition publications; original tracker/question/conversation foreign keys retained |
| Same-path recreation remains rejected (example 3) | 14 | Deleted-title conflict after accepted deletion; no resurrection or binding change |
| Existing owner, clean checkout, `main`, ancestry, typed-tree, add/edit and local-preservation rules remain | 1–2, 5, 7 | Existing controller and CLI regression suites; reuse evidence at these boundaries |

## Ordered slices

Every Behavior keeps its implementation, focused proof, and local cleanup in
one leaf. Later data-variation leaves should use existing behavior when it
already works, adding only missing observable evidence and fixing demonstrated
gaps. Do not manufacture extra production changes for proof-only variations.

### 1. Explain why a deletion must be isolated
Type: Behavior
Status: in-progress
Proof: A parameterized controller rejection case covers deletion+edit,
deletion+addition (including identical-blob rename), and two deletions; each
reports isolation guidance and leaves accepted binding and live notes unchanged.
Run backend verification, retaining existing add/edit, path, mode and README gates.

Behavior: A proposed commit removes a note along with another file change →
publish → reject the whole proposal with guidance to use an isolated deletion
commit, without inferring a rename or publishing part of it.

Move removed-path classification into the complete-diff decision so this policy
can be evaluated before projection. Keep **single deletions rejected** at the
final tree acceptance gate until slice 2. Do not let a new deletion kind fall
through the publisher's existing modified-note branch. Keep the accepted-mode
and ordinary-note-path checks on removals; include removal variants for reserved
README/non-regular paths in the same rejection suite. The current standalone
single-deletion rejection remains green here and is replaced in slice 2.

### 2. Publish one learned note's deletion
Type: Behavior
Status: planned
Proof: One controller success scenario accepts the authored deletion head/tree,
soft-deletes the target and its trackers, and excludes it from live notes/recall
while an unrelated learned note remains active. Run backend verification,
including slice 1's rejection matrix.

Behavior: A matching notebook contains a learned root note and another learned
note → publish only the target's deletion → accept that exact commit and
make only the target inactive, retaining the original note/tracker identities
and learning values.

Remove slice 1's temporary single-deletion gate and handle deletion explicitly
in the existing publisher: resolve the accepted path, call `destroy` with
`LEAVE_DEAD_LINKS`, and remove the note from the proposed live-note collection.
Do not parse the absent blob. Replace the obsolete rejection fixture with a
valid typed tree matching actual notes. Leave transaction/ref advertisement
ownership unchanged. Questions/conversations are covered by slice 13's richer
fixture rather than making this first success loop build every association.

### 3. Roll back a deletion when acceptance fails late
Type: Behavior
Status: planned
Proof: One deletion scenario in the existing late-binding-save failure harness
observes fresh committed note/tracker flags and timestamps, accepted head,
bundle and binding timestamp all unchanged. Run backend verification.

Behavior: Projection has applied a learned note's deletion but binding save
fails → publication fails → the note remains active and the accepted revision
remains unchanged. Reuse committed fixtures and failure injection in
`NotebookGitPublicationAtomicControllerTest`; no new failure framework,
compensation, or transaction policy. Stale-head and drift cases belong to 10–11.

### 4. Prepare the existing checkout harness for an explicit removal
Type: Structure
Status: planned
Proof: Existing installed-CLI notebook feature remains green; no product or
existing test-flow behavior changes. Run focused notebook E2E verification.

Structure: Add only a named single-path removal-and-commit task in
`cliE2eNotebookCloneTasks.ts`, its `notebookClone.ts` page-object method, and thin
`cli_notebook_clone.ts` step. Use system `git rm -- <path>` in the test-owned
checkout and return the new head through the existing accepted-head alias.
This enables **immediately following slice 5**, with no generic action model,
new checkout framework, or content-list sentinel for removed files. Do not
reuse the retained-file assertion that calls `cy.readFile` on every proposal
path; existing rejection proof remains in the CLI suites.

### 5. Publish the local removal through the installed CLI
Type: Behavior
Status: planned
Proof: One scenario in `e2e_test/features/cli/cli_notebook_clone.feature` clones,
removes and commits `Recipes/Pasta.md`, publishes, and observes the authored
accepted head and Donut's remaining note tree. Run the focused notebook E2E.

Behavior: The owner has a clean bound checkout → commits one local removal and
runs the installed publish command → the CLI reports acceptance and Donut shows
the deletion. Reuse slice 4's action and existing publish/tree assertions; no
additional test harness work or product copy changes belong here. This provides
early end-to-end evidence before later boundary variations. Guidance is slice 6.

### 6. Explain the supported deletion in existing CLI guidance
Type: Behavior
Status: planned
Proof: Existing clone-output assertion includes isolated deletion and its
leave-links-untouched policy. Run the focused CLI clone test and update/run the
existing notebook E2E exact-copy assertion if it changes.

Behavior: The owner clones a notebook → reads the existing next-step guidance
→ learns that one separately committed note deletion can be published, that
links remain authored, and that mixed deletion and same-path recreation are
unsupported. Update only existing publication-shape guidance in
`nonInteractiveCli.ts` and matching assertions; do not add a new help surface,
preview, chooser, or confirmation flow.

### 7. Receive the accepted deletion in another checkout
Type: Behavior
Status: planned
Proof: `cli/tests/notebookPull.fastForward.suite.ts` drives `run` with real Git:
checkout at the accepted parent → supply the accepted deletion bundle → pull;
assert absent file, exact head/tree, clean `main`, retained ancestry, unchanged
other files and no Portable metadata. Run focused CLI pull tests.

Behavior: A second eligible checkout is at the deletion's parent → pull → its
working tree receives the deletion without losing history. Slice 2 supplies
the exact accepted head/tree proof; slice 9 covers empty-tree/parent-file cases.
Use the existing accepted-bundle transport fixture, not a new multi-checkout
E2E framework. Do not change pull unless this focused proof exposes a gap.

### 8. Leave referring content unchanged
Type: Behavior
Status: planned
Proof: Publish deletion through the controller, then read a referrer through
`NoteController.showNote`; its authored body/frontmatter remains identical and
the exact target link is unresolved. Run backend verification.

Behavior: A note contains body and property references to the exact target path
→ publish only the target's deletion → retain its Markdown and let existing
resolution report the missing destination. Reuse reference fixtures; fix only a
demonstrated gap in the selected leave-dead-links flow.

### 9. Delete the last note without deleting its container
Type: Behavior
Status: planned
Proof: Parameterized controller cases for the only root note and the only note
in an existing folder; downloaded bundle has the exact remaining tree and
parent history while the notebook/folder still exists. Run backend verification.

Behavior: The target is the last note at the root or in a folder → publish its
isolated deletion → retain the Donut container without generating a README.
Include an empty final tree and confirm the parent commit still contains the
file. These are data variations of container preservation, not folder deletion.

### 10. Reject deletion based on a stale accepted head
Type: Behavior
Status: planned
Proof: Adapt the existing accepted-web-edit fixture in
`NotebookGitProjectionDriftControllerTest`: reject the old deletion proposal
and observe the winning head/content and active target unchanged. Run backend
verification.

Behavior: A web edit advances accepted `main` after the local deletion proposal
was built → publish that stale proposal → retain the newer accepted content.
Use sequential accepted writes; existing concurrency coverage supplies the
unchanged locking policy. No new race harness or rebase behavior.

### 11. Reject deletion when the projection has drifted
Type: Behavior
Status: planned
Proof: Adapt the existing unsupported-web-creation drift fixture: deletion
fails while accepted binding, live target/tracker state and the unsynchronized
web addition remain unchanged. Run backend verification.

Behavior: Current Donut content differs from accepted `main` → publish a
single-deletion child of that accepted head → reject without absorbing the web
change or deleting the target. No resnapshot or drift repair after setup.

### 12. Retry an accepted deletion without changing it
Type: Behavior
Status: planned
Proof: Controller accepts a deletion, time advances, then the identical head
is submitted again; head/bundle and note/tracker deletion timestamps remain
identical to first acceptance. Run backend verification.

Behavior: Accepted deletion still matches the current projection → retry the
original proposal → report the existing head without another deletion/commit.
Reuse existing rejection evidence for a retry whose projection has drifted.

### 13. Give a later same-content addition fresh identity
Type: Behavior
Status: planned
Proof: Controller publishes deletion then a separate addition at an available
path; new ID and no inherited tracker/question/conversation associations, while
original associations still reference the soft-deleted note. Run backend
verification.

Behavior: A note with private associations has an accepted deletion → publish
identical text at another available root path → create a fresh note without
reviving or transferring those associations. Adapt
`NotebookGitCopyIdentityControllerTest` fixtures/cleanup. Observe the old
associations through fresh persisted rows: deleted-note view/access filtering
must not be mistaken for erased history. Use public note views for the new note.
No empty-folder authoring or same-path collision fixture in this leaf.

### 14. Keep the deleted path reserved after publication
Type: Behavior
Status: planned
Proof: After controller-accepted deletion, a same-path addition produces the
existing deleted-title conflict and leaves head/bundle, deleted note and
tracker timestamps unchanged, with no new live note. Run backend verification.

Behavior: A deleted note still reserves its path → publish a later same-path
addition → reject without resurrecting the old note or creating another one.
Adapt `NotebookGitDeletedDestinationControllerTest` to reach this precondition
through accepted deletion rather than web deletion plus a test resnapshot.
Keep existing title reuse and restore policy unchanged.

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

Ready for direct execution of the revised plan; implementation has not begun.
Four bundled original slices were replaced; four were retained and reordered.
The result is 13 Behavior leaves and one immediately enabling Structure leaf.
No completed proof was discarded and no product scope was added or removed.

Sizing hypothesis: about five minutes per leaf for the bounded edit, its one
proof loop and local cleanup, with moderate confidence from inspected fixtures.
Slices 1–2 now separate complete-diff rejection from successful projection;
3/10/11 each reuse a different existing failure fixture; 4 prepares only 5;
6 is copy/assertion work; 13 and 14 no longer share a multi-outcome fixture.
Existing root/folder or body/property data variations remain together because
they exercise one outcome through the same boundary. No remaining leaf requires
an unexamined storage mechanism or a new test framework.

Test-runtime exceptions are **conditional**, not pre-granted elapsed-time waivers:
the required backend suite (backend leaves) or focused Cypress run (4–6) may
itself exceed five/ten minutes. Record actual runtime if that occurs. A second
verification command protecting the same changed boundary does not justify
combining independent behaviors; setup/debugging and wrap-up still count as
work. No blanket exception covers implementation or fixture overruns.

At five minutes scrutinize hidden outcomes/preparation. At ten minutes of
non-exempt work, preserve evidence/WIP and finer-decompose in this PLAN under
the repository's learning-escalation rule. Do not widen the story to fix an
unrelated synchronization limitation. A changed product boundary returns to
Story 5 refinement; repeated overruns require the prescribed story review.

Stop when deletion, receipt, and subsequent fresh identity are demonstrated.
Report the observed workflow and remaining restrictions before considering any
follow-on capability. On completion, remove this spent plan and reduce the home
story to delivered goal/scope while preserving its anchor and sibling stories.

Refinement validation: checked replacement ownership against every original
promise and inspected the affected existing fixtures. No product code was
changed or tested. This skill invocation edits this PLAN only; no commit or push
is part of refinement.
