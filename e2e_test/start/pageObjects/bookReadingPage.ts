import { pageIsNotLoading } from '../pageBase'

export type BookOutlineRow = { depth: number; title: string }

const bookReadingPage = () => ({
  expectBookStructureRows(expected: BookOutlineRow[]) {
    pageIsNotLoading()
    cy.location('pathname').should('match', /^\/d\/notebooks\/\d+\/book$/)
    cy.get('[data-testid="book-reading-page"]').should('exist')
    cy.get('[data-testid="book-reading-outline"]')
      .find('[data-testid="book-outline-node"]')
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
    cy.get('[data-testid="pdf-first-page-canvas"]')
      .should(($canvas) => {
        const el = $canvas[0] as HTMLCanvasElement
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
      })
      .then(($canvas) => {
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
      })
      .then((text) => {
        expect(text as string, 'OCR text from PDF page 1 canvas').to.contain(
          'DOUGHNUT_E2E_BOOK_PAGE1'
        )
      })
    return this
  },
  expectDownloadedBookPdfMatchesFixture(fixtureFilename: string) {
    pageIsNotLoading()
    cy.location('pathname').should('match', /^\/d\/notebooks\/\d+\/book$/)
    cy.get('[data-testid="book-reading-page"]').should('exist')
    cy.get('[data-testid="book-reading-download"]').should('exist').click()
    const downloadsFolder = Cypress.config('downloadsFolder') as string
    const downloadedPath = `${downloadsFolder}/${fixtureFilename}`
    return cy
      .task('fileShouldExistSoon', downloadedPath)
      .should('equal', true)
      .then(() =>
        cy.fixture(`book_reading/${fixtureFilename}`, null).then((expected) =>
          cy.readFile(downloadedPath, null).then((actual) => {
            expect(
              Cypress.Buffer.from(actual as ArrayBuffer).equals(
                Cypress.Buffer.from(expected as ArrayBuffer)
              ),
              'downloaded PDF bytes match fixture'
            ).to.be.true
          })
        )
      )
  },
})

export default bookReadingPage
