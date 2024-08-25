import { CertificatePopup } from './CertificatePopup'

export const assumeAssessmentAndCertificateHistoryPage = () => {
  cy.findByText('My Assessment and Certificate History').should('be.visible')

  const findNotebookCell = (notebook: string, column: number) => {
    return cy
      .get('.assessment-table tbody')
      .contains('td', notebook)
      .siblings('td')
      .eq(column)
  }

  return {
    expectTableWithNumberOfRow(n: number) {
      cy.get('.assessment-table tbody tr').should('have.length', n)
      return this
    },
    checkAttemptResult(notebook: string, result: string) {
      findNotebookCell(notebook, 1).contains(result)
    },
    expectCertificate(notebook: string) {
      findNotebookCell(notebook, 2).contains('View Certificate').click()
      return CertificatePopup()
    },
    expectNoCertificate(notebook: string) {
      findNotebookCell(notebook, 2)
        .contains('View Certificate')
        .should('not.exist')
    },
    viewCertificateAt(index: number) {
      cy.get('.assessment-table tbody')
        .findAllByText('View Certificate')
        .eq(index)
        .click()
      return CertificatePopup()
    },
  }
}

export const navigateToAssessmentAndCertificatePage = () => {
  cy.visit('/assessmentAndCertificateHistory')
  return assumeAssessmentAndCertificateHistoryPage()
}
