/**
 * Book reading progress (mark read / skimmed, current-block navigation).
 */
import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import bookReadingPage from '../start/pageObjects/bookReadingPage'

When(
  'I scroll the PDF book reader until the Reading Control Panel shows for {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (selectedBlockTitle: string) => {
    return bookReadingPage().scrollPdfUntilReadingControlPanelVisible(
      selectedBlockTitle
    )
  }
)

When('I wheel down over the Reading Control Panel', () => {
  bookReadingPage().wheelDownOverReadingControlPanel()
})

Then('the PDF book reader should have scrolled down', () => {
  bookReadingPage().expectPdfScrolledDownSinceWheel()
})

When(
  'I mark the book block {string} as read in the Reading Control Panel',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (blockTitle: string) => {
    return bookReadingPage().markBookBlockAsReadInReadingControlPanel(
      blockTitle
    )
  }
)

When(
  'I mark the book block {string} as skimmed in the Reading Control Panel',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (blockTitle: string) => {
    return bookReadingPage().markBookBlockAsSkimmedInReadingControlPanel(
      blockTitle
    )
  }
)

When(
  'I change the mark of book block {string} to read in the book layout',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (blockTitle: string) => {
    return bookReadingPage().changeBookBlockMarkToReadInBookLayout(blockTitle)
  }
)

When(
  'I clear the mark of book block {string} in the book layout',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (blockTitle: string) => {
    return bookReadingPage().clearBookBlockMarkInBookLayout(blockTitle)
  }
)

When(
  'I open the book again',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().openBookAgain()
  }
)

Then(
  'I should see that book block {string} is marked as read in the book layout',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string) => {
    return bookReadingPage().expectBookBlockMarkedAsReadInBookLayout(title)
  }
)

Then(
  'I should see that book block {string} is marked as skimmed in the book layout',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string) => {
    return bookReadingPage().expectBookBlockMarkedAsSkimmedInBookLayout(title)
  }
)

Then(
  'book block {string} should not be marked in the book layout',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string) => {
    return bookReadingPage().expectBookBlockNotMarkedInBookLayout(title)
  }
)

Then(
  'no book block should be marked in the book layout',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().expectNoBookBlockMarkedInBookLayout()
  }
)

Then(
  'I should see the current block navigation bar showing {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string) => {
    return bookReadingPage().expectCurrentBlockNavigationBar(title)
  }
)

When(
  'I start reading from the current block',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().startReadingFromCurrentBlock()
  }
)

When(
  'I go back to the selected book block',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().goBackToSelectedBookBlock()
  }
)

Then(
  'the current block navigation bar should not be visible',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().expectCurrentBlockNavigationBarNotVisible()
  }
)

Then(
  'the Reading Control Panel should be fully on the screen',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().expectReadingControlPanelFullyOnScreen()
  }
)
