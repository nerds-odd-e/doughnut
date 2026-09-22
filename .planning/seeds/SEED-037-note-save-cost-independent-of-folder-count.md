---
id: SEED-037
status: dormant
planted: 2026-09-22
planted_during: owner report that production note saves take 7 to 10 seconds after SEED-034#story-3 shipped
trigger_when: now; production note saves are unusably slow on foldered notebooks
scope: medium
---

# SEED-037: Save a note at a cost independent of the notebook's folder count

## Why This Matters

Saving one note's content in production takes 7 to 10 seconds on a notebook
with about 4,000 folders and 11,000 notes, while the same notebook saves in
under a second in the dev environment. The accepted Git tree is stored in
MySQL, one row per Git object, and a web save walks that whole tree twice,
one query per directory: once to read the accepted path-to-blob map
(`NotebookGitAcceptedTree.blobIds`) and once more inside
`NotebookGitCommitBuilder.append`, which reads the parent commit into a full
`DirCache` before writing the new tree. The prior investigation recorded on
2026-09-22 that a content save
issues 34 queries plus exactly 2 per folder, about 8,100 for that notebook.
Over a local socket that is well under a second; over the network to Cloud SQL
each query is a round trip of about a millisecond, so the same save takes
about 8 seconds.

This cost predates SEED-034#story-3. That story removed the whole-notebook
Hibernate load and measured a seven-times improvement on a fixture with 40
flat folders, so the folder-proportional part never showed. Its cost proof
counted Hibernate statements only, which never see these raw JDBC object
reads. A Git reset of the notebook would not help: the tree keeps the same
directories.

