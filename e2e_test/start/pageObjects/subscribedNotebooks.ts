/// <reference types="cypress" />
/// <reference path="../../support/index.d.ts" />
import { pageIsNotLoading } from '../pageBase'

/** Bazaar subscriptions appear in the merged notebooks catalog (same list as owned notebooks). */
export const subscribedNotebooks = () => {
  pageIsNotLoading()

  const openOverflowOnSubscribedCard = (notebookTitle: string) => {
    cy.get('[data-cy="notebook-card"]')
      .filter((_index, card) => {
        const $card = Cypress.$(card)
        const titleMatch = $card
          .find('.notebook-card h5')
          .toArray()
          .some((h) => h.textContent?.trim() === notebookTitle)
        if (!titleMatch) return false
        return $card.find('button[title="Edit subscription"]').length > 0
      })
      .first()
      .as('subscribedCatalogCard')
    cy.get('@subscribedCatalogCard')
      .find('[data-cy="notebook-catalog-overflow"]')
      .click()
  }

  return {
    card(notebookTitle: string) {
      return {
        openMoveToGroupDialog() {
          pageIsNotLoading()
          openOverflowOnSubscribedCard(notebookTitle)
          cy.get('@subscribedCatalogCard')
            .findByRole('button', { name: 'Move to group…' })
            .click()
        },
      }
    },
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
