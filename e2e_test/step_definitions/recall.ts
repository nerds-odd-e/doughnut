/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import { commonSenseSplit } from 'support/string_util'
import start from '../start'
import { assumeMemoryTrackerPage } from '../start/pageObjects/memoryTrackerPage'

function assertAssimilationDueOnDay(day: number, toAssimilateAndTotal: string) {
  start.testability().backendTimeTravelTo(day, 8)
  start.assimilation().expectAssimilationDueFromTriple(toAssimilateAndTotal)
}

Given('the browser and backend are on day {int}', (day: number) => {
  start.testability().timeTravelTo(day, 8)
})

Given("It's day {int}, {int} hour", (day: number, hour: number) => {
  start.testability().backendTimeTravelTo(day, hour)
})

Given("It's day {int}", (day: number) => {
  start.testability().backendTimeTravelTo(day, 8)
})

Given('I ask to do more recall', () => {
  start.recall().navigateToRecallPage().repeatMore()
})

Then('I recall {string}', (repeatNotes: string) => {
  start.recall().navigateToRecallPage().recallNotes(repeatNotes)
})

Then('I repeat more old {string}', (repeatNotes: string) => {
  start.recall().assumeRecallPage().recallNotes(repeatNotes)
})

Then(
  'On day {int} I recall {string} and assimilate new {string}',
  (day: number, repeatNotes: string, initialNotes: string) => {
    start.testability().timeTravelTo(day, 8)
    start.recall().navigateToRecallPage().recallNotes(repeatNotes)
    commonSenseSplit(initialNotes, ', ').forEach((title) => {
      if (title !== 'end') {
        start.testability().assimilateNote(title)
      }
    })
  }
)

Then(
  'I should see that I have {int} notes to recall',
  (numberOfNotes: number) => {
    cy.reload()
    start.recall().expectCount(numberOfNotes)
  }
)

Then(
  'On day {int} I should have {string} note for assimilation and {string} for recall',
  (day: number, toAssimilateAndTotal: string, numberOfRecalls: string) => {
    assertAssimilationDueOnDay(day, toAssimilateAndTotal)
    start.recall().expectRecallProgressFromTriple(numberOfRecalls)
  }
)

Then(
  'I should have {string} note for assimilation and {string} for recall',
  (toAssimilateAndTotal: string, numberOfRecalls: string) => {
    start.assimilation().expectAssimilationDueFromTriple(toAssimilateAndTotal)
    start.recall().assumeRecallPage().expectToRecallCounts(numberOfRecalls)
  }
)

Then(
  'On day {int} I should have {string} note for assimilation',
  (day: number, toAssimilateAndTotal: string) => {
    assertAssimilationDueOnDay(day, toAssimilateAndTotal)
  }
)

Then('I am recalling my note on day {int}', (day: number) => {
  start.testability().backendTimeTravelTo(day, 8)
  cy.reload()
  start.recall().visitRecallPage()
})

When('I visit recall for a due quiz question on day {int}', (day: number) => {
  start.testability().backendTimeTravelTo(day, 8)
  start.recall().visitRecallPageAndWaitForQuestion()
})

When(
  'I make {int} wrong answers over {int} days since day {int}, answering {string} to {string}',
  (
    _numWrongAnswers: number,
    numDays: number,
    startDay: number,
    wrongAnswer: string,
    questionStem: string
  ) => {
    const days = Array.from({ length: numDays }, (_, i) => startDay + i)

    const submitWrongAnswerForDay = (index: number) => {
      if (index >= days.length) {
        return
      }
      const day = days[index]!
      const isLast = index === days.length - 1
      start.testability().backendTimeTravelTo(day, 8)
      if (isLast) {
        start.recall().visitRecallPage()
        start.assumeQuestionPage(questionStem).answer(wrongAnswer)
        start
          .assumeAnsweredQuestionPage()
          .expectMCQAnswerToBeIncorrect(wrongAnswer)
      } else {
        start.testability().submitWrongMcqRecallAnswer(wrongAnswer)
      }
      cy.then(() => submitWrongAnswerForDay(index + 1))
    }
    submitWrongAnswerForDay(0)
  }
)

