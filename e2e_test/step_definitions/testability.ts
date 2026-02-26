/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

When('Someone triggered an exception', () => {
  start.testability().triggerException()
})

Then(
  'an admin should see {string} in the failure report',
  (content: string) => {
    start
      .loginAsAdminAndGoToAdminDashboard()
      .goToFailureReportList()
      .shouldContain(content)
  }
)

Then('each item in the failure report should have a checkbox', () => {
  cy.get('.failure-report').find('input[type="checkbox"]').should('exist')
})

When('I check the checkbox for the failure report item', () => {
  cy.get('.failure-report').find('input[type="checkbox"]').first().check()
})

When('I click the delete button', () => {
  cy.findByRole('button', { name: 'Delete Selected' }).click()
})

Then('the failure report should be empty', () => {
  cy.get('.failure-report').should('not.exist')
})

Then(
  'The {string} alert {string}',
  (expectedContent: string, shouldExistOrNot: string) => {
    cy.visit('/')
    cy.contains('Welcome')
    cy.contains(expectedContent).should(
      shouldExistOrNot === 'should exist' ? 'exist' : 'not.exist'
    )
  }
)

When('I go to the testability page to turn on the feature toggle', () => {
  cy.get('button[title="Testability"]').click()
  start.form.getField('Feature Toggle').click()
})
