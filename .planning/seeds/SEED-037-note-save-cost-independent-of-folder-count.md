---
id: SEED-037
status: dormant
planted: 2026-09-22
planted_during: owner report that production note saves take 7 to 10 seconds after SEED-034#story-3 shipped
trigger_when: now; production note saves are unusably slow on foldered notebooks
scope: small
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
`DirCache` before writing the new tree. Measured on 2026-09-22: a content save
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

- Path-scoped tree editing (preferred): read only the tree objects along each
  changed path, rewrite those trees, and reuse every untouched subtree by its
  id. Cost per save is proportional to the depth of the changed paths. Folder
  moves and renames still need the subtree under the moved prefix, which is
  what they change anyway.
- Batched level-by-level reads: keep the whole-tree walk but fetch each
  nesting level's tree objects in one `IN (...)` query and build the new commit
  from the map already read instead of walking a second time. About 34 plus
  depth queries, but the whole tree is still transferred every save.
- Doing nothing leaves production saves at 7 to 10 seconds and widens the
  window for the in-flight save inconsistency captured in SEED-036.

Decision: capture as the top-priority story; the story chooses between the
two shapes during delivery, favouring the path-scoped one when it does not
cost more code than the batched walk.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery. Estimates
are hypotheses. No executable plan or implementation is authorized by this
seed.

<a id="story-1"></a>

### Save a note without visiting every folder of its notebook

- **Identity:** SEED-037#story-1
- **Goal / beneficiary:** A note author on a large, deeply foldered notebook
  gets a content save whose database work depends on the changed paths, not on
  how many folders the notebook has.
- **Evaluation:** On a Git-backed notebook with thousands of nested folders, a
  content save issues a number of database queries that does not grow with the
  folder count (bounded by a constant plus the nesting depth of the changed
  paths), the accepted head still equals the full assembly, and the production
  save on the reported notebook returns in about a second.
- **Scope:** The accepted-change owner's read of the accepted tree and the
  commit append for web saves (content edits, note creation, moves, renames,
  README edits). The proof must count real JDBC queries against the object
  store, not Hibernate statements, on a fixture with thousands of nested
  folders. Proposal publication, cutover and bundle download keep their
  whole-tree reads.
- **Value / learning:** Production note saving becomes usable again and the
  cost signal that hid this is replaced by one that measures what production
  pays.
- **Effort hypothesis:** M, medium confidence; the object store already
  batches writes and existence checks, so only reads change.
- **Depends on:** none.
- **Safe stopping point:** Query count per content save is independent of
  folder count and the derived head equals the full assembly.

## Ordering and Scope Reduction

Single story, first in the product backlog. If SEED-034#story-5 (asynchronous
Git processing) is judged, judge it after this story, since this story is the
cost that keeps production saves slow.

## When to Surface

Now, as the first product-backlog story.

## Breadcrumbs

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
