# Note refinement layout extraction

Refine note assimilation so AI returns one layout of the current note instead of multiple alternative split suggestions. The user can select any layout points, including non-contiguous points, and extract the selected content into one new note.

## Requirement analysis

### What the user asked for

| # | Requirement | Observable outcome |
|---|-------------|-------------------|
| 1 | Generate the current note layout, not multiple ways to split it | Refine note shows one outline derived from current content |
| 2 | Layout has at most 2 levels of nesting | Outline supports top-level points and one level of child points only |
| 3 | Identify already extracted parts from simple wiki-link lines | Points represented by simple wiki-link-only lines are marked clearly as **Already extracted** |
| 4 | Already extracted points remain selectable | Checkbox stays enabled even when the point is marked **Already extracted** |
| 5 | Single **Extract** button in the refinement dialog | No per-point extract buttons; selected checked points are extracted together |
| 6 | Checkboxes choose which point(s) to extract | User can check any top-level or child point before clicking **Extract** |
| 7 | Parent checkbox auto-selects descendants | Checking a parent checks all child points |
| 8 | Manual child uncheck is allowed | Parent becomes indeterminate when only some child points are selected |
| 9 | Non-contiguous selections are allowed | AI receives the full layout plus selected points and produces one new note plus meaningful remaining parent content |
| 10 | Existing **Remove selected** remains | Removal continues to use selected checked points |
| 11 | Extraction placement and persistence stay the same | New note is created beside the source note as today; source note content is updated from AI output |

### Current behavior

- Backend AI generation returns a flat `List<String>` through `RefinementSuggestions` and `RefinementSuggestionsDTO`.
- `NoteRefinement.vue` renders a flat list with one checkbox per suggestion.
- Extraction is currently per row: each suggestion has its own **Extract note** button.
- `AiController.extractNote` validates that the request contains exactly one suggestion.
- Extraction prompt receives only the selected suggestion text, not the full suggestion set or user selection context.
- Removal accepts multiple selected suggestion strings and returns regenerated content, but does not persist it directly.

### Design decisions

1. Rename the generated concept from "suggestions" toward "layout" in new code where practical. Keep endpoint names temporarily if that avoids a larger generated API churn, but make the response/request schema express layout items.
2. Represent each layout item with a stable AI-generated id, text, optional children, and an `alreadyExtracted` boolean.
3. AI should mark `alreadyExtracted: true` when the source note has a simple standalone wiki-link line that appears to replace or point to extracted content. Wording shown to the user: **Already extracted**.
4. Top-level items can be extractable content or grouping headings. Selection should not assume every parent is a content leaf.
5. Extraction and removal requests should include the full layout and selected item ids. The backend can derive the selected item texts from ids for validation and prompt building.
6. Extraction prompt should receive both the full nested layout and the selected points, so AI can extract non-contiguous selections while leaving coherent parent content.
7. Tri-state checkbox behavior belongs in the frontend UI: checked parent = parent and all children selected; indeterminate parent = some but not all descendants selected.
8. Already extracted points are visually marked only; they are still valid extraction/removal selections.

### Suggested API shape

Names can be adjusted to match local generated API conventions during implementation.

```json
{
  "items": [
    {
      "id": "p1",
      "text": "Main concept",
      "alreadyExtracted": false,
      "children": [
        {
          "id": "p1-1",
          "text": "Supporting detail",
          "alreadyExtracted": true,
          "children": []
        }
      ]
    }
  ]
}
```

For extraction/removal:

```json
{
  "layout": { "items": [] },
  "selectedItemIds": ["p1", "p2-1"]
}
```

Validation rules:

- Layout depth must be at most 2 levels.
- Item ids must be unique inside the layout.
- `selectedItemIds` must be non-empty and must all exist in the submitted layout.
- Extraction should no longer require exactly one selected item.

## Phases

### Phase 1 - Return a nested note layout from AI

Status: done

Behavior: When a user opens note refinement for a non-empty note, the backend returns one note layout with at most two levels, including **Already extracted** flags for simple wiki-link-only lines.

Implementation notes:
- Replace or extend `RefinementSuggestions` with a structured layout response DTO.
- Update `AiToolFactory.generateRefinementSuggestionsAiTool()` prompt to request one current-content layout, not alternative breakdown suggestions.
- Include schema descriptions that prohibit grandchildren and explain wiki-link-only extracted markers.
- Keep blank-content behavior returning an empty layout.
- Regenerate TypeScript API after backend DTO/controller signature changes.

Tests:
- Extend `backend/src/test/java/com/odde/doughnut/controllers/AiControllerNoteRefinementTest.java` to assert structured layout response and prompt wording.
- Add validation/unit coverage for max depth and id uniqueness if validation is implemented outside the controller.
- Update frontend test fixtures that mock `generateRefinementSuggestions`.

Targeted checks:
- `CURSOR_DEV=true nix develop -c pnpm backend:test_only -- --tests com.odde.doughnut.controllers.AiControllerNoteRefinementTest`
- `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`

