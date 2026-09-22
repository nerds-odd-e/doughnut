# Save note edits at a cost proportional to the change, not the notebook

Status: **planned** (2026-09-22); all decisions settled. No execution
authorization.
Work item: **SEED-034#story-3**.
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
Type: Structure. Status: planned. Enables slice 3.

`NotebookGitCommitBuilder.build/append` take the final tree as a map from
path to blob id plus the contents of blobs that must be inserted; the
existing full-assembly callers hash their entries into that shape. No
behavior change. Proof: focused run of
`NotebookGitWebContentSaveControllerTest`, `NotebookGitHistoryResetControllerTest`
and `NotebookGitFolderRenameControllerTest` passes unchanged.

### 2. Capture the projection change at flush
Type: Structure. Status: planned. Enables slice 3.

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
Type: Behavior. Status: planned.

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
Type: Behavior. Status: planned.

Pre-condition: an empty folder holding `.keep`, and a folder holding one note.
Triggers: create a note in the empty folder; permanently remove the only note
of the other folder. Postcondition: `.keep` removed in the first directory and
restored in the second, the new and removed note paths applied, oracle
equality for each. Proof: example 3's creation half as a test; existing
`NotebookGitNoteCreation*` and `NotebookGitWebPermanentDelete*` unchanged.

### 5. Derive note moves and renames
Type: Behavior. Status: planned.

Pre-condition: a note linked from three referrers, and a note in a folder.
Triggers: rename the title with references rewritten; move the note to
another folder; trash it and recover it. Postcondition: the old path removed,
the new path added, referrers' blobs replaced, `.keep` applied to both
touched directories, `_trash/...` paths appear and disappear, oracle
equality. The fallback now covers only folder-row changes. Proof: example 4
test and example 3's trash half; existing `NotebookGitWebNoteMove*`,
`NotebookGitWebTrash*` unchanged.

### 6. Derive folder changes by prefix
Type: Behavior. Status: planned.

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
Type: Behavior. Status: planned.

Triggers: folder README edit; relationship reduction whose relation note and
source note sit in two notebooks. Postcondition: `README.md` replaced at the
folder's path; each locked notebook gets exactly its own commit from its own
captured rows; the full-assembly fallback is removed from the owner, so every
web change is derived. Proof: existing `NotebookGitWebContentFolderSave*`
and `NotebookGitWebRelationReduce*` unchanged; oracle test covers both.

### 8. One encoder for derived and full trees
Type: Structure. Status: planned. Enables nothing further; closes the story's
structural promise.

The full assembly used by cutover, history reset and the publication drift
check becomes the derivation applied to an empty base with every row as an
insertion. `PortableTreeSnapshot`, `NotebookLivePortableTree` and
`NotebookGitLivePortablePath` are removed or reduced to that one encoder.
Update the synchronization contract's domain-operation paragraph. Proof:
`NotebookGitHistoryResetControllerTest`, `NotebookGitProjectionDriftControllerTest`,
`NotebookGitConcurrentProjectionDriftControllerTest` and the oracle test pass
unchanged; full backend suite green.

### 9. Report the save-time comparison
Type: Behavior (demonstration). Status: planned.

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
