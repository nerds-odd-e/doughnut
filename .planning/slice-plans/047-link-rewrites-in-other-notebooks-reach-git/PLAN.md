# Link rewrites in other notebooks reach their Git

Work item: **SEED-035#story-25**.
Source: [refined story](../../seeds/SEED-035-ai-workspace-supporting-files.md#story-25)
(owner decisions 2026-09-26).

## Goal and scope

When an owner renames, moves or trashes a note or folder on the web and that
changes notes in *another* notebook they can edit, each such notebook gets one
accepted commit in the same transaction. A later local publish of that notebook
then passes the accepted-tree check. Donut stops changing notes in notebooks
the user cannot edit (for example a subscribed notebook owned by someone else).

Actions: note rename, note move within a notebook, trashing a note with
"remove from properties", folder rename, folder move within a notebook, folder
dissolve, and the third notebooks of a move to another notebook.

Excluded (see the story): repairing notebooks already out of step; notebook
rename; automatic drift repair; any change to which links are rewritten or how
their text changes.

Moves to another notebook already reach Git (plan `046-moves-to-another-notebook-reach-git` recoverable at
`9b887b6204`):
note moves run through `RelationController.webMove` →
`WebNoteEditService.edit(…, Set<Integer> notebookIds, …)` over {source,
destination}; folder moves, same- and cross-notebook, share
`FolderRelocationService.moveFolder` → the set form of `applyLiveFolderChange`;
`ProjectionChangeCapture` records a row that changes notebook as deleted in
the old notebook and inserted in the new one.

## Architecture

- **PFE:**
  - Owner: `AcceptedWebChangeService.apply(Set<Integer>, …)` already locks
    several notebooks and appends one commit per changed notebook, each derived
    from that notebook's own captured rows (`ProjectionChangeCapture` already
    records changed rows for *every* notebook; `commitIfChanged` only reads the
    locked ones). No new owner, capture or derivation is needed.
  - The contract is fixed
    ([domain operation ownership](../../../docs/notebook-git-synchronization.md#domain-operation-ownership)):
    "Determine that set before locking, lock bindings in ascending notebook-id
    order, and re-verify the set under lock. Refuse a mismatch."
    `RelationReduceService` is the existing example.
  - Link rewrites all go through `WikiLinkReferenceCapture.liveResolvedInboundReferences`
    → `WikiLinkRewriteSupport.applyInboundReferrerRewrite`. "Remove from
    properties" goes through `NoteReferenceHandling.removeNoteLinksFromReferrerProperties`.
    Both get referrers from `NoteReferenceService.distinctInboundReferencesForViewer`,
    whose visibility rule is "same notebook, or the viewer owns or subscribes to
    the referrer's notebook" (`AuthoredNoteReferenceInboundFacade.referrerVisibleToViewer`,
    `User.canReferTo`). Nothing checks edit rights.
- **One rule for which notes a web action may change:** only referrers in
  notebooks the viewer owns (`User.owns`, which includes circles; the same rule
  as `AuthorizationService`'s edit check). Apply it where the two change paths
  get their referrers, not in `distinctInboundReferencesForViewer` itself,
  because display ("linked from") and the rename prompt keep showing every
  visible referrer.
- **One rule for which notebooks a web action locks:** the action's own
  notebooks plus the notebooks of editable referrers of the notes whose links
  it rewrites or removes. Compute it before locking with one shared helper (for
  example on `NoteReferenceService`, taking the target notes and the viewer).
  A superset is fine: a locked notebook that ends up unchanged gets no commit.
  So the helper may use candidate referrer rows without live resolution, if
  that is cheaper. Only actions that rewrite or remove links call it. Content
  saves, folder trash and permanent deletes do not, so they get no extra cost.
- **Re-verify under lock in the owner, once:** after the operation flushes,
  `AcceptedWebChangeService.apply` refuses (409, "…changed notes in another
  notebook; retry") when the captured change touches a *bound* notebook it did
  not lock. This is the contract's "refuse a mismatch" for every caller at
  once. It also makes any caller that forgets a notebook fail loudly instead of
  silently leaving that notebook out of step (North Star, one set of names per
  folder: "a forgotten one fails loudly"). Unbound notebooks are skipped, as
  today.
- **Complexity:** adds one helper, one ownership filter and one owner check.
  It removes the "cross-notebook referrer rewrites remain outside this owner"
  exception from the contract. Callers change only the set they pass. Report
  the net line delta at the end.

## Key examples → proof

Test notebooks come from `createGitBackedNotebook`. "Publishes normally" is
proved by `assertAcceptedTreeMatchesTheFullAssembly`, which makes the same
comparison as the local publish check (`NotebookGitProjection.requireMatchingAcceptedTree`).

| Promise (story key example) | Slice | Proof |
| --- | --- | --- |
| 5. Subscribed notebook `Shared` (another user's) links `[[Science:Force]]`; rename `Force` → `Shared` unchanged; the owner's own linking note is rewritten | 1 | new `NotebookGitWebLinkingNotebookControllerTest` (rename case, no Git assertions needed) |
| 5, trash variant: trash `Force` with "remove from properties" → `Shared`'s property link stays | 1 | same test |
| 1. `Engineering/Bridge` says `[[Science:Force]]`; rename `Force` to `Load` → one commit in `Engineering`, `Bridge` says `[[Science:Load]]` in its tip, both trees match the full assembly | 2 | same test |
| Same-notebook note move: `Bridge` says `[[Science:physics/Force]]`; move `Force` to `mechanics` → `Engineering` commit with the new path, trees match | 2 | same test |
| 3. `Bridge` has `uses: "[[Science:Force]]"`; trash `Force` with "remove from properties" → `Engineering` commit without that link, trees match | 2 | same test |
| 2. `Bridge` says `[[Science:physics/Force]]`; rename folder `physics`, move it within `Science`, or dissolve it → `Engineering` commit with the new path, trees match | 3 | same test (folder cases) |
| 4. Move `Force` to notebook `Physics` → `Engineering` commit with `[[Physics:Force]]`, beside the move's two commits; all three trees match | 4 | same test |
| A web action whose operation changes a bound notebook it did not lock is refused and no notebook gets a commit | 5 | focused `AcceptedWebChangeService` test |

## Slices

### 1. Web actions no longer change notes in notebooks the user cannot edit
Type: Behavior
Status: done
Accepted proof: `NotebookGitWebLinkingNotebookControllerTest`
(`renameRewritesOwnLinkingNotebookButNotASubscribedOne`,
`trashRemovingFromPropertiesKeepsTheLinkInASubscribedNotebook`; setup
`seedForceLinkedFrom`) plus 39 regression tests across the link-rewrite suites.
The shared method is `NoteReferenceService.editableInboundReferencesForViewer`.
Proof: new `NotebookGitWebLinkingNotebookControllerTest`, through
`TextContentController` (title rename with reference handling) and the note
trash endpoint with `REMOVE_FROM_PROPERTIES`: a referrer in another user's
notebook that the current user subscribes to keeps its content; a referrer in
the user's own other notebook is still rewritten (database content, as today).
Existing rewrite suites stay green: `NotebookFolderRenameWikiLinkRewriteControllerTest`,
`NotebookFolderDissolveWikiLinkRewriteControllerTest`,
`NotebookGitWebNoteMoveLinkedReferrerControllerTest`.

Behavior: the two change paths (`WikiLinkReferenceCapture.liveResolvedInboundReferences`
and `NoteReferenceHandling.removeNoteLinksFromReferrerProperties`) take only
referrers whose notebook the viewer owns, through one shared method on
`NoteReferenceService`. Display and `isReferencedForViewer` are unchanged.

### 2. Note rename, move and trash reach linking notebooks' Git
Type: Behavior
Status: done
Accepted proof: `NotebookGitWebLinkingNotebookControllerTest`
(`renameCommitsTheRewrittenLinkInTheLinkingNotebook`,
`moveWithinTheNotebookCommitsTheRewrittenLinkInTheLinkingNotebook`,
`trashRemovingFromPropertiesCommitsTheLinkingNotebookWithoutTheLink`; helper
`assertLinkingNotebookCommittedOnce`) plus 202 regression tests. The helper is
`NoteReferenceService.notebooksToLock(Note, User, Integer...)` (single target
note; slice 3 widens it to a folder's notes). `WebNoteEditService.edit`
already had a set form, so no overload was added. `RelationController.webMove`
serves same- and cross-notebook note moves, so cross-notebook note moves
already lock linking notebooks; slice 4 adds their proof and the folder part.
Proof: same test, Git cases for key examples 1 and 3 and the same-notebook
note move: exactly one new commit in `Engineering`, the tip content of
`Bridge`, and `assertAcceptedTreeMatchesTheFullAssembly` for both notebooks.

Behavior: add the shared "notebooks to lock" helper. `WebNoteEditService.edit`
takes an optional set of extra notebook ids (or an overload does). Title
rename with reference handling, same-notebook note move
(`RelationController.webMove`) and trash with `REMOVE_FROM_PROPERTIES` pass
the editable linking notebooks of the note. Content saves pass nothing.

### 3. Folder rename, move and dissolve reach linking notebooks' Git
Type: Behavior
Status: planned
Proof: same test, folder cases for key example 2 (rename, move within
`Science`, dissolve): one commit in `Engineering` with the rewritten path, and
all trees match the full assembly.

Behavior: `FolderRelocationService`'s rename, same-notebook move and dissolve
add the editable linking notebooks of the notes in the folder's subtree to the
set they pass to `applyLiveFolderChange`. Folder trash and permanent delete
leave dead links and add nothing.

### 4. Moves to another notebook reach third notebooks' Git
Type: Behavior
Status: planned
Proof: same test, key example 4: `Engineering` gets one commit with
`[[Physics:Force]]`, and the trees of `Science`, `Physics` and `Engineering`
match the full assembly. `NotebookGitWebNoteCrossNotebookMoveControllerTest`
and `NotebookGitWebFolderCrossNotebookMoveControllerTest` stay green.

Behavior: the note and folder moves to another notebook add the moved
notes' editable linking notebooks to {source, destination}.

### 5. The owner refuses a change to a notebook it did not lock
Type: Behavior
Status: planned
Proof: focused `AcceptedWebChangeService` test: an operation that changes a
note in a bound notebook outside the locked set is refused with 409, and no
notebook gets a commit. Regression, because every web action now passes
through the check: the slice 1–4 tests,
`NotebookGitWebRelationReduceControllerTest`, and
`pnpm cy:run --spec e2e_test/features/note_topology/wiki_link_move.feature,e2e_test/features/folder_organization/folder_organization.feature`
(scenarios unchanged).

Behavior: after the operation flushes, `AcceptedWebChangeService.apply`
refuses when the captured change has rows for a bound notebook it did not
lock. In `docs/notebook-git-synchronization.md` (domain operation ownership),
remove "cross-notebook referrer rewrites remain outside this owner" and state
that the owner refuses a change to a bound notebook it did not lock.

## Current decisions

- No new end-to-end scenario: the frontend is unchanged, and the controller
  tests make the same comparison as local publishing (as plan 046 decided).
- The under-lock check comes last, after every rewriting caller passes its
  linking notebooks. Earlier, it would refuse actions that the later slices
  make correct.
- Rename's "linked from other notes" prompt still counts referrers in
  subscribed notebooks. Answering it then changes nothing there. That is
  harmless and needs no change.

## Learnings

- A subscribed-notebook fixture must also add the subscription to the current
  user's in-memory `subscriptions` list (`owner.getSubscriptions().add(…)`);
  otherwise the referrer is invisible and the test passes without the fix.
- Content saved after "remove from properties" gains `type: Note` in its
  frontmatter; fixtures that compare whole content include it.
