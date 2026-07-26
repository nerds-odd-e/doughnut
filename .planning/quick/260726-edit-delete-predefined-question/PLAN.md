# Edit or delete a question in the question list of a note (closed 2026-07-26)

Source: doughnut.odd-e.com/n24416 (Product Backlog). Both phases shipped;
kept as a short forensics record for the non-obvious gotchas below.

## Scope decision

The requirement note's screenshot showed an "Approved" checkbox column that
never existed in the entity/UI/API — treated as an outdated mockup and
explicitly excluded. Scope was edit + delete only.

Authorization requirement ("restrict to notebook owner only") needed no new
code: every existing endpoint's `authorizationService.assertAuthorization(note)`
already resolves to `user.owns(notebook)`; new endpoints just reuse it.

## Phase 1 — Delete a question

`DELETE /api/predefined-questions/{note}/note-questions/{question}` →
`PredefinedQuestionService.deleteQuestion`. Per-row delete button in
`Questions.vue` using the existing `popups.confirm(...)` pattern
(`useNoteDeleteFlow.ts`).

- `PredefinedQuestionService` had no delete-capable dependency
  (`EntityPersister` only exposes save/merge) — added a direct
  `PredefinedQuestionRepository` constructor dependency.
- E2E gotcha: the "Questions for the note" popup stays mounted across
  Cucumber steps within one scenario. The old "is it already open" check
  looked for `.question-table` in the DOM, which disappears once the list
  is empty (renders "No questions" instead) — switched detection to the
  always-present `button[title="Add Question"]`.

## Phase 2 — Edit a question

`PUT /api/predefined-questions/{note}/note-questions/{question}` →
`PredefinedQuestionService.updateQuestion` (copies only the three editable
fields onto the existing entity, never touches `note`/ownership). Reused
`NoteAddQuestion.vue` in an edit mode (optional `existingQuestion` prop,
hides Refine/Generate-by-AI per "manual-only" decision) rather than a new
component.

- Prefill cloning: `structuredClone()` throws `DataCloneError` on a Vue
  reactive prop (Proxy). Used `JSON.parse(JSON.stringify(...))` instead —
  safe since `PredefinedQuestion` is plain JSON data.
- E2E helper `fillQuestion` (add flow) unconditionally clicks `+` once to
  grow from 2 default choices to 3; reusing it for edit over-added a 4th
  empty choice and left Submit disabled. Split into `fillQuestionFields`
  (no `+` click, used by edit) and `fillQuestion` (`+` then fill, used by
  add/refine).

## Known pre-existing issue (unrelated, not fixed here)

The two `@usingMockedOpenAiService` scenarios ("Can generate the question by
AI", "Can refine the question by AI") in
`predefined_questions_management.feature` fail in this sandbox on
unmodified `main` too (confirmed via `git stash` + full SUT restart before
Phase 1, and reconfirmed after Phase 2) — a pre-existing environment/mock
flake, out of scope for this plan.

## Outstanding: push blocked

Both phases are committed locally on `feature/edit-delete-predefined-question`
(`6d5765c891`, `eff00d5008`) but `git push` fails:
`Permission to nerds-odd-e/doughnut.git denied to hung86223`. Needs a git
identity with push access, or push from elsewhere.
