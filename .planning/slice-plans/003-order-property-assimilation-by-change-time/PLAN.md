# Plan: Order property assimilation by change time

**Source:** [SEED-101#story-1](../../seeds/SEED-101-property-assimilation-order.md#story-1)
**Source review:** [source-review.md](source-review.md)
**Identity:** SEED-101#story-1

## Goal and scope

Untracked property values enter the assimilation sequence in the order their
values were last added or changed (committer time of the last accepted commit
that changed the parsed value, never earlier than the note's creation), within
the same note level, for owned and subscribed notebooks alike. Web saves and
Git pushes date the values they change in the same transaction; a maintenance
job fills missing dates from retained history; an undated value sorts at its
note's creation time. Goal, scope, required restriction, scope limits, known
consequences, and key examples are in the source story and are not repeated.

**Exclusions (from the story):** note-level unit ordering, any date in the UI,
author time, clock normalization for CLI commits, re-dating values whose
history was reset before this lands.

**Assumptions:** every notebook is Git-bound from creation
(`NotebookService.createNotebook` → `startHistory`), so every web save and
push has a commit to date from; test-data injection (`InjectNotesWorker`)
refreshes the index without a commit, so injected values stay undated and sort
at note creation, which is what the e2e scenario relies on for its baseline.

## Current decisions

- **One dater.** `NotebookGitPropertyValueDating` (services/notebookGit) walks
  one notebook's commits from a tip back to an exclusive older commit (or to
  the root), diffs each commit against its parent with JGit rename detection
  so a note's path is followed backward, parses the frontmatter of changed
  note files on both sides with the existing `NoteContentMarkdown` /
  `Frontmatter` readers, and records for each value at the tip the committer
  time of the newest commit where the value differs from the parent (a scalar
  by content, a list item by presence). Values unchanged across the range get
  no entry; values first seen in a parentless root commit are reported as
  "present at root". Keys and items follow `NotePropertyIndexPlanner`'s
  rules, so the dater and the index agree on what a value is. Push and
  maintenance use it; the web save uses the same `changedValues` comparison on
  its one step; no second comparison or event log.
- **Value identity for dating** lives in `NotePropertyIndexPlanner`: the
  (key, index value) of each planned row plus the scalar's content, so the
  dater's "changed" test is the planner's own notion of a value.
- **Write rule.** A date written to a row is `max(commit committer time,
  note.createdAt)` (the floor); "present at root" writes `note.createdAt`.
  Rows are addressed by (note, property key, property value). Dates are only
  written for rows the index already holds.
- **Index refresh carries dates.** `NotePropertyIndexService.refreshForNote`
  still deletes and reinserts the note's rows (an in-place list reorder would
  collide on the unique `(note_id, property_key, item_index)` key), and each
  reinserted row takes the date its (key, value) had before; new values get a
  null date. Row ids change per save, as before; later slices address rows by
  (note, key, value). Nothing else about the refresh or its callers changes.
- **Web save.** `AcceptedWebChangeService.commitIfChanged`, after the append,
  compares each inserted or updated Note's content before the change
  (`ProjectionChangeCapture` records it at first flush) with its saved content
  through `NotePropertyIndexService.dateChangedValues` (which uses
  `NotePropertyIndexPlanner.changedValues` and `writeValueDate`), dating each
  changed value at the commit time (`updatedAt`). This is the story's one-step case;
  running the Git dater here broke the save-cost guards (tree fetches 39 vs a
  cap of 13), so the web save reads no Git objects for dating. No
  whole-notebook note load.
- **Push.** `NotebookGitProposalPublisher.publish` runs the dater over the
  newly accepted range (`acceptedHead` exclusive → `mainHead`) on
  `proposal.repository()` after application and before acceptance, maps tip
  paths to notes with `NotebookGitProjection.requireOneNoteAtPath(proposedNotes, path)`,
  and writes dates in the same transaction. A push that changes no note
  documents runs no dater.
- **Maintenance.** `NotePropertyValueDateRebuildService.rebuildOne()` picks
  one notebook with an undated row, opens its accepted repository, runs the
  dater over the whole retained history, writes dates for undated rows only
  (dated rows are authoritative), and returns whether a notebook was
  processed. `NotePropertyValueDateRebuildJob` (`@Service @Profile("prod")`,
  `@Scheduled(fixedDelay = 5 minutes)`, `@SchedulerLock(name =
  "notePropertyValueDateRebuild", lockAtMostFor = "4m", lockAtLeastAt =
  "30s")`) calls it up to 10 times per run and stops early when nothing is
  left. Failures propagate to the existing scheduled-job error handler
  (ADR 0006). In other profiles the job bean does not exist; the service is
  tested directly.
