const notebookSettingsPage = () => {
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

    skipMemoryTracking() {
      cy.formField('Skip Memory Tracking Entirely').check()
      clickButton('Update')
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
    updateAiAssistantInstructions(instruction: string) {
      cy.formField('Additional Instructions to AI').type(instruction)
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
  }
}

export default notebookSettingsPage

