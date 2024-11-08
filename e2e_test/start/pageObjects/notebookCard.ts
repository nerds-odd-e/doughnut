import { notebookList } from './NotebookList'
import notebookQuestionsList from './notebookQuestionsList'
import notebookSettingsPopup from './notebookSettingsPopup'

export const notebookCard = (notebook: string) => ({
  ...notebookList(),
  shareNotebookToBazaar() {
    this.findNotebookCardButton(notebook, 'Share notebook to bazaar').click()
    cy.findByRole('button', { name: 'OK' }).click()
  },
  updateSubscription() {
    this.findNotebookCardButton(notebook, 'Edit subscription').click()
  },
  unsubscribe() {
    this.findNotebookCardButton(notebook, 'Unsubscribe').click()
  },
  openNotebookQuestions() {
    this.findNotebookCardButton(notebook, 'Notebook Questions').click()
    return notebookQuestionsList()
  },
  editNotebookSettings() {
    this.findNotebookCardButton(notebook, 'Edit notebook settings').click()
    return notebookSettingsPopup()
  },
  notebookAssistant() {
    this.findNotebookCardButton(notebook, 'Notebook Assistant').click()
    return {
      create(instruction: string) {
        cy.formField('Additional Instruction').type(instruction)
        cy.findByRole('button', {
          name: 'Create Assistant For Notebook',
        }).click()
        cy.pageIsNotLoading()
      },
    }
  },
})
