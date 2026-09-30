# Show the title choice when a typed Wikidata ID is saved and the search found nothing

**Identity:** SEED-064#story-4
**Source:** [story](../../seeds/SEED-064-note-properties-fixes.md#story-4), refined 2026-09-30 from the owner's answers:
typing an ID is an intended way to fill the property; a second Save is acceptable as long as the meaning is clear; no
new message, no Cypress scenario; the stale-ID hole is left out.

## Goal and scope

A user who types an ID in a property's Wikidata dialog, when the title search found nothing, sees the "Replace title" /
"Add as alias" choice after the first Save instead of an unchanged dialog.

Included: in the property dialog (`showSaveButton`), Save on a typed ID whose English label differs from the note title
shows the choice and keeps the dialog open, whether or not the search list is empty; "No Wikidata entries found" is not
shown beside it; a second Save without choosing applies the ID and closes, title unchanged (today's behavior, pinned).

Excluded (story decisions): new message text or ID rules; whether saving a property should offer to rename the note;
"No Wikidata entries found" wording; layout and keyboard; a Cypress scenario; the stale-ID hole (editing the ID while the
choice shows applies the new ID unchecked). No stored-Markdown behavior changes (ADR 0004 unaffected). No Accepted ADR
or North Star topic governs this, so it is an ordinary plan with no new direction.

## Existing solutions (PFE)

Nothing new is built. The choice panel, `showTitleOptionsForEntity`, `handleWikidataSave` and the checks in
`useWikidataPropertyDialog.ts` already exist; only the order of the template branches in
`WikidataAssociationDialogBody.vue` changes. Test reuse: `useWikidataAssociationDialogTestLifecycle`-style SDK mocks
(`mockSdkService`), `makeMe.aWikidataEntity.wikidataTitle`, `mountEditorOnNoteShow`
(`propertyValueDialogTestDom.ts`) and `expectReplaceTitleAndAddAliasControls`
(`wikidataAssociationDialogTestSupport.ts`).

## Decisive premises

| Premise | Operation that consumes it | Observation | Result |
| --- | --- | --- | --- |
| The report is caused by the choice being hidden by the empty-results branch, not by the check failing | slice 1's fix | throwaway component spec through the editor: a `wikidata_id` row, note title "Snake", search stub `[]`, entity label "Douglas Adams", type `Q42` in the dialog, click Save twice (`CURSOR_DEV=true nix develop -c pnpm -C frontend exec vitest run tests/components/form/tmpProbe.spec.ts`, deleted afterwards) | first Save: dialog open, text "No Wikidata entries" present, no "Add as alias"; second Save: dialog closed, `update:modelValue` emitted with `wikidata_id: Q42` |
| The second Save keeps the title and applies the ID (today's behavior the owner accepts) | slice 1's "second Save" example | same probe | confirmed: only the ID row changed, no title update |
| The dialog's other user, the note-creation form, cannot reach this state | slice 1's "nothing else changes" | read `NoteNewForm.vue` and the body: it uses `WikidataSearchByLabel` with no save button; `showTitleOptions` is only set after choosing a search result, so results are non-empty there | confirmed by reading; its spec `NoteNewForm.spec.ts` runs unchanged in slice 1 |
| Component tests run in Chromium and the named specs are green now | slice 1's proof | the probe run above (4.5 s, Chromium); note `console.log` fails a test | confirmed |

## Outside-in proof

| Promise | Owner | Observable proof |
| --- | --- | --- |
| Typed ID, empty search, label differs → Save shows the choice, dialog stays open, no "No Wikidata entries" text | slice 1 | component test in a new `RichMarkdownEditor.propertyWikidataDialog.spec.ts`, red before the change and green after |
| Second Save without choosing applies the ID and closes; the title is not touched | slice 1 | same spec: `update:modelValue` carries `wikidata_id: Q42`, dialog gone |
| Label equal to the title (any case) → saved and closed at once, no choice | slice 1 | same spec, green before and after |
| Nothing else in the dialog changed | slice 1 | `tests/notes/WikidataAssociationDialog.*.spec.ts`, `NoteNewForm.spec.ts` and `RichMarkdownEditor.propertyEntry.spec.ts` green with no assertion changed |
| Real iPad check | owner, optional, after slice 1 | manual: a note whose title finds no Wikidata entries, type `Q42`, Save, choice visible |

The invalid-ID message and the choose-a-result path are existing behavior owned by other specs, and the reorder does not
touch them, so they get no new test.

## Ordered slices

Size target is about 10 minutes including proof.

### 1. A typed ID with an empty search shows the title choice on the first Save
Type: Behavior
Status: planned
Proof: the new spec above (`CURSOR_DEV=true nix develop -c pnpm -C frontend exec vitest run
tests/components/form/RichMarkdownEditor.propertyWikidataDialog.spec.ts`), red before the change and green after; then
the existing specs of "Nothing else in the dialog changed", run once.

Behavior: a note titled "Snake" with a Wikidata property, search returns nothing → the user types `Q42` (label "Douglas
Adams") and taps Save → the "Replace title" and "Add as alias" choice shows and the dialog stays open; Save again →
`Q42` is saved, the dialog closes, the title is unchanged; an ID whose label equals the title saves and closes at once.

Change: in `WikidataAssociationDialogBody.vue`, render the title-choice branch (`showTitleOptions`) before the
"No Wikidata entries found" branch and drop the now redundant `&& !showTitleOptions` from the results branch. If the
first red run does not fail on the missing choice, stop and re-read the flow before changing anything.

## Current decisions

- The fix is the template branch order, not new state: `showTitleOptions` is already set when Save finds a differing
  label.
- The proof goes through the editor (real composable, real dialog) with the SDK stubbed, so the report's path is what
  is tested.
- Tests do not call `console.log`: it fails the run in this project's test setup.

## Learnings

_None yet._

## Real iPad check (owner, optional)

After slice 1, on a note whose title finds no Wikidata entries, type `Q42` in the property's Wikidata dialog, tap Save,
and confirm the choice is visible.
