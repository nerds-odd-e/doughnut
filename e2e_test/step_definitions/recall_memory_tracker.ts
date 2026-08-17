/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'
import { assumeMemoryTrackerPage } from '../start/pageObjects/memoryTrackerPage'
import type { NoteLevelTrackerKind } from '../start/pageObjects/assimilationPage/shared'

function openNoteLevelTracker(noteTitle: string, kind: NoteLevelTrackerKind) {
  return start
    .jumpToNotePage(noteTitle)
    .openAssimilationSettings()
    .openNoteLevelMemoryTracker(kind)
}

Then(
  'I should see a GOOD RecallLog with elapsed hours and no answer id',
  () => {
    assumeMemoryTrackerPage().expectGoodRecallLogWithoutAnswer()
  }
)

Then('I should see an AGAIN RecallLog', () => {
  assumeMemoryTrackerPage().expectAgainRecallLog()
})

Then('I should see Stability {int}', (stability: number) => {
  assumeMemoryTrackerPage().expectStability(stability)
})

Then('I should see Difficulty {float}', (difficulty: number) => {
  assumeMemoryTrackerPage().expectDifficulty(difficulty)
})

Then(
  'I should see {int} hours between last and next recall',
  (hours: number) => {
    assumeMemoryTrackerPage().expectHoursBetweenLastAndNextRecall(hours)
  }
)

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
  'I visit the commissioned memory tracker for {string}',
  (noteTitle: string) => {
    openNoteLevelTracker(noteTitle, 'commissioned')
  }
)

Given(
  'I credited a spelling recall of {string} and recorded its schedule',
  (noteTitle: string) => {
    start
      .testability()
      .creditSpellingRecallForNote(noteTitle)
      .then(() => start.testability().captureSpellingTrackerSchedule(noteTitle))
  }
)

Given('I recorded the spelling schedule of {string}', (noteTitle: string) => {
  start.testability().captureSpellingTrackerSchedule(noteTitle)
})

Then(
  'the spelling schedule of {string} should be unchanged',
  (noteTitle: string) => {
    start.testability().expectSpellingTrackerScheduleUnchanged(noteTitle)
  }
)

Then(
  'the spelling schedule of {string} should be brought forward without recall credit',
  (noteTitle: string) => {
    start
      .testability()
      .expectSpellingTrackerBroughtForwardWithoutRecallCredit(noteTitle)
  }
)

Given(
  'I marked the understanding tracker for {string} as recalled successfully',
  (noteTitle: string) => {
    start.testability().markUnderstandingTrackerRecalledSuccessfully(noteTitle)
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
