import {
  expectDaisyDialogBoxVisible,
  openDaisyDialog,
} from '../../support/daisyModalHelpers'
import { waitUntilAppIsNotBusy } from '../pageBase'
import { assertPdfCanvasIsRendered } from './bookReadingShared'

export const bookReadingPdfMethods = () => ({
  expectPdfBeginningVisible() {
    waitUntilAppIsNotBusy()
    cy.location('pathname').should('match', /^\/notebooks\/\d+\/book$/)
    this.expectCurrentPage(1)
    return this
  },
  expectCurrentPage(pageNumber: number) {
    waitUntilAppIsNotBusy()
    cy.get('[data-testid="book-reading-page-indicator"]')
      .should('be.visible')
      .and('contain', `${pageNumber} /`)
    cy.get('[data-testid="pdf-book-viewer"]')
      .should('be.visible')
      .get(
        `[data-testid="pdf-book-viewer"] .pdfViewer .page[data-page-number="${pageNumber}"] canvas`
      )
      .first()
      .should(($canvas) => {
        assertPdfCanvasIsRendered($canvas[0] as HTMLCanvasElement)
      })
    return this
  },
  scrollPdfBookReaderToBringPage2IntoPrimaryView() {
    waitUntilAppIsNotBusy()
    const page2Sel =
      '[data-testid="pdf-book-viewer"] .pdfViewer .page[data-page-number="2"]'
    cy.get(page2Sel)
      .first()
      // @ts-expect-error Cypress ScrollIntoViewOptions omits DOM `block`
      .scrollIntoView({ block: 'start' })
    // Block 2.2 starts at y0=89/1000 normalized on page 2. Scroll that extra
    // amount so block 2.2 is at the container top, guaranteeing its y0 is below
    // the viewport midpoint even with very short test viewports (e.g. 1200×280).
    cy.get(page2Sel)
      .first()
      .then(($page) => {
        const pageHeight = ($page[0] as HTMLElement).getBoundingClientRect()
          .height
        const extra = Math.ceil((89 / 1000) * pageHeight)
        cy.get('[data-testid="pdf-book-viewer"]').then(($viewer) => {
          cy.get('[data-testid="pdf-book-viewer"]').scrollTo(
            0,
            ($viewer[0] as HTMLElement).scrollTop + extra
          )
        })
      })
    return this
  },
  /**
   * Scrolls by 42% of the rendered page-1 height from §1's click position (y≈204 MinerU),
   * giving total scroll ≈ 624 MinerU — past §2's bbox bottom (y1=608) so §2 scrolls above the
   * viewport, making §2.1 (y0=631) the first visible anchor and therefore the current block.
   */
  scrollPdfBookReaderDownWithinSamePageForNextBbox() {
    waitUntilAppIsNotBusy()
    cy.get(
      '[data-testid="pdf-book-viewer"] .pdfViewer .page[data-page-number="1"]'
    )
      .first()
      .then(($page) => {
        const pageHeight = ($page[0] as HTMLElement).getBoundingClientRect()
          .height
        const deltaPx = Math.round(pageHeight * 0.42)
        cy.get('[data-testid="pdf-book-viewer"]').then(($el) => {
          const newTop = ($el[0] as HTMLElement).scrollTop + deltaPx
          cy.get('[data-testid="pdf-book-viewer"]').scrollTo(0, newTop)
        })
      })
    return this
  },
  /**
   * Scrolls down the PDF viewer in increments until the Reading Control Panel appears,
   * asserting it contains the selected block title.
   */
  scrollPdfUntilReadingControlPanelVisible(selectedBlockTitle: string) {
    waitUntilAppIsNotBusy()
    const step = 150
    const doScroll = (remaining: number): void => {
      if (remaining <= 0) return
      cy.get('[data-testid="book-reading-reading-control-panel"]').then(
        ($panel) => {
          if ($panel.length > 0 && $panel.is(':visible')) return
          cy.get('[data-testid="pdf-book-viewer"]').then(($viewer) => {
            const el = $viewer[0] as HTMLElement
            cy.get('[data-testid="pdf-book-viewer"]').scrollTo(
              0,
              el.scrollTop + step
            )
          })
          doScroll(remaining - 1)
        }
      )
    }
    doScroll(20)
    cy.get('[data-testid="book-reading-reading-control-panel"]', {
      timeout: 10000,
    })
      .should('be.visible')
      .and('contain', selectedBlockTitle)
    return this
  },
  expectContentBlockBboxOverlaysVisible() {
    waitUntilAppIsNotBusy()
    cy.get('[data-testid="pdf-book-viewer"]')
      .find('[data-testid="book-block-selection-bbox-highlight"]')
      .should('exist')
    return this
  },
  createBookBlockFromContentBlockOnPdf() {
    waitUntilAppIsNotBusy()
    cy.get('[data-book-content-block-id]')
      .first()
      .trigger('click', { bubbles: true, force: true })
    return this
  },
  createBookBlockFromLongTextContentBlockOnPdf() {
    waitUntilAppIsNotBusy()
    cy.get(
      '[data-testid="pdf-book-viewer"] .pdfViewer .page[data-page-number="2"]'
    )
      .first()
      // @ts-expect-error Cypress ScrollIntoViewOptions omits DOM `block`
      .scrollIntoView({ block: 'start' })
    cy.get('[data-testid="book-reading-book-layout"]')
      .find('[data-current-selection="true"]')
      .click()
    cy.get('[data-derived-title-truncated="true"][data-book-content-block-id]')
      .first()
      .scrollIntoView()
    cy.get('[data-derived-title-truncated="true"][data-book-content-block-id]')
      .first()
      .trigger('click', { bubbles: true, force: true })
    return this
  },
  expectNewBlockCallout() {
    waitUntilAppIsNotBusy()
    cy.get('[data-testid="new-book-block-callout"]').should('be.visible')
    return this
  },
  confirmNewBlockCallout() {
    waitUntilAppIsNotBusy()
    cy.get('[data-testid="new-book-block-callout-confirm"]')
      .should('be.visible')
      .click()
    return this
  },
  expectTitlePromptWithDefaultTitle() {
    waitUntilAppIsNotBusy()
    const dialog = '[data-testid="new-block-title-dialog"]'
    expectDaisyDialogBoxVisible(dialog)
    cy.get('[data-testid="new-block-title-input"]')
      .should('exist')
      .should('not.have.value', '')
    return this
  },
  confirmTitlePrompt() {
    waitUntilAppIsNotBusy()
    openDaisyDialog('[data-testid="new-block-title-dialog"]')
    cy.get('[data-testid="new-block-title-confirm"]').click({ force: true })
    return this
  },
})
