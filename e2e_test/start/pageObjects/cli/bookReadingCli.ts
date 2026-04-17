import { interactiveCli } from './interactiveCli'

/**
 * Book-reading CLI flows: `/use` + `/attach` against fixtures under
 * `e2e_test/fixtures/book_reading/`.
 */
export function bookReadingCli() {
  return {
    attachBookToNotebookViaInteractiveCli(opts: {
      fixtureFilename: string
      notebookTitle: string
    }): void {
      const { fixtureFilename, notebookTitle } = opts
      cy.task<string>(
        'getE2eFixtureAbsolutePath',
        `book_reading/${fixtureFilename}`
      ).then((absBookPath) => {
        const ic = interactiveCli()
        ic.enterSlashCommandInInteractiveCli(`/use ${notebookTitle}`)
          .then(() => {
            ic.pastCliAssistantMessages().expectContains(
              `Active notebook: ${notebookTitle}`
            )
          })
          .then(() =>
            ic.enterSlashCommandInInteractiveCli(`/attach ${absBookPath}`)
          )
          .then(() => {
            ic.pastCliAssistantMessages().expectContains('Attaching book...')
          })
      })
    },
  }
}
