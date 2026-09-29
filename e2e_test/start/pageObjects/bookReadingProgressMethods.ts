import { waitUntilAppIsNotBusy } from '../pageBase'
import router from '../router'
import {
  bookBlockRowByTitle,
  bookBlockRows,
  expectFullyOnScreen,
  notebookIdFromBookReadingPathname,
} from './bookReadingShared'

const markedBookBlockRow =
  '[data-direct-content-read], [data-direct-content-skimmed], [data-direct-content-skipped]'

/** The mark control is the sibling right after the chosen, marked row. */
const chooseInBookBlockMarkMenu = (
  blockTitle: string,
  optionTestId: string
) => {
  waitUntilAppIsNotBusy()
  bookBlockRowByTitle(blockTitle)
    .next('[data-testid="book-reading-book-block-mark-control"]')
    .find('[data-testid="book-reading-book-block-mark"]')
    .click()
  cy.get(`[data-testid="${optionTestId}"]`).click()
  waitUntilAppIsNotBusy()
}

export const bookReadingProgressMethods = () => ({
  expectReadingControlPanelFullyOnScreen() {
    waitUntilAppIsNotBusy()
    expectFullyOnScreen('[data-testid="book-reading-reading-control-panel"]')
    return this
  },
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
  changeBookBlockMarkToReadInBookLayout(blockTitle: string) {
    chooseInBookBlockMarkMenu(blockTitle, 'book-reading-change-mark-to-read')
    return this
  },
  clearBookBlockMarkInBookLayout(blockTitle: string) {
    chooseInBookBlockMarkMenu(blockTitle, 'book-reading-clear-mark')
    return this
  },
  /**
   * Book layout row marked as read: `data-direct-content-read="true"` plus success right border
   * and screen-reader “Marked as read” on the row.
   */
  expectBookBlockMarkedAsReadInBookLayout(title: string) {
    waitUntilAppIsNotBusy()
    bookBlockRowByTitle(title).should(
      'have.attr',
      'data-direct-content-read',
      'true'
    )
    return this
  },
  expectBookBlockMarkedAsSkimmedInBookLayout(title: string) {
    waitUntilAppIsNotBusy()
    bookBlockRowByTitle(title).should(
      'have.attr',
      'data-direct-content-skimmed',
      'true'
    )
    return this
  },
  expectBookBlockNotMarkedInBookLayout(title: string) {
    waitUntilAppIsNotBusy()
    bookBlockRowByTitle(title).should('not.match', markedBookBlockRow)
    return this
  },
  expectNoBookBlockMarkedInBookLayout() {
    waitUntilAppIsNotBusy()
    bookBlockRows().filter(markedBookBlockRow).should('have.length', 0)
    return this
  },
  /** Remounts the book reading page, reloading its records from the server. */
  openBookAgain() {
    waitUntilAppIsNotBusy()
    cy.location('pathname').then((pathname) => {
      router().visitNamed('bookReading', {
        notebookId: notebookIdFromBookReadingPathname(String(pathname)),
      })
    })
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
