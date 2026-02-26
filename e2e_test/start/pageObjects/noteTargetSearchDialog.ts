import { pageIsNotLoading } from '../pageBase'
import { form } from '../forms'

const findNoteCard = (title: string) =>
  cy.findByText(title, { selector: '.daisy-card-title .title-text' })

function searchNote(searchKey: string, options: string[]) {
  options?.forEach((option: string) => form.getField(option).check())
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
    expectExactRelationshipTargets: (targets: string[]) => {
      if (targets.length === 0) {
        cy.findByText('Search result', { selector: '.result-title' }).should(
          'be.visible'
        )
        cy.findByText('No matching notes found.').should('be.visible')
        return
      }
      cy.findByText('Search result', { selector: '.result-title' }).should(
        'be.visible'
      )
      cy.get('.search-result .daisy-card-title')
        .then((elms) => Cypress._.map(elms, 'innerText'))
        .should('deep.equal', targets)
    },
    expectExactDropdownTargets: (targets: string[]) => {
      if (targets.length === 0) {
        cy.findByText('Search result', { selector: '.result-title' }).should(
          'be.visible'
        )
        cy.findByText('No matching notes found.').should('be.visible')
        return
      }
      cy.findByText('Search result', { selector: '.result-title' }).should(
        'be.visible'
      )
      cy.get('.dropdown-list a')
        .then((elms) => Cypress._.map(elms, 'innerText'))
        .should('deep.equal', targets)
    },
    moveUnder() {
      cy.findByRole('button', { name: 'Move Under' }).click()
      cy.findByRole('button', { name: 'OK' }).click()
      pageIsNotLoading()
    },
    createRelationshipToTargetAs(toNoteTopic: string, relationType: string) {
      findNoteCard(toNoteTopic).then(($card) => {
        cy.wrap($card)
          .parent()
          .parent()
          .parent()
          .parent()
          .parent()
          .findByText('Add Relationship')
          .click()
      })
      form.getField('Relation Type').clickOption(relationType)
    },
    createRelationshipToTopLevelNoteAs(
      toNoteTopic: string,
      relationType: string
    ) {
      this.createRelationshipToTargetAs(toNoteTopic, relationType)
    },
    expectNoteInRecentlyUpdatedSection(noteTitle: string) {
      cy.findByText('Recently updated notes', {
        selector: '.result-title',
      }).should('be.visible')
      // Note can be in dropdown list or in cards, so search within the result section
      cy.contains('.result-section', noteTitle).should('be.visible')
      return this
    },
    expectRecentlyUpdatedSectionNotVisible() {
      cy.findByText('Recently updated notes', {
        selector: '.result-title',
      }).should('not.exist')
      return this
    },
  }
}
