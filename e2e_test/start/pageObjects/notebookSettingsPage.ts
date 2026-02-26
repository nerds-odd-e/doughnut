import { form } from '../forms'

const notebookSettingsPage = () => {
  const clickButton = (name: string) =>
    cy.findByRole('button', { name }).click()
  const assertButtonExists = (name: string) =>
    cy.findByRole('button', { name }).should('exist')
  const assertButtonNotExists = (name: string) =>
    cy.findByRole('button', { name }).should('not.exist')

  return {
    assertNoteHasSettingWithValue(setting: string, value: string) {
      form.fieldShouldHaveValue(setting, value)
    },

    skipMemoryTracking() {
      form.getField('Skip Memory Tracking').check()
      clickButton('Update Settings')
      cy.pageIsNotLoading()
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
        form.assignFieldValue(
          'Number of Questions in Assessment',
          `${settings.numberOfQuestion}`
        )
      }
      if (settings.certificateExpiry) {
        form.assignFieldValue(
          'Certificate Expiry',
          `${settings.certificateExpiry}`
        )
      }

      clickButton('Update Settings')
      cy.pageIsNotLoading()
    },
    updateAiAssistantInstructions(instruction: string) {
      form.getField('Additional Instructions to AI').type(instruction)
      clickButton('Update Notebook AI Assistant Settings')
      cy.pageIsNotLoading()
    },
    exportForObsidian() {
      cy.findByRole('button', { name: 'Export for Obsidian' }).click()
      cy.pageIsNotLoading()
      return this
    },
    importObsidianData(filename: string) {
      cy.contains('label', 'Import from Obsidian')
        .find('input[type="file"]')
        .selectFile(`e2e_test/fixtures/${filename}`, { force: true })
      cy.pageIsNotLoading()
      return this
    },
    reindexNotebook() {
      cy.findByRole('button', { name: 'Update index' }).click()
      // Wait for the indexing to complete - toast notification will appear
      cy.pageIsNotLoading()
      return this
    },
    shareNotebookToBazaar() {
      cy.findByRole('button', { name: 'Share notebook to bazaar' }).click()
      cy.findByRole('button', { name: 'OK' }).click()
      cy.pageIsNotLoading()
      return this
    },
    moveNotebookToCircle() {
      cy.findByRole('button', { name: 'Move to ...' }).click()
      return this
    },
  }
}

export default notebookSettingsPage
