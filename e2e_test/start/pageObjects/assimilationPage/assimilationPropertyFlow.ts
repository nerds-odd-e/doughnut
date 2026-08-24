import { assimilationPropertyRow } from './shared'

export function assimilationPropertyFlow() {
  return {
    expectPendingAssimilationProperty(propertyKey: string) {
      cy.get('[data-test="assimilation-properties-section"]').within(() => {
        cy.get('[data-test="assimilation-properties-toggle"]').should(
          'be.checked'
        )
        assimilationPropertyRow(propertyKey)
          .should('have.attr', 'data-test-pending', 'true')
          .and('be.visible')
      })
      return this
    },
    expectPendingAssimilationPropertyAbsent(propertyKey: string) {
      assimilationPropertyRow(propertyKey).should(
        'not.have.attr',
        'data-test-pending',
        'true'
      )
      return this
    },
  }
}
