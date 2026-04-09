/// <reference types="cypress" />
/// <reference path="../../support/index.d.ts" />
import { pageIsNotLoading } from '../pageBase'

const subscribedSectionHeading = 'Subscribed notebooks'

export const subscribedNotebooks = () => {
  pageIsNotLoading()
  cy.contains('h2', subscribedSectionHeading).should('exist')

  const getContainer = () =>
    cy.contains('h2', subscribedSectionHeading).parent()

  return {
    expectNotebook(notebookTitle: string) {
      getContainer().within(() => {
        cy.findByText(notebookTitle, {
          selector: '.notebook-card h5',
        }).should(($el) => {
          expect(
            $el.length,
            `Expected to find subscribed notebook "${notebookTitle}" in the subscribed notebooks section, but it was not found`
          ).to.be.greaterThan(0)
        })
      })
    },
    expectNotebookNotPresent(notebookTitle: string) {
      getContainer().then(($container) => {
        cy.wrap($container)
          .find('.notebook-card h5')
          .should(($titles) => {
            const titles = Array.from($titles, (el) => el.textContent)
            expect(
              titles.includes(notebookTitle),
              `Expected notebook "${notebookTitle}" not to be in the subscribed notebooks section, but found it among: ${titles.join(', ')}`
            ).to.be.false
          })
      })
    },
    openNotebook(notebookTitle: string) {
      getContainer().within(() => {
        cy.findByText(notebookTitle, {
          selector: '.notebook-card h5',
        }).click()
      })
    },
  }
}