Then('I choose yes I remember', () => {
  start.recall().assumeRecallPage().yesIRemember()
})

Then('I skip one question', () => {
  start.assumeQuestionPage().skipQuestion()
})

Then(
  'I should be asked spelling question {string} from notebook {string}',
  (question: string, notebook: string) => {
    start.assumeQuestionPage(question).forNotebook(notebook)
  }
)

Then('I type my answer {string}', (answer: string) => {
  start.recall().assumeRecallPage().typeSpellingAnswer(answer)
})

Then('I choose answer {string}', (answer: string) => {
  start.assumeQuestionPage().answer(answer)
})

Then(
  'I should see that my MCQ answer {string} is incorrect',
  (answer: string) => {
    start.assumeAnsweredQuestionPage().expectMCQAnswerToBeIncorrect(answer)
  }
)

Then(
  'I should see that my spelling answer {string} is incorrect',
  (answer: string) => {
    start.assumeAnsweredQuestionPage().expectSpellingAnswerToBeIncorrect(answer)
  }
)

Then(
  'I should see an accidental match reveal for spelling answer {string} with reviewed note {string} and matched note {string}',
  (answer: string, reviewedNoteTitle: string, matchedNoteTitle: string) => {
    start
      .assumeAnsweredQuestionPage()
      .expectAccidentalMatchReveal(answer, reviewedNoteTitle, matchedNoteTitle)
  }
)

When(
  'I link the matched note {string} as a wiki property from the accidental match result',
  (matchedNoteTitle: string) => {
    start
      .assumeAnsweredQuestionPage()
      .linkMatchedNoteAsProperty(matchedNoteTitle)
  }
)

When(
  'I link the matched note {string} as relationship {string} from the accidental match result',
  (matchedNoteTitle: string, relationType: string) => {
    start
      .assumeAnsweredQuestionPage()
      .linkMatchedNoteAsRelationship(matchedNoteTitle, relationType)
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

Then('I should see that my answer is correct as the last question', () => {
  start
    .assumeAnsweredQuestionPage()
    .goToLastAnsweredQuestion()
    .expectMCQAnswerToBeCorrect()
})

Then('I should see that my answer is correct', () => {
  // leave empty for now
})

Then('I should see that my last answer to spelling question is correct', () => {
  start
    .assumeAnsweredQuestionPage()
    .goToLastAnsweredQuestion()
    .expectSpellingAnswerToBeCorrect()
})

Then(
  'I should see spelling memory tracker recall count {int} for the last answer',
  (count: number) => {
    start
      .assumeAnsweredQuestionPage()
      .goToLastAnsweredQuestion()
      .viewMemoryTracker()
    assumeMemoryTrackerPage().expectRecallCount(count).expectSpellingEnabled()
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
    assumeMemoryTrackerPage().expectRecallCount(count).expectSpellingEnabled()
  }
)

Then('choose to remove the last memory tracker from recalls', () => {
  start
    .assumeAnsweredQuestionPage()
    .goToLastAnsweredQuestion()
    .viewMemoryTracker()
    .removeFromRecall()
})

When('I revive the memory tracker on this page', () => {
  assumeMemoryTrackerPage().reviveMemoryTracker()
})

Then('I should see the resume recall menu item', () => {
  cy.findByLabelText('Resume').should('exist')
})

When('I click resume recall from the menu', () => {
  cy.findByLabelText('Resume').click()
})

Then('I should be back to the current question', () => {
  start.recall().assumeRecallPage().expectCurrentQuestion()
})

Then('I should be asked {string}', (expectedQuestionStem: string) => {
  start.assumeQuestionPage(expectedQuestionStem)
})

When('I confirm re-assimilation', () => {
  start.assumeAnsweredQuestionPage().confirmReAssimilation()
})
