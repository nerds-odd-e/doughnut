export const assumeWikidataSearchDialog = () => {
  cy.findByText('Associate Wikidata').should('be.visible')

  const withinModalContainer = (callback: () => void) => {
    cy.findByText('Associate Wikidata')
      .closest('.modal-container')
      .within(callback)
  }

  return {
    selectResult(wikidataID: string) {
      withinModalContainer(() => {
        cy.get('select[name="wikidataSearchResult"]').select(wikidataID)
      })
      return this
    },
    confirmAssociation() {
      cy.findByRole('button', { name: 'Confirm' }).click()
      return this
    },
    confirmAssociationWithDifferentLabel(wikidataTitle: string) {
      withinModalContainer(() => {
        // Wait for the title options to appear and check that the wikidata title is visible
        cy.findByText(/Suggested Title:/)
          .should('be.visible')
          .should('contain.text', wikidataTitle)
        // Select "Replace title" option - this will immediately save and close the dialog
        cy.findByText('Replace title').click()
      })
      // Dialog should close automatically after selecting Replace title
      return this
    },
    setWikidataId(wikidataId: string) {
      withinModalContainer(() => {
        cy.formField('Wikidata Id').assignFieldValue(wikidataId)
      })
      return this
    },
    close() {
      withinModalContainer(() => {
        cy.findByRole('button', { name: 'Close' }).click()
      })
    },
    expectErrorOnWikidataId(message: string) {
      withinModalContainer(() => {
        cy.expectFieldErrorMessage('Wikidata Id', message)
      })
      this.close()
      return this
    },
    expectWikidataIdValue(value: string) {
      withinModalContainer(() => {
        cy.formField('Wikidata Id').fieldShouldHaveValue(value)
        cy.findByRole('button', { name: 'Close' }).click()
      })
      return this
    },
  }
}
