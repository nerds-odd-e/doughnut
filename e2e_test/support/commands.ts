/// <reference types="cypress" />
// @ts-check
import '@testing-library/cypress/add-commands'
import 'cypress-file-upload'

Cypress.Commands.add('clearFocusedText', () => {
  // cy.clear for now is an alias of cy.type('{selectall}{backspace}')
  // it doesn't clear the text sometimes.
  // Invoking it twice seems to solve the problem.
  cy.focused()
    .should('be.visible')
    .should('match', 'input, textarea, [contenteditable="true"]')
    .clear()
    .clear()
})
