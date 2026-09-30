import {
  expectDaisyDialogBoxVisible,
  openDaisyDialog,
} from '../../support/daisyModalHelpers'
import { waitUntilAppIsNotBusy } from '../pageBase'
import {
  BOOK_READING_PATHNAME,
  assertPdfCanvasIsRendered,
  expectUsesScreenWidth,
} from './bookReadingShared'

const pdfPageSelector = (pageNumber: number) =>
  `[data-testid="pdf-book-viewer"] .pdfViewer .page[data-page-number="${pageNumber}"]`

/** pdf.js lands a start within about 1% of the page height of the top, far less than the 40 pt padding it replaces. */
const START_AT_TOP_TOLERANCE_PX = 15

export const bookReadingPdfMethods = () => ({
  expectPdfPagesUseScreenWidth() {
    this.expectCurrentPage(1)
    expectUsesScreenWidth('[data-testid="pdf-book-viewer"]')
    return this
  },
  expectPdfBeginningVisible() {
    waitUntilAppIsNotBusy()
    cy.location('pathname').should('match', BOOK_READING_PATHNAME)
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
      .get(`${pdfPageSelector(pageNumber)} canvas`)
      .first()
      .should(($canvas) => {
        assertPdfCanvasIsRendered($canvas[0] as HTMLCanvasElement)
      })
    return this
  },
  scrollPdfBookReaderToPosition(pageNumber: number, normalizedY: number) {
    waitUntilAppIsNotBusy()
    cy.get(pdfPageSelector(pageNumber))
      .first()
      // @ts-expect-error Cypress ScrollIntoViewOptions omits DOM `block`
      .scrollIntoView({ block: 'start' })
      .then(($page) => {
        const pageHeight = ($page[0] as HTMLElement).getBoundingClientRect()
          .height
        const extra = Math.ceil((normalizedY / 1000) * pageHeight)
        cy.get('[data-testid="pdf-book-viewer"]').then(($viewer) => {
          cy.get('[data-testid="pdf-book-viewer"]').scrollTo(
            0,
            ($viewer[0] as HTMLElement).scrollTop + extra
          )
        })
      })
    return this
  },
  expectPdfPositionAtTopOfReader(pageNumber: number, normalizedY: number) {
    waitUntilAppIsNotBusy()
    cy.get('[data-testid="pdf-book-viewer"]').then(($viewer) => {
      const viewerTop = $viewer[0]!.getBoundingClientRect().top
      cy.get(`${pdfPageSelector(pageNumber)} canvas`)
        .first()
        .should(($canvas) => {
          const canvasRect = $canvas[0]!.getBoundingClientRect()
          const startTop =
            canvasRect.top +
            (normalizedY / 1000) * canvasRect.height -
            viewerTop
          expect(Math.abs(startTop)).to.be.lessThan(START_AT_TOP_TOLERANCE_PX)
        })
    })
    return this
  },
  scrollPdfBookReaderToBringPage2IntoPrimaryView() {
    // Block 2.2 starts at y0=89/1000 normalized on page 2.
    return this.scrollPdfBookReaderToPosition(2, 89)
  },
  /**
   * Scrolls by 37.8% of the rendered page-1 height from §1's start (y0=252 MinerU),
   * putting the view top at ≈630 MinerU — past §2's bbox bottom (y1=607) so §2 scrolls above the
   * viewport, making §2.1 (y0=631) the first visible anchor and therefore the current block.
   */
  scrollPdfBookReaderDownWithinSamePageForNextBbox() {
    waitUntilAppIsNotBusy()
    cy.get(pdfPageSelector(1))
      .first()
      .then(($page) => {
        const pageHeight = ($page[0] as HTMLElement).getBoundingClientRect()
          .height
        const deltaPx = Math.round(pageHeight * 0.378)
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
  wheelDownOverReadingControlPanel() {
    waitUntilAppIsNotBusy()
    cy.get('[data-testid="pdf-book-viewer"]').then(($viewer) => {
      cy.wrap(($viewer[0] as HTMLElement).scrollTop).as(
        'pdfScrollTopBeforeWheel'
      )
    })
    cy.get('[data-testid="book-reading-reading-control-panel"] p').trigger(
      'wheel',
      { deltaY: 300 }
    )
    return this
  },
  expectPdfScrolledDownSinceWheel() {
    cy.get<number>('@pdfScrollTopBeforeWheel').then((before) => {
      cy.get('[data-testid="pdf-book-viewer"]').should(($viewer) => {
        expect(($viewer[0] as HTMLElement).scrollTop).to.be.greaterThan(before)
      })
    })
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
