# Publish an identity-preserving note relocation

Status: planned; story and slice-plan refinement complete on 2026-09-07.
Source: [SEED-009 Story 12](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-12).
Prerequisites: Story 6 and the [Plan 52 corrections](../052-clarify-note-rename-publication/PLAN.md)
are delivered. Ready to execute as the selected parallel candidate alongside
[Plan 53](../053-reconcile-local-web-content/PLAN.md), using the coordination
boundary below. This planning request does not start implementation.

## Goal and scope

An owner files one learned note in another existing represented folder, or at
the notebook root, through an ordinary local Git commit and installed CLI
publication. The filename may change in the same commit. The note retains its
identity/private data, authored content and links. A separate later content
publication and receipt in another checkout continue at the new location.

The proposal is exactly one removed and one added regular Markdown note with
the same raw blob, and no other changed file, on one direct single-parent
commit above accepted main. Destination eligibility is based on the accepted
parent: root, or an existing Donut folder represented by tracked content under
its full path, including nested/README-only folders and represented ancestors.
The destination's own README is not required. Validate final folder/title
together, with no intermediate placement.

The destination path must be absent from the accepted tree and the final
folder/title must not be reserved by a deleted note. Source and destination
containers survive, even when source becomes unrepresented after its last
tracked note moves. Do not create a README; returning to an unrepresented
source folder is then outside the supported destination boundary.

Retain same-parent rename, additions/edits/deletion, ownership/readiness,
expected-head/drift checks, exact accepted commit/tree, atomicity and retry.
Publish and accept the unchanged relocation before separately committing and
publishing a content edit. Authored body/property links stay unchanged; exact
old-path references can stop resolving while shorthand references may remain
valid. No reference repair or redirect is implied.

Exclude new/unrepresented folders, folder/README moves, changed content or
accompanying referrer edits, multiple pairs, overwrite/restore/deleted-name
reuse, cross-notebook moves, multiple unpublished commits, structural rebase,
drift repair, web move synchronization, new commands, or Portable metadata.

An intended overwrite onto an identical live target can appear as an isolated
deletion because the target's tree entry is unchanged. That existing deletion
behavior is not relocation and never transfers identity to the target. Do not
invent intent detection or promise all such filesystem commands are rejected.
A one-file directory move producing an eligible pair likewise moves the note,
not its Donut folder.

## Inspection and decisions affecting execution

- Story 6 delivered exact same-parent correspondence; Plan 52 delivered
  publication guidance at cc7b053424 and contextual destination errors at
  6c94b5f802. Reuse both; obsolete pending-Plan-52 branches are removed here.
- NotebookGitProposalTreeShape.detectSameParentRename recognizes the exact
  two-entry/equal-blob shape but rejects different parents. Generalize this
  existing representation/handler; retain full-diff checks, regular modes,
  Markdown/path constraints, and rejection of changed-content or mixed pairs.
  No second similarity matcher or identity route.
- NotebookGitProposalPublisher.applyRename changes the managed original Note.
  It currently checks a deleted title against note.getFolder(). Resolve the
  final folder first, then check the final title there, and set folder/title in
  the existing publication transaction. Reuse withContext for the final path,
  preserving error type, fields, deletedNoteId and cause.
- NotebookGitProjection.requireRepresentedFolderIdForAddition already selects
  full folder paths and checks tracked accepted content beneath them. Share
  and capability-name that resolver for relocation when needed; keep additions
  working and avoid duplicate folder lookup. Root resolves to null.
- Publisher is currently 250 lines. Its reusable filename-title validation is
  a concrete extraction opportunity before adding placement logic (leaf 1).
  Keep it one representation across addition and relocation. Do not move the
  transaction owner or introduce a broad publication framework.
- NoteMotionService/web movement includes different placement sequencing and
  reference behavior. Do not call it, delete/recreate notes, flush an
  intermediate placement, normalize invalid filenames, or rewrite authored
  links to implement this publication.
