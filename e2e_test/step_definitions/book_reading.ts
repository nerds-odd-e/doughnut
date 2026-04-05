/**
 * Book-reading scenarios: thin glue to `e2e_test/start/pageObjects/cli`.
 */
import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import type { DataTable } from '@cucumber/cucumber'
import bookReadingPage, {
  type BookOutlineRow,
} from '../start/pageObjects/bookReadingPage'
import { cli } from '../start/pageObjects/cli'
import start from '../start'

function parseBookOutlineTable(data: DataTable): BookOutlineRow[] {
  return data.raw().map((row) => {
    const depth = parseInt(row[0] ?? '0', 10)
    const title = (row[1] ?? '').trim()
    return { depth, title }
  })
}

function pdfFixtureStem(fixtureFilename: string): string {
  return fixtureFilename.replace(/\.pdf$/i, '')
}

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
        ctx.pastCliAssistantMessages().expectContains('Main Topic 1')
        ctx.pastCliAssistantMessages().expectContains('Subtopic 1.1')
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
  'I should see the book structure in the browser:',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (data: DataTable) => {
    const expected = parseBookOutlineTable(data)
    return bookReadingPage().expectBookStructureRows(expected)
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
  'I choose the book outline row {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string) => {
    return bookReadingPage().clickOutlineRowByTitle(title)
  }
)

Then(
  'I should see PDF page 2 marker {string} in the book reader',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (marker: string) => {
    return bookReadingPage().expectPdfPageMarkerVisible(marker, 2)
  }
)

Then(
  'the book outline row {string} should be selected in the book reader',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string) => {
    return bookReadingPage().expectOutlineRowSelectedByTitle(title)
  }
)

Then(
  'same-page bbox outline entries should produce distinct PDF scroll positions',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().expectDistinctScrollForSamePageBboxOutline(
      'Subtopic 1.1',
      'Subtopic 1.2'
    )
  }
)
