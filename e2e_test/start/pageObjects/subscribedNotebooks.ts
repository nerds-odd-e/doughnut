/// <reference types="cypress" />
/// <reference path="../../support/index.d.ts" />
import { pageIsNotLoading } from '../pageBase'

/** Bazaar subscriptions appear in the merged notebooks catalog (same list as owned notebooks). */
export const subscribedNotebooks = () => {
  pageIsNotLoading()

  return {
    expectNotebook(notebookTitle: string) {
      cy.get('main').within(() => {
        cy.findByText(notebookTitle, {
          selector: '.notebook-card h5',
        }).should(($el) => {
          expect(
            $el.length,
            `Expected to find subscribed notebook "${notebookTitle}" in the notebook catalog, but it was not found`
          ).to.be.greaterThan(0)
        })
      })
    },
    expectNotebookNotPresent(notebookTitle: string) {
      cy.get('main').within(() => {
        cy.findByText(notebookTitle, { selector: '.notebook-card h5' }).should(
          'not.exist'
        )
      })
    },
    openNotebook(notebookTitle: string) {
      pageIsNotLoading()
      cy.get('main').within(() => {
        cy.findByText(notebookTitle, {
          selector: '.notebook-card h5',
        })
          .should('be.visible')
          .closest('.notebook-card')
          .click()
      })
    },
  }
}
