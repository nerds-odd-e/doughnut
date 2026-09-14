# Publish accumulated local commits without rewriting history

Status: in progress. Slice 8 done; slice 9 next. Slice 10 awaits the existing
unanswered deletion/recreation decision and remains non-executable until answered.
Source: [SEED-009 story 20](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-20).

## Planned-execution identity

- Originating checkout/branch: `/Users/terryyin/git/doughnut` on `main`
  (claim commit `f245ee2f5a`)
- Execution checkout/branch:
  `/Users/terryyin/git/doughnut-worktrees/122-publish-accumulated-local-commits`
  on `quick/122-publish-accumulated-local-commits`
- Integration target: `main`
- CI observer: mailbox `/tmp/dough-ci-501/watch-W7emfp`; workflow `ci.yml`
  (`donut CI`); repo `nerds-odd-e/doughnut`; branch
  `quick/122-publish-accumulated-local-commits`

## Goal and boundary

Publish multiple commits, each of which may mix already supported local edits,
without rewriting history. Donut gets the final notebook with the correct
identities and learning data; clone/pull receives the original complete chain.
Accept the whole range or leave the previous head and application state intact.

One bound notebook, matching live/accepted state, and a linear range from accepted
A to proposed T. Reuse the existing bundle endpoint, expected-head check and
single-commit behavior. No count limits or branch-per-mixture design.

Included: content edits, note additions/deletions, folders and Readmes represented
by added content, exact unchanged-content note renames/moves, and exact same-name
folder relocations. Compose those operations within and across commits. A
represented folder added earlier in the range is available to later operations.
Existing naming, ownership, final-content and identity constraints still apply.
An old handler's exclusivity is not a product constraint.

Excluded: new rename-with-edit inference, guessing ambiguous identities,
existing-Readme editing, `.keep`, trash compatibility, divergent reconciliation,
invalid-draft completeness, new transport/storage/receipts, background indexing,
and performance work. Those remain with the source story's siblings or ADR.
Deferred behavior need not be artificially rejected. Preserve existing safe
ambiguity refusal; do not infer delete/add just to accept a mixture.

## Keep the design small

