/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import {
  Given,
  Then,
  When,
  DataTable,
} from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

Given('I choose to share my notebook {string}', (noteTopic: string) => {
  start.routerToNotebooksPage().shareNotebookToBazaar(noteTopic)
})

Then(
  'I should see readonly notebook {string} in my notes',
  (noteTopic: string) => {
    start.routerToNotebooksPage()
    cy.findByText(noteTopic).click()
    cy.pageIsNotLoading()
    start.assumeNotePage().editNoteImage().shouldNotExist()
  }
)

Then(
  'I should be able to edit the subscription to notebook {string}',
  (noteTopic: string) => {
    start.routerToNotebooksPage().updateSubscription(noteTopic)
  }
)

When('I change notebook {string} to skip review', (noteTopic: string) => {
  start.routerToNotebooksPage().skipReview(noteTopic)
})

Then('I unsubscribe from notebook {string}', (noteTopic: string) => {
  start.routerToNotebooksPage().unsubscribe(noteTopic)
})

Given(
  'I set the number of questions per assessment of the notebook {string} to {int}',
  (notebook: string, numberOfQuestion: number) => {
    start
      .routerToNotebooksPage()
      .updateAssessmentSettings(notebook, { numberOfQuestion })
  }
)

Given(
  'the number of questions in assessment for notebook {string} is {int}',
  (notebook: string, numberOfQuestion: number) => {
    start
      .routerToNotebooksPage()
      .updateAssessmentSettings(notebook, { numberOfQuestion })
  }
)

When(
  'I have done the assessment of the notebook {string} {int} times',
  (notebook: string, numberOfAttempts: number) => {
    const table: DataTable = new DataTable([
      ['Question', 'Answer'],
      ['Where in the world is Singapore?', 'Asia'],
    ])
    Cypress._.times(numberOfAttempts, () => {
      start
        .navigateToBazaar()
        .selfAssessmentOnNotebook(notebook)
        .answerQuestionsFromTable(table.hashes())
    })
  }
)

Then(
  'I should not be able to do assessment of the notebook {string} any more today',
  (notebook: string) => {
    start
      .navigateToBazaar()
      .selfAssessmentOnNotebook(notebook)
      .expectReachedLimit()
  }
)

Then(
  'I should be able to do assessment of the notebook {string} again the next day',
  (notebook: string) => {
    start
      .testability()
      .backendTimeTravelRelativeToNow(25)
      .then(() => {
        start
          .navigateToBazaar()
          .selfAssessmentOnNotebook(notebook)
          .expectQuestion('Where in the world is Singapore?')
      })
  }
)
