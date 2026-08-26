/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import { commonSenseSplit } from 'support/string_util'
import start from '../start'

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

When('I repeat more old {string}', (repeatNotes: string) => {
  start.recall().assumeRecallPage().recallNotes(repeatNotes)
})

When(
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
  'I should see {int} potential learning session for notebook {string}',
  (count: number, notebookTitle: string) => {
    start.recall().expectPotentialLearningSession(count, notebookTitle)
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

When('I am recalling my note on day {int}', (day: number) => {
  start.testability().backendTimeTravelTo(day, 8)
  cy.reload()
  start.recall().visitRecallPage()
})

Given('a due recall prompt is ready on day {int}', (day: number) => {
  start.testability().backendTimeTravelTo(day, 8)
  start.testability().dueRecallPrompt()
})

When('I visit recall for a due recall prompt on day {int}', (day: number) => {
  start.testability().backendTimeTravelTo(day, 8)
  start.recall().visitRecallPageAndWaitForQuestions(1)
})

When(
  'I visit recall waiting for {int} due recall prompts on day {int}',
  (promptCount: number, day: number) => {
    start.testability().backendTimeTravelTo(day, 8)
    start.recall().visitRecallPageAndWaitForQuestions(promptCount)
  }
)

When('I visit recall', () => {
  start.recall().visitRecallPage()
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

When('I choose Good', () => {
  start.recall().assumeRecallPage().chooseGood()
})

When('I choose Again', () => {
  start.recall().assumeRecallPage().chooseAgain()
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

When('I type my answer {string}', (answer: string) => {
  start.recall().assumeRecallPage().typeSpellingAnswer(answer)
})

When('I choose answer {string}', (answer: string) => {
  start.assumeQuestionPage().answer(answer)
})

When('I switch away from the tab for {int} seconds', (seconds: number) => {
  start.assumeQuestionPage().switchAwayFromTabFor(seconds)
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

Then('I should see that my last answer to spelling question is correct', () => {
  start
    .assumeAnsweredQuestionPage()
    .goToLastAnsweredQuestion()
    .expectSpellingAnswerToBeCorrect()
})

Then('I should be able to resume recalling', () => {
  start.recall().expectResumeAvailable()
})

When('I resume recalling', () => {
  start.recall().resumeRecall()
})

Then('I should be back to the current question', () => {
  start.recall().assumeRecallPage().expectCurrentQuestion()
})

Then('I should be asked {string}', (expectedQuestionStem: string) => {
  start.assumeQuestionPage(expectedQuestionStem)
})

Then(
  'I should see a frequent failure warning for the note with {int} wrong answers in {int} days',
  (wrongCount: number, periodDays: number) => {
    start
      .assumeAnsweredQuestionPage()
      .expectFrequentFailureWarningForNote(wrongCount, periodDays)
  }
)
