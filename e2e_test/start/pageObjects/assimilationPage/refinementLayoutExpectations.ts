import { pageIsNotLoading } from '../../pageBase'
import {
  refinementLayoutPanel,
  removeRefinementLayoutButton,
  waitForExtractNote,
} from './shared'

export function assimilationRefinementLayoutExpectations() {
  const showRefinementLayout = function (this: {
    openRefineNoteModal(): unknown
  }) {
    this.openRefineNoteModal()
    refinementLayoutPanel().scrollIntoView().should('be.visible')
  }

  return {
    expectRefinementLayout(rows: Record<string, string>[]) {
      showRefinementLayout.call(this)
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
      showRefinementLayout.call(this)
      indices.forEach((index) => this.checkRefinementLayoutItem(index))
      removeRefinementLayoutButton().click()
      cy.findByRole('button', { name: 'OK' }).click()
      pageIsNotLoading()
      return this
    },
    expectNoRefinementLayoutSelection() {
      showRefinementLayout.call(this)
      refinementLayoutPanel()
        .find('input[type="checkbox"]:checked')
        .should('have.length', 0)
      removeRefinementLayoutButton().should('be.disabled')
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
