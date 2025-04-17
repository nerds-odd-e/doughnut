import { findNotebookCardButton, notebookList } from './NotebookList'
import notebookQuestionsList from './notebookQuestionsList'
import notebookSettingsPopup from './notebookSettingsPopup'

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
  },
  openNotebookQuestions() {
    findNotebookCardButton(notebook, 'Notebook Questions').click()
    return notebookQuestionsList()
  },
  editNotebookSettings() {
    findNotebookCardButton(notebook, 'Edit notebook settings').click()
    return notebookSettingsPopup()
  },
  exportForObsidian() {
    return this.editNotebookSettings().exportForObsidian()
  },
  importObsidianData(filename: string) {
    return this.editNotebookSettings().importObsidianData(filename)
  },
  shouldBeDefault() {
    cy.findByText(notebook, { selector: '.notebook-card *' })
      .parents('.notebook-card')
      .findByText('Default')
      .should('be.visible')
  },
})
