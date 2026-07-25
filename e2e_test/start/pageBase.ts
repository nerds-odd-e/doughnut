/// <reference types="cypress" />
// @ts-check

/** Waits until no [data-app-busy] loading UI remains. */
export const waitUntilAppIsNotBusy = () => {
  cy.get('[data-app-busy]', { timeout: 30000 }).should('not.exist')
}
