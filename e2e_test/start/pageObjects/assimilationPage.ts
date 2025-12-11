import { commonSenseSplit } from 'support/string_util'

export const assumeAssimilationPage = () => ({
  expectToAssimilateAndTotal(toAssimilateAndTotal: string) {
    const [assimlatedTodayCount, toAssimilateCountForToday, totalCount] =
      toAssimilateAndTotal.split('/')

    cy.get('.daisy-progress-bar').should(
      'contain',
      `Assimilating: ${assimlatedTodayCount}/${toAssimilateCountForToday}`
    )
    // Click progress bar to show tooltip
    cy.get('.daisy-progress-bar').first().click()

    // Check tooltip content
    cy.get('.tooltip-content').within(() => {
      cy.contains(
        `Daily Progress: ${assimlatedTodayCount} / ${toAssimilateCountForToday}`
      )
      cy.contains(`Total Progress: ${assimlatedTodayCount} / ${totalCount}`)
    })

    // Close tooltip
    cy.get('.tooltip-popup').click()
  },
  assimilateWithSpellingOption() {
    cy.formField('Remember Spelling').check()
    cy.pageIsNotLoading()
    cy.findByRole('button', { name: 'Keep for repetition' }).click()
    return this
  },
  assimilate(assimilations: Record<string, string>[]) {
    assimilations.forEach((assimilation) => {
      cy.initialReviewOneNoteIfThereIs(assimilation)
    })
  },
  assimilateNotes(noteTitles: string) {
    return this.assimilate(
      commonSenseSplit(noteTitles, ', ').map((title: string) => {
        return {
          'Review Type': title === 'end' ? 'initial done' : 'single note',
          Title: title,
        }
      })
    )
  },
  waitForNote(noteTitle: string) {
    cy.findByText(noteTitle, { selector: 'main *' }).should('be.visible')
    cy.pageIsNotLoading()
    return this
  },
  selectNoteType(noteType: string) {
    cy.get('[data-test="note-type-selection-dialog"]').within(() => {
      cy.get('select').select(noteType)
      cy.pageIsNotLoading()
    })
    return this
  },
  expectNoteTypePrompt() {
    cy.get('[data-test="note-type-selection-dialog"]', {
      timeout: 5000,
    }).should('be.visible')
    return this
  },
  expectNoteTypeOptions(options: string[]) {
    options.forEach((option) => {
      cy.get('[data-test="note-type-selection-dialog"]').within(() => {
        cy.findByRole('button', { name: option }).should('be.visible')
      })
    })
    return this
  },
  expectSummaryPoints(points: string[]) {
    cy.pageIsNotLoading()
    // Wait for the summary to be generated and displayed
    cy.get('[data-test="note-details-summary"]', { timeout: 10000 }).should(
      'be.visible'
    )
    points.forEach((point) => {
      cy.get('[data-test="note-details-summary"]').should('contain', point)
    })
    return this
  },
})

export const assimilation = () => {
  const getAssimilateListItemInSidebar = (
    fn: ($el: Cypress.Chainable<JQuery<HTMLElement>>) => void
  ) => cy.get('.main-menu').within(() => fn(cy.get('li[title="Assimilate"]')))

  return {
    expectCount(numberOfNotes: number) {
      getAssimilateListItemInSidebar(($el) => {
        $el.findByText(`${numberOfNotes}`, { selector: '.due-count' })
      })
      return this
    },
    goToAssimilationPage() {
      cy.routerToRoot()
      getAssimilateListItemInSidebar(($el) => {
        $el.click()
      })
      return assumeAssimilationPage()
    },
  }
}
