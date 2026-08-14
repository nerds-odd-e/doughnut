import { waitUntilAppIsNotBusy } from '../../pageBase'
import {
  assimilationPropertyRow,
  assimilateButtonSelector,
  returnToSequenceButtonSelector,
  reviveButtonSelector,
  skipButtonSelector,
} from './shared'

const clickPropertyRowButton = (
  propertyKey: string,
  buttonSelector: string
) => {
  assimilationPropertyRow(propertyKey)
    .scrollIntoView()
    .within(() => {
      cy.get(buttonSelector).scrollIntoView().click()
    })
}

export function assimilationPropertyFlow() {
  return {
    expandAssimilationPropertiesSection() {
      cy.get('[data-test="assimilation-properties-section"]').within(() => {
        cy.get('[data-test="assimilation-properties-toggle"]').click()
      })
      return this
    },
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
    assimilateProperty(propertyKey: string) {
      assimilationPropertyRow(propertyKey).within(() => {
        cy.get(assimilateButtonSelector).click()
      })
      waitUntilAppIsNotBusy()
      return this
    },
    skipPropertyOnPanel(propertyKey: string) {
      clickPropertyRowButton(propertyKey, skipButtonSelector)
      cy.findByRole('button', { name: 'OK' }).click()
      waitUntilAppIsNotBusy()
      return this
    },
    expectSkipForProperty(propertyKey: string) {
      assimilationPropertyRow(propertyKey).within(() => {
        cy.get(skipButtonSelector).should('exist')
        cy.get(returnToSequenceButtonSelector).should('not.exist')
        cy.get(reviveButtonSelector).should('not.exist')
      })
      return this
    },
    expectReturnToSequenceForProperty(propertyKey: string) {
      assimilationPropertyRow(propertyKey).within(() => {
        cy.get(returnToSequenceButtonSelector).should('exist')
        cy.get(skipButtonSelector).should('not.exist')
        cy.get(reviveButtonSelector).should('not.exist')
      })
      return this
    },
    returnPropertyToSequenceOnPanel(propertyKey: string) {
      clickPropertyRowButton(propertyKey, returnToSequenceButtonSelector)
      waitUntilAppIsNotBusy()
      return this
    },
    expectPropertyAssimilateDisabled(propertyKey: string) {
      assimilationPropertyRow(propertyKey).within(() => {
        cy.get(assimilateButtonSelector).should('be.disabled')
      })
      return this
    },
    expectPropertyAssimilateEnabled(propertyKey: string) {
      assimilationPropertyRow(propertyKey).within(() => {
        cy.get(assimilateButtonSelector).should('not.be.disabled')
      })
      return this
    },
  }
}
