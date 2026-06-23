# List Properties In Note Frontmatter

## Status

Phase 7 done. Phase 8 done. Phases 9–12 planned.

## Goal

Support Obsidian-compatible one-level list values in note YAML frontmatter while preserving Doughnut's exact-key property tracking model.

Users may keep using legacy suffix keys such as `example of 2`. Users may also use list values, and may mix both styles. A list belongs to one exact property key, so all list items under `example of` share the `example of` memory tracker. `example of 2` remains a separate key and tracker.

## Critical Requirement Refinements

- Obsidian passthrough keys `tags`, `aliases`, and `cssclasses` should parse, display, edit as content, and round-trip, but should not become property-memory tasks or affect link resolution in this plan.
- Empty lists are content-level values only. Compose them as `key: []`, but do not seed a property-index row or memory tracker for an empty list unless a later product decision explicitly wants empty properties to be assimilable.
- Current URL semantics should not change here. In this repo, `url` is currently reserved from automatic property tracking. List support may let users store multiple URLs, but this plan should not make URLs assimilable.
- List wiki-link gating applies to resolved target notes only. Missing or unresolved wiki links behave like today's scalar unresolved wiki link: no target note is stored, so it does not block the property unit.
- Only assimilable keys enter `note_property_index`. For list values, index rows should be minimal: one null-target row when an assimilable non-empty list has no resolved targets, or one row per resolved target when gating needs those targets. Do not create index rows for every non-link item.
- Scalar-only structural keys remain scalar-only: `image`, `image_mask`, `wikidata_id`, `title_pattern`, `question_generation_instruction`, `type`, `relation`, `source`, and `target`.
- Frontend and backend must converge on the same supported value shape: scalar string/number/boolean or one-level list of string/number/boolean. Nulls, nested lists, and nested mappings are unsupported.

## Settled Decisions

- Legacy suffix keys coexist indefinitely. Do not migrate on read. Do not perform a one-time migration.
- Memory tracking remains per exact property key, not per list item.
- A list property with multiple resolved wiki-link targets is gated until all those targets are assimilated, deleted, or skip memory tracking.
- `tags`, `aliases`, and `cssclasses` are passthrough only for now.
- Duplicate list items are allowed without warnings.
- Empty lists are valid and compose as `key: []`.
- List typing for predefined property keys is postponed.

## Discoveries

- Frontend parsing currently returns `NoteProperties` (`Record<string, PropertyValue>`) from `frontend/src/utils/noteContentFrontmatterParse.ts`; scalar compatibility via `scalarRecordFromNoteProperties` and `frontmatterScalar` in `frontend/src/utils/noteProperties.ts`.
- `yamlValueToPropertyValue` and `yamlRecordFromNoteProperties` in `noteProperties.ts` handle one-level list parse and Obsidian-style block compose (`key: []` for empty lists).
- `isScalarOnlyStructuralPropertyKey` in `noteContentPropertyKeys.ts` blocks list values on structural keys during parse.
- Frontend rich property rows currently model every row as `{ key: string; value: PropertyValue }` in `frontend/src/utils/noteContentPropertyRows.ts`.
- Rich editor body updates recompose frontmatter from property rows, so imported lists must be represented in rows before parse support can safely ship. Otherwise body-only edits could drop or corrupt list values.
- Backend frontmatter parsing is separate from the frontend parser. `backend/src/main/java/com/odde/doughnut/algorithms/Frontmatter.java` stores YAML values as `Object` but exposes most callers to `getString(...)` and `stringValuesInInsertionOrder()`.
- `note_property_index` has one row per `(note_id, property_key, item_index)` plus optional `target_note_id`. List properties use minimal rows: one per resolved wiki-link target (with `item_index` preserving YAML order), or one null-target row when a non-empty list has no resolved targets.
- Backend indexing resolves each list item's first wiki link independently in `NotePropertyIndexService.refreshForNote`, planned by `NotePropertyIndexPlanner`.
- `url` is currently considered reserved structural in `PropertyKeyNaming.isReservedStructuralKey`, so URL properties are excluded from automatic property tracking today.

## Phase 1 - Frontend Property Model Can Carry Scalars Without Behavior Change

**Done.**

Type: Structure.

Precondition: Existing notes contain only scalar frontmatter.

Trigger: Frontend parses, displays, edits, and composes scalar frontmatter.

Postcondition: Existing scalar behavior is unchanged, but the frontend has a `PropertyValue` / `NoteProperties` model and scalar compatibility helpers ready for list behavior in the next phase.

Scope:

