import { pageIsNotLoading } from '../pageBase'

export type BookLayoutRow = { depth: number; title: string }

const bookReadingPage = () => {
  const bookBlockRows = () =>
    cy
      .get('[data-testid="book-reading-book-layout"]')
      .find('[data-testid="book-reading-book-block"]')

  const assertPdfCanvasHasDarkPixels = (el: HTMLCanvasElement) => {
    expect(el.width, 'PDF canvas should have width').to.be.greaterThan(0)
    const ctx = el.getContext('2d')
    if (!ctx) throw new Error('No 2d context on PDF canvas')
    const w = el.width
    const h = el.height
    const data = ctx.getImageData(0, 0, w, h).data
    let darkPixels = 0
    const stride = 6
    for (let y = 0; y < h; y += stride) {
      for (let x = 0; x < w; x += stride) {
        const i = (y * w + x) * 4
        if ((data[i] ?? 255) < 128 && (data[i + 3] ?? 0) > 128) darkPixels++
      }
    }
    expect(
      darkPixels,
      'PDF canvas should have dark pixels (text rendered)'
    ).to.be.greaterThan(0)
  }

  const ocrPngBase64 = (base64: string) =>
    cy.task('ocrCanvasImage', base64, { timeout: 60000 })

  return {
    expectBookLayoutRows(expected: BookLayoutRow[]) {
      pageIsNotLoading()
      cy.location('pathname').should('match', /^\/d\/notebooks\/\d+\/book$/)
      cy.get('[data-testid="book-reading-page"]').should('exist')
      bookBlockRows()
        .should('have.length', expected.length)
        .each(($el, i) => {
          const row = expected[i]!
          cy.wrap($el)
            .should('have.attr', 'data-book-block-depth', String(row.depth))
            .and('contain', row.title)
        })
      return this
    },
    expectPdfBeginningVisible() {
      this.expectCurrentPage(1).expectVisibleOCRContains('Code Refactoring')
      return this
    },
    clickBookBlockByTitle(title: string) {
      pageIsNotLoading()
      bookBlockRows().contains(title).click()
      bookBlockRows()
        .contains(title)
        .should('have.attr', 'data-current-block', 'true')
      return this
    },
    expectBookBlockIsCurrentSelectionByTitle(title: string) {
      pageIsNotLoading()
      const row = bookBlockRows().contains(title)
      row.should('have.attr', 'data-current-selection', 'true')
      bookBlockRows()
        .filter('[data-current-selection="true"]')
        .should('have.length', 1)
      return this
    },
    expectCurrentPage(pageNumber: number) {
      pageIsNotLoading()
      cy.get('[data-testid="book-reading-page-indicator"]')
        .should('be.visible')
        .and('contain', `${pageNumber} /`)
      const afterPageCanvasInk = cy
        .get('[data-testid="pdf-book-viewer"]')
        .should('be.visible')
        .get(
          `[data-testid="pdf-book-viewer"] .pdfViewer .page[data-page-number="${pageNumber}"] canvas`
        )
        .first()
        .should(($canvas) => {
          assertPdfCanvasHasDarkPixels($canvas[0] as HTMLCanvasElement)
        })
      return {
        expectVisibleOCRContains(marker: string) {
          return afterPageCanvasInk.then(() => {
            let screenshotPath = ''
            return cy
              .get('[data-testid="pdf-book-viewer"]')
              .screenshot('book-reading-pdf-viewer-ocr', {
                log: false,
                overwrite: true,
                onAfterScreenshot(_$el, props) {
                  screenshotPath = props.path
                },
              })
              .then(() => {
                expect(
                  screenshotPath,
                  'Cypress element screenshot path'
                ).to.not.equal('')
                return cy.readFile(screenshotPath, 'base64', {
                  timeout: 30000,
                })
              })
              .then((base64) => ocrPngBase64(base64 as string))
              .then((text) => {
                expect(
                  text as string,
                  `OCR text from visible PDF book viewer (element screenshot); expect content from page ${pageNumber} in view`
                ).to.contain(marker)
              })
          })
        },
      }
    },
    scrollPdfBookReaderToBringPage2IntoPrimaryView() {
      pageIsNotLoading()
      cy.get(
        '[data-testid="pdf-book-viewer"] .pdfViewer .page[data-page-number="2"]'
      )
        .first()
        // @ts-expect-error Cypress ScrollIntoViewOptions omits DOM `block`
        .scrollIntoView({ block: 'start' })
      return this
    },
    /**
     * Scroll the PDF until the book layout row identified by `blockTitle` is the viewport-derived
     * current block (same DOM contract as expectBookBlockIsCurrentBlockByTitle).
     * For refactoring.pdf, §2.2 is anchored on page 2.
     */
    scrollPdfBookReaderToMakeBookBlockCurrent(blockTitle: string) {
      pageIsNotLoading()
      if (!blockTitle.includes('2.2 Refactoring as Strengthening')) {
        throw new Error(
          `scrollPdfBookReaderToMakeBookBlockCurrent: unsupported book block (add scroll strategy): ${blockTitle}`
        )
      }
      this.scrollPdfBookReaderToBringPage2IntoPrimaryView()
      cy.wait(200)
      this.expectBookBlockIsCurrentBlockByTitle(blockTitle)
      return this
    },
    /**
     * Scrolls by 42% of the rendered page-1 height from §1's click position (y≈204 MinerU),
     * giving total scroll ≈ 624 MinerU — past §2's bbox bottom (y1=608) so §2 scrolls above the
     * viewport, making §2.1 (y0=631) the first visible anchor and therefore the current block.
     */
    scrollPdfBookReaderDownWithinSamePageForNextBbox() {
      pageIsNotLoading()
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
    expectBookBlockIsCurrentBlockByTitle(title: string) {
      pageIsNotLoading()
      const row = bookBlockRows().contains(title)
      row
        .should('have.attr', 'data-current-block', 'true')
        .and('have.attr', 'aria-current', 'location')
      bookBlockRows()
        .filter('[data-current-block="true"]')
        .should('have.length', 1)
      return this
    },
    setBookReadingViewport(width: number, height: number) {
      cy.viewport(width, height)
      return this
    },
    /**
     * After PDF scroll updates the current block to a lower book block, the book layout aside should
     * scroll so that row is not clipped (Phase 6.5). Expects a short viewport so the list overflows.
     */
    expectCurrentBlockVisibleInBookLayoutAside(title: string) {
      this.expectBookBlockIsCurrentBlockByTitle(title)
      cy.get('[data-testid="book-reading-book-layout-aside"]').should(
        ($aside) => {
          expect(
            $aside[0]!.scrollTop,
            'book layout aside should have scrolled to reveal the current block'
          ).to.be.greaterThan(0)
        }
      )
      bookBlockRows().contains(title).should('be.visible')
      return this
    },
    /**
     * Reading Control Panel (Phase 2 reading record): bottom of PDF main pane.
     * Contract for production: data-testid book-reading-reading-control-panel + book-reading-mark-as-read.
     */
    markBookBlockAsReadInReadingControlPanel(blockTitle: string) {
      pageIsNotLoading()
      cy.get('[data-testid="book-reading-reading-control-panel"]')
        .should('be.visible')
        .and('contain', blockTitle)
      cy.get('[data-testid="book-reading-mark-as-read"]')
        .should('be.visible')
        .click()
      return this
    },
    /**
     * Book layout row marked as read: `data-direct-content-read="true"` plus success right border
     * and screen-reader “Marked as read” on the row.
     */
    expectBookBlockMarkedAsReadInBookLayout(title: string) {
      pageIsNotLoading()
      bookBlockRows()
        .contains(title)
        .should('have.attr', 'data-direct-content-read', 'true')
      return this
    },
  }
}

export default bookReadingPage
