# Refinement extract: preview, retry, export

## Goal

Improve the **Refine note** modal's extract flow (`NoteRefinement.vue`) so that extract is
reviewable and editable before it commits, can be retried against the same selection, and can
export its underlying AI request. Also add export of the layout-generation ("breakdown") AI
request from the same modal.

Requirement summary:
- Extract shows a **preview** of: original note's new content, new note title, new note content.
- All three preview fields are **editable** (plain markdown `<textarea>`, no rich editing).
- **Ask AI to retry**: re-run extraction with the *same selection* (not re-generate the layout).
- Button to **export the full AI request JSON for extracting**.
- In the refinement modal, a button to **export the AI request for the breakdown** (layout generation).

## Current behavior (baseline)

- `Extract` → `POST /api/ai/extract-note/{note}` with `{ layout, selectedItemIds }`.
- Backend runs the AI, gets `NoteExtractionResult` (`newNoteTitle`, `newNoteContent`,
  `updatedOriginalNoteContent`), **immediately persists both notes** via
  `NoteConstructionService.createNoteFromExtractedSuggestion(...)`, returns the new `NoteRealm`.
- Frontend navigates to the new note. No preview, edit, retry, or export exists in refinement.
- `Remove selected` flow is separate and unchanged by this work.

Key files:
- Frontend: `frontend/src/components/recall/NoteRefinement.vue`,
  `frontend/src/components/recall/RefinementLayoutItemRow.vue`,
  `frontend/tests/components/recall/NoteRefinement.*.spec.ts` (+ `noteRefinementTestSupport.ts`).
- Backend: `controllers/AiController.java`,
  `services/ai/AiNoteAutomationService.java`,
  `services/ai/tools/NoteRefinementAiToolFactory.java`,
  `services/NoteConstructionService.java`,
  `services/ai/NoteExtractionResult.java`.
- Export pattern to mirror: `PredefinedQuestionController.exportQuestionGeneration` +
  `services/openAiApis/StructuredResponseCreateParamsSerializer.java` +
  `frontend/src/components/notes/QuestionExportDialog.vue`.
- E2E: `e2e_test/features/assimilation/note_refinement.feature`,
  `e2e_test/step_definitions/note_refinement_ai.ts`.

## Confirmed design decisions

- **Preview placement**: inline in the Refine note modal, replacing the layout list, with a
  **Back** to return to the layout.
- **Commit flow (two-step)**: `Extract` now generates the preview (unpersisted); a separate
  **Create note** button persists the (edited) fields and navigates to the new note.
- **Retry with unsaved edits**: warn/confirm before discarding edits, then regenerate.
- **Export UI**: reuse the question-export pattern — a dialog with a read-only JSON textarea +
  copy button.
- **Export extract request**: enabled only when ≥1 layout point is selected (same gate as
  Extract), because the extract prompt depends on the selection.
- **Remove selected** flow: unchanged.

### Endpoint shape (target)

- `POST /api/ai/extract-note-preview/{note}` — body `{ layout, selectedItemIds }`, runs AI,
  returns `NoteExtractionResult` (3 fields), **persists nothing**.
- `POST /api/ai/create-extracted-note/{note}` — body is the three (edited) fields
  (`newNoteTitle`, `newNoteContent`, `updatedOriginalNoteContent`); persists both notes via
  `createNoteFromExtractedSuggestion`, returns `NoteRealm`. Keeps existing server-side
  validation (reserved `index` title, alias validation); errors surface back into the preview.
- `POST /api/ai/export-extract-request/{note}` — body `{ layout, selectedItemIds }`, returns
  `toBodyMap(params)` for the extract request without calling OpenAI.
- `GET /api/ai/export-refinement-layout-request/{note}` — returns `toBodyMap(params)` for the
  layout-generation ("breakdown") request without calling OpenAI.

The current one-shot `POST /api/ai/extract-note/{note}` (layout+selection → AI → persist) is
**replaced** by the preview + create pair above and removed once the frontend has switched
(Phase 1d). Adding the two new endpoints first (additive, existing behavior untouched) keeps
every commit green.

Requires exposing a "build params without executing" path in `AiNoteAutomationService`
(currently `buildStructuredResponseParams(...)` is private), mirroring how question generation
exposes `buildQuestionGenerationRequest`.

### Small-commit discipline

- Each sub-phase below is one commit-sized change, either **Behavior** (observable value,
  tested) or **Structure** (no external behavior change; existing tests stay green; immediately
  followed by the behavior it enables).
- Any sub-phase that changes a backend controller signature/DTO ends with
  `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`.
- Each sub-phase ends green (unit + touched E2E) and is committed + pushed (deploy gate) before
  the next starts.
- Targeted E2E only:
  `cypress run --spec e2e_test/features/assimilation/note_refinement.feature`.

## Phases (stop-safe, ordered by value; each sub-phase = one commit)

### Phase 1 — Extract shows an editable preview and a Create note step

Highest value: turns the one-shot destructive extract into a reviewable, editable one.

**1a — Structure (backend): add preview endpoint.** ✅
Add `POST /api/ai/extract-note-preview/{note}` that calls `automation.extractNote(layout, ids)`
and returns the `NoteExtractionResult` (3 fields) without persisting. Existing `extract-note`
untouched. Add a controller test (`AiControllerExtractNotePreviewTest`) covering happy path;
validation (empty content, invalid selection) covered via shared `extractNoteFromLayoutSelection`
in `AiControllerExtractNoteValidationTest`. Run `generateTypeScript`.
_Green: existing extract behavior + all tests unchanged._

