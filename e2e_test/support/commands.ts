// ***********************************************
// custom commands and overwrite existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************
//
//
// -- This is a parent command --
// Cypress.Commands.add("login", (email, password) => { ... })
//
//
// -- This is a child command --
// Cypress.Commands.add("drag", { prevSubject: 'element'}, (subject, options) => { ... })
//
//
// -- This is a dual command --
// Cypress.Commands.add("dismiss", { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This will overwrite an existing command --
// Cypress.Commands.overwrite("visit", (originalFn, url, options) => { ... })

/// <reference types="cypress" />
// @ts-check
import '@testing-library/cypress/add-commands'
import 'cypress-file-upload'

Cypress.Commands.add('pageIsNotLoading', () => {
  cy.get('.loading-bar').should('not.exist', { timeout: 10000 })
})

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

interface RouterPushOptions {
  name: string
  params: Record<string, string | number>
  query: Record<string, string | number>
}

interface RouteParams {
  [key: string]: string | number
}

type CustomWindow = Omit<Cypress.AUTWindow, 'Infinity' | 'NaN'> & {
  Infinity: number
  NaN: number
  router?: {
    push: (options: RouterPushOptions) => Promise<unknown>
  }
}

Cypress.Commands.add(
  'routerPush',
  (fallback: string, name: string, params: RouteParams) => {
    cy.get('@firstVisited').then((firstVisited) => {
      // Extract the value from the Cypress subject
      const isFirstVisited =
        (firstVisited as unknown as { valueOf(): string }).valueOf() === 'yes'
      cy.window().then((win: CustomWindow) => {
        if (win.router && isFirstVisited) {
          cy.wrap(
            win.router
              .push({
                name,
                params,
                query: { time: Date.now() }, // make sure the route re-render
              })
              .catch((error) => {
                cy.log('router push failed')
                cy.log(error as string)
                throw error
              })
          )
        } else {
          cy.wrap('yes').as('firstVisited')
          cy.visit(fallback)
        }
      })
    })
  }
)

Cypress.Commands.add('findCardTitle', (title) =>
  cy.findByText(title, { selector: '.daisy-card-title .title-text' })
)

Cypress.Commands.add('routerToRoot', () => {
  cy.routerPush('/', 'root', {})
})
