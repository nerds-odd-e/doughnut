import { form } from '../forms'

export const assumeAssociateWikidataDialog = () => {
  cy.findByText('Associate Wikidata').should('be.visible')

  const withinModalContainer = (callback: () => void) => {
    cy.findByText('Associate Wikidata')
      .closest('.modal-container')
      .within(callback)
  }

  return {
    // Actions - Input/Selection
    associate(wikiID: string) {
      withinModalContainer(() => {
        form.getField('Wikidata Id').assignValue(wikiID).type('{enter}')
      })
      return this
    },
    setWikidataId(wikidataId: string) {
      withinModalContainer(() => {
        form.getField('Wikidata Id').assignValue(wikidataId)
      })
      return this
    },
    selectResult(wikidataID: string) {
      withinModalContainer(() => {
        cy.get(`[data-wikidata-id="${wikidataID}"]`)
          .should('be.visible')
          .click()
      })
      return this
    },
    confirmAssociationWithDifferentLabel(wikidataTitle: string) {
      withinModalContainer(() => {
        // Wait for title-choice controls and check the suggested label is visible
        cy.findByText(/Suggested Title:/)
          .should('be.visible')
          .should('contain.text', wikidataTitle)
        // Select "Replace title" option - this will immediately save and close the dialog
        cy.findByText('Replace title').click()
      })
      // Dialog should close automatically after selecting Replace title
      return this
    },
    close() {
      withinModalContainer(() => {
        cy.findByRole('button', { name: 'Close' }).click()
      })
      // Wait for the dialog to fully disappear
      cy.findByText('Associate Wikidata').should('not.exist')
    },

    // Assertions
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
