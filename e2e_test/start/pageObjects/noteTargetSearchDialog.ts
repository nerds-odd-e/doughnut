import { pageIsNotLoading } from '../pageBase'
import { form } from '../forms'

const relationshipTargetListMaxAttempts = 5
const relationshipTargetListRetryMs = 400

/** Literal search lists note, folder, and notebook hits; relationship targets are notes only. */
export const relationshipTargetNoteTitleSelector =
  '.search-result .search-result-item-title a:not(.notebook-hit-title)'

function expectExactRelationshipTargetsWithRetry(targets: string[]) {
  const tryMatch = (attempt: number) => {
    cy.get(relationshipTargetNoteTitleSelector).then((elms) => {
      const actual = Cypress._.map(elms, (e) => e.textContent?.trim() ?? '')
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

function ensureAllMyNotebooksAndSubscriptionsScopeOn() {
  cy.findByRole('button', { name: 'All My Notebooks And Subscriptions' }).then(
    ($btn) => {
      if ($btn.is(':disabled')) {
        return
      }
      if (!$btn.hasClass('daisy-text-primary')) {
        cy.wrap($btn).click()
      }
    }
  )
}

function ensureSemanticSearchOn() {
  cy.findByRole('button', { name: 'Semantic search' }).then(($btn) => {
    if (!$btn.hasClass('daisy-text-primary')) {
      cy.wrap($btn).click()
    }
  })
}

function searchNote(searchKey: string, options: string[]) {
  if (options?.includes('All My Notebooks And Subscriptions')) {
    ensureAllMyNotebooksAndSubscriptionsScopeOn()
  }
  cy.findByPlaceholderText('Search').clear().type(searchKey)
  cy.tick(1000)
}

export const assumeNoteTargetSearchDialog = () => {
  return {
    enableSemanticSearch() {
      ensureSemanticSearchOn()
      return this
    },
    findTarget(target: string) {
      searchNote(target, ['All My Notebooks And Subscriptions'])
      return this
    },
    findTargetWithinNotebook(target: string) {
      searchNote(target, [])
      return this
    },
    expectNoRelationshipTargetNotes() {
      cy.findByText('Search result', { selector: '.result-title' }).should(
        'be.visible'
      )
      cy.get(relationshipTargetNoteTitleSelector).should('have.length', 0)
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
      cy.get('.dropdown-list a:not(.notebook-hit-title)')
        .then((elms) => Cypress._.map(elms, (e) => e.textContent?.trim() ?? ''))
        .should('deep.equal', targets)
    },
    moveUnder(folderTitle: string, notebookName?: string) {
      let cards = cy.get('[role=listitem]').filter((_, el) => {
        const t = el.querySelector('.folder-hit-title')
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
      cy.get('.search-result [role=listitem]')
        .filter((_, el) => {
          const a = el.querySelector(
            '.search-result-item-title a:not(.notebook-hit-title)'
          )
          return a?.textContent?.trim() === toNoteTopic
        })
        .findByRole('button', { name: 'Add link' })
        .click()
      cy.findByRole('button', { name: 'Add a new relationship note' }).click()
      form.getField('Relation Type').clickOption(relationType)
      pageIsNotLoading()
    },
    insertWikiLinkToTarget(toNoteTopic: string) {
      cy.get('.search-result [role=listitem]')
        .filter((_, el) => {
          const a = el.querySelector(
            '.search-result-item-title a:not(.notebook-hit-title)'
          )
          return a?.textContent?.trim() === toNoteTopic
        })
        .findByRole('button', { name: 'Add link' })
        .click()
      cy.findByRole('button', { name: 'Insert as a wiki link' }).click()
      pageIsNotLoading()
    },
    expectNoteInRecentlyUpdatedSection(noteTitle: string) {
      cy.findByText('Recently updated notes', {
        selector: '.result-title',
      }).should('be.visible')
      // Note can be in the dropdown list or the scrollable list panel
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
