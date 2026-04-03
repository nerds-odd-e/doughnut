import { interactiveCli } from './interactiveCli'

const NOTEBOOK_ACTIVE_TIMEOUT_MS = 20_000

/**
 * Book-reading CLI flows: `/use` + `/attach` against fixtures under
 * `e2e_test/fixtures/book_reading/`.
 */
export function bookReadingCli() {
  return {
    attachBookPdfToNotebookViaInteractiveCli(opts: {
      fixtureFilename: string
      notebookTitle: string
    }): void {
      const { fixtureFilename, notebookTitle } = opts
      cy.task<string>(
        'getE2eFixtureAbsolutePath',
        `book_reading/${fixtureFilename}`
      ).then((absPdfPath) => {
        const ic = interactiveCli()
        ic.enterSlashCommandInInteractiveCli(`/use ${notebookTitle}`)
          .then(() => {
            ic.pastCliAssistantMessages().expectContains(
              `Active notebook: ${notebookTitle}`,
              { timeoutMs: NOTEBOOK_ACTIVE_TIMEOUT_MS }
            )
          })
          .then(() =>
            ic.enterSlashCommandInInteractiveCli(`/attach ${absPdfPath}`)
          )
          .then(() => {
            ic.pastCliAssistantMessages().expectContainsBookReadingMineruStubLayoutExcerpt()
          })
      })
    },
  }
}
