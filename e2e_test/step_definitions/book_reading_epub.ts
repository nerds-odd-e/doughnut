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

Then(
  'the EPUB Reading Control Panel should be content-anchored',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().expectEpubReadingControlPanelContentAnchored()
  }
)

Then(
  'the EPUB text should use the screen width',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().expectEpubTextUsesScreenWidth()
  }
)

When(
  'I scroll the EPUB reader until the heading {string} passes the top',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (headingText: string) => {
    return bookReadingPage().scrollEpubReaderUntilHeadingPassesTop(headingText)
  }
)

Then(
  'the heading {string} should be at the top of the EPUB reader',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (headingText: string) => {
    return bookReadingPage().expectEpubHeadingAtTopOfReader(headingText)
  }
)

When(
  'I scroll the EPUB reader until the paragraph {string} is at the top',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (paragraphText: string) => {
    return bookReadingPage().scrollEpubReaderUntilParagraphIsAtTop(
      paragraphText
    )
  }
)

Then(
  'the paragraph {string} should be at the top of the EPUB reader',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (paragraphText: string) => {
    return bookReadingPage().expectEpubParagraphAtTopOfReader(paragraphText)
  }
)

When(
  'I follow the link {string} in the EPUB reader',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (linkText: string) => {
    return bookReadingPage().followEpubLinkInReader(linkText)
  }
)
