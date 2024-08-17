export const assumeViewAssessmentHistoryPage = () => {
  cy.findByText('Welcome To Assessment History').should('be.visible')

  return {
    expectTableWithNumberOfRow(n: number) {
      cy.get('.assessment-table tbody tr').should('have.length', n)
      return this
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
