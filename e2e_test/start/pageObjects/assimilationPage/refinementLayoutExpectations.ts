import { refinementSuggestionsPanel, waitForExtractNote } from './shared'

export function assimilationRefinementLayoutExpectations() {
  return {
    expectRefinementSuggestionsCount(count: number) {
      this.openRefineNoteModal()
      refinementSuggestionsPanel().scrollIntoView().should('be.visible')
      refinementSuggestionsPanel().find('ul li').should('have.length', count)
      return this
    },
    expectRefinementLayout(rows: Record<string, string>[]) {
      this.openRefineNoteModal()
      refinementSuggestionsPanel().scrollIntoView().should('be.visible')
      rows.forEach((row) => {
        refinementSuggestionsPanel()
          .contains(`[data-layout-level="${row.level}"] > label`, row.text)
          .should('be.visible')
          .and(($item) => {
            const itemText = $item.text()
            if (row.alreadyExtracted?.trim()) {
              expect(itemText).to.contain('Already extracted')
            } else {
              expect(itemText).not.to.contain('Already extracted')
            }
          })
      })
      return this
    },
    checkRefinementSuggestion(index: number) {
      refinementSuggestionsPanel()
        .find('input[type="checkbox"]')
        .eq(index)
        .check()
      return this
    },
    removeRefinementSuggestionsAt(indices: number[]) {
      this.openRefineNoteModal()
      indices.forEach((index) => this.checkRefinementSuggestion(index))
      cy.findByRole('button', { name: 'Remove selected' }).click()
      cy.findByRole('button', { name: 'OK' }).click()
      return this
    },
    extractLayoutPointsToNewNote(...suggestionTexts: string[]) {
      refinementSuggestionsPanel().within(() => {
        suggestionTexts.forEach((suggestionText) => {
          cy.contains('[data-layout-level] > label', suggestionText)
            .find('input[type="checkbox"]')
            .first()
            .check()
        })
        cy.findByRole('button', { name: 'Extract' }).click()
      })
      waitForExtractNote()
      return this
    },
  }
}