- **Ordering.** JPQL: `ORDER BY <level>, COALESCE(i.changedAt, n.createdAt),
  n.id, i.propertyKey, i.itemIndex` in both property queries;
  `AssimilationUnit` carries `time` (note unit: `createdAt`; property unit:
  the coalesced value, passed from JPQL) and `ORDER` compares level, time,
  note id, note-before-property, key. The merge of owned and subscribed
  streams in `AssimilationService` is unchanged.
- **Schema.** `V300000356__add_changed_at_to_note_property_index.sql` adds
  `changed_at DATETIME NULL`; regenerate `docs/database-erd.md` with the
  `database-erd` skill in the same slice.
- **Proof commands.** Backend, in this worktree:
  `./scripts/run.sh pnpm backend:test:worktree --tests '<one class pattern>'`
  (one `--tests` per invocation, observed 2026-11-04 through the equivalent
  `CURSOR_DEV=true nix develop -c` prefix that trunk replaced with
  `./scripts/run.sh` at a49cbb751c). E2E:
  `./scripts/run.sh pnpm cy:run --spec <feature file>`.
- **E2E clock.** `It's day N` sets the backend clock to 1976-06-(1+N) 08:00
  and injection stamps all injected notes with the current backend clock, so
  the e2e scenario sets the day before injecting and advances it before the
  save that must sort later.
- **ADR 0002.** Dates are derived ordering state written with the accepted
  head; the range walk reads intermediate commits as evidence and writes the
  index once; nothing rewrites history or adds a sync artifact. No ADR change.

## Decisive premises

| Premise | Consuming operation | Observation | Result |
| --- | --- | --- | --- |
| Property units order by level, note creation, note id, key, item index in JPQL and the in-memory comparator, and COALESCE in that ORDER BY already works. | Slice 2 changes both. | `NotePropertyIndexRepository.unassimilatedOrderBy` and `AssimilationUnit.ORDER` read 2026-11-04; `AssimilationServicePropertyUnitsTest` run in this worktree (see run below). | Settled. |
| The index is deleted and reinserted per save, and scalars are indexed with value `""`. | Slice 1 replaces the refresh; dater addresses rows by (key, value). | `NotePropertyIndexService.refreshForNote`, `NotePropertyIndexPlanner` read 2026-11-04. | Settled. |
| A web save appends one commit with committer time = `updatedAt`, with the previous head and new head in hand inside the publication transaction. | Slice 5 places the dater there. | `AcceptedWebChangeService.commitIfChanged` (lines 103–123) and `NotebookGitCommitBuilder.append`; `NotebookGitWebLinkingNotebookControllerTest` run in this worktree asserts one commit per web change (see run below). | Settled. |
| A push applies the final tree once and has `firstParentRange` over the accepted range with the stored notes list and `requireOneNoteAtPath`. | Slice 6. | `NotebookGitProposalPublisher.publish`, `NotebookGitProposalAncestry.firstParentRange`, `NotebookGitProjection.requireOneNoteAtPath` read 2026-11-04; `NotebookGitPublicationAtomicControllerTest` run in this worktree (see run below); test fixture `NotebookGitCommitFixtureTestSupport.commitOnTopOf` builds multi-commit chains (slice 4 added a committer-time overload). | Settled. |
| JGit rename detection is available and used in this codebase. | Slice 4 follows a note's path backward. | `NotebookGitProposalRenameDetector` uses `DiffEntry`/`RenameDetector` on JGit 7.8.0 (`backend/build.gradle:96`). | Settled for availability; the walker's own test proves the follow. |
| Scheduling is prod-only with ShedLock, and a job's method is testable directly. | Slice 8. | `SchedulingConfig` (`@Profile("prod")`), `ShedLockConfig`, `QuestionGenerationBatchMaintenanceJobTests` and `...SchedulerLockTest` exist and run in CI. | Settled. |
| Injected e2e notes are in a Git-bound notebook and keep injection-order creation times; adding a property on the note page commits through the web path. | Slice 5 e2e scenario. | `InjectNotesWorker.findOwnedOrCreate` → `notebookService.createNotebookForOwnership` → `startHistory`; `e2e_test/config/notebookPublicationState.ts` reads the injected notebook's accepted head; steps "I add a rich note property with key … and value …" and "I should be assimilating the note …" exist; `assimilation_walkthrough.feature` passes in "donut CI" on main at 8cc171f7bd (2026-11-03T23:28:33Z). | Settled for the route; the new scenario is the discriminating proof. |
| Retained histories are small enough for one whole-history walk per notebook inside one job call. | Slice 7 sizing in production. | Not observable here (production data). | Bounded, not blocking: the job processes one notebook per call under a 4-minute lock; a large history makes a run slow, not wrong, and the plan records it as a learning if production shows it. |

