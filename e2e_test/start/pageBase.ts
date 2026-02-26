/// <reference types="cypress" />
// @ts-check

export const pageIsNotLoading = () => {
  cy.get('.loading-bar').should('not.exist', { timeout: 10000 })
}
