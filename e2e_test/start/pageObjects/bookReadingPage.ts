import { pageIsNotLoading } from '../pageBase'

export type BookOutlineRow = { depth: number; title: string }

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
      cy.get('[data-testid="pdf-book-viewer"] .page canvas')
        .first()
        .should(($canvas) => {
          assertPdfCanvasHasDarkPixels($canvas[0] as HTMLCanvasElement)
        })
        .then(ocrCanvasFromFirstElement)
        .then((text) => {
          expect(text as string, 'OCR text from PDF page 1 canvas').to.contain(
            'DOUGHNUT_E2E_BOOK_PAGE1'
          )
        })
      return this
    },
    clickOutlineRowByTitle(title: string) {
      pageIsNotLoading()
      outlineNodes().contains(title).click()
      return this
    },
    expectOutlineRowSelectedByTitle(title: string) {
      pageIsNotLoading()
      outlineNodes()
        .contains(title)
        .should('have.attr', 'aria-current', 'location')
      outlineNodes()
        .filter('[aria-current="location"]')
        .should('have.length', 1)
      return this
    },
    expectPdfPageMarkerVisible(marker: string, pageNumber: number) {
      const chromeGeometryTolerancePx = 2

      cy.get(
        `[data-testid="pdf-book-viewer"] .pdfViewer .page[data-page-number="${pageNumber}"] canvas`
      )
        .first()
        .should(($canvas) => {
          const el = $canvas[0] as HTMLCanvasElement
          const pageRoot = el.closest('[data-testid="book-reading-page"]')
          const nav = pageRoot?.querySelector('nav.daisy-navbar')
          if (!nav)
            throw new Error(
              'book-reading GlobalBar (nav.daisy-navbar) not found'
            )
          const navBottom = nav.getBoundingClientRect().bottom
          const canvasTop = el.getBoundingClientRect().top
          expect(
            canvasTop,
            'PDF page top should be at or below GlobalBar bottom after outline jump'
          ).to.be.at.least(navBottom - chromeGeometryTolerancePx)
          assertPdfCanvasHasDarkPixels(el)
        })
        .then(ocrCanvasFromFirstElement)
        .then((text) => {
          expect(
            text as string,
            `OCR text from PDF page ${pageNumber} canvas`
          ).to.contain(marker)
        })
      return this
    },
  }
}

export default bookReadingPage
