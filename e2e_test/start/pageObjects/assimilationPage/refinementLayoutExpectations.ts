import { refinementLayoutPanel, waitForExtractNote } from './shared'

export function assimilationRefinementLayoutExpectations() {
  return {
    expectRefinementLayout(rows: Record<string, string>[]) {
      this.openRefineNoteModal()
      refinementLayoutPanel().scrollIntoView().should('be.visible')
      rows.forEach((row) => {
        refinementLayoutPanel()
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
    checkRefinementLayoutItem(index: number) {
      refinementLayoutPanel().find('input[type="checkbox"]').eq(index).check()
      return this
    },
    removeRefinementLayoutItemsAt(indices: number[]) {
      this.openRefineNoteModal()
      indices.forEach((index) => this.checkRefinementLayoutItem(index))
      cy.findByRole('button', { name: 'Remove selected' }).click()
      cy.findByRole('button', { name: 'OK' }).click()
      return this
    },
    extractLayoutPointsToNewNote(...layoutPointTexts: string[]) {
      refinementLayoutPanel().within(() => {
        layoutPointTexts.forEach((layoutPointText) => {
          cy.contains('[data-layout-level] > label', layoutPointText)
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
