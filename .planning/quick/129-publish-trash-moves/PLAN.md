# Publish ordinary moves across the trash boundary

Status: in progress
Source: [SEED-009 story 28](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-28).
Owner clarification, 2026-09-16: dependency recovery is exactly existing behavior;
reuse it cohesively, with no special recovery implementation. Planning and plan
refinement are authorized. Execution started 2026-09-16.

## Execution identity

- Originating checkout: `/Users/terryyin/git/doughnut` on `main`
- Claim commit: `a4507e5304`
- Execution checkout: `/Users/terryyin/git/doughnut-worktrees/story-28`
- Execution branch: `quick/129-publish-trash-moves`
- Integration target: `main`
- Replanning permission: allowed (existing plan-refinement authority)
- Authorized push destination: `origin` (`git@github.com:nerds-odd-e/doughnut.git`)
- CI: GitHub Actions workflow `ci.yml` display name `donut CI`
- Observer mailbox: `/tmp/dough-ci-501/watch-aUvKRw`
- Slice 2 delivered SHA: `b4f0e60a0d70b5436f59b2373000a54aa50e127f`

## Goal and boundaries

In one synchronized Git-backed notebook, publish unambiguous local note and
unchanged-subtree moves into or out of root `_trash`, preserving identity,
authored content and retained dependencies. Receive existing web trash/recovery
through ordinary pull. Construct parents required by the committed destination.
Use supported linear history composition and apply one final result atomically.

Availability, learning eligibility, reference resolution and removed-from-tracking
preferences keep their current domain rules. Retained references resolve again
only when their authored targets identify eligible destinations. Removed reference
properties stay removed. Recovery to another path does not reconstruct old links.
Retain schedules, history, note-ID URLs, folder Readmes and represented empty
descendants. Preserve canonical `.keep` representation and authored proposal bytes.

No new trash UI, Restore, Undo work, cleanup, deletion policy, migration, identity
heuristics, cross-notebook sync, divergence/rebase, or performance work. The folder
example uses the existing exact-subtree correspondence (including a source Readme);
broader folder identity inference is not promised. Examples do not add rejection
gates. Existing supported edit/rename histories must remain supported.

## Existing-solution assessment and decisions

Source and tests inspected at `147aed1893`; no runtime checks executed during
planning. These findings select reuse and proof, not completed slices.

| Responsibility | Existing owner and decision |
| --- | --- |
| Availability and retained learning | `Folder.isTrashed`, `Note.isAvailable`, `MemoryTracker.isActive`, existing repository availability predicates. Reuse unchanged; do not add reactivation writes or copied trash rules. |
| References | `WikiLinkResolver`, `AuthoredNoteReferenceInboundFacade`, existing reference capture/rewrite owners. Reuse current resolution. Web move rewriting and Git preservation of authored bytes are distinct existing caller purposes. |
| Note placement | Shared: `NoteMotionService.assignPlacement` assigns title/notebook/folder. Web Move, note Trash and Undo still call `executePlacement` (uniqueness + flush/merge). Git `applyRename` uses `assignPlacement` then caller-owned timestamp/save/content persist — not `executePlacement`, because mid-batch uniqueness+flush would collide with still-occupied titles. No web commit/reference orchestration. |
| Folder placement | `FolderMoveRelocation` owns web validation and persistence; `NotebookGitProposalFolderPlacement` already reuses destination rules but Git relocation reparents directly. Expose the necessary placement responsibility from its existing owner. Preserve explicit merge and trash suffix selection at their current callers; do not make Git choose a different proposed path. |
| Destination ancestry | `NotebookGitProposalFolderMaterialization` uses `FolderConstructionService` for added-document ancestors. Modularize this existing ancestry construction for admitted move destinations; preserve Readme persistence separately. No second path walker in a trash service. |
| Correspondence and publication | Existing JGit ordinary-note detector, exact folder correspondence and linear history composition feed `NotebookGitProposalPublisher`. Reuse final application and `proposalAcceptance`; do not replay live mutations for intermediate commits. |
| Web accepted changes | `AcceptedWebChangeService.apply` owns lock/load, drift check, complete mutation, final snapshot and one accepted child. Note trash/recovery and ordinary folder Move use it. `NotebookController.trashFolder` currently uses a plain transaction and bypasses it: integrate this existing operation. |
| Portable tree and transport | `PortableTreeSnapshot` owns Readmes and `.keep`. CLI `notebookPublishSubmission` submits a full Git bundle; pull fast-forwards accepted history. Reuse installed CLI and real Git test helpers; no trash protocol or client classifier is indicated. |

