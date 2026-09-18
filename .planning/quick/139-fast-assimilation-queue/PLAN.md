# Get the next item to assimilate quickly for large notebooks

Status: complete (all slices done 2026-09-18; awaiting retrospective and story wrap-up)
Source: [SEED-026 story 1](../../seeds/SEED-026-fast-assimilation-queue-for-large-notebooks.md#story-1),
refined 2026-09-18. Owner decisions: gated properties count as unassimilated;
the migration replaces the unusable index. Execution started 2026-09-18.

## Execution identity

- Mode: Story Branch Mode.
- Originating checkout: `/Users/terryyin/git/doughnut` on `main`; claim
  commit `28aa17d11c` (backlog entry moved to Taken).
- Execution checkout: `/Users/terryyin/git/doughnut/.claude/worktrees/139-fast-assimilation-queue`
  on branch `139-fast-assimilation-queue`.
- Integration: `main` in the originating checkout; remote `origin`
  (`nerds-odd-e/doughnut`). Push destination for this execution:
  `origin/139-fast-assimilation-queue`.
- CI observer: GitHub Actions, workflow `ci.yml` ("donut CI"), target branch
  `139-fast-assimilation-queue`; mailbox `/tmp/dough-ci-501/watch-xA3zgE`.

## Goal and scope

A learner with large notebooks asks for the next item to assimilate. Today
the request takes 143–159 s on the measured owner data (two notebooks,
21,600 notes, 12,604 property rows) because the property reference gate is
evaluated in Java for every property row on every request while counting,
and the per-link title lookup scans the notebook. After this plan the same
request completes in about one second on the same data, with the same queue
order, the same gate semantics for head selection, and fewer production
lines than today.

Included:

- Property unit counts are one SQL count per source (owned, per
  subscription) with the stream's tracker, skip, availability, and
  notebook-tracking conditions and no gate. A gated property counts as
  unassimilated (owner decision).
- Head selection keeps live reference resolution and evaluates the gate only
  while scanning ordered property candidates that can still precede the best
  candidate found so far.
- One migration replaces the functional title index with a plain index on
  `(notebook_id, title_uniqueness_key)`. (The unmapped
  `note_property_index.target_note_id` column was already removed by
  `V300000315`; see Learnings.)
- The Java reserved-key filter in the queue is deleted: rows are excluded at
  write time by `NotePropertyIndexPlanner`. Stale legacy rows, if production
  has any, are removed by a one-time ops statement (recorded under
  "Current decisions"), not by Flyway DML (`db-migration` skill).

Excluded (considered, not built or verified here):

- Speeding up the two ordered stream queries (about 220 ms each): note
  content hydration and the filesort on the coalesced level.
- Any gate-semantics change: index-time resolution, prerequisite pull-forward,
  caching, or a resolved-link table.
- The recursive `trashed_folder` view cost inside every note query.
- Frontend and API changes: the endpoint, request, and response shapes are
  unchanged, so no client regeneration.
- A permanent profiling document; the measurements live in the seed and this
  plan's learnings.

Assumptions:

- `LOWER(n.title)` in existing JPQL keeps working after the index swap
  because MySQL rewrites it to the stored generated column (verified 2026-09-18
  with EXPLAIN on MySQL 8.4: `Filter: (note.title_uniqueness_key = 'word')`).
- The two `AssimilationUnitSource` beans are injected in an order the service
  can rely on only if made explicit; the plan makes note sources precede
  property sources.

## Architecture

Ordinary plan; no North Star topic applies (the assimilation queue is not a
Git-backed web change). Accepted ADRs touched: 0004 (Markdown authoritative;
live resolution kept), 0006 (no new catch paths).

PFE results:

- Counting: reuse the JPQL fragments already shared by the property stream
  queries (`unassimilatedJoinPropertyTracker`, `unassimilatedWhereClause`,
  `unassimilatedDedupeByExactKey`) in two `count(i)` queries, mirroring
  `NoteRepository.countUnassimilatedByOwnership` / `countUnassimilatedByAncestor`.
  Prototype verified 2026-09-18 (patch kept locally at
  `~/.claude/jobs/b425f6af/tmp/prototype-sql-count.patch`): 0.74–0.90 s.
- Ordering bound: reuse `AssimilationUnit.ORDER`; the property stream is
  already ordered identically to the note stream, so `takeWhile` against the
  best-so-far candidate is the whole change. No new query.
- Index: the defect was introduced by `V300000329__ReplaceNoteTitleFunctionalIndex`
  (generated `title_uniqueness_key`), which made MySQL substitute the column
  for `lower(title)` and stop matching `idx_note_notebook_id_title`. The fix
  reuses that generated column.

## Key examples (from the seed)

1. Owner with the measured notebooks and no trackers → next unit is the first
   note by level, creation time, id; total is notes plus all untracked
   property families (33,292); about one second.
2. Carrier C `example of: [[A]], [[B]]`, A and B untracked → next is A; total
   counts A, B, and C's property (3); after A and B are assimilated, next is
   C's `example of`.
3. Target renamed after indexing → C's property offered without reindexing.
4. Wiki link title lookup in a 10,000-note notebook → one indexed row.
5. Subscription with remaining budget → its heads compete exactly as today.

## Outside-in proof

| Promise | Owning slice | Proof |
| --- | --- | --- |
| Title lookup is an index hit | 1 | test DB migrates; `docs/database-erd.md` unchanged or regenerated; dev-DB EXPLAIN shows `key: idx_note_notebook_id_title`, `rows: 1` (manual, recorded in learnings) |
| Gated property counts as unassimilated; counts need no gate | 2 | `AssimilationServicePropertyReferenceGateTest.gates_list_property_until_all_resolved_targets_are_assimilated` expects total 3; `UnassimilatedPropertyServiceTest` count and subscription-count tests green; stale-key tests deleted with the filter |
| Queue order and gate semantics unchanged; property scan bounded | 3 | `AssimilationService*Test` classes green; dev-DB digest shows 0 gate evaluations with no trackers (manual, recorded) |
| Whole request about one second on owner data | 3 | `curl` timing on the dev DB recorded in learnings (manual) |

## Ordered slices

### 1. Title lookups use an index

Type: Structure
Status: done (2026-09-18)
Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test` migrates the test
DB and stays green; `CURSOR_DEV=true nix develop -c pnpm export:database-erd`
updates `docs/database-erd.md`; on the dev DB,
`EXPLAIN SELECT id FROM note WHERE notebook_id=66879 AND lower(title)=lower('Word')`
shows `key: idx_note_notebook_id_title`, `rows: 1`.

Internal change: add `V300000331__replace_note_title_index.sql`:

```sql
ALTER TABLE note
  DROP INDEX idx_note_notebook_id_title,
  ADD INDEX idx_note_notebook_id_title (notebook_id, title_uniqueness_key);
```

Unchanged external behavior: every `LOWER(n.title)` query returns the same
rows. Enables slice 3's sub-second head selection (each gate evaluation's
title lookup drops from about 15 ms to well under 1 ms).

### 2. Counts include gated properties and never evaluate the gate

Type: Behavior
Status: done (2026-09-18)
Proof: `CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test --tests '*UnassimilatedPropertyServiceTest*' --tests '*AssimilationServicePropertyReferenceGateTest*' --tests '*AssimilationServicePropertyUnitsTest*' --tests '*AssimilationServiceDailyCapTest*' -Dspring.profiles.active=test --build-cache`
green, with the gate test's total changed from 2 to 3.

Behavior: owner with carrier C referencing untracked A and B → `getCounts()`
total is 3 (A, B, C's `example of`), and the count issues no per-row
resolution. Subscription counts follow the same rule.

Implementation: add `countUnassimilatedPropertiesForOwnership` and
`countUnassimilatedPropertiesForNotebook` to `NotePropertyIndexRepository`
from the existing fragments; `UnassimilatedPropertyService` count methods
call them; delete `countAssimilable`. Delete the
`PropertyKeyNaming.isReservedStructuralKey` filter from `streamAssimilable`
and the three `does_not_count_(stale_)reserved_*` tests that insert rows the
write path never produces. `AssimilationCounter` and the DTO are unchanged.

### 3. Head selection scans only property candidates that can still win

Type: Structure
Status: done (2026-09-18)
Proof: `CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test --tests '*AssimilationService*' --tests '*AssimilationController*' -Dspring.profiles.active=test --build-cache`
green; on the dev DB after
`TRUNCATE performance_schema.events_statements_summary_by_digest`, one
`curl -u old_learner:password 'http://127.0.0.1:8082/api/assimilation/next?timezone=Asia/Singapore'`
completes in about one second and the digest shows no
`findByNotebookNameAndNoteTitleOrderByIdAsc` rows (no trackers → no gate
evaluations). Record both numbers in learnings.

Internal change: in `AssimilationService.getNextAssimilationUnit`, fold the
candidate streams in a fixed order (owned notes, subscription notes within
budget, owned properties, subscription properties) keeping the best unit so
far; consume each stream with
`takeWhile(unit -> best == null || ORDER.compare(unit, best) < 0)` before its
`findFirst`. The property stream keeps its gate filter, so the gate runs only
for candidates that precede the best note. Make the source order explicit
(for example `@Order` on the two `AssimilationUnitSource` beans, or the
service asking the note source first) rather than relying on injection order.
Remove `headOfSubscription`'s separate min if the fold subsumes it.

Unchanged external behavior: the returned unit is the same minimum as today
because a property unit always sorts after its note's unit and after every
note unit ordered before its note. Enables the sub-second request on the
owner's data; this is the plan's last slice.

## Current decisions

- Gated properties count as unassimilated (owner, 2026-09-18).
- No Flyway DML. If production `note_property_index` holds stale reserved-key
  rows, run once as an ops statement after deploy and record the count:
  `DELETE FROM note_property_index WHERE lower(replace(property_key,'_','')) REGEXP '^(image|imagemask|wikidataid|url|titlepattern|questiongenerationinstruction|level|type|relation|source|target)( [0-9]+)?$'`.
  The dev database has zero such rows.
- Keep the index name `idx_note_notebook_id_title` so nothing else changes.
- Migration version must exceed 300000330 (`db-migration` skill); 331 is next.

## Learnings

- 2026-09-18, before slice 1: `note_property_index.target_note_id`, its
  index, and `fk_note_property_index_target_note` do not exist in any local
  database (dev and test at Flyway version 300000330). Migration
  `V300000315__replace_note_property_index_target_note_with_authored_reference.sql`
  already replaced the column; the plan's drop statements came from the
  pre-V315 text in `V100000000__baseline.sql`. Slice 1 is reduced to the
  index swap; the "dead column gone" promise was already satisfied.
- Slice 1 accepted proof: `CURSOR_DEV=true nix develop -c pnpm backend:test`
  green (2,475 tests) on a worktree test DB migrated to 300000331;
  `information_schema.statistics` shows `idx_note_notebook_id_title` =
  `notebook_id,title_uniqueness_key`; ERD export produced no diff (it lists no
  indexes). Dev DB (`doughnut_development`, migrated by starting the worktree
  backend on port 8082): `EXPLAIN SELECT id FROM note WHERE notebook_id=66879
  AND lower(title)=lower('Word')` → `type: ref, key:
  idx_note_notebook_id_title, rows: 1`. With only this index, one
  `GET /api/assimilation/next?timezone=Asia/Singapore` as `old_learner` took
  110 s (was 143–159 s in the seed measurement).
- Slice 2 accepted proof: the six `AssimilationService*Test` /
  `UnassimilatedPropertyServiceTest` classes green on the worktree test DB
  (`gates_list_property_until_all_resolved_targets_are_assimilated` asserts
  total 3 with next unit A). Kept
  `UnassimilatedPropertyServiceTest.does_not_count_reserved_keys_not_in_index`
  (it observes the planner's write-time exclusion through the SQL count);
  deleted only the two tests that inserted stale reserved-key rows. Refactor
  folded `unassimilatedDedupeByExactKey` into `unassimilatedWhereClause` and
  inlined the one-line stream wrapper; production delta so far −3 lines in
  `UnassimilatedPropertyService`, +21 in the repository (two count queries and
  shared FROM fragments).
- Slice 3 deviation: `takeWhile` in `AssimilationService` alone could not
  bound the gate, because the gate filter is composed inside
  `UnassimilatedPropertyService`'s stream; the bound (`canStillBeNext`) is
  passed into the property stream methods and applied before the gate.
  The refactor then removed `AssimilationUnitSource` and its two one-line
  forwarding beans: the service asks `UserService`, `SubscriptionService`,
  and `UnassimilatedPropertyService` directly in the fixed order (owned
  notes, subscription notes within budget, owned properties, subscription
  properties), which makes the order a readable fact of the fold instead of
  an injection-order assumption. Whole branch: −83 production lines versus
  `main`.
- Slice 3 accepted proof: `--tests '*AssimilationService*' --tests
  '*AssimilationController*' --tests '*UnassimilatedPropertyServiceTest*'`
  green (83 tests) on the worktree test DB, including the new
  `AssimilationServicePropertyUnitsTest.property_of_an_earlier_note_is_offered_before_a_later_untracked_note`.
  Dev DB (`old_learner`, no trackers), backend restarted on the slice code:
  three `GET /api/assimilation/next?timezone=Asia/Singapore` requests took
  0.60–0.63 s (was 110 s after slice 1 alone, 143–159 s before the story);
  response `totalUnassimilatedCount` 33,292 as the seed predicted. After
  `TRUNCATE performance_schema.events_statements_summary_by_digest` (as MySQL
  root over the local socket; the `doughnut` account cannot read
  `performance_schema`), one request issued 15 statements: two ordered
  stream queries at 0.22 s each, the property count at 0.04 s, the note
  count at 0.01 s, and zero title lookups (no gate evaluations).
- CI on `2519b829a9` (slice 2): the "End-to-End tests with Database
  (recall, wikidata, …)" job failed before any test ran, in the MySQL
  setup action, with a Docker Hub connection reset while pulling the
  `mysql:8.4` manifest; the matrix cancelled the sibling E2E jobs.
  Classified as CI infrastructure, not owned by this execution; the slice 3
  push re-covers E2E on the branch.
