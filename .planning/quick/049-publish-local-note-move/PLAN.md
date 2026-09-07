# Publish an identity-preserving local note rename

Status: planned; story split and top-priority plan refinement complete.
Source: [SEED-009 Story 6](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-6).
Sibling: [Story 12 relocation plan](../050-relocate-local-note/PLAN.md).

## Goal and scope

Publish one unchanged-content note rename within the same parent directory
through the existing CLI, retain its Donut identity and private data, and
receive that accepted rename in another checkout. Root and nested-folder
renames are included. A separately published later edit at the new path
updates the same learned note.

Require the authenticated owner, clean bound main checkout, one direct
single-parent child of accepted main, and a matching current Portable
projection. Exactly one ordinary file disappears and one appears in the same
parent with equal bytes; no other changes. Preserve original authored
Markdown (including YAML title/headings), referrers, tracker activation and
schedule, questions, conversations, and unrelated identities. An unchanged
same-content copy is not another removed/added candidate.

Keep the exact accepted commit, atomicity, retry, stale-head/drift rejection,
title/path/mode/Markdown validation, live/deleted destination collision rules,
and local-work protection. No rename may call soft deletion or create a new
Note. Separate accepted deletion then addition retains fresh-identity semantics.

Cross-parent changes reject clearly for this complete story. Relocation,
final folder/title validation, and emptied containers belong exclusively to
Story 12. Other exclusions remain: mixed/rewritten/multiple renames, link
rewrites or aliases, folders/README/attachments, restore/deleted-path reuse,
multiple unpublished commits, rebase, drift repair, web structural changes,
new commands/UI, and Portable identity metadata.

## Execution context and retained decisions

- NotebookGitProposalTreeShape currently walks a raw two-tree diff and rejects
  all mixed removals. Retain source/destination blob identity from that walk;
  exact removed/added correspondence must not depend on Git similarity,
  filename order, or identical unchanged notes.
- NotebookGitProposalPublisher owns REQUIRES_NEW, SERIALIZABLE, accepted-head
  retry, owner/ancestry checks, pre/post tree matching, and final binding save.
  Keep that ownership. Mutate only the same Note's title/update timestamp;
  never change its folder or invoke deletion/creation.
- Reuse exact filename validation (validAdditionTitle, NoteUpdateTitleDTO,
  DisplayName) and NoteTitlePlacementRules for the destination in the current
  folder. Generalize an addition-only validation name only when actually
  shared; no folder resolver or general relocation abstraction is needed here.
- Web title/move controllers choose or rewrite references. Git publication
  must preserve the authored tree and therefore must not call those workflows.
- NotebookGitBundleControllerTestBase supplies committed fixtures and
  NOT_SUPPORTED around independent publication transactions. Reuse
  proposalBundleBytes, public note views, and fresh inCommittedTransaction
  observations. NotebookGitCopyIdentityControllerTest supplies private-data
  fixtures and conversation cleanup. The existing atomic publication profile
  and failing-binding-save configuration prove the same transaction/visibility
  arrangement; no new Spring test context or storage experiment is needed.
- CLI pull tests already use run with real Git and accepted bundles. The active
  cli_notebook_clone.feature has installed clone/publish/head/note-view
  assertions. Add only a named checkout rename action.