**Learning:** `NoteExtractionResult` is fine as the API response type (same 3 fields); no separate
DTO needed. Preview and persist endpoints share `extractNoteFromLayoutSelection`.

**1b — Structure (backend): add create-from-edited-fields endpoint.** ✅
Add `POST /api/ai/create-extracted-note/{note}` accepting `{ newNoteTitle, newNoteContent,
updatedOriginalNoteContent }`, persisting both notes via
`createNoteFromExtractedSuggestion(...)`, returning `NoteRealm`. Reuse existing validation;
map validation failures to a clear error response. Add a controller test covering happy path +
reserved `index` title + alias validation. Run `generateTypeScript`.
_Green: additive; old `extract-note` still works._

**Learning:** `NoteExtractionResult` works as both preview response and create request body.
Validation errors (`ApiException` for reserved `index` title and invalid aliases) bubble from
`NoteConstructionService` unchanged — no controller mapping needed.

**1c — Behavior (frontend + E2E): two-step preview → edit → create.** ✅
`Extract` (with selection) calls `extract-note-preview`; the modal replaces the layout list with
3 editable `<textarea>`s (original note's new content, new note title, new note content), a
**Back** button (return to layout), and a **Create note** button that calls
`create-extracted-note` with the edited values and navigates to the new note. Loading modal on
both AI calls; surface create errors in the preview. Rework `NoteRefinement.extractNote.spec.ts`
and `noteRefinementTestSupport.ts`. Update the "Extract selected layout points to one new note"
scenario in `note_refinement.feature` and the stub/step in `note_refinement_ai.ts`; add a
scenario asserting **edited** preview content is what gets saved. The old `extract-note` endpoint
is now unused by the frontend.
_Green: two-step extract works end-to-end; new endpoints now E2E-covered._

**1d — Structure (cleanup): remove the old one-shot extract endpoint.** ✅
Delete `POST /api/ai/extract-note/{note}` and its now-obsolete test; remove dead code/helpers.
Run `generateTypeScript`.
_Green: no external behavior change (endpoint was already unused)._

**Learning:** Sanitization (path separators, title trim) runs in `AiNoteAutomationService` on preview only; persistence tests belong on `createExtractedNote`. Consolidated coverage from deleted `AiControllerExtractNoteTest` into preview and create test classes.

### Phase 2 — Ask AI to retry extraction with the same selection

**2a — Behavior: retry regenerates the preview.** ✅
Add an **Ask AI to retry** button on the preview that re-calls `extract-note-preview` with the
same `layout` + `selectedItemIds` and replaces the 3 fields. Frontend spec for the re-call +
field replacement. Add a retry stub sequence in `note_refinement_ai.ts` and a
`note_refinement.feature` scenario that retries then creates from the second result.

**2b — Behavior: confirm before discarding edits on retry.**
If any preview field was edited, `popups.confirm` before retrying; discard on accept, keep on
cancel. Frontend spec for both branches.

### Phase 3 — Export the extract AI request JSON

**3a — Structure (backend): expose params build + export endpoint.**
Expose a "build extract params without executing" path in `AiNoteAutomationService` (mirror
`buildQuestionGenerationRequest`). Add `POST /api/ai/export-extract-request/{note}` returning
`toBodyMap(params)` (no OpenAI call). Controller test asserts the body map contains
model/instructions/input/schema and reflects the selection. Run `generateTypeScript`.
_Green: additive; no UI yet._

**3b — Structure (frontend): a reusable AI-request export dialog.**
Generalize the question-export dialog into a shared component that renders arbitrary AI-request
JSON (read-only textarea + copy), keeping `QuestionExportDialog` behavior and its existing test
green.
_Green: existing question-export unchanged._

**3c — Behavior (frontend): export extract request button.**
**Export extract request** button on the layout screen, enabled only with a selection, opens the
shared dialog with the extract JSON. Frontend spec for gating + dialog content.

### Phase 4 — Export the breakdown (layout-generation) AI request JSON

**4a — Structure (backend): breakdown export endpoint.**
Add `GET /api/ai/export-refinement-layout-request/{note}` returning `toBodyMap(params)` for the
layout-generation request (no OpenAI call). Controller test for the body map. Run
`generateTypeScript`.
_Green: additive; no UI yet._

**4b — Behavior (frontend): export breakdown request button.**
**Export breakdown request** button in the Refine note modal (no selection needed) opens the
shared dialog with the layout-generation JSON. Frontend spec for the button + dialog.

## Testing strategy notes

- Frontend: Vitest browser mode via existing `noteRefinementTestSupport` helpers; test through
  user interactions and `data-test-id` selectors.
- Backend: controller-level tests with `makeMe` fixtures; assert AI instruction content and
  persistence as `AiControllerExtractNoteTest` already does.
- E2E: extend `note_refinement.feature` (capability-named) only; run targeted
  `cypress run --spec e2e_test/features/assimilation/note_refinement.feature`.
- After backend signature/DTO changes: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`.

## Status

- Phase 1 (extract preview + create): ✅ complete (1a–1d)
- Phase 2 (retry): 2a ✅, 2b — planned
- Phase 3 (export extract request): 3a, 3b, 3c — planned
- Phase 4 (export breakdown request): 4a, 4b — planned
