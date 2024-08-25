/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

Then('I should see {string} shared in the Bazaar', (notebooks: string) => {
  start.navigateToBazaar().expectNotebooks(notebooks)
})

Then('notebook {string} is shared to the Bazaar', (noteTopic: string) => {
  start.testability().shareToBazaar(noteTopic)
})

Then("there shouldn't be any note edit button", () => {
  start.assumeNotePage().editNoteImage().shouldNotExist()
})

When('I open the notebook {string} in the Bazaar', (noteTopic: string) => {
  cy.findCardTitle(noteTopic).click()
})

When(
  'I subscribe to notebook {string} in the bazaar, with target of learning {int} notes per day',
  (notebookTitle: string, count: string) => {
    start.navigateToBazaar().subscribe(notebookTitle, count)
  }
)

Then(
  'I should see a certification icon on the {string} notebook card in the bazaar',
  (notebook: string) => {
    start.navigateToBazaar().expectCertificationIcon(notebook, true)
  }
)

Then(
  'I should see no certification icon on the {string} notebook card in the bazaar',
  (notebook: string) => {
    start.navigateToBazaar().expectCertificationIcon(notebook, false)
  }
)

Then(
  'I can not see add the notebook {string} to my learning in the bazaar',
  (noteTopic: string) => {
    start.navigateToBazaar().expectNoAddToMyLearningButton(noteTopic)
  }
)

Then("I should see I've subscribed to {string}", (noteTopic: string) => {
  cy.findCardTitle(noteTopic)
})

Then('I should see message that says {string}', (message: string) => {
  cy.findByText(message)
})

Then("I should see I've not subscribed to {string}", (noteTopic: string) => {
  cy.findByText('Subscribed Notes').should('exist')
  cy.findCardTitle(noteTopic).should('not.exist')
})

Then('I should see it has link to {string}', (noteTopic: string) => {
  start.assumeNotePage().navigateToReference(noteTopic)
})
