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
