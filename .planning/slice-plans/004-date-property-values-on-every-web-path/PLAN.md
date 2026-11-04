# Plan: Date property values on every web path and keep one value identity

**Source:** [SEED-101#story-2](../../seeds/SEED-101-property-assimilation-order.md#story-2)
**Source review:** [source-review.md](source-review.md)
**Identity:** SEED-101#story-2

## Provenance and findings

Correction from the execution retrospective of SEED-101#story-1 (plan
`.planning/slice-plans/003-order-property-assimilation-by-change-time/PLAN.md` at
`aff3fe6ea556b0800f0a7cbc0c95f09eba144bd7`, commits
`44fb2233e0`..`0beb46cd03`, reviewed 2026-11-04 at `0beb46cd03`). Current
findings, re-read 2026-11-04 at `bae7906634` during story refinement:

1. `AiController.createExtractedNote` → `NoteConstructionService.createNoteFromExtractedSuggestion`
   rewrites the original note and creates a new one inside `@Transactional`
   only, outside `AcceptedWebChangeService.apply`, so no commit is appended
   and no value is dated. A value added this way sorts at its note's creation,
   the rebuild can only date it at creation (no history), and the notebook's
   Git history lacks both notes until a later change. `PropertyReifyService`
   shows the wrapping pattern. The missing commit is an older ADR 0002 gap that
   story-1's dating depends on. Every other note-writing web path already runs
   inside `apply` (see the story's facts).
2. `NotePropertyIndexRepository.findNotebookIdsWithUndatedValues` has no
   binding condition and selects notebooks without a binding;
   `NotePropertyValueDateRebuildService.rebuild` throws on a missing binding, so
   the same notebook can fail every run and starve the rest. Unbound notebooks
   are a supported state (`makeMe` notebooks, `resetHistory` creating the
   binding, `WebNoteImageUploadService` refusing one).
3. The (key, value) identity is defined four times:
   `NotePropertyIndexService.PropertyValueKey`,
   `NotebookGitPropertyValueDating.ValueKey`, `NotePropertyIndexPlanner.DatedValue`
   (plus content), and `NotePropertyValueDateRebuildService.ValueAtPath` (plus path).
4. `NotePropertyIndexServiceTest.changeDate` (body edit, reorder, value removed,
   new value left undated), the reorder and scalar-edit cases of
   `NotePropertyIndexPlannerTest.changedValues_*`, and
   `NotebookGitPropertyValueDatingTest.datesAListItemRemovedAndReAddedAtItsReAddition`
   repeat `NotebookGitWebContentPropertyDatingControllerTest` and
   `NotebookGitPublicationPropertyDatingControllerTest`, or check a state no
   learner sees.

## Goal and scope

The story's Goal, Scope, scope limits and key examples are in the source
story and are not repeated. **Exclusions (from the story):** re-dating on a
move between notebooks; the cross-notebook undo-trash branch found during
refinement; binding every notebook.

## Preserved promises and constraints

All SEED-101#story-1 behavior and its accepted tests stay green, except the
redundant tests that slice 4 removes. The extraction's refusals and response
are unchanged. Failing loudly on a corrupt repository stays (ADR 0006).
Web-save cost guards (`NotebookGitWebContentSaveCostControllerTest`) and
publication cost guards stay green. Values of a notebook without a binding stay
undated and sort at note creation.

## Current decisions

- AI extraction wraps its construction in `acceptedWebChangeService.apply(notebookId, ...)`
  as reify does (both now in `WebNoteCreationService`): re-read the original note inside the
  operation after the binding lock, construct both notes there, build the
  `NoteRealm` after `apply` returns; one change, one commit, dated by the
  existing `commitIfChanged` → `dateChangedValues` for the inserted note and
  the updated original. The commit time is `testabilitySettings.getCurrentUTCTimestamp()`.
- The rebuild query selects only notebooks with a Git binding
  (`AND EXISTS (SELECT b FROM NotebookGitBinding b WHERE b.notebook = n.notebook)`);
  `rebuild` keeps `orElseThrow()` (the selected notebook is bound), and no
  branch dates unbound notebooks.
- `NotePropertyIndexPlanner` owns one value-key record (for example
  `PlannedRow.valueKey()`); the service's carry-over, the dater and the rebuild
  use it, and `ValueAtPath` wraps it with the path.
- Proof commands, in this worktree:
  `./scripts/run.sh pnpm backend:test:worktree --tests '<one class pattern>'`
  (one `--tests` per invocation; Gradle fails a filter that matches nothing).

## Decisive premises

| Premise | Consuming operation | Observation | Result |
| --- | --- | --- | --- |
| Wrapping `createNoteFromExtractedSuggestion` in `apply` keeps the existing extraction tests and the save-cost guards green. | Slice 1. | Scratch edit in this worktree (2026-11-04, reverted): `AiController.createExtractedNote` wrapped in `acceptedWebChangeService.apply(note.getNotebook().getId(), ..., realm -> "Extract note: " + title, testabilitySettings.getCurrentUTCTimestamp())`; `./scripts/run.sh pnpm backend:test:worktree --tests '*AiControllerCreateExtractedNoteTest'` BUILD SUCCESSFUL (2m 40s) and `--tests '*NotebookGitWebContentSaveCostControllerTest'` BUILD SUCCESSFUL (2m 7s). The guards measure `TextContentController.updateNoteContent` only. | Settled. |
| `apply` dates the inserted and updated notes of any caller at the appended commit. | Slice 1 relies on it instead of a second dating call. | `AcceptedWebChangeService.commitIfChanged` → `dateChangedValues` reads inserted and updated notes from `ProjectionChangeCapture` with no caller knowledge (read 2026-11-04); `NotebookGitWebContentPropertyDatingControllerTest` (7) proves it through `updateNoteContent` and stays green on trunk (story-1 acceptance). | Settled for the mechanism; slice 1's own test observes it for the extraction's two notes. |
| The rebuild query can join the notebook's Git binding. | Slice 2. | Scratch edit (2026-11-04, reverted): the `EXISTS` subquery above added to `findNotebookIdsWithUndatedValues`; `--tests '*NotePropertyValueDateRebuildServiceTest'` BUILD SUCCESSFUL (1m 27s), 6 tests, 0 failures. | Settled. |
| Test notebooks are unbound unless a test starts a history, so a one-bound-one-unbound fixture is cheap. | Slice 2 proof. | `NotebookBuilder` creates no binding; `NotePropertyValueDateRebuildServiceTest` calls `notebookGitHistoryService.startHistory` itself (read 2026-11-04). | Settled. |
| Each story-1 example has a surviving owner after slice 4's removals. | Slice 4. | Plan 003's Outside-in proof table at `aff3fe6ea556b0800f0a7cbc0c95f09eba144bd7` read against the current tests: examples 2, 3, 4 are owned by `NotebookGitWebContentPropertyDatingControllerTest`, 5 by `NotebookGitPublicationPropertyDatingControllerTest` and the dater's first test, 3's change set by `NotePropertyIndexPlannerTest.changedValues_reports_an_item_whose_text_changed_as_the_new_item` and `..._reports_every_value_of_a_frontmatter_that_had_none`. | Settled by reading; slice 4 records the final mapping. |

## Outside-in proof

| Story example | Owning slice | Proof |
| --- | --- | --- |
| 1. Extraction commits both notes and dates the values it adds | 1 | new test beside `NotebookGitWebContentPropertyDatingControllerTest` |
| 2. A refused extraction commits nothing | 1 | existing `AiControllerCreateExtractedNoteTest` refusal cases (note count and content unchanged) stay green |
| 3. Unbound notebook beside a bound one: the bound one is dated, the job finishes, the unbound one stays undated | 2 | `NotePropertyValueDateRebuildServiceTest` |
| 4. One value identity | 3 | the dating suites stay green with one record |
| 5. Story-1 examples keep a surviving test after removals | 4 | the mapping above, recorded in slice 4 |

## Slices

### 1. AI note extraction commits its change and dates the values it adds

Type: Behavior
Status: done
Proof: a `NotebookGitWebContentControllerTestBase` subclass (beside
`NotebookGitWebContentPropertyDatingControllerTest`): on a Git-backed note,
`createExtractedNote` with an original rewrite that adds `example of` and new
content with `origin` → the accepted tip commit's parent is the previous head,
the tip tree holds both note files, and both rows carry `changed_at` equal to
the save time and that commit's committer time; an existing dated `topic` row
keeps its date. `AiControllerCreateExtractedNoteTest` and
`NotebookGitWebContentSaveCostControllerTest` stay green.

Behavior: an owner creates the extracted note from the refinement preview → the
change is applied as one accepted web change → one commit holds the rewritten
original and the new note, and the values that change added or changed are
dated at it.

Accepted (2026-11-04): `WebNoteCreationService.createExtractedNote` wraps the
construction in `apply`; the post-change refactor moved reify into the same
service (`reifyProperty`, shared private `createFrom`) and deleted
`PropertyReifyService`, because the two wrappers were the same shape.
`AiController.createExtractedNote` no longer carries `@Transactional`; `apply`
owns the transaction. Proof, each `./scripts/run.sh pnpm backend:test:worktree --tests`:
`'*NotebookGitWebContentExtractedNoteDatingControllerTest'` (new; one commit on
the previous head, tip holds `Atom.md` and `Electron.md`, committer time and
`changed_at` of `example of` and `origin` at the save time, `topic` keeps its
date), `'*AiControllerCreateExtractedNoteTest'` (7, refusals),
`'*NotebookGitWebContentSaveCostControllerTest'` (8),
`'*DisplayNameNormalizationMvcTest'` (11), `'*NoteReifyPropertyTests'`,
`'*NoteReifyPropertyRefusalTests'`, `'*NotebookNoteCreateControllerTest'`.
Learning: `WebNoteEditService.requireNote` is package-private, so wrappers that
re-read a note inside an accepted web change live in `services.notebookGit`.

### 2. Rebuild leaves out notebooks without a Git binding

Type: Behavior
Status: done
Proof: `NotePropertyValueDateRebuildServiceTest`: a notebook with undated rows
and no binding beside the fixture's bound notebook → `rebuildOne()` dates the
bound notebook, the next call returns false, and the unbound notebook's rows
stay undated.

Behavior: undated rows exist in a notebook with no binding → the job runs →
bound notebooks are dated and the job finishes without failing on the unbound
one.

Accepted (2026-11-04): the query, renamed
`findGitBoundNotebookIdsWithUndatedValues` by the refactor, adds the `EXISTS`
binding condition; `rebuildOne`'s javadoc names Git-bound notebooks. Proof:
`./scripts/run.sh pnpm backend:test:worktree --tests '*NotePropertyValueDateRebuildServiceTest'`
(7; new `aNotebookWithoutGitHistoryIsLeftUndatedWhileTheOthersAreDated`, seen
failing with `NoSuchElementException` before the fix) and
`--tests '*NotePropertyValueDateRebuildJobTests'` (2).

### 3. One property value identity

Type: Structure
Status: done
Proof: `'*NotePropertyIndex*'`, `'*NotebookGitPropertyValueDating*'`,
`'*NotePropertyValueDateRebuild*'`, `'*NotebookGitWebContentPropertyDating*'`,
`'*NotebookGitPublicationPropertyDating*'` stay green; one value-key record
remains (`PropertyValueKey`, `ValueKey` and the key half of `DatedValue` and
`ValueAtPath` are gone).

Internal change: the planner's value key replaces the four records. Enables
nothing further; it is the directly owned correction of finding 3.

Accepted (2026-11-04): `NotePropertyIndexPlanner.ValueKey(propertyKey, propertyValue)`
is the one record; `PlannedRow.valueKey()` and `NotePropertyIndex.valueKey()`
build it, `changedValues` returns `Set<ValueKey>`, `writeValueDate` takes it,
and the dater's `ValueDate` holds it (a fifth repetition, folded in). Content
stays only in the planner's private `DatedValue(ValueKey, content)` change
comparison. Proof, each `./scripts/run.sh pnpm backend:test:worktree --tests`:
`'*NotePropertyIndex*'` (32), `'*NotebookGitPropertyValueDating*'` (4),
`'*NotePropertyValueDateRebuild*'` (10), `'*NotebookGitWebContentPropertyDating*'` (7),
`'*NotebookGitPublicationPropertyDating*'` (5),
`'*NotebookGitWebContentExtractedNoteDating*'` (1), `'*AssimilationServiceProperty*'` (21).

### 4. Remove repeated dating tests

Type: Structure
Status: done
Proof: the cases in finding 4 are deleted; each story-1 example stays mapped
to a surviving test (the mapping in Decisive premises, re-read against the
tests after the deletion and recorded here as a reading); `'*NotePropertyIndex*'`
and `'*NotebookGitPropertyValueDating*'` stay green.

Internal change: delete `NotePropertyIndexServiceTest.changeDate`, the
reorder and scalar-edit `changedValues_*` cases, and the dater's re-add
test, with any helper only they used. Directly owned correction of finding 4.

Accepted (2026-11-04): deleted as listed. The deleted reorder case also held
the only reformat observation, so a reformat-only
`changedValues_ignores_reformatting_of_the_same_parsed_values` (same order,
quoting and list style changed) stays. Mapping reading after deletion: example
2 `NotebookGitWebContentPropertyDatingControllerTest.aBodyOnlySaveKeepsTheValueDate`;
3 `reorderingAListKeepsItsItemDatesAndDatesAnAddedItem` plus the planner's
`changedValues_reports_an_item_whose_text_changed_as_the_new_item` and
`..._reports_every_value_of_a_frontmatter_that_had_none`; 4
`editingAScalarRedatesIt`; 5
`NotebookGitPublicationPropertyDatingControllerTest.eachValueIsDatedAtTheCommitThatLastChangedIt`
and the dater's `datesEachValueAtTheCommitThatAddedItAndLeavesUnchangedValuesUndated`.
Sweep reading: `git grep` outside `.planning/` for the deleted names finds
nothing. Proof: `--tests '*NotePropertyIndex*'` (26 before the reformat case),
`'*NotePropertyIndexPlannerTest'` (10), `'*NotebookGitPropertyValueDating*'` (3).

## Concern evidence

Slice-plan refinement was not needed: four independent commit-sized slices,
each with its own focused proof, and the decisive premises were observed in
this worktree. Slice 1's proof is a Git-backed controller test, so it may
exceed the five-minute target; splitting it would separate the wrap from the
only observation that evaluates it. No remaining slice-specific concern was
identified.


## Story obligations

### O1. Move between notebooks re-dating deferred
Reported: slice 1 — "Source review lists the move-between-notebooks dating as a deferred scope limit"
Story clause: "How a note moved to another notebook keeps or loses its values' dates"
Disposition: excluded "How a note moved to another notebook keeps or loses its values' dates"
Commitment: X1


### O2. Cross-notebook undo-trash outside accepted web change deferred
Reported: slice 1 — "Source review lists the cross-notebook undo-trash branch as a deferred scope limit"
Story clause: "Undoing a trashed note whose prior folder now lies in another notebook"
Disposition: excluded "Undoing a trashed note whose prior folder now lies in another notebook"
Commitment: X2, X2.1

### O3. Binding every notebook deferred
Reported: slice 1 — "Source review lists binding every notebook as a deferred scope limit"
Story clause: "Binding every notebook so that the rebuild has none to leave out"
Disposition: excluded "Binding every notebook so that the rebuild has none to leave out"
Commitment: X3