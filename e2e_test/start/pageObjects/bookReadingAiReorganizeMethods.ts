import {
  expectDaisyDialogBoxVisible,
  openDaisyDialog,
} from '../../support/daisyModalHelpers'
import { waitUntilAppIsNotBusy } from '../pageBase'

export const bookReadingAiReorganizeMethods = () => ({
  requestAiReorganizationOfBookLayout() {
    waitUntilAppIsNotBusy()
    cy.get('[data-testid="book-reading-ai-reorganize-layout"]')
      .should('be.visible')
      .click()
    cy.get('[data-testid="book-reading-layout-full-busy"]', {
      timeout: 60000,
    }).should('not.exist')
    return this
  },
  expectReorganizationPreviewDialog() {
    waitUntilAppIsNotBusy()
    const dialog = '[data-testid="book-layout-reorganize-preview-dialog"]'
    expectDaisyDialogBoxVisible(dialog)
    cy.get('#book-layout-reorganize-preview-title').should(
      'contain',
      'Reorganize book layout (preview)'
    )
    return this
  },
  expectReorganizationPreviewBlockSuggestedDepth(
    blockTitle: string,
    suggestedDepth: number
  ) {
    waitUntilAppIsNotBusy()
    const dialog = '[data-testid="book-layout-reorganize-preview-dialog"]'
    expectDaisyDialogBoxVisible(dialog)
    cy.get(`${dialog}.daisy-modal-open .daisy-modal-box`).within(() => {
      cy.contains(
        '[data-testid="book-layout-reorganize-preview-row"]',
        blockTitle
      )
        .should('be.visible')
        .and('have.attr', 'data-suggested-depth', String(suggestedDepth))
    })
    return this
  },
  confirmAiReorganizeSuggestion() {
    waitUntilAppIsNotBusy()
    openDaisyDialog('[data-testid="book-layout-reorganize-preview-dialog"]')
    cy.get('[data-testid="book-layout-reorganize-preview-confirm"]').click({
      force: true,
    })
    waitUntilAppIsNotBusy()
    return this
  },
})
