/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'
import { assumeMemoryTrackerPage } from '../start/pageObjects/memoryTrackerPage'

// The assimilation page labels understanding tracker rows as 'normal'
// (NoteInfoMemoryTracker.vue trackerTypeLabel), so the 'understanding' kind
// maps to the 'normal' row label when opening the note-level tracker.
function openNoteLevelTracker(
  noteTitle: string,
  kind: 'understanding' | 'spelling'
) {
  return start
    .jumpToNotePage(noteTitle)
    .openAssimilationSettings()
    .openNoteLevelMemoryTracker(kind === 'spelling' ? 'spelling' : 'normal')
}

Then(
  'I should see the incorrect grade time as Last Recall Time for my last answer',
  () => {
    start
      .assumeAnsweredQuestionPage()
      .viewMemoryTracker()
      .expectLastRecallTimeTwelveHoursBeforeNextRecall()
  }
)

Then(
  'I should see Stability {int} after the incorrect recall',
  (stability: number) => {
    assumeMemoryTrackerPage().expectStability(stability)
  }
)

Then(
  'I should see Difficulty {int} after the incorrect recall',
  (difficulty: number) => {
    assumeMemoryTrackerPage().expectDifficulty(difficulty)
  }
)

Then('I should see 12 hours between last and next recall', () => {
  assumeMemoryTrackerPage().expectLastRecallTimeTwelveHoursBeforeNextRecall()
})

Then(
  'I should see that my last spelling answer was correct with recall count {int}',
  (count: number) => {
    start
      .assumeAnsweredQuestionPage()
      .goToLastAnsweredQuestion()
      .expectSpellingAnswerToBeCorrect()
      .viewMemoryTracker()
    assumeMemoryTrackerPage()
      .expectRecallCount(count)
      .expectTrackerType('SPELLING')
  }
)

Then(
  'I should see the understanding memory tracker with recall count {int}',
  (count: number) => {
    assumeMemoryTrackerPage()
      .expectRecallCount(count)
      .expectTrackerType('UNDERSTANDING')
  }
)

Then('I record the current memory tracker schedule', () => {
  assumeMemoryTrackerPage().captureSchedule()
})

When(
  'I visit the understanding memory tracker for {string}',
  (noteTitle: string) => {
    openNoteLevelTracker(noteTitle, 'understanding')
  }
)

When(
  'I visit the spelling memory tracker for {string}',
  (noteTitle: string) => {
    openNoteLevelTracker(noteTitle, 'spelling')
  }
)

Then(
  'I record the current memory tracker schedule for {string}',
  (noteTitle: string) => {
    assumeMemoryTrackerPage().captureSchedule(noteTitle)
  }
)

Then(
  'the spelling memory tracker for {string} should keep its recorded schedule',
  (noteTitle: string) => {
    openNoteLevelTracker(noteTitle, 'spelling').expectScheduleUnchanged(
      noteTitle
    )
  }
)

Then(
  'the spelling memory tracker for {string} should be brought forward without recall credit',
  (noteTitle: string) => {
    openNoteLevelTracker(
      noteTitle,
      'spelling'
    ).expectBroughtForwardWithoutRecallCredit()
  }
)

Then(
  'the understanding memory tracker for {string} should be brought forward without recall credit',
  (noteTitle: string) => {
    openNoteLevelTracker(
      noteTitle,
      'understanding'
    ).expectBroughtForwardWithoutRecallCredit()
  }
)

When('I choose to remove the last memory tracker from recalls', () => {
  start
    .assumeAnsweredQuestionPage()
    .goToLastAnsweredQuestion()
    .viewMemoryTracker()
    .removeFromRecall()
})

When('I revive the memory tracker on this page', () => {
  assumeMemoryTrackerPage().reviveMemoryTracker()
})

Then('the memory tracker should be available for recall again', () => {
  assumeMemoryTrackerPage().expectAvailableForRecall()
})