- Reuse controller fixtures in NotebookGitProposalRenameControllerTest,
  NotebookGitFolderNotePublicationControllerTest,
  NotebookGitProposalRenameRollbackControllerTest,
  NotebookGitDeletedDestinationControllerTest,
  NotebookGitDeletionContainerPublicationControllerTest and
  NotebookGitRenameThenEditControllerTest. Existing exact transaction/profile
  and failure reset apply. No DDL, API or transaction change or new storage
  uncertainty calls for an experiment.
- The existing rename referrer test changes an unqualified title. A same-title
  folder move needs an exact old-path fixture; that test alone does not prove
  the relocation reference promise (new leaf 9).
- The installed git-mv task accepts source/destination paths already. Reuse it
  and current note-view assertions. The accepted-history fast-forward suite
  already contains a rename-then-edit sequence; add cross-parent data there,
  without changing production pull/rebase behavior.
- Follow Accepted [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  for representations, authored references and empty folders, and
  [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md) for contextual
  business errors. ADR 0002 remains Proposed. No new architectural decision.

## Parallel execution boundary

Plan 50 and Plan 53 have independent product outcomes, but share some files
and test infrastructure. Keep separate worktrees and PLAN ownership; separate
worktrees alone do not isolate MySQL, Redis or the running SUT.

| Area | Coordination |
| --- | --- |
| Backend relocation | Plan 50 owns classifier/placement/resolver changes and relocation fixtures. Plan 53 should reuse the publication API; coordinate if its proof uncovers a required backend change. |
| CLI rebase | Plan 53 owns production pull/readiness/rebase changes. Plan 50 uses existing publish and fast-forward receipt; do not broaden or weaken divergent-history rejection. |
| Shared CLI guidance | Both plans touch nonInteractiveCli.ts and notebookClone.test.ts. Serialize/integrate these edits: keep relocation guidance and content-only pull guidance together, with neither claiming structural rebase. |
| Installed feature/harness | Both use cli_notebook_clone.feature and associated page objects/tasks. Plan 50 needs only existing git-mv/publish actions; retain Plan 53's pull observations and scenarios when integrating. |
| Pull tests | Plan 50 extends notebookPull.fastForward.suite.ts. Plan 53 owns divergent suites/registration. Keep suite registration serialized to preserve temporary-directory leak checks. |
| Planning | Update only Story 12 and this PLAN from this workstream. Leave Plan 53 and Story 8 to their executor; reconcile the shared seed/backlog at integration. |
| Verification | Serialize backend/E2E runs against a shared test DB/SUT, or use explicitly separate service instances. Do not run destructive test setup concurrently against the same databases. |

Before merging the second completed change, reconcile shared files and run
the combined affected CLI suites, backend suite if backend boundaries changed,
and focused installed feature on the integrated source. Verify sequential
receipt of relocation remains supported and Plan 53's divergent structural
history remains rejected. This is integration verification, not another story
or a requirement that Plan 53 finish before Plan 50's backend work can start.
Global backlog order remains unchanged.

## Promise ownership and refinement mapping

The former ten leaves are retained in scope and mapped below. No slices were
completed. Original leaf 1 bundled title-validation extraction, placement and
private-data proof; these are now leaves 1, 2 and 4. The old inherited reference
claim needs relocation-specific proof in leaf 9. Other leaves are tightened
around existing boundaries rather than duplicated.

| Promise / original leaf | Current owner and observable proof |
| --- | --- |
| One note at root/existing/nested/represented destination; exact commit / old 1 | 1 enables 2; controller note placement and downloaded head/tree |
| Final folder/title only, including intermediate collisions / old 2 | 3; accepted final location despite either hypothetical intermediate collision |
| Same identity/private state, untouched identical copy / old 1 | 4; fresh note/tracker/question/conversation relationships |
| Late-failure rollback / old 3 | 5; fresh original folder/title/timestamps/tracker and accepted binding |
| Emptied source container retained / old 4 | 6; original folder ID and exact Portable tree, no invented README |
| Missing/unrepresented destination / old 5 | 7; path-specific rejection with original source/folders/binding |
| Deleted final destination unavailable / old 6 | 8; final path/error semantics, source and deleted identity intact |
| Authored links / inherited claim | 9; unchanged body/property bytes and old exact-path resolution |
| Installed publication / old 7 | 10; accepted authored head and Donut note at the new path |
| Correct relocation guidance / old 8 | 11; actual clone output and aligned installed expectation |
| Subsequent edit retains identity / old 9 | 12; same note/tracker with separately accepted new content |
| Second checkout receives accepted history / old 10 | 13; exact real-Git head/tree/ancestry and clean main |
| Existing rename, malformed/mixed shapes, occupied-live cases | 2/3/8 retain relevant controller regressions; no new overwrite mechanism |
| Owner/readiness, retry/stale/drift and ordinary add/edit/delete | Existing controller and CLI proofs; rerun when the owning boundary changes |

A live occupied path cannot be the added side of an eligible pair. A deletion
plus modification stays rejected by complete-diff rules. An unchanged identical
target stays its own identity under existing isolated-deletion rules. Leaf 8
checks those boundaries rather than advertising an unobservable move intent.

Inspected tests are reusable evidence, not fresh green runs. Map new assertions
to the promise they establish and keep canonical assertions once. If changed
logic invalidates inherited proof, add the relevant observation before accepting
that leaf. Commands passing without the promised observation are insufficient.

## Ordered leaves

### 1. Keep filename-derived title validation in one place
Type: Structure
Status: planned
Proof: Existing controller invalid/normalizable title, addition and same-parent
rename behavior remains green. Backend suite.

Internal change: Extract the existing filename-derived-title validation from
the 250-line publisher into a focused capability-named collaborator, keeping
addition and rename callers on the same validation implementation. Preserve
messages and normalized-title rejection. Do not move transaction ownership,
add behavior or duplicate contextual error machinery.
Immediate next Behavior: leaf 2's destination assignment fits coherently in the
publisher without hiding a preparatory refactor in that implementation.
Sizing: ~5 minutes active work, medium confidence; one extraction/proof loop.

### 2. Relocate an unchanged note to an existing folder or root
Type: Behavior
Status: planned
Proof: Controller placement variants root→folder, folder→root, folder→folder,
and nested README-only destination with duplicate folder basenames. Observe
original note ID, final ancestor folders, unchanged bytes and exact proposed
head/tree. Assert full canonical shape once. Backend suite.

Behavior: An isolated equal-blob pair changes parent, keeping filename →
publish → put the same note at the full eligible destination.
Generalize the existing classifier/handler and accepted-parent resolver;
validate destination eligibility and deleted title before assigning the folder.
These guards are required now; leaves 7/8 add focused boundary proofs.
Temporarily reject combined parent/filename changes until leaf 3. Replace the
old cross-parent-rejection fixture with acceptance; retain mixed/content-changed
rejections. Rename helper/comments to match actual supported capability.
Sizing: ~5 minutes active work, medium confidence after leaf 1; existing resolver
and transaction keep this one placement loop. Scrutinize at five minutes.

### 3. Relocate and rename using only the final destination
Type: Behavior
Status: planned
Proof: Controller variants with a deleted title at the source/new-title or
destination/old-title hypothetical intermediate; final folder/title is free and
the original note appears there. Backend suite.

Behavior: The isolated pair changes folder and filename → publish → accept
only the final validated placement. Remove leaf 2's temporary filename limit.
Do not call sequential web rename/move or flush an intermediate state. Update
any temporary messages/fixtures that still reject this now-supported pair.
Sizing: ~5 minutes active work, high confidence; same placement path.

### 4. Retain private associations across the folder change
Type: Behavior
Status: planned
Proof: Extend the delivered private-identity fixture with cross-parent data;
fresh reads retain original tracker state and note-owned question/conversation
IDs, while an untouched identical-content note retains its own associations.
Backend suite.

Behavior: A learned note is relocated → publish → its private state continues
to belong to that same note. Reuse committed fixtures and conversation cleanup;
do not introduce mocks or a new transaction profile. This closes the wider
private-data proof outside leaf 2's focused placement loop.
Sizing: ~5 minutes active work, medium confidence; existing fixture variant.

### 5. Roll back a failed folder change
Type: Behavior
Status: planned
Proof: Relocation-plus-rename variant of the existing late binding-save failure
fixture. Fresh original folder/title, note timestamp and tracker state, and
accepted head/bundle/timestamp remain. Backend suite.

Behavior: Folder/title mutation occurs but acceptance fails late → publication
fails → original placement and accepted revision survive. Reuse the exact
notebook-git-publication-atomic-test profile, failure injection and reset.
No new transaction or compensating write mechanism.
Sizing: ~5 minutes active work, high confidence; one rollback fixture variant.

### 6. Retain a source folder after its last tracked note moves
Type: Behavior
Status: planned
Proof: Source has no README and contains only the moved note → publish →
original source/destination folder IDs survive; exact accepted Portable paths
omit the now-empty source and contain no invented README. Backend suite.

Behavior: Relocation empties the source's tracked content → publication →
retain the Donut container. Use the existing deletion-container fixture
pattern. Do not reinterpret the single note pair as a folder rename.
Sizing: ~5 minutes active work, high confidence; one container outcome.

### 7. Reject destinations absent from accepted content
Type: Behavior
Status: planned
Proof: Missing Donut folder and existing unrepresented folder, including a
source emptied by leaf 6's accepted move, yield a path-specific rejection;
source, containers and accepted binding remain unchanged. Backend suite.

Behavior: Local path implies an ineligible destination → publish → reject
without creating a folder or manufacturing representation. Also retain a
successful represented-ancestor variant with tracked content only below a
descendant: an own README is not a new requirement. Reuse the existing
represented-parent resolver fixtures and final-placement handler.
Sizing: ~5 minutes active work, medium confidence; one eligibility policy loop.

### 8. Preserve an unavailable final destination
Type: Behavior
Status: planned
Proof: A prior accepted deletion reserves the target folder/title → relocating
there returns final Portable path, original conflict type/fields/deletedNoteId
and cause; source, deleted identity and binding remain unchanged. Backend suite.

Behavior: Final placement is reserved → publish → reject atomically using the
destination's title rule and the delivered contextual-error behavior.
Retain live-target mixed-diff rejection and isolated-deletion regressions;
never add a heuristic that treats an unchanged identical target as a new move.
Do not resurrect, overwrite or transfer private associations.
Sizing: ~5 minutes active work, high confidence; deleted-destination fixture.

### 9. Leave exact-path referrers authored across relocation
Type: Behavior
Status: planned
Proof: Controller fixture resolves a source-note Portable path from both body
and YAML before publication; after moving that note, authored referrer bytes
remain exact and the old exact path is no longer resolved. Backend suite.

Behavior: Referrers name Inbox/Cell, with target moving to Biology/Cell →
publish → retain authored links and resolve against current placement.
Use path-qualified references; the old unqualified-title rename fixture does
not establish this. Retain existing shorthand resolution behavior without
asserting that every link must break. No reference rewrite or new alias system.
Sizing: ~5 minutes active work, medium confidence; one observable reference loop.

### 10. Publish relocation through the installed CLI
Type: Behavior
Status: planned
Proof: Existing installed feature: clone → git mv Recipes/Pasta.md to
Pasta basics.md → publish → authored accepted head and unchanged-content note
at the notebook root in Donut. Focused installed feature.

Behavior: Owner commits an eligible relocation/rename → installed publish →
sees the note at its final location. Reuse existing git-mv task/page objects
and note-view assertions. Snapshot only the initial fixture baseline; never
resnapshot after the action under test. Coordinate shared feature edits with
Plan 53; no additional harness or rebase path.
Sizing: ~5 minutes active work, high confidence; existing scenario vocabulary.

### 11. Explain supported relocation in existing CLI guidance
Type: Behavior
Status: planned
Proof: Actual clone output tests describe root/existing represented destinations,
unchanged bytes/links and relocation acceptance before a later edit. Update
the existing installed exact-copy expectation consistently.

Behavior: Owner reads next steps → understands how to publish a relocation
without being promised folder creation, overwrite or structural rebase.
Extend the completed Plan 52 wording. Integrate Plan 53's content-only
pull→inspect→publish guidance without replacing it. Run clone tests; run the
focused installed feature when its expectation changes.
Sizing: ~5 minutes active work, medium confidence; one guidance loop, with
shared-file coordination treated as an external wait if it blocks progress.

### 12. Edit the same note after accepted relocation
Type: Behavior
Status: planned
Proof: Sequential controller publication accepts relocation, then a separately
committed content edit at the final path, updating the original note with its
retained tracker. Backend suite.

Behavior: Relocation has been accepted → separately publish an edit there →
update the same learned concept. Reuse NotebookGitRenameThenEditControllerTest
with cross-parent data; do not batch unpublished relocation and edit commits.
Sizing: ~5 minutes active work, high confidence; existing continuation fixture.

### 13. Receive relocation and its later edit in another checkout
Type: Behavior
Status: planned
Proof: Real-Git CLI fast-forward fixture from the relocation parent receives
the accepted relocation/edit sequence: old path absent, exact final bytes,
accepted head/tree and ancestry, clean main and no Portable metadata.

Behavior: Another clean eligible checkout pulls accepted history → receives
the note's final path and later edit without history loss. Extend the existing
rename receipt fixture in notebookPull.fastForward.suite.ts; keep backend
acceptance ownership in leaf 12. No production rebase changes. Retain Plan 53's
structural-divergence rejection tests when both changes are integrated.
Sizing: ~5 minutes active work, high confidence; one fast-forward proof loop.

## Refinement result, verification and wrap-up

The original ten Behavior leaves become twelve Behavior leaves plus one
immediately enabling Structure leaf. All are planned; no completed evidence
was discarded. Main refinements: separate the concrete title-validation
extraction and private-state proof from placement, add path-specific reference
proof, make destination/overwrite/empty-folder boundaries explicit, and remove
obsolete pending-correction and wait-for-Story-8/9 instructions.

Ready for execution with the parallel coordination above. Estimates are
hypotheses: ~5 minutes including focused verification and local cleanup; at five
minutes scrutinize scope, at ten non-exempt minutes stop and finer-decompose.
Backend/E2E runtime or coordination waits justify an exception only when
recorded as the actual cause. No timing guarantee. Repeated qualifying overruns
or changed product boundaries return to Story 12 under learning escalation.

Commands during execution:

- Backend leaves: CURSOR_DEV=true nix develop -c pnpm backend:test_only
  (all backend unit tests per the backend rule).
- CLI guidance: CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run
  tests/notebookClone.test.ts.
- CLI receipt: CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run
  tests/notebookPull.test.ts. Include notebookPublish.test.ts when integration
  touches publication behavior; do not run suite fragments independently.
- Installed: CURSOR_DEV=true nix develop -c pnpm cypress run --spec
  e2e_test/features/cli/cli_notebook_clone.feature.
- Whitespace: scripts/check_diff_whitespace.sh.

No product tests are run by this planning-only request. At execution use
Jidoka → fresh post-change-refactor agent → API generation if needed →
coordinator format:changed once → plan update → commit/push and asynchronous
CI observation. Preserve other workstreams' edits and keep state in this PLAN,
not STATE.md. No full E2E, manual or mutation testing is requested.

On completion mark only Story 12 delivered, update Recently done, reduce its
home detail and remove this spent plan. Story 7 and structural rebase remain
unfinished. Backlog priority is not changed by selecting parallel execution.
