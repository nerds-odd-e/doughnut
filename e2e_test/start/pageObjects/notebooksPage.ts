import NotePath from '../../support/NotePath'
import { notebookList } from './NotebookList'
import noteCreationForm from './noteForms/noteCreationForm'
import { assumeNotePage } from './notePage'
import notebookQuestionsList from './notebookQuestionsList'

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
    assertNoteHasSettingWithValue(
      notebookTopic: string,
      setting: string,
      value: string
    ) {
      this.findNotebookCardButton(
        notebookTopic,
        'Edit notebook settings'
      ).click()
      cy.formField(setting).fieldShouldHaveValue(value)
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
    requestForNotebookApproval(notebook: string) {
      this.findNotebookCardButton(notebook, 'Edit notebook settings').click()
      cy.findByRole('button', { name: 'Send Request' }).click()
    },
    expectNotebookApprovalCannotBeRequested(notebook: string) {
      this.findNotebookCardButton(notebook, 'Edit notebook settings').click()
      cy.findByRole('button', { name: 'Send Request' }).should('not.exist')
    },
    expectNotebookApprovalCanBeRequested(notebook: string) {
      this.findNotebookCardButton(notebook, 'Edit notebook settings').click()
      cy.findByRole('button', { name: 'Send Request' }).should('exist')
    },
    expectNotebookApprovalStatus(notebook: string, status: string) {
      this.findNotebookCardButton(notebook, 'Edit notebook settings').click()
      cy.findByText('Approval {status}'.replace('{status}', status)).should(
        'exist'
      )
    },
    updateAssessmentSettings(
      notebook: string,
      settings: {
        numberOfQuestion?: number
        certificateExpiry?: string
      }
    ) {
      this.findNotebookCardButton(notebook, 'Edit notebook settings').click()
      if (settings.numberOfQuestion !== undefined) {
        cy.formField('Number Of Questions In Assessment').assignFieldValue(
          `${settings.numberOfQuestion}`
        )
      }
      if (settings.certificateExpiry) {
        cy.formField('Certificate Expiry').assignFieldValue(
          `${settings.certificateExpiry}`
        )
      }

      cy.findByRole('button', { name: 'Update' }).click()
      cy.pageIsNotLoading()
    },
    unsubscribe(notebook: string) {
      this.findNotebookCardButton(notebook, 'Unsubscribe').click()
      cy.findByRole('button', { name: 'OK' }).click()
    },
    openNotebookQuestions(notebook: string) {
      this.findNotebookCardButton(notebook, 'Notebook Questions').click()
      return notebookQuestionsList()
    },
  }
}
