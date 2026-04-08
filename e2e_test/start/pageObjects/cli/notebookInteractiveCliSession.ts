import { interactiveCli } from './interactiveCli'

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
        .expectContains(`Active notebook: ${notebookTitle}`)
    )
    .then(() => cy.wrap(notebookInteractiveCtx()))
}
