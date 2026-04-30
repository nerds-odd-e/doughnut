/// <reference types="cypress" />
// @ts-check

/** Waits until no `.loading-bar` nodes remain (thin bar + spinners). */
export const pageIsNotLoading = () => {
  cy.get('body').should(
    ($body) => {
      expect($body.find('.loading-bar').length).to.eq(0)
    },
    { timeout: 30000 }
  )
}
