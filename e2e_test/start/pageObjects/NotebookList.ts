import { commonSenseSplit } from '../../support/string_util'
import { assumeNotePage } from './notePage'

export const notebookList = () => {
  cy.pageIsNotLoading()
  return {
    expectNotebookCards: (notebooks: Record<string, string>[]) => {
      cy.get('.notebook-card .daisy-card-title').should(
        'have.length',
        notebooks.length
      )
      notebooks.forEach((elem) => {
        for (const propName in elem) {
          if (propName === 'Title') {
            cy.findByText(elem[propName]!, {
              selector: '.notebook-card .daisy-card-title',
            })
          } else {
            cy.findByText(elem[propName]!)
          }
        }
      })
    },
    expectNotebooks: (notebooks: string) => {
      cy.pageIsNotLoading()
      cy.get('.notebook-card .daisy-card-title').then(($els) => {
        const cardTitles = Array.from($els, (el) => el.innerText)
        expect(cardTitles).to.deep.eq(commonSenseSplit(notebooks, ','))
      })
    },
    navigateToChild(notebook: string) {
      cy.pageIsNotLoading()
      cy.get('.notebook-card').should('be.visible')
      cy.findByText(notebook, {
        selector: '.notebook-card .daisy-card-title',
      })
        .should('be.visible')
        .parents('.notebook-card')
        .click()
      return assumeNotePage()
    },
  }
}

export const findNotebookCardButton = (notebook: string, name: string) => {
  const finder = () => {
    cy.pageIsNotLoading()
    cy.get('.notebook-card').should('be.visible')
    // Find the card that contains the notebook title and store it as an alias
    cy.findByText(notebook, {
      selector: '.notebook-card .daisy-card-title',
    })
      .should('be.visible')
      .parents('.daisy-card')
      .as('notebookCard')
    // Now find the button within that card (breaking the chain to avoid DOM detachment)
    return cy.get('@notebookCard').findByRole('button', { name: name })
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
