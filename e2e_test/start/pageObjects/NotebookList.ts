import { commonSenseSplit } from '../../support/string_util'
import { pageIsNotLoading } from '../pageBase'
import { assumeNotePage } from './notePage'

export const notebookList = () => {
  pageIsNotLoading()
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
      pageIsNotLoading()
      cy.get('.notebook-card h5').then(($els) => {
        const cardTitles = Array.from($els, (el) => el.innerText)
        expect(cardTitles).to.deep.eq(commonSenseSplit(notebooks, ','))
      })
    },
    navigateToChild(notebook: string) {
      pageIsNotLoading()
      cy.get('.notebook-card').should('be.visible')
      cy.findByText(notebook, {
        selector: '.notebook-card h5',
      })
        .should('be.visible')
        .parents('.notebook-card')
        .click()
      return assumeNotePage()
    },
  }
}

const OVERFLOW_MENU_ACTION_NAMES = [
  'Edit notebook settings',
  'Edit subscription',
  'Move to group…',
] as const

function usesCatalogOverflowMenu(name: string): boolean {
  return (OVERFLOW_MENU_ACTION_NAMES as readonly string[]).includes(name)
}

export const findNotebookCardButton = (notebook: string, name: string) => {
  const finder = () => {
    pageIsNotLoading()
    cy.get('.notebook-card').should('be.visible')
    return cy
      .get('[data-cy="notebook-card"]')
      .filter((_index, card) => {
        const $card = Cypress.$(card)
        return $card
          .find('.notebook-card h5')
          .toArray()
          .some((heading) => heading.textContent?.trim() === notebook)
      })
      .then(($cards) => {
        expect($cards.length).to.be.greaterThan(0)
        const preferredCards = usesCatalogOverflowMenu(name)
          ? $cards
          : $cards.filter((_index, card) => {
              const $card = Cypress.$(card)
              return $card.find(`button[title="${name}"]`).length > 0
            })
        const targetCard =
          preferredCards.length > 0 ? preferredCards.first() : $cards.first()

        return cy.wrap(targetCard)
      })
      .as('notebookCatalogCard')
      .then(() => {
        if (usesCatalogOverflowMenu(name)) {
          cy.get('@notebookCatalogCard')
            .find('[data-cy="notebook-catalog-overflow"]')
            .click()
        }
      })
      .get('@notebookCatalogCard')
      .findByRole('button', { name: name })
  }

  return {
    click() {
      finder().click()
    },
    shouldNotExist() {
      finder().should('not.exist')
    },
  }
}
