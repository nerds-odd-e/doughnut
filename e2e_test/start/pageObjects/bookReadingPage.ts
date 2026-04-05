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
    cy.get('[data-testid="pdf-first-page-canvas"]').should(($canvas) => {
      const el = $canvas[0] as HTMLCanvasElement
      expect(
        el.width,
        'PDF canvas should have rendered width'
      ).to.be.greaterThan(0)
      expect(
        el.height,
        'PDF canvas should have rendered height'
      ).to.be.greaterThan(0)
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
