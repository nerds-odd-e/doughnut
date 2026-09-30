# Track each value of a list property separately

**Identity:** SEED-063#story-1
**Source:** [story](../../seeds/SEED-063-track-property-values-separately.md#story-1), refined 2026-09-30 from the
owner's answers: a value is identified by its key and the value itself (not its position); no tracker exists today on
a list-valued property; converting existing numbered keys is a separate queued story (SEED-063#story-2); one rule for
every list property, no per-key choice.

## Goal and scope

A learner can assimilate and recall each value of a list property (`example of: ["[[run]]", "[[past tense]]"]`) on its
own, and reducing a relationship note into a key the source already has adds a value instead of creating `key 2`.

Included: one tracker per list value, identified by (key, value text); one assimilation unit per list value; a value's
recall question and recall screen name that value; the property panel offers Assimilate per value; renaming or removing
a key carries all its value trackers; a tracked single value that becomes a list keeps its tracker on that value; the
reduce flow appends to an existing key; ADR 0001's "Property memory tracker" entry amended in place.

Excluded (story): converting existing numbered keys (story 2); skipping one value (a skip stays per key); a per-key
choice between whole-list and per-value tracking; reifying a list value; new list-editing controls; the add-property
row's rule for an existing key (SEED-064#story-6); keeping a tracker when its value text changes, including a link
rewritten by a linked note's rename.

## Architecture

- **Accepted ADRs.** ADR 0001 defines "Property memory tracker — Understanding memory tracker keyed by a property name".
  The owner's decision refines that term; ADR 0001 says to amend terms in place, so slice 2 amends it to "keyed by a
  property name and, for a list property, one of its values". ADR 0004's Markdown shape is unchanged: a YAML list is
  already valid and indexed.
- **One rule, no parallel representation.** `memory_tracker.property_value` and `note_property_index.property_value`
  hold the value text for a list item and `''` for a single (scalar) value, so today's scalar trackers keep working
  without a backfill and a scalar value edit keeps its tracker as today. Everything that matches a tracker to a unit
  matches on (key, value). The one transition rule, "a tracked single value that becomes a list follows that value",
  is one backend operation used by both the editor (slice 7) and the reduce flow (slice 8).
- **Limit.** `property_value` is `varchar(255)` like `property_key`; the unique key (user, note, type, key, value) then
  stays under InnoDB's 3072-byte limit (128 + 8 + 1020 + 1020 bytes). A list item longer than 255 characters is not
  indexed as a value (plan decision; such items are not realistic `example of` values).
- No North Star topic governs memory trackers; none is added.

## Existing solutions (PFE)

- `note_property_index` already has one row per list item (`item_index`), but only for reference items, and does not
  store the value text (`NotePropertyIndexPlanner` computes `valueText`; `NotePropertyIndexService.persistRowsForPropertyKey`
  drops it). Extend it; do not add a second index.
- The property-key rename/removal guard (`usePropertyMemoryTrackerGuard.ts`, `PATCH /api/memory-trackers/{id}/property-key`,
  `MemoryTrackerService.updatePropertyKey`) is the existing mechanism for moving trackers when content changes; slices
  6 and 7 extend it rather than adding a backend content diff.
- The frontend already appends to a list key (`propertyRowsAfterAppendingValueToExactKey`); the backend has no in-place
  list append (`FrontmatterInPlaceEdit` has only `rewriteSupportedValues` and `setTopLevelScalar`), so slice 8 adds one
  beside them.
- Tests to extend: `UnassimilatedPropertyServiceTest` (`list_property_emits_one_unit_after_refresh` changes meaning),
  `AssimilationServicePropertyReferenceGateTest`, `AssimilationControllerAssimilateTests`,
  `QuestionGenerationRequestBuilderTests`, `QuestionGenerationBatchJsonlRendererTest`, `RecallsControllerTests`,
  `MemoryTrackerUpdatePropertyKeyControllerTest`, `RelationControllerReduceToSourcePropertyTests` (the colliding-key
  suffix test at l.99 changes meaning), `RichMarkdownEditor.propertyMemoryTracking.spec.ts`,
  `usePropertyMemoryTrackerGuard.spec.ts`, `e2e_test/features/recall/property_memory_tracker.feature`.

## Decisive premises

Observed by a read-only code survey on 2026-09-30 (paths under `backend/src/main/java/com/odde/donut`, `frontend/src`).

| Premise | Operation that consumes it | Observation | Result |
| --- | --- | --- | --- |
| Tracker grain is (user, note, type, property_key) with a unique key of those four | slice 1a migration | `V300000323__retire_memory_tracker_deleted_at.sql:13`; `MemoryTracker.java:135-138` | confirmed by reading |
| "Unassimilated" joins the tracker by key only and dedupes to the lowest `item_index` per key | slices 1b, 2a | `NotePropertyIndexRepository.java:17-34` | confirmed by reading; slice 1b keeps the dedupe, 2a removes it |
| The index stores list rows only for reference items and no value text | slice 1b | `NotePropertyIndexPlanner.java:40-61`, `NotePropertyIndexService.java:121-155` | confirmed by reading |
| A list property gets `Property value: ` (empty) in the recall question today | slice 3 | `FocusContextMarkdownAugmenter.java:15-31` uses `Frontmatter.getString`, which is empty for a list (`Frontmatter.java:43-49`) | confirmed by reading; slice 3's red run observes it |
| Recall DTOs and the recall screen carry the key only | slice 4 | `RecalledNote.java:20`, `MemoryTrackerLite.java:16`, `FocusedPropertyIndicator.vue` | confirmed by reading |
| The panel and `AssimilationModes` look up one tracker per key and send `{propertyKey}` only | slice 5 | `assimilationMemoryTrackers.ts:13-35`, `AssimilationModes.vue:136-142` | confirmed by reading |
| The guard finds one tracker per key to rename or delete | slice 6 | `usePropertyMemoryTrackerGuard.ts:63-130` | confirmed by reading |
| Reduce picks `key N` via `nextAvailablePropertyKeyForBase`, its only backend caller, and re-homes trackers with `setPropertyKey` | slice 8 | `NoteReferenceHandling.java:60-85,174-189`, `NoteContentMarkdown.java:106-116`; `rg nextAvailablePropertyKeyForBase backend/src/main` | confirmed; deleting the helper after slice 8 leaves no backend caller |
| Frontend and backend parse a quoted list item such as `"[[run]]"` to the same string `[[run]]` | slices 5, 9 | backend `FrontmatterPropertyValue.ListItems`, frontend `PropertyValue { kind: "list"; items }`, | observed by reading: the frontend parses with the `yaml` package (`noteContentFrontmatterParse.ts`), the backend with SnakeYAML (`Frontmatter.java`); both yield the unquoted string. Slice 9's e2e is the cross-stack proof |

## Outside-in proof

| Promise (key example) | Owner | Observable proof |
| --- | --- | --- |
| A two-value list gives two assimilation units; assimilating one creates only its tracker | slices 2a, 2b | `UnassimilatedPropertyServiceTest`, `AssimilationControllerAssimilateTests` |
| Adding, reordering or removing another value leaves a value's tracker and unit state intact | slice 2a | `UnassimilatedPropertyServiceTest` |
| A single value keeps one tracker as today | slices 1b, 2a | existing scalar tests stay green |
| A value's recall question is about that value | slice 3 | `QuestionGenerationRequestBuilderTests`, `QuestionGenerationBatchJsonlRendererTest` |
| Recall names the value being recalled | slice 4 | `RecallsControllerTests`, recall component spec |
| The panel offers Assimilate per value | slice 5 | `RichMarkdownEditor.propertyMemoryTracking.spec.ts` |
| Renaming or removing a list key carries every value tracker | slice 6 | `usePropertyMemoryTrackerGuard.spec.ts`, `MemoryTrackerUpdatePropertyKeyControllerTest` |
| A tracked single value turned into a list keeps its tracker on that value | slice 7 | guard spec, controller test |
| Reducing into an existing key adds a value, no `key 2`, tracker follows the value | slice 8 | `RelationControllerReduceToSourcePropertyTests` |
| The journey in the app: assimilate one of two values, recall shows that value | slice 9 | `property_memory_tracker.feature`, run once |

Commands: backend `CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test --tests '<Class>' -Dspring.profiles.active=test`;
frontend `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/<path>.spec.ts` plus the frontend typecheck
(`.agents/agent-map.md`); e2e `CURSOR_DEV=true nix develop -c pnpm cy:run --spec <feature>`. API client regeneration uses
the generate-api-client skill in each slice that changes a controller signature or DTO.

## Ordered slices

### 1a. Trackers and index rows have a property value column
Type: Structure
Status: done — `V300000352`; `MemoryTrackerPropertyValueUniquenessTest`; focused tracker/assimilation/index/recall set green.
`MemoryTracker.propertyValue` is `@JsonIgnore` until slice 4 exposes it. Still matching by key only (correct while every
value is `''`): `MemoryTrackerAssimilation` duplicate check, `MemoryTrackerService.updatePropertyKey` collision check,
`NotePropertyIndexRepository` joins, `findByNote_IdAndPropertyKey`.
Proof: migrate the test DB; a `MemoryTrackerBuilder` pair with the same key and different values saves, the same
(key, value) pair is refused by the unique key; every existing tracker and assimilation test green.

Migration (next version above `V300000351`, see the db-migration skill): add `property_value varchar(255) NOT NULL
DEFAULT ''` to `memory_tracker` (unique key becomes user, note, type, key, value) and to `note_property_index` (unique
key stays note, key, item). Entities and builders gain the field. Enables slice 1b.

### 1b. The index keeps every list item with its value
Type: Structure
Status: done — `NotePropertyIndexPlannerTest`, `NotePropertyIndexServiceTest`, `NotePropertyIndexAuthoredReferenceTest`;
focused tracker/assimilation/index/recall/note-controller set green. Blank list items are skipped too (`''` means a
single value), so a list whose items are all blank or over 255 characters has no row. A list reference item whose
authored reference is missing keeps its row with a null reference. `Frontmatter.keys()` is `Set.copyOf` (order varies
per JVM run), so index row ids are ordered only within one key: tests assert per key.
Proof: `NotePropertyIndexPlannerTest` and a `NotePropertyIndexService` test: a list `[alpha, "[[B]]"]` gives two rows
with values `alpha` and `[[B]]` (the second with its reference), a scalar gives one row with `''`; every existing
assimilation and gate test green.

The index persists every list item with its value text (reference or not; items over 255 characters are skipped),
scalars `''`. The unassimilated query keeps its per-key dedupe, so external behavior is unchanged. Enables slice 2a.

### 2a. Each list value is its own assimilation unit
Type: Behavior
Status: planned
Proof: `UnassimilatedPropertyServiceTest`, `AssimilationServicePropertyReferenceGateTest`, red then green.

Behavior: note with `example of: ["[[run]]", "[[past tense]]"]` → the sequence and counts offer two units with values
`[[run]]` and `[[past tense]]`; a tracker on (`example of`, `[[run]]`) suppresses only that unit; reordering the list
or removing `[[past tense]]` leaves the `[[run]]` tracker matched; a scalar still gives one unit with value `''`; a key
skip still suppresses every value. `AssimilationUnit` and `AssimilationNextUnitDTO` carry the value; the query joins on
(key, value) and drops the dedupe; a value's gate is its own reference's target (a plain-text value is not gated).
Amend ADR 0001's "Property memory tracker" entry. If past about 10 minutes, split the gate change into its own slice.

### 2b. Assimilating one value creates only that value's tracker
Type: Behavior
Status: planned
Proof: `AssimilationControllerAssimilateTests`, red then green; API client regenerated.

Behavior: `POST assimilate` with `{noteId, propertyKey: "example of", propertyValue: "[[run]]"}` → one tracker with that
key and value; repeating it creates nothing; the other value stays unassimilated; the key's skip row is still deleted
as today. Omitted value means `''` (scalar), so existing callers are unchanged.

### 3. A value's recall question is about that value
Type: Behavior
Status: planned
Proof: `QuestionGenerationRequestBuilderTests`, `QuestionGenerationBatchJsonlRendererTest`, red then green.

Behavior: a tracker (`example of`, `[[run]]`) → the property focus block says `Property key: example of` and
`Property value: [[run]]` (today: empty for a list); a scalar tracker is unchanged. The tracker's value flows through
`RecallPromptService`, `McqService` and the batch renderer instead of the key alone.

### 4. Recall names the value being recalled
Type: Behavior
Status: planned
Proof: `RecallsControllerTests` (`MemoryTrackerLite`, `RecalledNote` carry `propertyValue`) and a recall component spec
on `FocusedPropertyIndicator`; API client regenerated.

Behavior: recalling the `[[run]]` tracker → the screen shows the focused property `example of` with `[[run]]`, and the
wrong-answer message names the value; a scalar tracker shows as today.

### 5. The property panel offers Assimilate for each value
Type: Behavior
Status: planned
Proof: `RichMarkdownEditor.propertyMemoryTracking.spec.ts`, red then green.

Behavior: open the panel of a list row → one Assimilate control per value, labelled with the value; assimilating
`[[run]]` sends its value and shows it as tracked while `[[past tense]]` stays offered; a scalar row's panel is
unchanged; Skip stays one control for the key. Tracker lookup in `assimilationMemoryTrackers.ts` matches (key, value).

### 6. Renaming or removing a list key carries every value's tracker
Type: Behavior
Status: planned
Proof: `usePropertyMemoryTrackerGuard.spec.ts`, `MemoryTrackerUpdatePropertyKeyControllerTest`, red then green.

Behavior: a list key with two tracked values → renamed in the rich row or the Markdown editor → after the one
confirmation both trackers have the new key with their values; removed → both trackers are deleted after the
confirmation. The rename conflict check compares (key, value).

### 7. A tracked single value that becomes a list keeps its tracker on that value
Type: Behavior
Status: planned
Proof: guard spec and a `MemoryTrackerController` test, red then green; API client regenerated.

Behavior: `example of: "[[run]]"` with a tracker (value `''`) → the author adds `[[past tense]]` (rich row append, list
dialog or Markdown editor) and saves → the tracker's value becomes `[[run]]`, history and schedule unchanged, and
`[[past tense]]` is offered to assimilate. The backend operation "tracker follows its value" (set `''` to the given
value for that note and key) is added to `MemoryTrackerService` and reused by slice 8. If the former value is not in the
list, nothing moves.

### 8. Reducing a relationship note into an existing key adds a value
Type: Behavior
Status: planned
Proof: `RelationControllerReduceToSourcePropertyTests` (replace the colliding-key suffix case), red then green.

Behavior: `Sentence` has `example of: "[[run]]"` with a tracker, and the tracked relationship note "Sentence an example
of past tense" is reduced → `Sentence` has `example of: ["[[run]]", "[[past tense]]"]`, no `example of 2`; the `[[run]]`
tracker has value `[[run]]` (slice 7's operation) and the relationship note's tracker has key `example of` and value
`[[past tense]]` with its history; reducing into a key that is already a list appends; a key the source lacks is still
written as a single value, as today. Add an in-place list append beside `FrontmatterInPlaceEdit.setTopLevelScalar`;
delete `addPropertyWithAvailableKeyToLeadingFrontmatter` and the backend `nextAvailablePropertyKeyForBase` once unused.

### 9. The journey in the app
Type: Behavior
Status: planned
Proof: new scenario in `e2e_test/features/recall/property_memory_tracker.feature`, run once.

Behavior: a note with `example of: ["[[run]]", "[[past tense]]"]` → the learner assimilates only `[[run]]` from the
property panel → on recall the question names `example of` and `[[run]]`, and `[[past tense]]` is still offered to
assimilate.

## Current decisions

- Scalars keep value `''`; no backfill of existing trackers, and editing a scalar value keeps its tracker as today.
- List items longer than 255 characters are not indexed as values.
- A value's assimilation gate is its own reference's target.
- A removed list value's tracker is left in place, as the backend does today for any content edit outside the guard;
  it is not promised to be cleaned up.
- Frontend numbered-key suggestions (`nextAvailablePropertyKeyFor*` in `noteContentPropertyKeys.ts`) stay; story 2 and
  SEED-064#story-6 decide their removal.
