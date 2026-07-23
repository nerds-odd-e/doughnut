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
    selectRefinementLayoutPoints(...layoutPointTexts: string[]) {
      showRefinementLayout.call(this)
      refinementLayoutPanel().within(() => {
        layoutPointTexts.forEach((layoutPointText) => {
          cy.contains('[data-layout-level] > label', layoutPointText)
            .find('input[type="checkbox"]')
            .first()
            .check()
        })
      })
      return this
    },
    openExtractionPreviewForLayoutPoints(...layoutPointTexts: string[]) {
      this.selectRefinementLayoutPoints(...layoutPointTexts)
      refinementLayoutPanel().within(() => {
        cy.findByRole('button', { name: 'Extract' }).click()
      })
      waitForExtractNotePreview()
      extractionPreviewPanel().should('be.visible')
      return this
    },
    exportExtractRequestForLayoutPoints(...layoutPointTexts: string[]) {
      this.selectRefinementLayoutPoints(...layoutPointTexts)
      refinementLayoutPanel()
        .find('[data-test-id="export-extract-request"]')
        .click()
      return this
    },
    exportBreakdownRequest() {
      showRefinementLayout.call(this)
      refinementLayoutPanel()
        .find('[data-test-id="export-breakdown-request"]')
        .click()
      return this
    },
    expectExportRequestDialogShowsAiRequestJson() {
      cy.get('[data-testid="export-textarea"]').should(($textarea) => {
        const content = ($textarea.val() as string).trim()
        expect(
          content,
          `Expected export dialog to show AI request JSON, but found: ${content}`
        ).to.not.equal('')
        expect(
          content,
          `Expected export dialog JSON to start with "{", but found: ${content}`
        ).to.match(/^\{/)
        const json = JSON.parse(content) as Record<string, unknown>
        expect(
          json,
          `Expected export JSON to include "model" and "instructions" keys, but found: ${content}`
        ).to.include.all.keys('model', 'instructions')
      })
      return this
    },
    editExtractionPreviewFields(fields: ExtractionPreviewFields) {
      extractionPreviewPanel().within(() => {
        cy.get('[data-test-id="extraction-preview-new-title"]')
          .clear()
          .invoke('val', fields.newNoteTitle)
          .trigger('input')
        cy.get('[data-test-id="extraction-preview-new-content"]')
          .clear()
          .invoke('val', fields.newNoteContent)
          .trigger('input')
        cy.get('[data-test-id="extraction-preview-original-content"]')
          .clear()
          .invoke('val', fields.updatedOriginalNoteContent)
          .trigger('input')
      })
      return this
    },
    clearExtractionPreviewNewNoteTitle() {
      extractionPreviewPanel().within(() => {
        cy.get('[data-test-id="extraction-preview-new-title"]').clear()
      })
      return this
    },
    expectExtractionPreviewCreateButtonDisabled() {
      extractionPreviewPanel()
        .find('[data-test-id="extraction-preview-create"]')
        .should('be.disabled')
      return this
    },
    expectExtractionPreviewOriginalContentTabActive() {
      extractionPreviewPanel().within(() => {
        cy.get('[data-test-id="extraction-preview-original-tab-content"]')
          .should('have.attr', 'aria-selected', 'true')
          .and('have.class', 'daisy-tab-active')
        cy.get('[data-test-id="extraction-preview-original-content"]').should(
          'be.visible'
        )
        cy.get('[data-testid="diff-left-pane"]').should('not.exist')
      })
      return this
    },
    expectExtractionPreviewOriginalContentFieldContains(content: string) {
      extractionPreviewPanel()
        .find('[data-test-id="extraction-preview-original-content"]')
        .should('have.value', content)
      return this
    },
    switchExtractionPreviewOriginalSectionToDiffTab() {
      extractionPreviewPanel()
        .find('[data-test-id="extraction-preview-original-tab-diff"]')
        .click()
      return this
    },
    expectExtractionPreviewOriginalDiffShows(
      originalContent: string,
      updatedContent: string
    ) {
      extractionPreviewPanel().within(() => {
        cy.get('[data-test-id="extraction-preview-original-tab-diff"]')
          .should('have.attr', 'aria-selected', 'true')
          .and('have.class', 'daisy-tab-active')
        cy.get('[data-test-id="extraction-preview-original-content"]').should(
          'not.exist'
        )
        cy.get('[data-testid="diff-left-pane"]').should(
          'contain.text',
          originalContent
        )
        cy.get('[data-testid="diff-right-pane"]').should(
          'contain.text',
          updatedContent
        )
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
    retryExtractionPreview() {
      extractionPreviewPanel()
        .find('[data-test-id="retry-extraction-preview"]')
        .click()
      waitForExtractNotePreview()
      return this
    },
  }
}
