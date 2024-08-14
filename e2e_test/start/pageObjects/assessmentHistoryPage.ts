export const assumeViewAssessmentHistoryPage = () => {
  return {
    expectToFindTitle() {
      cy.findByText('Welcome To Assessment History').should('be.visible')
    },
    expectTableWithNumberOfRow(n: number) {
      cy.get('.assessment-table tbody tr').should('have.length', n)
    },
  }
}

export const navigateToAssessmentHistory = () => {
  cy.visit('/assessmentHistory')
}
