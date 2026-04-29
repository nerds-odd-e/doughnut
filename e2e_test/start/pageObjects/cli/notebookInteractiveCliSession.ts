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
  notebookName: string
): Cypress.Chainable<ReturnType<typeof notebookInteractiveCtx>> {
  const ic = interactiveCli()
  return ic
    .enterSlashCommandInInteractiveCli(`/use ${notebookName}`)
    .then(() =>
      ic
        .pastCliAssistantMessages()
        .expectContains(`Active notebook: ${notebookName}`)
    )
    .then(() => cy.wrap(notebookInteractiveCtx()))
}
