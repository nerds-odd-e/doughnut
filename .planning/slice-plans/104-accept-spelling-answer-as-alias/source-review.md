# Source review: SEED-073#accept-spelling-answer-as-alias

Launch: fresh general-purpose subagent (no conversation inheritance, fork or
resume), 2026-11-17, during dough-execute-plan before slice 1 delegation.
Input: only the selected story section (Goal, Why, Required behavior,
Required restrictions, Out of scope, Key examples, UI, Considered, Effort),
copied verbatim from `.planning/seeds/SEED-073-learning-refinement-and-retrieval.md#accept-spelling-answer-as-alias`
without its state block; no plan, product, proof, or slice scope. Told to read
nothing else, change nothing, and number independently observable promises.

Unnumbered context returned: Why (retest evidence, miss counts); restriction
rationales ("the name belongs to a different note", "the note would reject
that alias"); "one offer, never both" covered by C9 and C10; Out of scope
items as scope limits only; Considered (exclusion rebuild declined; hand
editing is the baseline); Effort hypothesis; owner decisions 2026-11-17 as
sources of C7, C9, C10.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| C1 | A wrong, nonblank answer that names no other note, is a valid alias, on a note the learner may edit, shows an action beside "Your answer `X` is incorrect." named with the typed answer (e.g. "Add `つてを頼る` as an alias") | slice 1 | observed: e2e_test/features/recall/recall_quiz_spelling_question.feature "Accept a wrong spelling answer as an alias" step 'I add my spelling answer "sedation" as an alias' (button 'Add `sedation` as an alias' after 'Your answer `sedation` is incorrect.'); frontend/tests/components/recall/AnsweredSpellingQuestionAddAnswerAsAlias.spec.ts 'adds the typed answer…' |
| C2 | One click writes the typed answer into the reviewed note's `aliases`, no dialog or confirmation | slice 1 | observed: frontend/tests/components/recall/AnsweredSpellingQuestionAddAnswerAsAlias.spec.ts 'adds the typed answer to the reviewed note's aliases with one click': one click → one updateNoteContent for the reviewed note with aliases list holding the answer |
| C3 | After the write, the result shows the answer is now an alias (e.g. "`つてを頼る` is now an alias") | slice 1 | observed: e2e_test/features/recall/recall_quiz_spelling_question.feature "Accept a wrong spelling answer as an alias" step 'I should see that "sedation" is now an alias'; frontend/tests/components/recall/AnsweredSpellingQuestionAddAnswerAsAlias.spec.ts same test asserts done text |
| C4 | After the write, the note shown under the result lists the new alias | slice 1 | observed: e2e_test/features/recall/recall_quiz_spelling_question.feature "Accept a wrong spelling answer as an alias" step 'I should see the reviewed note context containing "sedation"' after the write (Note context article) |
| C5 | After the write, the action cannot add the same answer a second time | slice 1 | observed: frontend/tests/components/recall/AnsweredSpellingQuestionAddAnswerAsAlias.spec.ts 'adds the typed answer…' asserts no button after click; AnsweredQuestionPage.expectSpellingAnswerIsAlias asserts button absent |
| C6 | When the result is shown again later (e.g. from the last answered question), it still shows the answer as an alias and cannot add it twice | slice 1 | observed: frontend/tests/components/recall/AnsweredSpellingQuestionAddAnswerAsAlias.spec.ts 'shows the done state and no action when the aliases already hold the answer' (state derives from the reviewed note's current content, as when the result is shown again) |
| C7 | Adding the alias leaves the grade of the answer just given as recorded (still wrong) | slice 1 | observed: AddSpellingAnswerAsAlias.vue calls only noteStore.updateTextField; no backend change; e2e_test/features/recall/recall_quiz_spelling_question.feature "Accept a wrong spelling answer as an alias" note due again on day 3 per the wrong answer's schedule |
| C8 | The next spelling recall of the note answered with the added alias is correct through the existing title-or-alias match | slice 1 | observed: e2e_test/features/recall/recall_quiz_spelling_question.feature "Accept a wrong spelling answer as an alias" day-3 recall answered 'sedation' → 'my last answer to spelling question is correct' |
| C9 | An accidental-match answer keeps the existing "names another note" result and resolve action, and shows no alias action | slice 2 |  |
| C10 | A declared-overlap answer that asks for a retry keeps the existing retry feedback and shows no alias action | slice 2 |  |
| C11 | A wrong answer on a note in a read-only notebook shows no alias action | slice 2 |  |
| C12 | An answer containing `/`, `\`, `:`, `#`, `^`, `[[`, `]]` or a line break shows no alias action | slice 2 |  |
| C13 | A blank answer shows no alias action | slice 2 |  |
| C14 | A correct answer shows no alias action | slice 2 |  |