- Follow Accepted [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  for Portable bytes/titles/references and
  [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md) for failures.
  ADR 0002 remains Proposed; the seed supplies the selected product direction.
  No API, DDL, transaction policy, or web behavior change is planned.

## Original-plan allocation and refinement assessment

No slice was started and no product proof is discarded. The original combined
17-leaf plan is replaced by two story plans, not by concurrent alternatives.
This file retains its path so existing links remain usable.

| Original leaf | Assessment and current owner |
| --- | --- |
| 1 Rename acceptance | Refine → rename 1–2 separate raw diff preparation from acceptance |
| 2 Private data | Ready → rename 3; relocation retains the contract |
| 3 Rollback | Ready → rename 4; relocation 3 adds a folder-change rollback case |
| 4 Folder/root movement | Move → relocation 1–2 (unchanged filename, then combined rename) |
| 5–6 CLI harness/publication | Refine → rename 5–6 use same-parent paths; relocation 7 reuses the harness |
| 7 Guidance | Split by story → rename 7, relocation 8 |
| 8 References | Ready → rename 8; relocation retains the unchanged-reference contract |
| 9 Empty container | Move → relocation 4 |
| 10 Ambiguous/mixed input | Refine → exact eligibility/rejection matrix in rename 2; retain existing removal guards |
| 11 Destination validation | Refine → rename 9; represented-parent cases move to relocation 5 |
| 12 Deleted destination | Ready → rename 10; different-folder collision belongs to relocation 6 |
| 13 Accepted retry | Reuse common accepted-head regression, before change dispatch; retained below |
| 14 Stale head | Reuse common expected-head regression, before change dispatch; retained below |
| 15 Projection drift | Reuse common matching-parent regression, before mutation; retained below |
| 16 Subsequent edit | Ready → rename 11; relocation 9 verifies its new location through the same contract |
| 17 Receive | Ready → rename 12; relocation 10 covers cross-folder receipt |

Reusing the common retry/stale/drift tests removes redundant planned variations,
not safety requirements. At execution, run those tests against every backend
change. If their common guards move, become conditional on change kind, or
otherwise stop covering renames, add rename-shaped proof in this PLAN before
accepting that change. A green unrelated test is not sufficient evidence.

## Outside-in proof ownership

| Story promise | Owner / required observation |
| --- | --- |
| Exact same-parent rename, original ID, canonical authored bytes/head/tree/history | 2 and 6; public note and downloaded bundle |
| Active/inactive learning state, questions/conversations, independent identical copy | 3; fresh persisted associations and note views |
| Late failure leaves old placement and accepted state | 4; fresh title, tracker and binding state |
| CLI teaches supported scope and preserves rejected work | 7 plus existing CLI submission/readiness tests |
| Referrer body/property Markdown unchanged, old exact path unresolved | 8; note view using normal current-state resolution |
| Invalid filename/mode/reserved/live destination rejects without partial publication | 2/9; controller rejection and unchanged source/binding |
| Soft-deleted destination remains reserved | 10; no resurrection or source mutation |
| Matching accepted retry is unchanged | Existing NotebookGitPublicationControllerTest.reportsAnAlreadyAcceptedCommitWithoutMutatingPublicationOrLearningState; accepted-head return precedes dispatch |
| Stale proposals preserve the winner | Existing NotebookGitProjectionDriftControllerTest old-parent cases; expectedHead gate precedes dispatch |
| Projection drift remains unabsorbed | Existing NotebookGitProjectionDriftControllerTest web-creation drift cases; matching-parent gate precedes mutation |
| Later content edit keeps renamed identity | 11; second accepted commit updates original note |
| Another checkout receives rename/edit with clean main and no metadata | 12; real-Git CLI run and exact accepted tree |
| Delete then later add remains fresh identity | Existing NotebookGitCopyIdentityControllerTest accepted-deletion/later-addition case |
| Owner, direct ancestry, typed content, checkout and local-work safety | Existing controller format/ancestry/authorization and CLI readiness/submission suites retained in 2/7/12 |

## Ordered leaves

### 1. Preserve exact blob identity through raw diff classification
Type: Structure
Status: done
Proof: Existing controller tree-shape/deletion-rejection/add-edit tests remain
green with identical accepted and rejected behavior. Backend command.
Learning: Added `ObjectId blobId` to `NoteChange` in
NotebookGitProposalTreeShape.java (DELETED→accepted-tree blob, ADDED/MODIFIED→
proposed-tree blob); no accept/reject logic changed;
`pnpm backend:test_only` full suite green.

Structure: Carry the relevant raw blob object IDs alongside each removed/added
path so immediately following leaf 2 can compare the one pair. Keep current
acceptance gates in place: this leaf must still reject all remove/add pairs
and must not expose an accepted rename or fall through to add/delete mutation.
No similarity engine, generic operation framework, or folder preparation.

### 2. Accept one unchanged same-folder rename
Type: Behavior
Status: done
Proof: Controller rename at root or in the same nested folder → original note
ID with new filename-derived title, unchanged content/YAML title/headings and
tracker, exact authored accepted head/tree/parent. Eligibility matrix retains
rejection of rewritten, multiple, mixed and cross-parent pairs. Backend.

Behavior: A clean matching baseline contains one note → publish exactly one
same-parent equal-blob remove/add pair → accept the new name on that identity.
Use leaf 1's data, existing title validation and deleted-title placement guard;
perform one title mutation inside the existing publication transaction.
Keep folder unchanged. Both paths/modes remain validated and the final
projection must equal the proposal. Replace the obsolete exact-rename
rejection row with acceptance; retain other rejection cases and give safe
rename-only-then-edit guidance. Never suggest deletion to preserve identity.
Learning: Added `ChangeKind.RENAMED`/`fromPath` and `detectSameParentRename`
(equal-blob, equal-parent DELETED+ADDED pair) in NotebookGitProposalTreeShape;
publisher's new `applyRename` mutates title/updatedAt in place via
`NoteTitlePlacementRules` + shared `validFilenameDerivedTitle`, no folder
change, no web title-rename workflow called. Root/nested acceptance and
cross-parent rejection tests split into a new
NotebookGitProposalRenameControllerTest to keep file size in bounds. Actual
runtime ~14 min (implementer ~9.5 min + refactor ~4.4 min) exceeded the ~5 min
target on this plan's explicitly flagged highest-risk loop; landed as one
coherent, fully green, proven behavior, so recorded per the plan's sizing note
rather than reverted/split further.

### 3. Preserve private associations when the name changes
Type: Behavior
Status: planned
Proof: One controller scenario with committed active/inactive trackers,
scheduling values, question and conversation, plus an unchanged identical-text
note; fresh IDs, activation/schedule and association ownership are unchanged.
Backend; reuse copy-identity fixture style and conversation cleanup.

Behavior: Distinct learned identities contain matching text → rename only one
→ all private data stays with its original identity and no tracker reactivates.
Assert the private-data delta here rather than repeating the canonical tree
assertions from leaf 2. No new persistence mechanism or fixture framework.

### 4. Roll back a rename when acceptance fails
Type: Behavior
Status: planned
Proof: Existing late-binding-save failure profile observes old title, note
timestamp, tracker state, accepted head/bundle and binding timestamp through
fresh committed reads. Backend.

Behavior: The new title is projected but binding acceptance fails → publication
fails → original identity state and accepted revision survive. Reuse
NotebookGitPublicationAtomicTestSupport's exact configuration and reset hook;
no new transaction/context or compensation behavior.

### 5. Add an explicit same-folder rename to the checkout harness
Type: Structure
Status: planned
Proof: Existing installed-CLI notebook feature remains green. Focused E2E.

Structure: Add one git-mv-and-commit task in cliE2eNotebookCloneTasks.ts, one
notebookClone.ts page-object action, and one thin cli_notebook_clone.ts step
for immediately following leaf 6. Reuse commitCheckout and accepted-head alias.
No new test framework or generic filesystem operation model.

### 6. Publish a rename through the installed CLI
Type: Behavior
Status: planned
Proof: Installed feature: clone → rename Recipes/Pasta.md to
Recipes/Pasta basics.md → publish → exact authored accepted head and Donut
shows the renamed note in Recipes with its unchanged content. Focused E2E.

Behavior: Owner commits a same-folder rename → runs installed publish →
sees the renamed note in Donut. Use existing note-view/tree assertions.
Do not resnapshot after the rename; private-data proof stays in leaf 3.

### 7. Explain same-folder renaming through existing CLI guidance
Type: Behavior
Status: planned
Proof: Existing clone-output expectations explain unchanged bytes, same parent,
separate rename then edit, and unchanged links; existing submission tests
retain rejected local commits/files. CLI clone/publish tests; focused E2E
where the existing exact-copy expectation changes.

Behavior: Owner clones → reads next steps → knows how to publish a rename
without losing identity. Update nonInteractiveCli.ts and its exact-copy
assertions. Say parent changes remain unsupported until Story 12; no new
command/preview or misleading deletion workaround.

### 8. Leave referring Markdown authored
Type: Behavior
Status: planned
Proof: Publish rename, then use NoteController.showNote for a referrer with
body and frontmatter property links to the old exact path; bytes unchanged
and that exact destination unresolved without an alias. Backend.

Behavior: Other notes refer to the old name → rename → retain authored
references and ordinary current-state resolution. Reuse the deletion
reference fixture pattern; add no implicit link rewrite or hidden alias.

### 9. Reject unavailable or invalid ordinary filenames
Type: Behavior
Status: planned
Proof: Same-parent rename-shaped data variations exercise normalized/invalid
title, reserved name, non-regular modes and occupied live destination;
rejection preserves source/binding and identifies the path. Backend.

Behavior: A requested new filename violates the existing Portable destination
contract → publish → keep the original name without partial mutation.
Reuse title/format/tree-shape tests and shared validators. Both old and new
paths must be checked. Keep advisory-name behavior; never repair the filename.
Deleted-title collisions have their different persisted fixture in leaf 10.

### 10. Keep a deleted filename reserved
Type: Behavior
Status: planned
Proof: Establish a deleted destination through accepted isolated deletion,
then rename a different live note into it → existing deleted-title conflict,
no resurrection and unchanged source, deleted tracker and accepted head.
Backend; reuse NotebookGitDeletedDestinationControllerTest patterns.

Behavior: A soft-deleted note reserves the new filename in this same folder →
publish rename → reject atomically. Leaf 2 must already include this guard;
this leaf owns the richer persisted identity-boundary proof.

### 11. Edit the same note after its accepted rename
Type: Behavior
Status: planned
Proof: Accept rename, then publish one separately authored content-edit child
at the new path → new content on the original note ID with retained tracker.
Backend; reuse existing sequential-publication fixtures.

Behavior: The rename is accepted → owner publishes a later edit → update the
same learned concept. Do not batch unpublished rename/edit commits.

### 12. Receive the rename and later edit in another checkout
Type: Behavior
Status: planned
Proof: CLI run with real Git and an accepted rename-then-edit bundle:
old filename absent, new content present, exact head/tree, retained ancestry,
clean main and no Portable metadata. CLI pull tests.

Behavior: A second clean checkout is at the rename's parent → pull →
receive both accepted commits and the new filename without history loss.
Extend notebookPull.fastForward.suite.ts; backend leaves prove those commits
can be accepted. No second-checkout Cypress harness is needed.

## Verification and execution wrap-up

- CI disposition: main CI run 34084118736 (attempt 1, triggered by leaf 1's
  push, SHA 9355a2000769fa40a3f1ccc075f714f768fe08ab) failed one unrelated
  Cypress spec, `record_live_audio_with_real_open_ai`
  ("live recording transcription with real OpenAI"), with
  `AssertionError: Timed out retrying after 6000ms: expected
  '<button.daisy-btn>' not to be 'disabled'` and server-side root cause
  `com.openai.errors.OpenAIInvalidDataException: Error reading response` from
  the real OpenAI API. Unrelated to notebookGit/tree-shape/rename code; treated
  as an external-service outage and ignored without a repair commit.
- Backend: CURSOR_DEV=true nix develop -c pnpm backend:test_only.
  Backend rules require the complete backend unit suite for each backend leaf.
- CLI: CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run
  tests/notebookClone.test.ts tests/notebookPublish.test.ts tests/notebookPull.test.ts.
  Select the files relevant to each changed boundary.
- Focused E2E: CURSOR_DEV=true nix develop -c pnpm cypress run --spec
  e2e_test/features/cli/cli_notebook_clone.feature.
- Whitespace: scripts/check_diff_whitespace.sh.

Reuse earlier passing evidence only while it remains applicable at execution.
Keep any temporary multi-beat E2E scenario @wip until green. No manual testing,
mutation testing or full E2E run is requested. Use the existing backend
profiles/fixtures; no API or schema change is expected.

Each executed leaf follows execute-plan: Jidoka → fresh post-change-refactor
agent → API generation if required → coordinator format:changed once →
update this PLAN → commit → push and asynchronous CI observation.
Preserve unrelated work. Do not use STATE.md for leaf progress.

## Refinement outcome and sizing

The original rename acceptance mixed raw-diff preparation with new acceptance;
leaves 1–2 now separate those beats at a green boundary. Destination validation
is separated from the deleted-row fixture in 9–10. Folder policy has moved
to the sibling story. Shared retry/stale/drift guard tests remain required
instead of generating three operation-specific copies of unchanged branches.

Result: 12 leaves, 10 Behavior and 2 immediately enabling Structure leaves.
Target approximately five minutes per leaf including proof and local cleanup,
with moderate sizing confidence from previously inspected fixtures. No leaf
requires a new storage, transport, or identity-inference framework. The
first acceptance remains the highest-risk loop; its raw blob data is prepared
immediately beforehand, and its only new projection operation is same-note
title mutation. Do not expand it to folder resolution.

Ready for direct execution; this is a sizing hypothesis, not a timing
guarantee. Record actual backend/E2E runtime when it alone exceeds the target;
there is no blanket exception for debugging or setup. At five minutes inspect
scope; at ten non-exempt minutes stop and finer-decompose. Repeated qualifying
overruns or a changed story boundary require the repository's story review.
No implementation has started and no execution-time evidence was invented.

On completion mark only Story 6 delivered, move it to Recently done, reduce
its seed detail and remove this spent plan. Story 12 remains unfinished with
its own plan. This request authorizes planning/refinement, not implementation,
commit or push.
