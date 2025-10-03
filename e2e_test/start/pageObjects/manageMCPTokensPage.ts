import submittableForm from 'start/submittableForm'

export const manageMCPTokensPage = () => {
  return {
    generateToken(label: string) {
      cy.findByRole('button', { name: 'Generate Token' }).click()
      submittableForm.submitWith({
        Label: label,
      })
      return cy.get('[data-testid="token-result"]').invoke('text')
    },
    deleteToken(label: string) {
      cy.contains('tr', label).within(() => {
        cy.findByRole('button', { name: 'Delete' }).click()
      })
      return this
    },
    checkTokenWithLabelNotExists(label: string) {
      cy.findByText(label).should('not.exist')
    },
    checkTokenWithLabelExists(label: string) {
      cy.findByText(label).should('exist')
    },
    checkTokenWithLabelNotUsed(label: string) {
      cy.contains('tr', label).within(() => {
        cy.get('td').eq(1).contains('N/A')
      })
    },
    checkTokenWithLabelHasLastUsedTimestamp(label: string) {
      cy.contains('tr', label).within(() => {
        cy.get('td')
          .eq(1)
          .should('match', /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/)
      })
    },
  }
}
