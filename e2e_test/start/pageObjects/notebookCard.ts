import { findNotebookCardButton, notebookList } from './NotebookList'
import notebookSettingsPage from './notebookSettingsPage'

export const notebookCard = (notebook: string) => ({
  ...notebookList(),
  shareNotebookToBazaar() {
    findNotebookCardButton(notebook, 'Share notebook to bazaar').click()
    cy.findByRole('button', { name: 'OK' }).click()
  },
  updateSubscription() {
    findNotebookCardButton(notebook, 'Edit subscription').click()
  },
  unsubscribe() {
    findNotebookCardButton(notebook, 'Unsubscribe').click()
    cy.findByRole('button', { name: 'OK' }).click()
    cy.pageIsNotLoading()
  },
  editNotebookSettings() {
    findNotebookCardButton(notebook, 'Edit notebook settings').click()
    return notebookSettingsPage()
  },
  exportForObsidian() {
    return this.editNotebookSettings().exportForObsidian()
  },
  importObsidianData(filename: string) {
    return this.editNotebookSettings().importObsidianData(filename)
  },
})
