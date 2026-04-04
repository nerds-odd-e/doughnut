import { pageIsNotLoading } from '../pageBase'
import { findNotebookCardButton, notebookList } from './NotebookList'
import notebookPage from './notebookPage'

export const notebookCard = (notebook: string) => ({
  ...notebookList(),
  shareNotebookToBazaar() {
    return this.openNotebookPage().shareNotebookToBazaar()
  },
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
  exportForObsidian() {
    return this.openNotebookPage().exportForObsidian()
  },
  importObsidianData(filename: string) {
    return this.openNotebookPage().importObsidianData(filename)
  },
})
