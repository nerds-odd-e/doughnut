/**
 * Book-reading scenarios: thin glue to `e2e_test/start/pageObjects/cli`.
 */
import { When } from '@badeball/cypress-cucumber-preprocessor'
import { cli } from '../start/pageObjects/cli'

When(
  'I attach book {string} to the notebook {string} via the CLI',
  (fixtureFilename: string, notebookTitle: string) => {
    cli.bookReading().attachBookPdfToNotebookViaInteractiveCli({
      fixtureFilename,
      notebookTitle,
    })
  }
)
