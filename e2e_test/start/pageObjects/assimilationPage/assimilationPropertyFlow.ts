import { waitUntilAppIsNotBusy } from '../../pageBase'
import {
  assimilationPropertyRow,
  assimilateButtonSelector,
  removeFromRecallButtonSelector,
  returnToSequenceButtonSelector,
  secondaryActionSelectors,
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

const confirmPropertyRowButton = (
  propertyKey: string,
  buttonSelector: string
) => {
  clickPropertyRowButton(propertyKey, buttonSelector)
  cy.findByRole('button', { name: 'OK' }).click()
  waitUntilAppIsNotBusy()
}

const expectPropertySecondaryAction = (
  propertyKey: string,
  present: keyof typeof secondaryActionSelectors
) => {
  assimilationPropertyRow(propertyKey).within(() => {
    for (const [name, selector] of Object.entries(secondaryActionSelectors)) {
      cy.get(selector).should(name === present ? 'exist' : 'not.exist')
    }
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
      confirmPropertyRowButton(propertyKey, skipButtonSelector)
      return this
    },
    expectSkipForProperty(propertyKey: string) {
      expectPropertySecondaryAction(propertyKey, 'skip')
      return this
    },
    expectReturnToSequenceForProperty(propertyKey: string) {
      expectPropertySecondaryAction(propertyKey, 'returnToSequence')
      return this
    },
    expectRemoveFromRecallForProperty(propertyKey: string) {
      expectPropertySecondaryAction(propertyKey, 'removeFromRecall')
      return this
    },
    expectReviveForProperty(propertyKey: string) {
      expectPropertySecondaryAction(propertyKey, 'revive')
      return this
    },
    removePropertyFromRecallOnPanel(propertyKey: string) {
      confirmPropertyRowButton(propertyKey, removeFromRecallButtonSelector)
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
