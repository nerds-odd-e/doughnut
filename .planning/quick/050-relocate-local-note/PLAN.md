# Publish an identity-preserving note relocation

Status: planned; Story 6 prerequisite delivered. Stories 8 and 9 remain higher
product priorities; this plan has not been executed.
Source: [SEED-009 Story 12](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-12).
Prerequisite: [delivered Story 6](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-6).
Related corrections: [Plan 52](../052-clarify-note-rename-publication/PLAN.md).

## Goal and scope

An owner moves one learned note between existing represented folders or the
notebook root using the existing installed CLI, retaining that note's identity
and learning state. The same isolated commit may also change its filename.
Another checkout receives the new location and a separately published later
content edit on the same identity.

Extend the exact unchanged remove/add pair delivered by Story 6 to a changed
parent directory. Inherit that story's owner/readiness, single-child ancestry,
exact authored bytes, private-data preservation, unchanged reference policy,
atomicity, retry and stale/drift protection. Retain same-parent renaming,
addition/edit/deletion behavior, and fresh identity after separate deletion
and creation.

Only existing Donut folders represented in accepted parent history qualify.
Include root, nested folders and README-only destinations. Validate the final
folder/title together, not an intermediate title/location. Occupied or
soft-deleted final destinations reject. Retain all container identities,
including a source folder emptied of tracked files; never generate README.

Exclude new/unrepresented folders, whole-folder or README moves, cross-notebook
moves, changed content or accompanying reference rewrites, multiple moves,
restore/deleted-path reuse, multiple unpublished commits, rebase/conflicts,
drift repair, web structural synchronization, or new commands/UI/metadata.
This story does not require Stories 8/9 technically; they precede it by product
priority. Story 7 (whole-folder moves) follows this story.

## Reused execution context

Story 6 is delivered through `980114d23d`. Its implementation classifies one
raw equal-blob removed/added pair in
`NotebookGitProposalTreeShape.detectSameParentRename` and mutates the same
Note in `NotebookGitProposalPublisher.applyRename`. Extend that representation
and handler instead of creating another identity path. Recheck their current
form at execution, including any completed Plan 52 corrections.
The reusable boundaries are:

- NotebookGitProjection.requireRepresentedFolderIdForAddition already resolves
  a full destination folder path against accepted parent content, including
  README-only folders. Share/generalize it for relocation when needed.
- NoteTitlePlacementRules checks a deleted final title at a chosen folder.
  Resolve and validate the destination first, then assign the final title and
  folder inside NotebookGitProposalPublisher's existing transaction.
  Preserve path-specific conflict context from the Story 6 correction; if
  Plan 52 is still pending when this story is selected, reconcile its error
  correction with leaf 6 once rather than duplicate the work.
- NoteMotionService flushes and validates its current title; web movement also
  rewrites references. Do not sequence those workflows to implement an atomic
  final placement or accidentally validate an intermediate collision.
- Keep the existing projection checks, binding lock/save, transaction ownership
  and failure behavior. Reuse the committed controller fixtures, exact atomic
  failure profile, and real-Git CLI transport tests from the rename delivery.
- The rename story supplies the installed git-mv harness. Use it for
  cross-parent moves; do not create another task framework or second-checkout
  Cypress infrastructure.