Backend test run (this worktree, 2026-11-04): `pnpm backend:test:worktree --tests <class>` for `AssimilationServicePropertyUnitsTest`, `NotebookGitWebLinkingNotebookControllerTest` and `NotebookGitPublicationAtomicControllerTest`, one invocation each, at d761d3f0da plus the uncommitted seed: `BUILD SUCCESSFUL` in 26 s, 17 s and 18 s (Gradle fails a `--tests` filter that matches nothing); the last class report shows 2 tests, 0 failures, 0 errors at 2026-11-04T02:07:29Z.

## Outside-in proof

| Story example | Owning slice | Proof |
| --- | --- | --- |
| 1. Property added today to an old tracked note → newer untracked note first | 2 (ordering with a dated row), 5 (date written by a web save; e2e end to end) | `AssimilationServicePropertyUnitsTest`; new scenario in `assimilation_walkthrough.feature` |
| 2. Body edit keeps the property's date | 1 (refresh carries), 5 (dater writes nothing) | `NotePropertyIndexServiceTest`; web controller test |
| 3. List reorder keeps dates; added item dated now | 1, 3, 5 | `NotePropertyIndexServiceTest`; `NotePropertyIndexPlannerTest`; web controller test |
| 4. Untracked scalar edit re-dates; tracked scalar edit re-queues nothing | 3, 5, 2 | planner diff test; web controller test; existing tracker matching tests stay green |
| 5. Three-commit push: per-commit times; remove-then-re-add dated at the re-add | 4, 6 | dater unit test on an in-memory repository; publish controller test with a three-commit bundle |
| 6. Pushed note committed before publication → floor; note unit before its properties | 6, 2 | publish controller test asserting `changed_at == createdAt`; `AssimilationServicePropertyUnitsTest` |
| 7. Pre-existing notebook: undated → as today; job fills; root-present → creation; second run idle | 2 (undated fallback), 7, 8 | existing ordering tests unchanged; rebuild service test; job annotation test |
| 8. Subscribed notebook follows the same order | 2 | `AssimilationServiceSubscriptionQueueTest` |

## Slices

### 1. Property index keeps a per-value date across saves

Type: Structure
Status: done
Proof: `NotePropertyIndexServiceTest`: a row dated in the test keeps its
date after a body-only edit and after a list reorder (item index updated),
loses its row when the value is removed, and a newly added value's row has
no date; the existing refresh, rename, delete and cascade tests stay green.
`docs/database-erd.md` regenerated.

Internal change: migration `V300000356` adds `changed_at` (nullable) to
`note_property_index`; `NotePropertyIndex.changedAt`; `refreshForNote`
carries each surviving (key, value)'s date to its reinserted row. External behavior unchanged. Enables slice 2.

### 2. Sequence orders property values by their change date

