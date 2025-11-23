function searchNote(searchKey: string, options: string[]) {
  options?.forEach((option: string) => cy.formField(option).check())
  cy.findByPlaceholderText('Search').clear().type(searchKey)
  cy.tick(1000)
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
      if (targets.length === 0) {
        cy.findByText('No matching notes found.').should('be.visible')
        return
      }
      cy.get('.search-result .daisy-card-title')
        .then((elms) => Cypress._.map(elms, 'innerText'))
        .should('deep.equal', targets)
    },
    expectExactDropdownTargets: (targets: string[]) => {
      cy.get('.dropdown-list a')
        .then((elms) => Cypress._.map(elms, 'innerText'))
        .should('deep.equal', targets)
    },
    moveUnder() {
      cy.findByRole('button', { name: 'Move Under' }).click()
      cy.findByRole('button', { name: 'OK' }).click()
      cy.pageIsNotLoading()
    },
    linkToTargetAs(toNoteTopic: string, linkType: string) {
      cy.clickButtonOnCardBody(toNoteTopic, 'Link')
      cy.clickRadioByLabel(linkType)
    },
    linkTopLevelNoteToTargetAs(toNoteTopic: string, linkType: string) {
      this.linkToTargetAs(toNoteTopic, linkType)
      cy.findByRole('button', { name: 'OK' }).click()
    },
    expectNoteInRecentlyUpdatedSection(noteTitle: string) {
      cy.findByText('Recently updated notes').should('be.visible')
      // Note can be in dropdown list or in cards, so search within the recent notes section
      cy.contains('.recent-notes-section', noteTitle).should('be.visible')
      return this
    },
    expectRecentlyUpdatedSectionNotVisible() {
      cy.findByText('Recently updated notes').should('not.exist')
      return this
    },
  }
}
