# Web README edits enter the accepted-change boundary, and story 3's proof is tightened

Status: **executing** (bounded retrospective correction, 2026-09-22).
Execution authorized by the owner on 2026-09-22, with retrospective skipped.

## Execution

- Mode: Story Branch. Replanning preserved (allowed if a slice overruns).
- Originating checkout: `/Users/terryyin/git/doughnut` (integration, `main`).
- Execution checkout: `/Users/terryyin/git/doughnut-worktrees/011-readme-edits-through-accepted-change-owner`, branch `cursor/011-readme-edits-through-accepted-change-owner`, created this session from `a2660d71f61797895fed18155da10ea0745a5f42`.
- Published claim: `c5a3c91333427de908df87d585322cdd1d0d9871` on `origin/main`. Claim publisher `cursor-011-readme-edits`. Identity `quick/011-readme-edits-through-accepted-change-owner/PLAN.md`.
- Slice 1 increment: `7f217de349c5201c909e0b36d84cc7008543cf9d` on `origin/cursor/011-readme-edits-through-accepted-change-owner`.
- Slice 2 increment: `01a501700725b29568fc8d6c694c55c1f5690951` on that same branch, registered with the observer.
- Story-branch increments publish to `origin/cursor/011-readme-edits-through-accepted-change-owner`. Observer: `/tmp/dough-ci-501/watch-NJo55N` (GitHub Actions `ci.yml` / `donut CI`). The trunk claim is unobserved.
- Default checkout refresh: advanced to `c5a3c91333427de908df87d585322cdd1d0d9871`.
Kind: correction from the execution retrospective of
`quick/010-change-proportional-note-save/PLAN.md` (spent; recoverable at before-cleanup commit `b0b385a184`)
(SEED-034#story-3). No seed is required; this plan carries its own input.

## Source and provenance

- Original story: SEED-034#story-3 "Save note edits at a cost proportional to
  the change, not the notebook", executed on branch
  `claude/010-change-proportional-note-save`, commits `91bf241404`,
  `3ccc38510c`, `bd1b314b6f`, `741030adba`, `e24442f18e`, `929aad7e94`,
  `7ce390e463`, `a6f5223fbb`, `5a6b4754d7` (queue claim `12702ca604`).
- Retrospective (2026-09-22) findings, in impact order:
  1. `NotebookFolderController.updateFolderReadmeContent` and
     `NotebookController.updateNotebookReadmeContent` save and flush directly
     and never enter `AcceptedWebChangeService.apply` (identical on the
     pre-story base), so a web README edit never reaches accepted Git history.
     This conflicts with ADR 0002's one publication boundary and with the
     story's promise 1, which listed folder README edits. The encoder already
     derives README changes and is proven only through `apply`
     (`NotebookGitDerivedFolderTreeOracleControllerTest`, README tests).
  2. `NotebookGitWebContentSaveCostControllerTest` asserts prepared-statement
     equality between a large and a small notebook, which the pre-story full
     assembly would also have satisfied (fixed query count per save). The
     discriminating signals are that no executed query names
     `NotebookAttachment` or the note-rows projection (`PortableTreeNoteRow`).
  3. `NotebookGitTreeEncoder` and `ProjectionChangeCapture` are `public` only
     because `NotebookGitWebContentControllerTestBase` reaches the encoder
     for its oracle; every production user is in `services/notebookGit`.
     The attachment path `prefix + filename` is spelled in three places
     (`NotebookGitChangedFolders`, `NotebookGitTreeEncoder`,
     `NotebookGitProposalAcceptance`) while notes have
     `NotebookGitPortablePath.ofNote`. The committed referrer-authoring lambda
     (`inCommittedTransaction(..., () -> authorReferencingContent(...))`) has
     eleven exact copies in nine controller test files (one new).
  4. Five of the nine rewritten drift tests assert one structural property of
     the owner (no before-snapshot) that cannot vary per operation.
- Complexity delta of the original story, stated for the owner: backend
  production +703/-280 (net +423 lines: capture, changed-folder relocation
  and derivation machinery replaced three 160-line assemblers); tests +497;
  docs +9. That plan's slice 8 note "about 80 lines fewer" described that
  slice's refactor only.

## Beneficiary and bounded outcome

Note authors who edit a folder or notebook README on the web get that edit in
accepted Git history in the same request, exactly like every other web change,
derived from the accepted head. Story 3's cost proof distinguishes derivation
from a full assembly. The encoder is no longer exported for tests, one path
rule covers attachments, and redundant tests are gone.

## Scope

Included: routing the two README endpoints through `AcceptedWebChangeService`
with a commit message; oracle tests for README driven through those
controllers; the cost test's discriminating assertions; encoder and capture
visibility; `NotebookGitPortablePath.ofAttachment`; a committed
referrer-authoring helper; removal of five redundant drift tests.

Excluded: README format or rules (ADR 0004); web attachment upload or
deletion (SEED-035); any change to derivation semantics; the stray-row
tolerance in `NotebookGitPortablePath.folderPrefixes` (remove when the
containment migration is finalized).

## Preserved promises and constraints

- One commit per accepted web change on linear `main`, in the projection's
  transaction (ADR 0002). Blank README content removes `README.md` and
  restores `.keep` where the folder is otherwise empty (existing rule).
- A canonical no-op README save appends no commit (tree-id equality).
- Concurrent-writer behavior is unchanged;
  `NotebookGitConcurrentProjectionDriftControllerTest`'s README-bearing folder
  insertion scenario will now observe two commits (folder creation, then
  README edit); adjust its expectation rather than the product.

## Proof ownership

- Behavior: `NotebookGitDerivedFolderTreeOracleControllerTest` README tests
  call `NotebookFolderController.updateFolderReadmeContent` and
  `NotebookController.updateNotebookReadmeContent`; assert `README.md`
  appears and disappears at the prefix, one commit appended per edit, oracle
  equality with the full assembly. The `saveReadme` helpers are deleted.
- Cost: `NotebookGitWebContentSaveCostControllerTest` asserts the large save
  runs no query naming `NotebookAttachment` or `PortableTreeNoteRow`; the
  statement-count equality is dropped.
- Structure: the oracle compares the derived head's blob map with the blob
  map of a history reset (`NotebookGitCutoverService.resetHistory`, already
  exposed to tests as `snapshotCurrentPortableTree`), so
  `NotebookGitTreeEncoder`, `fullTree` and `ProjectionChangeCapture` become
  package-private and the base test loses its encoder dependency.
- Focused commands (execution worktree, isolated DB):
  `CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test -Dspring.profiles.active=test --tests 'com.odde.donut.controllers.NotebookGitDerived*' --tests 'com.odde.donut.controllers.NotebookGitWebContentSaveCost*' --tests 'com.odde.donut.controllers.NotebookGitConcurrentProjectionDrift*'`
  and, before delivery, `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

## Current decisions

1. README edits become ordinary accepted web changes through the existing
   owner; no new endpoint or service.
2. Tests reach the full assembly only through product entry points (history
   reset), never through the encoder.
3. Test removal is justified by named surviving coverage, listed in slice 4.

## Ordered slices

Target about 5 minutes each including tests; over 10 minutes, stop and split.

### 1. README edits through the accepted-change owner
Type: Behavior. Status: done.
Wrap both README endpoint bodies in `acceptedWebChangeService.apply(notebook.getId(), operation, r -> "<message>", now)`, dropping the manual flush. Switch the two README oracle tests to the controllers; delete `saveFolderReadme` / `saveNotebookReadme` / `saveReadme`; adjust the concurrent-drift README-bearing scenario to expect two commits. Proof: the focused command above plus `NotebookGitConcurrentProjectionDrift*`.
Learning: both README endpoints call `AcceptedWebChangeService.apply` (`Edit folder README: …` / `Edit notebook README`) with SERIALIZABLE isolation, matching the other web-change methods. Oracle tests call the controllers and assert `parents()` equals the prior commit list. The concurrent README scenario expects `new folder/README.md` and two commits from the prior head. Refactor moved folder queries to `NotebookFolderQuerySupport` and Git HTTP to `NotebookGitHttpSupport` so the controllers stay within 250 lines; README behavior was unchanged and that proof was not rerun. Compile of the split passed offline.

### 2. Cost proof that discriminates derivation from assembly
Type: Behavior (proof). Status: done.
In `NotebookGitWebContentSaveCostControllerTest`, replace statement-count equality with assertions that no executed query names `NotebookAttachment` or `PortableTreeNoteRow`; keep "one commit appended". Proof: that class green; temporarily forcing the full assembly should make it fail (do not keep that change).
Learning: `savingContentInALargeNotebookWithAttachmentsDoesNotQueryAttachmentsOrPortableTreeRows` asserts those two query names are absent and one commit is appended. Forcing `AcceptedWebChangeService.commitIfChanged` onto `fullTree` failed on the `NotebookAttachment` assertion and was reverted. Focused class passed after the revert. Rename-only refactor did not rerun that proof.

### 3. Encoder stays inside its package
Type: Structure. Status: done.
`assertAcceptedTreeMatchesTheFullAssembly` compares against a history reset's tree; `NotebookGitTreeEncoder`, `fullTree`, `ProjectionChangeCapture` and its nested types become package-private; add `NotebookGitPortablePath.ofAttachment(prefix, filename)` and use it in the three sites; add `authorReferencingContentCommitted(Note, String)` to `NotebookGitControllerTestBase`, moving its proposal-bundle helpers to `testability/GitBundleTestReader` (or a proposal support) to stay under 250 lines; replace the eleven copies. Proof: both oracle classes, `NotebookGitProposal*`, `NotebookGitPublication*`, `NotebookGitWebTrash*`, `NotebookGitFolder*` classes green.
Learning: the oracle reloads the notebook and compares blob ids before and after `snapshotCurrentPortableTree`. Proposal bundle bytes live on `NotebookGitCommitFixtureTestSupport`, which already owned the commit helpers, so `GitBundleTestReader` stayed read-only. Property-wiki publication cases moved to `NotebookGitPublicationPropertyWikiControllerTest` to keep the touched files within 250 lines. That move did not rerun the accepted proof.

### 4. Retire five redundant drift tests
Type: Structure (test cleanup). Status: planned.
Remove `preExistingPortableDriftIsNeitherBlockingTheFolderCreationNorAdoptedByIt` (NotebookGitFolderCreationControllerTest), `...FolderDissolve...` (NotebookGitFolderDissolveGuardControllerTest), `...FolderRename...` (NotebookGitFolderRenameGuardControllerTest), `...FolderTrash...` (NotebookGitWebFolderTrashGuardControllerTest), `...NoteCreation...` (NotebookGitNoteCreationControllerTest). Surviving coverage of decision 4: `NotebookGitWebContentSaveControllerTest.preExistingPortableDriftIsNeitherBlockingTheWebSaveNorAdoptedByIt`, `NotebookGitWebFolderMoveControllerTest.preExistingPortableDriftIsNeitherBlockingTheFolderMoveNorAdoptedByIt`, `NotebookGitNoteCreationFolderControllerTest.preExistingPortableDriftDoesNotBlockTheNoteCreationInsideTheDriftedFolder`, `NotebookGitWebRelationReduceControllerTest.reduceIntoDriftedSourceNotebookCommitsEachNotebookOnItsOwnAcceptedHead`. Proof: the five classes green; full backend suite before delivery.

## Remaining concerns

- Slice 1 changes product behavior (README edits now commit); it is within
  ADR 0002 and the story's promise 1, but the owner may prefer to schedule it
  as a backlog story rather than a correction.
