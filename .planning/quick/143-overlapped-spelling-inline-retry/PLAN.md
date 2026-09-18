# Inline retry for overlapped spelling matches

Status: planned
Source: [SEED-032 story 1](../../seeds/SEED-032-retry-overlapped-spelling-match.md#story-1).
Authority: 2026-09-18 owner request to record the removal constraint and write,
then refine if necessary, a slice plan. Planning only — this plan does not
authorize execution.

Baseline: `52f1fbf2da857ff6b03c467e48a4d1765d02345b` on `main`.

Execution started 2026-09-18. Mode: Story Branch Mode.

- Execution checkout/branch: worktree
  `.claude/worktrees/143-overlapped-spelling-inline-retry` on branch
  `143-overlapped-spelling-inline-retry`, created from
  `d5065f82d7` (Take SEED-032 story 1 for execution) on `main`.
- Integration: `main` in the originating checkout; remote `origin`
  (`nerds-odd-e/doughnut`). Push destination for this execution:
  `origin/143-overlapped-spelling-inline-retry`.
- CI observer: GitHub Actions, workflow `ci.yml` ("donut CI"), target branch
  `143-overlapped-spelling-inline-retry`; mailbox `/tmp/dough-ci-501/watch-5LSmbf`.
  Armed before slice 1 delegation via `.agents/skills/...` (the `.claude/skills/...`
  path lacks the runtime scripts in this checkout).

## Goal and scope

When a learner submits a spelling answer that matches a note already declared
as overlapping the focus note, Donut keeps the learner on the same unanswered
question. It explains that the answer matches an overlapped note but differs
from the expected answer, clears the submitted text, and focuses the answer
input so the learner can immediately try a more specific answer.

The focus note and matched note receive no Grade, Confusion, or other
memory-state transition. This is existing backend policy and remains unchanged.

Included:

- handle the existing `OVERLAP` response as an immediate continuation of the
  active spelling question rather than as an answered-question reveal;
- show: “Your answer matches an overlapped note, but that’s different from the
  expected answer” on that question;
- reconstruct or retain the same question with an empty, focused answer input;
- delete the overlap-only reveal message, “Try again” button, retry event and
  click-driven transition;
- delete tests and assertions that existed to verify those removed elements or
  interactions, without replacing them with absence assertions.

Excluded:

- changing accidental-match behavior when no declared overlap applies;
- changing overlap authoring, resolution, or wiki-link matching rules;
- changing the backend `OVERLAP` outcome, answer persistence, API shape, or
  ADR 0003 scheduling policy;
- adding negative tests for the deleted reveal or button.

## Key examples and constraints

1. With `colour` under recall and `Partner` declared as an overlap, submitting
   `Partner` shows the same `means a hue` question with the overlap explanation
   and an empty, focused spelling input; submitting `colour` then receives
   ordinary correct credit.
2. The same flow holds when an alias such as `hue` matches the overlapped note
   and the later answer uses the focus note's alias `color`.
3. For either overlap answer, both notes' scheduling state and recall logs stay
   unchanged. Existing controller tests own this policy proof.
4. Ordinary incorrect answers and undeclared accidental matches retain their
   current flows. This is a scope boundary, not a request for new assertions
   that removed overlap UI is absent.

## Existing solutions and selected design

| Responsibility | Existing owner / evidence | Choice |
| --- | --- | --- |
| Recognize a declared-overlap answer | `SpellingRecallGrading.answerSpelling` resolves authored overlap links, returns `AnswerOutcome.OVERLAP`, and exits before Grade or Confusion | Reuse unchanged. Do not add another overlap recognizer or frontend matching rule. |
| Preserve both notes' memory state | `RecallPromptOverlapTryAgainTests` proves the focus schedule/logs, partner schedule/logs, and failure count remain unchanged | Keep these positive state-policy proofs. No backend production change is expected. |
| Decide how an answer response changes the recall page | `useRecallAnswerHandling.onAnswered` currently turns `OVERLAP` into a previous answered question | Change this existing branch to continue the active question and provide its feedback. |
| Reconstruct a fresh spelling attempt | `spellingRetryNonce` keys `SpellingQuestionDisplay`; changing it remounts the display, refetches the next unanswered prompt for the same tracker, clears component-owned input, and reapplies the existing focus behavior | Reuse the remount mechanism immediately on the `OVERLAP` response; remove the later button click that currently triggers it. |
| Display the active question and own its input | `SpellingQuestionDisplay` owns the stem, form, `spellingAnswer`, and existing focus behavior | Give this active-question surface the overlap feedback; do not render overlap feedback through `AnsweredSpellingQuestion`. |
| Reveal a completed spelling answer | `AnsweredSpellingQuestion` renders the focus note and presently carries overlap-only warning/button branches | Delete only its overlap-specific presentation and retry event. Retain ordinary wrong-answer and accidental-match responsibilities. |

This changes the existing owners rather than introducing a second retry or
overlap representation. The short-term Git publication North Star topics do
not govern this local recall interaction.

### Accepted decision carried

[ADR 0003 — Spaced-repetition scheduling policy](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md)
defines declared Overlap as neither a Grade nor a memory-state transition and
says the learner retries with a more specific answer. The plan changes the UI
to express that accepted policy directly; it does not alter the decision.

## Outside-in proof ownership

| Promise | Owning proof |
| --- | --- |
| Same question, explanatory prompt, cleared input, and focused input after a title or alias overlap | Update `e2e_test/features/recall/overlap_try_again.feature` to proceed directly from the overlap submission to these positive signals and then submit the expected answer. |
| Page response stays on the current tracker and reconstructs the active spelling question immediately | Replace the old button-driven case in `frontend/tests/pages/RecallPageOverlap.spec.ts` with a positive page-level test of the prompt and fresh, focused input. Delete the old test first; do not retain assertions about the deleted answered view or button. |
| Both note schedules/logs remain unchanged | Reuse `RecallPromptOverlapTryAgainTests`; do not duplicate scheduling assertions in frontend or E2E tests. |
| Correct specific retry still receives credit | The final steps of the updated overlap E2E scenario exercise the ordinary correct-answer path. |

The new E2E and page tests should assert only the desired active-question
signals. They must not assert that the old reveal, “Try again” button, or
resolve UI is missing.

## Ordered slices

### Slice 1 — Continue the active spelling question after an overlap

Type: Behavior
Status: planned

Behavior: Given a spelling question with a declared overlapping note, when the
learner submits the overlap's title or alias, the same question is ready for
another answer with the overlap explanation, an empty value, and focus in the
answer input, while both notes' memory state remains unchanged.

Implementation and deletion boundary:

1. Change the overlap E2E scenario and page-object vocabulary from the
   answered-page alert/button sequence to the new active-question prompt,
   empty-input and focused-input signals. Run the feature to establish the
   expected failing proof before changing production code.
2. In the existing recall answer-handling flow, retain the current tracker,
   store the overlap feedback for the active spelling question, and advance the
   existing spelling remount key immediately. Pass the feedback through
   `RecallPromptCard` to `SpellingQuestionDisplay`; render it alongside the
   question form. Clear it when an answer leaves this overlap-retry flow so it
   cannot carry to another tracker.
3. Delete the overlap-only branch, message, button, `retry` event, and page
   wiring from `AnsweredSpellingQuestion`, `RecallPage`, and
   `useRecallAnswerHandling` where they no longer own behavior.
4. Delete `AnsweredSpellingQuestionOverlap.spec.ts` and the old button-driven
   page test. Add the positive page-level proof described above. Remove the
   obsolete `overlap-try-again` absence assertion from
   `AnsweredSpellingQuestionAddAsOverlapped.spec.ts`; do not replace any of
   these with negative assertions.
5. Remove the obsolete E2E answered-page helper, step definitions, and selectors
   for viewing the overlap alert and clicking “Try again.” Keep accidental-match
   reveal helpers intact.

Proof loop:

- First run the changed E2E feature and observe it fail at the new active-page
  signal before implementation:
  `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/overlap_try_again.feature`
- During implementation, run the focused page spec:
  `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/RecallPageOverlap.spec.ts`
- After the behavior is green, run the full frontend unit suite:
  `CURSOR_DEV=true nix develop -c pnpm frontend:test`
- Re-run the overlap E2E feature with the same command and both example rows.
- Verify the unchanged scheduling policy through the full backend unit suite,
  as required for backend tests:
  `CURSOR_DEV=true nix develop -c pnpm backend:test_only`

The slice is one coherent proof loop. Its focused E2E startup may exceed the
five-minute target, but splitting UI deletion from the new behavior would create
an unsafe intermediate state or separate tests from the behavior they prove.

## Completion gates for later authorized execution

- Apply the required post-change refactor pass to the implicated answer-flow and
  presentation concepts; it must preserve the single active-question behavior
  and the explicit deletion constraint.
- No API generation is expected because the backend contract is unchanged. If
  execution discovers that the contract must change, stop and refine this plan
  before generating the client.
- The coordinator runs `./scripts/run.sh pnpm format:changed` once after edits
  and refactoring, then follows the repository's commit, push, CI observation,
  and repair workflow.

## Current decisions

- The backend owns overlap recognition and memory-state preservation; the
  frontend owns presenting the returned outcome as an immediate continued
  attempt.
- Reconstructing the question through the existing remount/refetch path is
  acceptable because the story explicitly permits retaining or reconstructing
  the page, and this path already gives a fresh input and focus behavior.
- Removal means deletion, not preservation through hidden UI, disabled controls,
  compatibility branches, or negative absence tests.

## Learnings

- The existing backend already returns before scheduling either tracker and has
  controller-level proofs for the focus tracker, partner tracker, recall logs,
  and failure count.
- The current frontend already has the required same-tracker remount mechanism;
  only its trigger is misplaced behind the answered-note reveal and button.
