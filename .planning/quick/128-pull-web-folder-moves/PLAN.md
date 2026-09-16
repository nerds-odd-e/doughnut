# Pull web folder moves into a local notebook

Status: completed
Source: [SEED-009 story 40](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-40).

## Execution identity

- Originating checkout: `/Users/terryyin/git/doughnut` on `main` (claim `e51bc97c5c`)
- Execution checkout: `/Users/terryyin/git/doughnut/.worktrees/128-pull-web-folder-moves`
- Execution branch: `quick/128-pull-web-folder-moves`
- Integration target: `main`

## Goal and boundaries

After a web folder move in one synchronized notebook, pull into a clean local
checkout receives the complete new layout. Cells exists exactly once, at its new
path; its old path is absent. Content, folder documents, empty descendants,
in-notebook reference rewrites, server identities and learning state survive.
The checkout has no unpublished commits and may lag by multiple accepted commits.
Use existing destinations including root, and ordinary non-conflicting moves.

No subsequent local editing/publication scenario. No cross-notebook synchronization,
folder merge promise, trash/recovery extension, new destination creation, divergence
or drift repair, special relationship transformation, or performance target.
Preserve existing behavior; these exclusions are not new runtime prohibitions.
Core fixtures have no external referrers. Deferred broader outcomes remain in the seed.

## Existing-solution assessment

Inspected current source at ce4b7fd252; no behavioral tests executed during planning.

- `NotebookController.moveFolder` and `FolderRelocationService` already own the
  public operation. `FolderMoveRelocation` owns placement and delegates existing
  reference rewriting. Reuse them rather than rebuild a Git-specific move.
- `AcceptedWebChangeService.apply` owns lock/load, pre-change drift comparison,
  complete mutation, final persisted-tree projection and accepted commit in one
  SERIALIZABLE transaction. `WebNoteEditService` keeps note authorization and
  domain recipes, then delegates. Same-notebook folder moves go through
  `FolderRelocationService.moveFolderWithinNotebook` → `apply`, reloading the
  moving folder under the lock; `FolderMoveRelocation` still owns placement and
  destination parent resolution. Cross-notebook `moveFolder` stays on its
  DEFAULT `@Transactional` path.
- Affected existing callers: content/title saves, `RelationController` same-notebook
  note moves, `NoteTrashService` and `NoteTrashUndoService`. Preserve all on the same
  owner. Creation services also use `AcceptedSnapshotPersistence`; their creation
  semantics do not justify rewriting them in this story.
- `PortableTreeSnapshot`, `NotebookGitProjection`, `AcceptedSnapshotPersistence`
  already encode and store complete trees. Reuse them after all reference mutations.
- CLI `notebookPull.ts` already downloads accepted history and fast-forwards an
  ancestor checkout. No folder-specific client protocol is indicated.
- Existing E2E folder-page steps perform the real move. The CLI clone/pull steps
  and `notebookCloneCheckoutObservations.expectCanonicalTreeFor` observe the exact
  file list. Extend these stable boundaries rather than add a second harness.

