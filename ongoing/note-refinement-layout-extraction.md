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

Status: done

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

Completion notes:
- `NoteRefinement.vue` renders nested layout items with tri-state checkboxes, **Already extracted** badges, and one dialog-level **Extract** button beside **Remove selected**; both actions disable when nothing is selected.
- Selection logic lives in `useRefinementLayoutSelection`; shared row UI in `RefinementLayoutItemRow.vue`.
- Multi-item extract still shows a placeholder alert (Phase 3); single-item extract and removal continue using flat suggestion text in API requests.
- E2E scenarios assert nested layout levels and already-extracted markers instead of flat suggestion count.

### Phase 3 - Extract selected layout points into one note

Status: done

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

Completion notes:
- Extraction endpoint now accepts `NoteRefinementExtractRequestDTO` with full `layout` and `selectedItemIds`; validation ensures layout integrity and that every selected id exists.
- `AiToolFactory.extractNoteAiTool` builds a prompt with the full nested layout JSON, selected ids, and selected item texts; already-extracted items remain valid selections.
- `NoteRefinement.vue` sends layout plus selected ids for any selection count and navigates to the new note on success.
- E2E scenario extracts non-contiguous layout points B and D into one new note.

### Phase 4 - Remove selected layout points using the same selection model

Status: done

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

Completion notes:
- Removal endpoint now accepts `NoteRefinementRemoveRequestDTO` with full `layout` and `selectedItemIds`; validation mirrors extraction.
- `AiToolFactory.removeSelectedLayoutPointsFromContentAiTool` builds a prompt with the full nested layout JSON, selected ids, and selected item texts.
- `NoteRefinement.vue` sends layout plus selected ids for removal; response-only save flow unchanged.
- Dead `RefinementSuggestionsRequestDTO` removed; frontend tests share `refinementLayoutSelectionApiCall` helper.

### Phase 5 - Naming cleanup and generated API consistency

Status: done

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

Completion notes:
- Frontend UI label is now "Note layout:"; `data-test-id` values use `refinement-layout`, `extract-refinement-layout`, and `remove-refinement-layout`.
- Test helpers and E2E page objects use layout-oriented names (`refinementLayoutPanel`, `removeRefinementLayoutItemsAt`, etc.). `NoteRefinement.removeSuggestions.spec.ts` renamed to `NoteRefinement.removeLayout.spec.ts`.
- Removed dead E2E steps (`OpenAI generates refinement suggestions:`, flat item-count assertion) and unused `stubRefinementLayout`.
- Backend `AiToolFactory.refinementSuggestions()` renamed to `noteRefinementLayout()`; transitional REST/SDK names kept: `generate-refinement-suggestions`, `remove-refinement-suggestion`, `generateRefinementSuggestions`, `removeRefinementSuggestion`.

## Initial delivery complete

All five original phases delivered nested note refinement layout generation, tri-state selection, multi-point extraction, layout-based removal, and naming cleanup.

A follow-up code review surfaced bugs, robustness gaps, and code smells. They are decomposed into the **review follow-up phases** below, ordered by user value and stop-safe so the work can stop after any phase with proportional value delivered.

## Review follow-up phases

Source: code review on 2026-06-20 of the changes from phases 1-5.

### Phase 6 - Reset refinement layout after a successful removal

Status: done

Type: Behavior

Behavior:
- Pre-condition: the user has selected layout points in note refinement and the AI has removed the related content.
- Trigger: the removal API returns updated content and the note content is saved.
- Post-condition: the refinement panel no longer presents the just-removed points as still selected; the selection is cleared and the displayed layout reflects the current (post-removal) content rather than the stale pre-removal layout.

Why: `removeSelectedLayoutItems` in `NoteRefinement.vue` never clears `selectedItemIds` or refreshes `refinementLayoutItems` after success. The panel keeps showing removed points checked, so a second **Remove selected** click re-sends stale ids against already-changed content. This is the highest-value fix: a directly observable, repeatable user error.

Implementation notes:
- On a successful removal, clear the selection and re-derive the layout from current content (reload via the generate endpoint, or prune removed/disappeared ids from local state).
- Keep the existing response-only save flow and loading/error behavior unchanged.
- Extraction is unaffected (it navigates away), so do not change extraction here.

Completion notes:
- `NoteRefinement.vue` calls `loadRefinementLayout()` after a successful removal (clears selection and re-fetches layout from the generate endpoint).
- Unit test asserts selection cleared, layout re-queried, and panel reflects post-removal items.
- E2E removal scenario stubs a post-removal layout reload sequence and asserts no selection plus updated layout points.

