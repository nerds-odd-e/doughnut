import { pageIsNotLoading } from '../pageBase'

export type BookOutlineRow = { depth: number; title: string }

function scrollableAncestorWithinBookReadingPage(viewerEl: HTMLElement) {
  const pageRoot = viewerEl.closest('[data-testid="book-reading-page"]')
  if (!pageRoot) throw new Error('book-reading-page not found')
  let el: HTMLElement | null = viewerEl
  while (el && pageRoot.contains(el)) {
    if (el.scrollHeight > el.clientHeight + 1) {
      return el
    }
    el = el.parentElement
  }
  return viewerEl
}

const bookReadingPage = () => {
  const outlineNodes = () =>
    cy
      .get('[data-testid="book-reading-outline"]')
      .find('[data-testid="book-outline-node"]')

  const assertPdfCanvasHasDarkPixels = (el: HTMLCanvasElement) => {
    expect(el.width, 'PDF canvas should have width').to.be.greaterThan(0)
    const ctx = el.getContext('2d')
    if (!ctx) throw new Error('No 2d context on PDF canvas')
    const sampleW = Math.min(el.width, 200)
    const sampleH = Math.min(el.height, 200)
    const data = ctx.getImageData(0, 0, sampleW, sampleH).data
    let darkPixels = 0
    for (let i = 0; i < data.length; i += 4) {
      if ((data[i] ?? 255) < 128 && (data[i + 3] ?? 0) > 128) darkPixels++
    }
    expect(
      darkPixels,
      'PDF canvas should have dark pixels (text rendered)'
    ).to.be.greaterThan(0)
  }

  const ocrPngBase64 = (base64: string) =>
    cy.task('ocrCanvasImage', base64, { timeout: 60000 })

  return {
    expectBookStructureRows(expected: BookOutlineRow[]) {
      pageIsNotLoading()
      cy.location('pathname').should('match', /^\/d\/notebooks\/\d+\/book$/)
      cy.get('[data-testid="book-reading-page"]').should('exist')
      outlineNodes()
        .should('have.length', expected.length)
        .each(($el, i) => {
          const row = expected[i]!
          cy.wrap($el)
            .should('have.attr', 'data-outline-depth', String(row.depth))
            .and('contain', row.title)
        })
      return this
    },
    expectPdfBeginningVisible() {
      this.expectCurrentPage(1).expectVisibleOCRContains('Code Refactoring')
      return this
    },
    clickOutlineRowByTitle(title: string) {
      pageIsNotLoading()
      outlineNodes().contains(title).click()
      return this
    },
    expectOutlineRowSelectedByTitle(title: string) {
      pageIsNotLoading()
      const row = outlineNodes().contains(title)
      row.should('have.attr', 'data-outline-selected', 'true')
      outlineNodes()
        .filter('[data-outline-selected="true"]')
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
     * Scrolls by 30% of the rendered page-1 height so §2.1 (at ~63% of the page) lands in the
     * upper two-thirds of the visible area. Using a fraction of page height instead of fixed CSS
     * pixels makes the scroll viewport-size- and scale-independent.
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
          const deltaPx = Math.round(pageHeight * 0.3)
          cy.get('[data-testid="pdf-book-viewer"]').then(($viewer) => {
            const el = scrollableAncestorWithinBookReadingPage(
              $viewer[0] as HTMLElement
            )
            el.scrollTop += deltaPx
          })
        })
      return this
    },
    expectOutlineRowViewportCurrentByTitle(title: string) {
      pageIsNotLoading()
      const row = outlineNodes().contains(title)
      row
        .should('have.attr', 'data-outline-current', 'true')
        .and('have.attr', 'aria-current', 'location')
      outlineNodes()
        .filter('[data-outline-current="true"]')
        .should('have.length', 1)
      return this
    },
    setBookReadingViewport(width: number, height: number) {
      cy.viewport(width, height)
      return this
    },
    /**
     * After PDF scroll updates viewport-current to a lower row, the outline aside should
     * scroll so that row is not clipped (Phase 6.5). Expects a short viewport so the list overflows.
     */
    expectViewportCurrentOutlineRowVisibleInAside(title: string) {
      this.expectOutlineRowViewportCurrentByTitle(title)
      cy.get('[data-testid="book-reading-outline-aside"]').should(($aside) => {
        expect(
          $aside[0]!.scrollTop,
          'outline aside should have scrolled to reveal the current row'
        ).to.be.greaterThan(0)
      })
      outlineNodes().contains(title).should('be.visible')
      return this
    },
  }
}

export default bookReadingPage