Completion notes:
- The backend now returns `NoteRefinementLayoutDTO` from the existing `generate-refinement-suggestions` endpoint and validates AI layouts for unique ids, non-blank text, non-null children, and no grandchildren.
- The old flat AI response schema and DTO were removed. The existing extraction/removal request DTO remains flat until later phases change those request bodies.
- `NoteRefinement.vue` temporarily flattens returned layout items into the existing flat suggestion UI so Phase 1 can ship without implementing Phase 2 UI behavior early.
- Local verification required resetting a stale `doughnut_test` database row that conflicted with generated test user ids; after migrating the clean test DB, targeted backend tests passed.

### Phase 2 - Show the layout with tri-state selection

Status: planned

Behavior: The refinement dialog renders the nested layout, marks already extracted points clearly, and allows selecting parents, children, or non-contiguous points. Checking a parent checks its children; unchecking a child makes the parent indeterminate.

Implementation notes:
- Update `frontend/src/components/recall/NoteRefinement.vue` from a flat `string[]` to layout items.
- Remove per-row **Extract note** buttons from the item rows.
- Add one dialog-level **Extract** button beside the existing **Remove selected** button.
- Disable extract/remove buttons only when no items are selected.
- Preserve current loading/error behavior around AI operations.

Tests:
- Replace/extend `frontend/tests/components/recall/NoteRefinement.extractNote.spec.ts` to assert one extract button and no per-item extract buttons.
- Extend `frontend/tests/components/recall/NoteRefinement.removeSuggestions.spec.ts` for nested checkbox behavior, indeterminate parent state, already-extracted label, and non-contiguous selection.
- Update `e2e_test/features/assimilation/note_refinement.feature` to expect nested layout rendering rather than flat suggestion count.

Targeted checks:
- `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/NoteRefinement.extractNote.spec.ts tests/components/recall/NoteRefinement.removeSuggestions.spec.ts`
- `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/assimilation/note_refinement.feature`

### Phase 3 - Extract selected layout points into one note

Status: planned

Behavior: When the user selects one or more layout points and clicks **Extract**, AI creates one new note from those selected points and updates the original note so the remaining content is meaningful, even for non-contiguous selections.

Implementation notes:
- Change extraction request handling from exactly one suggestion to full layout plus selected ids.
- Build an extraction prompt that includes the full layout and highlights selected items.
- Preserve current note creation location and wiki-link sanitization behavior.
- Keep already-extracted selected items valid; the prompt should treat them as user-selected content, not as disabled or skipped content.

Tests:
- Update `backend/src/test/java/com/odde/doughnut/controllers/AiControllerExtractNoteTest.java` to allow multiple selected layout items and assert prompt includes both full layout and selected ids/text.
- Preserve existing location, sanitization, wiki-link cache, auth, and AI-null tests.
- Update frontend extraction tests to assert request body includes layout and selected ids, and navigation still goes to the new note.
- Update E2E extraction scenario to select multiple non-contiguous layout points and create one note.

Targeted checks:
- `CURSOR_DEV=true nix develop -c pnpm backend:test_only -- --tests com.odde.doughnut.controllers.AiControllerExtractNoteTest`
- `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/NoteRefinement.extractNote.spec.ts`
- `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/assimilation/note_refinement.feature`

### Phase 4 - Remove selected layout points using the same selection model

Status: planned

Behavior: When the user selects layout points and clicks **Remove selected**, AI removes those selected points from the note content while preserving unrelated content.

Implementation notes:
- Change removal request handling to accept full layout plus selected ids.
- Prompt removal with selected layout points rather than a flat suggestion string list.
- Preserve the existing response-only save flow in `NoteRefinement.vue`.

Tests:
- Update backend removal tests in `AiControllerNoteRefinementTest`.
- Update frontend removal tests to assert selected layout body is sent.
- Keep E2E removal scenario focused on user-observable note content update.

Targeted checks:
- `CURSOR_DEV=true nix develop -c pnpm backend:test_only -- --tests com.odde.doughnut.controllers.AiControllerNoteRefinementTest`
- `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/NoteRefinement.removeSuggestions.spec.ts`
- `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/assimilation/note_refinement.feature`

### Phase 5 - Naming cleanup and generated API consistency

Status: planned

Behavior: User-facing refinement behavior is stable, and code/API names no longer misleadingly imply a flat suggestions list where practical.

Implementation notes:
- Rename frontend variables and helper names from `refinementSuggestions` to layout-oriented names where touched.
- Consider endpoint/DTO naming cleanup only if the generated API impact is contained. Otherwise document the transitional endpoint names.
- Update E2E page object helper names to reflect layout items and selected points.

Tests:
- Run the focused frontend/backend tests touched by renames.
- Run whitespace check before handing off.

Targeted checks:
- `scripts/check_diff_whitespace.sh`
- Relevant focused frontend/backend tests from prior phases.

## Out of scope

- Changing where extracted notes are created.
- Preventing extraction of already extracted points.
- Adding a persisted extraction marker beyond the simple wiki-link-line heuristic.
- Building arbitrary-depth outlines.
- Reworking the broader assimilation panel outside note refinement.
