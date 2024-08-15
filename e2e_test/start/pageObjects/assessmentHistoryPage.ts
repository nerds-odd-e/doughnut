export const assumeViewAssessmentHistoryPage = () => {
  return {
    expectToFindTitle() {
      cy.findByText('Welcome To Assessment History').should('be.visible')
      return this
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
    expectCertificateModal() {
      cy.contains('Congratulations on your Certificate').should('be.visible')
    },
    clickGetCertificate() {
      cy.findByText('Get Certificate').click()
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
}
