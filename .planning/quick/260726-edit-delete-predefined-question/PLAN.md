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

### Phase 1 — Delete a question (Behavior) — [done]

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

### Phase 2 — Edit a question (Behavior) — [done]

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

### Phase 1 learnings / deviations

- `PredefinedQuestionService` needed a direct `PredefinedQuestionRepository`
  dependency (didn't have one before) — `EntityPersister` only exposes
  save/merge, not delete. Added the repository as a constructor param,
  `deleteQuestion()` just calls `repository.delete(...)`.
- E2E page objects: the "Questions for the note" popup stays mounted across
  Cucumber steps within one scenario (PopButton's `show` ref isn't reset by
  `router().push` to the same note URL). The existing `expectQuestionsInList`
  pattern re-detects "is the dialog already open" by checking for
  `.question-table` in the DOM — that check breaks once the list is empty
  (0 questions renders "No questions" instead of the table), so the new
  `deleteQuestion`/`expectQuestionNotInList` helpers detect open state via
  the always-present `button[title="Add Question"]` instead.
- Confirmed (via `git stash` + full SUT restart) that the two pre-existing
  `@usingMockedOpenAiService` scenarios ("Can generate the question by AI",
  "Can refine the question by AI") fail identically on unmodified `main` in
  this sandbox — a pre-existing environment/mock flake unrelated to Phase 1.
  Not fixed as part of this phase (out of scope).

### Phase 2 learnings / deviations

- `PredefinedQuestionService.updateQuestion` only copies the three editable
  fields (`multipleChoicesQuestion`, `correctAnswerIndex`) onto the existing
  entity and `entityPersister.merge`s it — `note`/ownership is never touched,
  matching decision #5's "no new auth/ownership logic" intent.
- `NoteAddQuestion.vue` prefill: cloning the incoming `existingQuestion` prop
  with `structuredClone()` throws `DataCloneError` because Vue wraps props in
  reactive Proxies, which the structured-clone algorithm can't clone. Used
  `JSON.parse(JSON.stringify(...))` instead — safe here since
  `PredefinedQuestion` is plain JSON-serialisable data with no
  functions/dates/cycles.
- E2E `fillQuestion` (used by "add") unconditionally clicks the `+` button
  once to grow from the form's default 2 choices to the 3 the test fixtures
  use. Reusing it for "edit" over-adds a 4th (empty) choice, which fails the
  `isMCQWithAnswerValid` check and leaves Submit disabled. Split it into
  `fillQuestionFields` (just types into existing fields, no `+` click) and
  `fillQuestion` (`+` click then `fillQuestionFields`, used by add/refine);
  `editQuestion` uses `fillQuestionFields` directly since edit mode is
  already prefilled with the right number of choices.
- Reused Phase 1's "is the popup already open" detection
  (`button[title="Add Question"]` presence) for the new `editQuestion`
  page-object method, so it works whether or not a prior step in the same
  scenario already opened the "Questions for the note" dialog.
