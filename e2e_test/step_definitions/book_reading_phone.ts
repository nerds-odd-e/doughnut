/**
 * Book layout drawer on narrow screens: thin glue to `bookReadingPage`.
 */
import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import bookReadingPage from '../start/pageObjects/bookReadingPage'

When('I tap the Book layout toggle', () => {
  bookReadingPage().clickBookLayoutToggle()
})

Then('the book layout should be open', () => {
  bookReadingPage().expectBookLayoutOpen()
})

Then('the book layout should be closed', () => {
  bookReadingPage().expectBookLayoutClosed()
})

Then('the book layout should be below the page bar', () => {
  bookReadingPage().expectBookLayoutBelowPageBar()
})

When('I tap outside the book layout', () => {
  bookReadingPage().tapOutsideBookLayout()
})
