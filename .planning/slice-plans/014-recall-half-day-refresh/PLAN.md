# Plan 014: Refresh recall automatically at each local half-day boundary

## Source

- Story: [SEED-056#story-1](../../seeds/SEED-056-recall-half-day-refresh.md#story-1)
- **Identity:** SEED-056#story-1

## Goal and scope

A learner who keeps Donut open across local noon or midnight sees the current
recall count and gets every question now due, without reloading, and can trust
"done" to mean done.

Included:

- When the half-day window ends while Donut is open, fetch due questions for the
  new window on any page, without user action.
- Add newly due questions that are not already queued to the end of the queue;
  never reset position or replace, reorder, or hide the current question or an
  answer in progress. No blocking spinner.
- The menu recall count follows the queue.
- Returning to the recall page uses the same add-to-the-end rule (no queue
  replacement, no position reset).
- Questions loaded ahead (3/7/14 days) stay queued.

Excluded (from the story): half-day rule changes, other counts such as
assimilation, changes from another device. Also out of this plan: the explicit
full reload after assimilating or removing a tracker (`requestDueRecallsRefresh`)
stays a replacement, because removal must drop queued trackers.

## Key examples

1. Browsing notes before noon with 3 due, 2 more due at noon → after noon the
   menu shows 5 without a reload; browsing is not interrupted.
2. On the recall page answering question 2 of 4, 1 more due at midnight → the
   question and typed answer stay; the queue has 5 with the new one last.
3. Recall "done" before midnight, page left open → after midnight the page
   offers the newly due questions.
4. Leave the recall page before noon, return after noon → unanswered questions
   and position are kept; newly due ones are added at the end.

## Current decisions

- **One catch-up operation.** A single frontend operation fetches
  `RecallsController.recalling` with `dueindays: 0`, adds returned `toRepeat`
  trackers whose `memoryTrackerId` is not still waiting in the shared queue (at
  or after the current position; an answered tracker due again is added) to its
  end, and updates `dueCommissioned`, `totalAssimilatedCount`, and
  `currentRecallWindowEndAt`. It never touches the position or diligent mode.
  Both the recall page's return path and the boundary timer call it; they do
  not each own a variant.
- **No window comparison.** Because adding only missing trackers is harmless
  within the same half-day, the same-window check (`sameHalfDayWindow`,
  `dueQueueNeedsReload` in `useRecallPageLoading.ts`) is deleted, not adapted.
  Its reason (commit 8150ad3dbc: stop the return path from replacing the queue)
  is served by never replacing.
- **Timer owner.** The app shell's always-mounted main menu (it already loads
  menu data for a signed-in user) owns one timer scheduled at the stored
  `currentRecallWindowEndAt`, rescheduled whenever that value changes and
  cleared on unmount. A small margin after the window end tolerates a browser
  clock slightly ahead of the server; if the server still answers with the same
  window end, no reschedule happens (no fetch loop), and the next return to the
  recall page catches up. A browser that slept past the end fires on wake.

## Decisive premises (observed during planning)

| Premise | Observation | Result |
| --- | --- | --- |
| Crossing a boundary only adds due trackers | Read `RecallService.getDueMemoryTrackers` / `getMemoryTrackersNeedToRepeat` and `TimestampOperations.alignByHalfADay` | Due = next recall before the current window end; the window end is returned as `currentRecallWindowEndAt` |
| Menu data and the recall fetch both carry the window end | Read `MainMenu.vue` `fetchMenuData`, `useRecallPageLoading.ts` | Both set `currentRecallWindowEndAt` in the shared `useRecallData` state |
| Main menu is mounted on every page, including recall | Read `DonutApp.vue` | `MainMenu` sits outside `router-view`; `RecallPage` is kept alive |
| Appending does not disturb the current question | Read `RecallPage.vue`, `RecallPromptCard.vue` | Page renders from shared `toRepeat` plus its own index; the prompt card refetches only when the current tracker id changes (`watch(currentMemoryTrackerId)`) |
| "Done" turns back into a question when the queue grows | Read `RecallPage.vue` template | "finished all recalls" shows only while `toRepeatCount === 0`, computed from queue length minus index |
| Existing tests pin the replacement behavior | Searched `frontend/tests` for the window check | `RecallPage.dueQueue.spec.ts` "remounts toRepeat when reactivated after the due window actually rolled over" and "keeps toRepeat … same half-day" (fetched tracker not added) must change |
| E2E can cross a boundary | Read `e2e_test/start/testabilityTimeTravel.ts`, `recallPage.ts` | `cy.clock` mocks `Date`/`setTimeout`, `cy.tick` is already used, backend time travel exists. Browser/backend time-zone alignment at the exact boundary is not observed; slice 2 bounds it (see Proof) |

