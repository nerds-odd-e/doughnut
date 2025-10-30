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
    checkLastUsedTokenTimestamp(timestamp: string) {
      const expectedDatePart = timestamp.split(' ')[0]
      // Find the row for the known label used in the scenario and inspect the 3rd column (Last used)
      cy.contains('tr', 'Tracking Test Token').within(() => {
        cy.get('td')
          .eq(2)
          .should('exist')
          .invoke('text')
          .then((text) => {
            const trimmed = text.trim()
            cy.log(`Last used displayed: ${trimmed}`)
            expect(trimmed).to.contain(expectedDatePart)
          })
      })
    },
    checkTokenWithLabelHasLastUsedTimestamp(label: string, notValue: string) {
      cy.contains('tr', label).within(() => {
        cy.get('td')
          .eq(2)
          .should('exist')
          .invoke('text')
          .should('not.equal', notValue)
      })
    },
  }
}
