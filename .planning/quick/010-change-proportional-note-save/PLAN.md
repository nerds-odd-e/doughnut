# Save note edits at a cost proportional to the change, not the notebook

Status: **executed** (all nine slices delivered 2026-09-22; awaiting retrospective and wrap-up).
Work item: **SEED-034#story-3**.

Execution identity (Story Branch Mode): worktree
`/Users/terryyin/git/doughnut-worktrees/010-change-proportional-note-save`,
branch `claude/010-change-proportional-note-save`, created by this execution
from fetched `origin/main` at `0e64e66041`; originating and integration
checkout `/Users/terryyin/git/doughnut` (`main`). Authorized remote target for
increments: `origin claude/010-change-proportional-note-save`. Queue claim
published to `origin/main` as `12702ca604` (`pendingCi: unobserved` on trunk).
CI observer: GitHub Actions `ci.yml` ("donut CI"), mailbox
`/tmp/dough-ci-501/watch-h71PBC`, target branch
`claude/010-change-proportional-note-save`. Published revisions on that target:
none yet. Replanning: in-place refinement allowed (no flag given).
Source: [refined story](../../seeds/SEED-034-faster-note-content-saving.md#story-3).
Planning inspection: `a27f572a14` (main, 2026-09-22).

## Outcome and boundaries

Note authors in large notebooks get web saves whose server work depends on
what they changed. Every web accepted change derives its commit's tree from
the accepted head's tree plus the rows the change inserted, updated or
deleted. Unchanged notes are neither rendered nor hashed; unchanged
attachments' bytes are neither read, hashed nor written, wherever they sit.
Path-only changes (folder rename, move, trash, note move) re-list entries
under their new paths from the accepted tree.

Included: all web changes that go through `AcceptedWebChangeService` today
(note content and title edits with referrer rewrites, note creation, move,
trash, recovery and permanent removal, folder creation, rename, move, trash
and permanent deletion, folder README edits, relationship reduction over its
notebook set), the `.keep` and README rules applied locally, canonical no-op
saves, the full assembly re-expressed as the same derivation over an empty
base, and the synchronization contract text.

Excluded: local publication acceptance and its drift check (still full
assembly, infrequent); web attachment upload or deletion; stored blob-id or
hash columns, caches or background work; asynchronous Git (story 5); removing
the attachment bytes column; a retained performance harness; title-edit
latency beyond the natural effect.

Assumptions: no repository on Note, Folder, NotebookAttachment or Notebook
uses bulk update queries (verified 2026-09-22), so Hibernate flush events see
every projection change; Hibernate 7 accepts an `Interceptor` instance through
`hibernate.session_factory.interceptor`; linked worktrees isolate backend
tests automatically (`docs/worktree-backend-tests.md`).

## Existing owners and architecture

- `AcceptedWebChangeService.apply` (backend/src/main/java/com/odde/donut/services/notebookGit/AcceptedWebChangeService.java:56-115)
  locks bindings, runs the operation, flushes, and today assembles the live
  tree twice (`open` :89-98 before, `commitIfChanged` :100-104 after) and
  compares whole path-to-blob maps. It stays the one owner; only how it
  computes the tree changes.
- `NotebookLivePortableTree` and `PortableTreeSnapshot` (services/notebookTree)
  assemble every consumer's full tree from rows, loading attachment bytes via
  `NotebookAttachmentRepository.findPortableTreeRowsByNotebookId`. After this
  story they are one encoder applied to an empty base, used by cutover,
  history reset and the publication drift check only.
- `NotebookGitAcceptedTree.blobIds(repository, commit)` already reads the
  accepted tree as a path-to-blob map without opening blobs: it is the base.
  `NotebookGitCommitBuilder.append` already skips unchanged blobs but takes
  entries with bytes; it becomes map-based.
- `NotebookGitLivePortablePath.ofNote` duplicates the path rule in
  `PortableTreeSnapshot`; both fold into the encoder.
- Trash is an ordinary folder move under `_trash/` (ADR 0004); attachments are
  cascade-deleted with their folder at the database level; `.keep` marks a
  represented folder with no other entry (subfolders count as entries).
- Existing proof surface: `backend/src/test/java/com/odde/donut/controllers/NotebookGitWeb*ControllerTest`
  and `NotebookGitFolder*`, `NotebookGitNoteCreation*`,
  `NotebookGitWebRelationReduce*` exercise every included operation through
  controllers and assert accepted trees, so they are regression proof for the
  derivation. `NotebookGitWebContentSaveControllerTest.savingContentDoesNotLoadEveryStoredNote`
  already asserts a size-independent save with Hibernate statistics and is the
  model for the statement-count proof.

Accepted decisions followed: [ADR 0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
(content authority, one publication boundary, linear history, one commit per
web change, projection and head committed together) and
[ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
(tree contents and encoding unchanged). The
[North Star](../../NORTH-STAR.md) "one accepted-change boundary" is followed:
the owner is extended, no second store of truth. No new North Star topic is
needed. The [synchronization contract's domain-operation paragraph](../../../docs/notebook-git-synchronization.md#domain-operation-ownership)
("read its final projection, then append one accepted commit") is updated in
slice 8 to describe derivation from the projection change, including its
drift sentence (decision 4).

## Outside-in proof

Key examples from the seed and their signals:

1. **Content edit in a large notebook** (seed example 1): controller test with
   30 unrelated notes and 3 attachments (a few KB each); the save's Hibernate
   prepared-statement count equals the count for the same save in a notebook
   with one note and no attachments; the new head's path-to-blob map equals
   the full assembly's map (oracle).
2. **Folder rename carrying attachments** (example 2): the renamed prefix's
   entries keep their blob ids; no query touches `notebook_attachment.content`
   (statement count equal with and without attachments); oracle.
3. **Folder emptiness marker** (example 3): `.keep` removed on first note,
   restored after trashing the last one, and `_trash/...` path appears; oracle.
4. **Title rename with referrers** (example 4): old path removed, new path
   added, referrers replaced; oracle.
5. **Canonical no-op** (example 5): existing tests
   `canonicalNoOpSaveKeepsAcceptedHistoryWhileUpdatingTheNoteTimestamp` and
   `repeatedCanonicalNoOpSaveKeepsTheRevisionCreatedByTheChangedSave` unchanged.
6. **Pre-existing drift** (example 6): `preExistingPortableDriftKeepsTheWebSaveAndAcceptedHistoryUnchanged`
   rewritten to decision 4: the edit is committed on the accepted head and
   the unsynchronized note stays absent from Git.

Oracle: one test class runs each included operation kind through its
controller and asserts `NotebookGitAcceptedTree.blobIds(repository, head)`
equals the map built by the full assembly of the projection. It survives
slice 8 because the full assembly remains as derivation over an empty base.

## Ordered slices

Target about 5 minutes per slice including tests; over 10 minutes, stop and
decompose further unless a stated reason applies.

### 1. Commit from a path-to-blob map
Type: Structure. Status: **done** (2026-09-22). Enables slice 3.

Delivered: `NotebookGitTreeContent(blobIds, blobs)` record (path-to-blob map
plus bytes of blobs that may need inserting; `of(entries)` is the one hasher).
`NotebookGitCommitBuilder.build/append` take it; `writeTree` inserts only blobs
the parent tree does not hold, then lists every path. Callers
(`AcceptedWebChangeService`, `NotebookGitCutoverService`, `NotebookGitProjection`)
wrap the full assembly; the duplicate entries hasher left
`NotebookGitAcceptedTree`. Accepted proof: focused run of the three planned
classes plus `NotebookGitCommitBuilderTest`, `NotebookGitJdbcObjectStoreTest`,
`NotebookGitAcceptedHistoryCompletenessTest`, proposal import/ancestry tests:
32 tests pass, no assertion changed
(`NotebookGitCommitBuilderTest.appendsCompleteSnapshotWithTheSameTreeAsAFreshBuild`
proves tree-id equality of the map shape).

`NotebookGitCommitBuilder.build/append` take the final tree as a map from
path to blob id plus the contents of blobs that must be inserted; the
existing full-assembly callers hash their entries into that shape. No
behavior change. Proof: focused run of
`NotebookGitWebContentSaveControllerTest`, `NotebookGitHistoryResetControllerTest`
and `NotebookGitFolderRenameControllerTest` passes unchanged.

### 2. Capture the projection change at flush
Type: Structure. Status: **done** (2026-09-22). Enables slice 3.

Delivered: `ProjectionChangeCapture` (a `@Component` that is both the Hibernate
`Interceptor`, registered through `HibernatePropertiesCustomizer` with
`AvailableSettings.INTERCEPTOR`, and the per-thread capture window).
`open()` returns an `AutoCloseable` `ProjectionChange`; `change.of(notebookId)`
yields `NotebookProjectionChange{inserted, updated, deleted}` keyed by
`ProjectionRow(kind, id)`, with `updated` holding the first-seen
`PreviousPath(containerId, name)`; Notebook rows count only on readme change.
`AcceptedWebChangeService.apply` opens the window around the whole operation
and still assembles the full tree. Accepted proof: literal `*Capture*` command
(1 test, previous title present after two in-window flushes, nothing outside
the window) plus content-save, folder-rename and concurrent-drift controller
classes, 18 tests green. Assumption verified: no `@Modifying` query on the four
tables.

A small component (working name: projection change capture) binds a
per-transaction collector to a Hibernate `Interceptor` registered through a
`HibernatePropertiesCustomizer`. It records, for Note, Folder,
NotebookAttachment and Notebook rows: inserts, updates with the first-seen
previous path fields (folder or parent id, title, name, filename) and
deletes, grouped by notebook id. The collector is bound only while
`AcceptedWebChangeService.apply` runs the operation and is cleared in
`finally`. Representative infrastructure proof (assumption: Hibernate 7 under
Spring Boot delivers `onFlushDirty` previous state inside the owner's
SERIALIZABLE transaction): a small test calls the component's own capture
entry point around a note title change plus flush and asserts one Note update
with the previous title. Literal command:
`CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test -Dspring.profiles.active=test --tests 'com.odde.donut.services.notebookGit.*Capture*'`.
Critical postcondition: previous title present, nothing recorded outside the
capture window. Record the result here before slice 3.

### 3. Derive a content edit's commit from the changed note
Type: Behavior. Status: **done** (2026-09-22).

Delivered: the before-snapshot is gone from `AcceptedWebChangeService`
(decision 4); `NotebookGitDerivedTree.of(change, acceptedBlobIds)` returns the
accepted map with each in-place Note update's blob replaced, or empty for any
other change kind, which still falls back to the full assembly. Accepted
proof: full `NotebookGit*` controller run plus the capture test, 376 tests
green. Example 1 cost signal:
`NotebookGitWebContentSaveCostControllerTest.savingContentCostsTheSameStatementsInALargeNotebookWithAttachmentsAsInASmallOne`
(statement-count equality and no query naming `NotebookAttachment`);
example 1 tree exactness and the oracle:
`NotebookGitDerivedTreeOracleControllerTest.contentEditMatchesTheFullAssembly`;
example 6: `preExistingPortableDriftIsNeitherBlockingTheWebSaveNorAdoptedByIt`;
example 5 tests unchanged. Nine former before-snapshot drift tests (content
save, folder dissolve, rename, move, trash, creation; note creation and
creation in folder; relation reduce) now assert decision 4's first half:
the operation is committed on the accepted head.

Pre-condition: Git-backed notebook with unrelated notes and attachments.
Trigger: save new content for one note. Postcondition: one commit whose tree
is the parent tree with that note's blob replaced; the request's statement
count equals the small-notebook count; oracle equality. The owner removes the
before-snapshot (decision 4) and derives when the capture
holds only note updates without path changes; any other captured change kind
still falls back to the full assembly. No-op stays tree-id equality. Proof:
example 1 test, examples 5 and 6 tests, focused `NotebookGitWebContentSave*`
run.

### 4. Derive note additions and removals with the folder marker rule
Type: Behavior. Status: **done** (2026-09-22).

Delivered: `NotebookGitDerivedTree.of` handles any mix of Note inserts,
deletes and in-place updates: removed paths come from the captured deleted
row's `RowPath`, added entries are hashed, then `applyEmptyDirectoryMarker`
applies the `.keep` rule to each touched directory prefix. Capture now holds
inserted entity instances (ids exist only after insert) and each deleted row's
`RowPath`. Accepted proof: `NotebookGit*` plus `services.notebookGit.*`, 385
tests green; oracle tests
`firstNoteInAnEmptyFolderReplacesItsMarkerAndMatchesTheFullAssembly` and
`permanentlyRemovingTheOnlyNoteOfAFolderRestoresItsMarkerAndMatchesTheFullAssembly`;
`NotebookGitNoteCreationControllerTest.preExistingPortableDriftIsNeitherBlockingTheNoteCreationNorAdoptedByIt`
now asserts the drifted note stays absent.

Pre-condition: an empty folder holding `.keep`, and a folder holding one note.
Triggers: create a note in the empty folder; permanently remove the only note
of the other folder. Postcondition: `.keep` removed in the first directory and
restored in the second, the new and removed note paths applied, oracle
equality for each. Proof: example 3's creation half as a test; existing
`NotebookGitNoteCreation*` and `NotebookGitWebPermanentDelete*` unchanged.

### 5. Derive note moves and renames
Type: Behavior. Status: **done** (2026-09-22).

Delivered: a Note update whose captured previous `RowPath` differs from its
current path removes the previous path and adds the current entry; referrer
rewrites are ordinary in-place updates. The fallback now covers only Folder,
NotebookAttachment and Notebook readme rows. Accepted proof: `NotebookGit*`
plus `services.notebookGit.*`, 388 tests green; oracle tests
`renamingALinkedNoteReplacesItsPathAndItsReferrersOnlyAndMatchesTheFullAssembly`
(example 4, with the no-`NotebookAttachment`-query signal),
`movingANoteToAnotherFolderMovesItsPathAndMarkersAndMatchesTheFullAssembly`,
`trashingAndRecoveringTheOnlyNoteOfAFolderMoveItsPathAndMatchTheFullAssembly`
(example 3 trash half).

Pre-condition: a note linked from three referrers, and a note in a folder.
Triggers: rename the title with references rewritten; move the note to
another folder; trash it and recover it. Postcondition: the old path removed,
the new path added, referrers' blobs replaced, `.keep` applied to both
touched directories, `_trash/...` paths appear and disappear, oracle
equality. The fallback now covers only folder-row changes. Proof: example 4
test and example 3's trash half; existing `NotebookGitWebNoteMove*`,
`NotebookGitWebTrash*` unchanged.

### 6. Derive folder changes by prefix
Type: Behavior. Status: **done** (2026-09-22), nested-ancestor composition
included; the split rule was not needed.

Delivered: `NotebookGitDerivedTree.of` accepts Note and Folder rows. Inner
`ChangedFolders` composes each changed folder's captured previous path through
its changed ancestors (unchanged ancestors from the live entity), re-lists
every accepted key under a changed prefix at its current prefix with the same
blob id (deepest changed ancestor wins, deleted prefix drops its keys and so
its cascade-deleted attachments), and names the touched directories in
current coordinates; markers are applied deepest first. Accepted proof:
`NotebookGit*` plus `services.notebookGit.*`, 393 tests green before the
refactor; after the refactor the oracle, folder, trash, note-move, creation
and content-save classes, 111 tests green. Example 2 and the folder cases live
in `NotebookGitDerivedFolderTreeOracleControllerTest`:
`renamingAFolderRelistsItsNotesAttachmentsAndSubfolderUnderTheNewPrefixAndMatchesTheFullAssembly`
(after-map equals before-map re-keyed, no `NotebookAttachment` query),
`creatingAFolderAddsItsMarkerAndMatchesTheFullAssembly`,
`trashingAndRecoveringAFolderMoveItsEntriesWithTheirBlobsAndMatchTheFullAssembly`,
`permanentlyDeletingAFolderDropsItsNotesAndAttachmentsWithItsPrefixAndMatchesTheFullAssembly`.
The five folder drift tests now assert the drifted note stays absent.

Pre-condition: a folder holding notes and attachments, with a nested
subfolder. Triggers: folder create, rename, move (including into `_trash/`
and back), permanent delete; a rename of both a folder and its subfolder in
one operation where that occurs. Postcondition: entries under the changed
prefix are re-listed with their existing blob ids (deepest changed ancestor
wins), cascade-deleted attachments disappear with their prefix, `.keep`
applies to the old and new parent directories, no attachment content query
(statement count equal with and without attachments), oracle equality.
Proof: example 2 test; existing `NotebookGitFolder*`, `NotebookGitWebFolder*`
unchanged.

### 7. Derive README edits and multi-notebook changes, retire the fallback
Type: Behavior. Status: **done** (2026-09-22).

Delivered: `NotebookGitDerivedTree.of` is total (no kind gate, no
`Optional`); a Folder or Notebook row change refreshes that prefix's
`README.md` through `PortableTreeEntry.ofReadme` (one README rule shared with
the full assembly) before markers; `AcceptedWebChangeService` no longer
depends on `NotebookLivePortableTree`; `NotebookGitChangedFolders` was
extracted; path arithmetic lives in `NotebookGitLivePortablePath`. Multi-notebook
`apply` already committed each opened notebook from its own captured rows.
Accepted proof: `NotebookGit*` plus `services.notebookGit.*`, 395 tests
green before the refactor, 116 across the derivation, full-assembly consumer
and snapshot classes after it. README examples through the owner's `apply`
entry point: `NotebookGitDerivedFolderTreeOracleControllerTest.editingAFolderReadmeReplacesItsMarkerWithTheReadmeAndMatchesTheFullAssembly`
and `editingTheNotebookReadmePutsItAtTheRootWithoutAMarkerAndMatchesTheFullAssembly`;
two-notebook reduce: `NotebookGitWebRelationReduceControllerTest.reduceAcrossNotebooksDerivesEachNotebookFromItsOwnRowsAndMatchesBothFullAssemblies`,
and the drifted-source test now asserts the unsynchronized note stays absent.

Triggers: folder README edit; relationship reduction whose relation note and
source note sit in two notebooks. Postcondition: `README.md` replaced at the
folder's path; each locked notebook gets exactly its own commit from its own
captured rows; the full-assembly fallback is removed from the owner, so every
web change is derived. Proof: existing `NotebookGitWebContentFolderSave*`
and `NotebookGitWebRelationReduce*` unchanged; oracle test covers both.

### 8. One encoder for derived and full trees
Type: Structure. Status: **done** (2026-09-22). Closes the story's structural
promise.

Delivered: `NotebookGitTreeEncoder` (renamed from `NotebookGitDerivedTree`)
has `derive(change, acceptedBlobIds)` and `fullTree(notebook)` /
`fullTree(notebook, folders, storedNotes)`, both ending in one private
`encode`; the full assembly is that encode over an empty map with every row as
an insertion, and attachments with bytes and the root readme enter only there.
`PortableTreeSnapshot`, `NotebookLivePortableTree` and
`NotebookGitLivePortablePath` are deleted; path arithmetic is
`NotebookGitPortablePath`, whose `folderPrefixes(rows)` is the one folder-row
walk (stray rows whose ancestry leaves the notebook get no prefix, as the old
assembler skipped them; `NotebookFollowsFolderContainmentMigrationTest` relies
on that). Contract text updated in `docs/notebook-git-synchronization.md`
(domain operation ownership) and `docs/note-content-saving.md`. Net about 80
lines fewer in the whole change. Accepted proof: full backend suite
`pnpm backend:test_only`, 535 classes, 2575 tests, 0 failures, rerun after the
refactor; `NotebookGitTreeEncoderTest` (2), `NotebookGitCutoverServiceTest`,
`NotebookGitHistoryResetControllerTest`, both oracle classes (12), projection
drift classes (6).

The full assembly used by cutover, history reset and the publication drift
check becomes the derivation applied to an empty base with every row as an
insertion. `PortableTreeSnapshot`, `NotebookLivePortableTree` and
`NotebookGitLivePortablePath` are removed or reduced to that one encoder.
Update the synchronization contract's domain-operation paragraph. Proof:
`NotebookGitHistoryResetControllerTest`, `NotebookGitProjectionDriftControllerTest`,
`NotebookGitConcurrentProjectionDriftControllerTest` and the oracle test pass
unchanged; full backend suite green.

### 9. Report the save-time comparison
Type: Behavior (demonstration). Status: **done** (2026-09-22).

Measured with the harness recovered from the parent of `a823dbbafe` into the
job scratch directory (nothing retained in the repository; one harness-only
fix: its export capture used the ZIP export endpoint removed in `670f8e7313`,
replaced by the accepted-history bundle download). Fixture: 11,000 notes, 40
folders, 28 root attachments totalling 12,554,240 bytes, identical on both
sides (same accepted tree id `0d8b524a` before any measured save). Sequential
runs on the same machine and MySQL, each backend compiled from a clean classes
directory, each run with its own isolated E2E database. Medians of samples 1
to 5 (sample 0 discarded), milliseconds:

| Save kind | Boundary | Before (`0e64e66041`) | After (`a6f5223fbb`) |
|---|---|---|---|
| keeps existing wiki links | request | 1297 | 180 |
| keeps existing wiki links | keystroke to settled editor | 2331 | 1215 |
| adds a wiki link | request | 1301 | 183 |
| adds a wiki link | keystroke to settled editor | 1338 | 215 |

The 1 s debounce before the request is unchanged (about 1006 vs 1010 ms on
the existing-links kind; immediate on the added-link kind). Spread within
each five-sample series is under 35 ms. Before-run request time matches the
seed's 2026-09-21 evidence (about 720 ms base plus 515 ms for the
attachments). Logs: job scratch `baseline-run.log` and `story-run.log`.
Caveat: a loaded developer laptop (dev stack, dashboard and Gradle daemons
were running for both runs), so absolute numbers are indicative; the ratio is
the finding.

Recover the deleted measurement harness temporarily from the parent of
`a823dbbafe` into the job scratch directory, run the 11,000-note fixture with
its 28 attachments against `main` before this story and against the story's
final commit, and record request medians here. Nothing from the harness is
retained in the repository. Proof: the recorded numbers in this plan.

## Current decisions

1. Derive, do not rebuild: the accepted head's tree is the base for every web
   commit; Git commits remain full snapshots shared by object id.
2. The owner captures the projection change at flush through Hibernate;
   domain operations declare nothing; rows in unlocked notebooks are ignored.
3. One encoder over path-to-blob maps; new blobs inserted only for added or
   replaced content; `.keep` evaluated on the tree for touched directories.
4. Owner decision 2026-09-22: drop the whole-tree before-snapshot. A derived
   commit never adopts drift at untouched paths, records the edit as ADR 0002
   requires, and drift stays detectable at local publication. The path-scoped
   pre-change content check was rejected.
5. No-op detection is tree-id equality.
6. Publication acceptance is untouched.

## Proof commands and ownership

- Focused backend tests from the execution worktree:
  `CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test -Dspring.profiles.active=test --tests '<fully qualified test class>'`
- Full backend suite (slice 8 and before delivery):
  `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- Each slice owns the tests named in its proof; the oracle test is created in
  slice 3 and extended in slices 4 to 7.

## Learnings

- Slice 1: derivation recipe for slice 3 is copy `acceptedBlobIds` (already
  read in `AcceptedWebChangeService.open`), put `path -> Formatter.idFor(BLOB,
  bytes)` for each changed note, remove deleted paths, and pass
  `new NotebookGitTreeContent(map, replacedBlobs)`; no-op stays
  `blobIds().equals(acceptedBlobIds)`.
- The before-snapshot could not survive for the fallback path (it is taken
  before the operation runs), so slice 3 removed it entirely and rewrote all
  nine drift tests to "the operation is committed on the accepted head". The
  fallback still adopts drift until each operation is derived; slices 4 to 7
  add the "drifted note stays absent" assertion for the operations they
  derive (note creation in slice 4; folder creation, rename, move, trash,
  dissolve in slice 6; relation reduce in slice 7).
- Slice 3: statement-count equality alone also holds for the full assembly
  (fixed query count); the distinguishing signal is that no executed query
  names `NotebookAttachment` (`Statistics.getQueries()` after `clear()`).
  Slice 6 should use that signal. `ProjectionChangeCapture.RowPath(containerId,
  name)` is the row path record (renamed from `PreviousPath`; `updated` holds
  the previous one). `PortableTreeEntry.ofNote(path, contentOrNull)` is the one
  note-file rule. `AcceptedHistory.parents()` / `tipPaths()` in
  `GitBundleTestReader` make "one commit appended" assertions two lines.
  `.keep` belongs inside `NotebookGitDerivedTree.of` after keys are added and
  removed, evaluated on touched directory prefixes.
- Slice 4: `of` is kind gate → added entries and removed paths → apply to a
  copy of the map → `applyEmptyDirectoryMarker` per touched prefix. A
  path-changed Note update (slice 5) contributes its previous path to removals
  and its new entry to additions; the empty return on path mismatch is the only
  line to replace. Cascade-deleted attachments (slice 6) arrive in `deleted`
  with `RowPath(folderId, filename)` but their folder may be deleted too, so
  remove them by the deleted folder's previous prefix, not by folder lookup.
  `NotebookGitLivePortablePath.ofNote(Folder, title)` is the one note-path
  rule; the `.keep` rule is duplicated in `PortableTreeSnapshot` until slice 8.
- Slice 5: trashing a note whose `_trash/<folder>` mirror does not exist yet
  inserts the `_trash` root and mirror folders (find-or-create) before the
  note move, so the first trash from a folder is a Folder insert and falls
  back until slice 6; recovery never deletes the emptied mirror. Test probes:
  `NotebookGitWebContentControllerTestBase.hibernateStatisticsOf(operation)`
  for the no-attachment-query signal; `ControllerTestBase.titleDto(title)`.
- Slice 6 observation (product gap, outside this story's boundary): the
  folder and notebook README endpoints
  (`NotebookFolderController.updateFolderReadmeContent`,
  `NotebookController.updateNotebookReadmeContent`) save and flush directly
  and never enter `AcceptedWebChangeService`, so a web README edit creates no
  accepted commit today. The plan's "included" list assumed otherwise. Not
  routed here; recommended as a backlog follow-up at wrap-up. Slice 7 still
  makes the derivation total: a Folder or Notebook row update refreshes that
  prefix's `README.md`, proven through the owner's `apply` entry point.
- Slice 6: no web path inserts or updates a `NotebookAttachment` row;
  cascade-deleted attachment rows are not captured (database cascade) and
  vanish with their deleted folder's prefix.
- Slice 7: the oracle helper `assertAcceptedTreeMatchesTheFullAssembly`
  reloads the notebook row because controller tests run without a transaction
  and a caller's `Notebook` instance is detached; slice 8 must keep comparing
  against stored rows. With the gate gone, a `NotebookAttachment` row update
  (unreachable through `apply` today; web attachment upload is out of scope)
  would remove its previous path without re-adding it; note for the
  retrospective. Slice 8 needs attachment entries (bytes from
  `NotebookAttachmentRepository.findPortableTreeRowsByNotebookId`) and the
  root readme fed directly, since `of` today adds attachments never and the
  root readme only from a captured Notebook row.
- Slice 8: the old assembler silently dropped rows whose folder ancestry
  left the notebook (pre-`V300000333` data); only the full suite exercises that
  shape. `services/notebookTree` keeps its package: its records are JPQL
  projections and ADR 0004 format concepts, not Git mechanics.
- Retrospective candidate (not acted on): the committed referrer-authoring
  pattern `inCommittedTransaction(..., () -> authorReferencingContent(...))`
  has about twenty copies across fourteen NotebookGit test files.

- Slice 2: interceptor callbacks receive real entity instances and lookups
  are keyed by exact entity class; `open()` overwrites any existing window,
  so `apply` must never re-enter itself (its five callers do not).

## Remaining concerns

- Slice 6 (folder prefixes) is the widest single proof loop: nested changed
  folders and cascade-deleted attachments share one rule but several
  arrangements. If it passes the hard limit, split nested-folder handling
  from the single-folder cases rather than pushing through.
- The interceptor is session-factory global; the per-transaction binding must
  not leak between requests. Slice 2's proof covers the capture window but not
  concurrency; the concurrent-writer tests in slice 3's focused run do.
- Previous state at multiple flushes: the collector must keep the first-seen
  previous path fields; a mid-operation flush followed by a second change would
  otherwise lose the original path.
- Statement-count equality as a proof depends on identical lazy-loading shape
  between the two notebooks; if it proves brittle, replace it with a
  Hibernate statement inspector asserting no `notebook_attachment.content`
  read, not with a timing threshold.
