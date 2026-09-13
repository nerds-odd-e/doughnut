# Web note trash and immediate undo

Status: planned
Source: [SEED-009, story 29](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-29)
Authority: Plan execution, commit, and push authorized by the 2026-09-13
`dough-execute-plan 115` instruction. Story 34 remains second and unrefined.

## Execution identity

- Originating checkout: `/Users/terryyin/git/doughnut`
- Originating branch and claim: `main` at `423199b4b1`
- Execution checkout: `/Users/terryyin/git/doughnut-115-web-note-trash-and-undo`
- Execution branch: `codex/115-web-note-trash-and-undo`
- Integration target: `main`
- Push destination: `origin/codex/115-web-note-trash-and-undo`
- CI observation: unavailable for this execution branch because `.github/workflows/ci.yml`
  triggers pushes only to `main`; local slice proof remains required.

## Goal and scope

An owner trashes a note through the web and can immediately Undo, retaining its
identity/history and independent tracking preferences. Preserve full-path trash
placement, missing trash parents, first-free collision suffixes, old-path reuse,
reference choices, authorized direct access/editing/Move, and legacy recovery.

Discovery of older trash is story 34; dedicated Restore is story 32; legacy data
migration and removal of note `deleted_at` are story 31; folder Trash is story
33. New Git/local compatibility belongs only to story 28. No new permanent-delete
UI, dashboard, timestamp metadata, stored trash flag, or restoration journal.
The schema-only derived view below does not move legacy data or retire its state.

## Assessment completed — 2026-09-13

The memory-tracker simplification landed in merge `a4d6aa2a53`. Current
`MemoryTracker.isActive()` reads its note, and `MemoryTrackerRepository` joins
`note n`; due recall, statistics and batch selection already share query
fragments. `NotePropertyIndexRepository` no longer filters a tracker deletion
field. The old tracker seed/plan was wrapped up; do not require or recreate it.

The previous 17-slice sequence overcounted consumer-specific preservation work.
Nine concrete leaves below replace it: three prepare one location-based
availability change; three prepare a reversible placement operation; two expose
those behaviors at existing public boundaries; the last wires the web action.
No assumption of a tiny final integration hides the controller and UI work.

## Selected structure and PFE evidence

