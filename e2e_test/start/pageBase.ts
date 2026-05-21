/// <reference types="cypress" />
// @ts-check

/** Waits until no `.loading-bar` nodes remain (thin bar + spinners). */
export const pageIsNotLoading = () => {
  cy.get('.loading-bar', { timeout: 30000 }).should('not.exist')
}
