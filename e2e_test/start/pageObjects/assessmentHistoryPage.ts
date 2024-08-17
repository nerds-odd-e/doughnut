export const assumeViewAssessmentHistoryPage = () => {
  return {
    expectToFindTitle() {
      cy.findByText('Welcome To Assessment History').should('be.visible')
      return this
    },
    expectTableWithNumberOfRow(n: number) {
      cy.get('.assessment-table tbody tr').should('have.length', n)
    },
    checkAttemptResult(notebook: string, result: string) {
      cy.get('.assessment-table tbody')
        .findByText(notebook)
        .next()
        .next()
        .contains(result)
    },
  }
}

export const navigateToAssessmentHistory = () => {
  cy.visit('/assessmentHistory')
  return assumeViewAssessmentHistoryPage()
}
