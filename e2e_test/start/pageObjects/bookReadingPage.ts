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

  const ocrCanvasFromFirstElement = ($canvas: JQuery<HTMLElement>) => {
    const el = $canvas[0] as HTMLCanvasElement
    const scale = 2
    const offscreen = document.createElement('canvas')
    offscreen.width = el.width * scale
    offscreen.height = el.height * scale
    const ctx = offscreen.getContext('2d')!
    ctx.scale(scale, scale)
    ctx.drawImage(el, 0, 0)
    const base64 = offscreen
      .toDataURL('image/png')
      .replace(/^data:image\/png;base64,/, '')
    return cy.task('ocrCanvasImage', base64, { timeout: 60000 })
  }

  const expectCanvasOcrContains = (
    canvas: Cypress.Chainable<JQuery<HTMLElement>>,
    assertCanvas: (el: HTMLCanvasElement) => void,
    ocrMessage: string,
    substring: string
  ) =>
    canvas
      .should(($canvas) => {
        assertCanvas($canvas[0] as HTMLCanvasElement)
      })
      .then(ocrCanvasFromFirstElement)
      .then((text) => {
        expect(text as string, ocrMessage).to.contain(substring)
      })

  const assertJumpedPageCanvas = (el: HTMLCanvasElement) => {
    const chromeGeometryTolerancePx = 2
    const pageRoot = el.closest('[data-testid="book-reading-page"]')
    const nav = pageRoot?.querySelector('nav.daisy-navbar')
    if (!nav)
      throw new Error('book-reading GlobalBar (nav.daisy-navbar) not found')
    const navBottom = nav.getBoundingClientRect().bottom
    const canvasTop = el.getBoundingClientRect().top
    expect(
      canvasTop,
      'PDF page top should be at or below GlobalBar bottom after outline jump'
    ).to.be.at.least(navBottom - chromeGeometryTolerancePx)
    assertPdfCanvasHasDarkPixels(el)
  }

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
      expectCanvasOcrContains(
        cy.get('[data-testid="pdf-book-viewer"] .page canvas').first(),
        assertPdfCanvasHasDarkPixels,
        'OCR text from PDF page 1 canvas',
        'DOUGHNUT_E2E_BOOK_PAGE1'
      )
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
    expectPdfPageMarkerVisible(marker: string, pageNumber: number) {
      expectCanvasOcrContains(
        cy
          .get(
            `[data-testid="pdf-book-viewer"] .pdfViewer .page[data-page-number="${pageNumber}"] canvas`
          )
          .first(),
        assertJumpedPageCanvas,
        `OCR text from PDF page ${pageNumber} canvas`,
        marker
      )
      return this
    },
    /** Two outline rows on the same page with different bboxes → scrollTop should change. */
    expectDistinctScrollForSamePageBboxOutline(
      topTitle: string,
      bottomTitle: string
    ) {
      const minDeltaPx = 80
      const scrollTopOfPdfReadingScrollChain = () =>
        cy.get('[data-testid="pdf-book-viewer"]').then(($viewer) => {
          const el = scrollableAncestorWithinBookReadingPage(
            $viewer[0] as HTMLElement
          )
          return el.scrollTop
        })
      pageIsNotLoading()
      this.clickOutlineRowByTitle(bottomTitle)
      scrollTopOfPdfReadingScrollChain().as('scrollAfterBottomBbox')
      this.clickOutlineRowByTitle(topTitle)
      cy.get<number>('@scrollAfterBottomBbox').then((bottomScroll) => {
        scrollTopOfPdfReadingScrollChain().should((topScroll) => {
          expect(
            Math.abs(Number(bottomScroll) - Number(topScroll)),
            'scrollTop should differ after bbox targets on the same page'
          ).to.be.at.least(minDeltaPx)
        })
      })
      return this
    },
    scrollPdfBookReaderToBringPage2IntoPrimaryView() {
      pageIsNotLoading()
      cy.get(
        '[data-testid="pdf-book-viewer"] .pdfViewer .page[data-page-number="2"]'
      )
        .first()
        .scrollIntoView({ offset: { top: -100, left: 0 } })
      return this
    },
    /**
     * Scrolls the book-reading PDF container without changing pages (fixture: two bbox rows on page 1).
     * Delta is in CSS pixels; tuned to cross mineru y gap between Subtopic 1.1 and 1.2 at page-width scale.
     */
    scrollPdfBookReaderDownWithinSamePageForNextBbox() {
      const deltaPx = 420
      pageIsNotLoading()
      cy.get('[data-testid="pdf-book-viewer"]').then(($viewer) => {
        const el = scrollableAncestorWithinBookReadingPage(
          $viewer[0] as HTMLElement
        )
        el.scrollTop += deltaPx
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
  }
}

export default bookReadingPage
