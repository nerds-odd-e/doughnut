/**
 * Book-reading attach / open / browse: thin glue to `bookReadingPage`.
 */
import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import type { DataTable } from '@cucumber/cucumber'
import bookReadingPage from '../start/pageObjects/bookReadingPage'
import { cli } from '../start/pageObjects/cli'
import notebookPage from '../start/pageObjects/notebookPage'
import start from '../start'
import testability from '../start/testability'
import { parseBookLayoutTable, pdfFixtureStem } from './book_reading_helpers'

const mineruOutputFixture = (stem: string) =>
  cy
    .fixture(`book_reading/mineru_output_for_${stem}.json`)
    .then((contentList: unknown) => contentList as Array<unknown>)

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
  (fixtureFilename: string, notebookName: string) => {
    const stem = pdfFixtureStem(fixtureFilename)
    return cli
      .useNotebook(notebookName)
      .then((ctx) => ctx.attachPdfBook(fixtureFilename))
      .then((ctx) => {
        ctx.pastCliAssistantMessages().expectContains(`Attached "${stem}"`)
      })
  }
)

When(
  'I attach a fake blank pdf book with book layout of {string} to the notebook {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (fixtureStem: string, notebookName: string) => {
    return mineruOutputFixture(fixtureStem).then((contentList) =>
      testability().attachBlankPdfBookToNotebook(
        notebookName,
        fixtureStem,
        contentList
      )
    )
  }
)

When(
  'I attach the pdf book {string} with its MinerU output to the notebook {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (fixtureFilename: string, notebookName: string) => {
    const stem = pdfFixtureStem(fixtureFilename)
    return mineruOutputFixture(stem).then((contentList) =>
      testability().attachBookToNotebook(
        notebookName,
        stem,
        contentList,
        fixtureFilename
      )
    )
  }
)

When(
  'I open the book attached to notebook {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (notebookName: string) => {
    return start.jumpToBookReadingPage(notebookName)
  }
)

When('I open the notebook settings for {string}', (notebookName: string) => {
  start.navigateToNotebookPage(notebookName)
})

When(
  'I attach the EPUB file {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (relativePath: string) => {
    return notebookPage().attachEpubFixture(relativePath)
  }
)

When(
  'I attempt to attach the EPUB file {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (relativePath: string) => {
    return notebookPage().attemptAttachEpubFixture(relativePath)
  }
)

When(
  'I open the reading view for the attached book {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (bookTitle: string) => {
    return notebookPage().readBook(bookTitle)
  }
)

Then(
  'I should see an EPUB attach error containing {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (messageSubstring: string) => {
    return notebookPage().expectEpubAttachErrorContaining(messageSubstring)
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
  'I scroll the PDF book reader to {int} of 1000 down page {int}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (normalizedY: number, pageNumber: number) => {
    return bookReadingPage().scrollPdfBookReaderToPosition(
      pageNumber,
      normalizedY
    )
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
    return bookReadingPage().chooseBookBlockByTitle(title)
  }
)

Then(
  'the book reader PDF viewport should be on page {int}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (pageNumber: number) => {
    return bookReadingPage().expectCurrentPage(pageNumber)
  }
)

Then(
  'the top of the PDF book reader should be at {int} of 1000 down page {int}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (normalizedY: number, pageNumber: number) => {
    return bookReadingPage().expectPdfPositionAtTopOfReader(
      pageNumber,
      normalizedY
    )
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
  'the book layout should not offer AI reorganization',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().expectNoAiReorganizationOfBookLayout()
  }
)

Then(
  'the current block in the book layout should not be the selected block',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().expectBookLayoutCurrentBlockDiffersFromSelection()
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
  'I should see that book block {string} is selected in the book layout',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string) => {
    return bookReadingPage().expectBookBlockIsCurrentSelectionByTitle(title)
  }
)

Then(
  'the PDF pages should use the screen width',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().expectPdfPagesUseScreenWidth()
  }
)

When('I tap the Book layout toggle', () => {
  bookReadingPage().clickBookLayoutToggle()
})

Then('the book layout should be open', () => {
  bookReadingPage().expectBookLayoutOpen()
})

Then('the book layout should be closed', () => {
  bookReadingPage().expectBookLayoutClosed()
})

Then('the book layout should be below the main menu', () => {
  bookReadingPage().expectBookLayoutBelowMainMenu()
})

When('I tap outside the book layout', () => {
  bookReadingPage().tapOutsideBookLayout()
})
