# Cohesive trash participation

Status: in progress
Source: [SEED-029 story 1](../../seeds/SEED-029-cohesive-trash-participation.md#story-1).
Authority: 2026-09-18 owner request for a slice plan, refined if needed, with
simpler architecture, fewer lines of code, and greater cohesion. Planning only.

## Goal and acceptance

Make the distinction between stored content and participating content explicit
and consistently owned. Remove redundant collection retrieval in learning
sessions, using existing availability semantics. Preserve the integrated
learning-session fix and all affected trash, recovery, and Portable contracts.

This is the owner's explicitly selected internal structural improvement.
Structure slices directly own that outcome; no new user-facing Behavior is
invented to justify them.

Baseline: `d17559eb271b4a18c2e437e0432a64a14c9ceaaa`. It includes the fix
`4ea717b3de88c233fceab5bafc08f0323ad28f0f`, whose full backend suite and CI passed.
That evidence supports the original fix, not these unimplemented changes.
At planning entry, existing local edits removed the stale SEED-027 backlink
and its seed warning. Preserve those edits and their ownership.

Completion requires all three:

- Simpler architecture: learning-session recording no longer independently
  fetches overlapping all-note and available-note entity collections; no new
  availability service, policy registry, persisted flag, or compatibility mode.
- Less code: aggregate handwritten production additions minus deletions against
  the baseline is negative after formatting. Include new/extracted files and
  all affected production packages; report test additions/deletions and total
handwritten-code delta separately. Moving code or deleting blank lines is
  not structural simplification. Do not remove useful tests to meet a count.
- More cohesion: stored-note queries explicitly include trash; participation
  uses existing Note availability semantics; occupancy counts stored content.
  Callers do not recreate root-name or ancestor rules. Preserve necessary
  database/current-object/frontend representations and their state semantics.

Use `git diff --numstat d17559eb271b4a18c2e437e0432a64a14c9ceaaa -- backend/src/main/java frontend/src cli/src mcp-server/src`
for the production delta, expanding paths if another production package is
actually touched. Inspect each deletion's purpose. New untracked files must
be included before concluding. Documentation, generated files and planning
cleanup cannot offset production growth. Slice 4 owns the final aggregate gate;
record per-slice deltas as useful evidence, not a quota for each slice.

## Existing solutions and selected design

| Responsibility | Existing owner/evidence | Choice |
| --- | --- | --- |
| Membership of loaded objects | Note.isAvailable delegates to Folder.isTrashed; FolderRepositoryTest proves current ancestry before flush | Reuse directly when deriving candidates from loaded notes. Do not substitute a stale formula value. |
| Query-time participating notes | Note.JPA_AVAILABLE and NATIVE_AVAILABLE, derived from the trashed_folder view; used by search, recall, aliases, semantic search | Retain shared predicates for selective queries. No copy of their SQL in LearningSessionService. |
| Complete stored tree | NoteRepository.findLiveNotesByNotebookIdOrderByIdAsc; NotebookExportRows; NotebookGitStateLoader | Rename to an explicit all-notes query, and propagate the meaning through snapshot/projection consumers. Preserve trash inclusion. |
| Stored folder occupancy | findLiveNoteFolderIdsByNotebookId; FolderSubtreeLiveNotes; health and purge callers | Name by occupancy/subtree content, retaining trash as occupied content. |
| UI location membership | frontend/src/utils/folderTrash.ts, already reused by note/folder screens | Keep; replacing it requires an API change without a demonstrated need. |

In LearningSessionService.record, fetch stored notebook notes once. Derive
recognized titles from that collection and candidates through Note.isAvailable.
Ambiguity and tracker lookup use the same available subset. Preserve the
parser's diagnostic distinction: a stored-but-trashed title is recognized but
cannot supply a commissioned tracker. Reuse stream/set operations; introduce
no wrapper class merely to hold these collections.

The repository currently hand-writes a simple all-notes JPQL query. Prefer its
equivalent Spring Data derived method `findAllByNotebookIdOrderByIdAsc`, removing
the redundant annotation; verify notebook scoping, ordering and trash inclusion.
Use `storedNotes` for complete persisted collections and `availableNotes` for
participation. Tentative publication collections contain the proposed result;
name them `proposedNotes`, not available notes. Do not rename unrelated uses of
“live” that refer to a current transaction entity or Git history.

Preserve [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md)
trash/permanent-deletion vocabulary and
[ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
root membership, identity retention and Portable preservation. Both are Accepted.
The existing NORTH-STAR topics were inspected: no change to accepted-web-change
transactions or final publication application is needed; keep their owners.
No new North Star topic or ADR is warranted.

## Outside-in proof ownership

Tests are under `backend/src/test/java/com/odde/donut/`.

| Promise | Slice | Setup and observation |
| --- | --- | --- |
| Unique available title ignores a trashed duplicate, preserving feedback recording | 2 | controllers/LearningSessionRecordTests.recordsAvailableNoteWhenATrashedNoteHasTheSameTitle: one available commissioned note plus trash duplicate; controller response records the available title with no rejection. |
| Real duplicate available titles still reject; trash-only reports do not alter history/schedule | 1 establishes, 2 preserves | Keep the existing trash-only controller test; add the missing true-ambiguity controller case with same-title notes in different folders. Parser tests alone do not prove candidate selection. |
| Current ancestry and case-insensitive root/descendant rules remain consistent | 1 establishes, 2 preserves | entities/repositories/FolderRepositoryTest membership/transaction tests; exercise nested-trash and ordinary nested `_trash` via the controller candidate setup, reusing existing proof where sufficient. |
| One note-list retrieval does not cause ancestor-query amplification | 1 measures baseline, 2 owns result | Isolated controller diagnostic with cold persistence context, notes spread across sibling/nested folders, one submitted commissioned item. Compare before/after SQL counts attributable to folder loading, identical fixture/selection. |
| Export retains trashed files and deterministic stored-note selection | 3 | controllers/NotebookExportControllerTest: extend ZIP observation with an available note and nested trash file; controller Git tests cover actual stored projection. Confirm scope/order if not already observed. |
| Trash/recovery retains Portable content, identity and tracking preferences | 3 | controllers/NotebookGitProposalFolderRelocationTrashRoundTripControllerTest publishes trash then recovery and inspects accepted paths/content and IDs; controllers/NoteTrashRecoveryLearningPreferencesTest retains independent tracker opt-out/history. |
| Occupied trash folders are not treated as empty or purged | 4 | controllers/NotebookHealthControllerTest: health/fix with a stored note in trash; assert occupied folder/note survives. Retain cascade/FK fixture coverage; no deletion semantic change. |
| Simpler architecture, lower production LOC, cohesive boundaries | 4 | Aggregate formatted diff and production-path count versus baseline, plus review of all changed query callers. No stale ambiguous collection API or added local trash detector. |

## Ordered slices

### 1. Establish the learning-participation preservation baseline

Type: Structure
Status: done
Outcome: close the missing controller-level participation proof and obtain the
matching query baseline required to evaluate retrieval simplification.

Perform the cold-context query observation before changing retrieval. Reuse
Hibernate Statistics, already used in services/RecallStatsPerformanceTest;
scope collection to the controller call after fixture creation and flush/clear.
Restore statistics state afterward. Keep diagnostic scaffolding disposable;
record its exact invocation, fixture sizes, SQL categories/counts and result in
this plan. Do not add a production export, mock repository, new Spring context,
or profiling framework for this observation.

Add only missing candidate-selection observations mapped above, parameterizing
membership examples when their assertion is identical. Use two cold fixtures
with the same report and note count but different numbers/depths of folders to
distinguish query growth from fixed overhead. Capture actual counts rather than
inventing a numeric performance threshold. Retain reusable diagnostic input
under this plan's directory until slice 2 has finished its comparison; it is
temporary evidence, not product instrumentation.

Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`, one complete
baseline run including the controller observations and diagnostic. Record
fixture identity, revision, counts and literal command here. Expected preserved
behavior must pass before changing production; an unexpected mismatch stops
only dependent work for diagnosis.

Sizing: ~5 minutes including the suite, medium confidence. Safe stop: stronger
behavioral coverage and recoverable measurement evidence; production unchanged.

Execution record, 2026-09-18, revision `7de4fd470c` (production unchanged):

- Added controller observations in `controllers/LearningSessionRecordTests`:
  `rejectsAmbiguousTitleWhenTwoAvailableNotesInDifferentFoldersShareIt` (two
  available commissioned `Hola` notes in `greetings` and `slang`; rejected as
  "Ambiguous note title in notebook.", no recall log, both trackers' learning
  state unchanged), parameterized
  `rejectsNoteBeneathRootTrashWithoutChangingItsLearningHistoryOrSchedule`
  (rows `_trash`, `_TRASH`, `_trash/sub`; each rejected "No commissioned
  memory tracker" with no recall log and unchanged tracker learning state; the
  former trash-only test is its `_trash` row, same scenario and assertions),
  and `recordsCommissionedNoteInAFolderNamedTrashBeneathAnOrdinaryRoot`
  (`docs/_trash`; recorded with the fixture tracker id). FolderRepositoryTest
  already proves the repository-level membership rules; nothing duplicated.
- Post-change refactor merged the pre-existing trash-only test into that
  parameterized test and re-ran
  `CURSOR_DEV=true nix develop -c pnpm backend:test:worktree --tests 'com.odde.donut.controllers.LearningSessionRecordTests'`:
  10 tests, 0 failures. The full-suite proof above stands for all other paths.
- Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` after
  `unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL`, in the execution
  worktree's isolated database: BUILD SUCCESSFUL, 2481 tests, 0 failures.
- Cold-context baseline (Hibernate Statistics, `em.flush(); em.clear();` then
  one `controller.record` of the one-line report `Hola: 4`), diagnostic source
  retained as `query-baseline-diagnostic.java.txt` in this directory and removed
  from `backend/src/test` before commit. Both fixtures hold six notes and one
  commissioned `Hola` tracker. Fixture A: one root folder, depth 1. Fixture B:
  roots `a`, `b`, `c`, `_trash`; nested `b/b-child` (holds `Hola`),
  `b/b-child/b-grandchild`, `_trash/sub` (holds a trashed note); seven folders,
  depth 3. Result for both fixtures, identical: prepareStatementCount 5,
  queryExecutionCount 3, entityLoadCount 10, entityFetchCount 0, Folder
  loads 0. The three HQL/native executions are the stored-notes list, the
  available-notes list, and one `MemoryTrackerRepository.findByUserAndNote`;
  the other two statements are the RecallLog insert and the notebook/user
  resolution after clearing. The current two-query design therefore has no
  folder-dependent growth: the trash predicate is evaluated in SQL through the
  `trashed_folder` view.

### 2. Derive learning participation from shared note availability

Type: Structure
Status: awaiting story review
Depends on: slice 1's passing behavior and query baseline.

Reassessment, 2026-09-18, from slice 1's baseline: `Note.folder` and
`Folder.parentFolder` are both lazy and `selectFromNote` has no fetch join, so
deriving candidates through `Note.isAvailable` over the single stored-notes
read would initialize one proxy per distinct folder and ancestor reached
(about +1 select on fixture A, about +7 on fixture B) while removing exactly one
bounded query. That is the per-folder/per-depth growth this slice names as its
stop condition, so the retrieval choice below must be revised before editing.
The invalidated assumption is the seed's selected structural direction "direct
reuse of Note.isAvailable for already-loaded objects" (SEED-029 story 1, Open
Decisions) and this plan's "Do not substitute a stale formula value" in the
existing-solutions table. Candidate revisions, none authorized yet:

- Filter the single stored-notes read by the row's already-loaded persisted
  membership (`Note.trashedInDatabase`, the same value `Note.JPA_AVAILABLE`
  tests in SQL). In `record` no folder moves precede the read, so this is not
  stale here; it keeps one query, removes the second list, and keeps the
  diagnostic distinction. It adds an object-level accessor for persisted
  membership beside `Note.isTrashed` (current ancestry), which the plan's
  cohesion gate must accept explicitly.
- Keep the existing two query-level reads (the plan's stated fallback). That
  leaves acceptance gate 1 unmet, so the story would be incomplete.
- Fetch-join folders in the stored read. Ancestor proxies still load per
  depth, so growth remains; not recommended.
Outcome: eliminate the duplicate note-list retrieval and use the shared
object-domain predicate for already-loaded note participation.

Apply the single-read candidate derivation in LearningSessionService. Simplify
intermediate title variables only where their removal improves readability.
Keep report parsing, validation order, scheduling, and rejection messages.
Preserve the mapped controller cases in this slice. They must be green
before delivery; no separate committed failing-test slice.

Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`, reusing slice 1's
baseline rather than measuring it again; all backend tests per project rules.
Compare folder SQL counts on the same cold fixture. If removing the second
list query introduces per-folder/per-depth query growth, stop this slice and
revise the retrieval choice. Do not claim fewer database calls merely because
one repository call was removed, or compensate with a global eager-loading
change/cache. Existing two-query behavior stays the fallback until a smaller
safe design is evidenced.

Sizing: ~5 minutes of edits and one relevant proof loop. The full backend suite
is an explicit test-duration exception to the 5-minute target if needed, not
permission for open-ended optimization. If slice 1 establishes that all-note
ancestry traversal already makes the proposed approach unsuitable, revise this
slice before editing; do not implement a known-bad retrieval to test it again.
Safe stop: original diagnostics and correct participation remain intact.

### 3. Make the complete stored-note boundary explicit

Type: Structure
Status: planned
Outcome: replace the misleading all-notes “live” query contract with an explicit
stored-content contract across repository, export and Git snapshot consumers.

Rename the query and use the equivalent derived method. Update every production
and test reference atomically; propagate stored/proposed collection meaning
through NotebookGitStateLoader, LockedNotebooks, NotebookGitProjection,
AcceptedWebChangeService and callers. Include NotebookExportRows documentation.
This is a mechanical semantic rename, not a change to Git correspondence,
identity mapping or transaction orchestration. No deprecated forwarding alias.

Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`; mapped export,
trash/recovery and scope/order observations must pass through real persistence.
Search the affected collection symbols to verify no obsolete contract remains.
No full Cypress run or API generation for an internal repository method rename.

Sizing: ~5 minutes, medium confidence: many references but one mechanical
contract and one proof loop. Inspect changed files over 250 lines under the
post-change refactor rule; do not turn a rename into unrelated decomposition.
If compliance requires broad restructuring, return that scope/sizing concern
before edits instead of hiding it in the rename.
Safe stop: export/Git retain all stored files and the same accepted results.

### 4. Align folder occupancy and verify the aggregate simplification

Type: Structure
Status: planned
Outcome: express health's occupancy rule as stored subtree content, completing
the stored-versus-participating distinction without excluding trash from purge
safety checks.

Rename findLiveNoteFolderIdsByNotebookId to findOccupiedFolderIdsByNotebookId.
Rename FolderSubtreeLiveNotes and its live-note terminology around stored
subtree occupancy, updating EmptyFolderHealthRule, ReadmeOnlyFolderHealthRule
and EmptyFolderBulkPurge. Retain DISTINCT, all-notes membership and existing
subtree/readme/cascade rules. Do not replace occupancy with availability.
Leave health rules that intentionally fetch available notes on that query;
clarify misleading local variable names when directly implicated.

Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`; real health/fix
preservation observations above. Then inspect the aggregate production diff,
report line deltas and removed responsibilities/queries, and apply all three
acceptance gates. A nonnegative production delta or added special-case owner
leaves the story incomplete even if tests pass. Do not trim unrelated code to
meet the gate.

Sizing: ~5 minutes, one occupancy contract and one proof loop. Safe stop:
occupied recoverable content cannot become an empty-folder deletion candidate.

## Execution identity

- Mode: Story Branch. Replanning: no `--replan`/`--no-replan` flag; the plan's
  own overrun guidance (stop/redecompose at ten minutes) applies.
- Originating checkout `/Users/terryyin/git/doughnut` on integration branch
  `main`; claim commit `7de4fd470c` moved SEED-029 to Taken.
- Execution checkout `/Users/terryyin/git/doughnut/.claude/worktrees/cohesive-trash-participation`
  on branch `cohesive-trash-participation`, created from the claim commit.
- Push destination `origin` (`nerds-odd-e/doughnut`) branch
  `cohesive-trash-participation`; later integration target `main`.
- CI observer: GitHub Actions `ci.yml` (display name `donut CI`) on branch
  `cohesive-trash-participation`; mailbox `/tmp/dough-ci-501/watch-wePcZd`.

## Execution and delivery constraints

Future execution uses one isolated Story Branch worktree from the then-current
main. No worktree/branch or queue claim is created by planning. Leave SEED-029
first in Backlog list until authorized execution takes it. Retain the baseline
and execution identity here once established; no duplicate tracking artifact.

Use general, backend/backend-code/backend-testing and unit-testing guidance.
All backend tests run for each changed slice, with real DB and MakeMe fixtures.
Target about five minutes per slice; scrutinize longer edits, and at ten minutes
stop/redecompose unless unavoidable required test time is the recorded reason.
The original bug-repair ten-minute no-replan attempt is complete and does not
become a new limit or retry entitlement for this story.

Each slice: Jidoka check, fresh dough-post-change-refactor agent, generation only
if actually triggered, coordinator `./scripts/run.sh pnpm format:changed` once,
update plan, commit with the check-only lint hook, then push and observe CI under
dough-execute-plan. Preserve unrelated changes. Do not manually edit generated
files. Keep this plan and source for retrospective and later story wrap-up.

## Exclusions and assessment

No UI change, rejection-message change, new trash API, generalized policy
service, schema redesign, permanent-deletion redesign, frontend helper rewrite,
or performance project. Necessary performance preservation in slice 1 is bounded
to the changed retrieval. Query/object membership implementations remain separate
because persisted state and current transaction ancestry differ.

Cumulative model: one meaning for stored content, one meaning for participation,
and existing membership owners; later slices clarify the same model instead of
adding recognizers. Main remaining concern is slice 2's lazy ancestor loading;
slice 1 now provides its prerequisite evidence. Slice 3 also has mechanical
breadth and file-size-rule risk. No timing or query improvement is claimed from
this planning inspection alone.

Plan refinement, 2026-09-18: replaced the original combined baseline-and-change
slice with slices 1 and 2. Four Structure slices remain. Slice 1 can begin once
execution is authorized; slice 2 is conditional on its evidence. Slices 3 and 4
each have one contract and one proof loop. All directly own the user-authorized
internal correction. No story resplit or broader product outcome is proposed.