- Introduce frontend `PropertyValue` and `NoteProperties`.
- Keep scalar helpers for existing callers, for example title pattern, relation detection, image reads, and property rows.
- Keep parser output behavior scalar-equivalent for existing scalar notes.
- Keep `PropertyRow` scalar-only in behavior for this phase, or adapt it internally while preserving the same external row behavior.

Tests:

- Extend existing scalar parse/compose tests only where type migration touches behavior.
- Existing rich editor property tests should remain green.

Targeted commands:

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/utils/noteContentFrontmatterParse.spec.ts
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/utils/noteContentFrontmatter.spec.ts
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.properties.spec.ts
```

## Phase 2 - Frontend Parses And Composes List Values

**Done.**

Type: Behavior.

Precondition: A note contains one-level list frontmatter.

Trigger: The frontend parses or composes note content.

Postcondition: Supported list values parse successfully and compose consistently. Unsupported nested or null values still produce `unsupported_value`.

Scope:

- Accept block sequences and flow arrays on parse.
- Normalize list item strings, numbers, and booleans to strings.
- Reject null list items, nested lists, nested mappings, top-level null values, and list values for scalar-only structural keys.
- Compose non-empty lists in Obsidian-friendly block style.
- Compose empty lists as `key: []`.
- Preserve duplicate-key error behavior.

Tests:

- Extend `frontend/tests/utils/noteContentFrontmatterParse.spec.ts`.
- Extend `frontend/tests/utils/noteContentFrontmatter.spec.ts`.
- Include cases for `tags`, generic lists, flow arrays, empty lists, scalar-only key rejection, nested value rejection, and duplicate list items.

Targeted commands:

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/utils/noteContentFrontmatterParse.spec.ts
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/utils/noteContentFrontmatter.spec.ts
```

## Phase 3 - Rich Editor Preserves Imported Lists Without Full List Editing

**Done.**

Type: Behavior.

Precondition: A note contains supported list frontmatter and the user opens it in rich editor mode.

Trigger: The user views properties or edits only the body.

Postcondition: The list properties no longer show the frontmatter parse-error banner, are visible in a compact read/edit-safe form, and are preserved when body-only edits emit updated markdown.

Scope:

- Let property rows carry scalar or list values.
- Render list values compactly in read-only and editable property lists.
- Keep inline editing limited to scalar values for now.
- If a list row cannot be edited inline yet, use the later popup phase rather than flattening it into a scalar input.
- Ensure `composeNoteContentFromPropertyRows` preserves list values.

Tests:

- Extend `frontend/tests/components/form/RichMarkdownEditor.properties.spec.ts`.
- Cover imported `tags` or `example of` list content with no parse error.
- Cover body edit or paste-complete preserving list frontmatter.

