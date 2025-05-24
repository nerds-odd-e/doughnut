export const manageMCPTokensPage = () => {
  return {
    generateToken() {
      cy.findByRole('button', { name: 'Generate Token' }).click()
      return cy.get('[data-testid="token-result"]').invoke('text')
    },
  }
}
