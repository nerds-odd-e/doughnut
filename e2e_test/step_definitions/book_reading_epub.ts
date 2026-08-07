/**
 * EPUB book-reading scenarios: thin glue to `bookReadingPage`.
 */
import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import bookReadingPage from '../start/pageObjects/bookReadingPage'

Then(
  'I should see the EPUB reading view with book name {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (name: string) => {
    return bookReadingPage().expectEpubReadingViewShowsBookName(name)
  }
)

Then(
  'I should see the text {string} in the EPUB reader',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (text: string) => {
    return bookReadingPage().expectEpubContentTextVisible(text)
  }
)

Then(
  'the book layout block {string} should have epub start href containing {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string, substring: string) => {
    return bookReadingPage().expectBookLayoutBlockEpubStartHrefContains(
      title,
      substring
    )
  }
)

When(
  'I leave the EPUB reading view and return to it',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().leaveEpubReadingViewAndReturn()
  }
)

When(
  'I scroll the EPUB reader until the text {string} is in the viewport',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (markerText: string) => {
    return bookReadingPage().scrollEpubReaderUntilTextInViewport(markerText)
  }
)

When(
  'I scroll the EPUB reader host to the top',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().scrollEpubReaderHostToTop()
  }
)

Then(
  'the EPUB Reading Control Panel should be content-anchored',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().expectEpubReadingControlPanelContentAnchored()
  }
)
