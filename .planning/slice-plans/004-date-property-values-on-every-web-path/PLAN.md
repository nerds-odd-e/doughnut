# Plan: Date property values on every web path and keep one value identity

**Source:** [SEED-101#story-2](../../seeds/SEED-101-property-assimilation-order.md#story-2)
**Identity:** SEED-101#story-2

## Provenance and findings

Correction from the execution retrospective of SEED-101#story-1 (plan
[003](../003-order-property-assimilation-by-change-time/PLAN.md), commits
`44fb2233e0`..`0beb46cd03`, reviewed 2026-11-04 at `0beb46cd03`). Current
findings:

1. `AiController.createExtractedNote` → `NoteConstructionService.createNoteFromExtractedSuggestion`
   rewrites the original note and creates a new one outside
   `AcceptedWebChangeService.apply`, so no commit is appended and no value is
   dated. A value added this way sorts at its note's creation, and the rebuild
   can only date it at creation (no history). `PropertyReifyService` shows the
   wrapping pattern. The missing commit is an older ADR 0002 gap that story-1's
   dating depends on.
2. `NotePropertyIndexRepository.findNotebookIdsWithUndatedValues` has no
   ordering and selects notebooks without a binding;
   `NotePropertyValueDateRebuildService.rebuild` throws on a missing binding, so
   the same notebook can fail every run and starve the rest.
3. The (key, value) identity is defined four times:
   `NotePropertyIndexService.PropertyValueKey`,
   `NotebookGitPropertyValueDating.ValueKey`, `NotePropertyIndexPlanner.DatedValue`
   (plus content), and `NotePropertyValueDateRebuildService.ValueAtPath` (plus path).
4. `NotePropertyIndexServiceTest.changeDate` (body edit, reorder, new value left
   undated), the reorder and scalar-edit cases of
   `NotePropertyIndexPlannerTest.changedValues_*`, and the re-add case of
   `NotebookGitPropertyValueDatingTest` repeat
   `NotebookGitWebContentPropertyDatingControllerTest` and
   `NotebookGitPublicationPropertyDatingControllerTest`, or check a state no
   learner sees.

## Preserved promises and constraints

All SEED-101#story-1 behavior and its accepted tests stay green, except the
redundant tests that slice 4 removes. Failing loudly on a corrupt repository
stays (ADR 0006). Web-save cost guards (`NotebookGitWebContentSaveCostControllerTest`)
and publication cost guards stay green. Out of scope: re-dating on a move
between notebooks.

## Current decisions

- AI extraction wraps its construction in `acceptedWebChangeService.apply(notebookId, ...)`
  as `PropertyReifyService` does; both notes are in one change and one commit.
- The rebuild query joins the notebook's Git binding.
- `NotePropertyIndexPlanner` owns one value-key record (for example
  `PlannedRow.valueKey()`); the others use it, and `ValueAtPath` wraps it with
  the path.

## Slices

### 1. AI note extraction dates the values it changes

Type: Behavior
Status: planned
Proof: `AiControllerCreateExtractedNoteTest` (or a sibling under
`NotebookGitWebContentControllerTestBase`): extracting adds `topic` to the
original note and creates the new note with `origin`; both rows carry the save
time, which equals the accepted tip commit's committer time, and the tip commit
contains both notes. The existing extraction tests stay green.

### 2. Rebuild skips notebooks it cannot rebuild

Type: Behavior
Status: planned
Proof: `NotePropertyValueDateRebuildServiceTest`: a notebook with undated rows
and no binding beside a bound one → `rebuildOne()` dates the bound notebook and
the next call returns false.

### 3. One property value identity

Type: Structure
Status: planned
Proof: `'*NotePropertyIndex*'`, `'*NotebookGitPropertyValueDating*'`,
`'*NotePropertyValueDateRebuild*'`, `'*NotebookGitWebContentPropertyDating*'`,
`'*NotebookGitPublicationPropertyDating*'` stay green; one value-key record
remains.

### 4. Remove repeated dating tests

Type: Structure
Status: planned
Proof: the removed cases are listed in finding 4; each story-1 example stays
mapped to a surviving test (plan 003's Outside-in proof table, read against the
current tests); `'*NotePropertyIndex*'` and `'*NotebookGitPropertyValueDating*'`
stay green.

## Concern evidence

Slice-plan refinement was not needed: four independent commit-sized slices,
each with its own focused proof. No remaining slice-specific concern was
identified.
