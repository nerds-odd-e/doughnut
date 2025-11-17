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
  }
}
