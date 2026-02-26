/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import type { DataTable } from '@cucumber/cucumber'
import start from '../start'

Given("It's day {int}, {int} hour", (day: number, hour: number) => {
  start.testability().backendTimeTravelTo(day, hour)
})

Given("It's day {int}", (day: number) => {
  start.testability().backendTimeTravelTo(day, 8)
})

Given('I ask to do more repetition', () => {
  start.recall().goToRecallPage().repeatMore()
})

Then('I recall {string}', (repeatNotes: string) => {
  start.recall().goToRecallPage().recallNotes(repeatNotes)
})

Then('I repeat more old {string}', (repeatNotes: string) => {
  start.recall().assumeRecallPage().recallNotes(repeatNotes)
})

Then(
  'On day {int} I recall {string} and assimilate new {string}',
  (day: number, repeatNotes: string, initialNotes: string) => {
    start.testability().timeTravelTo(day, 8)
    start.recall().goToRecallPage().recallNotes(repeatNotes)
    start.assimilation().goToAssimilationPage().assimilateNotes(initialNotes)
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
    start.testability().backendTimeTravelTo(day, 8)
    start
      .assimilation()
      .goToAssimilationPage()
      .expectToAssimilateAndTotal(toAssimilateAndTotal)
    start.recall().goToRecallPage().expectToRecallCounts(numberOfRecalls)
  }
)

Then('I am recalling my note on day {int}', (day: number) => {
  start.testability().backendTimeTravelTo(day, 8)
  cy.reload()
  start.recall().goToRecallPage()
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

    const runIteration = (index: number) => {
      if (index >= days.length) return
      const day = days[index]!
      start.testability().backendTimeTravelTo(day, 8)
      cy.reload()
      start.recall().goToRecallPage()
      start.assumeQuestionPage(questionStem).answer(wrongAnswer)
      start
        .assumeAnsweredQuestionPage()
        .expectMCQAnswerToBeIncorrect(wrongAnswer)
      cy.then(() => runIteration(index + 1))
    }
    runIteration(0)
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
  'I should see the memory tracker info of note {string}',
  (noteTopology: string, data: DataTable) => {
    start
      .assumeAnsweredQuestionPage()
      .showMemoryTracker(noteTopology)
      .expectMemoryTrackerInfo(data.hashes())
  }
)

Then('choose to remove the last memory tracker from recalls', () => {
  start
    .assumeAnsweredQuestionPage()
    .goToLastAnsweredQuestion()
    .showMemoryTracker()
    .removeMemoryTrackerFromRecall('normal')
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

When(
  'I have the true false question {string} rated as a good example',
  (questionStem: string) => {
    start.testability().injectSuggestedQuestion(questionStem, true)
  }
)

When(
  'I have the true false question {string} rated as a bad example',
  (questionStem: string) => {
    start.testability().injectSuggestedQuestion(questionStem, false)
  }
)

Then('I should be asked {string}', (expectedQuestionStem: string) => {
  start.assumeQuestionPage(expectedQuestionStem)
})

Then(
  'I suggest the question {string} of the note {string} as a good example',
  (questionStem: string, noteTopology: string) => {
    start
      .jumpToNotePage(noteTopology)
      .openQuestionList()
      .suggestingQuestionForFineTuning(questionStem)
      .suggestingPositiveFeedbackForFineTuning()
  }
)

Then(
  'I suggest the question {string} of the note {string} as a bad example',
  (questionStem: string, noteTopology: string) => {
    start
      .jumpToNotePage(noteTopology)
      .openQuestionList()
      .suggestingQuestionForFineTuning(questionStem)
      .suggestingNegativeFeedbackFineTuningExclusion()
  }
)

When('I confirm re-assimilation', () => {
  start.assumeAnsweredQuestionPage().confirmReAssimilation()
})
