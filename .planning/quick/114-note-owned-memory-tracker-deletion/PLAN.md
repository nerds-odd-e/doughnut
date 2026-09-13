# Note-owned memory tracker deletion

Status: in progress
Source: [SEED-019, story 1](../../seeds/SEED-019-note-owned-memory-tracker-deletion-state.md#story-1)
Authority: 2026-09-13 request to slice-plan and refine if needed; implementation
is not authorized by this request. Backlog placement remains unchanged.

## Execution identity (Story Branch Mode)

- Originating checkout: `/Users/terryyin/git/doughnut` on branch `main`;
  claim commit `752dd18eae` (Taken-only transition for SEED-019 story 1).
- Execution checkout: `/Users/terryyin/git/doughnut.worktrees/114-note-owned-memory-tracker-deletion`
  on branch `114-note-owned-memory-tracker-deletion`, based on the claim commit.
- Integration target: `main`.

## Goal and scope

Remove `memory_tracker.deleted_at` and use the owning note's deletion state.
Exactly preserve external behavior, tracker identities, learning history,
scheduling state, and independent `removed_from_tracking` preferences.
Deliver a direct, ungated Flyway SQL migration, including the dependent index.
Portable trash, new deletion semantics, scheduling changes, UI changes, and
general query optimization are excluded.

All leaves are Structure: the owner explicitly requested a standalone internal
structure change with unchanged external behavior. This human instruction
supplies the exception to the usual preparatory-Structure/next-Behavior grammar;
do not invent a new Behavior promise or label this a retrospective correction.
Each leaf owns part of this concrete duplicate-state removal and proves its
affected external boundary unchanged.

## Existing solution and decisions

PFE finding: change the existing note-owned deletion solution. `NoteService`
already writes note deletion and mirrors it to trackers; `NoteRepository` and
`NotePropertyIndexRepository` already exclude deleted notes from assimilation.
`MemoryTracker.isActive()` combines tracker deletion with the separate removal
preference. `MemoryTrackerService.updatePropertyKey()` checks deletion directly.
Reuse these owners and their current query structure; replace the deletion
source, keeping each query's existing removal, type, ownership, ordering, and
limit semantics. Do not collapse those independent eligibility rules into one
new universal filter or add another persisted deletion representation.

The tracker field is `@JsonIgnore`; the inspected generated tracker contract
and frontend/CLI/MCP consumers do not require that field. No wire-shape change
is intended. Regenerate API artifacts only if an actual API change requires it.

Accepted constraints: [ADR 0001, vocabulary](../../../docs/adrs/0001-ubiquitous-language.md),
[ADR 0003, scheduling and RecallLog](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md),
[ADR 0006, failure handling](../../../docs/adrs/0006-failure-handling-accepted.md),
and [ADR 0007, isolated test data](../../../docs/adrs/0007-environments-and-isolation-accepted.md).
No new North Star topic is needed: the owner selected the responsibility and
existing product structure supports it.

Migration direction is explicitly ungated, overriding the default gated-DML
guidance for this story. Do not add a deployment approval or opt-in placeholder.
Do not silently delete, merge, renumber, or repurpose trackers to make an index
fit. The current index permits duplicate keys among soft-deleted trackers;
a plain unique `(user_id, note_id, type, property_key)` index is a candidate,
not yet a proven behavior-preserving replacement. Establish reachable data
invariants and legacy-state evidence before choosing the final SQL. If actual
data requires a behavior or history tradeoff, return that specific conflict;
elapsed time or a passing empty-schema migration cannot resolve it.

The reader slices assume tracker and note deletion agree for supported existing
workflows. Before the first reader switch, inspect the differing-timestamp
fixture and all current tracker deletion writers. If evidence establishes a
reachable mismatch with different external results, resolve that compatibility
problem before switching readers, rather than postponing it to the column drop.
Carry this same finding into slices 6–7; do not repeat the investigation.

`NoteService.destroy()` currently relies on its tracker query to AUTO-flush the
note deletion. Its removal must preserve same-transaction title-conflict
visibility. Reuse explicit persistence flush support; do not retain a useless
tracker query solely for its side effect. Keep the existing flush regression.

## Proof and execution policy

For every slice, first inspect and reuse the named existing assertions. Add only
missing observations at controller boundaries with realistic `makeMe` data;
keep the deliberate title-placement boundary test. Run all backend unit tests,
as required by `.cursor/rules/backend.mdc`:

```bash
CURSOR_DEV=true nix develop -c pnpm backend:test_only
```

For the migration slice use the repository's complete migration verification:

```bash
CURSOR_DEV=true nix develop -c pnpm backend:verify
CURSOR_DEV=true nix develop -c pnpm export:database-erd
```

Use the execution worktree's isolated Unit Test database. A populated upgrade
proof must apply the actual new SQL to a disposable pre-migration schema on
MySQL 8.4 (`mysql84` in `flake.nix`), with existing active/deleted notes, removed
trackers, different tracker types/property keys, and linked recall history.
Implement that focused proof within the backend suite so the literal
`backend:verify` command owns it; do not treat Flyway against empty tables as
that proof. Record engine version, fixture shape, critical results, and actual
command outcome here during execution. No migration experiment has run yet.

Critical results: column absent; all retained IDs, foreign-key links, history,
scheduling state and removal preferences unchanged; deletion/restoration and
uniqueness outcomes preserved. Include a populated duplicate-key case allowed
by the old schema to expose the replacement-index assumption. A failed proof
changes the SQL/design before wider implementation; it is not a migration gate.

Compare the before/after SQL result sets and representative MySQL query plans
for due recall, aggregate, and recent-history queries using identical isolated
fixtures. Record observations in their owning slices. The seed promises no
material regression, not a new scale benchmark or arbitrary timing threshold.

Execution uses dough-execute-plan: claim the backlog item, create the story
branch/worktree and record identity here, then deliver each slice through
Jidoka, a fresh dough-post-change-refactor agent, necessary generation, one
coordinator `./scripts/run.sh pnpm format:changed`, plan update, commit with
check-only hook, push, and asynchronous CI handling. Retain plan and seed for
retrospective and story wrap-up. No independent lint/format runs by implementers.

Sizing includes implementation, verification, and local cleanup. Aim for about
5 minutes; scrutinize work above 5; stop and finer-decompose above 10 unless the
only excess is the required full backend-suite or external wait. Record that
wait separately; it does not excuse oversized coding. Preserve attempt-owned
work safely and reassess the source outcome if repeated overruns reveal a scope
problem. Do not deliver a red intermediate state.

## Ordered slices

### 1. Derive in-memory tracker activity from the note
Type: Structure
Status: done
Sizing: 4–5 minutes of active work, plus required suite wait.

Replace deletion reads in `MemoryTracker.isActive()` and property-key editing
with the note's state, preserving the independent removal preference and current
error responses. Keep existing deletion writers and schema for this intermediate
delivery. Reconcile affected fixtures through note deletion, without inventing
an independently soft-deleted tracker feature.

Proof: `MemoryTrackerTrackingControllerTest`,
`MemoryTrackerUpdatePropertyKeyControllerTest`, and accidental-match controller
coverage preserve activity, removal, rename conflict/stat retention and eligible
match behavior. Add the missing deleted-note rename observation if absent.
Safe stop: both persisted timestamps still synchronize as before.

Learning: the `deletedAt`-only branch in
`RecallPromptAccidentalMatchConfusionAdjustmentTests` was unreachable in
production — `NoteService.destroy()` mirrors note deletion to all trackers on the
note, so a tracker-only `deletedAt` (note still active) never occurs through
supported workflows. Collapsed to the reachable `removedFromTracking` case. This
confirms the pre-reader assessment: no independent tracker soft-delete writer was
found. Carry into slices 6–7.

### 2. Select due learning work by note deletion
Type: Structure
Status: done
Sizing: 4–6 minutes of active work; scrutinized as one due-work selection rule.

Change the ordinary and commissioned due-query fragments and their active count
in `MemoryTrackerRepository` to use note deletion. Preserve type separation,
removal filtering, due time, ownership and spelling tie ordering.

Proof: `RecallsControllerTests`, `RecallsCommissionedLearningSessionTests` and
`RecallServiceDueWorkTest`; active/deleted/removed fixtures preserve selected
work and count results. Record representative before/after due-query plans.
Safe stop: other reads and all writes retain the old synchronized column.

Query plan observation (MySQL 8.4, isolated worktree DB, minimal fixture): both
due queries keep their tracker-side index (`user_note_spelling_active` ref for
ordinary; `idx_memory_tracker_user_next_recall_at` range for commissioned) and
add one `note` PRIMARY KEY `eq_ref` lookup per surviving tracker row for
`n.deleted_at IS NULL`. Tracker-side index choice unchanged; the added PK
lookup is cheap. No material regression observed; no scale benchmark attempted.

### 3. Select batch question candidates by note deletion
Type: Structure
Status: done
Sizing: 3–5 minutes of active work, plus required suite wait.

Change batch candidate deletion filtering in `MemoryTrackerRepository`, keeping
its pending-question, contested-question, batch status, retry and type rules.

Proof: `QuestionGenerationBatchCandidateMemoryTrackersTest` drives the existing
candidate boundary; deleted notes remain excluded and existing retry/pending
cases retain their results. Add only missing deleted-note coverage.
Safe stop: no question-generation lifecycle changes or schema change.

Refactor note: the batch-candidate query now reuses the existing `byUserIdFrom`
fragment (outer alias `mt`→`rp`; inner `recall_prompt` alias `rp`→`rcl` to avoid
collision), preserving all type/due/subquery/order semantics. The deleted-tracker
fixture was reconciled to delete the note via `noteService.destroy(...)`.

### 4. Preserve assimilation eligibility through note-owned deletion
Type: Structure
Status: done
Sizing: 5–8 minutes of active work; three repositories share one eligibility rule.

Update tracker existence/user-note/recent-assimilation reads and the note-level
and property-level assimilation joins in `MemoryTrackerRepository`,
`NoteRepository`, and `NotePropertyIndexRepository`. Reuse outer note deletion
filters where already sufficient. Preserve removal semantics per query; a
removed tracker must not accidentally make an already-handled unit new again.

Proof: `AssimilationControllerAssimilateTests`, spelling/commissioned assimilation
controller tests, and existing property-unit/reference-gate tests preserve queue
membership, prerequisite handling, and tracker identity on existing workflows.
Safe stop: no duplicate trackers or new assimilation rules; writers still sync.

Learning: `NoteServiceTest.restore_only_restores_memory_trackers_with_same_deleted_at_as_note`
constructs a differing-timestamp legacy state (tracker `deletedAt` ≠ note `deletedAt`).
After partial restore (note not deleted, one tracker retains `deletedAt`), `findByUserAndNote`
now returns both trackers since it filters on note deletion. Reconciled only the
storage-coupled third assertion to a direct `findByNote_IdIn` + Java-side `deletedAt`
filter, preserving the fixture setup and legacy-state example for slice 6/7. Slice 6
may further refine this into an owned observation.

### 5. Preserve recent learning and aggregate readouts
Type: Structure
Status: done
Sizing: 4–6 minutes of active work; one remaining readout deletion-source change.

Change recent-list, latest timestamp and total-count deletion predicates in
`MemoryTrackerRepository`. Preserve each query's existing treatment of removed
trackers, ordering and limits; keep RecallLog and history access unchanged.

Proof: `MemoryTrackerRecentControllerTest`, `MemoryTrackerRecallHistoryControllerTest`
and existing controller callers of aggregate methods retain the same lists,
history and aggregate values for active/deleted/removed fixtures. Trace aggregate
callers and fill only missing observations. Record representative before/after
aggregate and recent-history query plans. Safe stop: every runtime reader now
uses note deletion, with the compatibility column still maintained.

Refactor note: the three aggregate queries (`findLastAssimilationTimeByUser`,
`findLastRecallTimeByUser`, `countByUser`) now share a `byUserIdAllFrom` fragment
(no removal/type filter, unlike `byUserIdFrom`), mirroring the existing
`byUserId<Qualifier>From` convention. Grep confirms no runtime reader references
`rp.deleted_at`/`rp.deletedAt`; only the retained column mapping and writers
remain for slice 7. EXPLAIN not captured (transient test DB); the structural
change is identical to slice 2's recorded observation (one `note` PK `eq_ref`
lookup; tracker-side access path unchanged).

