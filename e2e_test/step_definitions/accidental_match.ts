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

When(
  'I add the matched note {string} as a wiki property from the accidental match result',
  (matchedNoteTitle: string) => {
    start
      .assumeAnsweredQuestionPage()
      .linkMatchedNoteAsProperty(matchedNoteTitle)
  }
)

When(
  'I add the matched note {string} as relationship {string} from the accidental match result',
  (matchedNoteTitle: string, relationType: string) => {
    start
      .assumeAnsweredQuestionPage()
      .linkMatchedNoteAsRelationship(matchedNoteTitle, relationType)
  }
)

When(
  'I add the matched note {string} as overlapped from the accidental match result',
  (matchedNoteTitle: string) => {
    start.assumeAnsweredQuestionPage().openAddAsOverlappedNote(matchedNoteTitle)
  }
)

Then(
  'I should still be on the accidental match result for spelling answer {string} with matched note {string}',
  (answer: string, matchedNoteTitle: string) => {
    start
      .assumeAnsweredQuestionPage()
      .expectStillOnAccidentalMatchResult(answer, matchedNoteTitle)
  }
)

Then(
  'I should not see overlap try-again on the accidental match result',
  () => {
    start
      .assumeAnsweredQuestionPage()
      .expectNoOverlapTryAgainOnAccidentalMatchResult()
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

When(
  'I open resolve and navigate to matched note {string}',
  (matchedNoteTitle: string) => {
    start
      .assumeAnsweredQuestionPage()
      .openResolveDialog()
      .clickMatchedNoteTitle(matchedNoteTitle)
  }
)

When('I go back to the recall result', () => {
  start.assumeAnsweredQuestionPage().goBackToRecallResult()
})

Then(
  'I should see resolve available again for spelling answer {string} with matched note {string}',
  (answer: string, matchedNoteTitle: string) => {
    start
      .assumeAnsweredQuestionPage()
      .expectResolveAvailableAgainWithMatch(answer, matchedNoteTitle)
  }
)