Tests:
- Extend `frontend/tests/components/recall/NoteRefinement.removeLayout.spec.ts`: after a confirmed removal, assert the selection is cleared and the layout is refreshed (e.g. the generate endpoint is re-queried or removed ids are gone).
- Extend the removal scenario in `e2e_test/features/assimilation/note_refinement.feature` to assert the panel state after removal reflects updated content, not the stale selection.

Targeted checks:
- `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/NoteRefinement.removeLayout.spec.ts tests/components/recall/NoteRefinement.layoutSelection.spec.ts`
- `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/assimilation/note_refinement.feature`

### Phase 7 - Define indeterminate-parent selection semantics

Status: done

Type: Behavior

Decision: **Option A** — Parent's own id is included in extract/remove selection only when the parent is **fully** selected. An indeterminate parent contributes only its checked descendants (not the parent's own id).

Why: `useRefinementLayoutSelection.setItemSelection` previously added the parent's own id when the parent was checked and only removed a child's id when that child was unchecked, so an indeterminate parent still submitted its own id. Per design decision 4 a top-level item may be a grouping heading, so extracting/removing its heading text when the user only wanted some children may be wrong.

Behavior:
- Pre-condition: the user checks a parent, then unchecks one of its children (parent becomes indeterminate).
- Trigger: the user clicks **Extract** or **Remove selected**.
- Post-condition: the submitted `selectedItemIds` match Option A semantics, consistently for both extract and remove.

Implementation notes:
- `reconcileParentSelection` in `useRefinementLayoutSelection` adds the parent's id only when all child descendants are selected; otherwise removes it.
- Keep parent-checks-children and child-uncheck-makes-parent-indeterminate behavior intact.

Completion notes:
- `setItemSelection` calls `reconcileParentSelection` after each toggle so indeterminate parents exclude their own id from `selectedItemIds`.
- When all descendants are selected again, the parent id is re-included.
- Tests assert exact `selectedItemIds` for indeterminate-parent extract and remove, plus re-selection of all children.

Tests:
- Extend `frontend/tests/components/recall/NoteRefinement.layoutSelection.spec.ts` to assert the exact `selectedItemIds` for the indeterminate-parent case under Option A, for both extract and remove request bodies.

Targeted checks:
- `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/NoteRefinement.layoutSelection.spec.ts tests/components/recall/NoteRefinement.extractNote.spec.ts`

### Phase 8 - Align extract and remove edge-case handling

Status: done

Type: Behavior

Behavior:
- Pre-condition: the source note content is blank, or the AI returns no removal result.
- Trigger: the user invokes extraction on blank content; or removal returns unchanged content because the AI produced nothing.
- Post-condition: extraction rejects blank content the same way removal does (`BAD_REQUEST`), and a no-op removal does not trigger a redundant note save / `contentUpdated` emit.

Why: `AiController.extractNote` has no empty-content guard while `removeRefinementSuggestion` does (inconsistent contract). On an AI-null removal, `removeSelectedLayoutPointsAndRegenerateContent` returns the original content, yet the frontend still calls `updateTextField` and emits `contentUpdated` with unchanged text.

Implementation notes:
- Add the blank-content guard to `extractNote` mirroring `removeRefinementSuggestion`.
- In `NoteRefinement.vue`, skip the save/emit when removal returns content equal to the current content (or when the AI produced no change).

Tests:
- Extend `backend/src/test/java/com/odde/doughnut/controllers/AiControllerExtractNoteTest.java` for blank-content extraction returning `BAD_REQUEST`.
- Extend `frontend/tests/components/recall/NoteRefinement.removeLayout.spec.ts` to assert no `updateTextField`/`contentUpdated` when content is unchanged.

Targeted checks:
- `CURSOR_DEV=true nix develop -c pnpm backend:test_only -- --tests com.odde.doughnut.controllers.AiControllerExtractNoteTest`
- `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/NoteRefinement.removeLayout.spec.ts`

### Phase 9 - Make invalid AI layout diagnosable

Status: done

Type: Behavior

Behavior:
- Pre-condition: the AI returns a malformed layout (blank field, null children, duplicate id, or grandchildren).
- Trigger: layout generation runs `validOrEmpty`.
- Post-condition: the invalid layout is still not shown, but the rejection is **diagnosable** (a logged warning identifying why the layout was rejected) instead of silently collapsing the whole layout to empty with no signal.

Why: `NoteRefinementLayoutValidator.validOrEmpty` discards the entire AI layout if any single item is malformed, returning empty with no log. This makes "empty panel for a non-empty note" impossible to diagnose.

Implementation notes:
- Add a logged warning in the generation path when `isValid` fails (include the first failing reason if cheap to determine).
- Do not change the user-facing empty-layout fallback; this phase only adds observability.

Tests:
- Add/extend a backend unit test asserting that an invalid AI layout yields an empty layout (existing behavior preserved). Keep `NoteRefinementLayoutValidatorTest` focused on validation rules.