Affected callers include same-notebook and cross-notebook web note/folder moves,
Trash, note Undo, Git rename and folder relocation, and document additions using
parent construction. Only shared responsibilities change; each caller retains its
authorization, transaction, content and destination policy. No API/schema change
is anticipated. Do not add flags for web/Git/trash modes.

Follow [North Star: complete accepted web change](../../NORTH-STAR.md#one-complete-accepted-web-change),
[final publication result](../../NORTH-STAR.md#one-final-publication-result), and
[rename correspondence](../../NORTH-STAR.md#git-rename-correspondence). These already
govern the work; no new direction topic is needed. Accepted
[ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md#trash),
[ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md), and
[ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md)
govern location/Portable data, note URLs and learning state. ADR 0002 is Proposed.

### Current evidence and constraints requiring care

- `NotebookGitWebTrashControllerTest.ordinaryMoveAfterActualTrashAppendsAcceptedChildWithRecoveredPath`
  performs real web Trash then Move, observes accepted paths and retained trackers,
  preferences and recall-log count. This covers web recovery, not local publication.
- `NotebookGitProposalRenameReferrerControllerTest` and
  `NotebookGitProposalFolderRelocationReferrerControllerTest` explicitly preserve
  authored referrer bytes during publication. Calling web rewrite orchestration
  would violate existing behavior even if placement is shared.
- `NotebookGitProjection.requireRepresentedFolderId` requires an existing row;
  rename destinations alone do not enter current added-document materialization.
  `NotebookGitProposalFolderRelocationDestinationControllerTest` explicitly refuses
  a missing parent. The selected story now promises construction from the final
  proposed path, so that missing-parent expectation must change. Its separate
  pre-existing unrepresented-parent/drift safeguard remains required. Never use
  construction to adopt unsynchronized database state.
- `.keep` changes are excluded from ordinary note changes. Snapshot generation
  retains empty folders, so test proposals must represent retained source folders
  canonically instead of quietly deleting them. A new parent needs no dummy
  Readme, anchor note or redundant `.keep` beside its content.
- `NotebookGitProposalFolderRelocationEmptyDescendantControllerTest` creates an
  unrepresented descendant: that is drift, not evidence that a canonical tracked
  empty descendant cannot move. Keep that distinction in new examples.

## Verification and delivery contract

At each behavior slice, first drive the public boundary with the stated data.
If it already passes, retain sufficient proof and make no gratuitous product
change. Reuse sufficient existing observations. Structural changes first run the
relevant existing coverage as a baseline. Record literal command/results and
setup/assertion locations here during execution.

Slice 1 B (worktree `doughnut_wt_f5e1c98273ff46809f172d4da36df95d_test`): baseline
`CURSOR_DEV=true nix develop -c pnpm backend:test_only` BUILD SUCCESSFUL (~1m3s
after migrate); post-change same command BUILD SUCCESSFUL (~1m2s).

- **B:** `CURSOR_DEV=true nix develop -c pnpm backend:test_only` — all backend
  unit tests, per backend rules; use real controller/database collaborators and
  committed reloads for transaction observations, not a helper-class test suite.
- **N:** `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_web_trash.feature`
  — extend this existing spec for the installed-CLI local recovery journey.
- **F:** `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_folder_relocation.feature`
  — extend the existing local-folder-publication spec for trash locations.
- **W:** `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_web_folder_moves.feature`
  — extend the existing installed-CLI receipt spec for folder Trash/recovery.
- If CLI production code changes, also run
  `CURSOR_DEV=true nix develop -c pnpm cli:test`. API generation is required only
  if API signatures/types actually change. No manual testing is selected.

Each slice targets about five minutes including proof. Estimates below are
active-editing hypotheses; mandatory full backend runs and installed-CLI startup
are explicit test-wait exceptions, with actual elapsed time recorded separately.
Scrutinize active work above five minutes and stop/finer-decompose above ten.
Do not hide multiple implementation outcomes inside a test-wait exception.

Execution follows `dough-execute-plan`: select the execution location and take
the queued story only when execution is authorized; test-first corrections,
Jidoka, fresh `dough-post-change-refactor` agent, appropriate verification/API
generation, coordinator `./scripts/run.sh pnpm format:changed` once per delivery,
plan update without another routine format, commit with check-only hook, push
and asynchronous CI observation. Keep the plan and evidence for retrospective
and story wrap-up. This planning turn commits/pushes nothing.

## Ordered slices

### 1. Share existing note placement
Type: Structure
Status: done
Enables: slice 2, local recovery with the same domain placement as web Move.

Git ordinary-note rename no longer mutates title/folder itself.
`NoteMotionService.assignPlacement` is the shared assignment; `executePlacement`
keeps uniqueness+flush for web. Git `applyRename` still owns `publishedAt`,
`entityPersister.save`, and `AuthoredNoteDocumentPersistence.persist`. No recovery
code and no web rewrite routing.

Proof: B passed before and after the change.
- Command: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- Owner inspection: `NoteMotionService.assignPlacement` /
  `executePlacement`; `NotebookGitProposalOrdinaryNoteApplication.applyRename`
- Setup: none beyond existing controller/DB fixtures
- Observations reused:
  - `NotebookGitProposalRenameControllerTest.acceptsSeveralUniqueMovesRegardlessOfPathOrdering`
  - `NotebookGitProposalRenameControllerTest.preservesPrivateAssociationsOfTheRenamedNoteAndLeavesTheOtherIdenticalTextNoteUntouched`
  - `NotebookGitProposalRenameReferrerControllerTest.leavesReferringBodyAndPropertyLinksAuthoredWhenPublishingTheTargetsRename`
  - `NotebookGitProposalRenameRollbackControllerTest.lateBindingSaveFailureRollsBackARenameLeavingTheOldTitleTrackerAndAcceptedBinding`
  - `NotebookGitWebNoteMoveControllerTest.webMoveAppendsAcceptedChildAndLocalPublicationRetainsLearningHistory`
  - `NotebookGitWebTrashControllerTest.ordinaryMoveAfterActualTrashAppendsAcceptedChildWithRecoveredPath`
Refactor: none — already clean. No API generation.

### 2. Publish recovery of a retained note
Type: Behavior
Status: done

Existing publication already restores the same note after actual web Trash and
a local move to an existing active destination. No production Java/CLI change.
A leftover `Biology/.keep` beside the recovered note is an existing Portable
conflict; the installed-CLI journey removes it in the same commit as the rename.

Proof: B + N.
- B: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` pass (post-refactor ~1m7s)
  - Setup: Git-backed `Biology/Cells` with learned + removed trackers and authored
    referrer; real `noteController.trashNote`; occupier variant web-creates Cells
    after trash; then `publishNotebookGitProposal`
  - `NotebookGitWebTrashLocalRecoveryPublicationControllerTest.localPublicationAfterActualTrashRestoresSameNoteAvailabilityAndRetainedDependencies`
  - `.localPublicationAfterRemoveFromPropertiesTrashLeavesTheRemovedPropertyAbsent`
  - `.localPublicationRecoversToAFreePathWhenTheOriginalPathIsOccupied`
- N: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_web_trash.feature`
  pass (post-refactor 2 scenarios)
  - Setup: trash, pull, commit rename `_trash/Biology/Cells.md` → `Biology/Cells.md`
    plus remove `Biology/.keep`, installed publish, original note route
  - Scenario: Publishing a local recovery after pulling a web trash

Refactor: shared `countRecallLogsByTrackerId` on
`NotebookGitWebContentControllerTestBase`; split `cliE2eNotebookCloneGit.ts` and
`cli_notebook_clone_commits.ts`. No API generation.

### 3. Reuse publication destination construction
Type: Structure
Status: done
Enables: slice 4, moves whose destination ancestry is absent.

`NotebookGitProposalFolderMaterialization.ensureAncestry` is the one parent-path
walker for admitted destination paths. `materialize` still composes it then
`persistFolderReadmes`. `FolderConstructionService.createFolder` remains the
creation owner. Document additions still call `materialize`. No move-admission
or trash branch.

Proof: B `CURSOR_DEV=true nix develop -c pnpm backend:test_only` pass before
and after (~1m10s / ~1m2s). Setup: none beyond existing fixtures. One walker
inspected at `ensureAncestry` / private path loop; document caller
`NotebookGitProposalDocumentApplication`. Existing folder-creation, folder-note
publication, and `NotebookGitProposalFolderPublicationSafetyControllerTest` remain
the behavior coverage. Refactor: none — already clean.

CI repair (implementation was stashed, then restored): runs 35072853614 and
35074963239 failed `cli_notebook_web_folder_moves` successive-head pull because
publication-state SQL required an isolated DB. Repair SHA `8e918218ae` observes
shared `doughnut_e2e_test` when not isolated.

### 4. Publish note moves into missing parents
Type: Behavior
Status: done

Admitted note-rename destinations construct missing parents via `ensureAncestry`
inside ordinary-note application after `requireMatchingAcceptedTree`, then
`assignPlacement`. Source identity stays on the original note. Missing
`_trash/Biology` and new active `Research/` work without dummy Readmes. Source
`.keep` is retained. Unsynchronized empty-destination drift still conflicts.

Proof: B `CURSOR_DEV=true nix develop -c pnpm backend:test_only` pass
(post-refactor ~1m5s). Setup: Git-backed `Biology/Cells` then proposal with missing
destination ancestry; download accepted bundle for exact tree.
- `NotebookGitProposalMissingParentNoteMoveControllerTest.publishesAMoveIntoMissingTrashAncestryCreatingParentsAndRetainingTheNote`
- `.publishesARecoveryIntoMissingActiveAncestryWithoutDummyContent`
- `NotebookGitProposalRelocationDestinationControllerTest.relocatesIntoAMissingFolderCreatingRequiredParents`
- `.rejectsRelocationWhenAnEmptyDestinationAppearedOutsideAcceptedHistory` (preserved)

Refactor: ancestry call moved from publisher into
`NotebookGitProposalOrdinaryNoteApplication`; live folders seed construction.

### 5. Share existing folder placement
Type: Structure
Status: done
Enables: slice 6, subtree recovery through the same folder placement owner.

Git and web share `FolderMoveRelocation.assignPlacement`. Web Move/Trash keep
merge, wiki rewrite, trash-name selection, and flush/merge via
`persistFolderPlacement`. Git keeps `requireAllowed` and save/flush with the
proposed name. No Git-through-web commit orchestration.

Proof: B `CURSOR_DEV=true nix develop -c pnpm backend:test_only` pass before
and after (~1m15s / ~1m10s). Setup: existing folder relocation/Trash/Move
controller fixtures. Call sites: `NotebookGitProposalFolderRelocation`,
`persistFolderPlacement`, cross-notebook persist. Refactor: cross-notebook
web persist uses `persistFolderPlacement`.

CI repair (slice 5 implementation stashed as `24b6ef0bd0e56d2c1e4c198d1bb8b47418f2bbb5`):
run 35077284338 (SHA 8e918218ae) still failed the same scenario with
`Access denied for user 'root'@'172.17.0.1' (using password: NO)`. Observation
mysql now uses E2E SUT credentials doughnut/doughnut. Proof:
`CURSOR_DEV=true nix develop -c node --test scripts/sut-e2e-observation-database.test.mjs`.

### 6. Publish a retained subtree trash round trip
Type: Behavior
Status: done

Existing publication already retains the exact subtree across an existing
`_trash` parent and back. No production Java/CLI change. Tests observe
canonical tracked `Empty/.keep` (distinct from the unrepresented-descendant
refusal). Slice 2 dependency schedule/reference assertions are not repeated.

- B: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` BUILD SUCCESSFUL (~1m11s)
  - Setup: Git-backed `Biology` with Readme, distinct `Cells`/`Nucleus`, tracked
    empty descendant, existing `_trash`; two real `publishNotebookGitProposal`
    calls
  - `NotebookGitProposalFolderRelocationTrashRoundTripControllerTest.publishesASubtreeTrashRoundTripRetainingIdentitiesEligibilityAndCanonicalKeep`
- F: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_folder_relocation.feature`
  pass (5 scenarios, ~24s)
  - Setup: `Recipes` Readme, Pasta/Sauce, `Empty` under Pasta, existing `_trash`;
    clone twice; rename+remove `_trash/.keep`; publish; rename back+empty keep;
    publish; installed pull
  - Scenario: Publishing a folder subtree trash round trip is received by a later pull

### 7. Publish subtree moves into missing ancestry
Type: Behavior
Status: done

Admitted folder destinations reuse `ensureAncestry` after source mapping and
the original pre-mutation drift check. Missing `_trash/Research` and active
`Research/` construct parents; identities stay on the source subtree.
Unrepresented dest parents and unrepresented descendants still refuse.

Proof: B `CURSOR_DEV=true nix develop -c pnpm backend:test_only` pass
(post-refactor ~1m11s).
- Setup: Git-backed Biology Readme + Cells + tracked Empty; missing dest ancestry
- `NotebookGitProposalFolderRelocationDestinationControllerTest.publishesAnExactSubtreeMoveIntoMissingTrashAncestryCreatingParentsAndRetainingIdentities`
- `.publishesAFolderRecoveryIntoMissingActiveAncestryCreatingParents`
- `.rejectsAnExactFolderRelocationIntoAnExistingUnrepresentedParent`

Refactor: dest-parent prefix/lookup shared; recovery asserts only the new parent.

### 8. Receive an existing web folder trash operation locally
Type: Behavior
Status: done

`trashFolderWithinNotebook` runs the complete mutation inside
`AcceptedWebChangeService.apply`. Ancestors are not committed separately.
Installed CLI pull receives the subtree; ordinary web Move recovers it.

Proof: B + W.
- B: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` pass (post-refactor ~1m13s)
  - Setup: real `controller.trashFolder` on Git-backed Research/Biology subtree
  - `NotebookGitWebFolderTrashControllerTest.trashOfFolderSubtreeAppendsAcceptedChildWithConstructedParentsReadmeDescendantsAndKeep`
  - `.ordinaryMoveAfterActualFolderTrashAppendsAcceptedChildWithRecoveredPath`
  - `NotebookGitWebFolderTrashCollisionControllerTest.occupiedTrashNameSuffixingLeavesEarlierTrashBytesUnchanged`
  - `NotebookGitWebFolderTrashGuardControllerTest.unauthorizedTrashLeavesFolderAndAcceptedBindingUnchanged`
  - `.preExistingPortableDriftKeepsTheFolderTrashAndAcceptedHistoryUnchanged`
  - `NotebookGitWebFolderTrashAtomicControllerTest.lateBindingSaveFailureRollsBackConstructedParentsPlacementAndAcceptedBinding`
- W: `SUT_TIMEOUT_MS=360000 CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_web_folder_moves.feature`
  - Scenario: Pulling a web folder trash then recovering it by ordinary move

Refactor: shared `applyLiveFolderChange` with in-notebook move; tests split.
### 9. Preserve identity across accumulated trash moves
Type: Behavior
Status: planned
Behavior: a linear local range moves a retained note into trash and then to a
different active path before publication; the same note and dependencies appear
at the final path, with the original committed history retained.

Proof: B using existing composed-range helpers and the publication/download
boundary. Observe identity and final placement/content, unchanged dependent
records, and accepted intermediate-commit ancestry. A return to the original
endpoint must not fabricate recreation; existing deletion-gap recreation tests
retain their contrasting new-identity semantics. Reuse the single-commit final
application; no intermediate live mutation or new history journal.
Sizing: 3–5 minutes active work; if correspondence needs a new inference policy,
stop for source-scope reassessment instead of broadening this story.

### 10. Roll back constructed parents with rejected publication
Type: Behavior
Status: planned
Behavior: a late acceptance failure after constructing move destinations leaves
the original notebook, dependencies and accepted history unchanged.

Proof: B. Extend the existing late-binding-save-failure controller fixture with
missing destination ancestry; inspect committed state after rejection for no new
parents, unchanged note/folder placement and retained dependencies, and unchanged
accepted head/bundle. Cover note and folder callers only where their application
paths differ; share observations through existing test support. Reuse existing
stale-head, unauthorized, ambiguous correspondence, invalid-destination and drift
tests rather than create another full safety matrix.
Sizing: 3–5 minutes active work using existing failure injection and committed
transaction helpers. No exception swallowing or partial acceptance.

## Proof ownership and completion

| Source promise | Owning slices and observations |
| --- | --- |
| Existing dependency recovery and alternate active destination | 2 done: controller state/reference outcomes and original-route installed-CLI journey |
| Local trash/recovery with required parents and canonical retained folders | 4 done: controller publication and downloaded exact tree |
| Shared domain behavior, no duplicate recovery logic | 1 done: `assignPlacement`; 3 done: `ensureAncestry`; 5 done: folder `assignPlacement` |
| Retained subtree, learning and empty descendants | 6–7 done: identity/ancestry and exact Portable tree through publication/pull |
| Existing web operation received locally | 8 done: real web Trash/Move followed by installed pull, one accepted child per operation |
| Linear histories, final-only application, deletion-gap distinction | 9: composed publication, dependencies and original Git ancestry |
| Atomic failure including new folders; existing access/ambiguity/drift safeguards | 10 and existing guards run with B at each affected slice; slice 8 owns web-boundary variants |
| Preserve ordinary moves, authored Git bytes and web reference choices | 1, 5: existing rename/folder referrer, Move, Trash/Undo and cross-notebook caller coverage |

Slices 1–8 are delivered. Remaining required observations must be covered before
story closure. At each green boundary, retain the
specific evidence here; do not substitute a green command for a missing promised
observation. If existing code proves a behavior, close its proof without adding
another mechanism. All required observations must be covered before story closure.

## Sizing and cumulative-design assessment

Ten slices. During construction, separate shared ancestry extraction from note
move integration, then reuse it for subtree integration; keep web acceptance,
accumulated history and rollback as independent observable outcomes. Structure
slices immediately precede the Behavior they enable. There is no tests-only
preparation phase or temporarily broken delivery point.

The cumulative model remains ordinary correspondence → existing ancestry and
placement → existing location-derived availability → one final acceptance.
Folder and note correspondence have distinct established identity semantics;
that distinction does not authorize duplicate placement, recovery or construction.

Remaining slice-specific concerns: slice 1 must preserve batch/flush behavior;
4 and 7 must construct parents without changing original correspondence or
adopting drift; 6 must retain canonical empty descendants; 8 must move the whole
existing mutation inside the accepted boundary. These are integration/sizing
risks, not new product decisions. Runtime baselines and red/green observations
belong to execution. No larger-than-15-slices resplit recommendation. No product
implementation, tests, commit or push occurred while writing this plan.
