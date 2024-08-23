export const CertificatePopup = () => {
  return {
    expectCertificateFor(notebook: string) {
      const frame = cy.get('.certificate-container')
      frame.findByText('is granted the Certified').should('be.visible')
      frame.findByText(notebook).should('be.visible')
    },
    expectCertificateUser(user: string) {
      cy.get('.receiver-name').findByText(user).should('be.visible')
    },
    expectCertificateCreator(creator: string) {
      cy.get('.signature-creator-name').findByText(creator).should('be.visible')
    },
    expectDate(date: string) {
      cy.findByText(date).should('be.visible')
    },
    expectExpiryDate(expires: string) {
      cy.findByTestId('expired-date').contains(expires)
    },
  }
}