Targeted command:

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.properties.spec.ts
```

## Phase 4 - Backend Frontmatter Accessors Distinguish Scalars And Lists

**Done.**

Type: Structure.

Precondition: Backend code still consumes scalar frontmatter through existing helpers.

Trigger: Existing backend helpers read scalar notes.

Postcondition: Existing scalar backend behavior is unchanged, but `Frontmatter` exposes value accessors that can return scalar values and list item values without flattening nested objects into accidental strings.

Scope:

- Add backend value accessors for supported scalar and list item strings.
- Preserve `getString(...)` behavior for scalar-only structural callers.
- Add explicit helper behavior for unsupported values rather than relying on `Object.toString()` for lists/maps.
- Do not change property indexing yet.

Tests:

- Add or extend backend algorithm tests for `Frontmatter` / `NoteContentMarkdown` scalar regressions.

Targeted command:

```bash
CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests com.odde.doughnut.algorithms.*
```

## Phase 5 - Backend Wiki-Link Helpers See Links Inside List Items

**Done.**

Type: Behavior.

Precondition: A note contains list frontmatter with wiki links.

Trigger: Backend note-content helpers scan or rewrite frontmatter wiki links.

Postcondition: Wiki-link occurrence order and frontmatter link removal include supported list item values while preserving scalar behavior.

Scope:

- Make `NoteContentMarkdown.wikiLinkInnersInOccurrenceOrder` include list item values in YAML item order.
- Make frontmatter wiki-link removal update list items as values, not as Java collection strings.
- Drop a list item only if the same scalar-removal rule would leave it blank.
- Keep unsupported nested values outside this behavior.

Tests:

- Extend backend algorithm tests for wiki-link occurrence order.
- Extend existing link-removal tests if present; otherwise add focused coverage through `NoteContentMarkdown`.

Targeted command:

```bash
CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests com.odde.doughnut.algorithms.*
```

## Phase 6 - Property Index Schema Can Represent Multiple Rows Per Key Without Behavior Change

**Done.**

Type: Structure.

Precondition: Existing indexed notes have at most one row per property key.

Trigger: Migrations run and existing scalar property-index behavior is exercised.

Postcondition: Existing scalar index behavior is unchanged, but the schema can store multiple rows for one exact property key when later target gating needs them.

Scope:

- Add a Flyway migration:
  - add `item_index` with default `0`
  - replace the current unique key with `UNIQUE (note_id, property_key, item_index)`
  - add indexes needed for grouping by `(note_id, property_key)`
- Update `NotePropertyIndex` with `itemIndex`.
- Keep `NotePropertyIndexService.refreshForNote` writing only scalar item `0` rows in this phase.
- Ensure existing JDBC backfills remain compatible with the new column default.

Tests:

- Existing `NotePropertyIndexServiceTest` and unassimilated property tests remain green.

Targeted commands:

```bash
CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests com.odde.doughnut.services.NotePropertyIndexServiceTest
CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests com.odde.doughnut.services.UnassimilatedPropertyServiceTest
```

## Phase 7 - Assimilation Queries Group Rows By Exact Property Key

**Done.**

Type: Structure.

Precondition: The index schema allows multiple rows per property key, but production refresh still writes scalar item `0` rows.

Trigger: The assimilation queue counts and streams property units for scalar notes.

Postcondition: Existing scalar queue behavior is unchanged, and the query shape is ready to emit one property unit per exact key when multiple target rows arrive in the next phase.

Scope:

- Update repository queries to group or de-duplicate by `(note, property_key)`.
- Preserve tracker join semantics: one tracker for the exact key suppresses all rows for that exact key.
- Preserve reserved-key filtering.
- Keep ordering stable by note priority and property key.
- Do not index list-derived rows yet.

Tests:

- Extend or preserve `UnassimilatedPropertyServiceTest`.
- Include a direct setup with duplicate index rows for the same exact key if useful, verifying only one property unit is emitted and one tracker suppresses it.

Targeted command:

```bash
CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests com.odde.doughnut.services.UnassimilatedPropertyServiceTest
```

## Phase 8 - Backend Indexes List Property Targets And Gates On All Resolved Targets

**Done.**

Type: Behavior.

Precondition: A note contains a list property with zero, one, or multiple scalar items.

Trigger: The backend refreshes `note_property_index`, then the assimilation queue asks for unassimilated property units.

Postcondition: Non-empty assimilable list properties create only the index rows needed for property existence and resolved-target gating. The queue offers at most one unit per exact key. A property unit is hidden until every resolved target note across that exact key is assimilated, deleted, or skip memory tracking.

Scope:

- Update `NotePropertyIndexService.refreshForNote`:
  - scalar: one row with `item_index = 0`
  - non-empty list with resolved wiki-link targets: one row per resolved target, with `item_index` preserving the original YAML item order
  - non-empty list with no resolved targets: one null-target row with `item_index = 0`, so the exact property key can still become one property unit
  - empty list: no index rows and no memory tracker seeding
  - exact suffix keys are independent keys
- Exclude passthrough keys `tags`, `aliases`, and `cssclasses` from automatic property indexing and tracker seeding.
- Preserve current URL exclusion.
- Resolve each list item's first wiki link independently.
- Do not index every non-link item; non-link items only matter to the content value, not to target gating.
- Make target-note gate require all rows for the exact key to pass.
- Update tracker seeding/backfill logic to operate on distinct exact property keys.

Tests:

- Extend `backend/src/test/java/com/odde/doughnut/services/NotePropertyIndexServiceTest.java`.
- Extend `backend/src/test/java/com/odde/doughnut/services/AssimilationServicePropertyWikiLinkGateTest.java`.
- Extend `backend/src/test/java/com/odde/doughnut/services/UnassimilatedPropertyServiceTest.java`.
- Extend property tracker seeding/backfill tests if they assume one index row per key.
- Include a regression for mixed exact keys:
  - `example of: ["[[A]]", "[[B]]"]`
  - `example of 2: "[[C]]"`
  - `example of` and `example of 2` remain separate property units.
- Include a regression that a custom non-empty list with no resolved targets creates one property unit, not one unit per item.
- Include a regression that `tags`, `aliases`, `cssclasses`, `url`, and empty lists create no property-index rows.

Targeted commands:

```bash
CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests com.odde.doughnut.services.NotePropertyIndexServiceTest
CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests com.odde.doughnut.services.AssimilationServicePropertyWikiLinkGateTest
CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests com.odde.doughnut.services.UnassimilatedPropertyServiceTest
CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests com.odde.doughnut.services.NotePropertyTrackingBackfillTest
```

## Phase 9 - Scalar Property Values Can Be Edited In A Textarea Popup

Type: Behavior.

Precondition: A user edits a scalar text property in the rich editor.

Trigger: The user clicks the value edit icon button for that row.

Postcondition: A dialog opens with text mode selected and a textarea for the scalar value. Saving updates the scalar property without changing it into a list.

Scope:

- Add an icon button for popup editing on text-capable scalar rows.
- Use a textarea in text mode.
- Preserve existing inline scalar editing for now unless the implementation intentionally replaces it.
- Do not add list switching in this phase.
- Keep image, Wikidata, and relation specialized controls unchanged.

Tests:

- Extend `frontend/tests/components/form/RichMarkdownEditor.properties.spec.ts`.
- Cover open, edit textarea, save, cancel, and unchanged scalar YAML output shape.

Targeted command:

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.properties.spec.ts
```

