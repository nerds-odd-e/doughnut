# Show read-only note content after every spelling answer

## Source

- Story: [Show read-only note content after every spelling answer](../../seeds/SEED-052-spelling-answer-note-content.md#story-1)
- **Identity:** SEED-052#story-1

## Goal and scope

A spelling result shows the read-only note content inside the
note-under-question area for every outcome: correct, wrong, and an accidental
match. The **Show note context** button on a correct result and the
reveal-on-demand state behind it are removed.

Excluded (see the seed): any change to the recall flow (a correct answer still
moves on to the next question; its result is seen through **view last answered
question**), the recall history view, and non-spelling answered-question pages.

## Approach

One rule, no outcome branch: `AnsweredSpellingQuestion.vue` always renders
`NoteContextReader` inside `NoteUnderQuestion`. Delete the `showNoteContext`
computed, the `noteContextRevealed` ref, the `v-if` on the reader, and the
**Show note context** button. The wrong-answer and accidental-match paths
already render the same reader, so they are unchanged.

No North Star topic or ADR governs spelling result presentation; the change
stays inside one component.

Expected complexity delta: about −12 lines of product code, one conditional
and one piece of local state removed; no lines added.

## Decisive premises

| Premise | Observation | Result |
| --- | --- | --- |
| Only a correct result hides the note context, through one local flag | Read `frontend/src/components/recall/AnsweredSpellingQuestion.vue` | `showNoteContext = !answer.correct \|\| noteContextRevealed`; the button sets `noteContextRevealed` |
| The button exists nowhere else | `grep -rn "Show note context" --exclude-dir=node_modules .` | Only `AnsweredSpellingQuestion.vue:48` and `AnsweredSpellingQuestion.spec.ts:55` (plus this story's seed) |
| `AnsweredSpellingQuestion` has one product caller | `grep -rln AnsweredSpellingQuestion frontend/src` | Only `frontend/src/pages/RecallPage.vue` |
| A correct answer does not stop on its result (the flow stays as is) | Read `frontend/src/composables/useRecallAnswerHandling.ts` | `viewLastAnsweredQuestion` runs only when `!answerResult.answer?.correct` |
| The correct-answer E2E scenario already opens the correct result, and a note-context step exists | Read `e2e_test/features/recall/recall_quiz_spelling_question.feature` and `e2e_test/step_definitions/recall.ts:133,146` | "Spelling quiz accepts a correct answer" ends with a step that runs `goToLastAnsweredQuestion()`; "I should see the reviewed note context containing {string}" checks the `Note context` article |
| Focused specs are green on trunk | `CURSOR_DEV=true nix develop -c bash -c 'cd frontend && pnpm vitest run tests/components/recall/AnsweredSpellingQuestion.spec.ts tests/components/recall/AnsweredSpellingQuestionAccidentalMatchNoteContext.spec.ts tests/pages/RecallPage.spelling.spec.ts'` | 3 files, 7 tests passed |

## Proof ownership

| Promise | Slice | Proof |
| --- | --- | --- |
| Correct result shows the note content without a click | 1 | E2E: "Spelling quiz accepts a correct answer" gains `And I should see the reviewed note context containing "Sedition means incite violence"`; unit: the correct test in `AnsweredSpellingQuestion.spec.ts` asserts the note context text on mount |
| Wrong result still shows it | 1 | Unchanged: E2E "Spelling quiz reveals the reviewed note after a wrong answer" and `RecallPage.spelling.spec.ts` stay green |
| Accidental match still shows it | 1 | Unchanged: `AnsweredSpellingQuestionAccidentalMatchNoteContext.spec.ts` stays green |
| Button and its state removed | 1 | Code deletion; per the owner's rule no test checks for the button's absence |

## Slices

### 1. Every spelling result shows the note content

Type: Behavior
Status: done
Proof: the E2E scenario and the rewritten unit test in the table above, then
`CURSOR_DEV=true nix develop -c bash -c 'cd frontend && pnpm vitest run tests/components/recall/AnsweredSpellingQuestion.spec.ts tests/components/recall/AnsweredSpellingQuestionAccidentalMatchNoteContext.spec.ts tests/pages/RecallPage.spelling.spec.ts'`
and `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/recall/recall_quiz_spelling_question.feature`.

Behavior: a learner answered a spelling question correctly and recall moved
on → they open the result with **view last answered question** → it shows
"Correct!" and the note content containing "Sedition means incite violence",
with no extra click.

Steps: add the E2E line (red), rewrite the unit test "keeps the result brief
and reveals the note context on demand" into "shows the note context for a
correct answer" without the button click (red), then remove the flag, the
`v-if`, and the button (green).

Accepted proof: focused vitest (3 files, 7 tests), `vue-tsc --noEmit`, and the
spelling E2E feature (2 passing) all green; the correct-answer E2E and the
rewritten unit test observe the note context without a click.

Learning: `RecallPage.spelling.spec.ts` "focuses the spelling answer input when
resuming recall" uses a correct answered question, so its `showNote` mock now
matches that question's note because the reader loads for every outcome.

## Execution complete

Product advice: no change — the backlog order stands; the next queued stories
(SEED-053#story-1, SEED-051#story-1) are unaffected by this change.
