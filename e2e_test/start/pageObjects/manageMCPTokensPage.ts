export const manageMCPTokensPage = () => {
  return {
    generateToken(label?: string) {
      cy.findByRole('button', { name: 'Generate Token' }).click()
      return cy.get('[data-testid="token-result"]').invoke('text')
    },
    checkTokenWithLabelNotExists(label: string) {
      cy.findByText(label).should('not.exist')
    },
    checkTokenWithLabelExists(label: string) {
      cy.findByText(label).should('exist')
    },
  }
}
