/**
 * Book-reading scenarios: thin glue to `e2e_test/start/pageObjects/cli`.
 */
import { When } from '@badeball/cypress-cucumber-preprocessor'
import { cli } from '../start/pageObjects/cli'

When(
  'I attach book {string} to the notebook {string} via the CLI',
  (fixtureFilename: string, notebookTitle: string) => {
    cli
      .useNotebook(notebookTitle)
      .then((ctx) => ctx.attachPdfBook(fixtureFilename))
      .then((ctx) => {
        ctx.pastCliAssistantMessages().expectContains('Attached "top-maths"')
        ctx.pastCliAssistantMessages().expectContains('Stub Part A')
        ctx.pastCliAssistantMessages().expectContains('Stub Section One')
      })
  }
)