Follow the [parent North Star](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#north-star-and-completion-boundary),
[ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md#trash),
[ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md), and retained learning
state under [ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md).
ADR 0006 governs failure handling. ADR 0002 remains Proposed; no Git enhancement
is smuggled into this story. Existing supported callers remain working.

### One derived membership, existing query owners

Create a read-only SQL view `trashed_folder` from the existing folder adjacency
relation. It contains root folders matching `_trash` case-insensitively and all
descendants. It stores no rows and has no synchronization/invalidation writes.
A normal versioned Flyway schema migration creates it; no new table, stored
function, flag, or materialized cache. This is the smallest proven query bridge
between the existing JPA and native SQL consumers, not a new eligibility engine.

Sketch against production column names (execution verifies exact DDL):

```sql
CREATE VIEW trashed_folder AS
WITH RECURSIVE members AS (
  SELECT id FROM folder
  WHERE parent_folder_id IS NULL AND LOWER(name) = '_trash'
  UNION ALL
  SELECT child.id FROM folder child
  JOIN members parent ON child.parent_folder_id = parent.id
)
SELECT id FROM members;
```

A field-access Hibernate `@Formula` can expose query-only `trashed` membership
using `coalesce(folder_id in (select tf.id from trashed_folder tf), false)`.
Folder search can use the corresponding ID membership. Native SQL uses the same
view through a short named fragment. Activity remains legacy `deleted_at IS NULL`
AND outside trash. Existing permissions, notebook settings, ordering, limits,
tracker preferences, and query-specific eligibility stay where they are.

For Java objects being moved in the current transaction, compute `isTrashed()`
from the current root ancestor using the existing hierarchy traversal; do not
trust a formula value loaded before a move. Keep the SQL query field separate
from that accessor's current-state evaluation (field access mapping). Both derive
from the same location rule, with no second stored authority. `isAvailable()`
combines that membership with the retained legacy field. Do not fabricate a
`deleted_at` timestamp to obtain filtering. Test same-transaction move/undo state.

Use a small query-fragment owner for the existing fixed aliases, not a DSL,
session interceptor, request-wide ID cache, or application filtering after LIMIT.
The first two slices centralize the old predicate without changing results;
slice 4 changes the shared expressions and object condition together.

**Rejected:** recursive SQL directly inside Hibernate `@Formula`: the actual
Hibernate version qualifies the CTE name as a column and produces invalid SQL.
A view avoids that parser problem and repeated query-parameter plumbing. A
persisted cache is not justified; a general service-layer rewrite is unnecessary.

### Existing mutation and undo owners

- Reuse `FolderConstructionService` and `FolderSiblingNameValidation` for parent
  creation. Expose its domain-level parent operation if needed; keep the current
  controller/DTO adapter. Do not create a competing folder builder.
- Reuse `NoteMotionService` for identity-preserving placement, extending the same
  placement operation to include a requested title atomically when necessary.
  Trash retains full path, allocates an available basename, and preserves all
  dependent records. Ordinary active-destination collisions remain conflicts.
- Reuse `NoteReferenceHandling` via `NoteService`: extract the existing pre-delete
  transformation, invoke before relocation, and keep `destroy/restore` available
  for legacy callers. No move-link rewrite replaces the chosen deletion policy.
- Add the explicit trash/undo-trash controller adapters beside existing deletion
  endpoints, backed by that placement operation. Existing API callers retain
  their current behavior; the web switches to trash only in slice 9. Undo accepts
  prior placement/title from existing session history, validates ownership and
  destination normally, and performs one transaction, not rename then move calls.
- Extend `NoteEditingHistory`'s current ephemeral record with original title when
  needed. Reuse `StoredApiCollection`, `useNoteDeleteFlow`, and `NoteUndoButton`.
  Successful Undo restores the action's prior name/placement; failed Undo leaves
  the note in trash and its new trash-undo record available. Do not redesign
  unrelated history. Dedicated Restore later keeps visible suffixes instead.

## Consumer assessment: changes versus retained proof

| Existing responsibility | Required change | What stays unchanged |
| --- | --- | --- |
| Note/alias literal search, recent suggestions, structural peers | Use shared active-note condition | Matching, viewer scope, ordering, limits; existing search proof reused |
| Semantic search JDBC and non-prod variant | Add the same view membership to native condition | Ranking/embedding mechanics; no optimization project |
| Recall, counts, recent history, batch generation | Change existing shared note fragments and two Java activity checks | Already-landed joins, scheduling and pending/failure rules; no separate rewrite per query |
| Note/property assimilation | Change note predicate, retain tracker joins | Property and notebook skip policies; existing assimilation proof reused |
| Wiki resolution, inactive referrers and rewrite candidate eligibility | Use note availability at existing checks | Authored reference storage and resolver; no new index/cache |
| Folder search/move destination search | Exclude view-member folders before paging | Ordinary folder listings and direct folder access still work |
| Direct note reads, ordinary folder contents, storage/export loaders | Keep legacy deletion condition where content retention is intended | In particular `findLiveNotesByNotebookIdOrderByIdAsc` serves Git state loading; do not replace it with active-note filtering |
| Legacy title reservations and destroy/restore | Retain until story 31 | New trash frees paths by movement, not by changing legacy title rules |
| Existing deletion reference transformations | Expose/reuse the current operation | Cleanup/reduction outcomes and authorization; no second policy |

The consumer inventory is bounded by actual use, not every textual match:
notebook/ownership deletion fields, historical migrations/backfills, and storage
reads are not active-note predicates. `FolderSubtree` needs ordinary contents,
including retained trashed notes, not an active-learning query.

## Isolated query evidence

[Probe source and reproducible runner](evidence/README.md) use the repository's
resolved Hibernate 7.4.5.Final and local MySQL 8.4.11. The runner creates a uniquely
named disposable schema and drops it in `finally`; it does not read application
rows. Literal command:

```sh
CURSOR_DEV=true nix develop -c bash .planning/quick/115-web-note-trash-and-undo/evidence/run-query-proof.sh
```

Passed: case-insensitive root membership, descendants, nested ordinary `_trash`,
ordinary `Trash`, root notes, legacy-deleted exclusion, multiple trash roots,
Hibernate/native agreement, paging/counts before result delivery, direct access,
move-out refresh, subtree move, root rename and zero-trash-root behavior.
This proves the chosen query mechanism, not application integration, concurrency,
production-depth/performance behavior, or the future feature's implementation.
MySQL's existing recursive-depth limit remains an engine limit; do not introduce
an arbitrary application depth restriction. Reassess if actual supported data
exceeds it. No same-performance claim is made.

## Ordered slices

All leaves are planned. Target ~5 minutes active work, scrutinize >5; estimates
below name the broader leaves honestly. >10 minutes requires finer decomposition
unless an explicit justified exception applies. Required whole-suite external
wait time is separate from active work. No global release flag or prolonged
broken intermediate state: preparation preserves current behavior, each Behavior
switches its complete outcome, and the final web action has working Undo when
first exposed. No resplit is warranted solely by the withdrawn count.

### 1. Name existing object and JPA note availability
Type: Structure
Status: done
Size: 5–10 minutes; mechanical replacements within existing owners.
Proof: Existing controller deletion/recovery, search, and assimilation results stay green.

Delivered `Note.isAvailable()` and the shared legacy-only JPA predicate, including
availability naming through the implicated object and query callers. Proof:
`CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed.

Introduce shared legacy-only availability vocabulary for objects/JPA and route
the classified active consumers through it. Preserve independent conditions and
leave content-storage/legacy-title predicates alone. This prepares slice 4;
no folder-location behavior changes yet.

### 2. Name existing native note availability
Type: Structure
Status: done
Size: ~5 minutes; retain existing joins and shared fragments.
Proof: Existing semantic search and recall/statistics controller results stay green.

Delivered the shared fixed-alias native availability predicate across semantic
search, embedding, structural-peer, and memory-tracker query owners. Proof:
`CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed.

Use a short legacy-only native condition in the existing note/embedding/peer and
tracker fragments. Reuse `byUserIdFrom`, `byUserIdAllFrom` and commissioned variants;
no separate statistics or batch rewrite. This is the SQL side of slice 4.

### 3. Derive folder membership for queries
Type: Structure
Status: done
Size: 5–10 minutes plus schema/API tooling wait as applicable.
Proof: A focused real-database membership/query test reproduces the isolated
probe's distinguishing cases and same-transaction current-state behavior.

Delivered the recursive `trashed_folder` view, Hibernate query mappings, native
projection support, and current-ancestry object accessors. Both
`CURSOR_DEV=true nix develop -c pnpm backend:test_only` and
`CURSOR_DEV=true nix develop -c pnpm backend:verify` passed. The ERD exporter ran
against `doughnut_development`; the generated ERD was unchanged because the view
adds no table or foreign-key relationship.

Add the schema-only view and query field mappings; use existing ancestry for
current object accessors. No reader switches until slice 4. Carry production
column/collation details into the proof; regenerate ERD per schema guidance.
This exposes no stored authority and performs no legacy data migration.

### 4. Folder location controls active participation
Type: Behavior
Status: done
Size: 5–10 minutes; change the prepared condition owners together.
Proof: Existing public Move boundary moves a learned note into/out of a trash
folder; public search/recall/assimilation/reference observations change together
while history, independent preferences, and direct access remain intact.

Activated the derived membership in object, JPA, native, and folder-search
availability, and added the current-note trash warning without changing the API
shape. `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed with a
public Move scenario covering participation and retained data; `CURSOR_DEV=true
nix develop -c pnpm frontend:test` passed with mounted trash nodes warning proof.

Activate membership in object/JPA/native predicates atomically. Include folder
search exclusion and the derived note warning in the current view. Generate API
if adding the derived read field. A loaded note moved in the same transaction
reports its current membership, not a stale formula snapshot. Existing legacy
recovery remains operational. No new folder or trash navigation UI is added.

### 5. Reuse parent construction for full-path placement
Type: Structure
Status: done
Size: ~5 minutes.
Proof: Existing folder-creation controller behavior is unchanged; the placement
boundary reuses an existing root regardless of case and only creates missing parents.

Extended the existing folder-construction boundary to find or create the root
trash and mirror only missing original parents using the existing sibling-name
rules. `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed.

Expose the suitable parent-based construction operation behind its current
adapter. Prepare the original-path mirror beneath root trash, using existing
naming and ownership checks. This prepares slice 8, not general folder sync.

### 6. Prepare collision-safe reversible placement
Type: Structure
Status: done
Size: 5–10 minutes; one placement responsibility and its bounded proof loop.
Proof: The stable placement boundary preserves note ID/dependents, selects the
first free suffix, and restores a supplied prior title/placement atomically;
occupied targets leave content unchanged.

Extended `NoteMotionService` with one atomic exact-placement operation and a
collision-safe available-title variant backed by the existing placement rules.
`CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed, covering retained
IDs/dependents, first-free and length-bounded suffixing, exact reversal, and
pre-mutation conflict safety.

Extend existing placement, not its external reference rewriting policy. Allocate
incoming names under existing comparison and length rules. If a suffix needs
space at the name limit, shorten only the allocated trash basename enough to fit;
keep the original title in session undo for exact reversal. No earlier trash is
renamed. This bounded naming consequence does not add a persistent title journal.

### 7. Reuse deletion reference transformation
Type: Structure
Status: planned
Size: ~5 minutes.
Proof: NoteControllerDeleteReferenceHandlingTests and
NoteControllerDeleteReduceToSourceTests retain their existing outcomes.

Extract the current pre-delete policy invocation so legacy destroy and the next
trash operation use the same code. Resolve/transmute references before moving
the target. No automatic move rewrites or restoration of removed properties.

### 8. Offer an atomic trash and undo operation
Type: Behavior
Status: planned
Size: 5–10 minutes using slices 5–7; if orchestration grows, refine before exposure.
Proof: Through NoteController's authenticated boundary, trash then undo a learned
note, including repeated trash/name reuse and existing reference-choice deltas;
observe real state transitions, retained IDs/history and atomic conflicts.

Add the explicit trash and undo-trash adapters using the prepared operations.
Return the current realm for direct access. Undo validates the caller's prior
placement/title with normal authorization; existing legacy endpoints retain
behavior. Generate API here. The complete operation is callable without exposing
an incomplete web button; no new Git semantics are selected.

### 9. Use trash and immediate Undo from the web
Type: Behavior
Status: planned
Size: 5–10 minutes plus browser-suite wait; an explicit integration leaf.
Proof: Extend note_deletion.feature for the actual web Trash/Undo journey.
Existing mounted delete/undo/store tests cover reference choices, failed Undo,
post-action navigation, warning/editing/Move, and preserving the original title.

Wire existing Delete flow to the new adapter, with clear trash wording and
unchanged confirmation choices. Capture original placement/title before mutation
in existing ephemeral history; Undo calls the atomic inverse and consumes that
record after success. Keep original folder/root navigation and existing legacy
Undo paths. Direct access/Move remains useful after session history is gone.
No discovery affordance, Restore shortcut, or new folder action is introduced.

## Final proof ownership

| Promise | Owning slice | Required final observation |
| --- | --- | --- |
| Trash and immediate Undo as a web user | 9 | Real UI transition and return, not just already-trashed fixtures |
| Location-based search/learning/wiki eligibility; folder target exclusion | 4, exercised again through actual trash in 8 | Correct exclusion/restoration before paging/counts, including descendant membership |
| Full path, parents, suffixing, old-path reuse, distinct identities | 8 | Real create/trash/recreate/trash sequence and retained earlier note |
| Undo exact name/placement and conflict safety | 8–9 | Atomic reversal or ordinary conflict retaining trash and undo record |
| Reference choices and retained learning/preferences | 8 | Chosen transformation, same IDs/history, stopped trackers remain stopped |
| Direct URL, warning, editing/Move and existing navigation | 4, 9 | Current state shown through existing views and endpoints |
| Legacy recovery and current storage/callers remain working | 8–9 plus regression evidence from 1–7 | Existing workflows work alongside new trash, without data migration |

Each final transition reuses existing canonical regression assertions and adds
only missing deltas. Do not write one new test per internal class or duplicate
all participation assertions in the browser. The isolated probe is mechanism
evidence only; it does not replace controller/web proof during implementation.

## Verification and delivery

Backend: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` (all backend unit
tests per backend.mdc); the view migration also requires `pnpm backend:verify`
through the same Nix prefix. Frontend: `CURSOR_DEV=true nix develop -c pnpm frontend:test`.
E2E: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/note_deletion.feature`.
API when needed: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`.
No manual testing or full E2E sweep is requested. Use existing isolated test
configuration; no experiments on production or shared application data.

Later authorized execution follows Jidoka, fresh dough-post-change-refactor agent,
API generation if needed, one coordinator `./scripts/run.sh pnpm format:changed`,
plan update, commit/check-only hook, push and asynchronous CI handling. Preserve
this plan for retrospective/wrap-up. Planning authorization executes none of that.

## Refinement result

Nine slices replace the withdrawn seventeen. No additional story split is
recommended. Each leaf is assessed Ready as a planning hypothesis: one bounded
responsibility/outcome and proof loop, with the selected query mechanism actually
checked. All estimates include active implementation and focused proof; required
external suite/tooling waits are the only stated timing exception.

The real residual risks are full-query performance at production scale and
integration-specific identity/undo/reference details, which execution must verify
at the named boundaries. They no longer stand in for an unresolved architecture.
The plan is ready for direct execution when separately authorized; no product
implementation has occurred. The second story and deferred Git story remain
unrefined, and the backlog order is unchanged.
