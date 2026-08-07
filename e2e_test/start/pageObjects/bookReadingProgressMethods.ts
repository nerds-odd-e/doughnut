import { waitUntilAppIsNotBusy } from '../pageBase'

export const bookReadingProgressMethods = () => ({
  /**
   * Reading Control Panel: bottom of PDF main pane.
   * Contract for production: data-testid book-reading-reading-control-panel + book-reading-mark-as-read.
   */
  markBookBlockAsReadInReadingControlPanel(blockTitle: string) {
    waitUntilAppIsNotBusy()
    cy.get('[data-testid="book-reading-reading-control-panel"]')
      .should('be.visible')
      .and('contain', blockTitle)
    cy.get('[data-testid="book-reading-mark-as-read"]')
      .should('be.visible')
      .click()
    return this
  },
  markBookBlockAsSkimmedInReadingControlPanel(blockTitle: string) {
    waitUntilAppIsNotBusy()
    cy.get('[data-testid="book-reading-reading-control-panel"]')
      .should('be.visible')
      .and('contain', blockTitle)
    cy.get('[data-testid="book-reading-mark-as-skimmed"]')
      .should('be.visible')
      .click()
    return this
  },
  expectCurrentBlockNavigationBar(title: string) {
    waitUntilAppIsNotBusy()
    cy.get('[data-testid="current-block-navigation-bar"]', { timeout: 10000 })
      .should('be.visible')
      .and('contain', title)
    return this
  },
  expectCurrentBlockNavigationBarNotVisible() {
    waitUntilAppIsNotBusy()
    cy.get('[data-testid="current-block-navigation-bar"]').should('not.exist')
    return this
  },
  startReadingFromCurrentBlock() {
    waitUntilAppIsNotBusy()
    cy.get('[data-testid="read-from-here"]').should('be.visible').click()
    return this
  },
  goBackToSelectedBookBlock() {
    waitUntilAppIsNotBusy()
    cy.get('[data-testid="back-to-selected"]').should('be.visible').click()
    return this
  },
})