Follow [North Star: One complete accepted web change](../../NORTH-STAR.md#one-complete-accepted-web-change).
Accepted ADR 0004 owns Portable content and authored references, ADR 0005 keeps
web note identity independent of path, and ADR 0003 owns retained learning state.
ADR 0002 remains Proposed. No North Star or ADR change is needed.

## Ordered slices

### 1. Share the complete accepted-change boundary with folder operations
Type: Structure
Status: done

Modularize the notebook-level acceptance responsibility currently in
`WebNoteEditService`, keeping note authorization/domain recipes at their owners.
Existing note edits, moves, trash and Undo delegate to the same owner. Expose the
minimum subject-independent operation needed by the immediately following folder
move. Keep lock/load and final projection in the same proxied transaction; avoid
joining a DEFAULT-isolation controller transaction before requesting SERIALIZABLE.
No second snapshot algorithm, subject dispatch mode, event journal or identity map.

Proof: existing backend controller coverage for web content/title history, note
moves and linked referrers, trash/Undo, queued writers, non-Git behavior and drift
continues to pass. Inspect those assertions before accepting their coverage.
Command: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
Accepted: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed
(`BUILD SUCCESSFUL in 1m 8s`, worktree DB
`doughnut_wt_206029a6befd45a58f2b83f5cde2e888_test`). Inspected setup is Git-backed
fixtures (`snapshotCurrentPortableTree` / `createGitBackedNotebook`), non-Git
without bindings, and queued races via `NotebookGitConcurrentWriterTestSupport`.
Inspected observations: controller save/move/trash/Undo/drift tests through
`TextContentController`, `RelationController`, and `NoteController` into
`WebNoteEditService` → `AcceptedWebChangeService` (see
`NotebookGitWebContentSaveControllerTest`,
`NotebookGitWebNoteMoveControllerTest`,
`NotebookGitWebNoteMoveLinkedReferrerControllerTest`,
`NotebookGitWebNoteMoveGuardControllerTest`,
`NotebookGitWebTrashControllerTest`,
`NotebookGitWebTrashUndoControllerTest`,
`NotebookGitWebTrashQueuedWriterControllerTest`,
`NotebookGitProjectionDriftControllerTest`). Folder callers remain unwired.
Safe stopping point: existing product behavior unchanged; folder integration is next.
Sizing: approximately 5 minutes active work; full backend suite has an explicit
verification-time exception. If extraction itself exceeds 10 minutes, stop and
refine this slice around observed coupling before extending it.

### 2. Pull a web folder move without leaving a second copy
Type: Behavior
Status: done

From a synchronized notebook and clean checkout with Biology/Cells.md and Study/,
move Biology under Study on the web, then pull. Exactly one Cells file exists at
Study/Biology/Cells.md with the same bytes, old path absent, original head retained
as ancestor and checkout clean at the accepted head. Donut keeps the note/tracker
IDs and learning records. Do not edit locally or publish afterward.

First add and run the outside-in failing example before changing folder integration.
Reuse an existing CLI feature or add capability-named
`e2e_test/features/cli/cli_notebook_web_folder_moves.feature` using installed CLI and
folder-page UI steps. The initial Git snapshot is fixture setup only; never refresh
it after the web move. Use exact file-list observation to prove no duplicate.
Route the existing same-notebook folder operation through slice 1's owner, with
complete placement and reference work before final projection. Keep cross-notebook
behavior on its existing path. Do not gate by note count or subtree shape.

Controller proof: downloadable accepted tree has the new path once and one new
parented commit; database identity and learning state remain. Preserve permission,
self/descendant and collision rules with existing folder tests; add Git-head and
placement invariance observations for a synchronized destination-conflict fixture.
The conflicting folder must exist before its baseline Git snapshot. Check non-Git
moves create no binding and drift keeps the previous accepted head, reusing suitable
existing proof only where it reaches this folder caller.

Commands: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` and
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_web_folder_moves.feature`
(adjust the feature path if extending an existing feature).
Accepted:
- `unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL INPUT_DB_URL; CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed (`BUILD SUCCESSFUL in 1m 4s`). Setup: `seedLearnedCellsInBiologyWithEmptyStudy` / `snapshotCurrentPortableTree`; occupying Biology under Study **before** snapshot for collision; unsynchronized note **after** snapshot for drift. Observations: `NotebookGitWebFolderMoveControllerTest.webFolderMoveAppendsAcceptedChildAndRetainsNoteAndLearningIdentity` (`Study/Biology/Cells.md` once, not `Biology/Cells.md`, one parented commit, note/tracker/recall retained); `destinationCollisionLeavesPlacementAndAcceptedHeadUnchanged`; `nonGitNotebookFolderMoveCreatesNoBinding`; `preExistingPortableDriftKeepsTheFolderMoveAndAcceptedHistoryUnchanged`.
- Isolated `pnpm cy:run --spec e2e_test/features/cli/cli_notebook_web_folder_moves.feature` passed (`1 passing`). Setup: Git binding snapshot in Background before clone, never refreshed after the folder-page move. Observations: exact checkout `[Study/Biology/Cells.md]`, Cells bytes unchanged, original head ancestor and clean accepted head. No local edit/publish.
Safe stopping point: core journey and preserved boundaries green, no deliberate
failing test committed. Sizing: 5–10 minutes active work, low confidence; mandated
backend-suite and installed-CLI startup time excepted. At 10 active minutes stop
and refine if integration remains multi-beat; retain red/green evidence.

### 3. Receive the complete subtree and rewritten references
Type: Behavior
Status: done

Extend the same folder-move rule with a folder Readme, nested learned note, empty
folder and an in-notebook body/frontmatter path referrer. Pull receives exactly the
final subtree plus normal rewritten reference bytes. No original descendant paths
remain. Use controller bundle observations for projection details and the established
CLI observation for installed checkout behavior; avoid duplicating every assertion
at both boundaries. Ordinary conflict/merge handling is not redesigned.

Proof: a controller test via moveFolder then bundle download inspects complete
Portable paths/content and referrer rewrite. The installed pull boundary is already
owned by slice 2; do not duplicate this projection matrix there.
Command: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
Accepted: `unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL INPUT_DB_URL; CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed (`BUILD SUCCESSFUL in 1m 28s`). Setup: `seedCompleteBiologySubtreeWithReferrer` (Biology Readme, learned Cells, empty Empty, Study, Reading authored `REFERRER_BODY_BEFORE`, then snapshot). Observation: `NotebookGitWebFolderMoveControllerTest.webFolderMoveProjectsCompleteSubtreeAndRewrittenInNotebookReferences` — exact paths `Study/Biology/README.md`, `Study/Biology/Cells.md`, `Study/Biology/Empty/.keep`, `Reading.md`; README assemble bytes; Cells body unchanged; empty `.keep`; `Reading.md` rewritten to `Study/Biology/Cells` in body and frontmatter. Production path unchanged from slice 2.
Safe stopping point: full subtree projection observed, with the same domain rule.
Sizing: about 5 minutes active work; full backend-suite time excepted. If projection
needs another mechanism, stop and reassess rather than add a special-case recognizer.

### 4. Pull accumulated folder moves to the final layout
Type: Behavior
Status: done

From the clean checkout before either change, move Biology under Study and then
back to notebook root on the web before one pull. Observe both accepted commits
in the ancestor chain, a clean checkout at the accepted head and exactly one final
Cells copy at Biology/Cells.md. An unchanged final file list alone is insufficient:
the advanced history proves pull received the accepted changes.

Proof: extend the installed-CLI folder-move feature, using existing folder-to-root
and exact-tree steps. The web operations, rather than setup, must append both heads.
Command: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_web_folder_moves.feature`.
Accepted: isolated `pnpm cy:run --spec e2e_test/features/cli/cli_notebook_web_folder_moves.feature` passed (`2 passing`) after observation-helper cleanup. Setup: Background Git snapshot before clone; clone before either web move; snapshot not refreshed. Observations: record accepted head after Biology→Study and after move to root; both recorded heads are ancestors of pulled HEAD; original clone head is ancestor; checkout clean at accepted head; exact tree `Biology/Cells.md` and leftover empty `Study/.keep`. Production pull unchanged (ordinary fast-forward).
Safe stopping point: root destination and accumulated accepted history proved.
Sizing: about 5 minutes active work; CLI installation/stack startup wait excepted.
No new multi-commit algorithm is expected; a discovered need triggers reassessment.

## Proof ownership

| Promise | Owner and observation |
| --- | --- |
| New path once, old path absent, identical content | Slice 2 exact installed-checkout list and file content |
| Existing note and learning continuity | Slice 2 controller persisted IDs, tracker state and recall history |
| Folder Readmes, nested notes, empty descendants, in-notebook references | Slice 3 downloaded complete Portable tree |
| Root move and several accepted changes before pull | Slice 4 real pull, ancestry and exact final paths |
| Existing permissions, conflicts, non-Git behavior and drift | Slice 2 folder-boundary proof; slice 1 shared-owner regressions |
| Complete mutation and append-only accepted state | Slice 2 parent/tree observation and slice 3 final projection |
| Existing note-edit/move/trash/Undo behavior | Slice 1 inspected existing regression coverage |

## Delivery and assessment

Execution uses dough-execute-plan. Same-notebook folder moves hit
`AcceptedWebChangeService.apply` with no outer DEFAULT transaction. Reload the
moving folder under the lock; destination parent reload already lives in
`FolderMoveRelocation`. Slice 3 observed complete subtree projection on that
same rule (no extra snapshot algorithm). Slice 4 proved accumulated accepted
heads via recorded binding SHAs and `git merge-base --is-ancestor` on one pull.
CI observation: host bridge probe did not attach `CI_MONITOR_READY`; pushes
were not observed.

Target ~5 minutes including ordinary verification; >5 merits scrutiny and >10
requires finer decomposition unless the stated suite/runtime wait exception applies.
Record actual waits separately; the exception does not cover implementation thrash.

## Learnings

Moving `@Transactional(SERIALIZABLE)` onto `AcceptedWebChangeService.apply` is
required so `saveTitle` / `saveContent` self-calls of `edit` still join the
serializable transaction. Same-notebook folder move must not sit in a DEFAULT
controller/service transaction around `apply`. Destination `findById` in the
service lambda duplicated `FolderMoveRelocation.resolveNewParentFolder`. Complete
subtree projection is the existing snapshot after placement plus existing wiki-link
rewrite, not a folder-specific codec. Returning Biology to root leaves empty
`Study/.keep`. Recording binding heads after each web move proves pull received
both accepted commits; a final file list alone would not. 

## Plan-refinement assessment — 2026-09-16

Applied dough-slice-plan-refinement after initial construction. Replaced the combined
subtree/accumulated-moves slice with slices 3 and 4: complete projection and catching
up across multiple accepted operations have distinct observations and can stop safely
independently. Result: four slices, no source-scope change and no new plan.

Slice 1 is one cohesive extraction enabling slice 2. Slice 2 retains its complete
web-to-installed-checkout proof, including relevant preservation checks, rather than
splitting backend implementation from a temporarily failing E2E test. Its 5–10 minute
active-work estimate exceeds the target: existing UI/pull/checkout steps and unchanged
folder domain behavior bound the change, but transaction integration is the concrete
sizing concern. Keep the 10-minute active-work stop; do not disguise backend suite or
CLI startup latency as implementation work. Slices 3 and 4 now have single proof loops.
No resplit recommendation (four slices). No open product decision or known technical
blocker remains. Timing estimates remain hypotheses, with
slice 2's integration concern explicitly retained rather than certifying its duration.