## Ordered slices

### 1. Returning to recall adds newly due questions without resetting the queue

Type: Behavior
Status: done
Accepted proof: `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vitest run tests/pages/RecallPage tests/toolbars/MainMenu`
(64 pass) and `vue-tsc --noEmit`; observed by "keeps the queue and position on
return and adds newly due trackers at the end" and "loads the due queue on
first activation when there is no queue yet". The operation is
`useRecallCatchUp().catchUpDueRecalls()` in
`frontend/src/composables/useRecallCatchUp.ts` (with the shared
`fetchDueRecalls`), a sibling of `useRecallData` so page specs that mock
`useRecallData` still run it. For slice 2: the test mock's
`setCurrentRecallWindowEndAt` in `recallPageTestSupport.ts` is a bare `vi.fn()`
that does not write its ref.
Proof: `frontend/tests/pages/RecallPage.dueQueue.spec.ts` (KeepAlive
activation): with a queue of trackers 1–2 and position on tracker 2, returning
after the window changed with a fetch of trackers 2–3 leaves the queue as 1, 2,
3 and the position on tracker 2; the first-activation load with no queue still
loads the fetched list. Run `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vitest run tests/pages/RecallPage.dueQueue.spec.ts` (observed green on the current code during planning: 9 tests).

Behavior: learner has a recall queue and leaves the recall page → returns after
new questions became due → queue keeps its order and position and gains the new
ones at the end (example 4). Introduces the one catch-up operation in shared
recall state and deletes the window comparison. The two existing tests above are
rewritten to the new rule, not kept alongside it.

### 2. Recall catches up by itself when the half-day ends

Type: Behavior
Status: done
Accepted proof: `CURSOR_DEV=true nix develop -c pnpm -C frontend build` (its
`vue-tsc` caught a slice-1 test type error that CI also caught; slice 1's
typecheck had been piped into `tail`, so its reported exit code was `tail`'s), `… vitest run tests/pages/RecallPage tests/toolbars/MainMenu`
(65 pass), and `SUT_TIMEOUT_MS=360000 … pnpm cy:run --spec
e2e_test/features/recall/spaced_repetition.feature` (4 pass; the new scenario
failed with the timer callback disabled, showing "finished all recalls").
Repair to slice 1 found here: deduplicating against the whole queue hid an
answered tracker that came due again ("Strictly follow the schedule" failed);
deduplication now covers only waiting trackers. Time-zone note: on a +08
machine the mocked browser clock is ahead of the backend, so the timer fires
on the scenario's first `cy.tick`; the scenario stays deterministic because
nothing ticks before the full-day tick.
Proof:
- E2E, a new scenario in `e2e_test/features/recall/spaced_repetition.feature`
  (`@mockBrowserTime`): assimilate a note on day 1 morning; browser and backend
  on day 1 evening; on the recall page see "finished all recalls"; move the
  backend past day 2's due time and `cy.tick` the browser clock past the window
  end without reloading → the note's question appears and the menu recall count
  shows 1 (examples 1 and 3). Run it red first: it must fail because no catch-up
  happens, not because of harness timing. If the browser/backend time-zone
  offset makes the timer fire outside the ticked span, tick a full day rather
  than adding timezone machinery; if that still cannot be made deterministic,
  stop and replan the proof.
- Focused frontend test on the main menu with fake timers: before the stored
  window end no fetch happens; after it the catch-up runs once and the count
  updates; an unchanged window end in the reply schedules nothing new.
- Example 2 (question in progress untouched) is owned by slice 1's operation
  proof plus this slice's proof that the timer calls the same operation.

Behavior: Donut open on any page as the half-day ends → without user action the
queue gains newly due questions and the menu count updates; the current
question, answer, and position are untouched; no spinner.

## Proof ownership

| Promise | Slice | Observation |
| --- | --- | --- |
| Refresh at boundary without user action, any page | 2 | E2E tick + main menu timer test |
| Newly due added at end; current question/position untouched | 1 (operation), 2 (timer uses it) | dueQueue spec; menu timer test |
| Menu count matches | 2 | E2E count assertion |
| "Done" turns into newly due questions | 2 | E2E |
| Return to recall page uses the same rule | 1 | dueQueue spec |
| Questions loaded ahead stay queued | 1 | Naturally held by "add only missing"; covered by the kept-queue assertion, no separate test |

## Execution complete

Product advice: no backlog change. The recall queue is no longer replaced at a
half-day boundary, so the page's "Recalling x/y" count now includes questions
answered earlier in the same open session; this follows the story's "never
reset position" promise. Watch whether learners find the growing total
confusing before proposing a counter change. The main menu's first menu-data
load still fills the queue only when it is empty; it coincides with catch-up at
sign-in and needs no correction now.
