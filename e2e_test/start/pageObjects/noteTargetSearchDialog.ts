import { pageIsNotLoading } from '../pageBase'
import { form } from '../forms'

const relationshipTargetListMaxAttempts = 5
const relationshipTargetListRetryMs = 400

function expectExactRelationshipTargetsWithRetry(targets: string[]) {
  const tryMatch = (attempt: number) => {
    cy.get('.search-result .daisy-card-title').then((elms) => {
      const actual = Cypress._.map(elms, 'innerText')
      if (Cypress._.isEqual(actual, targets)) {
        return
      }
      if (attempt >= relationshipTargetListMaxAttempts - 1) {
        throw new Error(
          `expected ${JSON.stringify(actual)} to deeply equal ${JSON.stringify(targets)}`
        )
      }
      cy.wait(relationshipTargetListRetryMs)
      tryMatch(attempt + 1)
    })
  }
  tryMatch(0)
}

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
      expectExactRelationshipTargetsWithRetry(targets)
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
    moveUnder(folderTitle: string, notebookName?: string) {
      let cards = cy.get('[role=card]').filter((_, el) => {
        const t = el.querySelector('.folder-hit-card-title')
        if (!t?.textContent?.includes(folderTitle)) return false
        if (notebookName && !el.textContent?.includes(notebookName))
          return false
        return true
      })
      if (notebookName) {
        cards = cards.should('have.length', 1)
      } else {
        cards = cards.should('have.length.at.least', 1).first()
      }
      cards.findByRole('button', { name: 'Move Under' }).click()
      cy.findByRole('button', { name: 'OK' }).click()
      pageIsNotLoading()
    },
    createRelationshipToTargetAs(toNoteTopic: string, relationType: string) {
      cy.contains('.search-result .daisy-card-title', toNoteTopic)
        .closest('[role=card]')
        .findByRole('button', { name: 'Add Relationship' })
        .click()
      form.getField('Relation Type').clickOption(relationType)
      pageIsNotLoading()
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

    expectNotebookNameInSearchResults(notebookName: string) {
      cy.findByText('Search result', { selector: '.result-title' }).should(
        'be.visible'
      )
      cy.get('.search-result').within(() => {
        cy.contains('.notebook-name-label', notebookName).should('be.visible')
      })
      return this
    },
  }
}
