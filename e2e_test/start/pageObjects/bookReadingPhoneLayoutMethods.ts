import { pageBar } from '../pageBase'
import {
  bookLayoutAside,
  bookLayoutToggle,
  ensureOnBookReadingPage,
  expectFullyOnScreen,
} from './bookReadingShared'

/** The book layout as a drawer toggled from the reading view on narrow screens. */
export const bookReadingPhoneLayoutMethods = () => ({
  /** A real (not forced) click, so it fails when something covers the toggle. */
  clickBookLayoutToggle() {
    ensureOnBookReadingPage()
    bookLayoutToggle().click()
    return this
  },
  expectBookLayoutOpen() {
    bookLayoutToggle().should('have.attr', 'aria-expanded', 'true')
    cy.get(bookLayoutAside).should('be.visible')
    expectFullyOnScreen(bookLayoutAside)
    return this
  },
  expectBookLayoutClosed() {
    bookLayoutToggle().should('have.attr', 'aria-expanded', 'false')
    return this
  },
  expectBookLayoutBelowPageBar() {
    pageBar().then(($bar) => {
      const barBottom = $bar[0]!.getBoundingClientRect().bottom
      cy.get(bookLayoutAside).should(($aside) => {
        expect(
          $aside[0]!.getBoundingClientRect().top,
          'book layout top'
        ).to.be.at.least(barBottom)
      })
    })
    return this
  },
  /** Tap the page beside the open phone drawer. */
  tapOutsideBookLayout() {
    cy.window().then((win) => {
      cy.get('body').click(win.innerWidth - 10, win.innerHeight / 2)
    })
    return this
  },
})
