export const assumeWikidataSearchDialog = () => {
  cy.findByText('Search Wikidata').should('be.visible')

  const withinModalContainer = (callback: () => void) => {
    cy.findByText('Search Wikidata')
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
