import { form } from '../forms'
import { waitUntilAppIsNotBusy } from '../pageBase'

export const assumeAssociateWikidataDialog = () => {
  cy.findByText('Associate Wikidata').should('be.visible')

  const withinModalContainer = (callback: () => void) => {
    cy.findByText('Associate Wikidata')
      .closest('.modal-container')
      .within(callback)
  }

  return {
    associate(wikidataId: string) {
      withinModalContainer(() => {
        form.getField('Wikidata Id').assignValue(wikidataId).type('{enter}')
      })
      waitUntilAppIsNotBusy()
      return this
    },
    setWikidataId(wikidataId: string) {
      withinModalContainer(() => {
        form.getField('Wikidata Id').assignValue(wikidataId)
      })
      return this
    },
    selectResult(wikidataId: string) {
      withinModalContainer(() => {
        cy.get(`[data-wikidata-id="${wikidataId}"]`)
          .should('be.visible')
          .click()
      })
      waitUntilAppIsNotBusy()
      return this
    },
    confirmAssociationWithSuggestedTitle(suggestedTitle: string) {
      withinModalContainer(() => {
        cy.findByText(/Suggested Title:/)
          .should('be.visible')
          .should('contain.text', suggestedTitle)
        cy.findByText('Replace title').click()
      })
      waitUntilAppIsNotBusy()
      return this
    },
    close() {
      withinModalContainer(() => {
        cy.findByRole('button', { name: 'Close' }).click()
      })
      cy.findByText('Associate Wikidata').should('not.exist')
    },

    expectErrorOnWikidataId(message: string) {
      withinModalContainer(() => {
        form.getField('Wikidata Id').expectError(message)
      })
      this.close()
      return this
    },
    expectWikidataIdValue(value: string) {
      withinModalContainer(() => {
        form.getField('Wikidata Id').expectNoError().shouldHaveValue(value)
        cy.findByRole('button', { name: 'Close' }).click()
      })
      return this
    },
  }
}
