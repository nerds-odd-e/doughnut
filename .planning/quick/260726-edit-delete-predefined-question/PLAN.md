# Edit or delete a question in the question list of a note

Source: doughnut.odd-e.com/n24416 (Product Backlog).

## Scope decision

The requirement note's screenshot shows an "Approved" checkbox column that
does not exist anywhere in the current entity/UI/API (confirmed via
`PredefinedQuestion` entity, `Questions.vue`, `PredefinedQuestionController`).
Treated as an outdated mockup — **out of scope**. This plan covers only
**edit** and **delete** of an existing question.

## Clarified decisions

1. Scope: edit + delete only, no "approved" workflow.
2. Consistency: follow existing architecture/conventions exactly (see below).
3. Edit UI: reuse `NoteAddQuestion.vue` in an edit mode, not a new component.
4. Edit is manual-fields-only — no AI refine/generate wired into edit mode.
5. Authorization: restrict to notebook owner only. **Already satisfied for
   free** — `authorizationService.assertAuthorization(note)` (used by every
   existing endpoint in `PredefinedQuestionController`) resolves to
   `assertAuthorizationNote` → `hasFullAuthority` → `user.owns(notebook)`.
   New endpoints reuse this exact same call, no new auth logic needed.

## Existing conventions to follow

- Delete confirmation: `usePopups().popups.confirm(...)` (see
  `useNoteDeleteFlow.ts` / `NoteDeleteButton.vue`) — same pattern for
  question delete.
- Auth: `authorizationService.assertAuthorization(note)` on every endpoint.
- Persistence: `entityPersister.save`/`merge` (service layer), repository is
  a plain `CrudRepository<PredefinedQuestion, Integer>` — `deleteById`
  already available for free.
- Path-variable entity resolution: `PredefinedQuestion extends
  EntityIdentifiedByIdOnly`, so `@PathVariable("question") PredefinedQuestion
  question` resolves the same way `Note note` already does elsewhere in this
  controller.
- E2E scenarios live in the existing capability file
  `e2e_test/features/note_creation_and_update/predefined_questions_management.feature`
  — extend it, don't create a new file.

## Phases

### Phase 1 — Delete a question (Behavior) — [planned]

- **Pre-condition:** A note has an existing predefined question.
- **Trigger:** User clicks "Delete" on a question row in the "Questions for
  the note" dialog and confirms.
- **Post-condition:** The question is removed from the database and no
  longer appears in the list.

Touches:
- `backend/.../controllers/PredefinedQuestionController.java` — add
  `DELETE /api/predefined-questions/{note}/note-questions/{question}`.
- `backend/.../services/PredefinedQuestionService.java` — add
  `deleteQuestion(PredefinedQuestion)`.
- `frontend/src/components/notes/Questions.vue` — per-row delete button,
  `popups.confirm(...)`, call generated `deleteQuestion` API, splice from
  local `questions` list.
- Regenerate TS client: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`.
- E2E: new scenario "Delete a question from the note" in
  `predefined_questions_management.feature`, tag `@wip` until green.

### Phase 2 — Edit a question (Behavior) — [planned]

- **Pre-condition:** A note has an existing predefined question.
- **Trigger:** User clicks "Edit" on a question row; the reused
  `NoteAddQuestion.vue` form opens prefilled with that question's stem,
  choices, and correct-answer index; user changes fields and submits.
- **Post-condition:** The question's fields are updated in the database and
  the change is reflected in the list.

Touches:
- `backend/.../controllers/PredefinedQuestionController.java` — add
  `PUT /api/predefined-questions/{note}/note-questions/{question}`.
- `backend/.../services/PredefinedQuestionService.java` — add
  `updateQuestion(PredefinedQuestion existing, PredefinedQuestion updated)`.
- `frontend/src/components/notes/NoteAddQuestion.vue` — accept an optional
  existing `PredefinedQuestion` prop; when present, prefill fields, hide
  AI generate/refine buttons (manual-only per decision #4), submit calls the
  new update endpoint instead of add.
- `frontend/src/components/notes/Questions.vue` — per-row "Edit" button
  opening the form via `PopButton` (same pattern as "Add Question"),
  replace the edited row's data on success.
- Regenerate TS client.
- E2E: new scenario "Edit a question in the note" in
  `predefined_questions_management.feature`, tag `@wip` until green.

## Notes

- Both phases are independently shippable and stop-safe: Phase 1 alone
  delivers real value (removing bad/unwanted questions) even if Phase 2
  never lands.
- No database migration needed (no schema change).
- No new authorization logic needed (see clarified decision #5).
