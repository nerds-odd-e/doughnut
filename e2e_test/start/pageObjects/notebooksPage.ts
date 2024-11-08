import NotePath from '../../support/NotePath'
import { notebookList } from './NotebookList'
import notebookQuestionsList from './notebookQuestionsList'
import noteCreationForm from './noteForms/noteCreationForm'
import { assumeNotePage } from './notePage'

const notebookSettingsPopup = () => {
  const clickButton = (name: string) =>
    cy.findByRole('button', { name }).click()
  const assertButtonExists = (name: string) =>
    cy.findByRole('button', { name }).should('exist')
  const assertButtonNotExists = (name: string) =>
    cy.findByRole('button', { name }).should('not.exist')

  return {
    assertNoteHasSettingWithValue(setting: string, value: string) {
      cy.formField(setting).fieldShouldHaveValue(value)
    },

    skipReview() {
      cy.formField('Skip Review Entirely').check()
      clickButton('Update')
    },
    requestForNotebookApproval() {
      clickButton('Send Request')
    },
    expectNotebookApprovalCanBeRequested() {
      assertButtonExists('Send Request')
    },
    expectNotebookApprovalStatus(status: string) {
      assertButtonNotExists('Send Request')
      cy.findByText(`Approval ${status}`).should('exist')
    },
    updateAssessmentSettings(settings: {
      numberOfQuestion?: number
      certificateExpiry?: string
    }) {
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

      clickButton('Update')
      cy.pageIsNotLoading()
    },
  }
}

const notebookCard = (notebook: string) => ({
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
})

const notebooksPage = () => ({
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
  notebookCard(notebook: string) {
    return notebookCard(notebook)
  },
  ...notebookSettingsPopup(),
})

export const routerToNotebooksPage = () => {
  cy.pageIsNotLoading()
  cy.routerPush('/d/notebooks', 'notebooks', {})
  cy.findByText('Notebooks')
  return notebooksPage()
}
