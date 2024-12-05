import { commonSenseSplit } from '../../support/string_util'

export const notebookList = () => {
  cy.pageIsNotLoading()
  return {
    expectNotebooks: (notebooks: string) => {
      cy.pageIsNotLoading()
      cy.get('.notebook-card .card-title').then(($els) => {
        const cardTitles = Array.from($els, (el) => el.innerText)
        expect(cardTitles).to.deep.eq(commonSenseSplit(notebooks, ','))
      })
    },
  }
}

export const findNotebookCardButton = (notebook: string, name: string) => {
  const finder = () =>
    cy
      .findByText(notebook, { selector: '.notebook-card *' })
      .parents('.card')
      .findByRole('button', { name: name })

  return {
    click() {
      finder().click()
    },
    shouldNotExist() {
      finder().should('not.exist')
    },
  }
}