## Phase 10 - Popup Can Switch Between Text And List Values

Type: Behavior.

Precondition: A user edits a text-capable property row in the rich editor.

Trigger: The user opens the value popup and switches the mode between text and list.

Postcondition: The user can save either a scalar textarea value or a list value for that exact key.

Scope:

- Add text/list mode selection in the popup.
- List mode supports ordered item rows with add and remove.
- Duplicate list items are allowed.
- Empty string list items are rejected.
- Empty lists can be saved and compose as `key: []`.
- Hide or disable list mode for scalar-only structural keys.
- Keep reorder out of this phase.

Tests:

- Extend `frontend/tests/components/form/RichMarkdownEditor.properties.spec.ts`.
- Cover scalar-to-list, list-to-scalar, duplicate items allowed, empty item validation, empty list save, and scalar-only key blocking.

Targeted command:

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.properties.spec.ts
```

## Phase 11 - List Popup Supports Reordering

Type: Behavior.

Precondition: A property row has a list value with at least two items.

Trigger: The user reorders items in the value popup and saves.

Postcondition: The composed YAML preserves the new item order, and backend indexing later preserves that order through `item_index`.

Scope:

- Add move up/down or drag-style reorder controls using the repo's existing UI conventions.
- Preserve stable layout and accessible labels.
- Keep duplicate values reorderable as distinct items.

Tests:

- Extend `frontend/tests/components/form/RichMarkdownEditor.properties.spec.ts`.
- Cover reorder and YAML order.

Targeted command:

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.properties.spec.ts
```

## Phase 12 - Preset Families Can Add Values To Existing List-Capable Keys

Type: Behavior.

Precondition: A user edits a preset family such as `example of` in the rich property editor.

Trigger: The user adds another value to a property family through the rich UI.

Postcondition: The UI can add another value to the existing exact key as a list item, while legacy suffix keys remain valid and editable.

Scope:

- For new rich-editor "add another value" interactions, prefer list items when adding to an existing text/list key.
- Preserve existing suffix keys exactly when opening and saving unrelated edits.
- Do not automatically fold `key`, `key 2`, and `key 3` into a list unless the user explicitly edits that key's values in the dialog.
- Keep exact-key tracker semantics in behavior: adding to `example of` affects the `example of` property unit, not `example of 2`.

Tests:

- Extend `frontend/tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts`.
- Extend `frontend/tests/utils/noteContentPropertyRows.spec.ts` only for pure row transformation logic.

Targeted commands:

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/utils/noteContentPropertyRows.spec.ts
```

## Deferred

- First-class tag search/filtering.
- Alias-based wiki-link resolution.
- CSS behavior for `cssclasses`.
- One-time migration from suffix keys to list values.
- Type-specific list enforcement for predefined property keys beyond scalar-only structural blocking.
- Changing current URL property tracking semantics.
- Treating empty list properties as assimilable property units.
- Backend duplicate-key validation beyond current behavior.

## Phase Completion Checklist

For each phase:

- Add or update tests before or alongside implementation.
- Keep each phase to a small commit-sized change.
- Run only the targeted tests for touched behavior unless the change obviously crosses a wider boundary.
- Keep no intentionally failing tests at the phase boundary.
- Remove dead code and update this plan with discoveries.
- Commit, push, and let CD deploy before starting the next phase, unless the team explicitly agrees otherwise.
