/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import type { DataTable } from '@cucumber/cucumber'
import start from '../start'

Then('I assimilate these in sequence:', (data: DataTable) => {
  start.assimilation().goToAssimilationPage().assimilate(data.hashes())
})

Given("It's day {int}, {int} hour", (day: number, hour: number) => {
  start.testability().backendTimeTravelTo(day, hour)
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
  'On day {int} I recall {string} and initial review new {string}',
  (day: number, repeatNotes: string, initialNotes: string) => {
    start.testability().timeTravelTo(day, 8)
    start.recall().goToRecallPage().recallNotes(repeatNotes)
    start.assimilation().goToAssimilationPage().assimilateNotes(initialNotes)
  }
)

Given('I go to the recalls page', () => {
  start.recall().goToRecallPage()
})

Then(
  'I should see that I have {int} notes to recall',
  (numberOfNotes: number) => {
    cy.reload()
    start.recall().expectCount(numberOfNotes)
  }
)

Then(
  'I should see that I have {int} new notes to assimilate',
  (numberOfNotes: number) => {
    start.assimilation().expectCount(numberOfNotes)
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

Then('it should move to review page', () => {
  cy.url().should('eq', `${Cypress.config().baseUrl}/recalls`)
})

Then('I initial review {string}', (noteTopology: string) => {
  start.assimilation().goToAssimilationPage().assimilateNotes(noteTopology)
})

Then(
  'I added and learned one note {string} on day {int}',
  (noteTopology: string, day: number) => {
    cy.get<string>('@currentLoginUser').then((username) => {
      start.testability().injectNotes([{ Title: noteTopology }], username)
    })
    start.testability().backendTimeTravelTo(day, 8)
    start.assimilation().goToAssimilationPage().assimilateNotes(noteTopology)
  }
)

Then(
  'I assimilated one note {string} on day {int}',
  (noteTopology: string, day: number) => {
    start.testability().backendTimeTravelTo(day, 8)
    start.assimilation().goToAssimilationPage().assimilateNotes(noteTopology)
  }
)

Then('I am recalling my note on day {int}', (day: number) => {
  start.testability().backendTimeTravelTo(day, 8)
  cy.reload()
  start.recall().goToRecallPage()
})

Then('I am assimilating new note on day {int}', (day: number) => {
  start.testability().backendTimeTravelTo(day, 8)
  start.assimilation().goToAssimilationPage()
})

When(
  'one of the checklist topic is selected to ignore {string} and assimilate the note',
  (topicText: string) => {
    // start.assumeAssimilationPage().selectChecklistTopicToIgnore(topicText)
    start.assumeAssimilationPage().assimilateCurrentNote()
  }
)

Then(
  'I set the level of {string} to be {int}',
  (noteTopology: string, level: number) => {
    start.assumeNotePage(noteTopology)
    cy.formField('Level').then(($control) => {
      cy.wrap($control).within(() => {
        cy.findByRole('button', { name: `${level}` }).click()
      })
    })
  }
)

Then('I choose yes I remember', () => {
  cy.yesIRemember()
})

Then('I skip one question', () => {
  start.assumeQuestionPage().skipQuestion()
})

Then('I assimilate with the option of remembering spelling', () => {
  start.assumeAssimilationPage().assimilateWithSpellingOption()
})

Then(
  'I should be asked spelling question {string} from notebook {string}',
  (question: string, notebook: string) => {
    start.assumeQuestionPage(question).forNotebook(notebook)
  }
)

Then('I type my answer {string}', (answer: string) => {
  cy.pageIsNotLoading()
  cy.replaceFocusedTextAndEnter(answer)
})

Then('I choose answer {string}', (answer: string) => {
  start.assumeQuestionPage().answer(answer)
})

Then(
  'I should see the information of note {string}',
  (noteTopology: string) => {
    start.assumeNotePage(noteTopology)
  }
)

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
    .removeMemoryTrackerFromReview('normal')
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
  'I should see the question {string} is disabled',
  (questionStem: string) => {
    start.assumeQuestionPage(questionStem).isDisabled()
  }
)

Then(
  'I should see the question {string} is enabled',
  (questionStem: string) => {
    start.assumeQuestionPage(questionStem).isNotDisabled()
  }
)

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

When('I start assimilating {string}', (noteTitle: string) => {
  start.assimilation().goToAssimilationPage().expectNoteTitle(noteTitle)
})

Then('I should be able to select a note type', () => {
  start.assumeAssimilationPage().expectNoteTypePrompt()
})

Then(
  'I should see an understanding checklist with two points: {string} and {string}',
  (point1: string, point2: string) => {
    start.assumeAssimilationPage().expectUnderstandingPoints([point1, point2])
  }
)

Then(
  'I should see an understanding checklist with a maximum of {int} points',
  (maxPoints: number) => {
    start.assumeAssimilationPage().expectUnderstandingPointsAtMost(maxPoints)
  }
)

Given('the note type is {string}', (noteType: string) => {
  start.assumeAssimilationPage().selectNoteType(noteType)
})

Then('I can continue with the assimilation', () => {
  // Verify we're still on the assimilation page or moved to the next note
  cy.url().should('satisfy', (url: string) => {
    return url.includes('/assimilate') || url.includes('/recalls')
  })
})

When(
  'I mark the point {string} for removal as unrelated to the note',
  (point: string) => {
    start.assumeAssimilationPage().selectChecklistTopicToIgnore(point)
  }
)

Then(
  'the question generated for the note {string} should not include {string}',
  (noteTitle: string, excludedText: string) => {
    start.recall().goToRecallPage().expectCurrentQuestion()
    cy.pageIsNotLoading()
    // Check all question stems and choices in the question table
    cy.get('.quiz-instruction').within(() => {
      // Assert that the entire question instruction (stem and choices) does not include the excluded text
      cy.get('[data-test="stem"]').should('not.contain', excludedText)
      cy.get('.choice-text').each(($choice) => {
        cy.wrap($choice).should('not.contain', excludedText)
      })
    })
  }
)
