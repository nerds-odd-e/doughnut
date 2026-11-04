---
id: SEED-101
status: dormant
planted: 2026-11-04
planted_during: discussion of Git-derived property timestamps for assimilation
trigger_when: improving the order in which learners assimilate note properties
scope: medium
---

# SEED-101: Use property change times in assimilation

## Story Decomposition

<a id="story-2"></a>

### Date property values on every web path and keep one value identity

**Identity:** SEED-101#story-2
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/004-date-property-values-on-every-web-path/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"d158ab8c5e3a9d209a814dcb3350b4bbe4c61bb24dd0b7b1e1bc243ad303854f","plan":"a36b8473aa64cb30beab107bcdb2ecb5ea425de5414946cb6c6e0e8a4a535b6e"}}
```

Correction from the execution retrospective of
SEED-101#story-1 (commits `44fb2233e0`..`0beb46cd03`; story and plan at `aff3fe6ea556b0800f0a7cbc0c95f09eba144bd7` in `.planning/seeds/SEED-101-property-assimilation-order.md#story-1` and `.planning/slice-plans/003-order-property-assimilation-by-change-time/PLAN.md`); the plan is
[004](../slice-plans/004-date-property-values-on-every-web-path/PLAN.md).

#### Goal

A learner who adds or changes property values through AI note extraction meets
those values in the assimilation sequence at the time of that change, as they
already do for other web saves, because the extraction is an accepted web
change: the rewritten original note and the new note enter the notebook's Git
history in one commit, and the values that commit adds or changes are dated at
it. The maintenance job keeps dating the remaining notebooks when one notebook
has no Git history to rebuild from. One value identity serves the index
refresh, the history dater and the rebuild.

#### Scope

Required behavior:

- AI "create extracted note" commits its change through the accepted web
  change, the rewrite of the original note and the new note in one change and
  one commit, so the values it adds or changes are dated at that commit and
  both notes appear in Git history. Its refusals (reserved title, name already
  used in the folder, invalid aliases) and its response are unchanged.
- The rebuild selects only notebooks with a Git binding, so a notebook without
  one cannot stall the job for the others. Values in a notebook without a
  binding stay undated and keep sorting at their note's creation time, which is
  today's order and the order a rebuild of an empty history would give them.
- The (key, value) identity of a property value has one definition, used by the
  refresh carry-over, the dater and the rebuild.
- Tests that repeat the controller-level dating tests or check an index state
  no learner sees are removed; every story-1 key example stays mapped to a
  surviving test.

Scope limits (deferred, not rejected):

- How a note moved to another notebook keeps or loses its values' dates
  (awaiting an owner decision, from story-1's product advice).
- Undoing a trashed note whose prior folder now lies in another notebook
  places the note outside the accepted web change
  (`NoteController.undoTrashNote`, cross-notebook branch). Found during this
  refinement; not part of the reviewed correction. The owner decides whether
  to queue it.
- Binding every notebook so that the rebuild has none to leave out. A notebook
  without a binding is a state the product supports (a history reset creates
  the binding; image upload refuses).

#### Key examples

1. A Git-backed notebook; note A created 2024-03 holds untracked
   `topic: physics` dated 2024-05; note B was created 2025-06 → today the
   owner extracts part of A into new note C through the AI refinement panel,
   and the accepted rewrite of A adds `example of: "[[C]]"` → the notebook's
   accepted history gains one commit containing A's rewrite and C's file; A's
   `example of` and C's values carry that commit's time (today), `topic` keeps
   2024-05; in the sequence B's note unit is offered before A's `example of`.
   (Today: no commit; A's `example of` sorts at A's creation, before B.)
2. The extraction is refused (reserved title, a name already used in the
   folder, invalid aliases) → no commit, no note changed, the same error as
   today.
3. Notebook N has no Git binding and undated rows; notebook M is Git-backed
   with undated rows → the job runs → one call dates M; the next call finds
   nothing to do; N's values keep sorting at their notes' creation and the job
   never fails on N.
4. The same (key, value), a scalar by key with its content and a list item by
   key and text, is the one identity the refresh carry-over, the dater and the
   rebuild agree on: a list item `"[[C]]"` carried across a save, dated from a
   push and matched by the rebuild is one value.
5. After the repeated tests are removed, story-1's "body edit keeps the date"
   is proven by the web content dating controller test alone; "reorder keeps
   dates, added item dated now" by that test and the planner's change-set
   test; "remove then re-add in one push dates at the re-add" by the
   publication dating controller test.

#### UI

No new interaction: the owner opens Refine on a note page, previews the
extraction, edits it if they wish, and clicks Create; the modal closes as
today. A conflict with a concurrent change to the same notebook shows the
existing create error in the preview and the owner clicks Create again, as for
every web save. No interaction choice with architectural consequences was
found.

#### Challenge and investigation

- Strongest doubt: the goal named dating, but the capability is that the
  extraction is an accepted web change; dating is one consequence and the
  missing commit (the notebook's history, and a CLI pull, missing the
  extracted note) is the larger one. The goal now names both; the scope
  already required the commit.
- Doubt on the rebuild: dating an unbound notebook's values at note creation
  instead of leaving the notebook out would leave nothing undated, but the
  learner sees the same order either way (undated sorts at creation) and it
  adds a second rule to the rebuild; the one-condition selection stays.
- Facts (checked 2026-11-04 in this checkout): `AiController.createExtractedNote`
  calls `NoteConstructionService.createNoteFromExtractedSuggestion` inside
  `@Transactional` only; every other note-writing web path (`WebNoteEditService.edit`,
  `WebNoteCreationService`, `PropertyReifyService`, `RelationReduceService`,
  move, trash, same-notebook undo, folder and attachment services) runs inside
  `AcceptedWebChangeService.apply`, whose `commitIfChanged` dates inserted and
  updated notes for any caller, and `apply` refuses (409) a change to a bound
  notebook it did not lock only for callers inside it; every notebook created
  by `NotebookService.createNotebookForOwnership` is bound (`startHistory`),
  `resetHistory` binds an unbound notebook, `WebNoteImageUploadService` refuses
  an unbound notebook, and `makeMe` notebooks are unbound;
  `findNotebookIdsWithUndatedValues` has no binding condition and `rebuild`
  throws on a missing binding; the four identity records are
  `NotePropertyIndexService.PropertyValueKey`,
  `NotebookGitPropertyValueDating.ValueKey`, `NotePropertyIndexPlanner.DatedValue`
  (plus content) and `NotePropertyValueDateRebuildService.ValueAtPath` (plus
  path); the save-cost guards measure `TextContentController.updateNoteContent`
  only.
- Observed (2026-11-04, scratch edits reverted): wrapping the extraction in
  `apply` as `PropertyReifyService` does keeps `AiControllerCreateExtractedNoteTest`
  and `NotebookGitWebContentSaveCostControllerTest` green; adding
  `AND EXISTS (SELECT b FROM NotebookGitBinding b WHERE b.notebook = n.notebook)`
  to the undated-notebook query keeps `NotePropertyValueDateRebuildServiceTest`
  (6 tests) green.
- Open, not blocking: whether production holds notebooks without a binding is
  a production fact not observable here; the selection rule is justified by
  the supported unbound state either way.
- Architecture: no new concern. [ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
  already requires every web edit to go through Git publication; the first
  slice brings the extraction under it. [ADR 0006](../../docs/adrs/0006-failure-handling-accepted.md):
  the rebuild's loud failure stays legitimate, but repeating on the same
  notebook every run starves the others, so the selection prevents that state
  rather than catching it; no catch is added.
