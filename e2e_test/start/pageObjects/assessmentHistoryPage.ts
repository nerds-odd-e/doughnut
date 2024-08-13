export const assumeViewAssessmentHistoryPage = () => {
  return {
    expectToFindTitle() {
      cy.findByText('Welcome To Assessment History').should('be.visible')
    },
    expectTableWithNoRow() {
      cy.get('.assessment-table tbody tr').should('not.exist')
    },
  }
}

export const navigateToAssessmentHistory = () => {
  cy.visit('/assessmentHistory')
}
