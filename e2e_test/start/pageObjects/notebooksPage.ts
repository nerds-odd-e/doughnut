import NotePath from '../../support/NotePath'
import { notebookList } from './NotebookList'
import noteCreationForm from './noteForms/noteCreationForm'
import { assumeNotePage } from './notePage'

export const routerToNotebooksPage = () => {
  cy.pageIsNotLoading()
  cy.routerPush('/notebooks', 'notebooks', {})
  cy.findByText('Notebooks')
  return {
    ...notebookList(),
    navigateToPath(notePath: NotePath) {
      return notePath.path.reduce(
        (page, noteTopic) => page.navigateToChild(noteTopic),
        assumeNotePage()
      )
    },
    creatingNotebook(notebookTopic: string) {
      cy.findByText('Add New Notebook').click()
      return noteCreationForm.createNote(notebookTopic, undefined)
    },
    shareNotebookToBazaar(notebook: string) {
      this.findNotebookCardButton(notebook, 'Share notebook to bazaar').click()
      cy.findByRole('button', { name: 'OK' }).click()
    },
    updateSubscription(notebook: string) {
      this.findNotebookCardButton(notebook, 'Edit subscription').click()
      cy.findByRole('button', { name: 'Update' }).click()
    },
    skipReview(notebook: string) {
      this.findNotebookCardButton(notebook, 'Edit notebook settings').click()
      cy.formField('Skip Review Entirely').check()
      cy.findByRole('button', { name: 'Update' }).click()
    },
    updateAssessmentSettings(notebook: string, numberOfQuestion: number) {
      this.findNotebookCardButton(notebook, 'Edit notebook settings').click()
      cy.formField('Number Of Questions In Assessment').assignFieldValue(
        `${numberOfQuestion}`
      )
      cy.findByRole('button', { name: 'Update' }).click()
    },
    updateAssessmentSettingsCertificatePeriod(
      notebook: string,
      untilCerExpire: number
    ) {
      this.findNotebookCardButton(notebook, 'Edit notebook settings').click()
      cy.formField('Until Cert Expire (in days)').assignFieldValue(
        `${untilCerExpire}`
      )
      cy.findByRole('button', { name: 'Update' }).click()
    },
    unsubscribe(notebook: string) {
      this.findNotebookCardButton(notebook, 'Unsubscribe').click()
      cy.findByRole('button', { name: 'OK' }).click()
    },
  }
}