A content edit changes one file. Git only needs the tree objects on the path
from the root to that file (the notebook's nesting depth, 12 here), not every
directory in the notebook. The implementation visits every folder because it
treats the whole flattened tree as its unit of work, which Git does not
require.

## Alternatives and Decision

Owner direction on 2026-09-22: editing only one note's content must not visit
the rest of the notebook merely to retain Git history. Minimize SQL round
trips, ideally to one or a few. Subsequent owner decision: accept ancestor-only
reads for story 1 and authorize its slice plan; queue deeper optimization third
as story 2. Prefer simpler code, fewer lines where possible, and domain-cohesive
architecture. Justified exceptions are allowed. Implementation is not requested.

- **Selected for story 1: edit the existing Git tree along the changed paths.** Read
  the root and ancestor trees, replace the changed file's blob ID, rewrite
  those ancestors from leaf to root, and retain untouched subtree IDs. Reuse
  the trees already read throughout this accepted change. Batch the newly
  produced objects through the existing object-store write mechanism.
  Remove both whole-tree walks; batching just one of them is insufficient.
- **Simpler alternative: batch a whole-tree read and reuse its flattened
  map.** This reduces round trips but still transfers and processes every
  directory. It does not meet the owner's requirement to avoid unrelated
  work, so it is not the recommended final solution.
- **Strict few-query alternative: add a queryable tree-edge/path index or
  another storage representation that can retrieve the ancestor objects
  together.** The current rows contain opaque Git object bytes; each next
  tree ID is discovered by reading its parent. An index would need atomic
  maintenance across web saves, imported publications, cutover and reset,
  plus treatment of existing history. That is additional design and storage
  work, not an incidental batching change. Story 2 owns this deeper question;
  an index is a candidate, not a selected architecture.
- **Caching or asynchronous processing alone** does not remove the cold-save
  whole-tree work. Background acceptance also changes publication semantics;
  it remains the separate conditional SEED-034#story-5.
- Doing nothing leaves the reported production saves at 7 to 10 seconds and
  widens the window for the in-flight save inconsistency in SEED-036.

The owner accepted the path-scoped solution within the current architecture.
With the existing store, a note at folder depth 12 needs approximately 13
distinct tree-object reads, plus commit/ref and ordinary application work.
That is not a promise of one or a few SQL statements for the entire save.
The exact total needs measurement. Each ancestor is a complete Git tree
object, so its immediate sibling entries are read and serialized; their
subtrees and file contents need not be opened. Query count can be independent
of unrelated folder count while transferred bytes still depend on the widths
of the ancestor directories. Do not describe this as strictly constant total
work or latency.

For a genuinely changed single file at depth d, the expected new Git objects
are one blob, d + 1 ancestor trees including root, and one commit: d + 3
objects, or at most 15 new rows at depth 12 (existing identical objects can
reduce inserted rows). The current store already batches these into one
existence-check SELECT and one multi-row INSERT for fewer than 500 objects,
then updates the accepted ref with one compare-and-swap UPDATE. Binding
metadata/projection writes and transaction completion are additional. These
are code-derived expectations, not measured total SQL counts. Do not equate
object rows with SQL statements or JDBC calls with verified network round
trips. Measure all three where making claims about them.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery. Estimates
are hypotheses. The owner authorized a slice plan for story 1 only; neither
story is authorized for implementation by this refinement.

<a id="story-1"></a>

### Save a note without visiting every folder of its notebook

- **Identity:** SEED-037#story-1
- **Slice plan:** [Save a note by editing its Git ancestor trees](../quick/012-path-scoped-note-save/PLAN.md).
- **Goal / beneficiary:** A note author on a large, deeply foldered notebook
  can save an edit to one note in about a second, preserving complete Git
  history without walking unrelated notebook subtrees.
- **Evaluation:** Compare equivalent content-only saves at a fixed edited
  path and depth while increasing unrelated folders into the thousands.
  Count actual JDBC statements, including Git-object reads, and report total
  request SQL separately from the Git subset. No per-unrelated-folder query
  growth or loading of unrelated subtree objects, note bodies or attachment
  bytes is allowed merely to construct history. Check the resulting tree
  against a full assembly outside the measured save. Separately vary edited
  path depth and ancestor-directory width to expose remaining costs. Measure
  request latency under production-like database round-trip conditions and
  verify the about-one-second outcome on the reported production notebook
  after authorized deployment; local timing alone does not prove that outcome.
- **Scope:** The accepted-change owner's read of the accepted tree and the
  commit append for web saves (content edits, note creation, moves, renames,
  README edits). The defining performance scenario changes only one note's
  content at its existing path. Preserve complete capture of all rows changed
  by an operation, including when an edit has additional real content effects;
  the one-note example must not become an endpoint-specific shortcut that
  drops them. Folder relocation and other structural operations retain their
  existing behavior, including README and empty-folder representation. Their
  work may follow the affected paths/subtree; optimizing every such operation
  to the same latency is not a separate promise here.
- **Constraints:** Preserve synchronous, atomic publication of application
  state and accepted Git history; one commit per accepted Portable change,
  unchanged content produces no redundant web commit, and failures roll back
  both. An unchanged tree must not flush new Git object rows. Reuse untouched
  Git objects and preserve existing private identities,
  learning data, content encoding and authored-reference semantics. No history
  reset, compaction, extra content authority or per-endpoint declarations of
  changed paths.
- **Deferred promises:** Optimization of proposal publication, cutover, reset
  and bundle download; asynchronous Git processing; the separate in-flight
  undo defect; new UI; universal latency guarantees for large moves/renames.
  Whole-tree reads remain legitimate for operations that require the whole
  tree. A persistent index or storage redesign belongs to story 2; no
  scaffolding for it belongs in story 1.
- **Key examples:**
  1. A note in a 12-level folder path, beside thousands of unrelated folders
     and large attachments → edit its body without changing placement → one
     new accepted commit contains the edit and retains the other content;
     unrelated subtrees and attachment blobs are not fetched for history.
  2. The same path/depth with many more folders in an unrelated subtree →
     the same kind of body edit → no corresponding increase in Git SQL reads.
     An initially cold request must satisfy this; warming a cache is not proof.
  3. Save content that encodes to the accepted bytes → no extra Git commit and
     no full-tree traversal merely to discover that nothing changed.
  4. An existing creation, move, rename or README edit also changes folder
     representation or other Portable rows → all those effects remain in its
     single accepted commit, matching full assembly, with rollback preserved.
- **Architecture / reuse:** Keep `AcceptedWebChangeService` and
  `ProjectionChangeCapture` as the complete-operation boundary. Evolve the
  existing tree encoding/commit construction to consume changes against the
  accepted tree instead of requiring a complete flattened path map. Reuse the
  Portable codec and batched object insertion; keep full assembly for its
  existing whole-tree callers and as a correctness oracle. Product search
  found no existing incremental tree writer: proposal inspection reads trees
  for validation, while the CLI delegates local Git operations to Git rather
  than owning transactional server persistence. A request-local set of already
  read objects must remain scoped to the opened repository/transaction, without
  caching mutable accepted refs across requests. This direction preserves
  [ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
  and its [publication contract](../../docs/notebook-git-synchronization.md),
  and [ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  for Portable paths, README and `.keep` behavior.
- **Value / learning:** Production note saving becomes usable again and the
  cost signal that hid this is replaced by one that measures what production
  pays.
- **Effort hypothesis:** M–L, medium-low confidence. Reads and tree construction
  both change, including the flattened-map dependency of folder/marker logic;
  this is not merely replacing a SQL read method. Reuse existing behavior
  proofs. A persistent index would require renewed sizing and scope discussion.
- **Depends on:** none.
- **Safe stopping point:** The content save avoids unrelated subtree traversal,
  the selected query/latency target is demonstrated, and accepted content and
  history retain their existing correctness and atomicity.

No unresolved product or architecture decision blocks story 1's slice plan.
Production timing remains an execution observation, not a result of refinement.

<a id="story-2"></a>

### Save a note with only a few database round trips for Git history

- **Identity:** SEED-037#story-2
- **Goal / beneficiary:** A note author gets lower and more predictable save
  latency even for deeply nested notes, by reducing remaining Git-related
  database round trips beyond story 1's ancestor-only traversal.
- **Scope:** Start from story 1's measured complete save: object and ref reads,
  existence checks, object inserts, binding/projection writes and transaction
  completion. Seek one/few Git persistence round trips independent of nesting
  depth without loading unrelated content. Distinguish reducing round trips
  from reducing object rows, bytes and server work. Compare simpler use of the
  existing store first, then tree-edge/path indexing or storage redesign if
  needed. Deliver the selected improvement after refinement; this is not just
  a report recommending future implementation.
- **Evaluation:** A cold content-only save at the root and at depth 12 or more
  uses the refined small round-trip budget and improves measured end-to-end
  latency against story 1 under comparable database conditions. Unrelated
  notebook growth adds no traversal; accepted content/history and rollback
  remain correct. Agree the numeric budget after consuming story 1's complete
  measurements rather than inventing a promise now.
- **Design constraints:** Prefer fewer concepts and domain-cohesive owners,
  with less code when practical. Any added index, cache, store or maintenance
  mechanism must justify its total cost, including initialization, publication,
  recovery and migration. Retain native Git history and atomic synchronous
  acceptance under ADR 0002. Architectural exceptions remain human-owned.
- **Key examples:** The same one-note edit at shallow and deep paths should
  not incur one database round trip per ancestor; a cold first edit must gain
  the improvement too; a late failure must preserve prior content and history.
- **Deferred promises:** Background acknowledgement/worker delivery (the
  separate SEED-034#story-5), universal optimization of Git downloads/imports,
  and an exact chosen storage architecture. No index or migration is approved
  by this backlog entry.
- **Depends on:** SEED-037#story-1's measured outcome and read/write breakdown.
- **Effort hypothesis:** Unknown until the remaining cost and architectural
  tradeoff are established. Refine before slice planning; resplit if needed.
- **Safe stopping point:** The selected deeper optimization delivers its
  agreed latency/round-trip improvement with history and publication guarantees
  intact; story 1 remains independently useful if this work is deferred.
- **Open decisions:** Numeric budget, selected design, and whether its
  complexity/migration cost is justified. These do not block story 1.

## Ordering and Scope Reduction

Story 1 remains first. At the owner's direction story 2 is third, after the
in-flight undo consistency fix (SEED-036#story-1). The asynchronous assessment
shifts to fourth; its conditional activation remains unchanged. Preserve
unrelated backlog order. Story 1 must stand alone, without preparing an index,
alternate store or asynchronous worker for later stories.

## When to Surface

Now, as the first product-backlog story.

## Breadcrumbs

- Refinement inspection at `0f8e0bf6ba` confirmed both recursive tree reads and
  the Hibernate-only cost test. No new runtime measurements were made during
  refinement; the counts and timings below are prior investigation evidence.
- Owner report, 2026-09-22: production content saves take 7 to 10 seconds,
  slower than expected after v1.3.18; saves were made slowly, so no lock
  queuing was involved. The same notebook saves in under a second in dev.
- Production health check reported commit 54acba9fbb (v1.3.19), which
  includes SEED-034#story-3 and its correction plan.
- Measurement (throwaway test, 2026-09-22): content save queries = 34 + 2 per
  folder; 0 folders 34, 100 folders 234, 400 folders 834. Dev copy of the
  reported notebook: 4,046 folders, 11,184 notes, nesting depth 12.
- Both tree walks already existed at v1.3.17; the release removed the
  whole-notebook load and added no per-folder work.