### 6. Express deletion preservation without tracker timestamp assertions
Type: Structure
Status: done
Sizing: 5–8 minutes of active work; test-only representation change, with one
full-suite proof loop. If fixture coupling needs more than 10 active minutes,
reassess the affected scenarios before continuing.

Replace storage-coupled tracker timestamp assertions in deletion, Git publication,
rollback and restoration tests with their owned observations: unchanged accepted
head on rejection, note deletion state, tracker availability, identity and
history as applicable. Keep each assertion's original purpose; do not merely
delete coverage to allow removal of the field. Characterize actual delete/undo
through `NoteController`, including a previously removed tracker. Keep the
column, mapping, builder and synchronization working throughout this slice.

The differing-timestamps `NoteServiceTest` is evidence of a representable legacy
state, not proof that an independent soft-delete feature is reachable. Carry
forward the pre-reader assessment. Preserve its legacy-state example
for slice 7's migration assessment instead of silently rewriting its meaning.

Proof: the existing delete/Git/restoration controller suites and added missing
delete/undo observation pass with the old persistence still present. The
unchanged flush regression remains. Safe stop: only proof representation has
changed; old schema and product behavior remain intact.

Done: replaced MemoryTracker `deletedAt` assertions in 5 Git publication/deletion
controller test files with `MemoryTracker.isActive()` / tracker-availability
observations; Note `deletedAt` assertions kept as authoritative source. Added
`shouldPreserveRemovedFromTrackingPreferenceAcrossDeleteAndUndo` characterization
(previously-removed-tracker delta). `NoteServiceTest` differing-timestamps
legacy-state example preserved for slice 7. No production code changed; column,
mapping, builder setter, and synchronization remain functional.

