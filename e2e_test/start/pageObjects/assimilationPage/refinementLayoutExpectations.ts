import { pageIsNotLoading } from '../../pageBase'
import {
  refinementLayoutPanel,
  removeRefinementLayoutButton,
  waitForExtractNote,
  waitForExtractNotePreview,
} from './shared'

type ExtractionPreviewFields = {
  newNoteTitle: string
  newNoteContent: string
  updatedOriginalNoteContent: string
}

const extractionPreviewPanel = () =>
  cy.get('[data-test-id="extraction-preview"]')

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
      this.openExtractionPreviewForLayoutPoints(...layoutPointTexts)
      this.createNoteFromExtractionPreview()
      return this
    },
    openExtractionPreviewForLayoutPoints(...layoutPointTexts: string[]) {
      showRefinementLayout.call(this)
      refinementLayoutPanel().within(() => {
        layoutPointTexts.forEach((layoutPointText) => {
          cy.contains('[data-layout-level] > label', layoutPointText)
            .find('input[type="checkbox"]')
            .first()
            .check()
        })
        cy.findByRole('button', { name: 'Extract' }).click()
      })
      waitForExtractNotePreview()
      extractionPreviewPanel().should('be.visible')
      return this
    },
    editExtractionPreviewFields(fields: ExtractionPreviewFields) {
      extractionPreviewPanel().within(() => {
        cy.get('[data-test-id="extraction-preview-new-title"]')
          .clear()
          .type(fields.newNoteTitle)
        cy.get('[data-test-id="extraction-preview-new-content"]')
          .clear()
          .type(fields.newNoteContent)
        cy.get('[data-test-id="extraction-preview-original-content"]')
          .clear()
          .type(fields.updatedOriginalNoteContent)
      })
      return this
    },
    createNoteFromExtractionPreview() {
      extractionPreviewPanel()
        .find('[data-test-id="extraction-preview-create"]')
        .click()
      waitForExtractNote()
      return this
    },
  }
}
