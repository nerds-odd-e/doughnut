export const assumeWikidataSearchDialog = () => {
  return {
    open() {
      cy.findByRole('button', { name: 'Wikidata Id' }).click()
      cy.findByText('Search Wikidata').should('be.visible')
      return this
    },
    search(phrase: string) {
      cy.focused().clear().type(phrase)
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
