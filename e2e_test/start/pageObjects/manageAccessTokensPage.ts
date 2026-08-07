import { waitUntilAppIsNotBusy } from '../pageBase'
import { submittableForm } from 'start/forms'

export const visitManageAccessTokensPage = () => {
  cy.visit('/settings/access-tokens')
  waitUntilAppIsNotBusy()
  return manageAccessTokensPage()
}

export const manageAccessTokensPage = () => {
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
      cy.findByRole('button', { name: 'OK' }).click()
      waitUntilAppIsNotBusy()
      return this
    },
    expectTokenWithLabelNotListed(label: string) {
      cy.findByText(label).should('not.exist')
      return this
    },
    expectTokenWithLabelListed(label: string) {
      cy.contains(label).should(($el) => {
        expect(
          $el.text().trim(),
          `Expected token label "${label}" to be listed`
        ).to.equal(label)
      })
      return this
    },
  }
}