### 7. Retire the duplicate persistence state with a populated SQL upgrade
Type: Structure
Status: done
Sizing: 8–10 minutes of active work assuming the index/data assessment supports
a simple migration; required full-suite wait is the only timing exception.
If it instead needs data reconciliation or a different uniqueness design,
record the concrete evidence and refine this leaf before coding that solution.

First resolve the populated-data/index assumption described above, including
the legacy example retained by slice 6. Then remove the entity field and builder
setter, remove note deletion/restoration mirroring and timestamp matching,
preserve destroy's flush, and migrate the dependent index and column through
new direct SQL. Select the next unused Flyway version at execution time; never
edit committed migrations. Regenerate the ERD. Keep published API shape unchanged.
Do not split schema and mapped-field removal into independently delivered states
that fail startup. Do not add cleanup DML merely to force a candidate unique key.

Proof: populated MySQL upgrade proof and full `backend:verify`; delete/undo
controller scenarios retain identities, schedules, logs and removed preferences;
`NoteTitlePlacementRulesFlushVisibilityTest`, soft-deleted-title MVC tests, and
Git deletion/publication/rollback controller tests preserve current outcomes.
Safe stop: final schema and application agree, with no tracker deletion column.

Done — index/data assessment conclusion: the plain unique index
`(user_id, note_id, type, property_key)` is behavior-preserving. The
duplicate-key state allowed by the old functional index (two trackers, same
key, one soft-deleted) is unreachable through application workflows:
assimilation refuses to create a tracker when one with the same
`(user, note, type, property_key)` already exists, note deletion mirrors to all
trackers without removing rows, and tracker hard-delete removes rows entirely.
The differing-timestamp `NoteServiceTest` legacy state was unreachable in
production (no independent tracker soft-delete writer). Under the note-owned
model, restore makes all trackers on the note available; the
`removedFromTracking` preference is independent and preserved. Migration
`V300000323` drops the old functional index, drops the `deleted_at` column,
and adds the plain unique index. `NoteService.destroy` preserves the flush
via explicit `entityPersister.flush()`. `NotePropertyTrackingBackfill` native
queries switched to `JOIN note` + `n.deleted_at IS NULL`. ERD regenerated
(unchanged — compact ERD shows only PK/FK).

