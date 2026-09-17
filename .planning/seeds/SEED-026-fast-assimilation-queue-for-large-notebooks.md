---
id: SEED-026
status: dormant
planted: 2026-09-18
planted_during: product backlog capture from owner observation of slow next-to-assimilate retrieval
trigger_when: learners with large notebooks wait noticeably for the next item to assimilate
scope: medium
---

# SEED-026: Get the next item to assimilate quickly for large notebooks

## Why This Matters

Learners with large notebooks wait a noticeable time whenever Donut finds the
next note or property to assimilate. The wait recurs on every assimilation step,
so it slows the whole assimilation session.

Measured on 2026-09-18 against the local development database (owner
`old_learner`, two notebooks of 11,185 and 10,404 notes, 12,604 property index
rows, no memory trackers), `GET /api/assimilation/next` took **143–159 s** per
request (three runs).

## Alternatives and Decision

Profiling located the cost precisely; see "Evidence" under story 1. The whole
wait comes from evaluating the property "reference gate" in Java once per
property row, on every request, while counting. The set-based queries that
select the next note and count units cost well under a second in total.

Direction: keep the gate's live-resolution semantics, but evaluate it only for
head selection, never for counting; make counting pure SQL; and make the
per-link title lookup use an index. No cache, background precomputation, or
new resolution machinery. The delivered design must be simpler and shorter
than today's (owner requirement, 2026-09-18).

## Story Decomposition

<a id="story-1"></a>

### 1. Profile and optimize getting the assimilation queue for large notebooks

**Goal.** Learners with large notebooks get the next item to assimilate, and
the assimilation counts, without a noticeable wait. The owner has no numeric
target: remove the obvious slowness so the request is "in general much faster
than now", with a design that is more cohesive, simpler, and maps better to
the domain, ending with fewer lines of production code.

**Evidence (2026-09-18, dev database, worktree backend on port 8082).**

- Thread samples of the request thread sat in
  `UnassimilatedPropertyService.countAssimilable` → `isGated`: sibling row
  load, authored-reference proxy load, wiki-link title lookup, alias lookup,
  and tracker existence check, per property row, for all 12,607 rows.
- One request issued about 63,000 SQL statements (MySQL digest after reset):
  12,607 title lookups, 12,590 alias lookups, 10,747 tracker checks, 12,620
  sibling loads, 12,422 reference-row loads, 1,862 folder loads.
- The title lookup (`findByNotebookNameAndNoteTitleOrderByIdAsc`) examined
  about 10,600 rows per call. Cause: MySQL substitutes the stored generated
  column `title_uniqueness_key` for the expression `lower(title)`, after which
  the functional index `idx_note_notebook_id_title (notebook_id, lower(title))`
  no longer matches. A plain index on `(notebook_id, title_uniqueness_key)`
  makes the lookup a one-row index hit (verified with a temporary index and
  EXPLAIN). With only that index: 86–92 s per request.
- With the index and property counts done in SQL without the gate (prototype
  patch, 27 added / 8 removed lines): **0.74–0.90 s** per request. The head
  selection needed only 29 gate evaluations. Remaining cost: two ordered
  stream queries at about 220 ms each, the two count queries at about 60 ms
  combined.
- The set-based note head, note count, and property head queries alone take
  69 ms, 9 ms, and 69 ms in MySQL.
- `note_property_index.target_note_id` is not mapped by any entity or used by
  any code; it is a leftover from index-time resolution.
- Reserved structural keys are already excluded when index rows are written
  (`NotePropertyIndexPlanner`); the Java filter in the queue only guards stale
  legacy rows, and the dev database has none.

**Scope.**

Required behavior:

- Counting unassimilated units never evaluates the reference gate. Property
  units are counted by one SQL count per source (owned, per subscription),
  with the same tracker, skip, availability, and notebook-tracking conditions
  as the stream. Consequence (owner decision needed, recommended): a gated
  property counts as unassimilated. It is unassimilated; it is merely not yet
  offerable. `totalUnassimilatedCount` for the measured owner rises from
  22,573 to 33,292.
- Head selection keeps live reference resolution (rename, namesake ambiguity,
  viewer readability, deleted target, spelling-only tracker; commit
  `662a2f5edc`). It evaluates the gate only while scanning ordered property
  candidates until the first offerable unit.
- Bound the property scan without changing semantics: a property unit sorts
  after its own note's unit and after every note unit ordered before it, so
  once the ordered property stream passes the best note head it cannot win.
  Compute note heads first (cheap), then scan property streams only while the
  unit orders before that bound. With no trackers this needs zero gate checks.
