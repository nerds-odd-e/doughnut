import { waitUntilAppIsNotBusy } from '../../pageBase'
import {
  assimilationPropertyRow,
  assimilateButtonSelector,
  reviveButtonSelector,
  skipRecallButtonSelector,
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
    skipRecallProperty(propertyKey: string) {
      clickPropertyRowButton(propertyKey, skipRecallButtonSelector)
      cy.findByRole('button', { name: 'OK' }).click()
      waitUntilAppIsNotBusy()
      return this
    },
    reviveRecallProperty(propertyKey: string) {
      clickPropertyRowButton(propertyKey, reviveButtonSelector)
      waitUntilAppIsNotBusy()
      return this
    },
    expectReviveForProperty(propertyKey: string) {
      assimilationPropertyRow(propertyKey).within(() => {
        cy.get(reviveButtonSelector).should('exist')
        cy.root().then(($root) => {
          expect($root.find(skipRecallButtonSelector).length).to.equal(0)
        })
      })
      return this
    },
    expectSkipRecallForProperty(propertyKey: string) {
      assimilationPropertyRow(propertyKey).within(() => {
        cy.get(skipRecallButtonSelector).should('exist')
        cy.root().then(($root) => {
          expect($root.find(reviveButtonSelector).length).to.equal(0)
        })
      })
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
