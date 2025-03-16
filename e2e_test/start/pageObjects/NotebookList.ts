import { commonSenseSplit } from '../../support/string_util'
import { assumeNotePage } from './notePage'

export const notebookList = () => {
  cy.pageIsNotLoading()
  return {
    expectNotebookCards: (notebooks: Record<string, string>[]) => {
      cy.get('.notebook-card .daisy\\:card-title').should(
        'have.length',
        notebooks.length
      )
      notebooks.forEach((elem) => {
        for (const propName in elem) {
          if (propName === 'Title') {
            cy.findByText(elem[propName]!, {
              selector: '.notebook-card .daisy\\:card-title',
            })
          } else {
            cy.findByText(elem[propName]!)
          }
        }
      })
    },
    expectNotebooks: (notebooks: string) => {
      cy.pageIsNotLoading()
      cy.get('.notebook-card .daisy\\:card-title').then(($els) => {
        const cardTitles = Array.from($els, (el) => el.innerText)
        expect(cardTitles).to.deep.eq(commonSenseSplit(notebooks, ','))
      })
    },
    navigateToChild(notebook: string) {
      cy.findByText(notebook, { selector: '.notebook-card *' }).click()
      return assumeNotePage()
    },
  }
}

export const findNotebookCardButton = (notebook: string, name: string) => {
  const finder = () =>
    cy
      .findByText(notebook, { selector: '.notebook-card *' })
      .parents('.daisy\\:card')
      .findByRole('button', { name: name })

  return {
    click() {
      finder().click({ force: true })
    },
    shouldNotExist() {
      finder().should('not.exist')
    },
  }
}
