/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import { DataTable } from '@cucumber/cucumber'
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

Then('I initial review {string}', (noteTopic: string) => {
  start.assimilation().goToAssimilationPage().assimilateNotes(noteTopic)
})

Then(
  'I added and learned one note {string} on day {int}',
  (noteTopic: string, day: number) => {
    start.testability().injectNotes([{ Topic: noteTopic }])
    start.testability().backendTimeTravelTo(day, 8)
    start.assimilation().goToAssimilationPage().assimilateNotes(noteTopic)
  }
)

Then(
  'I learned one note {string} on day {int}',
  (noteTopic: string, day: number) => {
    start.testability().backendTimeTravelTo(day, 8)
    start.assimilation().goToAssimilationPage().assimilateNotes(noteTopic)
  }
)

Then('I am recalling my note on day {int}', (day: number) => {
  start.testability().backendTimeTravelTo(day, 8)
  start.recall().goToRecallPage()
})

Then('I am assimilating new note on day {int}', (day: number) => {
  start.testability().backendTimeTravelTo(day, 8)
  start.assimilation().goToAssimilationPage()
})

Then(
  'I set the level of {string} to be {int}',
  (noteTopic: string, level: number) => {
    start.assumeNotePage(noteTopic)
    cy.formField('Level').then(($control) => {
      cy.wrap($control).within(() => {
        cy.findByRole('button', { name: `${level}` }).click()
      })
    })
  }
)

Then('I have selected the choice {string}', (choice: string) => {
  cy.formField(choice).check()
  cy.findByRole('button', { name: 'Keep for repetition' }).click()
})

Then('I choose yes I remember', () => {
  cy.yesIRemember()
})

Then(
  'I should be asked spelling question {string} from notebook {string}',
  (question: string, notebook: string) => {
    cy.expectBreadcrumb(notebook)
    cy.findByText(question).should('be.visible')
  }
)

Then(
  'I should be asked link question {string} {string} with options {string}',
  (noteTopic: string, linkType: string, options: string) => {
    cy.shouldSeeQuizWithOptions([noteTopic, linkType], options)
  }
)

Then('I type my answer {string}', (answer: string) => {
  cy.pageIsNotLoading()
  cy.replaceFocusedTextAndEnter(answer)
})

Then('I choose answer {string}', (noteTopic: string) => {
  cy.findByRole('button', { name: noteTopic }).click()
})

Then('I should see the information of note {string}', (noteTopic: string) => {
  start.assumeNotePage(noteTopic)
})

Then('I should see that my answer {string} is incorrect', (answer) => {
  cy.findByText(`Your answer \`${answer}\` is incorrect.`)
})

Then('I should see the repetition is finished: {string}', (yesNo) => {
  cy.findByText(
    'You have finished all repetitions for this half a day!'
  ).should(yesNo === 'yes' ? 'exist' : 'not.exist')
})

Then('I should see that my answer is correct', () => {
  start.assumeAnsweredQuestionPage().expectLastAnswerToBeCorrect()
})

Then('I should see that my last answer is correct', () => {
  start
    .assumeAnsweredQuestionPage()
    .goToLastResult()
    .expectLastAnswerToBeCorrect()
})

Then(
  'I should see the memory tracker info of note {string}',
  (noteTopic: string, data: DataTable) => {
    start
      .assumeAnsweredQuestionPage()
      .showMemoryTracker(noteTopic)
      .expectMemoryTrackerInfo(data.hashes()[0] ?? {})
  }
)

Then('choose to remove the last memory tracker from recalls', () => {
  start
    .assumeAnsweredQuestionPage()
    .goToLastResult()
    .showMemoryTracker()
    .removeMemoryTrackerFromReview()
})

Then('the choice {string} should be correct', (choice: string) => {
  start.assumeQuestionPage().expectChoiceToBe(choice, 'correct')
})

Then('the choice {string} should be incorrect', (choice: string) => {
  start.assumeQuestionPage().expectChoiceToBe(choice, 'incorrect')
})

When(
  "I've got the following question for a note with topic {string}:",
  (noteTopic: string, question: DataTable) => {
    start.stubOpenAIQuestionGenerationAndSeeTheQuestion(
      noteTopic,
      question.hashes()[0] ?? {}
    )
  }
)

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
  (questionStem: string, noteTopic: string) => {
    start
      .jumpToNotePage(noteTopic)
      .openQuestionList()
      .suggestingQuestionForFineTuning(questionStem)
      .suggestingPositiveFeedbackForFineTuning()
  }
)

Then(
  'I suggest the question {string} of the note {string} as a bad example',
  (questionStem: string, noteTopic: string) => {
    start
      .jumpToNotePage(noteTopic)
      .openQuestionList()
      .suggestingQuestionForFineTuning(questionStem)
      .suggestingNegativeFeedbackFineTuningExclusion()
  }
)
