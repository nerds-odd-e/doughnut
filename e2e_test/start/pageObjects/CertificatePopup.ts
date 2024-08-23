export const CertificatePopup = () => {
  return {
    expectCertificateFor(notebook: string) {
      const frame = cy.get('.certificate-container')
      frame.findByText('is granted the Certified').should('be.visible')
      frame.findByText(notebook).should('be.visible')
      return this
    },
    expectCertificateUser(user: string) {
      cy.get('.receiver-name').should('have.text', user).should('be.visible')
      return this
    },
    expectCertificateCreator(creator: string) {
      cy.get('.signature-creator-name')
        .should('have.text', creator)
        .should('be.visible')
      return this
    },
    expectDate(date: string) {
      cy.findByText(date).should('be.visible')
      return this
    },
    expectExpiryDate(expires: string) {
      cy.findByTestId('expired-date').contains(expires)
      return this
    },
  }
}
