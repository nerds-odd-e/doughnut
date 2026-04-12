/**
 * Book-reading scenarios: thin glue to `e2e_test/start/pageObjects/bookReadingPage`.
 */
import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import type { DataTable } from '@cucumber/cucumber'
import bookReadingPage, {
  type BookLayoutRow,
} from '../start/pageObjects/bookReadingPage'
import { cli } from '../start/pageObjects/cli'
import start from '../start'
import testability from '../start/testability'

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
  'I attach a book with MinerU fixture {string} to the notebook {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (fixtureStem: string, notebookTitle: string) => {
    const bookName = fixtureStem
    cy.wrap(bookName).as('attachedBookPdfStem')
    return cy
      .fixture(`book_reading/mineru_output_for_${fixtureStem}.json`)
      .then((contentList: unknown) => {
        return testability().attachBookToNotebook(
          notebookTitle,
          bookName,
          contentList as Array<unknown>
        )
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
  'I scroll the PDF book reader down within the same page to move viewport past the next book block bbox',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().scrollPdfBookReaderDownWithinSamePageForNextBbox()
  }
)

When(
  'I choose the book block {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string) => {
    return bookReadingPage().clickBookBlockByTitle(title)
  }
)

When(
  'I scroll the PDF until the book block {string} is the current block in the book reader',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (blockTitle: string) => {
    return bookReadingPage().scrollPdfBookReaderToMakeBookBlockCurrent(
      blockTitle
    )
  }
)

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

Then(
  'I should see that book block {string} is marked as read in the book layout',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string) => {
    return bookReadingPage().expectBookBlockMarkedAsReadInBookLayout(title)
  }
)

Then(
  'I should see that book block {string} is selected in the book layout',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string) => {
    return bookReadingPage().expectBookBlockIsCurrentSelectionByTitle(title)
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
  'the book block {string} should be the current selection in the book reader',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string) => {
    return bookReadingPage().expectBookBlockIsCurrentSelectionByTitle(title)
  }
)

Then(
  'the book block {string} should be the current block in the book reader',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string) => {
    return bookReadingPage().expectBookBlockIsCurrentBlockByTitle(title)
  }
)

Then(
  'the book block {string} should be the current block and visible in the book layout aside',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string) => {
    return bookReadingPage().expectCurrentBlockVisibleInBookLayoutAside(title)
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
  'I click "Read from here" in the current block navigation bar',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().clickReadFromHere()
  }
)

When(
  'I click "Back to selected" in the current block navigation bar',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().clickBackToSelected()
  }
)

Then(
  'the current block navigation bar should not be visible',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().expectCurrentBlockNavigationBarNotVisible()
  }
)
