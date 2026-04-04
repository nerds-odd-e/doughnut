/**
 * Book-reading scenarios: thin glue to `e2e_test/start/pageObjects/cli`.
 */
import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import type { DataTable } from '@cucumber/cucumber'
import type { BookOutlineRow } from '../start/pageObjects/bookReadingPage'
import { cli } from '../start/pageObjects/cli'
import start from '../start'

function parseBookOutlineTable(data: DataTable): BookOutlineRow[] {
  return data.raw().map((row) => {
    const depth = parseInt(row[0] ?? '0', 10)
    const title = (row[1] ?? '').trim()
    return { depth, title }
  })
}

When(
  'I attach book {string} to the notebook {string} via the CLI',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (fixtureFilename: string, notebookTitle: string) => {
    return cli
      .useNotebook(notebookTitle)
      .then((ctx) => ctx.attachPdfBook(fixtureFilename))
      .then((ctx) => {
        ctx.pastCliAssistantMessages().expectContains('Attached "top-maths"')
        ctx.pastCliAssistantMessages().expectContains('Main Topic 1')
        ctx.pastCliAssistantMessages().expectContains('Subtopic 1.1')
      })
  }
)

Then(
  'I should see the book structure of the notebook {string} in the browser:',
  (notebookTitle: string, data: DataTable) => {
    const expected = parseBookOutlineTable(data)
    start
      .navigateToNotebookPage(notebookTitle)
      .readBook('top-maths')
      .expectBookStructureRows(expected)
  }
)
