import { interactiveCli } from './interactiveCli'

const NOTEBOOK_ACTIVE_TIMEOUT_MS = 2_000

function notebookInteractiveCtx() {
  return {
    attachPdfBook(fixtureFilename: string) {
      return cy
        .task<string>(
          'getE2eFixtureAbsolutePath',
          `book_reading/${fixtureFilename}`
        )
        .then((absPath) =>
          interactiveCli().enterSlashCommandInInteractiveCli(
            `/attach ${absPath}`
          )
        )
        .then(() => cy.wrap(interactiveCli()))
    },
  }
}

export function useNotebook(
  notebookTitle: string
): Cypress.Chainable<ReturnType<typeof notebookInteractiveCtx>> {
  const ic = interactiveCli()
  return ic
    .enterSlashCommandInInteractiveCli(`/use ${notebookTitle}`)
    .then(() =>
      ic
        .pastCliAssistantMessages()
        .expectContains(`Active notebook: ${notebookTitle}`, {
          timeoutMs: NOTEBOOK_ACTIVE_TIMEOUT_MS,
        })
    )
    .then(() => cy.wrap(notebookInteractiveCtx()))
}