- The per-link title lookup uses an index: one migration adding a plain index
  on `(notebook_id, title_uniqueness_key)` and dropping the unusable functional
  index `idx_note_notebook_id_title`. Existing `LOWER(n.title)` JPQL keeps
  working because MySQL rewrites it to the generated column.
- Existing queue ordering, subscription daily budgets, sequence skips, and
  notebook "skip memory tracking" behavior are unchanged.

Simplification expected from the delivery (fewer lines):

- Delete `countAssimilable` and the count-side gate path.
- Drop the Java reserved-key filter from the queue, since rows are excluded at
  write time; if production may hold stale legacy rows, remove them once in the
  same migration rather than filtering on every request (execution decision).
- Consider removing the unmapped `target_note_id` column and its index in the
  same migration.
- Fetch the reference row with the sibling rows instead of one lazy proxy load
  per sibling, if it is a one-line join fetch; otherwise leave it.

Deferred promises (not built or verified here):

- Making the two ordered stream queries faster than about 220 ms (for example
  not hydrating note content, or avoiding the filesort on the coalesced level).
- Any change to gate semantics, such as gating on index-time resolution or
  pulling prerequisite notes forward in the queue.
- Caching, background precomputation, or a resolved-link table.
- The publication-time cost of the recursive `trashed_folder` view, which is
  re-materialized in every note query.

**Key examples.**

- Owner with the measured two large notebooks and no trackers → requests
  `/api/assimilation/next` → the next unit is the first note by level, creation
  time, id; total unassimilated is notes plus all untracked property families;
  the request completes in about one second on the dev machine, not minutes.
- Carrier note C has `example of: [[A]], [[B]]`, targets A and B untracked →
  next unit is A; total counts A, B, and C's property (3, not 2); after A and
  B are assimilated the next unit is C's `example of` property.
- Target renamed after indexing so `[[Word]]` no longer resolves → C's
  property is offered without reindexing (existing behavior kept).
- Wiki link to a title in a 10,000-note notebook → the title lookup examines
  one row, not the whole notebook.
- Subscription with remaining daily budget → its note head and bounded
  property head compete with the owned heads exactly as today.

**Decisions for the owner.**

1. Confirm: gated properties count in "total unassimilated" (recommended; it
   is what makes counting pure SQL and removes the count-side gate entirely).
2. Confirm: stale reserved-key rows, if any exist in production, are removed
   once rather than filtered on every request.

- **Effort hypothesis:** M (1–2 hours) — medium confidence: the prototype
  proves the two main changes; the scan bound and migration are small.
- **Depends on:** none.
- **Safe stopping point:** The index migration alone is safe and halves the
  wait; SQL counts alone bring the wait to about one second. Each is a
  separately shippable slice that keeps all gate behavior.
- **Source:** owner observation and refinement on 2026-09-18. Prototype patch
  retained locally during refinement at
  `~/.claude/jobs/b425f6af/tmp/prototype-sql-count.patch` (not committed).

## Ordering and Scope Reduction

One story. Slice order that keeps every stop safe: index migration → SQL
counts (with the count-semantics change and its tests) → bounded property scan
→ cleanup deletions. Drop the cleanup deletions first if time runs short.

## Open Decisions

The two owner decisions above; both default to the recommendation if the
owner does not object.

## When to Surface

When a learner reports or the owner observes slow next-item retrieval during
assimilation, or when assimilation retrieval code changes.

## Breadcrumbs

- `backend/src/main/java/com/odde/donut/controllers/AssimilationController.java` — `GET /api/assimilation/next`
- `backend/src/main/java/com/odde/donut/services/AssimilationService.java` — head selection and counts
- `backend/src/main/java/com/odde/donut/services/UnassimilatedPropertyService.java` — gate (`isGated`) and count loop
- `backend/src/main/java/com/odde/donut/entities/repositories/NotePropertyIndexRepository.java`, `NoteRepository.java` — stream queries; `findByNotebookNameAndNoteTitleOrderByIdAsc`
- `backend/src/main/resources/db/migration/V100000000__baseline.sql` — `idx_note_notebook_id_title`, `title_uniqueness_key`
- Tests: `AssimilationServicePropertyReferenceGateTest`, `AssimilationServicePropertyUnitsTest`, `UnassimilatedPropertyServiceTest`, `AssimilationServiceQueueOrderingTest`
- [SEED-018](SEED-018-publish-large-authored-notebooks.md) — prior profile-then-optimize approach
