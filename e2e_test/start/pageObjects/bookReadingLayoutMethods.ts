import { waitUntilAppIsNotBusy } from '../pageBase'
import {
  type BookLayoutRow,
  bookBlockRowByTitle,
  bookBlockRows,
  ensureOnBookReadingPage,
} from './bookReadingShared'

export const bookReadingLayoutMethods = () => ({
  expectBookLayoutRows(expected: BookLayoutRow[]) {
    ensureOnBookReadingPage()
    for (const row of expected) {
      bookBlockRowByTitle(row.title)
        .should('have.attr', 'data-book-block-depth', String(row.depth))
        .find('[data-testid="book-reading-book-block-guides"]')
        .should('have.attr', 'data-book-block-guide-depth', String(row.depth))
        .parents('[data-testid="book-reading-book-block"]')
        .first()
        .find('[data-testid="book-reading-book-block-guide"]')
        .should('have.length', row.depth)
    }
    return this
  },
  /**
   * Choose a book layout block (PDF or EPUB). Waits until the row is the current block
   * so PDF page jumps and EPUB spine navigation settle before the next step.
   * When the PDF page indicator is present, wait for it so page layers can settle
   * before selection highlights attach (including off-screen pages).
   */
  chooseBookBlockByTitle(title: string) {
    ensureOnBookReadingPage()
    cy.get('[data-testid="book-reading-page"]').then(($page) => {
      if (
        $page.find('[data-testid="book-reading-page-indicator"]').length > 0
      ) {
        cy.wrap($page)
          .find('[data-testid="book-reading-page-indicator"]')
          .should('be.visible')
          .and('contain', ' /')
      }
    })
    bookBlockRowByTitle(title).click()
    bookBlockRowByTitle(title).should('have.attr', 'data-current-block', 'true')
    return this
  },
  expectBookBlockIsCurrentSelectionByTitle(title: string) {
    waitUntilAppIsNotBusy()
    bookBlockRowByTitle(title).should(
      'have.attr',
      'data-current-selection',
      'true'
    )
    bookBlockRows()
      .filter('[data-current-selection="true"]')
      .should('have.length', 1)
    return this
  },
  /**
   * No single layout row is both `data-current-selection` and `data-current-block` (reading
   * position has moved away from the explicit selection).
   */
  expectBookLayoutCurrentBlockDiffersFromSelection() {
    waitUntilAppIsNotBusy()
    cy.get('[data-testid="book-reading-book-layout"]')
      .find('[data-current-selection="true"][data-current-block="true"]')
      .should('not.exist')
    bookBlockRows()
      .filter('[data-current-block="true"]')
      .should('have.length', 1)
    return this
  },
  expectBookBlockIsFocusedByTitle(title: string) {
    waitUntilAppIsNotBusy()
    bookBlockRowByTitle(title).should('be.focused')
    return this
  },
  expectBookBlockAtDepth(title: string, depth: number) {
    waitUntilAppIsNotBusy()
    bookBlockRowByTitle(title).should(
      'have.attr',
      'data-book-block-depth',
      String(depth)
    )
    return this
  },
  indentFocusedBookBlockWithTab() {
    cy.focused().trigger('keydown', {
      key: 'Tab',
      code: 'Tab',
      keyCode: 9,
      which: 9,
      bubbles: true,
      getModifierState: () => false,
    })
    return this
  },
  outdentFocusedBookBlockWithShiftTab() {
    cy.focused().trigger('keydown', {
      key: 'Tab',
      code: 'Tab',
      keyCode: 9,
      which: 9,
      shiftKey: true,
      bubbles: true,
      getModifierState: (key: string) => key === 'Shift',
    })
    return this
  },
  cancelFocusedBookBlockWithBackspace() {
    cy.focused().trigger('keydown', {
      key: 'Backspace',
      code: 'Backspace',
      keyCode: 8,
      which: 8,
      bubbles: true,
      getModifierState: () => false,
    })
    return this
  },
  expectBookBlockNotPresent(title: string) {
    waitUntilAppIsNotBusy()
    cy.get('[data-testid="book-reading-book-layout"]').should(
      'not.contain',
      title
    )
    return this
  },
  expectBookBlockIsCurrentBlockByTitle(title: string) {
    waitUntilAppIsNotBusy()
    bookBlockRowByTitle(title)
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
   * scroll so that row is not clipped. Expects a short viewport so the list overflows.
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
    bookBlockRowByTitle(title).should('be.visible')
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
  expectNewChildBlockInLayout() {
    waitUntilAppIsNotBusy()
    cy.get('[data-testid="book-reading-book-layout"]')
      .find('[data-book-block-depth="1"]')
      .should('have.length.greaterThan', 0)
    return this
  },
})
