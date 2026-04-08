/**
 * Book-reading scenarios: thin glue to `e2e_test/start/pageObjects/cli`.
 */
import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import type { DataTable } from '@cucumber/cucumber'
import bookReadingPage, {
  type BookLayoutRow,
} from '../start/pageObjects/bookReadingPage'
import { cli } from '../start/pageObjects/cli'
import start from '../start'

function parseBookLayoutTable(data: DataTable): BookLayoutRow[] {
  return data.raw().map((row) => {
    const depth = parseInt(row[0] ?? '0', 10)
    const title = (row[1] ?? '').trim()
    return { depth, title }
  })
}

function pdfFixtureStem(fixtureFilename: string): string {
  return fixtureFilename.replace(/\.pdf$/i, '')
}

Given(
  'I set the book reading viewport to {int} by {int}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (width: number, height: number) => {
    return bookReadingPage().setBookReadingViewport(width, height)
  }
)

When(
  'I attach book {string} to the notebook {string} via the CLI',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (fixtureFilename: string, notebookTitle: string) => {
    const stem = pdfFixtureStem(fixtureFilename)
    cy.wrap(stem).as('attachedBookPdfStem')
    return cli
      .useNotebook(notebookTitle)
      .then((ctx) => ctx.attachPdfBook(fixtureFilename))
      .then((ctx) => {
        ctx.pastCliAssistantMessages().expectContains(`Attached "${stem}"`)
        ctx
          .pastCliAssistantMessages()
          .expectContains('Protecting Intention in Working Software')
        ctx.pastCliAssistantMessages().expectContains('Easier to Change')
      })
  }
)

When(
  'I open the book attached to notebook {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (notebookTitle: string) => {
    return cy.get<string>('@attachedBookPdfStem').then((stem) => {
      start.navigateToNotebookPage(notebookTitle).readBook(stem)
    })
  }
)

Then(
  'I should see the book layout in the browser:',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (data: DataTable) => {
    const expected = parseBookLayoutTable(data)
    return bookReadingPage().expectBookLayoutRows(expected)
  }
)

Then(
  'I should see the beginning of the PDF book {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (_fixtureFilename: string) => {
    return bookReadingPage().expectPdfBeginningVisible()
  }
)

When(
  'I scroll the PDF book reader to bring page 2 into primary view',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().scrollPdfBookReaderToBringPage2IntoPrimaryView()
  }
)

When(
  'I scroll the PDF book reader down within the same page to move viewport past the next book range bbox',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().scrollPdfBookReaderDownWithinSamePageForNextBbox()
  }
)

When(
  'I choose the book range {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string) => {
    return bookReadingPage().clickBookRangeByTitle(title)
  }
)

When(
  'I scroll the PDF until the book range {string} is the current range in the book reader',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (rangeTitle: string) => {
    return bookReadingPage().scrollPdfBookReaderToMakeBookRangeCurrent(
      rangeTitle
    )
  }
)

When(
  'I mark the book range {string} as read in the Reading Control Panel',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (rangeTitle: string) => {
    return bookReadingPage().markBookRangeAsReadInReadingControlPanel(
      rangeTitle
    )
  }
)

Then(
  'I should see that book range {string} is marked as read in the book layout',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string) => {
    return bookReadingPage().expectBookRangeMarkedAsReadInBookLayout(title)
  }
)

Then(
  'I should see that book range {string} is selected in the book layout',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string) => {
    return bookReadingPage().expectBookRangeIsCurrentSelectionByTitle(title)
  }
)

Then(
  'I should see in the book reader visible PDF viewport on page {int} text including {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (pageNumber: number, marker: string) => {
    return bookReadingPage()
      .expectCurrentPage(pageNumber)
      .expectVisibleOCRContains(marker)
  }
)

Then(
  'the book range {string} should be the current selection in the book reader',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string) => {
    return bookReadingPage().expectBookRangeIsCurrentSelectionByTitle(title)
  }
)

Then(
  'the book range {string} should be the current range in the book reader',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string) => {
    return bookReadingPage().expectBookRangeIsCurrentRangeByTitle(title)
  }
)

Then(
  'the book range {string} should be the current range and visible in the book layout aside',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string) => {
    return bookReadingPage().expectCurrentRangeVisibleInBookLayoutAside(title)
  }
)
