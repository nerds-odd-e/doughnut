export const assumeWikidataSearchDialog = () => {
  return {
    open() {
      cy.findByRole('button', { name: 'Wikidata Id' }).click()
      cy.findByText('Search Wikidata').should('be.visible')
      return this
    },
    search(phrase: string) {
      // Type in the title field (the dialog will search automatically when opened)
      cy.formField('Title').assignFieldValue(phrase)
      return this
    },
    selectResult(wikidataID: string) {
      cy.findByText('Search Wikidata')
        .closest('.modal-container')
        .within(() => {
          cy.get('select[name="wikidataSearchResult"]').select(wikidataID)
        })
      return this
    },
    confirmAssociation() {
      cy.findByRole('button', { name: 'Confirm' }).click()
      return this
    },
    expectVisible() {
      cy.findByText('Search Wikidata').should('be.visible')
      return this
    },
    setWikidataId(wikidataId: string) {
      cy.findByText('Search Wikidata')
        .closest('.modal-container')
        .within(() => {
          cy.formField('Wikidata Id').assignFieldValue(wikidataId)
        })
      return this
    },
    close() {
      cy.findByText('Search Wikidata')
        .closest('.modal-container')
        .within(() => {
          cy.findByRole('button', { name: 'Close' }).click()
        })
      return this
    },
    expectErrorOnWikidataId(message: string) {
      this.open()
      cy.findByText('Search Wikidata')
        .closest('.modal-container')
        .within(() => {
          cy.expectFieldErrorMessage('Wikidata Id', message)
        })
      this.close()
      return this
    },
    expectWikidataIdValue(value: string) {
      this.open()
      cy.findByText('Search Wikidata')
        .closest('.modal-container')
        .within(() => {
          cy.formField('Wikidata Id').fieldShouldHaveValue(value)
          cy.findByRole('button', { name: 'Close' }).click()
        })
      return this
    },
  }
}
