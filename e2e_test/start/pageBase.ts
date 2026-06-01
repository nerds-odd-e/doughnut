/// <reference types="cypress" />
// @ts-check

/** Waits until no `.loading-bar` or blocking loading modal remains. */
export const pageIsNotLoading = () => {
  cy.get('.loading-bar', { timeout: 30000 }).should('not.exist')
  cy.get('.loading-modal-mask', { timeout: 30000 }).should('not.exist')
}