Type: Behavior
Status: done
Proof: `AssimilationServicePropertyUnitsTest`: an old tracked note's
property dated after a newer untracked note's creation is offered after that
note; the same property undated is offered before it (today's order); a
dated property earlier than its note's creation cannot occur (floor) but the
comparator and JPQL agree for equal times (note unit first, then key).
`AssimilationServiceSubscriptionQueueTest`: a dated property in a subscribed
notebook interleaves with owned material by date. Existing ordering tests
stay green.

Behavior: index rows with dates → the learner asks for the next unit → owned
and subscribed property units sort by level, then `COALESCE(changed_at,
note.created_at)`, then note id, note-before-property, key, item index, with
`AssimilationUnit.ORDER` agreeing.

### 3. Value change set between two frontmatters

Type: Structure
Status: done
Proof: `NotePropertyIndexPlannerTest`: given two frontmatters, the changed
set is: added list items, scalars whose content differs, scalars and items
new in the second; reordering a list, body-only differences, excluded keys,
blank and over-length items contribute nothing; an item removed is not in
the set.

Internal change: `NotePropertyIndexPlanner` exposes the dating identity of
each planned value (key, index value, comparable content) and a
`changedValues(before, after)` over two frontmatters. Enables 4.

### 4. Date a note's values from a commit range

Type: Structure
Status: done
Proof: unit test on an `InMemoryRepository` built with
`NotebookGitCommitFixtureTestSupport.commitOnTopOf` (given a committer
time parameter): three commits as in example 5 yield per-commit times for
N1 and N3 and no entry for N2; remove-then-re-add yields the re-add commit;
a file renamed in a middle commit is followed so an older change still dates
its values; a value present in the parentless root is reported "present at
root"; a range starting at a non-root exclusive commit reports nothing for
values unchanged inside the range.

Internal change: `NotebookGitPropertyValueDating.dateValues(repository,
olderExclusiveOrNull, tip)` as decided above. Enables 5, 6, 7.

### 5. Web saves date the values they change

Type: Behavior
Status: done
Proof: a `NotebookGitWebContentControllerTestBase` subclass: saving content
that adds `topic: physics` leaves that row with `changed_at` equal to the
save time and the appended commit's committer time; a body-only save keeps
an earlier date; reordering a list keeps both items' dates and dates a new
item; changing an untracked scalar's content re-dates it; a save on a note
whose creation time is later than the commit time is impossible on the web
(same clock), so the floor is asserted in slice 6. E2E: new scenario (its own
feature file under `e2e_test/features/assimilation/`, with its own Background
so the order of clock and injection is owned by the scenario): It's day 1;
there are notes from Note 1 to Note 2; Note 1 was assimilated on day 1; It's
day 2; I add a rich note property to Note 1 on its page; I start assimilation
from the menu; I should be assimilating "Note 2". The clock is set before
injection and advanced before the save because injection stamps every note
with the testability clock (`InjectNotesWorker` passes one
`currentUTCTimestamp` to `buildIndividualNotes`), the save uses the same
clock, and the floor would otherwise date a 1976 save at a 2026 creation.

Behavior: a learner saves a note on the web → the save appends its commit →
the saved note's added or changed values carry that commit's time; unchanged
values keep theirs → the next assimilation reflects it.

### 6. Git publication dates values across the accepted range

Type: Behavior
Status: planned
Proof: a publish controller test (beside
`NotebookGitPublicationAtomicControllerTest`) with a three-commit proposal
bundle whose commits carry distinct committer times: N1's `topic` row is
dated at c1, N3's at c3, N2's rows unchanged; `alpha` removed in c1 and
re-added in c3 is dated at c3; a note added in a commit dated 08:00 and
published at 09:30 has `changed_at` equal to its `created_at`; a push with
only attachment changes writes no dates. Existing publication tests stay
green.

Behavior: a CLI push is accepted → each pushed note's added or changed
values are dated at the committer time of the commit that changed them,
floored at the note's creation, in the acceptance transaction.

### 7. Maintenance fills missing dates from retained history

Type: Behavior
Status: planned
Proof: `NotePropertyValueDateRebuildServiceTest`: a Git-backed notebook
whose root commit holds note A with `topic` and a later web save that adds
`example of` to A and creates note B with `colors: [x]`; the rows are then
cleared of dates (as a pre-existing notebook would be) → `rebuildOne()`
returns true and dates `topic` at A's creation, `example of` at the save
commit, `x` at B's creation; a second `rebuildOne()` returns false and
changes nothing; a row already dated is not rewritten. A notebook after
`resetHistory` (single root) dates every undated row at note creation.

Behavior: index rows without dates exist → the rebuild runs → every row of
that notebook is dated from history or at note creation; the sequence then
uses those dates.

### 8. Rebuild runs on its own in production

Type: Behavior
Status: planned
Proof: `NotePropertyValueDateRebuildJobSchedulerLockTest` (annotation
presence, lock name and durations, as the existing lock test does) and a
job test that `run()` calls `rebuildOne()` until it returns false, at most
10 times.

Behavior: production runs with undated rows → every five minutes the job
dates up to ten notebooks until none remain, under one ShedLock, failing
loudly through the scheduled-job error handler.

## Concern evidence

Slice-plan refinement did not run: the slices are commit-sized, each owns one
proof loop, and the one mechanism (the dater) is built once in 3–4 and
consumed by 5, 6 and 7 without parallel representations. Sizing: 4, 5 and
6 each exceed the five-minute target because their proof is a Git fixture
with several commits; splitting them would separate the mechanism from the
only observation that evaluates it, so they stay whole with that reason
stated. No remaining slice-specific concern was identified.

## Learnings

- Slice 1 (2026-11-04): the unique key `(note_id, property_key, item_index)`
  rules out updating rows in place on a list reorder, so the refresh carries
  dates across delete-and-reinsert. Accepted proof:
  `./scripts/run.sh pnpm backend:test:worktree --tests '*NotePropertyIndexServiceTest'`
  (16 pass, nested `changeDate`), plus the consumer suites
  `AssimilationServicePropertyUnitsTest`, `AssimilationServicePropertyReferenceGateTest`,
  `NotePropertyIndexAuthoredReferenceTest`, `UnassimilatedPropertyServiceTest`,
  `AssimilationControllerAssimilateTests`, `AssimilationControllerTests`,
  `NotePropertyIndexPlannerTest`, `NotebookGitWebLinkingNotebookControllerTest`.
  `docs/database-erd.md` regenerated with no diff (the ERD lists key columns
  only). The column ships with the code that maps it, as V300000352 did.
- Slice 2 (2026-11-04): units from separate queries must compare the times the
  database returns, so the note-unit query also selects `n.createdAt`
  (`NoteAssimilationQueries.unitTime`) instead of reading the in-memory entity,
  whose sub-second precision the column drops. Accepted proof:
  `./scripts/run.sh pnpm backend:test:worktree --tests '*AssimilationService*'`
  (13 classes, 41 tests), plus `'*Assimilation*'`, `'*MemoryTracker*'`,
  `'*SubscriptionControllerTest'`, `'*NotePropertyIndex*'`,
  `'*UnassimilatedPropertyServiceTest'`.
- Slice 3 (2026-11-04): `NotePropertyIndexPlanner.changedValues(before, after)`
  returns `Set<DatedValue(propertyKey, propertyValue, content)>`; pass
  `Frontmatter.empty()` for a note with no earlier version. `tags`, `aliases`,
  `overlaps`, `cssclasses` and some relationship keys (`source`) are not
  indexed, so dating tests use neutral keys (`topic`, `colors`, `origin`).
  Accepted proof: `'*NotePropertyIndexPlannerTest'` (11 tests),
  `'*NotePropertyIndex*'`, `'*AssimilationService*'`.
- Slice 4 (2026-11-04): `NotebookGitPropertyValueDating.dateValues(repository,
  olderExclusiveOrNull, tip)` returns `List<ValueDate(tipPath, propertyKey,
  propertyValue, Optional<Instant> committedAt)>`; an empty `committedAt` means
  present at root, and the floor is the caller's. It runs its own first-parent
  loop because `NotebookGitProposalAncestry.firstParentRange` cannot walk to a
  root. `commitOnTopOf(..., Instant committedAt)` sets the committer time
  (author stays now). Note recognition is `PortablePathKind.isNote`. Accepted
  proof: `'*NotebookGitPropertyValueDatingTest'` (4 tests) and
  `'*NotebookGit*'` (144 classes, 475 tests).
- Slice 5 (2026-11-04): the shared write is
  `NotePropertyIndexService.writeValueDate(note, key, value, Optional<Instant>)`
  (floor at `createdAt`; empty writes `createdAt`). The e2e scenario is
  `e2e_test/features/assimilation/assimilation_property_order.feature`
  (registered in `scripts/isolated-cypress-active-specs.mjs`); it fails with the
  dating call removed. The web-path floor is not separately tested (same
  clock); slice 6 owns it. For slice 6: the dater's `tipNotes` parses every
  note blob at the tip, so a push's cost grows with notebook size; discovering
  tip notes from each commit's diff (root as a diff against the empty tree)
  passed the dater tests in a prototype. Accepted proof:
  `'*NotebookGitWebContentPropertyDatingControllerTest'` (7 tests),
  `'*NotebookGitWebContentSaveCost*'`, the full backend suite (3021 tests),
  and `SUT_TIMEOUT_MS=360000 ./scripts/run.sh pnpm cy:run --spec
  e2e_test/features/assimilation/assimilation_walkthrough.feature,e2e_test/features/assimilation/assimilation_property_order.feature`
  (10/10).
