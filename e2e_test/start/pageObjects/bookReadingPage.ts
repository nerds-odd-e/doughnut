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
})

export default bookReadingPage
