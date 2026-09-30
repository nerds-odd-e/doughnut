# Reify a property into a relationship note

**Identity:** SEED-062#story-1
**Source:** [story](../../seeds/SEED-062-reify-property.md#story-1), refined 2026-09-30 from the owner's answers: the
relationship note takes source = the note holding the property, relation = the property key, target = the note the
wiki-link value names; it is created in the source note's notebook and folder; the property is removed from the source;
trackers follow the relationship note; a non-wiki-link value cannot be reified and the author is told why.

## Goal and scope

A note author turns one property whose value is a wiki link to a note into a relationship note, keeping every learner's
trackers of that property.

Included: one backend operation for one property of one note; a Reify action on the property row; a stated reason when the
value cannot be reified; trackers (all learners) re-pointed to the new note with history and schedule untouched.

Excluded (story): bulk conversion; reversing (the existing reduce-to-source-property already does the reverse for
relationship notes); list-valued properties (only a scalar value that is one whole wiki link); a wiki link that names no
existing note. No Accepted ADR is contradicted: ADR 0001 defines Relationship note and Property memory tracker, ADR 0004
the stored Markdown shape.

## Existing solutions (PFE)

The reverse operation exists and is mirrored, not rebuilt: `RelationController` `POST /{relationNote}/reduce-to-source-property`
-> `RelationReduceService` (one `acceptedWebChangeService.apply` around the change, returns a `NoteRealm`), and
`NoteReferenceHandling.rehomeNoteLevelMemoryTrackerToSourceProperty` (sets tracker note and key, `entityPersister.merge`).
Reified relationship Markdown is written by the frontend today (`relationshipNoteCompose.ts`, `AddRelationshipFinalize.vue`);
the backend has only the test fixture `RelationshipNoteMarkdown` and the parser `NoteReferenceHandling.parseRelationshipFrontmatter`,
so the backend needs a small Markdown writer for the same shape (`type: Relationship`, kebab `relation`, `source`, `target`).
Same-folder creation exists (`NoteCreationDTO.folderId`, `WebNoteCreationService`). Tests to reuse:
`RelationControllerReduceToSourcePropertyTests`, e2e scenario "Tracked relationship reduced keeps property memory tracker
on source" in `relationship_edit_and_remove.feature`.

## Decisive premises

| Premise | Operation that consumes it | Observation | Result |
| --- | --- | --- | --- |
| No helper removes one top-level frontmatter key from a note's content | slice 1 | `rg -n "remove|public static" backend/src/main/java/com/odde/donut/algorithms/FrontmatterInPlaceEdit.java backend/src/main/java/com/odde/donut/algorithms/NoteContentMarkdown.java` | observed: `NoteContentMarkdown` (in `algorithms/`) has only image, wikidata, link-removal, add and set helpers; `FrontmatterInPlaceEdit` removes an entry only when a rewrite empties it. No key-removal helper, so slice 1 stands |
| A property tracker is `note_id` + `property_key`; a note-level tracker has an empty key; the unique key is (user, note, type, key, active) | slice 4 | read `MemoryTracker.java`, `V100000000__baseline.sql` | confirmed by reading; slice 4's spec observes it |
| Recall logs, prompts and batch requests reference the tracker id, so history follows a re-pointed tracker | slice 4 | read `RecallLog`, `RecallPrompt` foreign keys; slice 4 asserts the log count is unchanged | confirmed by reading |
| `assimilation_sequence_skip` rows (user, note, property_key) hold no learning state, only "skip assimilating this property" | slice 4 | read `AssimilationSequenceSkip.java` (fields, queries) | observed: a skip only filters the assimilation sequence for a note/key; it is not a tracker. Decision: leave skip rows alone |
| Removing the property from content does not delete trackers | slice 4 | content edit only; cascades are `ON DELETE` of the note row | shown by slice 4's spec (tracker exists after the call); this is the consuming operation, so it is the first red/green check of that slice |
| The property row's per-row actions live in `RichFrontmatterPropertyPanel.vue` next to remove (`rich-note-property-row-remove`) | slice 6 | read the panel and row | confirmed by the agent report; recheck when slice 6 starts, because SEED-064#story-2 and story-5 rename and edit the row |

## Outside-in proof

| Promise | Owner | Observable proof |
| --- | --- | --- |
| Reifying `related: "[[Other]]"` on note `Src` creates a Relationship note with source `[[Src]]`, relation `related`, target `[[Other]]` in Src's notebook and folder, and Src no longer has the property | slices 1, 2 | controller test `NoteReifyPropertyTests` (extends `ControllerTestBase`) |
| A plain-text value, a missing key, or a link to no existing note is refused with a message and changes nothing | slice 3 | same test class, message asserted |
| Every learner's tracker of the property is now a tracker of the new note with unchanged stability, next recall and recall logs | slice 4 | same test class, two learners, one with a recall log |
| The author sees a Reify action for a linkable value, and the reason when the value cannot be reified | slices 6, 7 | frontend component spec on the property row |
| The whole journey in the app: reify a tracked property, the relationship note opens, the tracker is still due | slice 8 | `e2e_test/features/relationships/reify_property.feature`, run once |

## Ordered slices

### 1. One frontmatter property can be removed by key
Type: Structure
Status: done — `NoteContentMarkdown.removeFrontmatterProperty` (via `FrontmatterInPlaceEdit.removeTopLevelEntry`);
proof `NoteContentMarkdownTest.removeFrontmatterProperty_*` plus `*Frontmatter*`/`*WikiLink*` green
Proof: unit test on the new removal (`NoteContentMarkdown` level), removing the last property drops the block, other
lines untouched (`CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test --tests '*NoteContentMarkdown*' -Dspring.profiles.active=test`).

Add key removal beside `FrontmatterInPlaceEdit.rewriteSupportedValues` (reuse `entryRemoval`) and expose it on
`NoteContentMarkdown`. Enables slice 2. First run the premise-table search; if a helper exists, delete this slice.

### 2. Reifying a wiki-link property creates the relationship note and removes the property
Type: Behavior
Status: done — `POST /api/notes/{note}/reify-property?propertyKey=` → `PropertyReifyService` →
`NoteConstructionService.reifyPropertyIntoRelationshipNote`; writer `RelationshipNoteComposition` (fixture now uses it);
proof `NoteReifyPropertyTests` + `RelationControllerReduceToSourcePropertyTests` green
Proof: `NoteReifyPropertyTests` (new), red then green; `RelationControllerReduceToSourcePropertyTests` green.

Behavior: note `Src` in folder `F` with `related: "[[Other]]"`, no trackers → `POST /api/notes/{src}/reify-property` with
the key → response is the new note's realm; the new note is in Src's notebook and folder `F`, has `type: Relationship`,
`relation: related`, `source: "[[Src]]"`, `target: "[[Other]]"`; Src's content no longer has `related`. One accepted web
change. Add `RelationshipNoteMarkdown`-shaped writer in main code (kebab relation) and the service mirroring
`RelationReduceService`. Title follows the frontend rule "source relation-label target".
If this runs past about 10 minutes, split at "note created in the same notebook and folder" and "property removed from Src".

### 3. A value that cannot be reified is refused with the reason
Type: Behavior
Status: done — 400 with reason before any write (missing key; not one whole link, incl. list and text around a link;
unresolved link); proof `NoteReifyPropertyTests.ValueThatCannotBeReified` green
Proof: same test class.

Behavior: value `training` (plain text), a list value, a key that is not on the note, or `[[Nowhere]]` resolving to no
note → 400 with a message naming the cause; the note is unchanged and no note is created.

### 4. Every learner's trackers follow the relationship note
Type: Behavior
Status: done — `RelationshipMemoryTrackerRehoming` holds both directions (reify forward, reduce reverse); proof
`NoteReifyPropertyTests.TrackedProperty` + `RelationControllerReduceToSourcePropertyTests` green
Proof: same test class, two learners, one tracker with a recall log.

Behavior: learners A and B each have a property tracker for `related` on Src → reify → both trackers now belong to the new
note as note-level understanding trackers (empty key) with the same user, stability, difficulty, last and next recall,
and A's recall log still points to it; none remain on Src. Re-point before any change to Src's content. Assimilation skip rows are left alone (premise table). A learner with no
tracker gets none.

### 5. The frontend API client knows the operation
Type: Structure
Status: done — absorbed into slice 2's delivery (the new endpoint made `RobotsTests.openApiDocsMatchCommittedYaml`
fail, so the client was regenerated there to keep the increment CI-safe)
Proof: generate-api-client skill; frontend type check passes; `pnpm lint:all` OpenAPI validation as that skill states.

