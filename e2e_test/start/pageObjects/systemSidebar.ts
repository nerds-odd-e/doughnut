import { routerToNotebooksPage } from './notebooksPage'

export const systemSidebar = () => {
  routerToNotebooksPage()
  cy.pageIsNotLoading()
  cy.findByRole('button', { name: 'open sidebar' }).click({ force: true })

  return {}
}
