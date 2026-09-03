/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

Then(
  'I should see an accidental match reveal for spelling answer {string} with reviewed note {string} and matched note {string}',
  (answer: string, reviewedNoteTitle: string, matchedNoteTitle: string) => {
    start
      .assumeAnsweredQuestionPage()
      .expectAccidentalMatchReveal(answer, reviewedNoteTitle, matchedNoteTitle)
  }
)

Then('I should see an overlap try-again alert for spelling', () => {
  start.assumeAnsweredQuestionPage().expectOverlapTryAgainForSpelling()
})

Then(
  'I should not see matched notes or accidental match on the overlap result',
  () => {
    start
      .assumeAnsweredQuestionPage()
      .expectNoMatchedNotesOrAccidentalMatchOnOverlap()
  }
)

When('I try the spelling question again', () => {
  start.assumeAnsweredQuestionPage().trySpellingQuestionAgain()
})
