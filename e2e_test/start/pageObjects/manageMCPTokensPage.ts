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
    getRowByLabel(label: string) {
      const row = cy.contains('tr', label)
      return rowOfUserTokenTable(row)
    },
    checkTokenWithStatusExists(label: string, status: string) {
      this.getRowByLabel(label).statusShouldBe(status)
    },
    checkLastUsedTokenTimestamp(timestamp: string) {
      // TODO: implement actual check
      cy.findByText(timestamp).should('exist')
    },
    checkTokenWithLabelHasLastUsedTimestamp(label: string, notValue: string) {
      this.getRowByLabel(label).lastUsedTimestampShouldBe(notValue)
    },
  }
}

function rowOfUserTokenTable(
  row: Cypress.Chainable<JQuery<HTMLTableRowElement>>
) {
  return {
    statusShouldBe(status: string) {
      row.within(() => {
        cy.get('td').eq(1).should('have.text', status)
      })
    },
    lastUsedTimestampShouldBe(timestamp: string) {
      row.within(() => {
        cy.get('td').eq(2).should('have.text', timestamp)
      })
    },
  }
}
