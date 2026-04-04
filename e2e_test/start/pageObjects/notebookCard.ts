import { pageIsNotLoading } from '../pageBase'
import { findNotebookCardButton, notebookList } from './NotebookList'
import notebookPage from './notebookPage'

export const notebookCard = (notebook: string) => ({
  ...notebookList(),
  updateSubscription() {
    findNotebookCardButton(notebook, 'Edit subscription').click()
  },
  unsubscribe() {
    findNotebookCardButton(notebook, 'Unsubscribe').click()
    cy.findByRole('button', { name: 'OK' }).click()
    pageIsNotLoading()
  },
  openNotebookPage() {
    findNotebookCardButton(notebook, 'Edit notebook settings').click()
    return notebookPage()
  },
})
