/// <reference types="cypress" />
/// <reference path="../../support/index.d.ts" />
import { waitUntilAppIsNotBusy } from '../pageBase'
import { findDropdownPortalButton } from './dropdownPortal'

/** Bazaar subscriptions appear in the merged notebooks catalog (same list as owned notebooks). */
export const subscribedNotebooks = () => {
  waitUntilAppIsNotBusy()

  const openOverflowOnSubscribedCard = (notebookName: string) => {
    cy.get('[data-cy="notebook-card"]')
      .filter((_index, card) => {
        const $card = Cypress.$(card)
        const titleMatch = $card
          .find('.notebook-card h5')
          .toArray()
          .some((h) => h.textContent?.trim() === notebookName)
        if (!titleMatch) return false
        return $card.find('button[title="Unsubscribe"]').length > 0
      })
      .first()
      .as('subscribedCatalogCard')
    cy.get('@subscribedCatalogCard')
      .find('[data-cy="notebook-catalog-overflow"]')
      .click()
  }

  return {
    card(notebookName: string) {
      return {
        openMoveToGroupDialog() {
          waitUntilAppIsNotBusy()
          openOverflowOnSubscribedCard(notebookName)
          findDropdownPortalButton('Move to group…').click()
        },
      }
    },
    expectNotebook(notebookName: string) {
      cy.get('main').within(() => {
        cy.findByText(notebookName, {
          selector: '.notebook-card h5',
        }).should(($el) => {
          expect(
            $el.length,
            `Expected to find subscribed notebook "${notebookName}" in the notebook catalog, but it was not found`
          ).to.be.greaterThan(0)
        })
      })
    },
    expectNotebookNotPresent(notebookName: string) {
      cy.get('main').within(() => {
        cy.findByText(notebookName, { selector: '.notebook-card h5' }).should(
          'not.exist'
        )
      })
    },
    openNotebook(notebookName: string) {
      waitUntilAppIsNotBusy()
      cy.get('main').within(() => {
        cy.findByText(notebookName, {
          selector: '.notebook-card h5',
        })
          .should('be.visible')
          .closest('.notebook-card')
          .click()
      })
    },
  }
}
