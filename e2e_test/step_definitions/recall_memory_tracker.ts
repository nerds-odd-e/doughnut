/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'
import { assumeMemoryTrackerPage } from '../start/pageObjects/memoryTrackerPage'

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
