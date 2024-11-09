function searchNote(searchKey: string, options: string[]) {
  options?.forEach((option: string) => cy.formField(option).check())
  cy.findByPlaceholderText('Search').clear().type(searchKey)
  cy.tick(500)
}

export const assumeNoteTargetSearchDialog = () => {
  return {
    findTarget(target: string) {
      searchNote(target, ['All My Notebooks And Subscriptions'])
      return this
    },
    findTargetWithinNotebook(target: string) {
      searchNote(target, [])
      return this
    },
    expectExactLinkTargets: (targets: string[]) => {
      cy.get('.search-result a.card-title')
        .then((elms) => Cypress._.map(elms, 'innerText'))
        .should('deep.equal', targets)
    },
    moveUnder() {
      cy.findByRole('button', { name: 'Move Under' }).click()
      cy.findByRole('button', { name: 'OK' }).click()
      cy.pageIsNotLoading()
    },
  }
}