Regenerate the client after slice 2's controller signature. Enables slice 6.

### 6. A property row with a wiki-link value offers Reify
Type: Behavior
Status: done — Reify button in `RichFrontmatterPropertyPanel.vue` (`reifiable` from `isWellFormedWholeWikiLinkItem`),
`noteStore.reifyProperty` → `focusNoteRealm`; proof `RichMarkdownEditor.propertyReify.spec.ts` + vue-tsc green
Proof: new component spec near `RichMarkdownEditor.propertyEntry.spec.ts` conventions, red then green.

Behavior: an editable row whose scalar value is one whole wiki link, panel open → a Reify button is shown; clicking it
calls the client with note id and key and the app shows the new relationship note (reuse the reduce flow's navigation
after `reduce-to-source-property`). Add a whole-link check by trimming to `^\[\[[^\]]+\]\]$`, using existing
`wikiLinkMarkup.ts`. No button in the read-only view.

### 7. The row says why a value cannot be reified
Type: Behavior
Status: planned
Proof: same spec.

Behavior: panel open on a plain-text or list value → Reify is disabled and a short reason is visible ("Only a property
whose value is a link to a note can be reified"); nothing is sent.

### 8. Reify works end to end, trackers included
Type: Behavior
Status: planned
Proof: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/relationships/reify_property.feature` once.

Behavior: a tracked property `related: [[Other]]` → the author reifies it in the app → the relationship note opens and the
learner's recall of it is still due. Mirror the step definitions of "Tracked relationship reduced keeps property memory
tracker on source".

## Current decisions

- The operation is one server endpoint on the note, mirroring reduce; the frontend does not compose Markdown for it.
- The key names the property; a key appearing more than once reifies the first entry (SEED-063 owns repeated keys).
- A tracker becomes note-level on the relationship note; its property key is cleared.
- Order of writing inside the accepted change: create note, re-point trackers, remove property.
- Slice 6 starts from the row as it is on trunk then; if SEED-064#story-2 or story-5 are still in flight, stop after slice 5
  and report.
- 2026-09-30: execution stopped after slices 1–5 because SEED-064#story-5 is still Taken on trunk (its plan
  `slice-plans/005-note-property-read-only-row/` has no done slice). Resume at slice 6 once story-5 is delivered.
- Story-5 landed on main (merged into this branch at 93eb7fd3); execution resumed at slice 6.

## Learnings

- `entryRemoval` now cuts through the line holding the value node's last character, so block-list values are removed
  with their key; key lookup is shared with `setTopLevelScalar`.
- Any new endpoint must regenerate the API client in the same delivery (`RobotsTests` compares the OpenAPI yaml).
- Saving authored content adds `type: Note` to the source's frontmatter; assert absence of the key, not exact content.
- Slice 3's refusals: today a missing key or unresolved link fails via bare `orElseThrow()` in
  `NoteConstructionService.reifyPropertyIntoRelationshipNote`; `WikiLinkResolver.resolveFirstWikiLink` is shared with reduce.
- Reify moves every tracker type of the key (understanding, spelling, …) to note level, matched case-insensitively;
  reduce moves back only note-level understanding trackers and drops the rest with the note. Asymmetric by design of
  each direction; revisit only if the owner wants round-trips to keep spelling trackers.
- `MemoryTrackerBuilder.recallCount(n)` creates n recall logs.