Follow [One final publication result](../../NORTH-STAR.md#one-final-publication-result)
and the owner's final-only direction in
[Proposed ADR 0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization.md#apply-one-final-projection-atomically).
This story does not implement or approve the full ADR.

The final tree already exists in Git. Do not build another notebook model.

1. Verify the range and existing accepted projection.
2. Use the current tree comparison and exact correspondence rules. Where
   history matters, carry only each path's original accepted identity (or that
   it is new) through adjacent changes. Track folder correspondence the same way.
3. Read final content from T; apply surviving additions, resolved removals,
   final placements and edits once through the existing domain owners.
4. Check final Portable equality and accept the original bundle/head in the
   existing transaction. Do not persist or index intermediate revisions.

This is an evolution of the current publisher, not a new publication framework.
Introduce a small record/map only when a slice needs it. Do not preallocate a
class per operation, separate single/multi-commit pipelines, an operation log,
shadow entities, or a generic simulator. Remove replaced orchestration within
its owning slice. One-commit publication uses the same final application.

**PFE carried forward, with unnecessary preparation removed:**

- `NotebookGitProposalPublisher` already compares A/T, reads content from the
  proposed tip, locks state and accepts atomically. Reuse that for the first
  content-range behavior; no new final-content abstraction is needed.
- `NotebookGitProposalTreeShape` and `NotebookGitProposalFolderShape` own raw
  changes and exact correspondence. Change their whole-proposal restrictions
  where composition requires it, without adding a recognizer for each mixture.
- `NotebookGitProjection` / `NotebookGitAcceptedTree` bind accepted paths to
  identities and compare Portable trees. Extend their existing representation
  only enough to retain origin across a range.
- `NotebookGitProposalDocumentApplication` / `FolderMaterialization` already
  create added content and its folders. Supply the admitted additions alongside
  other changes; do not replace them with a new materialization subsystem.
- Keep content/index persistence, permanent deletion, folder creation and final
  placement rules with their existing shared domain owners. Validate final
  placement rather than replaying intermediate moves through those owners.
- Bundle import/write, binding persistence and CLI submission already transport
  full history. Reuse them. CLI receive/rebase analysis is a different purpose;
  do not copy its policy into a new cross-language synchronization engine.

Accepted ADRs remain applicable:
[0001](../../../docs/adrs/0001-ubiquitous-language.md) for domain identities,
[0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md) for
Portable content, [0006](../../../docs/adrs/0006-failure-handling-accepted.md) for
failure handling and [0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
for isolated tests. No schema, storage boundary or Accepted ADR change is planned.

## Slices

Target about 5 minutes per leaf including focused proof and cleanup; estimates
are hypotheses. Mandatory full-backend/E2E startup and test waiting is the only
sizing exception: record wait separately. More than 10 minutes of active work
requires finer decomposition, not another special case or a larger framework.
Each completed leaf must be green and preserve existing supported publication.

### 1. Publish several content-edit commits in one request
Type: Behavior
Status: done
Behavior: B and C each edit existing notes → publish once → Donut and a receiving
checkout contain C's contents and the original A → B → C history.
Change: Relax both ancestry guards to verify a whole single-parent range; keep
using the publisher's existing final-tree content application. Update obsolete
one-commit rejection tests and this path's rewrite advice. Other not-yet-handled
ranges remain safely refused until their slices, not partially applied.
Proof: Existing controller/CLI entry points observe final IDs/content and one
bundle submission without local mutation. Extend one installed-CLI round trip
in `cli_notebook_existing_note_edits.feature` to verify original SHAs/parents
and final files. Ancestry examples include a merge below a single-parent tip.
Estimate: 5 minutes active work; reuse existing commit/bundle and E2E helpers.
Learnings: Backend and CLI first-parent walks accept contiguous A..T; tip content
application unchanged. Publish cannot reuse pull's `rev-list … --not accepted`
seam when the accepted SHA is absent from the checkout. Merge tips / unrelated /
stale still refuse. Controller helper `ContentEditRange` and E2E
`readCheckoutStateAt` keep proof focused.

### 2. Accept new history with an unchanged final tree
Type: Behavior
Status: done
Behavior: B edits a note and C undoes that edit without deleting it → publish →
C is accepted with unchanged identities and the original history.
Proof: Controller plus downloaded bundle observes equal A/C trees and new head C.
Do not confuse identical trees with identical commits. Deletion/recreation is
not decided by this content-only example.
Estimate: 3–5 minutes active work.
Learnings: Empty tip-vs-accepted document changes now reuse projection match +
`acceptMatchingProposedTree` instead of refusing “no changed file.” Kept that
seam separate from addition-only ahead of slice 3. No same-tree identity shortcut.

### 3. Compose additions with existing-note edits
Type: Behavior
Status: done
Behavior: B adds a folder/Readme and notes while editing an existing note; C
edits a newly added note → publish → the complete final content is available.
Change: Feed additions to existing document application alongside ordinary
edits; stop treating addition-only shape as an exclusive application mode.
Proof: Controller tests assert new final content/derived references, retained
existing identities and represented root/nested destinations as data variations.
Reuse current creation and initial-tree tests. No new materializer architecture.
Estimate: 5 minutes active work.
Learnings: Removed exclusive `isAdditionOnly` branch. Admission of container
Readme additions + concept note changes lives in
`NotebookGitProposalTreeShape.requireAdmittedShape`; publisher applies admitted
additions through existing document application alongside MODIFIED. Reserved
Readme / rename-into-README refusals preserved.

### 4. Leave no live entity for a new note removed before publication
Type: Behavior
Status: done
Behavior: B adds a note, C edits it and D removes it while another edit survives
→ publish → only surviving final notes exist; all original commits remain.
Proof: Controller and downloaded history observe final absence and surviving
content. Review the application path to establish that the temporary note was
never persisted/indexed; final absence alone does not prove this architecture.
Estimate: 3–5 minutes active work.
Learnings: No production change. A→T tip comparison already nets add-then-delete
out of the admitted document set, so Temporary never reaches persist/remove.
Controller proof covers live absence, tip files, and A→B→C→D with Temporary only
on intermediate commits.

### 5. Retain original note correspondence independently of final content
Type: Structure
Status: done
Change: Adapt the existing note-change representation to carry its accepted
origin separately from final path/blob. Existing single-commit moves use it.
This is the minimum preparation for slice 6; no general operation hierarchy.
Proof: Existing rename/move, companion-edit, identity preservation and ambiguity
rejection tests remain green. Classification and correspondence perform no writes.
Estimate: 5 minutes active work; change existing representation, not all owners.
Learnings: `NoteChange` now carries `NoteOrigin(path, blobId)` instead of
`fromPath`; rename application uses `origin().path()`. `origin.blobId` is unused
until slice 6 when final blob may differ from the accepted origin blob.

### 6. Compose exact note moves with subsequent edits
Type: Behavior
Status: done
Behavior: B renames/moves a learned note unchanged with unrelated edits; C edits
that note → publish → its final path/content retains the original identity.
Change: Carry the existing exact correspondence through adjacent changes and
apply only its final placement/content. A later exact move follows the same
rule, including a destination represented by earlier additions in the range.
Proof: Controller checks retained note/tracker/question/conversation identities
and final content. Reuse final-path placement tests. A same-transition changed-
content move remains outside current inference; no endpoint-equality shortcut.
Estimate: 5 minutes active work after slice 5; refine if the origin map starts
needing simulated entities or copied domain rules.
Learnings: Exact-move origins walk adjacent first-parent steps via shared
`NotebookGitProposalAncestry.firstParentRange`; tip RENAMED keeps accepted
`NoteOrigin` when tip bytes differ. Publisher applies additions before renames
and persists tip content when blob ≠ origin. Placement accepts tip-represented
destinations. Same-transition changed-content moves still refuse. No simulated
entities.

### 7. Compose resolved removals with edits and additions
Type: Behavior
Status: done
Behavior: B deletes a learned note and edits another; C adds a different note
→ publish → final content includes the addition and excludes the deleted identity.
Change: Derive surviving origins/removals from the same correspondence and call
existing permanent removal once. No per-commit application or undo inference.
Proof: Reuse FK-complete deletion fixtures and committed-state assertions for
the removal closure, surviving learning data, and unchanged authored referrers.
Ambiguous removed/added pairs within one transition are not newly guessed.
Estimate: 5 minutes active work.
Learnings: Residual DELETED+ADDED refusal now runs per adjacent first-parent
step during origin walk, not on tip A→T net. Tip net deletion + later unrelated
addition publishes once via existing `permanentlyRemove`. Same-transition
unmatched remove/add still refused.

### 8. Allow an exact folder relocation alongside unrelated edits
Type: Behavior
Status: done
Behavior: A commit moves a complete unchanged same-name subtree and edits an
unrelated note → publish → both changes appear with Folder/Note IDs retained.
Change: Separate exact subtree correspondence from the current no-other-changes
check; feed the residual note changes through the existing application. Reuse
folder placement validation and final equality. No general folder graph engine.
Proof: Controller extends current relocation tests with companion edits; retain
partial-subtree, ambiguous mapping and invalid final-destination refusals.
Estimate: 5 minutes active work; this is narrower than the old combined
folder-refactor-plus-history work.
Learnings: Exact subtree correspondence no longer requires exclusivity;
`residualOutside` feeds companion edits through `requireAdmittedResidualShape`
after relocation. Partial-subtree / ambiguous / invalid-destination refusals
kept; obsolete mixed-change refusal removed.

### 9. Compose folder relocation across a mixed commit range
Type: Behavior
Status: planned
Behavior: B relocates a represented subtree; C edits a descendant and adds a note
→ publish → the final subtree/content retains the correct original identities.
Change: Carry folder origins and descendant paths through the same range
correspondence. Repeated or independent exact relocations follow that rule;
new folder rename/inexact-subtree inference is not included.
Proof: Controller checks final hierarchy, identities and content, including a
parent represented earlier in the range. Extend one folder-relocation CLI
round trip with mixed commits and verify the original history/final files.
Review that each existing folder is placed only at its resolved final location.
Estimate: 5 minutes active work after slice 8. Stop and decompose if final
collision handling requires more work; do not introduce per-mixture branches.

### 10. Settle deletion followed by recreation
Type: Behavior
Status: awaiting product answer
Decision: The previous question remains unanswered: deletion in B followed by
creation at the same path in C could use a new identity, matching today's
separately published operations. This remains a recommendation, not a decision.
Conditional behavior if confirmed: publish → a new note exists and the old
identity/learning closure is removed, even when final path/bytes equal A.
Proof: Controller checks IDs and complete dependent removal, not just tree
contents. No intermediate entity or inferred undo is introduced.
Estimate: 3–5 minutes active work after the answer. Do not execute this leaf or
claim the full composition promise complete while the policy is unresolved.
The answer may change this leaf; silence authorizes neither deletion nor a new
preservation/refusal rule.

### 11. Preserve atomic rejection of the composed range
Type: Behavior
Status: planned
Behavior: Final validation or the existing late binding-save seam fails for a
mixed range → publish → accepted history and committed application state remain A.
Proof: Extend the existing atomic controller tests with resolved move/add/delete
content; observe bundle, IDs, dependent rows and derived state after rejection.
Reuse the existing transaction, not a new compensation mechanism.
Estimate: 3–5 minutes active work.

### 12. Retry an accepted range without repeating its effects
Type: Behavior
Status: planned
Behavior: Response is lost after T is accepted → retry while T is still current
→ report T with unchanged IDs and no repeated creations/removals.
Proof: Extend existing idempotent controller and CLI transport/retry fixtures.
Check the local chain/files remain intact. No receipt lookup for historical tips.
Estimate: 3–5 minutes active work.

### 13. Preserve a competing accepted web save
Type: Behavior
Status: planned
Behavior: Web save wins while the local range still expects A → publish → stale
rejection preserves the winning web history and the unmodified local range.
Proof: Extend current concurrency and CLI stale-response fixtures to multiple
commits. No rebase, queue, or automatic resubmission against a changed head.
Estimate: 3–5 minutes active work.

## Proof and execution discipline

| Promise | Owner |
| --- | --- |
| Mixed commits, one publication, original history and final content | 1; mixed additions 3; moves 6; mixed folder range 9 |
| Same final tree with new history | 2 |
| No temporary live entities / one final application | 4 and structural review in 5/6/9 |
| Correct existing/new/removed identities | 3/6/7/9; recreation policy 10 |
| Whole-range atomicity, safe retry, stale protection | 11/12/13 |
| Current naming, Portable format, authorization, drift and ambiguity rules | Existing boundary tests at affected slices; ancestry 1, correspondence 5/8, final rejection 11 |

Reuse `NotebookGitBundleControllerTestBase.commitOnTopOf` and its bundle helpers,
`makeMe`, committed-transaction fixtures, `dependentCounts`, and the existing
rename/relocation/deletion tests. Test through controllers and CLI `run`, not
by injecting a pre-resolved mapping. Unit tests own identity/error variations;
the two CLI round trips own actual publication/receipt. Do not repeat the whole
scenario matrix at every layer. Existing full-bundle persistence and transaction
proofs remain applicable; no new storage-engine assumption is introduced.

During authorized execution, run the relevant commands:

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only
CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookPublish.test.ts tests/notebookPull.test.ts
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_existing_note_edits.feature
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_folder_relocation.feature
```

Backend changes require the full backend suite under the repository rule. Use
the execution checkout's owned disposable test/E2E allocation (ADR 0007).
No application tests were run during planning/refinement. Tests and code were
inspected; the existing Git research is not application acceptance evidence.

Follow dough-execute-plan when execution is authorized: Jidoka → fresh
post-change-refactor agent → API generation only if triggered → coordinator
runs `./scripts/run.sh pnpm format:changed` once → plan update → commit with
check-only hook → push and asynchronous CI observation. Keep one plan writer;
retain evidence for retrospective and let story wrap-up own cleanup.

## Refinement assessment

Replaced all 15 unstarted leaves with these 13. Removed the unnecessary upfront
content/application abstraction and addition-materialization refactor. Narrowed
folder work to companion changes first, then range correspondence. Kept the
one small note-origin Structure immediately before its Behavior. Reduced
repeated E2E requirements while preserving proof ownership for every promise.
No completed work or execution evidence existed to remove.

The solution still reuses the original PFE owners and North Star. Git owns the
final tree; a small origin mapping provides identity evidence; existing domain
owners apply the final result once. If implementation starts needing another
notebook model or an operation pipeline per mixture, the approach has drifted.

Slices 6 and 9 retain integration risk around correspondence/final placement,
but their work is now bounded to extending an existing representation and
existing placement rules. The active-work limit remains binding; the timing
exception covers mandatory test waiting only. Slice 10 remains non-executable
until its domain answer arrives. Execution itself has not been authorized.
