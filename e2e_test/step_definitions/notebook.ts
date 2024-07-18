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
      .updateAssessmentSettings(notebook, numberOfQuestion)
  }
)

When(
  'I set the number of Certificate Expiration Period of the notebook {string} to {int}',
  (notebook: string, untilCerExpire: number) => {
    start
      .routerToNotebooksPage()
      .updateAssessmentSettingsCertificatePeriod(notebook, untilCerExpire)
  }
)

Then(
  'I should see the expiration period of the notebook {string} to {int}',
  (notebook: string, untilCertExpire: number) => {
    start
      .routerToNotebooksPage()
      .expectPeriodCertification(notebook, untilCertExpire)
  }
)

Given('There is a notebook {string}', (notebook: string) => {
  start.navigateToBazaar().expectNotebook(notebook)
})

When(
  'The notebook owner set the number of questions in assessment of the notebook {string} to {int}',
  (notebook: string, numberOfQuestion: number) => {
    start
      .routerToNotebooksPage()
      .updateAssessmentSettings(notebook, numberOfQuestion)
  }
)

When(
  'The notebook owner set the number of maximum attempt per day of the notebook {string} to {int}',
  (_notebook: string, _maxAttempts: number) => {
    // do nothing, since it is currently hardcoded in backend
  }
)

When(
  'I have done the assessment of the notebook {string} {int} times',
  (notebook: string, numberOfAttempts: number) => {
    const table: DataTable = new DataTable([
      ['Question', 'Answer'],
      ['Where in the world is Singapore?', 'Asia'],
    ])
    for (let count = 0; count < numberOfAttempts; count++) {
      start
        .navigateToBazaar()
        .selfAssessmentOnNotebook(notebook)
        .answerQuestionsFromTable(table.hashes())
    }
  }
)

When(
  'I try to do assessment of the notebook {string} again',
  (notebook: string) => {
    start.navigateToBazaar().selfAssessmentOnNotebook(notebook)
  }
)

Then(
  'I should not be able to do anymore assessment of the notebook {string} today',
  (_notebook: string) => {
    cy.findByText(
      'You have reached the assessment limit for today. Please try again tomorrow.'
    )
  }
)
