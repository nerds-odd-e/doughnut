export const CertificatePage = (notebook?: string) => {
  return {
    expectValidCertificate() {
      cy.findByText(`is granted the Certified`).should('be.visible')
      cy.findByText(`${notebook}`).should('be.visible')
    },
    expectDate(date: string) {
      cy.findByText(date).should('be.visible')
    },
    expectExpiryDate(expires: string) {
      cy.findByTestId('expired-date').contains(expires)
    },
  }
}