- Accepted [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  governs representations/references/empty folders; Accepted
  [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md) governs
  failure handling. ADR 0002 remains Proposed. No DDL/API or transaction
  change is expected, so the existing storage/rollback evidence applies.

## Original scope allocation and proof ownership

The original combined plan's relocation leaf 4 is now leaves 1–2; original
container leaf 9 is leaf 4; original leaf 11's parent-eligibility cases are
leaf 5. Cross-folder variants of original rollback/deleted-path/CLI/receipt
obligations belong here. Other original obligations are delivered by the
rename plan and remain required regressions, not unfinished work delegated
to a later story.

| Promise | Owner and externally observable evidence |
| --- | --- |
| Root/folder/nested/README-only destination with same identity/private state | 1; public note placement and existing private-identity regression extended to a cross-parent fixture |
| Simultaneous filename change uses only final eligibility | 2; final title/folder accepted despite an occupied hypothetical intermediate location |
| Exact commit and folder/title rollback on late failure | 3; fresh original placement and accepted binding |
| Empty source container survives without README | 4; source/destination IDs and exact Portable tree |
| Missing/unrepresented parent rejects atomically | 5; source, folders and binding unchanged |
| Occupied/deleted final target rejects without resurrection | 6; final-placement conflict with both identities unchanged |
| Installed CLI relocation is visible in Donut | 7; authored head and note at new path |
| Guidance describes the new folder boundary | 8; existing clone/publish output |
| Later edit retains identity at the new location | 9; public note after a later accepted edit |
| Another checkout receives the relocation and edit | 10; exact real-Git CLI head/tree/history |
| Original tracker/question/conversation ownership and unchanged authored links | Rename private-data/reference regressions retained; 1 adds cross-parent placement data without invoking new reference handling |
| Retry, stale head, drift, owner/readiness, ordinary add/edit/delete safety | Common controller/CLI regression suites retained; shared gates must still precede mutation |

If a change invalidates inherited evidence (for example, a new mutation branch
changes associations or reference handling), add cross-parent proof before
claiming the affected promise. Do not treat a dependency's historical green
result as proof of changed behavior.

## Ordered leaves

### 1. Relocate an unchanged note to an existing folder or root
Type: Behavior
Status: planned
Proof: Controller data variations for root→folder, folder→root, folder→folder
and nested README-only destination. Public note retains its ID and private
state at the final folder; downloaded head/tree exactly match the proposal.
Extend the delivered identity fixture with a cross-parent case. Backend.

Behavior: One eligible equal-blob pair changes parent but keeps filename →
publish → move the same learned note to the resolved existing destination.
Remove the rename story's same-parent-only gate for this bounded case.
Reuse the accepted-parent folder resolver and final deleted-title check;
assign the folder inside existing publication transaction. Temporarily keep
combined parent/filename changes rejected until immediately following leaf 2.
Retain authored content and referrers without web move calls.

### 2. Relocate and rename using the final destination
Type: Behavior
Status: planned
Proof: Controller accepts a changed parent and filename when the final location
is free, including a deleted-title collision at either hypothetical intermediate
location. Observe same note at final title/folder. Backend.

Behavior: An isolated exact pair changes folder and filename → publish →
validate and accept only the final destination. Remove leaf 1's temporary
same-filename limit. No intermediate flush, web rename/move sequence, title
normalization repair, or reference rewriting.

### 3. Roll back a failed folder change
Type: Behavior
Status: planned
Proof: Add a relocation data variant to the delivered atomic-failure fixture;
after forced binding-save failure, fresh original title/folder/tracker state
and accepted head/bundle/timestamps remain. Backend.

Behavior: Folder/title mutation occurs but acceptance fails late → publish
fails → retain original placement and accepted revision. Reuse the exact
existing profile/configuration and failure reset; no new transaction mechanism.

### 4. Retain an emptied source folder
Type: Behavior
Status: planned
Proof: Move the last tracked note from a folder with no README → source and
destination container IDs survive and accepted Portable files match the
authored tree without an invented README. Backend.

Behavior: A note relocation empties its source's tracked content → publish →
leave the Donut container intact. Use the deletion container fixture pattern;
this must never be classified as a whole-folder move.

### 5. Reject a destination folder absent from accepted content
Type: Behavior
Status: planned
Proof: Controller cases for missing Donut folder and existing but unrepresented
folder; path-specific rejection with source, containers and binding unchanged.
Backend; reuse represented-parent validation fixtures.

Behavior: Local directories imply an ineligible destination → publish →
reject without creating/inventing a folder or moving the source. Root and
represented README-only destinations must retain their successful behavior.

### 6. Preserve a conflicting final destination
Type: Behavior
Status: planned
Proof: For a deleted final path established by accepted deletion, retain the existing
deleted-title conflict, unchanged source, deleted identity and binding. Backend.

Behavior: Final destination is unavailable → publish → reject atomically.
Apply the target folder's final title rule, not the source folder's or old
filename's rule. Reuse the rename collision fixture with target-folder data.
Retain the existing occupied-live-path rejection regression through the
complete-diff/tree rules; do not add a separate live-collision mechanism.

### 7. Publish cross-folder relocation through the installed CLI
Type: Behavior
Status: planned
Proof: Existing installed feature: clone → git mv Recipes/Pasta.md to
Pasta basics.md → publish → authored accepted head and unchanged-content
note at root in Donut. Focused notebook E2E.

Behavior: Owner commits an eligible relocation/rename → runs installed publish
→ sees the same note at its new path. Reuse the rename story's task, page
object and existing note-view assertions. No post-action test resnapshot.

### 8. Explain existing-folder relocation in CLI guidance
Type: Behavior
Status: planned
Proof: Clone-output tests and the existing exact-copy E2E expectation describe
root/existing represented destinations, unchanged content/links and a separately
published later edit. CLI clone/publish tests and focused E2E.

Behavior: Owner reads next-step guidance → learns the expanded destination
boundary without being promised folder creation or mixed moves. Extend the
same guidance corrected by Plan 52: commit and publish the relocation, wait
for acceptance, then edit and separately commit/publish. Keep the explanation
that links remain authored and may stop resolving. If Plan 52 remains pending,
reconcile its guidance correction here once; no parallel help surface.

### 9. Edit the same note after its accepted relocation
Type: Behavior
Status: planned
Proof: Backend sequential-publication fixture accepts relocation then a separate
content edit at that path on the original ID with its retained tracker. Backend.

Behavior: Relocation is accepted → separately publish an edit at the new path →
update the same learned concept. Reuse the rename continuation fixture with
cross-parent data; do not batch unpublished relocation/edit commits.

### 10. Receive the relocation and later edit in another checkout
Type: Behavior
Status: planned
Proof: Real-Git CLI pull fixture receives the accepted relocation/edit sequence:
old path absent, new bytes present, exact head/tree, retained ancestry and clean
main without Portable metadata. CLI pull tests.

Behavior: A second eligible checkout is at the relocation's parent → pull →
receive both accepted commits at the new path without history loss. Reuse the
rename receipt fixture with cross-parent data; leaf 9 owns backend acceptance.

## Verification and delivery

- Backend: CURSOR_DEV=true nix develop -c pnpm backend:test_only.
- CLI: CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run
  tests/notebookClone.test.ts tests/notebookPublish.test.ts tests/notebookPull.test.ts.
  Select relevant files per changed boundary.
- E2E: CURSOR_DEV=true nix develop -c pnpm cypress run --spec
  e2e_test/features/cli/cli_notebook_clone.feature.
- Whitespace: scripts/check_diff_whitespace.sh.

Run the complete backend suite for backend leaves. Reuse common rename,
atomicity, retry/stale/drift and readiness proofs while their coverage remains
valid. Keep temporary multi-beat E2E @wip until green; no full E2E, manual or
mutation run is requested.

At execution use the required execute-plan per-leaf wrap-up: Jidoka → fresh
post-change-refactor agent → API generation if needed → coordinator
format:changed once → plan update → commit → push and asynchronous CI
observation. Preserve unrelated files, and keep slice state here, not STATE.md.

## Readiness and sizing

Story 6's technical prerequisite is met. Select this story through the product
backlog after the higher-priority content divergence/conflict outcomes;
Stories 8/9 are not technical dependencies. Target approximately five minutes
per eventual execution leaf; estimates remain hypotheses. Recheck against
the delivered rename code and proof when execution is selected.

The split retains 10 Behavior leaves. Backend continuation and CLI receipt
remain separate proof loops as in the original plan; no verification is bundled
just to reduce the displayed count. The initial leaves are ready to select
after the prerequisite is delivered. Reassess leaves 1–2 against that delivered
rename implementation and use slice-plan-refinement if a trigger appears;
this request's explicit refinement pass applies only to the top rename plan.

Required test runtime may justify a recorded exception only when it actually
accounts for elapsed time. At five minutes inspect scope; at ten non-exempt
minutes stop and refine. Follow story-review escalation for repeated qualifying
overruns or changed outcome. No implementation/verification of product code,
commit or push is authorized by this planning request.

On completion mark only Story 12 delivered, update Recently done, reduce its
seed detail, and remove this spent plan. Do not mark Story 7 delivered.