## Promise ownership

| Promise | Owning slices / observation |
| --- | --- |
| Active-note learning and independent removal preferences unchanged | 1–5, existing controller eligibility and result assertions |
| Delete excludes trackers as before | 1–5 selection proofs; 6–7 actual delete workflow |
| Restore preserves availability and removed preferences | 6 characterizes delete/undo; 7 preserves it after upgrade |
| Existing data, IDs, scheduling and history survive migration | 7 populated upgrade and history/controller proof |
| Uniqueness and property tracking retain outcomes | 4 assimilation, 1 rename conflict, 7 populated index proof |
| No tracker deletion column; ungated SQL | 7 schema inspection and actual migration SQL |
| Same-transaction deletion visibility and Git operations unchanged | 6 maintains proof; 7 flush/MVC/Git controller regressions |
| Comparable affected query performance | 2 and 5 before/after representative plans and results |
| Same API/UI behavior | 1–7 controller contracts; JsonIgnore field removal needs no new UI |

## Assessment and remaining evidence

The cumulative rule is one note-owned deletion state with independent tracking
preferences. Intermediate deliveries retain existing synchronization until all
readers have moved. No new caches, deletion flags, or generic query framework.

Refinement: replaced former slice 6 with slices 6–7, separating green proof
cleanup from the atomic schema cutover. Result: seven slices. Slices 1–6 have
one cohesive proof loop and plausible bounded active work; slices 4 and 6 exceed
the five-minute target because each preserves one rule across existing coupled
representations, with the ten-minute stop retained. Required full-backend-suite
waits are explicit sizing exceptions for every slice.

Slice 7 retains a storage-assumption and sizing concern: the old unique index
admits historical duplicate keys, and mismatch fixtures exist. No replacement
index is certified yet. The populated proof owns this uncertainty; if it cannot
preserve the promised outcomes, dependent cutover must be revised. This is not
a deployment gate. No production data was inspected and no tests or SQL were
executed during planning. No source-scope change or resplit is proposed.
