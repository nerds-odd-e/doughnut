import { commonSenseSplit } from '../../support/string_util'
import { waitUntilAppIsNotBusy } from '../pageBase'
import { findDropdownPortalButton } from './dropdownPortal'
import notebookPage from './notebookPage'

export const notebookList = () => {
  waitUntilAppIsNotBusy()
  return {
    expectNotebookCards: (notebooks: Record<string, string>[]) => {
      cy.get('.notebook-card h5').should('have.length', notebooks.length)
      notebooks.forEach((elem) => {
        for (const propName in elem) {
          if (propName === 'Title') {
            cy.findByText(elem[propName]!, {
              selector: '.notebook-card h5',
            })
          } else {
            cy.findByText(elem[propName]!)
          }
        }
      })
    },
    expectNotebooks: (notebooks: string) => {
      waitUntilAppIsNotBusy()
      const expected = commonSenseSplit(notebooks, ',')
      cy.get('.notebook-card h5', { timeout: 15000 }).should(($els) => {
        const cardTitles = Array.from($els, (el) => el.innerText)
        expect(
          cardTitles,
          `Expected notebook cards [${expected.join(', ')}], but found [${cardTitles.join(', ')}]`
        ).to.deep.eq(expected)
      })
    },
    navigateToNotebook(notebookName: string) {
      waitUntilAppIsNotBusy()
      cy.get('.notebook-card').should('be.visible')
      cy.findByText(notebookName, {
        selector: '.notebook-card h5',
      })
        .should('be.visible')
        .click()
      waitUntilAppIsNotBusy()
      return notebookPage()
    },
  }
}

/** Catalog cards whose h5 title equals `notebookName` exactly. */
const notebookCardsNamed = (notebookName: string) => {
  waitUntilAppIsNotBusy()
  cy.get('.notebook-card').should('be.visible')
  return cy.get('[data-cy="notebook-card"]').filter((_index, card) => {
    const $card = Cypress.$(card)
    return $card
      .find('.notebook-card h5')
      .toArray()
      .some((heading) => heading.textContent?.trim() === notebookName)
  })
}

/** Opens the notebook page via the card title link (same as the user catalog path). */
export const clickNotebookCardTitleToOpenNotebookPage = (
  notebookName: string
) => {
  notebookCardsNamed(notebookName)
    .should('have.length.at.least', 1)
    .first()
    .within(() => {
      cy.findByText(notebookName, { selector: 'h5' })
        .should('be.visible')
        .closest('a')
        .click()
    })
  waitUntilAppIsNotBusy()
}

export const NOTEBOOK_CATALOG_EXPORT_ACTION =
  'Export as markdowns in a zip file'

const OVERFLOW_MENU_ACTION_NAMES = [
  'Edit subscription',
  'Move to group…',
  NOTEBOOK_CATALOG_EXPORT_ACTION,
] as const

function usesCatalogOverflowMenu(name: string): boolean {
  return (OVERFLOW_MENU_ACTION_NAMES as readonly string[]).includes(name)
}

/** Assert a catalog action button is absent on the named notebook card. */
export const expectNotebookCardButtonAbsent = (
  notebook: string,
  name: string
) => {
  notebookCardsNamed(notebook)
    .should('have.length.at.least', 1)
    .first()
    .find(`button[title="${name}"]`)
    .should('not.exist')
}

export const findNotebookCardButton = (notebook: string, name: string) => {
  const finder = () => {
    if (usesCatalogOverflowMenu(name)) {
      notebookCardsNamed(notebook)
        .should('have.length.at.least', 1)
        .first()
        .find('[data-cy="notebook-catalog-overflow"]')
        .should('be.visible')
        .click()
      return findDropdownPortalButton(name)
    }

    return notebookCardsNamed(notebook)
      .filter((_index, card) => {
        return Cypress.$(card).find(`button[title="${name}"]`).length > 0
      })
      .should('have.length.at.least', 1)
      .first()
      .findByRole('button', { name })
  }

  return {
    click() {
      finder().click()
    },
  }
}
