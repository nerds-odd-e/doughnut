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
  downloadForObsidian() {
    findNotebookCardButton(notebook, 'Download for Obsidian').click()
  },
})
