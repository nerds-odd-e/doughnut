# Save a note by editing its Git ancestor trees

Status: in progress; slices 1–2 delivered, slices 3–4 planned.
Work item: **SEED-037#story-1**.
Source: [refined story](../../seeds/SEED-037-note-save-cost-independent-of-folder-count.md#story-1)
and the owner's 2026-09-22 acceptance of ancestor-only reads, request for
critical design review and slice planning, and preference for simpler code.

## Goal and boundaries

Saving one note's content at an unchanged path should take about a second on
the reported large notebook, without visiting unrelated Git subtrees to retain
history. Reads may depend on edited-path depth. Preserve complete captured
operations, one commit per changed Portable tree, no commit for unchanged
content, and atomic application/Git publication.

The shared web-save tree derivation covers notes, folders, README and existing
attachment preservation. Keep their current behavior, including moves, trash,
renames and empty folders. No endpoint-specific fast path. Whole-tree import,
cutover/reset, projection drift checks and bundle download may retain their
legitimate complete reads. No schema change, persistent index, cross-request
cache, async worker, new synchronization protocol or API change is planned.
Deeper read/write round-trip reduction is [story 2](../../seeds/SEED-037-note-save-cost-independent-of-folder-count.md#story-2).

## Design decisions and critical review

Follow [North Star: One notebook tree, One format boundary, One accepted-change
boundary](../../NORTH-STAR.md), [ADR 0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
and its [publication contract](../../../docs/notebook-git-synchronization.md),
and [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md).
No new architectural policy or North Star topic is needed.

The existing owners remain: `ProjectionChangeCapture` records the complete
operation, `NotebookGitTreeEncoder` maps it to Portable content, the commit
builder creates native Git history, and the JDBC store persists it atomically.
The gap is tree editing, not another save service or another source of truth.

Use a small directory-tree representation: a directory owns named file or
child-tree entries; unchanged child trees can stay as object IDs. Resolve a
directory when an edit needs it and retain that read within this operation.
Put/remove files and relocate affected directory content through that model.
Serialize changed directories bottom-up using JGit's format and ordering;
reuse other object IDs. Do not implement a general filesystem, command/event
framework, custom hash, lazy `Map` facade, or a durable path cache.

Keep Portable rendering, README omission and `.keep` normalization in one
encoding owner. Full assembly may still enumerate all application rows and
produce a complete result, but must reuse those same format rules. A complete
snapshot and edits to an accepted tree are different inputs, not separate
content authorities. Do not force whole-tree callers to become incremental or
retain an obsolete production encoder merely for test fixtures.

Critical cases resolved by the design:

- **No-op:** compare the resulting native root tree ID with the parent before
  flushing buffered objects or creating a commit. A changed timestamp alone
  does not imply changed Portable content. Do not use whole-map equality.
- **Folder relocation:** preserve before/after paths captured by the existing
  owner. Resolve sources against the accepted tree before mutating destinations;
  handle parent/child relocations by the existing deepest-prefix semantics.
  An unchanged subtree can move by ID. If composition requires enumeration,
  enumerate only the affected subtree, never the whole notebook. Reuse the
  existing relocation rules; do not invent a second placement algorithm.
- **Empty folders:** distinguish explicitly retained empty folders from
  directories actually removed by the operation. Re-evaluate affected
  directories deepest-first; a child tree is representation without needing
  to open its descendants. Root never gets `.keep`.
- **Complete operation:** multiple changed notes/referrers share ancestors;
  combine their effects before writing each ancestor. Do not infer the change
  set from the requested note alone or add endpoint-maintained path lists.
- **Native correctness:** preserve regular-file modes and JGit byte ordering,
  including non-ASCII names and file/directory name-prefix cases. Root tree-ID
  equality with independent full assembly proves more than path/blob equality.
- **Durability:** objects, ref compare-and-swap and projection remain on the
  existing transaction connection. Keep mutable ref reads live. No new global
  cache or early response; existing committed-reader rollback proof still applies.

Deletion of old code is part of replacement: remove web-save flatten/copy/relist
of the entire tree, the parent `DirCache.read`, and the scan collecting all held
blob IDs. The store already deduplicates attempted writes. Retain whole-tree
helpers only for genuine remaining callers. Review total production lines and
concepts after replacement; prefer a net reduction, but do not compress clear
code or drop correctness to hit a line-count quota. Explain any net increase
by the one missing tree-editing responsibility, not speculative future reuse.

## Cost model and proof boundary

At folder depth d, an ordinary one-file edit should read at most d + 1 distinct
tree objects for derivation and construct at most d + 3 new objects (blob,
ancestor trees, commit). At depth 12: approximately 13 tree reads and 15 object
rows, with unchanged subtree IDs reused. Commit/ref/application queries are
additional; ancestor directory width affects bytes and CPU even at fixed depth.

`JdbcNotebookObjectDatabase.insertMissing` already chunks at 500: this example
fits one existence SELECT and one multi-row INSERT. The ref uses one conditional
UPDATE; JPA binding/timestamp writes and ordinary note/derived-state writes are
additional. These are expected shapes, not a measured total or a promise of
only two SQL writes for the entire request. Observe repeated flushes, attempted
objects and returned bytes as well as newly inserted rows; deduplication alone
can hide wasted work. An unchanged tree must cause no Git object INSERT/ref
UPDATE, although normal application timestamp semantics may still write.

Use controller-level proofs over the real database. Reuse/extend
`services/notebookGit/SqlStatementCallLog` rather than adding a monitoring
library. It currently records preparation only: extend test support to observe
actual executions and relevant parameters/results when proving reads and writes.
Scope recording to the real controller call and its transaction completion;
exclude fixture setup, assertion reads, full assembly and bundle download.
Use the established controller test context/transaction connection; do not
mock the tree/store or create one Spring context per cost scenario. A recorder
must be calibrated against known real SELECT/INSERT/UPDATE calls before its
counts are accepted. Report transaction completion separately; JDBC executions
are not automatically measured wire round trips.

Baseline captured on `853a01f996` before the slice 1 production edit, fixture
`depth-6-plus-4-sibling-folders` in
`NotebookGitWebContentSaveCostControllerTest.contentSaveJdbcObjectFetchesAreScopedToTheControllerCall`.
JDBC execute calls, not wire round trips: elapsedMs 37, objectFetches 26,
treeFetches 22, commitFetches 4, objectInsertExecutions 1,
bindingUpdateExecutions 1, jdbcExecutions 58. After removing the builder's
parent-tree read, the same fixture reported treeFetches 11 (one whole-tree
walk remains). The prior 34 + 2F measurement stays motivation, not this
baseline. Slice 3 still needs its own cold depth-12 / thousands-of-folders
observation; this depth-6 fixture does not satisfy that proof.

## Ordered slices

All slices are planned. Target about five minutes of implementation and focused
reasoning; scrutinize work beyond five and re-split before exceeding ten minutes
of implementation. Estimates below exclude the explicitly permitted wait for
the repository-mandated complete backend suite and controlled measurements;
those waits cannot be reduced by subdividing product behavior. An implementation
overrun is not covered by that exception. Keep each delivered slice green.

### 1. Append a complete tree without reading the parent tree again

Type: Behavior. Status: done. Estimate: 5–8 minutes plus suite wait.

Behavior: a web content save with a fully derived tree appends the same native
tree and parent commit while avoiding the builder's second recursive read.
Start with the matched baseline above. `append` already receives all final path
IDs and required changed blob bytes: remove `DirCache.read` and the held-blob
scan, build from those supplied IDs, and let existing batched storage deduplicate
attempted blobs. Preserve the complete-snapshot append callers in fixtures.

Proof accepted: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed
on this checkout. `append` no longer calls `DirCache.read`; `writeTree` builds
from the supplied path IDs with `DirCache.newInCore()`.
`NotebookGitCommitBuilderTest.appendsCompleteSnapshotWithTheSameTreeAsAFreshBuild`
asserts exact tree ID and parent.
`NotebookGitWebContentSaveCostControllerTest.contentSaveJdbcObjectFetchesAreScopedToTheControllerCall`
activates `SqlStatementCallLog` only around `updateNoteContent` and asserts
`treeFetches <= 12` (matched baseline was 22). JDBC batching and recorder
calibration live in `NotebookGitJdbcObjectStoreSqlObservationTest`
(`oneAppendAcrossManyTreesIssuesOneBatchedExistenceCheckQuery`,
`recorderCapturesSelectInsertAndUpdateExecutionsAgainstKnownStoreCalls`);
focused rerun after the test split passed. This slice is useful alone, but
still walks/builds the whole tree once and may batch many attempted objects;
it does not fulfill final acceptance.

### 2. Derive Portable changes through directory-owned tree edits

Type: Structure. Status: done. Estimate: 8–10 minutes plus suite wait.

Structure: replace the encoder's copied flat-map manipulation with the small
directory-tree model and native root construction described above. Initially
seed it eagerly from the existing accepted-tree read so caller behavior remains
unchanged. This directly enables slice 3's lazy accepted-tree source. Keep only
the minimal eager input bridge needed for this transition; slice 3 removes it.
Do not add a permanent old/new strategy switch or note-specific branch.

Proof accepted: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed.
`NotebookGitTreeEncoder.derive` edits a `NotebookGitDirectoryTree` seeded by
`fromBlobIds`. `assertAcceptedTreeMatchesTheFullAssembly` compares the derived
blob map and `tipTreeId` with a later `resetHistory` full assembly. The derived
and folder oracle controller tests call that assertion. Ordering is
`NotebookGitCommitBuilderTest.ordersFileBesideDirectoryWithSharedNamePrefixLikeGit`
(`foo.md`, `foo/.keep`, `föo.md`). Focused oracle rerun after moving `.keep`
hashing back onto the encoder also passed. `TreeRef` exists but stays
unpopulated; encode still flattens to path/blob IDs. Slice 3 still has to
emit native trees that reuse unresolved tree refs and remove this eager seed.

Relocation and empty-folder conversion fit this slice. The remaining gap is
slice 3's lazy accepted-tree source and native emission of unresolved tree
refs, not another encoder.

### 3. Save through the edited paths and reuse untouched subtrees

Type: Behavior. Status: planned. Estimate: 5–10 minutes plus suite wait.

Behavior: an existing note at root or depth 12 is edited beside thousands of
unrelated folders and attachments; publication reads only the edited ancestor
trees, persists changed objects in the existing batch, and retains all other
content. Repeating identical content leaves Git objects/head unchanged.

Replace the eager seed with the accepted root and on-demand directory reads.
Open each required tree once within the editor, share ancestors across edits,
compare native root IDs before flush, and pass the completed root to the commit
builder. Remove the eager bridge and whole-map comparison/copies. Existing
full-snapshot build/append consumers keep their supported complete-tree input
without reading a parent just to build it.

Proof: extend `NotebookGitWebContentSaveCostControllerTest` with fixed-path,
fixed-depth fixtures whose unrelated folder counts differ substantially,
including thousands of folders; clear the persistence context before each
measured call and reopen the accepted repository. Verify executed SQL/object
IDs show no unrelated tree/blob reads, no duplicate ancestor reads by the
editor, and no folder-proportional existence batches. Check one object batch
for the depth-12 example and distinguish rows from statements. Assert the
unchanged-save Git-write delta only in its own case. Check exact full-assembly
tree, parent continuity and existing late-failure committed-reader rollback
outside the measurement window. Reuse slice 2's structural proofs, adding a
composed-operation example only if their actual setup leaves a capture gap.

### 4. Demonstrate responsive saves on the representative notebook shape

Type: Behavior. Status: planned. Estimate: 5 minutes analysis plus bounded measurement wait.

Behavior: cold single-note saves on the reported shape (about 4,000 folders,
11,000 notes, depth 12, with unrelated attachment content) satisfy the selected
latency outcome under recorded database conditions, with complete Git history.

Compare the retained baseline and final revision under the same fixture and
environment. Record save latency distribution and total/Git read/write counts,
attempted/inserted objects and fetched content. Vary depth and ancestor width
separately from unrelated folder count. Reuse the controller cost harness for
deterministic counts; a test-only controlled per-execution delay may demonstrate
round-trip sensitivity but must be labelled a simulation. Do not put elapsed
time assertions in ordinary CI or add production instrumentation merely for
this experiment. Record the literal experiment command before running it.

Proof: the controller cost counts and matching native trees remain correct;
representative request measurements establish the local/controlled outcome.
The roughly one-second production promise requires an actual post-deployment
observation on the reported notebook. This plan does not authorize a release
or production edits. If that observation is unavailable, retain the plan with
that promise explicitly unproved; do not substitute simulated/local timing or
declare the whole story complete. An unexplained performance miss triggers
analysis of this story's remaining work, not automatic expansion into story 2.

## Verification and delivery

From the execution checkout, use `./scripts/run.sh bash scripts/worktree_setup.sh`
when its dependencies are not prepared. For each code-bearing slice run:

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only
```

This runs the complete backend suite, as the backend skill requires. The linked
worktree isolates its test database. No migration, generated API or frontend
check is expected; if an actual signature/schema change becomes necessary,
apply the corresponding project guidance instead of hand-editing generated files.

Proof ownership: slice 1 owns removal of the duplicate builder read and parent/
native-tree continuity; slice 2 owns unchanged structural and format behavior;
slice 3 owns depth-bounded cold reads, batch writes, no-op and atomicity;
slice 4 owns the measured latency outcome and honest production-observation gap.
Existing tests cited above are inspected coverage to reuse, not execution
results for this plan. Re-run them as part of the complete backend suite.

When execution is authorized, apply the normal delivery gates: Jidoka, fresh
`dough-post-change-refactor` agent, required generation if triggered, coordinator
`./scripts/run.sh pnpm format:changed` once, plan update, commit/check-only lint
hook and push through `dough-execute-plan`. Keep this source/plan for retrospective
and story wrap-up. Planning alone neither takes the backlog item nor begins
product implementation, CI observation or release.

## Assessment and current evidence

No unresolved product/ADR decision blocks these slices. Scope and reuse were
critically reviewed against current callers at `0f8e0bf6ba`. The only intermediate
whole-tree input is explicitly removed in slice 3. Final design has one editor,
one encoding owner and the existing transaction owner, with no persistent new
representation. Slice 2's relocation conversion fit the delivered slice. Slice 3 remains the
path-scoped read and native tree-ref emission.
Production latency is unproved until measured.

## Execution

Story Branch Mode. Execution checkout
`/Users/terryyin/git/doughnut-worktrees/012-path-scoped-note-save`, branch
`story/012-path-scoped-note-save`, created from `origin/main` at `cf2f317c27`.
Queue claim `853a01f996` is published on `origin/main`. Slice 1 increment
`9cf34be8b4` is published on `origin/story/012-path-scoped-note-save`. Later
increments publish to that same branch. GitHub Actions observer
`/tmp/dough-ci-501/watch-Pkeu0n` covers that story branch (`ci.yml`, display
name `donut CI`). The trunk claim is unobserved. Slice 1's learning for later
proof: activate `SqlStatementCallLog` only around the controller call; one
accepted-tree walk on the depth-6 fixture is about 11 tree fetches.
