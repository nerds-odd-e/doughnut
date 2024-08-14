export const assumeViewAssessmentHistoryPage = () => {
  return {
    expectToFindTitle() {
      cy.findByText('Welcome To Assessment History').should('be.visible')
    },
    expectTableWithNumberOfRow(n: number) {
      cy.get('.assessment-table tbody tr').should('have.length', n)
    },
    expectEnabledCertificateButton() {
      cy.contains('button', 'Get Certificate').should('be.enabled')
    },
    expectDisabledCertificateButton() {
      cy.contains('button', 'Get Certificate').should('be.disabled')
    },
  }
}

export const navigateToAssessmentHistory = () => {
  cy.visit('/assessmentHistory')
}
