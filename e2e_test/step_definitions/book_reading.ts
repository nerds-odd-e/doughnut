/**
 * Book-reading scenarios: thin glue to `e2e_test/start/pageObjects/cli`.
 */
import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import { cli } from '../start/pageObjects/cli'
import start from '../start'

When(
  'I attach book {string} to the notebook {string} via the CLI',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (fixtureFilename: string, notebookTitle: string) => {
    return cli
      .useNotebook(notebookTitle)
      .then((ctx) => ctx.attachPdfBook(fixtureFilename))
      .then((ctx) => {
        ctx.pastCliAssistantMessages().expectContains('Attached "top-maths"')
        ctx.pastCliAssistantMessages().expectContains('Stub Part A')
        ctx.pastCliAssistantMessages().expectContains('Stub Section One')
      })
  }
)

Then(
  'I should see attached book {string} with a Read control on notebook {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (bookTitle: string, notebookTitle: string) => {
    return start
      .navigateToNotebookPage(notebookTitle)
      .expectAttachedBookSectionWithRead(bookTitle)
  }
)