Targeted checks:
- `CURSOR_DEV=true nix develop -c pnpm backend:test_only -- --tests com.odde.doughnut.services.ai.NoteRefinementLayoutValidatorTest`
- `CURSOR_DEV=true nix develop -c pnpm backend:test_only -- --tests com.odde.doughnut.controllers.AiControllerNoteRefinementTest`

### Phase 10 - Unify the duplicate layout-selection request DTO

Status: done

Type: Structure

Behavior unchanged; verified externally by existing extract/remove tests passing and a clean regenerated API.

Why: `NoteRefinementExtractRequestDTO` and `NoteRefinementRemoveRequestDTO` are byte-for-byte identical (confirmed identical in the generated `types.gen.ts`). `AiController.validateLayoutSelectionRequest` is already shared, so the two DTOs should collapse into one.

Implementation notes:
- Introduce a single `NoteRefinementLayoutSelectionRequestDTO` (capability-named) and use it for both `extractNote` and `removeRefinementSuggestion` request bodies.
- Regenerate the TypeScript API and update `NoteRefinement.vue` request typing if the generated type name changes.
- Only do this structure phase because Phases 6-8 already touch these request paths; do not add speculative abstraction beyond merging the two identical DTOs.

Tests:
- Reuse existing extract/remove backend and frontend tests; assert request bodies still carry `layout` + `selectedItemIds`.

Targeted checks:
- `CURSOR_DEV=true nix develop -c pnpm backend:test_only -- --tests com.odde.doughnut.controllers.AiControllerExtractNoteTest --tests com.odde.doughnut.controllers.AiControllerNoteRefinementTest`
- `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`
- `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/NoteRefinement.extractNote.spec.ts tests/components/recall/NoteRefinement.removeLayout.spec.ts`

### Phase 11 - Tidy remaining layout naming and prompt text

Status: done

Type: Structure

Behavior unchanged; verified externally by existing tests passing.

Why: residual transitional debt from the deliberate Phase 5 deferral and stale prompt wording.

Implementation notes:
- Update `NoteExtractionResult` `@JsonClassDescription` to describe extracting **one or more selected layout points** (currently singular "a refinement suggestion").
- Re-evaluate whether the transitional REST/SDK names (`generate-refinement-suggestions`, `remove-refinement-suggestion`, `generateRefinementSuggestions`, `removeRefinementSuggestion`) and token constants (`REFINEMENT_SUGGESTIONS_MAX_OUTPUT_TOKENS`, `REMOVE_SUGGESTIONS_MAX_OUTPUT_TOKENS`) can be renamed with contained generated-API churn; if not, keep this phase to prompt/description text only and re-document the transitional names.
- Out of scope unless cheap: collapsing the asymmetric `NoteRefinementLayoutDTO` (response) vs `NoteRefinementLayout` (request) shapes.

Completion notes:
- `NoteExtractionResult` schema description now refers to one or more selected layout points.
- Internal token constants renamed to `NOTE_REFINEMENT_LAYOUT_MAX_OUTPUT_TOKENS` and `REMOVE_LAYOUT_POINTS_MAX_OUTPUT_TOKENS`; `AiToolFactory.generateNoteRefinementLayoutAiTool()` replaces the transitional factory method name.
- Remove endpoint OpenAPI summary updated to layout-oriented wording.
- **Transitional REST/SDK names retained** (Jidoka): renaming paths/operationIds would churn generated `sdk.gen.ts`, `types.gen.ts`, frontend mocks, and external API consumers; defer to a dedicated API-versioning pass. Transitional names: `generate-refinement-suggestions`, `remove-refinement-suggestion`, `generateRefinementSuggestions`, `removeRefinementSuggestion`.

Tests:
- Run the focused backend and frontend tests touched by any rename.
- `scripts/check_diff_whitespace.sh` before handing off.

Targeted checks:
- `scripts/check_diff_whitespace.sh`
- Relevant focused frontend/backend tests from prior phases.

## Plan complete

All review follow-up phases (6–11) are done. Note refinement now uses a nested layout with tri-state selection, multi-point extract/remove, post-removal layout refresh, indeterminate-parent semantics, aligned edge-case handling, diagnosable invalid layouts, unified layout-selection request DTO, and updated prompt/schema wording. Transitional REST/SDK endpoint names remain for a future contained API rename.

## Out of scope

- Changing where extracted notes are created.
- Preventing extraction of already extracted points.
- Adding a persisted extraction marker beyond the simple wiki-link-line heuristic.
- Building arbitrary-depth outlines.
- Reworking the broader assimilation panel outside note refinement.
- Server-side reconciliation of the client-submitted layout against the actual note content. The layout is round-tripped from the client by design (design decision 5); the note content used by the AI still comes from the server-side focus context, so this is an accepted trust boundary rather than a defect.
