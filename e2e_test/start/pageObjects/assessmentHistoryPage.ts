import { CertificatePopup } from './CertificatePopup'

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
    expectCertificate(notebook: string) {
      cy.get('.assessment-table tbody')
        .findByText(notebook)
        .next()
        .next()
        .next()
        .findByText('View Certificate')
        .click()
      return CertificatePopup()
    },
    expectNoCertificate(notebook: string) {
      cy.get('.assessment-table tbody')
        .findByText(notebook)
        .next()
        .next()
        .next()
        .findByText('View Certificate')
        .should('not.exist')
    },
    viewMultipleCertificates(index: number) {
      cy.get('.assessment-table tbody')
        .findAllByText('View Certificate')
        .each(($button, $index) => {
          if ($index === index) {
            cy.wrap($button).click()
          }
        })
    },
  }
}

export const navigateToAssessmentHistory = () => {
  cy.visit('/assessmentHistory')
  return assumeViewAssessmentHistoryPage()
}
